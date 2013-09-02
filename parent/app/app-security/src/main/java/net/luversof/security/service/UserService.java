package net.luversof.security.service;

import net.luversof.core.datasource.DataSource;
import net.luversof.core.datasource.DataSourceType;
import net.luversof.security.domain.User;
import net.luversof.security.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.SECURITY)
public class UserService {
	
	@Autowired
	private UserRepository userRepository;
	
	public User save(User user) {
		return userRepository.save(user);
	}
	
	@Transactional(readOnly = true)
	public User findOne(long id) {
		return userRepository.findOne(id);
	}

}
