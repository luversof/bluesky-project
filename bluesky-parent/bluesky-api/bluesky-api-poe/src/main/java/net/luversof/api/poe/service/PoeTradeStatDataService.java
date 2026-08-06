package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * PoE 거래소 스탯 필터 사전 — tools/poe-extract/trade-stats.mjs 가 만든 {@code
 * ~/.poe-gamedata/trade-stats.json}({explicit, pseudo} 섹션별 정규화 한글 텍스트 → stat id). 시뮬 결과 레어의 ko 모드
 * 라인을 거래소 검색 쿼리(q JSON)의 스탯 필터로 변환한다. stat id 는 글로벌/한국 서버 공통.
 */
@Service
public class PoeTradeStatDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeTradeStatDataService.class);

  /**
   * 합산(pseudo) 필터로 대체할 explicit 라인 — 정규화 explicit 텍스트 → 정규화 pseudo 텍스트. 생명력/저항/능력치는 시장 매물이 순수 모드 +
   * 하이브리드 모드로 쪼개져 있어(아이템 표시는 합산 한 줄) explicit 단일 모드 min 검색은 최상위 티어 롤만 잡혀 매물이 거의 없다. pseudo_total_*
   * 은 모드 합산 총량 기준이라 같은 min 으로 구매 가능한 매물이 잡힌다. 무기 로컬 모드(공격속도·물리피해 등)는 로컬/글로벌 의미가 달라 제외.
   */
  private static final Map<String, String> PSEUDO_TEXT_BY_EXPLICIT_TEXT =
      Map.ofEntries(
          Map.entry("생명력 최대치 +#", "생명력 최대치 총 +#"),
          Map.entry("마나 최대치 +#", "마나 최대치 총 +#"),
          Map.entry("에너지 보호막 최대치 +#", "에너지 보호막 최대치 총 +#"),
          Map.entry("화염 저항 +#%", "화염 저항 총 +#%"),
          Map.entry("냉기 저항 +#%", "냉기 저항 총 +#%"),
          Map.entry("번개 저항 +#%", "번개 저항 총 +#%"),
          Map.entry("카오스 저항 +#%", "카오스 저항 총 +#%"),
          Map.entry("모든 원소 저항 +#%", "모든 원소 저항력 총 +#%"),
          Map.entry("힘 +#", "힘 총 +#"),
          Map.entry("민첩 +#", "민첩 총 +#"),
          Map.entry("지능 +#", "지능 총 +#"),
          Map.entry("모든 능력치 +#", "모든 능력치 총 +#"));

  private final Path dataFile;
  private volatile Map<String, String> statIds = Map.of();
  private volatile Map<String, String> pseudoIds = Map.of();
  private volatile Map<String, String> implicitIds = Map.of();

  public PoeTradeStatDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "trade-stats.json");
    reload();
  }

  public synchronized void reload() {
    Map<String, String> loadedExplicit = Map.of();
    Map<String, String> loadedPseudo = Map.of();
    Map<String, String> loadedImplicit = Map.of();
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        Map<String, Object> raw =
            jsonMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
        if (raw.get("explicit") instanceof Map) {
          loadedExplicit = castStringMap(raw.get("explicit"));
          loadedPseudo = castStringMap(raw.getOrDefault("pseudo", Map.of()));
          loadedImplicit = castStringMap(raw.getOrDefault("implicit", Map.of()));
        } else {
          // 구 포맷(flat explicit 맵) 하위호환 — pseudo 없이 explicit 만
          loadedExplicit = castStringMap(raw);
        }
        logger.info(
            "PoE 거래소 스탯 사전 로드: {} (explicit {}건, pseudo {}건, implicit {}건)",
            dataFile,
            loadedExplicit.size(),
            loadedPseudo.size(),
            loadedImplicit.size());
      } catch (Exception e) {
        logger.warn("PoE 거래소 스탯 사전 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 거래소 스탯 사전 없음: {} — trade-stats.mjs 실행 필요", dataFile);
    }
    this.statIds = loadedExplicit;
    this.pseudoIds = loadedPseudo;
    this.implicitIds = loadedImplicit;
  }

  /**
   * 한글 키워드 중 하나라도 포함하는 임플리싯 stat id 목록. 결합(Synthesis) 유니크의 "빌드 유효 임플리싯 1개 이상" count 필터용 — cap 은
   * 호출처가 걸므로 **텍스트 길이 오름차순**(짧을수록 "화염 피해 #% 증가" 류 범용 핵심 모드, 길수록 조건부 희귀 모드)으로 정렬해 cap 에서 핵심이 잘리지 않게
   * 한다(id 정렬이었을 때 288매칭 중 핵심 전멸 실측). 동길이는 텍스트 순 — 결정성.
   */
  public java.util.List<String> implicitIdsMatching(java.util.List<String> koKeywords) {
    if (implicitIds.isEmpty() || koKeywords.isEmpty()) {
      return java.util.List.of();
    }
    return implicitIds.entrySet().stream()
        .filter(e -> koKeywords.stream().anyMatch(k -> e.getKey().contains(k)))
        .sorted(
            java.util.Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length())
                .thenComparing(Map.Entry::getKey))
        .map(Map.Entry::getValue)
        .distinct()
        .toList();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> castStringMap(Object value) {
    return value instanceof Map ? (Map<String, String>) value : Map.of();
  }

  /**
   * 한글 모드 라인 → 거래소 stat id. 숫자를 # 로 정규화해 조회, 없으면 로컬 변형("(특정)") → 감소→증가 반전 순서로 폴백. 거래소 사전은 증가 방향
   * 텍스트만 실려 있고 감소 모드는 같은 id 의 음수 값으로 검색한다(전창조 "충전 소모량 #% 감소" 등 — 유니크 플라스크에서 실측). 미매칭 null.
   */
  public String statIdFor(String koLine) {
    if (koLine == null || statIds.isEmpty()) {
      return null;
    }
    String id = lookup(normalize(koLine));
    if (id == null && koLine.contains("감소")) {
      id = lookup(normalize(koLine).replace("감소", "증가"));
    }
    return id;
  }

  private String lookup(String norm) {
    String id = statIds.get(norm);
    if (id == null) {
      id = statIds.get(norm + "(특정)");
    }
    if (id == null) {
      id = statIds.get(norm + " (특정)");
    }
    return id;
  }

  /** 한글 모드 라인 → 합산(pseudo) stat id — 매핑 대상이 아니거나 사전에 없으면 null(호출처가 explicit 폴백). */
  public String pseudoIdFor(String koLine) {
    if (koLine == null || pseudoIds.isEmpty()) {
      return null;
    }
    String pseudoText = PSEUDO_TEXT_BY_EXPLICIT_TEXT.get(normalize(koLine));
    return pseudoText != null ? pseudoIds.get(pseudoText) : null;
  }

  private static String normalize(String koLine) {
    return koLine.replaceAll("[0-9]+(\\.[0-9]+)?", "#").replaceAll("\\s+", " ").trim();
  }
}
