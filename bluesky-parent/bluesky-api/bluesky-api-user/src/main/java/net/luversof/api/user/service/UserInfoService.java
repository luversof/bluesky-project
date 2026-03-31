package net.luversof.api.user.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.repository.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserInfoService {

    private UserInfoRepository userInfoRepository;

    @Autowired
    public void setUserInfoRepository(UserInfoRepository userInfoRepository) {
        this.userInfoRepository = userInfoRepository;
    }

    public UserInfo save(UserInfo userInfo) {
        return userInfoRepository.save(userInfo);
    }

    public Optional<UserInfo> findById(UUID id) {
        return userInfoRepository.findById(id);
    }

    public List<UserInfo> findByIdIn(List<UUID> ids) {
        return userInfoRepository.findByIdIn(ids);
    }

    public Optional<UserInfo> findByUsername(String username) {
        return userInfoRepository.findByUsername(username);
    }

    public Optional<UserInfo> findByProviderAndProviderId(String provider, String providerId) {
        return userInfoRepository.findByProviderAndProviderId(provider, providerId);
    }

    /** Provider 이름을 정규화 (github-local → github) */
    private String normalizeProvider(String provider) {
        if (provider == null) {
            return null;
        }
        // github-local, github-dev 등을 모두 github로 통일
        if (provider.startsWith("github")) {
            return "github";
        }
        // kakao-local, kakao-dev 등을 모두 kakao로 통일
        if (provider.startsWith("kakao")) {
            return "kakao";
        }
        return provider;
    }

    /** OAuth2 로그인 사용자 정보 저장 (신규 생성 또는 업데이트) */
    public UserInfo saveOAuth2User(
            String provider, String providerId, String username, String email, String avatarUrl) {
        // Provider 정규화 (github-local → github)
        provider = normalizeProvider(provider);
        System.out.println(
                "saveOAuth2User 호출: provider="
                        + provider
                        + ", providerId="
                        + providerId
                        + ", username="
                        + username);

        // 기존 사용자 확인
        Optional<UserInfo> existingUser = findByProviderAndProviderId(provider, providerId);
        System.out.println(
                "기존 사용자 조회 결과: "
                        + (existingUser.isPresent()
                                ? "존재 (id=" + existingUser.get().getId() + ")"
                                : "없음"));

        if (existingUser.isPresent()) {
            // 기존 사용자 업데이트 - email과 avatarUrl만 갱신
            UserInfo userInfo = existingUser.get();
            userInfo.setEmail(email);
            userInfo.setAvatarUrl(avatarUrl);
            System.out.println(
                    "기존 사용자 업데이트: id=" + userInfo.getId() + ", username=" + userInfo.getUsername());
            return save(userInfo);
        }

        // 신규 사용자 생성 - username을 provider와 결합하여 유니크하게
        String uniqueUsername = username + "_" + provider;

        // 중복 확인 후 번호 추가
        Optional<UserInfo> duplicateCheck = findByUsername(uniqueUsername);
        if (duplicateCheck.isPresent()) {
            uniqueUsername = username + "_" + provider + "_" + providerId;
        }

        UserInfo userInfo = new UserInfo();
        // setId()를 호출하지 않음 - BeforeConvertCallback이 자동으로 UUID 생성
        userInfo.setProvider(provider);
        userInfo.setProviderId(providerId);
        userInfo.setUsername(uniqueUsername);
        userInfo.setEmail(email);
        userInfo.setAvatarUrl(avatarUrl);

        System.out.println("신규 사용자 생성 시도: username=" + uniqueUsername);
        UserInfo saved = save(userInfo);
        System.out.println("저장 완료: id=" + saved.getId());
        return saved;
    }
}
