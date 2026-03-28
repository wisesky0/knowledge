package com.example.tobe;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom ID Generator: timestamp + counter 방식
 * Hibernate 6.x 방식 - BeforeExecutionGenerator 구현
 *
 * 주의: allowAssignedIdentifiers 설정을 적용하지 않음
 * -> 직접 ID 할당 시 오류가 재현되어야 함
 */
public class CustomIdGenerator implements BeforeExecutionGenerator {

    private static final AtomicLong counter = new AtomicLong(0);

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        // Hibernate 6.x에서는 이미 할당된 ID를 여기서 확인하더라도
        // SimpleEntityPersister가 isNew() 판단 시 ID != null 이면 merge()로 처리하여 문제 발생
        if (owner instanceof Article) {
            Article article = (Article) owner;
            if (article.getId() != null) {
                return article.getId();
            }
        }
        if (owner instanceof ArticleVersioned) {
            ArticleVersioned article = (ArticleVersioned) owner;
            if (article.getId() != null) {
                return article.getId();
            }
        }
        // 자동 생성
        long ts = System.currentTimeMillis();
        long cnt = counter.incrementAndGet();
        return "GEN-" + ts + "-" + cnt;
    }
}
