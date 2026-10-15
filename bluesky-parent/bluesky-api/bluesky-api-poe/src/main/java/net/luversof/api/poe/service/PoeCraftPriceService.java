package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 제작 계산기 단가(10-15 C336) — tools/poe-extract/fetch-ninja-prices.mjs 가 받은 poe.ninja 거래소 시세 {@code
 * ninja/craft-prices.json}(카오스 기준). 파일이 바뀌면 다시 읽는다. 시세가 없으면 null(계산기는 시도 수만 보여 준다).
 */
@Service
public class PoeCraftPriceService {

  private static final Logger logger = LoggerFactory.getLogger(PoeCraftPriceService.class);

  /** 시세 파일 — chaos = 이름 → 카오스 값. */
  public record Prices(String league, String fetchedAt, Map<String, Double> chaos) {}

  private final Path file;
  private volatile Prices prices = new Prices(null, null, Map.of());
  private volatile FileTime loadedAt;

  public PoeCraftPriceService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.file = Path.of(dataDir, "ninja", "craft-prices.json");
  }

  public Prices prices() {
    try {
      FileTime t = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
      if (t != null && !t.equals(loadedAt)) {
        synchronized (this) {
          try (InputStream in = Files.newInputStream(file)) {
            Prices p = JsonMapper.builder().build().readValue(in, Prices.class);
            prices = p.chaos() == null ? new Prices(p.league(), p.fetchedAt(), Map.of()) : p;
            loadedAt = t;
            logger.info("PoE 제작 시세 로드: {} ({}종, {})", file, prices.chaos().size(), prices.league());
          }
        }
      }
    } catch (Exception e) {
      logger.warn("PoE 제작 시세 로드 실패: {}", file, e);
    }
    return prices;
  }

  /** 이름 → 카오스 값. 모르면 null. */
  public Double chaos(String name) {
    return name == null ? null : prices().chaos().get(name);
  }
}
