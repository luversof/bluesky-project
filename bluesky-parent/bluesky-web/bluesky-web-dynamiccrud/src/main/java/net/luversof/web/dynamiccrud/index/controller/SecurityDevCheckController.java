package net.luversof.web.dynamiccrud.index.controller;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import io.github.luversof.boot.devcheck.annotation.DevCheckDescription;

@DevCheckController
@RequestMapping(value = "/security", produces = MediaType.APPLICATION_JSON_VALUE)
public class SecurityDevCheckController {

  @DevCheckDescription("현재 사용자 권한 조회")
  @GetMapping("/authorities")
  public Collection<? extends GrantedAuthority> authorities() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities();
  }
}
