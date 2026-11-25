package net.luversof.api.user.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.service.UserInfoService;

/**
 * 사용자 · 세션
 *
 * <p>로그인 사용자 정보와, 게이트가 쓰는 세션 저장소(Redis) API.
 */
@RestController
@RequestMapping(value = "/api/userInfo", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserInfoController {

  private static final Logger log = LoggerFactory.getLogger(UserInfoController.class);

  private UserInfoService userInfoService;

  @Autowired private SessionRepository<? extends Session> sessionRepository;

  @Autowired
  public void setUserInfoService(UserInfoService userInfoService) {
    this.userInfoService = userInfoService;
  }

  /**
   * 세션 속성 저장
   *
   * <p>게이트의 ApiSessionRepository.save 가 부른다. removedAttributeNames 의 속성은 지우고 sessionAttributes 는
   * 덮어쓴다.
   *
   * <p>없는 세션 id 는 무시한다 - create-new-session 만 id 를 만든다. 로그아웃 뒤 늦게 도착한 저장이 세션을 되살리지 않게 하려는 것이다.
   *
   * @param request 세션 id, 저장할 속성(직렬화된 값), 지울 속성 이름
   */
  @PostMapping("/create-session")
  public void createSession(@RequestBody CreateSessionRequest request) {
    Session session = sessionRepository.findById(request.sessionId());
    if (session == null) {
      // Only create-new-session mints ids (new session, changeSessionId), so a missing id was
      // deleted or expired. Writing it anyway brought a logged-out session back to life with its
      // security context when a request still in flight saved after the logout.
      // The session id is a login credential, so it is not logged.
      log.debug("create-session ignored, session not found");
      return;
    }

    // Only names the client saw and removed, not "everything missing from the map": a concurrent
    // request may have added attributes (e.g. the security context at login) this one never saw.
    if (request.removedAttributeNames() != null) {
      request.removedAttributeNames().forEach(session::removeAttribute);
    }
    if (request.sessionAttributes() != null) {
      request.sessionAttributes().forEach(session::setAttribute);
    }

    ((SessionRepository) sessionRepository).save(session);
  }

  /**
   * 세션 생성
   *
   * <p>새 세션을 만들어 바로 저장하고 id 를 돌려준다.
   *
   * @return 새 세션 id
   */
  @PostMapping("/create-new-session")
  public String createNewSession() {
    // A new id is minted only here (and saved at once) so create-session can refuse ids it does not
    // know; ApiSession.changeSessionId asks for a new id the same way after login.
    Session session = sessionRepository.createSession();
    ((SessionRepository) sessionRepository).save(session);
    return session.getId();
  }

  /**
   * 세션 삭제
   *
   * <p>로그아웃, 로그인 때 세션 id 교체에 쓴다.
   *
   * @param request 지울 세션 id
   */
  @PostMapping("/delete-session")
  public void deleteSession(@RequestBody DeleteSessionRequest request) {
    sessionRepository.deleteById(request.sessionId());
  }

  public record CreateSessionRequest(
      String sessionId, Map<String, Object> sessionAttributes, Set<String> removedAttributeNames) {}

  public record DeleteSessionRequest(String sessionId) {}

  public record UserInfoResponse(
      String id,
      String username,
      String provider,
      String providerId,
      String email,
      String avatarUrl,
      List<String> authorities,
      Map<String, Object> sessionAttributes) {}

  /**
   * 세션 조회
   *
   * <p>세션을 읽고 마지막 접근 시각을 갱신한다(만료 연장). 게이트가 요청마다 부른다.
   *
   * @param sessionId 세션 id
   * @return 세션 속성(직렬화된 값), 세션이 없으면 빈 응답
   */
  @GetMapping("/validate-session")
  public UserInfoResponse validateSession(@RequestParam String sessionId) {
    Session session = sessionRepository.findById(sessionId);
    if (session == null) {
      log.debug("validate-session: session not found");
      return null;
    }
    session.setLastAccessedTime(Instant.now());
    ((SessionRepository) sessionRepository).save(session);

    Map<String, Object> sessionAttributes = new HashMap<>();
    session
        .getAttributeNames()
        .forEach(name -> sessionAttributes.put(name, session.getAttribute(name)));

    return new UserInfoResponse(null, null, null, null, null, null, null, sessionAttributes);
  }

  /**
   * 사용자 조회
   *
   * @param id 사용자 id
   * @return 사용자, 없으면 빈 값
   */
  @GetMapping("/{id}")
  public Optional<UserInfo> findById(@PathVariable UUID id) {
    return userInfoService.findById(id);
  }

  /**
   * 여러 사용자 조회
   *
   * <p>목록 화면의 작성자 이름 등에 쓴다.
   *
   * @param ids 사용자 id 목록
   * @return 찾은 사용자(없는 id 는 빠진다)
   */
  @GetMapping("/search/findByIdIn")
  public List<UserInfo> findByIdIn(@RequestParam List<UUID> ids) {
    return userInfoService.findByIdIn(ids);
  }

  /**
   * username으로 사용자 조회
   *
   * @param userName username(예: octo_github)
   * @return 사용자, 없으면 빈 값
   */
  @GetMapping("/search/findByUsername/{userName}")
  public Optional<UserInfo> findByUsername(@PathVariable String userName) {
    return userInfoService.findByUsername(userName);
  }

  /**
   * 로그인 제공자 계정으로 사용자 조회
   *
   * @param provider 제공자(github, kakao)
   * @param providerId 제공자의 사용자 id
   * @return 사용자, 없으면 빈 값
   */
  @GetMapping("/search/findByProvider")
  public Optional<UserInfo> findByProviderAndProviderId(
      @RequestParam String provider, @RequestParam String providerId) {
    return userInfoService.findByProviderAndProviderId(provider, providerId);
  }

  /**
   * OAuth2 로그인 사용자 저장
   *
   * <p>로그인 때 부른다. 없으면 만들고 있으면 이메일 · 아바타를 갱신한다. 응답의 authorities 를 client-user 가 ROLE_USER 에 더한다.
   */
  @PostMapping("/oauth2")
  public OAuth2UserResponse saveOAuth2User(@RequestBody OAuth2UserRequest request) {
    var userInfo =
        userInfoService.saveOAuth2User(
            request.provider(),
            request.providerId(),
            request.username(),
            request.email(),
            request.avatarUrl());
    return new OAuth2UserResponse(
        userInfo.getId() == null ? null : userInfo.getId().toString(),
        userInfo.getUsername(),
        userInfo.getProvider(),
        userInfo.getProviderId(),
        userInfo.getEmail(),
        userInfo.getAvatarUrl(),
        userInfo.getId() == null ? List.of() : userInfoService.findAuthorities(userInfo.getId()));
  }

  record OAuth2UserRequest(
      String provider, String providerId, String username, String email, String avatarUrl) {}

  /** client-user 의 UserInfoApiClient.UserInfoResponse 와 같은 이름(password 는 내보내지 않는다) */
  public record OAuth2UserResponse(
      String id,
      String username,
      String provider,
      String providerId,
      String email,
      String avatarUrl,
      List<String> authorities) {}
}
