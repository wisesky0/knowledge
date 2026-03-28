# Claude Code 협업 가이드

이 문서는 Claude Code와의 협업 방식, 코딩 규칙, 패키지 정책 등을 정의합니다.

## 패키지 그룹ID 정책

- **새롭게 작성되는 모든 기능/플러그인:** `com.github.wisesky0` 패키지 그룹ID 사용
- **예시:**
  - `com.github.wisesky0:generic-generator-extractor-maven-plugin`
  - `com.github.wisesky0:hibernate-migration-toolkit`

### Maven 좌표 형식

```xml
<groupId>com.github.wisesky0</groupId>
<artifactId>{feature-name}-maven-plugin</artifactId>
<version>1.0.0</version>
```

### 사용 명령어

```bash
mvn com.github.wisesky0:{artifact-id}:{version}:{goal} [options]
```
