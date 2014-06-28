package net.luversof.user.service;

import net.luversof.jdbc.datasource.DataSource;
import net.luversof.jdbc.datasource.DataSourceType;
import net.luversof.user.domain.UserAuthority;
import net.luversof.user.repository.UserAuthorityRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.SECURITY)
public class UserAuthorityService {

	@Autowired
	private UserAuthorityRepository userAuthorityRepository;
	
	
	public UserAuthority save(UserAuthority userAuthority) {
		return userAuthorityRepository.save(userAuthority);
	}
	
	@Transactional(readOnly = true)
	public UserAuthority findOne(long id) {
		return userAuthorityRepository.findOne(id);
	}
}
