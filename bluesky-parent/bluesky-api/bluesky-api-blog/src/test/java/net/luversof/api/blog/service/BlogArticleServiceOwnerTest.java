package net.luversof.api.blog.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.blog.domain.mariadb.BlogArticle;
import net.luversof.api.blog.repository.mariadb.BlogArticleRepository;

/** 남의 글은 고치거나 지울 수 없다 - 이 검사가 블로그 글 소유권의 유일한 방어선이다(2026-11-08 감사). */
class BlogArticleServiceOwnerTest {

  private final BlogArticleRepository repository = mock(BlogArticleRepository.class);
  private final BlogArticleService service = new BlogArticleService();

  BlogArticleServiceOwnerTest() {
    ReflectionTestUtils.setField(service, "blogArticleRepository", repository);
    BlogArticle stored = article("owner");
    when(repository.findByBlogArticleId("a1")).thenReturn(Optional.of(stored));
  }

  private static BlogArticle article(String userId) {
    BlogArticle article = new BlogArticle();
    article.setBlogArticleId("a1");
    article.setUserId(userId);
    article.setTitle("t");
    article.setContent("c");
    return article;
  }

  @Test
  void 다른_사용자는_수정할_수_없다() {
    assertThatThrownBy(() -> service.update(article("other"))).isInstanceOf(BlueskyException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void 다른_사용자는_삭제할_수_없다() {
    assertThatThrownBy(() -> service.delete(article("other"))).isInstanceOf(BlueskyException.class);
    verify(repository, never()).delete(any());
  }
}
