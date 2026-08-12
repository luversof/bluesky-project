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

  public PoeModTranslateService(
      PoeModPoolDataService poeModPoolDataService, PoeModDataService poeModDataService) {
    this.poeModPoolDataService = poeModPoolDataService;
    this.poeModDataService = poeModDataService;
  }

  @PostConstruct
  public void build() {
    Map<String, String> map = new HashMap<>();
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
        }
      }
    }
    this.dictionary = Map.copyOf(map);
    logger.info(
        "PoE 모드 번역 사전 구축: {}개 (큐레이션 {} + 전체 풀 보강 {})", map.size(), curated, map.size() - curated);
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
      return;
    }
    map.putIfAbsent(enTemplate.toLowerCase(Locale.ROOT), koTemplate);
  }

  /** 영문 모드 라인 → 한국어 (실패 시 원문 반환) */
  public String translate(String enLine) {
    if (enLine == null || enLine.isBlank() || dictionary.isEmpty()) {
      return enLine;
    }
    String template = normalize(enLine);
    String koTemplate = dictionary.get(template.toLowerCase(Locale.ROOT));
    if (koTemplate == null) {
      return enLine;
    }
    List<String> numbers = numbers(enLine);
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
    return enLines.stream().map(this::translate).toList();
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
