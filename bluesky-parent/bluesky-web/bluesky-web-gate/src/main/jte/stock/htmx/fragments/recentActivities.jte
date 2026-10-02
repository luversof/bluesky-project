@import java.util.List
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.controller.StockTradeHtmxController.Activity
@import java.time.ZoneId
@import java.time.format.DateTimeFormatter
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil

@param List<Activity> activities
@param String thisMonthLabel
@param long buyCount
@param long sellCount
@param BigDecimal buyAmount
@param BigDecimal sellAmount
@param long dividendCount
@param BigDecimal dividendAmount

!{
	java.util.function.Function<Object, String> countMessage = value -> java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), value);
	String recentActivityTitle = MessageUtil.getMessage("stock.recent.activity.title");
	String viewAllLabel = MessageUtil.getMessage("common.button.view.all");
	String noActivityLabel = MessageUtil.getMessage("stock.activity.message.no.data");
	String dividendLabel = MessageUtil.getMessage("stock.activity.label.dividend");
	String buyLabel = MessageUtil.getMessage("stock.trade.label.buy");
	String sellLabel = MessageUtil.getMessage("stock.trade.label.sell");
	String sharesPattern = MessageUtil.getMessage("stock.activity.label.shares");
	// The unit hugs the number in Korean ("111주") but needs a space in English ("111 shares"),
	// so the spacing lives in the message and the template splits it at the placeholder.
	String sharesBefore = net.luversof.web.gate.stock.util.StockMessageSplitUtil.beforePlaceholder(sharesPattern);
	String sharesAfter = net.luversof.web.gate.stock.util.StockMessageSplitUtil.afterPlaceholder(sharesPattern);
	// 1 주일 때의 뒤 꼬리(영어 " share") - 2026-10-01 영어 점검에서 "1 shares". 한국어는 같은 "주".
	String sharesAfterOne = net.luversof.web.gate.stock.util.StockMessageSplitUtil.afterPlaceholder(MessageUtil.getMessage("stock.activity.label.shares.one"));
	String monthSummaryLabel = java.text.MessageFormat.format(MessageUtil.getMessage("stock.recent.activity.month.summary"), thisMonthLabel);
}

@template._components.ui.card(cssClass = "h-full", body = @`
	<div class="flex justify-between items-center mb-3">
		<h2 class="card-title text-xl">${recentActivityTitle}</h2>
		<a href="/stock/activity" class="link link-hover text-xs">${viewAllLabel} <span aria-hidden="true">→</span></a>
	</div>
	<%-- 자리가 모자라면 요약이 아래 줄로 내려간다. 예전에는 요약이 폭을 고수해 왼쪽 목록이
	     94px 까지 눌리고 종목명이 0px 로 사라졌다(실측 2026-09-15, 320px).
	     2026-10-01: 기준을 카드 폭(480px, @container)으로 - 1440px 대시보드 카드(약 385px)에서 요약 열(144px)이 옆에 남아
	     종목명 자리가 110px 뿐이라 "TIGER 배…" 처럼 같은 운용사 종목끼리 구별이 안 됐다. 좁으면 요약을 목록 아래에 놓는다. --%>
	<div class="@container">
	<div class="flex flex-col gap-4 @[30rem]:flex-row">
		<%-- 좌: 활동 목록 --%>
		<div class="flex-1 min-w-0">
			@if(activities == null || activities.isEmpty())
				<div class="text-center text-base-content/60 py-4">${noActivityLabel}</div>
			@else
				<ul class="space-y-0">
					@for(Activity activity : activities)
						!{
							String badgeClass = "badge-neutral";
							String badgeText = "";
							// 매수·매도 금액은 거래 금액이지 손익이 아니다. 이 카드만 매도 금액을 text-info(rgb 32,98,171) 로,
							// 매수 금액을 text-base-content/60 으로 칠하고 있었다 - 파랑은 손실 색(text-loss rgb 49,89,196) 과
							// 같은 계열로 읽히고, 같은 카드 안에서 매수만 흐려 위계도 어긋난다. 실측 2026-09-11.
							// 활동 목록 뷰와 매매 상세 목록은 둘 다 거래 금액을 기본색으로 두고 배당만 색을 준다 - 그 규칙에 맞춘다.
							String amountColor = "text-base-content";

							if ("DIVIDEND".equals(activity.type())) {
								badgeClass = "badge-success badge-outline";
								badgeText = dividendLabel;
								amountColor = "text-dividend";
							} else if ("TRADE".equals(activity.type())) {
								if ("BUY".equals(activity.tradeType())) {
									badgeClass = "badge-error badge-outline";
									badgeText = buyLabel;
								} else {
									badgeClass = "badge-info badge-outline";
									badgeText = sellLabel;
								}
							}
						}
						<li class="flex items-start gap-2 py-1.5 border-b border-base-200 last:border-0">
							<div class="flex-1 min-w-0">
								<%-- 종목명은 두 줄까지(2026-10-01 자율 점검). 한 줄 잘림이면 1440px 에서 "TIGER 배…" · "TIGER 코리…" 처럼 같은 운용사 종목끼리
								     구별이 안 됐다. 한국어 종목명은 빈칸 없는 긴 낱말이라 낱말 안에서도 줄을 바꾼다. --%>
								<div class="flex items-start gap-1.5 mb-0.5">
									<span class="badge badge-xs ${badgeClass} mt-0.5 shrink-0">${badgeText}</span>
									<a href="${StockDetailLinkUtil.item(activity.stockItemId() != null ? activity.stockItemId().toString() : "")}" class="font-bold text-sm line-clamp-2 wrap-anywhere link link-hover" title="${activity.stockItemName()}">${activity.stockItemName()}</a>
								</div>
								<%-- 이 카드는 계좌를 아예 보여주지 않으므로(폭이 없다) 묶인 줄은 건수로만 드러난다.
								     그래서 accountCount 는 0 이다 - 배지 규칙은 "화면이 보여 주는 것으로 설명되지 않으면 건수를 적는다".
								     실측 2026-09-11: 이번 달 활동 7줄 중 3줄이 계좌 3개를 묶은 줄인데 이 카드엔 아무 표시가 없었다.
								     배지는 종목명 줄이 아니라 이 줄에 둔다 - 이름 줄에 두었더니 375px 에서 이름 폭이 48px -> 11px 로
								     줄어 한 글자도 못 읽었다(실측 2026-09-11, 이름 전체는 196px). 이 줄은 "42주" 뿐이라 자리가 있다. --%>
								<div class="text-xs text-base-content/60 flex items-center gap-1">
									<span>
										@if("TRADE".equals(activity.type()))
											${sharesBefore}<span class="amount-value">${activity.quantity()}</span>${Integer.valueOf(1).equals(activity.quantity()) ? sharesAfterOne : sharesAfter}
										@else
											${activity.description()}
										@endif
									</span>
									@template.stock.htmx.fragments.components.mergedRecordBadge(recordCount = activity.recordCount(), accountCount = 0)
								</div>
							</div>
							<div class="text-right shrink-0">
								<div class="font-mono font-bold text-xs amount-value ${amountColor}">
									₩${String.format("%,d", StockFormatUtil.displayWon(activity.amount()))}
								</div>
								<div class="text-xs text-base-content/60 mt-0.5">
									${activity.date() != null ? activity.date().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd")) : ""}
								</div>
							</div>
						</li>
					@endfor
				</ul>
			@endif
		</div>
		<%-- 우: 이번 달 요약 --%>
		<div class="border-t border-base-200 pt-3 @[30rem]:w-36 @[30rem]:shrink-0 @[30rem]:border-t-0 @[30rem]:border-l @[30rem]:pt-0 @[30rem]:pl-3" data-month-summary>
			<div class="text-xs font-semibold text-base-content/60 mb-2">${monthSummaryLabel}</div>
			<div class="grid grid-cols-3 gap-3 @[30rem]:block @[30rem]:space-y-2" data-month-summary-rows>
				<div class="flex justify-between items-center">
					<span class="badge badge-xs badge-error badge-outline">${buyLabel}</span>
					<div class="text-right">
						<div class="text-xs text-base-content/60">${countMessage.apply(buyCount)}</div>
						<div class="font-mono text-xs font-bold amount-value text-base-content">
							₩${String.format("%,d", StockFormatUtil.displayWon(buyAmount))}
						</div>
					</div>
				</div>
				<div class="flex justify-between items-center">
					<span class="badge badge-xs badge-info badge-outline">${sellLabel}</span>
					<div class="text-right">
						<div class="text-xs text-base-content/60">${countMessage.apply(sellCount)}</div>
						<div class="font-mono text-xs font-bold amount-value text-base-content">
							₩${String.format("%,d", StockFormatUtil.displayWon(sellAmount))}
						</div>
					</div>
				</div>
				<div class="flex justify-between items-center">
					<span class="badge badge-xs badge-success badge-outline">${dividendLabel}</span>
					<div class="text-right">
						<div class="text-xs text-base-content/60">${countMessage.apply(dividendCount)}</div>
						<div class="font-mono text-xs font-bold amount-value text-dividend">
							₩${String.format("%,d", StockFormatUtil.displayWon(dividendAmount))}
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	</div>
`)