package net.luversof.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.blog.domain.mongo.Blog;
import net.luversof.blog.repository.mongo.BlogRepository;

@Slf4j
public class MongoBlogTest extends GeneralTest {
	
	@Autowired
	private BlogRepository blogRepository;
	
	@Autowired
	@Qualifier("blogMongoTemplate")
	private MongoTemplate blogMongoTemplate;
	
//	@Autowired
//	private Map<String, MongoTemplate> mongoTemplateMap;

	@Test
	public void test() {
		var findAll = blogRepository.findAll();
		log.debug("test : {}", findAll);
	}
	
	
	@Test
	public void save() {
		Blog blog = new Blog() ;
		blog.setTestText("TEST2");
		var result = blogRepository.save(blog);
		log.debug("result : {}", result);
	}
}
