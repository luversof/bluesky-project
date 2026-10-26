@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeTattoo
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.util.CspNonceHolder

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param List<PoeTattoo> tattoos = java.util.List.of()

!{
	String pageTitle = MessageUtil.getMessage("poe.tattoos.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	boolean ko = PoeText.isKorean();
	// 대상 타입 → 한글 라벨 + 표시 순서(핵심노드 → 주요 스킬 → 마스터리 → 소형)
	var typeLabels = new java.util.LinkedHashMap<String, String>();
	typeLabels.put("Keystone", ko ? "핵심노드" : "Keystone");
	typeLabels.put("Notable", ko ? "주요 스킬" : "Notable");
	typeLabels.put("Mastery", ko ? "숙련" : "Mastery"); // 게임 낱말(10-23 Q124 — 문신 상세 숙련과 같게)
	typeLabels.put("Small Attribute", ko ? "소형(능력치)" : "Small attribute"); // 상세 툴팁과 같은 '능력치'(10-24 Q164 — 게임 '속성'은 속성 부여 뜻)
	typeLabels.put("Small Strength", ko ? "소형(힘)" : "Small strength");
	typeLabels.put("Small Dexterity", ko ? "소형(민첩)" : "Small dexterity");
	typeLabels.put("Small Intelligence", ko ? "소형(지능)" : "Small intelligence");
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
				<span class="text-sm text-base-content/60">${ko ? "패시브 스킬을 교체하는 카루이 문신 — " : "Karui tattoos replace passive nodes — "}<a class="link link-hover text-primary" href="/poe/tree">${ko ? "트리 에디터에서 우클릭으로 적용 →" : "apply in tree editor →"}</a></span>
				<span class="text-xs font-mono text-base-content/60">${tattoos.size()}</span>
			</div>

			<%-- 타입 칩 + 검색 — 클라이언트 필터(데이터 169건이라 서버 왕복 불필요) --%>
			<div class="flex flex-wrap items-center gap-1.5 rounded-2xl bg-base-100 p-3 shadow-sm">
				<a class="poe-chip badge badge-primary cursor-pointer" data-tattoo-type-chip="">${ko ? "전체" : "All"}</a>
				@for(var t : typeLabels.entrySet())
					<a class="poe-chip badge badge-ghost cursor-pointer" data-tattoo-type-chip="${t.getKey()}">${t.getValue()}</a>
				@endfor
				<input id="poeTattooSearch" type="search" class="input input-sm input-bordered w-full sm:w-64 sm:ml-2"
				       placeholder="${ko ? "문신 검색 (예: 화염, 토템)" : "Search tattoos"}" autocomplete="off" aria-label="${ko ? "문신 검색" : "Search tattoos"}">
				<span id="poeTattooCount" class="text-xs font-mono text-base-content/60"></span>
			</div>

			<div class="grid gap-2 md:grid-cols-2 [&>*]:min-w-0" id="poeTattooList">
				@for(PoeTattoo t : tattoos)
					!{
						// 아이콘 = tree-icons 평탄화 규칙(SkillIcons/ 이하 소문자 '_' ). 히네코라 전용 아트 58종은 미추출 — onerror 로 숨김.
						String tattooIcon = t.icon() != null && t.icon().contains("SkillIcons/")
							? "tree/" + t.icon().substring(t.icon().indexOf("SkillIcons/") + 11).toLowerCase().replace("/", "_")
							: null;
					}
					<div class="rounded-lg border border-base-200 bg-base-100 px-3 py-2" data-tattoo-type="${t.targetType()}">
						<div class="flex flex-wrap items-center gap-2">
							@if(tattooIcon != null)
								<img src="/poe-assets/${tattooIcon}" class="w-7 h-7 object-contain shrink-0" alt="" loading="lazy" data-onerror="hide">
							@endif
							<span class="font-semibold poe-entry-name">${PoeText.name(t.nameKo(), t.name())}</span>
							<span class="badge badge-ghost badge-xs">${typeLabels.getOrDefault(t.targetType(), t.targetType())}</span>
							<%-- 인접 조건(10-23 Q73) — 상한 100 은 데이터의 '제한 없음'이라 숫자로 안 보인다, 상한 1 도 보인다 --%>
							@if(t.minConnected() > 0)
								<span class="text-[10px] text-base-content/60" title="${ko ? "할당되어 있는 인접 패시브 스킬이 " + t.minConnected() + "개 이상이어야 합니다" : "Requires at least " + t.minConnected() + " adjacent Passive Skills to be allocated"}">${ko ? "인접 " + t.minConnected() + "개 이상" : "adj ≥" + t.minConnected()}</span>
							@elseif(t.maxConnected() == 1)
								<span class="text-[10px] text-base-content/60" title="${ko ? "할당되어 있는 인접 패시브 스킬이 1개 이하여야 합니다" : "Requires that no more than 1 adjacent Passive Skill is allocated"}">${ko ? "인접 1개 이하" : "adj ≤1"}</span>
							@endif
						</div>
						!{List<String> tattooLines = ko && t.statsKo() != null && !t.statsKo().isEmpty() ? t.statsKo() : t.stats();}
						@for(String line : tattooLines)
							<div class="poe-entry-stat text-[12px] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
						@endfor
					</div>
				@endfor
			</div>
			<script nonce="${CspNonceHolder.getNonce()}">
				(function () {
					var search = document.getElementById("poeTattooSearch");
					var count = document.getElementById("poeTattooCount");
					var activeType = "";
					function apply() {
						var q = (search.value || "").trim().toLowerCase().replace(/[ \t\u00a0]+/g, ""); // 띄어쓰기 무시(10-02 C25)
						var rows = document.querySelectorAll("#poeTattooList [data-tattoo-type]");
						var shown = 0;
						rows.forEach(function (r) {
							var hit = (!activeType || r.getAttribute("data-tattoo-type") === activeType)
								&& (!q || r.textContent.toLowerCase().replace(/[ \t\u00a0]+/g, "").indexOf(q) !== -1);
							r.style.display = hit ? "" : "none";
							if (hit) shown++;
						});
						count.textContent = (q || activeType) ? shown + " / " + rows.length : "";
					}
					// 필터 상태 URL 동기화(q=/type=) — 모드 필터바와 같은 관례(공유·새로고침 유지)
					function syncUrl() {
						var url = new URL(location.href);
						var q = (search.value || "").trim();
						if (q) url.searchParams.set("q", q); else url.searchParams.delete("q");
						if (activeType) url.searchParams.set("type", activeType); else url.searchParams.delete("type");
						history.replaceState(history.state, "", url);
					}
					function selectChip(chip) {
						activeType = chip.getAttribute("data-tattoo-type-chip");
						document.querySelectorAll("[data-tattoo-type-chip]").forEach(function (c) {
							c.classList.toggle("badge-primary", c === chip);
							c.classList.toggle("badge-ghost", c !== chip);
						});
						apply();
					}
					document.querySelectorAll("[data-tattoo-type-chip]").forEach(function (chip) {
						chip.addEventListener("click", function () {
							selectChip(chip);
							syncUrl();
						});
					});
					search.addEventListener("input", function () { apply(); syncUrl(); });
					// 최초 로드 복원 — 이 스크립트는 목록 아래에 있어 즉시 적용해도 안전
					var initParams = new URLSearchParams(location.search);
					if (initParams.get("q")) search.value = initParams.get("q");
					var initType = initParams.get("type");
					if (initType) {
						var initChip = [].find.call(document.querySelectorAll("[data-tattoo-type-chip]"), function (c) {
							return c.getAttribute("data-tattoo-type-chip") === initType;
						});
						if (initChip) selectChip(initChip);
					}
					if (search.value && !initType) apply();
				})();
			</script>
		</div>
	`)
