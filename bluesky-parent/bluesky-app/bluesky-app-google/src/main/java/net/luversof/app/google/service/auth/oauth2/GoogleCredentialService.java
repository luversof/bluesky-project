package net.luversof.app.google.service.auth.oauth2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import net.luversof.app.google.domain.GoogleIamServiceAccountInfo;
import net.luversof.app.google.service.GoogleIamServiceAccountInfoService;

@Service
public class GoogleCredentialService {

	@Autowired
	private GoogleIamServiceAccountInfoService googleIamServiceAccountInfoService;
	
	public GoogleCredentials getGoogleCredentialsByGoogleIamServiceAccountInfoId(UUID googleIamServiceAccountInfoId) {
		var googleIamServiceAccountInfo = googleIamServiceAccountInfoService.findById(googleIamServiceAccountInfoId);
		return getServiceAccountCredentialsByGoogleIamServiceAccountInfo(googleIamServiceAccountInfo);
	}

	public GoogleCredentials getGoogleCredentialsByUserId(UUID userId) {
		var googleIamServiceAccountInfo = googleIamServiceAccountInfoService.findByUserId(userId);
		return getServiceAccountCredentialsByGoogleIamServiceAccountInfo(googleIamServiceAccountInfo);

	}
	
	public GoogleCredentials getServiceAccountCredentialsByGoogleIamServiceAccountInfo(GoogleIamServiceAccountInfo googleIamServiceAccountInfo) {
		try {
			return ServiceAccountCredentials.fromStream(
					new ByteArrayInputStream(googleIamServiceAccountInfo.getKeyStr().getBytes(StandardCharsets.UTF_8)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
