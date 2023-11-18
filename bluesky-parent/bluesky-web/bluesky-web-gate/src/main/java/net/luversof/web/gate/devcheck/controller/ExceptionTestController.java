package net.luversof.web.gate.devcheck.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Controller
public class ExceptionTestController {

	@GetMapping(value = "/blueskyExceptionHtml", produces = MediaType.TEXT_HTML_VALUE)
	public void blueskyExceptionHtml() {
		throw new BlueskyException("exceptionTest1");
	}
	
	@GetMapping(value = "/blueskyExceptionJson", produces = MediaType.APPLICATION_JSON_VALUE)
	public void blueskyExceptionJson() {
		throw new BlueskyException("exceptionTest1");
	}
	
	@GetMapping(value = "/blueskyExceptionHtml500", produces = MediaType.TEXT_HTML_VALUE)
	public void blueskyExceptionHtml500() {
		throw new BlueskyException("exceptionTest1", 500);
	}
	
	@GetMapping(value = "/blueskyExceptionJson500", produces = MediaType.APPLICATION_JSON_VALUE)
	public void blueskyExceptionJson500() {
		throw new BlueskyException("exceptionTest1", 500);
	}
	
	
	@GetMapping("/bindException")
	public TestRecord bindException(@Validated TestRecord testRecord) {
		return testRecord;
	}
	
	@PostMapping("/methodArgumentNotValidException")
	public TestRecord methodArgumentNotValidException(@RequestBody @Validated TestRecord testRecord) {
		return testRecord;
	}
	
	public static record TestRecord(@NotNull String strKey, @Min(3) int numKey) {
		
	}
}
