package com.github.wisesky0;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Writes extraction results to console and/or file in text or CSV format.
 */
public class ReportWriter {

    private List<GenericGeneratorInfo> results;
    private boolean csvFormat;

    public ReportWriter(List<GenericGeneratorInfo> results, boolean csvFormat) {
        this.results = results;
        this.csvFormat = csvFormat;
    }

    /**
     * Write to console (stdout).
     */
    public void writeToConsole() {
        if (results.isEmpty()) {
            System.out.println("[No @GenericGenerator(strategy=...) annotations found]");
            return;
        }

        if (csvFormat) {
            writeConsoleCSV();
        } else {
            writeConsoleText();
        }
    }

    /**
     * Write to a file.
     *
     * @param reportFile the output file path
     * @throws IOException if file write fails
     */
    public void writeToFile(String reportFile) throws IOException {
        if (reportFile == null || reportFile.trim().isEmpty()) {
            return;
        }

        File file = new File(reportFile);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            if (csvFormat) {
                writeFileCSV(writer);
            } else {
                writeFileText(writer);
            }
        }

        System.out.println("[Report written to: " + reportFile + "]");
    }

    /**
     * Write console output in text format.
     */
    private void writeConsoleText() {
        System.out.println("[Found " + results.size() + " @GenericGenerator(strategy=...) annotations]");
        System.out.println();

        for (int i = 0; i < results.size(); i++) {
            GenericGeneratorInfo info = results.get(i);
            System.out.println("[" + (i + 1) + "] File: " + info.getFilePath());
            System.out.println("    Class: " + info.getClassName());
            System.out.println("    Line: " + info.getLineNumber());
            System.out.println("    Name: " + info.getName());
            System.out.println("    Strategy (AS-IS): " + info.getStrategy());
            System.out.println("    TO-BE Migration Hint: Change to type = " + info.getStrategy() + ".class");
            System.out.println();
        }
    }

    /**
     * Write console output in CSV format.
     */
    private void writeConsoleCSV() {
        System.out.println("File,Class,Line,Name,Strategy");

        for (GenericGeneratorInfo info : results) {
            System.out.println(
                    escapeCSV(info.getFilePath()) + "," +
                    escapeCSV(info.getClassName()) + "," +
                    info.getLineNumber() + "," +
                    escapeCSV(info.getName()) + "," +
                    escapeCSV(info.getStrategy())
            );
        }
    }

    /**
     * Write file output in text format.
     */
    private void writeFileText(Writer writer) throws IOException {
        writer.write("[Found " + results.size() + " @GenericGenerator(strategy=...) annotations]\n");
        writer.write("\n");

        for (int i = 0; i < results.size(); i++) {
            GenericGeneratorInfo info = results.get(i);
            writer.write("[" + (i + 1) + "] File: " + info.getFilePath() + "\n");
            writer.write("    Class: " + info.getClassName() + "\n");
            writer.write("    Line: " + info.getLineNumber() + "\n");
            writer.write("    Name: " + info.getName() + "\n");
            writer.write("    Strategy (AS-IS): " + info.getStrategy() + "\n");
            writer.write("    TO-BE Migration Hint: Change to type = " + info.getStrategy() + ".class\n");
            writer.write("\n");
        }
    }

    /**
     * Write file output in CSV format.
     */
    private void writeFileCSV(Writer writer) throws IOException {
        writer.write("File,Class,Line,Name,Strategy\n");

        for (GenericGeneratorInfo info : results) {
            writer.write(
                    escapeCSV(info.getFilePath()) + "," +
                    escapeCSV(info.getClassName()) + "," +
                    info.getLineNumber() + "," +
                    escapeCSV(info.getName()) + "," +
                    escapeCSV(info.getStrategy()) + "\n"
            );
        }
    }

    /**
     * Escape CSV field values (surround with quotes if contains comma, quote, or newline).
     */
    private String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
