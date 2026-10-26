package net.luversof.api.poe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 게임 아이템 복사 글 가져오기(10-20 C485) — 실제 추출 데이터(~/.poe-gamedata)로. 데이터가 없으면 건너뛴다. */
class PoeCraftImportServiceTest {

  private static PoeCraftImportService importer;

  @BeforeAll
  static void setUp() {
    String dir = System.getProperty("user.home") + "/.poe-gamedata";
    Assumptions.assumeTrue(Files.exists(Path.of(dir, "craft-mods.json")), "추출 데이터 없음");
    PoeCraftDataService data = new PoeCraftDataService(dir);
    PoeModDataService mods = new PoeModDataService(dir);
    importer =
        new PoeCraftImportService(
            new PoeCraftService(data, mods), data, mods, new PoeBaseItemDataService(dir));
  }

  private static List<String> ids(PoeCraftImportService.ImportResult r) {
    return r.item().affixes().stream().map(PoeCraftEngine.Affix::id).sorted().toList();
  }

  @Test
  void 군집_주얼_인챈트로_변형_영어_한국어_PoB() {
    // 10-20 C515 — 일반 복사(꼬리표 enchant) · 한국어 · PoB(Implicits 아래 {crafted}) 셋 다 같은 변형 · 주요 스킬
    String en =
        """
        Item Class: Jewels
        Rarity: Rare
        Glyph Glimmer
        Large Cluster Jewel
        --------
        Item Level: 84
        --------
        Adds 10 Passive Skills (enchant)
        2 Added Passive Skills are Jewel Sockets (enchant)
        Added Small Passive Skills grant: 12% increased Fire Damage (enchant)
        --------
        1 Added Passive Skill is Prismatic Heart
        1 Added Passive Skill is Burning Bright
        """;
    PoeCraftImportService.ImportResult r = importer.parse(en);
    assertThat(r.error()).isNull();
    assertThat(r.item().variant()).isEqualTo("cluster_large.affliction_fire_damage");
    assertThat(ids(r))
        .containsExactly("AfflictionNotableBurningBright", "AfflictionNotablePrismaticHeart__");
    // 패시브 수(10)는 게이트가 글에서 읽는다(C531) — 가져오기 경고 없음
    assertThat(r.warnings()).noneMatch(w -> w.contains("패시브 수"));
    String ko =
        """
        아이템 종류: 주얼
        아이템 희귀도: 희귀
        Glyph Glimmer
        대형 스킬 군 주얼
        --------
        아이템 레벨: 84
        --------
        패시브 스킬 8개 추가 (인챈트)
        주얼 슬롯 패시브 스킬 2개 추가 (인챈트)
        추가된 소형 패시브 스킬 효과: 화염 피해 12% 증가 (인챈트)
        --------
        분광 심장 패시브 스킬 1개 추가
        """;
    PoeCraftImportService.ImportResult k = importer.parse(ko);
    assertThat(k.error()).isNull();
    assertThat(k.item().variant()).isEqualTo("cluster_large.affliction_fire_damage");
    assertThat(ids(k)).containsExactly("AfflictionNotablePrismaticHeart__");
    assertThat(k.warnings()).noneMatch(w -> w.contains("패시브 수"));
    String pob =
        String.join(
            "\n",
            "Rarity: RARE",
            "Glyph Glimmer",
            "Medium Cluster Jewel",
            "Item Level: 84",
            "Implicits: 3",
            "{crafted}Adds 4 Passive Skills",
            "{crafted}1 Added Passive Skill is a Jewel Socket",
            "{crafted}Added Small Passive Skills grant: 12% increased Brand Damage",
            "Added Small Passive Skills have 25% increased Effect");
    PoeCraftImportService.ImportResult p = importer.parse(pob);
    assertThat(p.error()).isNull();
    assertThat(p.item().variant()).isEqualTo("cluster_medium.affliction_brand_damage");
    assertThat(ids(p)).containsExactly("AfflictionJewelSmallPassivesHaveIncreasedEffect");
    assertThat(p.warnings()).noneMatch(w -> w.contains("못 알아본"));
  }

  @Test
  void 고급_복사_영어_레어() {
    String text =
        """
        Item Class: Gloves
        Rarity: Rare
        Doom Grip
        Titan Gauntlets
        --------
        Armour: 120
        --------
        Requirements:
        Level: 69
        Str: 98
        --------
        Item Level: 84
        --------
        { Prefix Modifier "Athlete's" (Tier: 1) — Life }
        +120(115-129) to maximum Life
        { Prefix Modifier "Mammoth's" (Tier: 2) — Defences, Armour }
        42(39-42)% increased Armour
        17(16-17)% increased Stun and Block Recovery
        { Suffix Modifier "of the Ice" (Tier: 2) — Elemental, Cold, Resistance }
        +44(42-45)% to Cold Resistance
        { Fractured Suffix Modifier "of Grandmastery" (Tier: 1) — Attack, Speed }
        15(14-16)% increased Attack Speed (fractured)
        --------
        Shaper Item
        """;
    PoeCraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(r.item().itemClass()).isEqualTo("Gloves");
    assertThat(r.item().variant()).isEqualTo("str_armour");
    assertThat(r.item().influence()).isEqualTo("shaper");
    assertThat(r.item().rarity()).isEqualTo(PoeCraftEngine.Rarity.RARE);
    assertThat(r.item().ilvl()).isEqualTo(84);
    assertThat(ids(r))
        .containsExactly(
            "ColdResist7",
            "IncreasedAttackSpeed4",
            "IncreasedLife8",
            "LocalIncreasedPhysicalDamageReductionRatingPercentAndStunRecovery6");
    assertThat(r.item().affixes())
        .filteredOn(PoeCraftEngine.Affix::fractured)
        .extracting(PoeCraftEngine.Affix::id)
        .containsExactly("IncreasedAttackSpeed4");
    assertThat(r.values().get("IncreasedLife8")).containsExactly(120d);
    assertThat(r.warnings()).isEmpty();
  }

  @Test
  void 줄만_복사해도_수치_범위로_등급을_가른다() {
    String text =
        """
        Item Class: Gloves
        Rarity: Rare
        Doom Grip
        Titan Gauntlets
        --------
        Item Level: 84
        --------
        +120 to maximum Life
        42% increased Armour
        17% increased Stun and Block Recovery
        +44% to Cold Resistance
        15% increased Attack Speed (fractured)
        """;
    PoeCraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(ids(r))
        .containsExactly(
            "ColdResist7",
            "IncreasedAttackSpeed4",
            "IncreasedLife8",
            "LocalIncreasedPhysicalDamageReductionRatingPercentAndStunRecovery6");
    // 자가검사: 범위 밖 수치(+100 = 강건한 100~114)면 다른 등급
    PoeCraftImportService.ImportResult r2 = importer.parse(text.replace("+120 to", "+100 to"));
    assertThat(ids(r2)).contains("IncreasedLife7").doesNotContain("IncreasedLife8");
  }

  @Test
  void 한국어_고급_복사() {
    String text =
        """
        아이템 종류: 장갑
        아이템 희귀도: 희귀
        파멸 손아귀
        거신 건틀릿
        --------
        아이템 레벨: 84
        --------
        { 접두어 속성 부여 "늠름한" (등급: 1) — 생명력 }
        생명력 최대치 +120(115-129)
        { 접미어 속성 부여 "- 얼음판" (등급: 2) — 원소, 냉기, 저항 }
        냉기 저항 +44(42-45)%
        { 접두어 속성 부여 "작열하는" (등급: 3) — 피해, 원소, 화염, 공격 }
        공격 시 화염 피해 15(11-15)~27(23-27) 추가
        --------
        타락
        """;
    PoeCraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(r.item().corrupted()).isTrue();
    assertThat(ids(r)).containsExactly("AddedFireDamage6", "ColdResist7", "IncreasedLife8");
  }

  @Test
  void 매직은_이름_안의_베이스_장막_줄_고유는_거절() {
    String magic =
        """
        Item Class: Gloves
        Rarity: Magic
        Virile Iron Gauntlets of the Ice
        --------
        Item Level: 70
        --------
        +105 to maximum Life
        Veiled Suffix
        """;
    PoeCraftImportService.ImportResult r = importer.parse(magic);
    assertThat(r.error()).isNull();
    assertThat(r.baseName()).isEqualTo("Iron Gauntlets");
    assertThat(r.item().rarity()).isEqualTo(PoeCraftEngine.Rarity.MAGIC);
    assertThat(ids(r)).containsExactly("IncreasedLife7", PoeCraftEngine.VEILED_SUFFIX);
    PoeCraftImportService.ImportResult u =
        importer.parse(
            "Item Class: Gloves\nRarity: Unique\nSaqawal's Talon\nHydrascale Gauntlets\n");
    assertThat(u.error()).contains("고유");
    assertThat(importer.parse("hello").error()).isNotNull();
  }

  @Test
  void 실제_복사_글의_요구사항_홈_품질_메모_줄과_작업대_옵션() {
    String text =
        """
        Item Class: Gloves
        Rarity: Rare
        Doom Grip
        Titan Gauntlets
        --------
        Quality: +20% (augmented)
        Armour: 380 (augmented)
        --------
        Requirements:
        Level: 69
        Str: 98
        --------
        Sockets: R-R-G-B
        --------
        Item Level: 84
        --------
        { Prefix Modifier "Athlete's" (Tier: 1) — Life }
        +120(115-129) to maximum Life
        { Master Crafted Suffix Modifier "of Craft" (Rank: 2) — Elemental, Cold, Lightning, Resistance }
        +15(13-16)% to Cold and Lightning Resistances (crafted)
        --------
        Searing Exarch Item
        --------
        Note: ~price 5 chaos
        """;
    PoeCraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(ids(r)).containsExactly("EinharMasterColdAndLightningResist2", "IncreasedLife8");
    assertThat(r.item().affixes())
        .filteredOn(PoeCraftEngine.Affix::crafted)
        .extracting(PoeCraftEngine.Affix::id)
        .containsExactly("EinharMasterColdAndLightningResist2");
    assertThat(r.item().quality()).isNull(); // 장갑 품질은 기폭제가 아니다
    assertThat(r.warnings()).isEmpty();
  }

  @Test
  void 엘드리치_고정속성_등급_이름과_이름_없는_머리줄의_장막_줄() {
    String text =
        """
        Item Class: Gloves
        Rarity: Rare
        Doom Grip
        Titan Gauntlets
        --------
        Item Level: 84
        --------
        { Searing Exarch Implicit Modifier (Grand) — Attack, Speed }
        10% increased Attack Speed (implicit)
        --------
        { Prefix Modifier "Athlete's" (Tier: 1) — Life }
        +120(115-129) to maximum Life
        { Prefix Modifier }
        Veiled Prefix
        { Suffix Modifier "of the Ice" (Tier: 2) — Elemental, Cold, Resistance }
        +44(42-45)% to Cold Resistance
        --------
        Searing Exarch Item
        """;
    PoeCraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(ids(r))
        .containsExactly("ColdResist7", "IncreasedLife8", PoeCraftEngine.VEILED_PREFIX);
    assertThat(r.item().implicits())
        .extracting(PoeCraftEngine.Affix::id)
        .containsExactly("IncreasedAttackSpeedEldritchImplicit3");
    // 한국어 등급 이름(우수한 = Grand)도
    PoeCraftImportService.ImportResult k =
        importer.parse(
            text.replace("Searing Exarch Implicit Modifier (Grand)", "작열의 총주교 고정 속성 (우수한)"));
    assertThat(k.item().implicits())
        .extracting(PoeCraftEngine.Affix::id)
        .containsExactly("IncreasedAttackSpeedEldritchImplicit3");
    assertThat(r.warnings()).isEmpty();
  }

  @Test
  void 한국어_줄_복사_꼬리표_제작_분열() {
    String text =
        """
        아이템 종류: 장갑
        아이템 희귀도: 희귀
        파멸 손아귀
        거신 건틀릿
        --------
        방어도: 380 (증강됨)
        --------
        요구사항:
        레벨: 69
        힘: 98
        --------
        아이템 레벨: 84
        --------
        생명력 최대치 +120
        냉기 및 번개 저항 +15% (제작)
        공격 속도 15% 증가 (분열)
        """;
    PoeCraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(ids(r))
        .containsExactly(
            "EinharMasterColdAndLightningResist2", "IncreasedAttackSpeed4", "IncreasedLife8");
    assertThat(r.item().affixes())
        .filteredOn(PoeCraftEngine.Affix::fractured)
        .extracting(PoeCraftEngine.Affix::id)
        .containsExactly("IncreasedAttackSpeed4");
    assertThat(r.warnings()).isEmpty();
  }

  @Test
  void Path_of_Building_아이템_글() {
    String text =
        """
        Rarity: RARE
        Doom Grip
        Titan Gauntlets
        Unique ID: 0123456789abcdef
        Item Level: 84
        Quality: 20
        Sockets: R-R-G-B
        LevelReq: 69
        Implicits: 0
        {crafted}+15% to Cold and Lightning Resistances
        +120 to maximum Life
        {fractured}15% increased Attack Speed
        {range:0.5}+44% to Cold Resistance
        Shaper Item
        """;
    PoeCraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(r.item().influence()).isEqualTo("shaper");
    assertThat(ids(r))
        .containsExactly(
            "ColdResist7",
            "EinharMasterColdAndLightningResist2",
            "IncreasedAttackSpeed4",
            "IncreasedLife8");
    assertThat(r.item().affixes())
        .filteredOn(PoeCraftEngine.Affix::crafted)
        .extracting(PoeCraftEngine.Affix::id)
        .containsExactly("EinharMasterColdAndLightningResist2");
    assertThat(r.item().affixes())
        .filteredOn(PoeCraftEngine.Affix::fractured)
        .extracting(PoeCraftEngine.Affix::id)
        .containsExactly("IncreasedAttackSpeed4");
    assertThat(r.itemName()).isEqualTo("Doom Grip");
    // 게임 글(구분선 있음)은 그대로
    String game = "Rarity: Rare\nX\nTitan Gauntlets\n--------\nItem Level: 84\n";
    assertThat(PoeCraftImportService.fromPob(game)).isEqualTo(game);
  }

  @Test
  void 플라스크_세_줄_옵션과_한_항목_두_줄_옵션() {
    String text =
        """
        Item Class: Life Flasks
        Rarity: Magic
        Bubbling Divine Life Flask of Sealing
        --------
        Item Level: 84
        --------
        50% of Recovery applied Instantly
        36% reduced Amount Recovered
        135% increased Recovery rate
        Grants Immunity to Bleeding for 8 seconds if used while Bleeding
        Grants Immunity to Corrupted Blood for 8 seconds if used while affected by Corrupted Blood
        """;
    PoeCraftImportService.ImportResult r = importer.parse(text);
    assertThat(r.error()).isNull();
    assertThat(r.baseName()).isEqualTo("Divine Life Flask");
    assertThat(ids(r))
        .containsExactly("FlaskBleedCorruptingBloodImmunity1", "FlaskPartialInstantRecovery5_");
    assertThat(r.warnings()).isEmpty();
  }
}
