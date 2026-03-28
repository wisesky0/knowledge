# Generic Generator Extractor Maven Plugin

## 개요

Hibernate 5.x → 6.x 마이그레이션 시 `@GenericGenerator` 어노테이션의 `strategy` 속성이 `type` 속성으로 변경됩니다.
이 Maven 플러그인은 대상 소스 코드에서 **모든** `@GenericGenerator(strategy = "...")` 어노테이션을 자동으로 추출하여 마이그레이션 영향도를 사전 분석합니다.

## 빌드 및 설치

```bash
cd utils/generic-generator-extractor
mvn clean install
```

플러그인이 로컬 Maven 저장소에 설치됩니다:
```
~/.m2/repository/com/github/wisesky0/generic-generator-extractor-maven-plugin/1.0.0/
```

## 사용 방법

### 기본 실행 (콘솔 출력)

```bash
mvn com.github.wisesky0:generic-generator-extractor-maven-plugin:1.0.0:extract \
    -DsourceDir=./src/main/java
```

**출력 예시:**
```
[Found 2 @GenericGenerator(strategy=...) annotations]

[1] File: com/github/wisesky0/asis/Article.java
    Class: Article
    Line: 17
    Name: custom-id
    Strategy (AS-IS): com.github.wisesky0.asis.CustomIdGenerator
    TO-BE Migration Hint: Change to type = com.github.wisesky0.asis.CustomIdGenerator.class

[2] File: com/github/wisesky0/asis/ArticleVersioned.java
    ...
```

### 파라미터

| 파라미터 | 기본값 | 설명 |
|---------|-------|------|
| `sourceDir` | `${project.basedir}/src/main/java` | 스캔할 Java 소스 디렉토리 |
| `reportFile` | _(선택)_ | 보고서 저장 파일 경로 (미설정 시 콘솔 출력만) |
| `csvFormat` | `false` | CSV 형식 사용 여부 |
| `failOnError` | `false` | 파싱 에러 발생 시 빌드 실패 여부 |

### 텍스트 파일로 보고서 저장

```bash
mvn ...:extract \
    -DsourceDir=./src \
    -DreportFile=./target/migration-report.txt
```

### CSV 형식 보고서

```bash
mvn ...:extract \
    -DsourceDir=./src \
    -DreportFile=./target/migration-report.csv \
    -DcsvFormat=true
```

**CSV 출력 예시:**
```csv
File,Class,Line,Name,Strategy
com/github/wisesky0/asis/Article.java,Article,17,custom-id,com.github.wisesky0.asis.CustomIdGenerator
com/github/wisesky0/asis/ArticleVersioned.java,ArticleVersioned,17,custom-id-versioned,com.github.wisesky0.asis.CustomIdGenerator
```

## 검증

### AS-IS 코드 (Hibernate 5.x, strategy 사용)
```bash
cd testcode/asis
mvn com.github.wisesky0:generic-generator-extractor-maven-plugin:1.0.0:extract \
    -DsourceDir=./src/main/java
```

**결과:** `2건 검출` ✓
- `Article.java` - custom-id
- `ArticleVersioned.java` - custom-id-versioned

### TO-BE 코드 (Hibernate 6.6+, type 사용)
```bash
cd testcode/tobe
mvn mvn com.github.wisesky0:generic-generator-extractor-maven-plugin:1.0.0:extract \
    -DsourceDir=./src/main/java
```

**결과:** `0건 검출` ✓
(TO-BE는 `type` 속성을 사용하므로 `strategy` 기반 검색에서 제외됨)

## 마이그레이션 가이드

플러그인이 검출한 각 `@GenericGenerator` 어노테이션에 대해:

**AS-IS (Hibernate 5.x):**
```java
@GeneratedValue(generator = "custom-id")
@GenericGenerator(
    name = "custom-id",
    strategy = "com.github.wisesky0.asis.CustomIdGenerator"
)
private Long id;
```

**TO-BE (Hibernate 6.6+):**
```java
@GeneratedValue(generator = "custom-id")
@GenericGenerator(
    name = "custom-id",
    type = com.github.wisesky0.asis.CustomIdGenerator.class  // class 타입으로 변경
)
private Long id;
```

### 변경 사항 요약
1. `strategy = "..."` → `type = ....class`
2. String 값에서 Class 참조로 변경
3. 완전 패키지 경로 사용 필수

## 기술 스펙

- **Java 호환성:** Java 8+
- **JavaParser 버전:** 3.25.10 (Java 11+ 컴파일러 비호환 버전 제외)
- **Maven 버전:** 3.6.3+
- **패키징:** Maven Plugin

## 제약사항

- 오직 **`strategy` 멤버를 가진 `@GenericGenerator`만 추출**
  - `type` 속성은 자동으로 제외 (이미 TO-BE 형식)
  - `strategy` 없는 어노테이션은 제외

- **String 기반 strategy 값만 검출**
  - 상수 변수 참조는 미지원

- **파싱 에러**
  - 문법 오류가 있는 Java 파일은 로그 경고만 표시
  - `failOnError=true` 설정 시 빌드 실패

## 폴더 구조

```
utils/generic-generator-extractor/
├── pom.xml
├── README.md (this file)
└── src/main/java/com/github/wisesky0/
    ├── ExtractGenericGeneratorMojo.java      # Maven Mojo 진입점
    ├── GenericGeneratorInfo.java             # 데이터 VO
    ├── GenericGeneratorParser.java           # JavaParser 기반 파싱
    └── ReportWriter.java                     # 콘솔/파일 출력
```

## FAQ

**Q: 다른 프로젝트에서도 이 플러그인을 사용할 수 있나요?**
A: 네. 로컬 Maven 저장소에 설치되면 어디서든 사용 가능합니다. `mvn com.github.wisesky0:...` 명령으로 실행하면 됩니다.

**Q: 대용량 소스코드(1000+ 파일)에서도 빠른가요?**
A: 네. 병렬 처리는 아니지만, 일반적으로 몇 초 내에 완료됩니다.

**Q: Strategy가 변수나 상수 참조면 어떻게 되나요?**
A: String 리터럴만 추출하므로 변수 참조는 감지되지 않습니다. 수동으로 검색이 필요합니다.

**Q: CSV 파일을 엑셀에서 열면 한글이 깨져요.**
A: 파일을 UTF-8로 저장하고 있습니다. 엑셀에서 [데이터 > 텍스트를 열기] 메뉴를 사용하여 UTF-8 인코딩을 지정하세요.
