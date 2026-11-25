package net.luversof.api.common.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.providers.JavadocProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * springdoc 이 Javadoc 으로 만든 API 문서를 bluesky-api-* 공통 규칙에 맞춘다(규칙은 {@link JavadocDocsCustomizer}).
 */
@AutoConfiguration
@ConditionalOnClass(GlobalOperationCustomizer.class)
public class ApiCommonSpringDocAutoConfiguration {

  @Bean
  JavadocDocsCustomizer apiCommonJavadocDocsCustomizer(ObjectProvider<JavadocProvider> javadoc) {
    return new JavadocDocsCustomizer(javadoc.getIfAvailable());
  }

  /**
   * Javadoc 문서 규칙.
   *
   * <ul>
   *   <li>엔드포인트 Javadoc 의 첫 줄이 summary(목록에 보이는 짧은 이름, 예: "게시판 생성"), 그 뒤 {@code <p>} 문단이 description
   *       이다. springdoc 은 description 에 첫 줄까지 다시 넣으므로 앞의 summary 를 떼고, 남는 것이 없으면 비운다.
   *   <li>컨트롤러 클래스 Javadoc 의 첫 줄이 태그 이름(예: "게시판"), 나머지가 태그 설명이다. 클래스 이름에서 만든 "board-controller" 같은
   *       태그 대신 쓴다. 클래스 Javadoc 이 없으면 원래 태그를 둔다.
   * </ul>
   */
  public static class JavadocDocsCustomizer
      implements GlobalOperationCustomizer, GlobalOpenApiCustomizer {

    private final JavadocProvider javadoc;

    /** 원래 태그(board-controller) → 새 태그. */
    private final Map<String, Tag> replacedTags = new ConcurrentHashMap<>();

    public JavadocDocsCustomizer(JavadocProvider javadoc) {
      this.javadoc = javadoc;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
      String summary = operation.getSummary() == null ? null : operation.getSummary().strip();
      operation.setSummary(StringUtils.hasText(summary) ? summary : null);
      operation.setDescription(withoutSummary(operation.getDescription(), summary));

      Tag tag = tagOf(handlerMethod.getBeanType());
      if (tag != null) {
        if (operation.getTags() != null) {
          operation.getTags().forEach(original -> replacedTags.put(original, tag));
        }
        operation.setTags(new ArrayList<>(List.of(tag.getName())));
      }
      return operation;
    }

    @Override
    public void customise(OpenAPI openApi) {
      Map<String, Tag> tags = new LinkedHashMap<>();
      if (openApi.getTags() != null) {
        for (Tag existing : openApi.getTags()) {
          Tag tag = replacedTags.getOrDefault(existing.getName(), existing);
          tags.putIfAbsent(tag.getName(), tag);
        }
      }
      replacedTags.values().forEach(tag -> tags.putIfAbsent(tag.getName(), tag));
      openApi.setTags(new ArrayList<>(tags.values()));
    }

    Tag tagOf(Class<?> controller) {
      if (javadoc == null) {
        return null;
      }
      String doc = javadoc.getClassJavadoc(controller);
      if (!StringUtils.hasText(doc)) {
        return null;
      }
      String name = javadoc.getFirstSentence(doc).strip();
      if (!StringUtils.hasText(name)) {
        return null;
      }
      return new Tag().name(name).description(withoutSummary(doc, name));
    }

    static String withoutSummary(String description, String summary) {
      if (description == null) {
        return null;
      }
      String rest = description.strip();
      if (StringUtils.hasText(summary) && rest.startsWith(summary)) {
        rest = rest.substring(summary.length()).strip();
      }
      return rest.isEmpty() ? null : rest;
    }
  }
}
