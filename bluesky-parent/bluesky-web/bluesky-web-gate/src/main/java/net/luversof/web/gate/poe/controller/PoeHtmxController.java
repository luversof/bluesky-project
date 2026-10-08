package net.luversof.web.gate.poe.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.gate.poe.config.PoeIconVersion;
import net.luversof.web.gate.poe.dto.PoeBaseItem;
import net.luversof.web.gate.poe.dto.PoeGem;
import net.luversof.web.gate.poe.dto.PoeJobStatus;
import net.luversof.web.gate.poe.dto.PoeUniqueItem;
import net.luversof.web.gate.poe.dto.PoeUpgradeGuide;
import net.luversof.web.gate.poe.httpexchange.PoeBuildClient;
import net.luversof.web.gate.poe.httpexchange.PoeDataClient;
import net.luversof.web.gate.poe.httpexchange.PoeExtractClient;
import net.luversof.web.gate.poe.httpexchange.PoeOptimizeClient;
import net.luversof.web.gate.poe.httpexchange.PoeSimClient;
import net.luversof.web.gate.poe.httpexchange.PoeUpgradeGuideClient;

/**
 * PoE htmx fragment 컨트롤러 — 데이터/잡은 bluesky-api-poe 로 위임하고 여기선 로그인 게이팅 + fragment 렌더만 담당한다. 잡 시작은
 * {@code principal != null} 일 때만 클라이언트로 전달한다.
 */
@Controller
@RequestMapping(value = "/poe/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class PoeHtmxController {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(PoeHtmxController.class);

  private final PoeDataClient poeDataClient;
  private final PoeBuildClient poeBuildClient;
  private final PoeOptimizeClient poeOptimizeClient;
  private final PoeUpgradeGuideClient poeUpgradeGuideClient;
  private final PoeSimClient poeSimClient;
  private final PoeExtractClient poeExtractClient;
  private final PoeIconVersion poeIconVersion;

  public PoeHtmxController(
      PoeDataClient poeDataClient,
      PoeBuildClient poeBuildClient,
      PoeOptimizeClient poeOptimizeClient,
      PoeUpgradeGuideClient poeUpgradeGuideClient,
      PoeSimClient poeSimClient,
      PoeExtractClient poeExtractClient,
      PoeIconVersion poeIconVersion) {
    this.poeDataClient = poeDataClient;
    this.poeBuildClient = poeBuildClient;
    this.poeOptimizeClient = poeOptimizeClient;
    this.poeUpgradeGuideClient = poeUpgradeGuideClient;
    this.poeSimClient = poeSimClient;
    this.poeExtractClient = poeExtractClient;
    this.poeIconVersion = poeIconVersion;
  }

  /**
   * 최적 조합 탐색 상태 fragment — 고정 래퍼가 3초 간격으로 폴링한다(체인식 폴링은 요청 하나만 실패해도 영구히 끊겨 화면이 멈춘다). 유휴/완료 상태면 결과를
   * 인라인으로 담고 HTTP 286 을 돌려줘 htmx 폴링을 중단시킨다.
   */
  @GetMapping("/sim/optimize/status")
  public String optimizeStatus(
      java.security.Principal principal,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    PoeJobStatus.Optimize status = poeOptimizeClient.status();
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", status.available());
    model.addAttribute("running", status.running());
    model.addAttribute("status", status.status());
    model.addAttribute("phase", status.phase());
    model.addAttribute("phaseDone", status.phaseDone());
    model.addAttribute("phaseTotal", status.phaseTotal());
    model.addAttribute("evalCount", status.evalCount());
    model.addAttribute("logLines", status.logLines());
    model.addAttribute("result", status.result());
    model.addAttribute("tattooIcons", tattooIcons(status.result()));
    model.addAttribute("rareBases", rareBases(status.result()));
    java.util.Map<String, PoeBaseItem> uniqueOverrideBases = new java.util.HashMap<>();
    model.addAttribute("uniqueOverrides", uniqueOverrides(status.result(), uniqueOverrideBases));
    model.addAttribute("uniqueOverrideBases", uniqueOverrideBases);
    java.util.Map<String, PoeBaseItem> jewelOverrideBases = new java.util.HashMap<>();
    model.addAttribute("jewelOverrides", jewelOverrides(status.result(), jewelOverrideBases));
    model.addAttribute("jewelOverrideBases", jewelOverrideBases);
    model.addAttribute("ninjaBenchmark", benchmark(status.result()));
    model.addAttribute("realStart", realStart(status.result()));
    if (!status.running()) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/simOptimizeStatus";
  }

  /**
   * 시뮬 폼 실시간 성향 미리보기 — 선택한 (첫 스킬 × 전직)의 poe.ninja 실빌드 성향/프로파일을 폼에서 바로 보여준다(실행 전). 전직=auto(빈값)면 서버가
   * 스킬 단위 폴백. 스킬 미선택이면 빈 프래그먼트.
   */
  @GetMapping("/sim/archetype")
  public String simArchetype(
      @RequestParam(name = "skills", required = false) java.util.List<String> skills,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      Model model) {
    net.luversof.web.gate.poe.dto.ArchetypeBenchmark bench = null;
    if (skills != null && !skills.isEmpty() && skills.get(0) != null && !skills.get(0).isBlank()) {
      try {
        // 선택 스킬 **전부**의 젬 이름을 풀어 조합 벤치 요청 — RF+화염덫이면 두 스킬 모두 쓰는 캐릭터만 집계
        //   (사용자 요구). 단일 스킬이면 기존 아키타입 경로와 동일.
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String slug : skills) {
          if (slug == null || slug.isBlank()) {
            continue;
          }
          net.luversof.web.gate.poe.dto.PoeGem gem = poeDataClient.gem(slug);
          if (gem != null && gem.name() != null && !gem.name().isBlank()) {
            names.add(gem.name());
          }
        }
        if (names.size() >= 2) {
          bench =
              poeOptimizeClient.archetypeCombo(
                  String.join(",", names), ascendancy != null ? ascendancy : "");
        } else if (names.size() == 1) {
          bench = poeOptimizeClient.archetype(names.get(0), ascendancy != null ? ascendancy : "");
        }
      } catch (Exception e) {
        bench = null; // api-poe 미가동/미매칭 → 미표시
      }
    }
    model.addAttribute("ninjaBenchmark", bench);
    // 단일 스킬이면 실빌드 출발점도 — 조합(여러 스킬)은 대표 실빌드가 그 조합을 다 쓴다는 보장이 없어 뺀다
    if (bench != null && skills != null && skills.size() == 1 && bench.mainSkill() != null) {
      try {
        model.addAttribute(
            "realStart",
            poeOptimizeClient.realStart(bench.mainSkill(), ascendancy != null ? ascendancy : ""));
      } catch (RuntimeException e) {
        log.warn("실빌드 출발점 조회 실패({}): {}", bench.mainSkill(), e.toString());
      }
    }
    return "poe/htmx/archetypeHint";
  }

  /**
   * 결과의 (전직×메인스킬)에 해당하는 poe.ninja 실빌드 벤치마크 — 결과를 실빌드 중앙값과 비교 표시. api-poe 미가동/데이터 없음이면 null(게이트가
   * 미표시).
   */
  private net.luversof.web.gate.poe.dto.ArchetypeBenchmark benchmark(
      net.luversof.web.gate.poe.dto.PoeOptimizeResult result) {
    if (result == null || result.gemName() == null || result.gemName().isBlank()) {
      return null;
    }
    try {
      // 멀티스킬 결과면 선택 스킬 전부(메인+additionalSkills)를 쓰는 캐릭터만 집계한 조합 벤치 —
      //   RF+화염덫 결과에 RF 전체 아키타입을 보여주지 않는다(사용자 요구). 단일이면 기존 경로.
      java.util.List<String> names = new java.util.ArrayList<>();
      names.add(result.gemName());
      if (result.additionalSkills() != null) {
        for (var s : result.additionalSkills()) {
          if (s.name() != null && !s.name().isBlank() && !names.contains(s.name())) {
            names.add(s.name());
          }
        }
      }
      String asc = result.ascendancy() != null ? result.ascendancy() : "";
      return names.size() >= 2
          ? poeOptimizeClient.archetypeCombo(String.join(",", names), asc)
          : poeOptimizeClient.archetype(result.gemName(), asc);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 결과 문신 배지 아이콘 맵 — 한글명→아이콘 경로(tree-icons 평탄화). 문신이 없으면 빈 맵(추가 호출 없음). 실시간 상태와 이력 재조회가 같은 결과
   * fragment 를 쓰므로 공용 헬퍼로 뽑았다.
   */
  private java.util.Map<String, String> tattooIcons(
      net.luversof.web.gate.poe.dto.PoeOptimizeResult result) {
    java.util.Map<String, String> tattooIcons = new java.util.LinkedHashMap<>();
    if (result != null
        && result.treeTattooLabels() != null
        && !result.treeTattooLabels().isEmpty()) {
      for (var t : poeDataClient.tattoos()) {
        if (t.icon() != null && t.icon().contains("SkillIcons/")) {
          tattooIcons.put(
              t.nameKo() != null ? t.nameKo() : t.name(),
              "tree/"
                  + t.icon()
                      .substring(t.icon().indexOf("SkillIcons/") + 11)
                      .toLowerCase()
                      .replace("/", "_"));
        }
      }
    }
    return tattooIcons;
  }

  /**
   * 결과 레어 아이템의 베이스 조인 — 인게임 툴팁처럼 방어/무기 속성 + 요구사항을 보여주기 위함. 레어의 slug 는 베이스 slug 이므로 baseItem 으로
   * 조회한다. RARE 만, 실패는 조용히 건너뜀(툴팁은 모드만이라도 표시).
   */
  private java.util.Map<String, PoeBaseItem> rareBases(
      net.luversof.web.gate.poe.dto.PoeOptimizeResult result) {
    java.util.Map<String, PoeBaseItem> map = new java.util.HashMap<>();
    if (result == null || result.items() == null) {
      return map;
    }
    for (var item : result.items()) {
      if ("RARE".equals(item.rarity()) && item.slug() != null && !map.containsKey(item.slug())) {
        try {
          PoeBaseItem base = poeDataClient.baseItem(item.slug());
          if (base != null) {
            map.put(item.slug(), base);
          }
        } catch (Exception e) {
          // 베이스 조회 실패 — 그 아이템은 모드만 표시
        }
      }
    }
    return map;
  }

  /**
   * 결과 고유 중 원래 고유와 옵션이 다른 것 → 실제 장착 옵션으로 바꾼 사본(슬롯별, 10-05 C152). 삿된(Foulborn) · 변형 사본은 원래 고유와 slug
   * 가 같아 일반 상세(hx-get)가 **원래 옵션 · 기본 변형**을 띄웠다. 영문 결과 줄 = 도유 + 임플리싯 + 익스플리싯이라 앞의 두 묶음을 떼면 장착한
   * 익스플리싯이 남는다. C153 부터 표식 없는 고유도(결과 줄 = 계산이 쓴 롤) — 결과가 있을 때만 고유 수만큼 조회. bases 에는 그 슬롯의 베이스(분류 · 요구
   * 사항)를 채운다.
   */
  private java.util.Map<String, PoeUniqueItem> uniqueOverrides(
      net.luversof.web.gate.poe.dto.PoeOptimizeResult result,
      java.util.Map<String, PoeBaseItem> bases) {
    java.util.Map<String, PoeUniqueItem> map = new java.util.HashMap<>();
    if (result == null || result.items() == null) {
      return map;
    }
    for (var item : result.items()) {
      boolean foulborn = item.name() != null && item.name().startsWith("Foulborn ");
      // 표식 없는 고유도 — 결과 줄은 계산에 쓴 롤(최대롤)이라 일반 상세의 범위와 다르다(10-05 C153)
      if (!"UNIQUE".equals(item.rarity()) || item.slug() == null || item.modLinesEn() == null) {
        continue;
      }
      try {
        PoeUniqueItem u = poeDataClient.unique(item.slug());
        if (u == null) {
          continue;
        }
        java.util.List<String> en = item.modLinesEn();
        int off = 0;
        while (off < en.size() && en.get(off).startsWith("(anoint) ")) {
          off++;
        }
        // 변형 사본은 이름 꼬리 괄호 = 변형 이름(API uniqueVariants 규약) — 임플리싯 수가 기본 변형과 다른 변형이 78개라 그 변형 것으로
        java.util.List<String> baseImplicits = u.implicits();
        if (!foulborn
            && item.name().endsWith(")")
            && item.name().contains(" (")
            && u.variants() != null) {
          String label =
              item.name().substring(item.name().lastIndexOf(" (") + 2, item.name().length() - 1);
          for (var v : u.variants()) {
            if (label.equals(v.name())) {
              baseImplicits = v.implicits();
            }
          }
        }
        int implicitStart = off;
        off += baseImplicits == null ? 0 : baseImplicits.size();
        if (off > en.size()) {
          continue;
        }
        int end = en.size();
        java.util.List<String> explicits = java.util.List.copyOf(en.subList(off, end));
        java.util.List<String> ko = item.modLines();
        java.util.List<String> explicitsKo =
            ko != null && ko.size() == end ? java.util.List.copyOf(ko.subList(off, end)) : null;
        java.util.List<java.util.List<String>> rem =
            item.modReminders() != null && item.modReminders().size() == end
                ? java.util.List.copyOf(item.modReminders().subList(off, end))
                : null;
        java.util.List<java.util.List<String>> remKo =
            item.modRemindersKo() != null && item.modRemindersKo().size() == end
                ? java.util.List.copyOf(item.modRemindersKo().subList(off, end))
                : null;
        // 인게임 이름: 삿된은 "삿된 X"(Foulborn X), 변형 꼬리 괄호는 우리 표식이라 원래 이름
        map.put(
            item.slot(),
            new PoeUniqueItem(
                foulborn ? item.name() : u.name(),
                foulborn ? item.nameKo() : u.nameKo(),
                u.slug(),
                u.baseType(),
                u.baseTypeKo(),
                u.category(),
                u.requiredLevel(),
                u.league(),
                u.legacy(),
                u.radius(),
                // 임플리싯도 결과 줄(계산이 쓴 롤 · 고른 변형의 것)
                java.util.List.copyOf(en.subList(implicitStart, off)),
                explicitsKo == null ? null : java.util.List.copyOf(ko.subList(implicitStart, off)),
                explicits,
                explicitsKo,
                null,
                null,
                u.reqStr(),
                u.reqDex(),
                u.reqInt(),
                u.iconKey(),
                u.flavour(),
                u.flavourKo(),
                rem,
                remKo));
        PoeBaseItem base = poeDataClient.baseItemByName(u.baseType());
        if (base != null) {
          bases.put(item.slot(), base);
        }
      } catch (Exception e) {
        log.warn("결과 고유 사본 실패 {}: {}", item.slug(), e.toString());
      }
    }
    return map;
  }

  private static final java.util.regex.Pattern ROLL_RANGE =
      java.util.regex.Pattern.compile("[(]([0-9]+(?:[.][0-9]+)?)-([0-9]+(?:[.][0-9]+)?)[)]");

  /** "(4-6)%" → "6%" — 이로운 줄의 최대롤(API PoeOptimizeService.resolveRoll 과 같은 규칙, 10-05 C156). */
  static String maxRoll(String line) {
    return line == null ? null : ROLL_RANGE.matcher(line).replaceAll("$2");
  }

  /** 결과 고유 주얼 툴팁 사본의 키 — 같은 고유 주얼이 둘이어도(감시자의 눈 두 개) 고른 옵션으로 갈린다. 템플릿도 같은 식. */
  public static String jewelOverrideKey(String slug, java.util.List<String> linesEn) {
    return slug + "|" + (linesEn == null ? 0 : linesEn.hashCode());
  }

  /**
   * 결과 고유 주얼 중 빌드마다 옵션이 다른 것(감시자의 눈 · 금단 짝 — 결과가 고른 옵션 줄을 보낸다) → 그 옵션으로 바꾼 사본(10-05 C154, 장비 고유
   * C152 짝). 일반 상세는 감시자의 눈의 모든 후보 · 금단의 기본 노터블을 띄워 고른 물건과 달랐다. bases 엔 키별 베이스.
   */
  private java.util.Map<String, PoeUniqueItem> jewelOverrides(
      net.luversof.web.gate.poe.dto.PoeOptimizeResult result,
      java.util.Map<String, PoeBaseItem> bases) {
    java.util.Map<String, PoeUniqueItem> map = new java.util.HashMap<>();
    if (result == null || result.jewels() == null) {
      return map;
    }
    for (var jewel : result.jewels()) {
      if (jewel.slug() == null
          || jewel.slug().isBlank()
          || jewel.linesEn() == null
          || jewel.linesEn().isEmpty()) {
        continue;
      }
      try {
        PoeUniqueItem u = poeDataClient.unique(jewel.slug());
        if (u == null) {
          continue;
        }
        String key = jewelOverrideKey(jewel.slug(), jewel.linesEn());
        // PoB 아이템 속성 줄(금단 사본의 "Limited to: 1" · "Requires Class X")은 옵션이 아니다 — 한국어 줄엔 없어 수가 어긋났다
        java.util.List<String> en =
            jewel.linesEn().stream()
                .filter(l -> !l.startsWith("Limited to:") && !l.startsWith("Requires Class "))
                .toList();
        java.util.List<String> ko = jewel.lines();
        java.util.List<String> explicits = en;
        java.util.List<String> explicitsKo = ko != null && ko.size() == en.size() ? ko : null;
        java.util.List<java.util.List<String>> rem = null;
        java.util.List<java.util.List<String>> remKo = null;
        // 고른 옵션을 다 가진 데이터 변형이 있으면 그 변형 그대로(금단 166 변형 — 한국어 · 타락 줄 · 리마인더까지 인게임 문구)
        if (u.variants() != null) {
          for (var v : u.variants()) {
            if (v.explicits() != null && !en.isEmpty() && v.explicits().containsAll(en)) {
              explicits = v.explicits();
              explicitsKo = v.explicitsKo();
              rem = v.explicitsReminders();
              remKo = v.explicitsRemindersKo();
              break;
            }
          }
          // 못 찾으면(감시자의 눈 — 변형은 오라 옵션 하나씩) 모든 변형이 함께 가진 줄(최대 생명력 · 마나 · 보호막 %)을 앞에 붙인다
          if (explicits == en) {
            var first = u.variants().get(0);
            java.util.List<String> common = new java.util.ArrayList<>();
            java.util.List<String> commonKo = new java.util.ArrayList<>();
            for (int k = 0; first.explicits() != null && k < first.explicits().size(); k++) {
              String line = first.explicits().get(k);
              if (!en.contains(line)
                  && u.variants().stream()
                      .allMatch(v -> v.explicits() != null && v.explicits().contains(line))) {
                // 공통 줄(최대 생명력 · 마나 · 보호막 %)은 이로운 줄 — 계산(합성 감시자의 눈 · 최대롤)과 같게 범위의 큰 값으로(C156)
                common.add(maxRoll(line));
                commonKo.add(
                    maxRoll(
                        first.explicitsKo() != null && k < first.explicitsKo().size()
                            ? first.explicitsKo().get(k)
                            : line));
              }
            }
            if (!common.isEmpty()) {
              java.util.List<String> joined = new java.util.ArrayList<>(common);
              joined.addAll(en);
              explicits = joined;
              if (explicitsKo != null) {
                java.util.List<String> joinedKo = new java.util.ArrayList<>(commonKo);
                joinedKo.addAll(explicitsKo);
                explicitsKo = joinedKo;
              }
            }
          }
        }
        map.put(
            key,
            new PoeUniqueItem(
                u.name(),
                u.nameKo(),
                u.slug(),
                u.baseType(),
                u.baseTypeKo(),
                u.category(),
                u.requiredLevel(),
                u.league(),
                u.legacy(),
                u.radius(),
                u.implicits(),
                u.implicitsKo(),
                explicits,
                explicitsKo,
                null,
                null,
                u.reqStr(),
                u.reqDex(),
                u.reqInt(),
                u.iconKey(),
                u.flavour(),
                u.flavourKo(),
                rem,
                remKo));
        PoeBaseItem base = poeDataClient.baseItemByName(u.baseType());
        if (base != null) {
          bases.put(key, base);
        }
      } catch (Exception e) {
        log.warn("결과 고유 주얼 사본 실패 {}: {}", jewel.slug(), e.toString());
      }
    }
    return map;
  }

  /** 실행 중인 최적 조합 탐색 잡 중지 (로그인 필요) — 취소 요청만 보내고, UI 는 폴링 래퍼가 다음 상태로 갱신한다. */
  @PostMapping("/sim/optimize/stop")
  @org.springframework.web.bind.annotation.ResponseBody
  public String stopOptimize(java.security.Principal principal) {
    if (principal != null) {
      poeOptimizeClient.stop();
    }
    return "";
  }

  /** 최근 결과 목록 fragment (최신순). sim 페이지의 이력 컨테이너가 로드/완료 시 채운다. */
  @GetMapping("/sim/optimize/history")
  public String optimizeHistory(Model model) {
    model.addAttribute("history", poeOptimizeClient.history());
    return "poe/htmx/simOptimizeHistory";
  }

  /** 이력 결과 한 건을 되살려 결과 fragment 로 렌더 — 실시간 상태와 동일한 결과 DOM 을 그대로 복원한다. */
  @GetMapping("/sim/optimize/result")
  public String optimizeHistoryResult(@RequestParam long id, Model model) {
    net.luversof.web.gate.poe.dto.PoeOptimizeResult result = poeOptimizeClient.result(id);
    model.addAttribute("result", result);
    model.addAttribute("historyId", id); // "빌드 화면에서 열기"가 이 이력 결과로 연다(10-08)
    model.addAttribute("tattooIcons", tattooIcons(result));
    model.addAttribute("rareBases", rareBases(result));
    java.util.Map<String, PoeBaseItem> uniqueOverrideBases = new java.util.HashMap<>();
    model.addAttribute("uniqueOverrides", uniqueOverrides(result, uniqueOverrideBases));
    model.addAttribute("uniqueOverrideBases", uniqueOverrideBases);
    java.util.Map<String, PoeBaseItem> jewelOverrideBases = new java.util.HashMap<>();
    model.addAttribute("jewelOverrides", jewelOverrides(result, jewelOverrideBases));
    model.addAttribute("jewelOverrideBases", jewelOverrideBases);
    model.addAttribute("ninjaBenchmark", benchmark(result));
    model.addAttribute("realStart", realStart(result));
    return "poe/htmx/simOptimizeResult";
  }

  /** 최근 결과 한 건 삭제 (로그인 필요) — 삭제 후 갱신된 이력 목록 fragment 를 그대로 반환(htmx 가 목록만 교체). */
  @org.springframework.web.bind.annotation.DeleteMapping("/sim/optimize/history/{id}")
  public String deleteOptimizeHistory(
      @org.springframework.web.bind.annotation.PathVariable long id,
      java.security.Principal principal,
      Model model) {
    if (principal != null) {
      poeOptimizeClient.deleteHistory(id);
    } else {
      // 세션 만료 — 조용히 건너뛰면 목록이 그대로 200으로 내려가 화면은 성공 흐름을 탄다
      // (실사고: "삭제되었습니다"가 뜨는데 삭제 안 됨). 미수행 사실을 배너로 알린다.
      model.addAttribute("loginRequired", true);
    }
    model.addAttribute("history", poeOptimizeClient.history());
    return "poe/htmx/simOptimizeHistory";
  }

  /** 최적 조합 탐색 시작 (로그인 필요) — 폴링 래퍼를 새로 내려 interval 을 재장전한다 */
  @PostMapping("/sim/optimize")
  public String startOptimize(
      @RequestParam(required = false, defaultValue = "") String slug,
      @RequestParam(required = false, defaultValue = "auto") String objective,
      @RequestParam(required = false, defaultValue = "Pinnacle") String scenario,
      @RequestParam(required = false, defaultValue = "false") boolean buffs,
      @RequestParam(required = false, defaultValue = "false") boolean thorough,
      @RequestParam(required = false, defaultValue = "") String className,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false) java.util.List<String> uniques,
      @RequestParam(required = false) java.util.List<String> skills,
      // 트리 에디터에서 확정한 트리(콤마 id) — 지정하면 트리 탐색을 건너뛰고 그 트리로 최적화
      @RequestParam(required = false, defaultValue = "") String treeNodes,
      // 트리에서 고른 마스터리 효과 — 같이 넘겨야 확정 트리가 설계대로 평가된다
      @RequestParam(required = false, defaultValue = "") String masteries,
      // 트리에서 소켓에 꽂아둔 주얼 — 최적화기는 나머지 소켓만 채운다
      @RequestParam(required = false, defaultValue = "") String jewels,
      // 트리에서 꽂아둔 클러스터 주얼 구성 — 없으면 생성 노드(id ≥ 65536)를 PoB 가 무시해 더 낮은 수치로 최적화된다
      @RequestParam(required = false, defaultValue = "") String clusters,
      // 트리에서 새긴 문신 — 없으면 최적화기가 원래 패시브로 계산해 화면 수치와 어긋난다
      @RequestParam(required = false, defaultValue = "") String tattoos,
      // 트리에서 고른 도유 노터블 id — 없으면 최적화기가 자동 전수 스윕으로 고른다
      @RequestParam(required = false, defaultValue = "") String anoint,
      // 루미너리 용병 빌드 PoB 코드(선택)
      @RequestParam(required = false) String mercCode,
      java.security.Principal principal,
      Model model) {
    if (principal != null) {
      // 멀티셀렉트(반복 파라미터) 또는 콤마 텍스트 둘 다 수용 → 콤마 문자열로 합쳐 API 로 전달
      try {
        poeOptimizeClient.start(
            slug,
            objective,
            scenario,
            buffs,
            className,
            ascendancy,
            joinCsv(uniques),
            joinCsv(skills),
            treeNodes,
            masteries,
            jewels,
            clusters,
            tattoos,
            anoint,
            thorough,
            blankToNull(mercCode));
      } catch (RuntimeException e) {
        // 시작 자체가 거부됨(용병 코드를 못 읽는 등) — 폴링 대신 사유를 보인다. 원격 오류는 반드시 로그로 남긴다.
        log.warn(
            "최적화 시작 실패(용병 코드 {}): {}", blankToNull(mercCode) != null ? "있음" : "없음", e.toString());
        model.addAttribute(
            "startError",
            io.github.luversof.boot.context.support.MessageUtil.getMessage(
                blankToNull(mercCode) != null
                    ? "poe.sim.opt.start.error.merc"
                    : "poe.sim.opt.start.error"));
      }
    }
    return "poe/htmx/simOptimizeWrap";
  }

  /** 멀티셀렉트/텍스트 입력을 콤마 문자열로 정규화 (빈 값 제거). */
  private static String joinCsv(java.util.List<String> values) {
    if (values == null || values.isEmpty()) {
      return "";
    }
    return values.stream()
        .filter(v -> v != null && !v.isBlank())
        .map(String::trim)
        .reduce((a, b) -> a + "," + b)
        .orElse("");
  }

  /** 젬 랭킹 배치 상태 fragment — 고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단 */
  @GetMapping("/sim/status")
  public String simStatus(
      java.security.Principal principal,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    PoeJobStatus.Sim status = poeSimClient.status();
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", status.available());
    model.addAttribute("running", status.running());
    model.addAttribute("status", status.status());
    model.addAttribute("progressDone", status.progressDone());
    model.addAttribute("progressTotal", status.progressTotal());
    model.addAttribute("logLines", status.logLines());
    if (!status.running()) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/simStatus";
  }

  /** 젬 랭킹 배치 시작 (로그인 필요) — 폴링 래퍼를 새로 내려 interval 을 재장전한다 */
  @PostMapping("/sim/run")
  public String startSim(java.security.Principal principal) {
    if (principal != null) {
      poeSimClient.start();
    }
    return "poe/htmx/simWrap";
  }

  /** 젬 DPS 랭킹 목록 fragment */
  @GetMapping("/sim/ranking")
  public String simRanking(
      // 시즌 비교(10-02 사용자 요청) — season = 볼 시즌(비면 지금), against = 비교할 시즌(비면 바로 이전 시즌)
      @RequestParam(required = false, defaultValue = "") String season,
      @RequestParam(required = false, defaultValue = "") String against,
      Model model) {
    java.util.List<String> seasons = java.util.List.of();
    try {
      seasons = poeSimClient.rankingSeasons();
    } catch (RuntimeException e) {
      log.warn("PoE 젬 랭킹 시즌 조회 실패: {}", e.toString());
    }
    PoeJobStatus.SimRanking ranking =
        season.isBlank() ? poeSimClient.ranking() : poeSimClient.ranking(season);
    String viewed = season.isBlank() && !seasons.isEmpty() ? seasons.get(0) : season;
    String vs =
        against.isBlank()
            ? net.luversof.web.gate.poe.RankCompare.previousSeason(seasons, viewed)
            : against;
    PoeJobStatus.SimRanking prev =
        vs == null || vs.equals(viewed) ? null : poeSimClient.ranking(vs);
    model.addAttribute("ranking", ranking.ranking());
    model.addAttribute("rankingPatch", ranking.patch());
    model.addAttribute("seasons", seasons);
    model.addAttribute("season", viewed == null ? "" : viewed);
    model.addAttribute("against", prev == null ? "" : vs);
    model.addAttribute("againstPatch", prev == null ? "" : prev.patch());
    model.addAttribute(
        "changes",
        prev == null
            ? java.util.Map.of()
            : net.luversof.web.gate.poe.RankCompare.compare(
                ranking.ranking().stream()
                    .map(net.luversof.web.gate.poe.dto.PoeGemRank::slug)
                    .toList(),
                ranking.ranking().stream()
                    .map(net.luversof.web.gate.poe.dto.PoeGemRank::dps)
                    .toList(),
                prev.ranking().stream()
                    .map(net.luversof.web.gate.poe.dto.PoeGemRank::slug)
                    .toList(),
                prev.ranking().stream()
                    .map(net.luversof.web.gate.poe.dto.PoeGemRank::dps)
                    .toList()));
    return "poe/htmx/simRanking";
  }

  /** 결과의 (전직×메인스킬) 실빌드 출발점 — 대표 실빌드를 우리 엔진·표준 가정으로 재계산한 값과 코드. api-poe 미가동/데이터 없음이면 null(미표시). */
  private net.luversof.web.gate.poe.dto.RealStart realStart(
      net.luversof.web.gate.poe.dto.PoeOptimizeResult result) {
    if (result == null || result.gemName() == null || result.gemName().isBlank()) {
      return null;
    }
    try {
      return poeOptimizeClient.realStart(
          result.gemName(), result.ascendancy() != null ? result.ascendancy() : "");
    } catch (RuntimeException e) {
      log.warn("실빌드 출발점 조회 실패({} / {}): {}", result.ascendancy(), result.gemName(), e.toString());
      return null;
    }
  }

  /**
   * 빌드 화면 "실빌드에서 출발" — 대표 실빌드 코드를 서버에서 받아 임포트한 요약 fragment 와, 코드 입력칸을 그 코드로 채우는 OOB 조각을 함께
   * 돌려준다(재계산·가이드 버튼이 입력칸을 hx-include 로 보내므로 칸이 채워져 있어야 이어서 쓸 수 있다).
   */
  @GetMapping("/build/real-start")
  public String importRealStart(
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam String skill,
      Model model) {
    net.luversof.web.gate.poe.dto.RealStart start = null;
    try {
      start = poeOptimizeClient.realStart(skill, ascendancy);
    } catch (RuntimeException e) {
      log.warn("실빌드 출발점 조회 실패({} / {}): {}", ascendancy, skill, e.toString());
    }
    model.addAttribute("realStart", start);
    if (start == null || start.code() == null) {
      model.addAttribute("importError", true);
      return "poe/htmx/buildSummary";
    }
    try {
      model.addAttribute("build", poeBuildClient.importBuild(start.code()));
      model.addAttribute("engineAvailable", poeBuildClient.available());
    } catch (RuntimeException e) {
      log.warn("실빌드 출발점 임포트 실패({} / {}): {}", ascendancy, skill, e.toString());
      model.addAttribute("importError", true);
    }
    return "poe/htmx/buildRealStart";
  }

  /**
   * 빌드 화면 "시뮬레이터 결과 열기"(10-08, PoE2 /poe2/htmx/build/sim-result 짝) — 마지막 최적화 결과(id 없음) 또는 결과 이력의 그
   * 결과(id)의 PoB 코드를 불러오고 코드 칸도 채운다(OOB). 거래소 링크 · 업그레이드 가이드는 빌드 화면이 그대로 준다.
   */
  @GetMapping("/build/sim-result")
  public String importSimResult(@RequestParam(required = false) Long id, Model model) {
    String code = null;
    try {
      net.luversof.web.gate.poe.dto.PoeOptimizeResult r =
          id != null ? poeOptimizeClient.result(id) : poeOptimizeClient.status().result();
      code = r == null ? null : r.pobCode();
    } catch (RuntimeException e) {
      log.warn("시뮬레이터 결과 조회 실패({}): {}", id, e.toString());
    }
    if (code == null || code.isBlank()) {
      model.addAttribute("importError", true);
      return "poe/htmx/buildSummary";
    }
    try {
      model.addAttribute("build", poeBuildClient.importBuild(code));
      model.addAttribute("engineAvailable", poeBuildClient.available());
    } catch (RuntimeException e) {
      log.warn("시뮬레이터 결과 임포트 실패({}): {}", id, e.toString());
      model.addAttribute("importError", true);
    }
    model.addAttribute("oobCode", code);
    model.addAttribute("fromSim", true);
    return "poe/htmx/buildRealStart";
  }

  /** PoB 공유 코드 임포트 → 빌드 요약 fragment. 형식 오류는 같은 fragment 의 오류 상태로 표시한다. */
  @PostMapping("/build/import")
  public String importBuild(@RequestParam String code, Model model) {
    try {
      model.addAttribute("build", poeBuildClient.importBuild(code));
      model.addAttribute("engineAvailable", poeBuildClient.available());
    } catch (RuntimeException e) {
      // RuntimeException 까지 — API 가 JSON 이 아닌 응답(예: 톰캣 오류 HTML)을 주면 Jackson 3 의 StreamReadException
      // 이
      // RestClientException 이 아니라 그대로 빠져나가, 사용자 화면에 날 JSON 오류가 떴다(2026-09-29 제보).
      log.warn("PoB 임포트 실패(코드 {}자): {}", code.length(), e.toString());
      model.addAttribute("importError", true);
    }
    return "poe/htmx/buildSummary";
  }

  /**
   * 업그레이드 가이드 시작 — 빌드 화면에 붙여넣은 PoB 코드를 그대로 받아 잡을 띄우고, 상태를 3초마다 폴링하는 래퍼를 돌려준다. 엔진 평가를 수십 번 하므로 로그인
   * 사용자만 실행한다(비로그인이면 안내만 보여준다).
   */
  @PostMapping("/build/guide/start")
  public String startGuide(
      @RequestParam String code,
      @RequestParam(required = false) String mercCode,
      java.security.Principal principal,
      Model model) {
    if (principal == null) {
      model.addAttribute("loginRequired", true);
      return "poe/htmx/buildGuideStatus";
    }
    boolean started;
    try {
      started = poeUpgradeGuideClient.start(code, blankToNull(mercCode));
    } catch (RuntimeException e) {
      log.warn("업그레이드 가이드 시작 실패(코드 {}자): {}", code.length(), e.toString());
      started = false;
    }
    if (!started) {
      // 이미 돌고 있거나 코드를 못 읽은 경우 — 사유는 상태의 error 에 들어 있다(그대로 보여준다).
      model.addAttribute("status", safeGuideStatus());
      return "poe/htmx/buildGuideStatus";
    }
    return "poe/htmx/buildGuideWrap";
  }

  /** 업그레이드 가이드 상태 fragment — 진행 중이 아니면 HTTP 286 으로 htmx 폴링을 멈춘다. */
  @GetMapping("/build/guide/status")
  public String guideStatus(Model model, jakarta.servlet.http.HttpServletResponse response) {
    PoeUpgradeGuide.Status status = safeGuideStatus();
    model.addAttribute("status", status);
    if (status == null || !Boolean.TRUE.equals(status.running())) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/buildGuideStatus";
  }

  private PoeUpgradeGuide.Status safeGuideStatus() {
    try {
      return poeUpgradeGuideClient.status();
    } catch (RuntimeException e) {
      log.warn("업그레이드 가이드 상태 조회 실패: {}", e.toString());
      return null;
    }
  }

  /**
   * 자동 다듬기 시작 — 코드 칸의 빌드(보통 poe.ninja 실빌드 출발점)에 가이드 교체안을 하나씩 실제로 적용하며 다시 잰다(약 3~5분). 가이드와 같은 이유로
   * 로그인 사용자만.
   */
  @PostMapping("/build/refine/start")
  public String startRefine(
      @RequestParam String code, java.security.Principal principal, Model model) {
    if (principal == null) {
      model.addAttribute("loginRequired", true);
      return "poe/htmx/buildRefineStatus";
    }
    boolean started;
    try {
      started = poeUpgradeGuideClient.startRefine(code);
    } catch (RuntimeException e) {
      log.warn("자동 다듬기 시작 실패(코드 {}자): {}", code.length(), e.toString());
      started = false;
    }
    if (!started) {
      // 가이드·다듬기가 이미 돌고 있거나 코드를 못 읽은 경우 — 사유는 상태의 error(없으면 "다른 작업 중" 안내)
      model.addAttribute("status", safeRefineStatus());
      model.addAttribute("notStarted", true);
      return "poe/htmx/buildRefineStatus";
    }
    return "poe/htmx/buildRefineWrap";
  }

  /** 자동 다듬기 상태 fragment — 진행 중이 아니면 HTTP 286 으로 htmx 폴링을 멈춘다. */
  @GetMapping("/build/refine/status")
  public String refineStatus(Model model, jakarta.servlet.http.HttpServletResponse response) {
    PoeUpgradeGuide.RefineStatus status = safeRefineStatus();
    model.addAttribute("status", status);
    if (status == null || !Boolean.TRUE.equals(status.running())) {
      response.setStatus(286);
    }
    return "poe/htmx/buildRefineStatus";
  }

  private PoeUpgradeGuide.RefineStatus safeRefineStatus() {
    try {
      return poeUpgradeGuideClient.refineStatus();
    } catch (RuntimeException e) {
      log.warn("자동 다듬기 상태 조회 실패: {}", e.toString());
      return null;
    }
  }

  /**
   * 코드를 불러오면서 코드 칸도 그 코드로 채운다(OOB) — 다듬은 빌드 "불러오기"용. 일반 임포트는 칸이 이미 그 코드라 필요 없지만, 여기선 서버가 준 코드라 칸을
   * 바꿔야 재계산·가이드 버튼이 새 빌드로 돈다.
   */
  @PostMapping("/build/import-replace")
  public String importReplace(@RequestParam String code, Model model) {
    model.addAttribute("oobCode", code);
    try {
      model.addAttribute("build", poeBuildClient.importBuild(code));
      model.addAttribute("engineAvailable", poeBuildClient.available());
    } catch (RuntimeException e) {
      log.warn("PoB 임포트 실패(코드 {}자): {}", code.length(), e.toString());
      model.addAttribute("importError", true);
    }
    return "poe/htmx/buildRealStart";
  }

  /** PoB 계산 엔진(헤드리스)으로 빌드 스탯 재계산 → 결과 fragment */
  @PostMapping("/build/recalc")
  public String recalcBuild(
      @RequestParam String code, @RequestParam(required = false) String mercCode, Model model) {
    try {
      model.addAttribute("engineResult", poeBuildClient.recalculate(code, blankToNull(mercCode)));
      model.addAttribute("withMerc", blankToNull(mercCode) != null);
    } catch (RuntimeException e) {
      log.warn("PoB 재계산 실패(코드 {}자): {}", code.length(), e.toString());
      model.addAttribute("engineError", true);
    }
    return "poe/htmx/buildEngineResult";
  }

  /** 빈 칸(용병 코드 미입력)은 보내지 않는다 — API 가 "없음"으로 받게. */
  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  /** 트리 에디터에서 찍은 트리를 PoB 엔진으로 실계산 — 순수 트리 기여분(장비/보조젬 없음). */
  @PostMapping("/tree/stats")
  public String treeStats(
      @RequestParam(defaultValue = "0") int classId,
      @RequestParam(required = false) String ascendancy,
      @RequestParam String nodes,
      @RequestParam(required = false) String gem,
      @RequestParam(required = false) String masteries,
      @RequestParam(required = false) String jewels,
      // 클러스터 주얼 구성 — 생성 노드가 엔진에서 살아있으려면 함께 넘겨야 한다
      @RequestParam(required = false) String clusters,
      // 문신 — 그 패시브가 문신 노드로 교체돼 계산된다(반경 주얼과 짝지어 쓰는 실전 조합)
      @RequestParam(required = false) String tattoos,
      // 도유 노터블 id — nodes 에 끼우면 PoB 가 미연결 노드로 해제하므로 별도 인자
      @RequestParam(required = false) Integer anoint,
      Model model) {
    try {
      model.addAttribute(
          "treeEval",
          poeBuildClient.treeStats(
              classId,
              ascendancy == null || ascendancy.isBlank() ? null : ascendancy,
              nodes,
              gem,
              masteries,
              jewels,
              clusters,
              tattoos,
              anoint));
    } catch (RestClientException | BlueskyException e) {
      model.addAttribute("treeEvalError", true);
    }
    return "poe/htmx/treeStats";
  }

  /** 목록 렌더 상한 — 전체(1000+)를 한 번에 그리면 느려서 상위 N개만, 나머지는 검색/부위로 좁힌다 */
  private static final int LIST_LIMIT = 90;

  /** 요청 상한을 [LIST_LIMIT, 전체] 로 클램프 — 0/음수는 기본 한 페이지. */
  private static int listLimit(int requested, int total) {
    int wanted = requested <= 0 ? LIST_LIMIT : Math.max(requested, LIST_LIMIT);
    return Math.min(wanted, total);
  }

  @GetMapping("/items")
  public String baseItemList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      // 방어구 속성 베이스 필터 — 순수 str=방어도(AR)/dex=회피(EV)/int=보호막(ES),
      // 하이브리드 strdex=AR/EV, strint=AR/ES, dexint=EV/ES. 방어구 클래스에서만 적용한다.
      @RequestParam(required = false, defaultValue = "all") String attr,
      @RequestParam(required = false, defaultValue = "0") int limit,
      Model model) {
    var matched = poeDataClient.searchBaseItems(q, itemClass);
    // attr 는 방어구 부위(투구/갑옷/장갑/장화/방패)에서만 유효 — 무기 등에서 넘어와도 무시해 빈 목록을 막는다.
    boolean armourClass =
        switch (itemClass) {
          case "Helmet", "Body Armour", "Gloves", "Boots", "Shield" -> true;
          default -> false;
        };
    if (armourClass && !"all".equals(attr)) {
      matched =
          matched.stream()
              .filter(
                  it -> {
                    var a = it.armour();
                    if (a == null) {
                      return false;
                    }
                    boolean ar = a.armourMax() > 0;
                    boolean ev = a.evasionMax() > 0;
                    boolean es = a.energyShieldMax() > 0;
                    // 방어타입 완전 분할(겹침 없음): 순수 3 · 이중 3 · 삼중 1.
                    return switch (attr) {
                      case "str" -> ar && !ev && !es;
                      case "dex" -> ev && !ar && !es;
                      case "int" -> es && !ar && !ev;
                      case "strdex" -> ar && ev && !es;
                      case "strint" -> ar && es && !ev;
                      case "dexint" -> ev && es && !ar;
                      case "strdexint" -> ar && ev && es;
                      default -> true;
                    };
                  })
              .toList();
    }
    // 상위 템부터 노출 — 드랍(요구) 레벨 내림차순 정렬 후 상한 적용 (사용자 요청)
    matched =
        matched.stream()
            .sorted(java.util.Comparator.comparingInt(PoeBaseItem::dropLevel).reversed())
            .toList();
    model.addAttribute("attr", attr);
    int shown = listLimit(limit, matched.size());
    model.addAttribute("items", matched.subList(0, shown));
    model.addAttribute("matchedCount", matched.size());
    model.addAttribute("listQ", q == null ? "" : q);
    model.addAttribute("listItemClass", itemClass);
    model.addAttribute("nextLimit", shown + LIST_LIMIT);
    model.addAttribute("totalCount", poeDataClient.baseItemMeta().totalCount());
    return "poe/htmx/itemList";
  }

  /** 모드 페이지 — 아이템 클래스 하나의 접두/접미 티어 fragment (칩 클릭 시 본문 교체). */
  @GetMapping("/mods")
  public String modClass(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      Model model) {
    model.addAttribute("mods", poeDataClient.modsForItemClass(itemClass, variant, influence));
    // 엘드리치 임플리싯(총주교/포식자) — 방어구·목걸이 등 대상 슬롯만. API 가 대상 아니면 null 반환.
    model.addAttribute("eldritch", poeDataClient.eldritchForItemClass(itemClass));
    // 도유(속성 부여) — 아뮬렛 전용. htmx 로 아뮬렛 칩에 전환해도 섹션이 나타나야 한다(전체 로드와 동일)
    model.addAttribute("anoints", "Amulet".equals(itemClass) ? poeDataClient.anoints() : null);
    model.addAttribute("essences", poeDataClient.essencesForItemClass(itemClass));
    model.addAttribute("bench", poeDataClient.benchForItemClass(itemClass));
    return "poe/htmx/modClass";
  }

  @GetMapping("/items/detail")
  public String baseItemDetail(@RequestParam String slug, Model model) {
    PoeBaseItem item = poeDataClient.baseItem(slug);
    model.addAttribute("item", item);
    // 가능 모드 열거는 레이어에서 제외(사용자 요청) — 전체 모드 풀은 상세 페이지에서 제공. 불필요한 API 호출 생략.
    return "poe/htmx/itemDetail";
  }

  /** 추출 파이프라인 상태 fragment — 고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단 */
  @GetMapping("/admin/status")
  public String extractStatus(
      java.security.Principal principal,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    PoeJobStatus.Extract status = poeExtractClient.status();
    PoeJobStatus.ExtractVersion version = poeExtractClient.version(); // 최신 버전은 API 에서 캐시(10분)
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", status.available());
    model.addAttribute("running", status.running());
    model.addAttribute("status", status.status());
    model.addAttribute("logLines", status.logLines());
    model.addAttribute("latestPatch", version.latestPatch());
    model.addAttribute("outdated", version.latestPatch() != null && !version.upToDate());
    // 버전 비교를 **버튼 옆에서 바로** 보여주기 위한 값들 — 페이지 상단 배지만으로는
    // "검사를 했는지"조차 알 수 없다(최신일 때 아무 표시가 없었다).
    model.addAttribute("dataPatch", version.dataPatch());
    model.addAttribute("configPatch", version.configPatch());
    model.addAttribute("pobVersion", version.pobVersion()); // PoB 계산 엔진 버전(현재 패치 트리 여부 가시화)
    if (!status.running()) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/extractStatus";
  }

  /** 추출 파이프라인 시작 (로그인 필요) — config 패치를 최신으로 자동 교체 후 실행, 래퍼를 새로 내려 재장전 */
  @PostMapping("/admin/extract")
  public String startExtract(java.security.Principal principal) {
    if (principal != null) {
      poeExtractClient.start(true);
    }
    return "poe/htmx/extractWrap";
  }

  @GetMapping("/gems")
  public String gemList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String type,
      @RequestParam(required = false, defaultValue = "all") String color,
      @RequestParam(required = false, defaultValue = "all") String tag,
      Model model) {
    var meta = poeDataClient.gemMeta();
    // 목록 카드는 레벨별 데이터를 안 쓴다 — lite(10-02)
    model.addAttribute("gems", poeDataClient.searchGemsForList(q, type, color, tag, true));
    model.addAttribute("totalCount", meta.totalCount());
    model.addAttribute("iconVersion", poeIconVersion.value()); // 아이콘 URL 캐시버스터(재생성 때마다 갱신)
    return "poe/htmx/gemList";
  }

  @GetMapping("/uniques")
  public String uniqueList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      // 목록 상한 — "더 보기"가 한 단계씩 늘려 보낸다. 상한만 두고 더 볼 방법이 없으면
      // 1,268개 중 90개 밖은 검색어를 정확히 아는 사람만 도달할 수 있다.
      @RequestParam(required = false, defaultValue = "0") int limit,
      Model model) {
    var matched = poeDataClient.searchUniques(q, itemClass);
    int shown = listLimit(limit, matched.size());
    model.addAttribute("items", matched.subList(0, shown));
    model.addAttribute("matchedCount", matched.size());
    model.addAttribute("listQ", q == null ? "" : q);
    model.addAttribute("listItemClass", itemClass);
    model.addAttribute("nextLimit", shown + LIST_LIMIT);
    model.addAttribute("totalCount", poeDataClient.uniqueMeta().totalCount());
    // 삿된 옵션이 있는 유니크 — 목록에서 "이 아이템은 삿된판이 있다" 를 알리는 배지용
    model.addAttribute("foulbornNames", poeDataClient.foulbornNames());
    return "poe/htmx/uniqueList";
  }

  /**
   * 시뮬 폼의 세 선택(직업·스킬젬·고유템) — 서로의 선택에 따라 **많이 쓰는 순**으로 재배치해 다시 그린다.
   *
   * <p>정렬만 바꾸고 목록에서 빼지 않는다(실빌드 패싯은 상위 12개 절단이라 "없음 = 안 쓰임"이 아니다).
   */
  @GetMapping("/sim/selects")
  public String simSelects(
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "") java.util.List<String> skills,
      @RequestParam(required = false, defaultValue = "") java.util.List<String> uniques,
      Model model) {
    // 폼 최초 렌더(PoeViewController.sim)와 **같은 후보 집합**이어야 한다 — 다르면 재정렬 시 항목이 생겼다 사라진다
    java.util.List<net.luversof.web.gate.poe.dto.PoeGem> gems =
        poeDataClient.searchGemsLite("active", "all", true).stream()
            .sorted(
                java.util.Comparator.comparing(
                    gem -> gem.nameKo() != null ? gem.nameKo() : gem.name()))
            .toList();
    java.util.Set<String> nonEquip = java.util.Set.of("jewel", "tincture", "fishing");
    java.util.List<PoeUniqueItem> uniqueItems =
        poeDataClient.uniqueNames().stream() // 이름만(10-04 C124)
            .filter(u -> u.category() == null || !nonEquip.contains(u.category()))
            .sorted(java.util.Comparator.comparing(u -> u.nameKo() != null ? u.nameKo() : u.name()))
            .toList();
    // 정렬 대조는 **영문 이름**으로 한다(ninja 표기) — 화면 라벨(한글)과 분리
    java.util.Map<String, String> slugToName = new java.util.HashMap<>();
    for (var gem : gems) {
      slugToName.put(gem.slug(), gem.name());
    }
    String skillNames =
        skills.stream()
            .map(slugToName::get)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.joining(","));
    net.luversof.web.gate.poe.dto.PoeMetaOrder order =
        poeDataClient.metaOrder(ascendancy, skillNames);
    model.addAttribute("activeGems", gems);
    model.addAttribute("uniqueItems", uniqueItems);
    model.addAttribute("preSkills", new java.util.LinkedHashSet<>(skills));
    model.addAttribute("preUniques", new java.util.LinkedHashSet<>(uniques));
    model.addAttribute("ascendancy", ascendancy);
    model.addAttribute("skillOrder", order.skills());
    model.addAttribute("ascendancyOrder", order.ascendancies());
    model.addAttribute("itemOrder", order.items());
    return "poe/htmx/simSelects";
  }

  /**
   * 변형 칩 + 아이템 레이어 — 칩을 누르면 이 조각만 갈아끼워 변형별 모드를 보여준다. 변형이 많은 고유(소아사의 진주 656 등)는 칩 대신 select 라 주소를 응답
   * 머리(HX-Push-Url)로 밀어 준다(10-04 C80 — select 는 요소마다 다른 hx-push-url 을 못 단다).
   */
  @GetMapping("/uniques/variant")
  public String uniqueVariant(
      @RequestParam String slug,
      @RequestParam(required = false) Integer variant,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    if (variant != null) {
      response.setHeader(
          "HX-Push-Url",
          "/poe/uniques/"
              + java.net.URLEncoder.encode(slug, java.nio.charset.StandardCharsets.UTF_8)
              + "?variant="
              + variant);
    }
    PoeUniqueItem item = poeDataClient.unique(slug);
    model.addAttribute("item", item);
    model.addAttribute("base", poeDataClient.baseItemByName(item.baseType()));
    model.addAttribute("variant", variant);
    model.addAttribute("oob", true); // 단추(이 유니크로 최적화)도 그 변형으로 함께 바꾼다(C95)
    return "poe/htmx/uniqueVariants";
  }

  @GetMapping("/uniques/detail")
  public String uniqueDetail(@RequestParam String slug, Model model) {
    PoeUniqueItem item = poeDataClient.unique(slug);
    model.addAttribute("item", item);
    // 베이스 아이템(무기/방어 속성·아이템 클래스·요구사항)을 조인해 게임 툴팁처럼 채워 보여준다
    model.addAttribute("base", poeDataClient.baseItemByName(item.baseType()));
    return "poe/htmx/uniqueDetail";
  }

  /**
   * 게임 툴팁 형태의 문신 상세 레이어(#poePreview) — 결과/트리에서 문신 호버 시 유니크·젬과 동일한 아이템 레이어 파리티. name 은 한글명 또는 영문명(둘
   * 다 매칭). 못 찾으면 빈 fragment.
   */
  @GetMapping("/tattoos/detail")
  public String tattooDetail(@RequestParam String name, Model model) {
    net.luversof.web.gate.poe.dto.PoeTattoo tattoo =
        poeDataClient.tattoos().stream()
            .filter(t -> name.equals(t.nameKo()) || name.equals(t.name()))
            .findFirst()
            .orElse(null);
    model.addAttribute("tattoo", tattoo);
    return "poe/htmx/tattooDetail";
  }

  /** 게임 툴팁 형태의 젬 상세 레이어. level 파라미터로 표시 레벨을 바꾼다 (기본 20). */
  @GetMapping("/gems/detail")
  public String gemDetail(
      @RequestParam String slug,
      @RequestParam(required = false, defaultValue = "20") int level,
      Model model) {
    PoeGem gem = poeDataClient.gem(slug);
    int maxLevel = gem.levels().isEmpty() ? 1 : gem.levels().get(gem.levels().size() - 1).level();
    int displayLevel = Math.min(Math.max(level, 1), maxLevel);
    model.addAttribute("gem", gem);
    model.addAttribute("displayLevel", displayLevel);
    return "poe/htmx/gemDetail";
  }

  /**
   * API 호출 실패 공용 안전망 — htmx 핸들러 13곳이 try/catch 없이 API 를 부른다. 예외가 전역 핸들러로 새면 <b>빈 200</b> 이 내려가 htmx
   * 대상이 빈 내용으로 스왑되고, 사용자는 "눌렀는데 아무 일도 없는" 침묵 실패를 본다 (빌드 가져오기에서 실측). 컨트롤러 스코프 핸들러가 전역보다 우선하므로 여기서 오류
   * 프래그먼트를 렌더한다. (개별 핸들러의 try/catch 가 있으면 그쪽이 먼저 잡는다 — 맞춤 문구는 그대로 유지된다)
   */
  @org.springframework.web.bind.annotation.ExceptionHandler({
    RestClientException.class,
    BlueskyException.class
  })
  public String handleApiError(Exception e, org.springframework.ui.Model model) {
    model.addAttribute("notFound", isNotFound(e));
    return "poe/htmx/apiError";
  }

  /**
   * 없는 항목(404)인지 — 일시 장애와 구분하려고.
   *
   * <p>이전엔 모든 예외를 "잠시 후 다시 시도해 주세요"로 뭉갰다. 그런데 삭제·개명된 slug 로 호버하면 영영 안 되는데도 재시도를 권하게 된다(실측:
   * /poe/htmx/{gems,uniques,items}/detail?slug=bogus).
   *
   * <p>게이트는 API 404 를 BlueskyException 으로 감싸 받으므로 타입만으로는 못 가른다 — 메시지에 실린 상태를 본다. 문자열 의존이라 취약하지만, 못
   * 갈라도 기존 문구로 안전하게 떨어질 뿐이라 손해가 없다.
   */
  public static boolean isNotFound(Exception e) {
    if (e instanceof org.springframework.web.client.HttpClientErrorException.NotFound) {
      return true;
    }
    for (Throwable t = e; t != null; t = t.getCause()) {
      String message = String.valueOf(t.getMessage());
      if (message.contains("404")) {
        return true;
      }
      if (t instanceof BlueskyException be
          && String.valueOf(be.getErrorMessage()).contains("404")) {
        return true;
      }
      if (t.getCause() == t) {
        break;
      }
    }
    return false;
  }
}
