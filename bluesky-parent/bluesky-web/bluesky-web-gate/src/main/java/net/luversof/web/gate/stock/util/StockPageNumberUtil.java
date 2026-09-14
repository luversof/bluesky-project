package net.luversof.web.gate.stock.util;

import net.luversof.web.gate.stock.support.StockPageNumberParamException;

/**
 * How the {@code page} in the address is read.
 *
 * <p>Page numbers start at one. Anything below that is stopped with a name, the same rule its
 * siblings {@code sort}, {@code timeZone} and {@code size} already follow - measured 2026-09-12:
 * {@code size=0} raises a named 400 while {@code page=0} quietly drew page 1, in one method.
 *
 * <p>The top end is deliberately left alone. The list can shrink between two visits, and pulling a
 * bookmarked page 10 back to the last page is kinder than refusing it.
 */
public final class StockPageNumberUtil {

  private StockPageNumberUtil() {}

  /**
   * Returns the page when it is a page number, otherwise stops with the parameter name.
   *
   * @param name which parameter it was (it goes into the message)
   * @param page the value the request gave
   */
  public static int resolve(String name, int page) {
    if (page < 1) {
      throw new StockPageNumberParamException(name, page);
    }
    return page;
  }
}
