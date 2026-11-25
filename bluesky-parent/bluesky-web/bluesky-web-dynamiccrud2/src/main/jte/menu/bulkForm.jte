<%-- 엑셀로 여러 행 넣기(입력 작업, 작업 폼 레이어 #actionPanel 에 뜬다). 양식을 내려받아 채워 올린다.
     모든 행을 먼저 검사해 틀린 행이 하나라도 있으면 아무것도 넣지 않고 행 번호 · 사유를 보인다(MenuPageService.runBulk) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.menu.service.MenuPageService
@import net.luversof.web.dynamiccrud2.menu.service.MenuPageService.BulkForm

@param BulkForm form
@param String menuPath
@param String error
@param List<String> errors

!{var queryId = form.query().getId().toString();}

<form class="card grid gap-3" hx-post="${menuPath}/action/${queryId}/bulk" hx-encoding="multipart/form-data" hx-target="#actionPanel">
	<h2 class="font-semibold">${t("menu.bulk.title", form.query().getName())}</h2>
	@template._components.alert(message = error, cssClass = "")
	@if(!errors.isEmpty())
		<ul class="text-sm grid gap-0.5 max-h-60 overflow-auto">
			@for(var line : errors)
				<li>${line}</li>
			@endfor
		</ul>
	@endif
	<ol class="text-sm grid gap-1 list-decimal pl-5">
		<li>
			${t("menu.bulk.step.template")}
			<a class="link" href="${menuPath}/action/${queryId}/bulk-template">${t("menu.bulk.template")}</a>
			<span class="hint">(${String.join(" · ", form.headers())})</span>
		</li>
		<li>${t("menu.bulk.step.fill", MenuPageService.BULK_MAX_ROWS)}</li>
		<li>${t("menu.bulk.step.upload")}</li>
	</ol>
	@template._components.field(label = t("menu.bulk.file"), forId = "bulk-file", content = @`
		<input id="bulk-file" class="input w-full" type="file" name="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" required />
	`)
	@template._components.formActions(content = @`
		<button type="button" class="btn btn-sm btn-ghost" data-modal-close>${t("common.close")}</button>
		<button class="btn btn-sm btn-primary">${t("menu.bulk.run")}</button>
	`)
</form>
