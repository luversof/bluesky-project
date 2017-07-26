package net.luversof.bbs;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bbs.domain.Article;
import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.repository.ArticleRepository;
import net.luversof.bbs.repository.BbsRepository;

@Slf4j
public class ArticleTest extends GeneralTest {

	@Autowired
	private ArticleRepository bbsArticleRepository;
	
	@Autowired
	private BbsRepository bbsRepository;
	
	@Test
	public void 단건입력() {
		Bbs bbs = bbsRepository.getOne((long) 1);
		Article bbsArticle = new Article();
		bbsArticle.setBbs(bbs);
		bbsArticle.setUserId(1);
		bbsArticle.setTitle("테스트");
		bbsArticle.setContent("내용");
		bbsArticleRepository.save(bbsArticle);
	}
	
	@Test
	public void 다량입력() {
		Bbs bbs = bbsRepository.getOne((long) 1);

		List<Article> bbsArticleList = new ArrayList<>();
		for (int i = 0 ; i < 100000 ; i ++) {
			Article bbsArticle = new Article();
			bbsArticle.setBbs(bbs);
			bbsArticle.setUserId(1);
			bbsArticle.setTitle("테스트" + i);
			bbsArticle.setContent("내용" + i);
			bbsArticleList.add(bbsArticle);
		}
		bbsArticleRepository.saveAll(bbsArticleList);
	}
	
	@Test
	public void 페이징테스트() {
		Page<Article> bbsArticleList = bbsArticleRepository.findByBbsAlias("free", PageRequest.of(0, 20, new Sort(Direction.DESC, "id")));
		log.debug("result : {}", bbsArticleList.getContent());
		
	}
}
