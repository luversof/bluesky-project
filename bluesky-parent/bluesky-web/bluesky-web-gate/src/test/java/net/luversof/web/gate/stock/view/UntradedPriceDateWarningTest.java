package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.DataStatusResponse;

/**
 * 마지막 시세 일자가 '거래가 없던 날'일 때 관리 화면이 그 사실을 알리는지 본다.
 *
 * <p>시세 수집은 자동이 아니라(스케줄러도 CronJob 도 없다) 사람이 누른 시점에 따라 거래가 없던 시점의 값이 들어올 수 있다. 그때 KIS 는 직전 종가를 거래량 0
 * 으로 실어 보내므로, 그대로 저장하면 그 날짜에 '확정 종가'가 하나 생긴다.
 *
 * <p>실측 2026-08-22: 2026-08-20 행 9건이 <b>전부 거래량 0</b> 이고 종가는 2026-08-19 와 같았다(시가/고가/저가/거래량까지 같은 건 0
 * 건이라 단순 행 복제는 아니다). 그런데도 화면 곳곳은 "평가 기준 2026-08-20 종가"라고 적어 있지도 않은 최신성을 단언했다.
 */
class UntradedPriceDateWarningTest {

  private static final String TEMPLATE = "stock/htmx/fragments/adminActions.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private DataStatusResponse status(long itemCount, long sameClose, long zeroVolume) {
    return status(itemCount, sameClose, zeroVolume, 57459L, 1352L, 1L, CHANGED_CLOSE_ROWS);
  }

  /** 실측 2026-08-23 의 유일한 행. 종가가 정확히 1/5 이 됐다(액면분할). */
  private static final java.util.List<DataStatusResponse.ZeroVolumeChangedCloseRow>
      CHANGED_CLOSE_ROWS =
          java.util.List.of(
              new DataStatusResponse.ZeroVolumeChangedCloseRow(
                  "쌍방울",
                  LocalDate.parse("2025-05-08"),
                  new java.math.BigDecimal("13450"),
                  new java.math.BigDecimal("2690")));

  private DataStatusResponse status(
      long itemCount,
      long sameClose,
      long zeroVolume,
      long rowCount,
      long zeroVolumeRows,
      long zeroVolumeChangedClose,
      java.util.List<DataStatusResponse.ZeroVolumeChangedCloseRow> changedCloseRows) {
    return new DataStatusResponse(
        Instant.parse("2026-08-19T00:00:00Z"),
        250L,
        Instant.parse("2026-08-19T00:00:00Z"),
        193L,
        LocalDate.parse("2026-08-20"),
        86L,
        LocalDate.parse("2026-08-19"),
        itemCount,
        sameClose,
        0L,
        zeroVolume,
        rowCount,
        zeroVolumeRows,
        zeroVolumeChangedClose,
        changedCloseRows,
        java.time.LocalDate.parse("2026-08-04"),
        OVERDUE_PAYOUTS,
        0L,
        java.util.List.of());
  }

  /** 실측 2026-08-23: 월중 4종목이 34일 경과인데 각자 과거 최대 간격은 33일이었다(8월분 누락). */
  private static final java.util.List<DataStatusResponse.MonthlyDividendPayoutOverdueRow>
      OVERDUE_PAYOUTS =
          java.util.List.of(
              new DataStatusResponse.MonthlyDividendPayoutOverdueRow(
                  "KODEX 한국부동산리츠인프라", LocalDate.parse("2026-07-20"), 34, 33));

  private String render(DataStatusResponse dataStatus) {
    Map<String, Object> params = new HashMap<>();
    params.put("isAuthenticated", true);
    params.put("dataStatus", dataStatus);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, params, output);
    return output.toString();
  }

  @Test
  void 전_종목이_거래량_0_이고_종가가_직전과_같으면_경고한다() {
    assertThat(status(9, 9, 9).priceHistoryLastDateLooksUntraded()).isTrue();

    String html = render(status(9, 9, 9));
    assertThat(html).as("관리 화면이 그 날짜를 그냥 '마지막 시세일'로만 적고 있다").contains("2026-08-20");
    assertThat(html).as("거래가 없던 날이라는 사실과 실제로 쓰이는 종가일(2026-08-19)이 화면에 없다").contains("2026-08-19");
    assertThat(html).containsPattern("(looks untraded|거래가 없던 날)");
  }

  /** 한 종목만 거래가 없었던 것은 흔한 일이다. 그걸로 경고하면 경고가 무의미해진다. */
  @Test
  void 일부만_거래량_0_이면_경고하지_않는다() {
    assertThat(status(9, 9, 3).priceHistoryLastDateLooksUntraded()).isFalse();
    assertThat(status(9, 3, 9).priceHistoryLastDateLooksUntraded()).isFalse();
    assertThat(render(status(9, 9, 3))).doesNotContain("looks untraded");
  }

  @Test
  void 정상적인_거래일이면_경고하지_않는다() {
    assertThat(status(9, 0, 0).priceHistoryLastDateLooksUntraded()).isFalse();
    String html = render(status(9, 0, 0));
    assertThat(html).contains("2026-08-20");
    assertThat(html).doesNotContain("looks untraded");
  }

  /** 시세 행이 아예 없으면(신규 사용자·초기 상태) 판단 근거가 없다. 없는 근거로 경고하면 안 된다. */
  /**
   * 거래량 0 행이 실제로 정보를 갖고 있는지는 "종가가 직전과 다른 행 수"로만 알 수 있다.
   *
   * <p>실측 2026-08-22: 57,459 행 중 1,352 행(2.35%)이 거래량 0 이고 그중 종가가 다른 것은 1 행뿐이었다. 이 비율이 크게 달라지면 "거래량
   * 0 행을 종가로 쓰지 않는다"는 판단의 전제가 흔들린다.
   */
  @Test
  void 거래량_0_행_비율을_계산한다() {
    assertThat(
            status(9, 9, 9, 57459L, 1352L, 1L, CHANGED_CLOSE_ROWS)
                .priceHistoryZeroVolumeRatioPercent())
        .isCloseTo(2.35, org.assertj.core.data.Offset.offset(0.01));
    assertThat(
            status(9, 9, 9, 0L, 0L, 0L, java.util.List.of()).priceHistoryZeroVolumeRatioPercent())
        .isZero();
  }

  @Test
  void 근거가_없으면_경고하지_않는다() {
    assertThat(status(0, 0, 0).priceHistoryLastDateLooksUntraded()).isFalse();
    assertThat(render(status(0, 0, 0))).doesNotContain("looks untraded");
  }

  /**
   * 시세 품질 지표가 화면에 실제로 그려지는지.
   *
   * <p>api-stock 은 전체 행 수·거래량 0 행 수·그중 종가가 바뀐 행 수를 예전부터 보내고 있었는데, 게이트 화면에 그리는 곳이 <b>하나도 없었다</b> (실측
   * 2026-08-23: 관련 지표 5 개 모두 사용처 0). 값이 있어도 보이지 않으면 "거래량 0 행을 평가에서 빼도 되는가" 를 판단할 수 없다.
   */
  @Test
  void 시세_품질_지표를_화면에_적는다() {
    String html = render(status(9, 9, 9));

    assertThat(html).as("전체 시세 행 수가 없다").contains("57,459");
    assertThat(html).as("거래량 0 행 수가 없다").contains("1,352");
    assertThat(html).as("거래량 0 비율이 없다").contains("2.35");
  }

  /** 종가가 바뀐 행은 개수만으로는 수집 오류인지 액면분할인지 알 수 없다. 어느 종목의 어느 날인지 함께 적는다. */
  @Test
  void 종가가_바뀐_거래량0_행을_지목한다() {
    String html = render(status(9, 9, 9));

    assertThat(html).contains("쌍방울").contains("2025-05-08");
    assertThat(html).as("얼마나 뛰었는지 보이지 않는다").contains("13450").contains("2690");
  }

  /** 그런 행이 없으면 조용해야 한다. 늘 뜨는 줄은 곧 무시된다. */
  @Test
  void 종가가_바뀐_행이_없으면_지목하지_않는다() {
    String html = render(status(9, 9, 9, 57459L, 1352L, 0L, java.util.List.of()));

    assertThat(html).doesNotContain("쌍방울");
    assertThat(html).as("품질 줄 자체는 남아 있어야 한다").contains("57,459");
  }

  /**
   * 직전 거래일과의 동일 종목 수를 화면에 적는지.
   *
   * <p>종가만 같으면 '거래가 없던 날'이고, 시가·고가·저가·거래량까지 같으면 행 자체가 복제된 것이라 원인이 다르다. 이 구분에 쓸 유일한 값(sameAll)이
   * api-stock 에서 오는데도 화면에 그리는 곳이 없었다(실측 2026-08-23: 사용처 0).
   */
  @Test
  void 직전_거래일과의_동일_종목_수를_적는다() {
    String html = render(status(9, 9, 9));

    assertThat(html).containsPattern("(identical to the previous trading day|직전 거래일과 같은 행)");
  }

  /**
   * 이 화면이 받는 시세 품질 값이 하나도 빠짐없이 그려지는지.
   *
   * <p>api-stock 이 보내고 게이트 DTO 가 받는데 화면에 그리는 곳이 없으면 그 값은 존재하지 않는 것과 같다. 실제로 그런 값이 5 개 있었다(실측
   * 2026-08-23). 값을 늘릴 때 화면을 잊지 않도록 필드 이름으로 확인한다.
   */
  @Test
  void 시세_품질_값이_모두_화면에_쓰인다() throws java.io.IOException {
    String template =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/jte/stock/htmx/fragments/adminActions.jte"),
            java.nio.charset.StandardCharsets.UTF_8);

    java.util.List<String> unused = new java.util.ArrayList<>();
    for (String accessor :
        java.util.List.of(
            "priceHistoryRowCount",
            "priceHistoryZeroVolumeRowCount",
            "priceHistoryZeroVolumeRatioPercent",
            "priceHistoryZeroVolumeChangedCloseCount",
            "priceHistoryZeroVolumeChangedCloseRows",
            "priceHistoryItemCount",
            "priceHistorySameCloseCount",
            "priceHistorySameAllCount")) {
      if (!template.contains(accessor + "()")) {
        unused.add(accessor);
      }
    }

    assertThat(unused).as("api-stock 이 보내는데 화면이 쓰지 않는 값이다 - 없는 것과 같다").isEmpty();
  }

  /**
   * 월배당 지급 이력의 최신 시점과 밀린 종목을 화면에 적는지.
   *
   * <p>이 참조 데이터도 사람이 가져와야 하는데 관리 화면은 매매·배당·시세의 최신 시점만 보여줬다. 한 달 치가 비면 예상 월배당의 기준·평균 창·"다가올 배당"의 예상
   * 지급일이 함께 어긋나므로, 밀린 사실이 보이지 않으면 사용자가 알 방법이 없다.
   */
  @Test
  void 월배당_지급_이력의_최신_시점과_밀림을_적는다() {
    String html = render(status(9, 9, 9));

    assertThat(html).as("지급 이력 최신일이 없다").contains("2026-08-04");
    assertThat(html).as("밀린 종목을 지목하지 않는다").contains("KODEX 한국부동산리츠인프라").contains("2026-07-20");
    // 두 자리 숫자도 HTML 다른 곳에 흔히 나온다. 그 값이 실린 자리를 직접 본다.
    assertThat(html)
        .as("경과일과 기준 간격이 없다 - 왜 밀렸다고 보는지 알 수 없다")
        .contains("data-payout-elapsed=\"34\"")
        .contains("data-payout-widest=\"33\"");
  }

  /** 밀린 종목이 없으면 조용해야 한다. 늘 뜨는 경고는 곧 무시된다. */
  @Test
  void 밀린_종목이_없으면_경고하지_않는다() {
    DataStatusResponse clean =
        new DataStatusResponse(
            Instant.parse("2026-08-19T00:00:00Z"),
            250L,
            Instant.parse("2026-08-19T00:00:00Z"),
            193L,
            LocalDate.parse("2026-08-20"),
            86L,
            LocalDate.parse("2026-08-19"),
            9L,
            9L,
            0L,
            9L,
            57459L,
            1352L,
            1L,
            CHANGED_CLOSE_ROWS,
            LocalDate.parse("2026-08-04"),
            java.util.List.of(),
            0L,
            java.util.List.of());

    String html = render(clean);
    assertThat(html).contains("2026-08-04");
    assertThat(html).doesNotContain("KODEX 한국부동산리츠인프라");
  }
}
