package net.luversof.web.gate.poe;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.luversof.web.gate.poe.dto.PoeBaseItem;
import net.luversof.web.gate.poe.dto.PoeUniqueItem;
import net.luversof.web.gate.poe.dto.PoeUniqueVariant;

/**
 * 고유 상세가 조인할 베이스 — 변형마다 베이스가 다른 고유는 고른 변형의 베이스(10-09 C209).
 *
 * <p>PoB 는 그런 변형의 이름을 베이스 이름으로 쓴다: 광기의 신호 "Two-Toned Boots (Armour/Evasion)" 3종, 선구자의 상징 "Sapphire
 * Ring" · "Ruby Ring" 등. 예전엔 기본 변형 베이스 하나로만 조인해 다른 변형을 골라도 속성 · 요구 사항 · 베이스 이름이 기본 것 그대로였다. 변형 이름이
 * 우연히 다른 분류의 베이스 이름과 같은 경우(모조품 용송곳니의 비행 "Energy Blade" = 스킬 이름)는 아이템 분류가 다르므로 거른다.
 */
public final class PoeUniqueBase {

  private PoeUniqueBase() {}

  public static PoeBaseItem resolve(
      PoeUniqueItem item, Integer variant, Function<String, PoeBaseItem> lookup) {
    PoeBaseItem base = lookup.apply(item.baseType());
    if (variant == null || item.variants() == null || base == null) {
      return base;
    }
    for (PoeUniqueVariant v : item.variants()) {
      if (v.index() != variant || v.name() == null || v.name().equals(item.baseType())) {
        continue;
      }
      PoeBaseItem picked = lookup.apply(v.name());
      if (picked != null
          && picked.itemClass() != null
          && picked.itemClass().equals(base.itemClass())) {
        return picked;
      }
    }
    return base;
  }

  /**
   * 고유 암시 줄의 표시 문장 — 영어가 베이스 암시와 같으면 베이스의 한국어(10-09 C219). 베이스 문장은 실제 스탯으로 서술한 것이고, 고유 번역기는 뼈대가 같은
   * 다른 스탯 설명을 먼저 집을 수 있다(스태프 "+25% Chance to Block Attack Damage" → "+25%의 확률로 공격 피해를 막아냄", 인게임 ·
   * poedb 는 "공격 피해 막기 확률 +25%", 39줄).
   */
  public static java.util.List<String> implicitLines(
      java.util.List<String> shown, java.util.List<String> en, PoeBaseItem base, boolean korean) {
    if (!korean
        || shown == null
        || en == null
        || base == null
        || base.implicits() == null
        || shown.size() != en.size()) {
      return shown;
    }
    java.util.Map<String, String> ko = new java.util.HashMap<>();
    for (PoeBaseItem.ModLine m : base.implicits()) {
      if (m.en() != null && m.ko() != null) {
        ko.putIfAbsent(m.en(), m.ko());
      }
    }
    java.util.List<String> out = new java.util.ArrayList<>(shown);
    for (int i = 0; i < en.size(); i++) {
      String k = ko.get(en.get(i));
      if (k != null) {
        out.set(i, k);
      }
    }
    return out;
  }

  /**
   * 이 고유가 이 베이스로 나오는가 — 베이스 상세의 "이 베이스의 고유" 목록용(10-09 C209). 기본 베이스 또는 변형 베이스 이름이 같으면 참. PoB 표기의 발음
   * 구별 기호("Maelstrom") · 방어 유형 꼬리("Two-Toned Boots (Armour/Evasion)")도 게임 베이스와 맞춘다(API findByName 과
   * 같은 규칙).
   */
  public static boolean matches(PoeUniqueItem u, PoeBaseItem item) {
    if (u.baseType() != null && sameBase(u.baseType(), item)) {
      return true;
    }
    if (u.variants() != null) {
      for (PoeUniqueVariant v : u.variants()) {
        if (v.name() != null && sameBase(v.name(), item)) {
          return true;
        }
      }
    }
    return false;
  }

  /** "이름 (Armour/Evasion)" · "이름 (Cold/Lightning)" — 같은 이름 베이스(두 가지 색 장화 · 쌍보석 반지 3종)를 가르는 꼬리. */
  private static final Pattern VARIANT_SUFFIX =
      Pattern.compile("(.+?) \\(([A-Za-z ]+(?:/[A-Za-z ]+)+)\\)");

  private static final Set<String> DEFENCES = Set.of("Armour", "Evasion", "Energy Shield");

  static boolean sameBase(String pobName, PoeBaseItem item) {
    if (pobName.equalsIgnoreCase(item.name())) {
      return true;
    }
    Matcher m = VARIANT_SUFFIX.matcher(pobName);
    String bare = m.matches() ? m.group(1) : pobName;
    if (!fold(bare).equals(fold(item.name()))) {
      return false;
    }
    if (!m.matches()) {
      return true;
    }
    Set<String> words = Set.of(m.group(2).split("/"));
    if (!DEFENCES.containsAll(words)) {
      // 원소 꼬리 — 암시 옵션 영어 줄에 낱말이 다 들어 있는 베이스("+(12-16)% to Cold and Lightning Resistances")
      String implicits =
          item.implicits() == null
              ? ""
              : String.join(" ", item.implicits().stream().map(PoeBaseItem.ModLine::en).toList())
                  .toLowerCase(Locale.ROOT);
      return words.stream().allMatch(w -> implicits.contains(w.toLowerCase(Locale.ROOT)));
    }
    PoeBaseItem.Armour a = item.armour();
    Set<String> have = new HashSet<>();
    if (a != null && a.armourMax() > 0) {
      have.add("Armour");
    }
    if (a != null && a.evasionMax() > 0) {
      have.add("Evasion");
    }
    if (a != null && a.energyShieldMax() > 0) {
      have.add("Energy Shield");
    }
    return have.equals(words);
  }

  private static String fold(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
