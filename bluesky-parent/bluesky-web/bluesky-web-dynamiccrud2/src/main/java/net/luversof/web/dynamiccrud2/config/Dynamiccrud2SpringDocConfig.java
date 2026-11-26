package net.luversof.web.dynamiccrud2.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * JSON 호출 문서(Swagger UI: /swagger-ui.html, 명세: /v3/api-docs/{group}). 화면(@Controller)은 빼고 @RestController 만 싣는다.
 *
 * <ul>
 *   <li>use: 사용 화면 JSON 호출(메뉴 조회 · 작업 실행, MenuApiController)
 *   <li>admin: 관리 JSON 호출(/admin/api/**, 설정 관리 권한)
 * </ul>
 *
 * 호출은 로그인 세션(쿠키)으로 한다 - 화면과 같은 권한 검사를 거친다.
 */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Dynamic CRUD 2 API",
            version = "1",
            description =
                """
                Dynamic CRUD 2 의 JSON 호출. 로그인 세션(쿠키)으로 호출하고 화면과 같은 권한 검사를 거친다.
                - use: 사용 화면 - 메뉴 조회 · 입력/수정/삭제 실행(메뉴 권한 · 작업 권한)
                - admin: 관리 - 프로젝트 · 메인 메뉴 · 메뉴 · 쿼리 · 검색 설정 · 필드 · 변경 이력(설정 관리 권한)
                오류는 {"error": "사유"} 와 400(입력 · 검사), 401(로그인 없음), 403(권한 없음), 404(없는 대상).
                """))
public class Dynamiccrud2SpringDocConfig {

  @Bean
  GroupedOpenApi useApi() {
    return GroupedOpenApi.builder()
        .group("use")
        .displayName("사용 화면")
        .pathsToExclude("/admin/**")
        .addOpenApiMethodFilter(
            method -> method.getDeclaringClass().isAnnotationPresent(RestController.class))
        .build();
  }

  @Bean
  GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder()
        .group("admin")
        .displayName("관리")
        .pathsToMatch("/admin/api/**")
        .addOpenApiMethodFilter(
            method -> method.getDeclaringClass().isAnnotationPresent(RestController.class))
        .build();
  }
}
