package net.luversof.api.stock.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.MonthlyDividendProfile;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileReorderRequest;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileRequest;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendProfileResponse;

@Service
public class MonthlyDividendProfileService {

  private static final String PAYOUT_WINDOW_UNKNOWN = "UNKNOWN";

  @Autowired private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Autowired private StockItemRepository stockItemRepository;

  public List<MonthlyDividendProfileResponse> findProfiles(MonthlyDividendProfileRequest request) {
    UUID stockItemId =
        resolveStockItemId(
            request != null ? request.getStockItemId() : null,
            request != null ? request.getSymbol() : null);
    if (stockItemId != null) {
      return monthlyDividendProfileRepository.findByStockItemId(stockItemId).stream()
          .map(profile -> toResponse(profile, null))
          .toList();
    }

    List<MonthlyDividendProfile> profiles =
        request != null && Boolean.TRUE.equals(request.getActiveOnly())
            ? monthlyDividendProfileRepository.findByActiveOrderByDisplayOrderAscUpdatedDateDesc(
                true)
            : monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc();

    // 종목 정보는 1회 일괄 조회한다. (행마다 findById 하면 행 수만큼 SELECT 가 나가는 N+1)
    Map<UUID, StockItem> stockItemById = new HashMap<>();
    Set<UUID> stockItemIds =
        profiles.stream()
            .map(MonthlyDividendProfile::getStockItemId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (!stockItemIds.isEmpty()) {
      stockItemRepository
          .findAllById(stockItemIds)
          .forEach(item -> stockItemById.put(item.getId(), item));
    }
    return profiles.stream()
        .map(profile -> toResponse(profile, stockItemById.get(profile.getStockItemId())))
        .toList();
  }

  public MonthlyDividendProfileResponse upsert(MonthlyDividendProfileUpsertRequest request) {
    if (request == null || !StringUtils.hasText(request.getSymbol())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }

    StockItem stockItem = resolveStockItem(request.getSymbol());
    Instant now = Instant.now();

    MonthlyDividendProfile profile =
        monthlyDividendProfileRepository
            .findByStockItemId(stockItem.getId())
            .orElseGet(MonthlyDividendProfile::new);

    if (profile.getId() == null) {
      profile.setCreatedDate(now);
    }

    profile.setStockItemId(stockItem.getId());
    profile.setSourceUrl(trimToNull(request.getSourceUrl()));
    profile.setPayoutWindow(normalizePayoutWindow(request.getPayoutWindow()));
    profile.setDisplayOrder(
        resolveDisplayOrder(request.getDisplayOrder(), profile.getDisplayOrder()));
    profile.setActive(request.getActive() != null ? request.getActive() : true);
    profile.setNote(trimToNull(request.getNote()));
    profile.setLastVerifiedDate(request.getLastVerifiedDate());
    profile.setUpdatedDate(now);

    return toResponse(monthlyDividendProfileRepository.save(profile), stockItem);
  }

  public void reorder(MonthlyDividendProfileReorderRequest request) {
    List<String> requestedSymbols = normalizeSymbols(request);
    if (requestedSymbols.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbols are required");
    }

    if (requestedSymbols.size() != new HashSet<>(requestedSymbols).size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "duplicate symbols are not allowed");
    }

    List<MonthlyDividendProfile> profiles =
        monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc();
    Instant now = Instant.now();
    Set<UUID> reorderedStockItemIds = new HashSet<>();
    List<MonthlyDividendProfile> reorderedProfiles = new ArrayList<>();
    int displayOrder = 1;

    for (String symbol : requestedSymbols) {
      StockItem stockItem = resolveStockItem(symbol);
      MonthlyDividendProfile profile = findProfile(profiles, stockItem.getId(), symbol);
      profile.setDisplayOrder(displayOrder++);
      profile.setUpdatedDate(now);
      reorderedProfiles.add(profile);
      reorderedStockItemIds.add(stockItem.getId());
    }

    for (MonthlyDividendProfile profile : profiles) {
      if (reorderedStockItemIds.contains(profile.getStockItemId())) {
        continue;
      }

      profile.setDisplayOrder(displayOrder++);
      profile.setUpdatedDate(now);
      reorderedProfiles.add(profile);
    }

    monthlyDividendProfileRepository.saveAll(reorderedProfiles);
  }

  public void deleteBySymbol(String symbol) {
    if (!StringUtils.hasText(symbol)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }

    StockItem stockItem = resolveStockItem(symbol);
    MonthlyDividendProfile profile =
        monthlyDividendProfileRepository
            .findByStockItemId(stockItem.getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Monthly dividend profile not found: " + symbol));
    monthlyDividendProfileRepository.delete(profile);
  }

  private MonthlyDividendProfileResponse toResponse(
      MonthlyDividendProfile profile, StockItem providedStockItem) {
    StockItem stockItem =
        providedStockItem != null
            ? providedStockItem
            : stockItemRepository.findById(profile.getStockItemId()).orElse(null);

    return new MonthlyDividendProfileResponse(
        profile.getId(),
        profile.getStockItemId(),
        stockItem != null ? stockItem.getSymbol() : "",
        stockItem != null ? stockItem.getName() : "",
        profile.getSourceUrl(),
        profile.getPayoutWindow(),
        profile.getDisplayOrder(),
        Boolean.TRUE.equals(profile.getActive()),
        profile.getNote(),
        profile.getLastVerifiedDate(),
        profile.getUpdatedDate());
  }

  private UUID resolveStockItemId(UUID stockItemId, String symbol) {
    if (stockItemId != null) {
      return stockItemId;
    }

    if (!StringUtils.hasText(symbol)) {
      return null;
    }

    return resolveStockItem(symbol).getId();
  }

  private StockItem resolveStockItem(String symbol) {
    StockItem stockItem = stockItemRepository.findBySymbol(symbol.trim());
    if (stockItem == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown stock symbol: " + symbol);
    }
    return stockItem;
  }

  private String normalizePayoutWindow(String payoutWindow) {
    if (!StringUtils.hasText(payoutWindow)) {
      return PAYOUT_WINDOW_UNKNOWN;
    }

    String normalized = payoutWindow.trim().replace('-', '_').replace(' ', '_');
    if ("월중".equals(normalized)) {
      return "MID_MONTH";
    }
    if ("월말".equals(normalized)) {
      return "MONTH_END";
    }

    normalized = normalized.toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "MID_MONTH", "MONTH_END", "OTHER", PAYOUT_WINDOW_UNKNOWN -> normalized;
      default -> PAYOUT_WINDOW_UNKNOWN;
    };
  }

  private Integer resolveDisplayOrder(Integer requestedDisplayOrder, Integer existingDisplayOrder) {
    if (requestedDisplayOrder == null) {
      return existingDisplayOrder != null ? existingDisplayOrder : resolveNextDisplayOrder();
    }

    if (requestedDisplayOrder < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "displayOrder must be zero or greater");
    }

    return requestedDisplayOrder;
  }

  private int resolveNextDisplayOrder() {
    return monthlyDividendProfileRepository
        .findFirstByOrderByDisplayOrderDescUpdatedDateDesc()
        .map(profile -> profile.getDisplayOrder() != null ? profile.getDisplayOrder() + 1 : 1)
        .orElse(1);
  }

  private List<String> normalizeSymbols(MonthlyDividendProfileReorderRequest request) {
    if (request == null || request.getSymbols() == null) {
      return List.of();
    }

    return request.getSymbols().stream()
        .filter(StringUtils::hasText)
        .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
        .toList();
  }

  private MonthlyDividendProfile findProfile(
      List<MonthlyDividendProfile> profiles, UUID stockItemId, String symbol) {
    return profiles.stream()
        .filter(profile -> stockItemId.equals(profile.getStockItemId()))
        .findFirst()
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Monthly dividend profile not found: " + symbol));
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim();
  }
}
