package net.luversof.blog;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.blog.domain.mysql.Blog;

public class SimpleTest {

	@Test
	public void test() {
		
		Blog blog1 = new Blog();
		blog1.setBlogId(UUID.randomUUID().toString());
		blog1.setUserId(UUID.randomUUID().toString());
		
		Blog blog2 = new Blog();
		blog2.setBlogId(UUID.randomUUID().toString());
		blog2.setUserId(UUID.randomUUID().toString());
		
		System.out.println(blog1.equals(blog2));
	}
}
