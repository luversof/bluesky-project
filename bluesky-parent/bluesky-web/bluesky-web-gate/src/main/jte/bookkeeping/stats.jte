@import net.luversof.web.gate.util.CspNonceHolder
@import net.luversof.web.gate.bookkeeping.constant.CategoryKind
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.support.StatsView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingStatsViewController.StatsPage
@param StatsPage page
@param boolean isAuthenticated = false
@param String username = ""
@param String profile = "local"

<%-- 통계: 분류별 비중(표가 기본 경로 - 줄을 누르면 소분류로, 차트는 같은 수치를 그림으로)과 12개월 추이. 한 통화씩, 환산 없음.
     차트 데이터는 canvas 의 data-bk-chart(JSON)로 싣고 아래 nonce 스크립트가 그린다. --%>
!{ var book = page.book(); }
!{ var month = page.range().month(); }
!{ String title = BookkeepingView.msg("bookkeeping.title.stats"); }
!{ String base = "/bookkeeping/" + book.id() + "/stats"; }
!{ String kindLabel = BookkeepingView.msg("bookkeeping.kind." + page.kind().name()); }
!{ String breakdownTitle = BookkeepingView.msg("bookkeeping.stats.breakdown", kindLabel) + (page.parent() == null ? "" : " · " + page.parent().name()); }
!{ String trendTitle = BookkeepingView.msg("bookkeeping.stats.trend"); }
!{ var total = StatsView.sum(page.slices()); }
@template._layout.bookkeepingLayout(
	title = book.name() + " " + title,
	bookId = book.id(),
	active = "stats",
	isAuthenticated = isAuthenticated,
	username = username,
	profile = profile,
	content = @`
		@template._components.ui.pageHeader(
			title = book.name(),
			description = title + " · " + BookkeepingView.monthLabel(month) + " (" + BookkeepingView.rangeLabel(page.range()) + ")",
			actions = @`
				<div class="flex flex-wrap items-center gap-2">
					<a href="${base}?month=${month.minusMonths(1).toString()}&kind=${page.kind().name()}&currency=${page.currency()}" class="btn btn-sm btn-outline">${BookkeepingView.msg("bookkeeping.month.prev")}</a>
					<a href="${base}?month=${month.plusMonths(1).toString()}&kind=${page.kind().name()}&currency=${page.currency()}" class="btn btn-sm btn-outline">${BookkeepingView.msg("bookkeeping.month.next")}</a>
				</div>
			`
		)

		<div class="flex flex-wrap items-end gap-3 mb-4">
			<div class="join" role="group" aria-label="${BookkeepingView.msg("bookkeeping.field.type")}">
				@for(var k : CategoryKind.values())
					<a href="${base}?month=${month.toString()}&kind=${k.name()}&currency=${page.currency()}" class="btn btn-sm join-item ${k == page.kind() ? "btn-primary" : ""}" aria-current="${k == page.kind() ? "true" : null}">${BookkeepingView.msg("bookkeeping.kind." + k.name())}</a>
				@endfor
			</div>
			@if(page.currencies().size() > 1)
				<form method="get" action="${base}" class="flex items-end gap-2">
					<input type="hidden" name="month" value="${month.toString()}" />
					<input type="hidden" name="kind" value="${page.kind().name()}" />
					<label class="form-control">
						<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.asset.currency")}</span>
						<select name="currency" class="select select-sm select-bordered">
							@for(var c : page.currencies())
								<option value="${c}" selected="${c.equals(page.currency())}">${c}</option>
							@endfor
							<option value="FX" selected="${page.converted()}">${BookkeepingView.msg("bookkeeping.stats.converted", page.book().baseCurrency())}</option>
						</select>
					</label>
					<button type="submit" class="btn btn-sm">${BookkeepingView.msg("bookkeeping.stats.apply")}</button>
				</form>
			@endif
		</div>

		<section aria-labelledby="bk-stats-breakdown" class="mb-6">
			<h2 id="bk-stats-breakdown" class="text-base font-semibold mb-2">${breakdownTitle}</h2>
			@if(page.fxMissing() != null && !page.fxMissing().isEmpty())
				<p role="status" class="mb-2 text-xs"><span class="badge badge-warning badge-sm">${BookkeepingView.msg("bookkeeping.stats.fxMissing", String.join(", ", page.fxMissing()))}</span></p>
			@endif
			@if(page.parent() != null)
				<a href="${base}?month=${month.toString()}&kind=${page.kind().name()}&currency=${page.currency()}" class="btn btn-xs btn-ghost mb-2">${BookkeepingView.msg("bookkeeping.stats.back")}</a>
			@endif
			@if(page.slices().isEmpty())
				@template._components.ui.emptyState(message = BookkeepingView.msg("bookkeeping.stats.empty"), boxed = true)
			@else
				<div class="grid gap-4 md:grid-cols-2 items-start">
					<div class="mx-auto w-full max-w-xs">
						<canvas data-bk-chart="doughnut" data-bk-data="${StatsView.breakdownJson(page.slices())}" role="img" aria-label="${breakdownTitle}"></canvas>
					</div>
					<table class="table table-sm" aria-label="${breakdownTitle}">
						<thead>
							<tr>
								<th scope="col">${BookkeepingView.msg("bookkeeping.field.category")}</th>
								<th scope="col" class="text-right">${BookkeepingView.msg("bookkeeping.field.amount")}</th>
								<th scope="col" class="text-right">${BookkeepingView.msg("bookkeeping.stats.share")}</th>
								@if(page.budgets() != null)
									<th scope="col" class="text-right">${BookkeepingView.msg("bookkeeping.budget.title")}</th>
								@endif
							</tr>
						</thead>
						<tbody>
							@for(var s : page.slices())
								!{ String name = s.name(); }
								<tr>
									<td class="wrap-anywhere">
										@if(s.drillParentId() != null)
											<a class="link" href="${base}?month=${month.toString()}&kind=${page.kind().name()}&currency=${page.currency()}&parent=${s.drillParentId().toString()}">${name}</a>
										@else
											${name}
										@endif
									</td>
									<td class="text-right amount-value">${BookkeepingView.amount(s.amount(), page.displayCurrency())}</td>
									<td class="text-right">${s.percent().toPlainString()}%</td>
									@if(page.budgets() != null)
										!{ var b = page.budgets().of(s.categoryId()); }
										<td class="text-right amount-value">${b == null ? "-" : BookkeepingView.amount(b, page.displayCurrency())}</td>
									@endif
								</tr>
							@endfor
						</tbody>
						<tfoot>
							<tr>
								<th scope="row">${BookkeepingView.msg("bookkeeping.stats.total")}</th>
								<td class="text-right font-semibold amount-value">${BookkeepingView.amount(total, page.displayCurrency())}</td>
								<td></td>
								@if(page.budgets() != null)
									<td class="text-right amount-value">${page.budgets().total() == null ? "-" : BookkeepingView.amount(page.budgets().total(), page.displayCurrency())}</td>
								@endif
							</tr>
						</tfoot>
					</table>
				</div>
			@endif
		</section>

		<section aria-labelledby="bk-stats-trend" class="mb-6">
			<h2 id="bk-stats-trend" class="text-base font-semibold mb-2">${trendTitle} (${page.converted() ? BookkeepingView.msg("bookkeeping.stats.converted", page.book().baseCurrency()) : page.currency()})</h2>
			@if(page.trend().isEmpty())
				@template._components.ui.emptyState(message = BookkeepingView.msg("bookkeeping.stats.empty"), boxed = true)
			@else
				<div class="w-full overflow-x-auto">
					<div class="min-w-[20rem] h-64">
						<canvas data-bk-chart="bar" data-bk-data="${StatsView.trendJson(page.trend(), BookkeepingView.msg("bookkeeping.summary.income"), BookkeepingView.msg("bookkeeping.summary.expense"))}" role="img" aria-label="${trendTitle}"></canvas>
					</div>
				</div>
			@endif
		</section>

		<script src="/js/vendor/chart.umd.min.js"></script>
		<%-- 차트 테마 색 · 애니메이션 기본값을 다른 화면과 맞춘다(ChartSupportScriptTest). --%>
		<script src="/js/stock-charts.js"></script>
		<script nonce="${CspNonceHolder.getNonce()}">
			(function () {
				var palette = ["#3763d0", "#e0803a", "#2a9d8f", "#c0457a", "#8a6bd1", "#b49a2e", "#4f8fbf", "#a35d3d", "#5c9e5a", "#7f7f8f"];
				var text = getComputedStyle(document.body).color;
				document.querySelectorAll("canvas[data-bk-chart]").forEach(function (canvas) {
					var data = JSON.parse(canvas.getAttribute("data-bk-data"));
					if (canvas.getAttribute("data-bk-chart") === "doughnut") {
						new Chart(canvas, {
							type: "doughnut",
							data: { labels: data.labels, datasets: [{ data: data.values, backgroundColor: data.labels.map(function (_, i) { return palette[i % palette.length]; }) }] },
							options: { plugins: { legend: { position: "bottom", labels: { color: text } } } }
						});
					} else {
						new Chart(canvas, {
							type: "bar",
							data: { labels: data.labels, datasets: data.series.map(function (s, i) { return { label: s.label, data: s.values, backgroundColor: palette[i] }; }) },
							options: { maintainAspectRatio: false, plugins: { legend: { labels: { color: text } } }, scales: { x: { ticks: { color: text } }, y: { ticks: { color: text } } } }
						});
					}
				});
			})();
		</script>
	`
)
