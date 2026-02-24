@import java.util.List
@import java.util.UUID
@import java.time.LocalDate
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.DividendView
@import net.luversof.web.common.menu.domain.Pagination
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.StockItem
@import java.time.format.DateTimeFormatter
@import java.text.DecimalFormat

@param List<DividendView> dividendList
@param Pagination pagination
@param int totalItems
@param int totalPages
@param int currentPage
@param int size
@param List<Account> accountList
@param List<StockItem> stockItemList
@param UUID selectedAccountId
@param UUID selectedStockItemId
@param LocalDate startDate
@param LocalDate endDate
@param String sort
@param BigDecimal totalGrossAmount
@param BigDecimal totalNetAmount
@param BigDecimal totalTax
@param BigDecimal totalTaxableAmount

!{
    String sortField = sort != null ? sort.split(",")[0] : "";
    String sortDir = sort != null && sort.contains(",") ? sort.split(",")[1] : "";
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DecimalFormat decimalFormat = new DecimalFormat("#,##0");
}

<div id="dividendListFragment">
	<!-- Filter & Search Form -->
	<form id="dividendSearchForm" class="flex flex-wrap gap-2 mb-4 items-end bg-base-100 p-2 rounded-box shadow-sm"
	      hx-get="/stock/htmx/dividend/list" 
	      hx-target="#dividendListContainer" 
	      hx-select="#dividendListContainer"
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
		        hx-get="/stock/htmx/dividend/list" 
		        hx-target="#dividendListFragment" 
		        hx-select="#dividendListFragment"
		        hx-swap="outerHTML">초기화</button>
	</form>

	<div id="dividendListContainer" class="flex flex-col h-full relative">
	    <div class="overflow-x-auto flex-1 min-h-0">
	        <table class="table table-hover table-striped w-full table-sm table-pin-rows">
	            <thead>
	            <tr class="bg-base-200">
	                <th class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListContainer" hx-select="#dividendListContainer" hx-swap="outerHTML" hx-include="#dividendSearchForm"
	                    hx-vals='{"sort": "payDate,${sortField.equals("payDate") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
	                    지급일 @if(sortField.equals("payDate"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
	                </th>
	                <th class="cursor-pointer hover:bg-base-300 hidden sm:table-cell" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListContainer" hx-select="#dividendListContainer" hx-swap="outerHTML" hx-include="#dividendSearchForm"
	                    hx-vals='{"sort": "accountName,${sortField.equals("accountName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
	                    계좌 @if(sortField.equals("accountName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
	                </th>
	                <th class="cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListContainer" hx-select="#dividendListContainer" hx-swap="outerHTML" hx-include="#dividendSearchForm"
	                    hx-vals='{"sort": "stockItemName,${sortField.equals("stockItemName") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
	                    종목명 @if(sortField.equals("stockItemName"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
	                </th>
	                <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListContainer" hx-select="#dividendListContainer" hx-swap="outerHTML" hx-include="#dividendSearchForm"
	                    hx-vals='{"sort": "grossAmount,${sortField.equals("grossAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
	                    배당금(세전) @if(sortField.equals("grossAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
	                </th>
	                <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListContainer" hx-select="#dividendListContainer" hx-swap="outerHTML" hx-include="#dividendSearchForm"
	                    hx-vals='{"sort": "netAmount,${sortField.equals("netAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
	                    배당금(세후) @if(sortField.equals("netAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
	                </th>
	                <th class="text-right cursor-pointer hover:bg-base-300 hidden lg:table-cell" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListContainer" hx-select="#dividendListContainer" hx-swap="outerHTML" hx-include="#dividendSearchForm"
	                    hx-vals='{"sort": "tax,${sortField.equals("tax") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
	                    세금 @if(sortField.equals("tax"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
	                </th>
	                <th class="text-right cursor-pointer hover:bg-base-300" hx-get="/stock/htmx/dividend/list" hx-target="#dividendListContainer" hx-select="#dividendListContainer" hx-swap="outerHTML" hx-include="#dividendSearchForm"
	                    hx-vals='{"sort": "taxableAmount,${sortField.equals("taxableAmount") && sortDir.equals("asc") ? "desc" : "asc"}"}'>
	                    과세금액 @if(sortField.equals("taxableAmount"))<span>${sortDir.equals("asc") ? "▲" : "▼"}</span>@endif
	                </th>
	            </tr>
	            </thead>
	            <tbody>
	            @for(DividendView item : dividendList)
	            <tr>
	                <td>${item.payDate() != null ? dateFormatter.format(item.payDate().atZone(java.time.ZoneId.systemDefault())) : ""}</td>
	                <td class="hidden sm:table-cell">${item.accountName()}</td>
	                <td class="font-bold">${item.stockItemName()}</td>
	                <td class="text-right amount-value">${item.grossAmount() != null ? decimalFormat.format(item.grossAmount()) : "0"}</td>
	                <td class="text-right text-dividend font-bold amount-value">${item.netAmount() != null ? decimalFormat.format(item.netAmount()) : "0"}</td>
	                <td class="text-right hidden lg:table-cell amount-value">${item.tax() != null ? decimalFormat.format(item.tax()) : "0"}</td>
	                <td class="text-right amount-value">${item.taxableAmount() != null ? decimalFormat.format(item.taxableAmount()) : "0"}</td>
	            </tr>
	            @endfor
	            @if(dividendList == null || dividendList.isEmpty())
	            <tr>
		          	<td colspan="7" class="text-center py-10 opacity-50">배당 내역이 없습니다.</td>
		        </tr>
		        @endif
	            </tbody>
	            <!-- Footer -->
				<tfoot>
					<tr class="font-bold bg-base-200">
						<td colspan="3" class="text-center sm:text-right sm:pr-4">합계</td>
						<td class="text-right font-mono amount-value">${totalGrossAmount != null ? decimalFormat.format(totalGrossAmount) : "0"}</td>
						<td class="text-right font-mono text-dividend amount-value">${totalNetAmount != null ? decimalFormat.format(totalNetAmount) : "0"}</td>
						<td class="text-right font-mono hidden lg:table-cell amount-value">${totalTax != null ? decimalFormat.format(totalTax) : "0"}</td>
						<td class="text-right font-mono amount-value">${totalTaxableAmount != null ? decimalFormat.format(totalTaxableAmount) : "0"}</td>
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
				        hx-get="/stock/htmx/dividend/list" 
				        hx-include="#dividendSearchForm"
				        hx-vals='{"page": ${pagination.getFirstNav().page()}}'
				        hx-target="#dividendListContainer"
				        hx-select="#dividendListContainer"
				        hx-swap="outerHTML">≪</button>
				        
				<!-- Previous -->
				<button class="join-item btn btn-sm ${!pagination.getPrevNav().isActive() ? "btn-disabled" : ""}" 
				        disabled="${!pagination.getPrevNav().isActive()}"
				        hx-get="/stock/htmx/dividend/list"
				        hx-include="#dividendSearchForm"
				        hx-vals='{"page": ${pagination.getPrevNav().page()}}'
				        hx-target="#dividendListContainer"
				        hx-select="#dividendListContainer"
				        hx-swap="outerHTML">&lt;</button>

				<!-- Page Numbers -->
				@for(Pagination.Nav nav : pagination.getNavList())
				<button class="join-item btn btn-sm ${nav.isActive() ? "btn-active" : ""}" 
				        hx-get="/stock/htmx/dividend/list"
				        hx-include="#dividendSearchForm"
				        hx-vals='{"page": ${nav.page()}}'
				        hx-target="#dividendListContainer"
				        hx-select="#dividendListContainer"
				        hx-swap="outerHTML">${nav.page()}</button>
				@endfor

				<!-- Next -->
				<button class="join-item btn btn-sm ${!pagination.getNextNav().isActive() ? "btn-disabled" : ""}" 
				        disabled="${!pagination.getNextNav().isActive()}"
				        hx-get="/stock/htmx/dividend/list"
				        hx-include="#dividendSearchForm"
				        hx-vals='{"page": ${pagination.getNextNav().page()}}'
				        hx-target="#dividendListContainer"
				        hx-select="#dividendListContainer"
				        hx-swap="outerHTML">&gt;</button>
				        
				<!-- Last -->
				<button class="join-item btn btn-sm ${!pagination.getLastNav().isActive() ? "btn-disabled" : ""}" 
				        disabled="${!pagination.getLastNav().isActive()}"
				        hx-get="/stock/htmx/dividend/list"
				        hx-include="#dividendSearchForm"
				        hx-vals='{"page": ${pagination.getLastNav().page()}}'
				        hx-target="#dividendListContainer"
				        hx-select="#dividendListContainer"
				        hx-swap="outerHTML">≫</button>
			</div>
		</div>
		@endif
	</div>
</div>