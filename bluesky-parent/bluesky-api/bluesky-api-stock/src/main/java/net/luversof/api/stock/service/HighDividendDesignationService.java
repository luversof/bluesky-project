package net.luversof.api.stock.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.HighDividendDesignation;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.HighDividendDesignationRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.HighDividendDesignationUpsertRequest;
import net.luversof.api.stock.web.dto.response.HighDividendDesignationResponse;

/** 고배당기업 지정 관리. */
@Service
public class HighDividendDesignationService {

  @Autowired private HighDividendDesignationRepository highDividendDesignationRepository;

  @Autowired private StockItemRepository stockItemRepository;

  /** 사업연도 최신순, 같은 해면 종목 이름순. */
  public List<HighDividendDesignationResponse> findAll() {
    List<HighDividendDesignation> rows = new ArrayList<>();
    highDividendDesignationRepository.findAll().forEach(rows::add);
    Map<UUID, StockItem> items =
        StockItemLookup.byIds(
            stockItemRepository,
            rows.stream().map(HighDividendDesignation::getStockItemId).toList());
    return rows.stream()
        .map(row -> toResponse(row, items.get(row.getStockItemId())))
        .sorted(
            Comparator.comparingInt(HighDividendDesignationResponse::businessYear)
                .reversed()
                .thenComparing(r -> r.stockItemName() == null ? "" : r.stockItemName()))
        .toList();
  }

  public HighDividendDesignationResponse upsert(HighDividendDesignationUpsertRequest request) {
    validate(request);
    StockItem stockItem = StockItemLookup.requireBySymbol(stockItemRepository, request.symbol());
    HighDividendDesignation row =
        request.id() == null
            ? new HighDividendDesignation()
            : highDividendDesignationRepository
                .findById(request.id())
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown designation: " + request.id()));
    row.setStockItemId(stockItem.getId());
    row.setBusinessYear(request.businessYear());
    row.setPayDateFrom(request.payDateFrom());
    row.setPayDateTo(request.payDateTo());
    row.setNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
    return toResponse(highDividendDesignationRepository.save(row), stockItem);
  }

  public void delete(UUID id) {
    highDividendDesignationRepository.deleteById(id);
  }

  static void validate(HighDividendDesignationUpsertRequest request) {
    if (request == null || !StringUtils.hasText(request.symbol())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }
    if (request.businessYear() == null
        || request.payDateFrom() == null
        || request.payDateTo() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "businessYear, payDateFrom and payDateTo are required");
    }
    if (request.payDateFrom().isAfter(request.payDateTo())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "payDateFrom must not be after payDateTo");
    }
    if (request.note() != null && request.note().length() > 1000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "note is too long");
    }
  }

  private static HighDividendDesignationResponse toResponse(
      HighDividendDesignation row, StockItem stockItem) {
    return new HighDividendDesignationResponse(
        row.getId(),
        row.getStockItemId(),
        stockItem != null ? stockItem.getSymbol() : null,
        stockItem != null ? stockItem.getName() : null,
        row.getBusinessYear(),
        row.getPayDateFrom(),
        row.getPayDateTo(),
        row.getNote());
  }
}
