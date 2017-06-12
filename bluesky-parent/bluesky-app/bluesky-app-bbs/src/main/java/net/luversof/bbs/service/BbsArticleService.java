package net.luversof.bbs.service;

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
	
	public Page<BbsArticle> selectBbsArticleList(String bbsAlias, Pageable pageable) {
		return bbsArticleRepository.findByBbsAlias(bbsAlias, pageable);
	}
	
	public BbsArticle selectBbsArticle(long id) {
		return bbsArticleRepository.getOne(id);
	}
	
}
