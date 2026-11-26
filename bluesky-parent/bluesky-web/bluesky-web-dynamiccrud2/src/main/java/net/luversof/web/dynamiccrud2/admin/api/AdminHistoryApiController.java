package net.luversof.web.dynamiccrud2.admin.api;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.Affected;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.ApiError;
import net.luversof.web.dynamiccrud2.admin.service.ChangeLogService;
import net.luversof.web.dynamiccrud2.admin.service.FieldService;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.admin.service.SettingLogService;
import net.luversof.web.dynamiccrud2.menu.service.MenuPageService;

/**
 * 관리 JSON 호출 - 변경 이력. 설정 변경(관리 화면에서 바꾼 것)과 데이터 변경(사용 화면 실행)을 화면과 같은 거르기 · 쪽(20건)으로 보고, 데이터 변경을
 * 되돌린다.
 */
@RestController
@RequestMapping("/admin/api")
@Tag(name = "관리 - 변경 이력", description = "설정 변경 이력(전체 · 프로젝트 · 메뉴 · 테이블 필드) · 데이터 변경 이력 · 되돌리기")
@ApiResponse(
    responseCode = "400",
    description = "되돌릴 수 없음(입력 · 이미 되돌림 · 그 뒤 다시 바뀐 행 등), DB 거부",
    content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(responseCode = "401", description = "로그인 없음")
@ApiResponse(responseCode = "403", description = "설정 관리 권한 · 작업 실행 권한 없음")
@ApiResponse(
    responseCode = "404",
    description = "없는 id",
    content = @Content(schema = @Schema(implementation = ApiError.class)))
public class AdminHistoryApiController {

  private final MenuService menuService;
  private final FieldService fieldService;
  private final SettingLogService settingLogService;
  private final ChangeLogService changeLogService;
  private final MenuPageService useService;

  public AdminHistoryApiController(
      MenuService menuService,
      FieldService fieldService,
      SettingLogService settingLogService,
      ChangeLogService changeLogService,
      MenuPageService useService) {
    this.menuService = menuService;
    this.fieldService = fieldService;
    this.settingLogService = settingLogService;
    this.changeLogService = changeLogService;
    this.useService = useService;
  }

  @Operation(
      summary = "전체 설정 변경 이력",
      description = "모든 프로젝트 + 어느 프로젝트에도 속하지 않는 변경(프로젝트 순서 · 테이블 필드 등)")
  @GetMapping("/history")
  public SettingLogService.HistoryPage allHistory(
      @Parameter(description = "사용자") @RequestParam(required = false) String user,
      @Parameter(description = "대상(PROJECT · MAIN_MENU · MENU · QUERY · SEARCH · FIELD …)")
          @RequestParam(required = false)
          String target,
      @Parameter(description = "시작일(그날 포함)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "종료일(그날 포함)")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @Parameter(description = "쪽(0 부터)") @RequestParam(defaultValue = "0") int page) {
    return settingLogService.page(
        null, null, new SettingLogService.Filter(user, target, from, to), page);
  }

  @Operation(
      summary = "프로젝트 설정 변경 이력",
      description = "프로젝트 · 메인 메뉴 · 그 아래 메뉴(지운 메뉴 포함) · 쿼리 · 검색 설정")
  @GetMapping("/projects/{id}/history")
  public SettingLogService.HistoryPage projectHistory(
      @PathVariable UUID id,
      @RequestParam(required = false) String user,
      @RequestParam(required = false) String target,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page) {
    menuService.getProject(id);
    return settingLogService.page(
        null, id, new SettingLogService.Filter(user, target, from, to), page);
  }

  @Operation(summary = "메뉴 설정 변경 이력", description = "메뉴 · 쿼리 · 검색 설정 + 그 메뉴 테이블의 필드 변경")
  @GetMapping("/menus/{id}/history/settings")
  public SettingLogService.HistoryPage menuSettingHistory(
      @PathVariable UUID id,
      @RequestParam(required = false) String user,
      @RequestParam(required = false) String target,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page) {
    return settingLogService.page(
        menuService.getMenu(id), null, new SettingLogService.Filter(user, target, from, to), page);
  }

  @Operation(
      summary = "메뉴 데이터 변경 이력",
      description = "사용 화면에서 실행한 입력 · 수정 · 삭제(전후 값). 줄의 id 로 되돌린다(revertible = 되돌릴 수 있음)")
  @GetMapping("/menus/{id}/history/data")
  public ChangeLogService.HistoryPage menuDataHistory(
      @PathVariable UUID id,
      @RequestParam(required = false) String user,
      @Parameter(description = "작업(쿼리) id") @RequestParam(required = false) UUID query,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page) {
    return changeLogService.page(
        menuService.getMenu(id), new ChangeLogService.Filter(user, query, from, to), page);
  }

  @Operation(summary = "테이블 필드 변경 이력", description = "필드 표시 설정 변경 · 컬럼 다시 읽기로 바뀐 컬럼")
  @GetMapping("/tables/{id}/history")
  public SettingLogService.HistoryPage tableHistory(
      @PathVariable UUID id,
      @RequestParam(required = false) String user,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page) {
    fieldService.get(id);
    return settingLogService.tablePage(
        id, new SettingLogService.Filter(user, null, from, to), page);
  }

  @Operation(
      summary = "데이터 변경 되돌리기",
      description =
          "수정 = 같은 수정 작업을 바뀌기 전 값으로(그 뒤 행이 다시 바뀌었으면 거부), 삭제 = 지운 행을 다시 넣기(같은 키 행이 있으면 거부)."
              + " 입력 · 되돌리기 이력은 되돌리지 않고, 한 이력은 한 번만. 그 작업 실행 권한이 필요하다")
  @PostMapping("/menus/{id}/history/{logId}/revert")
  public Affected revert(@PathVariable UUID id, @PathVariable UUID logId) {
    menuService.getMenu(id);
    return new Affected(useService.revert(id, logId));
  }
}
