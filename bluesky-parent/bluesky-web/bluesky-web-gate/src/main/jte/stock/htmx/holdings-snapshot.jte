@import java.util.List
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.HoldingsSnapshotItem

@param List<HoldingsSnapshotItem> holdings
@param String date

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
        @endif
    </div>
</div>
