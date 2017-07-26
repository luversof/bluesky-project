package net.luversof.bbs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bbs.domain.Article;

@Repository("bbsArticleRepository")
@Transactional(readOnly = true)
public interface ArticleRepository extends JpaRepository<Article, Long> {
	Page<Article> findByBbsAlias(String bbsAlias, Pageable pageable);
}
