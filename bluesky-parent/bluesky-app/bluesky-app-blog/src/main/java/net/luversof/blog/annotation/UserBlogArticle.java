package net.luversof.blog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그인한 유저의 BlogArticle 정보를 확인하기 위해 사용하는 aop
 * @author bluesky
 *
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface UserBlogArticle {

	public boolean checkBlogParameter() default false;	// 외부 접근 파라메터에 대해 유저의 blog 인지 확인 여부
	
	public boolean addBlog() default false;	// 유저의 블로그 정보를 추가
	
	public boolean checkAndAddBlog() default false;	// 외부 접근에 대해 유저의 블로그인지 여부 체크 및 BlogArticle에 blog 정보 추가
}
