package net.luversof.bbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bbs.domain.Article;

@Transactional(readOnly = true)
public interface ArticleRepository extends JpaRepository<Article, Long> {

}
