package net.luversof.client.user.session;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.springframework.session.MapSession;
import org.springframework.session.Session;

import net.luversof.client.user.httpexchange.UserInfoApiClient;

public class ApiSession implements Session, Serializable {

  private static final long serialVersionUID = 1L;

  private final MapSession delegate;
  private final UserInfoApiClient userInfoApiClient;

  /**
   * 마지막으로 서버와 일치한다고 아는 '직렬화된' 속성 맵.
   *
   * <p>Spring Session 의 {@code SessionRepositoryFilter} 는 요청이 세션을 건드리기만 하면 커밋 시점에 저장소의 {@code save}
   * 를 무조건 부른다. 여기 구현은 그때마다 user API 로 세션 전체를 다시 써 왔다 — 읽기만 한 요청도 마찬가지라, 프래그먼트 한 번에 user API 호출이 2회씩
   * 나갔다(실측). 저장 직전 직렬화 결과가 이 스냅샷과 같으면 서버에 이미 같은 내용이 있는 것이므로 쓰기를 건너뛴다.
   *
   * <p>더티 플래그가 아니라 '직렬화 결과 비교'인 이유는, {@code setAttribute} 없이 담긴 객체 내부만 바뀌는 경우까지 잡기 위해서다. 비교가 어긋나면
   * 그냥 예전처럼 쓰므로 틀리는 방향이 안전하다.
   *
   * <p>만료 갱신은 이 쓰기에 기대지 않는다. user API 의 {@code validate-session} 이 조회할 때마다 {@code
   * setLastAccessedTime} 후 저장하므로, 읽기만으로 TTL 이 이미 연장된다.
   */
  private transient Map<String, Object> savedAttributes;

  public ApiSession(String id, UserInfoApiClient userInfoApiClient) {
    this.delegate = new MapSession(id);
    this.userInfoApiClient = userInfoApiClient;
  }

  public ApiSession(MapSession session, UserInfoApiClient userInfoApiClient) {
    this.delegate = session;
    this.userInfoApiClient = userInfoApiClient;
  }

  Map<String, Object> getSavedAttributes() {
    return savedAttributes;
  }

  void setSavedAttributes(Map<String, Object> savedAttributes) {
    this.savedAttributes = savedAttributes;
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public String changeSessionId() {
    String newSessionId = userInfoApiClient.createNewSession();
    delegate.setId(newSessionId);
    // 새 id 로 옮겨 담아야 하므로 다음 save 는 반드시 서버에 써야 한다.
    this.savedAttributes = null;
    return newSessionId;
  }

  @Override
  public <T> T getAttribute(String attributeName) {
    return delegate.getAttribute(attributeName);
  }

  @Override
  public Set<String> getAttributeNames() {
    return delegate.getAttributeNames();
  }

  @Override
  public void setAttribute(String attributeName, Object attributeValue) {
    delegate.setAttribute(attributeName, attributeValue);
  }

  @Override
  public void removeAttribute(String attributeName) {
    delegate.removeAttribute(attributeName);
  }

  @Override
  public Instant getCreationTime() {
    return delegate.getCreationTime();
  }

  @Override
  public void setLastAccessedTime(Instant lastAccessedTime) {
    delegate.setLastAccessedTime(lastAccessedTime);
  }

  @Override
  public Instant getLastAccessedTime() {
    return delegate.getLastAccessedTime();
  }

  @Override
  public void setMaxInactiveInterval(Duration interval) {
    delegate.setMaxInactiveInterval(interval);
  }

  @Override
  public Duration getMaxInactiveInterval() {
    return delegate.getMaxInactiveInterval();
  }

  @Override
  public boolean isExpired() {
    return delegate.isExpired();
  }
}
