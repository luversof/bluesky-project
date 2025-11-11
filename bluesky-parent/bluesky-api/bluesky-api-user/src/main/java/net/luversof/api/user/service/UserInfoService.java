package net.luversof.api.user.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import net.luversof.api.user.domain.UserInfo;
import net.luversof.api.user.repository.UserInfoRepository;

@Service
public class UserInfoService {

	@Setter(onMethod_ = @Autowired)
	private UserInfoRepository userInfoRepository;

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

}
