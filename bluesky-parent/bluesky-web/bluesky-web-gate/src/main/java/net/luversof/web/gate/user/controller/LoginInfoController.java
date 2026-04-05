package net.luversof.web.gate.user.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.client.user.util.UserUtil;

@RestController
@RequestMapping(value = "/api/loginInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class LoginInfoController {

  @GetMapping
  public Map<String, Object> loginInfo() {
    return Map.of(
        "authenticated", SecurityContextHolder.getContext().getAuthentication().isAuthenticated(),
        "userId", UserUtil.getUserId() != null ? UserUtil.getUserId().toString() : "",
        "username", UserUtil.getUsername() != null ? UserUtil.getUsername() : "");
  }
}
