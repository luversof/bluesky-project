package net.luversof.web.gate.poe.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** PoE 게임 데이터 산출물(아이콘 등)을 정적 리소스로 서빙한다. 데이터 디렉토리는 git 밖(~/.poe-gamedata). */
@Configuration
public class PoeWebConfig implements WebMvcConfigurer {

  private final String dataDir;

  public PoeWebConfig(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataDir = dataDir;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // htmx fragment 응답은 캐시 금지 — 브라우저가 상세 레이어 GET 을 캐시하면 데이터 갱신 후에도
    // 옛 툴팁이 계속 보이는 문제가 생긴다(고유 속성/변동 수치색 등 갱신 반영 안 됨).
    registry
        .addInterceptor(
            new org.springframework.web.servlet.HandlerInterceptor() {
              @Override
              public void postHandle(
                  jakarta.servlet.http.HttpServletRequest request,
                  jakarta.servlet.http.HttpServletResponse response,
                  Object handler,
                  org.springframework.web.servlet.ModelAndView modelAndView) {
                response.setHeader("Cache-Control", "no-store, must-revalidate");
              }
            })
        .addPathPatterns("/poe/htmx/**");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/poe-assets/**")
        .addResourceLocations(Path.of(dataDir, "icons").toUri().toString())
        .setCachePeriod(60 * 60 * 24);
    // 클라이언트 렌더링용 데이터(패시브 트리 등) — 표시 화면과 같은 공개 데이터라 그대로 서빙한다
    registry
        .addResourceHandler("/poe-data/**")
        .addResourceLocations(Path.of(dataDir).toUri().toString())
        .setCachePeriod(60 * 60);
  }
}
