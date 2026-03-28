package com.example.asis;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom ID Generator: timestamp + counter 방식
 * Hibernate 5.x 방식 - IdentifierGenerator 구현
 */
public class CustomIdGenerator implements IdentifierGenerator {

    private static final AtomicLong counter = new AtomicLong(0);

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        // 이미 ID가 할당된 경우 그대로 반환 (직접 할당 지원)
        if (object instanceof Article) {
            Article article = (Article) object;
            if (article.getId() != null) {
                return article.getId();
            }
        }
        if (object instanceof ArticleVersioned) {
            ArticleVersioned article = (ArticleVersioned) object;
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
