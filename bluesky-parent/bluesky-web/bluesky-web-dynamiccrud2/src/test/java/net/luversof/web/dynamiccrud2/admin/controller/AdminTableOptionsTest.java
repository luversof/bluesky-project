package net.luversof.web.dynamiccrud2.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapter;
import net.luversof.web.dynamiccrud2.adapter.DatabaseAdapterResolver;
import net.luversof.web.dynamiccrud2.admin.service.MenuService;
import net.luversof.web.dynamiccrud2.domain.MainMenu;

/** 메뉴 추가: 대상 연결이 안 되면 오류 화면(500) 대신 안내 한 줄(DMT2 UDP-6560 과 같게) */
class AdminTableOptionsTest {

  private final DatabaseAdapterResolver resolver = mock(DatabaseAdapterResolver.class);
  private final MenuService menuService = mock(MenuService.class);
  private final AdminController controller =
      new AdminController(menuService, null, null, null, resolver, null, null, null, null);

  /** 테이블 목록: 목록 자리에 안내, 예외 이름 · 원문은 보이지 않는다 */
  @Test
  void unreachableConnectionShowsGuideInTableList() {
    var adapter = mock(DatabaseAdapter.class);
    when(resolver.resolve("SomeDb")).thenReturn(adapter);
    when(adapter.listTables())
        .thenThrow(
            new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection: refused"));
    var model = new ExtendedModelMap();
    assertThat(controller.tableOptions(" SomeDb ", model)).isEqualTo("admin/tableOptions");
    assertThat((String) model.get("error"))
        .contains("SomeDb")
        .doesNotContain("Exception")
        .doesNotContain("refused");
    assertThat((java.util.List<?>) model.get("tables")).isEmpty();
    // 관리 JSON 호출도 같은 안내(400 - AdminApiAdvice 가 IllegalArgumentException 을 {"error"} 로)
    var api =
        new net.luversof.web.dynamiccrud2.admin.api.AdminStructureApiController(
            menuService, null, resolver);
    assertThatThrownBy(() -> api.tables("SomeDb"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SomeDb")
        .hasMessageNotContaining("refused");
  }

  /** 메뉴 저장(테이블 컬럼 읽기): 프로젝트 관리 화면으로 돌아가 안내. 설정 DB 오류는 연결 실패로 바꾸지 않는다 */
  @Test
  void createMenuConnectionFailureShowsGuide() {
    var mainMenuId = UUID.randomUUID();
    var mainMenu = new MainMenu();
    mainMenu.setProjectId(UUID.randomUUID());
    when(menuService.getMainMenu(mainMenuId)).thenReturn(mainMenu);
    when(menuService.createMenu(any(), any(), any(), eq("SomeDb"), any()))
        .thenThrow(new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection"));
    var redirect = new RedirectAttributesModelMap();
    assertThat(controller.createMenu(mainMenuId, "c", "n", "SomeDb", "||t", redirect))
        .isEqualTo("redirect:/admin?project=" + mainMenu.getProjectId());
    assertThat((String) redirect.getFlashAttributes().get("error"))
        .contains("SomeDb")
        .doesNotContain("Exception");

    when(menuService.createMenu(any(), any(), any(), eq("Other"), any()))
        .thenThrow(new DataIntegrityViolationException("settings db"));
    assertThatThrownBy(() ->
            controller.createMenu(
                mainMenuId, "c", "n", "Other", "||t", new RedirectAttributesModelMap()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
