package net.luversof.api.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.user.domain.UserInfo;

public interface UserInfoRepository extends CrudRepository<UserInfo, UUID>{
	
	Optional<UserInfo> findByUsername(String username);
	
}
