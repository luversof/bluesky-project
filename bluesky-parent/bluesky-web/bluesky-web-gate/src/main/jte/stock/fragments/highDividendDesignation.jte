@import io.github.luversof.boot.context.support.MessageUtil
@import java.util.List
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.response.HighDividendDesignationResponse

<%-- 관리 화면 "고배당기업 지정" 탭(2026-11-27). 공용 값 - 회사 공시(KRX KIND)를 보고 사람이 넣는다.
     판정은 지급일 기간으로 한다: 결산배당은 다음 해 4월쯤 지급되어 지급 연도와 사업연도가 어긋난다. --%>
@param List<HighDividendDesignationResponse> designations = List.of()
@param List<StockItem> stockItems = List.of()
@param HighDividendDesignationResponse edit = null
@param boolean loadFailed = false
@param String result = ""

!{
	String resultKey = switch (result == null ? "" : result) {
		case "saved" -> "stock.admin.high.dividend.result.saved";
		case "deleted" -> "stock.admin.high.dividend.result.deleted";
		case "save-failed" -> "stock.admin.high.dividend.result.save.failed";
		case "delete-failed" -> "stock.admin.high.dividend.result.delete.failed";
		default -> null;
	};
	boolean resultIsError = result != null && result.endsWith("-failed");
	String editSymbol = edit != null && edit.stockItemSymbol() != null ? edit.stockItemSymbol() : "";
}

<div class="space-y-6" data-high-dividend-admin>
	@if(resultKey != null)
		<div class="alert ${resultIsError ? "alert-error" : "alert-success"}" role="alert">
			<span>${MessageUtil.getMessage(resultKey)}</span>
		</div>
	@endif
	@if(loadFailed)
		<div class="alert alert-warning" role="alert">
			<span>${MessageUtil.getMessage("stock.admin.high.dividend.load.failed")}</span>
		</div>
	@endif

	@template._components.ui.card(bodyClass = "gap-4", labelledBy = "high-dividend-form-title", body = @`
		<div class="space-y-2">
			<h2 id="high-dividend-form-title" class="card-title">${MessageUtil.getMessage(edit != null ? "stock.admin.high.dividend.form.edit.title" : "stock.admin.high.dividend.form.title")}</h2>
			<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.admin.high.dividend.form.desc")}</p>
		</div>
		<form method="post" action="/stock/admin/high-dividend" class="grid gap-4 md:grid-cols-2">
			@if(edit != null)
				<input type="hidden" name="id" value="${String.valueOf(edit.id())}" />
			@endif
			<label class="form-control min-w-0">
				<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.admin.high.dividend.field.symbol")}</span></div>
				<select name="symbol" class="select select-bordered w-full min-w-0 select-sm" required>
					<option value="" disabled selected="${editSymbol.isEmpty()}">${MessageUtil.getMessage("stock.admin.high.dividend.field.symbol.placeholder")}</option>
					@for(StockItem stockItem : stockItems)
						<option value="${stockItem.symbol()}" selected="${stockItem.symbol().equalsIgnoreCase(editSymbol)}">${stockItem.symbol()} · ${stockItem.name()}</option>
					@endfor
				</select>
			</label>
			<label class="form-control min-w-0">
				<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.admin.high.dividend.field.business.year")}</span></div>
				<input name="businessYear" type="number" min="2025" max="2100" step="1" required value="${edit != null ? String.valueOf(edit.businessYear()) : ""}" class="input input-bordered w-full min-w-0 input-sm" />
			</label>
			<label class="form-control min-w-0">
				<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.admin.high.dividend.field.pay.date.from")}</span></div>
				<input name="payDateFrom" type="date" required value="${edit != null ? String.valueOf(edit.payDateFrom()) : ""}" class="input input-bordered w-full min-w-0 input-sm" />
			</label>
			<label class="form-control min-w-0">
				<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.admin.high.dividend.field.pay.date.to")}</span></div>
				<input name="payDateTo" type="date" required value="${edit != null ? String.valueOf(edit.payDateTo()) : ""}" class="input input-bordered w-full min-w-0 input-sm" />
			</label>
			<label class="form-control min-w-0 md:col-span-2">
				<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.admin.high.dividend.field.note")}</span></div>
				<input name="note" type="text" maxlength="1000" value="${edit != null && edit.note() != null ? edit.note() : ""}" class="input input-bordered w-full min-w-0 input-sm" />
			</label>
			<div class="flex flex-wrap gap-2 md:col-span-2">
				<button type="submit" class="btn btn-primary btn-sm">${MessageUtil.getMessage("stock.admin.high.dividend.submit")}</button>
				@if(edit != null)
					<a href="/stock/admin?tab=high-dividend" class="btn btn-outline btn-sm">${MessageUtil.getMessage("stock.admin.high.dividend.cancel")}</a>
				@endif
			</div>
		</form>
	`)

	@template._components.ui.card(bodyClass = "gap-4", labelledBy = "high-dividend-list-title", body = @`
		<h2 id="high-dividend-list-title" class="card-title">${MessageUtil.getMessage("stock.admin.high.dividend.list.title")}</h2>
		@if(designations == null || designations.isEmpty())
			<p class="text-sm text-base-content/60">${MessageUtil.getMessage("stock.admin.high.dividend.list.empty")}</p>
		@else
			<div class="overflow-x-auto">
				<table class="table data-table" aria-labelledby="high-dividend-list-title">
					<thead>
						<tr>
							<th scope="col" class="whitespace-nowrap">${MessageUtil.getMessage("stock.admin.high.dividend.field.symbol")}</th>
							<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.admin.high.dividend.field.business.year")}</th>
							<th scope="col" class="whitespace-nowrap">${MessageUtil.getMessage("stock.admin.high.dividend.col.pay.period")}</th>
							<th scope="col">${MessageUtil.getMessage("stock.admin.high.dividend.field.note")}</th>
							<th scope="col" class="whitespace-nowrap"><span class="sr-only">${MessageUtil.getMessage("stock.admin.high.dividend.col.actions")}</span></th>
						</tr>
					</thead>
					<tbody>
						@for(HighDividendDesignationResponse d : designations)
							<tr data-high-dividend-row>
								<td class="whitespace-nowrap">${d.stockItemSymbol()} · ${d.stockItemName()}</td>
								<td class="text-right tabular-nums">${d.businessYear()}</td>
								<td class="tabular-nums"><span class="whitespace-nowrap date-whole">${String.valueOf(d.payDateFrom())}</span> ~ <span class="whitespace-nowrap date-whole">${String.valueOf(d.payDateTo())}</span></td>
								<td class="text-sm text-base-content/70">${d.note() != null ? d.note() : ""}</td>
								<td class="whitespace-nowrap">
									<div class="flex gap-2">
										<a href="/stock/admin?tab=high-dividend&designationId=${String.valueOf(d.id())}" class="btn btn-outline btn-xs">${MessageUtil.getMessage("stock.admin.high.dividend.edit")}</a>
										<form method="post" action="/stock/admin/high-dividend/delete">
											<input type="hidden" name="id" value="${String.valueOf(d.id())}" />
											<button type="submit" class="btn btn-error btn-outline btn-xs">${MessageUtil.getMessage("stock.admin.high.dividend.delete")}</button>
										</form>
									</div>
								</td>
							</tr>
						@endfor
					</tbody>
				</table>
			</div>
		@endif
	`)
</div>
