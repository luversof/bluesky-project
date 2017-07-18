package net.luversof.security.exception;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.core.exception.BlueskyException;
import net.luversof.core.exception.ErrorMessage;
import net.luversof.core.exception.ErrorPage;
import net.luversof.core.util.CoreUtil;

@Slf4j
@ControllerAdvice
public class SecurityExceptionHandler {
	
	public static final String RESULT = "result";

	private DefaultMessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
	
	@Autowired
	private ContentNegotiatingViewResolver contentNegotiatingViewResolver;

	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
	public ModelAndView preAuthenticatedCredentialsNotFoundException(PreAuthenticatedCredentialsNotFoundException exception) {
		return new ModelAndView("login");
	}

	@Value("${oauth2.client.battleNet.clientId}")
	private String battleNetClientId;
	
	@SneakyThrows
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
	public ModelAndView accessDeniedException(HttpServletResponse response, AccessDeniedException exception, HandlerMethod  handlerMethod, NativeWebRequest request) throws IOException {
		
		if (((HttpServletRequest) request.getNativeRequest()).getRequestURI().equals("/battleNet/d3/index")) {
			response.sendRedirect("https://kr.battle.net/oauth/authorize?client_id=" + battleNetClientId + "&redirect_uri=https://localhost:8443/oauth/battleNetAuthorizeResult&scope=wow.profile&response_type=code");
			return new ModelAndView("redirect:https://kr.battle.net/oauth/authorize?client_id=" + battleNetClientId + "&redirect_uri=https://localhost:8443/oauth/battleNetAuthorizeResult&scope=wow.profile&response_type=code");
		} else {
		}
		
		if (contentNegotiatingViewResolver.getContentNegotiationManager().resolveMediaTypes(request).contains(MediaType.APPLICATION_JSON)
				|| Arrays.asList(handlerMethod.getMethodAnnotation(RequestMapping.class).produces()).contains(MediaType.APPLICATION_JSON_VALUE)) {
			log.debug("json exception");
			Map<String, ErrorMessage> resultMap = new HashMap<>();
			resultMap.put(RESULT, getErrorMessage(exception));
			return new ModelAndView(ErrorPage.DEFAULT, resultMap);
		}
		
		throw exception;
	}
	
	

	private ErrorMessage getErrorMessage(Exception exception) {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setExceptionClassName(exception.getClass().getSimpleName());
		
		if (exception instanceof BlueskyException) {
			String targetErrorCode = ((BlueskyException) exception).getErrorCode();
			//ERROR_CODE가 enum 값인 경우와 일반 String 인 경우를 구분지어야 함.
			//두 경우의 구분은 어떻게 해야할까? -> 단순히 공백이 있고 없고로 판단하면 되지 않을까?
			//공백이 있는 문자열의 경우 메세지로 판단
			if (targetErrorCode == null || targetErrorCode.contains(" ")) {
				errorMessage.setMessage(targetErrorCode);
			} else {
				String[] errorCodes = messageCodesResolver.resolveMessageCodes(exception.getClass().getSimpleName(), targetErrorCode);
				log.debug("[Exception error message] code : {}", Arrays.asList(errorCodes));
				DefaultMessageSourceResolvable defaultMessageSourceResolvable = new DefaultMessageSourceResolvable(errorCodes,  targetErrorCode);
				String localizedMessage = CoreUtil.getMessage(defaultMessageSourceResolvable);
				errorMessage.setMessage(localizedMessage);
				errorMessage.setDisplayableMessage(true);
			};
		} else {
			errorMessage.setMessage(exception.getLocalizedMessage());
		}
		return errorMessage;
	}
}
