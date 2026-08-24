package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 종목 캐시가 "못 찾은 결과" 까지 담지 않는지 본다.
 *
 * <p>{@code stockItems} 캐시는 TTL 도 크기 제한도 없는 {@code ConcurrentHashMap} 이다. 미스를 담으면 존재하지 않는 키를 부르는 만큼
 * 무한히 자란다 &mdash; 실측(수정 전): 임의 UUID 100 개와 없는 이름 200 개가 전부 캐시에 남아 재조회 시 DB 를 한 번도 타지 않았다. 이름은 화면
 * 검색으로 들어오는 값이라 특히 위험하다.
 *
 * <p>{@code unless} 표현식에 특히 주의한다. 스프링은 이 식을 평가하기 전에 {@code Optional} 을 <b>이미 벗겨낸다</b>. 그래서 {@code
 * #result.isPresent()} 를 쓰면 실제 종목을 조회할 때마다 {@code SpelEvaluationException} 으로 500 이 난다(실측: EL1004E
 * Method isPresent() cannot be found on type StockItem).
 */
class StockItemCacheMissTest {

  private static final Path SERVICE =
      Path.of("src/main/java/net/luversof/api/stock/service/StockItemService.java");

  private String read() throws IOException {
    assertThat(SERVICE).as("파일이 옮겨졌거나 사라졌다: " + SERVICE).exists();
    return Files.readString(SERVICE, StandardCharsets.UTF_8);
  }

  /** {@code @Cacheable} 애노테이션 줄만 모은다. 설명 주석에 같은 문자열이 있어도 오탐하지 않도록. */
  private List<String> cacheableLines() throws IOException {
    List<String> lines = new ArrayList<>();
    for (String line : read().split("\\n")) {
      if (line.strip().startsWith("@Cacheable")) {
        lines.add(line.strip());
      }
    }
    return lines;
  }

  @Test
  void 캐시하는_조회는_모두_미스를_제외한다() throws IOException {
    List<String> annotations = cacheableLines();
    assertThat(annotations).as("@Cacheable 이 사라졌다면 이 검사가 무의미하다").isNotEmpty();
    assertThat(annotations)
        .as("@Cacheable 마다 미스 제외 조건이 있어야 캐시가 무한히 자라지 않는다")
        .allMatch(line -> line.contains("unless = \"#result == null\""));
  }

  @Test
  void Optional_반환에_isPresent_를_쓰지_않는다() throws IOException {
    assertThat(cacheableLines())
        .as("스프링이 Optional 을 벗긴 뒤 unless 를 평가하므로 실제 조회마다 500 이 난다")
        .noneMatch(line -> line.contains("isPresent()"));
  }
}
