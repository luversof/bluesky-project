package net.luversof.web.gate.feign.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import io.github.luversof.boot.autoconfigure.web.util.ExceptionUtil;

@ControllerAdvice
public class GateFeignExceptionHandler {
	
	@Autowired
	private ObjectMapper objectMapper;

	@ExceptionHandler
	public <T extends FeignException> Object handleException(T exception, HandlerMethod handlerMethod, NativeWebRequest nativeWebRequest) {
		ProblemDetail problemDetail = null;
		try {
			problemDetail = objectMapper.readValue(exception.contentUTF8(), ProblemDetail.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		}
		return ExceptionUtil.handleException(problemDetail, handlerMethod, nativeWebRequest);
	}
}
