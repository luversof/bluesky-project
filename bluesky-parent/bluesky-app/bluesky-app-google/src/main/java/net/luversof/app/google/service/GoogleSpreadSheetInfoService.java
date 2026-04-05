package net.luversof.app.google.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.app.google.constant.GoogleSpreadSheetInfoType;
import net.luversof.app.google.domain.GoogleSpreadSheetInfo;
import net.luversof.app.google.repository.GoogleSpreadSheetInfoRepository;

@Service
public class GoogleSpreadSheetInfoService {

  @Autowired private GoogleSpreadSheetInfoRepository googleSpreadSheetInfoRepository;

  public List<GoogleSpreadSheetInfo> findByGoogleIamServiceAccountInfoId(
      UUID googleIamServiceAccountInfoId) {
    return googleSpreadSheetInfoRepository.findByGoogleIamServiceAccountInfoId(
        googleIamServiceAccountInfoId);
  }

  public GoogleSpreadSheetInfo findByGoogleIamServiceAccountInfoIdAndType(
      UUID googleIamServiceAccountInfoId, GoogleSpreadSheetInfoType type) {
    return googleSpreadSheetInfoRepository
        .findByGoogleIamServiceAccountInfoIdAndType(googleIamServiceAccountInfoId, type)
        .orElseThrow(() -> new BlueskyException("AppGoogleError.NOT_FOUND_SPREADSHEET_INFO"));
  }

  public GoogleSpreadSheetInfo findById(UUID id) {
    return googleSpreadSheetInfoRepository
        .findById(id)
        .orElseThrow(() -> new BlueskyException("AppGoogleError.NOT_FOUND_SPREADSHEET_INFO"));
  }
}
