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

function applyActivityView(root: Element, rawMode: string | null) {
	const mode =
		rawMode === "timeline" || rawMode === "list" ? rawMode : "calendar";
	Object.keys(ACTIVITY_VIEW_PANEL_ID).forEach((key) => {
		const panel = root.querySelector("#" + ACTIVITY_VIEW_PANEL_ID[key]);
		if (panel) panel.classList.toggle("hidden", key !== mode);
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
	if (cell) {
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
			panel.classList.remove("is-open");
			cell.classList.remove("bg-base-200");
			return;
		}
		panel
			.querySelectorAll("[data-cal-detail]")
			.forEach((other) => other.classList.add("hidden"));
		root
			.querySelectorAll("[data-cal-date].bg-base-200")
			.forEach((selected) => selected.classList.remove("bg-base-200"));
		detail.classList.remove("hidden");
		panel.classList.add("is-open");
		cell.classList.add("bg-base-200");
	}
});

document.addEventListener("DOMContentLoaded", restoreActivityView);
document.addEventListener("htmx:afterSettle", restoreActivityView);

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
