package com.github.wisesky0;

/**
 * Data transfer object holding extracted @GenericGenerator annotation information.
 * Java 8 compatible: manual constructor, getters, toString (no Lombok).
 */
public class GenericGeneratorInfo {

    private String filePath;
    private String className;
    private int lineNumber;
    private String name;
    private String strategy;

    public GenericGeneratorInfo(String filePath, String className, int lineNumber, String name, String strategy) {
        this.filePath = filePath;
        this.className = className;
        this.lineNumber = lineNumber;
        this.name = name;
        this.strategy = strategy;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    @Override
    public String toString() {
        return "GenericGeneratorInfo{" +
                "filePath='" + filePath + '\'' +
                ", className='" + className + '\'' +
                ", lineNumber=" + lineNumber +
                ", name='" + name + '\'' +
                ", strategy='" + strategy + '\'' +
                '}';
    }
}
