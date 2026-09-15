package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 배당 캘린더의 빈 상태(보유한 월배당 종목이 없을 때).
 *
 * <p>이 상자만 <b>손으로 만들어져</b> 공용 {@code _components/ui/emptyState.jte} 의 규칙 밖에 있었다 &mdash; 문구는 "월배당 기준
 * 데이터에서 종목을 등록하고 보유 정보를 가져오세요" 라고 목적지를 말하면서 정작 <b>그리로 가는 링크가 없었다</b>. 빈 상태에 다음 걸음을 그 자리에 두는 것은 이
 * 저장소가 이미 정한 규칙이다({@link EmptyStateWidenCtaTest} 가 기간 넓히기 CTA 를 네 화면에 붙인 것과 같은 이유).
 *
 * <p>대비는 문제가 아니었다 &mdash; 실측 2026-09-15(캔버스 합성, 자가검사 검정/흰 21:1): {@code text-base-content/60} 이
 * {@code bg-base-200} 위에서 라이트 4.83:1 · 다크 5.7:1 로 둘 다 4.5 를 넘는다. (내 대비 바닥은 base-100 위에서 잰 4.95:1 이라
 * 더 어두운 base-200 을 따로 확인한 것이다.)
 */
class DividendCalendarEmptyStateTest {

  private static final String PAGE = "src/main/jte/stock/dividend.jte";

  private static final String COMPONENT = "src/main/jte/_components/ui/emptyState.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 공용 컴포넌트가 링크형 CTA 를 지원한다 &mdash; 이게 빠지면 아래 검사가 헛돈다. */
  @Test
  void 공용_빈상태가_링크_CTA_를_지원한다() throws IOException {
    String component = read(COMPONENT);

    assertThat(component).contains("@param String actionLabel");
    assertThat(component).contains("@param String actionHref");
    assertThat(component).contains("@param boolean boxed");
  }

  @Test
  void 달력_빈상태가_공용_컴포넌트를_쓴다() throws IOException {
    String page = read(PAGE);

    assertThat(page)
        .as("손으로 만든 상자는 CTA 규칙 밖에 있다")
        .doesNotContain("bg-base-200 rounded-box p-10 text-center text-base-content/60");
    assertThat(page).contains("@template._components.ui.emptyState(");
  }

  @Test
  void 달력_빈상태가_기준_데이터로_가는_길을_준다() throws IOException {
    String page = read(PAGE);

    assertThat(page)
        .as("문구는 '기준 데이터에서 등록하라' 인데 링크가 없으면 다음 걸음이 없다")
        .contains("actionHref = \"/stock/admin?tab=monthly-reference\"");
    assertThat(page).contains("actionLabel = MessageUtil.getMessage(");
  }
}
