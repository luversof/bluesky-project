package net.luversof.core.util;

import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import lombok.Setter;
import lombok.SneakyThrows;


/**
 * ValidationUtil 을 spring data rest에서 편하게 사용하기 위해 제공하는 유틸
 * @author bluesky
 *
 */
public abstract class ValidationUtil {
	
	@Setter
	private static Validator validator;

	@SneakyThrows
	public static void validate(Object object, Object... validationHints) {
		BeanPropertyBindingResult beanPropertyBindingResult = new BeanPropertyBindingResult(object, StringUtils.uncapitalize(object.getClass().getSimpleName()));
		ValidationUtils.invokeValidator(validator, object, beanPropertyBindingResult, validationHints);
		if (beanPropertyBindingResult.hasErrors()) {
			throw new BindException(beanPropertyBindingResult);
		}
	}
}
