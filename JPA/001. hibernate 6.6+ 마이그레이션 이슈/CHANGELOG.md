# Changelog

## [1.0.1] - 2026-03-29

### 변경 요약
- `hibernate-migration-asis`: 낙관적 잠금 예외 검증 테스트 추가 → [자세히 보기](./testcode/asis/CHANGELOG.md)
- `hibernate-migration-tobe`: 낙관적 잠금 예외 검증 테스트 추가, AS-IS/TO-BE 동일 동작 확인 → [자세히 보기](./testcode/tobe/CHANGELOG.md)

### docs
- `README.md`: 낙관적 잠금(tc_opt1) 테스트 결과 섹션 추가
  - AS-IS/TO-BE 모두 `StaleObjectStateException` → `ObjectOptimisticLockingFailureException` 변환 경로가 동일함을 문서화
