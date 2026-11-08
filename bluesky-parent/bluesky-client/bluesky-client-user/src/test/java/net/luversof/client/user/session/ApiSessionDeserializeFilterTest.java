package net.luversof.client.user.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.management.BadAttributeValueExpException;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

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
  void 로그인_보안_컨텍스트는_그대로_읽힌다() throws IOException {
    var authority = new OAuth2UserAuthority(Map.of("id", 1, "login", "octo"));
    var user = new DefaultOAuth2User(List.of(authority), Map.of("id", 1, "login", "octo"), "login");
    SecurityContext context =
        new SecurityContextImpl(new OAuth2AuthenticationToken(user, List.of(authority), "github"));
    String id = server.createNewSession();
    server.sessions.get(id).put("SPRING_SECURITY_CONTEXT", serialize(context));

    ApiSession session = repository.findById(id);

    SecurityContext read = session.getAttribute("SPRING_SECURITY_CONTEXT");
    assertThat(read.getAuthentication().getName()).isEqualTo("octo");
  }
}
