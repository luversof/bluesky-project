package net.luversof.web.gate.stock.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.dto.response.HighDividendDesignationResponse;
import net.luversof.web.gate.stock.httpexchange.HighDividendDesignationClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;

/** 관리 화면 "고배당기업 지정" 탭의 모델. 조회가 실패해도 탭은 뜨고, 실패 사실을 따로 적는다. */
@Component
public class HighDividendAdminSupport {

  public static final String ADMIN_TAB_HIGH_DIVIDEND = "high-dividend";

  private static final Logger log = LoggerFactory.getLogger(HighDividendAdminSupport.class);

  @Autowired private HighDividendDesignationClient highDividendDesignationClient;

  @Autowired private StockItemClient stockItemClient;

  /** {@code editId} 가 목록에 있으면 그 행을 폼에 채운다. */
  public void populate(Model model, UUID editId) {
    try {
      List<HighDividendDesignationResponse> designations = highDividendDesignationClient.findAll();
      model.addAttribute("highDividendDesignations", designations);
      if (editId != null) {
        designations.stream()
            .filter(d -> editId.equals(d.id()))
            .findFirst()
            .ifPresent(d -> model.addAttribute("highDividendEdit", d));
      }
    } catch (RuntimeException e) {
      log.warn("high dividend designation lookup failed", e);
      model.addAttribute("highDividendLoadFailed", true);
    }
    try {
      List<StockItem> items = stockItemClient.getStockItems();
      model.addAttribute(
          "highDividendStockItems",
          items == null
              ? List.of()
              : items.stream()
                  .filter(i -> i.symbol() != null)
                  .sorted(Comparator.comparing(StockItem::symbol))
                  .toList());
    } catch (RuntimeException e) {
      log.warn("stock item lookup failed", e);
      model.addAttribute("highDividendLoadFailed", true);
    }
  }
}
