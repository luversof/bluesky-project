package net.luversof.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.user.domain.UserAuthority;
import net.luversof.user.repository.UserAuthorityRepository;

@Service
public class UserAuthorityService {

	@Autowired
	private UserAuthorityRepository userAuthorityRepository;
	
	
	public UserAuthority save(UserAuthority userAuthority) {
		return userAuthorityRepository.save(userAuthority);
	}
	
	public UserAuthority findOne(long id) {
		return userAuthorityRepository.getReferenceById(id);
	}
}
