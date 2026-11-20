package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockNews;

/** 입력 뉴스 고르기: 공시는 기사 상한에 밀리지 않는다. */
class StockAnalysisNewsPickTest {

  private static StockNews news(String source, String title, String at) {
    StockNews n = new StockNews();
    n.setSource(source);
    n.setTitle(title);
    n.setPublishedDate(Instant.parse(at));
    return n;
  }

  @Test
  void 공시는_기사_상한과_상관없이_모두_넣고_최신순으로_합친다() {
    List<StockNews> all = new ArrayList<>();
    // 공시는 그날 00:00 KST 로 저장된다 - 같은 날 기사 20 건보다 늘 오래되었다.
    all.add(news(StockNews.SOURCE_DART, "공시", "2026-11-19T15:00:00Z"));
    for (int i = 0; i < 20; i++) {
      all.add(
          news(
              StockNews.SOURCE_NAVER,
              "기사" + i,
              "2026-11-20T0" + (i % 10) + ":0" + (i / 10) + ":00Z"));
    }

    List<StockNews> picked = StockAnalysisService.pickNews(all);

    assertThat(picked).hasSize(StockAnalysisService.NEWS_PER_ITEM + 1);
    assertThat(picked.get(picked.size() - 1).getSource()).isEqualTo(StockNews.SOURCE_DART);
    assertThat(picked)
        .extracting(StockNews::getPublishedDate)
        .isSortedAccordingTo((a, b) -> b.compareTo(a));
  }
}
