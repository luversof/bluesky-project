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

function applyActivityView(root: Element, rawMode: string | null) {
	const mode =
		rawMode === "timeline" || rawMode === "list" ? rawMode : "calendar";
	const panels: Record<string, Element | null> = {
		calendar: root.querySelector("#activityCalendarView"),
		timeline: root.querySelector("#activityTimelineView"),
		list: root.querySelector("#activityListView"),
	};
	Object.keys(panels).forEach((key) => {
		const panel = panels[key];
		if (panel) panel.classList.toggle("hidden", key !== mode);
	});
	root.querySelectorAll("[data-activity-view-tab]").forEach((tab) => {
		const isActive = tab.getAttribute("data-activity-view-tab") === mode;
		ACTIVITY_TAB_ACTIVE_CLASSES.forEach((cls) =>
			tab.classList.toggle(cls, isActive),
		);
		tab.classList.toggle("text-base-content/60", !isActive);
	});
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
	applyActivityView(root, saved);
}

document.addEventListener("click", (event) => {
	const target = event.target as HTMLElement;
	if (!target || !target.closest) return;

	const tab = target.closest("[data-activity-view-tab]");
	if (tab) {
		const root = tab.closest("#activityListFragment");
		if (!root) return;
		const mode = tab.getAttribute("data-activity-view-tab");
		applyActivityView(root, mode);
		try {
			localStorage.setItem(
				ACTIVITY_VIEW_KEY,
				mode === "timeline" || mode === "list" ? mode : "calendar",
			);
		} catch (e) {
			// localStorage 불가 환경에서는 저장 없이 전환만
		}
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

// [data-page-param-from-query="page"]: 현재 URL 쿼리의 페이지 번호를 요청 파라미터로 전달 (없으면 1)
document.addEventListener("htmx:configRequest", (event: any) => {
	const key = (event.detail?.elt as HTMLElement | undefined)?.dataset
		?.pageParamFromQuery;
	if (key) {
		event.detail.parameters[key] =
			new URLSearchParams(globalThis.location.search).get(key) || "1";
	}
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
