package net.luversof.api.poe.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * PoE 고유 아이템 검색 서비스. 데이터는 {@code ~/.poe-gamedata/unique-items.json}(파생 산출물, git 미관리)에서 부팅 시 1회
 * 로드한다. 갱신 = tools/poe-extract 의 parse-uniques.mjs 재실행 + 재시작.
 */
@Service
public class PoeUniqueDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeUniqueDataService.class);

  private record PoeUniqueData(String patch, List<PoeUniqueItem> items) {}

  private final Path dataFile;
  private volatile PoeUniqueData data;

  public PoeUniqueDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "unique-items.json");
    reload();
  }

  /** 데이터 파일을 다시 읽는다 (추출 파이프라인 완료 후 재시작 없이 반영). */
  public synchronized void reload() {
    PoeUniqueData loaded = new PoeUniqueData("", List.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, PoeUniqueData.class);
        logger.info("PoE 고유 아이템 데이터 로드: {} ({}개)", dataFile, loaded.items().size());
      } catch (IOException e) {
        logger.warn("PoE 고유 아이템 데이터 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 고유 아이템 데이터 없음: {} — tools/poe-extract 파이프라인 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public int totalCount() {
    return data.items().size();
  }

  public List<String> categories() {
    return data.items().stream().map(PoeUniqueItem::category).distinct().sorted().toList();
  }

  public Optional<PoeUniqueItem> findBySlug(String slug) {
    return data.items().stream().filter(item -> item.slug().equals(slug)).findFirst();
  }

  /** 영문 이름 정확 일치 (대소문자 무시) — PoB 임포트 매칭용 */
  public Optional<PoeUniqueItem> findByName(String name) {
    return data.items().stream().filter(item -> item.name().equalsIgnoreCase(name)).findFirst();
  }

  /** query: 이름/베이스 부분 일치(한/영), category: all 또는 카테고리명 */
  public List<PoeUniqueItem> search(String query, String category) {
    String normalizedQuery =
        query != null && !query.isBlank() ? query.trim().toLowerCase(Locale.ROOT) : null;

    return data.items().stream()
        .filter(
            item ->
                normalizedQuery == null
                    || item.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || (item.nameKo() != null && item.nameKo().contains(normalizedQuery))
                    || item.baseType().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || (item.baseTypeKo() != null && item.baseTypeKo().contains(normalizedQuery)))
        .filter(
            item -> category == null || "all".equals(category) || category.equals(item.category()))
        .toList();
  }
}
