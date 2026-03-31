package net.luversof.app.google.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.app.google.constant.TestConstant;
import net.luversof.app.google.domain.GoogleIamServiceAccountInfo;

public class GoogleIamServiceAccountInfoRepositoryTest implements GeneralTest {

    @Autowired private GoogleIamServiceAccountInfoRepository googleIamServiceAccountInfoRepository;

    @Test
    void saveTest() {
        GoogleIamServiceAccountInfo info = new GoogleIamServiceAccountInfo();
        info.setUserId(TestConstant.USER_ID);
        info.setKeyStr("testKeyStr");

        GoogleIamServiceAccountInfo savedInfo = googleIamServiceAccountInfoRepository.save(info);

        assertThat(savedInfo).isNotNull();
    }

    @Test
    void findAllTest() {
        var result = googleIamServiceAccountInfoRepository.findAll();
        assertThat(result).isNotEmpty();
    }
}
