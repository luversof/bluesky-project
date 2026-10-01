package net.luversof.api.poe.poe2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PoE2 무기 세트(1·2) — PoB-PoE2 는 저장된 빌드의 "지금 켜진 세트"(ItemSet useSecondWeaponSet) 하나로만 계산한다. 세트별 패시브
 * (WeaponSet1/2 노드, allocMode 1·2)와 세트별 무기는 그 세트가 켜졌을 때만 적용된다(CalcSetup "Condition:WeaponSet1/2").
 *
 * <p>실빌드 60개 실측(10-01): 33개가 세트마다 패시브를 따로 찍었고(각 ~24점) 42개가 교체 무기를 들었으며, 18개는 세트 2가 켜진 채 저장됐다 — 세트를
 * 바꾸면 DPS 가 1.48M ↔ 213k(패스파인더 얼음 화살), EHP 가 14.2k ↔ 5.6k(스미스 오브 키타바)처럼 달라진다. 그래서 시뮬레이션·다듬기는 두 세트를
 * 각각 잰다(사용자 요청: "무기 슬롯 1, 2에 대해 시뮬레이션이 되어야 해").
 */
final class Poe2WeaponSets {

  private static final Pattern SET_NODES =
      Pattern.compile("<WeaponSet[12]\\b[^>]*\\bnodes=\"([^\"]*)\"");
  private static final Pattern SWAP_SLOT =
      Pattern.compile("<Slot\\b[^>]*\\bname=\"Weapon [12] Swap\"[^>]*>");
  private static final Pattern ITEM_ID = Pattern.compile("\\bitemId=\"(\\d+)\"");
  private static final Pattern SAVED_SECOND =
      Pattern.compile("<ItemSet\\b[^>]*\\buseSecondWeaponSet=\"true\"");

  private Poe2WeaponSets() {}

  /** 세트를 나눠 쓰는 빌드인가 — 세트별 패시브가 있거나 교체 무기 칸에 아이템이 있으면. */
  static boolean uses(String xml) {
    Matcher m = SET_NODES.matcher(xml);
    while (m.find()) {
      if (!m.group(1).isBlank()) {
        return true;
      }
    }
    Matcher s = SWAP_SLOT.matcher(xml);
    while (s.find()) {
      Matcher id = ITEM_ID.matcher(s.group());
      if (id.find() && !"0".equals(id.group(1))) {
        return true;
      }
    }
    return false;
  }

  /** 저장된 빌드에서 켜져 있던 세트(1 또는 2). */
  static int saved(String xml) {
    return SAVED_SECOND.matcher(xml).find() ? 2 : 1;
  }

  /** 세트 1 또는 2를 켠 빌드 — Items 와 모든 ItemSet 의 useSecondWeaponSet 을 바꾼다(PoB 는 활성 ItemSet 의 값을 쓴다). */
  static String withSet(String xml, int set) {
    String v = set == 2 ? "true" : "false";
    String out =
        xml.replaceAll(
            "(<Items\\b[^>]*?\\buseSecondWeaponSet=\")(true|false)(\")", "$1" + v + "$3");
    out =
        out.replaceAll(
            "(<ItemSet\\b[^>]*?\\buseSecondWeaponSet=\")(true|false)(\")", "$1" + v + "$3");
    return out;
  }
}
