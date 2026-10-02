package net.luversof.web.gate.poe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import net.luversof.web.gate.poe.dto.PoeGroups;
import net.luversof.web.gate.poe.httpexchange.PoeDataClient;
import net.luversof.web.gate.poe2.dto.Poe2;
import net.luversof.web.gate.poe2.httpexchange.Poe2DataClient;

/**
 * 아이템 분류 표시 이름(10-02 AAA) — 영어 화면의 상세 · 툴팁 "아이템 클래스" 줄이 내부 id(FishingRod · HybridFlask)로 나오지 않게 게임
 * 영어 이름 (ItemClasses.Name — 한국어 줄이 이미 쓰는 같은 표의 영어 칸, 인게임 한국어 툴팁 첫 줄 "쇠뇌" 와 같은 출처)으로 바꾼다.
 *
 * <p>표는 칩이 쓰는 분류 목록 API 에서 만든다(데이터에 이미 있음 — 레코드마다 필드를 늘리지 않으려고). 30분마다 새로 받고, 받지 못하면 직전 표, 그것도 없으면
 * id 그대로 — 화면이 비거나 깨지지 않는다. jte 에서 정적 호출로 쓴다.
 */
@Component
public class PoeClassNames {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(PoeClassNames.class);

  private static final long REFRESH_MS = 30 * 60 * 1000L;

  private static final long RETRY_MS = 60 * 1000L;

  private static volatile PoeClassNames instance;

  private final PoeDataClient poe1Client;
  private final Poe2DataClient poe2Client;
  private final Table poe1 = new Table();
  private final Table poe2 = new Table();

  public PoeClassNames(PoeDataClient poe1Client, Poe2DataClient poe2Client) {
    this.poe1Client = poe1Client;
    this.poe2Client = poe2Client;
    instance = this; // jte 정적 호출용 — 싱글턴 빈이라 한 번
  }

  /** PoE1 분류 줄 — 한국어 화면이면 ko, 영어면 게임 영어 이름(없으면 id). */
  public static String poe1(String ko, String id) {
    return PoeText.name(ko, enOf(id, false));
  }

  /** PoE2 분류 줄 — 한국어 화면이면 ko, 영어면 게임 영어 이름(없으면 id). */
  public static String poe2(String ko, String id) {
    return PoeText.name(ko, enOf(id, true));
  }

  private static String enOf(String id, boolean isPoe2) {
    PoeClassNames self = instance;
    if (self == null || id == null || PoeText.isKorean()) {
      return id;
    }
    Map<String, String> table =
        isPoe2 ? self.poe2.get(self::loadPoe2) : self.poe1.get(self::loadPoe1);
    return pick(table, id);
  }

  /** 표에 있으면 그 이름, 없거나 비었으면 id. */
  static String pick(Map<String, String> table, String id) {
    String en = table == null ? null : table.get(id);
    return en == null || en.isBlank() ? id : en;
  }

  /** 분류 목록 → id 별 영어 이름 표(이름 없는 항목은 빼서 id 로 떨어지게). */
  static Map<String, String> fromGroups(List<PoeGroups.ClassGroup> groups) {
    Map<String, String> out = new HashMap<>();
    if (groups != null) {
      for (PoeGroups.ClassGroup g : groups) {
        for (PoeGroups.Entry e : g.entries()) {
          if (e.en() != null && !e.en().isBlank()) {
            out.putIfAbsent(e.key(), e.en());
          }
        }
      }
    }
    return out;
  }

  static Map<String, String> fromClasses(List<Poe2.ItemClass> classes) {
    Map<String, String> out = new HashMap<>();
    if (classes != null) {
      for (Poe2.ItemClass c : classes) {
        if (c.en() != null && !c.en().isBlank()) {
          out.putIfAbsent(c.key(), c.en());
        }
      }
    }
    return out;
  }

  private Map<String, String> loadPoe1() {
    Map<String, String> m = fromGroups(poe1Client.itemClassGroups());
    // 고유에만 있는 분류(팅크 · 낚싯대)는 고유 분류 목록에 있다
    fromGroups(poe1Client.uniqueCategoryGroups()).forEach(m::putIfAbsent);
    return m;
  }

  private Map<String, String> loadPoe2() {
    Map<String, String> m = fromClasses(poe2Client.itemClasses());
    fromClasses(poe2Client.uniqueClasses()).forEach(m::putIfAbsent);
    return m;
  }

  /** 한 게임의 표 — 30분이 지나면 다시 받는다. 실패하면 직전 표를 그대로 쓰고 다음 요청에 다시 시도. */
  private static final class Table {
    private volatile Map<String, String> map;
    private volatile long loadedAt;

    Map<String, String> get(Supplier<Map<String, String>> loader) {
      long now = System.currentTimeMillis();
      if (map == null || now - loadedAt > REFRESH_MS) {
        synchronized (this) {
          if (map == null || now - loadedAt > REFRESH_MS) {
            try {
              map = Map.copyOf(loader.get());
              loadedAt = now;
            } catch (RuntimeException e) {
              loadedAt = now - REFRESH_MS + RETRY_MS; // 요청마다 API 를 두드리지 않게 1분 뒤 다시
              log.warn("아이템 분류 영어 이름 표를 받지 못함(직전 표 또는 id 로 표시): {}", e.toString());
            }
          }
        }
      }
      return map;
    }
  }
}
