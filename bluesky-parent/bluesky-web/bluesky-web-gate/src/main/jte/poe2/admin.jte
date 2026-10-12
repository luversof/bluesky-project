@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param Poe2.Meta meta = null
@param boolean loggedIn = false
@param String patch = ""
@param String latestPatch = null
@param boolean upToDate = false
@param String latestCdnVersion = null
@param java.util.List<net.luversof.web.gate.poe.controller.PoeViewController.DataArtifact> artifacts = java.util.List.of()
@param Long dataLoadedAtEpochMs = null
@param boolean dataStaleInApi = false
@param String dataDir = ""
@param boolean imageMagickInstalled = false
@param boolean engineAvailable = false

!{
	String pageTitle = MessageUtil.getMessage("poe2.admin.title");
	String treeReadyLabel = MessageUtil.getMessage("poe.admin.tree.ready");
	String treeMissingLabel = MessageUtil.getMessage("poe.admin.tree.missing");
	String countUnit = MessageUtil.getMessage("common.count");
	int gems = meta == null || meta.gems() == null ? 0 : meta.gems();
	int uniques = meta == null || meta.uniques() == null ? 0 : meta.uniques();
	int baseItems = meta == null || meta.baseItems() == null ? 0 : meta.baseItems();
	int augments = meta == null || meta.augments() == null ? 0 : meta.augments();
	boolean hasTree = meta != null && Boolean.TRUE.equals(meta.tree());
	boolean hasAtlas = artifacts.stream().anyMatch(a -> "atlas-tree.json".equals(a.file()) && a.exists());
}

<%-- PoE2 데이터 관리 — PoE1 관리 화면(poe/admin.jte)과 같은 구성 · 같은 문구(10-12 C261 — 사용자 지적: 현재/최신 버전 · 데이터 카드 ·
     산출물 · 실행 환경이 PoE2 에만 없었고 단추 이름도 "데이터 추출 실행" 으로 달랐다). 실행은 로그인 사용자만. --%>
@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	menu = "poe2",
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-center gap-2">
				<h1 class="text-2xl font-bold mr-1">${pageTitle}</h1>
				<span class="badge badge-ghost">${MessageUtil.getMessage("poe.admin.version.current")}: ${patch == null ? "-" : patch}</span>
				@if(latestPatch == null)
					<span class="badge badge-ghost badge-outline text-base-content/60">${MessageUtil.getMessage("poe.admin.version.unknown")}</span>
				@elseif(upToDate)
					<span class="badge badge-success badge-outline">${MessageUtil.getMessage("poe.admin.version.uptodate")}</span>
				@else
					<span class="badge badge-warning">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.admin.version.outdated"), latestPatch)}</span>
				@endif
				<span class="text-xs text-base-content/60 font-mono ml-auto min-w-0 [overflow-wrap:anywhere]">${dataDir}</span>
			</div>

			<%-- 최초 실행 안내: 데이터가 하나도 없을 때(PoE1 과 같은 문구) --%>
			@if(gems == 0 && uniques == 0 && baseItems == 0 && !hasTree)
				<div class="alert alert-info text-sm">${MessageUtil.getMessage("poe.admin.initial.hint")}</div>
			@endif

			<!-- 현재 데이터 상태 -->
			<div class="grid grid-cols-1 sm:grid-cols-4 gap-3">
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.tab.gems")}</div>
						<div class="text-xl font-bold font-mono">${java.text.MessageFormat.format(countUnit, gems)}</div>
				`)
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.tab.uniques")}</div>
						<div class="text-xl font-bold font-mono">${java.text.MessageFormat.format(countUnit, uniques)}</div>
				`)
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.tab.items")}</div>
						<div class="text-xl font-bold font-mono">${java.text.MessageFormat.format(countUnit, baseItems)}</div>
				`)
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe.tab.tree")}</div>
						<div class="text-xl font-bold">${hasTree ? treeReadyLabel : treeMissingLabel}</div>
				`)
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("layout.menu.poe.atlas")}</div>
						<div class="text-xl font-bold">${hasAtlas ? treeReadyLabel : treeMissingLabel}</div>
				`)
				<%-- PoE2 에만 있는 데이터: 룬 · 영혼 핵 --%>
				@template._components.ui.card(bodyClass = "gap-1 py-4", body = @`
						<div class="text-xs font-semibold text-base-content/60">${MessageUtil.getMessage("poe2.admin.data.augments")}</div>
						<div class="text-xl font-bold font-mono">${java.text.MessageFormat.format(countUnit, augments)}</div>
				`)
			</div>

			<!-- 데이터 산출물 · 실행 환경(PoE1 과 같은 표) -->
			@template._components.ui.card(bodyClass = "gap-2", body = @`
					<h2 class="text-sm font-bold text-base-content/70">${MessageUtil.getMessage("poe.admin.data.inventory")}</h2>
					<div class="text-[11px] text-base-content/60 -mt-1">${MessageUtil.getMessage("poe.admin.data.inventory.hint")}</div>
					@if(dataLoadedAtEpochMs != null)
						<div class="text-[11px] ${dataStaleInApi ? "text-warning" : "text-base-content/60"}" data-poe-dataload>
							${MessageUtil.getMessage("poe.admin.data.loadedat")}
							<span class="font-mono">${java.time.Instant.ofEpochMilli(dataLoadedAtEpochMs).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))}</span>
							@if(dataStaleInApi)
								· <span class="font-bold">${MessageUtil.getMessage("poe.admin.data.stale.api")}</span>
							@endif
						</div>
					@endif
					<div class="grid gap-x-6 gap-y-1 sm:grid-cols-2 lg:grid-cols-3 text-[12px]" data-poe-artifacts>
						@for(var a : artifacts)
							<div class="flex flex-wrap items-baseline gap-x-2 border-b border-base-200/60 py-0.5" data-artifact="${a.file()}">
								<span class="w-28 shrink-0 text-base-content/70">${MessageUtil.getMessage(a.labelKey())}</span>
								@if(!a.exists())
									<span class="badge badge-warning badge-sm">${MessageUtil.getMessage("poe.admin.data.missing")}</span>
								@else
									<span class="font-mono text-base-content/60">${String.format("%,d KB", a.sizeBytes() / 1024)}</span>
									<span class="ml-auto font-mono text-[11px] text-base-content/60">${a.modifiedEpochMs() == null ? "" : java.time.Instant.ofEpochMilli(a.modifiedEpochMs()).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))}</span>
									@if(a.stale())
										<span class="badge badge-warning badge-sm shrink-0">${MessageUtil.getMessage("poe.admin.data.stale")}</span>
									@endif
								@endif
							</div>
						@endfor
					</div>

					<h2 class="text-sm font-bold text-base-content/70">${MessageUtil.getMessage("poe.admin.env.title")}</h2>
					<div class="flex flex-wrap items-center gap-2 text-sm">
						<span class="w-40 text-base-content/60">${MessageUtil.getMessage("poe.admin.env.imagemagick")}</span>
						@if(imageMagickInstalled)
							<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.env.ok")}</span>
						@else
							<span class="badge badge-warning badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.env.missing")}</span>
							<code class="text-xs bg-base-200 rounded px-1.5 py-0.5">winget install ImageMagick.ImageMagick</code>
							<span class="text-xs text-base-content/60">${MessageUtil.getMessage("poe.admin.env.imagemagick.hint")}</span>
						@endif
					</div>
					<div class="flex flex-wrap items-center gap-2 text-sm">
						<span class="w-40 text-base-content/60">${MessageUtil.getMessage("poe.admin.env.engine")}</span>
						@if(engineAvailable)
							<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.env.ok")}</span>
						@else
							<span class="badge badge-warning badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.env.missing")}</span>
							<code class="text-xs bg-base-200 rounded px-1.5 py-0.5">winget install DEVCOM.LuaJIT</code>
							<span class="text-xs text-base-content/60">${MessageUtil.getMessage("poe2.admin.env.engine.hint")}</span>
						@endif
					</div>
			`)

			<!-- 추출 파이프라인 실행/상태 -->
			<div id="poe2Extract" hx-get="/poe2/htmx/admin/status" hx-trigger="load" hx-swap="innerHTML">
				<div class="flex justify-center p-6"><span class="loading loading-spinner loading-md text-primary"></span></div>
			</div>
		</div>
	`
)
