package net.luversof.bookkeeping.service;

import lombok.Getter;
import net.luversof.bookkeeping.domain.User;
import net.luversof.bookkeeping.repository.UserRepository;
import net.luversof.data.jpa.service.GeneralService;
import net.luversof.jdbc.datasource.DataSource;
import net.luversof.jdbc.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BOOKKEEPING)
public class UserService extends GeneralService<User, Long> {
	@Autowired
	@Getter
	private UserRepository repository;
}
