package net.luversof.blog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그인한 유저의 Blog 정보를 주입하기 위해 사용하는 aop
 * @author bluesky
 *
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface UserBlog {

	public boolean checkBlog() default false;	// 외부 접근 파라메터에 대해 유저의 blog 인지 확인 여부
}
