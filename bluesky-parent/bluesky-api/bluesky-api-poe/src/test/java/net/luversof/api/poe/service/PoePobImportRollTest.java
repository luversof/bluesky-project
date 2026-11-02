package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/** PoB 코드 읽기 — 굴림 범위 · 고른 변형 · 활성 세트(10-29 R8). */
class PoePobImportRollTest {

  @Test
  void 굴림_범위는_PoB_applyRange_처럼_계산한다() {
    assertThat(PoePobImportService.applyRange("+(500-600) to maximum Life", 0.5))
        .isEqualTo("+550 to maximum Life");
    assertThat(PoePobImportService.applyRange("(20-30)% increased Damage", 1))
        .isEqualTo("30% increased Damage");
    assertThat(PoePobImportService.applyRange("(20-30)% increased Damage", 0.25))
        .as("20 + 0.25 × 10 = 22.5 → 0 에서 먼 쪽 반올림 23")
        .isEqualTo("23% increased Damage");
    assertThat(PoePobImportService.applyRange("Adds (5-10) to (15-20) Physical Damage", 0))
        .isEqualTo("Adds 5 to 15 Physical Damage");
    assertThat(PoePobImportService.applyRange("(0.4-0.6)% of Damage Leeched as Life", 0.5))
        .isEqualTo("0.5% of Damage Leeched as Life");
    assertThat(PoePobImportService.applyRange("+(-20--10)% to Cold Resistance", 0))
        .as("음수 범위")
        .isEqualTo("-20% to Cold Resistance");
    assertThat(PoePobImportService.applyRange("No range here", 0.5)).isEqualTo("No range here");
  }

  @Test
  void 고른_변형이_없으면_마지막_변형() {
    assertThat(
            PoePobImportService.selectedVariant(
                List.of("Rarity: UNIQUE", "Variant: Old", "Variant: New")))
        .isEqualTo(2);
    assertThat(
            PoePobImportService.selectedVariant(
                List.of("Variant: Old", "Variant: New", "Selected Variant: 1")))
        .isEqualTo(1);
    assertThat(PoePobImportService.selectedVariant(List.of("Rarity: RARE"))).isZero();
  }

  @Test
  void 활성_세트는_순번이_아니라_id() throws Exception {
    var doc =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(
                new java.io.ByteArrayInputStream(
                    "<S><Set id=\"2\" n=\"b\"/><Set id=\"1\" n=\"a\"/></S>"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    var nodes = doc.getDocumentElement().getElementsByTagName("Set");
    List<Element> sets = List.of((Element) nodes.item(0), (Element) nodes.item(1));
    assertThat(PoePobImportService.activeById(sets, "1").getAttribute("n"))
        .as("예전엔 순번 1 = id 2 세트를 골랐다")
        .isEqualTo("a");
    assertThat(PoePobImportService.activeById(sets, "9").getAttribute("n"))
        .as("모르는 id 면 순번(끝으로 붙임)")
        .isEqualTo("a");
    assertThat(PoePobImportService.activeById(sets, "").getAttribute("n")).isEqualTo("b");
  }
}
