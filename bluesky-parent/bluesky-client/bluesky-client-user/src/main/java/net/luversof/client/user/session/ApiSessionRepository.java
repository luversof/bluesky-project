package net.luversof.client.user.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;
import org.springframework.web.context.request.RequestContextHolder;

import io.github.luversof.boot.web.util.RequestAttributeUtil;
import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.client.user.httpexchange.UserInfoApiClient.CreateSessionRequest;
import net.luversof.client.user.httpexchange.UserInfoApiClient.DeleteSessionRequest;

public class ApiSessionRepository implements SessionRepository<ApiSession> {

  private final UserInfoApiClient userInfoApiClient;

  private static final String SESSION_BY_ID = "__apiSession_by_id_";

  public ApiSessionRepository(UserInfoApiClient userInfoApiClient) {
    this.userInfoApiClient = userInfoApiClient;
  }

  @Override
  public ApiSession createSession() {
    String sessionId = userInfoApiClient.createNewSession();
    return new ApiSession(sessionId, userInfoApiClient);
  }

  @Override
  public void save(ApiSession session) {
    Map<String, Object> attributes = new HashMap<>();
    session
        .getAttributeNames()
        .forEach(
            name -> {
              Object value = session.getAttribute(name);
              Object serialized = serialize(value);
              if (value != null && serialized == null) {
                System.err.println(
                    "ApiSessionRepository.save serialization failed. key: "
                        + name
                        + ", value: "
                        + value);
              }
              attributes.put(name, serialized);
            });

    // 내용이 그대로면 서버에 이미 같은 값이 있다. 만료 갱신은 validate-session 쪽에서 이미 하므로
    // 여기서 굳이 왕복을 한 번 더 쓸 이유가 없다(자세한 근거는 ApiSession#savedAttributes 주석).
    if (attributes.equals(session.getSavedAttributes())) {
      return;
    }

    userInfoApiClient.createSession(new CreateSessionRequest(session.getId(), attributes));
    session.setSavedAttributes(attributes);
  }

  /**
   * Spring Session looks a session up once per request and caches it, but that cache is cleared
   * when the response is committed. A response larger than the Tomcat output buffer (8KB) commits
   * while the view is still rendering, so the filter chain looked the session up again afterwards
   * and spent a second remote validate-session round trip.
   *
   * <p>Measured 2026-09-10 on bluesky-web-gate: fragments of 2,843 / 4,295 / 5,586 bytes made one
   * call, fragments of 12,994 / 80,260 / 148,916 bytes made two. Reusing the lookup for the rest of
   * the request is the same request-scoped reuse the rest of the project uses.
   */
  @Override
  public ApiSession findById(String id) {
    if (RequestContextHolder.getRequestAttributes() == null) {
      return loadById(id);
    }
    return RequestAttributeUtil.getObject(SESSION_BY_ID + id, () -> loadById(id));
  }

  private ApiSession loadById(String id) {
    try {
      var userInfo = userInfoApiClient.validateSession(id);
      if (userInfo == null) {
        System.err.println("ApiSessionRepository.findById failed. userInfo is null. id: " + id);
        return null;
      }

      MapSession session = new MapSession(id);
      if (userInfo.sessionAttributes() != null) {
        userInfo
            .sessionAttributes()
            .forEach(
                (k, v) -> {
                  session.setAttribute(k, deserialize(v));
                });
      }

      session.setLastAccessedTime(Instant.now());
      session.setMaxInactiveInterval(Duration.ofMinutes(30));

      ApiSession apiSession = new ApiSession(session, userInfoApiClient);
      // 방금 서버에서 받은 '직렬화된' 상태 그대로를 기준으로 삼는다.
      apiSession.setSavedAttributes(
          userInfo.sessionAttributes() == null
              ? new HashMap<>()
              : new HashMap<>(userInfo.sessionAttributes()));
      return apiSession;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public void deleteById(String id) {
    // Drop the request-scoped reuse so a lookup later in the same request (logout, session
    // invalidation) asks the server instead of handing back the session that was just deleted.
    if (RequestContextHolder.getRequestAttributes() != null) {
      RequestAttributeUtil.setRequestAttribute(SESSION_BY_ID + id, Optional.empty());
    }
    userInfoApiClient.deleteSession(new DeleteSessionRequest(id));
  }

  private Object serialize(Object object) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(object);
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    } catch (IOException e) {
      System.err.println("Failed to serialize object: " + object.getClass().getName());
      e.printStackTrace();
      return null;
    }
  }

  private Object deserialize(Object object) {
    if (object instanceof String str) {
      try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(str));
          ObjectInputStream in = new ObjectInputStream(bis)) {
        return in.readObject();
      } catch (IOException | ClassNotFoundException e) {
        System.err.println("Failed to deserialize object: " + object);
        e.printStackTrace();
        return null;
      }
    }
    return object;
  }
}
