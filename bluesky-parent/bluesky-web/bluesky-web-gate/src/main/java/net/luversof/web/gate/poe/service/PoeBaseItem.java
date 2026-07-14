package net.luversof.web.gate.poe.service;

import java.util.List;

/** tools/poe-extract parse-items.mjs 가 생성한 일반(베이스) 아이템 (base-items.json). */
public record PoeBaseItem(
    String name,
    String nameKo,
    String slug,
    String itemClass,
    String itemClassKo,
    String category,
    int dropLevel,
    int reqStr,
    int reqDex,
    int reqInt,
    Armour armour,
    Weapon weapon,
    List<ModLine> implicits) {

  public record Armour(
      int armourMin,
      int armourMax,
      int evasionMin,
      int evasionMax,
      int energyShieldMin,
      int energyShieldMax,
      int wardMin,
      int wardMax) {}

  public record Weapon(
      int damageMin, int damageMax, double critChance, double attacksPerSecond, int range) {}

  public record ModLine(String en, String ko) {}
}
