@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.util.List
@import net.luversof.web.gate.stock.dto.response.MonthlyDividendSnapshotResponse

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String dividendTab = "history"
@param List<MonthlyDividendSnapshotResponse> midRows = java.util.List.of()
@param List<MonthlyDividendSnapshotResponse> endRows = java.util.List.of()
@param List<MonthlyDividendSnapshotResponse> otherRows = java.util.List.of()
@param BigDecimal midTotal = java.math.BigDecimal.ZERO
@param BigDecimal endTotal = java.math.BigDecimal.ZERO
@param BigDecimal otherTotal = java.math.BigDecimal.ZERO
@param BigDecimal monthlyTotal = java.math.BigDecimal.ZERO
@param BigDecimal annualTotal = java.math.BigDecimal.ZERO

!{
    boolean isCalendar = "calendar".equals(dividendTab);
    String activeTabClass = "tab tab-active flex-1 font-bold !bg-primary !text-primary-content shadow-sm";
    String inactiveTabClass = "tab flex-1 text-base-content/60 hover:text-base-content";

    String midLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.mid.month");
    String endLabel = MessageUtil.getMessage("stock.page.dividend.monthly.reference.profile.payout.window.month.end");
    String otherLabel = MessageUtil.getMessage("stock.dividend.calendar.window.other");
    String monthlyTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.monthly.total");
    String annualTotalLabel = MessageUtil.getMessage("stock.dividend.calendar.annual.total");
    String calendarDesc = MessageUtil.getMessage("stock.dividend.calendar.desc");
    String emptyLabel = MessageUtil.getMessage("stock.dividend.calendar.empty");
    boolean calendarEmpty = (midRows == null || midRows.isEmpty())
        && (endRows == null || endRows.isEmpty())
        && (otherRows == null || otherRows.isEmpty());
}

@template._layout.stockLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-6">
			@template._components.ui.pageHeader(title = MessageUtil.getMessage("stock.page.dividend.title"))

			<!-- 같은 페이지 탭: 클릭 시 이 페이지에서 내용만 교체된다 -->
			<div role="tablist" class="tabs tabs-box w-full bg-base-200/70 p-1">
				<a role="tab" href="/stock/dividend?tab=history" class="${!isCalendar ? activeTabClass : inactiveTabClass}">${MessageUtil.getMessage("stock.page.dividend.tab.history")}</a>
				<a role="tab" href="/stock/dividend?tab=calendar" class="${isCalendar ? activeTabClass : inactiveTabClass}">${MessageUtil.getMessage("stock.page.dividend.tab.calendar")}</a>
			</div>

			<!-- 월배당 도구: 별도 페이지로 이동(링크). 탭과 구분되도록 버튼 형태로 표기 -->
			<div class="flex flex-wrap items-center gap-2 text-sm">
				<span class="text-base-content/50">${MessageUtil.getMessage("stock.page.dividend.monthly.tools")}</span>
				<a href="/stock/admin?tab=monthly-reference" class="btn btn-sm btn-outline gap-1">
					${MessageUtil.getMessage("stock.page.dividend.tab.monthly.reference")}
					<svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
				</a>
				<a href="/stock/simulator?tab=monthly-dividend" class="btn btn-sm btn-outline gap-1">
					${MessageUtil.getMessage("stock.page.dividend.tab.simulator")}
					<svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
				</a>
			</div>

			@if(isCalendar)
				<p class="text-sm text-base-content/60">${calendarDesc}</p>
				@if(calendarEmpty)
					<div class="bg-base-200 rounded-box p-10 text-center text-base-content/60">${emptyLabel}</div>
				@else
					<div class="grid grid-cols-2 gap-3">
						@template._components.ui.statCard(
							label = monthlyTotalLabel,
							value = String.format("%,d", monthlyTotal != null ? monthlyTotal.longValue() : 0),
							valueTitle = String.format("%,d원", monthlyTotal != null ? monthlyTotal.longValue() : 0),
							tone = "text-dividend")
						@template._components.ui.statCard(
							label = annualTotalLabel,
							value = String.format("%,d", annualTotal != null ? annualTotal.longValue() : 0),
							valueTitle = String.format("%,d원", annualTotal != null ? annualTotal.longValue() : 0),
							tone = "text-dividend")
					</div>
					<div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
						@template.stock.fragments.dividendCalendarGroup(title = midLabel, rows = midRows, subtotal = midTotal)
						@template.stock.fragments.dividendCalendarGroup(title = endLabel, rows = endRows, subtotal = endTotal)
					</div>
					@if(otherRows != null && !otherRows.isEmpty())
						<div class="grid grid-cols-1">
							@template.stock.fragments.dividendCalendarGroup(title = otherLabel, rows = otherRows, subtotal = otherTotal)
						</div>
					@endif
				@endif
			@else
				<div class="min-h-[200px]" hx-get="/stock/htmx/dividend/list" hx-trigger="load" hx-target="this" hx-swap="outerHTML" hx-include="#globalDateRangeInputs input">
					<div class="flex items-center gap-2 text-base-content/60">
						<span class="loading loading-spinner loading-sm"></span>
						<span>${MessageUtil.getMessage("common.message.loading")}</span>
					</div>
				</div>
			@endif
		</div>
	`
)
