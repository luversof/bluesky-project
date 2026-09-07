package net.luversof.web.gate.stock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 주식 화면 컨트롤러들의 소스를 <b>통째로</b> 읽는다.
 *
 * <p>"컨트롤러가 이 값을 모델에 넣는다" 를 소스로 확인하는 검사들이 파일 이름을 하나씩 박아 두고 있었다. 그래서 컨트롤러를 쪼개자 <b>동작은 그대로인데 검사만</b>
 * 깨졌다(실측 2026-09-07: 상세 화면을 새 컨트롤러로 옮기니 3 개 파일 4 건이 실패). 검사가 묻고 싶은 것은 "어느 파일" 이 아니라 "컨트롤러 층이 그렇게
 * 하는가" 다.
 */
public final class StockControllerSources {

  private static final Path CONTROLLER_DIR =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller");

  private StockControllerSources() {}

  /** 파일 이름 -> 소스. 클래스 단위 접두어처럼 파일 경계가 필요한 검사가 쓴다. */
  public static java.util.Map<String, String> byFile() {
    try (Stream<Path> walk = Files.walk(CONTROLLER_DIR)) {
      java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
      walk.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .sorted()
          .forEach(
              path -> {
                try {
                  result.put(
                      path.getFileName().toString(),
                      Files.readString(path, StandardCharsets.UTF_8));
                } catch (IOException ex) {
                  throw new UncheckedIOException(ex);
                }
              });
      return result;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /** 컨트롤러 패키지의 모든 소스를 이어 붙인 것. */
  public static String all() {
    try (Stream<Path> walk = Files.walk(CONTROLLER_DIR)) {
      return walk.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java"))
          .sorted()
          .map(
              path -> {
                try {
                  return Files.readString(path, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                  throw new UncheckedIOException(ex);
                }
              })
          .collect(Collectors.joining("\n"));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
