package net.luversof.api.blog.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.luversof.api.blog.domain.mariadb.Blog;

/**
 * {@link BlogController} 의 요청·응답 예시.
 *
 * <p>설명은 컨트롤러 Javadoc 에서 나온다. 여기에는 예시처럼 Javadoc 으로 쓸 수 없는 것만 둔다. 컨트롤러가 이 인터페이스를 구현하므로 메서드 시그니처가
 * 어긋나면 컴파일되지 않는다.
 */
interface BlogControllerDocs {

  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "사용자 블로그 만들기",
                      value =
                          """
                          {
                            "userId": "CBB5D7CA-5A48-E011-8195-18A90577F94A"
                          }
                          """)))
  @ApiResponse(
      responseCode = "200",
      content =
          @Content(
              examples =
                  @ExampleObject(
                      name = "만든 블로그",
                      value =
                          """
                          {
                            "idx": 1,
                            "userId": "CBB5D7CA-5A48-E011-8195-18A90577F94A",
                            "blogId": "E50D9C4B-050C-4DCC-B331-B6F095661E9D"
                          }
                          """)))
  Blog create(Blog blog);
}
