package net.luversof.api.blog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocJavadocConfiguration;
import org.springdoc.core.configuration.SpringDocPageableConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.luversof.api.blog.service.BlogArticleCategoryService;
import net.luversof.api.blog.service.BlogArticleCommentService;
import net.luversof.api.blog.service.BlogArticleService;
import net.luversof.api.blog.service.BlogService;

/**
 * API 문서는 컨트롤러 Javadoc(설명)과 BlogControllerDocs(예시)에서 나온다. Javadoc 을 읽어 들이는 scribe 프로세서가 빠지거나 컨트롤러가
 * 문서 인터페이스를 놓으면 여기서 깨진다.
 */
@SpringBootTest(
    classes = BlogApiDocsTest.Config.class,
    properties = "spring.cloud.config.import-check.enabled=false")
class BlogApiDocsTest {

  @MockitoBean BlogService blogService;
  @MockitoBean BlogArticleService blogArticleService;
  @MockitoBean BlogArticleCategoryService blogArticleCategoryService;
  @MockitoBean BlogArticleCommentService blogArticleCommentService;

  @Autowired WebApplicationContext context;

  @Test
  void javadocAndExamplesBecomeApiDocs() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    String blog = "$.paths['/api/blog'].post";
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        // Javadoc
        .andExpect(jsonPath(blog + ".summary").value("블로그를 만든다."))
        .andExpect(jsonPath(blog + ".requestBody.description").value("만들 블로그(userId 필수)"))
        .andExpect(
            jsonPath(
                    "$.paths['/api/blog/search/findByBlogId/{blogId}'].get.parameters[0].description")
                .value("블로그 ID"))
        .andExpect(
            jsonPath(
                    "$.paths['/api/blogArticleComment/search/countByBlogArticleId/{blogArticleId}']"
                        + ".get.summary")
                .value("글의 댓글 수."))
        // BlogControllerDocs
        .andExpect(
            jsonPath(blog + ".requestBody.content['application/json'].examples['사용자 블로그 만들기']")
                .exists())
        .andExpect(
            jsonPath(blog + ".responses['200'].content['application/json'].examples['만든 블로그']")
                .exists());
  }

  @SpringBootConfiguration
  @ImportAutoConfiguration({
    DispatcherServletAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    SpringDocConfiguration.class,
    SpringDocConfigProperties.class,
    SpringDocJavadocConfiguration.class,
    SpringDocPageableConfiguration.class,
    SpringDocWebMvcConfiguration.class
  })
  @Import({
    BlogController.class,
    BlogArticleController.class,
    BlogArticleCategoryController.class,
    BlogArticleCommentController.class
  })
  static class Config {}
}
