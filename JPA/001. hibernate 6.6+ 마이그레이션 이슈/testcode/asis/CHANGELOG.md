# Changelog

## [1.0.1] - 2026-03-29

### test
- 낙관적 잠금(Optimistic Locking) 동작 검증 테스트 케이스 추가 (`tc_opt1_낙관적잠금_버전충돌`)
  - 두 트랜잭션이 동일 엔티티를 동시에 수정할 때 나중 커밋이 `ObjectOptimisticLockingFailureException`을 발생시키는 시나리오 검증
  - 실제 예외 클래스 출력 추가: `org.springframework.orm.ObjectOptimisticLockingFailureException` (원인: `org.hibernate.StaleObjectStateException`)
  - 관련 파일: `src/test/java/com/example/asis/CustomGeneratorTest.java`
- `OptimisticLockingFailureException` import 추가
