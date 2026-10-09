@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.util.CspNonceHolder

<%-- 모드 검색 + 아이템 레벨 필터 (모디파이어 페이지 / 아이템 상세 공용 — PoE2 옵션 풀도 같은 막대, 10-01).
     scope 안의 [data-mod-family] 카드를 대상으로 하며, htmx 로 scope 내용이 바뀌어도 조건을 다시 적용한다.
     카드 요약 갱신 위치는 선택 표식으로 고를 수 있다(없으면 PoE1 마크업 기준):
       [data-family-stats]  요약 스탯 자리(data-join 이 있으면 줄을 그 문자열로 이어 글자로)
       [data-family-tiers]  티어 수 배지(data-tiers-format 의 {0} 에 개수, 없으면 "NT")
       [data-tier-lines]    티어 행 안의 스탯 줄 묶음(없으면 3번째 칸 div 중 첫째=모드명을 뺀 나머지) --%>
@param String scope
@param String idPrefix

!{ boolean ko = PoeText.isKorean(); }

<div class="flex flex-wrap items-center gap-2">
	<input id="${idPrefix}Search" type="search" class="input input-sm input-bordered w-full sm:w-72"
	       placeholder="${ko ? "모드 검색 (예: 저항, 공격 속도)" : "Search mods (e.g. resistance)"}" autocomplete="off" aria-label="${ko ? "모드 검색 (예: 저항, 공격 속도)" : "Search mods (e.g. resistance)"}">
	<label class="flex items-center gap-1.5 text-sm text-base-content/60">
		${ko ? "아이템 레벨" : "Item level"}
		<input id="${idPrefix}Ilvl" type="number" min="1" max="100" class="input input-sm input-bordered w-20"
		       placeholder="${ko ? "전체" : "all"}" autocomplete="off" aria-label="${ko ? "전체" : "all"}">
	</label>
	<span id="${idPrefix}Count" class="text-xs font-mono text-base-content/60"></span>
</div>
<script nonce="${CspNonceHolder.getNonce()}">
	(function () {
		//  - 검색어: 패밀리 카드 텍스트(모드명/스탯/키)에 포함된 것만.
		//  - 아이템 레벨: 그 레벨에서 실제로 뜰 수 있는 티어만 보이고(요구 ilvl 초과 티어는 숨김),
		//    쓸 수 있는 티어가 하나도 없는 패밀리는 통째로 숨긴다(poedb Modifiers 방식).
		var scope = "${scope}";
		var input = document.getElementById("${idPrefix}Search");
		var ilvlInput = document.getElementById("${idPrefix}Ilvl");
		var count = document.getElementById("${idPrefix}Count");
		if (!input) return;
		/** 카드 요약(스탯줄 + NT 배지)을 가용 최상위 티어 기준으로 갱신. filtered=false 면 원래대로 복원. */
		function updateSummary(card, filtered) {
			var statBox = card.querySelector("[data-family-stats]") || card.querySelector("summary .min-w-0");
			var badge = card.querySelector("[data-family-tiers]") || card.querySelector("summary span");
			var ilvlBadge = card.querySelector("summary [data-top-ilvl]");
			if (!statBox || !badge) return;
			// 티어 행이 아예 없는 카드(엘드리치 등)는 요약 갱신 대상이 아니다 — 방치하면
			// 레벨 필터 시 배지가 "0T"로 덮여버렸다(6T→0T 실증).
			if (!card.querySelector("tr[data-tier-ilvl]")) filtered = false;
			if (card.dataset.origStats === undefined) {
				card.dataset.origStats = statBox.innerHTML;
				card.dataset.origBadge = badge.textContent;
				if (ilvlBadge) card.dataset.origIlvl = ilvlBadge.textContent;
			}
			if (!filtered) {
				statBox.innerHTML = card.dataset.origStats;
				badge.textContent = card.dataset.origBadge;
				if (ilvlBadge && card.dataset.origIlvl) ilvlBadge.textContent = card.dataset.origIlvl;
				return;
			}
			var rows = [].filter.call(card.querySelectorAll("tr[data-tier-ilvl]"), function (r) {
				return r.style.display !== "none";
			});
			badge.textContent = badge.dataset.tiersFormat ? badge.dataset.tiersFormat.replace("{0}", rows.length) : rows.length + "T";
			if (!rows.length) return;
			// 요구 ilvl 배지도 가용 최상위 티어 기준으로 — 원래 값(T1)을 두면 레벨 필터와 모순돼 보인다
			if (ilvlBadge) ilvlBadge.textContent = "i" + rows[0].getAttribute("data-tier-ilvl");
			// 효과 칸(3번째 td)의 div 중 첫째는 모드명, 나머지가 스탯 줄 — [data-tier-lines] 가 있으면 그 자식들이 스탯 줄
			var lineBox = rows[0].querySelector("[data-tier-lines]");
			var divs = lineBox ? [].slice.call(lineBox.children) : [].slice.call(rows[0].querySelectorAll("td")[2].querySelectorAll("div"), 1);
			if (statBox.hasAttribute("data-join")) {
				statBox.textContent = divs.map(function (d) { return d.textContent.trim(); }).join(statBox.getAttribute("data-join"));
				return;
			}
			statBox.innerHTML = divs
				.map(function (d) {
					return '<span class="text-[13px] text-[#8888ff] block truncate">' + d.innerHTML + "</span>";
				})
				.join("");
		}
		function applyFilter() {
			var q = (input.value || "").trim().toLowerCase().replace(/[ \t\u00a0]+/g, ""); // 띄어쓰기 무시(10-02 C25)
			var lvl = parseInt(ilvlInput.value, 10);
			var hasLvl = !isNaN(lvl) && lvl > 0;
			var cards = document.querySelectorAll(scope + " [data-mod-family]");
			var shown = 0;
			cards.forEach(function (card) {
				var available = true;
				var tierRows = card.querySelectorAll("tr[data-tier-ilvl]");
				if (hasLvl && tierRows.length) {
					// 티어 행이 없는 카드(도유/엘드리치)는 레벨 개념이 없다 — 레벨 필터로 숨기면
					// ilvl 50 입력 시 도유 470행이 통째로 사라졌다(실증 버그).
					var visibleTiers = 0;
					tierRows.forEach(function (r) {
						var need = parseInt(r.getAttribute("data-tier-ilvl"), 10);
						var ok = isNaN(need) || need <= lvl;
						r.style.display = ok ? "" : "none";
						if (ok) visibleTiers++;
					});
					available = visibleTiers > 0;
				} else {
					card.querySelectorAll("tr[data-tier-ilvl]").forEach(function (r) { r.style.display = ""; });
				}
				var hit = (!q || card.textContent.toLowerCase().replace(/[ \t\u00a0]+/g, "").indexOf(q) !== -1) && available;
				card.style.display = hit ? "" : "none";
				if (hit) shown++;
				// 요약줄은 기본이 **최상위 티어** 수치다. 레벨 필터를 걸면 그 레벨에서 못 뜨는 값을 계속
				// 보여줘 오해를 부르므로, 가용 최상위 티어(티어 행은 ilvl 내림차순이라 첫 보이는 행)로 바꾼다.
				updateSummary(card, hasLvl);
			});
			count.textContent = (q || hasLvl) ? shown + " / " + cards.length : "";
		}
		// 검색어/레벨을 URL(q=/lvl=)에 반영 — 필터 상태 공유·새로고침 유지(#33 URL 반영 관례).
		// htmx push-url(클래스 전환)과 공존: replaceState 로 현재 URL 의 쿼리만 갱신한다.
		function syncUrl() {
			var url = new URL(location.href);
			var q = (input.value || "").trim();
			var lvl = (ilvlInput.value || "").trim();
			if (q) url.searchParams.set("q", q); else url.searchParams.delete("q");
			if (lvl) url.searchParams.set("lvl", lvl); else url.searchParams.delete("lvl");
			history.replaceState(history.state, "", url);
		}
		input.addEventListener("input", function () { applyFilter(); syncUrl(); });
		ilvlInput.addEventListener("input", function () { applyFilter(); syncUrl(); });
		// htmx 가 scope 안을 갈아끼워도(클래스/영향력 전환) 현재 조건을 다시 적용
		document.body.addEventListener("htmx:afterSwap", function (e) {
			if (e.target && ("#" + e.target.id) === scope) applyFilter();
		});
		// 최초 로드: URL 의 q=/lvl= 복원 — 이 스크립트는 본문(scope 내용)보다 앞에 있어
		// 즉시 applyFilter() 하면 카드 0개에 적용된다(무효). DOM 완성 후에 적용한다.
		var initParams = new URLSearchParams(location.search);
		if (initParams.get("q")) input.value = initParams.get("q");
		if (initParams.get("lvl")) ilvlInput.value = initParams.get("lvl");
		if (input.value || ilvlInput.value) {
			if (document.readyState === "loading") {
				document.addEventListener("DOMContentLoaded", applyFilter);
			} else {
				applyFilter();
			}
		}
	})();
</script>
