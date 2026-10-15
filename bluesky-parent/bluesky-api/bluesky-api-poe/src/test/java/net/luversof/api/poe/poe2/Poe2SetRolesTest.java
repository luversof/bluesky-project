package net.luversof.api.poe.poe2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

/** 무기 세트 성격(10-15 C332) — 공통 특성 파일 읽기 · 역할 → 다른 세트 목표 · 결과에 싣는 세트별 목표. */
class Poe2SetRolesTest {

  private static final String JSON =
      """
      {"n":8,"used":8,"sets":[
        {"set":1,"kind":"support","focus":["speed","defense","accuracy"],"weapon":"Spear","weaponCount":8,"active":0},
        {"set":2,"kind":"attack","focus":["damage","crit"],"weapon":"Spear+Sceptre","weaponKo":"창 + 셉터","weaponCount":8,"active":8}]}
      """;

  private static Poe2NinjaService.Consensus consensus(Poe2NinjaService.SetRoles sr) {
    return new Poe2NinjaService.Consensus(
        8,
        "Mercenary",
        "Gemling Legionnaire",
        List.of(),
        Map.of(),
        Map.of(),
        0.6,
        Map.of(),
        Map.of(),
        Map.of(),
        sr);
  }

  @Test
  void 파일의_세트_성격을_읽는다() {
    Poe2NinjaService.SetRoles sr =
        Poe2NinjaService.setRoles(JsonMapper.builder().build().readTree(JSON));
    assertThat(sr.n()).isEqualTo(8);
    assertThat(sr.sets()).hasSize(2);
    assertThat(sr.sets().get(1).focus()).containsExactly("damage", "crit");
    assertThat(sr.sets().get(1).weapon()).isEqualTo("Spear+Sceptre");
    assertThat(sr.sets().get(1).weaponKo()).isEqualTo("창 + 셉터");
    assertThat(sr.sets().get(0).weaponKo()).isEmpty(); // 옛 파일엔 없다
    assertThat(consensus(sr).roleOf(1)).isEqualTo("support");
    assertThat(consensus(sr).roleOf(2)).isEqualTo("attack");
  }

  @Test
  void 없거나_모양이_다르면_null() {
    JsonMapper m = JsonMapper.builder().build();
    assertThat(Poe2NinjaService.setRoles(m.readTree("null"))).isNull();
    assertThat(Poe2NinjaService.setRoles(m.readTree("{\"n\":3}"))).isNull();
    assertThat(consensus(null).roleOf(1)).isNull();
  }

  @Test
  void 역할마다_다른_세트_목표() {
    assertThat(Poe2RefineService.roleObjective("attack")).isEqualTo("dps");
    assertThat(Poe2RefineService.roleObjective("defense")).isEqualTo("ehp");
    assertThat(Poe2RefineService.roleObjective("support")).isEqualTo("balanced");
    assertThat(Poe2RefineService.roleObjective(null)).isEqualTo("balanced");
  }

  @Test
  void 무기_칸_변경의_세트() {
    assertThat(
            Poe2RefineService.changeWeaponSet(
                "{\"type\":\"unique\",\"slot\":\"Weapon 1\",\"raw\":\"x\"}"))
        .isEqualTo(1);
    assertThat(
            Poe2RefineService.changeWeaponSet("{\"type\":\"unique\",\"slot\":\"Weapon 2 Swap\"}"))
        .isEqualTo(2);
    assertThat(
            Poe2RefineService.changeWeaponSet(
                "{\"type\": \"unique\", \"slot\": \"Weapon 1 Swap\"}"))
        .isEqualTo(2);
    assertThat(Poe2RefineService.changeWeaponSet("{\"type\":\"unique\",\"slot\":\"Helmet\"}"))
        .isZero();
    assertThat(Poe2RefineService.changeWeaponSet("{\"type\":\"gem\",\"from\":\"a\"}")).isZero();
    assertThat(Poe2RefineService.changeWeaponSet(null)).isZero();
  }

  @Test
  void 주_세트는_고른_목표_다른_세트는_역할_목표() {
    Poe2NinjaService.SetRoles sr =
        Poe2NinjaService.setRoles(JsonMapper.builder().build().readTree(JSON));
    List<Poe2SimService.SetRoleView> v = Poe2SimService.setRoleViews(consensus(sr), 2, "ehp");
    assertThat(v).hasSize(2);
    assertThat(v.get(0).main()).isFalse();
    assertThat(v.get(0).objective()).isEqualTo("balanced"); // 지원 세트
    assertThat(v.get(1).main()).isTrue();
    assertThat(v.get(1).objective()).isEqualTo("ehp"); // 주 세트는 사용자가 고른 목표 그대로
    assertThat(v.get(1).n()).isEqualTo(8);
    // 결과가 세트를 안 나누면(mainSet null) 싣지 않는다
    assertThat(Poe2SimService.setRoleViews(consensus(sr), null, "dps")).isNull();
    assertThat(Poe2SimService.setRoleViews(consensus(null), 1, "dps")).isNull();
  }
}
