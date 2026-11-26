package net.luversof.web.dynamiccrud2.admin.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.ApiError;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.FieldUpdate;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.MenuView;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.TableView;
import net.luversof.web.dynamiccrud2.admin.service.FieldService;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.admin.service.QueryService;
import net.luversof.web.dynamiccrud2.admin.service.SearchService;
import net.luversof.web.dynamiccrud2.domain.ParamSetting;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/** 관리 JSON 호출 - 메뉴의 작업(쿼리) · 검색 설정 · 테이블 필드. 관리 화면과 같은 서비스 · 검사 · 설정 변경 이력 */
@RestController
@RequestMapping("/admin/api")
@Tag(name = "관리 - 쿼리 · 검색 · 필드", description = "작업(조회 · 입력 · 수정 · 삭제 쿼리) · 검사 · 실행 계획, 검색 설정, 테이블 필드 표시 설정")
@ApiResponse(
    responseCode = "400",
    description = "쿼리 검사 거부(문법 · 종류 · WHERE 없는 수정 등), 입력 형식",
    content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(responseCode = "401", description = "로그인 없음")
@ApiResponse(responseCode = "403", description = "설정 관리 권한 없음")
@ApiResponse(
    responseCode = "404",
    description = "없는 id",
    content = @Content(schema = @Schema(implementation = ApiError.class)))
public class AdminQueryApiController {

  private final MenuService menuService;
  private final QueryService queryService;
  private final SearchService searchService;
  private final FieldService fieldService;

  public AdminQueryApiController(
      MenuService menuService,
      QueryService queryService,
      SearchService searchService,
      FieldService fieldService) {
    this.menuService = menuService;
    this.queryService = queryService;
    this.searchService = searchService;
    this.fieldService = fieldService;
  }

  @Schema(
      description =
          "작업(쿼리) 저장 · 검사 본문. params = 파라미터 이름 → {role: SEARCH|INPUT|ROW_KEY, required, defaultValue}(비우면 쿼리를 보고"
              + " 추정 - 검사 응답 params 를 고쳐 넣으면 된다). authority = 이 작업만의 권한(비우면 메뉴 권한)")
  public record QueryInput(
      QueryKind kind,
      String name,
      String queryText,
      String authority,
      Map<String, ParamSetting> params) {}

  @Schema(
      description =
          "검색칸 하나. operator = EQUALS|CONTAINS|STARTS_WITH|RANGE …(비우면 그 컬럼 기본), pick = INPUT(직접 입력)|LIST(필드 값 목록)|DISTINCT(DB 값 전체)|CUSTOM(values 목록),"
              + " values = 검색용 값 목록(줄마다 저장값=표시), required = 꼭 입력해야 목록을 보인다")
  public record SearchField(
      net.luversof.web.dynamiccrud2.domain.SearchOperator operator,
      net.luversof.web.dynamiccrud2.domain.SearchSettings.Pick pick,
      String values,
      Boolean required) {}

  @Schema(description = "검색 설정 저장 본문. fields = 결과 이름 → 검색칸(빠진 결과는 검색칸에서 뺀다, 넣은 순서가 화면 순서), keyword = 통합 검색에 넣을 결과 이름")
  public record SearchInput(Map<String, SearchField> fields, List<String> keyword) {}

  @Schema(description = "검색 설정 후보. parsed = 조회 쿼리를 해석했는지(못 했으면 후보 없음)")
  public record SearchView(
      boolean parsed,
      List<SearchService.Candidate> candidates,
      List<SearchService.Candidate> keywordCandidates) {}

  // ---- 작업(쿼리) ----

  @Operation(summary = "기본 쿼리 초안", description = "테이블 정보로 만든 입력 · 수정 · 삭제(조회) 쿼리. 키가 없으면 error")
  @GetMapping("/menus/{menuId}/queries/draft")
  public QueryService.Draft draft(@PathVariable UUID menuId, @RequestParam QueryKind kind) {
    return queryService.draft(menuService.getMenu(menuId), kind);
  }

  @Operation(
      summary = "쿼리 검사",
      description = "저장하지 않고 검사 - 조회는 앞 몇 행 미리 보기, 입력 · 수정 · 삭제는 실행하지 않고 DB 에서 검사. error 가 있으면 저장되지 않는다")
  @PostMapping("/menus/{menuId}/queries/inspect")
  public QueryService.QueryInspection inspect(@PathVariable UUID menuId, @RequestBody QueryInput body) {
    return queryService.inspect(menuService.getMenu(menuId), body.kind(), body.queryText(), body.params());
  }

  @Operation(summary = "실행 계획", description = "실행하지 않고 DB 계획(PostgreSQL 은 값과 무관한 일반 계획)")
  @PostMapping("/menus/{menuId}/queries/explain")
  public QueryService.ExplainResult explain(@PathVariable UUID menuId, @RequestBody QueryInput body) {
    return queryService.explain(menuService.getMenu(menuId), body.kind(), body.queryText());
  }

  @Operation(summary = "작업 추가", description = "입력 · 수정 · 삭제 작업(조회는 메뉴마다 하나 - 바꾸기로)")
  @PostMapping("/menus/{menuId}/queries")
  @ResponseStatus(HttpStatus.CREATED)
  public MenuView addQuery(@PathVariable UUID menuId, @RequestBody QueryInput body) {
    menuService.getMenu(menuId);
    return MenuView.of(
        queryService.save(
            menuId, null, body.kind(), body.name(), body.queryText(), body.params(), body.authority()));
  }

  @Operation(summary = "작업 바꾸기", description = "쿼리 · 이름 · 권한 · 파라미터 설정")
  @PutMapping("/menus/{menuId}/queries/{queryId}")
  public MenuView updateQuery(
      @PathVariable UUID menuId, @PathVariable UUID queryId, @RequestBody QueryInput body) {
    var kind =
        menuService.getMenu(menuId).getQueries().stream()
            .filter(q -> q.getId().equals(queryId))
            .findFirst()
            .orElseThrow()
            .getKind();
    return MenuView.of(
        queryService.save(
            menuId, queryId, kind, body.name(), body.queryText(), body.params(), body.authority()));
  }

  @Operation(summary = "작업 지우기", description = "조회 작업은 지우지 않는다")
  @DeleteMapping("/menus/{menuId}/queries/{queryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteQuery(@PathVariable UUID menuId, @PathVariable UUID queryId) {
    queryService.delete(menuId, queryId);
  }

  // ---- 검색 설정 ----

  @Operation(summary = "검색 설정 후보", description = "조회 결과 컬럼마다 쓸 수 있는 검색 방식 · 지금 설정 · 인덱스 사용")
  @GetMapping("/menus/{id}/search")
  public SearchView search(@PathVariable UUID id) {
    var setup = searchService.setup(menuService.getMenu(id));
    return new SearchView(setup.parsed(), setup.candidates(), setup.keywordCandidates());
  }

  @Operation(summary = "검색 설정 저장", description = "모르는 방식 · 고르기는 그 컬럼 기본으로 바꾼다")
  @PutMapping("/menus/{id}/search")
  public MenuView saveSearch(@PathVariable UUID id, @RequestBody SearchInput body) {
    var fields = new java.util.LinkedHashMap<String, SearchService.FieldInput>();
    if (body.fields() != null) {
      body.fields()
          .forEach(
              (label, field) ->
                  fields.put(
                      label,
                      new SearchService.FieldInput(
                          field == null ? null : field.operator(),
                          field == null ? null : field.pick(),
                          field == null ? null : field.values(),
                          field != null && Boolean.TRUE.equals(field.required()))));
    }
    searchService.saveFields(id, fields, body.keyword() == null ? List.of() : body.keyword());
    return MenuView.of(menuService.getMenu(id));
  }

  // ---- 테이블 필드 ----

  @Operation(summary = "테이블 필드", description = "메뉴의 dbTableId. 같은 테이블을 쓰는 메뉴가 같이 쓴다")
  @GetMapping("/tables/{id}")
  public TableView table(@PathVariable UUID id) {
    return TableView.of(fieldService.get(id));
  }

  @Operation(summary = "필드 표시 설정 바꾸기", description = "라벨 · 입력 형식 · 최대 길이 · 정렬 허용 · 수정 가능 · 값 목록 · 날짜 형식")
  @PutMapping("/tables/{id}/fields/{columnName}")
  public TableView updateField(
      @PathVariable UUID id, @PathVariable String columnName, @RequestBody FieldUpdate body) {
    return TableView.of(
        fieldService.updateField(
            id,
            columnName,
            body.label(),
            body.inputType(),
            body.maxLength(),
            body.sortable(),
            body.updatable(),
            body.valueOptions(),
            body.dateFormat()));
  }

  @Operation(summary = "컬럼 다시 읽기", description = "DB 의 컬럼 · 타입을 다시 읽는다(사용자가 고친 값은 유지, 바뀐 컬럼은 이력에)")
  @PostMapping("/tables/{id}/resync")
  public TableView resync(@PathVariable UUID id) {
    return TableView.of(fieldService.resync(id));
  }
}
