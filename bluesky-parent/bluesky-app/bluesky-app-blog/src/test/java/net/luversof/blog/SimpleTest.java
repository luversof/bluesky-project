package net.luversof.blog;

import net.luversof.blog.domain.Blog;

import org.junit.Test;

public class SimpleTest {

	@Test
	public void test() {
		
		Blog blog1 = new Blog();
		blog1.setId(1);
		blog1.setUserId(123);
		blog1.setUserType(new String("test"));
		
		Blog blog2 = new Blog();
		blog2.setId(1);
		blog2.setUserId(123);
		blog2.setUserType(new String("test"));
		
		System.out.println(blog1.equals(blog2));
	}
}
