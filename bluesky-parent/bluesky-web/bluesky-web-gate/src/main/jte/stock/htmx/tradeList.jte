@import java.util.List
@import java.util.Map
@import java.util.UUID
@import java.time.LocalDate
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.common.menu.domain.Pagination
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.constant.TradeType
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat

@param BigDecimal totalFee
@param BigDecimal totalTax
@param BigDecimal totalRealizedProfit
@param List<TradeResponse> tradeList
@param Pagination pagination
@param Map<UUID, String> accountNames
@param List<Account> accountList
@param List<StockItem> stockItemList
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param LocalDate startDate
@param LocalDate endDate

!{
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DecimalFormat decimalFormat = new DecimalFormat("#,##0");
}

<div id="tradeListFragment">
	<!-- Filter & Search Form -->
	<form id="tradeSearchForm" class="flex flex-wrap gap-2 mb-4 items-end bg-base-100 p-2 rounded-box shadow-sm"
	      hx-get="/stock/htmx/trade/list" 
	      hx-target="#tradeListContainer" 
	      hx-select="#tradeListContainer"
	      hx-swap="outerHTML"
	      hx-trigger="submit">
		
		<div class="form-control w-full sm:w-auto">
			<label class="label p-1"><span class="label-text text-xs">기간</span></label>
			<div class="join">
				<input type="date" name="startDate" class="input input-sm input-bordered join-item" value="${startDate != null ? startDate.toString() : ""}">
				<input type="date" name="endDate" class="input input-sm input-bordered join-item" value="${endDate != null ? endDate.toString() : ""}">
			</div>
		</div>

		<div class="form-control w-full sm:w-auto">
			<label class="label p-1"><span class="label-text text-xs">계좌</span></label>
			<select name="accountIdList" class="select select-sm select-bordered">
				<option value="">전체</option>
				@for(Account account : accountList)
				<option value="${account.id().toString()}" selected="${account.id().equals(selectedAccountId)}">${account.name()}</option>
				@endfor
			</select>
		</div>

		<div class="form-control w-full sm:w-auto">
			<label class="label p-1"><span class="label-text text-xs">종목</span></label>
			<select name="stockItemIdList" class="select select-sm select-bordered">
				<option value="">전체</option>
				@for(StockItem stockItem : stockItemList)
				<option value="${stockItem.id().toString()}" selected="${stockItem.id().equals(selectedStockItemId)}">${stockItem.name()}</option>
				@endfor
			</select>
		</div>

		<button type="submit" class="btn btn-sm btn-primary">조회</button>
		<button type="button" class="btn btn-sm text-xs" 
		        hx-get="/stock/htmx/trade/list" 
		        hx-target="#tradeListFragment" 
		        hx-select="#tradeListFragment"
		        hx-swap="outerHTML">초기화</button>
	</form>

	<div id="tradeListContainer" class="flex flex-col h-full relative">
		<div class="overflow-x-auto flex-1 min-h-0">
		  <table class="table table-hover table-striped w-full table-sm table-pin-rows">
		      <thead>
		          <tr class="bg-base-200">
		              <th>날짜</th>
		              <th>종목</th>
		              <th>구분</th>
		              <th>계좌</th>
		              <th class="text-right">단가</th>
		              <th class="text-right">수량</th>
		              <th class="text-right">금액</th>
		              <th class="text-right">수수료</th>
		              <th class="text-right">거래세</th>
		              <th class="text-right">실현손익</th>
		          </tr>
		      </thead>
		      <tbody>
		          @for(TradeResponse trade : tradeList)
		          <tr>
		              <td>${trade.tradeDate() != null ? dateFormatter.format(trade.tradeDate().atZone(java.time.ZoneId.systemDefault())) : ""}</td>
		              <td class="font-bold whitespace-nowrap">${trade.stockItemName()}</td>
		              <td>
		                  <span class="${trade.type() == TradeType.BUY ? "badge badge-error badge-outline badge-sm" : "badge badge-info badge-outline badge-sm"}">
		                      ${trade.type() == TradeType.BUY ? "매수" : "매도"}
		                  </span>
		              </td>
		              <td class="whitespace-nowrap">${accountNames.get(trade.accountId())}</td>
		              <td class="text-right font-mono amount-value">${trade.price() != null ? decimalFormat.format(trade.price()) : "0"}</td>
		              <td class="text-right font-mono amount-value">${decimalFormat.format(trade.quantity())}</td>
		              <td class="text-right font-mono amount-value">${trade.amount() != null ? decimalFormat.format(trade.amount()) : "0"}</td>
		              <td class="text-right text-xs text-base-content/60 amount-value">${trade.fee() != null ? decimalFormat.format(trade.fee()) : "0"}</td>
		              <td class="text-right text-xs text-base-content/60 amount-value">${trade.tax() != null ? decimalFormat.format(trade.tax()) : "0"}</td>
		              <td class="text-right font-bold font-mono amount-value ${trade.realizedProfit() != null ? (trade.realizedProfit().compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss") : ""}">
		                  ${trade.realizedProfit() != null ? decimalFormat.format(trade.realizedProfit()) : ""}
		              </td>
		          </tr>
		          @endfor
		          @if(tradeList == null || tradeList.isEmpty())
		          <tr>
		          	<td colspan="10" class="text-center py-10 opacity-50">거래 내역이 없습니다.</td>
		          </tr>
		          @endif
		      </tbody>
		      <!-- Footer -->
		      <tfoot>
		          <tr class="font-bold bg-base-200">
		              <td colspan="7" class="text-center">합계</td>
		              <td class="text-right font-mono amount-value">${totalFee != null ? decimalFormat.format(totalFee) : "0"}</td>
		              <td class="text-right font-mono amount-value">${totalTax != null ? decimalFormat.format(totalTax) : "0"}</td>
		              <td class="text-right font-mono amount-value ${totalRealizedProfit != null && totalRealizedProfit.compareTo(BigDecimal.ZERO) >= 0 ? "text-profit" : "text-loss"}">
		                  ${totalRealizedProfit != null ? decimalFormat.format(totalRealizedProfit) : "0"}
		              </td>
		          </tr>
		      </tfoot>
		  </table>
		</div>
		
		<!-- Pagination -->
		@if(pagination != null && pagination.getTotalPage() > 1)
		<div class="flex justify-center p-2 border-t border-base-200 bg-base-100">
			<div class="join">
				<!-- First -->
				<button class="join-item btn btn-sm ${!pagination.getFirstNav().isActive() ? "btn-disabled" : ""}" 
				        disabled="${!pagination.getFirstNav().isActive()}"
				        hx-get="/stock/htmx/trade/list" 
				        hx-include="#tradeSearchForm"
				        hx-vals='{"page": ${pagination.getFirstNav().page()}}'
				        hx-target="#tradeListContainer"
				        hx-select="#tradeListContainer"
				        hx-swap="outerHTML">≪</button>
				        
				<!-- Previous -->
				<button class="join-item btn btn-sm ${!pagination.getPrevNav().isActive() ? "btn-disabled" : ""}" 
				        disabled="${!pagination.getPrevNav().isActive()}"
				        hx-get="/stock/htmx/trade/list"
				        hx-include="#tradeSearchForm"
				        hx-vals='{"page": ${pagination.getPrevNav().page()}}'
				        hx-target="#tradeListContainer"
				        hx-select="#tradeListContainer"
				        hx-swap="outerHTML">＜</button>
				
				<!-- Page Numbers -->
				@for(Pagination.Nav nav : pagination.getNavList())
				<button class="join-item btn btn-sm ${nav.isActive() ? "btn-active" : ""}" 
				        hx-get="/stock/htmx/trade/list" 
				        hx-include="#tradeSearchForm"
				        hx-vals='{"page": ${nav.page()}}'
				        hx-target="#tradeListContainer"
				        hx-select="#tradeListContainer"
				        hx-swap="outerHTML">${nav.page()}</button>
				@endfor
				
				<!-- Next -->
				<button class="join-item btn btn-sm ${!pagination.getNextNav().isActive() ? "btn-disabled" : ""}" 
				        disabled="${!pagination.getNextNav().isActive()}"
				        hx-get="/stock/htmx/trade/list"
				        hx-include="#tradeSearchForm"
				        hx-vals='{"page": ${pagination.getNextNav().page()}}'
				        hx-target="#tradeListContainer"
				        hx-select="#tradeListContainer"
				        hx-swap="outerHTML">＞</button>

				<!-- Last -->
				<button class="join-item btn btn-sm ${!pagination.getLastNav().isActive() ? "btn-disabled" : ""}" 
				        disabled="${!pagination.getLastNav().isActive()}"
				        hx-get="/stock/htmx/trade/list"
				        hx-include="#tradeSearchForm"
				        hx-vals='{"page": ${pagination.getLastNav().page()}}'
				        hx-target="#tradeListContainer"
				        hx-select="#tradeListContainer"
				        hx-swap="outerHTML">≫</button>
			</div>
		</div>
		@endif
	</div>
</div>