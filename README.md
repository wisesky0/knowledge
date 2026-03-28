# Knowledge Base

기술 문제 해결 및 마이그레이션 관련 노하우 정리

## 문서 인덱스

### JPA

- **[001. Hibernate 6.6+ 마이그레이션 이슈](./JPA/001.%20hibernate%206.6%2B%20%EB%A7%88%EC%9D%B4%EA%B7%B8%EB%A0%88%EC%9D%B4%EC%85%98%20%EC%9D%B4%EC%8A%88/README.md)**
  - Entity ID에 `@GeneratedValue`와 Custom Generator를 함께 사용하면서 ID를 수동으로 설정할 경우 발생하는 이슈
  - Hibernate 6.6+ 버전에서의 Optimistic Locking 규칙 강화로 인한 문제와 해결 방법
  - 테스트 결과 및 `allowAssignedIdentifiers()` 적용 가이드
