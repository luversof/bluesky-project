@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param int gemCount = 0
@param int uniqueCount = 0
@param int baseItemCount = 0
@param boolean hasTreeData = false

!{
	String pageTitle = MessageUtil.getMessage("poe.admin.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String dataStatusLabel = MessageUtil.getMessage("poe.admin.data.status");
	String gemsLabel = MessageUtil.getMessage("poe.tab.gems");
	String uniquesLabel = MessageUtil.getMessage("poe.tab.uniques");
	String treeLabel = MessageUtil.getMessage("poe.tab.tree");
	String treeReadyLabel = MessageUtil.getMessage("poe.admin.tree.ready");
	String treeMissingLabel = MessageUtil.getMessage("poe.gems.empty");
	String countUnit = MessageUtil.getMessage("common.count");
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
				<span class="badge badge-ghost">${patchLabel}</span>
			</div>


			<!-- 현재 데이터 상태 -->
			<div class="grid grid-cols-1 sm:grid-cols-4 gap-3">
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/50">${gemsLabel}</div>
						<div class="text-xl font-bold font-mono">${java.text.MessageFormat.format(countUnit, gemCount)}</div>
				`)
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/50">${uniquesLabel}</div>
						<div class="text-xl font-bold font-mono">${java.text.MessageFormat.format(countUnit, uniqueCount)}</div>
				`)
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("poe.tab.items")}</div>
						<div class="text-xl font-bold font-mono">${java.text.MessageFormat.format(countUnit, baseItemCount)}</div>
				`)
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/50">${treeLabel}</div>
						<div class="text-xl font-bold">${hasTreeData ? treeReadyLabel : treeMissingLabel}</div>
				`)
			</div>

			<!-- 추출 파이프라인 실행/상태 (fragment 가 스스로 폴링) -->
			<div id="poeExtractStatus" hx-get="/poe/htmx/admin/status" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-6">
					<span class="loading loading-spinner loading-md text-primary"></span>
				</div>
			</div>
		</div>
	`
)
