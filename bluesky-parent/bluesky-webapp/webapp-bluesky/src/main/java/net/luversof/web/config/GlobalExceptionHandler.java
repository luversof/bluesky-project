package net.luversof.web.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import static net.luversof.core.Constants.JSON_MODEL_KEY;

@Slf4j
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
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	public ModelAndView handleException(BindException exception) {
		List<Map<String, String>> errorList = new ArrayList<>();
		for (FieldError fieldError : exception.getFieldErrors()) {
			Map<String, String> errorMap = new HashMap<>();
			errorMap.put("message", fieldError.getDefaultMessage());
			errorMap.put("objectName", fieldError.getObjectName());
			errorMap.put("field", fieldError.getField());
			errorList.add(errorMap);
		}
		Map<String, List<Map<String, String>>> resultMap = new HashMap<>();
		resultMap.put(JSON_MODEL_KEY, errorList);
		return new ModelAndView("/error", resultMap);
	}
}
