package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.support.StockPageNumberParamException;

/**
 * How the {@code page} in the address is read.
 *
 * <p>Measured 2026-09-12 on the trade history fragment (258 rows): {@code size=0} and {@code
 * size=100000} already stop with a named 400, but {@code page=0} and {@code page=-1} drew page 1
 * without a word - in the same method. The screen never sends those, so they come from a
 * hand-written address, and quietly answering with another page reads as if the number was
 * accepted.
 *
 * <p>The top end still clamps on purpose: {@code page=99999} shows the last page, because a list
 * can shrink between two visits and refusing a bookmarked page would be worse.
 */
class StockPageNumberUtilTest {

  private static final String Q = String.valueOf((char) 34);

  @Test
  void 한_쪽부터가_쪽_번호다() {
    assertThat(StockPageNumberUtil.resolve("page", 1)).isEqualTo(1);
    assertThat(StockPageNumberUtil.resolve("page", 13)).isEqualTo(13);
    assertThat(StockPageNumberUtil.resolve("page", 99999))
        .as("top end is not this rule")
        .isEqualTo(99999);
  }

  @Test
  void 한_쪽_미만은_이름을_들고_끊는다() {
    for (int bad : new int[] {0, -1, -20}) {
      assertThatThrownBy(() -> StockPageNumberUtil.resolve("page", bad))
          .as(bad + " must not become page 1 in silence")
          .isInstanceOf(StockPageNumberParamException.class);
    }
    assertThat(
            catchThrowableOfType(
                    () -> StockPageNumberUtil.resolve("tradePage", 0),
                    StockPageNumberParamException.class)
                .getName())
        .isEqualTo("tradePage");
  }

  /** The list endpoint must go through the rule, and keep clamping the top end. */
  @Test
  void 목록이_규칙을_거치고_위끝은_그대로_당긴다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java"),
            StandardCharsets.UTF_8);
    assertThat(source)
        .contains(
            "page = net.luversof.web.gate.stock.util.StockPageNumberUtil.resolve("
                + Q
                + "page"
                + Q
                + ", page);");
    assertThat(source)
        .as("the top end still pulls back to the last page")
        .contains("int currentPage = Math.min(page, Math.max(1, totalPages));");
    assertThat(source)
        .as("the old silent floor must be gone")
        .doesNotContain("Math.max(1, Math.min(page,");
  }

  /** Stopping must produce the named message, like its siblings do. */
  @Test
  void 끊은_뒤에는_이름_있는_문구가_나간다() throws IOException {
    String resolver =
        Files.readString(
            Path.of("src/main/java/net/luversof/web/gate/stock/config/StockHtmxErrorResolver.java"),
            StandardCharsets.UTF_8);
    assertThat(count(resolver, "StockPageNumberParamException"))
        .as("4xx decision once, message choice once")
        .isEqualTo(2);
    assertThat(resolver).contains("stock.error.badrequest.page.desc");
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.error.badrequest.page.desc");
    }
  }

  /** The pager itself must never send a page number the rule would reject. */
  @Test
  void 쪽나눔은_규칙이_거부할_값을_보내지_않는다() throws IOException {
    String jte =
        Files.readString(
            Path.of("src/main/jte/_components/htmxPagination.jte"), StandardCharsets.UTF_8);
    for (String nav : new String[] {"FirstNav", "PrevNav", "NextNav", "LastNav"}) {
      assertThat(jte)
          .as(nav + " must fall back to the current page when it is off")
          .contains(
              "${pagination.get"
                  + nav
                  + "().isActive() ? pagination.get"
                  + nav
                  + "().page() : pagination.getCurrentPage() + 1}");
    }
  }

  private int count(String source, String needle) {
    int found = 0;
    int at = source.indexOf(needle);
    while (at >= 0) {
      found++;
      at = source.indexOf(needle, at + needle.length());
    }
    return found;
  }
}
