package net.luversof.client.user.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import net.luversof.client.user.httpexchange.UserInfoApiClient;

/**
 * 지운 세션 속성은 다음 요청에서 사라져야 한다(2026-11-03 - 게이트에서 한 번 읽힌 플래시 오류가 만료될 때까지 매 요청 다시 나타났다. 서버는 받은 속성만 set
 * 하고, 지운 속성은 알 길이 없었다).
 */
class ApiSessionRemoveAttributeTest {

  /**
   * user API create-session 계약: removedAttributeNames 를 지우고 sessionAttributes 를 set 한다. 없는 id 는 쓰지
   * 않는다(지워진 세션을 되살리지 않도록).
   */
  static final class FakeSessionServer implements UserInfoApiClient {
    final Map<String, Map<String, Object>> sessions = new HashMap<>();
    final List<CreateSessionRequest> writes = new ArrayList<>();

    @Override
    public String createNewSession() {
      String id = UUID.randomUUID().toString();
      sessions.put(id, new HashMap<>());
      return id;
    }

    @Override
    public void createSession(CreateSessionRequest request) {
      writes.add(request);
      var stored = sessions.get(request.sessionId());
      if (stored == null) {
        return;
      }
      if (request.removedAttributeNames() != null) {
        request.removedAttributeNames().forEach(stored::remove);
      }
      stored.putAll(request.sessionAttributes());
    }

    @Override
    public UserInfoResponse validateSession(String sessionId) {
      var stored = sessions.get(sessionId);
      return stored == null
          ? null
          : new UserInfoResponse(null, null, null, null, null, null, null, new HashMap<>(stored));
    }

    @Override
    public void deleteSession(DeleteSessionRequest request) {
      sessions.remove(request.sessionId());
    }

    @Override
    public UserInfoResponse saveOAuth2User(SaveOAuth2UserRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public UserInfoResponse findByProviderAndProviderId(String provider, String providerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<UserInfoResponse> findByIdIn(List<UUID> ids) {
      throw new UnsupportedOperationException();
    }
  }

  private final FakeSessionServer server = new FakeSessionServer();
  private final ApiSessionRepository repository = new ApiSessionRepository(server);

  @AfterEach
  void unbindRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  private String sessionWith(Map<String, String> attributes) {
    ApiSession session = repository.createSession();
    attributes.forEach(session::setAttribute);
    repository.save(session);
    return session.getId();
  }

  @Test
  void 지운_속성은_다음_요청에서_사라진다() {
    String id = sessionWith(Map.of("keep", "k", "flash", "f"));

    ApiSession request1 = repository.findById(id);
    assertThat(request1.<String>getAttribute("flash")).isEqualTo("f");
    request1.removeAttribute("flash");
    repository.save(request1);

    ApiSession request2 = repository.findById(id);
    assertThat(request2.getAttributeNames()).containsExactly("keep");
    assertThat(server.writes.getLast().removedAttributeNames()).containsExactly("flash");
  }

  @Test
  void 다른_요청이_그사이_넣은_속성은_지우지_않는다() {
    String id = sessionWith(Map.of("keep", "k"));

    ApiSession slow = repository.findById(id);
    ApiSession login = repository.findById(id);
    login.setAttribute("SPRING_SECURITY_CONTEXT", "ctx");
    repository.save(login);

    slow.setAttribute("other", "o");
    repository.save(slow);

    assertThat(repository.findById(id).getAttributeNames())
        .containsExactlyInAnyOrder("keep", "SPRING_SECURITY_CONTEXT", "other");
  }

  @Test
  void 읽지_못한_속성은_지운_것으로_보지_않는다() {
    String id = sessionWith(Map.of("keep", "k"));
    server
        .sessions
        .get(id)
        .put("fromOtherBuild", Base64.getEncoder().encodeToString("not java".getBytes()));

    ApiSession session = repository.findById(id);
    assertThat(session.getAttributeNames()).containsExactly("keep");
    session.setAttribute("x", "1");
    repository.save(session);

    assertThat(server.writes.getLast().removedAttributeNames()).isEmpty();
    assertThat(server.sessions.get(id)).containsKey("fromOtherBuild");
  }

  @Test
  void 아이디를_바꾼_세션은_지울_것이_없다() {
    String id = sessionWith(Map.of("keep", "k"));

    ApiSession session = repository.findById(id);
    session.changeSessionId();
    repository.save(session);

    assertThat(server.writes.getLast().removedAttributeNames()).isEmpty();
    assertThat(server.sessions.get(session.getId())).containsOnlyKeys("keep");
  }

  /**
   * 로그인(세션 고정 보호)으로 id 를 바꾸면 로그인 전 세션(OAuth2 인가 요청 · SavedRequest)은 서버에서 지운다(2026-11-03 - TTL 4시간까지
   * 남았다). 같은 요청 안에서 옛 id 를 다시 찾아도 지운 세션을 돌려주지 않는다.
   */
  @Test
  void 아이디를_바꾸면_옛_세션은_서버에서_지운다() {
    String oldId = sessionWith(Map.of("OAUTH2_AUTHORIZATION_REQUEST", "req"));
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));

    ApiSession session = repository.findById(oldId);
    session.setAttribute("SPRING_SECURITY_CONTEXT", "ctx");
    session.changeSessionId();
    repository.save(session);

    assertThat(server.sessions).doesNotContainKey(oldId);
    assertThat(repository.findById(oldId)).isNull();
    assertThat(server.sessions.get(session.getId()))
        .containsOnlyKeys("OAUTH2_AUTHORIZATION_REQUEST", "SPRING_SECURITY_CONTEXT");
  }
}
