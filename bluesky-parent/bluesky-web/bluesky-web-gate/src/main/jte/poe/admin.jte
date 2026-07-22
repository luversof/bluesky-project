@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param String latestPatch = null
@param boolean upToDate = false
@param int gemCount = 0
@param int uniqueCount = 0
@param int baseItemCount = 0
@param boolean hasTreeData = false
@param boolean hasAtlasData = false
@param String dataDir = ""
@param boolean imageMagickInstalled = false
@param boolean engineAvailable = false

!{
	String pageTitle = MessageUtil.getMessage("poe.admin.title");
	String dataStatusLabel = MessageUtil.getMessage("poe.admin.data.status");
	String gemsLabel = MessageUtil.getMessage("poe.tab.gems");
	String uniquesLabel = MessageUtil.getMessage("poe.tab.uniques");
	String treeLabel = MessageUtil.getMessage("poe.tab.tree");
	String treeReadyLabel = MessageUtil.getMessage("poe.admin.tree.ready");
	// 데이터 준비 여부 카드용 — 검색 결과 메시지(poe.gems.empty)를 재사용하면 "검색 결과가 없습니다"가 떠 뜻이 어긋난다
	String treeMissingLabel = MessageUtil.getMessage("poe.admin.tree.missing");
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
			<div class="flex flex-wrap items-center gap-2">
				<h1 class="text-2xl font-bold mr-1">${pageTitle}</h1>
				<span class="badge badge-ghost">${MessageUtil.getMessage("poe.admin.version.current")}: ${patch}</span>
				@if(latestPatch == null)
					<span class="badge badge-ghost badge-outline text-base-content/50">${MessageUtil.getMessage("poe.admin.version.unknown")}</span>
				@elseif(upToDate)
					<span class="badge badge-success badge-outline">${MessageUtil.getMessage("poe.admin.version.uptodate")}</span>
				@else
					<span class="badge badge-warning">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.admin.version.outdated"), latestPatch)}</span>
				@endif
				<span class="text-xs text-base-content/40 font-mono ml-auto">${dataDir}</span>
			</div>

			<%-- 최초 실행 안내: 데이터가 하나도 없을 때 --%>
			@if(gemCount == 0 && uniqueCount == 0 && baseItemCount == 0 && !hasTreeData)
				<div class="alert alert-info text-sm">${MessageUtil.getMessage("poe.admin.initial.hint")}</div>
			@endif

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
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/50">${MessageUtil.getMessage("layout.menu.poe.atlas")}</div>
						<div class="text-xl font-bold">${hasAtlasData ? treeReadyLabel : treeMissingLabel}</div>
				`)
			</div>

			<!-- 실행 환경: 없으면 설치 안내 (아이콘 = ImageMagick, 빌드/시뮬레이터 = LuaJIT+PoB 소스) -->
			@template._components.ui.card(bodyClass = "gap-2", body = @`
					<h2 class="text-sm font-bold text-base-content/70">${MessageUtil.getMessage("poe.admin.env.title")}</h2>
					<div class="flex flex-wrap items-center gap-2 text-sm">
						<span class="w-40 text-base-content/60">${MessageUtil.getMessage("poe.admin.env.imagemagick")}</span>
						@if(imageMagickInstalled)
							<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.env.ok")}</span>
						@else
							<span class="badge badge-warning badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.env.missing")}</span>
							<code class="text-xs bg-base-200 rounded px-1.5 py-0.5">winget install ImageMagick.ImageMagick</code>
							<span class="text-xs text-base-content/50">${MessageUtil.getMessage("poe.admin.env.imagemagick.hint")}</span>
						@endif
					</div>
					<div class="flex flex-wrap items-center gap-2 text-sm">
						<span class="w-40 text-base-content/60">${MessageUtil.getMessage("poe.admin.env.engine")}</span>
						@if(engineAvailable)
							<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.env.ok")}</span>
						@else
							<span class="badge badge-warning badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.env.missing")}</span>
							<code class="text-xs bg-base-200 rounded px-1.5 py-0.5">winget install DEVCOM.LuaJIT</code>
							<span class="text-xs text-base-content/50">${MessageUtil.getMessage("poe.admin.env.engine.hint")}</span>
						@endif
					</div>
			`)

			<!-- 추출 파이프라인 실행/상태 (고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단) -->
			@template.poe.htmx.extractWrap()
		</div>
	`
)
