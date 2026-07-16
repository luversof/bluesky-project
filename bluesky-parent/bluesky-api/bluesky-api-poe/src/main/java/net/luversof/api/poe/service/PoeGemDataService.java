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
 * PoE 스킬젬 데이터 검색 서비스.
 *
 * <p>데이터는 tools/poe-extract 오프라인 파이프라인이 생성하는 파생 산출물이라 git 으로 관리하지 않고, 기본적으로 {@code
 * ~/.poe-gamedata/skill-gems.json} 에서 부팅 시 1회 로드한다(poe.data-dir 프로퍼티로 변경 가능). 파일이 없으면 부팅은 정상 진행되고
 * 화면에 안내가 표시된다. 시즌 갱신 = 파이프라인 재실행 + 서버 재시작.
 */
@Service
public class PoeGemDataService {

  private static final Logger logger = LoggerFactory.getLogger(PoeGemDataService.class);

  private record PoeGemData(String patch, List<PoeGem> gems) {}

  private final Path dataFile;
  private volatile PoeGemData data;

  public PoeGemDataService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataFile = Path.of(dataDir, "skill-gems.json");
    reload();
  }

  /** 데이터 파일을 다시 읽는다 (추출 파이프라인 완료 후 재시작 없이 반영). */
  public synchronized void reload() {
    PoeGemData loaded = new PoeGemData("", List.of());
    if (Files.exists(dataFile)) {
      JsonMapper jsonMapper = JsonMapper.builder().build();
      try (InputStream inputStream = Files.newInputStream(dataFile)) {
        loaded = jsonMapper.readValue(inputStream, PoeGemData.class);
        logger.info(
            "PoE 스킬젬 데이터 로드: {} ({}개, patch {})", dataFile, loaded.gems().size(), loaded.patch());
      } catch (IOException e) {
        logger.warn("PoE 스킬젬 데이터 로드 실패: {}", dataFile, e);
      }
    } else {
      logger.warn("PoE 스킬젬 데이터 없음: {} — tools/poe-extract 파이프라인 실행 필요", dataFile);
    }
    this.data = loaded;
  }

  public boolean hasData() {
    return !data.gems().isEmpty();
  }

  public String patch() {
    return data.patch();
  }

  public int totalCount() {
    return data.gems().size();
  }

  public Optional<PoeGem> findBySlug(String slug) {
    return data.gems().stream().filter(gem -> gem.slug().equals(slug)).findFirst();
  }

  /** 영문 이름 정확 일치 (대소문자 무시) — PoB 임포트 매칭용 */
  public Optional<PoeGem> findByName(String name) {
    return data.gems().stream().filter(gem -> gem.name().equalsIgnoreCase(name)).findFirst();
  }

  /**
   * @param query 이름 부분 일치 (한/영, 대소문자 무시)
   * @param type all | active | support
   * @param color all | red | green | blue | white
   */
  public List<PoeGem> search(String query, String type, String color) {
    String normalizedQuery =
        query != null && !query.isBlank() ? query.trim().toLowerCase(Locale.ROOT) : null;

    return data.gems().stream()
        .filter(
            gem ->
                normalizedQuery == null
                    || gem.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || (gem.nameKo() != null && gem.nameKo().contains(normalizedQuery)))
        .filter(
            gem ->
                type == null
                    || "all".equals(type)
                    || ("support".equals(type) ? gem.isSupport() : !gem.isSupport()))
        .filter(gem -> color == null || "all".equals(color) || color.equals(gem.color()))
        .toList();
  }
}
