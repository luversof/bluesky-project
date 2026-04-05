package net.luversof.web.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginRedirectController {

  @GetMapping("/login/redirect")
  public String redirect(@RequestParam String redirectUrl, HttpSession session) {
    session.setAttribute("redirectUrl", redirectUrl);
    return "redirect:/login";
  }
}
