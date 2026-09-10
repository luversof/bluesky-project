package net.luversof.web.gate.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.client.user.session.ApiSessionRepository;

/**
 * The session store is remote, so every lookup is a round trip. Spring Session caches the session
 * per request, but clears that cache when the response is committed - and a response bigger than
 * the Tomcat output buffer (8KB) commits while the view is still rendering, so the lookup happened
 * twice.
 *
 * <p>Measured 2026-09-10 on this gate: fragments of 2,843 / 4,295 / 5,586 bytes made one
 * validate-session call, fragments of 12,994 / 80,260 / 148,916 bytes made two.
 */
class ApiSessionRequestReuseTest {

  private static final String SESSION_ID = "test-session-id";

  static final class CountingUserInfoApiClient implements UserInfoApiClient {
    int validateSessionCount;
    int deleteSessionCount;

    @Override
    public UserInfoResponse validateSession(String sessionId) {
      validateSessionCount++;
      return new UserInfoResponse(
          UUID.randomUUID().toString(), "tester", "github", "1", null, null, List.of(), Map.of());
    }

    @Override
    public void deleteSession(DeleteSessionRequest request) {
      deleteSessionCount++;
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

    @Override
    public void createSession(CreateSessionRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String createNewSession() {
      throw new UnsupportedOperationException();
    }
  }

  private void bindRequest() {
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  @AfterEach
  void clearRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void looksTheSessionUpOncePerRequest() {
    var client = new CountingUserInfoApiClient();
    var repository = new ApiSessionRepository(client);
    bindRequest();

    var first = repository.findById(SESSION_ID);
    var second = repository.findById(SESSION_ID);

    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(client.validateSessionCount)
        .as("the remote session store must be asked once per request, not once per lookup")
        .isEqualTo(1);
  }

  @Test
  void looksItUpAgainInTheNextRequest() {
    var client = new CountingUserInfoApiClient();
    var repository = new ApiSessionRepository(client);

    bindRequest();
    repository.findById(SESSION_ID);
    RequestContextHolder.resetRequestAttributes();

    bindRequest();
    repository.findById(SESSION_ID);

    assertThat(client.validateSessionCount)
        .as("reuse must not outlive the request - another request may have changed the session")
        .isEqualTo(2);
  }

  @Test
  void worksWithoutARequestBound() {
    var client = new CountingUserInfoApiClient();
    var repository = new ApiSessionRepository(client);

    assertThat(repository.findById(SESSION_ID)).isNotNull();
    assertThat(client.validateSessionCount).isEqualTo(1);
  }

  @Test
  void asksAgainAfterTheSessionIsDeleted() {
    var client = new CountingUserInfoApiClient();
    var repository = new ApiSessionRepository(client);
    bindRequest();

    repository.findById(SESSION_ID);
    repository.deleteById(SESSION_ID);
    repository.findById(SESSION_ID);

    assertThat(client.deleteSessionCount).isEqualTo(1);
    assertThat(client.validateSessionCount)
        .as("a lookup after a delete must not hand back the session that was just deleted")
        .isEqualTo(2);
  }
}
