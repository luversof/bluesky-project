package net.luversof.api.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.luversof.api.user.domain.UserInfo;
import org.springframework.data.repository.CrudRepository;

public interface UserInfoRepository extends CrudRepository<UserInfo, UUID> {

    List<UserInfo> findByIdIn(List<UUID> ids);

    Optional<UserInfo> findByUsername(String username);

    Optional<UserInfo> findByProviderAndProviderId(String provider, String providerId);
}
