package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** PoE1 제작 엔진 문구의 영어(10-22 C558) — 아는 문구 · 값이 끼는 문구 · 모르는 문구 그대로, 엔진의 고정 실패 문구가 모두 번역표에 있는지. */
class PoeCraftTextTest {

  @Test
  void 아는_문구와_값이_끼는_문구() {
    assertThat(PoeCraftText.en("접두 자리가 없습니다")).isEqualTo("No open prefix");
    assertThat(PoeCraftText.en("아이템 레벨 45 이하에만 쓸 수 있는 에센스입니다"))
        .isEqualTo("Item level 45 or lower only for this essence");
    assertThat(PoeCraftText.en("모르는 화석: Foo")).isEqualTo("Unknown fossil: Foo");
    // 모르는 문구 · null 은 그대로
    assertThat(PoeCraftText.en("처음 보는 문구")).isEqualTo("처음 보는 문구");
    assertThat(PoeCraftText.en((String) null)).isNull();
    assertThat(PoeCraftText.en(Map.of("CHAOS", "레어 아이템에만 쓸 수 있습니다")))
        .containsEntry("CHAOS", "Rare items only");
  }

  @Test
  void 엔진의_고정_실패_문구는_모두_번역표에() throws Exception {
    Path engine =
        Path.of(
            "../../bluesky-api/bluesky-api-poe/src/main/java/net/luversof/api/poe/service/PoeCraftEngine.java");
    Assumptions.assumeTrue(Files.exists(engine), "API 소스 없음");
    String src = Files.readString(engine, StandardCharsets.UTF_8);
    Matcher m = Pattern.compile("fail\\(\\w+,\\s*\"([^\"]*)\"\\s*\\)").matcher(src);
    List<String> missing = new ArrayList<>();
    int n = 0;
    while (m.find()) {
      n++;
      String ko = m.group(1);
      if (PoeCraftText.en(ko).equals(ko)) {
        missing.add(ko);
      }
    }
    assertThat(n).isGreaterThan(50);
    // 상수로 넘기는 문구(C560 검토 — META_MOD_ERROR 등은 위 정규식에 안 걸렸다)
    Matcher c = Pattern.compile("static final String \\w*ERROR\\w* =\\s*\"([^\"]*)\"").matcher(src);
    int nc = 0;
    while (c.find()) {
      nc++;
      if (PoeCraftText.en(c.group(1)).equals(c.group(1))) {
        missing.add(c.group(1));
      }
    }
    assertThat(nc).isGreaterThanOrEqualTo(3);
    // 조건으로 고르는 문구 fail(item, 조건 ? "가" : "나")
    Matcher t =
        Pattern.compile(
                "fail\\(\\w+,\\s*[^;\"]*\\?\\s*\"([^\"]*)\"\\s*:\\s*\"([^\"]*)\"\\s*\\)\\s*;")
            .matcher(src);
    while (t.find()) {
      for (String ko : List.of(t.group(1), t.group(2))) {
        if (PoeCraftText.en(ko).equals(ko)) {
          missing.add(ko);
        }
      }
    }
    assertThat(missing).as("번역 없는 엔진 문구").isEmpty();
    // 조합 문구 견본(이름 · 원소가 끼는 것)
    assertThat(PoeCraftText.en("그을린 화석 은(는) 이 아이템 종류에 쓸 수 없습니다"))
        .isEqualTo("This fossil cannot be used on this item class");
    assertThat(PoeCraftText.en("Fire 저항 옵션이 없습니다")).isEqualTo("No Fire resistance modifier");
    assertThat(PoeCraftText.en("지울 접두어가 없습니다(분열 · 잠긴 쪽 제외)")).startsWith("No prefix");
  }
}
