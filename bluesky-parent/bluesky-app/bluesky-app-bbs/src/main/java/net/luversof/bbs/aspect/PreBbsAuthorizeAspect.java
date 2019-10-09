package net.luversof.bbs.aspect;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.luversof.bbs.annotation.PreBbsAuthorize;
import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.service.BbsService;
import net.luversof.boot.exception.BlueskyException;

@Aspect
@Component
public class PreBbsAuthorizeAspect {

	@Autowired
	private BbsService bbsService;
	
	@Before("@annotation(preBbsAuthorize)")
	public void before(JoinPoint joinPoint, PreBbsAuthorize preBbsAuthorize) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		if (!Arrays.stream(signature.getParameterNames()).anyMatch(parameterName -> preBbsAuthorize.boardAlias().equals(parameterName))) {
			throw new BlueskyException("bbs.NOT_EXIST_PARAMETER_BOARD_ALIAS");
		}
		
		String boardAlias = null;
		for (int i = 0 ; i < signature.getParameterNames().length ; i++) {
			Arrays.stream(signature.getParameterNames()).anyMatch(parameterName -> preBbsAuthorize.boardAlias().equals(parameterName));
			if (preBbsAuthorize.boardAlias().equals(signature.getParameterNames()[i])) {
				boardAlias = (String) joinPoint.getArgs()[i];
				break;
			}
		}
		
		Bbs bbs = bbsService.findByAlias(boardAlias);
		checkBbsActivated(preBbsAuthorize, bbs);
		checkArticleActivated(preBbsAuthorize, bbs);
		checkReplyActivated(preBbsAuthorize, bbs);
		checkCommentActivated(preBbsAuthorize, bbs);
	}
	
	private void checkBbsActivated(PreBbsAuthorize preBbsAuthorize, Bbs bbs) {
		if (!preBbsAuthorize.checkBbsActivated()) {
			return;
		}
		if (!bbs.isBbsActivated()) {
			throw new BlueskyException("bbs.NOT_ACTIVATED_BBS");
		}
	}
	
	private void checkArticleActivated(PreBbsAuthorize preBbsAuthorize, Bbs bbs) {
		if (!preBbsAuthorize.checkArticleActivated()) {
			return;
		}
		if (!bbs.isArticleActivated()) {
			throw new BlueskyException("bbs.NOT_ACTIVATED_ARTICLE");
		}
	}
	
	private void checkReplyActivated(PreBbsAuthorize preBbsAuthorize, Bbs bbs) {
		if (!preBbsAuthorize.checkReplyActivated()) {
			return;
		}
		if (!bbs.isReplyActivated()) {
			throw new BlueskyException("bbs.NOT_ACTIVATED_REPLY");
		}
	}
	
	private void checkCommentActivated(PreBbsAuthorize preBbsAuthorize, Bbs bbs) {
		if (!preBbsAuthorize.checkCommentActivated()) {
			return;
		}
		if (!bbs.isCommentActivated()) {
			throw new BlueskyException("bbs.NOT_ACTIVATED_COMMENT");
		}
	}
}
