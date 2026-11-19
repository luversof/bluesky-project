package net.luversof.web.dynamiccrud2.config;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 상단 언어 전환(_components/localeToggle). 고른 언어를 bluesky-boot 로케일 처리(쿠키)에 저장하고 보던 화면으로 돌아간다. 주소 파라미터로
 * 바꾸지 않는 것은 사용 화면 검색 파라미터(q · f.* · s.*)와 섞이지 않게 하려는 것.
 */
@Controller
public class LocaleController {

  static final Set<String> SUPPORTED = Set.of("ko", "en");

  private final LocaleResolver localeResolver;

  public LocaleController(LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  @PostMapping("/locale")
  public String change(
      @RequestParam String lang,
      @RequestHeader(name = "Referer", required = false) String referer,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (SUPPORTED.contains(lang)) {
      localeResolver.setLocale(request, response, "en".equals(lang) ? Locale.US : Locale.KOREA);
    }
    return "redirect:" + backTo(referer);
  }

  /** 같은 사이트 안 경로로만 돌아간다(다른 호스트 Referer 로 열린 리다이렉트를 만들지 않게) */
  static String backTo(String referer) {
    if (referer == null || referer.isBlank()) {
      return "/";
    }
    try {
      var uri = URI.create(referer);
      var path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
      if (!path.startsWith("/") || path.startsWith("//")) {
        return "/";
      }
      return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    } catch (IllegalArgumentException e) {
      return "/";
    }
  }
}
