package net.luversof.api.poe.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * 임포트한 빌드의 영문 모드 라인 → 한국어. 모드 풀(en/ko 티어 라인)에서 숫자를 자리표시(§)로 정규화한 사전을 만들어, 들어온 영문 라인을 같은 방식으로
 * 정규화·매칭한 뒤 실제 롤 숫자를 한국어 템플릿에 되꽂는다. 매칭 실패 시 영문 그대로.
 *
 * <p>PoB 아이템 텍스트는 GGG 스탯 설명 기반이라 모드 풀의 en 과 대부분 일치한다. 숫자 개수/순서가 en/ko 에서 같다는 가정.
 */
@Service
public class PoeModTranslateService {

  private static final Logger logger = LoggerFactory.getLogger(PoeModTranslateService.class);
  private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");
  private static final String SLOT = "§"; // §

  private final PoeModPoolDataService poeModPoolDataService;
  private final PoeModDataService poeModDataService;
  private volatile Map<String, String> dictionary = Map.of();
  // 정규화 en → 리마인더 Id(전체 풀 mods.json 의 tier.rem, 10-04 C131) — 빌드 화면 레어 아이템 툴팁의 회색 부연
  private volatile Map<String, List<String>> reminderIds = Map.of();

  private final PoeTreeGraphService poeTreeGraphService;

  public PoeModTranslateService(
      PoeModPoolDataService poeModPoolDataService,
      PoeModDataService poeModDataService,
      PoeTreeGraphService poeTreeGraphService) {
    this.poeModPoolDataService = poeModPoolDataService;
    this.poeModDataService = poeModDataService;
    this.poeTreeGraphService = poeTreeGraphService;
  }

  // "Allocates 노터블"(도유 아뮬렛 등) — 이름이 바뀌는 자리라 § 사전으로 못 옮긴다. 게임 문구는 "X 할당"(고유 데이터 · 금단 줄과 같은 어순, 10-04
  // C135)
  private static final Pattern ALLOCATES = Pattern.compile("^Allocates (\\D.*)$");
  private static final Pattern RANGE = Pattern.compile("\\(-?\\d+(?:\\.\\d+)?-\\d+(?:\\.\\d+)?\\)");
  private static final Pattern RANGE_OR_NUMBER =
      Pattern.compile("\\(-?\\d+(?:\\.\\d+)?--?\\d+(?:\\.\\d+)?\\)|-?\\d+(?:\\.\\d+)?");

  private String allocates(String enLine) {
    Matcher m = ALLOCATES.matcher(enLine);
    if (!m.matches() || !poeTreeGraphService.hasData()) {
      return null;
    }
    String name = m.group(1);
    for (PoeTreeGraphService.TreeNode n : poeTreeGraphService.allNodes()) {
      if (name.equalsIgnoreCase(n.name()) && n.nameKo() != null && !n.nameKo().isBlank()) {
        return n.nameKo() + " 할당";
      }
    }
    return null;
  }

  @PostConstruct
  public void build() {
    Map<String, String> map = new HashMap<>();
    Map<String, List<String>> remMap = new HashMap<>();
    // ① 큐레이션 풀(mod-pool.json) 먼저 — putIfAbsent 라 먼저 넣은 쪽이 이긴다. 검증된 번역을 유지한다.
    if (poeModPoolDataService.hasData()) {
      for (PoeModPoolDataService.ModFamily family : poeModPoolDataService.families()) {
        for (PoeModPoolDataService.ModTier tier : family.tiers()) {
          registerPairs(map, tier.en(), tier.ko());
        }
      }
    }
    int curated = map.size();
    // ② 전체 풀(mods.json)로 빈 자리를 채운다. 큐레이션 풀은 시뮬 후보만 담고 있어 플라스크·주얼 등
    //    거기 없는 모드는 임포트한 빌드에서 영문으로 남았다(실측: 매직 플라스크 모드 2줄 전부 영문).
    if (poeModDataService.hasData()) {
      for (PoeModDataService.ModFamily family : poeModDataService.allFamilies()) {
        for (PoeModDataService.ModTier tier : family.tiers()) {
          registerPairs(map, tier.en(), tier.ko());
          // 최소롤 표기도 같은 문장이라 사전에 넣어두면 롤 값이 낮은 줄까지 잡힌다
          registerPairs(map, tier.enMin(), tier.koMin());
          registerReminders(remMap, tier.en(), tier.rem());
          registerReminders(remMap, tier.enMin(), tier.rem());
        }
      }
      // ③ 풀 밖 옵션(플라스크 · 제작대 · 엘드리치 …) — 맨 마지막(putIfAbsent 라 위 번역이 이긴다, 10-04 C135)
      for (PoeModDataService.ExtraPair pair : poeModDataService.extraPairs()) {
        registerPairs(map, pair.en(), pair.ko());
        registerPairs(map, pair.enMin(), pair.koMin());
        registerReminders(remMap, pair.en(), pair.rem());
        registerReminders(remMap, pair.enMin(), pair.rem());
      }
    }
    this.dictionary = Map.copyOf(map);
    this.reminderIds = Map.copyOf(remMap);
    logger.info(
        "PoE 모드 번역 사전 구축: {}개 (큐레이션 {} + 전체 풀 보강 {})", map.size(), curated, map.size() - curated);
  }

  private void registerReminders(
      Map<String, List<String>> remMap, List<String> en, List<List<String>> rem) {
    if (en == null || rem == null) {
      return;
    }
    for (int i = 0; i < en.size() && i < rem.size(); i++) {
      if (en.get(i) == null || rem.get(i) == null || rem.get(i).isEmpty()) {
        continue;
      }
      remMap.putIfAbsent(normalize(en.get(i)).toLowerCase(Locale.ROOT), List.copyOf(rem.get(i)));
    }
  }

  /** 영문 모드 라인의 인게임 리마인더(회색 부연) 문구 — 없으면 빈 목록(10-04 C131). */
  public List<PoeReminderText> reminders(String enLine) {
    if (enLine == null || enLine.isBlank() || reminderIds.isEmpty()) {
      return List.of();
    }
    List<String> ids = reminderIds.get(normalize(enLine).toLowerCase(Locale.ROOT));
    if (ids == null) {
      return List.of();
    }
    Map<String, PoeReminderText> text = poeModDataService.reminderText();
    return ids.stream().map(text::get).filter(java.util.Objects::nonNull).toList();
  }

  private void registerPairs(Map<String, String> map, List<String> en, List<String> ko) {
    if (en == null || ko == null) {
      return;
    }
    for (int i = 0; i < en.size() && i < ko.size(); i++) {
      register(map, en.get(i), ko.get(i));
    }
  }

  private void register(Map<String, String> map, String en, String ko) {
    if (en == null || ko == null || en.isBlank() || ko.isBlank()) {
      return;
    }
    String enTemplate = normalize(en);
    String koTemplate = normalize(ko);
    // 숫자 자리 개수가 en/ko 에서 같아야 위치 치환이 안전
    if (count(enTemplate) != count(koTemplate)) {
      // 한국어에만 있는 글자 그대로의 숫자("1초마다 생명력 128 재생", "플라스크 1 충전")는 롤이 아니다 — 영문 수치와 값이 같은 숫자만 자리로(10-04
      // C135,
      //   PoE2 Poe2ModTranslator rangesOnly 와 같은 취지. 이것 때문에 재생 · 플라스크 충전 줄이 통째로 영어로 남았다)
      koTemplate = slotsMatchingValues(ko, numbers(en));
      if (count(enTemplate) != count(koTemplate)) {
        return;
      }
    }
    map.putIfAbsent(enTemplate.toLowerCase(Locale.ROOT), koTemplate);
  }

  /** ko 의 숫자 중 enValues 에 같은 값이 남아 있는 것만 § 로(하나씩 소비), 나머지는 글자 그대로. */
  private static String slotsMatchingValues(String ko, List<String> enValues) {
    List<String> remaining = new java.util.ArrayList<>(enValues);
    Matcher m = NUMBER.matcher(ko);
    StringBuilder out = new StringBuilder();
    while (m.find()) {
      String n = m.group();
      m.appendReplacement(out, remaining.remove(n) ? SLOT : Matcher.quoteReplacement(n));
    }
    m.appendTail(out);
    return out.toString();
  }

  /** 영문 모드 라인 → 한국어 (실패 시 원문 반환) */
  public String translate(String enLine) {
    if (enLine == null || enLine.isBlank() || dictionary.isEmpty()) {
      return enLine;
    }
    String template = normalize(enLine);
    String koTemplate = dictionary.get(template.toLowerCase(Locale.ROOT));
    List<String> numbers = numbers(enLine);
    if (koTemplate == null && RANGE.matcher(enLine).find()) {
      // 범위 표기 "+(25-35) to Strength"(최적화기 · 시뮬이 만든 PoB 텍스트의 고유 줄) — 범위를 자리 하나로 접어 찾고, 그 자리엔 범위를
      // 그대로(10-04 C139,
      //   PoE2 Poe2ModTranslator 와 같은 취지)
      List<String> values = new java.util.ArrayList<>();
      Matcher m = RANGE_OR_NUMBER.matcher(enLine);
      StringBuilder folded = new StringBuilder();
      while (m.find()) {
        values.add(m.group());
        m.appendReplacement(folded, SLOT);
      }
      m.appendTail(folded);
      koTemplate = dictionary.get(folded.toString().toLowerCase(Locale.ROOT));
      numbers = values;
    }
    if (koTemplate == null) {
      String allocated = allocates(enLine);
      return allocated != null ? allocated : enLine;
    }
    if (count(koTemplate) != numbers.size()) {
      return enLine;
    }
    StringBuilder result = new StringBuilder();
    int n = 0;
    for (int i = 0; i < koTemplate.length(); i++) {
      char c = koTemplate.charAt(i);
      if (SLOT.charAt(0) == c) {
        result.append(numbers.get(n++));
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  public List<String> translate(List<String> enLines) {
    if (enLines == null) {
      return null;
    }
    // PoB 는 긴 옵션 하나를 두세 줄로 쪼개 적는다("…Elemental Damage with Hits and Ailments for" / "each type of
    // …").
    //   한 줄로 못 찾으면 다음 줄과 합쳐 다시 찾고, 맞으면 첫 줄에 한국어 · 나머지 줄은 빈 문자열(길이 · 순서 유지 — 줄별 종류 · 부연과 맞춘다, 10-04
    // C139)
    List<String> out = new java.util.ArrayList<>(enLines.size());
    for (int i = 0; i < enLines.size(); i++) {
      String line = enLines.get(i);
      String ko = translate(line);
      if (ko != null && ko.equals(line)) {
        for (int w = 2; w <= 3 && i + w <= enLines.size(); w++) {
          String joined = String.join(" ", enLines.subList(i, i + w));
          String joinedKo = translate(joined);
          if (joinedKo != null && !joinedKo.equals(joined)) {
            out.add(joinedKo);
            for (int k = 1; k < w; k++) {
              out.add("");
            }
            i += w - 1;
            ko = null;
            break;
          }
        }
      }
      if (ko != null) {
        out.add(ko);
      }
    }
    return out;
  }

  private static String normalize(String line) {
    return NUMBER.matcher(line).replaceAll(SLOT);
  }

  private static List<String> numbers(String line) {
    List<String> out = new java.util.ArrayList<>();
    Matcher m = NUMBER.matcher(line);
    while (m.find()) {
      out.add(m.group());
    }
    return out;
  }

  private static int count(String template) {
    int c = 0;
    for (int i = 0; i < template.length(); i++) {
      if (template.charAt(i) == SLOT.charAt(0)) {
        c++;
      }
    }
    return c;
  }
}
