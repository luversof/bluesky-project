package net.luversof.core.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
import net.luversof.boot.autoconfigure.context.MessageUtil;

@Slf4j
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CoreExceptionHandler {
	
	public static final String RESULT = "result";

	private DefaultMessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
	
	@Autowired
	private ContentNegotiatingViewResolver contentNegotiatingViewResolver;
	
	@ExceptionHandler
	@SneakyThrows
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(BlueskyException exception, HandlerMethod  handlerMethod, NativeWebRequest request) {
		if (contentNegotiatingViewResolver.getContentNegotiationManager().resolveMediaTypes(request).contains(MediaType.APPLICATION_JSON)) {
			log.debug("json exception");
		}
		
		if (Arrays.asList(handlerMethod.getMethodAnnotation(RequestMapping.class).produces()).contains(MediaType.APPLICATION_JSON_VALUE) ){
			log.debug("json exception");
		};

		Map<String, ErrorMessage> resultMap = new HashMap<>();
		resultMap.put(RESULT, getErrorMessage(exception));
		return new ModelAndView(exception.getErrorPage(), resultMap);
	}
	
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(BindException exception) {
		exception.printStackTrace();
		List<ErrorMessage> errorMessageList = new ArrayList<>();
		List<? extends ObjectError> objectErrorList = exception.getFieldErrors().isEmpty() ? exception.getBindingResult().getAllErrors() : exception.getFieldErrors();
		for (ObjectError objectError : objectErrorList) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setExceptionClassName(exception.getClass().getSimpleName());
			errorMessage.setMessage(MessageUtil.getMessage(objectError));
			errorMessage.setObject(objectError.getObjectName());
			errorMessage.setDisplayableMessage(true);
			if (objectError instanceof FieldError) {
				errorMessage.setField(((FieldError) objectError).getField());
			}
			errorMessageList.add(errorMessage);
			log.debug("[bindException error message] code : {}, arguments : {}", Arrays.asList(objectError.getCodes()), Arrays.asList(objectError.getArguments()));
		}
		
		Map<String, List<ErrorMessage>> resultMap = new HashMap<>();
		resultMap.put(RESULT, errorMessageList);
		return new ModelAndView(ErrorPage.DEFAULT, resultMap);
	}
	
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(MethodArgumentNotValidException exception) {
		List<ErrorMessage> errorMessageList = new ArrayList<>();
		List<? extends ObjectError> objectErrorList = exception.getBindingResult().getFieldErrors().isEmpty() ? exception.getBindingResult().getAllErrors() : exception.getBindingResult().getFieldErrors();
		for (ObjectError objectError : objectErrorList) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setExceptionClassName(exception.getClass().getSimpleName());
			errorMessage.setMessage(MessageUtil.getMessage(objectError));
			errorMessage.setObject(objectError.getObjectName());
			errorMessage.setDisplayableMessage(true);
			if (objectError instanceof FieldError) {
				errorMessage.setField(((FieldError) objectError).getField());
			}
			errorMessageList.add(errorMessage);
			log.debug("[bindException error message] code : {}, arguments : {}", Arrays.asList(objectError.getCodes()), Arrays.asList(objectError.getArguments()));
		}
		
		Map<String, List<ErrorMessage>> resultMap = new HashMap<>();
		resultMap.put(RESULT, errorMessageList);
		return new ModelAndView(ErrorPage.DEFAULT, resultMap);
	}
	
	@ExceptionHandler
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(Exception exception) {
		log.debug("error : {}", exception);
		Map<String, ErrorMessage> resultMap = new HashMap<>();
		resultMap.put(RESULT, getErrorMessage(exception));
		return new ModelAndView(ErrorPage.DEFAULT, resultMap);
	}

	private ErrorMessage getErrorMessage(Exception exception) {
		ErrorMessage errorMessage = new ErrorMessage();
		errorMessage.setExceptionClassName(exception.getClass().getSimpleName());
		if (!(exception instanceof BlueskyException)) {
			errorMessage.setMessage(MessageUtil.getMessage(exception.getClass().getSimpleName(), exception.getLocalizedMessage()));
			return errorMessage;
		}
		
		String targetErrorCode = ((BlueskyException) exception).getErrorCode();
		//ERROR_CODE가 enum 값인 경우와 일반 String 인 경우를 구분지어야 함.
		//두 경우의 구분은 어떻게 해야할까? -> 단순히 공백이 있고 없고로 판단하면 되지 않을까?
		//공백이 있는 문자열의 경우 메세지로 판단
		if (targetErrorCode == null || targetErrorCode.contains(" ")) {
			errorMessage.setMessage(targetErrorCode);
			return errorMessage;
		}
		
		String[] errorCodes = messageCodesResolver.resolveMessageCodes(exception.getClass().getSimpleName(), targetErrorCode);
		log.debug("[Exception error message] code : {}", Arrays.asList(errorCodes));
		DefaultMessageSourceResolvable defaultMessageSourceResolvable = new DefaultMessageSourceResolvable(errorCodes, ((BlueskyException) exception).getErrorMessageArgs(), targetErrorCode);
		String localizedMessage = MessageUtil.getMessage(defaultMessageSourceResolvable);
		errorMessage.setErrorCode(((BlueskyException) exception).getErrorCode());
		errorMessage.setMessage(localizedMessage);
		errorMessage.setDisplayableMessage(true);
		
		return errorMessage;
	}
}
