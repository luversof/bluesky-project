// 화면에 숫자·날짜를 찍을 때 쓰는 **앱 로케일**. 브라우저 로케일이 아니다.
//
// 실측 2026-09-15: 서식을 만드는 자리 아홉 곳이 로케일 인자 없이 toLocaleString()/Intl 을
// 불러 브라우저 로케일을 따랐다. 브라우저를 de-DE 로 두고 한국어 화면을 열면 같은 화면
// 안에서 툴팁·카드만 "\u20a91.143.757.310" 으로 갈렸다(16 자리). 규칙은 한 곳에 둔다 -
// common.js 는 모든 화면에 가장 먼저 로드되므로 인라인 스크립트에서도 부를 수 있다.
function appLocale(): string {
	const body = document.body as HTMLElement | null;
	return (
		(body && body.dataset && body.dataset.locale) ||
		document.documentElement?.lang ||
		"ko-KR"
	);
}
(globalThis as any).appLocale = appLocale;
(globalThis as any).__appLocaleInternals = { appLocale };

// 화면의 기준 **시간대**. 브라우저 시간대가 아니다.
//
// 서버는 StockZoneUtil 로 기준을 정한다 - 주소의 timeZone 이 있으면 그것, 없으면 서버 존.
// 브라우저가 찍는 날짜·시각이 그 기준을 안 따르면 한 화면에 기준이 둘이 된다. 존은 일자
// 경계를 옮기므로(실측 2026-09-16: 같은 타임스탬프가 UTC+14 에서 9/16, UTC-12 에서 9/15)
// 조용한 어긋남은 틀린 값을 그럴듯하게 만든다.
function appTimeZone(): string {
	const fromUrl = new URLSearchParams(location.search).get("timeZone");
	if (fromUrl) {
		return fromUrl;
	}
	const cfg = document.getElementById("app-config") as HTMLElement | null;
	return (
		(cfg && cfg.dataset && cfg.dataset.timeZone) ||
		Intl.DateTimeFormat().resolvedOptions().timeZone
	);
}
(globalThis as any).appTimeZone = appTimeZone;
(globalThis as any).__appTimeZoneInternals = { appTimeZone };

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
	applyActivityViewTitle(root, mode);
}

/**
 * 보고 있는 뷰를 문서 제목에 적는다.
 *
 * 실측 2026-09-12: 세 뷰가 "활동 내역" 한 종류를 써서 브라우저 탭·기록·즐겨찾기에서 구분되지 않았다.
 * 다른 탭 화면(시뮬레이터 3/3 · 관리 2/2 · 배당 2/2)은 이미 탭 이름을 붙인다.
 * 제목은 탭이 `data-view-page-title` 로 들고 있다 - 이미 붙어 있는 패널로 되돌아가는 전환은 요청이 없어서
 * 조각의 [data-page-title] 기제로는 닿지 않기 때문이다.
 */
function applyActivityViewTitle(root: ParentNode, mode: string) {
	const tab = root.querySelector(
		'[data-activity-view-tab="' + mode + '"]',
	) as HTMLElement | null;
	if (!tab) return;
	const name = tab.getAttribute("data-view-page-title");
	if (!name) return;
	document.title = pageTitleFor(name, document.title);
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

// [data-confirm]: 폼을 보내기 전에 확인을 받는다(2026-10-14 - 관리 월배당 지급 이력의 행 삭제. 단건 폼을 걷어내며 행마다 삭제를 두었는데
// 한 번 누르면 바로 지워지면 안 된다). 잡는 단계(capture)에서 물어 취소하면 덮개(submitOverlay)도 뜨지 않는다.
document.addEventListener(
	"submit",
	(event) => {
		const form = event.target as HTMLFormElement | null;
		const message = form && typeof form.getAttribute === "function" ? form.getAttribute("data-confirm") : null;
		if (message && !globalThis.confirm(message)) event.preventDefault();
	},
	true,
);

// [data-reload-after-request]: htmx 요청 성공 시 페이지 새로고침 (관리 데이터 갱신 버튼 등)
document.addEventListener("htmx:afterRequest", (event: any) => {
	const el = (event.target as HTMLElement)?.closest?.(
		"[data-reload-after-request]",
	);
	if (el && event.detail?.successful) {
		globalThis.location.reload();
	}
});

const RANGE_QUERY_KEYS = ["startDate", "endDate", "rangeMode"];

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
	// 기간은 세 키가 한 묶음이다. 키 단위로만 덮어쓰면 URL 의 rangeMode 와 hx-include 의 날짜가 섞여
	// 버튼과 실제 데이터가 달라진다. 실측 2026-09-11(저장된 기간 1개월 상태에서):
	//   ?rangeMode=ytd            → 버튼 '올해' 인데 날짜는 2026-08-12~09-11 (20행)
	//   ?startDate=..&endDate=..  → 버튼 '1개월' 인데 날짜는 2026-01-01~09-11 (115행)
	// URL 이 세 키 중 하나라도 들고 있으면 나머지도 URL 쪽으로 맞춰 빈 것은 보내지 않는다
	// (서버는 rangeMode 만 받으면 날짜를 직접 계산하고, 날짜만 받으면 프리셋 강조를 뺀다).
	if (RANGE_QUERY_KEYS.some((key) => key in merged)) {
		for (const key of RANGE_QUERY_KEYS) {
			if (!(key in merged)) delete event.detail.parameters[key];
		}
	}
});

// activity-list fragment 요청에는 저장된 뷰 모드를 실어 보낸다.
// 서버가 그 뷰 하나만 렌더하므로 숨은 뷰의 마크업(실측 1370KB)이 아예 생성되지 않는다.
// data-params-from-query 훅보다 뒤에 등록해 URL 에 남은 옛 값을 localStorage 값으로 덮어쓴다.
/**
 * 첫 조회가 어느 뷰를 받을지.
 *
 * 주소에 뷰가 적혀 있으면 그것을 따른다. 2026-09-12 까지는 주소를 보지 않고 저장값으로 덮어써서,
 * `?activityView=timeline` 같은 링크가 **어떤 조합에서도 효과가 없었다**(실측: 주소 calendar/timeline/list
 * 셋 다 활성 탭은 늘 저장값, 저장값이 없으면 캘린더). 서버는 이 값을 받아 처리하는데도 그랬다.
 * 기간(rangeMode)에서 같은 이유로 공유 링크가 무력했던 것을 고친 것과 같은 규칙이다.
 *
 * 뷰 전환은 htmx 가 아니라 loadActivityView 의 fetch 로 돌기 때문에 이 규칙이 전환을 막지 않는다.
 */
function resolveActivityViewForRequest(
	search: string,
	saved: string | null,
): string {
	const known = (v: string | null) =>
		v === "timeline" || v === "list" || v === "calendar";
	let fromUrl: string | null = null;
	try {
		fromUrl = new URLSearchParams(search || "").get("activityView");
	} catch (e) {
		fromUrl = null;
	}
	if (known(fromUrl)) return fromUrl as string;
	return known(saved) ? (saved as string) : "calendar";
}

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
	const search = (globalThis.location && globalThis.location.search) || "";
	const view = resolveActivityViewForRequest(search, saved);
	event.detail.parameters.activityView = view;
	// 링크로 들어온 뷰는 지금 상태가 된다 - 안 그러면 조회는 그 뷰로 하고 복원은 저장값으로 해서 어긋난다.
	if (view !== saved) {
		try {
			localStorage.setItem(ACTIVITY_VIEW_KEY, view);
		} catch (e) {
			/* ignore */
		}
	}
});

// [data-sync-url="<fragment 경로>"]: 화면 래퍼에 지정한 목록 엔드포인트로의 GET 이 성공하면
// 그 조회 조건을 페이지 URL 에 반영한다(첫 사용자 변경만 pushState, 나머지는 replaceState).
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
	const nextUrl = globalThis.location.pathname + query;
	// 사용자가 조건을 처음 바꿀 때만 히스토리를 한 칸 쌓는다(사용자 결정 2026-09-23: "들어올 때 기간으로 한 번").
	// 뒤로 1번 = 이 화면에 들어올 때의 기간, 2번 = 이전 화면. 그 뒤의 변경은 덮어쓴다 - 프리셋을 여러 번
	// 눌러 봐도 화면을 뜨는 데 뒤로를 그만큼 누르지 않게.
	// "사용자가 바꿨다" 는 요청에서 알 수 없다 - 기간 변경은 스크립트가 htmx.ajax 로 여러 요청을 내보내고,
	// 화면을 열 때의 기간 복원도 같은 길을 지난다(실측 2026-09-23: 요청의 triggeringEvent 로 가르면 한 번도 안 쌓였다).
	// 그래서 사람이 실제로 기간 선택기([data-picker])를 누른 직후에 **보낸** 요청인지로 가른다(아래 htmx:beforeRequest 가 적는다).
	const userChange = event.detail.xhr?.[RANGE_USER_CHANGE_FLAG] === true;
	if (userChange && !rangeHistoryPushed && nextUrl !== globalThis.location.pathname + globalThis.location.search) {
		rangeHistoryPushed = true;
		globalThis.history.pushState({ gateSyncUrl: true }, "", nextUrl);
		return;
	}
	globalThis.history.replaceState(
		globalThis.history.state,
		"",
		nextUrl,
	);
});

// 쌓은 칸과 들어올 때의 칸은 같은 문서라 뒤로/앞으로가 페이지를 다시 그리지 않는다 - 주소만 바뀌고
// 화면은 마지막 기간 그대로 남는다. 그래서 이 문서가 한 칸을 쌓은 뒤의 이동은 다시 불러 주소대로 그린다.
let rangeHistoryPushed = false;
let rangePickerTouchedAt = 0;
// 선택기를 누른 뒤 조각 요청을 보내기까지의 여유(보내는 것은 거의 즉시다).
const RANGE_PICKER_INTENT_MS = 8000;
for (const type of ["click", "change"]) {
	document.addEventListener(
		type,
		(ev: Event) => {
			// isTrusted: 스크립트가 흉내 낸 클릭(기간 복원 등)은 사람의 변경이 아니다.
			const target = ev.target as HTMLElement | null;
			if (ev.isTrusted && target?.closest?.("[data-picker]")) {
				rangePickerTouchedAt = Date.now();
			}
		},
		true,
	);
}
// 사람의 변경인지는 **요청을 보낼 때** 가른다. 받을 때 재면 느린 응답이 창을 넘긴다 - 실측 2026-09-23: 조각 응답을 9 초 늦추자
// 첫 변경이 pushState 가 아니라 replaceState 로 끝나 뒤로가 들어올 때 기간이 아니라 이전 화면으로 나갔다(재기동 직후 한 번 난 것과 같은 모양).
const RANGE_USER_CHANGE_FLAG = "__gateRangeUserChange";
document.addEventListener("htmx:beforeRequest", (event: any) => {
	const xhr = event.detail?.xhr;
	if (xhr) xhr[RANGE_USER_CHANGE_FLAG] = Date.now() - rangePickerTouchedAt < RANGE_PICKER_INTENT_MS;
});
globalThis.addEventListener("popstate", () => {
	if (rangeHistoryPushed) {
		globalThis.location.reload();
	}
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

// [data-overlay] 레이어 닫기 처리는 2026-09-14 에 걷어냈다.
// 그 속성을 붙이는 마크업이 소스 어디에도 없어 **모든 화면에서 클릭·ESC 마다 헛돌기만** 했다
// (selectorsResolve 검사가 KNOWN_DEAD 로 기록해 두고 있었다).
// 실제 오버레이는 data-submit-overlay 다 - 느린 POST 를 덮어 **두 번째 제출을 막는** 것이라
// 배경 클릭·ESC 로 닫히면 오히려 안 된다. 닫기 처리를 그쪽에 이어 붙이지 말 것.

// PoE 아이템 호버 미리보기 — .poe-hover 요소에 마우스를 올리면 hx-get 으로 로드된 게임 툴팁을
// 요소 근처(뷰포트 안)에 띄운다. 트리거와 툴팁 사이를 오가는 동안은 유지되고(150ms 유예), 둘 다 벗어나거나 Esc 면 닫힌다(10-02 C31).
(() => {
	let host: HTMLElement | null = null;
	let active: Element | null = null;
	function ensureHost(): HTMLElement | null {
		if (host && document.body?.contains(host)) return host;
		if (!document.body) return null;
		host = document.createElement("div");
		host.id = "poePreview";
		host.setAttribute("role", "tooltip");
		host.className = "fixed z-[95] hidden";
		// 포인터가 툴팁 위로 올라갈 수 있어야 한다(1.4.13 hoverable, 10-02 C31) — 숨김은 display:none(hidden)이라 안 보일 땐 아무것도 가리지 않는다
		host.style.pointerEvents = "auto";
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
	// 키보드로 연 툴팁은 포커스 받은 요소의 설명으로 잇는다(aria-describedby, 10-02 C33) — 낭독기가 이름 뒤에 툴팁 내용을 읽는다. 닫으면 원래대로.
	let described: Element | null = null;
	let describedPrev: string | null = null;
	function describe(el: Element | null) {
		if (described) {
			if (describedPrev === null) described.removeAttribute("aria-describedby");
			else described.setAttribute("aria-describedby", describedPrev);
		}
		described = el;
		describedPrev = el ? el.getAttribute("aria-describedby") : null;
		if (el) el.setAttribute("aria-describedby", (describedPrev ? describedPrev + " " : "") + "poePreview");
	}
	function hide() {
		if (pinned) return;
		describe(null);
		host?.classList.add("hidden");
		if (host) host.replaceChildren();
		active = null;
	}
	function unpin() {
		pinned = false;
		describe(null);
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
	// WCAG 1.4.13(10-02 C31) — 툴팁 위로 포인터를 옮겨도 닫히지 않게(hoverable): 트리거에서 벗어나면 바로 닫지 않고 잠깐 기다렸다가,
	//   그 사이 툴팁(#poePreview)이나 같은 트리거로 들어오면 취소한다. 예전엔 툴팁이 pointer-events:none 이라 옮기는 순간 사라졌다.
	let hideTimer: number | undefined;
	function cancelHide() {
		if (hideTimer !== undefined) window.clearTimeout(hideTimer);
		hideTimer = undefined;
	}
	function hideSoon() {
		cancelHide();
		hideTimer = window.setTimeout(() => {
			hideTimer = undefined;
			hide();
		}, 150);
	}
	document.addEventListener("mouseover", (event) => {
		if (pinned) return;
		if ((event.target as Element)?.closest?.("#poePreview")) {
			cancelHide(); // 툴팁 위 — 그대로 둔다
			return;
		}
		const trigger = (event.target as Element)?.closest?.(".poe-hover");
		if (trigger === active && trigger) cancelHide();
		if (!trigger || trigger === active) return;
		openFor(trigger, false);
	});
	/** 툴팁 열기 — 마우스 호버와 키보드 포커스 공용. 키보드면 htmx 의 mouseenter 요청을 직접 불러 준다(마우스는 htmx 가 스스로 듣는다). */
	function openFor(trigger: Element, viaKeyboard: boolean) {
		cancelHide();
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
				if (viaKeyboard) (window as any).htmx?.trigger(trigger, "mouseenter");
			}
		}
	}
	// 키보드(10-02 C32) — Tab 으로 .poe-hover(또는 그 안의 링크)에 오면 같은 툴팁, 포커스가 트리거 · 툴팁 밖으로 나가면 닫는다. Esc 는 C31.
	document.addEventListener("focusin", (event) => {
		if (pinned) return;
		const target = event.target as Element;
		const trigger = target?.closest?.(".poe-hover");
		if (!trigger || trigger === active) return;
		if (!target.matches?.(":focus-visible")) return; // 마우스 클릭 포커스는 호버가 이미 처리
		openFor(trigger, true);
		describe(target);
	});
	document.addEventListener("focusout", (event) => {
		if (pinned || !active) return;
		const to = (event as FocusEvent).relatedTarget as Element | null;
		if (to && (active.contains(to) || to.closest?.("#poePreview"))) return;
		if ((event.target as Element)?.closest?.(".poe-hover") === active) {
			cancelHide();
			hide();
		}
	});
	document.addEventListener("mouseout", (event) => {
		if (pinned) return;
		const from = event.target as Element;
		const trigger = from?.closest?.(".poe-hover");
		const fromLayer = from?.closest?.("#poePreview");
		if (!trigger && !fromLayer) return;
		const to = (event as MouseEvent).relatedTarget as Element | null;
		if (to?.closest?.("#poePreview") || (to && active && active.contains(to))) return; // 툴팁 ↔ 트리거 사이 이동
		if (!to?.closest?.(".poe-hover")) hideSoon();
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
		if (event.key !== "Escape") return;
		if (pinned) unpin();
		// 호버로만 뜬 툴팁도 Esc 로 닫는다(WCAG 1.4.13 dismissible, 10-02 C31) — 포인터를 옮기지 않고 가린 내용을 볼 수 있게
		else if (host && !host.classList.contains("hidden")) {
			cancelHide();
			hide();
		}
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
		// 레이어 **안의** 단추(젬 레벨 1·5·10… 등)가 부른 응답은 버리지 않는다 — 예전엔 "활성 트리거가 아니면 폐기"에 같이 걸려
		//   PoE1·PoE2 젬 툴팁의 레벨 단추를 눌러도 아무 일이 없었다(10-01 탐침으로 발견).
		const fromLayer = !!elt && !!host && host.contains(elt);
		if (elt && active && elt !== active && !fromLayer) {
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

// [data-copy-target]: 대상(textarea/input)의 값을 클립보드로 — 누른 단추 글자를 잠깐 data-copied-label 로 바꾼다(10-01, PoE2 시뮬 결과 PoB 코드).
//   PoE1 시뮬은 인라인 onclick 이었는데, 조각마다 같은 코드를 되풀이하지 않게 공용으로 둔다.
document.addEventListener("click", (event) => {
	const btn = (event.target as HTMLElement)?.closest?.("[data-copy-target]") as HTMLElement | null;
	if (!btn) return;
	const target = document.querySelector(btn.getAttribute("data-copy-target") || "") as HTMLInputElement | HTMLTextAreaElement | null;
	if (!target) return;
	const label = btn.textContent || "";
	navigator.clipboard
		.writeText(target.value)
		.then(() => {
			btn.textContent = btn.getAttribute("data-copied-label") || label;
			setTimeout(() => (btn.textContent = label), 1500);
		})
		.catch(() => {
			target.select();
		});
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

// 가로로 스크롤하는 탭 줄(.tabs-scroll, 글자가 안 들어가면 탭을 줄이지 않고 스크롤 - 2026-09-17)에서 지금 탭이 줄 밖에 있으면
// 줄만 옆으로 옮겨 보이게 한다. 안 그러면 큰 글꼴 폰 폭에서 '적립식 복리' 탭에 있어도 줄 첫머리만 보여 지금 어느 탭인지 모른다.
// 문서는 움직이지 않는다(scrollIntoView 는 탭 줄이 화면 아래에 있으면 페이지까지 내린다) - 줄의 scrollLeft 만 바꾼다.
function tabScrollLeftFor(scrollLeft: number, barWidth: number, tabLeft: number, tabWidth: number): number {
	// 줄보다 넓은 탭은 온전히 보일 수 없다 - 글자가 시작하는 왼쪽 끝을 맞춘다(글꼴 200% 320px 에서 "종목별 기여" 176px / 줄 158px).
	if (tabWidth >= barWidth) return tabLeft;
	if (tabLeft < scrollLeft) return tabLeft;
	if (tabLeft + tabWidth > scrollLeft + barWidth) return tabLeft + tabWidth - barWidth;
	return scrollLeft;
}
function revealTab(bar: HTMLElement, tab: HTMLElement) {
	if (bar.scrollWidth <= bar.clientWidth + 1) return;
	// .tabs-scroll 은 position: relative 라 탭의 offsetLeft 가 줄 기준이다.
	bar.scrollLeft = tabScrollLeftFor(bar.scrollLeft, bar.clientWidth, tab.offsetLeft, tab.offsetWidth);
}
function revealActiveTabs(root: ParentNode = document) {
	root.querySelectorAll<HTMLElement>(".tabs-scroll").forEach((bar) => {
		const active = bar.querySelector<HTMLElement>(".tab-active");
		if (active) revealTab(bar, active);
	});
}
// 저장된 패널 탭 복원(restorePanelTabs) 뒤에 등록돼 있어 복원된 탭을 기준으로 옮긴다.
document.addEventListener("DOMContentLoaded", () => revealActiveTabs());
document.addEventListener("htmx:afterSettle", () => revealActiveTabs());
// 가로로 스크롤하는 상자(탭 줄 · 표 래퍼) 안에서 키보드 포커스를 받은 요소가 일부만 보이면 그 상자만 옆으로 옮겨 온전히 보이게 한다.
// 브라우저는 포커스 요소가 조금이라도 보이면 상자를 스크롤하지 않는다 - 실측 2026-09-17: 탭 줄 5 줄(tabs-scroll-keyboard.js, 기본 글꼴 375px
// 자산 성장 "연도별 세금·비용" 이 절반 잘린 채 포커스), 표 상자 375px 15 개에서 포커스 514 번 중 114 번(table-scroll-keyboard.js - 자산 성장
// 종목 링크 · 활동 · 관리 월배당 기준 · 자산 현황 정렬 단추, 1024px 에서도 자산 현황 정렬 단추 2).
// 좁은 화면(< 48rem)의 표는 첫 칸이 왼쪽에 고정(sticky)되어 그 폭은 가려진 자리다 - 빼고 맞춘다. 고정 첫 칸 안의 요소는 늘 보인다.
// 마우스 클릭으로 받은 포커스(:focus-visible 아님)는 움직이지 않는다 - 누른 자리가 옆으로 튀면 안 된다.
/** 보이는 폭(inset 을 뺀 자리)에 요소를 온전히 넣는 scrollLeft. 요소 위치는 화면 좌표, 보이는 왼쪽 끝도 화면 좌표. */
function focusScrollLeftFor(scrollLeft: number, visibleLeft: number, visibleWidth: number, elLeft: number, elWidth: number): number {
	return tabScrollLeftFor(scrollLeft, visibleWidth, scrollLeft + (elLeft - visibleLeft), elWidth);
}
function horizontalScrollBox(el: HTMLElement): HTMLElement | null {
	for (let p = el.parentElement; p && p !== document.body; p = p.parentElement) {
		const ox = getComputedStyle(p).overflowX;
		if ((ox === "auto" || ox === "scroll") && p.scrollWidth > p.clientWidth + 1) return p;
	}
	return null;
}
/**
 * 이 행에서 첫 열 자리를 차지한 칸. 위 행의 첫 칸이 rowspan 으로 내려와 있으면 그 칸이고, 이 행의 첫 칸은 둘째 열이다
 * (실측 2026-09-17: 월배당 시뮬 두 줄 머리 - 둘째 줄 정렬 링크 6개가 고정된 rowspan 첫 칸 밑에 숨었는데 자기 행 첫 칸만 봐서 0 을 뺐다).
 */
function firstColumnCell(row: Element): Element | null {
	const section = row.parentElement;
	if (!section) return row.firstElementChild;
	let owner: Element | null = null;
	let until = 0;
	let index = 0;
	for (let r = section.firstElementChild; r; r = r.nextElementSibling, index++) {
		if (index >= until) {
			owner = r.firstElementChild;
			const span = owner ? (owner as HTMLTableCellElement).rowSpan : 1;
			until = index + (span === 0 ? Infinity : Math.max(1, span || 1));
		}
		if (r === row) return owner;
	}
	return row.firstElementChild;
}
/** 같은 행의 첫 열 칸이 왼쪽 고정이면 그 폭(가려진 자리). 요소가 그 고정 칸 안이면 -1(늘 보인다). */
function stickyFirstCellInset(el: HTMLElement): number {
	const cell = el.closest("td, th");
	const first = cell && cell.parentElement ? (firstColumnCell(cell.parentElement) as HTMLElement | null) : null;
	if (!first || getComputedStyle(first).position !== "sticky") return 0;
	return first === cell ? -1 : first.getBoundingClientRect().width;
}
function revealFocused(target: EventTarget | null) {
	if (!(target instanceof HTMLElement) || !target.matches(":focus-visible")) return;
	const box = horizontalScrollBox(target);
	if (!box) return;
	const inset = stickyFirstCellInset(target);
	if (inset < 0) return;
	const visibleLeft = box.getBoundingClientRect().left + box.clientLeft + inset;
	const rect = target.getBoundingClientRect();
	const next = focusScrollLeftFor(box.scrollLeft, visibleLeft, box.clientWidth - inset, rect.left, rect.width);
	if (Math.abs(next - box.scrollLeft) >= 1) box.scrollLeft = next;
}
document.addEventListener("focusin", (event) => revealFocused(event.target));
(globalThis as any).__tabScrollInternals = { tabScrollLeftFor, focusScrollLeftFor, revealTab, revealActiveTabs, revealFocused, firstColumnCell, stickyFirstCellInset };
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
	// data-swap-announce: 구역 이름(일반 div 에 aria-label 을 달면 보조기술 규칙 위반이라 따로 둔다, 2026-10-13).
	const name = (target.getAttribute("data-swap-announce") || target.getAttribute("aria-label") || docTitle.split(" \u00B7 ")[0] || "").trim();
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
	// *Fragment 조각, 또는 data-swap-announce 를 단 결과 구역(시뮬레이터 · 월배당 ETF · 관리 기준 데이터 - 2026-10-13 구역 교체로 생겼는데 이름이
	// Fragment 로 끝나지 않아 칩 · 정렬 뒤 아무 안내가 없었다).
	if (!(target instanceof Element)) return false;
	const announced = target.getAttribute("data-swap-announce") !== null;
	if (!announced && (!target.id || !target.id.endsWith("Fragment"))) return false;
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
(globalThis as any).__pageTitleInternals = { pageTitleFor, applyFragmentTitle, applyActivityViewTitle };

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

// 말풍선은 트리거 가운데에 맞춰 펼쳐지는데 뷰포트 충돌 회피가 없다. 실측 2026-09-11(활동 화면):
// 375px 에서 툴팁 48개 중 34개가 화면 밖으로 나갔고, 숨어 있는 말풍선도 레이아웃에 남아 문서가 36px 가로로 밀렸다
// (768px 에서도 5개 잘림 + 32px). 트리거 위치와 말풍선 폭을 재서 안 들어가면 좌/우 끝 맞춤으로 바꾼다.
// 순수 함수라 검사에서 그대로 돌린다. margin 은 화면 가장자리에 붙지 않게 두는 여백.
const TOOLTIP_ALIGN_START = "tooltip-align-start";
const TOOLTIP_ALIGN_END = "tooltip-align-end";
function tooltipAlign(left: number, right: number, bubbleWidth: number, viewportWidth: number, margin = 8): string {
	if (!(bubbleWidth > 0) || !(viewportWidth > 0)) return "center";
	const center = (left + right) / 2;
	const half = bubbleWidth / 2;
	if (center - half >= margin && center + half <= viewportWidth - margin) return "center";
	// 끝 맞춤: start 는 트리거 왼쪽 끝에 맞춰 오른쪽으로, end 는 오른쪽 끝에 맞춰 왼쪽으로 펼친다.
	if (left + bubbleWidth <= viewportWidth - margin) return "start";
	if (right - bubbleWidth >= margin) return "end";
	// 어느 쪽으로도 안 들어가면(말풍선이 화면보다 넓음) 남는 공간이 큰 쪽으로 붙인다.
	return left >= viewportWidth - right ? "end" : "start";
}
function tooltipBubbleWidth(el: Element): number {
	const content = el.querySelector(".tooltip-content");
	if (content && content.parentElement === el) return content.getBoundingClientRect().width;
	const raw = getComputedStyle(el, "::before").width;
	const w = parseFloat(raw || "0");
	return Number.isFinite(w) ? w : 0;
}
// 세로 변형(tooltip-right/left)과 마크업이 이미 끝 맞춤을 고른 툴팁은 건드리지 않는다.
function tooltipAlignable(el: Element): boolean {
	const cl = el.classList;
	return !cl.contains("tooltip-right") && !cl.contains("tooltip-left")
		&& !cl.contains("tooltip-bottom-start") && !cl.contains("tooltip-bottom-end");
}
// 화면 가장자리만 보면 고정 옆 메뉴 밑으로 말풍선이 들어간다. 실측 2026-10-21(대시보드 1280px): 옆 메뉴(fixed z-40, 불투명)가
// [0,224] 를 덮는데 통합손익 계산 내역 말풍선이 가운데 맞춤으로 [197,517] 에 놓여 왼쪽 27px 이 메뉴에 가려 "잘려" 보였다
// (말풍선 z 20 < 메뉴 40). 레이아웃이 본문 칸에 data-tooltip-bounds 를 달면 그 칸(화면과 겹치는 부분)을 경계로 잰다.
function tooltipBounds(el: Element, viewportWidth: number): { left: number; width: number } {
	const host = el.closest("[data-tooltip-bounds]");
	if (!host) return { left: 0, width: viewportWidth };
	const r = host.getBoundingClientRect();
	const left = Math.max(0, r.left);
	const right = Math.min(viewportWidth, r.right);
	return right > left ? { left, width: right - left } : { left: 0, width: viewportWidth };
}
function alignTooltip(el: Element, viewportWidth: number): string {
	if (!tooltipAlignable(el)) return "skip";
	el.classList.remove(TOOLTIP_ALIGN_START, TOOLTIP_ALIGN_END);
	const rect = el.getBoundingClientRect();
	if (!(rect.width > 0)) return "hidden";
	const bounds = tooltipBounds(el, viewportWidth);
	const align = tooltipAlign(rect.left - bounds.left, rect.right - bounds.left, tooltipBubbleWidth(el), bounds.width);
	if (align === "start") el.classList.add(TOOLTIP_ALIGN_START);
	else if (align === "end") el.classList.add(TOOLTIP_ALIGN_END);
	return align;
}
function alignTooltips(root: ParentNode = document): number {
	const vw = document.documentElement ? document.documentElement.clientWidth : 0;
	let n = 0;
	root.querySelectorAll(".tooltip").forEach((el) => {
		if (alignTooltip(el, vw) !== "skip") n++;
	});
	return n;
}
let tooltipAlignTimer: any = null;
function scheduleTooltipAlign(): void {
	if (tooltipAlignTimer) clearTimeout(tooltipAlignTimer);
	tooltipAlignTimer = setTimeout(() => alignTooltips(), 120);
}
document.addEventListener("DOMContentLoaded", () => alignTooltips());
document.addEventListener("htmx:afterSettle", () => alignTooltips());
window.addEventListener("resize", scheduleTooltipAlign);
// 열리기 직전에 한 번 더 잰다(표 가로 스크롤·접힘 펼침으로 트리거가 움직인 경우).
document.addEventListener("pointerenter", (event) => {
	const target = event.target;
	if (target instanceof Element && target.classList && target.classList.contains("tooltip")) {
		alignTooltip(target, document.documentElement.clientWidth);
	}
}, true);
document.addEventListener("focusin", (event) => {
	const target = event.target;
	if (target instanceof Element && target.classList && target.classList.contains("tooltip")) {
		alignTooltip(target, document.documentElement.clientWidth);
	}
});
(globalThis as any).__tooltipAlignInternals = { tooltipAlign, tooltipAlignable, tooltipBounds, alignTooltip, alignTooltips, TOOLTIP_ALIGN_START, TOOLTIP_ALIGN_END };


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

// 구역을 바꿔 끼워도 열어 둔 details 를 다시 연다(2026-10-18 C440 UX QA: PoE2 제작에서 화폐를 쓸 때마다 펼친 에센스 · 훼손 · 징조 묶음이 접혔다).
// 대상에 data-keep-open="키 속성 이름"(예: data-craft-group)을 달면 교체 직전 그 속성이 있는 열린 details 의 키를 적어 두고, 교체 뒤 같은 키를 연다.
// 닫는 일은 없다(더하기만) - 교체 직전 열려 있던 것은 사용자가 열었든 서버가 열었든 다시 연다(2026-10-18 C445 검토로 주석 정정).
// 스왑이 없던 응답(4xx/5xx · 204)의 메모가 남지 않게 요청(requestConfig)과 짝지어 같은 요청의 afterSwap 에서만 쓴다(C445 검토).
type KeepOpenMemo = { id: string; attr: string; keys: string[] };
const keepOpenMemos: { cfg: unknown; memo: KeepOpenMemo }[] = [];
function rememberOpenDetails(target: unknown): KeepOpenMemo | null {
	if (!(target instanceof Element) || typeof target.getAttribute !== "function") return null;
	const attr = target.getAttribute("data-keep-open");
	if (!attr || !target.id) return null;
	const keys = Array.from(target.querySelectorAll("details[" + attr + "]"))
		.filter((d: any) => d.open)
		.map((d) => d.getAttribute(attr) || "")
		.filter((k) => k);
	return { id: target.id, attr, keys };
}
/** 새 대상에서 적어 둔 키의 details 를 연다 - 연 개수. */
function restoreOpenDetails(memo: KeepOpenMemo | null, doc: Pick<Document, "getElementById"> = document): number {
	if (!memo || memo.keys.length === 0) return 0;
	const root = doc.getElementById(memo.id);
	if (!root) return 0;
	let n = 0;
	root.querySelectorAll("details[" + memo.attr + "]").forEach((d: any) => {
		if (!d.open && memo.keys.includes(d.getAttribute(memo.attr) || "")) {
			d.open = true;
			n++;
		}
	});
	return n;
}
document.addEventListener("htmx:beforeSwap", (event: any) => {
	const memo = rememberOpenDetails(event.detail?.target);
	if (memo) keepOpenMemos.push({ cfg: event.detail?.requestConfig, memo });
});
// 기준 자리 복원(아래 afterSwap)보다 먼저 열어야 높이가 맞는다 - 같은 이벤트는 먼저 등록된 처리기가 먼저 돈다(그래서 이 블록이 위에 있다)
document.addEventListener("htmx:afterSwap", (event: any) => {
	const cfg = event.detail?.requestConfig;
	for (const m of keepOpenMemos.splice(0)) if (m.cfg === cfg) restoreOpenDetails(m.memo);
});

// 정착 뒤 대상 안 오류가 고정 헤더 밑 · 화면 밖이면 보이게 굴린다(2026-10-18 C440 UX QA: 390px 에서 오류가 헤더에 가려 '눌렀는데 아무 일 없음'으로 보였다).
// 대상에 data-error-into-view="선택자". 오류 요소의 scroll-margin-top 을 헤더 높이로 본다.
function needsReveal(rect: { top: number; bottom: number }, marginTop: number, viewH: number): boolean {
	return rect.top < marginTop || rect.bottom > viewH;
}
document.addEventListener("htmx:afterSettle", (event: any) => {
	const t = event.detail?.target;
	if (!(t instanceof Element) || typeof t.getAttribute !== "function") return;
	const host = (t.id && document.getElementById(t.id)) || t;
	const sel = host.getAttribute?.("data-error-into-view");
	if (!sel) return;
	const el = host.querySelector(sel) as HTMLElement | null;
	if (!el) return;
	const margin = parseFloat(getComputedStyle(el).scrollMarginTop || "0") || 0;
	if (needsReveal(el.getBoundingClientRect(), margin, globalThis.innerHeight)) el.scrollIntoView({ block: "start" });
});
(globalThis as any).__keepOpenInternals = { rememberOpenDetails, restoreOpenDetails, needsReveal };

// 구역을 바꿔 끼워도 고른 값을 되살린다(2026-10-19 C450 UX QA: PoE1 제작에서 화폐를 쓰면 고른 에센스 · 수확 · 작업대가 첫 항목으로, 화석 체크가 전부 풀려
// 연타하면 엉뚱한 화폐를 썼다). 대상에 data-keep-values="이름,이름" - 그 이름의 select 값과 체크된 checkbox 값들을 적어 두고 같은 요청의 교체 뒤 되돌린다.
// 새 select 에 그 값이 없으면 그대로(서버가 고른 기본값), 꺼진 체크박스는 켜지 않는다. change 이벤트는 내지 않는다(요청이 다시 나가지 않게).
type KeepValuesMemo = { id: string; selects: Record<string, string>; checks: Record<string, string[]> };
const keepValuesMemos: { cfg: unknown; memo: KeepValuesMemo }[] = [];
function rememberValues(target: unknown): KeepValuesMemo | null {
	if (!(target instanceof Element) || typeof target.getAttribute !== "function") return null;
	const names = (target.getAttribute("data-keep-values") || "").split(",").map((n) => n.trim()).filter((n) => n);
	if (names.length === 0 || !target.id) return null;
	const selects: Record<string, string> = {};
	const checks: Record<string, string[]> = {};
	for (const n of names) {
		const sel = target.querySelector('select[name="' + n + '"]') as HTMLSelectElement | null;
		if (sel) selects[n] = sel.value;
		const boxes = Array.from(target.querySelectorAll('input[type="checkbox"][name="' + n + '"]')) as HTMLInputElement[];
		if (boxes.length) checks[n] = boxes.filter((b) => b.checked).map((b) => b.value);
	}
	return { id: target.id, selects, checks };
}
/** 새 대상에 적어 둔 값을 되돌린다 - 바꾼 칸 수. */
function restoreValues(memo: KeepValuesMemo | null, doc: Pick<Document, "getElementById"> = document): number {
	if (!memo) return 0;
	const root = doc.getElementById(memo.id);
	if (!root) return 0;
	let n = 0;
	for (const [name, value] of Object.entries(memo.selects)) {
		const sel = root.querySelector('select[name="' + name + '"]') as HTMLSelectElement | null;
		if (sel && sel.value !== value && Array.from(sel.options).some((o) => o.value === value)) {
			sel.value = value;
			n++;
		}
	}
	for (const [name, values] of Object.entries(memo.checks)) {
		(Array.from(root.querySelectorAll('input[type="checkbox"][name="' + name + '"]')) as HTMLInputElement[]).forEach((b) => {
			if (!b.disabled && !b.checked && values.includes(b.value)) {
				b.checked = true;
				n++;
			}
		});
	}
	return n;
}
document.addEventListener("htmx:beforeSwap", (event: any) => {
	const memo = rememberValues(event.detail?.target);
	if (memo) keepValuesMemos.push({ cfg: event.detail?.requestConfig, memo });
});
document.addEventListener("htmx:afterSwap", (event: any) => {
	const cfg = event.detail?.requestConfig;
	for (const m of keepValuesMemos.splice(0)) if (m.cfg === cfg) restoreValues(m.memo);
});
(globalThis as any).__keepValuesInternals = { rememberValues, restoreValues };

// 단추로 다른 자리의 details 를 열고 닫는다(2026-10-19 C454: PoE2 제작 둘째 줄 단추 → 그 줄 아래 펼침 자리의 묶음). data-toggle-details="선택자".
// details 의 toggle 은 버블링되지 않아 capture 로 듣고, 그 details 를 가리키는 단추의 aria-expanded(+ data-toggle-active-class)를 맞춘다(교체 뒤 keep-open 이 다시 열어도).
// daisyUI 컴포넌트 클래스엔 aria-expanded: 변형이 안 만들어져 켜진 모양은 클래스로 붙인다.
function syncDetailsTogglers(d: Element): void {
	document.querySelectorAll("[data-toggle-details]").forEach((b) => {
		const sel = b.getAttribute("data-toggle-details") || "";
		let target: Element | null = null;
		try {
			target = document.querySelector(sel);
		} catch (e) {
			target = null;
		}
		if (target !== d) return;
		const open = (d as HTMLDetailsElement).open;
		b.setAttribute("aria-expanded", open ? "true" : "false");
		const cls = b.getAttribute("data-toggle-active-class");
		if (cls) b.classList.toggle(cls, open);
	});
}
document.addEventListener("click", (event) => {
	const btn = (event.target as HTMLElement)?.closest?.("[data-toggle-details]") as HTMLElement | null;
	if (!btn) return;
	let d: HTMLDetailsElement | null = null;
	try {
		d = document.querySelector(btn.getAttribute("data-toggle-details") || "") as HTMLDetailsElement | null;
	} catch (e) {
		d = null;
	}
	if (!d) return;
	event.preventDefault();
	if (!d.open) closeExclusiveSiblings(d);
	d.open = !d.open;
	syncDetailsTogglers(d);
});
// 한 번에 하나(2026-10-20 C476 사용자 요청 "제작 방식은 선택이 하나만 되도록"): [data-toggle-exclusive] 안 단추들이 여는 details 는 하나를 열면 나머지를 닫는다.
// data-toggle-keep 단추의 칸은 예외(PoE2 징조 — 다음 화폐에 걸어 두는 칸이라 다른 칸과 같이 연다). 사람이 누를 때만(스크립트가 여는 것은 그대로 — 탐침이 전부 열어 넘침을 본다).
function closeExclusiveSiblings(d: Element): void {
	document.querySelectorAll("[data-toggle-exclusive]").forEach((box) => {
		const pairs: { b: Element; t: Element | null }[] = [];
		box.querySelectorAll("[data-toggle-details]").forEach((b) => {
			let t: Element | null = null;
			try {
				t = document.querySelector(b.getAttribute("data-toggle-details") || "");
			} catch (e) {
				t = null;
			}
			pairs.push({ b, t });
		});
		const mine = pairs.find((x) => x.t === d);
		if (!mine || mine.b.hasAttribute("data-toggle-keep")) return;
		pairs.forEach((x) => {
			if (x.t && x.t !== d && !x.b.hasAttribute("data-toggle-keep") && (x.t as HTMLDetailsElement).open) (x.t as HTMLDetailsElement).open = false;
		});
	});
}
// 묶음 머리(summary)를 눌러 여는 것도 같게
document.addEventListener("click", (event) => {
	const sum = (event.target as HTMLElement)?.closest?.("summary");
	const d = sum?.parentElement;
	// 태그로 판정(시험용 가짜 DOM 엔 HTMLDetailsElement 가 없다)
	if (d && d.tagName === "DETAILS" && !(d as HTMLDetailsElement).open && sum === d.querySelector(":scope > summary")) closeExclusiveSiblings(d);
});
document.addEventListener("toggle", (event) => {
	const d = event.target;
	if (d instanceof Element && d.tagName === "DETAILS") syncDetailsTogglers(d);
}, true);
// 목록 거르기 · 고른 줄(2026-10-19 C458: PoE1 계산기 목표 고르기 한 화면).
// [data-filter-input="선택자"] — 입력 글자가 들어간 항목만 보인다(공백 무시, 대소문자 무시).
// [data-picked-list] 안 [data-picked-row] — 안의 select · 체크박스에 값이 있으면 data-picked(CSS 가 맨 위로 · 강조), 같은 폼의 [data-picked-count] 에 개수.
function filterItems(input: HTMLInputElement): void {
	const sel = input.getAttribute("data-filter-input") || "";
	const q = input.value.replace(/\s+/g, "").toLowerCase();
	let items: NodeListOf<HTMLElement>;
	try {
		items = document.querySelectorAll<HTMLElement>(sel);
	} catch (e) {
		return;
	}
	items.forEach((it) => {
		const text = (it.textContent || "").replace(/\s+/g, "").toLowerCase();
		it.hidden = q !== "" && !text.includes(q) && !it.hasAttribute("data-picked");
	});
}
document.addEventListener("input", (event) => {
	const el = (event.target as HTMLElement)?.closest?.("[data-filter-input]") as HTMLInputElement | null;
	if (el) filterItems(el);
});
function syncPicked(): void {
	document.querySelectorAll("[data-picked-list]").forEach((list) => {
		let n = 0;
		list.querySelectorAll<HTMLElement>("[data-picked-row]").forEach((row) => {
			const picked = Array.from(row.querySelectorAll<HTMLInputElement | HTMLSelectElement>("select, input")).some((c) =>
				c instanceof HTMLInputElement && (c.type === "checkbox" || c.type === "radio") ? c.checked : c.value !== "",
			);
			row.toggleAttribute("data-picked", picked);
			if (picked) n++;
		});
		const form = list.closest("form");
		form?.querySelectorAll("[data-picked-count]").forEach((c) => (c.textContent = String(n)));
	});
}
document.addEventListener("change", (event) => {
	if ((event.target as HTMLElement)?.closest?.("[data-picked-list]")) syncPicked();
});
document.addEventListener("DOMContentLoaded", syncPicked);
document.addEventListener("htmx:afterSettle", syncPicked);
function syncAllDetailsTogglers(): void {
	document.querySelectorAll("[data-toggle-details]").forEach((b) => {
		try {
			const d = document.querySelector(b.getAttribute("data-toggle-details") || "");
			if (d) syncDetailsTogglers(d);
		} catch (e) {
			/* 잘못된 선택자는 무시 */
		}
	});
}
document.addEventListener("DOMContentLoaded", syncAllDetailsTogglers);
document.addEventListener("htmx:afterSettle", syncAllDetailsTogglers);

// 바꾸면 화면 상태가 초기화되는 고르기 폼(2026-10-18 C442 UX QA: PoE2 제작에서 아이템 레벨을 바꾸면 만들던 아이템이 말없이 사라졌다).
// 폼(또는 조상)에 data-confirm-dirty="선택자" · data-confirm-text="문구" - 선택자 입력 중 값이 있는 게 있으면 확인을 받고, 취소하면 폼을 처음 값으로.
function isDirtyValues(values: string[]): boolean {
	return values.some((v) => v !== "" && v !== "[]" && v !== "{}");
}
document.addEventListener("htmx:confirm", (event: any) => {
	const elt = event.detail?.elt;
	const host = elt && typeof elt.closest === "function" ? (elt.closest("[data-confirm-dirty]") as HTMLElement | null) : null;
	if (!host) return;
	const values = Array.from(document.querySelectorAll(host.getAttribute("data-confirm-dirty") || "")).map((i: any) => String(i.value ?? ""));
	if (!isDirtyValues(values)) return;
	event.preventDefault();
	if (globalThis.confirm(host.getAttribute("data-confirm-text") || "")) {
		event.detail.issueRequest(true);
	} else {
		const form = (host.tagName === "FORM" ? host : host.closest("form")) as HTMLFormElement | null;
		form?.reset();
	}
});
(globalThis as any).__confirmDirtyInternals = { isDirtyValues };

// 구역을 바꿔 끼울 때 기준 요소를 화면의 같은 자리에 둔다(사용자 지적 2026-10-13: 시뮬레이터 월배당에서 지급 시기 칩을 전체 ↔ 월중 · 월말로
// 바꾸면 칩 위치가 달라졌다). scrollY 는 그대로인데 구역 안에서 칩보다 위에 있는 적립 추천 카드가 조건에 따라 길이가 바뀐다 - 그만큼 칩이 밀린다.
// 교체 대상에 data-swap-anchor="선택자" 를 달면 교체 직전 그 요소의 화면 위치를 재 두고, 교체 직후(그리기 전) 새 요소가 같은 자리에 오도록 굴린다.
// 누른 컨트롤이 data-swap-anchor-key="이름" 상자 안에 있으면 그 상자를 기준으로 삼는다 - 화면 가운데의 카드를 눌러도 그 카드가 제자리에
// 머문다(2026-10-13: 월배당 ETF 기준별 비교 카드). 상자가 없으면 대상의 선택자.
type SwapAnchorMemo = { selector: string; top: number };
// 요청(requestConfig)과 짝 - 스왑이 없던 응답의 메모는 다음 afterSwap 에서 버려진다(2026-10-18 C445 검토: 오류 응답 뒤 다른 구역 교체에서 화면이 튀었다)
const swapAnchorMemos: { cfg: unknown; memo: SwapAnchorMemo }[] = [];
function rememberSwapAnchor(target: unknown, doc: Pick<Document, "querySelector"> = document, trigger: unknown = null): SwapAnchorMemo | null {
	// 다른 처리기와 같은 이벤트를 듣는다 - 속성을 못 읽는 대상(시험 가짜 등)이면 조용히 빠진다.
	if (!(target instanceof Element) || typeof target.getAttribute !== "function") return null;
	const keyed = trigger instanceof Element && typeof trigger.closest === "function" ? trigger.closest("[data-swap-anchor-key]") : null;
	const key = keyed ? keyed.getAttribute("data-swap-anchor-key") : null;
	const selector = key ? '[data-swap-anchor-key="' + key + '"]' : target.getAttribute("data-swap-anchor");
	if (!selector) return null;
	const anchor = doc.querySelector(selector);
	if (!anchor) return null;
	return { selector, top: anchor.getBoundingClientRect().top };
}
/** 새 기준 요소를 옛 자리로 굴리고 굴린 양을 돌려준다(못 찾으면 0). */
function restoreSwapAnchor(memo: SwapAnchorMemo | null, doc: Pick<Document, "querySelector"> = document, scrollBy: (x: number, y: number) => void = (x, y) => globalThis.scrollBy(x, y)): number {
	if (!memo) return 0;
	const anchor = doc.querySelector(memo.selector);
	if (!anchor) return 0;
	const delta = Math.round(anchor.getBoundingClientRect().top - memo.top);
	if (delta !== 0) scrollBy(0, delta);
	return delta;
}
document.addEventListener("htmx:beforeSwap", (event: any) => {
	// 폼 제출이면 elt 는 폼이다 - 누른 단추(submitter)로 상자를 찾는다(2026-10-18 C440: PoE2 제작 화폐 단추가 결과 뒤 21~45px 밀렸다)
	const cfg = event.detail?.requestConfig;
	const memo = rememberSwapAnchor(event.detail?.target, document, cfg?.triggeringEvent?.submitter || cfg?.elt);
	if (memo) swapAnchorMemos.push({ cfg, memo });
});
// outerHTML 교체에서는 afterSwap 의 target 이 떨어져 나간 옛 요소일 수 있다 - 선택자로 새 요소를 찾는다.
document.addEventListener("htmx:afterSwap", (event: any) => {
	const cfg = event.detail?.requestConfig;
	for (const m of swapAnchorMemos.splice(0)) if (m.cfg === cfg) restoreSwapAnchor(m.memo);
});
(globalThis as any).__swapAnchorInternals = { rememberSwapAnchor, restoreSwapAnchor };






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
	// 날짜는 기준 시간대로 찍는다 - 안 그러면 차트 요약만 브라우저 존으로 하루 어긋난다.
	const dateOptions = { timeZone: appTimeZone() };
	if (raw instanceof Date) return raw.toLocaleDateString(locale, dateOptions);
	if (typeof raw === "number" && raw > 1e11)
		return new Date(raw).toLocaleDateString(locale, dateOptions);
	if (raw == null) return "";
	return String(raw);
}
function chartSummaryValue(point: unknown): number | null {
	// 떠 있는 막대([시작, 끝])는 끝값으로 읽는다 - 캔들 몸통 [시가, 종가] 면 종가(2026-10-02). 예전에는 배열이라 숫자로 못 읽고 빠져, 주가
	// 캔들 차트의 요약이 평단 선만 말했다.
	if (Array.isArray(point)) return chartSummaryValue(point[point.length - 1]);
	const v = point !== null && typeof point === "object" ? (point as any).y : point;
	const n = typeof v === "string" ? Number(v) : v;
	return typeof n === "number" && Number.isFinite(n) ? n : null;
}
// 금액 가리기(hide-amounts)는 화면을 흐리게만 한다 - 차트 요약은 sr-only 글자라 보조기술에는 그대로 읽힌다.
// 실측 2026-09-11(가리기를 켠 채 9 화면): 요약 14 곳이 금액을 그대로 담고 있었다
// (예: "투자원금: 39개 지점, 처음 2026-01-01 637,902,360, 끝 ..."). 라벨과 비중(%)은 화면에서도 안 가리므로 그대로 두고
// 금액만 뺀다.
function chartSummaryAmountsHidden(): boolean {
	const root = document.documentElement;
	// 최소 DOM 스텁(검사)에는 classList.contains 가 없을 수 있다 - 없으면 가리기가 꺼진 것으로 본다.
	return (
		!!root && !!root.classList && typeof root.classList.contains === "function"
		&& root.classList.contains("hide-amounts")
	);
}
/**
 * 차트 위 표시(캔들의 매수 ▲ · 매도 ▼ · 분배락 ◆ 등)의 개수 문장. 화면에는 모양으로만 보여 요약문에 없으면 보조기술 사용자는 표시가 있는지조차 모른다
 * (2026-10-02). 차트가 options.plugins.a11ySummary.counts = [{ label, count }] 로 넘긴다(0 은 뺀다).
 */
function chartSummaryCounts(chart: any, ko: boolean): string {
	const counts = chart?.options?.plugins?.a11ySummary?.counts;
	if (!Array.isArray(counts)) return "";
	const parts = counts.filter((c: any) => c && c.label && c.count > 0).map((c: any) => (ko ? String(c.label) + " " + c.count + "개" : String(c.label) + " " + c.count));
	if (!parts.length) return "";
	return (ko ? ". 표시: " : ". Markers: ") + parts.join(", ");
}
function chartSummaryText(chart: any, lang: string = document.documentElement.lang): string {
	const locale = chartSummaryLocale(lang);
	const ko = locale === "ko-KR";
	const fmt = new Intl.NumberFormat(locale, { maximumFractionDigits: 2 });
	const hidden = chartSummaryAmountsHidden();
	const type = chart?.config?.type || chart?.config?._config?.type || "";
	const labels: unknown[] = chart?.data?.labels || [];
	// summaryHidden: 요약에서 뺄 보조 계열(캔들 꼬리 [저가, 고가] - 몸통의 종가가 이미 그 날을 말한다).
	const datasets: any[] = (chart?.data?.datasets || []).filter((ds: any, i: number) => ds && !ds.summaryHidden && Array.isArray(ds.data) && ds.data.length && (typeof chart.isDatasetVisible !== "function" || chart.isDatasetVisible(i)));
	if (!datasets.length) return "";
	if (type === "doughnut" || type === "pie" || type === "polarArea") {
		const values = datasets[0].data.map(chartSummaryValue);
		const total = values.reduce((a: number, v: number | null) => a + (v || 0), 0);
		const parts = values.slice(0, CHART_SUMMARY_MAX_ITEMS).map((v: number | null, i: number) => {
			const share = total > 0 && v != null ? Math.round((v / total) * 1000) / 10 : 0;
			const label = chartSummaryLabel(labels[i], locale);
			return hidden ? label + " (" + share + "%)" : label + " " + fmt.format(v || 0) + " (" + share + "%)";
		});
		const rest = values.length - parts.length;
		if (rest > 0) parts.push(ko ? "외 " + rest + "개" : "and " + rest + " more");
		return (ko ? "항목 " + values.length + "개: " : values.length + " items: ") + parts.join(", ");
	}
	// 도넛은 넘치는 항목을 "외 N개" 로 밝히는데 선/막대는 4 개에서 조용히 잘랐다.
	// 실측 2026-09-12(배당 내역 월별 배당금, 데이터셋 10 개): 요약 356 자가 앞 4 계열만 담고
	// 나머지 6 개(기타 · 최근 12개월 합 포함)는 흔적도 없었다 - 보조기술은 그런 계열이 있는지조차 모른다.
	const shownDatasets = datasets.slice(0, CHART_SUMMARY_MAX_DATASETS);
	const restDatasets = datasets.length - shownDatasets.length;
	const body = shownDatasets.map((ds: any) => {
		const pts = ds.data.map((point: unknown, i: number) => ({ v: chartSummaryValue(point), l: chartSummaryLabel(labels[i] ?? (point && typeof point === "object" ? (point as any).x : undefined), locale) })).filter((x: any) => x.v != null);
		if (!pts.length) return "";
		let hi = pts[0], lo = pts[0];
		for (const x of pts) { if (x.v > hi.v) hi = x; if (x.v < lo.v) lo = x; }
		const first = pts[0], last = pts[pts.length - 1];
		const name = ds.label ? String(ds.label) + ": " : "";
		if (hidden)
			return ko
				? name + pts.length + "개 지점(금액 가림)"
				: name + pts.length + " points (amounts hidden)";
		return ko
			? name + pts.length + "개 지점, 처음 " + first.l + " " + fmt.format(first.v) + ", 끝 " + last.l + " " + fmt.format(last.v) + ", 최고 " + fmt.format(hi.v) + " (" + hi.l + "), 최저 " + fmt.format(lo.v) + " (" + lo.l + ")"
			: name + pts.length + " points, first " + first.l + " " + fmt.format(first.v) + ", last " + last.l + " " + fmt.format(last.v) + ", high " + fmt.format(hi.v) + " (" + hi.l + "), low " + fmt.format(lo.v) + " (" + lo.l + ")";
	}).filter(Boolean).join(". ");
	const counts = chartSummaryCounts(chart, ko);
	if (restDatasets <= 0) return body + counts;
	return body + (ko ? ". 외 " + restDatasets + "개 계열" : ". and " + restDatasets + " more series") + counts;
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
/** 금액 가리기 토글이 바뀌면 이미 그려진 차트의 요약도 다시 쓴다(토글은 클래스만 바꾸고 차트는 갱신되지 않는다). */
function resyncChartSummaries(root: ParentNode = document): number {
	const lib = (globalThis as any).Chart;
	if (!lib || typeof lib.getChart !== "function") return 0;
	let n = 0;
	root.querySelectorAll("canvas").forEach((canvas) => {
		const chart = lib.getChart(canvas);
		if (chart) {
			syncChartSummary(chart);
			n++;
		}
	});
	return n;
}
// 근거 문구는 백분율 옆에 붙어 있어 요소 자체는 가림 대상이 아니다 - 그래서 가리기를 켠 채 hover 하면
// title 로 정확한 금액이 그대로 보였다(실측 2026-09-11, 대시보드 합산 수익률: "총 합산 수익 1,271,376,178 ...").
// 자릿수 구분이 있는 수만 가린다(백분율은 화면에서도 안 가리므로 그대로 둔다).
// 숫자에 바로 붙은 "원" 도 같이 삼킨다 - 안 그러면 "1,622,109,770원" 이 "가림원" 이 된다(실측 2026-09-12).
// 띄어쓰기가 있으면 삼키지 않는다("642,014,000 원금" 의 원금이 깨지지 않게).
const AMOUNT_IN_TEXT = /₩?-?\d{1,3}(?:,\d{3})+(?:\.\d+)?(?:원)?/g;
function maskAmountBasis(root: ParentNode = document): number {
	const hidden = chartSummaryAmountsHidden();
	const ko = (document.documentElement.lang || "").toLowerCase().startsWith("ko");
	const mask = ko ? "가림" : "hidden";
	let n = 0;
	root.querySelectorAll("[data-amount-basis]").forEach((el) => {
		const original = el.getAttribute("data-amount-basis");
		if (!original) return;
		const next = hidden ? original.replace(AMOUNT_IN_TEXT, mask) : original;
		// 금액이 본문 글자에 들어 있는 안내문(툴팁도 sr-only 도 아닌 자리)은 글자 자체를 바꿔야 한다 -
		// 실측 2026-09-12: 자산 현황의 "이 표에 없는 35종목의 실현손익 85,279,840원…" 이 가리기를 켜도 그대로 보였다.
		if (el.getAttribute("data-amount-basis-text") !== null) {
			if (el.textContent !== next) el.textContent = next;
			n++;
			return;
		}
		if (el.getAttribute("title") !== next) el.setAttribute("title", next);
		const sr = el.querySelector(".sr-only");
		if (sr && sr.textContent !== next) sr.textContent = next;
		n++;
	});
	// 흐리게 하는 자리(.amount-value)에 붙은 title 은 흐림이 안 걸린다 - 켜 놓고 hover 하면 정확한 금액이 그대로 뜬다.
	// 실측 2026-09-12(가리기 켜고 9 화면): 31 개가 그랬다(대시보드 "1,622,109,770원" · 종목 상세 "1,359,088,500원" 등).
	// 템플릿 52 곳이 이런 title 을 내므로 하나씩 표식을 다는 대신 여기서 한 번에 가린다. 원본은 되돌리려고 남겨 둔다.
	root.querySelectorAll(".amount-value[title]").forEach((el) => {
		const kept = el.getAttribute("data-amount-title") ?? el.getAttribute("title");
		if (kept === null) return;
		if (el.getAttribute("data-amount-title") === null) el.setAttribute("data-amount-title", kept);
		const next = hidden ? kept.replace(AMOUNT_IN_TEXT, mask) : kept;
		if (el.getAttribute("title") !== next) el.setAttribute("title", next);
		// 같은 값을 낭독기에 주는 sr-only 짝도 함께 가린다 - 2026-09-12 에 title 만 가렸더니
		// 대시보드 10 곳에서 title 은 "가림" 인데 sr-only 는 "1,622,109,770원" 그대로였다.
		// data-amount-basis 쪽은 예전부터 둘을 같이 가려 왔다 - 규칙을 맞춘다.
		const srExact = el.querySelector(".sr-only");
		if (srExact) {
			const keptSr = el.getAttribute("data-amount-sr") ?? (srExact.textContent || "");
			if (el.getAttribute("data-amount-sr") === null) el.setAttribute("data-amount-sr", keptSr);
			const nextSr = hidden ? keptSr.replace(AMOUNT_IN_TEXT, mask) : keptSr;
			if (srExact.textContent !== nextSr) srExact.textContent = nextSr;
		}
		n++;
	});
	return n;
}
document.addEventListener("DOMContentLoaded", () => maskAmountBasis());
document.addEventListener("htmx:afterSettle", () => maskAmountBasis());
let chartSummaryHiddenState = false;
function watchHideAmounts(): void {
	const root = document.documentElement;
	if (!root || typeof MutationObserver !== "function") return;
	chartSummaryHiddenState = chartSummaryAmountsHidden();
	new MutationObserver(() => {
		const now = chartSummaryAmountsHidden();
		if (now === chartSummaryHiddenState) return;
		chartSummaryHiddenState = now;
		resyncChartSummaries();
		maskAmountBasis();
	}).observe(root, { attributes: true, attributeFilter: ["class"] });
}
watchHideAmounts();
// (counts 는 chartSummaryCounts 참고)
const chartSummaryPlugin = {
	id: "a11ySummary",
	afterInit: (chart: any) => { syncChartSummary(chart); },
	afterUpdate: (chart: any) => { syncChartSummary(chart); },
};
(globalThis as any).__chartSummaryInternals = { chartSummaryText, syncChartSummary, chartSummaryPlugin, chartSummaryAmountsHidden, resyncChartSummaries, maskAmountBasis };

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
	// 행 머리 칸의 이름은 자식들의 글자를 이어 붙여 만들어진다 - 체크박스 이름까지 삼켜
	// "삼성전자 선택 삼성전자" 가 된다(실측 2026-09-12: 세 화면 행머리 127 중 83). 칸이 읽히는
	// 자리마다(행 안의 칸을 옮길 때마다) 같은 이름을 두 번 듣게 되므로 칸에 이름을 직접 준다.
	// 넣기 전에 읽어야 한다 - 넣고 나면 사본에 빈 label 이 섞인다.
	if (cell.tagName === "TH" && !cell.getAttribute("aria-label")) {
		const bareName = rowSelectName(row, "{0}");
		if (bareName) cell.setAttribute("aria-label", bareName);
	}
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

// ---- 머리 줄의 전체 선택 체크박스(사용자 요청 2026-10-27 "모든 선택 처리에 대해 일괄 적용") ----
// 행 체크박스가 있는 표마다 머리 줄 첫 칸에 하나 붙인다. 누르면 보이는 행을 하나씩 클릭한 것과 같게 만든다 -
// 표마다 따로 있는 선택 코드와 선택 합산이 그대로 돈다(행 체크박스와 같은 길). 일부만 고르면 '일부' 표시(indeterminate).
// 머리 줄이 없는 표(배당 변화 기여 목록)는 붙일 자리가 없어 건너뛴다.
const ROW_SELECT_ALL_BOX = "[data-row-select-all]";

/** 첫 열의 머리 칸. 첫 줄이 묶음 머리(colspan)로 시작하면 그 아래 줄에서 찾는다(rowspan 으로 내려온 칸은 첫 줄에 있다). */
function selectAllHeaderCell(table: Element): Element | null {
	const head = table.querySelector("thead");
	if (!head) return null;
	const headRows = head.querySelectorAll("tr");
	for (let i = 0; i < headRows.length; i++) {
		const cell = headRows[i].querySelector("th, td");
		if (cell && Number(cell.getAttribute("colspan") || "1") <= 1) return cell;
	}
	return null;
}

/** 보이는 선택 가능한 행 - 거르기로 숨긴 행은 전체 선택에 넣지 않는다. */
function visibleSelectableRows(table: Element): HTMLElement[] {
	const rows = table.querySelectorAll<HTMLElement>(ROW_SELECT_ROW);
	const out: HTMLElement[] = [];
	for (let i = 0; i < rows.length; i++) {
		const row = rows[i];
		if (row.hidden || row.classList.contains("hidden")) continue;
		if (typeof row.getClientRects === "function" && row.getClientRects().length === 0) continue;
		out.push(row);
	}
	return out;
}

/** 머리 체크박스를 행들의 선택 상태에 맞춘다: 모두 → 체크, 일부 → 일부, 없음 → 빈 칸. */
function syncSelectAll(table: Element | null): void {
	if (!table) return;
	const box = table.querySelector<HTMLInputElement>(ROW_SELECT_ALL_BOX);
	if (!box) return;
	const rows = visibleSelectableRows(table);
	let selected = 0;
	for (const row of rows) if (isRowSelected(row)) selected++;
	box.checked = rows.length > 0 && selected === rows.length;
	box.indeterminate = selected > 0 && selected < rows.length;
	box.disabled = rows.length === 0;
}

/** 행 체크박스가 있는 표의 머리 칸에 전체 선택 체크박스를 붙인다(이미 있으면 상태만). 붙였으면 true. */
function ensureSelectAllCheckbox(table: Element): boolean {
	if (table.querySelector(ROW_SELECT_ALL_BOX)) {
		syncSelectAll(table);
		return false;
	}
	const firstRow = table.querySelector(ROW_SELECT_ROW);
	const cell = firstRow ? selectAllHeaderCell(table) : null;
	if (!firstRow || !cell) return false;
	const template = firstRow.getAttribute("data-row-select") || "{0}";
	const label = document.createElement("label");
	label.className = "row-select-box";
	const box = document.createElement("input");
	box.type = "checkbox";
	box.className = "checkbox checkbox-sm";
	box.setAttribute("data-row-select-all", "");
	box.setAttribute("aria-label", template.indexOf("{0}") < 0 ? template : template.replace("{0}", isKoreanDocument() ? "전체" : "all").trim());
	label.appendChild(box);
	cell.insertBefore(label, cell.firstChild);
	syncSelectAll(table);
	return true;
}

/** 전체 선택을 누르면 보이는 행 중 상태가 다른 것만 클릭한다. 행 코드가 거부한 행이 있으면 머리가 '일부' 로 되맞춰진다. */
function applySelectAll(box: HTMLInputElement): void {
	const table = box.closest("table");
	if (!table) return;
	const target = box.checked;
	for (const row of visibleSelectableRows(table)) {
		if (isRowSelected(row) !== target) row.click();
	}
	syncSelectAll(table);
}

/** 문서(또는 가지) 안에서 행 체크박스가 있는 표마다 전체 선택을 붙인다. 붙인 수를 돌려준다. */
function markSelectAllCheckboxes(root: ParentNode = document): number {
	const rows = root.querySelectorAll(ROW_SELECT_ROW);
	const self = root as unknown as Element;
	const tables: Element[] = [];
	const add = (row: Element) => {
		const table = row.closest("table");
		if (table && tables.indexOf(table) < 0) tables.push(table);
	};
	for (let i = 0; i < rows.length; i++) add(rows[i]);
	if (typeof self.matches === "function" && self.matches(ROW_SELECT_ROW)) add(self);
	let added = 0;
	for (const table of tables) if (ensureSelectAllCheckbox(table)) added++;
	return added;
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
					syncSelectAll(target.closest("table"));
				}
				continue;
			}
			mutation.addedNodes.forEach((added) => {
				const element = added as unknown as Element;
				if (element && typeof element.querySelectorAll === "function") {
					markRowSelectCheckboxes(element);
					markSelectAllCheckboxes(element);
				}
			});
		}
	});
	observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ["aria-selected"] });
	return observer;
}

function startRowSelection(): void {
	const run = () => {
		markRowSelectCheckboxes();
		markSelectAllCheckboxes();
		observeRowSelection();
		document.addEventListener("change", (event) => {
			const target = event.target;
			if (target instanceof HTMLInputElement && target.matches(ROW_SELECT_BOX)) reconcileRowSelection(target);
			else if (target instanceof HTMLInputElement && target.matches(ROW_SELECT_ALL_BOX)) applySelectAll(target);
		});
	};
	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", run, { once: true });
		return;
	}
	run();
}
startRowSelection();
(globalThis as any).__rowSelectInternals = { rowSelectName, ensureRowCheckbox, markRowSelectCheckboxes, reconcileRowSelection, observeRowSelection, selectAllHeaderCell, visibleSelectableRows, syncSelectAll, ensureSelectAllCheckbox, applySelectAll, markSelectAllCheckboxes };

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
	// 키보드로 칩을 고르는 중이면 굴리지 않는다 - 칩에 포커스를 둔 채 ↓·PageDown 으로 페이지가 움직이면 현재 구역 칩으로 되굴러
	// 포커스한 칩이 줄 밖으로 나갔다(실측 2026-09-17, section-nav-keyscroll.js: 21번 중 21번). 마우스로 누른 칩은 계속 따라간다.
	const list = nav.querySelector("ul");
	const currentLink = links[current] as HTMLElement | undefined;
	const focused = document.activeElement as HTMLElement | null;
	const choosingByKeyboard = !!focused && nav.contains(focused) && focused.matches(":focus-visible");
	if (list && currentLink && !choosingByKeyboard && list.scrollWidth > list.clientWidth) {
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

/**
 * 붙박이 상단바의 실제 높이를 --site-header-height 로 알린다. 구역 막대의 top 과 html scroll-padding-top 이 이 값을 따른다.
 * 상단바는 폭·글꼴에 따라 줄이 접혀 64~320px 가 되는데(실측 2026-09-17) 71px 고정값이던 때는 접힌 만큼 막대가 상단바 밑에 깔렸다.
 * 인쇄 등으로 상단바가 사라져 높이가 0 이면 이전 값을 둔다.
 */
function syncSiteHeaderHeight(header: Element): void {
	const height = header.getBoundingClientRect().height;
	if (height > 0) document.documentElement.style.setProperty("--site-header-height", height + "px");
}

/**
 * 붙박이 필터 줄(.table-filter-sticky)까지 더한 높이를 --table-head-sticky-top 으로 알린다.
 * 표 머리글이 그 아래에 붙어야 겹치지 않는데, 필터 줄 높이는 글월 길이 · 로케일 · 폭에 따라 달라져
 * CSS 로는 알 수 없다(실측 2026-09-22: 1440px 216px, 1024px 336px, 390px 515px).
 * 필터 줄이 없는 화면에서는 상단바 높이만 쓴다.
 */
function syncStickyStackTop(): void {
	const header = document.querySelector("header.navbar");
	const headerHeight = header ? header.getBoundingClientRect().height : 0;
	const holder = document.querySelector(".table-filter-sticky");
	// 붙지 않는 폭(좁은 화면)에서는 그 줄이 자리를 차지하지 않는다 - position 으로 가린다.
	const stuck = holder != null && getComputedStyle(holder).position === "sticky";
	const holderHeight = stuck ? holder!.getBoundingClientRect().height : 0;
	const top = headerHeight + holderHeight;
	if (top > 0) document.documentElement.style.setProperty("--table-head-sticky-top", top + "px");
	syncStickyTheadHeights();
}

/**
 * 머리글이 붙는 표마다 머리 줄 높이를 --sticky-thead-height 로 알린다(표 상자에 걸어 본문이 물려받는다).
 * 본문 칸에 포커스가 가면 브라우저는 scroll-padding-top(상단바 + 구역 막대)만큼만 비우고 굴린다 - 그 아래 붙은
 * 필터 줄 · 머리 줄 밑으로 들어간다. 실측 2026-09-23(1440px, 월배당 ETF): Shift+Tab 으로 되짚은 종목 링크 21 개가
 * 필터 줄(216px) · 머리 줄 뒤에 전부 가렸다. main.css 가 이 높이로 본문 칸의 scroll-margin-top 을 늘린다.
 */
function syncStickyTheadHeights(): void {
	document.querySelectorAll<HTMLElement>(".table-head-sticky, .table-head-sticky-xl").forEach((box) => {
		const head = box.querySelector(":scope > table > thead");
		const height = head ? head.getBoundingClientRect().height : 0;
		if (height > 0) box.style.setProperty("--sticky-thead-height", height + "px");
	});
}

let stickyStackObserver: ResizeObserver | null = null;
function observeStickyStack(observer: ResizeObserver): void {
	const header = document.querySelector("header.navbar");
	if (header) observer.observe(header);
	const holder = document.querySelector(".table-filter-sticky");
	if (holder) observer.observe(holder);
	// 머리 줄은 폭 · 글꼴에 따라 줄이 접혀 높이가 바뀐다.
	document.querySelectorAll(":is(.table-head-sticky, .table-head-sticky-xl) > table > thead").forEach((head) => observer.observe(head));
}
function watchStickyStackTop(): void {
	syncStickyStackTop();
	if (typeof ResizeObserver !== "function") return;
	stickyStackObserver = new ResizeObserver(() => syncStickyStackTop());
	observeStickyStack(stickyStackObserver);
}
// 구역을 바꿔 끼우면(월배당 ETF · 시뮬레이터 비교표, 2026-10-13) 붙는 줄 · 표 상자 · 머리 줄이 새 요소가 된다 - 다시 재고(표 상자의
// --sticky-thead-height 는 새 상자에 없다) 새 요소를 관찰한다. 같은 요소를 다시 observe 해도 한 번만 걸린다.
document.addEventListener("htmx:afterSettle", () => {
	syncStickyStackTop();
	if (stickyStackObserver) observeStickyStack(stickyStackObserver);
});
if (document.readyState === "loading") {
	document.addEventListener("DOMContentLoaded", watchStickyStackTop, { once: true });
} else {
	watchStickyStackTop();
}

function watchSiteHeaderHeight(): void {
	const header = document.querySelector("header.navbar");
	if (!header) return;
	syncSiteHeaderHeight(header);
	if (typeof ResizeObserver === "function") new ResizeObserver(() => syncSiteHeaderHeight(header)).observe(header);
}
if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", watchSiteHeaderHeight, { once: true });
else watchSiteHeaderHeight();
(globalThis as any).__stickyHeaderInternals = { syncSiteHeaderHeight, watchSiteHeaderHeight, syncStickyStackTop, watchStickyStackTop, syncStickyTheadHeights };
(globalThis as any).__sectionNavInternals ={ sectionLabel, pageSections, sectionAnchorId, buildSectionNav, markCurrentSection, renderSectionNav, sectionsContainer };
(globalThis as any).__activityViewInternals = { resolveActivityViewForRequest };

// 서버의 MessageFormat 은 "{0,choice,0#{0} items|1#{0} item|1<{0} items}" 를 수에 맞춰 고르지만 브라우저의 replace 는 그 꼴을 모른다.
// 영어 "1 items" · "1 stocks selected" 를 막으려고(2026-10-01) 같은 꼴만 여기서 골라 준다 - 화면마다 쓰도록 common 에 둔다 - 1 이면 1# 쪽, 그 밖(0 · 소수 · 2 이상)은 1< 쪽. 다른 꼴은 그대로 둔다.
function applyCountChoice(pattern: string, value: number): string {
    // 중괄호는 글자 코드(123 · 125)로 센다 - 문자열 속 중괄호는 최소화기가 그대로 남겨 시험의 함수 추출기가 짝으로 센다.
    const marker = "0,choice,";
    const at = pattern.indexOf(marker);
    if (at < 1 || pattern.charCodeAt(at - 1) !== 123) return pattern;
    const start = at - 1;
    let depth = 1;
    let end = at + marker.length;
    for (; end < pattern.length && depth > 0; end++) {
        const code = pattern.charCodeAt(end);
        if (code === 123) depth++;
        else if (code === 125) depth--;
    }
    if (depth !== 0) return pattern;
    const parts = pattern.slice(at + marker.length, end - 1).split("|");
    const one = parts.find((part) => part.startsWith("1#"));
    const many = parts.find((part) => part.startsWith("1<"));
    if (!one || !many) return pattern;
    return pattern.slice(0, start) + (value === 1 ? one.slice(2) : many.slice(2)) + pattern.slice(end);
}
(globalThis as any).applyCountChoice = applyCountChoice;

// PoE2 키워드 설명(10-04 C111) — 인게임처럼 Alt 를 누르는 동안 툴팁의 강조 용어 정의를 보인다(html.poe-alt → main.css .poe-kw-box).
//   용어 안내([data-kw]) · 제작 카드 상세 줄([data-craft-advanced], 10-22 Q3c — 굴린 값 뒤 범위)이 있는 화면에서만 Alt 기본 동작(떼면 브라우저 메뉴로
//   포커스)을 막는다 — 다른 화면의 Alt 는 그대로
const setPoeAlt = (on: boolean) => document.documentElement.classList.toggle("poe-alt", on);
const POE_ALT_TARGETS = "[data-kw], [data-craft-advanced]";
globalThis.addEventListener("keydown", (event) => {
    if (event.key !== "Alt" || !document.querySelector(POE_ALT_TARGETS)) return;
    event.preventDefault();
    setPoeAlt(true);
});
globalThis.addEventListener("keyup", (event) => {
    if (event.key !== "Alt") return;
    if (document.querySelector(POE_ALT_TARGETS)) event.preventDefault();
    setPoeAlt(false);
});
globalThis.addEventListener("blur", () => setPoeAlt(false));
// 안내 단추를 누르면 펼치고(터치 · 키보드 — Alt 가 없는 환경, 10-04 C128), 펼친 정의 상자를 누르면 접는다
document.addEventListener("click", (event) => {
    const target = event.target as HTMLElement | null;
    const hint = target?.closest<HTMLElement>("[data-kw-hint]");
    const holder = target?.closest<HTMLElement>("[data-kw]");
    if (!holder) return;
    if (hint) {
        holder.classList.add("poe-kw-open");
        hint.setAttribute("aria-expanded", "true");
    } else if (target?.closest("[data-kw-box]") && holder.classList.contains("poe-kw-open")) {
        holder.classList.remove("poe-kw-open");
        holder.querySelector("[data-kw-hint]")?.setAttribute("aria-expanded", "false");
    }
});

// 이미지 실패 처리 — 인라인 onerror 대신(CSP 강제 대비, 10-13 C287). common.js 는 head 에서 동기로 돌아
// 본문 그림보다 먼저 걸린다(error 는 거품이 안 올라와 capture 로 받는다). 동작은 데이터 속성으로 고른다:
//   data-fallback-src="주소"  → 한 번만 그 주소로 바꾼다(고유 그림 → 베이스 그림)
//   data-onerror="remove" · "hide" · "hide-reveal-next"(옆 대체 칸을 보인다) · "text"(data-onerror-text 글자로 바꾼다)
function handleImgError(img: any): string {
    if (!img || img.tagName !== "IMG") return "skip";
    const fallback = img.dataset ? img.dataset.fallbackSrc : undefined;
    if (fallback && !img.dataset.fallbackTried) {
        img.dataset.fallbackTried = "1";
        img.src = fallback;
        return "fallback";
    }
    switch (img.dataset ? img.dataset.onerror : undefined) {
        case "remove":
            img.remove();
            return "remove";
        case "hide":
            img.style.display = "none";
            return "hide";
        case "hide-reveal-next":
            img.style.display = "none";
            img.nextElementSibling?.classList.remove("hidden");
            return "hide-reveal-next";
        case "text":
            img.replaceWith(document.createTextNode(img.dataset.onerrorText || ""));
            return "text";
        default:
            return "none";
    }
}
document.addEventListener("error", (event) => handleImgError(event.target), true);
(globalThis as any).__imgErrorInternals = { handleImgError };

// 화폐를 고르고 아이템을 눌러 적용(2026-10-20 C477 사용자 요청 "제작을 선택하면 마우스 포인터가 해당 화폐 모양이 되고 아이템을 클릭하면 적용 — craftofexile 과 같은 방식").
// [data-craft-arm-scope] 안 name=action 단추(되돌리기 · 새 아이템 · 장막 해제 고르기 · 직접 편집 · 재조합 보관 등은 빼고)를 누르면 바로 보내지 않고 '고른' 상태로 두고,
// [data-craft-arm-target](아이템 카드)을 누르면(또는 Enter) 그 단추를 눌러 보낸다. 같은 단추를 다시 누르거나 Esc 면 취소. 교체 뒤에도 고른 화폐를 유지해 카드를 거듭 누르면 연타.
// 탐침은 window.__craftDirectApply 로 예전처럼 바로 적용(lib.js launch).
const CRAFT_IMMEDIATE = /^(UNDO|UNDO_TO:|RESET|UNVEIL|MANUAL|COMMIT|FORESEE_OFF|STASH|SWAP|UNSTASH|RECOMBINE|SPAM|GHOST)/;
let craftArmed: string | null = null;
let craftPassThrough = false;
function craftArmable(b: Element | null): b is HTMLButtonElement {
	if (!b || b.tagName !== "BUTTON" || b.getAttribute("name") !== "action") return false;
	if (!b.closest("[data-craft-arm-scope]")) return false;
	return !CRAFT_IMMEDIATE.test(b.getAttribute("value") || "");
}
function craftArmedButton(scope: Element): HTMLButtonElement | null {
	if (!craftArmed) return null;
	let found: HTMLButtonElement | null = null;
	scope.querySelectorAll<HTMLButtonElement>('button[name="action"]').forEach((b) => {
		if (!found && b.value === craftArmed) found = b;
	});
	return found;
}
function syncCraftArm(): void {
	document.querySelectorAll("[data-craft-arm-scope]").forEach((scope) => {
		scope.querySelectorAll("[data-craft-armed]").forEach((b) => {
			b.removeAttribute("data-craft-armed");
			b.removeAttribute("aria-pressed");
		});
		let btn = craftArmedButton(scope);
		if (btn && btn.disabled) {
			craftArmed = null;
			btn = null;
		}
		if (btn) {
			btn.setAttribute("data-craft-armed", "");
			btn.setAttribute("aria-pressed", "true"); // 화면 낭독기가 고른 상태를 알게(Q7)
		}
		const target = scope.querySelector<HTMLElement>("[data-craft-arm-target]");
		if (target) {
			const img = btn?.querySelector("img") as HTMLImageElement | null;
			target.style.cursor = btn ? (img ? `url("${img.src}") 16 16, crosshair` : "crosshair") : "";
			target.toggleAttribute("data-craft-arm-on", !!btn);
			if (btn) target.setAttribute("tabindex", "0");
		}
		const hint = scope.querySelector<HTMLElement>("[data-craft-arm-hint]");
		if (hint) {
			hint.hidden = !btn;
			const label = hint.querySelector("[data-craft-arm-label]");
			if (label && btn) label.textContent = (btn.textContent || "").replace(/\s+/g, " ").trim() || btn.value;
		}
	});
	// 고른 화폐 기준 옵션 표(P3) — 고르기 · 풀기 · 교체 뒤
	scheduleCraftPool();
}
// 제작 요청이 도는 동안의 카드 클릭은 하나만 기억했다가 교체가 끝난 뒤 보낸다(10-23 Q34 UX QA — 120ms 연타에 htmx 가 아직 처리하지 않은 새 폼이
// 일반 GET 으로 제출돼 페이지가 다시 로드되고 단계 기록 · 되돌리기가 사라졌다, PoE2 는 아이템까지 바뀜)
let craftBusy = false;
let craftBusySince = 0;
let craftPending: Element | null = null;
let craftXhr: unknown = null;
const craftReq = (e: Event) => {
	const d = (e as CustomEvent).detail;
	const elt = d?.elt as Element | undefined;
	return !!elt?.closest?.("[data-craft-arm-scope]") && /\/htmx\/craft$/.test(String(d?.pathInfo?.requestPath || d?.requestConfig?.path || ""));
};
document.addEventListener("htmx:beforeRequest", (e) => {
	if (!craftReq(e)) return;
	craftBusy = true;
	craftBusySince = Date.now();
	craftXhr = (e as CustomEvent).detail?.xhr ?? null;
});
document.addEventListener("htmx:afterRequest", (e) => {
	const d = (e as CustomEvent).detail;
	if (craftReq(e) && d?.xhr === craftXhr && !d?.successful) {
		craftBusy = false;
		craftPending = null;
		craftXhr = null;
	}
});
document.addEventListener("htmx:afterSettle", (e) => {
	if (!craftBusy || !craftReq(e) || (e as CustomEvent).detail?.xhr !== craftXhr) return;
	craftXhr = null;
	craftBusy = false;
	const scope = craftPending;
	craftPending = null;
	if (scope && craftArmed) {
		// 교체로 바뀐 같은 칸(교체 뒤 새 요소)
		const live = scope.isConnected ? scope : document.querySelector("[data-craft-arm-scope]");
		if (live) craftFire(live);
	}
});
function craftFire(scope: Element): boolean {
	const b = craftArmedButton(scope);
	if (!b || b.disabled) return false;
	if (craftBusy && Date.now() - craftBusySince < 15_000) {
		craftPending = scope;
		return true;
	}
	craftPassThrough = true;
	try {
		b.click();
	} finally {
		craftPassThrough = false;
	}
	return true;
}
document.addEventListener(
	"click",
	(event) => {
		if ((globalThis as any).__craftDirectApply || craftPassThrough) return;
		const t = event.target as HTMLElement | null;
		const btn = t?.closest?.("button") as Element | null;
		if (craftArmable(btn)) {
			event.preventDefault();
			event.stopPropagation();
			craftArmed = craftArmed === btn.value ? null : btn.value;
			syncCraftArm();
			if (craftArmed && window.innerWidth < 1024) {
				const card = btn.closest("[data-craft-arm-scope]")?.querySelector<HTMLElement>("[data-craft-arm-target]");
				const r = card?.getBoundingClientRect();
				if (card && r && (r.top > window.innerHeight - 80 || r.bottom < 0)) card.scrollIntoView({ block: "nearest", behavior: "auto" });
			}
			return;
		}
		const target = t?.closest?.("[data-craft-arm-target]");
		const scope = target?.closest?.("[data-craft-arm-scope]");
		if (target && scope && craftArmed && craftFire(scope)) event.preventDefault();
	},
	true,
);
document.addEventListener("keydown", (event) => {
	if (!craftArmed) return;
	// 글자를 입력하는 칸만(Q15 검토 — 체크박스 · 라디오 · select 에 포커스가 남으면 Esc 로 고르기를 못 풀었다)
	const inField = (event.target as HTMLElement | null)?.closest?.('input:not([type="checkbox"]):not([type="radio"]), textarea');
	if (event.key === "Escape" && !inField) {
		craftArmed = null;
		syncCraftArm();
		return;
	}
	const target = (event.target as HTMLElement | null)?.closest?.("[data-craft-arm-target]");
	const scope = target?.closest?.("[data-craft-arm-scope]");
	if ((event.key === "Enter" || event.key === " ") && target && scope && craftFire(scope)) event.preventDefault();
});
document.addEventListener("htmx:afterSettle", syncCraftArm);

// 고른 화폐 기준 옵션 표(2026-10-22 P3, poedb 속성 부여 — 화석을 고르면 같은 표가 그 기준으로): 화폐를 고르면(craftArmed) 계열 표 칸
// [data-craft-pool-groups=주소] 를 그 수단이 처음 뽑는 옵션의 확률 · 배수로 바꿔 끼우고, 풀면 지금 아이템 기준으로 되돌린다. 고른 화폐의 고르기 값
// (에센스 · 화석 · 작업대 · 수확)이 바뀌어도 다시. 화폐를 써서 칸이 새로 그려지면(지금 아이템 기준) 고른 채면 다시 바꾼다.
// 칸 속성 data-craft-pool-fields(보낼 아이템 칸) · data-craft-pool-inputs(고르기 값) — 없으면 PoE1 이름(10-22 Q2 PoE2 짝: poolKey · 등급 · 징조 · 뼈)
const CRAFT_POOL_INPUTS = ["essence", "fossil", "bench", "harvest"];
const CRAFT_POOL_FIELDS = ["itemClass", "variant", "influence", "ilvl", "state"];
const CRAFT_POOL_ALL_INPUTS = [...CRAFT_POOL_INPUTS, "tier", "omen", "bone"];
function craftPoolKey(armed: string | null, entries: [string, string][], inputs: string[] = CRAFT_POOL_INPUTS): string {
	if (!armed) return "";
	return armed + "|" + entries.filter(([k]) => inputs.includes(k)).map(([k, v]) => `${k}=${v}`).join("&");
}
function craftPoolLabel(btn: HTMLButtonElement, form: HTMLFormElement): string {
	const base = (btn.textContent || "").replace(/\s+/g, " ").trim() || btn.value;
	const act = btn.value.split(":")[0];
	const picked = (name: string) => form.querySelector<HTMLSelectElement>(`select[name="${name}"]`)?.selectedOptions[0]?.textContent?.trim() || "";
	let extra = "";
	if (act === "ESSENCE" || act === "BENCH" || act === "HARVEST") extra = picked(act.toLowerCase());
	else if (act === "FOSSIL")
		extra = Array.from(form.querySelectorAll<HTMLInputElement>('input[name="fossil"]:checked'))
			.map((c) => c.closest("label")?.querySelector(".font-medium")?.textContent?.trim() || c.value)
			.join(" + ");
	return extra ? `${base} (${extra})` : base;
}
function refreshCraftPool(): void {
	document.querySelectorAll<HTMLElement>("[data-craft-arm-scope]").forEach((scope) => {
		const box = scope.querySelector<HTMLElement>("[data-craft-pool-groups]");
		const form = scope.querySelector<HTMLFormElement>("form[data-craft-toolbar]");
		if (!box || !form) return;
		const btn = craftArmedButton(scope);
		const armed = btn && !btn.disabled ? craftArmed : null;
		const entries: [string, string][] = [];
		new FormData(form).forEach((v, k) => {
			if (typeof v === "string") entries.push([k, v]);
		});
		const inputs = box.dataset.craftPoolInputs ? box.dataset.craftPoolInputs.split(",") : CRAFT_POOL_INPUTS;
		const fields = box.dataset.craftPoolFields ? box.dataset.craftPoolFields.split(",") : CRAFT_POOL_FIELDS;
		const key = craftPoolKey(armed, entries, inputs);
		if ((box.dataset.craftPoolKey || "") === key) return;
		box.dataset.craftPoolKey = key;
		const values: Record<string, string> = {};
		for (const n of fields) {
			const el = form.querySelector<HTMLInputElement>(`input[name="${n}"]`);
			if (el) values[n] = el.value;
		}
		// 여러 화석 · 징조는 쉼표 하나로(게이트가 List 로 나눈다)
		for (const [k, v] of entries) if (inputs.includes(k)) values[k] = values[k] ? `${values[k]},${v}` : v;
		if (armed && btn) {
			values.action = armed;
			values.actionLabel = craftPoolLabel(btn, form);
		}
		// source = 칸(요청 요소) — 없으면 body 라 [data-no-success-toast] 를 못 찾아 고를 때마다 'Done.' 알림이 떴다
		(globalThis as any).htmx?.ajax("POST", box.dataset.craftPoolGroups, { source: box, target: box, swap: "innerHTML", values });
	});
}
// 요청이 실패하면 열쇠를 지워 같은 화폐를 다시 골라도 다시 받게(Q3e 검토 — 열쇠를 먼저 박아 실패 뒤 예전 표가 남았다)
for (const ev of ["htmx:responseError", "htmx:sendError", "htmx:timeout"]) {
	document.addEventListener(ev, (event: any) => {
		const t = event.detail?.target as HTMLElement | undefined;
		if (t?.hasAttribute?.("data-craft-pool-groups")) delete t.dataset.craftPoolKey;
	});
}
let craftPoolTimer: ReturnType<typeof setTimeout> | null = null;
function scheduleCraftPool(): void {
	if (craftPoolTimer) clearTimeout(craftPoolTimer);
	craftPoolTimer = setTimeout(refreshCraftPool, 120);
}
document.addEventListener("change", (event) => {
	const t = event.target as HTMLInputElement | null;
	if (t?.closest?.("[data-craft-arm-scope]") && CRAFT_POOL_ALL_INPUTS.includes(t.name)) scheduleCraftPool();
});
(globalThis as any).__craftPoolInternals = { craftPoolKey };

// 같은 옵션을 얻는 다른 길(2026-10-22 P4 — craftFamilyWays.jte): 단추를 누르면 그 화폐의 고르기 값(에센스 · 작업대 · 수확 select, 화석은 그 하나만 체크)을
// 맞추고 그 화폐를 고른 상태로 — 옵션 표가 그 기준으로 바뀐다(P3). 지금 아이템에 못 쓰는 값 · 화폐면 고르지 않고 그 칸에 안내
function wayValueUsable(form: HTMLFormElement, act: string, value: string): boolean {
	if (act === "FOSSIL") {
		const c = Array.from(form.querySelectorAll<HTMLInputElement>('input[name="fossil"]')).find((x) => x.value === value);
		return !!c && !c.disabled;
	}
	const sel = form.querySelector<HTMLSelectElement>(`select[name="${act.toLowerCase()}"]`);
	return !!sel && Array.from(sel.options).some((o) => o.value === value && !o.disabled);
}
// 다른 길 칩을 미리 맞춤(Q7 UX QA — 눌러 봐야 '쓸 수 없습니다'를 알았다): 지금 아이템에 못 쓰는 화폐 · 값이면 끄고 까닭(화폐 단추 title · 꺼진 옵션 글)
/** 못 쓰는 까닭(Q7) — 화폐 단추 title · 꺼진 고르개 옵션 글. 칩 title · 누를 때 안내에 같이 쓴다. */
function wayChipWhy(b: HTMLElement): string {
	const scope = b.closest("[data-craft-arm-scope]");
	const form = scope?.querySelector<HTMLFormElement>("form[data-craft-toolbar]");
	const act = b.dataset.craftWay || "";
	const value = b.dataset.craftWayValue || "";
	const actBtn = scope?.querySelector<HTMLButtonElement>(`button[name="action"][value="${act}"]`);
	const opt = form?.querySelector<HTMLSelectElement>(`select[name="${act.toLowerCase()}"]`)?.querySelector<HTMLOptionElement>(`option[value="${CSS.escape(value)}"]`);
	return (actBtn?.disabled && actBtn.title) || (opt?.disabled && opt.textContent?.trim()) || "";
}
/**
 * 다른 길 칩 켜고 끄기 — 못 쓰면 disabled 대신 aria-disabled(10-23 Q21 — disabled 면 Tab 으로 못 닿아 까닭(title)을 키보드로 읽을 수 없었다).
 * 누르면 클릭 처리기가 까닭을 안내 줄에. 서버가 준 title 은 data-way-title 에 두고 다시 쓸 수 있게 되면 되돌린다(전엔 한 번 꺼지면 '쓸 수 없습니다'가 남았다).
 */
function syncWayChips(root: ParentNode = document): void {
	root.querySelectorAll<HTMLButtonElement>("button[data-craft-way]").forEach((b) => {
		const scope = b.closest("[data-craft-arm-scope]");
		const form = scope?.querySelector<HTMLFormElement>("form[data-craft-toolbar]");
		const act = b.dataset.craftWay || "";
		const value = b.dataset.craftWayValue || "";
		const actBtn = scope?.querySelector<HTMLButtonElement>(`button[name="action"][value="${act}"]`);
		const ok = !!form && !!actBtn && !actBtn.disabled && wayValueUsable(form, act, value);
		if (b.dataset.wayTitle === undefined) b.dataset.wayTitle = b.title;
		b.disabled = false;
		b.setAttribute("aria-disabled", ok ? "false" : "true");
		if (ok) {
			b.title = b.dataset.wayTitle || "";
		} else {
			const ko = (document.documentElement.lang || "").startsWith("ko");
			const why = wayChipWhy(b);
			b.title = (ko ? "지금 아이템에는 쓸 수 없습니다" : "Can't be used on the current item") + (why ? " — " + why : "");
		}
	});
}
document.addEventListener("htmx:afterSettle", () => syncWayChips());
// PoE2 에센스 칩은 서버가 처음부터 그린다 — 첫 로딩에도
document.addEventListener("DOMContentLoaded", () => syncWayChips());
// 수치 범위 단추(Q7 — Alt 를 모르거나 터치 기기): 누르는 동안이 아니라 켜고 끄기, Alt 와 같은 표시
document.addEventListener("click", (event) => {
	const b = (event.target as HTMLElement | null)?.closest?.<HTMLElement>("[data-craft-alt-toggle]");
	if (!b) return;
	const on = !document.documentElement.classList.contains("poe-alt-lock");
	document.documentElement.classList.toggle("poe-alt-lock", on);
	document.querySelectorAll("[data-craft-alt-toggle]").forEach((x) => x.setAttribute("aria-pressed", on ? "true" : "false"));
});
document.addEventListener("htmx:afterSettle", () => {
	const on = document.documentElement.classList.contains("poe-alt-lock");
	document.querySelectorAll("[data-craft-alt-toggle]").forEach((x) => x.setAttribute("aria-pressed", on ? "true" : "false"));
});
// 계열을 펼치면 '다른 길'을 불러온다(펼침 유지로 다시 열릴 때도 — toggle 은 거품이 안 올라 잡기 단계로)
function loadFamilyWays(d: Element): void {
	const slot = d.querySelector<HTMLElement>("[data-craft-ways-slot]");
	if (!slot || slot.dataset.loading || !slot.dataset.craftWaysUrl) return;
	slot.dataset.loading = "1";
	(globalThis as any).htmx?.ajax("GET", slot.dataset.craftWaysUrl, { target: slot, swap: "outerHTML" });
}
document.addEventListener(
	"toggle",
	(event) => {
		const d = event.target as HTMLDetailsElement | null;
		if (d?.matches?.("details[data-craft-fam]") && d.open) loadFamilyWays(d);
	},
	true,
);
document.addEventListener("htmx:afterSettle", () => document.querySelectorAll("details[data-craft-fam][open]").forEach(loadFamilyWays));
document.addEventListener("click", (event) => {
	const b = (event.target as HTMLElement | null)?.closest?.<HTMLElement>("[data-craft-way]");
	if (!b) return;
	const scope = b.closest("[data-craft-arm-scope]");
	const form = scope?.querySelector<HTMLFormElement>("form[data-craft-toolbar]");
	const act = b.dataset.craftWay || "";
	const value = b.dataset.craftWayValue || "";
	const msg = b.closest("[data-craft-ways]")?.querySelector<HTMLElement>("[data-craft-way-msg]");
	const ko = (document.documentElement.lang || "").startsWith("ko");
	const say = (text: string) => {
		if (!msg) return;
		msg.textContent = text;
		msg.hidden = text === "";
	};
	const actBtn = scope?.querySelector<HTMLButtonElement>(`button[name="action"][value="${act}"]`);
	if (!scope || !form || !actBtn || actBtn.disabled || !wayValueUsable(form, act, value)) {
		// 까닭도 같이(Q21 — 꺼진 칩도 키보드로 눌러 읽는다)
		const why = wayChipWhy(b);
		say((ko ? "지금 아이템에는 이 화폐를 쓸 수 없습니다" : "This currency can't be used on the current item") + (why ? " — " + why : ""));
		// 미리 꺼진 칩(aria-disabled)은 까닭만 — 고른 화폐는 그대로(Q23 검토). 켜져 보였는데 못 쓰면 이전 고르기를 남기지 않는다(Q7)
		if (b.getAttribute("aria-disabled") !== "true") {
			craftArmed = null;
			syncCraftArm();
		}
		return;
	}
	if (act === "FOSSIL") {
		form.querySelectorAll<HTMLInputElement>('input[name="fossil"]').forEach((c) => {
			const on = c.value === value;
			if (c.checked !== on) {
				c.checked = on;
				c.dispatchEvent(new Event("change", { bubbles: true }));
			}
		});
	} else {
		const sel = form.querySelector<HTMLSelectElement>(`select[name="${act.toLowerCase()}"]`)!;
		if (sel.value !== value) {
			sel.value = value;
			sel.dispatchEvent(new Event("change", { bubbles: true }));
		}
	}
	say("");
	craftArmed = act;
	syncCraftArm();
});

// 꺼진 옵션이 골라진 채 남지 않게(2026-10-20 C478b): 아이템이 바뀌어 지금 고른 옵션이 꺼지면(값 유지 장치가 예전 값을 되살려도) 첫 켜진 옵션으로.
// 꺼진 option 은 폼에 실리지 않아 '모르는 수확 제작'으로 실패했다(허리띠 레어 단추 전수 탐침).
function fixDisabledSelections(): void {
	document.querySelectorAll<HTMLSelectElement>("[data-craft-arm-scope] select").forEach((sel) => {
		const cur = sel.selectedOptions && sel.selectedOptions[0];
		if (!cur || !cur.disabled) return;
		const first = Array.from(sel.options).find((o) => !o.disabled);
		if (first) sel.value = first.value;
	});
}
document.addEventListener("DOMContentLoaded", fixDisabledSelections);
document.addEventListener("htmx:afterSettle", () => setTimeout(fixDisabledSelections, 0));

// Ctrl+Z 되돌리기(2026-10-20 C517, craftofexile 'Ctrl+Z to revert emulator actions'): 제작 화면에서 입력 칸 밖이면 에뮬레이터 '되돌리기' 단추를 누른다(PoE1 · PoE2).
// 한글 입력 상태에선 key 가 'ㅋ' 라 물리 키(code KeyZ)로도 본다. 되돌릴 게 없으면(단추 꺼짐) 브라우저 기본 동작 그대로.
document.addEventListener("keydown", (event) => {
	if (!(event.ctrlKey || event.metaKey) || event.shiftKey || event.altKey) return;
	if (event.code !== "KeyZ" && (event.key || "").toLowerCase() !== "z") return;
	const t = event.target as HTMLElement | null;
	if (t && (t.closest?.("input, textarea, select, [contenteditable]") || t.isContentEditable)) return;
	const btn = document.querySelector<HTMLButtonElement>('#craft-emu button[data-craft-action="UNDO"], #craft2-emu button[data-craft-action="UNDO"]');
	if (!btn || btn.disabled) return;
	event.preventDefault();
	btn.click();
});

// 장막 아이템 우클릭 → 장막 해제(2026-10-20 C482): 인게임 안내 "이 아이템에 우클릭하여 장막을 해제하십시오." 그대로 — 카드에 장막 줄 그림이 있으면 '장막 해제' 단추를 누른다.
document.addEventListener("contextmenu", (event) => {
	const target = (event.target as HTMLElement | null)?.closest?.("[data-craft-arm-target]");
	if (!target || !target.querySelector("[data-craft-veiled-img]")) return;
	const scope = target.closest("[data-craft-arm-scope]");
	const btn = scope?.querySelector<HTMLButtonElement>('button[name="action"][value="UNVEIL_OPEN"]');
	if (!btn || btn.disabled) return;
	event.preventDefault();
	btn.click();
});

// 게임 아이템 붙여넣기(2026-10-20 C485, craftofexile 의 Import item): 제작 화면 아무 데서나 Ctrl+V 하면 게임 복사 글인지 보고 붙여넣기 칸에 넣어 바로 보낸다.
// 입력 칸 안에서 붙여 넣는 건 그대로 둔다(검색 · 수치 칸).
function looksLikeItemText(text: string): boolean {
	return /^(Item Class|아이템 종류|Rarity|아이템 희귀도)\s*:/m.test(text) && /^-{4,}\s*$/m.test(text);
}
document.addEventListener("paste", (event) => {
	const form = document.querySelector<HTMLFormElement>("[data-craft-import-form]");
	if (!form) return;
	const t = event.target as HTMLElement | null;
	if (t && (t.closest("input, textarea, select, [contenteditable]") || t.isContentEditable)) return;
	const text = event.clipboardData?.getData("text/plain") || "";
	if (!looksLikeItemText(text)) return;
	event.preventDefault();
	craftImportFrom(text);
});
/** 게임 글을 붙여넣기 칸에 넣어 보낸다(C485 · C545 공용) — 칸이 없거나 게임 글이 아니면 false. */
function craftImportFrom(text: string): boolean {
	const form = document.querySelector<HTMLFormElement>("[data-craft-import-form]");
	const area = form?.querySelector<HTMLTextAreaElement>("[data-craft-import-text]");
	if (!form || !area || !looksLikeItemText(text)) return false;
	area.value = text;
	const box = form.closest("details");
	if (box) box.open = true;
	form.requestSubmit();
	return true;
}
// 아이템 붙여넣기 단추(2026-10-21 C545 사용자 요청 "아이템 글 복사가 있는데 인게임에서 복사한 아이템 붙여넣기도"): 카드 아래 '아이템 글 복사'의 짝.
// 클립보드를 읽어 게임 글이면 바로 보내고, 읽을 수 없거나(권한 거부 · 미지원) 게임 글이 아니면 위 붙여넣기 칸을 열어 커서를 두고 Ctrl+V 를 안내한다.
document.addEventListener("click", (event) => {
	const b = (event.target as HTMLElement | null)?.closest?.<HTMLElement>("[data-craft-import-paste]");
	if (!b) return;
	const ko = (document.documentElement.lang || "").startsWith("ko");
	const msg = b.closest("[data-craft-export-row]")?.querySelector<HTMLElement>("[data-craft-export-msg]");
	const say = (s: string) => {
		if (!msg) return;
		msg.textContent = s;
		setTimeout(() => {
			if (msg.textContent === s) msg.textContent = "";
		}, 5000);
	};
	const openBox = (why: string) => {
		const form = document.querySelector<HTMLFormElement>("[data-craft-import-form]");
		const box = form?.closest("details");
		if (box) box.open = true;
		const area = form?.querySelector<HTMLTextAreaElement>("[data-craft-import-text]");
		area?.scrollIntoView({ block: "center" });
		area?.focus();
		say(why);
	};
	if (!navigator.clipboard?.readText) {
		openBox(ko ? "위 붙여넣기 칸에 Ctrl+V" : "Press Ctrl+V in the paste box above");
		return;
	}
	navigator.clipboard.readText().then(
		(text) => {
			if (craftImportFrom(text)) say(ko ? "붙여 넣었습니다" : "Pasted");
			else openBox(ko ? "클립보드에 게임 아이템 글이 없습니다 — 게임에서 아이템에 마우스를 올리고 Ctrl+C" : "No game item text on the clipboard — hover an item in game and press Ctrl+C");
		},
		() => openBox(ko ? "클립보드 읽기가 막혀 있습니다 — 위 칸에 Ctrl+V" : "Clipboard access is blocked — press Ctrl+V in the box above"),
	);
});

// 에센스 고르기(2026-10-21 C547 사용자 요청 "에센스 셀렉트가 너무 길다 — 종류를 아이콘으로 고르고 등급을 고르는 식"): 숨은 select[name=essence] 가 값의
// 주인(서버 · 열린 값 유지 · 적중 확률 그대로). 종류 단추 = 그 종류의 등급 줄을 보이고 지금 등급과 같은 단계(없으면 쓸 수 있는 가장 높은 등급)를 고름,
// 등급 단추 = 그 에센스. 고르면 select 에 change 를 보내고, select 가 바뀌면(탐침 · 값 유지) 단추 표시를 맞춘다.
function syncEssencePicker(root: ParentNode = document): void {
	const boxes = root instanceof Element && root.matches("[data-craft-essence]") ? [root as HTMLElement] : Array.from(root.querySelectorAll<HTMLElement>("[data-craft-essence]"));
	boxes.forEach((box) => {
		const sel = box.querySelector<HTMLSelectElement>("select[data-craft-ess-select]");
		if (!sel) return;
		const slug = sel.value;
		const pick = Array.from(box.querySelectorAll<HTMLElement>("[data-craft-ess-pick]")).find((b) => b.dataset.craftEssPick === slug);
		const famOfPick = pick?.closest<HTMLElement>("[data-craft-ess-tiers]")?.dataset.craftEssTiers || "";
		const fam = box.dataset.viewFam || famOfPick;
		// 고른 단추 = 화폐 단추와 같은 btn-primary(btn-ghost 와 겹치면 글자가 안 보였다)
		const mark = (b: HTMLElement, on: boolean) => {
			b.setAttribute("aria-pressed", on ? "true" : "false");
			b.classList.toggle("btn-primary", on);
			b.classList.toggle("btn-ghost", !on);
		};
		box.querySelectorAll<HTMLElement>("[data-craft-ess-fam]").forEach((b) => mark(b, b.dataset.craftEssFam === fam));
		box.querySelectorAll<HTMLElement>("[data-craft-ess-tiers]").forEach((row) => (row.hidden = row.dataset.craftEssTiers !== fam));
		box.querySelectorAll<HTMLElement>("[data-craft-ess-pick]").forEach((b) => mark(b, b.dataset.craftEssPick === slug));
		const desc = box.querySelector<HTMLElement>("[data-craft-ess-desc]");
		if (desc) {
			const ko = (document.documentElement.lang || "").startsWith("ko");
			const viewRow = box.querySelector<HTMLElement>(`[data-craft-ess-tiers="${CSS.escape(fam)}"]`);
			const shown = famOfPick === fam || slug === "" ? pick : undefined;
			const why = !shown && viewRow ? viewRow.querySelector<HTMLElement>("[data-craft-ess-pick][data-why]")?.dataset.why : undefined;
			desc.textContent = shown ? shown.dataset.desc || "" : why ? (ko ? "지금 아이템에 못 씀 — " : "Not usable on this item — ") + why : "";
		}
	});
}
document.addEventListener("click", (event) => {
	const t = event.target as HTMLElement | null;
	const pb = t?.closest?.<HTMLButtonElement>("[data-craft-ess-pick]");
	const fb = pb ? null : t?.closest?.<HTMLButtonElement>("[data-craft-ess-fam]");
	if (!pb && !fb) return;
	const box = (pb || fb)!.closest<HTMLElement>("[data-craft-essence]");
	const sel = box?.querySelector<HTMLSelectElement>("select[data-craft-ess-select]");
	if (!box || !sel) return;
	let slug: string | undefined;
	if (pb) {
		slug = pb.dataset.craftEssPick || "";
		delete box.dataset.viewFam;
	} else {
		const fam = fb!.dataset.craftEssFam || "";
		box.dataset.viewFam = fam;
		const row = box.querySelector<HTMLElement>(`[data-craft-ess-tiers="${CSS.escape(fam)}"]`);
		const picks = Array.from(row?.querySelectorAll<HTMLButtonElement>("[data-craft-ess-pick]") || []).filter((x) => !x.disabled);
		const curTier = Array.from(box.querySelectorAll<HTMLElement>("[data-craft-ess-pick]")).find((b) => b.dataset.craftEssPick === sel.value)?.dataset.tier;
		const chosen = picks.find((x) => x.dataset.tier === curTier) || picks[picks.length - 1];
		slug = chosen?.dataset.craftEssPick;
		if (slug !== undefined) delete box.dataset.viewFam;
	}
	if (slug !== undefined && sel.value !== slug) {
		sel.value = slug;
		sel.dispatchEvent(new Event("change", { bubbles: true }));
	}
	syncEssencePicker(box);
});
document.addEventListener("change", (e) => {
	const sel = e.target as HTMLElement | null;
	if (!sel?.matches?.("select[data-craft-ess-select]")) return;
	const box = sel.closest<HTMLElement>("[data-craft-essence]");
	if (box) {
		delete box.dataset.viewFam;
		syncEssencePicker(box);
	}
});
document.addEventListener("htmx:afterSettle", () => syncEssencePicker());
document.addEventListener("DOMContentLoaded", () => syncEssencePicker());

// 계산기 영향력 바로가기(2026-10-21 C548 사용자 요청 "제작 계산기에 정복자 옵션이 안 보인다"): 위쪽 선택 칸의 영향력을 바꿔(change → 그 조건으로 페이지
// 구역을 다시 받음 · 만들던 아이템이 있으면 확인) 그 영향력 옵션을 계산기 목표 목록에 넣는다
document.addEventListener("click", (event) => {
	const b = (event.target as HTMLElement | null)?.closest?.<HTMLElement>("[data-craft-set-influence]");
	if (!b) return;
	const sel = document.querySelector<HTMLSelectElement>('[data-craft-picker] select[name="influence"]');
	if (!sel) return;
	sel.value = b.dataset.craftSetInfluence || "";
	sel.dispatchEvent(new Event("change", { bubbles: true }));
});

// 아이템 글 내보내기 · 공유 링크(2026-10-20 C489, craftofexile 의 RAW 내보내기 · Share link): 게임 고급 복사(Ctrl+Alt+C)와 같은 형식이라
// 이 화면 붙여넣기(C485)로 다시 들어오고 다른 도구에도 붙는다. 머리줄 문구는 게임 ClientStrings(ModDescriptionLinePrefix · Tier 등) 그대로.
function craftExportText(btn: HTMLElement): string {
	const d = btn.dataset;
	const ko = d.exLang === "ko";
	const card = btn.closest("[data-craft-arm-scope]")?.querySelector("[data-craft-tooltip]") || document.querySelector("[data-craft-arm-scope] [data-craft-tooltip]");
	const out: string[] = [];
	const rarity = d.exRarity || "NORMAL";
	const rarName: Record<string, [string, string]> = { NORMAL: ["Normal", "일반"], MAGIC: ["Magic", "마법"], RARE: ["Rare", "희귀"] };
	out.push((ko ? "아이템 종류: " : "Item Class: ") + (d.exClass || ""));
	out.push((ko ? "아이템 희귀도: " : "Rarity: ") + (rarName[rarity] || rarName.NORMAL)[ko ? 1 : 0]);
	if (rarity === "RARE") {
		out.push(d.exName && d.exName !== (ko ? "레어 아이템" : "Rare Item") ? d.exName : ko ? "레어 아이템" : "Rare Item");
		out.push(d.exBase || "");
	} else if (rarity === "MAGIC") {
		out.push(d.exName || d.exBase || "");
	} else {
		out.push(d.exBase || "");
	}
	out.push("--------");
	const quality = card?.querySelector<HTMLElement>("[data-craft-quality]");
	if (quality) {
		out.push(quality.innerText.trim());
		out.push("--------");
	}
	out.push((ko ? "아이템 레벨: " : "Item Level: ") + (d.exIlvl || ""));
	// 군집 주얼 인챈트(C515) — 게임 복사 글처럼 아이템 레벨 다음 칸, 꼬리표 (enchant)
	const enchants = card ? Array.from(card.querySelectorAll<HTMLElement>("[data-craft-enchant] > div")).map((e) => e.innerText.trim()).filter(Boolean) : [];
	// PoE2 바알 타락 인챈트(10-23 Q28 — 카드처럼 인챈트 칸으로, 기본 보기 글만)
	if (card)
		card.querySelectorAll<HTMLElement>('[data-craft-line][data-craft-gen="corrupted"] div.inline').forEach((v) => {
			const t = (v.querySelector(".poe-classic")?.textContent ?? v.innerText).replace(/\s+/g, " ").trim();
			if (t) enchants.push(t);
		});
	if (enchants.length) {
		out.push("--------");
		enchants.forEach((e) => out.push(e + " (enchant)"));
	}
	// 기본 보기 글만(Q3c 상세 줄 · Alt 를 누른 채 복사해도 범위가 섞이지 않게 — Q3e 검토)
	const values = (el: Element) =>
		Array.from(el.querySelectorAll<HTMLElement>("div.inline"))
			.map((v) => (v.querySelector(".poe-classic")?.textContent ?? v.innerText).replace(/\s+/g, " ").trim())
			.filter(Boolean);
	const implicits = card ? Array.from(card.querySelectorAll<HTMLElement>("[data-craft-implicit]")) : [];
	if (implicits.length) {
		out.push("--------");
		implicits.forEach((el) => {
			const side = el.dataset.craftSide;
			const label = el.dataset.craftExTierLabel || "";
			const head = side === "exarch" ? (ko ? `작열의 총주교 고정 속성 (${label})` : `Searing Exarch Implicit Modifier (${label})`) : side === "eater" ? (ko ? `세계 포식자 고정 속성 (${label})` : `Eater of Worlds Implicit Modifier (${label})`) : ko ? "타락 고정 속성 부여" : "Corruption Implicit Modifier";
			out.push(`{ ${head} }`);
			values(el).forEach((v) => out.push(v + " (implicit)"));
		});
	}
	const lines = card ? Array.from(card.querySelectorAll<HTMLElement>("[data-craft-line]")).filter((el) => el.dataset.craftGen !== "corrupted") : [];
	if (lines.length) {
		out.push("--------");
		lines.forEach((el) => {
			const id = el.dataset.craftLine || "";
			const pre = el.dataset.craftGen === "prefix";
			if (id.startsWith("Veiled")) {
				out.push(`{ ${ko ? (pre ? "접두어 속성 부여" : "접미어 속성 부여") : pre ? "Prefix Modifier" : "Suffix Modifier"} }`);
				out.push(ko ? (pre ? "장막 접두어" : "장막 접미어") : pre ? "Veiled Prefix" : "Veiled Suffix");
				return;
			}
			const crafted = el.dataset.craftExCrafted === "1";
			const fractured = el.dataset.craftExFractured === "1";
			const mark = crafted ? (ko ? "대가의 제작 " : "Master Crafted ") : fractured ? (ko ? "분열된 " : "Fractured ") : "";
			const kind = ko ? (pre ? "접두어 속성 부여" : "접미어 속성 부여") : pre ? "Prefix Modifier" : "Suffix Modifier";
			const tier = !crafted && el.dataset.craftExTier ? (ko ? ` (등급: ${el.dataset.craftExTier})` : ` (Tier: ${el.dataset.craftExTier})`) : "";
			const tags = el.dataset.craftExTags ? ` — ${el.dataset.craftExTags}` : "";
			out.push(`{ ${mark}${kind} "${el.dataset.craftExName || ""}"${tier}${tags} }`);
			values(el).forEach((v) => out.push(v + (fractured ? " (fractured)" : crafted ? " (crafted)" : "")));
		});
	}
	if (d.exInfluence || d.exCorrupted === "1") {
		out.push("--------");
		if (d.exInfluence) out.push(d.exInfluence);
		if (d.exCorrupted === "1") out.push(ko ? "타락" : "Corrupted");
	}
	return out.join("\n") + "\n";
}
function craftShareLink(btn: HTMLElement): string {
	const scope = btn.closest("[data-craft-arm-scope]") || document;
	const form = scope.querySelector<HTMLFormElement>("form") || document.querySelector<HTMLFormElement>("#craft-emu form, #craft2-emu form");
	const val = (n: string) => (form?.querySelector<HTMLInputElement>(`input[name="${n}"]`)?.value ?? "");
	const p = new URLSearchParams();
	let path = "/poe/craft";
	if (val("poolKey")) {
		path = "/poe2/craft";
		p.set("pool", val("poolKey"));
		if (val("base")) p.set("base", val("base"));
		// 레어 이름(C518 — PoE2 공유 링크에 빠져 있었다, PoE1 은 C489 부터)
		if (val("itemName")) p.set("itemName", val("itemName"));
		if (val("itemNameKo") && val("itemNameKo") !== val("itemName")) p.set("itemNameKo", val("itemNameKo"));
	} else {
		p.set("itemClass", val("itemClass"));
		p.set("variant", val("variant"));
		p.set("influence", val("influence"));
		if (val("baseName")) p.set("base", val("baseName"));
		if (val("itemName")) p.set("itemName", val("itemName"));
		// 레어 이름 한국어(C518 — 링크로 다시 열면 한국어 화면에 영어 이름이 나왔다)
		if (val("itemNameKo") && val("itemNameKo") !== val("itemName")) p.set("itemNameKo", val("itemNameKo"));
		// 군집 주얼 패시브 수(C531)
		if (val("passives")) p.set("passives", val("passives"));
	}
	p.set("ilvl", val("ilvl"));
	p.set("state", val("state"));
	p.set("rolls", val("rolls"));
	return `${location.origin}${path}?${p.toString()}`;
}
function craftNote(msgFrom: HTMLElement, text: string): void {
	const msg = msgFrom.parentElement?.querySelector<HTMLElement>("[data-craft-export-msg]");
	if (msg) {
		msg.textContent = text;
		setTimeout(() => (msg.textContent = ""), 2500);
	}
}
function craftCopy(text: string, msgFrom: HTMLElement, ok: string): void {
	const done = () => craftNote(msgFrom, ok);
	const fallback = () => {
		const ta = document.createElement("textarea");
		ta.value = text;
		document.body.appendChild(ta);
		ta.select();
		document.execCommand("copy");
		ta.remove();
		done();
	};
	if (navigator.clipboard?.writeText) navigator.clipboard.writeText(text).then(done, fallback);
	else fallback();
}
document.addEventListener("click", (event) => {
	const t = event.target as HTMLElement | null;
	const ex = t?.closest?.<HTMLElement>("[data-craft-export-text]");
	if (ex) {
		const ko = ex.dataset.exLang === "ko";
		craftCopy(craftExportText(ex), ex, ko ? "아이템 글을 복사했습니다" : "Copied item text");
		return;
	}
	const sh = t?.closest?.<HTMLElement>("[data-craft-share-link]");
	if (sh) {
		const ko = document.documentElement.lang?.startsWith("ko") || !!document.querySelector("[data-craft-export-text][data-ex-lang=ko]");
		craftCopy(craftShareLink(sh), sh, ko ? "링크를 복사했습니다" : "Copied link");
		return;
	}
	const sv = t?.closest?.<HTMLElement>("[data-craft-inv-save]");
	if (sv) {
		const d = sv.parentElement?.querySelector<HTMLElement>("[data-craft-export-text]")?.dataset || {};
		const ko = d.exLang === "ko";
		const url = craftShareLink(sv);
		// 지금 고른 보관함 탭에(Q22)
		const tab = craftInvCurrentTab(craftInvLoad());
		const item: CraftInvItem = { name: d.exName || d.exBase || "", base: d.exBase || "", rarity: d.exRarity || "NORMAL", url, at: Date.now(), tab };
		const saved = craftInvStore([item, ...craftInvLoad().filter((x) => x.url !== url)]);
		const where = tab ? (ko ? `보관함 '${tab}' 탭에` : `inventory tab '${tab}'`) : ko ? "보관함에" : "inventory";
		craftNote(sv, saved ? (ko ? `${where} 저장했습니다` : `Saved to ${where}`) : ko ? "이 브라우저는 저장소를 쓸 수 없습니다" : "Browser storage is unavailable");
		renderCraftInventory();
		return;
	}
	// 보관함 탭(Q22) — 고르기 · 만들기 · 지우기(아이템은 기본 탭으로)
	const tabBtn = t?.closest?.<HTMLElement>("[data-craft-inv-tab]");
	if (tabBtn) {
		craftInvSetTab(tabBtn.dataset.craftInvTab || "");
		renderCraftInventory();
		return;
	}
	if (t?.closest?.("[data-craft-inv-tab-add]")) {
		craftInvAddTab();
		return;
	}
	if (t?.closest?.("[data-craft-inv-tab-del]")) {
		const items = craftInvLoad();
		const cur = craftInvCurrentTab(items);
		if (cur) {
			craftInvStore(craftInvDropTab(items, cur));
			craftInvSaveTabs(craftInvSavedTabs().filter((x) => x !== cur));
			craftInvSetTab("");
			renderCraftInventory();
		}
		return;
	}
	const del = t?.closest?.<HTMLElement>("[data-craft-inv-del]");
	if (del) {
		const url = del.dataset.craftInvDel || "";
		craftInvStore(craftInvLoad().filter((x) => x.url !== url));
		renderCraftInventory();
		return;
	}
	// 보관함 옮기기(C535) — 목록을 글(JSON)로 복사 · 붙여넣은 글을 합치기
	const box = t?.closest?.<HTMLElement>("[data-craft-inv-move]");
	const msg = box?.querySelector<HTMLElement>("[data-craft-inv-msg]");
	const ko = document.querySelector<HTMLElement>("[data-craft-inventory]")?.dataset.invLang === "ko";
	const note = (s: string) => {
		if (msg) {
			msg.textContent = s;
			setTimeout(() => (msg.textContent = ""), 3000);
		}
	};
	if (t?.closest?.("[data-craft-inv-export]")) {
		const text = JSON.stringify(craftInvLoad());
		const n = craftInvLoad().length;
		const done = () => note(ko ? `${n}개를 복사했습니다` : `Copied ${n} items`);
		// 클립보드를 못 쓰면(거부 · 없음) 성공 문구 대신 아래 칸의 글을 직접 복사하라고(10-21 C555 — 전엔 거부돼도 '복사했습니다')
		const manual = () => note(ko ? "클립보드에 넣지 못했습니다 — 아래 칸의 글을 직접 복사하세요" : "Could not use the clipboard — copy the text below manually");
		const ta = box?.querySelector<HTMLTextAreaElement>("[data-craft-inv-import-text]");
		if (ta) ta.value = text;
		if (navigator.clipboard?.writeText) navigator.clipboard.writeText(text).then(done, manual);
		else manual();
		return;
	}
	if (t?.closest?.("[data-craft-inv-import]")) {
		const ta = box?.querySelector<HTMLTextAreaElement>("[data-craft-inv-import-text]");
		const r = craftInvImport(ta?.value || "");
		if (!r) note(ko ? "보관함 글이 아닙니다" : "Not inventory text");
		else if (!r.saved) note(ko ? "이 브라우저는 저장소를 쓸 수 없습니다" : "Browser storage is unavailable");
		else {
			// 60개를 넘으면 오래된 것부터 빠진다 — 말없이 자르지 않고 알린다(C555)
			const cut = r.dropped ? (ko ? ` · ${CRAFT_INV_MAX}개까지만 보관해 오래된 ${r.dropped}개가 빠졌습니다` : ` · only ${CRAFT_INV_MAX} are kept, ${r.dropped} oldest removed`) : "";
			note((ko ? `${r.added}개를 더했습니다` : `Added ${r.added} items`) + cut);
			renderCraftInventory();
		}
	}
});

/**
 * 붙여넣은 보관함 글(JSON 배열)을 합친다(C535) — 같은 사이트의 제작 화면 주소(/poe/craft · /poe2/craft, 이 화면 게임)만, 같은 주소는 한 번.
 * 글이 아니면 null. added = 더한 수 · dropped = 최대 개수를 넘어 빠진 수(오래된 것부터) · saved = 저장했나(C555).
 */
function craftInvImport(text: string): { added: number; dropped: number; saved: boolean } | null {
	let list: unknown;
	try {
		list = JSON.parse(text);
	} catch {
		return null;
	}
	if (!Array.isArray(list)) return null;
	const path = location.pathname.startsWith("/poe2/") ? "/poe2/craft" : "/poe/craft";
	const cur = craftInvLoad();
	const seen = new Set(cur.map((x) => x.url));
	let added = 0;
	const fresh: CraftInvItem[] = [];
	for (const raw of list) {
		if (!raw || typeof raw !== "object") continue;
		const o = raw as Record<string, unknown>;
		let u: URL;
		try {
			u = new URL(String(o.url || ""), location.origin);
		} catch {
			continue;
		}
		if (u.origin !== location.origin || u.pathname !== path) continue;
		const url = u.toString();
		if (seen.has(url)) continue;
		seen.add(url);
		fresh.push({
			name: String(o.name || "").slice(0, 80),
			base: String(o.base || "").slice(0, 80),
			rarity: ["NORMAL", "MAGIC", "RARE"].includes(String(o.rarity)) ? String(o.rarity) : "NORMAL",
			url,
			at: Number.isFinite(Number(o.at)) ? Number(o.at) : Date.now(),
			// 보관함 탭(Q22) — 옮긴 글의 탭 그대로
			tab: craftInvTabName(o.tab),
		});
		added++;
	}
	const merged = [...fresh, ...cur];
	const saved = craftInvStore(merged);
	return { added, dropped: Math.max(0, merged.length - CRAFT_INV_MAX), saved };
}

// 사용자 가격(2026-10-21 C529, craftofexile 'custom pricing'): '가격 직접 입력' 칸 값을 이 브라우저에 저장하고, 제작 htmx 요청마다 prices(JSON)로
// 보낸다(게이트가 계산 · 쓴 화폐 · 최적 조합 요청에 싣는다). 비우면 시세. 저장소를 못 쓰면 이번 화면에서만.
// PoE1 = 카오스 · PoE2 = 디바인(C530), 게임마다 따로
const CRAFT_PRICE_KEY = location.pathname.startsWith("/poe2/") ? "craftPrices.poe2" : "craftPrices.poe1";
let craftPricesMem: Record<string, number> = {};
function craftPricesLoad(): Record<string, number> {
	try {
		const v = JSON.parse(localStorage.getItem(CRAFT_PRICE_KEY) || "{}");
		return v && typeof v === "object" ? v : {};
	} catch {
		return craftPricesMem;
	}
}
function craftPricesSave(p: Record<string, number>): void {
	craftPricesMem = p;
	try {
		if (Object.keys(p).length) localStorage.setItem(CRAFT_PRICE_KEY, JSON.stringify(p));
		else localStorage.removeItem(CRAFT_PRICE_KEY);
	} catch {
		/* 저장 못 하면 이번 화면만 */
	}
}
function syncCraftPrices(): void {
	const box = document.querySelector<HTMLElement>("[data-craft-prices]");
	if (!box) return;
	const p = craftPricesLoad();
	box.querySelectorAll<HTMLInputElement>("[data-craft-price-name]").forEach((i) => {
		const v = p[i.dataset.craftPriceName || ""];
		if (document.activeElement !== i) i.value = v ? String(v) : "";
	});
	const n = Object.keys(p).length;
	const c = box.querySelector<HTMLElement>("[data-craft-prices-count]");
	if (c) c.textContent = n ? `(${n})` : "";
}
document.addEventListener("DOMContentLoaded", syncCraftPrices);
document.addEventListener("htmx:afterSettle", syncCraftPrices);
document.addEventListener("input", (e) => {
	const i = (e.target as HTMLElement | null)?.closest?.<HTMLInputElement>("[data-craft-price-name]");
	if (!i) return;
	const p = { ...craftPricesLoad() };
	const name = i.dataset.craftPriceName || "";
	const v = parseFloat(i.value);
	if (Number.isFinite(v) && v > 0) p[name] = v;
	else delete p[name];
	craftPricesSave(p);
	syncCraftPrices();
});
document.addEventListener("click", (e) => {
	if (!(e.target as HTMLElement | null)?.closest?.("[data-craft-prices-reset]")) return;
	craftPricesSave({});
	syncCraftPrices();
});
document.addEventListener("htmx:configRequest", (e) => {
	const d = (e as CustomEvent).detail;
	const path = String(d?.path || "");
	if (!path.startsWith("/poe/htmx/craft") && !path.startsWith("/poe2/htmx/craft")) return;
	const p = craftPricesLoad();
	if (Object.keys(p).length) d.parameters["prices"] = JSON.stringify(p);
	// 올플레임 복제본 수(C532) — 계산기 결과에 '복제본 N개 중 고르기' 표
	if (path.startsWith("/poe/htmx/craft/calc") && craftGhostN() > 1) d.parameters["ghost"] = String(craftGhostN());
});

// 보관함(2026-10-20 C518, craftofexile beta 의 Inventory): 지금 에뮬레이터 아이템을 이 브라우저에 담아 두고(공유 링크 = 아이템 상태 전부 + 이름 · 베이스 ·
// 희귀도 · 시각) 페이지의 '보관함' 칸에서 다시 열기 · 지우기. PoE1 · PoE2 따로, 최대 60개(새 것 먼저, 같은 아이템은 한 번). 저장소를 못 쓰면(사생활 보호 창 등) 안내만.
type CraftInvItem = { name: string; base: string; rarity: string; url: string; at: number; tab?: string };
const CRAFT_INV_MAX = 60;
function craftInvKey(): string {
	return location.pathname.startsWith("/poe2/") ? "craftInventory.poe2" : "craftInventory.poe1";
}
function craftInvLoad(): CraftInvItem[] {
	try {
		const v = JSON.parse(localStorage.getItem(craftInvKey()) || "[]");
		return Array.isArray(v) ? v.filter((x) => x && typeof x.url === "string") : [];
	} catch {
		return [];
	}
}
function craftInvStore(items: CraftInvItem[]): boolean {
	try {
		localStorage.setItem(craftInvKey(), JSON.stringify(items.slice(0, CRAFT_INV_MAX)));
		return true;
	} catch {
		return false;
	}
}
// 보관함 탭(2026-10-23 Q22, craftofexile 'create and manage storage tabs'): 아이템마다 tab(빈 글 = 기본 탭). 빈 탭도 남게 탭 이름 목록을
// 따로 저장하고, 지금 고른 탭도 이 브라우저에. 탭을 지우면 그 아이템은 기본 탭으로 옮긴다(지우지 않는다). 이름 20자 · 탭 12개(기본 포함)까지.
const CRAFT_INV_TAB_MAX = 12;
// 저장소를 못 쓸 때 이번 화면 값
let craftInvTabsMem: string[] = [];
let craftInvTabMem = "";
function craftInvTabName(raw: unknown): string {
	return String(raw ?? "").replace(/\s+/g, " ").trim().slice(0, 20);
}
/** 탭 목록 — 기본("") 먼저, 저장한 이름 순, 아이템에만 있는 이름은 뒤에(중복 없이, 최대 CRAFT_INV_TAB_MAX). */
function craftInvTabs(items: CraftInvItem[], saved: string[]): string[] {
	const out = [""];
	for (const t of [...saved, ...items.map((x) => x.tab || "")]) {
		const n = craftInvTabName(t);
		if (n && !out.includes(n) && out.length < CRAFT_INV_TAB_MAX) out.push(n);
	}
	return out;
}
/** 탭 지우기 — 그 탭 아이템은 기본 탭으로(데이터는 그대로). */
function craftInvDropTab(items: CraftInvItem[], tab: string): CraftInvItem[] {
	return items.map((x) => (craftInvTabName(x.tab) === tab ? { ...x, tab: "" } : x));
}
/** 한 탭의 아이템(목록에 없는 탭 이름은 기본 탭으로 본다). */
function craftInvInTab(items: CraftInvItem[], tabs: string[], tab: string): CraftInvItem[] {
	return items.filter((x) => {
		const n = craftInvTabName(x.tab);
		return (tabs.includes(n) ? n : "") === tab;
	});
}
function craftInvSavedTabs(): string[] {
	try {
		const v = JSON.parse(localStorage.getItem(craftInvKey() + ".tabs") || "[]");
		return Array.isArray(v) ? v.map(craftInvTabName).filter(Boolean) : [];
	} catch {
		return craftInvTabsMem;
	}
}
function craftInvSaveTabs(tabs: string[]): void {
	craftInvTabsMem = tabs.filter(Boolean);
	try {
		if (craftInvTabsMem.length) localStorage.setItem(craftInvKey() + ".tabs", JSON.stringify(craftInvTabsMem));
		else localStorage.removeItem(craftInvKey() + ".tabs");
	} catch {
		/* 저장 못 하면 이번 화면만 */
	}
}
/** 지금 탭 — 목록에 없는 이름(다른 창에서 지움)이면 기본 탭. */
function craftInvCurrentTab(items: CraftInvItem[]): string {
	let t = craftInvTabMem;
	try {
		t = craftInvTabName(localStorage.getItem(craftInvKey() + ".tab") || "");
	} catch {
		/* 이번 화면 값 */
	}
	return craftInvTabs(items, craftInvSavedTabs()).includes(t) ? t : "";
}
function craftInvSetTab(t: string): void {
	craftInvTabMem = t;
	try {
		if (t) localStorage.setItem(craftInvKey() + ".tab", t);
		else localStorage.removeItem(craftInvKey() + ".tab");
	} catch {
		/* 이번 화면만 */
	}
}
function craftInvAddTab(): void {
	const box = document.querySelector<HTMLElement>("[data-craft-inventory]");
	const ko = box?.dataset.invLang === "ko";
	const input = box?.querySelector<HTMLInputElement>("[data-craft-inv-tab-new]");
	const msg = box?.querySelector<HTMLElement>("[data-craft-inv-tab-msg]");
	const note = (text: string, bad = false) => {
		if (!msg) return;
		msg.textContent = text;
		msg.classList.toggle("text-success", !bad);
		msg.classList.toggle("text-warning", bad);
		setTimeout(() => (msg.textContent = ""), 3000);
	};
	const name = craftInvTabName(input?.value);
	const items = craftInvLoad();
	const tabs = craftInvTabs(items, craftInvSavedTabs());
	if (!name) return note(ko ? "탭 이름을 넣으세요" : "Enter a tab name", true);
	// 기본 탭 이름과 같은 탭은 단추가 둘로 보인다(Q28)
	if (["기본", "main"].includes(name.toLowerCase())) return note(ko ? "'기본'은 이미 있는 탭입니다 — 다른 이름을 넣으세요" : "'Main' already exists — use another name", true);
	if (!tabs.includes(name) && tabs.length >= CRAFT_INV_TAB_MAX) return note(ko ? `탭은 ${CRAFT_INV_TAB_MAX}개(기본 포함)까지입니다` : `Up to ${CRAFT_INV_TAB_MAX} tabs (including Main)`, true);
	// 아이템에만 있던 탭(가져온 글)도 목록에 남겨 비어도 사라지지 않게(Q28)
	if (!craftInvSavedTabs().includes(name)) craftInvSaveTabs([...craftInvSavedTabs(), name]);
	craftInvSetTab(name);
	if (input) input.value = "";
	renderCraftInventory();
	note(ko ? `'${name}' 탭 — 저장하면 이 탭에 담깁니다` : `Tab '${name}' — new saves go here`);
}
// 최적 조합이 늦게 와도 읽던 비교 표가 밀리지 않게(10-23 Q32 ③ UX QA — 결과가 오면 표가 500~950px 아래로 밀렸다, 브라우저 스크롤 고정은
// outerHTML 교체엔 안 걸림). 최적 조합 자리를 지나 아래를 읽던 중일 때만 — 위쪽을 보던 중이면 최적 조합이 보이는 게 맞다
let craftOptAnchor: { el: HTMLElement; top: number } | null = null;
function craftScroller(el: Element): HTMLElement | null {
	for (let p = el.parentElement; p; p = p.parentElement) {
		const oy = getComputedStyle(p).overflowY;
		if ((oy === "auto" || oy === "scroll") && p.scrollHeight > p.clientHeight + 1) return p;
	}
	return null;
}
document.addEventListener("htmx:beforeSwap", (e) => {
	craftOptAnchor = null;
	const t = (e as CustomEvent).detail?.target as HTMLElement | undefined;
	if (!t || (t.id !== "craft-opt-result" && t.id !== "craft2-opt-result")) return;
	const table = document.getElementById(t.id === "craft2-opt-result" ? "craft2-calc-result" : "craft-calc-result");
	if (!table) return;
	const sc = craftScroller(t);
	const viewTop = sc ? sc.getBoundingClientRect().top : 0;
	if (t.getBoundingClientRect().top >= viewTop) return;
	craftOptAnchor = { el: table, top: table.getBoundingClientRect().top };
});
document.addEventListener("htmx:afterSwap", (e) => {
	const a = craftOptAnchor;
	craftOptAnchor = null;
	const t = (e as CustomEvent).detail?.target as HTMLElement | undefined;
	if (!a) {
		// 위쪽을 보던 중이면 최적 조합 머리가 잘리지 않게(Q32 — 1440 에서 머리가 화면 위로 23px 잘렸다)
		const id = t?.id;
		const opt = id === "craft-opt-result" || id === "craft2-opt-result" ? document.getElementById(id) : null;
		if (opt && opt.hasAttribute("data-craft-optimize-result")) {
			// 결과 칸 안 스크롤 먼저, 그래도 붙박이 상단바 아래로 숨으면 페이지를(결과 칸 자체가 페이지 스크롤로 위로 올라가 있었다)
			const sc = craftScroller(opt);
			if (sc) {
				const d = opt.getBoundingClientRect().top - sc.getBoundingClientRect().top - 8;
				if (d < 0) sc.scrollTop += d;
			}
			const hv = getComputedStyle(document.documentElement).getPropertyValue("--site-header-height").trim();
			const head = (hv.endsWith("rem") ? parseFloat(hv) * parseFloat(getComputedStyle(document.documentElement).fontSize) : parseFloat(hv)) || 0;
			const d2 = opt.getBoundingClientRect().top - head - 8;
			if (d2 < 0) window.scrollBy(0, d2);
		}
		return;
	}
	if (!t || !a.el.isConnected) return;
	const d = a.el.getBoundingClientRect().top - a.top;
	if (Math.abs(d) < 1) return;
	const sc = craftScroller(a.el);
	if (sc) sc.scrollTop += d;
	else window.scrollBy(0, d);
});
// 계산기 조건이 바뀌면 이전 결과를 흐리게 + 안내(10-23 Q32 UX QA ⑤ — 목표를 바꿔도 옛 결과 · 옛 오류가 그대로 보여 지금 조건의 답처럼 읽혔다).
// 결과가 있을 때만, 다시 계산을 보내면 푼다. 거르기 검색칸은 조건이 아니라 뺀다
const CRAFT_CALC_RESULT_SEL = "[data-craft-calc-method], [data-craft2-calc-method], [data-craft-optimize-result], [data-craft-strategy-step], [data-craft2-strategy-step], [role=alert]";
function craftCalcBox(form: Element): HTMLElement | null {
	return form.matches("[data-craft2-calc-form]") ? document.querySelector<HTMLElement>("[data-craft2-calc-results]") : document.querySelector<HTMLElement>("[data-craft-calc-results]");
}
document.addEventListener("change", (e) => {
	const t = e.target as HTMLInputElement | null;
	const form = t?.closest?.("[data-craft-calc-form], [data-craft2-calc-form]");
	if (!form || !t?.name || t.matches('input[type="search"]') || /^(preset|[soumg][1-6])$/.test(t.name)) return;
	const box = craftCalcBox(form);
	if (!box || box.dataset.craftCalcStale === "1" || !box.querySelector(CRAFT_CALC_RESULT_SEL)) return;
	box.dataset.craftCalcStale = "1";
	const ko = (document.documentElement.lang || "").startsWith("ko");
	const note = document.createElement("div");
	note.setAttribute("role", "status");
	note.className = "alert alert-info py-2 text-sm";
	note.dataset.craftCalcStaleNote = "";
	note.textContent = ko ? "목표나 조건이 바뀌었습니다 — 아래 결과는 이전 조건의 것입니다. 다시 찾기를 누르세요." : "Targets or options changed — the results below are for the previous settings. Run it again.";
	box.prepend(note);
});
document.addEventListener("htmx:beforeRequest", (e) => {
	const elt = (e as CustomEvent).detail?.elt as Element | undefined;
	const form = elt?.closest?.("[data-craft-calc-form], [data-craft2-calc-form]");
	if (!form || elt?.matches?.("[data-craft-strategy-run], [data-craft2-strategy-run]")) return;
	const box = craftCalcBox(form);
	if (!box) return;
	delete box.dataset.craftCalcStale;
	box.querySelectorAll("[data-craft-calc-stale-note]").forEach((n) => n.remove());
});
// 최적 조합을 다시 보내면 앞 작업의 조회를 끊는다(10-23 Q108 — 앞 작업(예: 탐광 목표)이 아직 1.5초마다 조회 중일 때 새 조건으로 다시 찾으면,
// 옛 조회가 먼저 끝나 결과 자리를 차지하고 새 요청의 응답은 떨어져 나간 옛 요소에 붙어 사라졌다 — 지금 조건이 아닌 결과가 보였다).
// 새 요청을 보내는 순간 지금 자리의 기다림 요소를 '지난 것'으로 표시 · 진행 중 조회를 끊고, 그 요소의 다음 조회 · 늦은 응답은 버린다
document.addEventListener("htmx:beforeRequest", (e) => {
	const elt = (e as CustomEvent).detail?.elt as HTMLElement | undefined;
	if (!elt?.matches) return;
	if (elt.hasAttribute("data-craft-optimize-stale")) {
		e.preventDefault();
		return;
	}
	if (!elt.matches("[data-craft-optimize-trigger], [data-craft2-optimize-trigger]")) return;
	const cur = document.getElementById((elt.getAttribute("hx-target") || "").replace(/^#/, ""));
	if (!cur?.hasAttribute("data-craft-optimize-wait")) return;
	cur.setAttribute("data-craft-optimize-stale", "");
	(window as any).htmx?.trigger(cur, "htmx:abort");
});
document.addEventListener("htmx:beforeSwap", (e) => {
	const d = (e as CustomEvent).detail;
	if ((d?.requestConfig?.elt as Element | undefined)?.hasAttribute?.("data-craft-optimize-stale")) d.shouldSwap = false;
});
// 계산기 옵션 검색칸에서 Enter = 거르기만(10-23 Q31 UX QA — 폼 제출로 계산 · 최적 조합 요청이 나갔다)
document.addEventListener("keydown", (e) => {
	if (e.key !== "Enter" || e.isComposing) return;
	const t = e.target as HTMLElement | null;
	if (t?.matches?.('input[type="search"]') && t.closest("[data-craft-calc-form], [data-craft2-calc-form]")) e.preventDefault();
});
document.addEventListener("keydown", (e) => {
	if (e.key !== "Enter" || e.isComposing || e.keyCode === 229 || !(e.target as HTMLElement | null)?.closest?.("[data-craft-inv-tab-new]")) return;
	e.preventDefault();
	craftInvAddTab();
});
// 아이템을 다른 탭으로
document.addEventListener("change", (e) => {
	const sel = (e.target as HTMLElement | null)?.closest?.<HTMLSelectElement>("[data-craft-inv-move-to]");
	if (!sel) return;
	const url = sel.dataset.craftInvMoveTo || "";
	craftInvStore(craftInvLoad().map((x) => (x.url === url ? { ...x, tab: craftInvTabName(sel.value) } : x)));
	renderCraftInventory();
});
(globalThis as any).__craftInvInternals = { craftInvTabs, craftInvDropTab, craftInvInTab, craftInvTabName, CRAFT_INV_TAB_MAX };
function renderCraftInventory(): void {
	const box = document.querySelector<HTMLElement>("[data-craft-inventory]");
	if (!box) return;
	const ko = box.dataset.invLang === "ko";
	const list = box.querySelector<HTMLElement>("[data-craft-inv-list]");
	const count = box.querySelector<HTMLElement>("[data-craft-inv-count]");
	const all = craftInvLoad();
	if (count) count.textContent = all.length ? `(${all.length})` : "";
	if (!list) return;
	// 탭 줄(Q22) — 기본 + 만든 탭, 고른 탭은 aria-pressed · 채운 단추
	const tabs = craftInvTabs(all, craftInvSavedTabs());
	const cur = craftInvCurrentTab(all);
	const tabLabel = (t: string) => t || (ko ? "기본" : "Main");
	const bar = box.querySelector<HTMLElement>("[data-craft-inv-tabs]");
	if (bar) {
		bar.replaceChildren();
		tabs.forEach((t) => {
			const b = document.createElement("button");
			b.type = "button";
			b.className = t === cur ? "btn btn-xs btn-primary" : "btn btn-xs btn-ghost border border-base-300";
			b.dataset.craftInvTab = t;
			b.setAttribute("aria-pressed", t === cur ? "true" : "false");
			b.textContent = `${tabLabel(t)} (${craftInvInTab(all, tabs, t).length})`;
			bar.appendChild(b);
		});
	}
	const delTab = box.querySelector<HTMLElement>("[data-craft-inv-tab-del]");
	if (delTab) delTab.hidden = cur === "";
	const items = craftInvInTab(all, tabs, cur);
	list.replaceChildren();
	if (!items.length) {
		const p = document.createElement("p");
		p.className = "text-xs text-base-content/60";
		p.dataset.craftInvEmpty = "";
		p.textContent = all.length
			? ko
				? `'${tabLabel(cur)}' 탭에는 아이템이 없습니다 — 이 탭을 고른 채 '보관함에 저장'하면 여기에 담깁니다.`
				: `No items in '${tabLabel(cur)}' — 'Save to inventory' while this tab is selected puts it here.`
			: ko
				? "저장한 아이템이 없습니다 — 카드 아래 '보관함에 저장'으로 담아 두면 나중에 그대로 열 수 있습니다(이 브라우저에만)."
				: "No saved items — use 'Save to inventory' under the card to reopen it later (this browser only).";
		list.appendChild(p);
		return;
	}
	const rarLabel: Record<string, [string, string]> = { NORMAL: ["Normal", "일반"], MAGIC: ["Magic", "마법"], RARE: ["Rare", "희귀"] };
	items.forEach((it) => {
		const row = document.createElement("div");
		row.className = "flex flex-wrap items-center gap-x-2 gap-y-0.5 text-sm min-w-0";
		row.dataset.craftInvRow = "";
		const badge = document.createElement("span");
		badge.className = "badge badge-ghost badge-sm";
		badge.textContent = (rarLabel[it.rarity] || rarLabel.NORMAL)[ko ? 1 : 0];
		const a = document.createElement("a");
		a.href = it.url;
		a.className = "link link-hover font-medium wrap-anywhere";
		a.textContent = it.name || it.base || "?";
		a.title = ko ? "이 아이템으로 열기" : "Open this item";
		const meta = document.createElement("span");
		meta.className = "text-xs text-base-content/60";
		// 앱 로케일 · 기준 시간대로(AppTimeZoneFormatTest · LocaleAwareFormatTest 가드)
		const when = new Date(it.at).toLocaleString(appLocale(), { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", timeZone: appTimeZone() });
		meta.textContent = (it.base && it.base !== it.name ? it.base + " · " : "") + when;
		const btn = document.createElement("button");
		btn.type = "button";
		btn.className = "btn btn-xs btn-ghost";
		btn.dataset.craftInvDel = it.url;
		btn.textContent = ko ? "지우기" : "Delete";
		btn.setAttribute("aria-label", (ko ? "보관함에서 지우기: " : "Delete from inventory: ") + a.textContent);
		row.append(badge, a, meta);
		// 다른 탭으로 옮기기(Q22) — 탭이 둘 이상일 때만
		if (tabs.length > 1) {
			const mv = document.createElement("select");
			mv.className = "select select-xs select-bordered w-auto max-w-32";
			mv.dataset.craftInvMoveTo = it.url;
			mv.setAttribute("aria-label", (ko ? "탭 옮기기: " : "Move to tab: ") + a.textContent);
			tabs.forEach((t) => {
				const o = document.createElement("option");
				o.value = t;
				o.textContent = tabLabel(t);
				o.selected = t === cur;
				mv.appendChild(o);
			});
			row.append(mv);
		}
		row.append(btn);
		list.appendChild(row);
	});
}
document.addEventListener("DOMContentLoaded", renderCraftInventory);
globalThis.addEventListener("storage", (event) => {
	// 탭 목록 · 고른 탭(.tabs · .tab)이 다른 창에서 바뀌어도(Q28)
	if (event.key?.startsWith(craftInvKey())) renderCraftInventory();
});

// 목표 적중 확률(2026-10-20 C494, craftofexile 에뮬레이터의 목표 확률): 계산기에서 목표를 고르면 에뮬레이터 화폐 단추마다 '지금 아이템에 한 번 써서
// 목표를 다 채울 확률'을 작은 표로. 아이템이 바뀌거나(htmx 교체) 목표를 바꾸면 다시 잰다(0.4초 모아서).
let craftHitTimer: ReturnType<typeof setTimeout> | undefined;
// 마지막으로 잰 적중 확률(C507 — 연타 줄 '한 번 확률 · 평균 몇 번')
let craftHitLast: Record<string, number | null> | null = null;
// 올플레임 제작(C524) — 복제본 N 개면 한 번 쓸 때 목표를 채울 확률 = 1 − (1 − p)^N(복제본마다 원본에 따로 적용). 꺼져 있으면 p 그대로
// 올플레임에 쓸 수 있는 화폐 — PoeCraftController.GHOST_ACTS 와 같은 목록
const CRAFT_GHOST_OK = new Set(["TRANSMUTE", "ALTERATION", "AUGMENTATION", "REGAL", "ALCHEMY", "CHAOS", "EXALT", "ANNUL", "SCOUR", "ESSENCE", "FOSSIL", "CONQUEROR", "FRACTURE", "VEILED_CHAOS", "VEILED_EXALT", "EMBER", "ICHOR", "ELDRITCH_CHAOS", "ELDRITCH_EXALT", "ELDRITCH_ANNUL", "DOMINANCE", "DUCAT", "CONFLICT", "FOULBORN_AUG", "FOULBORN_REGAL", "FOULBORN_EXALT"]);
function craftGhostN(): number {
	const v = parseInt(document.querySelector<HTMLSelectElement>("#craft-emu [data-craft-ghost-count]")?.value || "0", 10);
	return v >= 2 ? Math.min(4, v) : 1;
}
function ghostChance(c: number): number {
	const n = craftGhostN();
	return n > 1 ? 1 - Math.pow(1 - c, n) : c;
}
function syncSpamExpect(): void {
	const sel = document.querySelector<HTMLSelectElement>("[data-craft-spam-act]");
	const out = document.querySelector<HTMLElement>("[data-craft-spam-expect]");
	if (!sel || !out) return;
	const raw = craftHitLast ? craftHitLast[sel.value] : undefined;
	const c = raw === undefined || raw === null || !CRAFT_GHOST_OK.has(sel.value) ? raw : ghostChance(raw);
	const ko = (document.documentElement.lang || "").startsWith("ko");
	if (c === undefined || c === null) {
		out.textContent = "";
	} else if (c === 0) {
		out.textContent = ko ? "한 번에 다 채울 확률 0% — 연타로는 안 될 수 있음" : "0% per use — repeating may not work";
	} else {
		const n = Math.max(1, Math.round(1 / c));
		out.textContent = ko ? `한 번 ${(c * 100).toFixed(c < 0.1 ? 1 : 0)}% · 평균 약 ${n.toLocaleString(appLocale())}번` : `${(c * 100).toFixed(c < 0.1 ? 1 : 0)}% per use · ~${n.toLocaleString(appLocale())} uses on average`;
	}
}
let craftHitSeq = 0;
function scheduleCraftHit(): void {
	if (craftHitTimer) clearTimeout(craftHitTimer);
	craftHitTimer = setTimeout(updateCraftHit, 400);
}
function updateCraftHit(): void {
	// PoE1 #craft-emu + [data-craft-calc-form], PoE2 #craft2-emu + [data-craft2-calc-form](C500)
	const poe2 = !document.getElementById("craft-emu") && !!document.getElementById("craft2-emu");
	const emu = document.getElementById(poe2 ? "craft2-emu" : "craft-emu");
	const calc = document.querySelector<HTMLFormElement>(poe2 ? "[data-craft2-calc-form]" : "[data-craft-calc-form]");
	const emuForm = emu?.querySelector<HTMLFormElement>("form");
	if (!emu || !calc || !emuForm) return;
	const body = new URLSearchParams();
	new FormData(calc).forEach((v, k) => {
		if (k.startsWith("t_") && typeof v === "string" && v !== "" && v !== "0") body.append(k, v);
	});
	const clear = () => emu.querySelectorAll("[data-craft-hit]").forEach((e) => e.remove());
	if (body.toString() === "") {
		clear();
		return;
	}
	for (const n of poe2 ? ["poolKey", "ilvl", "state"] : ["itemClass", "variant", "influence", "ilvl", "state"]) {
		const el = emuForm.querySelector<HTMLInputElement>(`input[name="${n}"]`);
		if (el) body.set(n, el.value);
	}
	if (poe2) {
		const tierEl = emu.querySelector<HTMLInputElement>('input[name="tier"]:checked');
		if (tierEl && tierEl.value) body.set("tier", tierEl.value);
		emu.querySelectorAll<HTMLInputElement>('input[name="omen"]:checked').forEach((o) => body.append("omen", o.value));
	}
	const ess = emu.querySelector<HTMLSelectElement>('select[name="essence"]');
	if (ess && ess.value) body.set("essence", ess.value);
	if (!poe2) {
		// 고른 화석 · 수확도(C507)
		emu.querySelectorAll<HTMLInputElement>('input[name="fossil"]:checked').forEach((f) => body.append("fossil", f.value));
		const har = emu.querySelector<HTMLSelectElement>('select[name="harvest"]');
		if (har && har.value) body.set("harvest", har.value);
	}
	const seq = ++craftHitSeq;
	fetch(poe2 ? "/poe2/htmx/craft/hit-chance" : "/poe/htmx/craft/hit-chance", { method: "POST", body, headers: { "Content-Type": "application/x-www-form-urlencoded" } })
		.then((r) => (r.ok ? r.json() : null))
		.then((j) => {
			if (seq !== craftHitSeq) return;
			clear();
			craftHitLast = j && j.chances ? (j.chances as Record<string, number | null>) : null;
			syncSpamExpect();
			if (!j || !j.chances) return;
			const ko = (document.documentElement.lang || "").startsWith("ko");
			for (const [act, c] of Object.entries(j.chances as Record<string, number | null>)) {
				if (c === null || c === undefined) continue;
				emu.querySelectorAll<HTMLButtonElement>(`button[data-craft-action="${act}"]`).forEach((b) => {
					// 올플레임(C524) — 복제본을 고를 수 있는 화폐면 1 − (1 − p)^N
					const gn = b.closest("#craft-emu") && CRAFT_GHOST_OK.has(act) ? craftGhostN() : 1;
					const cc = gn > 1 ? 1 - Math.pow(1 - c, gn) : c;
					const s = document.createElement("span");
					s.setAttribute("data-craft-hit", cc === 0 ? "0" : "1");
					if (gn > 1) s.setAttribute("data-craft-hit-ghost", String(gn));
					s.textContent = cc === 0 ? "0%" : cc < 0.001 ? "<0.1%" : `${(cc * 100).toFixed(cc < 0.1 ? 1 : 0)}%`;
					s.title =
						(ko ? `계산기에서 고른 목표 ${j.targets}개를 지금 아이템에 이 화폐 한 번으로 다 채울 확률` : `Chance to meet all ${j.targets} calculator targets with one use on the current item`) +
						(gn > 1 ? (ko ? ` — 올플레임 복제본 ${gn}개 중 고르기 기준(복제본 하나 ${(c * 100).toFixed(1)}%)` : ` — keeping the best of ${gn} Ghostly Copies (one copy ${(c * 100).toFixed(1)}%)`) : "");
					b.appendChild(s);
				});
			}
		})
		.catch(() => {});
}
document.addEventListener("htmx:afterSettle", (e) => {
	const t = (e as CustomEvent).detail?.target as HTMLElement | undefined;
	if (t && (t.id === "craft-emu" || t.id === "craft2-emu" || t.closest?.("#craft-emu, #craft2-emu") || t.querySelector?.("#craft-emu, #craft2-emu"))) scheduleCraftHit();
});
document.addEventListener("change", (e) => {
	const t = e.target as HTMLElement | null;
	if (t?.closest?.("[data-craft-calc-form], [data-craft2-calc-form]") && (t as HTMLSelectElement).name?.startsWith("t_")) scheduleCraftHit();
	if (t?.closest?.("#craft-emu, #craft2-emu") && ["essence", "tier", "omen", "fossil", "harvest"].includes((t as HTMLInputElement).name)) scheduleCraftHit();
	if ((t as HTMLElement | null)?.hasAttribute?.("data-craft-spam-act")) syncSpamExpect();
	// 올플레임 복제본 수를 바꾸면 적중 확률 · 연타 기대를 다시(C524)
	if ((t as HTMLElement | null)?.hasAttribute?.("data-craft-ghost-count")) scheduleCraftHit();
});
document.addEventListener("DOMContentLoaded", scheduleCraftHit);

// 확률 표에서 바로 목표 고르기(2026-10-20 C496, craftofexile 처럼 옵션 표에서 목표 지정): ☆ = 계산기의 그 계열 목표를 T1~T(이 줄)로, 같은 등급이 이미 골라져
// 있으면 끔. 계산기 select 에 change 를 보내 목표 칸 · 적중 확률(C494)이 함께 바뀐다.
function syncPoolTargets(): void {
	document.querySelectorAll<HTMLButtonElement>("[data-craft-pool-target]").forEach((b) => {
		const fam = b.dataset.craftPoolTarget;
		const sel = document.querySelector<HTMLSelectElement>(`[data-craft-calc-family="${fam}"] select, [data-craft2-calc-family="${fam}"] select`);
		const on = !!sel && sel.value !== "" && sel.value === b.dataset.tier;
		b.textContent = on ? "★" : "☆";
		b.setAttribute("aria-pressed", on ? "true" : "false");
		b.classList.toggle("text-warning", on);
	});
}
document.addEventListener("click", (event) => {
	const b = (event.target as HTMLElement | null)?.closest?.<HTMLButtonElement>("[data-craft-pool-target]");
	if (!b) return;
	// 계열 줄 머리(summary, 10-21 C542) 안의 ☆ 는 목표만 — 펼치거나 접지 않는다
	if (b.closest("summary")) event.preventDefault();
	const fam = b.dataset.craftPoolTarget;
	const sel = document.querySelector<HTMLSelectElement>(`[data-craft-calc-family="${fam}"] select, [data-craft2-calc-family="${fam}"] select`);
	if (!sel) return;
	const tier = b.dataset.tier || "";
	sel.value = sel.value === tier ? "" : tier;
	sel.dispatchEvent(new Event("change", { bubbles: true }));
	syncPoolTargets();
});
document.addEventListener("change", (e) => {
	if ((e.target as HTMLElement | null)?.closest?.("[data-craft-calc-form], [data-craft2-calc-form]")) syncPoolTargets();
});
document.addEventListener("htmx:afterSettle", syncPoolTargets);
document.addEventListener("DOMContentLoaded", syncPoolTargets);

// 계열 표 거르기(2026-10-22 P1, poedb 속성 부여의 태그 칩 · 검색 — craftFamFilter.jte): 글자(공백 · 대소문자 무시) · 태그(여러 개면 모두 가진 계열) ·
// '붙을 수 있는 것만'. 브라우저 안에서만 거르고, 화폐를 써서 표가 바뀌어도 같은 거르기를 다시 건다(화면마다 · 탭 안에서만 기억).
type FamFilter = { q: string; tags: string[]; rollable: boolean };
const EMPTY_FAM_FILTER: FamFilter = { q: "", tags: [], rollable: false };
const famFilterMemo: Record<string, FamFilter> = {};
function famFilterMatch(text: string, tagset: string[], rollable: boolean, f: FamFilter): boolean {
	if (f.rollable && !rollable) return false;
	if (f.tags.length > 0 && !f.tags.every((t) => tagset.includes(t))) return false;
	const q = f.q.replace(/\s+/g, "").toLowerCase();
	return q === "" || text.replace(/\s+/g, "").toLowerCase().includes(q);
}
function famFilterActive(f: FamFilter): boolean {
	return f.q.trim() !== "" || f.tags.length > 0 || f.rollable;
}
function famFilterKey(): string {
	return "craftFamFilter:" + location.pathname;
}
function readFamFilter(): FamFilter {
	const key = famFilterKey();
	if (famFilterMemo[key]) return famFilterMemo[key];
	try {
		const v = JSON.parse(sessionStorage.getItem(key) || "null");
		if (v && typeof v.q === "string" && Array.isArray(v.tags)) return { q: v.q, tags: v.tags.filter((t: unknown) => typeof t === "string"), rollable: !!v.rollable };
	} catch (e) {
		/* 저장소를 못 쓰면 빈 거르기 */
	}
	return EMPTY_FAM_FILTER;
}
function saveFamFilter(f: FamFilter): void {
	const key = famFilterKey();
	famFilterMemo[key] = f;
	try {
		sessionStorage.setItem(key, JSON.stringify(f));
	} catch (e) {
		/* 저장소를 못 쓰면 이 화면에서만 */
	}
}
function famFilterFromBar(bar: Element): FamFilter {
	return {
		q: bar.querySelector<HTMLInputElement>("[data-craft-fam-q]")?.value || "",
		tags: Array.from(bar.querySelectorAll<HTMLElement>('[data-craft-fam-tag][aria-pressed="true"]')).map((b) => b.dataset.craftFamTag || ""),
		rollable: !!bar.querySelector<HTMLInputElement>("[data-craft-fam-rollable]")?.checked,
	};
}
function setFamTagPressed(b: Element, on: boolean): void {
	b.setAttribute("aria-pressed", on ? "true" : "false");
	b.classList.toggle("btn-primary", on);
	b.classList.toggle("btn-ghost", !on);
}
// 거르기 막대 값으로 계열 줄을 숨기고 보인 수를 돌려준다. 쪽마다 다 숨으면 '맞는 계열 없음'
function applyFamFilter(root: ParentNode = document): number {
	const bar = root.querySelector("[data-craft-fam-filter]");
	if (!bar) return 0;
	const f = famFilterFromBar(bar);
	let shown = 0;
	let total = 0;
	root.querySelectorAll("[data-craft-fam-list]").forEach((list) => {
		let listShown = 0;
		let listTotal = 0;
		list.querySelectorAll<HTMLElement>("[data-craft-fam]").forEach((d) => {
			listTotal++;
			const tagset = (d.dataset.craftFamTagset || "").split("|").filter((t) => t !== "");
			// 검색 글 = 옵션 문구 · 태그 · 접사 이름(data-craft-fam-search) — 확률 · 'T1' 같은 표시 글은 빼고(Q3e 검토)
			const ok = famFilterMatch(d.dataset.craftFamSearch ?? (d.querySelector("summary")?.textContent || ""), tagset, d.hasAttribute("data-craft-fam-rollable"), f);
			d.hidden = !ok;
			if (ok) listShown++;
		});
		list.querySelector("[data-craft-fam-none]")?.toggleAttribute("hidden", !(listTotal > 0 && listShown === 0));
		shown += listShown;
		total += listTotal;
	});
	const count = bar.querySelector("[data-craft-fam-count]");
	if (count) count.textContent = `${shown}/${total}`; // 거르기 전에도 늘(Q7 — 비어 있으면 무엇인지 몰랐다)
	return shown;
}
// 표를 다시 그린 뒤 기억한 거르기를 막대에 되돌리고 다시 건다(없는 태그는 버림)
function restoreFamFilter(root: ParentNode = document): boolean {
	const bar = root.querySelector("[data-craft-fam-filter]");
	if (!bar) return false;
	const f = readFamFilter();
	const q = bar.querySelector<HTMLInputElement>("[data-craft-fam-q]");
	if (q && document.activeElement !== q) q.value = f.q;
	const roll = bar.querySelector<HTMLInputElement>("[data-craft-fam-rollable]");
	if (roll) roll.checked = f.rollable;
	bar.querySelectorAll("[data-craft-fam-tag]").forEach((b) => setFamTagPressed(b, f.tags.includes((b as HTMLElement).dataset.craftFamTag || "")));
	applyFamFilter(root);
	return true;
}
function onFamFilterChanged(bar: Element): void {
	saveFamFilter(famFilterFromBar(bar));
	applyFamFilter(bar.closest("[data-craft-pool]") || document);
}
document.addEventListener("input", (event) => {
	const t = event.target as HTMLElement | null;
	const bar = t?.closest?.("[data-craft-fam-filter]");
	if (bar && t?.hasAttribute("data-craft-fam-q")) onFamFilterChanged(bar);
});
document.addEventListener("change", (event) => {
	const t = event.target as HTMLElement | null;
	const bar = t?.closest?.("[data-craft-fam-filter]");
	if (bar && t?.hasAttribute("data-craft-fam-rollable")) onFamFilterChanged(bar);
});
document.addEventListener("click", (event) => {
	const t = event.target as HTMLElement | null;
	const tag = t?.closest?.("[data-craft-fam-tag]");
	const clear = t?.closest?.("[data-craft-fam-clear]");
	const bar = (tag || clear)?.closest("[data-craft-fam-filter]");
	if (!bar) return;
	if (tag) {
		setFamTagPressed(tag, tag.getAttribute("aria-pressed") !== "true");
	} else {
		const q = bar.querySelector<HTMLInputElement>("[data-craft-fam-q]");
		if (q) q.value = "";
		const roll = bar.querySelector<HTMLInputElement>("[data-craft-fam-rollable]");
		if (roll) roll.checked = false;
		bar.querySelectorAll("[data-craft-fam-tag]").forEach((b) => setFamTagPressed(b, false));
	}
	onFamFilterChanged(bar);
});
document.addEventListener("htmx:afterSettle", () => restoreFamFilter());
document.addEventListener("DOMContentLoaded", () => restoreFamFilter());
(globalThis as any).__famFilterInternals = { famFilterMatch, famFilterActive, readFamFilter, saveFamFilter, EMPTY_FAM_FILTER };

// 카드 옵션을 계산기 목표로(2026-10-20 C497, craftofexile 의 가져온 아이템 → 목표): 붙여넣은 아이템과 같은 것을 처음부터 만드는 최소 비용을 보려고 —
// 카드 줄 옵션 id 를 계산기 계열 줄(data-craft-calc-ids "id=등급")에서 찾아 T1~그 등급으로, 다른 목표는 비우고 '지금 아이템에서 시작'은 끈다.
document.addEventListener("click", (event) => {
	const b = (event.target as HTMLElement | null)?.closest?.<HTMLElement>("[data-craft-to-targets]");
	if (!b) return;
	const card = b.closest("[data-craft-arm-scope]")?.querySelector("[data-craft-tooltip]");
	const form = document.querySelector<HTMLFormElement>("[data-craft-calc-form], [data-craft2-calc-form]");
	if (!card || !form) return;
	const rows = Array.from(form.querySelectorAll<HTMLElement>("[data-craft-calc-ids]"));
	form.querySelectorAll<HTMLSelectElement>('select[name^="t_"]').forEach((s) => (s.value = ""));
	let n = 0;
	let skipped = 0;
	card.querySelectorAll<HTMLElement>("[data-craft-line]").forEach((line) => {
		const id = line.dataset.craftLine || "";
		const row = rows.find((r) => (r.dataset.craftCalcIds || "").split(" ").some((t) => t.startsWith(id + "=")));
		const tok = row ? (row.dataset.craftCalcIds || "").split(" ").find((t) => t.startsWith(id + "=")) : undefined;
		const sel = row?.querySelector<HTMLSelectElement>("select");
		if (!tok || !sel) {
			skipped++;
			return;
		}
		sel.value = tok.substring(id.length + 1);
		n++;
	});
	const from = form.querySelector<HTMLInputElement>('[data-craft-from-state], input[name="fromState"]');
	if (from) from.checked = false;
	form.querySelector<HTMLSelectElement>('select[name^="t_"]')?.dispatchEvent(new Event("change", { bubbles: true }));
	const ko = b.textContent?.includes("목표") ?? false;
	const msg = b.parentElement?.querySelector<HTMLElement>("[data-craft-export-msg]");
	if (msg) {
		msg.textContent = ko ? `목표 ${n}개를 계산기에 넣었습니다${skipped ? ` (못 넣은 옵션 ${skipped}개)` : ""}` : `Set ${n} targets${skipped ? ` (${skipped} skipped)` : ""}`;
		setTimeout(() => (msg.textContent = ""), 4000);
	}
});
