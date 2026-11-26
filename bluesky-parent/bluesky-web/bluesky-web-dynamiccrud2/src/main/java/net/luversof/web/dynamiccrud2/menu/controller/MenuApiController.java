package net.luversof.web.dynamiccrud2.menu.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.exception.BlueskyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.menu.service.MenuPageService;

/**
 * 사용 화면의 JSON 호출(로그인 세션으로). 화면(MenuPageController)과 같은 처리 · 권한 검사(메뉴 권한 · 작업 권한 · 행 서명 · 동시 수정 검사)를
 * 거치고 응답만 JSON 이다. 호출 설명은 메뉴 관리 화면 "JSON 호출"(MenuPageService.apiDoc).
 *
 * <ul>
 *   <li>조회: GET {메뉴 주소}/api - 화면과 같은 s. · f. · q · page · sort · desc
 *   <li>작업: POST {메뉴 주소}/api/action/{쿼리 id} - 본문 {"inputs": {이름: 값}, "rows": [목록의 row 값]}
 * </ul>
 *
 * 로그인 · 권한이 없으면 화면 이동 대신 401 · 403(AccessDeniedAdvice 가 JSON 요청을 가린다)
 */
@RestController
@Tag(name = "메뉴 사용", description = "사용 화면과 같은 조회 · 작업 실행. 메뉴 주소는 /{프로젝트}/{메인 메뉴}/{메뉴} (주소 이름 code)")
@ApiResponse(
    responseCode = "400",
    description = "입력값 · 검색값 · 행 서명 · 동시 수정 검사 실패, DB 거부",
    content = @Content(schema = @Schema(implementation = MenuApiController.ApiError.class)))
@ApiResponse(responseCode = "401", description = "로그인 없음")
@ApiResponse(responseCode = "403", description = "메뉴 · 작업 권한 없음")
@ApiResponse(responseCode = "404", description = "없거나 숨긴 메뉴 · 작업")
public class MenuApiController {

  private final MenuPageService useService;

  public MenuApiController(MenuPageService useService) {
    this.useService = useService;
  }

  /** 결과 컬럼. label = 결과 이름(정렬 sort 에 쓴다), header = 화면 제목 */
  @Schema(description = "결과 컬럼. label = 결과 이름(정렬 sort 에 쓴다), header = 화면 제목, inputType = 입력 형식")
  public record ApiColumn(String label, String header, String inputType, boolean sortable) {}

  /** 행. values 는 columns 순서(결과 이름이 겹칠 수 있어 맵이 아닌 목록), row 는 수정 · 삭제에 넘길 서명된 행 값 */
  @Schema(description = "행. values 는 columns 순서, row 는 수정 · 삭제 작업에 그대로 넘길 서명된 행 값")
  public record ApiRow(List<String> values, String row) {}

  /** message: 필수 검색값이 없거나 조회가 실패하면 그 사유(그때 rows 는 비어 있다) */
  public record ApiPage(
      long total,
      int page,
      int pageSize,
      String sort,
      boolean descending,
      String message,
      List<ApiColumn> columns,
      List<ApiRow> rows) {}

  @Schema(
      description =
          "작업 실행 본문. inputs = 파라미터 이름 → 값(화면 입력칸과 같은 글자), rows = 고른 행(조회 응답 rows[].row 그대로,"
              + " 수정 · 삭제처럼 행 키를 쓰는 작업만)")
  public record ApiAction(Map<String, String> inputs, List<String> rows) {}

  @Schema(description = "영향 행 수 합")
  public record ApiResult(int affected) {}

  @Schema(description = "오류 사유(화면에 보이는 문구와 같다)")
  public record ApiError(String error) {}

  @Operation(
      summary = "메뉴 조회",
      description =
          """
          사용 화면 목록과 같은 조회(한 쪽). 검색값은 화면과 같은 이름의 쿼리 파라미터로 넘긴다:
          - s.{파라미터} : 조회 쿼리의 :파라미터 검색칸
          - f.{결과 이름} : 메뉴 검색 설정 칸(범위의 끝은 f.{결과 이름}.to)
          - q : 통합 검색어
          메뉴마다 쓸 수 있는 이름은 메뉴 관리 화면의 "JSON 호출" 에 나온다. 필수 검색값이 없으면 rows 는 비고 message 에 사유.
          """)
  @GetMapping(MenuPageController.MENU_PATH + "/api")
  public ApiPage list(
      @Parameter(description = "프로젝트 주소 이름") @PathVariable String project,
      @Parameter(description = "메인 메뉴 주소 이름") @PathVariable String mainMenu,
      @Parameter(description = "메뉴 주소 이름") @PathVariable String menu,
      @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> query,
      @Parameter(description = "쪽 번호(0 부터)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "정렬할 결과 이름(정렬 허용 컬럼만, columns[].sortable)")
          @RequestParam(required = false)
          String sort,
      @Parameter(description = "내림차순") @RequestParam(defaultValue = "false") boolean desc) {
    var path = useService.resolve(project, mainMenu, menu);
    var usePage =
        useService.page(
            path.menu().getId(), MenuPageController.searchInput(query), page, sort, desc);
    return new ApiPage(
        usePage.total(),
        usePage.page(),
        usePage.pageSize(),
        usePage.sort(),
        usePage.descending(),
        usePage.message(),
        usePage.columns().stream()
            .map(c -> new ApiColumn(c.label(), c.header(), c.inputType().name(), c.sortable()))
            .toList(),
        usePage.rows().stream().map(r -> new ApiRow(r.values(), r.json())).toList());
  }

  @Operation(
      summary = "작업 실행(입력 · 수정 · 삭제)",
      description =
          "화면 작업 단추와 같은 실행 · 검사(필수 · 기본값 · 형식 · 수정 불가 컬럼 · 동시 수정). 한 트랜잭션, 데이터 변경 이력에 남는다."
              + " 작업 id 는 메뉴 관리 화면 \"JSON 호출\" 에 나온다.")
  @PostMapping(MenuPageController.MENU_PATH + "/api/action/{queryId}")
  public ApiResult action(
      @Parameter(description = "프로젝트 주소 이름") @PathVariable String project,
      @Parameter(description = "메인 메뉴 주소 이름") @PathVariable String mainMenu,
      @Parameter(description = "메뉴 주소 이름") @PathVariable String menu,
      @Parameter(description = "작업(쿼리) id") @PathVariable UUID queryId,
      @RequestBody ApiAction body) {
    var path = useService.resolve(project, mainMenu, menu);
    return new ApiResult(
        useService.run(
            path.menu().getId(),
            queryId,
            body.inputs() == null ? Map.of() : body.inputs(),
            body.rows() == null ? List.of() : body.rows()));
  }

  /** 입력값 · 행 서명 · 동시 수정 검사 실패(화면이면 폼에 보일 사유) */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError invalid(IllegalArgumentException e) {
    return new ApiError(e.getMessage());
  }

  @ExceptionHandler(BlueskyException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError rejected(BlueskyException e) {
    var message = QueryErrorCode.messageOf(e);
    return new ApiError(message == null ? e.getErrorCode() : message);
  }

  /** DB 가 받지 않은 실행(제약 위반 등) - 첫 줄만 */
  @ExceptionHandler(DataAccessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError failed(DataAccessException e) {
    var message = e.getMostSpecificCause().getMessage();
    return new ApiError(
        Msg.t(
            "menu.action.failed",
            message == null ? "" : message.lines().findFirst().orElse(message)));
  }
}
