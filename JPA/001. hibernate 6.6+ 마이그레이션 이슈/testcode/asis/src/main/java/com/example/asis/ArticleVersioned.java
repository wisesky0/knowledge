package com.example.asis;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * ArticleVersioned 엔티티 - @GeneratedValue + Custom Generator + @Version
 * javax.persistence 사용 (Hibernate 5.x / Spring Boot 2.x)
 */
@Entity
@Table(name = "article_versioned")
public class ArticleVersioned {

    @Id
    @GeneratedValue(generator = "custom-id-versioned")
    @GenericGenerator(name = "custom-id-versioned", strategy = "com.example.asis.CustomIdGenerator")
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content")
    private String content;

    @Version
    @Column(name = "version")
    private Long version;

    public ArticleVersioned() {}

    public ArticleVersioned(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public ArticleVersioned(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getVersion() { return version; }

    @Override
    public String toString() {
        return "ArticleVersioned{id='" + id + "', title='" + title + "', version=" + version + "}";
    }
}
