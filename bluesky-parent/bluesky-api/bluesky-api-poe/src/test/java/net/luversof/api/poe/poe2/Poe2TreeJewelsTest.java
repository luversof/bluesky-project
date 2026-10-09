package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** 트리 평가에 주얼 끼우기(10-03 C73) — PoB 원문을 아이템으로 넣고 &lt;Spec&gt;&lt;Sockets&gt; 로 그 칸에 꽂는다. */
class Poe2TreeJewelsTest {

  private static final String XML =
      "<PathOfBuilding2><Build/>"
          + "<Tree activeSpec=\"1\"><Spec ascendClassId=\"0\" nodes=\"1,2\"><Overrides/></Spec></Tree>"
          + "<Items activeItemSet=\"1\"><Item id=\"3\">\nRarity: RARE\nX\n</Item><ItemSet id=\"1\"/></Items>"
          + "</PathOfBuilding2>";

  @Test
  void 아이템_id_는_있던_것_다음부터_칸마다_꽂는다() {
    Map<Integer, String> raws = new LinkedHashMap<>();
    raws.put(26725, "Controlled Metamorphosis\nDiamond\nRadius: Variable");
    raws.put(61834, "From Nothing\nDiamond\nPassives in Radius of Blood Magic can be Allocated");
    String out = Poe2BuildService.withJewels(XML, raws);
    assertThat(out).contains("<Item id=\"4\">\nRarity: UNIQUE\nControlled Metamorphosis");
    assertThat(out).contains("<Item id=\"5\">\nRarity: UNIQUE\nFrom Nothing");
    assertThat(out)
        .contains(
            "<Sockets><Socket nodeId=\"26725\" itemId=\"4\"/><Socket nodeId=\"61834\" itemId=\"5\"/></Sockets></Spec>");
    // 원래 아이템은 그대로
    assertThat(out).contains("<Item id=\"3\">");
  }

  @Test
  void 원문의_꺾쇠와_앰퍼샌드는_XML_로_이스케이프() {
    String out = Poe2BuildService.withJewels(XML, Map.of(1, "A & B <c>"));
    assertThat(out).contains("A &amp; B &lt;c&gt;");
  }

  @Test
  void 주얼이_없거나_틀이_없으면_그대로() {
    assertThat(Poe2BuildService.withJewels(XML, Map.of())).isEqualTo(XML);
    String noItems = XML.replaceAll("(?s)<Items.*</Items>", "");
    assertThat(Poe2BuildService.withJewels(noItems, Map.of(1, "X"))).isEqualTo(noItems);
  }

  @Test
  void 변형을_고르면_그_변형의_옵션_줄로_원문을_짠다() {
    // From Nothing 변형 = 핵심 하나씩(10-03 C74) — PoB "Selected Variant" 번호는 앞에 붙은 버전 변형 때문에 우리 번호와 어긋나
    // 직접 짠다
    Poe2.UniqueVariant blood =
        new Poe2.UniqueVariant(
            4,
            "Blood Magic",
            "피의 마법",
            List.of(),
            List.of(),
            List.of(
                "Passives in radius of Blood Magic can be Allocated without being connected to your tree",
                "Corrupted"),
            List.of(),
            null);
    Poe2.Unique u =
        new Poe2.Unique(
            "From Nothing",
            null,
            "from-nothing",
            "Diamond",
            null,
            "Jewel",
            null,
            "jewel",
            null,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(blood),
            1,
            null,
            null,
            null,
            null,
            "Small",
            null);
    assertThat(Poe2BuildService.variantText(u, blood))
        .isEqualTo(
            "Rarity: UNIQUE\nFrom Nothing\nDiamond\nRadius: Small\n"
                + "Passives in radius of Blood Magic can be Allocated without being connected to your tree\nCorrupted\n");
  }
}
