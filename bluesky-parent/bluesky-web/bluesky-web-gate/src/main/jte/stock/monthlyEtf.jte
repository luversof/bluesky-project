@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.text.DecimalFormat
@import java.util.List
@import net.luversof.web.gate.stock.dto.view.MonthlyEtfRowView
@import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport
@import net.luversof.web.gate.stock.service.MonthlyEtfViewSupport
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil

<%-- 월배당 ETF 목록. 등록된 월배당 기준 데이터(프로필 + 지급 이력)로 만든 **종목 단위** 정보다.
     사용자 요청 2026-09-21: 내가 받을 배당금(시뮬레이터 월배당 탭)과 성격이 다르므로 화면을 나눴다.
     금액 · 수량 · 예상 배당금은 여기에 없다 - 보유 중인지만 표시한다. --%>
@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param List<MonthlyEtfRowView> monthlyEtfRows = java.util.List.of()
@param int monthlyEtfTotalCount = 0
@param long monthlyEtfHeldCount = 0L
@param long monthlyEtfMissingPayoutCount = 0L
@param String monthlyEtfSort = "display-order"
@param String monthlyEtfDirection = "asc"
@param String monthlyEtfKeyword = ""
@param BigDecimal monthlyEtfMinAnnualYield = null
@param String monthlyEtfPayoutWindow = ""
@param String monthlyEtfHolding = "all"
@param String monthlyEtfAccount = ""
@param java.util.List<net.luversof.web.gate.stock.service.MonthlyEtfViewSupport.MonthlyEtfPick> monthlyEtfPicks = java.util.List.of()
@param java.util.Map<String, net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.ContributionPick> monthlyEtfContributionPicks = java.util.Map.of()
@param String monthlyEtfView = ""
@param int monthlyEtfPeriod = 12
@param String monthlyEtfBasis = "dividend"
@param java.util.Map<String, net.luversof.web.gate.stock.service.MonthlyContributionPickSupport.ContributionCandidate> monthlyEtfCandidates = java.util.Map.of()
@param java.util.List<Integer> monthlyEtfPeriods = java.util.List.of(1, 3, 6, 12)

!{
	DecimalFormat amountFormat = net.luversof.web.gate.stock.util.StockFormatUtil.wonFormat();
	DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
	String referenceManageUrl = "/stock/admin?tab=monthly-reference";
	String simulatorUrl = "/stock/simulator?tab=monthly-dividend";
	// 머리칸 정렬 링크는 화면의 필터를 함께 싣는다 - 안 그러면 정렬 한 번에 검색어 · 필터가 조용히 풀린다
	// (시뮬레이터 표에서 실제로 났던 일, 2026-09-13).
	String baseUrl = "/stock/monthly-etf?"
		+ (monthlyEtfKeyword == null || monthlyEtfKeyword.isBlank() ? ""
			: "keyword=" + java.net.URLEncoder.encode(monthlyEtfKeyword, java.nio.charset.StandardCharsets.UTF_8) + "&")
		+ (monthlyEtfMinAnnualYield == null ? "" : "minAnnualYield=" + monthlyEtfMinAnnualYield.toPlainString() + "&")
		+ (monthlyEtfPayoutWindow == null || monthlyEtfPayoutWindow.isBlank() ? "" : "payoutWindow=" + monthlyEtfPayoutWindow + "&")
		+ ("all".equals(monthlyEtfHolding) ? "" : "holding=" + monthlyEtfHolding + "&")
		+ (monthlyEtfAccount == null || monthlyEtfAccount.isBlank() ? "" : "account=" + monthlyEtfAccount + "&")
		+ (monthlyEtfView == null || monthlyEtfView.isBlank() ? "" : "view=" + monthlyEtfView + "&")
		+ ("dividend".equals(monthlyEtfBasis) ? "" : "basis=" + monthlyEtfBasis + "&")
		+ "period=" + monthlyEtfPeriod + "&";
	// 추천 기준 단추는 자기 기준만 갈아 끼운다(사용자 요청 2026-10-06) - 필터 · 보기 · 기간 · 정렬은 그대로.
	String basisBase = baseUrl.replaceAll("basis=[a-z]+&", "");
	String basisSortQuery = "sort=" + monthlyEtfSort + "&direction=" + monthlyEtfDirection;
	// 기간 단추는 자기 기간만 갈아 끼운다 - 나머지 필터와 정렬은 그대로 들고 간다.
	String periodQuery = baseUrl.substring("/stock/monthly-etf?".length())
		.replaceAll("period=[0-9]+&", "")
		+ "sort=" + monthlyEtfSort + "&direction=" + monthlyEtfDirection + "&";
	// 초기화는 보이는 필터만 비운다 - 폼 밖에서 고른 보기(이번 적립) · 기간 · 정렬은 지킨다
	// (실측 2026-09-24: 이번 적립 보기에서 초기화를 누르면 전체 보기 · 기간 1년으로 튕겼다).
	String resetUrl = "/stock/monthly-etf?"
		+ (monthlyEtfView == null || monthlyEtfView.isBlank() ? "" : "view=" + monthlyEtfView + "&")
		+ ("dividend".equals(monthlyEtfBasis) ? "" : "basis=" + monthlyEtfBasis + "&")
		+ "period=" + monthlyEtfPeriod + "&sort=" + monthlyEtfSort + "&direction=" + monthlyEtfDirection;
	String minAnnualYieldValue = monthlyEtfMinAnnualYield != null ? monthlyEtfMinAnnualYield.stripTrailingZeros().toPlainString() : "";
	String directionIndicator = "asc".equals(monthlyEtfDirection) ? "↑" : "↓";
	String midMonthLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month");
	String monthEndLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end");
	String otherWindowLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.other");
	String unknownWindowLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.unknown");
	String latestLabel = MessageUtil.getMessage("stock.simulator.monthly.table.cell.latest.short");
	String averageLabel = MessageUtil.getMessage("stock.simulator.monthly.table.cell.average.short");
	String trendLabel = MessageUtil.getMessage("stock.monthly.etf.table.cell.trend.short");
	// 위험 지표는 1 년 기준이다. 이력이 그만큼 없는 종목은 "언제부터" 를 적어야 "1 년 기준" 이 거짓이 되지 않는다.
	// 며칠 어긋나는 것까지 적으면 거의 모든 줄에 붙으므로, 한 달 넘게 모자랄 때만 적는다.
	java.time.LocalDate riskFullFrom = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).minusMonths(12).plusMonths(1);
	java.util.function.Predicate<java.time.LocalDate> riskPartial = from -> from.isAfter(riskFullFrom);
	String trendUnknownLabel = MessageUtil.getMessage("stock.monthly.etf.table.cell.trend.unknown");
	// 날짜는 한 덩어리로 둔다(사용자 요청 2026-09-30: "날짜 줄바꿈도 날짜만 묶어서"). 칸이 좁으면 "2024-12-/03" 처럼 하이픈에서 갈렸다.
	// 날짜 줄 전체를 묶으면 표가 1134 -> 1245px 로 넘친다(실측) - 날짜만 묶고 앞뒤 낱말은 흐르게 둔다.
	// 문장 속 날짜 자리({0})에 표식을 넣어 앞뒤를 가른다. 표식은 제어 문자라 메시지에 나올 일이 없다.
	String dateMark = String.valueOf((char) 1);
	java.util.function.Function<String, String[]> aroundDate = key -> {
		String[] parts = java.text.MessageFormat.format(MessageUtil.getMessage(key), dateMark).split(java.util.regex.Pattern.quote(dateMark), -1);
		return parts.length == 2 ? parts : new String[] { parts[0], "" };
	};
	String[] listingDateParts = aroundDate.apply("stock.monthly.etf.table.cell.listing.date");
	String[] riskFromParts = aroundDate.apply("stock.monthly.etf.table.cell.risk.from");
	String[] periodBaseParts = aroundDate.apply("stock.monthly.etf.table.cell.period.base");
	String[] periodMissingParts = aroundDate.apply("stock.monthly.etf.table.cell.period.missing");
}

@template._layout.stockLayout(
	pageTitle = MessageUtil.getMessage("stock.page.monthly.etf.title"),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.monthly.etf.title"), description = MessageUtil.getMessage("stock.page.monthly.etf.desc"))

			<div class="grid gap-4 stat-card-grid grid-cols-2 lg:grid-cols-3">
				@template._components.ui.statCard(label = MessageUtil.getMessage("stock.monthly.etf.summary.count"), value = amountFormat.format(monthlyEtfTotalCount), sub = MessageUtil.getMessage("stock.monthly.etf.summary.count.sub"))
				@template._components.ui.statCard(label = MessageUtil.getMessage("stock.monthly.etf.summary.held"), value = amountFormat.format(monthlyEtfHeldCount), sub = MessageUtil.getMessage("stock.monthly.etf.summary.held.sub"))
				@template._components.ui.statCard(label = MessageUtil.getMessage("stock.monthly.etf.summary.missing.payout"), value = amountFormat.format(monthlyEtfMissingPayoutCount), sub = MessageUtil.getMessage("stock.monthly.etf.summary.missing.payout.sub"), tone = monthlyEtfMissingPayoutCount > 0 ? "warning" : "")
			</div>

			@template._components.ui.card(bodyClass = "gap-5", body = @`

				<%-- 표 제목·검색·기간 줄을 헤더 아래에 붙여 둔다(사용자 요청 2026-09-22). 목록이 21 행이 되면서
				     좁하려고 맨 위까지 되돌아가야 했고, 어느 표를 보는 중인지도 사라진다.
				     좁은 화면에서는 안 붙는다(main.css 참고). --%>
				<div class="table-filter-sticky space-y-3">
					<div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
						<div class="space-y-2">
							<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.monthly.etf.section.table.title")}</h2>
							<p class="text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.section.table.desc")}</p>
						</div>
						<div class="flex flex-wrap items-center gap-2">
							<a href="${referenceManageUrl}" class="btn btn-outline btn-sm h-auto max-w-full whitespace-normal">${MessageUtil.getMessage("stock.monthly.etf.manage")}</a>
							<a href="${simulatorUrl}" class="btn btn-ghost btn-sm h-auto max-w-full whitespace-normal">${MessageUtil.getMessage("stock.monthly.etf.simulator.link")}</a>
						</div>
					</div>
					<form method="get" action="/stock/monthly-etf" class="grid gap-3 rounded-box bg-base-200/40 p-3 md:grid-cols-2 xl:grid-cols-5">
						<input type="hidden" name="sort" value="${monthlyEtfSort}" />
						<input type="hidden" name="direction" value="${monthlyEtfDirection}" />
						<%-- 필터를 적용해도 "이번 적립만 보기" 는 이어진다. --%>
						@if(monthlyEtfView != null && !monthlyEtfView.isBlank())<input type="hidden" name="view" value="${monthlyEtfView}" />@endif
						<%-- 기간도 이어진다 - 정렬 링크처럼(없으면 필터를 적용할 때마다 기간이 기본값으로 풀렸다, 2026-09-24). --%>
						<input type="hidden" name="period" value="${monthlyEtfPeriod}" />
						@if(!"dividend".equals(monthlyEtfBasis))<input type="hidden" name="basis" value="${monthlyEtfBasis}" />@endif
						<label class="form-control min-w-0 max-w-full">
							<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.keyword")}</span></div>
							<input name="keyword" type="search" value="${monthlyEtfKeyword}" placeholder="${MessageUtil.getMessage("stock.monthly.etf.filter.keyword.placeholder")}" class="input input-bordered input-sm w-full max-w-full" />
						</label>
						<label class="form-control min-w-0 max-w-full">
							<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.min.annual.yield")}</span></div>
							<input name="minAnnualYield" type="number" step="0.01" min="0" value="${minAnnualYieldValue}" class="input input-bordered input-sm w-full max-w-full" />
						</label>
						<label class="form-control min-w-0 max-w-full">
							<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.payout.window")}</span></div>
							<select name="payoutWindow" class="select select-bordered select-sm w-full max-w-full">
								<option value="" selected="${monthlyEtfPayoutWindow == null || monthlyEtfPayoutWindow.isBlank()}">${MessageUtil.getMessage("stock.monthly.etf.filter.payout.window.all")}</option>
								<option value="MID_MONTH" selected="${"MID_MONTH".equals(monthlyEtfPayoutWindow)}">${midMonthLabel}</option>
								<option value="MONTH_END" selected="${"MONTH_END".equals(monthlyEtfPayoutWindow)}">${monthEndLabel}</option>
								<option value="OTHER" selected="${"OTHER".equals(monthlyEtfPayoutWindow)}">${otherWindowLabel}</option>
								<option value="UNKNOWN" selected="${"UNKNOWN".equals(monthlyEtfPayoutWindow)}">${unknownWindowLabel}</option>
							</select>
						</label>
						<label class="form-control min-w-0 max-w-full">
							<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.account")}</span></div>
						<%-- 과세표준 비중 10% 를 기준으로 가른다 - 적립 추천과 같은 규칙이다(사용자 요청 2026-09-22). --%>
						<select name="account" class="select select-bordered select-sm w-full max-w-full">
							<option value="" selected="${monthlyEtfAccount == null || monthlyEtfAccount.isBlank()}">${MessageUtil.getMessage("stock.monthly.etf.filter.account.all")}</option>
							<option value="BROKERAGE" selected="${"BROKERAGE".equals(monthlyEtfAccount)}">${MessageUtil.getMessage("stock.monthly.etf.filter.account.brokerage")}</option>
							<option value="PENSION" selected="${"PENSION".equals(monthlyEtfAccount)}">${MessageUtil.getMessage("stock.monthly.etf.filter.account.pension")}</option>
						</select>
					</label>
					<label class="form-control min-w-0 max-w-full">
						<div class="label py-1"><span class="label-text text-xs font-medium">${MessageUtil.getMessage("stock.monthly.etf.filter.holding")}</span></div>
							<select name="holding" class="select select-bordered select-sm w-full max-w-full">
								<option value="all" selected="${"all".equals(monthlyEtfHolding)}">${MessageUtil.getMessage("stock.monthly.etf.filter.holding.all")}</option>
								<option value="held" selected="${"held".equals(monthlyEtfHolding)}">${MessageUtil.getMessage("stock.monthly.etf.filter.holding.held")}</option>
								<option value="not-held" selected="${"not-held".equals(monthlyEtfHolding)}">${MessageUtil.getMessage("stock.monthly.etf.filter.holding.not.held")}</option>
							</select>
						</label>
						<div class="flex items-end gap-2">
							<button type="submit" class="btn btn-primary btn-sm max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere">${MessageUtil.getMessage("stock.monthly.etf.filter.apply")}</button>
							<%-- 필터 지우기 링크는 일부러 기준 주소(필터를 싣는 주소)를 쓰지 않는다. --%>
							<a data-filter-reset href="${resetUrl}" class="btn btn-ghost btn-sm max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere">${MessageUtil.getMessage("stock.monthly.etf.filter.reset")}</a>
						</div>
					</form>

					<%-- 기간을 고르는 단추 한 줄(사용자 결정 2026-09-21). 열을 여덟 개 더 늘리는 대신 한 번에 한 기준만 본다.
					     정렬도 이 기간을 따른다 - 주소에 period 를 싣고 다니므로 정렬 링크를 눌러도 기간이 풀리지 않는다. --%>
					<%-- 기간 · 추천 기준을 한 줄에(사용자 요청 2026-10-08: "추천 기준도 sticky") - 이 줄이 붙어 있으므로 표를 내려 봐도 기준을 바꿀 수 있다.
					     넓은 화면(1280px~)에서 한 줄로 접혀 붙은 줄이 높아지지 않는다. 표가 비면 고를 것이 없으니 기준 단추를 내지 않는다. --%>
					<div class="flex flex-wrap items-center gap-x-6 gap-y-2">
						<div class="flex flex-wrap items-center gap-2" role="group" aria-label="${MessageUtil.getMessage("stock.monthly.etf.period.label")}">
							<span class="text-sm font-medium text-base-content/70">${MessageUtil.getMessage("stock.monthly.etf.period.label")}</span>
							@for(int periodOption : monthlyEtfPeriods)
								!{String periodLabel = periodOption == 12
								    ? MessageUtil.getMessage("stock.monthly.etf.period.year")
								    : java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.period.month"), periodOption);}
								!{boolean periodSelected = periodOption == monthlyEtfPeriod;}
								<a data-period-option href="/stock/monthly-etf?${periodQuery}period=${periodOption}"
									class="btn btn-sm h-auto min-h-8 whitespace-normal py-2 leading-snug ${periodSelected ? "btn-primary" : "btn-ghost border border-base-300"}"
									aria-current="${periodSelected ? "true" : "false"}">${periodLabel}</a>
							@endfor
						</div>
						@if(monthlyEtfRows != null && !monthlyEtfRows.isEmpty() && (!monthlyEtfPicks.isEmpty() || !monthlyEtfContributionPicks.isEmpty() || !"dividend".equals(monthlyEtfBasis)))
						<div class="flex flex-wrap items-center gap-2" data-pick-basis="${monthlyEtfBasis}" role="group" aria-label="${MessageUtil.getMessage("stock.pick.basis.label")}">
							<span class="text-sm font-medium text-base-content/70">${MessageUtil.getMessage("stock.pick.basis.label")}</span>
							@for(String basisOption : java.util.List.of("dividend", "total", "stable", "steady"))
								<a href="${basisBase + ("dividend".equals(basisOption) ? "" : "basis=" + basisOption + "&") + basisSortQuery}" class="btn btn-sm h-auto min-h-8 whitespace-normal py-2 leading-snug break-keep ${basisOption.equals(monthlyEtfBasis) ? "btn-primary" : "btn-ghost border border-base-300"}" aria-current="${basisOption.equals(monthlyEtfBasis) ? "true" : null}" data-pick-basis-link="${basisOption}">${net.luversof.web.gate.stock.util.StockPickBasisLabelUtil.label(basisOption)}</a>
							@endfor
						</div>
						@endif
					</div>
				</div>

				@if(monthlyEtfRows == null || monthlyEtfRows.isEmpty())
					<div class="rounded-2xl border border-dashed border-base-300 bg-base-50 px-6 py-10 text-center">
						@if(monthlyEtfTotalCount > 0)
							<p class="text-lg font-semibold text-base-content">${MessageUtil.getMessage("stock.monthly.etf.empty.filtered.title")}</p>
							<p class="mt-2 text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.empty.filtered.desc")}</p>
						@else
							<p class="text-lg font-semibold text-base-content">${MessageUtil.getMessage("stock.monthly.etf.empty.title")}</p>
							<p class="mt-2 text-sm leading-6 text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.empty.desc")}</p>
							<a href="${referenceManageUrl}" class="btn btn-primary btn-sm mt-4">${MessageUtil.getMessage("stock.monthly.etf.manage")}</a>
						@endif
					</div>
				@else
					<%-- 넓은 화면에서는 열 이름이 늘 보이게 머리글을 붙인다(사용자 요청 2026-09-22).
					     가로 스크롤 상자를 걷어내야 sticky 가 페이지에 붙는다 - main.css 의 .table-head-sticky 참고. --%>
					<%-- 지금 걸린 조건 안에서 고른 셋(사용자 결정 2026-09-22). 셈은 적립 추천과 같다 -
					     연배당에서 분배금이 줄어든 만큼 뺀다. 추세를 아직 모르는 종목은 뺀다. --%>
					<%-- 추천 기준(사용자 요청 2026-10-06: "etf 추천 기준을 다양하게"). 아래 "지금 눈여겨볼 종목" 과 자리별 적립 추천이 같은 기준을 쓴다.
					     후보는 기준과 상관없이 같고 셈만 바뀐다(MonthlyContributionPickSupport.Basis). --%>
					@if(!monthlyEtfPicks.isEmpty() || !monthlyEtfContributionPicks.isEmpty() || !"dividend".equals(monthlyEtfBasis))
						<%-- 기준 단추는 위 고정 줄(기간 옆)로 옮겼다(사용자 요청 2026-10-08) - 설명은 카드 옆에 남긴다. --%>
						<p class="text-xs leading-5 text-base-content/60" data-pick-basis-desc>${net.luversof.web.gate.stock.util.StockPickBasisLabelUtil.description(monthlyEtfBasis)}</p>
					@endif
					@if(!monthlyEtfPicks.isEmpty())
						<div class="rounded-box border border-base-300 bg-base-200/40 p-2 sm:p-4 space-y-3" data-etf-picks>
							<div class="flex flex-wrap items-baseline gap-x-2 gap-y-1">
								<h3 class="font-semibold">${MessageUtil.getMessage("stock.monthly.etf.pick.title")}</h3>
								@if("dividend".equals(monthlyEtfBasis))<p class="text-xs leading-5 text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.pick.desc")}</p>@endif
							</div>
							<div class="grid gap-3 sm:grid-cols-3">
							@for(int pickIndex = 0; pickIndex < monthlyEtfPicks.size(); pickIndex++)
								!{var pick = monthlyEtfPicks.get(pickIndex);}
								<div class="rounded-box bg-base-100 p-2 sm:p-3 space-y-1" data-etf-pick data-pick-symbol="${pick.row().stockItemSymbol()}">
									<div class="flex flex-wrap items-baseline gap-2">
										<span class="badge badge-sm badge-neutral" data-pick-rank>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.pick.rank"), pickIndex + 1)}</span>
										<%-- 지급 시기를 함께 적는다(사용자 요청 2026-09-23) - 월중과 월말은 적립하는 날이 다르다. --%>
										<span class="badge badge-sm badge-ghost" data-pick-window="${pick.row().payoutWindow()}">${"MID_MONTH".equals(pick.row().payoutWindow()) ? midMonthLabel : ("MONTH_END".equals(pick.row().payoutWindow()) ? monthEndLabel : ("OTHER".equals(pick.row().payoutWindow()) ? otherWindowLabel : unknownWindowLabel))}</span>
										<span class="font-medium break-keep wrap-anywhere">${pick.row().stockItemName()}</span>
									</div>
									<div class="text-xs text-base-content/60">${pick.row().stockItemSymbol()}</div>
									!{var pickCandidate = monthlyEtfCandidates.get(pick.row().stockItemSymbol());}
									<div class="flex flex-wrap items-baseline gap-1">
										<span class="text-lg font-semibold tabular-nums" data-pick-score>${"steady".equals(monthlyEtfBasis) && pickCandidate != null ? String.valueOf(pickCandidate.payoutCutCount()) : percentFormat.format(pick.score())}</span>
										<span class="text-xs text-base-content/60">${"total".equals(monthlyEtfBasis) ? MessageUtil.getMessage("stock.pick.basis.unit.total") : ("stable".equals(monthlyEtfBasis) ? MessageUtil.getMessage("stock.pick.basis.unit.stable") : ("steady".equals(monthlyEtfBasis) ? MessageUtil.getMessage("stock.pick.basis.unit.steady") : MessageUtil.getMessage("stock.monthly.etf.pick.score")))}</span>
									</div>
									<%-- 점수가 어떻게 나왔는지 뺄셈으로 적는다(사용자 요청 2026-09-30). 두 수는 점수를 낼 때와 똑같이 반올림한다. --%>
									!{BigDecimal pickCut = MonthlyContributionPickSupport.cutOf(pick.row().payoutTrendPct());}
									!{String pickYield = percentFormat.format(pick.row().annualYieldPct().setScale(2, java.math.RoundingMode.HALF_UP));}
									<div class="text-xs text-base-content/60 tabular-nums" data-pick-basis>
										@if("total".equals(monthlyEtfBasis))
											${java.text.MessageFormat.format(MessageUtil.getMessage("stock.pick.basis.value.total"), percentFormat.format(pick.score()), pickYield)}
										@elseif("steady".equals(monthlyEtfBasis) && pickCandidate != null)
											${java.text.MessageFormat.format(MessageUtil.getMessage("stock.pick.basis.value.steady"), pickCandidate.payoutCutPairs(), pickCandidate.payoutCutCount(), percentFormat.format(pickCandidate.dividendScore()))}
										@elseif("stable".equals(monthlyEtfBasis))
											${java.text.MessageFormat.format(MessageUtil.getMessage("stock.pick.basis.value.stable"), percentFormat.format(MonthlyContributionPickSupport.score(pick.row().annualYieldPct(), pick.row().payoutTrendPct())), percentFormat.format(pick.row().volatilityPct()), percentFormat.format(pick.score()))}
										@elseif(pickCut.signum() > 0)
											${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.pick.basis"), pickYield, percentFormat.format(pickCut), percentFormat.format(pick.score()))}
										@else
											${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.pick.basis.nocut"), pickYield, percentFormat.format(pick.score()))}
										@endif
									</div>
								</div>
							@endfor
							</div>
							<%-- 점수 계산 방법(사용자 요청 2026-09-30). 카드마다 뺄셈이 적혀 있으니 여기서는 규칙과 예만 - 접어 둔다. --%>
							@if("dividend".equals(monthlyEtfBasis))
							<details class="text-xs leading-5 text-base-content/70" data-score-how>
								<summary class="cursor-pointer font-medium text-base-content/80">${MessageUtil.getMessage("stock.monthly.etf.pick.how.title")}</summary>
								<div class="mt-2 space-y-1">
									<p class="font-medium text-base-content/80" data-score-formula>${MessageUtil.getMessage("stock.monthly.etf.pick.how.formula")}</p>
									<p>${MessageUtil.getMessage("stock.monthly.etf.pick.how.cut")}</p>
									<p>${MessageUtil.getMessage("stock.monthly.etf.pick.how.why")}</p>
									<p class="tabular-nums">${MessageUtil.getMessage("stock.monthly.etf.pick.how.example")}</p>
									<p>${MessageUtil.getMessage("stock.monthly.etf.pick.how.missing")}</p>
									<p><a href="${baseUrl}sort=score&direction=desc" class="link link-primary" data-score-sort-link>${MessageUtil.getMessage("stock.monthly.etf.pick.how.sort")}</a></p>
								</div>
							</details>
							@endif
						</div>
					@endif

					<%-- 자리별 적립 추천만 보기(사용자 요청 2026-09-23: ETF 가 많아지면 네 자리만 보고 싶다. 결정: 한 번 누르는 단추).
					     2026-09-30 "이번 적립" 에서 이름을 바꿨다 - "월중 월말 섞어서 나오는데 기준이 뭐야? 다음 월말이나 다음 월중을 보여주는게 아닌거 같은데".
					     다음 지급일이 아니라 자리(지급 시기 x 계좌)마다 하나다. 그래서 자리 차례로 놓고(sort=slot), 무슨 자리인지 배지에 적는다.
					     다른 조건은 그대로 들고 간다. 추천이 하나도 없으면 단추를 내지 않는다(0 을 누르면 빈 표뿐이다). --%>
					!{String viewlessBase = baseUrl.replace("view=" + monthlyEtfView + "&", "");}
					!{String viewlessQuery = viewlessBase + "sort=" + ("slot".equals(monthlyEtfSort) ? "display-order" : monthlyEtfSort) + "&direction=" + monthlyEtfDirection;}
					@if(!monthlyEtfContributionPicks.isEmpty() || !monthlyEtfView.isBlank())
						<div class="flex flex-wrap gap-2" data-contribution-view>
							<a href="${viewlessBase.replace("/stock/monthly-etf?", "/stock/monthly-etf?view=contribution&") + "sort=slot&direction=asc"}" class="btn btn-sm ${"contribution".equals(monthlyEtfView) ? "btn-primary" : "btn-outline"} max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere" aria-current="${"contribution".equals(monthlyEtfView) ? "page" : null}" data-contribution-view-link="contribution">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.view.contribution"), monthlyEtfContributionPicks.size())}</a>
							<a href="${viewlessQuery}" class="btn btn-sm ${"contribution".equals(monthlyEtfView) ? "btn-outline" : "btn-primary"} max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere" aria-current="${"contribution".equals(monthlyEtfView) ? null : "page"}" data-contribution-view-link="all">${MessageUtil.getMessage("stock.monthly.etf.view.all")}</a>
						</div>
						@if("contribution".equals(monthlyEtfView))
							<p class="text-xs leading-5 text-base-content/70" data-contribution-view-desc>${MessageUtil.getMessage("stock.monthly.etf.view.contribution.desc")}</p>
							<%-- 자리마다 기준별 1위(사용자 질문 2026-10-06). 시뮬레이터 자리 카드와 같은 조각. --%>
							<div class="grid gap-3 sm:grid-cols-2" data-contribution-compare-cards>
								@for(var slotPick : monthlyEtfContributionPicks.values())
									<div class="rounded-box border border-base-200 bg-base-100 p-3" data-contribution-compare-card="${slotPick.payoutWindow()}|${slotPick.account()}">
										<div class="text-xs font-medium text-base-content/60">${"MID_MONTH".equals(slotPick.payoutWindow()) ? midMonthLabel : monthEndLabel} · ${"BROKERAGE".equals(slotPick.account()) ? MessageUtil.getMessage("stock.simulator.monthly.contribution.account.brokerage") : MessageUtil.getMessage("stock.simulator.monthly.contribution.account.pension")}</div>
										<div class="mt-1 flex flex-wrap items-baseline gap-2"><span class="font-mono font-semibold">${slotPick.symbol()}</span><span class="text-sm leading-tight wrap-anywhere">${slotPick.name()}</span></div>
										@template.stock.fragments.contributionCompare(pick = slotPick, percentFormat = percentFormat)
									</div>
								@endfor
							</div>
						@endif
					@endif

					<div class="overflow-x-auto table-head-sticky">
						<table class="table table-zebra table-sm whitespace-nowrap print-dense" aria-label="${MessageUtil.getMessage("stock.monthly.etf.section.table.title")}">
							<thead>
								<tr>
									<%-- 종목 열 최소 폭 9rem -> 8rem(2026-09-30): 영어 1440px 표가 상자를 5px 넘었다(열 최소 폭 합 1,139 / 상자 1,134). 긴 종목명은 원래 줄을 바꾼다. --%>
									<th scope="col" class="min-w-[8rem] align-bottom" aria-sort="${"symbol".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=symbol&direction=${"symbol".equals(monthlyEtfSort) && "asc".equals(monthlyEtfDirection) ? "desc" : "asc"}" class="link link-hover inline-flex items-center gap-1 font-medium no-underline">
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.item")}</span>
											@if("symbol".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
										</a>
									</th>
									<th scope="col" data-keep-words class="align-bottom" aria-sort="${"payout-window".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=payout-window&direction=${"payout-window".equals(monthlyEtfSort) && "asc".equals(monthlyEtfDirection) ? "desc" : "asc"}" class="link link-hover inline-flex items-center gap-1 font-medium no-underline">
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.payout.window")}</span>
											@if("payout-window".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"monthly-dividend".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=monthly-dividend&direction=${"monthly-dividend".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
											@if("monthly-dividend".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.dividend.per.share")}</span>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"taxable-base".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=taxable-base&direction=${"taxable-base".equals(monthlyEtfSort) && "asc".equals(monthlyEtfDirection) ? "desc" : "asc"}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
											@if("taxable-base".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.taxable")}</span>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"expense-ratio".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=expense-ratio&direction=${"expense-ratio".equals(monthlyEtfSort) && "asc".equals(monthlyEtfDirection) ? "desc" : "asc"}" class="link link-hover no-underline">
											@if("expense-ratio".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.expense.ratio")}</span>
											<div class="text-[11px] font-normal text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.header.expense.ratio.desc")}</div>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right">${MessageUtil.getMessage("stock.monthly.etf.table.header.price")}</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"max-drawdown".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=max-drawdown&direction=${"max-drawdown".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover no-underline">
											@if("max-drawdown".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.drawdown")}</span>
											<div class="text-[11px] font-normal text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.header.drawdown.desc")}</div>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"volatility".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=volatility&direction=${"volatility".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover no-underline">
											@if("volatility".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.volatility")}</span>
											<div class="text-[11px] font-normal text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.header.volatility.desc")}</div>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"annual-yield".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=annual-yield&direction=${"annual-yield".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
											@if("annual-yield".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.yield")}</span>
										</a>
									</th>
									<%-- 점수 열(사용자 요청 2026-09-30: "점수 기준에 대한 설명도 추가해주고 점수 기준으로 정렬해서 볼 수 있으면").
									     "지금 눈여겨볼 종목" 과 같은 셈 - 바로 왼쪽 연배당에서 분배금이 줄어든 만큼 뺀다. 칸 안에 그 뺄셈을 적는다. --%>
									<th scope="col" class="align-bottom text-right" aria-sort="${"score".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}" data-score-header>
										<a href="${baseUrl}sort=score&direction=${"score".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover no-underline">
											@if("score".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.score")}</span>
											<div class="text-[11px] font-normal text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.header.score.desc")}</div>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"period-price".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=period-price&direction=${"period-price".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover no-underline">
											@if("period-price".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.period.price")}</span>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"period-total".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=period-total&direction=${"period-total".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover no-underline">
											@if("period-total".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.period.total")}</span>
											<div class="text-[11px] font-normal text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.header.period.total.desc")}</div>
										</a>
									</th>
									<th scope="col" class="align-bottom text-right" aria-sort="${"payout-count".equals(monthlyEtfSort) ? ("asc".equals(monthlyEtfDirection) ? "ascending" : "descending") : "none"}">
										<a href="${baseUrl}sort=payout-count&direction=${"payout-count".equals(monthlyEtfSort) && "desc".equals(monthlyEtfDirection) ? "asc" : "desc"}" class="link link-hover inline-flex items-center justify-end gap-1 font-medium no-underline">
											@if("payout-count".equals(monthlyEtfSort))<span class="text-xs text-base-content/60" aria-hidden="true">${directionIndicator}</span>@endif
											<span>${MessageUtil.getMessage("stock.monthly.etf.table.header.history")}</span>
										</a>
									</th>
								</tr>
							</thead>
							<tbody>
								@for(MonthlyEtfRowView row : monthlyEtfRows)
									!{String windowLabel = "MID_MONTH".equals(row.payoutWindow()) ? midMonthLabel : ("MONTH_END".equals(row.payoutWindow()) ? monthEndLabel : ("OTHER".equals(row.payoutWindow()) ? otherWindowLabel : unknownWindowLabel));}
									<tr data-monthly-etf-row data-symbol="${row.stockItemSymbol()}">
										<th scope="row" class="align-top font-medium">
											<div class="flex items-center gap-2 whitespace-nowrap">
												<span>${row.stockItemSymbol()}</span>
												@if(row.held())
													<span class="badge badge-xs badge-primary" data-held-badge title="${MessageUtil.getMessage("stock.monthly.etf.table.cell.held.title")}">${MessageUtil.getMessage("stock.monthly.etf.table.cell.held")}<span class="sr-only"> ${MessageUtil.getMessage("stock.monthly.etf.table.cell.held.title")}</span></span>
												@endif
												@if(!row.active())
													<span class="badge badge-xs badge-ghost" data-inactive-badge>${MessageUtil.getMessage("stock.monthly.etf.table.cell.inactive")}</span>
												@endif
											</div>
											<div class="mt-1 text-sm font-normal leading-tight text-base-content/70">@if(row.stockItemId() != null)<a class="link link-hover" href="${StockDetailLinkUtil.item(row.stockItemId().toString())}">${row.stockItemName()}</a>@else${row.stockItemName()}@endif</div>
											<%-- 이번 적립 자리(시뮬레이터 월배당 탭의 4자리 추천과 같은 답 - 같은 코드로 낸다). --%>
											!{var contributionPick = monthlyEtfContributionPicks.get(row.stockItemSymbol());}
											@if(contributionPick != null)
												<div class="mt-1"><span class="badge badge-sm badge-primary whitespace-normal h-auto" data-contribution-pick="${contributionPick.payoutWindow()}|${contributionPick.account()}">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.table.cell.contribution"), "MID_MONTH".equals(contributionPick.payoutWindow()) ? midMonthLabel : monthEndLabel, "BROKERAGE".equals(contributionPick.account()) ? MessageUtil.getMessage("stock.simulator.monthly.contribution.account.brokerage") : MessageUtil.getMessage("stock.simulator.monthly.contribution.account.pension"))}</span></div>
											@endif
										</th>
										<td class="align-top" data-keep-line>${windowLabel}</td>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												<div><span class="mr-1 text-[11px] text-base-content/60">${averageLabel}</span><span class="amount-value">${StockFormatUtil.fullKrw(row.averageDividendPerShare1y(), 2)}</span></div>
												<div class="text-sm text-base-content/80"><span class="mr-1 text-[11px] text-base-content/60">${latestLabel}</span><span class="amount-value">${StockFormatUtil.fullKrw(row.latestDividendPerShare(), 2)}</span></div>
												<%-- 분배금 추세(사용자 결정 2026-09-22): 최근 3 회 평균을 바로 위 1 년 평균과 견준다. 연배당 수익률은 1 년 평균으로
												     내는 값이라 최근 삭감을 앞쪽 큰 분배금이 가려 준다 - 그 삭감이 여기서 드러난다(실측: PLUS 고배당주위클리커버드콜 -23.3%).
												     이력이 여섯 회에 못 미치면 0% 대신 "이력 부족" 이라 적는다. 0 은 "안정적" 으로 읽혀 거짓이 된다.
												     금액과 증감률은 두 줄로 놓는다(사용자 결정 2026-09-22). 한 줄로 두면 이 칸이 106 → 196px 로 벌어져
												     1440px 넘침이 24 → 114px 가 됐다(실측). 두 줄이면 116px · 넘침 35px 이고 행만 20px 높아진다. --%>
												<div class="text-sm">
													<span class="mr-1 text-[11px] text-base-content/60">${trendLabel}</span>
													@if(row.payoutTrendPct() != null && row.averageDividendPerShare3m() != null)
														!{String trendBasis = java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.table.cell.trend.title"), StockFormatUtil.fullKrw(row.averageDividendPerShare3m(), 2), StockFormatUtil.fullKrw(row.averageDividendPerShare1y(), 2));}
														<span class="amount-value" title="${trendBasis}">${StockFormatUtil.fullKrw(row.averageDividendPerShare3m(), 2)}</span>
														<span class="block font-medium ${row.payoutTrendPct().signum() > 0 ? "text-profit" : (row.payoutTrendPct().signum() < 0 ? "text-loss" : "text-base-content/60")}" data-payout-trend title="${trendBasis}">(${row.payoutTrendPct().signum() > 0 ? "+" : ""}${percentFormat.format(row.payoutTrendPct())}%)<span class="sr-only"> ${trendBasis}</span></span>
													@else
														<span class="text-base-content/60" data-payout-trend-missing>${trendUnknownLabel}</span>
													@endif
												</div>
											</div>
										</td>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												@if(row.averageTaxableBaseRatio1y() == null)<div><span class="text-base-content/60" data-taxable-unknown>${MessageUtil.getMessage("stock.monthly.taxable.unknown")}</span></div>@else<div>${percentFormat.format(row.averageTaxableBaseRatio1y())}%</div>@endif
												<div class="text-sm text-base-content/60"><span class="mr-1 text-[11px]">${MessageUtil.getMessage("stock.monthly.etf.table.cell.taxable.per.share")}</span>@if(row.averageTaxableBasePerShare1y() == null)<span class="text-base-content/60" data-taxable-unknown>${MessageUtil.getMessage("stock.monthly.taxable.unknown")}</span>@else<span class="amount-value">${StockFormatUtil.fullKrw(row.averageTaxableBasePerShare1y(), 2)}</span>@endif</div>
											</div>
										</td>
										<%-- 총보수(연) · 상장일(2026-09-28). 운용사가 적은 자릿수 그대로(0.09% · 0.0795%) - 두 자리로 자르면 서로 다른 보수가 같아 보인다.
										     모르면 0% 가 아니라 "미확인" 이다(0 은 보수 없음으로 읽힌다). --%>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												@if(row.totalExpenseRatioPct() != null)
													<div data-expense-ratio>${row.totalExpenseRatioPct().stripTrailingZeros().toPlainString()}%</div>
												@else
													<div class="text-base-content/60" data-expense-missing>${MessageUtil.getMessage("stock.monthly.etf.table.cell.expense.unknown")}</div>
												@endif
												@if(row.listingDate() != null)
													<div class="text-[11px] text-base-content/60" data-listing-date>${listingDateParts[0]}<span class="whitespace-nowrap date-whole">${String.valueOf(row.listingDate())}</span><wbr>${listingDateParts[1]}</div>
												@endif
											</div>
										</td>
										<%-- 시세가 없는 종목은 현재가 0 원이다. 그대로 두면 "수익률 0.00%" 가 사실처럼 읽힌다(실측 2026-09-21:
										     KODEX 200커버드콜액티브가 이력 2 건인데 0.00%). 값이 없으면 '-' 로 적고 까닭을 함께 말한다. --%>
										!{boolean priceKnown = row.currentPrice() != null && row.currentPrice().signum() > 0;}
										!{boolean priceStale = priceKnown && MonthlyEtfViewSupport.priceStale(row.currentPriceDate(), java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));}
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												@if(priceKnown)
													<div class="amount-value">${StockFormatUtil.fullKrw(row.currentPrice(), 0)}</div>
													@if(row.currentPriceDate() != null)
														<div class="text-[11px] ${priceStale ? "text-warning" : "text-base-content/60"}" data-price-date><span class="whitespace-nowrap date-whole">${row.currentPriceDate().toString()}</span>@if(priceStale) ${MessageUtil.getMessage("stock.monthly.etf.table.cell.price.stale")}@endif</div>
													@endif
												@else
													<div class="text-base-content/60" data-price-missing>${MessageUtil.getMessage("stock.monthly.etf.table.cell.price.missing")}</div>
												@endif
											</div>
										</td>
										<%-- 최근 1 년 위험(사용자 결정 2026-09-22). 합산 수익률만 보면 "어떻게 벌었는지" 를 못 본다 -
										     실측: 합산 +165% 인 종목이 그 사이 전고점에서 43.1% 빠졌다. 이력이 1 년을 못 덮으면 언제부터인지 적는다. --%>
										<td class="align-top text-right">
											@if(row.maxDrawdownPct() != null)
												<div class="font-semibold ${row.maxDrawdownPct().signum() < 0 ? "text-loss" : "text-base-content/60"}" data-max-drawdown>${percentFormat.format(row.maxDrawdownPct())}%</div>
												@if(row.riskFromDate() != null && riskPartial.test(row.riskFromDate()))
													<div class="text-[11px] text-base-content/60" data-risk-from>${riskFromParts[0]}<span class="whitespace-nowrap date-whole">${String.valueOf(row.riskFromDate())}</span><wbr>${riskFromParts[1]}</div>
												@endif
											@else
												<div class="text-base-content/60" data-risk-missing>${MessageUtil.getMessage("stock.monthly.etf.table.cell.risk.unknown")}</div>
											@endif
										</td>
										<td class="align-top text-right">
											@if(row.volatilityPct() != null)
												<div class="tabular-nums" data-volatility>${percentFormat.format(row.volatilityPct())}%</div>
											@else
												<div class="text-base-content/60">-</div>
											@endif
										</td>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												@if(priceKnown)
													<div class="font-semibold" data-annual-yield>${percentFormat.format(row.annualYieldPct() != null ? row.annualYieldPct() : BigDecimal.ZERO)}%</div>
													<div class="text-sm text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.cell.monthly.yield")} ${percentFormat.format(row.monthlyYieldPct() != null ? row.monthlyYieldPct() : BigDecimal.ZERO)}%</div>
												@else
													<%-- 수익률은 현재가로 나눈 값이라 시세가 없으면 만들 수 없다 - 0% 라고 적지 않는다. --%>
													<div class="text-base-content/60" data-yield-unknown>${MessageUtil.getMessage("stock.monthly.etf.table.cell.yield.unknown")}</div>
												@endif
											</div>
										</td>
										<%-- 점수 = 연배당 - 분배금 감소. 두 수를 따로 반올림해 빼므로 적힌 뺄셈이 그대로 맞는다.
										     못 내면 0 이 아니라 까닭(시세 없음 · 지급 6 회 미만)을 적는다 - 빈 칸이 "가장 낮은 점수" 로 읽히면 안 된다. --%>
										!{BigDecimal rowScore = MonthlyEtfViewSupport.scoreOf(row);}
										<td class="align-top text-right" data-score-cell>
											@if(rowScore != null)
												!{BigDecimal rowCut = MonthlyContributionPickSupport.cutOf(row.payoutTrendPct());}
												!{String rowYield = percentFormat.format(row.annualYieldPct().setScale(2, java.math.RoundingMode.HALF_UP));}
												<div class="font-semibold tabular-nums" data-score>${percentFormat.format(rowScore)}</div>
												@if(rowCut.signum() > 0)
													<div class="text-[11px] tabular-nums text-base-content/60" data-score-basis>${rowYield} − ${percentFormat.format(rowCut)}<span class="sr-only"> ${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.table.cell.score.basis"), rowYield, percentFormat.format(rowCut))}</span></div>
												@else
													<div class="text-[11px] text-base-content/60" data-score-no-cut>${MessageUtil.getMessage("stock.monthly.etf.table.cell.score.no.cut")}</div>
												@endif
											@elseif(!priceKnown)
												<div class="text-base-content/60" data-score-missing>${MessageUtil.getMessage("stock.monthly.etf.table.cell.yield.unknown")}</div>
											@else
												<div class="text-base-content/60" data-score-missing>${MessageUtil.getMessage("stock.monthly.etf.table.cell.score.unknown")}</div>
											@endif
										</td>
										<%-- 고른 기간의 수익률. 이력이 그 기간을 못 덮으면 값이 없다 - 그때는 언제부터 있는지 적는다. --%>
										!{boolean periodKnown = row.periodPriceReturnPct() != null && row.periodTotalReturnPct() != null;}
										<td class="align-top text-right">
											@if(periodKnown)
												<div class="font-semibold ${row.periodPriceReturnPct().signum() > 0 ? "text-profit" : (row.periodPriceReturnPct().signum() < 0 ? "text-loss" : "text-base-content/60")}" data-period-price>${percentFormat.format(row.periodPriceReturnPct())}%</div>
												@if(row.periodBaseDate() != null)
													
													<div class="text-[11px] text-base-content/60" data-period-base>${periodBaseParts[0]}<span class="whitespace-nowrap date-whole">${String.valueOf(row.periodBaseDate())}</span><wbr>${periodBaseParts[1]}</div>
												@endif
											@else
												<div class="text-base-content/60" data-period-missing>@if(row.priceHistoryStartDate() != null)${periodMissingParts[0]}<span class="whitespace-nowrap date-whole">${String.valueOf(row.priceHistoryStartDate())}</span><wbr>${periodMissingParts[1]}@else${MessageUtil.getMessage("stock.monthly.etf.table.cell.yield.unknown")}@endif</div>
											@endif
										</td>
										<td class="align-top text-right">
											@if(periodKnown)
												<div class="font-semibold ${row.periodTotalReturnPct().signum() > 0 ? "text-profit" : (row.periodTotalReturnPct().signum() < 0 ? "text-loss" : "text-base-content/60")}" data-period-total>${percentFormat.format(row.periodTotalReturnPct())}%</div>
											@else
												<div class="text-base-content/60">-</div>
											@endif
										</td>
										<td class="align-top text-right">
											<div class="space-y-1 tabular-nums">
												<div>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.monthly.etf.table.cell.payout.count"), row.payoutCount())}</div>
												@if(row.latestPayDate() != null)
													<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.cell.latest.pay.date")} <span class="whitespace-nowrap date-whole">${row.latestPayDate().toString()}</span></div>
												@endif
												@if(row.lastVerifiedDate() != null)
													<div class="text-[11px] text-base-content/60">${MessageUtil.getMessage("stock.monthly.etf.table.cell.verified")} <span class="whitespace-nowrap date-whole">${row.lastVerifiedDate().toString()}</span></div>
												@endif
												@if(row.sourceUrl() != null && !row.sourceUrl().isBlank())
													<div class="text-[11px]"><a class="link link-hover" href="${row.sourceUrl()}" target="_blank" rel="noopener noreferrer" aria-label="${row.stockItemName() + " " + MessageUtil.getMessage("stock.monthly.etf.table.cell.source") + " (" + MessageUtil.getMessage("common.label.opens.new.window") + ")"}">${MessageUtil.getMessage("stock.monthly.etf.table.cell.source")}</a></div>
												@endif
											</div>
										</td>
									</tr>
								@endfor
							</tbody>
						</table>
					</div>
				@endif
			`)

			@template._components.ui.card(bodyClass = "gap-4", body = @`
				<h2 class="card-title text-lg">${MessageUtil.getMessage("stock.monthly.etf.criteria.title")}</h2>
				<ul class="list-disc space-y-3 pl-5 text-sm leading-6 text-base-content/70">
					<li>${MessageUtil.getMessage("stock.monthly.etf.criteria.one")}</li>
					<li>${MessageUtil.getMessage("stock.monthly.etf.criteria.two")}</li>
					<li>${MessageUtil.getMessage("stock.monthly.etf.criteria.three")}</li>
				</ul>
			`)
		</div>
	`
)
