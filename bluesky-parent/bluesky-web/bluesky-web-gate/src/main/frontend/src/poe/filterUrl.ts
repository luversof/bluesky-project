// 필터 폼 상태 → 페이지 URL 동기화 (form[data-url-sync] 전용).
// 검색어·칩 선택이 URL 에 반영되지 않아 새로고침하면 초기화되던 문제(사용자 지적) —
// htmx 요청이 끝날 때마다 폼 값을 replaceState 로 주소창에 반영한다(서버는 이미 이 파라미터로 초기 상태를 복원한다).
// 폼 필드명과 페이지 URL 파라미터명이 다르면 data-url-map="폼필드:URL파라미터,..." 로 매핑(예: itemClass:slot).
(function () {
	function sync(form: HTMLFormElement) {
		const map: { [key: string]: string } = {};
		for (const pair of (form.getAttribute("data-url-map") || "").split(",")) {
			const parts = pair.split(":");
			if (parts[0] && parts[1]) map[parts[0].trim()] = parts[1].trim();
		}
		const params = new URLSearchParams(globalThis.location.search);
		const data = new FormData(form);
		data.forEach(function (value, key) {
			const urlKey = map[key] || key;
			const v = String(value).trim();
			// 빈 검색어와 "all"(기본 칩)은 URL 을 지저분하게만 하니 제거 — 없는 것이 곧 기본값
			if (!v || v === "all") params.delete(urlKey);
			else params.set(urlKey, v);
		});
		const qs = params.toString();
		globalThis.history.replaceState(null, "", globalThis.location.pathname + (qs ? "?" + qs : ""));
	}
	document.addEventListener("htmx:afterRequest", function (ev) {
		const detail = (ev as CustomEvent).detail as { elt?: HTMLElement } | undefined;
		const elt = detail && detail.elt;
		if (!elt || !elt.closest) return;
		const form = elt.closest("form[data-url-sync]") as HTMLFormElement | null;
		if (form) sync(form);
	});
})();
