package net.luversof.web.dynamiccrud2.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class RowSignerTest {

  private final RowSigner signer = new RowSigner((String) null);
  private final UUID menuId = UUID.randomUUID();

  @Test
  void signedRowRoundTrips() {
    var json = "{\"id\":\"1\",\"이름\":\"a.b\"}";
    assertThat(signer.verify(menuId, signer.sign(menuId, json))).isEqualTo(json);
  }

  @Test
  void rejectsTamperedUnsignedOrOtherMenuRows() {
    var signed = signer.sign(menuId, "{\"id\":\"1\"}");
    var tampered = signed.replace("\"1\"", "\"2\"");
    assertThatThrownBy(() -> signer.verify(menuId, tampered))
        .as("값을 바꾼 행")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> signer.verify(menuId, "{\"id\":\"1\"}"))
        .as("서명 없는 행")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> signer.verify(UUID.randomUUID(), signed))
        .as("다른 메뉴의 행")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> signer.verify(menuId, signed.substring(0, 10) + signed))
        .as("서명 앞을 고친 행")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RowSigner((String) null).verify(menuId, signed))
        .as("다른 키(재시작)")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void configuredKeySurvivesRestartAndReplicas() {
    var json = "{\"id\":\"1\"}";
    var signed = new RowSigner("secret").sign(menuId, json);
    assertThat(new RowSigner("secret").verify(menuId, signed)).isEqualTo(json);
    assertThatThrownBy(() -> new RowSigner("other").verify(menuId, signed))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
