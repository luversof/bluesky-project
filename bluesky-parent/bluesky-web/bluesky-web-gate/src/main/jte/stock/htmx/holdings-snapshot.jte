@import java.util.List
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.constant.TradeType

@param List<HoldingsSnapshotItem> holdings
@param List<TradeResponse> trades
@param String date
@param String tradePeriod

<div class="card bg-base-100 shadow-xl mt-4">
    <div class="card-body p-4">
        <div class="flex items-center justify-between mb-3">
            <h3 class="card-title text-base">
                📋 보유 종목 스냅샷
                @if(date != null && !date.isEmpty())
                    <span class="text-sm font-normal text-base-content/60 ml-2">${date} 기준</span>
                @endif
            </h3>
            <button type="button" class="btn btn-xs btn-ghost btn-circle"
                    onclick="document.getElementById('holdings-snapshot-container').innerHTML='';"
                    title="닫기">✕</button>
        </div>

        @if(holdings == null || holdings.isEmpty())
            <div class="text-center py-6 text-base-content/50">
                <p>해당 날짜의 보유 종목 데이터가 없습니다.</p>
                <p class="text-xs mt-1">차트 조회 후 스냅샷이 저장된 날짜만 조회 가능합니다.</p>
            </div>
        @else
            !{
                BigDecimal totalValue = holdings.stream()
                    .map(HoldingsSnapshotItem::value)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalUnrealized = holdings.stream()
                    .map(HoldingsSnapshotItem::unrealizedProfit)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                boolean totalPositive = totalUnrealized.compareTo(BigDecimal.ZERO) >= 0;
            }
            <div class="flex gap-4 mb-3 text-sm">
                <div class="stat bg-base-200 rounded-box py-2 px-4 flex-1">
                    <div class="stat-title text-xs">총 평가액</div>
                    <div class="stat-value text-base amount-value">${String.format("%,d", totalValue.longValue())}원</div>
                </div>
                <div class="stat bg-base-200 rounded-box py-2 px-4 flex-1">
                    <div class="stat-title text-xs">총 평가 손익</div>
                    <div class="stat-value text-base amount-value ${totalPositive ? "text-red-500" : "text-blue-500"}">
                        ${totalPositive ? "+" : ""}${String.format("%,d", totalUnrealized.longValue())}원
                    </div>
                </div>
            </div>
            <div class="overflow-x-auto">
                <table class="table table-xs table-zebra w-full">
                    <thead>
                        <tr class="text-xs text-base-content/60">
                            <th>종목명</th>
                            <th class="text-right">수량</th>
                            <th class="text-right">평균 단가</th>
                            <th class="text-right">기준 가격</th>
                            <th class="text-right">평가액</th>
                            <th class="text-right">평가 손익</th>
                            <th class="text-right">수익률</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(HoldingsSnapshotItem item : holdings)
                        !{
                            boolean isProfit = item.unrealizedProfit() != null && item.unrealizedProfit().compareTo(BigDecimal.ZERO) >= 0;
                            String colorClass = isProfit ? "text-red-500" : "text-blue-500";
                            String sign = isProfit ? "+" : "";
                            BigDecimal cost = item.avgCost() != null && item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) > 0
                                ? item.avgCost().multiply(item.quantity())
                                : BigDecimal.ZERO;
                            String pct = cost.compareTo(BigDecimal.ZERO) != 0 && item.unrealizedProfit() != null
                                ? String.format("%.2f", item.unrealizedProfit().divide(cost, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue())
                                : "0.00";
                            // 수량은 정수로 표시
                            String qtyDisplay = item.quantity() != null
                                ? String.format("%,d", item.quantity().longValue())
                                : "0";
                        }
                        <tr>
                            <td>
                                <div class="font-medium">${item.name()}</div>
                                @if(item.symbol() != null && !item.symbol().isEmpty())
                                    <div class="text-xs text-base-content/50">${item.symbol()}</div>
                                @endif
                            </td>
                            <td class="text-right amount-value">${qtyDisplay}</td>
                            <td class="text-right amount-value">${item.avgCost() != null ? String.format("%,d", item.avgCost().longValue()) : "-"}원</td>
                            <td class="text-right amount-value">${item.priceAtDate() != null ? String.format("%,d", item.priceAtDate().longValue()) : "-"}원</td>
                            <td class="text-right font-medium amount-value">${item.value() != null ? String.format("%,d", item.value().longValue()) : "-"}원</td>
                            <td class="text-right font-medium ${colorClass} amount-value">
                                ${sign}${item.unrealizedProfit() != null ? String.format("%,d", item.unrealizedProfit().longValue()) : "0"}원
                            </td>
                            <td class="text-right ${colorClass}">${sign}${pct}%</td>
                        </tr>
                        @endfor
                    </tbody>
                </table>
            </div>
            @if(trades != null && !trades.isEmpty())
            !{
                java.util.List<TradeResponse> buyTrades = trades.stream().filter(t -> t.type() == TradeType.BUY).collect(java.util.stream.Collectors.toList());
                java.util.List<TradeResponse> sellTrades = trades.stream().filter(t -> t.type() == TradeType.SELL).collect(java.util.stream.Collectors.toList());
                BigDecimal totalBuyAmt = buyTrades.stream().map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalSellAmt = sellTrades.stream().map(t -> t.amount() != null ? t.amount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalRealizedProfit = sellTrades.stream().map(t -> t.realizedProfit() != null ? t.realizedProfit() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            <div class="divider text-xs my-2">💱 거래 내역 <span class="font-normal text-base-content/50">(${tradePeriod != null ? tradePeriod : date})</span></div>
            <div class="flex gap-2 mb-2 text-xs flex-wrap">
                @if(!buyTrades.isEmpty())
                <div class="badge badge-error badge-outline gap-1">
                    매수 ${String.format("%,d", totalBuyAmt.longValue())}원
                </div>
                @endif
                @if(!sellTrades.isEmpty())
                <div class="badge badge-info badge-outline gap-1">
                    매도 ${String.format("%,d", totalSellAmt.longValue())}원
                    @if(totalRealizedProfit.compareTo(BigDecimal.ZERO) != 0)
                    &nbsp;|&nbsp;실현손익 ${totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "+" : ""}${String.format("%,d", totalRealizedProfit.longValue())}원
                    @endif
                </div>
                @endif
            </div>
            <div class="overflow-x-auto mb-2">
                <table class="table table-xs w-full">
                    <thead>
                        <tr class="text-xs text-base-content/60">
                            <th>구분</th>
                            <th>종목명</th>
                            <th class="text-right">수량</th>
                            <th class="text-right">단가</th>
                            <th class="text-right">거래금액</th>
                            <th class="text-right">실현손익</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for(TradeResponse trade : trades)
                        !{
                            boolean isBuy = trade.type() == TradeType.BUY;
                            String typeClass = isBuy ? "text-red-500 font-bold" : "text-blue-500 font-bold";
                            String typeLabel = isBuy ? "매수" : "매도";
                            boolean hasProfit = trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) != 0;
                            String profitSign = trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                            String profitClass = trade.realizedProfit() != null && trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-red-500" : "text-blue-500";
                        }
                        <tr>
                            <td><span class="${typeClass}">${typeLabel}</span></td>
                            <td>${trade.stockItemName() != null ? trade.stockItemName() : "-"}</td>
                            <td class="text-right amount-value">${String.format("%,d", (long)trade.quantity())}</td>
                            <td class="text-right amount-value">${trade.price() != null ? String.format("%,d", trade.price().longValue()) : "-"}원</td>
                            <td class="text-right font-medium amount-value">${trade.amount() != null ? String.format("%,d", trade.amount().longValue()) : "-"}원</td>
                            <td class="text-right amount-value ${hasProfit ? profitClass : "text-base-content/40"}">
                                @if(hasProfit)
                                    ${profitSign}${String.format("%,d", trade.realizedProfit().longValue())}원
                                @else
                                    -
                                @endif
                            </td>
                        </tr>
                        @endfor
                    </tbody>
                </table>
            </div>
            @endif
        @endif
    </div>
</div>
