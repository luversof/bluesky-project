package net.luversof.bbs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.bbs.domain.Article;
import net.luversof.bbs.repository.ArticleRepository;

@Service("bbsArticleService")
public class ArticleService {

	@Autowired
	private ArticleRepository bbsArticleRepository;
	
	public Page<Article> selectBbsArticleList(String bbsAlias, Pageable pageable) {
		return bbsArticleRepository.findByBbsAlias(bbsAlias, pageable);
	}
	
	public Article selectBbsArticle(long id) {
		return bbsArticleRepository.getOne(id);
	}
	
}
