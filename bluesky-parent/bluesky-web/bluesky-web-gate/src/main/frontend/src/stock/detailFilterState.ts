// <details> 의 접힘/펼침 상태를 htmx 스왑(정렬/필터/기간 변경)에도 유지한다.
//  1) [data-detail-filter]      : 상세 조회 필터 — 모든 곳이 같은 상태 공유(전역 공통 키), 기본 펼침.
//  2) [data-persist-open="키"]  : 개별 섹션(상세 목록 등) — 각자 상태 저장, 저장값이 있을 때만 복원(기본은 서버 렌더 상태).
// localStorage 사용(탭 간 공유). import/export 없이 classic <script src> 로 로드한다.
(function () {
	var FILTER_KEY = "stockDetailFilterOpen";
	var FILTER_SELECTOR = "[data-detail-filter]";
	var SECTION_SELECTOR = "[data-persist-open]";
	var SECTION_PREFIX = "stockDetailSectionOpen:";

	function lsGet(key: string): string | null {
		try {
			return typeof localStorage !== "undefined"
				? localStorage.getItem(key)
				: null;
		} catch (e) {
			return null;
		}
	}
	function lsSet(key: string, value: string) {
		try {
			localStorage.setItem(key, value);
		} catch (e) {}
	}

	// 1) 상세 조회 필터: 모든 [data-detail-filter] 가 같은 상태를 공유한다.
	// 기본은 접힘 - 실측 2026-09-10(qa/filter-space.cjs): 배당·매매·활동·자산성장 네 화면 모두 펼친 채로
	// 열려 화면당 116~124px 를 먹는데 적용된 필터는 0개였다. 다만 필터가 걸려 있으면 왜 결과가 좁혀졌는지
	// 보여야 하므로 펼친다. 사용자가 한 번이라도 접거나 펼치면 그 선택이 이긴다.
	function hasActiveFilter(): boolean {
		var els = document.querySelectorAll(FILTER_SELECTOR);
		for (var i = 0; i < els.length; i++) {
			if (els[i].getAttribute("data-filter-active") === "true") return true;
		}
		return false;
	}
	function filterIsOpen(): boolean {
		var v = lsGet(FILTER_KEY);
		if (v !== null) return v === "1";
		return hasActiveFilter();
	}
	function applyFilters() {
		var open = filterIsOpen();
		var els = document.querySelectorAll(FILTER_SELECTOR);
		for (var i = 0; i < els.length; i++) {
			var el: any = els[i];
			if (el.open !== open) el.open = open;
		}
	}

	// 2) 개별 섹션: 저장값이 있을 때만 복원(없으면 서버 렌더 상태 유지).
	function applySections() {
		var els = document.querySelectorAll(SECTION_SELECTOR);
		for (var i = 0; i < els.length; i++) {
			var el: any = els[i];
			var v = lsGet(SECTION_PREFIX + (el.getAttribute("data-persist-open") || ""));
			if (v === null) continue;
			var open = v === "1";
			if (el.open !== open) el.open = open;
		}
	}

	function applyAll() {
		applyFilters();
		applySections();
	}

	// <details> 의 toggle 이벤트는 버블링하지 않으므로 capture 단계에서 위임 수신한다.
	document.addEventListener(
		"toggle",
		function (e) {
			var t: any = e.target;
			if (!t || typeof t.matches !== "function") return;
			if (t.matches(FILTER_SELECTOR)) {
				lsSet(FILTER_KEY, t.open ? "1" : "0");
				applyFilters(); // 같은 페이지의 다른 필터도 동일 상태로 동기화
			} else if (t.matches(SECTION_SELECTOR)) {
				lsSet(
					SECTION_PREFIX + (t.getAttribute("data-persist-open") || ""),
					t.open ? "1" : "0",
				);
			}
		},
		true,
	);

	function init() {
		applyAll();
	}
	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", init);
	} else {
		init();
	}

	// htmx 스왑(정렬/필터 등) 직후 복원. 스왑 후 다른 스크립트(date-range-picker 재초기화 등)가
	// <details> 를 다시 펼칠 수 있어, settle 시점 + 마이크로태스크 이후에도 재확정한다.
	function applyAfterSwap() {
		applyAll();
		setTimeout(applyAll, 0);
	}
	document.addEventListener("htmx:afterSwap", applyAfterSwap);
	document.addEventListener("htmx:afterSettle", applyAfterSwap);
})();
