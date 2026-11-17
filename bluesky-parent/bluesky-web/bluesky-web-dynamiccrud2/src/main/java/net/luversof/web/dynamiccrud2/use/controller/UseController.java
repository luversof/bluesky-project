package net.luversof.web.dynamiccrud2.use.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.security.MenuAuthorizer;
import net.luversof.web.dynamiccrud2.use.service.UseService;
import net.luversof.web.dynamiccrud2.use.service.ValueConverter;

/** 사용 화면. 검색 값은 s.{파라미터}, 작업 입력은 v.{파라미터}, 작업 대상 행은 rows(체크박스) · row(행 버튼)로 받는다(행 값 JSON). */
@Controller
@RequestMapping("/use")
public class UseController {

  private final UseService useService;
  private final MenuAuthorizer authorizer;

  public UseController(UseService useService, MenuAuthorizer authorizer) {
    this.useService = useService;
    this.authorizer = authorizer;
  }

  @GetMapping
  public String index(Model model) {
    authorizer.checkLoggedIn();
    model.addAttribute("tree", useService.visibleTree());
    return "use/index";
  }

  @GetMapping("/menu/{menuId}")
  public String menu(
      @PathVariable UUID menuId,
      @RequestParam MultiValueMap<String, String> query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "false") boolean desc,
      Model model) {
    var search = prefixed(query, "s.");
    var usePage = useService.page(menuId, search, page, sort, desc);
    model.addAttribute("usePage", usePage);
    model.addAttribute("searchQuery", searchQuery(search));
    return "use/menu";
  }

  /** 작업 폼(htmx 조각). 행은 체크박스 rows 또는 행 버튼 row(행 JSON) */
  @PostMapping("/menu/{menuId}/action/{queryId}/form")
  public String form(
      @PathVariable UUID menuId,
      @PathVariable UUID queryId,
      @RequestParam MultiValueMap<String, String> form,
      Model model) {
    model.addAttribute(
        "form", useService.form(menuId, queryId, targetRows(form.get("rows"), form.get("row"))));
    model.addAttribute("error", null);
    model.addAttribute("inputs", Map.of());
    return "use/actionForm";
  }

  /** 작업 실행(htmx). 성공하면 보던 목록을 다시 연다, 실패하면 폼을 사유와 함께 다시 그린다 */
  @PostMapping("/menu/{menuId}/action/{queryId}/run")
  public String run(
      @PathVariable UUID menuId,
      @PathVariable UUID queryId,
      @RequestParam MultiValueMap<String, String> form,
      HttpServletRequest request,
      Model model) {
    var inputs = prefixed(form, "v.");
    // @RequestParam List<String> 은 값이 하나면 쉼표로 쪼갠다 - 행 JSON 이 깨지므로 맵에서 그대로 꺼낸다
    var targets = form.get("rows") == null ? List.<String>of() : form.get("rows");
    String error;
    try {
      useService.run(menuId, queryId, inputs, targets);
      var current = request.getHeader("HX-Current-URL");
      // HttpServletResponse 를 인자로 받으면 요청이 처리된 것으로 보여 실패 때 폼을 못 그린다 - 그래서 여기서 꺼낸다
      ((org.springframework.web.context.request.ServletRequestAttributes)
              org.springframework.web.context.request.RequestContextHolder
                  .currentRequestAttributes())
          .getResponse()
          .setHeader("HX-Redirect", current == null ? "/use/menu/" + menuId : current);
      return "use/empty";
    } catch (IllegalArgumentException e) {
      error = e.getMessage();
    } catch (DataAccessException e) {
      var message = e.getMostSpecificCause().getMessage();
      error = "실행 실패: " + (message == null ? "" : message.lines().findFirst().orElse(message));
    } catch (BlueskyException e) {
      var message = QueryErrorCode.messageOf(e);
      error = message == null ? e.getErrorCode() : message;
    }
    model.addAttribute("form", useService.form(menuId, queryId, targets));
    model.addAttribute("error", error);
    model.addAttribute("inputs", inputs);
    return "use/actionForm";
  }

  @GetMapping("/menu/{menuId}/excel")
  public void excel(
      @PathVariable UUID menuId,
      @RequestParam MultiValueMap<String, String> query,
      @RequestParam(required = false) String sort,
      @RequestParam(defaultValue = "false") boolean desc,
      HttpServletResponse response)
      throws IOException {
    var export = useService.export(menuId, prefixed(query, "s."), sort, desc);
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader(
        "Content-Disposition",
        "attachment; filename*=UTF-8''"
            + URLEncoder.encode("export-" + menuId + ".xlsx", StandardCharsets.UTF_8));
    try (var workbook = new SXSSFWorkbook(100)) {
      var sheet = workbook.createSheet("data");
      var header = sheet.createRow(0);
      for (int i = 0; i < export.headers().size(); i++) {
        header.createCell(i).setCellValue(export.headers().get(i));
      }
      for (int r = 0; r < export.rows().size(); r++) {
        var row = sheet.createRow(r + 1);
        var values = export.rows().get(r);
        for (int c = 0; c < values.size(); c++) {
          var value = values.get(c);
          var cell = row.createCell(c);
          if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
          } else {
            cell.setCellValue(ValueConverter.toText(value));
          }
        }
      }
      workbook.write(response.getOutputStream());
      workbook.dispose();
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

  /** 정렬 · 페이지 링크에 다시 붙일 검색 값 */
  private static String searchQuery(Map<String, String> search) {
    var parts = new ArrayList<String>();
    search.forEach((k, v) ->
        parts.add(
            URLEncoder.encode("s." + k, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(v, StandardCharsets.UTF_8)));
    return parts.stream().collect(Collectors.joining("&"));
  }
}
