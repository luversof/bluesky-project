package net.luversof.api.poe.poe2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import net.luversof.api.poe.service.PoePobImportService;

/**
 * 빌드 화면 업그레이드 가이드의 "닌자 공통 특성 중 이 빌드에 없는 것"(10-14 C318, 사용자 요청 "빌드에서는 PoB 코드를 가져오면 가져온 빌드에 대해 개선할 점을
 * 알려주는 업그레이드 가이드" + "닌자의 해당 직업/스킬의 특성을 참고"). 가져온 빌드의 전직 · 주 스킬 아키타입 공통
 * 특성(Poe2NinjaService.consensus)과 견줘, 실빌드 절반 이상이 쓰는데 없는 보조젬 · 칸별 고유, 4분의 3 이상이 찍는데 없는 본 트리 특화 · 핵심을
 * 인원과 함께 돌려준다. 엔진 계산 없음(즉시) — 효과는 가이드 표 · 다듬기가 잰다.
 */
@Service
public class Poe2CommonGapsService {

  /** 한 줄 — name 영문 · nameKo 한국어 · slot 장비 칸(고유만) · count 쓰는 실빌드 수. */
  public record Gap(String name, String nameKo, String slot, int count) {}

  /**
   * n = 공통 특성 실빌드 수(0 이면 그 아키타입은 집계 대상이 아님 — 나머지는 빈 목록), ascendancy · skill = 가져온 빌드에서 읽은 전직 · 주
   * 스킬(영문).
   */
  public record CommonGaps(
      int n,
      String ascendancy,
      String ascendancyKo,
      String skill,
      String skillKo,
      List<Gap> supports,
      List<Gap> uniques,
      List<Gap> nodes) {}

  private static final Pattern BUILD = Pattern.compile("<Build\\b[^>]*>");
  private static final Pattern ATTR_ASC = Pattern.compile("\\bascendClassName=\"([^\"]*)\"");
  private static final Pattern ATTR_MAIN = Pattern.compile("\\bmainSocketGroup=\"(\\d+)\"");
  private static final Pattern SKILL = Pattern.compile("<Skill\\b([^>]*)>([\\s\\S]*?)</Skill>");
  private static final Pattern MAIN_ACTIVE = Pattern.compile("\\bmainActiveSkill=\"(\\d+)\"");
  private static final Pattern GEM = Pattern.compile("<Gem\\b[^>]*>");
  private static final Pattern NAME_SPEC = Pattern.compile("\\bnameSpec=\"([^\"]*)\"");
  private static final Pattern GEM_ID = Pattern.compile("\\bgemId=\"([^\"]*)\"");
  private static final Pattern ENABLED = Pattern.compile("\\benabled=\"false\"");
  private static final Pattern SPEC_NODES = Pattern.compile("<Spec\\b[^>]*\\bnodes=\"([^\"]*)\"");

  private final PoePobImportService decoder;
  private final Poe2NinjaService ninja;
  private final Poe2DataService data;

  public Poe2CommonGapsService(
      PoePobImportService decoder, Poe2NinjaService ninja, Poe2DataService data) {
    this.decoder = decoder;
    this.ninja = ninja;
    this.data = data;
  }

  public CommonGaps gaps(String code) {
    return gapsFromXml(decoder.decodeToXml(code.trim()));
  }

  CommonGaps gapsFromXml(String rawXml) {
    String xml = Poe2RefineService.unescapeXml(rawXml);
    Matcher b = BUILD.matcher(xml);
    String build = b.find() ? b.group() : "";
    Matcher a = ATTR_ASC.matcher(build);
    String asc = a.find() ? a.group(1) : "";
    Matcher m = ATTR_MAIN.matcher(build);
    int mainIdx = m.find() ? Integer.parseInt(m.group(1)) - 1 : 0;
    String skill = null;
    Matcher s = SKILL.matcher(xml);
    for (int i = 0; s.find(); i++) {
      if (i != mainIdx) {
        continue;
      }
      // 그룹 안 몇 번째 액티브 젬이 주 스킬인지(mainActiveSkill) — 메타 젬 묶음("원소 상태 이상 시 시전" + 혜성)이면 첫 액티브는 메타
      // 젬이다(10-14 탐침이 잡음)
      Matcher ma = MAIN_ACTIVE.matcher(s.group(1));
      int want = ma.find() ? Integer.parseInt(ma.group(1)) : 1;
      int seen = 0;
      Matcher g = GEM.matcher(s.group(2));
      while (g.find()) {
        String tag = g.group();
        Matcher id = GEM_ID.matcher(tag);
        Matcher nm = NAME_SPEC.matcher(tag);
        if (ENABLED.matcher(tag).find() || !nm.find()) {
          continue;
        }
        if (!(id.find() && id.group(1).contains("Support")) && ++seen == want) {
          skill = nm.group(1);
          break;
        }
      }
      break;
    }
    String ascKo = asc.isEmpty() ? null : data.treeIndex().ascendancyKo().get(asc);
    String skillKo =
        skill == null ? null : data.gemByName(skill).map(Poe2.Gem::nameKo).orElse(null);
    Poe2NinjaService.Consensus c = skill == null ? null : ninja.consensus(asc, skill);
    if (c == null || c.n() < 2) {
      return new CommonGaps(0, asc, ascKo, skill, skillKo, List.of(), List.of(), List.of());
    }
    List<Gap> supports = new ArrayList<>();
    for (Map.Entry<String, Integer> e : c.supports().entrySet()) {
      if (e.getValue() >= c.n() * Poe2RefineService.COMMON_SHARE
          && !xml.contains("nameSpec=\"" + e.getKey() + "\"")) {
        supports.add(
            new Gap(
                e.getKey(),
                data.gemByName(e.getKey()).map(Poe2.Gem::nameKo).orElse(null),
                null,
                e.getValue()));
      }
    }
    List<Gap> uniques = new ArrayList<>();
    Set<String> seenUnique = new HashSet<>();
    for (Map.Entry<String, Map<String, Integer>> sl : c.slotUniques().entrySet()) {
      sl.getValue().entrySet().stream()
          .max(Map.Entry.comparingByValue())
          .filter(t -> t.getValue() >= c.n() * Poe2RefineService.COMMON_SHARE)
          .filter(t -> !Poe2RefineService.equipsUnique(xml, t.getKey()))
          .filter(t -> seenUnique.add(t.getKey())) // 교체 무기 칸 등 같은 고유가 두 칸에 잡히면 한 번만
          .ifPresent(
              t ->
                  uniques.add(
                      new Gap(
                          t.getKey(),
                          data.uniqueByName(t.getKey()).map(Poe2.Unique::nameKo).orElse(null),
                          sl.getKey(),
                          t.getValue())));
    }
    Set<Integer> mine = new HashSet<>();
    Matcher sn = SPEC_NODES.matcher(xml);
    if (sn.find()) {
      for (String id : sn.group(1).split(",")) {
        if (!id.isBlank()) {
          mine.add(Integer.valueOf(id.trim()));
        }
      }
    }
    List<Gap> nodes = new ArrayList<>();
    if (c.nodeCounts() != null) {
      c.nodeCounts().entrySet().stream()
          .filter(e -> e.getValue() >= c.n() * Poe2RefineService.COMMON_NODE_SHARE)
          .filter(e -> !mine.contains(e.getKey()))
          .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
          .forEach(
              e -> {
                Poe2DataService.TreeNodeLite t = data.treeIndex().nodes().get(e.getKey());
                if (t != null
                    && (t.ascendancy() == null || t.ascendancy().isBlank())
                    && ("notable".equals(t.kind()) || "keystone".equals(t.kind()))) {
                  nodes.add(new Gap(t.name(), t.nameKo(), null, e.getValue()));
                }
              });
    }
    supports.sort((x, y) -> Integer.compare(y.count(), x.count()));
    uniques.sort((x, y) -> Integer.compare(y.count(), x.count()));
    return new CommonGaps(c.n(), asc, ascKo, skill, skillKo, supports, uniques, nodes);
  }
}
