package net.luversof.web.gate.stock.controller;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequestGroup;
import net.luversof.web.gate.stock.httpexchange.AccountClient;
import net.luversof.web.gate.stock.httpexchange.DividendClient;
import net.luversof.web.gate.stock.httpexchange.StockItemClient;
import net.luversof.web.gate.stock.httpexchange.TradeClient;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

/** 주식 HTMX 컨트롤러들의 공통 베이스 클래스. 공통 상수, 의존성, 헬퍼 메서드를 제공한다. */
public abstract class StockBaseHtmxController {

  /**
   * 타임존 문자열을 ZoneId 로 바꾼다. 알 수 없는 값이면 서버 기본 타임존으로 떨어진다.
   *
   * <p>ZoneId.of 를 그대로 부르면 ZoneRulesException 이 컨트롤러 밖으로 나가는데, 공통 예외 처리기가 이를 본문 없는 200 으로 바꿔 htmx 가
   * 빈 내용을 갈아끼운다 — 화면이 조용히 비어버린다(실측: 자산성장/배당내역/매매내역).
   *
   * <p>바꾸는 규칙 자체는 {@link net.luversof.web.gate.stock.util.StockZoneUtil#resolve} 한 곳에만 둔다. 템플릿은 그
   * 유틸을 직접 부르므로, 두 벌이 있으면 같은 화면의 컨트롤러와 템플릿이 다른 존을 쓸 수 있다 — 이 세션에서 실제로 그 유형의 결함이 반복해 나왔다.
   */
  protected ZoneId resolveZoneIdOrDefault(String timeZone) {
    return net.luversof.web.gate.stock.util.StockZoneUtil.resolve(timeZone);
  }

  protected static final String ERROR_ATTRIBUTE = "error";
  protected static final String ERROR_VIEW = "stock/htmx/error";

  /**
   * 로그인이 풀린 조각 요청에 같은 안내를 돌려준다.
   *
   * <p>조각은 전체 페이지처럼 로그인으로 리다이렉트할 수 없어 오류 화면을 그리는데, 안내 문구를 넣은 곳과 넣지 않은 곳이 섞여 있었다(실측: 세션 만료 상태에서
   * {@code trade/list} 는 "로그인이 필요합니다", {@code summary}/{@code asset-status} 는 "오류가 발생했습니다"). 같은
   * 상황이면 같은 안내가 나와야 한다.
   */
  protected String loginRequiredView(org.springframework.ui.Model model) {
    model.addAttribute(ERROR_ATTRIBUTE, msg("stock.label.login.required"));
    return ERROR_VIEW;
  }

  /**
   * 백엔드 호출이 실패한 조각에 "불러오지 못했다" 는 안내를 돌려준다.
   *
   * <p>실패를 삼키고 값 없는 화면을 그리면 사용자는 <b>계산 결과가 없다</b> 고 읽는다. 실측: 자산증가 화면의 기간수익률 조각은 api-stock 호출이 예외로
   * 끝나도 요약을 {@code null} 로 넘겨, 기초 평가액이 너무 작아 비율을 못 내는 정상 상황과 <b>똑같이</b> "계산할 수 없음" 으로 그렸다. 두 상황은
   * 사용자가 할 일이 다르다 &mdash; 앞은 다시 시도, 뒤는 기간 변경이다.
   */
  protected String remoteFailureView(org.springframework.ui.Model model) {
    model.addAttribute(ERROR_ATTRIBUTE, msg("stock.error.fragment.title"));
    return ERROR_VIEW;
  }

  protected static final List<String> ACCOUNT_PRINCIPAL_CONFIG_KEYS =
      List.of("manualPrincipalAmount", "manualPrincipal", "principalAmount", "principal");

  protected final TradeProfitClient tradeProfitClient;
  protected final TradeClient tradeClient;
  protected final AccountClient accountClient;
  protected final StockItemClient stockItemClient;
  protected final DividendClient dividendClient;
  protected final MessageSource messageSource;

  protected StockBaseHtmxController(
      TradeProfitClient tradeProfitClient,
      TradeClient tradeClient,
      AccountClient accountClient,
      StockItemClient stockItemClient,
      DividendClient dividendClient,
      MessageSource messageSource) {
    this.tradeProfitClient = tradeProfitClient;
    this.tradeClient = tradeClient;
    this.accountClient = accountClient;
    this.stockItemClient = stockItemClient;
    this.dividendClient = dividendClient;
    this.messageSource = messageSource;
  }

  /** 현재 로케일에 맞는 메시지를 반환한다. */
  /**
   * 로케일에 맞는 짧은 월 표기. 한국어면 "8월", 영어면 "Aug".
   *
   * <p>이전에는 숫자에 "월"/"일" 을 직접 붙여, 영어 화면에도 한글이 그대로 나왔다.
   */
  protected String shortMonthLabel(java.time.LocalDate date) {
    return date.getMonth()
        .getDisplayName(java.time.format.TextStyle.SHORT, LocaleContextHolder.getLocale());
  }

  /** "8월 20일" / "Aug 20" 처럼 로케일에 맞는 월·일 표기. */
  protected String monthDayLabel(java.time.LocalDate date) {
    return msg("stock.common.date.month.day", shortMonthLabel(date), date.getDayOfMonth());
  }

  protected String msg(String code, Object... args) {
    return messageSource.getMessage(
        code, args.length > 0 ? args : null, code, LocaleContextHolder.getLocale());
  }

  protected BigDecimal resolveAccountManualPrincipal(Account account) {
    if (account == null || account.jsonConfig() == null || account.jsonConfig().isEmpty()) {
      return null;
    }

    for (String key : ACCOUNT_PRINCIPAL_CONFIG_KEYS) {
      BigDecimal parsed = parseJsonBigDecimal(account.jsonConfig().get(key));
      if (parsed != null && parsed.compareTo(BigDecimal.ZERO) >= 0) {
        return parsed;
      }
    }

    return null;
  }

  private BigDecimal parseJsonBigDecimal(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof BigDecimal decimalValue) {
      return decimalValue;
    }

    if (value instanceof Number numberValue) {
      try {
        return new BigDecimal(numberValue.toString());
      } catch (NumberFormatException ignored) {
        return null;
      }
    }

    if (value instanceof String stringValue) {
      String normalized = stringValue.replace(",", "").trim();
      if (normalized.isEmpty()) {
        return null;
      }

      try {
        return new BigDecimal(normalized);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }

    return null;
  }

  /**
   * 손익 보강에 쓰는 이름 맵. 한 요청에서 손익을 여러 번 계산해도 계좌/종목 목록은 같으므로 한 번만 읽어 돌려쓴다.
   *
   * <p>없이 호출하면 보강할 때마다 두 목록을 다시 읽는다(측정: 매매내역 한 요청에 계좌 4회, 종목목록 4회 = 43KB).
   */
  // 보유 원가 계산은 요약 화면과 포트폴리오 화면이 함께 쓴다. 예전에는 두 곳이 따로 계산했고
  // 폴백이 서로 달랐다 - 포트폴리오는 '평균단가 x 보유수량'(실측 오차 5 원), 요약은 totalBuyCost
  // (실측 735,958,622 로 실제 632,223,825 보다 103,734,796 원 과대). 같은 뜻의 값이 화면마다
  // 달라지지 않도록 한 곳에 둔다.
  protected BigDecimal resolveCurrentHoldingCost(TradeProfit holding) {
    if (holding == null) {
      return BigDecimal.ZERO;
    }

    return resolveCurrentHoldingCost(
        holding.evaluationAmount(),
        holding.evaluationProfit(),
        holding.averageBuyPrice(),
        holding.holdingQuantity());
  }

  /**
   * 지금 보유분의 원가.
   *
   * <p>api-stock 이 {@code evaluationProfit = 평가액 - 원가} 로 두므로 {@code 평가액 - 평가손익} 이 곧 원가다(실측: 이렇게 구한
   * 합 632,223,826 이 api-stock 시계열 보유원가와 정확히 같다). 현재가가 없어 평가액이 0 이어도 항등식은 그대로 성립한다.
   *
   * <p>폴백은 {@code 평균단가 x 보유수량} 이다. 예전에는 {@code totalBuyAmount} 를 썼는데 그것은 <b>기간 누적 매수액</b>이라 성격이
   * 다르다(실측: 포트폴리오 735,929,747 vs 실제 보유원가 632,223,826, 삼성전자는 466,231,000 vs 362,525,079 로 29% 과대).
   * 지금 api-stock 은 두 값을 항상 채우므로 이 폴백은 실제로 타지 않지만, 타는 날에 틀린 값을 쓰면 안 된다.
   */
  protected BigDecimal resolveCurrentHoldingCost(
      BigDecimal evaluationAmount,
      BigDecimal evaluationProfit,
      BigDecimal averageBuyPrice,
      int holdingQuantity) {
    if (evaluationAmount != null && evaluationProfit != null) {
      return evaluationAmount.subtract(evaluationProfit);
    }

    if (averageBuyPrice != null && holdingQuantity > 0) {
      return averageBuyPrice.multiply(BigDecimal.valueOf(holdingQuantity));
    }

    return BigDecimal.ZERO;
  }

  protected record TradeProfitNames(
      Map<UUID, String> accountNames, Map<UUID, String> stockItemNames) {}

  /** 이미 조회해 둔 목록이 있으면 그것으로 이름 맵을 만든다(추가 조회 없음). */
  protected TradeProfitNames toTradeProfitNames(
      List<Account> accountList, List<StockItem> stockItemList) {
    String unknownLabel = msg("stock.label.unknown");
    return new TradeProfitNames(
        emptyIfNull(accountList).stream()
            .filter(account -> account != null && account.id() != null)
            .collect(
                Collectors.toMap(
                    Account::id,
                    account -> account.name() != null ? account.name() : unknownLabel,
                    (a, b) -> a)),
        emptyIfNull(stockItemList).stream()
            .collect(Collectors.toMap(StockItem::id, StockItem::name, (a, b) -> a)));
  }

  // Helper to get enriched data
  /**
   * 요청된 필터 id 중 이 사용자에게 실제로 있는 것만 남긴다.
   *
   * <p>예전에는 하나라도 없는 id 가 섞이면 필터를 통째로 버려 "전체 보기"가 됐다. 필터는 넓어지는 쪽이 아니라 좁아지는 쪽으로 실패해야 한다 — 실측: 유효 계좌
   * 1개 + 없는 계좌 1개를 주면 26행으로 좁아지는 대신 250행 전체가 나왔다.
   *
   * @return 필터를 요청하지 않았으면 null(= 필터 없음), 요청했으면 유효한 id 만 남긴 목록(전부 무효면 빈 목록 = 결과 없음)
   */
  /** 날짜 없이 프리셋 상태만 왔을 때 적용할 기간. {@code end} 는 '오늘까지'를 뜻하는 배타적 경계다. */
  protected record PresetRange(java.time.Instant start, java.time.Instant end, String mode) {}

  /**
   * 화면의 기간 프리셋을 서버에서 그대로 계산한다.
   *
   * <p>{@code rangeMode} 는 어떤 프리셋 버튼이 눌렸는지 알리는 상태값이고 기간 자체는 {@code startDate}/{@code endDate} 로 온다.
   * 그래서 날짜 없이 이 값만 오면 기간이 정해지지 않는다. 화면 쪽 계산( {@code date-range-picker.ts})은 선택 상태가 없을 때 오늘을 기준으로
   * 삼고, N 개월 프리셋은 "정확히 N 개월"이 되도록 시작일을 하루 밀어 준다. 여기서는 그 규칙을 그대로 옮긴다.
   *
   * <p>{@code "all"} 은 이 메서드를 부르기 전에 호출부가 걸러 낸다(기간 없음이 곧 의도다). 알 수 없는 값은 예전 기본값인 올해(YTD)로 떨어뜨린다.
   */
  protected PresetRange resolvePresetRange(String rangeMode, ZoneId zone) {
    java.time.LocalDate today = java.time.LocalDate.now(zone);
    String mode = rangeMode == null ? "" : rangeMode.trim();
    java.time.LocalDate from;
    String resolvedMode;
    if ("mtd".equalsIgnoreCase(mode)) {
      from = today.withDayOfMonth(1);
      resolvedMode = "mtd";
    } else if (mode.matches("[1-9][0-9]{0,3}") && Long.parseLong(mode) <= 1200L) {
      // 화면과 같은 규칙: minusMonths 는 양끝 포함이라 하루를 더해 정확히 N 개월로 만든다.
      from = today.minusMonths(Long.parseLong(mode)).plusDays(1);
      resolvedMode = mode;
    } else {
      from = java.time.LocalDate.of(today.getYear(), 1, 1);
      resolvedMode = "ytd";
    }
    return new PresetRange(
        from.atStartOfDay(zone).toInstant(),
        today.plusDays(1).atStartOfDay(zone).toInstant(),
        resolvedMode);
  }

  /**
   * 요청에 실린 계좌 필터를 이 사용자의 실제 계좌로 좁힌다.
   *
   * <p>없는 계좌 id 가 하나라도 섞이면 api-stock 이 요청 전체를 거절한다(계좌 소유 검증). 그러면 화면이 "불러오지 못했습니다" 로 통째로 죽는다 — 저장해
   * 둔 선택이 가리키던 계좌가 지워졌을 때 그렇게 된다(실측: summary / asset-status / asset-growth / portfolio 가 가짜 계좌 id
   * 하나에 전부 오류 화면). 목록·활동 화면은 이미 교집합을 취해 정상 동작하므로 같은 방식으로 맞춘다.
   *
   * @return 요청에 계좌 필터가 없으면 {@code null}, 있으면 유효한 것만 남긴 목록(하나도 유효하지 않으면 빈 목록)
   */
  /**
   * 종목 필터를 실제로 존재하는 종목으로 좁힌다.
   *
   * <p>지금까지 이 자리는 {@code containsAll} 로 "하나라도 없는 id 가 섞이면 선택 전체를 버리는" 전부-아니면-전무였다. 그래서 유효한 종목 하나와
   * 지워진 종목 하나를 함께 고르면 결과가 통째로 비었다(실측: trade/list 금액 9개 -> 0개). 바로 옆 계좌 필터는 이미 교집합을 취하고 있어 같은 화면
   * 안에서도 규칙이 달랐다.
   *
   * @return 필터 자체가 없으면 {@code null}, 있으면 유효한 것만 남긴 목록(하나도 없으면 빈 목록 = '고른 게 없음')
   */
  protected List<UUID> retainAvailableStockItemIds(
      StockTagSelection selection, java.util.Set<UUID> availableStockIds) {
    if (selection == null || !selection.hasFilter()) {
      return null;
    }
    List<UUID> retained = retainAvailableIds(selection.requestedStockItemIds(), availableStockIds);
    return retained != null ? retained : List.of();
  }

  /**
   * 계좌 필터가 있으면 <b>보내기 전에</b> 이 사용자의 계좌로 좁힌다. 좁힌 결과가 비면 {@code true} 를 돌려준다.
   *
   * <p>두 가지가 함께 걸려 있다.
   *
   * <ul>
   *   <li>남의 계좌 id 가 섞여 오면 백엔드가 요청을 거절해 화면이 통째로 오류가 됐다. 그래서 보내기 전에 좁힌다.
   *   <li>좁힌 결과가 <b>빈 목록</b>이면 그대로 보낼 수 없다 &mdash; 파라미터가 아예 빠져 '필터 없음'(= 전체)이 되어 오히려 전부 보인다. 호출부는 이
   *       반환값이 참이면 조회를 건너뛴다.
   * </ul>
   *
   * <p>필터가 없으면 계좌 조회를 <b>기다리지 않는다</b>(좁힐 것이 없다). 이 지연 join 이 성능상 의도된 부분이다.
   *
   * <p>예전에는 같은 코드가 자산성장·포트폴리오(2곳)에 복사돼 있었다. 한쪽만 고쳐지면 그 화면만 남의 계좌를 그대로 보내거나 필터가 전체로 뒤집힌다.
   */
  protected boolean narrowToOwnedAccounts(
      TradeProfitRequest request, CompletableFuture<List<Account>> accountsFuture) {
    boolean hasAccountFilter =
        request.getAccountIdList() != null && !request.getAccountIdList().isEmpty();
    if (!hasAccountFilter) {
      return false;
    }
    request.setAccountIdList(
        retainOwnedAccountIds(
            request.getAccountIdList(),
            net.luversof.web.gate.stock.support.StockAsyncSupport.join(accountsFuture)));
    return request.getAccountIdList().isEmpty();
  }

  protected List<UUID> retainOwnedAccountIds(List<UUID> requestedIds, List<Account> accountList) {
    return retainAvailableIds(
        requestedIds,
        emptyIfNull(accountList).stream()
            .map(Account::id)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet()));
  }

  protected static List<UUID> retainAvailableIds(
      List<UUID> requestedIds, java.util.Set<UUID> availableIds) {
    if (requestedIds == null || requestedIds.isEmpty()) {
      return null;
    }
    return requestedIds.stream()
        .filter(java.util.Objects::nonNull)
        .filter(availableIds::contains)
        .distinct()
        .toList();
  }

  protected List<TradeProfit> getEnrichedTradeProfits(TradeProfitRequest request) {
    return getEnrichedTradeProfits(request, (TradeProfitNames) null);
  }

  protected List<TradeProfit> getEnrichedTradeProfits(
      TradeProfitRequest request, TradeProfitNames names) {
    return enrichTradeProfits(
        emptyIfNull(tradeProfitClient.calculateProfit(request.toParams())),
        request.getUserId(),
        names);
  }

  /**
   * 이미 조회한 손익 목록에 계좌명/종목명을 붙인다. 원격 호출을 미리(혹은 병렬로) 끝낸 뒤 이름만 입히는 경우에 쓴다. 메시지 조회가 LocaleContextHolder
   * 에 의존하므로 반드시 요청 스레드에서 호출할 것.
   */
  protected List<TradeProfit> enrichTradeProfits(
      List<TradeProfit> tradeProfitList, UUID userId, TradeProfitNames names) {
    String unknownLabel = msg("stock.label.unknown");

    // 계좌명은 사용자 계좌 목록 1회 조회로 해결한다.
    // (이전엔 등장 계좌 수만큼 getAccountById 를 호출해 요청당 N번의 HTTP 왕복이 발생했다.)
    Map<UUID, String> accountNames =
        names != null
            ? names.accountNames()
            : emptyIfNull(accountClient.getAccountsByUserId(userId)).stream()
                .filter(account -> account != null && account.id() != null)
                .collect(
                    Collectors.toMap(
                        Account::id,
                        account -> account.name() != null ? account.name() : unknownLabel,
                        (a, b) -> a));

    Map<UUID, String> stockItemNames =
        names != null
            ? names.stockItemNames()
            : emptyIfNull(stockItemClient.getStockItems()).stream()
                .collect(Collectors.toMap(StockItem::id, StockItem::name, (a, b) -> a));

    return tradeProfitList.stream()
        .map(
            profit ->
                TradeProfit.withNames(
                    profit,
                    stockItemNames.getOrDefault(profit.stockItemId(), unknownLabel),
                    profit.accountId() != null
                        ? accountNames.getOrDefault(profit.accountId(), unknownLabel)
                        : null))
        .toList();
  }

  protected List<TradeProfit> getEnrichedTradeProfits(
      TradeProfitRequest request, TradeProfitRequestGroup groupBy) {
    return getEnrichedTradeProfits(request, groupBy, null);
  }

  protected List<TradeProfit> getEnrichedTradeProfits(
      TradeProfitRequest request, TradeProfitRequestGroup groupBy, TradeProfitNames names) {
    if (groupBy == null || groupBy == request.getGroupBy()) {
      return getEnrichedTradeProfits(request, names);
    }

    TradeProfitRequest requestCopy = copyTradeProfitRequest(request);
    requestCopy.setGroupBy(groupBy);
    return getEnrichedTradeProfits(requestCopy, names);
  }

  protected TradeProfitRequest copyTradeProfitRequest(TradeProfitRequest request) {
    TradeProfitRequest requestCopy = new TradeProfitRequest();
    requestCopy.setUserId(request.getUserId());
    requestCopy.setAccountIdList(request.getAccountIdList());
    requestCopy.setStockItemIdList(request.getStockItemIdList());
    requestCopy.setStartDate(request.getStartDate());
    requestCopy.setEndDate(request.getEndDate());
    requestCopy.setTimeZone(request.getTimeZone());
    requestCopy.setGroupBy(request.getGroupBy());
    return requestCopy;
  }

  /** 종목 단위(계좌 무시)로 그룹핑된 손익 목록을 반환한다. */
  protected List<TradeProfit> getStockGroupedTradeProfits(
      TradeProfitRequest request, boolean includeZeroHoldings) {
    return getStockGroupedTradeProfits(request, includeZeroHoldings, null);
  }

  protected List<TradeProfit> getStockGroupedTradeProfits(
      TradeProfitRequest request, boolean includeZeroHoldings, TradeProfitNames names) {
    List<TradeProfit> stockGroupedTradeProfits =
        new ArrayList<>(getEnrichedTradeProfits(request, TradeProfitRequestGroup.STOCKITEM, names));
    if (!includeZeroHoldings) {
      stockGroupedTradeProfits.removeIf(tp -> tp.holdingQuantity() == 0);
    }
    return stockGroupedTradeProfits;
  }

  /** 종목별 실현손익 행으로 변환한다 (수수료/세금 반영 + 매수/매도 금액 포함). */
  protected TradeProfit toStockRealized(TradeProfit profit) {
    return TradeProfit.ofStockRealized(
        profit.stockItemId(),
        profit.stockItemName(),
        profit.holdingQuantity(),
        profit.totalSellQuantity(),
        profit.totalBuyAmount(),
        profit.totalSellAmount(),
        profit.evaluationAmount(),
        profit.evaluationProfit(),
        profit.realizedProfit(),
        profit.realizedProfitNet(),
        profit.totalBuyCost(),
        profit.totalSellProceeds(),
        profit.totalBuyFee(),
        profit.totalSellFee(),
        profit.totalSellTax());
  }

  protected List<String> getAvailableStockTags(List<StockItem> stockItemList) {
    if (stockItemList == null || stockItemList.isEmpty()) {
      return List.of();
    }

    return stockItemList.stream()
        .filter(Objects::nonNull)
        .flatMap(stockItem -> stockItem.tags() != null ? stockItem.tags().stream() : Stream.empty())
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .sorted(String::compareToIgnoreCase)
        .toList();
  }

  protected static <T> List<T> emptyIfNull(List<T> values) {
    return values != null ? values : List.of();
  }

  protected StockTagSelection resolveStockTagSelection(
      List<StockItem> stockItemList, List<UUID> stockItemIdList, List<String> stockTagList) {
    List<String> selectedStockTags = normalizeStockTags(stockTagList);
    boolean hasFilter =
        (stockItemIdList != null && !stockItemIdList.isEmpty()) || !selectedStockTags.isEmpty();

    if (!hasFilter) {
      return new StockTagSelection(selectedStockTags, null, false);
    }

    var requestedStockItemIds = new LinkedHashSet<UUID>();
    if (stockItemIdList != null) {
      stockItemIdList.stream().filter(Objects::nonNull).forEach(requestedStockItemIds::add);
    }

    if (!selectedStockTags.isEmpty() && stockItemList != null) {
      stockItemList.stream()
          .filter(Objects::nonNull)
          .filter(stockItem -> stockItem.id() != null)
          .filter(stockItem -> stockItem.tags() != null && !stockItem.tags().isEmpty())
          .filter(
              stockItem ->
                  stockItem.tags().stream()
                      .filter(StringUtils::hasText)
                      .map(String::trim)
                      .anyMatch(selectedStockTags::contains))
          .map(StockItem::id)
          .forEach(requestedStockItemIds::add);
    }

    return new StockTagSelection(selectedStockTags, new ArrayList<>(requestedStockItemIds), true);
  }

  private List<String> normalizeStockTags(List<String> stockTagList) {
    if (stockTagList == null || stockTagList.isEmpty()) {
      return List.of();
    }

    var normalizedStockTags = new LinkedHashSet<String>();
    stockTagList.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .forEach(normalizedStockTags::add);
    return new ArrayList<>(normalizedStockTags);
  }

  protected record StockTagSelection(
      List<String> selectedStockTags, List<UUID> requestedStockItemIds, boolean hasFilter) {}
}
