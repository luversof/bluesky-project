// URL query parameter 처리
const param = (() => {
	let _params = new URLSearchParams(globalThis.location.search);

	return {
		refreshUrl() {
			globalThis.history.replaceState(null, "", "?" + _params.toString());
		},
		getParams() {
			return _params;
		},
		getParam(paramKey: string) {
			if (_params.get(paramKey) === "") _params.delete(paramKey);
			return _params.get(paramKey) === null ? null : _params.get(paramKey);
		},
		setParam(paramKey: string, paramValue: string | null) {
			if (paramValue === null || paramValue === "") {
				_params.delete(paramKey);
			} else {
				_params.set(paramKey, paramValue);
			}
			this.refreshUrl();
		},
		deleteParam(paramKey: string) {
			_params.delete(paramKey);
		},
		resetParam() {
			_params = new URLSearchParams();
			this.refreshUrl();
		},
		getRequestPage() {
			const page = this.getParam("page");
			return (page === null ? 1 : Number.parseInt(page, 10)) - 1;
		},
	};
})();

// 전역으로 노출
(globalThis as any).param = param;

// 테마 토글([data-theme-toggle]): light/dark 전환 + localStorage 저장.
// 초기 적용은 defaultLayout <head>의 early-apply 스크립트가 담당한다.
document.addEventListener("click", (event) => {
	const toggle = (event.target as HTMLElement).closest("[data-theme-toggle]");
	if (!toggle) return;
	const html = document.documentElement;
	const next = html.getAttribute("data-theme") === "dark" ? "light" : "dark";
	html.setAttribute("data-theme", next);
	try {
		localStorage.setItem("theme", next);
	} catch {
		// localStorage 불가 환경(사생활 보호 모드 등)에서는 저장 없이 전환만
	}
});

// 활동 내역 [캘린더|타임라인|목록] 뷰 전환/복원 + 캘린더 날짜 상세 토글.
// 프래그먼트 인라인 스크립트가 아니라 문서 위임 + htmx:afterSettle 복원으로 처리해서
// htmx 스왑의 스크립트 실행 타이밍과 무관하게 항상 동작하게 한다.
const ACTIVITY_VIEW_KEY = "activityViewMode";
const ACTIVITY_TAB_ACTIVE_CLASSES = [
	"tab-active",
	"font-bold",
	"!bg-primary",
	"!text-primary-content",
	"shadow-sm",
];

const ACTIVITY_VIEW_PANEL_ID: Record<string, string> = {
	calendar: "activityCalendarView",
	timeline: "activityTimelineView",
	list: "activityListView",
};

function applyActivityTabState(root: Element, mode: string) {
	root.querySelectorAll("[data-activity-view-tab]").forEach((tab) => {
		const isActive = tab.getAttribute("data-activity-view-tab") === mode;
		ACTIVITY_TAB_ACTIVE_CLASSES.forEach((cls) =>
			tab.classList.toggle(cls, isActive),
		);
		tab.classList.toggle("text-base-content/60", !isActive);
	});
}

// 지연 로드된 보기 패널을 탭에 잇는다(aria-controls). 서버는 보이는 뷰 하나만 그리므로 나머지 탭은 패널이 붙은 뒤에야 가리킬 대상이 생긴다
// (실측 2026-09-09: 없는 id 를 가리키던 탭 2개). 패널이 없으면 속성을 두지 않는다.
function linkActivityTabToPanel(root: ParentNode, mode: string): boolean {
	const panelId = ACTIVITY_VIEW_PANEL_ID[mode];
	const tab = root.querySelector('[data-activity-view-tab="' + mode + '"]');
	if (!panelId || !tab) return false;
	const panel = root.querySelector("#" + panelId);
	if (!panel) {
		tab.removeAttribute("aria-controls");
		return false;
	}
	if (!panel.hasAttribute("role")) panel.setAttribute("role", "tabpanel");
	tab.setAttribute("aria-controls", panelId);
	return true;
}
function applyActivityView(root: Element, rawMode: string | null) {
	const mode =
		rawMode === "timeline" || rawMode === "list" ? rawMode : "calendar";
	Object.keys(ACTIVITY_VIEW_PANEL_ID).forEach((key) => {
		const panel = root.querySelector("#" + ACTIVITY_VIEW_PANEL_ID[key]);
		if (panel) {
			panel.classList.toggle("hidden", key !== mode);
			// 숨어 있던 패널은 폭이 0 이라 스크롤 표 래퍼의 tabindex 판정이 빠졌다 - 보일 때 다시 잰다.
			if (key === mode) syncScrollableFocus(panel);
		}
	});
	applyActivityTabState(root, mode);
}

// 현재 화면이 만들어진 조회 조건은 data-sync-url 이 페이지 URL 에 반영해 둔다.
// 그 쿼리를 그대로 재사용해야 지금 보이는 데이터와 같은 조건의 뷰를 받는다.
async function loadActivityView(root: Element, mode: string) {
	const panelId = ACTIVITY_VIEW_PANEL_ID[mode];
	if (!panelId || root.querySelector("#" + panelId)) {
		applyActivityView(root, mode);
		return;
	}
	const params = new URLSearchParams(globalThis.location.search);
	params.set("activityView", mode);
	root.setAttribute("aria-busy", "true");
	try {
		const res = await fetch(
			"/stock/htmx/activity-list?" + params.toString(),
			{ headers: { "HX-Request": "true" } },
		);
		if (!res.ok) throw new Error("HTTP " + res.status);
		const doc = new DOMParser().parseFromString(await res.text(), "text/html");
		const panel = doc.getElementById(panelId);
		if (!panel) throw new Error("panel " + panelId + " 없음");
		const panels = Object.keys(ACTIVITY_VIEW_PANEL_ID)
			.map((k) => root.querySelector("#" + ACTIVITY_VIEW_PANEL_ID[k]))
			.filter(Boolean) as Element[];
		const last = panels[panels.length - 1];
		const imported = document.importNode(panel, true);
		if (last) last.after(imported);
		else root.appendChild(imported);
		applyActivityView(root, mode);
		linkActivityTabToPanel(root, mode);
	} catch (e) {
		// 받아오지 못하면 "조회" 버튼의 htmx 경로로 프래그먼트 전체를 다시 그린다.
		const refresh = root.querySelector(
			"[data-activity-refresh]",
		) as HTMLElement | null;
		if (refresh) refresh.click();
	} finally {
		root.removeAttribute("aria-busy");
	}
}

function restoreActivityView() {
	const root = document.getElementById("activityListFragment");
	if (!root) return;
	let saved: string | null = null;
	try {
		saved = localStorage.getItem(ACTIVITY_VIEW_KEY);
	} catch (e) {
		saved = null;
	}
	const mode =
		saved === "timeline" || saved === "list" ? saved : "calendar";
	// 저장된 뷰가 응답에 없으면(직접 URL 로 다른 activityView 를 부른 경우 등)
	// 실제로 온 뷰를 보여 준다 — 셋 다 hidden 이라 빈 화면이 되는 것을 막는다.
	if (root.querySelector("#" + ACTIVITY_VIEW_PANEL_ID[mode])) {
		applyActivityView(root, mode);
		return;
	}
	const present = Object.keys(ACTIVITY_VIEW_PANEL_ID).find((k) =>
		root.querySelector("#" + ACTIVITY_VIEW_PANEL_ID[k]),
	);
	applyActivityView(root, present || mode);
}

// href 없는 <a role="tab"> 는 브라우저가 포커스도, Enter 도 주지 않는다(실측 2026-09-09: 주식 화면 4곳 탭 13개가 키보드로 닿지 않았다).
// 템플릿이 tabindex="0" 을 주고 여기서 Enter/Space 를 click 으로 바꾼다. 좌우 화살표는 같은 tablist 안에서 포커스를 옮긴다(WAI-ARIA 탭 관례).
document.addEventListener("keydown", (event) => {
	const target = event.target as HTMLElement;
	if (!target || !target.matches) return;
	if (!target.matches('a[role="tab"]:not([href])')) return;
	if (event.key === "Enter" || event.key === " ") {
		event.preventDefault();
		target.click();
		return;
	}
	if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
	const list = target.closest('[role="tablist"]');
	if (!list) return;
	const tabs = Array.from(list.querySelectorAll<HTMLElement>('[role="tab"]'));
	const index = tabs.indexOf(target);
	if (index < 0) return;
	event.preventDefault();
	const next = tabs[(index + (event.key === "ArrowRight" ? 1 : tabs.length - 1)) % tabs.length];
	next.focus();
});

// 활동 달력의 날짜 칸: 그날 상세(미리 렌더된 hidden 블록)를 펼치거나 접는다.
// 마우스 클릭과 키보드(Enter/Space)가 같은 길을 탄다 - 칸은 role="button" tabindex="0" 인 div 라
// 브라우저가 대신 click 을 만들어 주지 않는다(실측 2026-09-09: 전체 기간 181칸이 키보드로 열리지 않았다).
function toggleCalendarDay(cell: Element) {
	const root = cell.closest("#activityListFragment");
	if (!root) return;
	const dateKey = cell.getAttribute("data-cal-date");
	const monthKey = cell.getAttribute("data-cal-month");
	const panel = root.querySelector('[data-cal-panel="' + monthKey + '"]');
	if (!panel) return;
	const detail = panel.querySelector('[data-cal-detail="' + dateKey + '"]');
	if (!detail) return;
	const isSameOpen =
		panel.classList.contains("is-open") &&
		!detail.classList.contains("hidden");
	if (isSameOpen) {
		// 접기: 상세 블록도 다시 숨긴다. 실측 2026-09-10: aria-expanded 만 false 로 바뀌고 블록은 계속 보였다(상태 불일치).
		detail.classList.add("hidden");
		panel.classList.remove("is-open");
		cell.classList.remove("bg-base-200");
		cell.setAttribute("aria-expanded", "false");
		return;
	}
	panel
		.querySelectorAll("[data-cal-detail]")
		.forEach((other) => other.classList.add("hidden"));
	root
		.querySelectorAll("[data-cal-date].bg-base-200")
		.forEach((selected) => {
			selected.classList.remove("bg-base-200");
			selected.setAttribute("aria-expanded", "false");
		});
	detail.classList.remove("hidden");
	panel.classList.add("is-open");
	cell.classList.add("bg-base-200");
	cell.setAttribute("aria-expanded", "true");
}

document.addEventListener("keydown", (event) => {
	if (event.key !== "Enter" && event.key !== " ") return;
	const target = event.target as HTMLElement;
	if (!target || !target.closest) return;
	// 칸 안의 링크 등 스스로 키를 처리하는 요소는 건드리지 않는다
	if (target.closest("button, a, input, select, textarea")) return;
	const cell = target.closest("[data-cal-date]");
	if (!cell) return;
	event.preventDefault(); // Space 가 화면을 스크롤하지 않게
	toggleCalendarDay(cell);
});

document.addEventListener("click", (event) => {
	const target = event.target as HTMLElement;
	if (!target || !target.closest) return;

	const tab = target.closest("[data-activity-view-tab]");
	if (tab) {
		const root = tab.closest("#activityListFragment");
		if (!root) return;
		const raw = tab.getAttribute("data-activity-view-tab");
		const mode = raw === "timeline" || raw === "list" ? raw : "calendar";
		try {
			localStorage.setItem(ACTIVITY_VIEW_KEY, mode);
		} catch (e) {
			// localStorage 불가 환경에서는 저장 없이 전환만
		}
		// 서버가 보이는 뷰 하나만 그리므로, 아직 없는 뷰는 받아와야 한다.
		// (activityView=all 로 3종을 다 받은 경우엔 그대로 즉시 전환된다)
		if (root.querySelector("#" + ACTIVITY_VIEW_PANEL_ID[mode])) {
			applyActivityView(root, mode);
			return;
		}
		// 없는 뷰는 그 뷰만 받아 DOM 에 붙인다. 프래그먼트 전체를 다시 스왑하면
		// 이미 받아 둔 뷰까지 버려져 되돌아갈 때마다 왕복이 생긴다(실측 209ms).
		applyActivityTabState(root, mode);
		void loadActivityView(root, mode);
		return;
	}

	const cell = target.closest("[data-cal-date]");
	if (cell) toggleCalendarDay(cell);
});

document.addEventListener("DOMContentLoaded", restoreActivityView);
document.addEventListener("htmx:afterSettle", restoreActivityView);

// ---------------------------------------------------------------------------
// 범용 패널 탭.
//
// 표를 여러 개 세로로 쌓으면 아래쪽은 스크롤해야 나와서 있는 줄도 모르게 된다(실측 2026-09-07:
// 자산 성장이 5,075px = 5.6 화면, 카드 9 개). 서버가 이미 다 그려 보낸 조각을 탭으로 보이고
// 숨기기만 한다 - 왕복이 없다.
//
// 위의 활동 탭은 뷰를 서버에서 받아 오는 일까지 하므로 그쪽과 섞지 않고 따로 둔다.
const PANEL_TAB_ACTIVE_CLASSES = ACTIVITY_TAB_ACTIVE_CLASSES;

function panelTabKey(group: string) {
	return "panel-tab:" + group;
}

function applyPanelTabs(root: ParentNode, group: string, name: string) {
	root
		.querySelectorAll('[data-panel-tab-group="' + group + '"] [data-panel-tab]')
		.forEach((tab) => {
			const isActive = tab.getAttribute("data-panel-tab") === name;
			PANEL_TAB_ACTIVE_CLASSES.forEach((cls) =>
				tab.classList.toggle(cls, isActive),
			);
			tab.classList.toggle("text-base-content/60", !isActive);
		});
	root
		.querySelectorAll('[data-panel-group="' + group + '"]')
		.forEach((panel) => {
			const shown = panel.getAttribute("data-panel") === name;
			panel.classList.toggle("hidden", !shown);
			// axe 실측 2026-09-09(375px): 자산성장 '연도별 성과' 패널의 .overflow-x-auto 가 포커스 불가 - 로드 때는 hidden 이라
			// scrollWidth 가 0 이었다. 보이게 된 패널만 다시 잰다(다른 패널은 그대로).
			if (shown) syncScrollableFocus(panel);
		});
}

/** 저장된 탭이 지금 화면에 없으면(그 기간에는 그 표가 안 그려졌다) 첫 탭으로 떨어뜨린다. */
function restorePanelTabs() {
	document.querySelectorAll("[data-panel-tab-group]").forEach((bar) => {
		const group = bar.getAttribute("data-panel-tab-group");
		if (!group) return;
		const names = Array.from(bar.querySelectorAll("[data-panel-tab]"))
			.map((tab) => tab.getAttribute("data-panel-tab"))
			.filter((name): name is string => !!name);
		if (!names.length) return;
		let saved: string | null = null;
		try {
			saved = localStorage.getItem(panelTabKey(group));
		} catch (e) {
			saved = null;
		}
		// 화면이 더 나은 기본 탭을 알려 줄 수 있다(data-panel-tab-default). 실측 2026-09-10: 배당 '기간별 집계' 는
		// 3개월을 고르면 연도별이 한 줄뿐이라 아무것도 말해 주지 않는다(자산 성장이 같은 이유로 월 단위를 먼저 보여 준다).
		// 사용자가 직접 고른 탭(저장값)이 있으면 그것이 우선이다.
		const preferred = bar.getAttribute("data-panel-tab-default");
		const fallback = preferred && names.indexOf(preferred) >= 0 ? preferred : names[0];
		const name = saved && names.indexOf(saved) >= 0 ? saved : fallback;
		applyPanelTabs(document, group, name);
	});
}

document.addEventListener("click", (event) => {
	const target = event.target as HTMLElement;
	if (!target || !target.closest) return;
	const tab = target.closest("[data-panel-tab]");
	if (!tab) return;
	const bar = tab.closest("[data-panel-tab-group]");
	const group = bar && bar.getAttribute("data-panel-tab-group");
	const name = tab.getAttribute("data-panel-tab");
	if (!group || !name) return;
	try {
		localStorage.setItem(panelTabKey(group), name);
	} catch (e) {
		// localStorage 불가 환경에서는 저장 없이 전환만
	}
	applyPanelTabs(document, group, name);
});

document.addEventListener("DOMContentLoaded", restorePanelTabs);
document.addEventListener("htmx:afterSettle", restorePanelTabs);

// CSP 대응: hx-on:/hx-vals="js:" 는 htmx 가 eval 로 실행해 nonce 기반 CSP 와 함께 쓸 수 없다.
// 아래 데이터 속성 + 문서 위임으로 대체한다.

// [data-reload-after-request]: htmx 요청 성공 시 페이지 새로고침 (관리 데이터 갱신 버튼 등)
document.addEventListener("htmx:afterRequest", (event: any) => {
	const el = (event.target as HTMLElement)?.closest?.(
		"[data-reload-after-request]",
	);
	if (el && event.detail?.successful) {
		globalThis.location.reload();
	}
});

// [data-params-from-query]: 페이지 최초 로드 fragment 요청에 현재 URL 쿼리를 병합한다.
// 필터 조건이 URL 에 남아 있으면(아래 data-sync-url 로 기록됨) 새로고침/공유 시 그대로 복원된다.
// URL 의 키는 hx-include(전역 기간 입력 등)로 들어온 같은 키를 덮어쓴다.
document.addEventListener("htmx:configRequest", (event: any) => {
	const el = event.detail?.elt as HTMLElement | undefined;
	if (!el?.matches?.("[data-params-from-query]")) return;
	const merged: Record<string, string[]> = {};
	new URLSearchParams(globalThis.location.search).forEach((value, key) => {
		(merged[key] = merged[key] || []).push(value);
	});
	for (const key in merged) {
		event.detail.parameters[key] =
			merged[key].length > 1 ? merged[key] : merged[key][0];
	}
});

// activity-list fragment 요청에는 저장된 뷰 모드를 실어 보낸다.
// 서버가 그 뷰 하나만 렌더하므로 숨은 뷰의 마크업(실측 1370KB)이 아예 생성되지 않는다.
// data-params-from-query 훅보다 뒤에 등록해 URL 에 남은 옛 값을 localStorage 값으로 덮어쓴다.
document.addEventListener("htmx:configRequest", (event: any) => {
	const path = event.detail?.path;
	if (typeof path !== "string" || !path.endsWith("/stock/htmx/activity-list"))
		return;
	let saved: string | null = null;
	try {
		saved = localStorage.getItem(ACTIVITY_VIEW_KEY);
	} catch (e) {
		saved = null;
	}
	event.detail.parameters.activityView =
		saved === "timeline" || saved === "list" ? saved : "calendar";
});

// [data-sync-url="<fragment 경로>"]: 화면 래퍼에 지정한 목록 엔드포인트로의 GET 이 성공하면
// 그 조회 조건을 페이지 URL 에 반영한다(replaceState — 히스토리 오염 없음).
// 요청 주체(elt)는 스왑 대상 div 일 수 있으므로, 스왑에서 살아남는 래퍼에서 경로를 대조한다.
document.addEventListener("htmx:afterRequest", (event: any) => {
	const el = event.detail?.elt as HTMLElement | undefined;
	if (!event.detail?.successful) return;
	if (event.detail.requestConfig?.verb !== "get") return;
	const syncRoot = el?.closest?.("[data-sync-url]") as HTMLElement | null;
	if (!syncRoot) return;
	const responseUrl = event.detail.xhr?.responseURL || "";
	let pathname = "";
	let query = "";
	try {
		const parsed = new URL(responseUrl);
		pathname = parsed.pathname;
		query = parsed.search;
	} catch (e) {
		return;
	}
	if (pathname !== syncRoot.getAttribute("data-sync-url")) return;
	globalThis.history.replaceState(
		null,
		"",
		globalThis.location.pathname + query,
	);
});

// [data-page-param-from-query="page"]: 현재 URL 쿼리의 페이지 번호를 요청 파라미터로 전달 (없으면 1)
document.addEventListener("htmx:configRequest", (event: any) => {
	const key = (event.detail?.elt as HTMLElement | undefined)?.dataset
		?.pageParamFromQuery;
	if (key) {
		event.detail.parameters[key] =
			new URLSearchParams(globalThis.location.search).get(key) || "1";
	}
});

// [data-overlay] 레이어 닫기: X 버튼([data-overlay-close]), 배경 클릭, ESC
document.addEventListener("click", (event) => {
	const target = event.target as HTMLElement;
	const closeButton = target.closest?.("[data-overlay-close]");
	if (closeButton) {
		closeButton.closest("[data-overlay]")?.remove();
		return;
	}
	if (target.matches?.("[data-overlay]")) {
		target.remove();
	}
});
document.addEventListener("keydown", (event) => {
	if (event.key !== "Escape") return;
	const overlays = document.querySelectorAll("[data-overlay]");
	if (overlays.length) overlays[overlays.length - 1].remove();
});

// PoE 아이템 호버 미리보기 — .poe-hover 요소에 마우스를 올리면 hx-get 으로 로드된 게임 툴팁을
// 요소 근처(뷰포트 안)에 띄운다. 툴팁은 pointer-events-none 이라 마우스가 카드를 벗어나면 사라진다.
(() => {
	let host: HTMLElement | null = null;
	let active: Element | null = null;
	function ensureHost(): HTMLElement | null {
		if (host && document.body?.contains(host)) return host;
		if (!document.body) return null;
		host = document.createElement("div");
		host.id = "poePreview";
		host.className = "fixed z-[95] hidden";
		host.style.pointerEvents = "none";
		document.body.appendChild(host);
		return host;
	}
	function position(trigger: Element) {
		const h = ensureHost();
		if (!h || !h.firstElementChild) return;
		const r = trigger.getBoundingClientRect();
		h.style.visibility = "hidden";
		h.classList.remove("hidden");
		const hw = h.offsetWidth;
		const hh = h.offsetHeight;
		let left = r.right + 12;
		if (left + hw > window.innerWidth - 8) left = r.left - hw - 12;
		if (left < 8) left = 8;
		let top = r.top;
		if (top + hh > window.innerHeight - 8) top = window.innerHeight - hh - 8;
		if (top < 8) top = 8;
		h.style.left = left + "px";
		h.style.top = top + "px";
		h.style.visibility = "";
	}
	// 클릭 시 툴팁을 고정(pinned)하면 상호작용 가능(레벨 버튼 등). 밖 클릭/ESC 로 해제.
	let pinned = false;
	function hide() {
		if (pinned) return;
		host?.classList.add("hidden");
		if (host) host.replaceChildren();
		active = null;
	}
	function unpin() {
		pinned = false;
		if (host) host.style.pointerEvents = "none";
		host?.classList.add("hidden");
		if (host) host.replaceChildren();
		active = null;
	}
	function showInline(trigger: Element): boolean {
		const inline = trigger.querySelector("[data-poe-tip]");
		if (inline && !trigger.hasAttribute("hx-get")) {
			const h = ensureHost();
			if (h) {
				h.innerHTML = inline.innerHTML;
				position(trigger);
				return true;
			}
		}
		return false;
	}
	// 레이어 캐시 — 같은 hx-get URL 을 이미 불러왔으면 재요청/로딩 없이 재사용(hx-get URL 키).
	const layerCache = new Map<string, string>();
	function showCached(trigger: Element): boolean {
		const url = trigger.getAttribute("hx-get");
		if (!url || !layerCache.has(url)) return false;
		const h = ensureHost();
		if (!h) return false;
		h.innerHTML = layerCache.get(url) as string;
		// 캐시본을 직접 주입했으므로 내부 상호작용 요소(예: 젬 레벨 버튼)를 htmx 에 다시 등록
		const htmx = (window as any).htmx;
		if (htmx) htmx.process(h);
		position(trigger);
		return true;
	}
	// htmx 가 hx-target="#poePreview" 를 찾을 수 있게 DOM 준비되면 미리 생성
	if (document.body) ensureHost();
	else document.addEventListener("DOMContentLoaded", () => ensureHost());
	// 로딩 문구도 로케일에 맞춰야 한다 — 예전엔 한글 고정이라 EN 화면 호버 중에 "불러오는 중…"이 떴다.
	//    lang 은 <html lang="…"> (서버가 로케일대로 찍는다).
	const LOADING_LABEL =
		(document.documentElement.lang || "ko").toLowerCase().indexOf("en") === 0 ? "Loading…" : "불러오는 중…";
	// 서버 왕복(hx-get) 동안 띄우는 로딩 툴팁 — 빈 화면 대신 스피너를 보여준다(afterSwap 이 교체).
	const LOADING_TIP =
		'<div class="poe-tooltip poe-rar-white shadow-2xl" style="border-color:#c8c8c8"><div class="px-6 py-4 flex items-center justify-center gap-2"><span class="loading loading-spinner loading-sm text-primary"></span><span class="text-[12px] text-white/60">' + (LOADING_LABEL) + '</span></div></div>';
	document.addEventListener("mouseover", (event) => {
		if (pinned) return;
		const trigger = (event.target as Element)?.closest?.(".poe-hover");
		if (!trigger || trigger === active) return;
		active = trigger;
		// 인라인 툴팁: [data-poe-tip] 자식이 있으면 서버 왕복 없이 복제해 띄운다
		// (hx-get 이 있는 요소는 htmx 가 로드 → afterSwap 에서 위치)
		if (!showInline(trigger) && trigger.hasAttribute("hx-get")) {
			// 캐시에 있으면 재사용(로딩 생략), 없으면 서버 왕복 동안 스피너
			if (!showCached(trigger)) {
				const h = ensureHost();
				if (h) {
					h.innerHTML = LOADING_TIP;
					position(trigger);
				}
			}
		}
	});
	document.addEventListener("mouseout", (event) => {
		if (pinned) return;
		const trigger = (event.target as Element)?.closest?.(".poe-hover");
		if (!trigger) return;
		const to = (event as MouseEvent).relatedTarget as Element | null;
		if (!to?.closest?.(".poe-hover")) hide();
	});
	// 클릭: 트리거를 누르면 고정(상호작용 가능), 툴팁/트리거 밖을 누르면 해제
	document.addEventListener("click", (event) => {
		const trigger = (event.target as Element)?.closest?.(".poe-hover");
		if (trigger) {
			const h = ensureHost();
			if (!h) return;
			active = trigger;
			if (!h.firstElementChild) showInline(trigger); // 아직 안 떴으면(인라인) 지금 띄운다
			pinned = true;
			h.style.pointerEvents = "auto";
			h.classList.remove("hidden");
			position(trigger);
			return;
		}
		if (pinned && !(event.target as Element)?.closest?.("#poePreview")) unpin();
	});
	document.addEventListener("keydown", (event) => {
		if (event.key === "Escape" && pinned) unpin();
	});
	// 캐시에 있는 레이어면 서버 재요청을 아예 취소(mouseover 에서 이미 캐시본을 띄웠다).
	document.addEventListener("htmx:beforeRequest", (event: any) => {
		const elt = event.detail?.elt;
		if (!elt || !elt.classList?.contains?.("poe-hover")) return;
		const url = elt.getAttribute?.("hx-get");
		if (url && layerCache.has(url)) event.preventDefault(); // 캐시 재사용 — 네트워크 생략
	});
	// 여러 아이템을 빠르게 옮겨 호버하면 이전 레이어의 hx-get 응답이 뒤늦게 도착해 최신 레이어를 덮는다.
	// beforeSwap 에서 "지금 활성인 트리거의 응답"이 아니면 스왑을 취소해 항상 마지막 선택만 뜨게 한다.
	document.addEventListener("htmx:beforeSwap", (event: any) => {
		if (event.target?.id !== "poePreview") return;
		const elt = event.detail?.requestConfig?.elt;
		if (elt && active && elt !== active) {
			event.detail.shouldSwap = false; // 낡은 응답 폐기 — 활성 트리거 응답만 반영
		}
	});
	document.addEventListener("htmx:afterSwap", (event: any) => {
		if (event.target?.id !== "poePreview") return;
		// 방금 스왑된 레이어를 URL 키로 캐시 → 다음 호출 때 재사용(재요청 없음)
		const url = event.detail?.requestConfig?.elt?.getAttribute?.("hx-get");
		if (url && host && host.innerHTML) layerCache.set(url, host.innerHTML);
		if (active) position(active);
	});
	window.addEventListener("scroll", hide, true);
})();

// 부위 선택 칩(poedb 식): 클릭 시 활성 표시를 옮기고, 연결된 폼의 숨은 필터 값을 갱신 후
// change 를 발생시켜 htmx 재요청(검색어 q 와 부위 필터가 함께 반영된다).
document.addEventListener("click", (event) => {
	const chip = (event.target as HTMLElement)?.closest?.(".poe-chip") as HTMLElement | null;
	if (!chip) return;
	const group = chip.closest("[data-chip-group]") as HTMLElement | null;
	// 이 핸들러는 [data-chip-group] 필터 시스템 전용이다. 그룹 밖의 .poe-chip(모드 페이지 클래스/변형 칩처럼
	// 서버가 활성 상태를 렌더하는 것들)에 손대면, 이전 활성을 지울 그룹이 없어 poe-chip-active 가 **누적**돼
	// 고른 적 있는 칩이 전부 활성으로 보인다(사용자 지적 버그).
	if (!group) return;
	group.querySelectorAll(".poe-chip-active").forEach((c) => c.classList.remove("poe-chip-active"));
	chip.classList.add("poe-chip-active");
	const targetSel = group.getAttribute("data-chip-target");
	const field = group.getAttribute("data-chip-field");
	if (targetSel && field) {
		const form = document.querySelector(targetSel);
		const input = form?.querySelector(`[name="${field}"]`) as HTMLInputElement | null;
		if (input) {
			input.value = chip.getAttribute("data-chip-value") || "";
			input.dispatchEvent(new Event("change", { bubbles: true }));
		}
	}
});

// 아이템 탭 전환(일반↔고유): 현재 활성 칩의 정규 슬롯(data-slot) + 검색어를 ?slot&q 로 넘겨
// 탭 이동 후에도 필터가 유지되게 한다("전체"이거나 검색어가 없으면 그대로 이동).
document.addEventListener("click", (event) => {
	const tab = (event.target as HTMLElement)?.closest?.(
		"[data-poe-item-tab]",
	) as HTMLAnchorElement | null;
	if (!tab) return;
	const activeChip = document.querySelector(".poe-chip-active") as HTMLElement | null;
	const slot = (activeChip?.getAttribute("data-slot") || "").trim();
	const q = (document.querySelector("input[name='q']") as HTMLInputElement | null)?.value?.trim() || "";
	const params = new URLSearchParams();
	if (slot) params.set("slot", slot);
	if (q) params.set("q", q);
	const qs = params.toString();
	if (qs) {
		event.preventDefault();
		location.assign(tab.getAttribute("href") + "?" + qs);
	}
});

// [data-empty-widen-range]: 빈 상태 CTA — 같은 화면의 기간 프리셋 '전체' 버튼을 눌러 기간을 넓힌다
document.addEventListener("click", (event) => {
	const cta = (event.target as HTMLElement).closest?.(
		"[data-empty-widen-range]",
	);
	if (!cta) return;
	const allButton = document.querySelector<HTMLButtonElement>(
		'[data-picker-action="set"][data-picker-arg="0"]',
	);
	allButton?.click();
});

// HTMX beforeSwap 이벤트 처리
document.addEventListener("htmx:beforeSwap", (event: any) => {
	if ("hx-indicator" in event.target.attributes) {
		const indicator = document
			.querySelector(event.target.getAttribute("hx-indicator"))
			?.cloneNode(true) as HTMLElement;
		if (indicator) {
			indicator.style.display = "block";
			event.target.innerHTML = "";
			event.target.appendChild(indicator);
		}
	}
});

// 트리·아틀라스 툴바(details.poe-tools) 펼침을 뷰포트에 맞춘다.
// 데스크톱은 항상 펼쳐야 하는데, 닫힌 details 는 Chrome 이 내용을 아예 렌더하지 않아 CSS 로는 펼칠 수 없다.
// (사이클 302 의 CSS 트릭이 그래서 무효였고, 데스크톱 트리 컨트롤이 전부 사라져 있었다.)
// 모바일은 접힌 채로 두되 **사용자가 연 상태는 건드리지 않는다** — 브레이크포인트를 넘을 때만 동기화한다.
(() => {
	const mq = window.matchMedia("(min-width: 640px)");
	const apply = (desktop: boolean) => {
		document.querySelectorAll("details.poe-tools").forEach((d) => {
			if (desktop) d.setAttribute("open", "");
			else d.removeAttribute("open");
		});
	};
	const init = () => apply(mq.matches);
	if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
	else init();
	// change 는 브레이크포인트를 실제로 넘을 때만 발생 — 모바일에서 사용자가 연 툴바를 리사이즈마다 닫지 않는다.
	mq.addEventListener("change", (e) => apply(e.matches));
})();

// 가로로 넘치는 표 래퍼(.overflow-x-auto)는 키보드로도 스크롤할 수 있어야 한다(axe scrollable-region-focusable).
// 실제로 넘칠 때만 tabindex="0" 을 준다 - 데스크톱에서 넘치지 않는 표까지 탭 정지가 되면 안 된다.
// 실측 2026-09-09: 375px 에서 매매·시뮬레이터·종목 상세의 표 래퍼가 걸렸다. 창 크기·조각 교체 때마다 다시 잰다.
function syncScrollableFocus(root: ParentNode = document) {
	root.querySelectorAll<HTMLElement>(".overflow-x-auto").forEach((el) => {
		const scrolls = el.scrollWidth > el.clientWidth + 1;
		const managed = el.dataset.scrollFocus === "true";
		if (scrolls && !el.hasAttribute("tabindex")) {
			el.setAttribute("tabindex", "0");
			el.dataset.scrollFocus = "true";
		} else if (!scrolls && managed) {
			el.removeAttribute("tabindex");
			delete el.dataset.scrollFocus;
		}
	});
}
document.addEventListener("DOMContentLoaded", () => syncScrollableFocus());
document.addEventListener("htmx:afterSettle", () => syncScrollableFocus());
let scrollFocusTimer: ReturnType<typeof setTimeout> | undefined;
globalThis.addEventListener("resize", () => {
	clearTimeout(scrollFocusTimer);
	scrollFocusTimer = setTimeout(() => syncScrollableFocus(), 150);
});
(globalThis as any).__scrollFocusInternals = { syncScrollableFocus, applyPanelTabs, applyActivityView, restorePanelTabs };
(globalThis as any).__activityTabInternals = { linkActivityTabToPanel };

// 탭 패널 이름 붙이기(aria-labelledby=탭 id). 실측 2026-09-09: role=tabpanel 8개 중 aria-labelledby 0. 탭에 id 가 없는 그룹이 있어
// aria-controls 를 가진 탭마다 id 를 보장한다(없으면 'tab-for-' + 패널 id). 패널이 없으면 건너뛴다.
function labelTabPanels(root: ParentNode = document): number {
	let n = 0;
	root.querySelectorAll('[role="tab"][aria-controls]').forEach((tab) => {
		const panelId = tab.getAttribute("aria-controls");
		if (!panelId) return;
		const panel = document.getElementById(panelId);
		if (!panel || panel.getAttribute("role") !== "tabpanel") return;
		if (!tab.id) tab.id = "tab-for-" + panelId;
		// 여러 탭이 한 패널을 나눠 쓰면(도넛 지표/그룹) 선택된 탭이 이름이다.
		if (panel.getAttribute("aria-labelledby") === tab.id) return;
		if (tab.getAttribute("aria-selected") === "true" || !panel.hasAttribute("aria-labelledby")) {
			panel.setAttribute("aria-labelledby", tab.id);
			n++;
		}
	});
	return n;
}
document.addEventListener("DOMContentLoaded", () => labelTabPanels());
document.addEventListener("htmx:afterSettle", () => labelTabPanels());
document.addEventListener("click", (event) => {
	const el = event.target as Element | null;
	if (el && typeof el.closest === "function" && el.closest('[role="tab"]')) setTimeout(() => labelTabPanels(), 0);
});
(globalThis as any).__tabPanelLabelInternals = { labelTabPanels };

// 포커스로 열리는 드롭다운(.dropdown:focus-within)의 열림 상태를 트리거의 aria-expanded 로 알린다. 실측 2026-09-09: 네비바 로케일·프로필,
// 상세 '다른 종목/계좌' 트리거(22개)에 aria-expanded 가 없었고 Escape 로 닫히지도 않았다. 열림 = 컨테이너가 포커스를 품고 있음.
const DROPDOWN_TRIGGER = '.dropdown > [role="button"][aria-haspopup]';
function isDropdownOpen(dropdown: Element): boolean {
	if (dropdown.hasAttribute("open")) return true;
	const active = document.activeElement;
	return !!active && active !== document.body && dropdown.contains(active);
}
function syncDropdownExpanded(root: ParentNode = document): number {
	let changed = 0;
	root.querySelectorAll(DROPDOWN_TRIGGER).forEach((trigger) => {
		const dropdown = trigger.parentElement;
		if (!dropdown) return;
		const next = isDropdownOpen(dropdown) ? "true" : "false";
		if (trigger.getAttribute("aria-expanded") !== next) {
			trigger.setAttribute("aria-expanded", next);
			changed++;
		}
	});
	return changed;
}
// Escape: 포커스가 든 드롭다운을 닫는다. 트리거에 포커스를 되돌리면 focus-within 이 유지돼 다시 열리므로 blur 한다.
function closeDropdownOnEscape(active: unknown): boolean {
	if (!(active instanceof HTMLElement)) return false;
	const dropdown = active.closest(".dropdown");
	if (!dropdown || !dropdown.querySelector(DROPDOWN_TRIGGER)) return false;
	active.blur();
	dropdown.removeAttribute("open");
	syncDropdownExpanded();
	return true;
}
document.addEventListener("DOMContentLoaded", () => syncDropdownExpanded());
document.addEventListener("htmx:afterSettle", () => syncDropdownExpanded());
document.addEventListener("focusin", () => syncDropdownExpanded(), true);
document.addEventListener("focusout", () => setTimeout(() => syncDropdownExpanded(), 0), true);
document.addEventListener("keydown", (event) => {
	if (event.key === "Escape") closeDropdownOnEscape(document.activeElement);
});
// 트리거에서 Space/Enter: 기본 동작(Space 스크롤)을 막고 메뉴 첫 항목으로 들어간다. 실측 2026-09-10: Space 에 페이지가 437px 튀었다.
function enterDropdownMenu(trigger: unknown): boolean {
	if (!(trigger instanceof Element) || !trigger.matches(DROPDOWN_TRIGGER)) return false;
	const dropdown = trigger.parentElement;
	const first = dropdown && (dropdown.querySelector(".dropdown-content a[href], .dropdown-content button, .dropdown-content [tabindex]") as HTMLElement | null);
	if (!first) return false;
	first.focus();
	syncDropdownExpanded();
	return true;
}
document.addEventListener("keydown", (event) => {
	if (event.key !== " " && event.key !== "Enter") return;
	const target = event.target;
	if (!(target instanceof Element) || !target.matches(DROPDOWN_TRIGGER)) return;
	event.preventDefault();
	enterDropdownMenu(target);
});
(globalThis as any).__dropdownExpandedInternals = { isDropdownOpen, syncDropdownExpanded, closeDropdownOnEscape, enterDropdownMenu };

// 입력 도움말/미리보기를 컨트롤의 설명(aria-describedby)으로 잇는다. 실측 2026-09-10: .form-control 안의 .label-text-alt(형식화된 금액
// 미리보기)와 p.text-xs(안내 문구) 6개가 컨트롤과 연결돼 있지 않았다. 도움말에 id 가 없으면 컨트롤 id/name 으로 만든다.
const CONTROL_HELPER = ".label-text-alt, p.text-xs";
let describeSeq = 0;
function describeControls(root: ParentNode = document): number {
	let linked = 0;
	root.querySelectorAll<HTMLElement>("input:not([type=hidden]), select, textarea").forEach((control) => {
		const wrap = control.closest(".form-control");
		if (!wrap) return;
		const helpers = Array.from(wrap.querySelectorAll<HTMLElement>(CONTROL_HELPER)).filter(
			(h) => !h.contains(control) && (h.textContent || "").trim() !== "",
		);
		if (!helpers.length) return;
		const ids = helpers.map((h) => {
			if (!h.id) h.id = (control.id || control.getAttribute("name") || "control") + "-help-" + (++describeSeq);
			return h.id;
		});
		const current = (control.getAttribute("aria-describedby") || "").split(/\s+/).filter(Boolean);
		const missing = ids.filter((id) => !current.includes(id));
		if (!missing.length) return;
		control.setAttribute("aria-describedby", current.concat(missing).join(" "));
		linked++;
	});
	return linked;
}
document.addEventListener("DOMContentLoaded", () => describeControls());
document.addEventListener("htmx:afterSettle", () => describeControls());
(globalThis as any).__describeControlsInternals = { describeControls };

// 조각 교체 알림. 실측 2026-09-10: 기간 프리셋으로 목록이 통째로 바뀌어도 매매·활동·자산성장에서 live 영역 변화가 0 이라 화면 읽기
// 프로그램은 조회 결과가 바뀐 줄 몰랐다. #stockLiveStatus(role=status)에 '화면 이름: 내용 갱신됨' 을 넣는다 - 같은 문구를 다시
// 넣어도 읽히도록 먼저 비운다. 대상 이름은 조각의 aria-label, 없으면 문서 제목(접미사 앞부분).
function swapAnnouncement(target: Element, docTitle: string, template: string): string {
	const name = (target.getAttribute("aria-label") || docTitle.split(" \u00B7 ")[0] || "").trim();
	return template.replace("{0}", name);
}
// 사용자 조작 전(로드 때 불러오는 조각)의 교체는 알리지 않는다 - 실측: 종목 상세가 열리자마자 '갱신됨' 을 읽었다.
const swapAnnounceState = { userInteracted: false };
function noteUserInteraction(): void {
	swapAnnounceState.userInteracted = true;
}
["click", "keydown", "submit", "change"].forEach((type) => document.addEventListener(type, noteUserInteraction, true));
function announceSwap(target: unknown, root: ParentNode = document): boolean {
	if (!swapAnnounceState.userInteracted) return false;
	if (!(target instanceof Element) || !target.id || !target.id.endsWith("Fragment")) return false;
	const region = root.querySelector("#stockLiveStatus") as HTMLElement | null;
	if (!region) return false;
	const template = region.getAttribute("data-message-updated") || "{0}: updated";
	const text = swapAnnouncement(target, document.title || "", template);
	region.textContent = "";
	setTimeout(() => {
		region.textContent = text;
	}, 50);
	return true;
}
document.addEventListener("htmx:afterSettle", (event: any) => announceSwap(event.detail?.target));
(globalThis as any).__swapAnnounceInternals = { swapAnnouncement, announceSwap, noteUserInteraction, swapAnnounceState };
(globalThis as any).__calendarDayInternals = { toggleCalendarDay };





// role="tab" 의 선택 상태: 네 곳(활동 뷰·자산 성장 패널·매매/배당 도넛)이 tab-active 클래스(색)만 바꾼다.
// 실측 2026-09-09: 탭 13개 모두 aria-selected 가 없어 보조기술에는 어느 탭이 켜졌는지 없었다(WCAG 1.4.1).
// 클래스를 바꾸는 코드 네 곳을 각각 고치는 대신, class 속성 변화를 관찰해 한 곳에서 맞춘다 - 인라인 스크립트 쪽도 함께 덮인다.
function syncTabSelected(tab: Element) {
	tab.setAttribute("aria-selected", tab.classList.contains("tab-active") ? "true" : "false");
}
function syncAllTabs(root: ParentNode = document) {
	root.querySelectorAll('[role="tab"]').forEach(syncTabSelected);
}
new MutationObserver((records) => {
	for (const r of records) {
		const el = r.target as Element;
		if (el.getAttribute && el.getAttribute("role") === "tab") syncTabSelected(el);
	}
}).observe(document.documentElement, { attributes: true, attributeFilter: ["class"], subtree: true });
document.addEventListener("DOMContentLoaded", () => syncAllTabs());
document.addEventListener("htmx:afterSettle", () => syncAllTabs());

// 조각이 실어 온 화면 이름([data-page-title])으로 문서 제목을 맞춘다. 종목·계좌 상세의 껍데기는 이름을 모르고
// 빵부스러기 라벨로만 제목을 만든다(실측 2026-09-09: 종목 탭을 여러 개 열면 전부 "종목 상세 · Bluesky Stock").
function pageTitleFor(name: string | null | undefined, current: string): string {
	const trimmed = (name || "").trim();
	if (!trimmed) return current;
	return trimmed + " \u00B7 Bluesky Stock";
}
function applyFragmentTitle(root: ParentNode = document) {
	const el = root.querySelector("[data-page-title]") as HTMLElement | null;
	if (!el) return;
	document.title = pageTitleFor(el.getAttribute("data-page-title"), document.title);
}
document.addEventListener("htmx:afterSettle", () => applyFragmentTitle());
(globalThis as any).__pageTitleInternals = { pageTitleFor, applyFragmentTitle };

// 장식용 인라인 svg 는 보조기술에서 숨긴다. 실측 2026-09-09: 13화면의 svg 265개 중 aria-hidden 이 0개라 화면 읽기
// 프로그램이 아이콘마다 '그래픽' 을 읽었다. 이름(aria-label/role/title)이 있는 svg 는 의미 있는 것이니 손대지 않는다.
// 아이콘만 있는 버튼·링크의 이름은 그 요소의 aria-label 이 맡는다(axe button-name/link-name 이 지킨다).
function isDecorativeSvg(svg: Element): boolean {
	if (svg.hasAttribute("aria-hidden") || svg.hasAttribute("aria-label") || svg.hasAttribute("aria-labelledby")) return false;
	if (svg.hasAttribute("role")) return false;
	return !svg.querySelector("title");
}
function hideDecorativeSvgs(root: ParentNode = document): number {
	let n = 0;
	root.querySelectorAll("svg").forEach((svg) => {
		if (isDecorativeSvg(svg)) {
			svg.setAttribute("aria-hidden", "true");
			n++;
		}
	});
	return n;
}
document.addEventListener("DOMContentLoaded", () => hideDecorativeSvgs());
document.addEventListener("htmx:afterSettle", () => hideDecorativeSvgs());
(globalThis as any).__decorativeSvgInternals = { isDecorativeSvg, hideDecorativeSvgs };

// 툴팁을 Escape 로 닫는다(WCAG 1.4.13 dismissable). 실측 2026-09-09: 4/4 툴팁이 Escape 에 안 닫혔다.
// 열린 것(hover 또는 포커스)에 tooltip-dismissed 를 붙이면 CSS 가 숨기고, 포인터가 떠나거나 포커스가 빠지면 되돌려 다음엔 다시 열린다.
// 포커스는 옮기지 않는다(닫기만). 다른 Escape 처리(overlay 등)와는 독립이다.
const TOOLTIP_DISMISSED = "tooltip-dismissed";
function dismissOpenTooltips(root: ParentNode = document): number {
	let n = 0;
	root.querySelectorAll(".tooltip:hover, .tooltip:focus-visible").forEach((el) => {
		if (!el.classList.contains(TOOLTIP_DISMISSED)) {
			el.classList.add(TOOLTIP_DISMISSED);
			n++;
		}
	});
	return n;
}
function restoreTooltip(target: EventTarget | null): boolean {
	if (!(target instanceof Element) || !target.classList.contains("tooltip")) return false;
	target.classList.remove(TOOLTIP_DISMISSED);
	return true;
}
document.addEventListener("keydown", (event) => {
	if (event.key === "Escape") dismissOpenTooltips();
});
// mouseleave/focusout 은 버블링하지 않으므로 캡처로 받는다.
document.addEventListener("mouseleave", (event) => restoreTooltip(event.target), true);
document.addEventListener("focusout", (event) => restoreTooltip(event.target), true);
(globalThis as any).__tooltipInternals = { dismissOpenTooltips, restoreTooltip, TOOLTIP_DISMISSED };

// htmx 가 걷어내는 요소 안의 canvas 에 붙은 Chart.js 인스턴스를 파괴한다. 실측 2026-09-09: 자산성장·배당 화면에서 기간 프리셋을
// 10회 바꾸자 DOM 에 없는 canvas 를 쥔 인스턴스 20개(교체마다 2개)가 Chart.instances 에 남았다 - 조각 초기화 클로저가 매번 새로
// 만들어져 이전 인스턴스를 모르기 때문. 인스턴스마다 resize 관찰자·애니메이션 프레임이 살아 있으니 걷어낼 때 함께 정리한다.
// htmx:beforeCleanupElement 는 제거되는 요소와 그 자식마다 한 번씩 버블링으로 온다.
function destroyChartsIn(target: EventTarget | null, chartLib: any = (globalThis as any).Chart): number {
	if (!chartLib || typeof chartLib.getChart !== "function" || !(target instanceof Element)) return 0;
	const canvases: Element[] = target.tagName === "CANVAS" ? [target] : Array.from(target.querySelectorAll("canvas"));
	let n = 0;
	for (const canvas of canvases) {
		const chart = chartLib.getChart(canvas);
		if (chart && typeof chart.destroy === "function") {
			try {
				chart.destroy();
				n++;
			} catch (e) {}
		}
	}
	return n;
}
document.addEventListener("htmx:beforeCleanupElement", (event) => destroyChartsIn(event.target));
(globalThis as any).__chartCleanupInternals = { destroyChartsIn };

// htmx 조각 요청 중인 대상 영역에 aria-busy 를 건다(WAI-ARIA 갱신 중 표시). 실측 2026-09-09: htmx 는 요청 요소에 htmx-request 클래스만
// 붙이고 hx-target 에는 아무 표시가 없어 4/4 화면에서 보조기술이 갱신 중임을 알 수 없었다. 정착(afterSettle) 또는 실패(afterRequest 의
// successful=false) 때 지운다. outerHTML 교체면 옛 요소와 함께 사라지므로 따로 처리하지 않는다.
function markBusy(target: unknown): boolean {
	if (!(target instanceof Element)) return false;
	target.setAttribute("aria-busy", "true");
	return true;
}
function clearBusy(target: unknown): boolean {
	if (!(target instanceof Element) || !target.hasAttribute("aria-busy")) return false;
	target.removeAttribute("aria-busy");
	return true;
}
document.addEventListener("htmx:beforeRequest", (event: any) => markBusy(event.detail?.target));
document.addEventListener("htmx:afterSettle", (event: any) => clearBusy(event.detail?.target));
document.addEventListener("htmx:afterRequest", (event: any) => {
	if (event.detail && event.detail.successful === false) clearBusy(event.detail.target);
});
(globalThis as any).__ariaBusyInternals = { markBusy, clearBusy };

// 보유 스냅샷 포커스 흐름. 실측 2026-09-09: 조회(Enter) 뒤 포커스가 날짜 입력에 남아 새 카드가 안내되지 않았고(카드까지 Tab 5회),
// 닫기 버튼으로 닫으면 포커스가 body 로 떨어졌다. 정착하면 카드 제목(h3, tabindex=-1)으로, 닫으면 폼의 날짜 입력으로 옮긴다.
// 차트 점 클릭(htmx.ajax) 경로도 같은 정착 이벤트라 카드가 화면 아래에 있어도 함께 드러난다.
const SNAPSHOT_CONTAINER_ID = "holdings-snapshot-container";
function focusSnapshotHeading(target: unknown): boolean {
	if (!(target instanceof Element) || target.id !== SNAPSHOT_CONTAINER_ID) return false;
	const heading = target.querySelector("h3") as HTMLElement | null;
	if (!heading) return false;
	if (!heading.hasAttribute("tabindex")) heading.setAttribute("tabindex", "-1");
	heading.focus();
	return true;
}
// 우선순위 순서로 하나씩 찾는다. 셀렉터를 쉼표로 합치면 문서 순서의 첫 요소가 잡혀 자산성장에서는 폼보다 앞에 있는
// canvas(포커스 불가)가 걸렸다(실측 2026-09-09: 복귀 호출은 됐는데 activeElement 가 그대로).
const SNAPSHOT_FOCUS_RETURN_TARGETS = [
	"[data-holdings-snapshot-form] input[type=date]",
	"[data-holdings-snapshot-form] button",
	"canvas#assetGrowthChart",
	"canvas#accountDetailChart",
];
function restoreSnapshotFormFocus(root: ParentNode = document): boolean {
	for (const selector of SNAPSHOT_FOCUS_RETURN_TARGETS) {
		const target = root.querySelector(selector) as HTMLElement | null;
		if (!target) continue;
		if (target.tagName === "CANVAS" && !target.hasAttribute("tabindex")) target.setAttribute("tabindex", "-1");
		target.focus();
		return true;
	}
	return false;
}
document.addEventListener("htmx:afterSettle", (event: any) => focusSnapshotHeading(event.detail?.target));
document.addEventListener("click", (event) => {
	const el = event.target as Element | null;
	if (el && typeof el.closest === "function" && el.closest("[data-holdings-snapshot-close]")) restoreSnapshotFormFocus();
});
(globalThis as any).__snapshotFocusInternals = { focusSnapshotHeading, restoreSnapshotFormFocus };

// 조각 교체 뒤 포커스 복원. 실측 2026-09-09: 정렬 헤더(배당·매매 th[hx-get])와 활동 '조회' 버튼을 키보드로 누르면 조각이 outerHTML 로
// 통째로 바뀌며 포커스가 body 로 떨어졌다(교체를 일으킨 컨트롤 5/5). htmx 는 id 있는 요소만 되살리는데 이 컨트롤들엔 id 가 없다.
// 교체 전 활성 요소의 id, 없으면 교체 대상 기준 구조 경로(tag:nth-child 사슬)를 기억해 정착 뒤 같은 자리를 포커스한다.
// (정렬 헤더의 hx-vals 는 방향이 뒤집혀 렌더마다 달라지므로 키로 쓸 수 없다.) 이미 다른 코드가 포커스를 옮겼으면 건드리지 않는다.
type FocusMemo = { id: string | null; path: string | null; targetId: string };
const NATIVELY_FOCUSABLE = new Set(["A", "BUTTON", "INPUT", "SELECT", "TEXTAREA", "SUMMARY"]);
function structuralPath(el: Element, root: Element): string | null {
	const parts: string[] = [];
	let cur: Element | null = el;
	while (cur && cur !== root) {
		const parent: Element | null = cur.parentElement;
		if (!parent) return null;
		const idx = Array.prototype.indexOf.call(parent.children, cur) + 1;
		parts.unshift(cur.tagName.toLowerCase() + ":nth-child(" + idx + ")");
		cur = parent;
	}
	return cur === root && parts.length ? ":scope > " + parts.join(" > ") : null;
}
function rememberFocus(target: unknown, active: unknown = document.activeElement): FocusMemo | null {
	if (!(target instanceof Element) || !(active instanceof Element) || active === document.body) return null;
	if (active !== target && !target.contains(active)) return null;
	return { id: active.id || null, path: structuralPath(active, target), targetId: target.id || "" };
}
function restoreFocus(memo: FocusMemo | null, container: Element | null, active: unknown = document.activeElement): boolean {
	if (!memo) return false;
	if (active instanceof Element && active !== document.body) return false;
	let el: Element | null = memo.id ? document.getElementById(memo.id) : null;
	if (!el && memo.path && container) {
		try {
			el = container.querySelector(memo.path);
		} catch (e) {
			el = null;
		}
	}
	if (!el || typeof (el as HTMLElement).focus !== "function") return false;
	if (!NATIVELY_FOCUSABLE.has(el.tagName) && !el.hasAttribute("tabindex")) el.setAttribute("tabindex", "-1");
	(el as HTMLElement).focus({ preventScroll: true });
	return true;
}
const swapFocusMemos = new Map<string, FocusMemo>();
document.addEventListener("htmx:beforeSwap", (event: any) => {
	const memo = rememberFocus(event.detail?.target);
	if (memo) swapFocusMemos.set(memo.targetId, memo);
});
document.addEventListener("htmx:afterSettle", (event: any) => {
	const target = event.detail?.target;
	const key = target instanceof Element ? target.id || "" : "";
	const memo = swapFocusMemos.get(key) || null;
	if (!memo) return;
	swapFocusMemos.delete(key);
	const container = (memo.targetId && document.getElementById(memo.targetId)) || (target instanceof Element ? target : null);
	restoreFocus(memo, container);
});
(globalThis as any).__swapFocusInternals = { structuralPath, rememberFocus, restoreFocus };






// htmx 트리거를 <th> 같은 비대화형 요소에 단 곳(배당 목록 정렬 머리 7개, 실측 2026-09-09)은 tabindex 를 줘도 Enter 가 통하지 않는다.
// 버튼/링크가 아닌 포커스 가능한 hx-get 요소에서 Enter/Space 를 click 으로 바꾼다.
document.addEventListener("keydown", (event) => {
	if (event.key !== "Enter" && event.key !== " ") return;
	const target = event.target as HTMLElement;
	if (!target || !target.matches) return;
	if (!target.matches("[hx-get][tabindex]:not(a):not(button):not(input):not(select):not(textarea)")) return;
	event.preventDefault();
	target.click();
});

// 뒤로가기 스크롤 복원. 실측 2026-09-10(qa/back-nav.cjs): 목록 4화면에서 300px 내려간 뒤 상세로 갔다 Back 하면 81px 에 멈춘다 -
// 첫 HTML 은 스켈레톤(881px)이라 브라우저 복원이 그 높이에 잘리고, htmx 조각이 채운 뒤에는 다시 시도하지 않는다.
// 떠날 때(pagehide) URL 별로 scrollY 를 sessionStorage 에 적고, back_forward 진입이면 조각이 정착돼 높이가 충분해질 때 한 번 복원한다.
const SCROLL_MEMO_PREFIX = "scrollY:";
function scrollMemoKey(loc: { pathname: string; search: string } = globalThis.location): string {
	return SCROLL_MEMO_PREFIX + loc.pathname + loc.search;
}
function saveScrollMemo(storage: Pick<Storage, "setItem" | "removeItem"> = globalThis.sessionStorage, y: number = globalThis.scrollY, key: string = scrollMemoKey()): void {
	try {
		if (y > 0) storage.setItem(key, String(Math.round(y)));
		else storage.removeItem(key);
	} catch (e) {
		// 저장소를 못 쓰는 환경(프라이빗 모드 등)이면 복원만 포기한다.
	}
}
function readScrollMemo(storage: Pick<Storage, "getItem"> = globalThis.sessionStorage, key: string = scrollMemoKey()): number {
	try {
		const value = Number(storage.getItem(key));
		return Number.isFinite(value) && value > 0 ? value : 0;
	} catch (e) {
		return 0;
	}
}
function isBackForwardNavigation(perf: { getEntriesByType?: (type: string) => any[] } | undefined = globalThis.performance): boolean {
	const entries = perf?.getEntriesByType?.("navigation") || [];
	return entries.length > 0 && entries[0]?.type === "back_forward";
}
/** 저장 위치까지 스크롤할 수 있는 높이면 복원하고 true(끝). 사용자가 이미 더 내려갔으면 건드리지 않고 true. 아직 짧으면 false(다음 정착 때 재시도). */
function restoreScrollIfTall(saved: number, doc: { scrollHeight: number } = document.documentElement, viewport: number = globalThis.innerHeight, current: number = globalThis.scrollY, scroll: (x: number, y: number) => void = (x, y) => globalThis.scrollTo(x, y)): boolean {
	if (saved <= 0) return true;
	if (current > saved) return true;
	if (doc.scrollHeight - viewport < saved) return false;
	scroll(0, saved);
	return true;
}
globalThis.addEventListener("pagehide", () => saveScrollMemo());
if (isBackForwardNavigation()) {
	const savedScrollY = readScrollMemo();
	if (!restoreScrollIfTall(savedScrollY)) {
		const onSettle = () => {
			if (restoreScrollIfTall(savedScrollY)) document.removeEventListener("htmx:afterSettle", onSettle);
		};
		document.addEventListener("htmx:afterSettle", onSettle);
		setTimeout(() => document.removeEventListener("htmx:afterSettle", onSettle), 10000);
	}
}
(globalThis as any).__scrollRestoreInternals = { scrollMemoKey, saveScrollMemo, readScrollMemo, isBackForwardNavigation, restoreScrollIfTall };

// 차트 텍스트 대안(WCAG 1.1.1). 실측 2026-09-10(qa/chart-alt.cjs): 캔버스 15개 중 13개가 같은 카드 안에 표·목록이 없어
// 보조기술엔 aria-label(제목)만 닿았다. Chart.js 전역 플러그인으로 차트마다 sr-only 요약을 캔버스 뒤에 두고 aria-describedby 로 잇는다.
// 선/막대: 데이터셋별 지점 수·처음·끝·최고·최저(라벨 포함). 도넛/파이: 항목별 값과 비중. 갱신(afterUpdate)마다 다시 쓴다.
// 정의는 여기(모든 화면), 등록은 stock-charts.ts 와 시뮬레이터 두 모듈(chart.umd 를 직접 로드)에서 한다.
const CHART_SUMMARY_MAX_ITEMS = 10;
const CHART_SUMMARY_MAX_DATASETS = 4;
function chartSummaryLocale(lang: string): string {
	return (lang || "").toLowerCase().startsWith("ko") ? "ko-KR" : "en-US";
}
function chartSummaryLabel(raw: unknown, locale: string): string {
	if (raw instanceof Date) return raw.toLocaleDateString(locale);
	if (typeof raw === "number" && raw > 1e11) return new Date(raw).toLocaleDateString(locale);
	if (raw == null) return "";
	return String(raw);
}
function chartSummaryValue(point: unknown): number | null {
	const v = point !== null && typeof point === "object" ? (point as any).y : point;
	const n = typeof v === "string" ? Number(v) : v;
	return typeof n === "number" && Number.isFinite(n) ? n : null;
}
function chartSummaryText(chart: any, lang: string = document.documentElement.lang): string {
	const locale = chartSummaryLocale(lang);
	const ko = locale === "ko-KR";
	const fmt = new Intl.NumberFormat(locale, { maximumFractionDigits: 2 });
	const type = chart?.config?.type || chart?.config?._config?.type || "";
	const labels: unknown[] = chart?.data?.labels || [];
	const datasets: any[] = (chart?.data?.datasets || []).filter((ds: any, i: number) => ds && Array.isArray(ds.data) && ds.data.length && (typeof chart.isDatasetVisible !== "function" || chart.isDatasetVisible(i)));
	if (!datasets.length) return "";
	if (type === "doughnut" || type === "pie" || type === "polarArea") {
		const values = datasets[0].data.map(chartSummaryValue);
		const total = values.reduce((a: number, v: number | null) => a + (v || 0), 0);
		const parts = values.slice(0, CHART_SUMMARY_MAX_ITEMS).map((v: number | null, i: number) => {
			const share = total > 0 && v != null ? Math.round((v / total) * 1000) / 10 : 0;
			return chartSummaryLabel(labels[i], locale) + " " + fmt.format(v || 0) + " (" + share + "%)";
		});
		const rest = values.length - parts.length;
		if (rest > 0) parts.push(ko ? "외 " + rest + "개" : "and " + rest + " more");
		return (ko ? "항목 " + values.length + "개: " : values.length + " items: ") + parts.join(", ");
	}
	return datasets.slice(0, CHART_SUMMARY_MAX_DATASETS).map((ds: any) => {
		const pts = ds.data.map((point: unknown, i: number) => ({ v: chartSummaryValue(point), l: chartSummaryLabel(labels[i] ?? (point && typeof point === "object" ? (point as any).x : undefined), locale) })).filter((x: any) => x.v != null);
		if (!pts.length) return "";
		let hi = pts[0], lo = pts[0];
		for (const x of pts) { if (x.v > hi.v) hi = x; if (x.v < lo.v) lo = x; }
		const first = pts[0], last = pts[pts.length - 1];
		const name = ds.label ? String(ds.label) + ": " : "";
		return ko
			? name + pts.length + "개 지점, 처음 " + first.l + " " + fmt.format(first.v) + ", 끝 " + last.l + " " + fmt.format(last.v) + ", 최고 " + fmt.format(hi.v) + " (" + hi.l + "), 최저 " + fmt.format(lo.v) + " (" + lo.l + ")"
			: name + pts.length + " points, first " + first.l + " " + fmt.format(first.v) + ", last " + last.l + " " + fmt.format(last.v) + ", high " + fmt.format(hi.v) + " (" + hi.l + "), low " + fmt.format(lo.v) + " (" + lo.l + ")";
	}).filter(Boolean).join(". ");
}
/** 캔버스 바로 뒤에 sr-only 요약을 두고(없으면 만들고) aria-describedby 로 잇는다. 요약이 비면 둘 다 거둔다. */
function syncChartSummary(chart: any): HTMLElement | null {
	const canvas = chart?.canvas as HTMLElement | undefined;
	const parent = canvas?.parentElement;
	if (!canvas || !parent) return null;
	const id = (canvas.id || "chart-" + (chart.id ?? "x")) + "-summary";
	const text = chartSummaryText(chart);
	let el = document.getElementById(id);
	if (!text) {
		if (el) el.remove();
		canvas.removeAttribute("aria-describedby");
		return null;
	}
	if (!el) {
		el = document.createElement("p");
		el.id = id;
		el.className = "sr-only";
		el.setAttribute("data-chart-summary", "");
		parent.insertBefore(el, canvas.nextSibling);
	}
	if (el.textContent !== text) el.textContent = text;
	canvas.setAttribute("aria-describedby", id);
	return el;
}
const chartSummaryPlugin = {
	id: "a11ySummary",
	afterInit: (chart: any) => { syncChartSummary(chart); },
	afterUpdate: (chart: any) => { syncChartSummary(chart); },
};
(globalThis as any).__chartSummaryInternals = { chartSummaryText, syncChartSummary, chartSummaryPlugin };

// 한국어 데이터 표시(WCAG 3.1.2 Language of Parts). 실측 2026-09-10(qa/lang-of-parts.cjs): 영어 화면(문서 lang=en-US) 12개에
// 한글 텍스트 1,190곳(종목명·계좌명 등)이 lang 표시 없이 있었다 - 보조기술이 영어 음성으로 읽는다. 화면 문구가 아니라
// 데이터라 로케일을 바꿔도 한국어이므로, 한글을 직접 품은 요소에 lang="ko" 를 붙인다(요소에 붙이면 그 안의 aria-label·title 도 따라온다).
const HANGUL_PATTERN = /[\uAC00-\uD7A3]/;
function isKoreanDocument(lang: string = document.documentElement.lang): boolean {
	return (lang || "").toLowerCase().startsWith("ko");
}
/** 조상 중 가장 가까운 lang 이 한국어인지. 이미 표시된 가지는 다시 건드리지 않는다. */
function hasKoreanLangAncestor(el: Element | null): boolean {
	for (let node: Element | null = el; node; node = node.parentElement) {
		const lang = node.getAttribute("lang");
		if (lang) return lang.toLowerCase().startsWith("ko");
	}
	return false;
}
/** root 안에서 한글을 직접 품은 요소에 lang="ko" 를 붙이고, 새로 붙인 개수를 돌려준다. */
function markKoreanParts(root: ParentNode | Document = document, docLang?: string): number {
	if (isKoreanDocument(docLang)) return 0;
	const scope: Node = root instanceof Document ? root.body || root : (root as Node);
	if (!scope || typeof document.createTreeWalker !== "function") return 0;
	const walker = document.createTreeWalker(scope, NodeFilter.SHOW_TEXT);
	let marked = 0;
	let node: Node | null;
	while ((node = walker.nextNode())) {
		const text = node.textContent || "";
		if (!HANGUL_PATTERN.test(text)) continue;
		const el = node.parentElement;
		if (!el || el.tagName === "SCRIPT" || el.tagName === "STYLE") continue;
		if (el.hasAttribute("lang") || hasKoreanLangAncestor(el)) continue;
		el.setAttribute("lang", "ko");
		marked++;
	}
	return marked;
}
// 실측 2026-09-10: common.js 는 <head> 에서 동기 로드되므로 이 시점의 body 는 비어 있다(초기 호출이 0 을 표시했다).
// 또 outerHTML 교체에서는 afterSettle 의 target 이 떨어져 나간 옛 요소라 그 가지를 훑어도 화면에 반영되지 않는다
// (실측: 매매·자산성장은 0곳, 내부 교체를 쓰는 자산 현황만 41곳 표시됐다). 그래서 항상 문서 전체를 훑는다 -
// 이미 표시된 요소는 건너뛰므로 반복 호출이 싸다(한글 320곳 화면에서도 한 번 훑고 0 을 돌려준다).
function markKoreanPartsWhenReady(): void {
	// 두 갈래 모두 훑기 + 관찰자를 함께 시작해야 한다. 실측 2026-09-10: 관찰자를 "이미 로드됨" 갈래에만 두었더니 실제 화면(head 로드 시 readyState="loading")에서는 관찰자가 생기지 않아
	// 교체본이 통째로 표시되지 않았다(매매 319곳).
	const run = () => {
		markKoreanParts();
		observeKoreanParts();
	};
	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", run, { once: true });
		return;
	}
	run();
}
markKoreanPartsWhenReady();
// htmx 교체뿐 아니라 스크립트가 나중에 만드는 DOM(도넛 범례, 차트 요약 문단)도 있다 - 실측 2026-09-10: afterSettle 만으로는
// 배당 11곳·매매 9곳이 남았고 나중에 다시 훑으면 전부 잡혔다. 더해지는 가지만 훑는 관찰자로 시점 문제를 없앤다.
function observeKoreanParts(): MutationObserver | null {
	if (isKoreanDocument() || typeof MutationObserver !== "function" || !document.body) return null;
	const observer = new MutationObserver((mutations) => {
		for (const mutation of mutations) {
			mutation.addedNodes.forEach((added) => {
				if (added instanceof Element) markKoreanParts(added);
			});
		}
	});
	observer.observe(document.body, { childList: true, subtree: true });
	return observer;
}
(globalThis as any).__langPartsInternals = { isKoreanDocument, hasKoreanLangAncestor, markKoreanParts, observeKoreanParts, markKoreanPartsWhenReady };

// ---- 선택 가능한 표의 행 체크박스 ----
// 선택을 tr 의 aria-selected 로만 표시하면 보조기술에 아무것도 전달되지 않는다: 순수 table 안의 row 는 그 속성을
// 접근성 트리에 내보내지 않는다(실측 2026-09-10 qa/selectable-rows-a11y.cjs - 자산 현황 종목 9행·계좌 5행,
// 배당 수익률 14행, 매매 실현손익 8행 모두 선택 뒤 속성이 focusable/focused 뿐이었다).
//
// 표마다 선택 코드가 따로 있어(assetStatus.ts 2곳, dividendHistory.ts, 조각 안 인라인 2곳) 각자 고치면 같은 코드가
// 다섯 벌 늘어난다. 그래서 표 코드는 건드리지 않고 여기서 한 번만 처리한다:
//   1) data-row-select 를 단 행의 첫 칸에 진짜 체크박스를 만들어 넣는다(이름은 그 행 첫 칸 글자에서 만든다).
//   2) 체크박스가 바뀌면 행을 클릭한 것과 같게 만든다 - 기존 선택 코드가 그대로 돈다.
//   3) 행의 aria-selected 가 바뀌면(행 클릭·전체 해제 등) 체크박스를 따라가게 한다.
const ROW_SELECT_ROW = "[data-row-select]";
const ROW_SELECT_BOX = "[data-row-select-checkbox]";

/**
 * 행을 알아볼 이름. 첫 칸 글자를 한 줄로 눌러 쓴다.
 *
 * 첫 칸에는 종목·계좌 이름 말고도 펼침 버튼("보유 종목 보기 (3)")이나 끌기 손잡이("::"), 정렬 번호가 같이 있다
 * - 실측 2026-09-10: 그대로 쓰면 이름이 "KB증권 위탁 보유 종목 보기 (3) 선택", ":: 1 476800 ..." 이 됐다.
 * 사본에서 그런 것들을 걷어내고 읽는다(원본은 건드리지 않는다). 자르는 것은 이름이지 템플릿이 아니다.
 */
function rowSelectName(row: Element, template: string): string {
	const cell = row.querySelector("td, th");
	if (!cell) return template.replace("{0}", "").trim();
	const copy = cell.cloneNode(true) as Element;
	const noise = copy.querySelectorAll('button, [data-profile-order-handle], [data-profile-order-value], [aria-hidden="true"]');
	for (let i = 0; i < noise.length; i++) noise[i].remove();
	const text = (copy.textContent || "").replace(/\s+/g, " ").trim().slice(0, 40);
	if (template.indexOf("{0}") < 0) return template;
	return template.replace("{0}", text).trim();
}

function isRowSelected(row: Element): boolean {
	return row.getAttribute("aria-selected") === "true";
}

/** 한 행에 체크박스를 붙인다(이미 있으면 상태만 맞춘다). 붙였으면 true. */
function ensureRowCheckbox(row: Element): boolean {
	const existing = row.querySelector<HTMLInputElement>(ROW_SELECT_BOX);
	if (existing) {
		existing.checked = isRowSelected(row);
		return false;
	}
	const cell = row.querySelector("td, th");
	if (!cell) return false;
	const template = row.getAttribute("data-row-select") || "{0}";
	const label = document.createElement("label");
	label.className = "row-select-box";
	const box = document.createElement("input");
	box.type = "checkbox";
	box.className = "checkbox checkbox-sm";
	box.setAttribute("data-row-select-checkbox", "");
	box.setAttribute("aria-label", rowSelectName(row, template));
	box.checked = isRowSelected(row);
	label.appendChild(box);
	cell.insertBefore(label, cell.firstChild);
	// 행이 스스로 탭 정지였다면 이제 체크박스가 그 몫을 한다 - 행마다 탭 정지가 둘이 되지 않게 뗀다.
	if (row.getAttribute("tabindex") === "0") row.removeAttribute("tabindex");
	return true;
}

/** 문서(또는 주어진 가지)의 선택 가능한 행에 체크박스를 붙인다. 새로 붙인 수를 돌려준다. */
function markRowSelectCheckboxes(root: ParentNode = document): number {
	let added = 0;
	const rows = root.querySelectorAll(ROW_SELECT_ROW);
	for (let i = 0; i < rows.length; i++) {
		if (ensureRowCheckbox(rows[i])) added++;
	}
	// instanceof Element 은 Element 를 안 만든 테스트 스텁에서 터진다 - 있는지로 본다.
	const self = root as unknown as Element;
	if (typeof self.matches === "function" && self.matches(ROW_SELECT_ROW) && ensureRowCheckbox(self)) added++;
	return added;
}

/** 체크박스와 행의 선택 상태를 맞춘다. 어긋나면 행을 클릭해 기존 선택 코드를 그대로 태운다. */
function reconcileRowSelection(box: HTMLInputElement): void {
	const row = box.closest<HTMLElement>(ROW_SELECT_ROW);
	if (!row) return;
	if (isRowSelected(row) === box.checked) return;
	row.click();
	// 행 코드가 선택을 거부했을 수도 있다 - 화면이 진실이므로 되맞춘다.
	box.checked = isRowSelected(row);
}

function observeRowSelection(): MutationObserver | null {
	if (typeof MutationObserver !== "function" || !document.body) return null;
	const observer = new MutationObserver((mutations) => {
		for (const mutation of mutations) {
			if (mutation.type === "attributes") {
				const target = mutation.target as unknown as Element;
				if (target && typeof target.matches === "function" && target.matches(ROW_SELECT_ROW)) {
					const box = target.querySelector<HTMLInputElement>(ROW_SELECT_BOX);
					if (box) box.checked = isRowSelected(target);
				}
				continue;
			}
			mutation.addedNodes.forEach((added) => {
				const element = added as unknown as Element;
				if (element && typeof element.querySelectorAll === "function") markRowSelectCheckboxes(element);
			});
		}
	});
	observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ["aria-selected"] });
	return observer;
}

function startRowSelection(): void {
	const run = () => {
		markRowSelectCheckboxes();
		observeRowSelection();
		document.addEventListener("change", (event) => {
			const target = event.target;
			if (target instanceof HTMLInputElement && target.matches(ROW_SELECT_BOX)) reconcileRowSelection(target);
		});
	};
	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", run, { once: true });
		return;
	}
	run();
}
startRowSelection();
(globalThis as any).__rowSelectInternals = { rowSelectName, ensureRowCheckbox, markRowSelectCheckboxes, reconcileRowSelection, observeRowSelection };

// ---- 화면 안 구역 막대 ----
// 긴 화면에는 화면 안에서 구역으로 뛸 방법이 없었다 - 실측 2026-09-10(qa/ui-nav.cjs): 배당 7.2화면·활동 5.5화면·
// 매매 5.4화면·시뮬레이터 4.4화면인데 화면 안 이동 링크는 건너뛰기 링크 1개뿐이었고, 제목에 id 가 붙은 것도 0개였다.
// 배당 화면은 마지막 구역(상세 목록)이 3,185px 아래에 있어 거기까지 굴려야만 닿았다.
//
// 구역은 화면이 data-page-section 으로 알려 준다(값을 주면 그 값이 이름, 안 주면 그 요소의 글자가 이름).
// 구역이 3개보다 적으면 막대를 만들지 않는다 - 짧은 화면에서는 자리만 차지한다.
const SECTION_MARK = "[data-page-section]";
const SECTION_NAV_MIN = 3;
// 짧은 화면에서는 막대가 자리만 차지한다 - 두 화면을 넘게 굴려야 하는 화면에만 만든다
// (실측 2026-09-10: 자산 현황은 1,836~1,904px 라 2배로 잡으면 창 높이에 따라 막대가 생겼다 사라졌다 한다).
const SECTION_NAV_MIN_SCROLL = 2.5;

function sectionLabel(element: Element): string {
	const given = element.getAttribute("data-page-section");
	if (given && given.trim()) return given.trim();
	const text = (element.textContent || "").replace(/\s+/g, " ").trim();
	return text.slice(0, 24);
}

/** 화면이 알려 준 구역들. 보이는 것만, 문서 순서대로. */
function pageSections(root: ParentNode = document): Element[] {
	const found: Element[] = [];
	const marked = root.querySelectorAll(SECTION_MARK);
	for (let i = 0; i < marked.length; i++) {
		const element = marked[i] as HTMLElement;
		if (element.offsetParent === null && element.getClientRects().length === 0) continue;
		if (!sectionLabel(element)) continue;
		found.push(element);
	}
	return found;
}

/** 뛰어갈 곳이 되려면 id 가 있어야 한다. 없으면 붙인다. */
function sectionAnchorId(element: Element, index: number): string {
	if (element.id) return element.id;
	const id = "page-section-" + (index + 1);
	element.id = id;
	return id;
}

function buildSectionNav(sections: Element[], label: string): HTMLElement | null {
	if (sections.length < SECTION_NAV_MIN) return null;
	const nav = document.createElement("nav");
	nav.className = "page-section-nav";
	nav.setAttribute("data-page-section-nav", "");
	nav.setAttribute("aria-label", label);
	const list = document.createElement("ul");
	for (let i = 0; i < sections.length; i++) {
		const item = document.createElement("li");
		const link = document.createElement("a");
		link.href = "#" + sectionAnchorId(sections[i], i);
		link.textContent = sectionLabel(sections[i]);
		item.appendChild(link);
		list.appendChild(item);
	}
	nav.appendChild(list);
	return nav;
}

/** 지금 보고 있는 구역을 막대에서 표시한다. */
function markCurrentSection(nav: HTMLElement, sections: Element[]): void {
	const links = nav.querySelectorAll("a");
	let current = 0;
	for (let i = 0; i < sections.length; i++) {
		const top = sections[i].getBoundingClientRect().top;
		if (top - 120 <= 0) current = i;
	}
	for (let i = 0; i < links.length; i++) {
		const link = links[i];
		const on = i === current;
		link.classList.toggle("is-current", on);
		if (on) link.setAttribute("aria-current", "true");
		else link.removeAttribute("aria-current");
	}
	// 칩이 화면보다 넓으면 지금 보고 있는 칩이 밖으로 밀린다 - 실측 2026-09-10(375px, 배당):
	// 현재 칩이 폭 343px 짜리 막대의 764px 지점에 있어 끝까지 보이지 않았다. 막대만 옆으로 굴린다
	// (scrollIntoView 는 페이지까지 움직인다).
	const list = nav.querySelector("ul");
	const currentLink = links[current] as HTMLElement | undefined;
	if (list && currentLink && list.scrollWidth > list.clientWidth) {
		const left = currentLink.offsetLeft;
		const right = left + currentLink.offsetWidth;
		const margin = 16;
		if (left < list.scrollLeft + margin) list.scrollLeft = Math.max(0, left - margin);
		else if (right > list.scrollLeft + list.clientWidth - margin)
			list.scrollLeft = right - list.clientWidth + margin;
	}
}

/**
 * 구역들을 모두 담는 가장 가까운 조상. 막대를 main 바로 아래에 두면 본문 열을 벗어나 왼쪽 사이드바 밑까지 깔린다
 * (실측 2026-09-10: main 은 1,600px, 본문 열은 240~1,584px 이라 앞의 두 구역이 사이드바에 가렸다).
 */
function sectionsContainer(sections: Element[], fallback: Element): Element {
	if (sections.length === 0) return fallback;
	let common: Element | null = sections[0].parentElement;
	while (common && common !== fallback) {
		let holdsAll = true;
		for (let i = 1; i < sections.length; i++) {
			if (!common.contains(sections[i])) { holdsAll = false; break; }
		}
		if (holdsAll) return common;
		common = common.parentElement;
	}
	return fallback;
}

let sectionScrollHandler: (() => void) | null = null;

/** 구역 막대를 만들어 본문 맨 앞에 둔다. 만들었으면 true. */
function renderSectionNav(): boolean {
	const viewport = window.innerHeight || 800;
	const tallEnough = (document.body ? document.body.scrollHeight : 0) > viewport * SECTION_NAV_MIN_SCROLL;
	const main = document.getElementById("mainContent");
	if (!main) return false;
	const old = main.querySelector("[data-page-section-nav]");
	if (old) old.remove();
	if (sectionScrollHandler) {
		window.removeEventListener("scroll", sectionScrollHandler);
		sectionScrollHandler = null;
	}
	const sections = pageSections(main);
	const label = main.getAttribute("data-section-nav-label") || "Sections";
	const nav = tallEnough ? buildSectionNav(sections, label) : null;
	if (!nav) return false;
	const host = sectionsContainer(sections, main);
	host.insertBefore(nav, host.firstChild);
	markCurrentSection(nav, sections);
	sectionScrollHandler = () => markCurrentSection(nav, sections);
	window.addEventListener("scroll", sectionScrollHandler, { passive: true });
	return true;
}

function startSectionNav(): void {
	const run = () => {
		renderSectionNav();
		// 조각이 바뀌면 구역도 바뀐다(배당·매매는 htmx 로 본문을 통째로 갈아 끼운다).
		// 교체분이 그려진 뒤에 막대를 끼우면 아래가 53px 밀린다(실측 2026-09-10: 배당 CLS 0.023, 매매 0.0252).
		// afterSwap 은 교체와 같은 태스크라 그리기 전에 들어간다. afterSettle 은 늦게 자라난 본문(차트 등)을 위한 보완.
		document.addEventListener("htmx:afterSwap", () => renderSectionNav());
		document.addEventListener("htmx:afterSettle", () => renderSectionNav());
	};
	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", run, { once: true });
		return;
	}
	run();
}
startSectionNav();
(globalThis as any).__sectionNavInternals = { sectionLabel, pageSections, sectionAnchorId, buildSectionNav, markCurrentSection, renderSectionNav, sectionsContainer };
