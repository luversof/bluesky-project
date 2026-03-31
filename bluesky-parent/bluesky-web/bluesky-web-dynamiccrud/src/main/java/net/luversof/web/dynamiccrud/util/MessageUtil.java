package net.luversof.web.dynamiccrud.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import io.github.luversof.boot.context.ApplicationContextUtil;

public class MessageUtil {
    public static String getMessage(String code) {
        var messageSource =
                ApplicationContextUtil.getApplicationContext().getBean(MessageSource.class);
        return messageSource.getMessage(code, null, "", LocaleContextHolder.getLocale());
    }

    public static String getMessage(String code, Object... args) {
        var messageSource =
                ApplicationContextUtil.getApplicationContext().getBean(MessageSource.class);
        return messageSource.getMessage(code, args, "", LocaleContextHolder.getLocale());
    }
}
