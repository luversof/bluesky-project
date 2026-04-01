package net.luversof.web.gate.advice;

import net.luversof.client.user.util.UserUtil;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated() {
        return UserUtil.getUserId() != null;
    }

    @ModelAttribute("username")
    public String username() {
        return UserUtil.getUsername();
    }
}
