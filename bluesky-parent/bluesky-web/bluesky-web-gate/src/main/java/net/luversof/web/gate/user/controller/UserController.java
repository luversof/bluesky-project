package net.luversof.web.gate.user.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.luversof.boot.security.access.prepost.BlueskyPreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.domain.LoginUser;
import net.luversof.client.user.util.UserUtil;

@Controller
public class UserController {

  @Value("${bluesky.client.user.login-url}")
  private String loginUrl;

  @BlueskyPreAuthorize
  @GetMapping("/user/info")
  @ResponseBody
  public Map<String, Object> userInfo(
      @AuthenticationPrincipal LoginUser principal, Authentication authentication) {
    if (principal == null) {
      return Map.of("error", "Not authenticated");
    }

    return Map.of(
        "loginUser",
        principal,
        "authorities",
        authentication.getAuthorities(),
        "userId",
        UserUtil.getUserId() != null ? UserUtil.getUserId().toString() : "null",
        "username",
        UserUtil.getUsername() != null ? UserUtil.getUsername() : "null");
  }

  @GetMapping("/login")
  public String login(
      @RequestParam(required = false) String redirectUrl, HttpServletRequest request) {
    if (redirectUrl == null) {
      redirectUrl = request.getHeader(org.springframework.http.HttpHeaders.REFERER);
    }

    if (redirectUrl != null) {
      return "redirect:"
          + UriComponentsBuilder.fromUriString(loginUrl)
              .replaceQueryParam("redirectUrl", redirectUrl)
              .build()
              .toUriString();
    }

    return "redirect:" + loginUrl;
  }
}
