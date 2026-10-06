package net.luversof.web.gate.poe.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * /poe-data/ 데이터(패시브 · 아틀라스 트리 JSON, 스프라이트 색인) 캐시버스터(10-03 C38) — 파일 수정 시각을 {@code ?v=} 로 붙인다.
 *
 * <p>/poe-data/** 는 1시간 캐시라 데이터 갱신(run-all) 뒤에도 브라우저가 옛 트리를 계속 썼다(실측: 전직 선택지 표시를 넣은 새 트리를 받았는데 화면은
 * 옛 JSON 으로 "전직 9 / 8"). 아이콘은 {@link PoeIconVersion} 이 같은 일을 한다. 파일이 없거나 못 읽으면 빈 문자열 — URL 그대로.
 */
@Component
public class PoeDataVersion {

  private static final String PREFIX = "/poe-data/";

  private static volatile PoeDataVersion instance;

  private final Path dataDir;

  public PoeDataVersion(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataDir = Path.of(dataDir);
    instance = this;
  }

  /** jte 에서 {@code ${url}${PoeDataVersion.q(url)}} 로 쓴다. */
  public static String q(String url) {
    PoeDataVersion self = instance;
    return self == null ? "" : self.suffix(url);
  }

  String suffix(String url) {
    if (url == null || !url.startsWith(PREFIX)) {
      return "";
    }
    String relative = url.substring(PREFIX.length());
    int query = relative.indexOf('?');
    if (query >= 0) {
      relative = relative.substring(0, query);
    }
    try {
      Path file = dataDir.resolve(relative).normalize();
      if (!file.startsWith(dataDir) || !Files.isRegularFile(file)) {
        return "";
      }
      long modified = Files.getLastModifiedTime(file).toMillis();
      return (url.indexOf('?') >= 0 ? "&v=" : "?v=") + modified;
    } catch (Exception e) {
      return ""; // 캐시버스팅만 약해질 뿐 화면은 그대로
    }
  }
}
