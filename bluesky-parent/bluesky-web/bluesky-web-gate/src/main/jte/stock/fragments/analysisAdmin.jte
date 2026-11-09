@import io.github.luversof.boot.context.support.MessageUtil
@import java.time.LocalDate
@import java.time.ZoneId

<%-- 시황 분석 예측 관리(사용자 결정 2026-11-09): 기준일의 채점 전 예측만 지운다. 다시 예측은 PC 의 아침 작업이 한다 -
     예측은 PC 의 claude 가 만들고, 그때 DB 의 마지막 종가 날짜가 기준일이 되므로 지난 기준일은 다시 만들어지지 않는다. --%>
!{String today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();}
@template._components.ui.card(bodyClass = "p-4 space-y-3", labelledBy = "analysis-admin-title", body = @`
		<h2 id="analysis-admin-title" class="card-title">${MessageUtil.getMessage("stock.admin.analysis.title")}</h2>
		<p class="text-sm text-base-content/70">${MessageUtil.getMessage("stock.admin.analysis.description")}</p>
		<form class="flex flex-wrap items-end gap-3"
		      hx-delete="/api/stock/admin/analysis/predictions"
		      hx-confirm="${MessageUtil.getMessage("stock.admin.analysis.confirm")}"
		      hx-target="#analysis-delete-result"
		      hx-swap="innerHTML">
			<label class="form-control">
				<span class="label-text text-sm">${MessageUtil.getMessage("stock.admin.analysis.baseDate")}</span>
				<input name="baseDate" type="date" required value="${today}" class="input input-bordered input-sm" />
			</label>
			<button type="submit" class="btn btn-outline btn-error btn-sm">${MessageUtil.getMessage("stock.admin.analysis.delete")}</button>
		</form>
		<p id="analysis-delete-result" class="text-sm" role="status" aria-live="polite"></p>
		<p class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.admin.analysis.rerun")}
			<code class="font-mono">schtasks /run /tn "\bluesky\market-analysis-morning"</code></p>
	`)
