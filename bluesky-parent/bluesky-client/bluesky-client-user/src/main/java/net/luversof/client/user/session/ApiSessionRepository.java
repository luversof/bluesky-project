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

import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;

import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.client.user.httpexchange.UserInfoApiClient.CreateSessionRequest;
import net.luversof.client.user.httpexchange.UserInfoApiClient.DeleteSessionRequest;

public class ApiSessionRepository implements SessionRepository<ApiSession> {

  private final UserInfoApiClient userInfoApiClient;

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

  @Override
  public ApiSession findById(String id) {
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
