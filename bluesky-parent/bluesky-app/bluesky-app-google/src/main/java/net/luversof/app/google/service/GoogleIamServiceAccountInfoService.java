package net.luversof.app.google.service;

import java.util.UUID;
import net.luversof.app.google.domain.GoogleIamServiceAccountInfo;
import net.luversof.app.google.repository.GoogleIamServiceAccountInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleIamServiceAccountInfoService {

    @Autowired private GoogleIamServiceAccountInfoRepository googleIamServiceAccountInfoRepository;

    public GoogleIamServiceAccountInfo findById(UUID id) {
        return googleIamServiceAccountInfoRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "AppGoogleError.NOT_FOUND_SERVICE_ACCOUNT_INFO"));
    }

    public GoogleIamServiceAccountInfo findByUserId(UUID userId) {
        return googleIamServiceAccountInfoRepository
                .findByUserId(userId)
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "AppGoogleError.NOT_FOUND_SERVICE_ACCOUNT_INFO"));
    }
}
