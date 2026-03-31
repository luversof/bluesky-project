package net.luversof.web.common.menu.domain;

import io.github.luversof.boot.context.support.MessageUtil;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class Menu {
    private String messageCode;
    private String url;
    private String activeUrlPattern;
    private boolean isDisplay = true;

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getActiveUrlPattern() {
        return activeUrlPattern;
    }

    public void setActiveUrlPattern(String activeUrlPattern) {
        this.activeUrlPattern = activeUrlPattern;
    }

    public boolean isDisplay() {
        return isDisplay;
    }

    public void setDisplay(boolean isDisplay) {
        this.isDisplay = isDisplay;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Menu other = (Menu) obj;
        return Objects.equals(activeUrlPattern, other.activeUrlPattern)
                && isDisplay == other.isDisplay
                && Objects.equals(messageCode, other.messageCode)
                && Objects.equals(url, other.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activeUrlPattern, isDisplay, messageCode, url);
    }

    @Override
    public String toString() {
        return "Menu [messageCode="
                + messageCode
                + ", url="
                + url
                + ", activeUrlPattern="
                + activeUrlPattern
                + ", isDisplay="
                + isDisplay
                + "]";
    }

    public boolean isCurrentMenu() {
        var requestUri =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getRequest()
                        .getRequestURI();
        if (activeUrlPattern != null) {
            return Pattern.compile(activeUrlPattern).matcher(requestUri).matches();
        }
        return requestUri.startsWith(url);
    }

    public String getName() {
        return MessageUtil.getMessage(messageCode, messageCode);
    }
}
