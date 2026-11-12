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

  @PostMapping("/create-new-session")
  public String createNewSession() {
    // A new id is minted only here (and saved at once) so create-session can refuse ids it does not
    // know; ApiSession.changeSessionId asks for a new id the same way after login.
    Session session = sessionRepository.createSession();
    ((SessionRepository) sessionRepository).save(session);
    return session.getId();
  }

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

  @GetMapping("/{id}")
  public Optional<UserInfo> findById(@PathVariable UUID id) {
    return userInfoService.findById(id);
  }

  @GetMapping("/search/findByIdIn")
  public List<UserInfo> findByIdIn(@RequestParam List<UUID> ids) {
    return userInfoService.findByIdIn(ids);
  }

  @GetMapping("/search/findByUsername/{userName}")
  public Optional<UserInfo> findByUsername(@PathVariable String userName) {
    return userInfoService.findByUsername(userName);
  }

  @GetMapping("/search/findByProvider")
  public Optional<UserInfo> findByProviderAndProviderId(
      @RequestParam String provider, @RequestParam String providerId) {
    return userInfoService.findByProviderAndProviderId(provider, providerId);
  }

  @PostMapping("/oauth2")
  public UserInfo saveOAuth2User(@RequestBody OAuth2UserRequest request) {
    return userInfoService.saveOAuth2User(
        request.provider(),
        request.providerId(),
        request.username(),
        request.email(),
        request.avatarUrl());
  }

  record OAuth2UserRequest(
      String provider, String providerId, String username, String email, String avatarUrl) {}
}
