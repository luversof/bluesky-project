package net.luversof.web.gate.poe2;

import java.util.List;

import net.luversof.web.gate.poe2.dto.Poe2;

/** PoE2 템플릿 도우미 — JTE 안에선 메서드를 정의할 수 없어 여기 둔다. */
public final class Poe2View {

  private Poe2View() {}

  /** null 이면 0. */
  public static int nz(Integer v) {
    return v == null ? 0 : v;
  }

  /**
   * 키워드 정의 → data 속성용 JSON [{"t":용어,"d":정의}] — 로케일에 맞는 말로(10-04 C126, 트리 꽂은 주얼 칸 Alt 설명). 없으면 빈
   * 문자열(null 은 "고유 것을 쓴다"는 뜻이라 변형 칸에선 빈 문자열과 구분해 그대로 둔다).
   */
  public static String keywordData(List<Poe2.Keyword> keywords) {
    if (keywords == null) {
      return "";
    }
    StringBuilder out = new StringBuilder("[");
    for (Poe2.Keyword k : keywords) {
      if (out.length() > 1) {
        out.append(',');
      }
      out.append("{\"t\":");
      jsonString(out, net.luversof.web.gate.poe.PoeText.name(k.termKo(), k.term()));
      out.append(",\"d\":");
      jsonString(out, net.luversof.web.gate.poe.PoeText.name(k.defKo(), k.def()));
      out.append('}');
    }
    return out.append(']').toString();
  }

  private static void jsonString(StringBuilder out, String s) {
    out.append('"');
    for (char c : (s == null ? "" : s).toCharArray()) {
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
  }

  /** 베이스 암시 옵션 → 표시 줄(한국어 화면이면 한국어, 없으면 영문). */
  public static List<String> implicitLines(Poe2.BaseItem base) {
    if (base == null || base.implicits() == null) {
      return List.of();
    }
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    return base.implicits().stream()
        .map(l -> ko && l.ko() != null && !l.ko().isBlank() ? l.ko() : l.en())
        .toList();
  }

  /**
   * 베이스 목록 카드 요약 수치 — PoE1 베이스 목록(poe/htmx/itemList.jte)과 같은 줄(10-01). 방어구 = 막기 · AR · EV · ES, 무기
   * = 피해 · 초당 공격 · 치명타 · 물리 DPS(평균 피해 × 초당 공격), 플라스크 = 회복량 · 시간. 0 인 값은 뺀다.
   */
  public static String cardSummary(Poe2.BaseItem b) {
    if (b == null) {
      return "";
    }
    boolean ko = net.luversof.web.gate.poe.PoeText.isKorean();
    StringBuilder sb = new StringBuilder();
    Poe2.Armour a = b.armour();
    if (a != null) {
      if (nz(a.block()) > 0) {
        sb.append(ko ? "막기 " : "Block ").append(a.block()).append("%  ");
      }
      if (nz(a.armour()) > 0) {
        sb.append("AR ").append(a.armour()).append("  ");
      }
      if (nz(a.evasion()) > 0) {
        sb.append("EV ").append(a.evasion()).append("  ");
      }
      if (nz(a.energyShield()) > 0) {
        sb.append("ES ").append(a.energyShield()).append("  ");
      }
    }
    Poe2.Weapon w = b.weapon();
    if (w != null && nz(w.damageMax()) > 0) {
      double aps = w.attacksPerSecond() == null ? 0 : w.attacksPerSecond();
      double pdps = (nz(w.damageMin()) + nz(w.damageMax())) / 2.0 * aps;
      sb.append(nz(w.damageMin())).append("-").append(w.damageMax());
      sb.append(" · ").append(String.format(java.util.Locale.ROOT, "%.2f", aps)).append("/s");
      if (w.critChance() != null) {
        sb.append(" · ")
            .append(String.format(java.util.Locale.ROOT, "%.2f", w.critChance()))
            .append("%");
      }
      sb.append(" · ").append(Math.round(pdps)).append(" DPS");
    }
    Poe2.Flask f = b.flask();
    if (f != null) {
      if (nz(f.lifePerUse()) > 0) {
        sb.append(ko ? "생명력 " : "Life ").append(f.lifePerUse()).append("  ");
      }
      if (nz(f.manaPerUse()) > 0) {
        sb.append(ko ? "마나 " : "Mana ").append(f.manaPerUse()).append("  ");
      }
      if (f.recoverySeconds() != null && f.recoverySeconds() > 0) {
        sb.append(String.format(java.util.Locale.ROOT, "%.1f", f.recoverySeconds()))
            .append(ko ? "초" : "s");
      }
    }
    return sb.toString().trim();
  }

  /** 옵션 한 단계의 표시 줄. */
  public static List<String> tierLines(Poe2.ModTier t) {
    return net.luversof.web.gate.poe.PoeText.lines(t.textKo(), t.text());
  }

  /** PoB 장비 칸 이름 → 한국어(화면이 한국어일 때). 모르는 칸은 그대로. */
  public static String slotName(String slot) {
    if (slot == null || !net.luversof.web.gate.poe.PoeText.isKorean()) {
      return slot;
    }
    String s =
        // 무기 칸은 번호가 뜻이다(1 = 주무기, 2 = 보조) — 번호를 떼고 고르면 "Weapon 2" 도 "무기" 가 됐다
        switch (slot.startsWith("Weapon") ? slot : slot.replaceAll(" \\d+$", "")) {
          case "Weapon", "Weapon 1" -> "무기";
          case "Weapon 2" -> "보조 무기";
          case "Weapon 1 Swap" -> "무기(교체)";
          case "Weapon 2 Swap" -> "보조 무기(교체)";
          case "Helmet" -> "투구";
          case "Body Armour" -> "갑옷";
          case "Gloves" -> "장갑";
          case "Boots" -> "장화";
          case "Amulet" -> "목걸이";
          case "Ring" -> "반지";
          case "Belt" -> "허리띠";
          case "Flask" -> "플라스크";
          case "Charm" -> "호신부";
          default -> null;
        };
    if (s == null) {
      return slot;
    }
    java.util.regex.Matcher m = java.util.regex.Pattern.compile(" (\\d+)$").matcher(slot);
    return m.find() && !slot.startsWith("Weapon") ? s + " " + m.group(1) : s;
  }

  /** 증강물 효과를 아이템 클래스로 거를 때 — API 와 같은 규칙(분류 대상 클래스, 없으면 분류 이름). */
  public static boolean appliesTo(Poe2.AugmentEffect e, String itemClass) {
    if (itemClass == null || itemClass.isBlank()) {
      return true;
    }
    if (e.targetClasses() != null && !e.targetClasses().isEmpty()) {
      return e.targetClasses().contains(itemClass);
    }
    String t = e.target() == null ? "" : e.target();
    List<String> armour =
        List.of("Helmet", "Body Armour", "Gloves", "Boots", "Shield", "Buckler", "Focus");
    List<String> nonMartial =
        List.of(
            "Helmet",
            "Body Armour",
            "Gloves",
            "Boots",
            "Shield",
            "Buckler",
            "Focus",
            "Wand",
            "Staff",
            "Sceptre",
            "Amulet",
            "Ring",
            "Belt");
    return switch (t) {
      case "All" -> true;
      case "Armour" -> armour.contains(itemClass);
      case "Martial Weapon" -> !nonMartial.contains(itemClass);
      default -> t.equals(itemClass);
    };
  }
}
