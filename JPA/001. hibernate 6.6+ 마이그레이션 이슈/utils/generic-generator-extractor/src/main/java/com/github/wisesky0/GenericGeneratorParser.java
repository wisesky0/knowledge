package com.github.wisesky0;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for extracting @GenericGenerator annotations from Java source files.
 * Uses JavaParser AST to identify @GenericGenerator with strategy member.
 */
public class GenericGeneratorParser {

    private String filePath;
    private List<GenericGeneratorInfo> results;

    public GenericGeneratorParser(String filePath) {
        this.filePath = filePath;
        this.results = new ArrayList<>();
    }

    /**
     * Parse a Java file and extract @GenericGenerator annotations with strategy attribute.
     *
     * @param sourceFile the Java source file to parse
     * @return list of extracted GenericGeneratorInfo objects
     * @throws Exception if parsing fails
     */
    public List<GenericGeneratorInfo> parse(File sourceFile) throws Exception {
        this.results = new ArrayList<>();

        // Parse the file into a CompilationUnit (AST root)
        CompilationUnit cu = StaticJavaParser.parse(sourceFile);

        // Visit all annotation expressions in the tree
        cu.walk(NormalAnnotationExpr.class, annotation -> visitAnnotation(annotation, cu));

        return results;
    }

    /**
     * Visit a NormalAnnotationExpr and check if it's @GenericGenerator with strategy.
     */
    private void visitAnnotation(NormalAnnotationExpr annotation, CompilationUnit cu) {
        // Check if this is a @GenericGenerator annotation
        String annotationName = annotation.getNameAsString();
        if (!"GenericGenerator".equals(annotationName)) {
            return;
        }

        // Look for 'strategy' member in the annotation
        String strategy = null;
        String name = null;

        for (com.github.javaparser.ast.expr.MemberValuePair pair : annotation.getPairs()) {
            String memberName = pair.getNameAsString();

            if ("strategy".equals(memberName)) {
                // Extract strategy value (must be a string literal)
                if (pair.getValue() instanceof StringLiteralExpr) {
                    strategy = ((StringLiteralExpr) pair.getValue()).getValue();
                }
            } else if ("name".equals(memberName)) {
                // Extract name value (for reference)
                if (pair.getValue() instanceof StringLiteralExpr) {
                    name = ((StringLiteralExpr) pair.getValue()).getValue();
                }
            }
        }

        // Only collect if strategy is present (TO-BE 'type' attribute is auto-excluded)
        if (strategy != null) {
            String className = findEnclosingClassName(annotation);
            int lineNumber = annotation.getBegin().map(pos -> pos.line).orElse(-1);

            GenericGeneratorInfo info = new GenericGeneratorInfo(
                    filePath,
                    className,
                    lineNumber,
                    name != null ? name : "(unnamed)",
                    strategy
            );
            results.add(info);
        }
    }

    /**
     * Find the name of the class that encloses this annotation.
     */
    private String findEnclosingClassName(AnnotationExpr annotation) {
        // Walk up the parent chain until we find a ClassOrInterfaceDeclaration
        java.util.Optional<com.github.javaparser.ast.Node> currentNode = annotation.getParentNode();
        while (currentNode.isPresent()) {
            com.github.javaparser.ast.Node node = currentNode.get();
            if (node instanceof ClassOrInterfaceDeclaration) {
                return ((ClassOrInterfaceDeclaration) node).getNameAsString();
            }
            currentNode = node.getParentNode();
        }
        return "(unknown)";
    }

    public List<GenericGeneratorInfo> getResults() {
        return results;
    }
}
