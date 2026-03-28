package com.github.wisesky0;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Maven Mojo for extracting @GenericGenerator(strategy=...) annotations.
 * Recursively scans source directory and generates console/file reports.
 *
 * Usage:
 *   mvn com.github.wisesky0:generic-generator-extractor-maven-plugin:1.0.0:extract \
 *       -DsourceDir=./src/main/java
 *
 *   With file output:
 *   mvn ...:extract -DsourceDir=./src -DreportFile=./target/report.txt
 *
 *   With CSV format:
 *   mvn ...:extract -DsourceDir=./src -DreportFile=./target/report.csv -DcsvFormat=true
 */
@Mojo(name = "extract", requiresProject = false)
public class ExtractGenericGeneratorMojo extends AbstractMojo {

    /**
     * Source directory containing Java files to scan.
     * Default: ${project.basedir}/src/main/java
     */
    @Parameter(property = "sourceDir", defaultValue = "${project.basedir}/src/main/java")
    private File sourceDir;

    /**
     * Optional output file path for the report.
     */
    @Parameter(property = "reportFile")
    private String reportFile;

    /**
     * Whether to use CSV format for output (default: false = text format).
     */
    @Parameter(property = "csvFormat", defaultValue = "false")
    private boolean csvFormat;

    /**
     * Whether to fail the build if parsing errors occur (default: false).
     */
    @Parameter(property = "failOnError", defaultValue = "false")
    private boolean failOnError;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // Validate sourceDir
            if (sourceDir == null) {
                throw new MojoExecutionException("sourceDir parameter is required");
            }

            if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                throw new MojoExecutionException(
                        "sourceDir does not exist or is not a directory: " + sourceDir.getAbsolutePath()
                );
            }

            getLog().info("Scanning source directory: " + sourceDir.getAbsolutePath());

            // Collect all Java files
            List<File> javaFiles = collectJavaFiles(sourceDir);
            getLog().info("Found " + javaFiles.size() + " Java files");

            // Parse all files and collect results
            List<GenericGeneratorInfo> allResults = new ArrayList<>();
            int parseErrors = 0;

            for (File javaFile : javaFiles) {
                try {
                    GenericGeneratorParser parser = new GenericGeneratorParser(
                            getRelativePath(javaFile)
                    );
                    List<GenericGeneratorInfo> fileResults = parser.parse(javaFile);
                    allResults.addAll(fileResults);
                } catch (Exception e) {
                    parseErrors++;
                    getLog().warn("Failed to parse " + javaFile.getAbsolutePath() + ": " + e.getMessage());
                    if (failOnError) {
                        throw new MojoExecutionException("Parse error in " + javaFile.getAbsolutePath(), e);
                    }
                }
            }

            // Write report
            ReportWriter writer = new ReportWriter(allResults, csvFormat);
            writer.writeToConsole();

            if (reportFile != null && !reportFile.trim().isEmpty()) {
                try {
                    writer.writeToFile(reportFile);
                } catch (Exception e) {
                    throw new MojoExecutionException("Failed to write report file: " + reportFile, e);
                }
            }

            // Summary
            getLog().info("");
            getLog().info("========================================");
            getLog().info("Total @GenericGenerator(strategy) found: " + allResults.size());
            if (parseErrors > 0) {
                getLog().warn("Parse errors: " + parseErrors);
            }
            getLog().info("========================================");

        } catch (Exception e) {
            throw new MojoExecutionException("Error during execution", e);
        }
    }

    /**
     * Recursively collect all Java files in a directory.
     */
    private List<File> collectJavaFiles(File dir) {
        List<File> javaFiles = new ArrayList<>();

        if (!dir.isDirectory()) {
            return javaFiles;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return javaFiles;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                javaFiles.addAll(collectJavaFiles(file));
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }

        return javaFiles;
    }

    /**
     * Get file path relative to sourceDir for cleaner output.
     */
    private String getRelativePath(File file) {
        try {
            String sourcePath = sourceDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();

            if (filePath.startsWith(sourcePath)) {
                return filePath.substring(sourcePath.length() + 1)
                        .replace(File.separator, "/");
            }
        } catch (Exception e) {
            // Fallback to absolute path if relative path calculation fails
        }

        return file.getAbsolutePath();
    }
}
