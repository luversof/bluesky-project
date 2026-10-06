package net.luversof.api.poe.service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

/**
 * 레어 아이템 이름 한국어 — 인게임 레어 이름 = 접두 낱말 + 접미 낱말("Mind Reach" → "마음의 역량"). 낱말표는
 * tools/poe-extract/rare-names.mjs 가 게임 Words 표(Wordlist 1 접두 · 2 접미)에서 만든
 * rare-name-words.json(10-04 C143). 표에 없는 낱말이 하나라도 있으면 null(화면은 영문 유지).
 */
@Service
public class PoeRareNameService {

  private static final Logger logger = LoggerFactory.getLogger(PoeRareNameService.class);

  // bases = 장비 베이스 표 밖 베이스(팅크처)의 영 · 한 — 마법 이름 번역용(10-05 C162). 옛 파일엔 없어 null
  private record Words(
      Map<String, String> prefix, Map<String, String> suffix, Map<String, String> bases) {}

  private final Path file;
  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private volatile Words words;

  public PoeRareNameService(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.file = Path.of(dataDir, "rare-name-words.json");
  }

  private Words words() {
    Words w = words;
    if (w != null) {
      return w;
    }
    w = new Words(Map.of(), Map.of(), Map.of());
    if (Files.exists(file)) {
      try (InputStream in = Files.newInputStream(file)) {
        w = jsonMapper.readValue(in, Words.class);
      } catch (Exception e) {
        logger.warn("레어 이름 낱말표 읽기 실패: {}", e.toString());
      }
    }
    words = w;
    return w;
  }

  /** 이름 속 추가 베이스(팅크처 — 장비 베이스 표 밖) {영, 한}. 가장 긴 것을 고른다. 없으면 null(10-05 C162). */
  public String[] extraBaseWithin(String name) {
    Map<String, String> bases = name == null ? null : words().bases();
    if (bases == null) {
      return null;
    }
    String best = null;
    for (String b : bases.keySet()) {
      if (name.contains(b) && (best == null || b.length() > best.length())) {
        best = b;
      }
    }
    return best == null ? null : new String[] {best, bases.get(best)};
  }

  /** "접두 접미" 두 낱말 이름 → 한국어(못 옮기면 null). */
  public String translate(String name) {
    if (name == null) {
      return null;
    }
    int space = name.indexOf(' ');
    if (space <= 0 || space != name.lastIndexOf(' ')) {
      return null;
    }
    Words w = words();
    String p = w.prefix() == null ? null : w.prefix().get(name.substring(0, space));
    String s = w.suffix() == null ? null : w.suffix().get(name.substring(space + 1));
    return p != null && s != null ? p + " " + s : null;
  }
}
