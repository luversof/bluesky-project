package net.luversof.user.repository;

import org.springframework.data.repository.CrudRepository;

import net.luversof.user.domain.UserAuthority;

public interface UserAuthorityRepository extends CrudRepository<UserAuthority, Long> {
	
	long deleteByUserId(String userId);

}
