package net.luversof.bbs;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bbs.domain.BbsArticle;
import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.repository.BbsArticleRepository;
import net.luversof.bbs.repository.BbsRepository;

@Slf4j
class BbsArticleTest implements GeneralTest {

	@Autowired
	private BbsArticleRepository bbsArticleRepository;
	
	@Autowired
	private BbsRepository bbsRepository;
	
	@Test
	void 단건입력() {
		Bbs bbs = bbsRepository.getReferenceById((long) 1);
		BbsArticle bbsArticle = new BbsArticle();
		bbsArticle.setBbsId(bbs.getBbsId());
		bbsArticle.setUserId("1");
		bbsArticle.setTitle("테스트");
		bbsArticle.setContent("내용");
		bbsArticleRepository.save(bbsArticle);
	}
	
	@Test
	void 다량입력() {
		Bbs bbs = bbsRepository.getReferenceById((long) 1);

		List<BbsArticle> bbsArticleList = new ArrayList<>();
		for (int i = 0 ; i < 100000 ; i ++) {
			BbsArticle bbsArticle = new BbsArticle();
			bbsArticle.setBbsId(bbs.getBbsId());
			bbsArticle.setUserId("1");
			bbsArticle.setTitle("테스트" + i);
			bbsArticle.setContent("내용" + i);
			bbsArticleList.add(bbsArticle);
		}
		bbsArticleRepository.saveAll(bbsArticleList);
	}
	
	@Test
	void 페이징테스트() {
		Page<BbsArticle> bbsArticleList = bbsArticleRepository.findByBbsId("free", PageRequest.of(0, 20, Sort.by("id").descending()));
		log.debug("result : {}", bbsArticleList.getContent());
		
	}
}
