package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 머리 막대의 금액 숨김 토글은 주식 화면에만(사용자 요청 2026-10-01: "상단 메뉴의 금액 숨김은 stock일 때 보이면 좋을 것 같아"). 게시판 · 가계부 ·
 * PoE 에는 가릴 금액이 없어 눌러도 아무 일이 없었다. 토글이 없는 화면은 숨김 상태도 걸지 않는다 - 끌 단추 없이 금액만 흐려지면 안 된다.
 */
class HideAmountsToggleScopeTest {

  private static String read(String path) throws IOException {
    return Files.readString(Path.of("src/main/jte/" + path), StandardCharsets.UTF_8)
        .replaceAll("\\s+", " ");
  }

  @Test
  void 토글은_보이라고_한_화면에만_그린다() throws IOException {
    String navbar = read("_components/body/navbar/navbar.jte");
    assertThat(navbar).contains("@param boolean showHideAmounts = false");
    int guard = navbar.indexOf("@if(showHideAmounts)");
    int label = navbar.indexOf("<label id=\"hideAmountsLabel\"");
    assertThat(guard).as("토글을 감싸는 조건").isGreaterThanOrEqualTo(0);
    assertThat(label).as("조건 안에 토글").isGreaterThan(guard);
    assertThat(navbar.indexOf("@endif", label)).isGreaterThan(label);
  }

  @Test
  void 주식_화면만_켜고_다른_화면은_기본값이다() throws IOException {
    assertThat(read("_layout/stockLayout.jte")).contains("showHideAmounts = true,");
    for (String other :
        List.of("_layout/boardLayout.jte", "_layout/poeLayout.jte", "index.jte", "dev.jte")) {
      assertThat(read(other)).as(other).doesNotContain("showHideAmounts = true");
    }
    String layout = read("_layout/defaultLayout.jte");
    assertThat(layout)
        .contains("@param boolean showHideAmounts = false")
        .contains("hideHamburger = hideHamburger, showHideAmounts = showHideAmounts)")
        .as("토글이 없는 화면은 저장된 숨김 상태를 걸지 않는다")
        .contains(
            "@if(showHideAmounts) if (localStorage.getItem('hideAmounts') === 'true') document.documentElement.classList.add('hide-amounts'); @endif");
  }
}
