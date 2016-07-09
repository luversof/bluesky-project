package net.luversof.web.bbs.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bbs.domain.BbsArticle;
import net.luversof.bbs.service.BbsArticleService;
import net.luversof.bbs.service.BbsService;

@Slf4j
@Controller
@RequestMapping("/board/{aliasName}")
public class BbsArticleController {
	
	@Autowired
	private BbsService bbsService;
	
	@Autowired
	private BbsArticleService bbsArticleService;
	
	@RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Page<BbsArticle> list(@PathVariable String aliasName, @RequestParam(defaultValue = "1") int page) {
		Page<BbsArticle> bbsArticleList = bbsArticleService.selectBbsArticleList(aliasName, new PageRequest(page - 1, 20, new Sort(Direction.DESC, "id")));
		log.debug("bbsArticleList : {}", bbsArticleList);
		return bbsArticleList;
	}
	
	@RequestMapping(value = "/view/{id}", method = RequestMethod.GET)
	@ResponseBody
	public BbsArticle view(@PathVariable String aliasName, @PathVariable  long id) {
		BbsArticle bbsArticle = bbsArticleService.selectBbsArticle(id);
		log.debug("bbsArticleList : {}", bbsArticle);
		return bbsArticle;
	}
	
	@RequestMapping(value = "/view2/{id}", method = RequestMethod.GET)
	public void view2(@PathVariable String aliasName, @PathVariable  long id, ModelMap modelMap) {
		BbsArticle bbsArticle = bbsArticleService.selectBbsArticle(id);
		log.debug("bbsArticleList : {}", bbsArticle);
		modelMap.addAttribute(bbsArticle);
	}
}
