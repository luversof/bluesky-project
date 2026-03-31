package net.luversof.web.dynamiccrud.index.controller;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;

@DevCheckController
@RequestMapping(value = "/security", produces = MediaType.APPLICATION_JSON_VALUE)
public class SecurityDevCheckController {

    @GetMapping("/authorities")
    public Collection<? extends GrantedAuthority> authorities() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    }
}
