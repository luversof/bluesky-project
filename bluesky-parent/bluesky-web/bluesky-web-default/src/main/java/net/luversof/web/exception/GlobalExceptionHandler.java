package net.luversof.web.exception;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;
import net.luversof.core.exception.BlueskyException;
import net.luversof.core.exception.ErrorCode;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
	
	@Resource(name = "messageSourceAccessor")
	private MessageSourceAccessor messageSourceAccessor;
	
	public static final String RESULT = "result";

	public static final String PAGE_ERROR = "error/error";
	
	private DefaultMessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
	
	private static final String EXCEPTION_OBJECT_NAME = "exceptionMessage";
	
//	@ExceptionHandler
//	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
//	public ModelAndView handleException(Exception exception) {
//		Map<String, String> map = new HashMap<>();
//		map.put("class", exception.getClass().getName());
//		// MappingJacksonJsonView의 경우  (entry.getValue() instanceof BindingResult) 조건 체크를 하여 exception은 리턴하지 않으므로 별도의 결과값을 주어야 함
//		// Exception Message 규칙을 정할 필요가 있음
//		map.put("message", exception.getLocalizedMessage());
//		log.debug("GlobalControllerExceptionHandler called {}", exception);
//		
//		Map<String, Map<String, String>> resultMap = new HashMap<>();
//		resultMap.put("result", map);
//		return new ModelAndView("/error", resultMap);
//	}
	
	@ExceptionHandler
	public ModelAndView handleException(BlueskyException exception) {
		if (exception.getErrorCode() == ErrorCode.NOT_EXIST_BLOG.name()) {
			return new ModelAndView("redirect:/blog/create");
		}
		if (exception.getErrorCode() == ErrorCode.NOT_EXIST_BOOKKEEPING.name()) {
			return new ModelAndView("redirect:/bookkeeping/create");
		}
		
		String[] errorCodes = messageCodesResolver.resolveMessageCodes(EXCEPTION_OBJECT_NAME, String.valueOf(exception.getErrorCode()));
		log.debug("[HaException error message] code : {}", Arrays.deepToString(errorCodes));
		DefaultMessageSourceResolvable defaultMessageSourceResolvable = new DefaultMessageSourceResolvable(errorCodes, exception.getMessage() == null ? exception.getErrorCode() : exception.getMessage());
    	String localizedMessage = messageSourceAccessor.getMessage(defaultMessageSourceResolvable);
		
		ErrorMessage errorMessage = new ErrorMessage();
		if(StringUtils.isEmpty(localizedMessage)){
			errorMessage.setMessage(exception.getMessage());
		} else {
			errorMessage.setMessage(localizedMessage);
			errorMessage.setDisplayableMessage(true);
		}
		errorMessage.setObject(exception.getErrorCode());
		errorMessage.setExceptionClassName(exception.getClass().getSimpleName());

		Map<String, ErrorMessage> resultMap = new HashMap<>();
		resultMap.put(RESULT, errorMessage);
		return new ModelAndView(PAGE_ERROR, resultMap);
	}
	
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(BindException exception) {
		List<ErrorMessage> errorMessageList = new ArrayList<>();
		exception.getBindingResult().getAllErrors().get(0);
		for (FieldError fieldError : exception.getFieldErrors()) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage(messageSourceAccessor.getMessage(fieldError));
			errorMessage.setField(fieldError.getField());
			errorMessage.setObject(fieldError.getObjectName());
			errorMessage.setDisplayableMessage(true);
			errorMessageList.add(errorMessage);
			log.debug("[bindException error message] code : {}, arguments : {}", Arrays.deepToString(fieldError.getCodes()), Arrays.deepToString(fieldError.getArguments()));
		}
		if (exception.getFieldErrors().isEmpty()) {
			for (ObjectError objectError : exception.getBindingResult().getAllErrors()) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage(messageSourceAccessor.getMessage(objectError));
				errorMessage.setObject(objectError.getObjectName());
				errorMessage.setDisplayableMessage(true);
				errorMessageList.add(errorMessage);
				log.debug("[bindException error message] code : {}, arguments : {}", Arrays.deepToString(objectError.getCodes()), Arrays.deepToString(objectError.getArguments()));
			}
		}
		Map<String, List<ErrorMessage>> resultMap = new HashMap<>();
		resultMap.put(RESULT, errorMessageList);
		return new ModelAndView(PAGE_ERROR, resultMap);
	}
	
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(Exception exception) {
		log.error("exception", exception);
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setExceptionClassName(exception.getClass().getSimpleName());
		errorMessage.setMessage(exception.getLocalizedMessage());
		Map<String, ErrorMessage> resultMap = new HashMap<>();
		resultMap.put("errorMessage", errorMessage);
		return new ModelAndView(PAGE_ERROR, resultMap);
	}

	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
	public ModelAndView preAuthenticatedCredentialsNotFoundException(PreAuthenticatedCredentialsNotFoundException PreAuthenticatedCredentialsNotFoundException) {
		return new ModelAndView("login");
	}

	@Value("${oauth2.client.battleNet.clientId}")
	private String battleNetClientId;
	
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
	public ModelAndView accessDeniedException(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
		if (request.getRequestURI().equals("/battleNet/d3/index")) {
			response.sendRedirect("https://kr.battle.net/oauth/authorize?client_id=" + battleNetClientId + "&redirect_uri=https://localhost:8443/oauth/battleNetAuthorizeResult&scope=wow.profile&response_type=code");
			return new ModelAndView("redirect:https://kr.battle.net/oauth/authorize?client_id=" + battleNetClientId + "&redirect_uri=https://localhost:8443/oauth/battleNetAuthorizeResult&scope=wow.profile&response_type=code");
		} else {
		}
		return new ModelAndView("login");
	}
}
