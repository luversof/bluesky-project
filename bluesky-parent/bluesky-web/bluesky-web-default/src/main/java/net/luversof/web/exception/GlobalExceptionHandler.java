package net.luversof.web.exception;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import net.luversof.core.exception.BlueskyException;
import net.luversof.core.exception.ErrorCode;

@ControllerAdvice
public class GlobalExceptionHandler {
	
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
		return null;
	}
	
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(BindException exception) {
		List<ErrorMessage> errorList = new ArrayList<>();
		for (FieldError fieldError : exception.getFieldErrors()) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage(fieldError.getDefaultMessage());
			errorMessage.setObjectName(fieldError.getObjectName());
			errorMessage.setField(fieldError.getField());
			errorList.add(errorMessage);
		}
		Map<String, List<ErrorMessage>> resultMap = new HashMap<>();
		resultMap.put("errorList", errorList);
		return new ModelAndView("error", resultMap);
	}
	
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(Exception exception) {
		exception.printStackTrace();
		
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setObjectName(exception.getClass().getSimpleName());
		errorMessage.setMessage(exception.getLocalizedMessage());
		Map<String, ErrorMessage> resultMap = new HashMap<>();
		resultMap.put("errorMessage", errorMessage);
		return new ModelAndView("error", resultMap);
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
