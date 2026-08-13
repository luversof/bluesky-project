package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 삿된(Foulborn) 모드 풀 — tools/poe-extract/parse-foulborn.mjs 가 만든 {@code
 * ~/.poe-gamedata/foulborn-mods.json}.
 *
 * <p>삿된 = 현재 리그 화폐(삿된 확장/제왕/엑잘티드 오브)로 <b>유니크의 기존 모드를 다른 모드로 대체</b>하는 기제. 예) 붉은 악몽의 "반경 내 화염 저항 → 공격
 * 막기 50%" 가 "→ 생명력 최대치 50%" 로 바뀐다.
 *
 * <p><b>토큰이 곧 식별자다.</b> 원본 id 는 {@code MutatedUnique<카테고리><번호><효과>} 형식인데, 그 {@code <카테고리><번호>}(예:
 * Jewel85)를 실제 유니크 이름(붉은 악몽)으로 잇는 매핑 테이블이 게임 데이터에 없다(UniqueStashLayout 은 이름 키만, Mods 에 기본 유니크 모드는
 * 부재). 그래서 이름을 추측해 붙이지 않고 토큰을 그대로 노출한다 — 틀린 이름을 다는 것보다 낫다.
 */
@Service
public class PoeFoulbornDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeFoulbornDataService.class);

  /**
   * 삿된 모드 하나.
   *
   * @param id 원본 모드 id (추적용 — 토큰 매핑이 생기면 이걸로 잇는다)
   * @param en 최대 롤 영문 문장들 / @param ko 한글
   * @param enMin 최소 롤 (범위 표시용) / @param koMin 한글
   */
  public record FoulbornMod(
      String id,
      List<String> en,
      List<String> ko,
      List<String> enMin,
      List<String> koMin,
      // 이 옵션이 밀어내는 **원본 모드**. PoB 삿된 지도에서 온다(없으면 대체 대상 불명).
      String origId,
      List<String> origEn,
      List<String> origKo) {}

  /**
   * 한 유니크의 삿된 옵션 묶음 — 인게임 "삿된 XXX" 한 자루가 가질 수 있는 대체 모드 풀.
   *
   * @param uniqueName 이 옵션이 붙는 유니크(영문). 해석 실패 시 null → 화면은 토큰만 보여준다.
   */
  public record FoulbornGroup(
      String token,
      String category,
      String categoryKo,
      String uniqueName,
      String uniqueNameKo,
      String uniqueSlug,
      List<FoulbornMod> mods) {}

  private record FoulbornData(
      String patch, List<FoulbornGroup> groups, Map<String, Integer> byCategory) {}

  private final Path dataFile;
  private volatile FoulbornData data = new FoulbornData("", List.of(), Map.of());

  public PoeFoulbornDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "foulborn-mods.json");
    reload();
  }

  public synchronized void reload() {
    FoulbornData loaded = new FoulbornData("", List.of(), Map.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, FoulbornData.class);
        logger.info(
            "PoE 삿된 모드 로드: {} (그룹 {}개)",
            dataFile,
            loaded.groups() == null ? 0 : loaded.groups().size());
      } catch (Exception e) {
        logger.warn("PoE 삿된 모드 로드 실패: {}", dataFile, e);
      }
    } else {
      // 신규 산출물이라 갱신 전에는 없는 게 정상 — 경고만 남기고 빈 데이터로 동작(화면은 섹션을 감춘다)
      logger.warn("PoE 삿된 모드 없음: {} — 데이터 갱신 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public boolean hasData() {
    return data.groups() != null && !data.groups().isEmpty();
  }

  public List<FoulbornGroup> all() {
    return data.groups() == null ? List.of() : data.groups();
  }

  /** 화면 분류(한글) → 모드 수. 칩 개수 표시용. */
  public Map<String, Integer> byCategory() {
    return data.byCategory() == null ? Map.of() : data.byCategory();
  }

  /** 삿된 옵션이 있는 유니크의 영문명 집합 — 목록 배지용(그룹 전체를 내리지 않으려고 따로 둔다). */
  public java.util.Set<String> uniqueNames() {
    java.util.Set<String> names = new java.util.LinkedHashSet<>();
    for (FoulbornGroup group : all()) {
      if (group.uniqueName() != null) {
        names.add(group.uniqueName());
      }
    }
    return names;
  }

  /** 이 유니크(영문명)에 붙는 삿된 옵션. 없으면 빈 목록. */
  public List<FoulbornGroup> forUnique(String uniqueName) {
    if (uniqueName == null || uniqueName.isBlank()) {
      return List.of();
    }
    List<FoulbornGroup> out = new ArrayList<>();
    String wanted = nameKey(uniqueName);
    for (FoulbornGroup group : all()) {
      if (group.uniqueName() != null && wanted.equals(nameKey(group.uniqueName()))) {
        out.add(group);
      }
    }
    return out;
  }

  /**
   * 분류·검색어로 거른 그룹 목록.
   *
   * @param categoryKo 화면 분류(한글). 비면 전체.
   * @param query 모드 문구/토큰 부분 일치(한글·영문 모두). 비면 전체.
   */
  public List<FoulbornGroup> search(String categoryKo, String query) {
    String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<FoulbornGroup> out = new ArrayList<>();
    for (FoulbornGroup group : all()) {
      if (categoryKo != null && !categoryKo.isBlank() && !categoryKo.equals(group.categoryKo())) {
        continue;
      }
      if (q.isEmpty()) {
        out.add(group);
        continue;
      }
      // 유니크 이름(한/영) → 분류(칩과 같은 말을 검색창에 치는 건 자연스럽다) → 토큰(이름 해석 실패분의 유일한 식별 수단)
      boolean hit =
          contains(group.uniqueNameKo(), q)
              || contains(group.uniqueName(), q)
              || contains(group.categoryKo(), q)
              || contains(group.category(), q)
              || group.token().toLowerCase(Locale.ROOT).contains(q);
      if (!hit) {
        for (FoulbornMod mod : group.mods()) {
          if (matches(mod.ko(), q) || matches(mod.en(), q)) {
            hit = true;
            break;
          }
        }
      }
      if (hit) {
        out.add(group);
      }
    }
    return out;
  }

  /** 이름 대조 키 — 발음부호·대소문자 무시(Mjölner ↔ Mjolner 같은 표기 흔들림 흡수). */
  private static String nameKey(String name) {
    return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .trim();
  }

  private static boolean contains(String value, String q) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(q);
  }

  private static boolean matches(List<String> lines, String q) {
    if (lines == null) {
      return false;
    }
    for (String line : lines) {
      if (line != null && line.toLowerCase(Locale.ROOT).contains(q)) {
        return true;
      }
    }
    return false;
  }
}
