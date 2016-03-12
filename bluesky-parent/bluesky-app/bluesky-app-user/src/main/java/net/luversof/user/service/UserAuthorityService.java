package net.luversof.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.user.domain.UserAuthority;
import net.luversof.user.repository.UserAuthorityRepository;

@Service
@Transactional("userTransactionManager")
public class UserAuthorityService {

	@Autowired
	private UserAuthorityRepository userAuthorityRepository;
	
	
	public UserAuthority save(UserAuthority userAuthority) {
		return userAuthorityRepository.save(userAuthority);
	}
	
	@Transactional(value = "userTransactionManager", readOnly = true)
	public UserAuthority findOne(long id) {
		return userAuthorityRepository.findOne(id);
	}
}
