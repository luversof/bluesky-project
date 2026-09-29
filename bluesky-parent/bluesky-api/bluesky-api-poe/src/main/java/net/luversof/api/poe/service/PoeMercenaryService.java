package net.luversof.api.poe.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 루미너리 용병 — 용병을 따로 만든 PoB 빌드(오라·저주 젬, 장비)로 계산해, 그 오라·저주·링크·전투 함성 버프를 사용자 빌드의 PoB 파티 탭({@code
 * <Party>})에 넣는다. 가이드·재계산·최적화기가 같은 변환을 쓴다(사용자 요청 2026-09-29 "가이드에서 해결되면 최적화기에도").
 *
 * <p>PoB 엔진엔 용병이 없다 — 계산 모듈(Calc*.lua)에 용병 개념이 없고, 계산되는 아이템은 플레이어 활성 세트와 장비 입는 소환수 전용 세트뿐이다. 대신
 * PoB 파티 기능이 "동료 빌드의 버프"를 받는 통로라 그걸 쓴다. 용병 장비의 오라 효과 옵션·젬 레벨·품질은 용병 빌드 계산에 들어가 효과 배율에 반영된다.
 * 용병 <b>자신의 딜·생존</b>은 계산하지 않는다(기본 능력치 데이터가 없다).
 *
 * <p>수여된 기사 작위(노드 {@value #BESTOWED_KNIGHTHOOD}, 게임 데이터 3.29 "Your Mercenary has 50% increased effect of Non-Curse
 * Auras from Skills")가 사용자 트리에 있으면 용병 빌드에 같은 효과의 사용자 정의 모드를 넣는다. 용병 빌드에 이미 그 줄이 있으면 넣지 않는다(이중
 * 적용 방지). ⚠ PoB 트리 폴더 중 3_29_ruthless 는 이 노드가 30% 라 값을 거기서 읽으면 틀린다.
 *
 * <p>적 상태·적 모드 내보내기(EnemyConditions/EnemyMods)는 받지 않는다 — 파티원 빌드의 <b>설정 탭 기본값</b>(중독·감전, 광기 15중첩 "받는 피해 60%
 * 증가" 등)이 그대로 실려 와 DPS 를 엉뚱하게 부풀린다(실측 2026-09-29). 용병 스킬이 적에게 거는 효과는 오라(디버프형)·저주 항목으로 들어온다.
 */
@Service
public class PoeMercenaryService {

  private static final Logger logger = LoggerFactory.getLogger(PoeMercenaryService.class);

  /** 수여된 기사 작위 노드 id(passive-tree.json). */
  public static final int BESTOWED_KNIGHTHOOD = 56292;

  /** 기사 작위를 용병 빌드 쪽에서 본 문장 — 용병에겐 "자기 스킬의 오라"다. PoB 가 읽는 것을 실측으로 확인(효과 100% → 150%). */
  public static final String KNIGHTHOOD_MOD =
      "50% increased effect of Non-Curse Auras from your Skills";

  /** 파티 탭으로 받는 항목 — 적 상태·적 모드는 받지 않는다(클래스 주석). */
  private static final List<String> SECTIONS =
      List.of("Aura", "Curse", "Warcry Skills", "Link Skills");

  /** 버프 하나 — effect = 효과 배율 %(100 = 기본). */
  public record Buff(String name, Double effect) {}

  /**
   * 용병 빌드에서 뽑은 버프.
   *
   * @param sections 파티 탭 항목 이름 → PoB 형식 텍스트
   * @param knighthood 수여된 기사 작위를 이 계산에 넣었는지
   * @param knighthoodInMerc 용병 빌드에 이미 그 모드가 있어 따로 넣지 않았는지
   */
  public record MercBuffs(
      Map<String, String> sections,
      List<Buff> auras,
      List<Buff> curses,
      List<Buff> links,
      boolean knighthood,
      boolean knighthoodInMerc) {

    /** 화면 요약 — "분노 150% · 증오 150%" 처럼 이름과 효과 배율. */
    public String summary() {
      List<String> parts = new ArrayList<>();
      for (List<Buff> list : List.of(auras, curses, links)) {
        for (Buff b : list) {
          parts.add(b.name() + " " + Math.round(b.effect() == null ? 100 : b.effect()) + "%");
        }
      }
      return String.join(" · ", parts);
    }

    public boolean isEmpty() {
      return auras.isEmpty() && curses.isEmpty() && links.isEmpty();
    }
  }

  /** party-export.lua 출력. */
  private record ExportResult(
      Map<String, String> sections, List<Buff> auras, List<Buff> curses, List<Buff> links) {}

  private final PoePobImportService importService;
  private final PoePobEngineService engine;

  /** 같은 용병 빌드는 한 번만 계산한다(키 = 용병 XML + 기사 작위 여부의 해시). 최적화기는 후보마다 같은 용병을 쓴다. */
  private final Map<String, MercBuffs> cache = new ConcurrentHashMap<>();

  public PoeMercenaryService(PoePobImportService importService, PoePobEngineService engine) {
    this.importService = importService;
    this.engine = engine;
  }

  /**
   * 용병 PoB 코드 → 버프.
   *
   * @param knighthood 사용자 트리에 수여된 기사 작위가 있는지({@link #hasBestowedKnighthood})
   * @throws IllegalArgumentException 코드를 못 읽을 때
   * @throws IllegalStateException 엔진 실행 실패
   */
  public MercBuffs export(String mercCode, boolean knighthood) {
    String mercXml;
    try {
      mercXml = importService.decodeToXml(mercCode);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("용병 PoB 코드를 읽지 못했습니다: " + e.getMessage(), e);
    }
    boolean alreadyInMerc = mercXml.contains("increased effect of Non-Curse Auras from your Skills");
    String prepared = knighthood && !alreadyInMerc ? withCustomMod(mercXml, KNIGHTHOOD_MOD) : mercXml;
    String key = sha256(prepared);
    MercBuffs cached = cache.get(key);
    if (cached != null) {
      return cached;
    }
    ExportResult r;
    try {
      r = JsonMapper.builder().build().readValue(engine.exportPartyBuffs(prepared), ExportResult.class);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("용병 버프 결과를 읽지 못했습니다: " + e.getMessage(), e);
    }
    Map<String, String> sections = new LinkedHashMap<>();
    for (String name : SECTIONS) {
      String text = r.sections() == null ? null : r.sections().get(name);
      if (text != null && !text.isBlank()) {
        sections.put(name, text);
      }
    }
    MercBuffs out =
        new MercBuffs(
            Map.copyOf(sections),
            r.auras() == null ? List.of() : List.copyOf(r.auras()),
            r.curses() == null ? List.of() : List.copyOf(r.curses()),
            r.links() == null ? List.of() : List.copyOf(r.links()),
            knighthood,
            knighthood && alreadyInMerc);
    if (cache.size() > 64) {
      cache.clear(); // 드물게 쓰는 기능이라 단순 상한으로 충분하다
    }
    cache.put(key, out);
    logger.info(
        "용병 버프: {} (기사 작위 {}{})",
        out.summary(),
        knighthood ? "반영" : "없음",
        out.knighthoodInMerc() ? " — 용병 빌드에 이미 있어 추가 안 함" : "");
    return out;
  }

  /**
   * 사용자 빌드에 용병 버프를 넣은 XML — 기존 {@code <Party>}(자식 없는 것 포함)는 바꾼다. 사용자가 PoB 파티 탭에 이미 넣어 둔 버프와 겹쳐 두 번 적용되는 것을
   * 막는다.
   */
  public static String withParty(String playerXml, MercBuffs buffs) {
    StringBuilder party =
        new StringBuilder("<Party destination=\"All\" append=\"false\" ShowAdvanceTools=\"false\">\n");
    for (String name : SECTIONS) {
      String text = buffs.sections().get(name);
      if (text == null || text.isBlank()) {
        continue;
      }
      if ("Curse".equals(name) && buffs.curses().isEmpty()) {
        continue; // 저주가 없으면 "저주 한도" 줄만 남는다 — 넣지 않는다
      }
      party
          .append("<ImportedBuffs name=\"")
          .append(name)
          .append("\">")
          .append(escapeXml(text))
          .append("</ImportedBuffs>\n");
    }
    party.append("</Party>\n");
    String stripped = withoutParty(playerXml);
    int end = stripped.lastIndexOf("</PathOfBuilding>");
    if (end < 0) {
      return stripped;
    }
    return stripped.substring(0, end) + party + stripped.substring(end);
  }

  /** {@code <Party>} 요소를 모두 뺀 XML. */
  static String withoutParty(String xml) {
    String out = xml;
    while (true) {
      int a = indexOfTag(out, "<Party");
      if (a < 0) {
        return out;
      }
      int gt = out.indexOf('>', a);
      if (gt < 0) {
        return out;
      }
      int end;
      if (out.charAt(gt - 1) == '/') {
        end = gt + 1;
      } else {
        int close = out.indexOf("</Party>", gt);
        if (close < 0) {
          return out;
        }
        end = close + "</Party>".length();
      }
      out = out.substring(0, a) + out.substring(end);
    }
  }

  /** 태그 이름이 정확히 일치하는 위치("<Party" 뒤가 공백·'>'·'/') — "<PartyX" 같은 다른 요소는 건너뛴다. */
  private static int indexOfTag(String xml, String open) {
    int from = 0;
    while (true) {
      int a = xml.indexOf(open, from);
      if (a < 0 || a + open.length() >= xml.length()) {
        return -1;
      }
      char next = xml.charAt(a + open.length());
      if (next == ' ' || next == '>' || next == '/' || next == '\t' || next == '\n' || next == '\r') {
        return a;
      }
      from = a + 1;
    }
  }

  /** 활성 트리 스펙에 수여된 기사 작위가 찍혀 있는지 — {@code <Tree activeSpec="n">} 의 n 번째 {@code <Spec nodes="…">}. */
  public static boolean hasBestowedKnighthood(String playerXml) {
    String treeTag = tagAt(playerXml, playerXml.indexOf("<Tree"));
    int active = parseInt(attr(treeTag, "activeSpec"), 1);
    int from = 0;
    int index = 0;
    while (true) {
      int a = playerXml.indexOf("<Spec ", from);
      if (a < 0) {
        return false;
      }
      index++;
      String tag = tagAt(playerXml, a);
      if (index == active) {
        String nodes = attr(tag, "nodes");
        if (nodes == null) {
          return false;
        }
        for (String id : nodes.split(",")) {
          if (id.trim().equals(String.valueOf(BESTOWED_KNIGHTHOOD))) {
            return true;
          }
        }
        return false;
      }
      from = a + 1;
    }
  }

  /**
   * 용병 빌드에 사용자 정의 모드를 넣은 XML — 활성 설정 묶음({@code <ConfigSet id=활성>}) 안에 {@code <CustomModifierBlock>} 으로, 설정 묶음이
   * 없는 옛 형식이면 {@code <Config>} 바로 아래에(ConfigTab:Load 의 옛 형식 분기가 첫 설정 묶음에 넣는다), {@code <Config>} 가 없으면 만든다.
   */
  static String withCustomMod(String xml, String modLine) {
    String block =
        "<CustomModifierBlock title=\"Luminary\" enabled=\"true\">"
            + escapeXml(modLine)
            + "</CustomModifierBlock>";
    int configAt = indexOfTag(xml, "<Config");
    if (configAt < 0) {
      int end = xml.lastIndexOf("</PathOfBuilding>");
      return end < 0 ? xml : xml.substring(0, end) + "<Config>" + block + "</Config>\n" + xml.substring(end);
    }
    String configTag = tagAt(xml, configAt);
    String active = attr(configTag, "activeConfigSet");
    int from = configAt;
    while (true) {
      int a = xml.indexOf("<ConfigSet", from);
      if (a < 0) {
        break;
      }
      String tag = tagAt(xml, a);
      String id = attr(tag, "id");
      if (active == null || active.equals(id)) {
        int gt = xml.indexOf('>', a);
        if (xml.charAt(gt - 1) == '/') {
          // 자식 없는 설정 묶음 — 풀어서 넣는다
          return xml.substring(0, gt - 1) + ">" + block + "</ConfigSet>" + xml.substring(gt + 1);
        }
        return xml.substring(0, gt + 1) + block + xml.substring(gt + 1);
      }
      from = a + 1;
    }
    // 설정 묶음이 없는 옛 형식
    int gt = xml.indexOf('>', configAt);
    if (xml.charAt(gt - 1) == '/') {
      return xml.substring(0, gt - 1) + ">" + block + "</Config>" + xml.substring(gt + 1);
    }
    return xml.substring(0, gt + 1) + block + xml.substring(gt + 1);
  }

  private static String tagAt(String xml, int at) {
    if (at < 0) {
      return "";
    }
    int gt = xml.indexOf('>', at);
    return gt < 0 ? "" : xml.substring(at, gt + 1);
  }

  private static String attr(String tag, String name) {
    String key = " " + name + "=\"";
    int a = tag.indexOf(key);
    if (a < 0) {
      return null;
    }
    int from = a + key.length();
    int b = tag.indexOf('"', from);
    return b < 0 ? null : tag.substring(from, b);
  }

  private static int parseInt(String s, int fallback) {
    try {
      return s == null ? fallback : Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static String escapeXml(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static String sha256(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      return Integer.toHexString(s.hashCode());
    }
  }
}
