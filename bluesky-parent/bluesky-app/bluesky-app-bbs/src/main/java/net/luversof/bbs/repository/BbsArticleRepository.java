package net.luversof.bbs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bbs.domain.BbsArticle;

@Transactional(readOnly = true)
public interface BbsArticleRepository extends JpaRepository<BbsArticle, Long> {
	Page<BbsArticle> findByBbsAlias(String bbsAlias, Pageable pageable);
}
