package net.luversof.core.util;


import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 요청받은 request 의 parameter를 기반으로 modelAttribute object를 호출하는 유틸.
 * servletRequest를 기반으로 하기 때문에 되도록 controller 에서 사용해야함
 * @author bluesky
 *
 */
public class ServletRequestDataBinderUtil {

	public static <T> T getObject(String objectName, Class<T> clazz, Object... validationHints) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

		T instantiateClass = BeanUtils.instantiateClass(clazz);
		
		ServletRequestDataBinder servletRequestDataBinder = new ServletRequestDataBinder(instantiateClass, objectName);
		servletRequestDataBinder.bind(request);
		
		@SuppressWarnings("unchecked")
		T target = (T) servletRequestDataBinder.getTarget();
		
		if (validationHints != null) {
			ValidationUtil.validate(target, validationHints);
		}
		
		return target;
	}

	public static <T> T getObject(Class<T> clazz) {
		return getObject(null, clazz, (Object[]) null);
	}
	
	public static <T> T getObject(String objectName, Class<T> clazz) {
		return getObject(objectName, clazz, (Object[]) null);
	}
	
	public static <T> T getObject(Class<T> clazz, Object... validationHints) {
		return getObject(null, clazz, validationHints);
	}
}