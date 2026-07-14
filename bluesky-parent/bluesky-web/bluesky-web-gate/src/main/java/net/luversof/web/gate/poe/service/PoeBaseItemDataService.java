package net.luversof.web.gate.poe.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/** PoE 일반(베이스) 아이템 검색 서비스. 데이터는 {@code ~/.poe-gamedata/base-items.json}(파생 산출물, git 미관리)에서 로드한다. */
@Service
public class PoeBaseItemDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeBaseItemDataService.class);

  private record PoeBaseItemData(String patch, List<PoeBaseItem> items) {}

  private final Path dataFile;
  private volatile PoeBaseItemData data;

  public PoeBaseItemDataService(
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "base-items.json");
    reload();
  }

  /** 데이터 파일을 다시 읽는다 (추출 파이프라인 완료 후 재시작 없이 반영). */
  public synchronized void reload() {
    PoeBaseItemData loaded = new PoeBaseItemData("", List.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, PoeBaseItemData.class);
        logger.info("PoE 일반 아이템 데이터 로드: {} ({}개)", dataFile, loaded.items().size());
      } catch (IOException e) {
        logger.warn("PoE 일반 아이템 데이터 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 일반 아이템 데이터 없음: {} — tools/poe-extract 파이프라인 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public int totalCount() {
    return data.items().size();
  }

  /** 아이템 클래스 id → 한국어 라벨 (표시 순서 유지) */
  public Map<String, String> itemClasses() {
    Map<String, String> classes = new LinkedHashMap<>();
    for (PoeBaseItem item : data.items()) {
      classes.putIfAbsent(
          item.itemClass(), item.itemClassKo() != null ? item.itemClassKo() : item.itemClass());
    }
    return classes;
  }

  public Optional<PoeBaseItem> findBySlug(String slug) {
    return data.items().stream().filter(item -> item.slug().equals(slug)).findFirst();
  }

  /** 영문 이름 정확 일치 (대소문자 무시) — PoB 임포트 매칭용 */
  public Optional<PoeBaseItem> findByName(String name) {
    return data.items().stream().filter(item -> item.name().equalsIgnoreCase(name)).findFirst();
  }

  /** query: 이름 부분 일치(한/영), itemClass: all 또는 클래스 id */
  public List<PoeBaseItem> search(String query, String itemClass) {
    String normalizedQuery =
        query != null && !query.isBlank() ? query.trim().toLowerCase(Locale.ROOT) : null;

    return data.items().stream()
        .filter(
            item ->
                normalizedQuery == null
                    || item.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || (item.nameKo() != null && item.nameKo().contains(normalizedQuery)))
        .filter(
            item ->
                itemClass == null || "all".equals(itemClass) || itemClass.equals(item.itemClass()))
        .toList();
  }
}
