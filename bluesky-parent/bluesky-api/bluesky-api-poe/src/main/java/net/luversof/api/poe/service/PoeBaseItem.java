package net.luversof.api.poe.service;

import java.util.List;

/** tools/poe-extract parse-items.mjs 가 생성한 일반(베이스) 아이템 (base-items.json). */
public record PoeBaseItem(
    String name,
    String nameKo,
    String slug,
    String itemClass,
    String itemClassKo,
    /** 게임 영어 분류 이름(ItemClasses.Name — "Hybrid Flasks"). 옛 데이터면 null. */
    String itemClassName,
    String category,
    int dropLevel,
    int reqStr,
    int reqDex,
    int reqInt,
    Armour armour,
    Weapon weapon,
    Flask flask,
    List<ModLine> implicits) {

  public record Armour(
      int armourMin,
      int armourMax,
      int evasionMin,
      int evasionMax,
      int energyShieldMin,
      int energyShieldMax,
      int wardMin,
      int wardMax,
      int block) {}

  public record Weapon(
      int damageMin, int damageMax, double critChance, double attacksPerSecond, int range) {}

  /** 플라스크 속성 — type 1=생명 2=마나 3=하이브리드 4=특수, durationSeconds=회복/지속(초). */
  public record Flask(
      int type,
      int lifePerUse,
      int manaPerUse,
      double durationSeconds,
      int maxCharges,
      int perCharge,
      List<ModLine> buffLines) {}

  /** 암시 줄 하나 — reminders(Ko) 는 그 줄 밑 인게임 회색 부연(10-04 C116), 없으면 null. */
  public record ModLine(String en, String ko, List<String> reminders, List<String> remindersKo) {}
}
