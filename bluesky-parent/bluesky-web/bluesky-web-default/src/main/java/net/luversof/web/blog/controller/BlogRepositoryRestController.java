package net.luversof.web.blog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import net.luversof.blog.domain.Article;
import net.luversof.blog.repository.ArticleRepository;

/**
 * custom RepositoryRestController가 hal browser에 노출되지 않는 문제가 있음.
 * 관련한 수동 설정 방법이 있지만 차라리 패치 버전 기다리는게 더 나을 것 같음.
 * https://tutel.me/c/programming/questions/34285829/spring+data+rest++how+to+expose+custom+rest+controller+method+in+the+hal+browser
 * @author bluesky
 *
 */
@RepositoryRestController
@RequestMapping("/blogArticles")
public class BlogRepositoryRestController {
	
	@Autowired
	private ArticleRepository articleRepository;

	@GetMapping("/search/test")
	public @ResponseBody ResponseEntity<?> findAllTest(String testParam) {
		List<Article> articleList = articleRepository.findAll();
		Resources<Article> resources = new Resources<Article>(articleList); 
        resources.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(BlogRepositoryRestController.class).findAllTest(testParam)).withSelfRel()); 
        return ResponseEntity.ok(resources); 
	}
	
	@GetMapping("/search/test2")
	public @ResponseBody ResponseEntity<?> findAllTest2() {
		List<Article> articleList = articleRepository.findAll();
		Resources<Article> resources = new Resources<Article>(articleList); 
        resources.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(BlogRepositoryRestController.class).findAllTest2()).withSelfRel()); 
        return ResponseEntity.ok(resources); 
	}
}
