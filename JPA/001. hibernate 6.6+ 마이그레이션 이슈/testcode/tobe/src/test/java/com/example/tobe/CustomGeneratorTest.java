package com.example.tobe;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TO-BE: Hibernate 6.6.38 Custom Generator 동작 테스트
 * Spring Boot 3.x / jakarta.persistence
 *
 * allowAssignedIdentifiers=true 적용 후:
 * - 직접 ID 할당 케이스도 INSERT=N, SELECT=0 으로 정상 동작해야 함
 */
@SpringBootTest
@Import(TestDataSourceConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CustomGeneratorTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ArticleVersionedRepository articleVersionedRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        SessionFactory sf = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        stats = sf.getStatistics();
        stats.clear();
        TestDataSourceConfig.BATCH_LISTENER.reset();
    }

    // =========================================================
    // tc1: @GeneratedValue (Custom Generator), @Version 없음
    // =========================================================

    @Test
    @DisplayName("tc1_1_직접ID_단건: 직접 ID 할당, 단건 save - INSERT=1, SELECT=0")
    @Transactional
    void tc1_1_직접ID_단건() {
        stats.clear();
        TestDataSourceConfig.BATCH_LISTENER.reset();

        Article article = new Article("MANUAL-001", "제목1", "내용1");
        articleRepository.save(article);
        entityManager.flush();

        // @Version 없는 엔티티 + 직접 ID 할당
        // → Spring Data JPA isNew()=false → merge() 경로
        // → JDBC SELECT 1건 발생 (PK 존재 여부 확인)
        // → row 없으면 INSERT
        System.out.printf("[tc1_1] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount()
        );
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("@Version 없음 + 직접ID: merge() 경로로 SELECT 1건 발생")
            .isEqualTo(1);
        assertThat(stats.getEntityInsertCount())
            .as("SELECT 후 INSERT 1건")
            .isEqualTo(1);
        System.out.println("[tc1_1] INSERT=" + stats.getEntityInsertCount()
                + ", SELECT(query)=" + stats.getQueryExecutionCount());
        // JDBC 레벨 SELECT 카운팅 (datasource-proxy)
        int actualSelectCount = TestDataSourceConfig.BATCH_LISTENER.getSelectCount();
        System.out.println("[tc1_1] JDBC SELECT count: " + actualSelectCount);
    }

    @Test
    @DisplayName("tc1_3_자동생성_단건: ID 미할당(자동생성), 단건 save - INSERT=1, SELECT=0")
    @Transactional
    void tc1_3_자동생성_단건() {
        stats.clear();
        TestDataSourceConfig.BATCH_LISTENER.reset();

        Article article = new Article("자동생성제목", "자동생성내용");
        articleRepository.save(article);
        entityManager.flush();

        System.out.printf("[tc1_3] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount()
        );
        assertThat(stats.getEntityInsertCount()).isEqualTo(1);
        assertThat(stats.getQueryExecutionCount()).isEqualTo(0);
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("@Version 없음 + 자동생성: isNew()=true → persist() → SELECT 없음")
            .isEqualTo(0);
        System.out.println("[tc1_3] INSERT=" + stats.getEntityInsertCount()
                + ", SELECT(query)=" + stats.getQueryExecutionCount());
        // JDBC 레벨 SELECT 카운팅 (datasource-proxy)
        int actualSelectCount = TestDataSourceConfig.BATCH_LISTENER.getSelectCount();
        System.out.println("[tc1_3] JDBC SELECT count: " + actualSelectCount);
    }

    @Test
    @DisplayName("tc1_5_직접ID_배치10건: 직접 ID 할당, 10건 saveAll - INSERT=10, SELECT=10 (merge() 경로)")
    @Transactional
    void tc1_5_직접ID_배치10건() {
        TestDataSourceConfig.BATCH_LISTENER.reset();
        stats.clear();

        List<Article> articles = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            articles.add(new Article(String.format("BATCH-%03d", i), "제목" + i, "내용" + i));
        }
        articleRepository.saveAll(articles);
        entityManager.flush();

        // Hibernate Statistics 검증 (기존)
        assertThat(stats.getEntityInsertCount()).isEqualTo(10);
        // @Version 없음 + 직접ID 10건: 각 save마다 isNew()=false → merge() → SELECT 10건 발생
        // allowAssignedIdentifiers=true 는 Spring Data JPA isNew() 판단에 영향 없음 (Hibernate 내부 동작과 무관)
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("@Version 없음 + 직접ID 배치10건: merge() 경로로 SELECT 10건 발생 (tobe도 asis와 동일)")
            .isEqualTo(10);

        // JDBC Batch 검증 (신규)
        int batchExecCount = TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount();
        int batchedStmtCount = TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount();
        System.out.printf("[tc1_5] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            batchExecCount,
            batchedStmtCount
        );
        System.out.println("[tc1_5] INSERT=" + stats.getEntityInsertCount()
                + ", SELECT(query)=" + stats.getQueryExecutionCount());
        System.out.println("[tc1_5] executeBatch()횟수=" + batchExecCount
                + ", batch묶음수=" + batchedStmtCount);
        assertThat(batchExecCount)
                .as("executeBatch() 호출 횟수 - 실제 JDBC batch 전송 여부")
                .isGreaterThanOrEqualTo(1);
        assertThat(batchedStmtCount)
                .as("batch에 묶인 statement 수")
                .isEqualTo(10);
    }

    @Test
    @DisplayName("tc1_6_자동생성_배치10건: ID 미할당(자동생성), 10건 saveAll - INSERT=10, SELECT=0")
    @Transactional
    void tc1_6_자동생성_배치10건() {
        TestDataSourceConfig.BATCH_LISTENER.reset();
        stats.clear();

        List<Article> articles = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            articles.add(new Article("자동제목" + i, "자동내용" + i));
        }
        articleRepository.saveAll(articles);
        entityManager.flush();

        // Hibernate Statistics 검증 (기존)
        assertThat(stats.getEntityInsertCount()).isEqualTo(10);
        assertThat(stats.getQueryExecutionCount()).isEqualTo(0);
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("@Version 없음 + 자동생성 배치10건: isNew()=true → persist() → SELECT 없음")
            .isEqualTo(0);

        // JDBC Batch 검증 (신규)
        int batchExecCount = TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount();
        int batchedStmtCount = TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount();
        System.out.printf("[tc1_6] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            batchExecCount,
            batchedStmtCount
        );
        System.out.println("[tc1_6] INSERT=" + stats.getEntityInsertCount()
                + ", SELECT(query)=" + stats.getQueryExecutionCount());
        System.out.println("[tc1_6] executeBatch()횟수=" + batchExecCount
                + ", batch묶음수=" + batchedStmtCount);
        assertThat(batchExecCount)
                .as("executeBatch() 호출 횟수 - 실제 JDBC batch 전송 여부")
                .isGreaterThanOrEqualTo(1);
        assertThat(batchedStmtCount)
                .as("batch에 묶인 statement 수")
                .isEqualTo(10);
    }

    // =========================================================
    // tc2: @GeneratedValue (Custom Generator) + @Version
    // =========================================================

    @Test
    @DisplayName("tc2_1_버전_직접ID_단건: @Version + 직접 ID 할당, 단건 save - INSERT=1, SELECT=0")
    @Transactional
    void tc2_1_버전_직접ID_단건() {
        stats.clear();
        TestDataSourceConfig.BATCH_LISTENER.reset();

        ArticleVersioned article = new ArticleVersioned("VERSIONED-001", "버전제목1", "버전내용1");
        articleVersionedRepository.save(article);
        entityManager.flush();

        System.out.printf("[tc2_1] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount()
        );
        assertThat(stats.getEntityInsertCount()).isEqualTo(1);
        assertThat(stats.getQueryExecutionCount()).isEqualTo(0);
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("@Version 있음 + 직접ID: version=null → isNew()=true → persist() → SELECT 없음")
            .isEqualTo(0);
        System.out.println("[tc2_1] INSERT=" + stats.getEntityInsertCount()
                + ", SELECT(query)=" + stats.getQueryExecutionCount());
        // JDBC 레벨 SELECT 카운팅 (datasource-proxy)
        int actualSelectCount = TestDataSourceConfig.BATCH_LISTENER.getSelectCount();
        System.out.println("[tc2_1] JDBC SELECT count: " + actualSelectCount);
    }

    @Test
    @DisplayName("tc2_3_버전_자동생성_단건: @Version + ID 미할당, 단건 save - INSERT=1, SELECT=0")
    @Transactional
    void tc2_3_버전_자동생성_단건() {
        stats.clear();
        TestDataSourceConfig.BATCH_LISTENER.reset();

        ArticleVersioned article = new ArticleVersioned("버전자동제목", "버전자동내용");
        articleVersionedRepository.save(article);
        entityManager.flush();

        System.out.printf("[tc2_3] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount()
        );
        assertThat(stats.getEntityInsertCount()).isEqualTo(1);
        assertThat(stats.getQueryExecutionCount()).isEqualTo(0);
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("@Version 있음 + 자동생성: version=null → isNew()=true → persist() → SELECT 없음")
            .isEqualTo(0);
        System.out.println("[tc2_3] INSERT=" + stats.getEntityInsertCount()
                + ", SELECT(query)=" + stats.getQueryExecutionCount());
        // JDBC 레벨 SELECT 카운팅 (datasource-proxy)
        int actualSelectCount = TestDataSourceConfig.BATCH_LISTENER.getSelectCount();
        System.out.println("[tc2_3] JDBC SELECT count: " + actualSelectCount);
    }

    @Test
    @DisplayName("tc2_5_버전_직접ID_배치10건: @Version + 직접 ID 할당, 10건 saveAll - INSERT=10, SELECT=0")
    @Transactional
    void tc2_5_버전_직접ID_배치10건() {
        TestDataSourceConfig.BATCH_LISTENER.reset();
        stats.clear();

        List<ArticleVersioned> articles = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            articles.add(new ArticleVersioned(String.format("VBATCH-%03d", i), "버전제목" + i, "버전내용" + i));
        }
        articleVersionedRepository.saveAll(articles);
        entityManager.flush();

        // Hibernate Statistics 검증 (기존)
        assertThat(stats.getEntityInsertCount()).isEqualTo(10);
        assertThat(stats.getQueryExecutionCount()).isEqualTo(0);
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("@Version 있음 + 직접ID 배치10건: version=null → isNew()=true → persist() → SELECT 없음")
            .isEqualTo(0);

        // JDBC Batch 검증 (신규)
        int batchExecCount = TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount();
        int batchedStmtCount = TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount();
        System.out.printf("[tc2_5] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            batchExecCount,
            batchedStmtCount
        );
        System.out.println("[tc2_5] INSERT=" + stats.getEntityInsertCount()
                + ", SELECT(query)=" + stats.getQueryExecutionCount());
        System.out.println("[tc2_5] executeBatch()횟수=" + batchExecCount
                + ", batch묶음수=" + batchedStmtCount);
        assertThat(batchExecCount)
                .as("executeBatch() 호출 횟수 - 실제 JDBC batch 전송 여부")
                .isGreaterThanOrEqualTo(1);
        assertThat(batchedStmtCount)
                .as("batch에 묶인 statement 수")
                .isEqualTo(10);
    }

    // =========================================================
    // tc_dup: 이미 존재하는 ID로 중복 저장 시도
    // =========================================================

    @Test
    @DisplayName("tc_dup1_기존ID_중복저장_단건: 이미 존재하는 ID로 단건 save → @Version 없으면 SELECT→UPDATE(예외 없음, AS-IS와 동일)")
    void tc_dup1_기존ID_중복저장_단건() {
        // 1. 첫 번째 저장 (정상 INSERT) - 별도 트랜잭션으로 커밋
        transactionTemplate.execute(status -> {
            Article first = new Article("MANUAL-001", "First", "내용1");
            articleRepository.save(first);
            return null;
        });

        stats.clear();
        TestDataSourceConfig.BATCH_LISTENER.reset();

        // 2. 동일 ID로 두 번째 저장 시도
        // TO-BE (@Version 없음): Spring Data JPA isNew() → PK SELECT → 존재 확인 → merge → UPDATE
        // allowAssignedIdentifiers=true 는 Spring Data JPA isNew() 판단에 영향 없음
        // AS-IS와 동일하게 예외 없이 UPDATE 실행됨
        transactionTemplate.execute(status -> {
            Article duplicate = new Article("MANUAL-001", "Duplicate", "내용2");
            articleRepository.save(duplicate);
            return null;
        });

        // PK SELECT 1회 후 UPDATE 수행
        System.out.printf("[tc_dup1] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount()
        );
        System.out.println("[tc_dup1] SELECT(query)=" + stats.getQueryExecutionCount()
            + ", entityLoad=" + stats.getEntityLoadCount()
            + ", INSERT=" + stats.getEntityInsertCount()
            + ", UPDATE=" + stats.getEntityUpdateCount());
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("TO-BE @Version 없음: PK SELECT 1건 발생 (JDBC 레벨)")
            .isEqualTo(1);
        assertThat(stats.getEntityLoadCount())
            .as("TO-BE @Version 없음: PK SELECT로 isNew() 판단 → entityLoad=1 (AS-IS와 동일)")
            .isEqualTo(1);
        assertThat(stats.getEntityUpdateCount())
            .as("TO-BE @Version 없음: 기존 엔티티 발견 → merge → UPDATE=1")
            .isEqualTo(1);
        assertThat(stats.getEntityInsertCount())
            .as("TO-BE @Version 없음: INSERT 시도 없음")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("tc_dup2_버전_기존ID_중복저장_단건: @Version 엔티티에서 이미 존재하는 ID로 단건 save → INSERT 시도 → DataIntegrityViolationException")
    void tc_dup2_버전_기존ID_중복저장_단건() {
        // 1. 첫 번째 저장 (정상 INSERT) - 별도 트랜잭션으로 커밋
        transactionTemplate.execute(status -> {
            ArticleVersioned first = new ArticleVersioned("VERSIONED-001", "First", "버전내용1");
            articleVersionedRepository.save(first);
            return null;
        });

        stats.clear();
        TestDataSourceConfig.BATCH_LISTENER.reset();

        // 2. 동일 ID로 두 번째 저장 시도
        // TO-BE (allowAssignedIdentifiers=true): isNew()=true → persist() → SELECT 없이 INSERT 시도 → PK 중복 오류
        assertThrows(DataIntegrityViolationException.class, () ->
            transactionTemplate.execute(status -> {
                ArticleVersioned duplicate = new ArticleVersioned("VERSIONED-001", "Duplicate", "버전내용2");
                articleVersionedRepository.save(duplicate);
                return null;
            })
        );

        // SELECT 없이 INSERT 시도만 해야 함
        System.out.printf("[tc_dup2] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount(),
            TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount()
        );
        System.out.println("[tc_dup2] SELECT(query)=" + stats.getQueryExecutionCount());
        assertThat(stats.getQueryExecutionCount())
            .as("TO-BE allowAssignedIdentifiers=true: SELECT 없이 INSERT 시도 → DB PK 중복 오류 (SELECT=0)")
            .isEqualTo(0);
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("TO-BE @Version 있음: JDBC 레벨 SELECT 없음 (SELECT=0)")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("tc2_6_버전_자동생성_배치10건: @Version + ID 미할당, 10건 saveAll - INSERT=10, SELECT=0")
    @Transactional
    void tc2_6_버전_자동생성_배치10건() {
        TestDataSourceConfig.BATCH_LISTENER.reset();
        stats.clear();

        List<ArticleVersioned> articles = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            articles.add(new ArticleVersioned("버전자동제목" + i, "버전자동내용" + i));
        }
        articleVersionedRepository.saveAll(articles);
        entityManager.flush();

        // Hibernate Statistics 검증 (기존)
        assertThat(stats.getEntityInsertCount()).isEqualTo(10);
        assertThat(stats.getQueryExecutionCount()).isEqualTo(0);
        assertThat(TestDataSourceConfig.BATCH_LISTENER.getSelectCount())
            .as("@Version 있음 + 자동생성 배치10건: version=null → isNew()=true → persist() → SELECT 없음")
            .isEqualTo(0);

        // JDBC Batch 검증 (신규)
        int batchExecCount = TestDataSourceConfig.BATCH_LISTENER.getBatchExecutionCount();
        int batchedStmtCount = TestDataSourceConfig.BATCH_LISTENER.getBatchedStatementCount();
        System.out.printf("[tc2_6] INSERT=%d, SELECT=%d, executeBatch=%d, batchSize=%d%n",
            stats.getEntityInsertCount(),
            TestDataSourceConfig.BATCH_LISTENER.getSelectCount(),
            batchExecCount,
            batchedStmtCount
        );
        System.out.println("[tc2_6] INSERT=" + stats.getEntityInsertCount()
                + ", SELECT(query)=" + stats.getQueryExecutionCount());
        System.out.println("[tc2_6] executeBatch()횟수=" + batchExecCount
                + ", batch묶음수=" + batchedStmtCount);
        assertThat(batchExecCount)
                .as("executeBatch() 호출 횟수 - 실제 JDBC batch 전송 여부")
                .isGreaterThanOrEqualTo(1);
        assertThat(batchedStmtCount)
                .as("batch에 묶인 statement 수")
                .isEqualTo(10);
    }
}
