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
@param java.util.List<net.luversof.web.gate.poe.controller.PoeViewController.DataArtifact> artifacts = java.util.List.of()
@param Long dataLoadedAtEpochMs = null
@param boolean dataStaleInApi = false
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
					<%-- 데이터 산출물 인벤토리 — 갱신 후 무엇이 실제로 생성됐는지, 어떤 파일만 옛 버전으로 남았는지.
					     한 추출 스텝이 조용히 실패해도 이전엔 화면에 드러나지 않았다. --%>
					<h2 class="text-sm font-bold text-base-content/70">${MessageUtil.getMessage("poe.admin.data.inventory")}</h2>
					<div class="text-[11px] text-base-content/50 -mt-1">${MessageUtil.getMessage("poe.admin.data.inventory.hint")}</div>
					<%-- 파일 시각과 **API 가 읽은 시각**을 나란히 — 앱 밖에서 파이프라인을 돌리면 재기동 전까지 반영되지 않는다.
					     이게 안 보여서 2026-08-11 에 "재기동했더니 최적화 기준선이 -0.97% 움직인" 원인 추적에 시간을 썼다. --%>
					@if(dataLoadedAtEpochMs != null)
						<div class="text-[11px] ${dataStaleInApi ? "text-warning" : "text-base-content/50"}" data-poe-dataload>
							${MessageUtil.getMessage("poe.admin.data.loadedat")}
							<span class="font-mono">${java.time.Instant.ofEpochMilli(dataLoadedAtEpochMs).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))}</span>
							@if(dataStaleInApi)
								· <span class="font-bold">${MessageUtil.getMessage("poe.admin.data.stale.api")}</span>
							@endif
						</div>
					@endif
					<div class="grid gap-x-6 gap-y-1 sm:grid-cols-2 lg:grid-cols-3 text-[12px]" data-poe-artifacts>
						@for(var a : artifacts)
							<div class="flex items-baseline gap-2 border-b border-base-200/60 py-0.5" data-artifact="${a.file()}">
								<span class="w-28 shrink-0 text-base-content/70">${MessageUtil.getMessage(a.labelKey())}</span>
								@if(!a.exists())
									<span class="badge badge-warning badge-sm">${MessageUtil.getMessage("poe.admin.data.missing")}</span>
								@else
									<span class="font-mono text-base-content/50">${String.format("%,d KB", a.sizeBytes() / 1024)}</span>
									<span class="ml-auto font-mono text-[11px] text-base-content/40">${a.modifiedEpochMs() == null ? "" : java.time.Instant.ofEpochMilli(a.modifiedEpochMs()).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))}</span>
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
