package net.luversof.web.gate.index.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
public class IndexController {

  @GetMapping({"/", "/index"})
  public String index() {
    return "index";
  }

  @GetMapping("/dev")
  public void dev() {}

  /** 표 UI 견본(2026-11-10): 주식 화면 표의 기준 모양을 고르는 개발용 화면. 가짜 데이터만 그린다. */
  @GetMapping("/dev/ui-catalog")
  public String uiCatalog() {
    return "dev/uiCatalog";
  }
}
