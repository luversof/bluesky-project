package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 적립 추천 카드가 시뮬레이터 월배당 탭에 닿아 있는가(사용자 요청 2026-09-22).
 *
 * <p>프래그먼트는 <b>부모가 파라미터를 넘겨 줘야</b> 그린다. 컨트롤러가 모델에 담기만 하고 {@code simulator.jte} 가 안 넘기면 카드가 조용히 사라진다
 * &mdash; 실측 2026-09-22 에 그렇게 빈 화면이 나왔다. 그래서 세 자리(컨트롤러 · 부모 · 프래그먼트)를 한 덩이로 본다.
 */
class ContributionPickCardTest {

  private static final Path PAGE = Path.of("src/main/jte/stock/simulator.jte");

  private static final Path FRAGMENT =
      Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

  private static final Path CONTROLLER =
      Path.of("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌거나 사라졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 추천이_컨트롤러에서_화면까지_이어진다() throws IOException {
    assertThat(read(CONTROLLER))
        .as("모델에 안 담으면 화면이 받을 것이 없다")
        .contains("model.addAttribute(")
        .contains("\"monthlyContributionPicks\"");
    assertThat(read(PAGE))
        .as("부모가 안 넘기면 프래그먼트의 기본값(빈 목록)이 쓰여 카드가 조용히 사라진다")
        .contains("monthlyContributionPicks = monthlyContributionPicks");
    assertThat(read(FRAGMENT))
        .as("프래그먼트가 파라미터를 안 받으면 부모가 넘겨도 컴파일이 깨진다")
        .contains("@param java.util.List<")
        .contains("monthlyContributionPicks");
  }

  @Test
  void 카드가_근거를_함께_적는다() throws IOException {
    String fragment = read(FRAGMENT);

    // 점수만 보여 주면 무슨 수인지 알 수 없다.
    assertThat(fragment)
        .as("연배당과 분배금 추세를 함께 적어야 한다")
        .contains("stock.simulator.monthly.contribution.basis");
    assertThat(fragment)
        .as("무엇을 제쳤는지 알려야 따를 수 있다")
        .contains("stock.simulator.monthly.contribution.runner.up");
    assertThat(fragment)
        .as("제친 종목 · 같은 점수 종목은 이름부터 적는다 - 코드만으로는 무슨 종목인지 모른다(사용자 요청 2026-09-30)")
        .contains(
            "MessageUtil.getMessage(\"stock.simulator.monthly.contribution.runner.up\"), pick.runnerUpName(), pick.runnerUpSymbol(),")
        .contains(
            "MessageUtil.getMessage(\"stock.simulator.monthly.contribution.tied\"), pick.runnerUpName(), pick.runnerUpSymbol())");
    assertThat(fragment)
        .as("점수가 같으면 갈리지 않는다고 말해야 한다")
        .contains("stock.simulator.monthly.contribution.tied");
    assertThat(fragment)
        .as("추세를 모르는 종목에 0% 를 적으면 안 된다")
        .contains("stock.simulator.monthly.contribution.trend.unknown");
  }

  @Test
  void 추천이_없으면_카드를_안_그린다() throws IOException {
    // 빈 카드가 남으면 "추천이 없다" 가 아니라 "고장났다" 로 읽힌다.
    String fragment = read(FRAGMENT);
    int guard = fragment.indexOf("@if(!monthlyContributionPicks.isEmpty())");
    int list = fragment.indexOf("data-contribution-picks");

    assertThat(guard).as("빈 목록 가드가 없다").isGreaterThanOrEqualTo(0);
    assertThat(list).as("목록 표식이 가드 안에 있어야 한다").isGreaterThan(guard);
  }

  @Test
  void 추천_문구가_두_로케일에_다_있다() throws IOException {
    String english = read(Path.of("src/main/resources/uiMessage.properties"));
    String korean = read(Path.of("src/main/resources/uiMessage_ko.properties"));

    for (String key :
        List.of(
            "stock.simulator.monthly.contribution.title",
            "stock.simulator.monthly.contribution.desc",
            "stock.simulator.monthly.contribution.account.brokerage",
            "stock.simulator.monthly.contribution.account.pension",
            "stock.simulator.monthly.contribution.basis",
            "stock.simulator.monthly.contribution.trend.unknown",
            "stock.simulator.monthly.contribution.runner.up",
            "stock.simulator.monthly.contribution.tied")) {
      assertThat(english).as(key + " 가 영어 메시지에 없다").contains(key + " =");
      assertThat(korean).as(key + " 가 한글 메시지에 없다").contains(key + " =");
    }

    // 영어 파일에 한글·기호가 섞이면 편집기 코드페이지에서 깨진다.
    for (String line : english.split("\r\n")) {
      if (!line.contains("monthly.contribution")) {
        continue;
      }
      for (char c : line.toCharArray()) {
        assertThat((int) c).as("영어 메시지 줄에 ASCII 밖 글자가 있다: " + line).isLessThan(128);
      }
    }
  }
}
