@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeClusterNotable
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param List<PoeClusterNotable> notables = java.util.List.of()

!{
	String pageTitle = MessageUtil.getMessage("layout.menu.poe.clusters");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	boolean ko = PoeText.isKorean();
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
				<span class="text-sm text-base-content/50">${ko ? "클러스터 주얼 노터블 사전 — " : "Cluster jewel notables — "}<a class="link link-hover text-primary" href="/poe/tree">${ko ? "트리 에디터의 클러스터 소켓에서 구성 →" : "configure in tree editor →"}</a></span>
				<span class="text-xs font-mono text-base-content/40">${notables.size()}</span>
			</div>

			<div class="flex flex-wrap items-center gap-1.5 rounded-2xl bg-base-100 p-3 shadow-sm">
				<input id="poeClusterSearch" type="search" class="input input-sm input-bordered w-full sm:w-72"
				       placeholder="${ko ? "노터블 검색 (예: 생명력, 투사체)" : "Search notables"}" autocomplete="off">
				<span id="poeClusterCount" class="text-xs font-mono text-base-content/50"></span>
			</div>

			<div class="grid gap-2 md:grid-cols-2 [&>*]:min-w-0" id="poeClusterList">
				@for(PoeClusterNotable n : notables)
					!{
						String clusterIcon = n.icon() != null && n.icon().contains("SkillIcons/")
							? "tree/" + n.icon().substring(n.icon().indexOf("SkillIcons/") + 11).toLowerCase().replace("/", "_")
							: null;
					}
					<div class="rounded-lg border border-base-200 bg-base-100 px-3 py-2" data-cluster-notable>
						<div class="flex items-center gap-2">
							@if(clusterIcon != null)
								<img src="/poe-assets/${clusterIcon}" class="w-12 h-12 object-contain shrink-0" alt="" loading="lazy" onerror="this.style.display='none'">
							@endif
							<%-- 인게임 색 관례: 노터블 **이름**은 툴팁 제목색(흰색), **스탯 줄**이 모드 파랑(#8888ff).
							     여기만 반대로 이름이 파랑·스탯이 본문색이라 트리 노드 팝업(.poe-popup-title/.poe-popup-stat)과 어긋났다. --%>
							<span class="poe-entry-name font-semibold">${PoeText.name(n.nameKo(), n.name())}</span>
							<span class="hidden">${n.name()}</span>
						</div>
						!{List<String> clusterLines = ko && n.statsKo() != null && !n.statsKo().isEmpty() ? n.statsKo() : n.stats();}
						@for(String line : clusterLines)
							<div class="poe-entry-stat text-[12px] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
						@endfor
					</div>
				@endfor
			</div>
			<script nonce="${CspNonceHolder.getNonce()}">
				(function () {
					var search = document.getElementById("poeClusterSearch");
					var count = document.getElementById("poeClusterCount");
					function apply() {
						var q = (search.value || "").trim().toLowerCase();
						var rows = document.querySelectorAll("#poeClusterList [data-cluster-notable]");
						var shown = 0;
						rows.forEach(function (r) {
							var hit = !q || r.textContent.toLowerCase().indexOf(q) !== -1;
							r.style.display = hit ? "" : "none";
							if (hit) shown++;
						});
						count.textContent = q ? shown + " / " + rows.length : "";
					}
					// URL q= 동기화(#133 관례)
					function syncUrl() {
						var url = new URL(location.href);
						var q = (search.value || "").trim();
						if (q) url.searchParams.set("q", q); else url.searchParams.delete("q");
						history.replaceState(history.state, "", url);
					}
					search.addEventListener("input", function () { apply(); syncUrl(); });
					var initParams = new URLSearchParams(location.search);
					if (initParams.get("q")) { search.value = initParams.get("q"); apply(); }
				})();
			</script>
		</div>
	`)
