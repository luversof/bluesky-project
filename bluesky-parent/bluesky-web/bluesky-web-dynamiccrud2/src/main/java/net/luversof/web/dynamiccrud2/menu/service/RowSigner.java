package net.luversof.web.dynamiccrud2.menu.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import net.luversof.web.dynamiccrud2.config.Msg;

/**
 * 목록 행 JSON 서명. 행 키 · 수정 불가 값은 화면이 돌려보내는 행 JSON 에서 꺼내므로, 서버가 조회 결과로 내보낸 행인지 확인해야 한다(서명이 없으면 행 JSON
 * 을 고쳐 조회 쿼리에 안 보이는 행을 수정 · 삭제하거나 수정 불가 값을 바꿀 수 있다). 메뉴 id 를 함께 서명해 다른 메뉴의 행으로 쓰지 못하게 한다.
 */
@Component
public class RowSigner {

  private static final String ALGORITHM = "HmacSHA256";
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  // shortcut: 키가 메모리에만 있어 재시작 전에 연 목록의 행은 다시 열어야 한다, replica 가 2개 이상이 되면 설정 키로 바꿀 것
  private final SecretKeySpec key;

  public RowSigner() {
    var bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    this.key = new SecretKeySpec(bytes, ALGORITHM);
  }

  /** 화면에 내보낼 값: 서명.JSON */
  public String sign(UUID menuId, String json) {
    return mac(menuId, json) + "." + json;
  }

  /**
   * 서명을 확인하고 JSON 을 돌려준다.
   *
   * @throws IllegalArgumentException 서명이 없거나 맞지 않을 때(메시지는 화면에 보인다)
   */
  public String verify(UUID menuId, String signed) {
    var dot = signed == null ? -1 : signed.indexOf('.');
    if (dot > 0) {
      var json = signed.substring(dot + 1);
      var expected = mac(menuId, json).getBytes(StandardCharsets.US_ASCII);
      var actual = signed.substring(0, dot).getBytes(StandardCharsets.US_ASCII);
      if (MessageDigest.isEqual(expected, actual)) {
        return json;
      }
    }
    throw new IllegalArgumentException(Msg.t("menu.action.rowTampered"));
  }

  private String mac(UUID menuId, String json) {
    try {
      var mac = Mac.getInstance(ALGORITHM);
      mac.init(key);
      mac.update(menuId.toString().getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0);
      return ENCODER.encodeToString(mac.doFinal(json.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
