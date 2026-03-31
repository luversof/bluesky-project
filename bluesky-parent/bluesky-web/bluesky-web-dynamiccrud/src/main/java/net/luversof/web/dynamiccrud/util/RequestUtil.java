package net.luversof.web.dynamiccrud.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtil {
    public static String[] getParameterValues(String name) {
        var reqAttr = RequestContextHolder.getRequestAttributes();
        if (reqAttr instanceof ServletRequestAttributes sra) {
            return sra.getRequest().getParameterValues(name);
        }
        return null;
    }

    public static String getParameter(String name) {
        var values = getParameterValues(name);
        return (values != null && values.length > 0) ? values[0] : null;
    }
}
