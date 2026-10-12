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

  /** 아이콘 칸 수 캐시 — 파일 경로 → {수정 시각, 가로 칸, 세로 칸}. */
  private final java.util.Map<Path, long[]> iconCellCache =
      new java.util.concurrent.ConcurrentHashMap<>();

  public PoeDataVersion(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.dataDir = Path.of(dataDir);
    instance = this;
  }

  /** jte 에서 {@code ${url}${PoeDataVersion.q(url)}} 로 쓴다. */
  public static String q(String url) {
    PoeDataVersion self = instance;
    return self == null ? "" : self.suffix(url);
  }

  /**
   * /poe-data/ 아이콘 PNG 의 인벤토리 칸 {가로, 세로}(10-12 C280) — 게임 2DArt 아이콘은 칸당 52px + 2(54 · 106 · 158 ·
   * 210px)라 그림 크기에서 칸 수가 나온다. PoE2 고유 목록이 PoE1 처럼 인벤토리 칸 프레임을 그릴 때 쓴다(고유 레코드엔 칸 크기가 없다). 못 읽으면
   * null.
   */
  public static int[] iconCells(String url) {
    PoeDataVersion self = instance;
    return self == null ? null : self.cells(url);
  }

  int[] cells(String url) {
    if (url == null || !url.startsWith(PREFIX)) {
      return null;
    }
    try {
      Path file = dataDir.resolve(url.substring(PREFIX.length())).normalize();
      if (!file.startsWith(dataDir) || !Files.isRegularFile(file)) {
        return null;
      }
      long modified = Files.getLastModifiedTime(file).toMillis();
      long[] hit = iconCellCache.get(file);
      if (hit == null || hit[0] != modified) {
        byte[] head = new byte[24];
        try (java.io.InputStream in = Files.newInputStream(file)) {
          if (in.readNBytes(head, 0, 24) < 24
              || head[12] != 'I'
              || head[13] != 'H'
              || head[14] != 'D'
              || head[15] != 'R') {
            return null; // PNG 가 아니다
          }
        }
        java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(head);
        hit = new long[] {modified, toCells(b.getInt(16)), toCells(b.getInt(20))};
        iconCellCache.put(file, hit);
      }
      return new int[] {(int) hit[1], (int) hit[2]};
    } catch (Exception e) {
      return null;
    }
  }

  private static int toCells(int px) {
    return Math.max(1, (int) Math.round((px - 2) / 52.0));
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
