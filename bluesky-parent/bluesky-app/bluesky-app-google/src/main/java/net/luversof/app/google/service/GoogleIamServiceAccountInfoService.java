package net.luversof.app.google.service;

import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.app.google.domain.GoogleIamServiceAccountInfo;
import net.luversof.app.google.repository.GoogleIamServiceAccountInfoRepository;

@Service
public class GoogleIamServiceAccountInfoService {

	@Autowired
	private GoogleIamServiceAccountInfoRepository googleIamServiceAccountInfoRepository;
	
	public GoogleIamServiceAccountInfo findById(@NonNull UUID id) {
		return googleIamServiceAccountInfoRepository.findById(id).orElseThrow(() -> new RuntimeException("AppGoogleError.NOT_FOUND_SERVICE_ACCOUNT_INFO"));
	}
	
	public GoogleIamServiceAccountInfo findByUserId(@NonNull UUID userId) {
		return googleIamServiceAccountInfoRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("AppGoogleError.NOT_FOUND_SERVICE_ACCOUNT_INFO"));
	}
	
}
