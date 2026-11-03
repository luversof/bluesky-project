package net.luversof.api.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.session.MapSessionRepository;
import org.springframework.test.util.ReflectionTestUtils;

import net.luversof.api.user.controller.UserInfoController.CreateSessionRequest;
import net.luversof.api.user.controller.UserInfoController.DeleteSessionRequest;

/** create-session 은 removedAttributeNames 를 지운다(2026-11-03 - 지운 플래시가 서버에 남아 매 요청 다시 나타났다). */
class UserInfoControllerSessionTest {

  private final UserInfoController controller = new UserInfoController();

  UserInfoControllerSessionTest() {
    ReflectionTestUtils.setField(
        controller, "sessionRepository", new MapSessionRepository(new ConcurrentHashMap<>()));
  }

  private Set<String> namesOf(String id) {
    return controller.validateSession(id).sessionAttributes().keySet();
  }

  @Test
  void 지운_이름만_지우고_나머지는_둔다() {
    String id = controller.createNewSession();
    controller.createSession(
        new CreateSessionRequest(id, Map.of("keep", "k", "flash", "f", "other", "o"), null));

    controller.createSession(new CreateSessionRequest(id, Map.of("keep", "k2"), Set.of("flash")));

    assertThat(namesOf(id)).containsExactlyInAnyOrder("keep", "other");
    assertThat(controller.validateSession(id).sessionAttributes()).containsEntry("keep", "k2");
  }

  @Test
  void 지운_이름이_없는_옛_요청은_예전처럼_더한다() {
    String id = controller.createNewSession();
    controller.createSession(new CreateSessionRequest(id, Map.of("a", "1"), null));
    controller.createSession(new CreateSessionRequest(id, Map.of("b", "2"), null));

    assertThat(namesOf(id)).containsExactlyInAnyOrder("a", "b");
  }

  /**
   * 로그아웃(delete-session) 뒤 같은 세션으로 진행 중이던 요청이 저장해도 지운 세션이 SPRING_SECURITY_CONTEXT 와 함께 같은 id 로
   * 되살아나면 안 된다(2026-11-03 - 찾지 못한 id 로 새 세션을 만들었다).
   */
  @Test
  void 지운_세션에_늦게_온_저장은_되살리지_않는다() {
    String id = controller.createNewSession();
    controller.createSession(new CreateSessionRequest(id, Map.of("ctx", "c"), null));
    controller.deleteSession(new DeleteSessionRequest(id));

    controller.createSession(
        new CreateSessionRequest(id, Map.of("SPRING_SECURITY_CONTEXT", "c"), Set.of()));

    assertThat(controller.validateSession(id)).isNull();
  }

  /** 새 id 는 create-new-session 이 먼저 만들어 두므로 첫 저장(새 세션 · changeSessionId 뒤)은 그대로 쓴다. */
  @Test
  void 새로_발급한_아이디의_첫_저장은_쓴다() {
    String id = controller.createNewSession();
    controller.createSession(new CreateSessionRequest(id, Map.of("a", "1"), Set.of()));

    assertThat(namesOf(id)).containsExactly("a");
  }
}
