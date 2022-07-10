package net.luversof.user.service;

import java.util.List;
import java.util.Optional;

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
	
	public Iterable<UserAuthority> saveAll(List<UserAuthority> userAuthorityList) {
		return userAuthorityRepository.saveAll(userAuthorityList);
	}
	
	public Optional<UserAuthority> findById(long id) {
		return userAuthorityRepository.findById(id);
	}
	
	public Long deleteByUserId(String userId) {
		return userAuthorityRepository.deleteByUserId(userId);
	}
}
