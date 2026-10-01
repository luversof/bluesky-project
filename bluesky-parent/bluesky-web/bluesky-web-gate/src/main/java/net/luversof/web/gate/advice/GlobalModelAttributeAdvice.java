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
    return profileOf(environment);
  }

  /** 예외 처리 경로처럼 이 어드바이스가 돌지 않는 화면도 같은 badge 를 쓰도록 규칙을 한 곳에 둔다. */
  public static String profileOf(Environment environment) {
    String[] activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length == 0 ? "local" : String.join(",", activeProfiles);
  }
}
