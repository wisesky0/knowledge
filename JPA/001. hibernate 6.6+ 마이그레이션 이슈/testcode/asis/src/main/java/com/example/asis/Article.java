package com.example.asis;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * Article 엔티티 - @GeneratedValue + Custom Generator (버전 없음)
 * javax.persistence 사용 (Hibernate 5.x / Spring Boot 2.x)
 */
@Entity
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", strategy = "com.example.asis.CustomIdGenerator")
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content")
    private String content;

    public Article() {}

    public Article(String id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public Article(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public String toString() {
        return "Article{id='" + id + "', title='" + title + "'}";
    }
}
