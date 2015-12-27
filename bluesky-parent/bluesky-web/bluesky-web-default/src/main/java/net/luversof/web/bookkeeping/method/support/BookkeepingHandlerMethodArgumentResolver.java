package net.luversof.web.bookkeeping.method.support;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.core.exception.BlueskyException;
import net.luversof.core.exception.ErrorCode;
import net.luversof.security.core.userdetails.BlueskyUser;

public class BookkeepingHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Bookkeeping.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof BlueskyUser)) {
			throw new PreAuthenticatedCredentialsNotFoundException("BlogHandlerMethodArgumentResolver");
		}
		BlueskyUser user = (BlueskyUser) authentication.getPrincipal();
		
		List<Bookkeeping> bookkeepingList = bookkeepingService.findByUserId(user.getId());
		if (bookkeepingList.isEmpty()) {
			throw new BlueskyException(ErrorCode.NOT_EXIST_BOOKKEEPING);
		}
		return bookkeepingList.get(0);
	}

}
