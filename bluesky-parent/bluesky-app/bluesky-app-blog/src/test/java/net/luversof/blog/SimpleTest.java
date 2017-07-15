package net.luversof.blog;

import net.luversof.blog.domain.Blog;

import java.util.UUID;

import org.junit.Test;

public class SimpleTest {

	@Test
	public void test() {
		
		Blog blog1 = new Blog();
		blog1.setId(UUID.randomUUID());
		blog1.setUserId("123");
		
		Blog blog2 = new Blog();
		blog2.setId(UUID.randomUUID());
		blog2.setUserId("123");
		
		System.out.println(blog1.equals(blog2));
	}
}
