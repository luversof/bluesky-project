package net.luversof.web.dynamiccrud2.admin.api;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import net.luversof.web.dynamiccrud2.domain.DbField;
import net.luversof.web.dynamiccrud2.domain.DbTable;
import net.luversof.web.dynamiccrud2.domain.InputType;
import net.luversof.web.dynamiccrud2.domain.MainMenu;
import net.luversof.web.dynamiccrud2.domain.Menu;
import net.luversof.web.dynamiccrud2.domain.MenuQuery;
import net.luversof.web.dynamiccrud2.domain.Project;
import net.luversof.web.dynamiccrud2.domain.QueryKind;

/** 관리 JSON 호출(/admin/api/**)의 요청 · 응답 모양. 엔티티를 그대로 내보내지 않는다(저장 구조가 바뀌어도 호출 모양은 그대로) */
public final class AdminApiViews {

  private AdminApiViews() {}

  @Schema(description = "오류 사유(화면에 보이는 문구와 같다)")
  public record ApiError(String error) {}

  @Schema(description = "주소 이름(code: 영문 소문자 · 숫자 · -) · 이름")
  public record CodeName(String code, String name) {}

  @Schema(description = "사용 화면 메뉴 목록에 보일지")
  public record Display(boolean display) {}

  @Schema(description = "새 순서(id 목록). 메뉴 순서에 다른 메인 메뉴의 메뉴 id 를 넣으면 이 메인 메뉴로 옮긴다(같은 프로젝트만)")
  public record Order(List<UUID> ids) {}

  @Schema(description = "영향 행 수")
  public record Affected(int affected) {}

  public record ProjectView(
      UUID id,
      String code,
      String name,
      boolean enableDisplay,
      int displayOrder,
      Instant createdDate) {
    static ProjectView of(Project p) {
      return new ProjectView(
          p.getId(),
          p.getCode(),
          p.getName(),
          p.isEnableDisplay(),
          p.getDisplayOrder(),
          p.getCreatedDate());
    }
  }

  public record MainMenuView(
      UUID id, UUID projectId, String code, String name, boolean enableDisplay, int displayOrder) {
    static MainMenuView of(MainMenu m) {
      return new MainMenuView(
          m.getId(),
          m.getProjectId(),
          m.getCode(),
          m.getName(),
          m.isEnableDisplay(),
          m.getDisplayOrder());
    }
  }

  @Schema(description = "메뉴 요약(구조 트리)")
  public record MenuSummary(
      UUID id,
      String code,
      String name,
      String authority,
      boolean enableDisplay,
      int displayOrder) {
    static MenuSummary of(Menu m) {
      return new MenuSummary(
          m.getId(),
          m.getCode(),
          m.getName(),
          m.getAuthority(),
          m.isEnableDisplay(),
          m.getDisplayOrder());
    }
  }

  @Schema(description = "프로젝트 → 메인 메뉴 → 메뉴 트리(순서대로)")
  public record ProjectTree(ProjectView project, List<MainMenuTree> mainMenus) {}

  public record MainMenuTree(MainMenuView mainMenu, List<MenuSummary> menus) {}

  @Schema(description = "작업(쿼리). paramSettings = 파라미터 이름 → {role, required, defaultValue}")
  public record QueryView(
      UUID id,
      QueryKind kind,
      String name,
      String queryText,
      String authority,
      Map<String, Object> paramSettings,
      int displayOrder) {
    static QueryView of(MenuQuery q) {
      return new QueryView(
          q.getId(),
          q.getKind(),
          q.getName(),
          q.getQueryText(),
          q.getAuthority(),
          q.getParamSettings(),
          q.getDisplayOrder());
    }
  }

  @Schema(description = "메뉴 설정 전체(작업 · 검색 설정 포함). searchSettings 는 저장된 JSON 그대로")
  public record MenuView(
      UUID id,
      UUID mainMenuId,
      UUID dbTableId,
      String code,
      String name,
      String authority,
      int pageSize,
      boolean enableExcel,
      boolean enableDisplay,
      int displayOrder,
      Map<String, Object> searchSettings,
      List<QueryView> queries) {
    static MenuView of(Menu m) {
      return new MenuView(
          m.getId(),
          m.getMainMenuId(),
          m.getDbTableId(),
          m.getCode(),
          m.getName(),
          m.getAuthority(),
          m.getPageSize(),
          m.isEnableExcel(),
          m.isEnableDisplay(),
          m.getDisplayOrder(),
          m.getSearchSettings(),
          m.getQueries().stream()
              .sorted(
                  Comparator.comparing(MenuQuery::getKind)
                      .thenComparingInt(MenuQuery::getDisplayOrder))
              .map(QueryView::of)
              .toList());
    }
  }

  @Schema(description = "메뉴 설정 바꾸기")
  public record MenuUpdate(
      String code,
      String name,
      String authority,
      int pageSize,
      boolean enableExcel,
      boolean enableDisplay) {}

  @Schema(
      description =
          "메뉴 만들기: 연결 이름(connection) · 테이블(catalog · schema · table - 테이블 목록 응답 그대로)을 고르면 필드를 등록하고 조회 쿼리를"
              + " 만든다")
  public record MenuCreate(
      String code,
      String name,
      String connectionName,
      String catalog,
      String schema,
      String table) {}

  @Schema(description = "메뉴 복사. code · name 을 비우면 \"이름 복사본\" · code-copy(겹치면 -copy2 …)")
  public record MenuCopy(String code, String name) {}

  @Schema(description = "필드(테이블 단위 - 같은 테이블을 쓰는 메뉴가 같이 쓴다)")
  public record FieldView(
      String columnName,
      String label,
      InputType inputType,
      Integer maxLength,
      boolean sortable,
      boolean updatable,
      String valueOptions,
      String dateFormat,
      String dataType,
      boolean required,
      boolean autoIncrement,
      boolean virtual,
      boolean missingInDb,
      String dbComment) {
    static FieldView of(DbField f) {
      return new FieldView(
          f.getColumnName(),
          f.getLabel(),
          f.getInputType(),
          f.getMaxLength(),
          f.isSortable(),
          f.isUpdatable(),
          f.getValueOptions(),
          f.getDateFormat(),
          f.getDataType(),
          f.isRequired(),
          f.isAutoIncrement(),
          f.isVirtual(),
          f.isMissingInDb(),
          f.getDbComment());
    }
  }

  public record TableView(
      UUID id,
      String connectionName,
      String catalogName,
      String schemaName,
      String tableName,
      Instant columnsReadDate,
      List<FieldView> fields) {
    static TableView of(DbTable t) {
      return new TableView(
          t.getId(),
          t.getConnectionName(),
          t.getCatalogName(),
          t.getSchemaName(),
          t.getTableName(),
          t.getColumnsReadDate(),
          t.getFields().stream()
              .sorted(
                  Comparator.comparing(DbField::isVirtual)
                      .thenComparingInt(DbField::getDisplayOrder))
              .map(FieldView::of)
              .toList());
    }
  }

  @Schema(description = "필드 표시 설정 바꾸기(테이블 필드 관리 화면과 같은 항목). valueOptions 는 줄마다 \"저장값=표시\"")
  public record FieldUpdate(
      String label,
      InputType inputType,
      Integer maxLength,
      boolean sortable,
      boolean updatable,
      String valueOptions,
      String dateFormat) {}
}
