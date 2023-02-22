package net.luversof.web.gate.index.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;

@RestControllerAdvice
public class GateExceptionHandler {
	
	@Autowired
	private ObjectMapper objectMapper;

	@ExceptionHandler
	public ProblemDetail handleException(FeignException exception) {
		ProblemDetail problemDetail = null;
		try {
			problemDetail = objectMapper.readValue(exception.contentUTF8(), ProblemDetail.class);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		}
		return problemDetail;
	}
}
