package com.example.asis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleVersionedRepository extends JpaRepository<ArticleVersioned, String> {
}
