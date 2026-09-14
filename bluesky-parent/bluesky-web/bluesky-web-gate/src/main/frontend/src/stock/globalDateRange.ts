// 전역 날짜 범위(localStorage 'globalDateRange') ↔ 페이지 내 hidden input / HTMX 폼 동기화.
// localStorage 를 쓰므로 탭이 달라도(예: 종목 상세를 새 탭으로 여러 개) 같은 기간을 공유한다.
// 파싱 중 동기 실행되어야 하므로(아래 htmx 의 load 트리거보다 먼저 hidden input 을 채워야 함)
// classic <script src> 로 로드한다.

interface GlobalDateRange {
	start?: string;
	end?: string;
	mode?: string;
	timeZone?: string;
	timezone?: string;
}

// 날짜(YYYY-MM-DD) -> 서버에 보낼 instant. 규칙은 date-range-picker.ts 에 한 벌만 둔다
// (종료일은 다음 날 00:00 = 배타적 경계). 예전에는 이 파일 안에 두 벌이 복사돼 있었고 정본과도
// 셋이 서로 달랐다 - 실측으로 "2026-08-" 은 두 사본에서 조용히 2026-07-31 이 됐고,
// "2026-08-23T00:00:00" 은 사본에서만 예외를 던졌다.
// 레이아웃(stockLayout.jte)이 date-range-picker.js 를 이 파일보다 먼저 동기 로드한다.
function localToIso(ds: string, addDays?: number): string {
	const shared = (globalThis as any).__dateRangePickerInternals;
	if (!shared || typeof shared.localDateToInstantIso !== "function") {
		// 규칙을 여기서 다시 구현하지 않는다. 날짜를 지어내느니 기간 없음으로 두는 쪽이 안전하다.
		return "";
	}
	return shared.localDateToInstantIso(ds, addDays);
}

const TRADE_HISTORY_PATH = "/stock/htmx/trade-history";

/**
 * 기간이 바뀔 때 매매 내역 패널을 다시 불러올 주소.
 *
 * 기간만 바꾸고 나머지 조건(계좌·종목 필터, 페이지 크기, 정렬)은 그대로 이어받는다.
 * 실측 2026-09-12(자산 성장, 한 계좌로 좁힌 상태에서 "1년" 클릭): 여기서 만든 주소에 계좌·종목
 * 필터가 빠져 있어 좁혀 놓은 표에 다섯 계좌의 거래가 +83ms~+135ms 동안 보였다가 뒤늦게 정정됐다.
 * 뒤에 오는 뷰 전체 재조회가 덮어 주지만, 덮기 전까지는 틀린 표다.
 *
 * 조건은 패널이 들고 있는 hx-get 을 먼저 보고, 없으면(교체된 뒤라 속성이 사라진 경우) 주소에서 읽는다.
 */
function tradeHistoryRefreshUrl(
	panelHxGet: string,
	locationSearch: string,
	start: string,
	end: string,
): string {
	const fromPanel =
		panelHxGet.indexOf(TRADE_HISTORY_PATH) === 0 && panelHxGet.indexOf("?") >= 0;
	const query = fromPanel
		? panelHxGet.slice(panelHxGet.indexOf("?") + 1)
		: locationSearch.replace("?", "");
	const params = new URLSearchParams(query);
	// 기간을 뜻하는 값은 이 갱신이 정한다 - 남겨 두면 옛 기간이 같이 실린다.
	["rangeMode", "startDate", "endDate", "timeZone", "locale"].forEach((k) =>
		params.delete(k),
	);
	if (start) params.set("from", start);
	else params.delete("from");
	if (end) params.set("to", end);
	else params.delete("to");
	const qs = params.toString();
	return TRADE_HISTORY_PATH + (qs ? "?" + qs : "");
}

function input(id: string): HTMLInputElement | null {
	return document.getElementById(id) as HTMLInputElement | null;
}

// 전역 기간 원본 읽기: localStorage 우선(탭 간 공유), 없으면 sessionStorage 폴백(기존 세션 호환).
function readGlobalRangeRaw(): string | null {
	try {
		if (typeof localStorage !== "undefined") {
			const v = localStorage.getItem("globalDateRange");
			if (v) return v;
		}
	} catch (e) {}
	try {
		if (typeof sessionStorage !== "undefined") {
			return sessionStorage.getItem("globalDateRange");
		}
	} catch (e) {}
	return null;
}

// 1) 최초 로드 시 전역 기간 값을 hidden input 에 반영
(function () {
	try {
		const raw = readGlobalRangeRaw();
		if (raw) {
			const obj: GlobalDateRange = JSON.parse(raw);
			const gStart = input("globalStartInstantInput");
			const gEnd = input("globalEndInstantInput");
			const gTz = input("globalTimeZoneInput");
			const gMode = input("globalRangeModeInput");
			if (gStart) gStart.value = obj.start ? localToIso(obj.start, 0) : "";
			if (gEnd) gEnd.value = obj.end ? localToIso(obj.end, 1) : "";
			if (gTz) gTz.value = obj.timeZone || "";
			if (gMode) gMode.value = obj.mode || "";
		}
	} catch (e) {}
})();

// 2) globalDateRange:changed 이벤트를 받아 폼/프래그먼트로 전파
(function () {
	try {
		function applyGlobalDate(obj: GlobalDateRange) {
			try {
				const gStart = input("globalStartInstantInput");
				const gEnd = input("globalEndInstantInput");
				const gTz = input("globalTimeZoneInput");
				const gMode = input("globalRangeModeInput");
				let nextTimeZone =
					obj.timeZone || obj.timezone || (gTz && gTz.value) || "";
				if (!nextTimeZone) {
					try {
						const savedRaw = readGlobalRangeRaw();
						if (savedRaw) {
							const saved: GlobalDateRange = JSON.parse(savedRaw);
							nextTimeZone =
								saved && (saved.timeZone || saved.timezone)
									? saved.timeZone || saved.timezone || ""
									: "";
						}
					} catch (e) {}
				}
				if (gStart) gStart.value = obj.start ? localToIso(obj.start, 0) : "";
				if (gEnd) gEnd.value = obj.end ? localToIso(obj.end, 1) : "";
				if (gTz) gTz.value = nextTimeZone || "";
				if (gMode) gMode.value = obj.mode || "";

				// update per-form inputs so HTMX includes the new range when forms submit
				document
					.querySelectorAll<HTMLInputElement>('input[name="startDate"]')
					.forEach((inp) => {
						inp.value = obj.start ? localToIso(obj.start, 0) : "";
					});
				document
					.querySelectorAll<HTMLInputElement>('input[name="endDate"]')
					.forEach((inp) => {
						inp.value = obj.end ? localToIso(obj.end, 1) : "";
					});
				document
					.querySelectorAll<HTMLInputElement>('input[name="timeZone"]')
					.forEach((inp) => {
						inp.value = nextTimeZone || "";
					});
				document
					.querySelectorAll<HTMLInputElement>('input[name="rangeMode"]')
					.forEach((inp) => {
						inp.value = obj.mode || "";
					});
			} catch (e) {}
		}

		// Listen for global date changes and propagate to forms/fragments
		window.addEventListener(
			"globalDateRange:changed",
			(ev: Event) => {
				try {
					const detail = (ev as CustomEvent)?.detail || null;
					let obj: GlobalDateRange | null = null;
					if (
						detail &&
						(detail.start !== undefined || detail.end !== undefined)
					) {
						obj = {
							start: detail.start || "",
							end: detail.end || "",
							mode: detail.mode || "",
							timeZone: detail.timeZone || detail.timezone || "",
						};
					} else {
						const raw = readGlobalRangeRaw();
						if (raw) {
							try {
								obj = JSON.parse(raw);
							} catch (e) {
								obj = null;
							}
						}
					}
					if (!obj) return;
					applyGlobalDate(obj);

					// Refresh trade-history once for this global range (avoid duplicates)
					try {
						const w = window as any;
						const tkey = (obj.start || "") + "::" + (obj.end || "");
						if (!w.lastGlobalTradeKey || w.lastGlobalTradeKey !== tkey) {
							try {
								w.lastGlobalTradeKey = tkey;
							} catch (e) {}
							const tradeHistoryPanel = document.getElementById(
								"trade-history-panel",
							);
							if (
								tradeHistoryPanel &&
								typeof w.htmx !== "undefined" &&
								w.htmx &&
								typeof w.htmx.ajax === "function"
							) {
								// 기간만 바꾸고 나머지 조건은 그대로 이어받는다.
								// 실측 2026-09-12(자산 성장, 한 계좌로 좁힌 상태에서 "1년" 클릭):
								// 여기서 만든 주소에 계좌·종목 필터가 빠져 있어서, 좁혀 놓은 표에
								// 다섯 계좌의 거래가 +83ms~+135ms 동안 보였다가 뒤늦게 정정됐다.
								// 뒤에 오는 뷰 전체 재조회가 덮어 주지만, 덮기 전까지는 틀린 표다.
								const url = tradeHistoryRefreshUrl(
									tradeHistoryPanel.getAttribute("hx-get") || "",
									(window.location && window.location.search) || "",
									obj.start || "",
									obj.end || "",
								);
								w.htmx.ajax("GET", url, {
									target: tradeHistoryPanel,
									swap: "outerHTML",
								});
							}
						}
					} catch (e) {}

					// Trigger common HTMX-driven search forms to reload (if present)
					try {
						const formsToTrigger = [
							"tradeSearchForm",
						];
						formsToTrigger.forEach((id) => {
							try {
								if (detail && detail.sourceFormId && detail.sourceFormId === id)
									return;
								const f = document.getElementById(id) as HTMLFormElement | null;
								if (!f) return;
								const hasHx =
									f.hasAttribute("hx-get") ||
									f.hasAttribute("hx-post") ||
									f.querySelector("[hx-get], [hx-post], [hx-put], [hx-delete]");
								if (hasHx) {
									if (typeof f.requestSubmit === "function") {
										f.requestSubmit();
									} else {
										f.dispatchEvent(
											new Event("submit", { bubbles: true, cancelable: true }),
										);
									}
								}
							} catch (e) {}
						});
					} catch (e) {}
				} catch (e) {}
			},
			false,
		);
	} catch (e) {}
})();

// 3) 다른 탭에서 기간이 바뀌면(localStorage 'storage' 이벤트) 이 탭에도 반영한다.
//    storage 이벤트는 변경을 일으킨 탭이 아닌 '다른' 탭에서만 발생한다.
(function () {
	try {
		window.addEventListener("storage", (ev: StorageEvent) => {
			try {
				if (ev.key !== "globalDateRange") return;
				const raw = ev.newValue || readGlobalRangeRaw();
				if (!raw) return;
				const obj: GlobalDateRange = JSON.parse(raw);
				if (!obj) return;
				// 기존 changed 핸들러가 hidden input/폼 동기화 + 표준 검색폼 재조회를 처리한다.
				window.dispatchEvent(
					new CustomEvent("globalDateRange:changed", { detail: obj }),
				);
			} catch (e) {}
		});
	} catch (e) {}
})();

(globalThis as any).__globalDateRangeInternals = { tradeHistoryRefreshUrl };

// 4) 스왑이 끝나면 프리셋 버튼의 눌린 표시를 "서버가 실제로 적용한 기간" 으로 되돌린다.
//    버튼은 클릭 즉시 눌린 표시를 바꾸는데(낙관적), 앞 요청이 진행 중이면 뒤 제출이 버려진다.
//    그러면 화면은 "이번달" 이라고 말하면서 3년 데이터를 보여 준다
//    (실측 2026-09-13, 매매 3년→이번달 연속 클릭 12 회 중 6 회). 값이 맞는 정상 경로에서는 아무것도 바뀌지 않는다.
function pressedTargetsForMode(
	mode: string,
	args: string[],
): boolean[] {
	// 프리셋 버튼은 data-picker-arg 로 자기 모드를 들고 있다. '전체' 만 서버 값 all 에 대해 0 이다.
	return args.map(
		(arg) => mode !== "" && (arg === mode || (mode === "all" && arg === "0")),
	);
}

(function () {
	try {
		const FRAGMENT_IDS = ["tradeListFragment", "activityListFragment"];
		const syncPressed = (mode: string) => {
			const btns = Array.from(
				document.querySelectorAll("button.date-range-btn"),
			);
			const flags = pressedTargetsForMode(
				mode,
				btns.map((b) => b.getAttribute("data-picker-arg") || ""),
			);
			btns.forEach((b, i) => {
				const on = flags[i];
				b.classList.toggle("btn-primary", on);
				b.classList.toggle("btn-ghost", !on);
				b.setAttribute("aria-pressed", on ? "true" : "false");
			});
		};
		let lastAppliedMode: string | null = null;
		let requestSeen = false;
		document.body.addEventListener(
			"htmx:afterSwap",
			(ev: Event) => {
				try {
					const target = ev.target as HTMLElement | null;
					if (!target || !target.id) return;
					if (FRAGMENT_IDS.indexOf(target.id) < 0) return;
					const applied = target.getAttribute("data-applied-range-mode");
					if (applied === null) return;
					lastAppliedMode = applied;
					syncPressed(applied);
				} catch (e) {}
			},
			false,
		);
		// 클릭했는데 요청이 아예 안 나가는 경우(앞 요청 때문에 버려짐)에는 스왑도 없다.
		// 그때는 마지막으로 서버가 적용한 기간으로 표시를 되돌려, 화면이 거짓말하지 않게 한다.
		document.body.addEventListener(
			"htmx:beforeRequest",
			() => {
				requestSeen = true;
			},
			true,
		);
		document.body.addEventListener(
			"click",
			(ev: Event) => {
				try {
					const el = ev.target as HTMLElement | null;
					const btn = el && el.closest ? el.closest("button.date-range-btn") : null;
					if (!btn) return;
					requestSeen = false;
					setTimeout(() => {
						try {
							if (requestSeen) return;
							if (lastAppliedMode === null) return;
							syncPressed(lastAppliedMode);
						} catch (e) {}
					}, 400);
				} catch (e) {}
			},
			true,
		);
	} catch (e) {}
})();
