@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import java.time.ZoneId
@import java.util.List
@import java.util.Map
@import java.util.UUID
@import net.luversof.web.gate.stock.constant.TradeType
@import net.luversof.web.gate.stock.domain.Account
@import net.luversof.web.gate.stock.domain.TradeProfit
@import net.luversof.web.gate.stock.dto.response.TradeResponse
@import net.luversof.web.gate.stock.dto.response.DividendResponse
@import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint
@import java.time.format.DateTimeFormatter
@import java.time.LocalDate
@import java.time.Instant
@import net.luversof.web.gate.stock.util.RealizedBasisGap
@import net.luversof.web.gate.stock.util.ChartSeriesJs

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param boolean contentReady = false
@param boolean notFound = false
@param String accountIdParam = ""
@param Account account = null


<%-- 이 템플릿은 비 htmx 진입의 껍데기만 그린다(contentReady=false). htmx 요청은 컨트롤러가 내용 템플릿
     (stock/htmx/accountDetailContent) 을 바로 돌려준다 - 예전 @else 분기(내용 템플릿에 값 30 여 개를 넘기던 것)는 닿지 않아
     2026-10-21 에 지웠다. 표시(contentReady)는 DetailShellContractTest 가 지킨다. --%>
@template._layout.stockLayout(
	pageTitle = notFound ? MessageUtil.getMessage("stock.account.detail.notfound.title") : MessageUtil.getMessage("stock.account.detail.breadcrumb"),
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="w-full">
			@if(!contentReady)
				<%-- 셸: 콘텐츠를 전역 기간과 함께 htmx로 로드 (새로고침/이동 시에도 선택 기간 유지) --%>
				<%-- URL 의 기간(rangeMode 또는 날짜)이 조각 요청까지 가야 공유 주소가 산다. 없으면 hx-include 의
				     전역 기간(= 마지막으로 보던 기간)만 실려, 주소에 적힌 기간이 조용히 무시됐다(실측 2026-09-11:
				     ?rangeMode=ytd 로 들어가도 저장된 1개월 그대로였다). 기간 세 키를 한 묶음으로 다루는 규칙은
				     common.ts 의 data-params-from-query 훅에 있다.
				     id 는 hx-vals 로 준다 - hx-get 경로에 두면 그 훅이 URL 의 같은 키를 한 번 더 붙여 "id,id" 로
				     묶인다(실측 2026-09-11: 상세가 통째로 '찾을 수 없습니다' 가 됐다). --%>
				<div id="accountDetailFragment" data-params-from-query
				     hx-get="/stock/account"
				     hx-vals='{"accountId": "${accountIdParam}"}'
				     hx-trigger="load"
				     hx-include="#globalDateRangeInputs input"
				     hx-select="#accountDetailFragment"
				     hx-swap="outerHTML">
					<div class="flex justify-center p-10"><span class="loading loading-spinner text-primary loading-lg"></span></div>
				</div>
			@endif
		</div>
	`
)
