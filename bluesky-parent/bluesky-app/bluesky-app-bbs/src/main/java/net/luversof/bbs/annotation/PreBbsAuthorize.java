package net.luversof.bbs.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreBbsAuthorize {
	String boardAlias() default "boardAlias";
	boolean checkBbsActivated() default false;
	boolean checkArticleActivated() default false;
	boolean checkReplyActivated() default false;
	boolean checkCommentActivated() default false;
}
