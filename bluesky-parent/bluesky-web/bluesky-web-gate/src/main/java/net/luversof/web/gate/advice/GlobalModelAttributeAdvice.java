package net.luversof.web.gate.advice;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import net.luversof.client.user.util.UserUtil;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

  private final Environment environment;

  public GlobalModelAttributeAdvice(Environment environment) {
    this.environment = environment;
  }

  @ModelAttribute("isAuthenticated")
  public boolean isAuthenticated() {
    return UserUtil.getUserId() != null;
  }

  @ModelAttribute("username")
  public String username() {
    return UserUtil.getUsername();
  }

  /** 상단 badge 에 표시할 활성 프로파일. 미지정 실행(로컬 IDE 등)일 때만 "local". */
  @ModelAttribute("profile")
  public String profile() {
    String[] activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length == 0 ? "local" : String.join(",", activeProfiles);
  }
}
