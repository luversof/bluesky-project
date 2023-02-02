package net.luversof.web.gate.index.controller;
//package net.luversof.api.gate.index.controller;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ProblemDetail;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import io.github.luversof.boot.exception.BlueskyException;
//
//@RestControllerAdvice
//public class GateExceptionHandler {
//
//	@ExceptionHandler
//	public ProblemDetail handleException(BlueskyException exception) {
//		var problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.LENGTH_REQUIRED, exception.getMessage());
//		problemDetail.setProperty("testKey", "testvalue");
//		return problemDetail;
//	}
//}
