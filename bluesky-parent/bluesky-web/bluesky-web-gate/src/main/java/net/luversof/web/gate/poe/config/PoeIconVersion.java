package net.luversof.web.gate.poe.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 젬 아이콘 캐시버스터 값 제공. {@code icons/version.txt}(icons.mjs 가 재생성 때마다 갱신하는 타임스탬프)를 읽어 {@code
 * /poe-assets/gems/*.png?v=} 쿼리에 붙인다. patch 만으로는 같은 patch 에서 아이콘 생성 방식만 바꿔 재생성하면 URL 이 동일해 브라우저가 옛
 * 아이콘을 계속 재사용하는 문제가 있어 별도 버전으로 분리한다.
 */
@Component
public class PoeIconVersion {

  private final Path versionFile;

  // 템플릿(JTE)에서 모델 주입 없이 캐시버스터를 쓰도록 정적 접근 제공(고유 아이콘 등 여러 표면 공용).
  private static volatile PoeIconVersion instance;

  public PoeIconVersion(@Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.versionFile = Path.of(dataDir, "icons", "version.txt");
    instance = this;
  }

  /** JTE 에서 직접 호출하는 캐시버스터. 빈 초기화 전이면 "0". */
  public static String current() {
    return instance != null ? instance.value() : "0";
  }

  /** 아이콘 세트 버전 문자열. 파일이 없으면 "0"(초기/미생성 상태)을 돌려준다. */
  public String value() {
    try {
      if (Files.exists(versionFile)) {
        return Files.readString(versionFile).trim();
      }
    } catch (Exception e) {
      // 읽기 실패는 캐시버스팅만 약화될 뿐 기능 영향 없음 → 조용히 폴백
    }
    return "0";
  }
}
