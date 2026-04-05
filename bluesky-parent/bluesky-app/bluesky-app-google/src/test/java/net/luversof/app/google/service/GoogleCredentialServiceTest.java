package net.luversof.app.google.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.app.google.constant.TestConstant;
import net.luversof.app.google.service.auth.oauth2.GoogleCredentialService;

class GoogleCredentialServiceTest implements GeneralTest {

  @Autowired private GoogleCredentialService googleCredentialService;

  @Test
  void getGoogleCredentialsByUserIdTest() {
    var credentials = googleCredentialService.getGoogleCredentialsByUserId(TestConstant.USER_ID);
    assertThat(credentials).isNotNull();
  }
}
