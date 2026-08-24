@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.net.URLEncoder
@import java.nio.charset.StandardCharsets
@import java.text.DecimalFormat
@import java.time.LocalDate
@import java.util.List
@import net.luversof.web.gate.stock.domain.StockItem
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutImportRequest
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest
@import net.luversof.web.gate.stock.dto.request.MonthlyDividendProfileUpsertRequest
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendProfileResponse
@import net.luversof.web.gate.stock.dto.view.MonthlyDividendReferenceSummaryView

@param List<StockItem> stockItems = java.util.List.of()
@param List<MonthlyDividendProfileResponse> monthlyDividendProfiles = java.util.List.of()
@param List<MonthlyDividendPayoutResponse> monthlyDividendPayouts = java.util.List.of()
@param MonthlyDividendReferenceSummaryView monthlyDividendReferenceSummary = null
@param MonthlyDividendProfileUpsertRequest monthlyDividendProfileForm = null
@param MonthlyDividendPayoutUpsertRequest monthlyDividendPayoutForm = null
@param MonthlyDividendPayoutImportRequest monthlyDividendPayoutImportForm = null
@param String selectedMonthlyDividendSymbol = ""
@param String selectedMonthlyDividendSourceUrl = ""
@param String monthlyDividendProfileSort = "display-order"
@param String monthlyDividendProfileDirection = "asc"
@param boolean monthlyDividendProfileExists = false
@param boolean monthlyDividendPayoutExists = false
@param String selectedMonthlyDividendPayoutKey = ""
@param String selectedMonthlyDividendPayoutRecordDate = ""
@param String selectedMonthlyDividendPayoutPayDate = ""
@param String monthlyDividendReferenceErrorMessage = ""
@param String monthlyDividendReferenceResult = ""
@param String monthlyDividendReferenceResultMessage = ""
@param boolean monthlyDividendReferenceResultIsError = false

!{
	DecimalFormat amountFormat = new DecimalFormat("#,##0.####");
	DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
	MonthlyDividendProfileUpsertRequest profileForm = monthlyDividendProfileForm != null ? monthlyDividendProfileForm : new MonthlyDividendProfileUpsertRequest();
	MonthlyDividendPayoutUpsertRequest payoutForm = monthlyDividendPayoutForm != null ? monthlyDividendPayoutForm : new MonthlyDividendPayoutUpsertRequest();
	MonthlyDividendPayoutImportRequest payoutImportForm = monthlyDividendPayoutImportForm != null ? monthlyDividendPayoutImportForm : new MonthlyDividendPayoutImportRequest();
	String profileSymbolValue = profileForm.getSymbol() != null ? profileForm.getSymbol() : selectedMonthlyDividendSymbol;
	String payoutSymbolValue = payoutForm.getSymbol() != null ? payoutForm.getSymbol() : selectedMonthlyDividendSymbol;
	String payoutImportSymbolValue = payoutImportForm.getSymbol() != null ? payoutImportForm.getSymbol() : selectedMonthlyDividendSymbol;
	String sourceImportUrlValue = selectedMonthlyDividendSourceUrl != null ? selectedMonthlyDividendSourceUrl : "";
	// content block(내부 클래스)에서 참조하므로 effectively final 이어야 한다 — 가변 계산은 Tmp 에서 하고 한 번만 할당
	String payoutSymbolLabelTmp = payoutSymbolValue;
	for (StockItem stockItem : stockItems) {
		if (stockItem.symbol() != null && stockItem.symbol().equalsIgnoreCase(payoutSymbolValue)) {
			payoutSymbolLabelTmp = stockItem.symbol() + " · " + stockItem.name();
			break;
		}
	}
	String payoutSymbolLabel = payoutSymbolLabelTmp;
	String sourceUrlValue = profileForm.getSourceUrl() != null ? profileForm.getSourceUrl() : "";
	String payoutWindowValue = profileForm.getPayoutWindow() != null ? profileForm.getPayoutWindow() : "UNKNOWN";
	String noteValue = profileForm.getNote() != null ? profileForm.getNote() : "";
	String payoutBulkInputValue = payoutImportForm.getBulkInput() != null ? payoutImportForm.getBulkInput() : "";
	String lastVerifiedDateValue = profileForm.getLastVerifiedDate() != null ? String.valueOf(profileForm.getLastVerifiedDate()) : "";
	boolean activeValue = profileForm.getActive() == null || profileForm.getActive();
	String profileSortValue = monthlyDividendProfileSort != null ? monthlyDividendProfileSort : "display-order";
	String profileDirectionValue = monthlyDividendProfileDirection != null ? monthlyDividendProfileDirection : "asc";
	boolean profileReorderEnabled = "display-order".equals(profileSortValue) && "asc".equals(profileDirectionValue);
	String profileOrderEndpoint = "/stock/dividend/monthly-reference/profile/order";
	StringBuilder referenceListBaseUrlSb = new StringBuilder("/stock/admin?tab=monthly-reference");
	String monthlyDividendSimulatorUrl = "/stock/simulator?tab=monthly-dividend";
	if (selectedMonthlyDividendSymbol != null && !selectedMonthlyDividendSymbol.isEmpty()) {
		monthlyDividendSimulatorUrl += "&symbol=" + URLEncoder.encode(selectedMonthlyDividendSymbol, StandardCharsets.UTF_8);
	}
	if (selectedMonthlyDividendSymbol != null && !selectedMonthlyDividendSymbol.isEmpty()) {
		referenceListBaseUrlSb.append("&symbol=").append(URLEncoder.encode(selectedMonthlyDividendSymbol, StandardCharsets.UTF_8));
	}
	if (selectedMonthlyDividendPayoutRecordDate != null && !selectedMonthlyDividendPayoutRecordDate.isEmpty()) {
		referenceListBaseUrlSb.append("&payoutRecordDate=").append(URLEncoder.encode(selectedMonthlyDividendPayoutRecordDate, StandardCharsets.UTF_8));
	}
	if (selectedMonthlyDividendPayoutPayDate != null && !selectedMonthlyDividendPayoutPayDate.isEmpty()) {
		referenceListBaseUrlSb.append("&payoutPayDate=").append(URLEncoder.encode(selectedMonthlyDividendPayoutPayDate, StandardCharsets.UTF_8));
	}
	String referenceListBaseUrl = referenceListBaseUrlSb.toString();
	String displayOrderSortDirection = "display-order".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String symbolSortDirection = "symbol".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String payoutWindowSortDirection = "payout-window".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String sourceUrlSortDirection = "source-url".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String lastVerifiedDateSortDirection = "last-verified-date".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String activeSortDirection = "active".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String recordDateValue = payoutForm.getRecordDate() != null ? String.valueOf(payoutForm.getRecordDate()) : String.valueOf(LocalDate.now());
	String payDateValue = payoutForm.getPayDate() != null ? String.valueOf(payoutForm.getPayDate()) : String.valueOf(LocalDate.now());
	String distributionRateValue = payoutForm.getDistributionRatePct() != null ? payoutForm.getDistributionRatePct().stripTrailingZeros().toPlainString() : "";
	String dividendAmountValue = payoutForm.getDividendAmountPerShare() != null ? payoutForm.getDividendAmountPerShare().stripTrailingZeros().toPlainString() : "";
	String taxableBaseValue = payoutForm.getTaxableBasePerShare() != null ? payoutForm.getTaxableBasePerShare().stripTrailingZeros().toPlainString() : "";
	String successMessage = "";
	if ("profile-saved".equals(monthlyDividendReferenceResult)) {
		successMessage = MessageUtil.getMessage("stock.page.dividend.monthly.reference.saved.profile");
	} else if ("payout-saved".equals(monthlyDividendReferenceResult)) {
		successMessage = MessageUtil.getMessage("stock.page.dividend.monthly.reference.saved.payout");
	} else if ("payout-imported".equals(monthlyDividendReferenceResult)) {
		successMessage = MessageUtil.getMessage("stock.page.dividend.monthly.reference.imported.payout");
	} else if ("payout-source-imported".equals(monthlyDividendReferenceResult)) {
		successMessage = MessageUtil.getMessage("stock.page.dividend.monthly.reference.imported.payout.source");
	} else if ("profile-deleted".equals(monthlyDividendReferenceResult)) {
		successMessage = MessageUtil.getMessage("stock.page.dividend.monthly.reference.deleted.profile");
	} else if ("payout-deleted".equals(monthlyDividendReferenceResult)) {
		successMessage = MessageUtil.getMessage("stock.page.dividend.monthly.reference.deleted.payout");
	}
	BigDecimal latestDividend = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.latestDividendAmountPerShare() : BigDecimal.ZERO;
	BigDecimal averageDividend = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.averageDividendAmountPerShare1y() : BigDecimal.ZERO;
	BigDecimal averageTaxableRatio = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.averageTaxableBaseRatio1y() : BigDecimal.ZERO;
	int payoutCount = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.payoutCount() : 0;
}

<div class="space-y-6">
	@if(!successMessage.isEmpty())
		<div class="alert alert-success border border-success/20 bg-success/10 text-success-content">
			<span>${successMessage}</span>
		</div>
	@endif
	@if(monthlyDividendReferenceErrorMessage != null && !monthlyDividendReferenceErrorMessage.isEmpty())
		<div class="alert alert-error border border-error/20 bg-error/10 text-error-content">
			<span>${monthlyDividendReferenceErrorMessage}</span>
		</div>
	@endif
	@if(monthlyDividendReferenceResultMessage != null && !monthlyDividendReferenceResultMessage.isEmpty())
		<div class="alert ${monthlyDividendReferenceResultIsError ? "alert-error border border-error/20 bg-error/10 text-error-content" : "alert-success border border-success/20 bg-success/10 text-success-content"}">
			<span>${monthlyDividendReferenceResultMessage}</span>
		</div>
	@endif

	<div class="rounded-2xl border border-base-300 bg-base-100 p-4 shadow-sm">
		<div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
			<div class="space-y-2">
				<h3 class="text-lg font-semibold">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.title")}</h3>
				<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.desc")}</p>
				<p class="text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.selection.desc")}</p>
			</div>
			<a href="${monthlyDividendSimulatorUrl}" class="btn btn-outline btn-sm">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.to.simulator")}</a>
		</div>
	</div>

	<div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
		<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
			<p class="text-xs font-medium uppercase tracking-wide text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.payout.count")}</p>
			<p class="mt-3 text-2xl font-semibold text-base-content tabular-nums">${amountFormat.format(payoutCount)}</p>
			<p class="mt-2 text-sm text-base-content/60">${selectedMonthlyDividendSymbol}</p>
		</div>
		<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
			<p class="text-xs font-medium uppercase tracking-wide text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.latest.dividend")}</p>
			<p class="mt-3 text-2xl font-semibold text-base-content tabular-nums">₩${amountFormat.format(latestDividend)}</p>
			<p class="mt-2 text-sm text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.per.share")}</p>
		</div>
		<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
			<p class="text-xs font-medium uppercase tracking-wide text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.average.dividend")}</p>
			<p class="mt-3 text-2xl font-semibold text-base-content tabular-nums">₩${amountFormat.format(averageDividend)}</p>
			<p class="mt-2 text-sm text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.last.year")}</p>
		</div>
		<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
			<p class="text-xs font-medium uppercase tracking-wide text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.average.taxable.ratio")}</p>
			<p class="mt-3 text-2xl font-semibold text-base-content tabular-nums">${percentFormat.format(averageTaxableRatio)}%</p>
			<p class="mt-2 text-sm text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.last.year")}</p>
		</div>
	</div>

	<%-- 모바일 단일 컬럼도 minmax(0,1fr) 로 못 박는다. 암시적 트랙은 auto 라
	     내용의 min-content 만큼 벌어져 화면 밖으로 나간다(실측: 320px 에서 227px). --%>
	<div class="grid gap-6 grid-cols-[minmax(0,1fr)] xl:grid-cols-[24rem_minmax(0,1fr)]">
		<div class="space-y-6">
			@template._components.ui.card(bodyClass = "gap-4", body = @`
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.desc")}</p>
					</div>
					<form method="post" action="/stock/dividend/monthly-reference/profile" class="grid gap-4">
						<input type="hidden" name="profileSort" value="${profileSortValue}" />
						<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}</span></div>
							<input type="hidden" name="symbol" value="${profileSymbolValue}" />
							<select name="profileSymbolSelection" class="select select-bordered w-full" size="1" data-no-multi="1" required data-sync-hidden-field="symbol">
								@if(stockItems == null || stockItems.isEmpty())
									<option value="" selected disabled>${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.symbol.empty")}</option>
								@endif
								@for(StockItem stockItem : stockItems)
									@if(stockItem.symbol() != null && stockItem.symbol().equalsIgnoreCase(profileSymbolValue))
										<option value="${stockItem.symbol()}">${stockItem.symbol()} · ${stockItem.name()}</option>
									@endif
								@endfor
								@for(StockItem stockItem : stockItems)
									@if(stockItem.symbol() == null || !stockItem.symbol().equalsIgnoreCase(profileSymbolValue))
										<option value="${stockItem.symbol()}">${stockItem.symbol()} · ${stockItem.name()}</option>
									@endif
								@endfor
							</select>
							<p class="mt-2 text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.symbol.desc")}</p>
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.source.url")}</span></div>
							<input name="sourceUrl" type="url" value="${sourceUrlValue}" class="input input-bordered w-full" />
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window")}</span></div>
							<select name="payoutWindow" class="select select-bordered w-full" data-no-multi="1">
								<option value="MID_MONTH" selected="${"MID_MONTH".equals(payoutWindowValue)}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month")}</option>
								<option value="MONTH_END" selected="${"MONTH_END".equals(payoutWindowValue)}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end")}</option>
								<option value="OTHER" selected="${"OTHER".equals(payoutWindowValue)}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.other")}</option>
								<option value="UNKNOWN" selected="${"UNKNOWN".equals(payoutWindowValue)}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.unknown")}</option>
							</select>
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active")}</span></div>
							<select name="active" class="select select-bordered w-full" data-no-multi="1">
								<option value="true" selected="${activeValue}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active.true")}</option>
								<option value="false" selected="${!activeValue}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active.false")}</option>
							</select>
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.last.verified.date")}</span></div>
							<input name="lastVerifiedDate" type="date" value="${lastVerifiedDateValue}" class="input input-bordered w-full" />
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.note")}</span></div>
							<textarea name="note" rows="4" class="textarea textarea-bordered w-full">${noteValue}</textarea>
						</label>
						<button type="submit" class="btn btn-primary w-full">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.submit")}</button>
					</form>
					@if(monthlyDividendProfileExists)
						<form method="post" action="/stock/dividend/monthly-reference/profile/delete">
							<input type="hidden" name="profileSort" value="${profileSortValue}" />
							<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
							<input type="hidden" name="symbol" value="${profileSymbolValue}" />
							<button type="submit" class="btn btn-error btn-outline w-full">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.delete")}</button>
						</form>
					@endif
			`)

			@template._components.ui.card(bodyClass = "gap-4", body = @`
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.desc")}</p>
					</div>
					<div class="rounded-2xl border border-base-300 bg-base-50/70 p-4">
						<div class="space-y-2">
							<h3 class="text-sm font-semibold text-base-content">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.title")}</h3>
							<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.desc")}</p>
						</div>
						@if(monthlyDividendProfileExists && sourceImportUrlValue != null && !sourceImportUrlValue.isEmpty())
							<form method="post" action="/stock/dividend/monthly-reference/payout/import/source" class="mt-4 grid gap-4">
								<input type="hidden" name="profileSort" value="${profileSortValue}" />
								<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
								<input type="hidden" name="symbol" value="${payoutImportSymbolValue}" />
								<label class="form-control">
									<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.url")}</span></div>
									<div class="rounded-box border border-base-300 bg-base-200/40 px-4 py-3 text-sm text-base-content break-all">${sourceImportUrlValue}</div>
								</label>
								<button type="submit" class="btn btn-secondary w-full">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.submit")}</button>
							</form>
						@else
							<div class="mt-4 rounded-box border border-base-300 bg-base-100 px-4 py-3 text-sm text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.empty")}</div>
						@endif
						<div class="divider my-4 text-xs text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.title")}</div>
						<div class="space-y-2">
							<h3 class="text-sm font-semibold text-base-content">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.title")}</h3>
							<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.desc")}</p>
						</div>
						<form method="post" action="/stock/dividend/monthly-reference/payout/import" class="mt-4 grid gap-4">
							<input type="hidden" name="profileSort" value="${profileSortValue}" />
							<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
							<label class="form-control">
								<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}</span></div>
								<input type="hidden" name="symbol" value="${payoutImportSymbolValue}" />
								<div class="rounded-box border border-base-300 bg-base-200/40 px-4 py-3 text-sm text-base-content">${payoutSymbolLabel}</div>
								<p class="mt-2 text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.symbol.desc")}</p>
							</label>
							<label class="form-control">
								<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.bulk.input")}</span></div>
								<textarea name="bulkInput" rows="10" class="textarea textarea-bordered w-full font-mono text-xs leading-6" placeholder="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.placeholder")}" required>${payoutBulkInputValue}</textarea>
							</label>
							<button type="submit" class="btn btn-primary w-full">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.submit")}</button>
						</form>
					</div>
					<div class="divider my-0 text-xs text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.manual.title")}</div>
					<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.manual.desc")}</p>
					<form method="post" action="/stock/dividend/monthly-reference/payout" class="grid gap-4">
						<input type="hidden" name="profileSort" value="${profileSortValue}" />
						<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}</span></div>
							<input type="hidden" name="symbol" value="${payoutSymbolValue}" />
							<div class="rounded-box border border-base-300 bg-base-200/40 px-4 py-3 text-sm text-base-content">${payoutSymbolLabel}</div>
							<p class="mt-2 text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.symbol.desc")}</p>
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.record.date")}</span></div>
							<input name="recordDate" type="date" value="${recordDateValue}" class="input input-bordered w-full" required />
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.pay.date")}</span></div>
							<input name="payDate" type="date" value="${payDateValue}" class="input input-bordered w-full" required />
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.distribution.rate")}</span></div>
							<input name="distributionRatePct" type="number" min="0" step="0.01" value="${distributionRateValue}" class="input input-bordered w-full" />
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.dividend.amount")}</span></div>
							<input name="dividendAmountPerShare" type="number" min="0" step="0.0001" value="${dividendAmountValue}" class="input input-bordered w-full" required />
						</label>
						<label class="form-control">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.taxable.base")}</span></div>
							<input name="taxableBasePerShare" type="number" min="0" step="0.0001" value="${taxableBaseValue}" class="input input-bordered w-full" required />
						</label>
						<button type="submit" class="btn btn-outline w-full">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.submit")}</button>
					</form>
					@if(monthlyDividendPayoutExists)
						<form method="post" action="/stock/dividend/monthly-reference/payout/delete">
							<input type="hidden" name="profileSort" value="${profileSortValue}" />
							<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
							<input type="hidden" name="symbol" value="${payoutSymbolValue}" />
							<input type="hidden" name="recordDate" value="${selectedMonthlyDividendPayoutRecordDate}" />
							<input type="hidden" name="payDate" value="${selectedMonthlyDividendPayoutPayDate}" />
							<button type="submit" class="btn btn-error btn-outline w-full">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.delete")}</button>
						</form>
					@endif
			`)
		</div>

		<div class="space-y-6">
			@template._components.ui.card(bodyClass = "gap-5", body = @`
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.desc")}</p>
						<p class="text-xs leading-5 text-base-content/60">${profileReorderEnabled ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.reorder.enabled") : MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.reorder.disabled")}</p>
					</div>
					<div class="rounded-2xl border border-base-300 bg-base-50/70 p-4">
						<div class="space-y-1">
							<h3 class="text-sm font-semibold text-base-content">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.title")}</h3>
							<p class="text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.desc")}</p>
						</div>
						<div class="mt-3 flex flex-wrap gap-2">
							<form method="post" action="/stock/dividend/monthly-reference/payout/import/source/bulk">
								<input type="hidden" name="profileSort" value="${profileSortValue}" />
								<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
								<input type="hidden" name="payoutWindow" value="MID_MONTH" />
								<button type="submit" class="btn btn-secondary btn-sm">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.mid.month")}</button>
							</form>
							<form method="post" action="/stock/dividend/monthly-reference/payout/import/source/bulk">
								<input type="hidden" name="profileSort" value="${profileSortValue}" />
								<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
								<input type="hidden" name="payoutWindow" value="MONTH_END" />
								<button type="submit" class="btn btn-secondary btn-sm">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.month.end")}</button>
							</form>
						</div>
					</div>
					<div id="monthlyDividendProfileOrderStatus" class="hidden"></div>
					@if(monthlyDividendProfiles == null || monthlyDividendProfiles.isEmpty())
						<div class="rounded-2xl border border-dashed border-base-300 bg-base-50 px-6 py-10 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.empty")}</div>
					@else
						<div class="overflow-x-auto">
							<table class="table table-sm">
								<thead>
									<tr>
										<th scope="col">
											<a href="${referenceListBaseUrl}&profileSort=display-order&profileDirection=${displayOrderSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.display.order")}
												@if("display-order".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col">
											<a href="${referenceListBaseUrl}&profileSort=symbol&profileDirection=${symbolSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}
												@if("symbol".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col">
											<a href="${referenceListBaseUrl}&profileSort=payout-window&profileDirection=${payoutWindowSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window")}
												@if("payout-window".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col">
											<a href="${referenceListBaseUrl}&profileSort=source-url&profileDirection=${sourceUrlSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.source.url")}
												@if("source-url".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col">
											<a href="${referenceListBaseUrl}&profileSort=last-verified-date&profileDirection=${lastVerifiedDateSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.last.verified.date")}
												@if("last-verified-date".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col">
											<a href="${referenceListBaseUrl}&profileSort=active&profileDirection=${activeSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active")}
												@if("active".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
									</tr>
								</thead>
								<tbody id="monthlyDividendProfileOrderList" data-profile-order-list="true" data-profile-order-status-id="monthlyDividendProfileOrderStatus" data-profile-order-endpoint="${profileOrderEndpoint}" data-profile-order-handle-selector="[data-profile-order-handle]" data-profile-reorder-enabled="${profileReorderEnabled}" data-success-message="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.reordered.profile")}" data-fallback-error-message="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.reordered.profile.failed")}">
									@for(MonthlyDividendProfileResponse row : monthlyDividendProfiles)
										<tr data-profile-order-row data-symbol="${row.stockItemSymbol()}" draggable="${profileReorderEnabled}" class="${selectedMonthlyDividendSymbol.equalsIgnoreCase(row.stockItemSymbol()) ? "bg-base-200/60" : ""}${profileReorderEnabled ? " cursor-move" : ""}">
											<td>
												<div class="flex items-center gap-2">
													@if(profileReorderEnabled)
														<span class="font-mono text-xs text-base-content/60 select-none" data-profile-order-handle>::</span>
													@endif
													<span data-profile-order-value>${row.displayOrder() != null ? String.valueOf(row.displayOrder()) : "-"}</span>
												</div>
											</td>
											<td>
												<a href="/stock/admin?tab=monthly-reference&symbol=${row.stockItemSymbol()}&profileSort=${profileSortValue}&profileDirection=${profileDirectionValue}" class="link link-hover font-medium">${row.stockItemSymbol()}</a>
												<div class="text-xs text-base-content/60">${row.stockItemName()}</div>
												<div class="mt-2 flex flex-wrap gap-2">
													<a href="/stock/admin?tab=monthly-reference&symbol=${row.stockItemSymbol()}&profileSort=${profileSortValue}&profileDirection=${profileDirectionValue}" class="btn btn-xs btn-outline">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.select")}</a>
													<a href="/stock/simulator?tab=monthly-dividend&symbol=${row.stockItemSymbol()}&sort=display-order&direction=asc" class="btn btn-xs btn-primary">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.to.simulator")}</a>
												</div>
											</td>
											<td>${row.payoutWindow()}</td>
											<td>
												@if(row.sourceUrl() != null && !row.sourceUrl().isEmpty())
													<div class="space-y-1">
														<a href="${row.sourceUrl()}" class="link link-primary" target="_blank" rel="noreferrer">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.open.link")}</a>
														<div class="max-w-xs break-all text-xs text-base-content/60">${row.sourceUrl()}</div>
													</div>
												@else
													-
												@endif
											</td>
											<td>${row.lastVerifiedDate() != null ? String.valueOf(row.lastVerifiedDate()) : "-"}</td>
											<td>${row.active() ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active.true") : MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active.false")}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					@endif
			`)

			@template._components.ui.card(bodyClass = "gap-5", body = @`
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.list.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.list.desc")}</p>
					</div>
					@if(monthlyDividendPayouts == null || monthlyDividendPayouts.isEmpty())
						<div class="rounded-2xl border border-dashed border-base-300 bg-base-50 px-6 py-10 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.empty")}</div>
					@else
						<div class="overflow-x-auto">
							<table class="table table-sm">
								<thead>
									<tr>
										<th scope="col" class="whitespace-nowrap">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.record.date")}</th>
										<th scope="col" class="whitespace-nowrap">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.pay.date")}</th>
										<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.distribution.rate")}</th>
										<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.dividend.amount")}</th>
										<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.taxable.base")}</th>
									</tr>
								</thead>
								<tbody>
									@for(MonthlyDividendPayoutResponse row : monthlyDividendPayouts)
										<tr class="${selectedMonthlyDividendPayoutKey.equals(String.valueOf(row.recordDate()) + "|" + String.valueOf(row.payDate())) ? "bg-base-200/60" : ""}">
											<td class="whitespace-nowrap tabular-nums">
												<a href="/stock/admin?tab=monthly-reference&symbol=${row.stockItemSymbol()}&profileSort=${profileSortValue}&profileDirection=${profileDirectionValue}&payoutRecordDate=${String.valueOf(row.recordDate())}&payoutPayDate=${String.valueOf(row.payDate())}" class="link link-hover font-medium tabular-nums">${String.valueOf(row.recordDate())}</a>
											</td>
											<td class="whitespace-nowrap tabular-nums">${String.valueOf(row.payDate())}</td>
											<td class="text-right tabular-nums">@if(row.distributionRatePct() != null)${percentFormat.format(row.distributionRatePct())}%@else-@endif</td>
											<td class="text-right tabular-nums">₩${amountFormat.format(row.dividendAmountPerShare() != null ? row.dividendAmountPerShare() : BigDecimal.ZERO)}</td>
											<td class="text-right tabular-nums">₩${amountFormat.format(row.taxableBasePerShare() != null ? row.taxableBasePerShare() : BigDecimal.ZERO)}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					@endif
			`)
		</div>
	</div>
</div>

<script nonce="${CspNonceHolder.getNonce()}">
	(function() {
		var selects = document.querySelectorAll('select[data-sync-hidden-field]');
		for (var i = 0; i < selects.length; i++) {
			var select = selects[i];
			if (select.dataset.syncHiddenFieldBound === 'true') {
				continue;
			}
			select.dataset.syncHiddenFieldBound = 'true';
			select.addEventListener('change', function() {
				this.form.elements[this.dataset.syncHiddenField].value = this.value;
			});
		}
	})();
</script>