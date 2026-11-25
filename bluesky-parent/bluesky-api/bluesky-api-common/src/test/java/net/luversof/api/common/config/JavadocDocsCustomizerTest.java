package net.luversof.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springdoc.core.providers.SpringDocJavadocProvider;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.tags.Tag;
import net.luversof.api.common.config.ApiCommonSpringDocAutoConfiguration.JavadocDocsCustomizer;

/**
 * Javadoc 첫 줄은 summary · 태그 이름, 나머지는 description 이 된다(springdoc 이 첫 줄을 description 에 다시 넣는 것을 뗀다).
 */
class JavadocDocsCustomizerTest {

  /**
   * 게시판
   *
   * <p>alias 로 구분한다.
   */
  static class SampleController {

    /** 게시판 목록 조회 */
    public void list() {}

    /**
     * 게시판 수정
     *
     * <p>OWNER 만 할 수 있다.
     */
    public void update() {}
  }

  static class UndocumentedController {
    public void list() {}
  }

  private final SpringDocJavadocProvider javadoc = new SpringDocJavadocProvider();
  private final JavadocDocsCustomizer customizer = new JavadocDocsCustomizer(javadoc);

  /** springdoc 이 Javadoc 으로 채운 그대로의 operation. */
  private Operation springdocOperation(Class<?> type, String method) throws Exception {
    var m = type.getMethod(method);
    String description = javadoc.getMethodJavadocDescription(m);
    return new Operation()
        .summary(description == null ? null : javadoc.getFirstSentence(description))
        .description(description)
        .tags(new ArrayList<>(List.of("sample-controller")));
  }

  private Operation customize(Class<?> type, String method) throws Exception {
    return customizer.customize(
        springdocOperation(type, method),
        new HandlerMethod(type.getDeclaredConstructor().newInstance(), type.getMethod(method)));
  }

  @Test
  void 첫_줄만_있으면_summary_만_남고_description_은_비운다() throws Exception {
    Operation op = customize(SampleController.class, "list");

    assertThat(op.getSummary()).isEqualTo("게시판 목록 조회");
    assertThat(op.getDescription()).isNull();
  }

  @Test
  void description_에서_summary_반복을_뗀다() throws Exception {
    Operation op = customize(SampleController.class, "update");

    assertThat(op.getSummary()).isEqualTo("게시판 수정");
    assertThat(op.getDescription()).isEqualTo("<p>OWNER 만 할 수 있다.");
  }

  @Test
  void 클래스_Javadoc_첫_줄이_태그가_된다() throws Exception {
    Operation op = customize(SampleController.class, "list");
    OpenAPI openApi =
        new OpenAPI().tags(new ArrayList<>(List.of(new Tag().name("sample-controller"))));
    customizer.customise(openApi);

    assertThat(op.getTags()).containsExactly("게시판");
    assertThat(openApi.getTags())
        .singleElement()
        .satisfies(tag -> {
          assertThat(tag.getName()).isEqualTo("게시판");
          assertThat(tag.getDescription()).isEqualTo("<p>alias 로 구분한다.");
        });
  }

  @Test
  void 클래스_Javadoc_이_없으면_태그를_두지_않는다() throws Exception {
    Operation op = customize(UndocumentedController.class, "list");

    assertThat(op.getTags()).containsExactly("sample-controller");
  }
}
