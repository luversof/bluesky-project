package net.luversof.web.dynamiccrud2.menu.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.config.Msg;
import net.luversof.web.dynamiccrud2.menu.service.MenuPageService;
import net.luversof.web.dynamiccrud2.menu.service.MenuPageService.MenuPath;
import net.luversof.web.dynamiccrud2.menu.service.ValueConverter;
import net.luversof.web.dynamiccrud2.security.MenuAuthorizer;

/**
 * 사용 화면. 주소는 /{프로젝트}/{메인 메뉴}/{메뉴}(각 code). 검색 값은 s.{파라미터}, 작업 입력은 v.{파라미터}, 작업 대상 행은 rows(체크박스) ·
 * row(행 버튼)로 받는다(행 값 JSON).
 *
 * <p>code 는 영문 소문자 · 숫자 · - 라 점이 든 정적 파일(/main.css, /js/x.js)과 겹치지 않고, /admin 등 앱 경로는 프로젝트 code 로
 * 등록할 수 없다(MenuService.RESERVED_PROJECT_CODES). 글자 그대로인 경로(/admin/...)가 변수 경로보다 먼저 맞는다.
 */
@Controller
public class MenuPageController {

  static final String CODE = "[a-z0-9][a-z0-9-]*";
  static final String MENU_PATH =
      "/{project:" + CODE + "}/{mainMenu:" + CODE + "}/{menu:" + CODE + "}";

  private final MenuPageService useService;
  private final MenuAuthorizer authorizer;

  public MenuPageController(MenuPageService useService, MenuAuthorizer authorizer) {
    this.useService = useService;
    this.authorizer = authorizer;
  }

  /** 볼 수 있는 프로젝트 목록 */
  @GetMapping("/")
  public String index(Model model) {
    authorizer.checkLoggedIn();
    model.addAttribute("tree", useService.visibleTree());
    return "menu/index";
  }

  /** 프로젝트 주소만 오면 그 프로젝트의 첫 메뉴로 */
  @GetMapping("/{project:" + CODE + "}")
  public String project(@PathVariable String project) {
    authorizer.checkLoggedIn();
    var node =
        useService
            .visibleProject(project)
            .orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND));
    var mainMenu = node.mainMenus().get(0);
    return "redirect:" + MenuPath.url(node.project(), mainMenu.mainMenu(), mainMenu.menus().get(0));
  }

  /** 메인 메뉴 주소까지만 오면 그 메인 메뉴의 첫 메뉴로(볼 수 있는 메뉴가 없으면 404) */
  @GetMapping("/{project:" + CODE + "}/{mainMenu:" + CODE + "}")
  public String mainMenu(@PathVariable String project, @PathVariable String mainMenu) {
    authorizer.checkLoggedIn();
    var node = useService.visibleProject(project).orElseThrow(MenuPageController::notFound);
    var mainMenuNode =
        node.mainMenus().stream()
            .filter(m -> m.mainMenu().getCode().equals(mainMenu))
            .findFirst()
            .orElseThrow(MenuPageController::notFound);
    return "redirect:"
        + MenuPath.url(node.project(), mainMenuNode.mainMenu(), mainMenuNode.menus().get(0));
  }

  private static org.springframework.web.server.ResponseStatusException notFound() {
    return new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.NOT_FOUND);
  }

  @GetMapping(MENU_PATH)
  public String menu(
      @PathVariable String project,
      @PathVariable String mainMenu,
      @PathVariable String menu,
      @RequestParam MultiValueMap<String, String> query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "false") boolean desc,
      Model model) {
    var path = useService.resolve(project, mainMenu, menu);
    var usePage = useService.page(path.menu().getId(), searchInput(query), page, sort, desc);
    model.addAttribute("usePage", usePage);
    model.addAttribute("searchQuery", searchQuery(query));
    model.addAttribute("menuPath", path.url());
    // 왼쪽 메뉴(지금 프로젝트)와 상단 프로젝트 고르기(볼 수 있는 프로젝트)를 한 번 읽은 목록으로
    var tree = useService.visibleTree();
    model.addAttribute(
        "nav",
        tree.stream().filter(n -> n.project().getCode().equals(project)).findFirst().orElse(null));
    model.addAttribute("projects", tree.stream().map(n -> n.project()).toList());
    return "menu/page";
  }

  /** 작업 폼(htmx 조각). 행은 체크박스 rows 또는 행 버튼 row(행 JSON) */
  @PostMapping(MENU_PATH + "/action/{queryId}/form")
  public String form(
      @PathVariable String project,
      @PathVariable String mainMenu,
      @PathVariable String menu,
      @PathVariable UUID queryId,
      @RequestParam MultiValueMap<String, String> form,
      Model model) {
    var path = useService.resolve(project, mainMenu, menu);
    try {
      model.addAttribute(
          "form",
          useService.form(
              path.menu().getId(), queryId, targetRows(form.get("rows"), form.get("row"))));
    } catch (IllegalArgumentException e) {
      // 서명이 맞지 않는 행(RowSigner) - 공통 처리기가 500 으로 바꾸지 않게 400
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
    model.addAttribute("menuPath", path.url());
    model.addAttribute("error", null);
    model.addAttribute("inputs", Map.of());
    return "menu/actionForm";
  }

  /** 작업 실행(htmx). 성공하면 보던 목록을 다시 연다, 실패하면 폼을 사유와 함께 다시 그린다 */
  @PostMapping(MENU_PATH + "/action/{queryId}/run")
  public String run(
      @PathVariable String project,
      @PathVariable String mainMenu,
      @PathVariable String menu,
      @PathVariable UUID queryId,
      @RequestParam MultiValueMap<String, String> form,
      Model model) {
    var path = useService.resolve(project, mainMenu, menu);
    var menuId = path.menu().getId();
    model.addAttribute("menuPath", path.url());
    var inputs = prefixed(form, "v.");
    // @RequestParam List<String> 은 값이 하나면 쉼표로 쪼갠다 - 행 JSON 이 깨지므로 맵에서 그대로 꺼낸다
    var targets = form.get("rows") == null ? List.<String>of() : form.get("rows");
    String error;
    try {
      useService.run(menuId, queryId, inputs, targets);
      // 보던 목록을 다시 연다. HX-Redirect(새 이동)는 맨 위로 가지만 HX-Refresh(새로 고침)는 스크롤 위치를 지킨다
      // HttpServletResponse 를 인자로 받으면 요청이 처리된 것으로 보여 실패 때 폼을 못 그린다 - 그래서 여기서 꺼낸다
      ((org.springframework.web.context.request.ServletRequestAttributes)
              org.springframework.web.context.request.RequestContextHolder
                  .currentRequestAttributes())
          .getResponse()
          .setHeader("HX-Refresh", "true");
      return "menu/empty";
    } catch (IllegalArgumentException e) {
      error = e.getMessage();
    } catch (DataAccessException e) {
      var message = e.getMostSpecificCause().getMessage();
      error =
          Msg.t(
              "menu.action.failed",
              message == null ? "" : message.lines().findFirst().orElse(message));
    } catch (BlueskyException e) {
      var message = QueryErrorCode.messageOf(e);
      error = message == null ? e.getErrorCode() : message;
    }
    MenuPageService.ActionForm actionForm;
    try {
      actionForm = useService.form(menuId, queryId, targets);
    } catch (IllegalArgumentException e) {
      // 행 서명이 맞지 않으면 그 행을 다시 싣지 않는다(오류 문구가 목록을 다시 열라고 안내한다)
      actionForm = useService.form(menuId, queryId, List.of());
    }
    model.addAttribute("form", actionForm);
    model.addAttribute("error", error);
    model.addAttribute("inputs", inputs);
    return "menu/actionForm";
  }

  @GetMapping(MENU_PATH + "/excel")
  public void excel(
      @PathVariable String project,
      @PathVariable String mainMenu,
      @PathVariable String menu,
      @RequestParam MultiValueMap<String, String> query,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "false") boolean desc,
      HttpServletResponse response)
      throws IOException {
    var path = useService.resolve(project, mainMenu, menu);
    // 행은 SXSSF 가 임시 파일로 내보내고 응답 헤더 · 본문은 다 읽은 뒤에 쓴다 - 읽다가 실패하면 첨부 파일이 아닌 오류 화면이 나간다
    try (var workbook = new SXSSFWorkbook(100)) {
      var sheet = workbook.createSheet("data");
      useService.export(
          path.menu().getId(),
          searchInput(query),
          sort,
          desc,
          new DatabaseAdapter.RowSink() {
            private int next;

            @Override
            public void labels(List<String> labels) {
              writeRow(sheet.createRow(next++), List.<Object>copyOf(labels));
            }

            @Override
            public void row(List<Object> values) {
              writeRow(sheet.createRow(next++), values);
            }
          });
      response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      response.setHeader(
          "Content-Disposition",
          "attachment; filename*=UTF-8''"
              + URLEncoder.encode(
                  path.project().getCode() + "-" + path.menu().getCode() + ".xlsx",
                  StandardCharsets.UTF_8));
      workbook.write(response.getOutputStream());
      workbook.dispose();
    }
  }

  private static final int EXCEL_MAX_TEXT = SpreadsheetVersion.EXCEL2007.getMaxTextLength();

  /** 수는 엑셀 수로(15자리를 넘으면 엑셀이 끝자리를 버려 글자로), 글자는 엑셀 칸 한도까지 */
  private static void writeRow(Row row, List<Object> values) {
    for (int c = 0; c < values.size(); c++) {
      var value = values.get(c);
      var cell = row.createCell(c);
      if (value instanceof Number number && ValueConverter.fitsExcelNumber(number)) {
        cell.setCellValue(number.doubleValue());
      } else {
        var text = ValueConverter.toText(value);
        cell.setCellValue(
            text != null && text.length() > EXCEL_MAX_TEXT
                ? text.substring(0, EXCEL_MAX_TEXT)
                : text);
      }
    }
  }

  private static List<String> targetRows(List<String> rows, List<String> row) {
    if (row != null && !row.isEmpty()) {
      return row;
    }
    return rows == null ? List.of() : rows;
  }

  /** 접두어가 붙은 요청 값만 접두어를 떼서 모은다 */
  private static Map<String, String> prefixed(MultiValueMap<String, String> query, String prefix) {
    var result = new LinkedHashMap<String, String>();
    query.forEach((key, values) -> {
      if (key.startsWith(prefix) && !values.isEmpty()) {
        result.put(key.substring(prefix.length()), values.get(0));
      }
    });
    return result;
  }

  /** 검색 입력: 쿼리 파라미터 칸(s.) · 메뉴 검색칸(f.) · 통합 검색(q) */
  static MenuPageService.SearchInput searchInput(MultiValueMap<String, String> query) {
    return new MenuPageService.SearchInput(
        prefixed(query, "s."), prefixed(query, "f."), query.getFirst("q"));
  }

  /** 정렬 · 페이지 · 엑셀 링크에 다시 붙일 검색 값(비어 있는 칸은 뺀다) */
  private static String searchQuery(MultiValueMap<String, String> query) {
    var parts = new ArrayList<String>();
    query.forEach((key, values) -> {
      if ((key.startsWith("s.") || key.startsWith("f.") || key.equals("q"))
          && !values.isEmpty()
          && values.get(0) != null
          && !values.get(0).isBlank()) {
        parts.add(
            URLEncoder.encode(key, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(values.get(0), StandardCharsets.UTF_8));
      }
    });
    return parts.stream().collect(Collectors.joining("&"));
  }
}
