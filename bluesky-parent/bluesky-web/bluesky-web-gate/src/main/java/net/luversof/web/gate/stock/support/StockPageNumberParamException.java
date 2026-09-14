package net.luversof.web.gate.stock.support;

/**
 * When the page number in the address cannot be used. Carries the parameter name so the message can
 * say which value was wrong.
 *
 * <p>Measured 2026-09-12 on the trade history fragment: {@code size} out of range already stops
 * with a named 400, but {@code page=0} and {@code page=-1} were quietly turned into page 1 in the
 * very same method. A page number below one is not a page at all, and the screen never sends one.
 *
 * <p>The upper end is not checked here: a bookmarked page 10 should still open when the list has
 * shrunk to five pages, so that side keeps clamping to the last page.
 */
public class StockPageNumberParamException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String name;

  public StockPageNumberParamException(String name, int value) {
    super("page parameter cannot be used: " + name + "=" + value);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
