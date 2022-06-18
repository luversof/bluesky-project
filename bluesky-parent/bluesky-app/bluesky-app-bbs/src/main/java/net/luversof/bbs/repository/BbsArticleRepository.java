package net.luversof.bbs.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.bbs.domain.BbsArticle;

@Repository("bbsArticleRepository")
@Transactional(readOnly = true)
public interface BbsArticleRepository extends JpaRepository<BbsArticle, Long> {
	
	Optional<BbsArticle> findByBbsArticleId(String bbsArticleId);
	
	Page<BbsArticle> findByBbsId(String bbsId, Pageable pageable);
}
