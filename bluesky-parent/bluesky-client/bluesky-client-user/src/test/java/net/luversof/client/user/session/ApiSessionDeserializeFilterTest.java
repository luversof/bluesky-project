package net.luversof.client.user.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;

import javax.management.BadAttributeValueExpException;

import org.junit.jupiter.api.Test;

import net.luversof.client.user.domain.LoginUser;
import net.luversof.client.user.security.LoginUserSecurityContextRepository;
import net.luversof.client.user.session.ApiSessionRemoveAttributeTest.FakeSessionServer;

/**
 * Session bytes come from bluesky-api-user, so gate must not build arbitrary classes from them
 * (2026-11-08 audit: the session endpoints were open, so anyone could plant a gadget chain).
 */
class ApiSessionDeserializeFilterTest {

  private final FakeSessionServer server = new FakeSessionServer();
  private final ApiSessionRepository repository = new ApiSessionRepository(server);

  private static String serialize(Object value) throws IOException {
    var bos = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(bos)) {
      out.writeObject(value);
    }
    return Base64.getEncoder().encodeToString(bos.toByteArray());
  }

  @Test
  void 허용하지_않은_클래스는_만들지_않는다() throws IOException {
    String id = server.createNewSession();
    server.sessions.get(id).put("keep", serialize("k"));
    // javax.management.BadAttributeValueExpException is the entry point of known gadget chains.
    server.sessions.get(id).put("gadget", serialize(new BadAttributeValueExpException("x")));

    ApiSession session = repository.findById(id);

    assertThat(session.getAttributeNames()).containsExactly("keep");
  }

  @Test
  void 로그인_사용자는_그대로_읽힌다() throws IOException {
    var loginUser =
        new LoginUser(
            "00000000-0000-0000-0000-000000000001",
            "octo",
            "github",
            null,
            null,
            List.of("ROLE_ADMIN"));
    String id = server.createNewSession();
    server
        .sessions
        .get(id)
        .put(LoginUserSecurityContextRepository.LOGIN_USER_ATTRIBUTE, serialize(loginUser));

    ApiSession session = repository.findById(id);

    LoginUser read = session.getAttribute(LoginUserSecurityContextRepository.LOGIN_USER_ATTRIBUTE);
    assertThat(read).isEqualTo(loginUser);
  }
}
