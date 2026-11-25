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
public class MenuApiController {

  private final MenuPageService useService;

  public MenuApiController(MenuPageService useService) {
    this.useService = useService;
  }

  /** 결과 컬럼. label = 결과 이름(정렬 sort 에 쓴다), header = 화면 제목 */
  public record ApiColumn(String label, String header, String inputType, boolean sortable) {}

  /** 행. values 는 columns 순서(결과 이름이 겹칠 수 있어 맵이 아닌 목록), row 는 수정 · 삭제에 넘길 서명된 행 값 */
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

  public record ApiAction(Map<String, String> inputs, List<String> rows) {}

  public record ApiResult(int affected) {}

  public record ApiError(String error) {}

  @GetMapping(MenuPageController.MENU_PATH + "/api")
  public ApiPage list(
      @PathVariable String project,
      @PathVariable String mainMenu,
      @PathVariable String menu,
      @RequestParam MultiValueMap<String, String> query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "false") boolean desc) {
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

  @PostMapping(MenuPageController.MENU_PATH + "/api/action/{queryId}")
  public ApiResult action(
      @PathVariable String project,
      @PathVariable String mainMenu,
      @PathVariable String menu,
      @PathVariable UUID queryId,
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
