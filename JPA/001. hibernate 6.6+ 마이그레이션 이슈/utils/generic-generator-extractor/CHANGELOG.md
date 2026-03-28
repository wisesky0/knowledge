# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-03-28

### Added
- `ExtractGenericGeneratorMojo` - Maven plugin goal for scanning Java source files
- `GenericGeneratorParser` - JavaParser-based AST analyzer to detect `@GenericGenerator(strategy=...)` annotations
- `GenericGeneratorInfo` - Data model for holding extracted annotation metadata
- `ReportWriter` - Report output writer (console / file)
- Java 8 compatible implementation using JavaParser 3.25.10
- Support for Hibernate 5.x → 6.x migration analysis

### Dependencies
- `maven-plugin-api:3.6.3`
- `maven-plugin-annotations:3.6.4`
- `javaparser-core:3.25.10`
- `slf4j-api:1.7.36`