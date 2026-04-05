package net.luversof.app.google.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.app.google.constant.GoogleSheetsApiCase;
import net.luversof.app.google.constant.TestConstant;
import net.luversof.app.google.domain.GoogleSpreadSheetInfo;

public class GoogleSpreadSheetInfoRepositoryTest implements GeneralTest {

  @Autowired private GoogleSpreadSheetInfoRepository googleSpreadSheetInfoRepository;

  @Autowired private GoogleIamServiceAccountInfoRepository googleIamServiceAccountInfoRepository;

  @ParameterizedTest
  @EnumSource(GoogleSheetsApiCase.class)
  void saveTest(GoogleSheetsApiCase googleSheetsApiCase) {
    var googleIamServiceAccountInfo =
        googleIamServiceAccountInfoRepository.findByUserId(TestConstant.USER_ID).orElseThrow();

    GoogleSpreadSheetInfo info = new GoogleSpreadSheetInfo();
    info.setGoogleIamServiceAccountInfoId(googleIamServiceAccountInfo.getId());
    info.setSpreadsheetId("testSpreadSheetId");
    info.setType(googleSheetsApiCase.getType());
    info.setRange(googleSheetsApiCase.getRange());

    GoogleSpreadSheetInfo savedInfo = googleSpreadSheetInfoRepository.save(info);

    assertThat(savedInfo).isNotNull();
  }

  @Test
  void findAllTest() {
    var result = googleSpreadSheetInfoRepository.findAll();
    assertThat(result).isNotEmpty();
  }
}
