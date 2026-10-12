@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param Poe2.ExtractStatus status
@param Poe2.ExtractVersion version = null
@param boolean loggedIn = false

!{
	boolean running = Boolean.TRUE.equals(status.running());
	boolean available = status.available() == null || Boolean.TRUE.equals(status.available());
	java.util.List<String> log = status.log() == null ? java.util.List.of() : status.log();
	// 로그는 마지막 40줄만(전체는 API 상태에 200줄)
	java.util.List<String> tail = log.size() > 40 ? log.subList(log.size() - 40, log.size()) : log;
	String dataPatch = version == null ? null : version.dataPatch();
	String latestPatch = version == null ? null : version.latestPatch();
	String configPatch = version == null ? null : version.configPatch();
	boolean outdated = latestPatch != null && version != null && !Boolean.TRUE.equals(version.upToDate());
	// 최신과 다르면 단추에 목표 버전(PoE1 과 같은 문구 · 같은 키)
	String runLabel = outdated
		? java.text.MessageFormat.format(MessageUtil.getMessage("poe.admin.extract.run.latest"), latestPatch)
		: MessageUtil.getMessage("poe.admin.extract.run");
	String result = status.status();
}

<%-- PoE2 추출 상태 조각 — PoE1 관리 화면(poe/htmx/extractStatus.jte)과 같은 구성 · 같은 문구(10-12 C261).
     도는 동안 2초마다 스스로 다시 불러온다(끝나면 폴링 속성 자체가 없다 — JTE 는 null 속성을 빼고 그린다). --%>
<div hx-get="${running ? "/poe2/htmx/admin/status" : null}" hx-trigger="${running ? "every 2s" : null}"
     hx-target="${running ? "#poe2Extract" : null}" hx-swap="${running ? "innerHTML" : null}">
	@template._components.ui.card(bodyClass = "gap-3", body = @`
			<%-- 버전 비교 — 갱신 단추 바로 옆. 최신이어도 "검사 결과"를 보여야 확인했다는 걸 안다(PoE1 과 같다) --%>
			<div class="flex flex-wrap items-center gap-2 text-xs" data-poe2-version>
				<span class="text-base-content/60">${MessageUtil.getMessage("poe.admin.version.current")}</span>
				<span class="font-mono font-semibold">${dataPatch == null ? "-" : dataPatch}</span>
				<span class="text-base-content/60">→</span>
				<span class="text-base-content/60">${MessageUtil.getMessage("poe.admin.version.latest")}</span>
				<span class="font-mono font-semibold ${outdated ? "text-warning" : ""}">${latestPatch == null ? "?" : latestPatch}</span>
				@if(latestPatch == null)
					<span class="badge badge-ghost badge-sm">${MessageUtil.getMessage("poe.admin.version.unknown")}</span>
				@elseif(outdated)
					<span class="badge badge-warning badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.admin.version.outdated"), latestPatch)}</span>
				@else
					<span class="badge badge-success badge-outline badge-sm">${MessageUtil.getMessage("poe.admin.version.uptodate")}</span>
				@endif
				@if(configPatch != null && dataPatch != null && !configPatch.equals(dataPatch))
					<span class="badge badge-info badge-outline badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe.admin.version.config"), configPatch)}</span>
				@endif
				<%-- 한국 IP 는 패치 서버가 카카오 CDN 을 준다 — 받는 곳 번호(PoE1 의 PoB 버전 배지 자리) --%>
				@if(version != null && version.latestCdnVersion() != null)
					<span class="badge badge-ghost badge-sm">${java.text.MessageFormat.format(MessageUtil.getMessage("poe2.admin.version.kakao"), version.latestCdnVersion())}</span>
				@endif
			</div>
			<div class="flex flex-wrap items-center gap-3">
				<div class="text-sm leading-6 text-base-content/60 min-w-0 flex-1">${MessageUtil.getMessage("poe2.admin.desc")}</div>
				@if(!available)
					<span class="badge badge-ghost">${MessageUtil.getMessage("poe.admin.extract.unavailable")}</span>
				@elseif(!loggedIn)
					<span class="badge badge-ghost">${MessageUtil.getMessage("stock.label.login.required")}</span>
				@elseif(running)
					<button type="button" class="btn btn-primary btn-sm" disabled>
						<span class="loading loading-spinner loading-xs"></span> ${MessageUtil.getMessage("poe.admin.extract.running")}
					</button>
				@else
					<button type="button" class="btn btn-sm ${outdated ? "btn-warning" : "btn-primary"}"
					        hx-post="/poe2/htmx/admin/extract" hx-target="#poe2Extract" hx-swap="innerHTML">${runLabel}</button>
				@endif
				@if("SUCCESS".equals(result) && !running)
					<span class="badge badge-success badge-outline">${MessageUtil.getMessage("poe.admin.extract.status.success")}</span>
				@elseif("FAILED".equals(result) && !running)
					<span class="badge badge-error badge-outline">${MessageUtil.getMessage("poe.admin.extract.status.failed")}</span>
				@endif
			</div>
			@if(!tail.isEmpty())
				<pre id="poe2ExtractLog" class="max-h-72 overflow-y-auto rounded-box bg-neutral text-neutral-content/80 p-3 text-xs leading-5 whitespace-pre-wrap">@for(String line : tail)${line}
@endfor</pre>
				<%-- 폴링 스왑마다 pre 가 새로 생겨 스크롤이 맨 위로 돌아간다 — PoE1 과 같은 꼬리 따라가기(위로 올려 읽는 중이면 그 자리 유지) --%>
				<script nonce="${CspNonceHolder.getNonce()}">
					(function () {
						var el = document.getElementById("poe2ExtractLog");
						if (!el) return;
						var s = window.__poe2ExtractLogScroll;
						if (s && !s.atBottom) {
							el.scrollTop = s.top;
						} else {
							el.scrollTop = el.scrollHeight;
						}
						el.addEventListener("scroll", function () {
							window.__poe2ExtractLogScroll = {
								top: el.scrollTop,
								atBottom: el.scrollHeight - el.scrollTop - el.clientHeight < 24
							};
						});
					})();
				</script>
			@endif
	`)
</div>
