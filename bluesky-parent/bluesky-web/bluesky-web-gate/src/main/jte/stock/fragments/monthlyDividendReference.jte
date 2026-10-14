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
<%-- 종목코드(대문자) -> 가장 늦은 지급 기준일(2026-10-13 사용자 요청 - 목록에서 갱신 여부 확인). --%>
@param java.util.Map<String, net.luversof.web.gate.stock.dto.view.MonthlyDividendLatestPayoutView> monthlyDividendLatestPayoutBySymbol = java.util.Map.of()
<%-- 목록 지급 시기 거르기(2026-10-13 사용자 요청): MID_MONTH · MONTH_END · 빈 값(전체), 칩에 적을 개수는 거르기 전 전체 기준. --%>
@param String monthlyDividendProfileWindow = ""
@param java.util.Map<String, Long> monthlyDividendProfileWindowCounts = java.util.Map.of()
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
<%-- 가져오기는 됐지만 출처 표의 잘못된 행을 건너뛰었을 때의 안내. 성공(초록)도 오류(빨강)도 아니다. --%>
@param String monthlyDividendReferenceWarningMessage = ""

<%-- 주소에 싣는 종목 코드는 인코딩해서 싣는다.
     이 파일은 같은 값을 74·77 행에서는 URLEncoder 로 싣고 표 안 링크 네 곳에서는 날것으로 싣고 있었다.
     실측 2026-09-14: 등록된 86 종목이 모두 6 자리 [0-9CDEGMNPR] 라 지금은 둘 다 같은 결과다 -
     즉 화면으로는 영원히 안 드러난다. 코드 모양을 강제하는 것은 게이트가 아니라 원장이므로
     (검증은 '등록된 코드인가' 만 본다) 한 기능 안에서 규칙이 갈린 채로 두지 않는다. --%>
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
	// 지급 이력의 분배율은 출처에서 오지 않는다 - 실측 2026-09-11: 저장된 210 건 전부 null 이라 열이 "-" 로만 찼다.
	// 붙여넣기로 직접 채우면 값이 생기므로, 열을 지우지 않고 값이 있을 때만 낸다.
	boolean showDistributionRate = monthlyDividendPayouts != null
			&& monthlyDividendPayouts.stream().anyMatch(payout -> payout.distributionRatePct() != null);
	String sourceUrlValue = profileForm.getSourceUrl() != null ? profileForm.getSourceUrl() : "";
	String payoutWindowValue = profileForm.getPayoutWindow() != null ? profileForm.getPayoutWindow() : "UNKNOWN";
	String noteValue = profileForm.getNote() != null ? profileForm.getNote() : "";
	String payoutBulkInputValue = payoutImportForm.getBulkInput() != null ? payoutImportForm.getBulkInput() : "";
	String lastVerifiedDateValue = profileForm.getLastVerifiedDate() != null ? String.valueOf(profileForm.getLastVerifiedDate()) : "";
	String totalExpenseRatioValue = profileForm.getTotalExpenseRatioPct() != null ? profileForm.getTotalExpenseRatioPct().stripTrailingZeros().toPlainString() : "";
	String listingDateValue = profileForm.getListingDate() != null ? String.valueOf(profileForm.getListingDate()) : "";
	boolean activeValue = profileForm.getActive() == null || profileForm.getActive();
	String profileSortValue = monthlyDividendProfileSort != null ? monthlyDividendProfileSort : "display-order";
	String profileDirectionValue = monthlyDividendProfileDirection != null ? monthlyDividendProfileDirection : "asc";
	String profileWindowValue = monthlyDividendProfileWindow != null ? monthlyDividendProfileWindow : "";
	boolean profileWindowFiltered = !profileWindowValue.isEmpty();
	// 거른 목록에서 끌어 옮기면 보이는 줄만 다시 번호를 매겨 숨은 줄과 순서가 겹친다 - 거르는 동안은 끈다.
	boolean profileReorderEnabled = "display-order".equals(profileSortValue) && "asc".equals(profileDirectionValue) && !profileWindowFiltered;
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
	String profileWindowQuery = profileWindowFiltered ? "&profileWindow=" + URLEncoder.encode(profileWindowValue, StandardCharsets.UTF_8) : "";
	// 거르기 칩은 정렬은 그대로 두고 거르기만 바꾼다.
	String profileWindowBaseUrl = referenceListBaseUrlSb + "&profileSort=" + URLEncoder.encode(profileSortValue, StandardCharsets.UTF_8) + "&profileDirection=" + URLEncoder.encode(profileDirectionValue, StandardCharsets.UTF_8);
	String referenceListBaseUrl = referenceListBaseUrlSb + profileWindowQuery;
	String profileWindowFilterLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.window.filter");
	String displayOrderSortDirection = "display-order".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String symbolSortDirection = "symbol".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String payoutWindowSortDirection = "payout-window".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String sourceUrlSortDirection = "source-url".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	// 검증한 적이 없으면 이 칸은 "-" 한 글자가 된다. 실측 2026-09-12: 프로필 8개가 모두 그랬고 그 칸에는
	// title 도 sr-only 도 없어 마우스로도 낭독기로도 까닭을 알 수 없었다(이 화면은 탭 주소를 빼먹어 그동안 훑기에서 빠져 있었다).
	String notVerifiedLabel = MessageUtil.getMessage("stock.monthly.reference.profile.not.verified");
	// 줄마다 같은 글자의 링크가 선다 - 실측 2026-09-12(접근성 트리): "이 값으로 시뮬레이터 채우기" 9개,
	// "이 종목 보기" 8개, "링크 열기 (새 창에서 열림)" 8개가 이름만으로는 구분되지 않았다(목적지는 전부 다르다).
	String profileActionAriaPattern = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.action.aria");
	String openLinkLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.open.link");
	// 행 안에서 지급 이력 펼치기(사용자 요청 2026-09-29 A안) - 예전 "이 종목 보기" 는 페이지를 다시 불러 맨 위로 돌아가 지급 이력까지 2,019px 을 내려가야 했다.
	// "이 종목 보기" 는 하는 일(왼쪽 폼에 불러오기)대로 "편집" 이라 부르고 폼 자리로 곧장 간다.
	String payoutPeekLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.peek.toggle");
	String payoutPeekLoading = MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.peek.loading");
	String payoutPeekFailed = MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.peek.failed");
	String profileEditLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.edit");
	String opensNewWindowLabel = MessageUtil.getMessage("common.label.opens.new.window");
	String lastVerifiedDateSortDirection = "last-verified-date".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String activeSortDirection = "active".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String latestRecordDateSortDirection = "latest-record-date".equals(profileSortValue) && "asc".equals(profileDirectionValue) ? "desc" : "asc";
	String noPayoutLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.latest.record.date.none");
	String staleRecordLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.latest.record.date.stale");
	String staleRecordTitle = java.text.MessageFormat.format(MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.latest.record.date.stale.title"), String.valueOf(net.luversof.web.gate.stock.dto.view.MonthlyDividendLatestPayoutView.STALE_AFTER_DAYS));
	String daysAgoPattern = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.latest.record.date.days.ago");
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
	} else if ("price-refreshed".equals(monthlyDividendReferenceResult)) {
		successMessage = MessageUtil.getMessage("stock.page.dividend.monthly.reference.refreshed.price");
	} else if ("payout-deleted".equals(monthlyDividendReferenceResult)) {
		successMessage = MessageUtil.getMessage("stock.page.dividend.monthly.reference.deleted.payout");
	}
	BigDecimal latestDividend = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.latestDividendAmountPerShare() : BigDecimal.ZERO;
	BigDecimal averageDividend = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.averageDividendAmountPerShare1y() : BigDecimal.ZERO;
	// 과세표준을 아는 행이 없으면 null(확인 안 됨, 2026-10-07).
	BigDecimal averageTaxableRatio = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.averageTaxableBaseRatio1y() : BigDecimal.ZERO;
	// 문구가 말하는 기준 건수. 두 카드는 서로 다른 행을 쓰므로 각자 자기 수를 말한다.
	int averageBasisRows = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.averageBasisRowCount() : 0;
	int taxableBasisRows = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.taxableRatioBasisRowCount() : 0;
	String basisRowsTemplate = MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.basis.rows");
	int payoutCount = monthlyDividendReferenceSummary != null ? monthlyDividendReferenceSummary.payoutCount() : 0;
}

<div class="space-y-6">
	<%-- 결과 알림 묶음. 목록 행의 "출처에서 가져오기" 는 페이지를 새로 부르지 않고 목록만 바꿔 끼우므로(2026-10-14) 이 묶음도 함께 바꾼다
	     (hx-select-oob). 늘 그려 두고, 알림이 없으면 숨긴다(빈 칸이 위에 틈을 만들지 않게). --%>
	<div id="monthlyReferenceResultArea" class="space-y-4 [&:not(:has(*))]:hidden">
	@if(!successMessage.isEmpty())
		<div class="alert alert-success border border-success/20 bg-success/10 text-success-content" role="status">
			<span>${successMessage}</span>
		</div>
	@endif
	@if(monthlyDividendReferenceErrorMessage != null && !monthlyDividendReferenceErrorMessage.isEmpty())
		<div class="alert alert-error border border-error/20 bg-error/10 text-error-content" role="alert">
			<span>${monthlyDividendReferenceErrorMessage}</span>
		</div>
	@endif
	@if(monthlyDividendReferenceResultMessage != null && !monthlyDividendReferenceResultMessage.isEmpty())
		<div class="alert ${monthlyDividendReferenceResultIsError ? "alert-error border border-error/20 bg-error/10 text-error-content" : "alert-success border border-success/20 bg-success/10 text-success-content"}" role="alert">
			<span>${monthlyDividendReferenceResultMessage}</span>
		</div>
	@endif
	<%-- 출처 사이트의 오타 한 줄 때문에 가져오기가 통째로 실패하던 것을, 그 줄만 건너뛰게 바꿨다(실측 2026-09-17: RISE 44J2
	     실지급일 2025-05-19, 사용자 결정). 건너뛴 줄을 말하지 않으면 이력에 빈 달이 생겨도 모른다 - 그래서 사유를 그대로 적는다. --%>
	@if(monthlyDividendReferenceWarningMessage != null && !monthlyDividendReferenceWarningMessage.isEmpty())
		<div class="alert alert-warning border border-warning/30 bg-warning/10 text-base-content" role="alert" data-import-skipped-warning>
			<span>${monthlyDividendReferenceWarningMessage}</span>
		</div>
	@endif
	</div>

	<div class="rounded-2xl border border-base-300 bg-base-100 p-4 shadow-sm">
		<div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
			<div class="space-y-2">
				<h2 class="text-lg font-semibold">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.title")}</h2>
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
			<%-- 평균은 넷째 자리까지 구해 두고 **표시할 때 한 번만** 반올림한다. 그대로 찍으면 같은 카드가
			     종목에 따라 ₩32.25 와 ₩106.3636 으로 갈린다(실측 2026-09-16). --%>
			<p class="mt-3 text-2xl font-semibold text-base-content tabular-nums">₩${amountFormat.format(averageDividend.setScale(2, java.math.RoundingMode.HALF_UP))}</p>
			<p class="mt-2 text-sm text-base-content/60">${java.text.MessageFormat.format(basisRowsTemplate, averageBasisRows)}</p>
		</div>
		<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
			<p class="text-xs font-medium uppercase tracking-wide text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.summary.average.taxable.ratio")}</p>
			<p class="mt-3 text-2xl font-semibold text-base-content tabular-nums">@if(averageTaxableRatio == null)<span class="text-base-content/60" data-taxable-unknown>${MessageUtil.getMessage("stock.monthly.taxable.unknown")}</span>@else${percentFormat.format(averageTaxableRatio)}%@endif</p>
			<p class="mt-2 text-sm text-base-content/60">${java.text.MessageFormat.format(basisRowsTemplate, taxableBasisRows)}</p>
		</div>
	</div>

	<%-- 모바일 단일 컬럼도 minmax(0,1fr) 로 못 박는다. 암시적 트랙은 auto 라
	     내용의 min-content 만큼 벌어져 화면 밖으로 나간다(실측: 320px 에서 227px). --%>
	<div class="grid gap-6 grid-cols-[minmax(0,1fr)] xl:grid-cols-[24rem_minmax(0,1fr)]">
		<div class="space-y-6">
			<%-- 링크만으로 등록(사용자 요청 2026-09-21): 종목 · 월배당 태그 · 프로필 · 지급 이력을 한 번에.
			     여러 줄을 넣으면 줄마다 하나씩 등록하고, 실패한 줄은 사유와 함께 알린다. --%>
			@template._components.ui.card(bodyClass = "gap-4", body = @`
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.link.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.link.desc")}</p>
					</div>
					<form method="post" action="/stock/dividend/monthly-reference/profile/import/links" class="grid gap-4" data-submit-overlay="true" data-submit-overlay-title="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.link.submit")}">
						<input type="hidden" name="profileSort" value="${profileSortValue}" />
						<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
						<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.link.field")}</span></div>
							<textarea name="links" rows="4" class="textarea textarea-bordered w-full min-w-0 font-mono text-xs" placeholder="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.link.placeholder")}" required></textarea>
						</label>
						<button type="submit" class="btn btn-primary w-full h-auto min-h-12 whitespace-normal py-3 leading-snug">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.link.submit")}</button>
					</form>
			`)
			@template._components.ui.card(bodyClass = "gap-4", body = @`
					<div class="space-y-2">
						<h2 id="monthly-reference-editor" class="card-title text-lg scroll-mt-24">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.desc")}</p>
					</div>
					<form method="post" action="/stock/dividend/monthly-reference/profile" class="grid gap-4">
						<input type="hidden" name="profileSort" value="${profileSortValue}" />
						<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
						<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}</span></div>
							<input type="hidden" name="symbol" value="${profileSymbolValue}" />
							<select name="profileSymbolSelection" class="select select-bordered w-full min-w-0" size="1" data-no-multi="1" required data-sync-hidden-field="symbol">
								@if(stockItems == null || stockItems.isEmpty())
									<option value="" selected disabled>${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.symbol.empty")}</option>
								@endif
								<%-- 고른 종목을 맨 위로 끌어올리지 않는다 - 그러면 목록이 코드 순이 아니게 된다(사용자 요청 2026-09-21).
								     selected 를 달면 브라우저가 제자리에서 고른다. --%>
								@for(StockItem stockItem : stockItems)
									<option value="${stockItem.symbol()}" selected="${stockItem.symbol() != null && stockItem.symbol().equalsIgnoreCase(profileSymbolValue)}">${stockItem.symbol()} · ${stockItem.name()}</option>
								@endfor
							</select>
							<p class="mt-2 text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.symbol.desc")}</p>
						</label>
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.source.url")}</span></div>
							<input name="sourceUrl" type="url" value="${sourceUrlValue}" class="input input-bordered w-full min-w-0" />
						</label>
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window")}</span></div>
							<select name="payoutWindow" class="select select-bordered w-full min-w-0" data-no-multi="1">
								<option value="MID_MONTH" selected="${"MID_MONTH".equals(payoutWindowValue)}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month")}</option>
								<option value="MONTH_END" selected="${"MONTH_END".equals(payoutWindowValue)}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end")}</option>
								<option value="OTHER" selected="${"OTHER".equals(payoutWindowValue)}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.other")}</option>
								<option value="UNKNOWN" selected="${"UNKNOWN".equals(payoutWindowValue)}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.unknown")}</option>
							</select>
						</label>
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active")}</span></div>
							<select name="active" class="select select-bordered w-full min-w-0" data-no-multi="1">
								<option value="true" selected="${activeValue}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active.true")}</option>
								<option value="false" selected="${!activeValue}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active.false")}</option>
							</select>
						</label>
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.last.verified.date")}</span></div>
							<input name="lastVerifiedDate" type="date" value="${lastVerifiedDateValue}" class="input input-bordered w-full min-w-0" />
						</label>
						<%-- 총보수 · 상장일(2026-09-28). 저장이 전체 덮어쓰기라 폼에 실어야 저장할 때 안 지워진다. 보통은 "새로 가져오기" 가 채운다. --%>
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.total.expense.ratio")}</span></div>
							<input name="totalExpenseRatioPct" type="number" step="0.0001" min="0" max="99.9999" inputmode="decimal" value="${totalExpenseRatioValue}" class="input input-bordered w-full min-w-0" />
						</label>
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.listing.date")}</span></div>
							<input name="listingDate" type="date" value="${listingDateValue}" class="input input-bordered w-full min-w-0" />
						</label>
						<label class="form-control min-w-0">
							<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.note")}</span></div>
							<textarea name="note" rows="4" class="textarea textarea-bordered w-full min-w-0">${noteValue}</textarea>
						</label>
						<button type="submit" class="btn btn-primary w-full h-auto min-h-12 whitespace-normal py-3 leading-snug">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.submit")}</button>
					</form>
					@if(monthlyDividendProfileExists)
						<%-- 이 종목 시세만 다시 받는다(사용자 요청 2026-09-22). 전체 갱신은 53 종목에 18~22 초가 걸려
						     게이트 읽기 제한(10 초)에 끊긴다 - 한 종목이면 1 초 안쪽이다. --%>
						<form method="post" action="/stock/dividend/monthly-reference/profile/refresh-price" class="mb-2">
							<input type="hidden" name="profileSort" value="${profileSortValue}" />
							<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
							<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
							<input type="hidden" name="symbol" value="${profileSymbolValue}" />
							<button type="submit" data-refresh-price class="btn btn-outline w-full h-auto min-h-12 whitespace-normal py-3 leading-snug">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.refresh.price")}</button>
						</form>
						<form method="post" action="/stock/dividend/monthly-reference/profile/delete">
							<input type="hidden" name="profileSort" value="${profileSortValue}" />
							<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
							<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
							<input type="hidden" name="symbol" value="${profileSymbolValue}" />
							<button type="submit" class="btn btn-error btn-outline w-full h-auto min-h-12 whitespace-normal py-3 leading-snug">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.delete")}</button>
						</form>
					@endif
			`)

			@template._components.ui.card(bodyClass = "gap-4", body = @`
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.desc")}</p>
					</div>
					<div class="rounded-2xl border border-base-300 bg-base-50/70 p-4">
						<%-- 출처에서 가져오기는 프로필 목록의 행 단추로 옮겼다(사용자 요청 2026-10-14 - 따로 구성하지 말고 목록에 단추). --%>
						<div class="space-y-2">
							<h3 class="text-sm font-semibold text-base-content">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.title")}</h3>
							<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.desc")}</p>
						</div>
						<form method="post" action="/stock/dividend/monthly-reference/payout/import" class="mt-4 grid gap-4" data-submit-overlay="true" data-submit-overlay-title="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.submit")}" data-submit-overlay-desc="${MessageUtil.getMessage("stock.admin.message.please.wait")}">
							<input type="hidden" name="profileSort" value="${profileSortValue}" />
							<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
							<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
							<label class="form-control min-w-0">
								<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}</span></div>
								<input type="hidden" name="symbol" value="${payoutImportSymbolValue}" />
								<div class="rounded-box border border-base-300 bg-base-200/40 px-4 py-3 text-sm text-base-content [overflow-wrap:anywhere]">${payoutSymbolLabel}</div>
								<p class="mt-2 text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.symbol.desc")}</p>
							</label>
							<label class="form-control min-w-0">
								<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.bulk.input")}</span></div>
								<textarea name="bulkInput" rows="10" class="textarea textarea-bordered w-full min-w-0 font-mono text-xs leading-6" placeholder="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.placeholder")}" required>${payoutBulkInputValue}</textarea>
							</label>
							<button type="submit" class="btn btn-primary w-full h-auto min-h-12 whitespace-normal py-3 leading-snug">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.submit")}</button>
						</form>
					</div>
					<%-- 단건 입력 · 수정 폼과 그 삭제는 걷어냈다(사용자 요청 2026-10-14 "단건 수정도 굳이 있을 필요가 없을거 같아") - 이력은 출처 가져오기 ·
					     붙여넣기로 넣고, 잘못 들어간 행은 아래 지급 이력 표의 행 삭제로 지운다. --%>
			`)
		</div>

		<div class="space-y-6">
			@template._components.ui.card(bodyClass = "gap-5", body = @`
					<div class="space-y-2">
						<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.title")}</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.desc")}</p>
						<p class="text-xs leading-5 text-base-content/60" id="monthlyReferenceReorderNote">${profileReorderEnabled ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.reorder.enabled") : (profileWindowFiltered ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.reorder.filtered") : MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.reorder.disabled"))}</p>
					</div>
					<div class="rounded-2xl border border-base-300 bg-base-50/70 p-4">
						<div class="space-y-1">
							<h3 class="text-sm font-semibold text-base-content">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.title")}</h3>
							<p class="text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.desc")}</p>
						</div>
						<%-- 단추 글자가 길어 좁은 폭 + 큰 글꼴에서 줄이 안 접혔다(실측 2026-09-21: 문서 413px). 접히게 두고 한 줄씩 차지하게. --%>
						<div class="mt-3 flex flex-wrap gap-2 [&>form]:min-w-0 [&>form]:flex-1">
							<form method="post" action="/stock/dividend/monthly-reference/payout/import/source/bulk" data-submit-overlay="true" data-submit-overlay-title="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.mid.month")}" data-submit-overlay-desc="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.import.overlay.desc.source")}">
								<input type="hidden" name="profileSort" value="${profileSortValue}" />
								<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
								<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
								<input type="hidden" name="payoutWindow" value="MID_MONTH" />
								<button type="submit" class="btn btn-secondary btn-sm h-auto min-h-8 whitespace-normal py-2 leading-snug">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.mid.month")}</button>
							</form>
							<form method="post" action="/stock/dividend/monthly-reference/payout/import/source/bulk" data-submit-overlay="true" data-submit-overlay-title="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.month.end")}" data-submit-overlay-desc="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.import.overlay.desc.source")}">
								<input type="hidden" name="profileSort" value="${profileSortValue}" />
								<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
								<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
								<input type="hidden" name="payoutWindow" value="MONTH_END" />
								<button type="submit" class="btn btn-secondary btn-sm h-auto min-h-8 whitespace-normal py-2 leading-snug">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.bulk.month.end")}</button>
							</form>
						</div>
					</div>
					<div class="rounded-2xl border border-base-300 bg-base-50/70 p-4" data-profile-facts-refresh>
						<div class="space-y-1">
							<h3 class="text-sm font-semibold text-base-content">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.facts.refresh.title")}</h3>
							<p class="text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.facts.refresh.desc")}</p>
						</div>
						<div class="mt-3 flex flex-wrap gap-2 [&>form]:min-w-0 [&>form]:flex-1">
							<form method="post" action="/stock/dividend/monthly-reference/profile/facts/refresh" data-submit-overlay="true" data-submit-overlay-title="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.facts.refresh.button")}" data-submit-overlay-desc="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.import.overlay.desc.source")}">
								<input type="hidden" name="profileSort" value="${profileSortValue}" />
								<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
								<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
								<button type="submit" class="btn btn-secondary btn-sm h-auto min-h-8 whitespace-normal py-2 leading-snug">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.facts.refresh.button")}</button>
							</form>
						</div>
					</div>
					<%-- 프로필 목록 구역. 지급 시기 칩 · 정렬 머리는 페이지를 새로 부르지 않고 이 구역만 바꿔 끼운다(사용자 요청 2026-10-13 - 시뮬레이터 ·
					     월배당 ETF 와 같은 방식). 함께 바뀌는 것: 위 끌어 옮기기 안내(거르면 꺼진다) · 아래 지급 이력(행 링크가 정렬 · 거르기를 싣는다).
					     왼쪽 편집 폼은 바꾸지 않는다 - 입력하던 값이 날아간다. 대신 폼의 숨은 정렬 · 거르기 칸을 이 구역의 data-* 값으로 맞춘다(아래 스크립트).
					     종목 고르기(편집 · 종목코드)는 편집 폼을 바꾸므로 예전처럼 페이지를 옮긴다. data-swap-anchor: 칩이 같은 자리에 머문다. --%>
					<div class="flex flex-col gap-5" id="monthlyReferenceProfileList" hx-target="#monthlyReferenceProfileList" hx-select="#monthlyReferenceProfileList" hx-select-oob="#monthlyReferenceReorderNote,#monthlyReferencePayoutList,#monthlyReferenceResultArea" hx-swap="outerHTML show:none" hx-push-url="false" hx-replace-url="true" data-swap-anchor="[data-profile-window-filter]" data-swap-announce="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.title")}" data-profile-sort-value="${profileSortValue}" data-profile-direction-value="${profileDirectionValue}" data-profile-window-value="${profileWindowValue}">
					<%-- 월중만 · 월말만 보기(2026-10-13 사용자 요청). 개수는 거르기 전 전체 기준이라 칩을 누르기 전에 몇 줄이 남을지 보인다. --%>
					<div class="flex flex-wrap items-center gap-2" role="group" aria-label="${profileWindowFilterLabel}" data-profile-window-filter hx-boost="true">
						<span class="text-xs font-medium text-base-content/60" aria-hidden="true">${profileWindowFilterLabel}</span>
						@for(String windowOption : java.util.List.of("", "MID_MONTH", "MONTH_END"))
							!{boolean windowSelected = windowOption.equals(profileWindowValue);}
							<a href="${profileWindowBaseUrl + (windowOption.isEmpty() ? "" : "&profileWindow=" + windowOption)}" class="btn btn-sm h-auto min-h-8 whitespace-normal py-2 leading-snug break-keep ${windowSelected ? "btn-primary" : "btn-ghost border border-base-300"}" aria-current="${windowSelected ? "true" : null}" data-profile-window-link="${windowOption.isEmpty() ? "all" : windowOption}">${windowOption.isEmpty() ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.window.all") : net.luversof.web.gate.stock.util.StockPayoutWindowLabelUtil.label(windowOption)} <span class="tabular-nums">${String.valueOf(monthlyDividendProfileWindowCounts.getOrDefault(windowOption, 0L))}</span></a>
						@endfor
					</div>
					<div id="monthlyDividendProfileOrderStatus" class="hidden"></div>
					@if(monthlyDividendProfiles == null || monthlyDividendProfiles.isEmpty())
						<div class="rounded-2xl border border-dashed border-base-300 bg-base-50 px-6 py-10 text-center text-sm text-base-content/60" data-profile-list-empty>${profileWindowFiltered ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.window.empty") : MessageUtil.getMessage("stock.page.dividend.monthly.reference.empty")}</div>
					@else
						<div class="overflow-x-auto">
							<table class="table table-sm" aria-label="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.list.title")}">
								<thead hx-boost="true">
									<tr>
										<th scope="col" aria-sort="${"display-order".equals(profileSortValue) ? ("asc".equals(profileDirectionValue) ? "ascending" : "descending") : "none"}">
											<a href="${referenceListBaseUrl}&profileSort=display-order&profileDirection=${displayOrderSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.display.order")}
												@if("display-order".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60" aria-hidden="true">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col" aria-sort="${"symbol".equals(profileSortValue) ? ("asc".equals(profileDirectionValue) ? "ascending" : "descending") : "none"}">
											<a href="${referenceListBaseUrl}&profileSort=symbol&profileDirection=${symbolSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.simulator.monthly.form.field.symbol")}
												@if("symbol".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60" aria-hidden="true">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col" aria-sort="${"payout-window".equals(profileSortValue) ? ("asc".equals(profileDirectionValue) ? "ascending" : "descending") : "none"}">
											<a href="${referenceListBaseUrl}&profileSort=payout-window&profileDirection=${payoutWindowSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window")}
												@if("payout-window".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60" aria-hidden="true">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col" aria-sort="${"source-url".equals(profileSortValue) ? ("asc".equals(profileDirectionValue) ? "ascending" : "descending") : "none"}">
											<a href="${referenceListBaseUrl}&profileSort=source-url&profileDirection=${sourceUrlSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.source.url")}
												@if("source-url".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60" aria-hidden="true">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col" aria-sort="${"last-verified-date".equals(profileSortValue) ? ("asc".equals(profileDirectionValue) ? "ascending" : "descending") : "none"}">
											<a href="${referenceListBaseUrl}&profileSort=last-verified-date&profileDirection=${lastVerifiedDateSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.last.verified.date")}
												@if("last-verified-date".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60" aria-hidden="true">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<th scope="col" aria-sort="${"latest-record-date".equals(profileSortValue) ? ("asc".equals(profileDirectionValue) ? "ascending" : "descending") : "none"}">
											<a href="${referenceListBaseUrl}&profileSort=latest-record-date&profileDirection=${latestRecordDateSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.latest.record.date")}
												@if("latest-record-date".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60" aria-hidden="true">${profileDirectionValue}</span>
												@endif
											</a>
										</th>
										<%-- 이 칸만 aria-sort 가 없었다(나머지 다섯은 있음) - 낭독기가 정렬 가능/현재 상태를 못 듣는다(실측 2026-09-21). --%>
										<th scope="col" aria-sort="${"active".equals(profileSortValue) ? ("asc".equals(profileDirectionValue) ? "ascending" : "descending") : "none"}">
											<a href="${referenceListBaseUrl}&profileSort=active&profileDirection=${activeSortDirection}" class="link link-hover font-medium no-underline">
												${MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active")}
												@if("active".equals(profileSortValue))
													<span class="ml-1 text-xs uppercase text-base-content/60" aria-hidden="true">${profileDirectionValue}</span>
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
												<a href="/stock/admin?tab=monthly-reference&symbol=${URLEncoder.encode(row.stockItemSymbol(), StandardCharsets.UTF_8)}&profileSort=${profileSortValue}&profileDirection=${profileDirectionValue}${profileWindowQuery}#monthly-reference-editor" class="link link-hover font-medium">${row.stockItemSymbol()}</a>
												<div class="text-xs text-base-content/60">${row.stockItemName()}</div>
												<div class="mt-2 flex flex-wrap gap-2">
													<button type="button" class="btn btn-xs btn-outline h-auto min-h-6 whitespace-normal" data-payout-peek-toggle data-peek-url="/stock/dividend/monthly-reference/payout/peek?symbol=${URLEncoder.encode(row.stockItemSymbol(), StandardCharsets.UTF_8)}" data-peek-loading="${payoutPeekLoading}" data-peek-failed="${payoutPeekFailed}" aria-expanded="false" aria-controls="payout-peek-${row.stockItemSymbol()}" aria-label="${java.text.MessageFormat.format(profileActionAriaPattern, payoutPeekLabel, row.stockItemName() != null && !row.stockItemName().isBlank() ? row.stockItemName() : row.stockItemSymbol())}">${payoutPeekLabel} <span aria-hidden="true" data-payout-peek-icon>▾</span></button>
													<a href="/stock/admin?tab=monthly-reference&symbol=${URLEncoder.encode(row.stockItemSymbol(), StandardCharsets.UTF_8)}&profileSort=${profileSortValue}&profileDirection=${profileDirectionValue}${profileWindowQuery}#monthly-reference-editor" class="btn btn-xs btn-outline" aria-label="${java.text.MessageFormat.format(profileActionAriaPattern, profileEditLabel, row.stockItemName() != null && !row.stockItemName().isBlank() ? row.stockItemName() : row.stockItemSymbol())}">${profileEditLabel}</a>
													@if(row.sourceUrl() != null && !row.sourceUrl().isEmpty())
														<%-- 출처에서 지급 이력 가져오기(사용자 요청 2026-10-14 - 따로 구성하지 말고 목록에 단추). 출처를 내려받아 오래 걸리므로 덮개를 띄운다. --%>
														<form method="post" action="/stock/dividend/monthly-reference/payout/import/source" class="inline" hx-boost="true" hx-replace-url="false" data-submit-overlay="true" data-submit-overlay-title="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.submit")}" data-submit-overlay-desc="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.import.overlay.desc.source")}" data-payout-source-import="${row.stockItemSymbol()}">
															<input type="hidden" name="profileSort" value="${profileSortValue}" />
															<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
															<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
															<input type="hidden" name="symbol" value="${row.stockItemSymbol()}" />
															<%-- 가져온 뒤에도 보던 종목(왼쪽 편집 · 아래 지급 이력)은 그대로 - 목록만 바꿔 끼우므로 선택이 바뀌면 화면이 갈린다. --%>
															<input type="hidden" name="returnSymbol" value="${selectedMonthlyDividendSymbol}" />
															<button type="submit" class="btn btn-xs btn-outline h-auto min-h-6 whitespace-normal" aria-label="${java.text.MessageFormat.format(profileActionAriaPattern, MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.row"), row.stockItemName() != null && !row.stockItemName().isBlank() ? row.stockItemName() : row.stockItemSymbol())}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.import.source.row")}</button>
														</form>
													@endif
													<a href="/stock/simulator?tab=monthly-dividend&symbol=${URLEncoder.encode(row.stockItemSymbol(), StandardCharsets.UTF_8)}&sort=display-order&direction=asc" class="btn btn-xs btn-primary" aria-label="${java.text.MessageFormat.format(profileActionAriaPattern, MessageUtil.getMessage("stock.page.dividend.monthly.reference.to.simulator"), row.stockItemName() != null && !row.stockItemName().isBlank() ? row.stockItemName() : row.stockItemSymbol())}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.to.simulator")}</a>
												</div>
											</td>
											<%-- 같은 화면의 편집 폼은 "월중/월말/기타/미확인" 으로 보여 주는데 이 칸만 코드였다
												 (실측 2026-09-12: 8 행이 MID_MONTH · MONTH_END). 폼과 같은 문구를 쓴다. --%>
											<td>${net.luversof.web.gate.stock.util.StockPayoutWindowLabelUtil.label(row.payoutWindow())}</td>
											<td>
												@if(row.sourceUrl() != null && !row.sourceUrl().isEmpty())
													<div class="space-y-1">
														<a href="${row.sourceUrl()}" class="link link-primary" target="_blank" rel="noopener noreferrer" aria-label="${java.text.MessageFormat.format(profileActionAriaPattern, openLinkLabel, row.stockItemName() != null && !row.stockItemName().isBlank() ? row.stockItemName() : row.stockItemSymbol()) + " (" + opensNewWindowLabel + ")"}">${openLinkLabel}<span class="sr-only"> (${opensNewWindowLabel})</span></a>
														<div class="max-w-xs break-all text-xs text-base-content/60">${row.sourceUrl()}</div>
													</div>
												@else
													-
												@endif
											</td>
											<td title="${row.lastVerifiedDate() == null ? notVerifiedLabel : null}">${row.lastVerifiedDate() != null ? String.valueOf(row.lastVerifiedDate()) : "-"}@if(row.lastVerifiedDate() == null)<span class="sr-only">${notVerifiedLabel}</span>@endif</td>
											!{var latestPayout = row.stockItemSymbol() != null ? monthlyDividendLatestPayoutBySymbol.get(row.stockItemSymbol().trim().toUpperCase(java.util.Locale.ROOT)) : null;}
											<%-- 마지막 기준일 + 며칠 전. 한 달 치가 빠졌을 만큼 오래됐으면 "갱신 확인" 표시(색만이 아니라 글자로). --%>
											@if(latestPayout != null)
												<td class="whitespace-nowrap" data-profile-latest-record-date="${String.valueOf(latestPayout.recordDate())}" data-profile-latest-record-stale="${latestPayout.stale() ? "true" : "false"}">
													<div>${String.valueOf(latestPayout.recordDate())}</div>
													<%-- 미리 발표돼 등록된 기준일은 아직 안 왔다 - "-1일 전" 으로 찍혔다(실측 2026-10-14, 10-15 기준일). 오늘 · N일 뒤로 적는다. --%>
													<div class="text-xs text-base-content/60">${latestPayout.daysSinceRecord() > 0 ? java.text.MessageFormat.format(daysAgoPattern, String.valueOf(latestPayout.daysSinceRecord())) : (latestPayout.daysSinceRecord() == 0 ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.latest.record.date.today") : java.text.MessageFormat.format(MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.latest.record.date.days.ahead"), String.valueOf(-latestPayout.daysSinceRecord())))}</div>
													@if(latestPayout.stale())
														<span class="badge badge-warning badge-sm mt-1" title="${staleRecordTitle}">${staleRecordLabel}</span>
													@endif
												</td>
											@else
												<td title="${noPayoutLabel}" data-profile-latest-record-date="">-<span class="sr-only">${noPayoutLabel}</span></td>
											@endif
											<td>${row.active() ? MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active.true") : MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.active.false")}</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					@endif
					</div>
			`)

			@template._components.ui.card(bodyClass = "gap-5", body = @`
					<%-- id: 프로필 목록을 바꿔 끼울 때 함께 바꾼다(행 링크가 정렬 · 거르기를 싣는다, 2026-10-13). --%>
					<div class="flex flex-col gap-5" id="monthlyReferencePayoutList">
					<div class="space-y-2">
						<%-- 입력칸은 size=20 만큼의 내재 폭을 갖는다 - w-full 로는 안 줄어들어 320px/200% 에서 폼이 280px 바닥을 만든다.
						     min-w-0 를 함께 줘야 좁은 폭에서 접힌다(실측 2026-09-21). --%>
						<%-- 배지는 기본이 한 줄(white-space: nowrap)이라 종목명이 길면 문서 폭을 늘린다 - 실측 2026-09-21:
						     320px/200% 에서 이 배지 하나가 422px 이 되어 문서가 566px(넘침 246px)였다. 접히게 둔다. --%>
						<h2 id="monthly-payout-list" class="card-title text-lg scroll-mt-24">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.list.title")}@if(payoutSymbolLabel != null && !payoutSymbolLabel.isBlank())<span class="badge badge-ghost badge-sm ml-2 align-middle h-auto whitespace-normal py-1 text-left" data-payout-history-symbol>${payoutSymbolLabel}</span>@endif</h2>
						<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.list.desc")}</p>
					</div>
					@if(monthlyDividendPayouts == null || monthlyDividendPayouts.isEmpty())
						<div class="rounded-2xl border border-dashed border-base-300 bg-base-50 px-6 py-10 text-center text-sm text-base-content/60">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.empty")}</div>
					@else
						<div class="overflow-x-auto">
							<table class="table table-sm" aria-label="${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.list.title")}">
								<thead>
									<tr>
										<th scope="col" class="whitespace-nowrap">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.record.date")}</th>
										<th scope="col" class="whitespace-nowrap">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.pay.date")}</th>
										@if(showDistributionRate)
											<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.distribution.rate")}</th>
										@endif
										<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.dividend.amount")}</th>
										<th scope="col" class="whitespace-nowrap text-right">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.taxable.base")}</th>
										<th scope="col"><span class="sr-only">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.delete")}</span></th>
									</tr>
								</thead>
								<tbody>
									@for(MonthlyDividendPayoutResponse row : monthlyDividendPayouts)
										<tr class="${selectedMonthlyDividendPayoutKey.equals(String.valueOf(row.recordDate()) + "|" + String.valueOf(row.payDate())) ? "bg-base-200/60" : ""}">
											<td class="whitespace-nowrap tabular-nums">
												<span class="font-medium" data-payout-record-date>${String.valueOf(row.recordDate())}</span>
											</td>
											<td class="whitespace-nowrap tabular-nums">${String.valueOf(row.payDate())}</td>
											@if(showDistributionRate)
												<td class="text-right tabular-nums">@if(row.distributionRatePct() != null)${percentFormat.format(row.distributionRatePct())}%@else-@endif</td>
											@endif
											<td class="text-right tabular-nums">₩${amountFormat.format(row.dividendAmountPerShare() != null ? row.dividendAmountPerShare() : BigDecimal.ZERO)}</td>
											<td class="text-right tabular-nums">@if(row.taxableBasePerShare() == null)<span class="text-base-content/60" data-taxable-unknown>${MessageUtil.getMessage("stock.monthly.taxable.unknown")}</span>@else₩${amountFormat.format(row.taxableBasePerShare())}@endif</td>
											<td class="text-right">
												<form method="post" action="/stock/dividend/monthly-reference/payout/delete" class="inline" data-confirm="${java.text.MessageFormat.format(MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.row.delete.confirm"), String.valueOf(row.recordDate()))}">
													<input type="hidden" name="profileSort" value="${profileSortValue}" />
													<input type="hidden" name="profileDirection" value="${profileDirectionValue}" />
													<input type="hidden" name="profileWindow" value="${profileWindowValue}" />
													<input type="hidden" name="symbol" value="${row.stockItemSymbol()}" />
													<input type="hidden" name="recordDate" value="${String.valueOf(row.recordDate())}" />
													<input type="hidden" name="payDate" value="${String.valueOf(row.payDate())}" />
													<button type="submit" class="btn btn-xs btn-ghost text-error h-auto min-h-6 whitespace-normal" aria-label="${java.text.MessageFormat.format(MessageUtil.getMessage("stock.page.dividend.monthly.reference.payout.row.delete.aria"), String.valueOf(row.recordDate()))}">${MessageUtil.getMessage("stock.page.dividend.monthly.reference.delete")}</button>
												</form>
											</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					@endif
					</div>
			`)
		</div>
	</div>
</div>

@template._components.ui.submitOverlay()
<script nonce="${CspNonceHolder.getNonce()}">
	// 프로필 목록만 바꿔 끼운 뒤(2026-10-13) 왼쪽 편집 폼의 숨은 정렬 · 거르기 칸을 새 값으로 맞춘다 - 안 그러면 저장 뒤 옛 정렬 · 거르기로 돌아간다.
	document.addEventListener('htmx:afterSettle', function() {
		var list = document.getElementById('monthlyReferenceProfileList');
		if (!list) {
			return;
		}
		var pairs = [['profileSort', list.dataset.profileSortValue], ['profileDirection', list.dataset.profileDirectionValue], ['profileWindow', list.dataset.profileWindowValue]];
		for (var p = 0; p < pairs.length; p++) {
			var inputs = document.querySelectorAll('input[type="hidden"][name="' + pairs[p][0] + '"]');
			for (var k = 0; k < inputs.length; k++) {
				inputs[k].value = pairs[p][1] || '';
			}
		}
	});
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