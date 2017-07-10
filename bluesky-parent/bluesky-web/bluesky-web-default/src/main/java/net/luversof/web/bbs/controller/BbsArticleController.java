package net.luversof.web.bbs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bbs.annotation.PreBbsAuthorize;
import net.luversof.bbs.domain.BbsArticle;
import net.luversof.bbs.service.BbsArticleService;

@Slf4j
@RestController
@RequestMapping(value = "/board/{boardAlias}", produces = MediaType.APPLICATION_JSON_VALUE)
public class BbsArticleController {
	
	@Autowired
	private BbsArticleService bbsArticleService;
	
	@PreBbsAuthorize(checkBbsActivated = true)
	@GetMapping("/article")
	public Page<BbsArticle> list(@PathVariable String boardAlias, @RequestParam(defaultValue = "1") int page) {
		Page<BbsArticle> bbsArticleList = bbsArticleService.selectBbsArticleList(boardAlias, PageRequest.of(page - 1, 20, new Sort(Direction.DESC, "id")));
		log.debug("bbsArticleList : {}", bbsArticleList);
		return bbsArticleList;
	}
	
	@GetMapping(value = "/article/{id}")
	public BbsArticle view(@PathVariable String boardAlias, @PathVariable  long id) {
		BbsArticle bbsArticle = bbsArticleService.selectBbsArticle(id);
		log.debug("bbsArticleList : {}", bbsArticle);
		return bbsArticle;
	}

}
