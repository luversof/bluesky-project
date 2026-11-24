<%-- 변경 이력(메뉴 관리 → "변경 이력"): 사용 화면에서 실행한 추가 · 수정 · 삭제를 최근 순으로. 사용자 · 작업(쿼리) · 기간으로 거른다(GET, 주소에 남아
     쪽 넘김 · 새로 고침에도 유지). 이력에는 넣은 값만 있고 바뀌기 전 값은 없다(ChangeLogService) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.admin.controller.Crumb
@import net.luversof.web.dynamiccrud2.domain.Menu
@import net.luversof.web.dynamiccrud2.domain.QueryKind
@import net.luversof.web.dynamiccrud2.admin.service.ChangeLogService.Filter
@import net.luversof.web.dynamiccrud2.admin.service.ChangeLogService.HistoryPage

@param Menu menu
@param Filter filter = null
@param HistoryPage history = null
@param boolean settingsTab = false
@param net.luversof.web.dynamiccrud2.admin.service.SettingLogService.Filter settingFilter = null
@param net.luversof.web.dynamiccrud2.admin.service.SettingLogService.HistoryPage settings = null
@param String pageUrl
@param java.util.List<net.luversof.web.dynamiccrud2.admin.controller.Crumb> breadcrumb = null
@param String targetNoun = net.luversof.web.dynamiccrud2.config.Msg.t("target.table")

!{var menuId = menu.getId().toString();}

@template._layout.layout(title = t("admin.history.titleOf", menu.getName()), breadcrumb = breadcrumb, content = @`
	<%-- 메뉴 설정 변경(관리 화면) · 테이블 데이터 변경(사용 화면 실행) 은 따로 저장 · 조회한다. 이름은 누가 아니라 무엇을 바꿨는지로(사용자 결정).
	     메뉴 관리에서 오므로 설정 변경이 왼쪽 · 기본(tab 없음), 데이터 변경은 tab=data(사용자 결정 11-24) --%>
	@template._components.tabNav(label = t("admin.history.title"), current = settingsTab ? 0 : 1, cssClass = "mb-2", items = List.of(
		new Crumb(t("admin.history.tab.settings"), "/admin/menu/" + menuId + "/history"),
		new Crumb(t("admin.history.tab.data", targetNoun), "/admin/menu/" + menuId + "/history?tab=data")))
	<p class="hint mb-4">${settingsTab ? t("admin.history.tab.settings.desc") : t("admin.history.tab.data.desc")}</p>
	@if(settingsTab)
		@template.admin.settingHistory(filter = settingFilter, settings = settings, action = "/admin/menu/" + menuId + "/history", pageUrl = pageUrl, settingsTab = true)
	@else
	@template._components.card(title = t("admin.history.filter"), help = t("admin.history.help"), body = @`
		<form method="get" action="/admin/menu/${menuId}/history" role="search"
			class="grid gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_10rem_10rem_auto] items-end">
			<input type="hidden" name="tab" value="data" />
			@template._components.field(label = t("admin.history.user"), forId = "h-user-input", content = @`
				@template.admin.userPicker(id = "h-user", usernames = history.usernames(), selected = filter.username())
			`)
			@template._components.field(label = t("admin.history.query"), forId = "h-query", content = @`
				<select id="h-query" class="select w-full" name="query">
					<option value="">${t("admin.history.all")}</option>
					@for(var query : menu.getQueries())
						@if(query.getKind() != QueryKind.READ)
							<option value="${query.getId().toString()}" selected="${query.getId().equals(filter.queryId())}">${query.getName()} (${query.getKind().name()})</option>
						@endif
					@endfor
				</select>
			`)
			@template._components.field(label = t("admin.history.from"), forId = "h-from", content = @`
				<input id="h-from" class="input w-full" type="date" name="from" value="${filter.from() == null ? null : filter.from().toString()}" />
			`)
			@template._components.field(label = t("admin.history.to"), forId = "h-to", content = @`
				<input id="h-to" class="input w-full" type="date" name="to" value="${filter.to() == null ? null : filter.to().toString()}" />
			`)
			<div class="flex gap-2">
				<button class="btn btn-primary">${t("menu.page.search")}</button>
				<a class="btn btn-ghost" href="/admin/menu/${menuId}/history?tab=data">${t("menu.page.reset")}</a>
			</div>
		</form>
	`)

	@template._components.card(title = t("admin.history.list", history.total()), body = @`
		@if(history.rows().isEmpty())
			@template._components.emptyState(message = t("admin.history.empty"))
		@else
			<div class="relative overflow-x-auto">
				<table class="table" aria-label="${t("admin.history.title")}">
					<thead>
						<tr><th>${t("admin.history.col.time")}</th><th>${t("admin.history.user")}</th><th>${t("admin.history.query")}</th><th class="text-right">${t("admin.history.col.affected")}</th><th>${t("admin.history.col.values")}</th></tr>
					</thead>
					<tbody>
						@for(var row : history.rows())
							<tr>
								<td class="font-mono text-xs whitespace-nowrap">${row.time()}</td>
								<td class="whitespace-nowrap">${row.username()}</td>
								<td class="whitespace-nowrap">
									@if(row.kind() != null)<span class="badge ${row.kind() == QueryKind.DELETE ? "badge-warning" : ""}">${row.kind().name()}</span>@endif
									${row.queryName()}
								</td>
								<td class="text-right tabular-nums">${row.affectedRows()}</td>
								<td class="text-xs">
									<%-- 수정: 바뀐 칸은 이전값(지움) → 새값, 안 바뀐 칸은 값만 흐리게. 삭제: 지운 행 전체(지움 표시). 입력: 넣은 값 --%>
									@for(var values : row.rows())
										<div class="py-0.5">
											@for(var value : values)
												<span class="mr-2 inline-block">
													<span class="hint">${value.label()}</span>
													@if(value.changed())
														<del class="diff-old break-all"><span class="sr-only">${t("admin.history.before")} </span>${value.before()}</del>
														<span aria-hidden="true">→</span>
														<ins class="diff-new break-all"><span class="sr-only">${t("admin.history.after")} </span>${value.after()}</ins>
													@elseif(value.after() == null)
														<del class="diff-old break-all"><span class="sr-only">${t("admin.history.deleted")} </span>${value.before()}</del>
													@elseif(value.before() != null)
														<span class="hint break-all">${value.after()}</span>
													@else
														<span class="break-all">${value.after()}</span>
													@endif
												</span>
											@endfor
										</div>
									@endfor
								</td>
							</tr>
						@endfor
					</tbody>
				</table>
			</div>
			@template._components.pagination(current = history.page(), lastPage = history.lastPage(), pageUrl = pageUrl)
		@endif
	`)
	@endif
`)
