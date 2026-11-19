<%-- 설정 변경 이력(SettingLog) 찾기 + 목록. 메뉴 변경 이력의 "설정 변경" 탭과 프로젝트 설정 변경 이력이 같이 쓴다.
     바뀐 항목만 "이전 → 이후"(만들기는 이후만, 지우기는 이전만 줄 그어) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.domain.SettingLog
@import net.luversof.web.dynamiccrud2.admin.service.SettingLogService.Filter
@import net.luversof.web.dynamiccrud2.admin.service.SettingLogService.HistoryPage

@param Filter filter
@param HistoryPage settings
@param String action
@param String pageUrl
@param boolean settingsTab = false

@template._components.card(title = t("admin.history.filter"), help = t("admin.history.settings.help"), body = @`
	<form method="get" action="${action}" role="search"
		class="grid gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_10rem_10rem_auto] items-end">
		@if(settingsTab)<input type="hidden" name="tab" value="settings" />@endif
		@template._components.field(label = t("admin.history.user"), forId = "s-user-input", content = @`
			@template.admin.userPicker(id = "s-user", usernames = settings.usernames(), selected = filter.username())
		`)
		@template._components.field(label = t("admin.history.target"), forId = "s-target", content = @`
			<select id="s-target" class="select w-full" name="target">
				<option value="">${t("admin.history.all")}</option>
				@for(var target : SettingLog.Target.values())
					<option value="${target.name()}" selected="${target.name().equals(filter.target())}">${t("setting.target." + target.name())}</option>
				@endfor
			</select>
		`)
		@template._components.field(label = t("admin.history.from"), forId = "s-from", content = @`
			<input id="s-from" class="input w-full" type="date" name="from" value="${filter.from() == null ? null : filter.from().toString()}" />
		`)
		@template._components.field(label = t("admin.history.to"), forId = "s-to", content = @`
			<input id="s-to" class="input w-full" type="date" name="to" value="${filter.to() == null ? null : filter.to().toString()}" />
		`)
		<div class="flex gap-2">
			<button class="btn btn-primary">${t("menu.page.search")}</button>
			<a class="btn btn-ghost" href="${action}${settingsTab ? "?tab=settings" : ""}">${t("menu.page.reset")}</a>
		</div>
	</form>
`)

@template._components.card(title = t("admin.history.list", settings.total()), body = @`
	@if(settings.rows().isEmpty())
		@template._components.emptyState(message = t("admin.history.empty"))
	@else
		<div class="overflow-x-auto">
			<table class="table" aria-label="${t("admin.history.settingsTitle")}">
				<thead>
					<tr><th>${t("admin.history.col.time")}</th><th>${t("admin.history.user")}</th><th>${t("admin.history.target")}</th><th>${t("admin.history.col.action")}</th><th>${t("admin.history.col.changes")}</th></tr>
				</thead>
				<tbody>
					@for(var row : settings.rows())
						<tr>
							<td class="font-mono text-xs whitespace-nowrap">${row.time()}</td>
							<td class="whitespace-nowrap">${row.username()}</td>
							<td>
								<span class="badge">${t("setting.target." + row.target().name())}</span>
								@if(row.targetName() != null)<span class="ml-1">${row.targetName()}</span>@endif
							</td>
							<td class="whitespace-nowrap">${t("setting.action." + row.action().name())}</td>
							<td class="text-xs">
								@for(var change : row.changes())
									<div class="py-0.5">
										<span class="hint">${change.label()}</span>
										@if(change.before() != null && change.after() != null)
											<del class="diff-old break-all"><span class="sr-only">${t("admin.history.before")} </span>${change.before()}</del>
											<span aria-hidden="true">→</span>
											<ins class="diff-new break-all"><span class="sr-only">${t("admin.history.after")} </span>${change.after()}</ins>
										@elseif(change.after() != null)
											<ins class="diff-new break-all"><span class="sr-only">${t("admin.history.after")} </span>${change.after()}</ins>
										@elseif(change.before() != null)
											<del class="diff-old break-all"><span class="sr-only">${t("admin.history.deleted")} </span>${change.before()}</del>
										@else
											<span class="hint">-</span>
										@endif
									</div>
								@endfor
							</td>
						</tr>
					@endfor
				</tbody>
			</table>
		</div>
		@template._components.pagination(current = settings.page(), lastPage = settings.lastPage(), pageUrl = pageUrl)
	@endif
`)
