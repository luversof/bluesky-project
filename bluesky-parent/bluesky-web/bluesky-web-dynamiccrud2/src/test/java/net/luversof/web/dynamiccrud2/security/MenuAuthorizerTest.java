package net.luversof.web.dynamiccrud2.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import net.luversof.web.dynamiccrud2.setting.domain.Menu;
import net.luversof.web.dynamiccrud2.setting.domain.MenuQuery;

class MenuAuthorizerTest {

  private final MenuAuthorizer authorizer = new MenuAuthorizer();
  private final SettingAccessInterceptor interceptor = new SettingAccessInterceptor(authorizer);

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static void loginAs(String... roles) {
    var token = new TestingAuthenticationToken("user", "pw", roles);
    token.setAuthenticated(true);
    SecurityContextHolder.getContext().setAuthentication(token);
  }

  private static Menu menu(String authority) {
    var menu = new Menu();
    menu.setAuthority(authority);
    return menu;
  }

  private static MenuQuery query(String authority) {
    var query = new MenuQuery();
    query.setAuthority(authority);
    return query;
  }

  @Test
  void anonymousCannotUseEvenOpenMenu() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    assertThat(authorizer.canRead(menu(null))).isFalse();
    assertThatThrownBy(() -> interceptor.preHandle(null, null, null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void emptyMenuAuthorityMeansAnyLoggedInUser() {
    loginAs("ROLE_USER");
    assertThat(authorizer.canRead(menu(null))).isTrue();
    assertThat(authorizer.canRead(menu(" "))).isTrue();
    assertThat(authorizer.canRead(menu("ROLE_ADMIN"))).isFalse();
  }

  @Test
  void roleHierarchyLetsMasterIntoAdminMenu() {
    loginAs("ROLE_MASTER");
    assertThat(authorizer.canRead(menu("ROLE_ADMIN"))).isTrue();
    assertThat(authorizer.canRead(menu("ROLE_USER"))).isTrue();
    assertThat(authorizer.canManageSettings()).isTrue();
  }

  @Test
  void queryAuthorityFallsBackToMenuAuthority() {
    loginAs("ROLE_USER");
    assertThat(authorizer.canRun(menu(null), query(null))).isTrue();
    assertThat(authorizer.canRun(menu(null), query("ROLE_ADMIN"))).isFalse();
    assertThat(authorizer.canRun(menu("ROLE_ADMIN"), query(null))).isFalse();
    // 작업 권한이 있으면 메뉴 권한보다 우선한다
    assertThat(authorizer.canRun(menu("ROLE_ADMIN"), query("ROLE_USER"))).isTrue();
    assertThatThrownBy(() -> authorizer.checkRun(menu(null), query("ROLE_ADMIN")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void settingScreensAreAdminOnly() throws Exception {
    loginAs("ROLE_USER");
    assertThatThrownBy(() -> interceptor.preHandle(null, null, null))
        .isInstanceOf(AccessDeniedException.class);
    loginAs("ROLE_ADMIN");
    assertThat(interceptor.preHandle(null, null, null)).isTrue();
    assertThat(List.of(SettingAccessInterceptor.PATH_PATTERN)).containsExactly("/setting/**");
  }
}
