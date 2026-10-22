package net.luversof.web.gate.poe2.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import net.luversof.web.gate.poe2.dto.Poe2Craft;
import net.luversof.web.gate.poe2.httpexchange.Poe2CraftClient;

/**
 * PoE2 계산기 목표의 쪽 표시(10-21 C552) — 전략 모의가 계산기와 같은 해석(calcTargets)을 쓰고 이 표시로 접두 · 접미 목표를 고른다. 묶음 · 빈
 * 자리 목표는 표시가 없고(null) 그 뒤 목표의 쪽이 밀리면 안 된다. 묶음 · 빈 자리 해석은 PoE1 과 공용(CraftTargetParams).
 */
class Poe2CraftTargetSidesTest {

  private static Poe2Craft.PoolLine line(String id, String gen, String family, int rank) {
    return new Poe2Craft.PoolLine(
        id, gen, id, id, List.of(id), List.of(id), rank, 1, 0.1, family, List.of(), List.of());
  }

  private static Poe2CraftController controller() {
    List<Poe2Craft.PoolLine> pool =
        List.of(
            line("Life1", "prefix", "Life", 1),
            line("Life2", "prefix", "Life", 2),
            line("Mana1", "prefix", "Mana", 1),
            line("Armour1", "prefix", "Armour", 1),
            line("Cold1", "suffix", "ColdRes", 1),
            line("Cold2", "suffix", "ColdRes", 2));
    List<Poe2Craft.PoolLine> dese = List.of(line("DeseSpeed", "suffix", "Speed", 1));
    Poe2CraftClient client =
        (Poe2CraftClient)
            Proxy.newProxyInstance(
                Poe2CraftClient.class.getClassLoader(),
                new Class<?>[] {Poe2CraftClient.class},
                (proxy, method, args) -> {
                  if (method.isDefault()) {
                    return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                  }
                  if ("pool".equals(method.getName())) {
                    return pool;
                  }
                  if ("desecrated".equals(method.getName())) {
                    return dese;
                  }
                  throw new UnsupportedOperationException(method.getName());
                });
    return new Poe2CraftController(client, null);
  }

  @Test
  void groupAndOpenTargetsDoNotShiftLaterSides() {
    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("t_grp:prefix:Life", "1");
    params.add("t_grp:prefix:Mana", "1");
    params.add("t_grpk", "2");
    params.add("t_open:suffix", "1");
    params.add("t_prefix:Armour", "1");
    params.add("t_suffix:ColdRes", "2");
    params.add("t_dese:DeseSpeed", "1");
    List<String> labels = new ArrayList<>();
    List<String> gens = new ArrayList<>();
    List<Poe2Craft.Target> targets = controller().calcTargets("gloves", 82, params, labels, gens);
    // 묶음 2개 · 빈 접미 1개(표시 없음) · 갑옷(접두) · 냉기 저항(접미) · 훼손(접미)
    assertThat(targets).hasSize(6);
    assertThat(gens).containsExactly(null, null, null, "prefix", "suffix", "suffix");
    assertThat(targets.get(0).anyOf()).containsExactlyInAnyOrder("Life1", "Life2", "Mana1");
    assertThat(targets.get(2).anyOf()).containsExactly("@open:suffix");
    assertThat(targets.get(4).anyOf()).containsExactly("Cold1", "Cold2");
    // 이름은 묶음 하나 · 빈 자리 하나 · 나머지 셋
    assertThat(labels).hasSize(5);
  }

  @Test
  void gensOptional() {
    LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("t_prefix:Life", "2");
    List<Poe2Craft.Target> targets =
        controller().calcTargets("gloves", 82, params, new ArrayList<>(), null);
    assertThat(targets).hasSize(1);
    assertThat(targets.get(0).anyOf()).containsExactly("Life1", "Life2");
  }
}
