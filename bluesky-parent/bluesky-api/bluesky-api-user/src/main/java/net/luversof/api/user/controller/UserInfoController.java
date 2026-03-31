package net.luversof.api.user.controller;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.util.ReflectionUtils;
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
            session = sessionRepository.createSession();

            try {
                if (session instanceof MapSession) {
                    ((MapSession) session).setId(request.sessionId());
                } else {
                    Field cachedField = ReflectionUtils.findField(session.getClass(), "cached");
                    if (cachedField != null) {
                        ReflectionUtils.makeAccessible(cachedField);
                        MapSession cached = (MapSession) cachedField.get(session);
                        cached.setId(request.sessionId());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (request.sessionAttributes() != null) {
            request.sessionAttributes().forEach(session::setAttribute);
        }

        ((SessionRepository) sessionRepository).save(session);
    }

    @PostMapping("/create-new-session")
    public String createNewSession() {
        // Session session = sessionRepository.createSession();
        // sessionRepository.save(session);
        // return session.getId();

        // createSession 호출로 생성된 session은 redis에 즉시 저장되지 않음
        // save를 호출해야 redis에 저장되는데
        // redis key가 spring:session:sessions:(sessionId) 로 생성됨
        // 근데 createSession 호출 시점에 생성된 sessionId가 반환됨

        // 근데 문제는 client에서 createSession을 호출하고 나서
        // 그 sessionId를 가지고 다시 create-session을 호출함
        // 이때 client는 빈 세션을 가지고 있는데
        // server에서는 이미 세션이 생성되어 있음

        // client flow
        // 1. ApiSessionRepository.createSession() 호출
        // 2. UserInfoApiClient.createNewSession() 호출 (Server)
        // 3. Server에서 createNewSession() 실행 -> session 생성 및 저장 -> sessionId 반환
        // 4. Client에서 ApiSession 생성 (sessionId 사용)
        // 5. Client에서 ApiSessionRepository.save() 호출 -> UserInfoApiClient.createSession() 호출
        // (Server)
        // 6. Server에서 createSession() 실행 -> sessionId로 조회 -> 있으면 update, 없으면 create

        // 문제는 4번에서 5번으로 넘어가는 사이에
        // Client는 세션의 속성을 변경하고 save를 호출하게 되는데...

        // 확인 결과:
        // Client의 ApiSessionRepository.createSession() 구현을 보면:
        // String sessionId = userInfoApiClient.createNewSession();
        // return new ApiSession(sessionId, userInfoApiClient);

        // 즉 Server에서 createNewSession()이 호출되어 세션이 생성되고 저장된 상태임.
        // 그리고 Client는 그 ID를 받아서 ApiSession 객체만 만듦 (이때는 아직 Client쪽 변경사항 없음)

        // 이후 Client 로직 어딘가에서 session.setAttribute(...) 등을 하고 sessionRepository.save(session)을 호출하면
        // ApiSessionRepository.save() -> userInfoApiClient.createSession(sessionId, attributes) 가
        // 호출됨.

        // Server의 createSession(request) 메소드를 보면:
        // Session session = sessionRepository.findById(request.sessionId());
        // if (session == null) { ... }

        // 정상적인 경우라면 findById에서 찾아져야 함.
        // 만약 찾아지지 않아서 중복 세션이 생긴다면?

        // RedisSerialization 문제일 가능성이 높음.
        // createNewSession() 에서는 sessionRepository.createSession()을 사용하는데
        // 이는 RedisIndexedSessionRepository를 사용하고, 내부적으로 설정된 Serializer를 사용함.

        // createSession() 에서는 sessionRepository.findById()를 사용함.

        // 사용자가 의심하는 "중복 세션"은
        // 1. 초기 createNewSession()으로 만들어진 세션 (올바른 Redis Hash 구조)
        // 2. createSession()의 예외 처리 로직(if session == null)으로 인해 만들어지는 엉뚱한 세션?

        // 코드를 보면 if (session == null) 블록 내에서
        // session = sessionRepository.createSession();
        // ...
        // sessionRepository.save(session);
        // 이렇게 하면 또 새로운 세션 ID가 발급되고, 요청받은 ID와 다른 ID로 저장될 텐데...

        // 아, 핵심은 "왠지 refresh랑 create랑 세션이 일치하지 않는게 아닌가" 임.
        // 즉 Security Context가 저장되는 세션과, 일반 Session이 따로 노는 것 같다는 의미일 수도.

        // 지금 코드를 보면
        // createNewSession()은 단순히 빈 세션을 만들고 ID를 리턴함. -> spring:session:sessions:UUID1

        // 클라이언트(Gate)가 로그인 성공 후
        // SecurityContext를 저장하려고 할 때
        // Gate는 자신의 세션 저장소(ApiSessionRepository)를 통해 저장을 시도함.
        // ApiSessionRepository.save() -> API 서버의 createSession() 호출.

        // 이때 넘겨주는 request.sessionId()가 UUID1 이어야 하는데
        // 만약 Gate가 로그인 과정에서 세션을 갈아치웠다면? (Session Fixation Protection)
        // Spring Security는 로그인 시 기존 세션을 파기하고 새 세션을 만듦 (changeSessionId)

        // 하지만 ApiSessionRepository는 changeSessionId를 지원하지 않음 (Not Implemented or No-Op in
        // MapSession-based impl?)
        // ApiSessionRepository.java 소스를 보면 changeSessionId 관련 구현이 안 보임 (Base class MapSession?
        // SessionRepository interface?)
        // ApiSession은 MapSession을 감쌈. MapSession은 changeSessionId()가 있음.

        // 만약 Gate에서 changeSessionId가 발생했다면
        // Gate의 세션 ID는 UUID2가 되었을 것임.
        // 그리고 save() 호출 시 UUID2를 보낼 것임.
        // API 서버에는 UUID1만 있음.
        // API 서버의 createSession()에서 findById(UUID2) -> Null
        // -> if (session == null) 로직 진입.
        // -> sessionRepository.createSession() -> UUID3 생성.
        // -> save(session) -> UUID3로 저장. (Redis Key: spring:session:sessions:UUID3)
        // -> 하지만 클라이언트는 UUID2라고 생각하고 있음.
        // -> 결과적으로 UUID1(최초), UUID3(새로 생성됨) 두 개가 생기고
        // -> UUID2에 대한 요청은 계속 실패하거나 매번 새로운 세션을 만들게 됨.

        // 해결책 1: Gate 쪽에서 changeSessionId가 발생했을 때 이를 API 서버에 전파해야 함. (복잡함)
        // 해결책 2: API 서버의 createSession(request)에서 세션이 없을 때,
        //         요청받은 ID(UUID2)로 강제로 세션을 만들어야 함.

        // 현재 구현 (주석 처리된 롤백 코드 밑에 있는 코드):
        /*
        session = sessionRepository.createSession();
        // 생성된 세션의 ID를 요청받은 ID로 변경... 하지만 Spring Session은 ID 변경 API가 제한적.
        */

        // RedisIndexedSessionRepository를 쓴다면 MapSession 구현체를 다룸.
        // MapSession.setId()는 public임.
        // 따라서 생성 후 ID를 바꿔치기해서 저장하면 됨.

        Session session = sessionRepository.createSession();
        ((SessionRepository) sessionRepository).save(session);
        return session.getId();
    }

    @PostMapping("/delete-session")
    public void deleteSession(@RequestBody DeleteSessionRequest request) {
        sessionRepository.deleteById(request.sessionId());
    }

    public record CreateSessionRequest(String sessionId, Map<String, Object> sessionAttributes) {}

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
            System.err.println(
                    "UserInfoController.validateSession session is null. sessionId: " + sessionId);
            return null;
        }
        session.setLastAccessedTime(Instant.now());
        ((SessionRepository) sessionRepository).save(session);

        Map<String, Object> sessionAttributes = new HashMap<>();
        session.getAttributeNames()
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
