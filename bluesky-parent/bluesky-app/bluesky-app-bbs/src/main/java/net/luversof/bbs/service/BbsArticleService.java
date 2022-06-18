package net.luversof.bbs.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.bbs.domain.BbsArticle;
import net.luversof.bbs.repository.BbsArticleRepository;

@Service
public class BbsArticleService {

	@Autowired
	private BbsArticleRepository bbsArticleRepository;
	
	public Page<BbsArticle> findByBbsId(String bbsAlias, Pageable pageable) {
		return bbsArticleRepository.findByBbsId(bbsAlias, pageable);
	}
	
	public Optional<BbsArticle> findByBbsArticleId(String bbsArticleId) {
		return bbsArticleRepository.findByBbsArticleId(bbsArticleId);
	}
	
}
