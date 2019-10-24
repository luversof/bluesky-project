package net.luversof.web.bbs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bbs.annotation.PreBbsAuthorize;
import net.luversof.bbs.domain.Article;
import net.luversof.bbs.service.ArticleService;

@Slf4j
@RestController
@RequestMapping(value = "/bbs/{boardAlias}/article", produces = MediaType.APPLICATION_JSON_VALUE)
public class BbsArticleController {
	
	@Autowired
	private ArticleService bbsArticleService;
	
	@PreBbsAuthorize(checkBbsActivated = true)
	@GetMapping
	public Page<Article> list(@PathVariable String boardAlias, @RequestParam(defaultValue = "1") int page) {
		Page<Article> bbsArticleList = bbsArticleService.selectBbsArticleList(boardAlias, PageRequest.of(page - 1, 20, Sort.by("id").descending()));
		log.debug("bbsArticleList : {}", bbsArticleList);
		return bbsArticleList;
	}
	
	@GetMapping(value = "/{id}")
	public Article view(@PathVariable String boardAlias, @PathVariable  long id) {
		Article bbsArticle = bbsArticleService.selectBbsArticle(id);
		log.debug("bbsArticleList : {}", bbsArticle);
		return bbsArticle;
	}

}
