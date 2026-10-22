package net.luversof.web.gate.poe.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import net.luversof.web.gate.poe.dto.PoeCraft;
import net.luversof.web.gate.poe.httpexchange.PoeCraftClient;

/**
 * 계산기 목표의 쪽 표시(10-21 C550) — 전략 모의가 이 표시로 접두 · 접미 목표를 고른다. 묶음 · 빈 자리 목표는 목표 여럿에 표시가 없어(null) 그 뒤
 * 목표의 쪽이 밀리면 안 된다(정리 전 전략 쪽 해석은 묶음 뒤 접두/접미 번호가 한 칸씩 어긋났다).
 */
class PoeCraftTargetSidesTest {

  private static PoeCraft.PoolLine line(String id, String gen, String family, int rank) {
    return new PoeCraft.PoolLine(
        id,
        gen,
        family,
        id,
        id,
        List.of(id),
        List.of(id),
        List.of(id),
        List.of(id),
        rank,
        1,
        1000,
        0.1,
        List.of(),
        List.of());
  }

  private static PoeCraftController controller() {
    List<PoeCraft.PoolLine> pool =
        List.of(
            line("Life1", "prefix", "Life", 1),
            line("Life2", "prefix", "Life", 2),
            line("Mana1", "prefix", "Mana", 1),
            line("Armour1", "prefix", "Armour", 1),
            line("Cold1", "suffix", "ColdRes", 1),
            line("Cold2", "suffix", "ColdRes", 2));
    PoeCraftClient client =
        (PoeCraftClient)
            Proxy.newProxyInstance(
                PoeCraftClient.class.getClassLoader(),
                new Class<?>[] {PoeCraftClient.class},
                (proxy, method, args) -> {
                  if ("pool".equals(method.getName())) {
                    return pool;
                  }
                  if ("calcExtras".equals(method.getName())) {
                    return extras();
                  }
                  throw new UnsupportedOperationException(method.getName());
                });
    return new PoeCraftController(client, null, null);
  }

  /** 에센스 전용 옵션(C551) — 공포 등급 5~7. */
  private static PoeCraft.CalcExtras extras() {
    List<PoeCraft.EssenceTier> tiers = new ArrayList<>();
    for (int t = 5; t <= 7; t++) {
      tiers.add(
          new PoeCraft.EssenceTier(
              t, "MinionDamage" + t, "Fear " + t, "공포 " + t, List.of("m" + t), List.of("m" + t)));
    }
    return new PoeCraft.CalcExtras(
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        null,
        List.of(new PoeCraft.EssenceOnly("Fear", "prefix", null, List.of(), List.of(), tiers)),
        null);
  }

  private static PoeCraft.Item item() {
    return new PoeCraft.Item("Gloves", "str_armour", "", 84, "NORMAL", List.of(), false);
  }

  @Test
  void groupTargetsDoNotShiftLaterSides() {
    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("t_grp:prefix:Life", "1");
    params.add("t_grp:prefix:Mana", "1");
    params.add("t_grpk", "2");
    params.add("t_prefix:Armour", "1");
    params.add("t_suffix:ColdRes", "2");
    List<String> labels = new ArrayList<>();
    List<String> gens = new ArrayList<>();
    List<PoeCraft.Target> targets =
        controller().calcTargets("Gloves", "str_armour", item(), params, labels, gens, true);
    // 묶음 2개(표시 없음) · 갑옷(접두) · 냉기 저항(접미)
    assertThat(targets).hasSize(4);
    assertThat(gens).containsExactly(null, null, "prefix", "suffix");
    assertThat(targets.get(2).anyOf()).containsExactly("Armour1");
    assertThat(targets.get(3).anyOf()).containsExactly("Cold1", "Cold2");
    // 이름은 묶음 하나에 하나
    assertThat(labels).hasSize(3);
  }

  @Test
  void openSlotsHaveNoSide() {
    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("t_open:prefix", "2");
    params.add("t_suffix:ColdRes", "1");
    List<String> gens = new ArrayList<>();
    List<PoeCraft.Target> targets =
        controller()
            .calcTargets("Gloves", "str_armour", item(), params, new ArrayList<>(), gens, true);
    assertThat(targets).hasSize(3);
    assertThat(gens).containsExactly(null, null, "suffix");
  }

  @Test
  void gensOptional() {
    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("t_prefix:Life", "2");
    List<PoeCraft.Target> targets =
        controller()
            .calcTargets("Gloves", "str_armour", item(), params, new ArrayList<>(), null, false);
    assertThat(targets).hasSize(1);
    assertThat(targets.get(0).anyOf()).containsExactly("Life1", "Life2");
  }

  @Test
  void essenceOnlyTargetUsesTiersAtOrAbove() {
    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("t_ess:Fear", "6");
    params.add("t_suffix:ColdRes", "1");
    List<String> labels = new ArrayList<>();
    List<String> gens = new ArrayList<>();
    List<PoeCraft.Target> targets =
        controller().calcTargets("Gloves", "str_armour", item(), params, labels, gens, true);
    assertThat(targets).hasSize(2);
    assertThat(targets.get(0).anyOf()).containsExactly("MinionDamage6", "MinionDamage7");
    assertThat(gens).containsExactly("prefix", "suffix");
    assertThat(labels.get(0)).contains("m6");
  }
}
