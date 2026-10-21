package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** PoE2 게임 아이템 복사 글 가져오기(10-20 C486) — 실제 추출 데이터(~/.poe-gamedata/poe2)로. 없으면 건너뛴다. */
class Poe2CraftImportServiceTest {

  private static Poe2CraftImportService importer;

  @BeforeAll
  static void setUp() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata/poe2";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft2-mods.json")), "추출 데이터 없음");
    Poe2DataService data2 = new Poe2DataService(dir, new Poe2TradeStatDataService(dir));
    importer = new Poe2CraftImportService(new Poe2CraftService(data2, dir), data2);
  }

  private static List<String> ids(Poe2CraftImportService.ImportResult r) {
    return r.item().affixes().stream().map(Poe2CraftEngine.Affix::id).sorted().toList();
  }

  @Test
  void 영어_줄만_복사() {
    String text =
        """
        Item Class: Gloves
        Rarity: Rare
        Grim Grasp
        Stocky Mitts
        --------
        Item Level: 80
        --------
        +110 to maximum Life
        Adds 22 to 35 Fire damage to Attacks
        +38% to Cold Resistance
        12% increased Attack Speed (fractured)
        """;
    Poe2CraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(r.item().poolKey()).isEqualTo("Gloves|Armour");
    assertThat(r.item().ilvl()).isEqualTo(80);
    assertThat(ids(r))
        .containsExactly(
            "AddedFireDamage8", "ColdResist7", "IncreasedAttackSpeed3", "IncreasedLife8");
    assertThat(r.item().affixes())
        .filteredOn(Poe2CraftEngine.Affix::fractured)
        .extracting(Poe2CraftEngine.Affix::id)
        .containsExactly("IncreasedAttackSpeed3");
    assertThat(r.values().get("AddedFireDamage8")).containsExactly(22d, 35d);
  }

  @Test
  void 한국어_고급_복사_이름으로() {
    String text =
        """
        아이템 종류: 장갑
        아이템 희귀도: 희귀
        음울한 손아귀
        다부진 미트
        --------
        아이템 레벨: 80
        --------
        { 접두어 속성 부여 "강건한" (등급: 2) — 생명력 }
        생명력 최대치 +110(100-119)
        { 접두어 속성 부여 "폭발하는" (등급: 2) — 피해, 원소, 화염, 공격 }
        공격 시 화염 피해 22(20-24)~35(33-36) 추가
        { 접미어 속성 부여 "얼음판" (등급: 2) — 원소, 냉기, 저항 }
        냉기 저항 +38(36-40)%
        """;
    Poe2CraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(ids(r)).containsExactly("AddedFireDamage8", "ColdResist7", "IncreasedLife8");
    // 자가검사: 고유는 거절
    assertThat(importer.parse("Item Class: Gloves\nRarity: Unique\nX\nStocky Mitts\n").error())
        .contains("고유");
  }
}
