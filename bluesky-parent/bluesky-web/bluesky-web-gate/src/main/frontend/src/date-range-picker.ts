// DateRangePicker (TypeScript)
type PickerState = { start: string; end: string; mode: string };

function fmtDate(d: Date): string {
	return (
		d.getFullYear() +
		"-" +
		String(d.getMonth() + 1).padStart(2, "0") +
		"-" +
		String(d.getDate()).padStart(2, "0")
	);
}

/**
 * 화면이 고른 날짜(YYYY-MM-DD)를 서버에 보낼 instant 로 바꾼다.
 *
 * 시작일은 그대로(addDays=0), 종료일은 <b>다음 날 00:00</b>(addDays=1) 을 보낸다 - api-stock 의
 * 기간 규약이 배타적이기 때문이다(시계열의 toInclusiveEndDate, 필터 id 조회의 `< :endDate`).
 * 이 한 줄이 모든 주식 화면의 기간을 정한다.
 *
 * 형식이 아니면 "" 를 돌려준다(= 기간 없음). 예전에는 자릿수를 확인하지 않아 두 가지가 생겼다.
 *  - "abc" 처럼 아예 못 읽는 값에서 RangeError 가 나 그 뒤 대입이 통째로 건너뛰어졌다.
 *  - "2026-08-" 처럼 잘린 값은 Number("") 가 0 이라 조용히 2026-07-31 이 됐다(더 나쁘다 - 틀린 기간이
 *    아무 표시 없이 조회에 실린다).
 *
 * 같은 규칙이 globalDateRange.ts 에 두 벌 더 있었고 셋의 동작이 서로 달랐다. 지금은 이 함수 하나만
 * 있고 globalDateRange 는 __dateRangePickerInternals 로 이 함수를 가져다 쓴다(레이아웃이 이 파일을
 * 먼저 로드한다).
 */
/**
 * 사용자 지정(모드 없는) 구간을 한 칸 앞뒤로 옮긴다.
 *
 * <p>표시 구간은 <b>양끝 포함</b>이라 길이는 {@code end - start + 1}일이다. 예전에는 {@code end - start} 만큼만 옮겨
 * 직전 구간이 원래 시작일을 다시 덮었다 - 실측 2026-09-11: 5/10~5/19(10일)에서 '이전' 이 5/1~5/10 을 줘 5/10 이 두 구간에
 * 들어갔고, 하루짜리 구간(6/10~6/10)은 {@code ms <= 0} 이라 아예 움직이지 않았다.
 */
/**
 * N개월 프리셋을 <b>데이터 처음부터 앞으로</b> 잡을지 정한다(현재 규칙을 그대로 옮겨 적은 것이다).
 *
 * 보고 있는 창이 데이터의 시작에 붙어 있고 끝(오늘)에는 닿지 않았을 때만 그렇게 한다 - 처음부터 훑어 보는 사람이
 * "1년" 을 누르면 첫 1년을 보고 싶어 하기 때문이다. 그 밖에는 늘 오늘에서 뒤로 N개월이다.
 *
 * <p>알려진 함정(실측 2026-09-12, 오늘 09-12): 주소에 {@code 2009-01-01~2026-09-11} 처럼 <b>데이터 전체를 덮되
 * 끝이 오늘보다 이른</b> 구간을 싣고 들어가 "1년" 을 누르면 2009-10-06~2010-10-05 가 나온다 - 같은 버튼이 깨끗한
 * 진입에서는 2025-09-13~2026-09-12 를 준다. 경계는 데이터 첫날이다(시작 2009-10-06 이하면 앞으로, 10-07 부터는 오늘 기준).
 * {@code rangeMode=all}·모드 없는 진입·2010 년 이후 시작은 모두 정상이라 화면의 링크로는 닿지 않는다.
 * "이미 전체를 보고 있을 때 프리셋이 무엇을 뜻하는가" 는 제품 결정이라 여기서 바꾸지 않았다.
 */
function presetAnchorsAtDataStart(
	curStart: string,
	curEnd: string,
	minDateStr: string | null | undefined,
	maxDateStr: string,
	todayStr: string,
): boolean {
	const atDataEnd = !curEnd || curEnd >= todayStr;
	if (atDataEnd) return false;
	return !!minDateStr && !!curStart && curStart <= minDateStr;
}

function shiftFreeRange(startStr: string, endStr: string, dir: number) {
	if (!startStr || !endStr) return null;
	const s = new Date(startStr + "T00:00:00");
	const e = new Date(endStr + "T00:00:00");
	const span = e.getTime() - s.getTime();
	if (isNaN(span) || span < 0) return null;
	const step = span + 86400000; // 양끝 포함이므로 하루를 더해야 구간이 겹치지 않는다
	return {
		start: new Date(s.getTime() + dir * step),
		end: new Date(e.getTime() + dir * step),
	};
}

/**
 * 사용자 지정 구간을 그 방향으로 옮길 수 있는가.
 *
 * <p>앞으로 갈 때는 <b>시작</b>이 최대일(오늘)을 넘을 때만 막는다. 끝이 넘는 경우는 적용 쪽이 오늘로 자른다.
 * 예전에는 끝으로 막아 그 자르기 코드가 닿지 못했고, 사용자 지정 구간만 마지막 창에 도달하지 못했다 -
 * 실측 2026-09-11: 8/20~8/29 를 앞으로 밀면 8/30~9/8 에서 버튼이 죽어 9/9~9/11 을 볼 수 없었다
 * (같은 화면의 1개월 창은 9/1~9/11 로 잘려 도달한다).
 */
function freeRangeShiftAllowed(
	startStr: string,
	endStr: string,
	dir: number,
	maxDateStr: string,
	minDateStr?: string | null,
): boolean {
	const moved = shiftFreeRange(startStr, endStr, dir);
	if (!moved) return false;
	if (dir > 0) {
		if (!maxDateStr) return true;
		return moved.start <= new Date(maxDateStr + "T00:00:00");
	}
	if (minDateStr) return moved.start >= new Date(minDateStr + "T00:00:00");
	return true;
}

/**
 * 저장된 전역 기간(stored)이 조각이 지금 보여 주는 기간(current)과 같은가.
 *
 * create() 는 초기화 때 전역 기간을 복원하고 "데이터가 실리도록" 폼을 한 번 제출한다. 그런데 조각은 페이지의
 * hx-include(#globalDateRangeInputs)로 이미 그 기간을 받아 렌더된 상태라, 같은 조회가 두 번 나갔다
 * (실측 2026-09-09: /stock/dividend 진입 시 dividend/list 196KB x2, /stock/asset-growth 진입 시 asset-growth/view 66KB x2,
 * 두 번째는 첫 번째와 바이트까지 같았다). 같으면 제출할 이유가 없다.
 */
function restoredRangeAlreadyShown(
	current: PickerRange,
	stored: PickerRange,
): boolean {
	const norm = (v: unknown) => (v == null ? "" : String(v));
	return (
		norm(current.start) === norm(stored.start) &&
		norm(current.end) === norm(stored.end) &&
		norm(current.mode) === norm(stored.mode)
	);
}

type PickerRange = { start?: string | null; end?: string | null; mode?: string | null };

/**
 * 초기화 때 어느 기간을 쓸지. 조각(current)은 페이지가 hx-include 로 보낸 전역 기간, 또는 URL 쿼리가 덮어쓴 기간으로 이미
 * 그려져 있다(common.ts 의 data-params-from-query: "URL 의 키가 hx-include 를 덮어쓴다").
 *
 * - 조각에 기간이 없다: 저장값(stored)으로 예전처럼 한 번 제출해 데이터를 실어 온다.
 * - 같다: 이미 그려져 있으니 아무것도 하지 않는다.
 * - 다르다: 조각이 이긴다. 저장값을 조각의 기간으로 갱신하고 제출하지 않는다.
 *   (실측 2026-09-09: /stock/dividend?...rangeMode=3 으로 들어가면 첫 조회는 3개월인데 곧 저장된 ytd 로 재조회돼 공유 링크가 무력했다.)
 */
type InitialRange = { range: PickerRange; submit: boolean; persist: boolean };

/** 기간이라고 할 만한 것이 하나라도 들어 있는가. */
function hasRange(r: PickerRange | null): boolean {
	return !!r && !!(r.start || r.end || r.mode);
}

function resolveInitialRange(
	current: PickerRange | null,
	stored: PickerRange,
): InitialRange {
	if (!hasRange(current)) return { range: stored, submit: true, persist: false };
	if (restoredRangeAlreadyShown(current as PickerRange, stored))
		return { range: stored, submit: false, persist: false };
	return { range: current as PickerRange, submit: false, persist: true };
}

/**
 * 저장된 전역 기간이 아직 없는 첫 방문에 쓸 기간.
 *
 * 예전에는 저장값이 없으면 초기화를 통째로 건너뛰었다. 그래서 공유 링크의 기간이 그 화면 한 장에서만 살고, 메뉴를 한 번만 눌러도 사라졌다
 * (실측 2026-09-11, 저장값을 지운 상태: /stock/trade?rangeMode=3 은 3개월로 열리지만 배당 화면으로 넘어가면 올해로 돌아갔다.
 * 종목 상세에서 '다른 종목' 으로 바꿔도 마찬가지였다). 저장값이 한 번이라도 있으면 멀쩡했던 이유가 이것이다.
 *
 * 조각은 이미 그 기간으로 그려져 있으니 저장만 하고 제출하지 않는다 - 여기서 제출하면 같은 조회가 두 번 나간다.
 */
function firstVisitRange(current: PickerRange | null): InitialRange | null {
	if (!hasRange(current)) return null;
	return { range: current as PickerRange, submit: false, persist: true };
}

function localDateToInstantIso(ds: string, addDays?: number): string {
	if (!ds) return "";
	const matched = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(ds);
	if (!matched) return "";
	const y = Number.parseInt(matched[1], 10);
	const m = Number.parseInt(matched[2], 10) - 1;
	const d = Number.parseInt(matched[3], 10);
	const dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
	return Number.isNaN(dt.getTime()) ? "" : dt.toISOString();
}

// 기간 창 이동 계산. 예전에는 초기화 클로저 안에 있어 node 테스트가 닿지 못했고,
// 실행 시점에만 내부 노출 객체에 얹혔다(브라우저에서 초기화되기 전에는 undefined).
// 순수 계산이라 모듈 범위로 올린다 - 실측 2026-09-12: 이동/왕복/경계 규칙에 가드가 하나도 없었다.
const parseLocalDate = (value: string) => new Date(value + "T00:00:00");
const isLastDayOfMonth = (date: Date) =>
	date.getDate() ===
	new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
const addMonthsClamped = (date: Date, months: number) => {
	const targetFirst = new Date(
		date.getFullYear(),
		date.getMonth() + months,
		1,
	);
	const targetLastDay = new Date(
		targetFirst.getFullYear(),
		targetFirst.getMonth() + 1,
		0,
	).getDate();
	return new Date(
		targetFirst.getFullYear(),
		targetFirst.getMonth(),
		Math.min(date.getDate(), targetLastDay),
	);
};
// 상대 개월 구간(1/3/6/12/36개월)은 "정확히 N개월"이어야 한다.
// minusMonths(N) 은 양끝 포함이라 N개월+1일이 되므로, 시작일은 +1일,
// (데이터 시작 앵커의) 종료일은 -1일 보정한다.
const addDays = (date: Date, days: number) =>
	new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
const isWholeMonthRange = (startStr: string, endStr: string) => {
	if (!startStr || !endStr) return false;
	const startDate = parseLocalDate(startStr);
	const endDate = parseLocalDate(endStr);
	return startDate.getDate() === 1 && isLastDayOfMonth(endDate);
};
const shiftNumericMonthRange = (
	startStr: string,
	endStr: string,
	months: number,
	dir: number,
	maxDate?: Date,
	minDate?: Date | null,
) => {
	if (!startStr || !endStr) return null;
	const startDate = parseLocalDate(startStr);
	const endDate = parseLocalDate(endStr);
	let nextStart: Date;
	let nextEnd: Date;

	if (isWholeMonthRange(startStr, endStr)) {
		nextStart = new Date(
			startDate.getFullYear(),
			startDate.getMonth() + dir * months,
			1,
		);
		nextEnd = new Date(
			nextStart.getFullYear(),
			nextStart.getMonth() + months,
			0,
		);
	} else {
		// 종료일은 옮긴 시작일에서 다시 센다. 양끝을 따로 클램프하면 창 길이가 달라진다 -
		// 실측 2026-09-11: 1개월 창을 뒤로 걸어가면 1/28~2/28 처럼 32 일짜리가 나왔다(31 일이어야 한다).
		// 통월 가지가 이미 같은 방식이고, 서버의 프리셋 정의(minusMonths(N).plusDays(1))와도 맞는다.
		nextStart = addMonthsClamped(startDate, dir * months);
		nextEnd = addDays(addMonthsClamped(nextStart, months), -1);
	}

	if (dir > 0 && maxDate) {
		if (nextStart > maxDate) return null;
		if (nextEnd > maxDate) nextEnd = new Date(maxDate);
	}
	if (dir < 0 && minDate && nextStart < minDate) return null;

	return {
		start: fmtDate(nextStart),
		end: fmtDate(nextEnd),
	};
};

const DateRangePicker = (function () {
	function create(cfg: any) {
		const _s: PickerState = { start: "", end: "", mode: "" };

		const isCallback = () => typeof cfg.onApply === "function";
		// 프리셋(이번 달·1M·3M…)은 눌린 상태를 색(btn-primary)으로만 보였다. 토글 버튼의 상태는 aria-pressed 로도 알려야 한다
		// (실측 2026-09-09 axe 는 잡지 못하는 항목 - 상태를 색으로만 전달, WCAG 1.4.1). 활성/비활성을 바꾸는 모든 자리에서 함께 맞춘다.
// '가장 이른 기간으로'(«) 는 데이터 시작일을 알아야 목표를 정할 수 있다. 모르면 doJumpToEdge 가
// 그냥 돌아오는데, 버튼은 눌리는 채로 남아 아무 반응이 없다 - 실측 2026-09-13: 종목·계좌 상세에서
// minDate 가 빈 값이라 두 번 눌러도 기간이 그대로였다(매매 화면은 2009-10-06 으로 이동).
// '전체' 일 때도 같은 이유로 돌아오므로 함께 막는다.
function jumpStartDisabled(minDate: string, mode: string): boolean {
	return !minDate || mode === "all";
}

const activeClass = () => cfg.activeClass || "btn-primary";
		const resolvedTimeZone = () => {
			try {
				return Intl?.DateTimeFormat?.().resolvedOptions().timeZone || "";
			} catch (e) {
				return "";
			}
		};


		const el = (id?: string) => (id ? document.getElementById(id) : null);
		const btns = (root?: Element | Document) =>
			cfg.btnClass
				? Array.from((root || document).querySelectorAll("." + cfg.btnClass))
				: ([] as Element[]);

		function clearActive(root?: Element | Document) {
			btns(root).forEach((b) => {
				b.classList.remove(activeClass());
				b.setAttribute("aria-pressed", "false");
			});
		}

		// CSP 정리로 프리셋 버튼의 인라인 onclick 이 제거되어(data-picker-action/arg 로 전환)
		// 버튼-모드 매칭은 data 속성을 우선 사용하고, 과거 onclick 문자열 매칭은 폴백으로 유지한다.
		const btnSetArg = (b: Element): string | null => {
			const d = (b as HTMLElement).dataset;
			if (!d || d.pickerAction !== "set" || d.pickerArg == null) return null;
			return String(d.pickerArg);
		};
		const btnMatchesMode = (b: Element, mode: any): boolean => {
			const modeStr = String(mode);
			const arg = btnSetArg(b);
			if (arg !== null) {
				if (arg === modeStr) return true;
				return modeStr === "all" && arg === "0";
			}
			const onclick = b.getAttribute("onclick") || "";
			return (
				onclick.indexOf("set(" + modeStr + ",") !== -1 ||
				onclick.indexOf("set('" + modeStr + "'") !== -1 ||
				(modeStr === "all" && onclick.indexOf("set(0,") !== -1)
			);
		};

		const prevClassName = cfg.prevClass || "date-range-prev";
		const nextClassName = cfg.nextClass || "date-range-next";

		function canShift(dir: number): boolean {
			try {
				const start = getStart();
				const end = getEnd();
				const mode = getMode();
				if (!start || mode === "all") return false;
				const maxDate = new Date(maxDateStr() + "T00:00:00");
				const minDate = cfg.minDate
					? new Date(cfg.minDate + "T00:00:00")
					: null;

				// Quick boundary checks: if current view already reaches data edge,
				// disallow shifting further in that direction.
				try {
					const sDate = start ? new Date(start + "T00:00:00") : null;
					const eDate = end ? new Date(end + "T00:00:00") : null;
					if (dir > 0 && eDate && eDate >= maxDate) return false;
					if (dir < 0 && sDate && minDate && sDate <= minDate) return false;
				} catch (e) {}
				const isMtd = mode === "mtd";
				const isYtd =
					!isMtd &&
					(mode === "ytd" || (mode === "" && start.slice(5) === "01-01"));
				if (isMtd) {
					const curFirst = new Date(start + "T00:00:00");
					const newFirst = new Date(
						curFirst.getFullYear(),
						curFirst.getMonth() + dir,
						1,
					);
					const thisMonthFirst = new Date(
						maxDate.getFullYear(),
						maxDate.getMonth(),
						1,
					);
					if (dir > 0 && newFirst > thisMonthFirst) return false;
					if (dir < 0 && minDate) return newFirst >= minDate;
					return true;
				}
				if (isYtd) {
					const newYear = Number.parseInt(start.slice(0, 4), 10) + dir;
					if (dir > 0 && newYear > maxDate.getFullYear()) return false;
					if (dir < 0 && minDate) {
						const ns = new Date(newYear + "-01-01T00:00:00");
						return ns >= minDate;
					}
					return true;
				}
				if (mode && !isNaN(+mode) && +mode > 0) {
					const months = +mode;
					return !!shiftNumericMonthRange(
						start,
						end || start,
						months,
						dir,
						maxDate,
						minDate,
					);
				}
				// Free range
				if (!end) return false;
				return freeRangeShiftAllowed(
					start,
					end,
					dir,
					maxDateStr(),
					cfg.minDate || null,
				);
			} catch (e) {
				return false;
			}
		}

		function updatePrevNextState() {
			try {
				const root = cfg.rootSelector
					? document.querySelector(cfg.rootSelector) || document
					: document;
				const prevEls = Array.from(
					(root as Element).querySelectorAll("." + prevClassName),
				) as HTMLButtonElement[];
				const nextEls = Array.from(
					(root as Element).querySelectorAll("." + nextClassName),
				) as HTMLButtonElement[];
				const start = getStart();
				const end = getEnd();
				const mode = getMode();
				const maxDate = new Date(maxDateStr() + "T00:00:00");
				const minDate = cfg.minDate
					? new Date(cfg.minDate + "T00:00:00")
					: null;
				let disablePrev = !canShift(-1);
				let disableNext = !canShift(1);
				// 서버가 그린 btn-disabled 도 여기 계산에 맞춘다. 실측 2026-09-09: 서버는 '다음' 을 막았는데(btn-disabled) 클라이언트 계산은
				// 허용해 disabled 만 풀렸다 - 비활성처럼 보이는데 눌리고 포커스도 받는 버튼이 됐다(axe 도 대비 위반으로 잡음).
				prevEls.forEach((el) => {
					el.disabled = disablePrev;
					el.classList.toggle("btn-disabled", disablePrev);
					if (disablePrev) {
						el.classList.add("opacity-40");
						el.setAttribute("aria-disabled", "true");
						try {
							el.style.opacity = "0.2";
						} catch (e) {}
					} else {
						el.classList.remove("opacity-40");
						el.removeAttribute("aria-disabled");
						try {
							el.style.opacity = "";
						} catch (e) {}
					}
				});
				// 눌리는데 아무 일도 안 하는 버튼을 남기지 않는다.
				const jumpStartEls = Array.from(
					(root as Element).querySelectorAll(
						'[data-picker-action="jump"][data-picker-arg="start"]',
					),
				) as HTMLButtonElement[];
				const disableJumpStart = jumpStartDisabled(cfg.minDate || "", mode);
				jumpStartEls.forEach((el) => {
					// « 는 '이전' 과 같은 클래스(date-range-prev)를 달고 있어 위에서 이미 판정이 끝났다.
					// 여기서 되살리면 최저 구간에 도착해 막아 둔 버튼이 다시 눌리게 된다 - 막기만 한다.
					const off = disableJumpStart || el.disabled;
					el.disabled = off;
					el.classList.toggle("btn-disabled", off);
					if (off) {
						el.classList.add("opacity-40");
						el.setAttribute("aria-disabled", "true");
						try {
							el.style.opacity = "0.2";
						} catch (e) {}
					} else {
						el.classList.remove("opacity-40");
						el.removeAttribute("aria-disabled");
						try {
							el.style.opacity = "";
						} catch (e) {}
					}
				});
				nextEls.forEach((el) => {
					el.disabled = disableNext;
					el.classList.toggle("btn-disabled", disableNext);
					if (disableNext) {
						el.classList.add("opacity-40");
						el.setAttribute("aria-disabled", "true");
						try {
							el.style.opacity = "0.2";
						} catch (e) {}
					} else {
						el.classList.remove("opacity-40");
						el.removeAttribute("aria-disabled");
						try {
							el.style.opacity = "";
						} catch (e) {}
					}
				});
			} catch (e) {
				// swallow
			}
		}

		function getStart(): string {
			if (isCallback()) return _s.start;
			const dEl = el(cfg.startId) as HTMLInputElement | null;
			if (dEl && dEl.value) return dEl.value;
			const instEl = el(cfg.instantStartId) as HTMLInputElement | null;
			if (instEl && instEl.value) {
				try {
					const dt = new Date(instEl.value);
					if (!isNaN(dt.getTime())) return fmtDate(dt);
				} catch (e) {}
			}
			return "";
		}
		function getEnd(): string {
			if (isCallback()) return _s.end;
			const dEl = el(cfg.endId) as HTMLInputElement | null;
			if (dEl && dEl.value) return dEl.value;
			const instEl = el(cfg.instantEndId) as HTMLInputElement | null;
			if (instEl && instEl.value) {
				try {
					const dt = new Date(instEl.value);
					if (!isNaN(dt.getTime()))
						return fmtDate(
							new Date(dt.getFullYear(), dt.getMonth(), dt.getDate()),
						);
				} catch (e) {}
			}
			return "";
		}
		function getMode(): string {
			return isCallback()
				? _s.mode
				: (el(cfg.rangeModeId) as HTMLInputElement | null)?.value || "";
		}

		/** 전역 저장(localStorage)과 레이아웃의 공용 숨은 입력을 이 기간으로 맞춘다. 이벤트는 내지 않는다(재조회 없음). */
		function persistGlobalRange(startStr: string, endStr: string, modeStr: string) {
			const tz = resolvedTimeZone() || "";
			try {
				if (cfg.globalKey && typeof localStorage !== "undefined") {
					localStorage.setItem(
						cfg.globalKey,
						JSON.stringify({ start: startStr, end: endStr, mode: modeStr, timeZone: tz }),
					);
				}
			} catch (e) {}
			try {
				const g = (id: string) => document.getElementById(id) as HTMLInputElement | null;
				const gStart = g("globalStartInstantInput");
				const gEnd = g("globalEndInstantInput");
				const gTz = g("globalTimeZoneInput");
				const gMode = g("globalRangeModeInput");
				if (gStart) gStart.value = startStr ? localDateToInstantIso(startStr, 0) : "";
				if (gEnd) gEnd.value = endStr ? localDateToInstantIso(endStr, 1) : "";
				if (gTz) gTz.value = tz;
				if (gMode) gMode.value = modeStr || "";
			} catch (e) {}
		}

		function maxDateStr() {
			return cfg.maxDate || fmtDate(new Date());
		}

		function applyRange(startStr: string, endStr: string, modeStr: string) {
			// helper to update shared hidden inputs used by layout/HTMX
			function setGlobalHiddenInputs(sStr: string, eStr: string, mStr: string) {
				try {
					const gStart = document.getElementById(
						"globalStartInstantInput",
					) as HTMLInputElement | null;
					const gEnd = document.getElementById(
						"globalEndInstantInput",
					) as HTMLInputElement | null;
					const gTz = document.getElementById(
						"globalTimeZoneInput",
					) as HTMLInputElement | null;
					const gMode = document.getElementById(
						"globalRangeModeInput",
					) as HTMLInputElement | null;
					const tzVal = resolvedTimeZone();
					if (gStart) gStart.value = sStr ? localDateToInstantIso(sStr, 0) : "";
					if (gEnd) gEnd.value = eStr ? localDateToInstantIso(eStr, 1) : "";
					if (gTz) gTz.value = tzVal || "";
					if (gMode) gMode.value = mStr || "";
				} catch (e) {}
			}

			try {
				if (isCallback()) {
					_s.start = startStr;
					_s.end = endStr;
					_s.mode = modeStr;
					cfg.onApply(startStr, endStr, modeStr);
					try {
						if (cfg.globalKey && typeof localStorage !== "undefined") {
							const tz = resolvedTimeZone() || null;
							localStorage.setItem(
								cfg.globalKey,
								JSON.stringify({
									start: startStr || "",
									end: endStr || "",
									mode: modeStr || "",
									timeZone: tz || "",
								}),
							);
						}
					} catch (e) {}
					try {
						setGlobalHiddenInputs(startStr || "", endStr || "", modeStr || "");
					} catch (e) {}
					try {
						if (
							typeof window !== "undefined" &&
							typeof (window as any).dispatchEvent === "function"
						) {
							window.dispatchEvent(
								new CustomEvent("globalDateRange:changed", {
									detail: {
										start: startStr || "",
										end: endStr || "",
										mode: modeStr || "",
										timeZone: resolvedTimeZone() || "",
									},
								}),
							);
						}
					} catch (e) {}
					try {
						updatePrevNextState();
						setTimeout(() => {
							try {
								updatePrevNextState();
							} catch (e) {}
						}, 80);
					} catch (e) {}
				} else {
					const se = el(cfg.startId) as HTMLInputElement | null;
					const ee = el(cfg.endId) as HTMLInputElement | null;
					const me = el(cfg.rangeModeId) as HTMLInputElement | null;
					if (se) se.value = startStr || "";
					if (ee) ee.value = endStr || "";
					if (me) me.value = modeStr || "";
					try {
						if (cfg.instantStartId) {
							const instSe = el(cfg.instantStartId) as HTMLInputElement | null;
							if (instSe)
								instSe.value = startStr
									? localDateToInstantIso(startStr, 0)
									: "";
						}
						if (cfg.instantEndId) {
							const instEe = el(cfg.instantEndId) as HTMLInputElement | null;
							if (instEe)
								instEe.value = endStr ? localDateToInstantIso(endStr, 1) : "";
						}
						try {
							const tz = resolvedTimeZone() || "UTC";
							if (cfg.timeZoneId) {
								const tzEl = el(cfg.timeZoneId) as HTMLInputElement | null;
								if (tzEl) tzEl.value = tz || "UTC";
							}
						} catch (e) {}
						try {
							if (cfg.globalKey && typeof localStorage !== "undefined") {
								const tz2 = resolvedTimeZone() || null;
								localStorage.setItem(
									cfg.globalKey,
									JSON.stringify({
										start: startStr || "",
										end: endStr || "",
										mode: modeStr || "",
										timeZone: tz2 || "",
									}),
								);
							}
						} catch (e) {}
						try {
							setGlobalHiddenInputs(
								startStr || "",
								endStr || "",
								modeStr || "",
							);
						} catch (e) {}
						try {
							if (
								typeof window !== "undefined" &&
								typeof (window as any).dispatchEvent === "function"
							) {
								window.dispatchEvent(
									new CustomEvent("globalDateRange:changed", {
										detail: {
											start: startStr || "",
											end: endStr || "",
											mode: modeStr || "",
											timeZone: resolvedTimeZone() || "",
										},
									}),
								);
							}
						} catch (e) {}
					} catch (e) {}
					try {
						updatePrevNextState();
						setTimeout(() => {
							try {
								updatePrevNextState();
							} catch (e) {}
						}, 80);
					} catch (e) {}
					const form = el(cfg.formId) as HTMLFormElement | null;
					if (form) {
						if (typeof form.requestSubmit === "function") form.requestSubmit();
						else {
							try {
								const ev = new Event("submit", {
									bubbles: true,
									cancelable: true,
								});
								const prevented = !form.dispatchEvent(ev);
								if (!prevented) {
									const tmp = document.createElement("button");
									tmp.type = "submit";
									tmp.style.display = "none";
									form.appendChild(tmp);
									tmp.click();
									tmp.remove();
								}
							} catch (e) {
								try {
									const tmp2 = document.createElement("button");
									tmp2.type = "submit";
									tmp2.style.display = "none";
									form.appendChild(tmp2);
									tmp2.click();
									tmp2.remove();
								} catch (e2) {
									form.submit();
								}
							}
						}
					}
				}
			} catch (e) {}
		}

		// Apply range to inputs and update UI without submitting the form.
		function applyRangeNoSubmit(
			startStr: string,
			endStr: string,
			modeStr: string,
		) {
			try {
				const se = el(cfg.startId) as HTMLInputElement | null;
				const ee = el(cfg.endId) as HTMLInputElement | null;
				const me = el(cfg.rangeModeId) as HTMLInputElement | null;
				if (se) se.value = startStr || "";
				if (ee) ee.value = endStr || "";
				if (me) me.value = modeStr || "";
				try {
					if (cfg.instantStartId) {
						const instSe = el(cfg.instantStartId) as HTMLInputElement | null;
						if (instSe)
							instSe.value = startStr ? localDateToInstantIso(startStr, 0) : "";
					}
					if (cfg.instantEndId) {
						const instEe = el(cfg.instantEndId) as HTMLInputElement | null;
						if (instEe)
							instEe.value = endStr ? localDateToInstantIso(endStr, 1) : "";
					}
					try {
						const tz = resolvedTimeZone() || "UTC";
						if (cfg.timeZoneId) {
							const tzEl = el(cfg.timeZoneId) as HTMLInputElement | null;
							if (tzEl) tzEl.value = tz || "UTC";
						}
					} catch (e) {}
				} catch (e) {}
				try {
					updatePrevNextState();
					setTimeout(() => {
						try {
							updatePrevNextState();
						} catch (e) {}
					}, 80);
				} catch (e) {}
			} catch (e) {}
		}

		function doSet(months: any, btn: Element | null) {
			clearActive();
			if (btn) {
				try {
					btn.classList.add(activeClass());
					btn.classList.remove("btn-ghost");
					btn.setAttribute("aria-pressed", "true");
				} catch (e) {}
			}
			const maxStr = maxDateStr();
			const maxDate = new Date(maxStr + "T00:00:00");
			const today = new Date();
			today.setHours(0, 0, 0, 0);
			let startStr = "",
				endStr = "",
				modeStr = "";
			if (months === 0) {
				startStr = "";
				endStr = "";
				modeStr = "all";
			} else if (months === "mtd") {
				startStr = fmtDate(new Date(today.getFullYear(), today.getMonth(), 1));
				endStr = fmtDate(today);
				modeStr = "mtd";
			} else if (months === "ytd") {
				startStr = today.getFullYear() + "-01-01";
				endStr = fmtDate(today);
				modeStr = "ytd";
			} else {
				const curEnd = getEnd();
				const curStart = getStart();
				const todayStr = fmtDate(today);
				const atDataStart = presetAnchorsAtDataStart(
					curStart,
					curEnd,
					cfg.minDate,
					maxStr,
					todayStr,
				);
				if (atDataStart) {
					const minD = new Date(cfg.minDate + "T00:00:00");
					let e = addDays(addMonthsClamped(minD, months), -1);
					if (e > maxDate) e = new Date(maxDate);
					startStr = fmtDate(minD);
					endStr = fmtDate(e);
				} else {
					const s = addDays(addMonthsClamped(maxDate, -months), 1);
					startStr = fmtDate(s);
					endStr = maxStr;
				}
				modeStr = String(months);
			}
			try {
				const root = cfg.rootSelector
					? document.querySelector(cfg.rootSelector) || document
					: document;
				btns(root).forEach((b) => {
					b.classList.remove(activeClass());
					b.classList.add("btn-ghost");
					b.setAttribute("aria-pressed", "false");
				});
				if (btn) {
					btn.classList.add(activeClass());
					btn.classList.remove("btn-ghost");
					btn.setAttribute("aria-pressed", "true");
				}
			} catch (e) {}
			applyRange(startStr, endStr, modeStr);
		}

		function doJumpToEdge(direction: string) {
			const mode = getMode();
			if (mode === "all") return;
			const maxStr = maxDateStr();
			const maxDate = new Date(maxStr + "T00:00:00");
			const start = getStart();
			const isMtd = mode === "mtd";
			const isYtd =
				!isMtd &&
				(mode === "ytd" ||
					(mode === "" && !!start && start.slice(5) === "01-01"));
			let edgeMode = isMtd ? "1" : isYtd ? "12" : mode;
			clearActive();
			let startStr = "",
				endStr = "";
			if (direction === "end") {
				endStr = maxStr;
				if (isMtd) {
					startStr = fmtDate(
						new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
					);
				} else if (isYtd) {
					startStr = maxDate.getFullYear() + "-01-01";
				} else if (edgeMode === "1" && isWholeMonthRange(start || "", getEnd() || "")) {
					// 통월 뷰에서 끝으로 이동 = 이번달. 상대 1개월(오늘-1개월~오늘)로 변질시키지 않는다.
					startStr = fmtDate(
						new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
					);
					edgeMode = "mtd";
				} else if (edgeMode && !isNaN(Number(edgeMode)) && +edgeMode > 0) {
					const s = addDays(addMonthsClamped(maxDate, -+edgeMode), 1);
					startStr = fmtDate(s);
				} else {
					const cs = getStart(),
						ce = getEnd();
					if (!cs || !ce) return;
					const ms =
						new Date(ce + "T00:00:00").getTime() -
						new Date(cs + "T00:00:00").getTime();
					if (ms <= 0) return;
					startStr = fmtDate(new Date(maxDate.getTime() - ms));
				}
			} else {
				if (!cfg.minDate) return;
				const minD = new Date(cfg.minDate + "T00:00:00");
				if (isMtd) {
					const first = new Date(minD.getFullYear(), minD.getMonth(), 1);
					const last = new Date(minD.getFullYear(), minD.getMonth() + 1, 0);
					startStr = fmtDate(first);
					endStr = fmtDate(last > maxDate ? maxDate : last);
				} else if (isYtd) {
					const minYear = minD.getFullYear();
					startStr = minYear + "-01-01";
					endStr =
						minYear === maxDate.getFullYear() ? maxStr : minYear + "-12-31";
				} else if (edgeMode && !isNaN(Number(edgeMode)) && +edgeMode > 0) {
					let e = addDays(addMonthsClamped(minD, +edgeMode), -1);
					if (e > maxDate) e = new Date(maxDate);
					startStr = fmtDate(minD);
					endStr = fmtDate(e);
				} else {
					const cs = getStart(),
						ce = getEnd();
					if (!cs || !ce) return;
					const ms =
						new Date(ce + "T00:00:00").getTime() -
						new Date(cs + "T00:00:00").getTime();
					if (ms <= 0) return;
					let ne = new Date(minD.getTime() + ms);
					if (ne > maxDate) ne = new Date(maxDate);
					startStr = fmtDate(minD);
					endStr = fmtDate(ne);
				}
			}
			applyRange(startStr, endStr, edgeMode);
		}

		function doShift(dir: number) {
			const start = getStart(),
				end = getEnd(),
				mode = getMode();
			if (!start || mode === "all") return;
			const maxStr = maxDateStr();
			const maxDate = new Date(maxStr + "T00:00:00");
			clearActive();
			const isMtd = mode === "mtd";
			const isYtd =
				!isMtd &&
				(mode === "ytd" || (mode === "" && start.slice(5) === "01-01"));
			let newStart = "",
				newEnd = "";
			let newMode = isMtd ? "1" : isYtd ? "12" : mode;
			if (isMtd) {
				const curFirst = new Date(start + "T00:00:00");
				const newFirst = new Date(
					curFirst.getFullYear(),
					curFirst.getMonth() + dir,
					1,
				);
				const newLast = new Date(
					curFirst.getFullYear(),
					curFirst.getMonth() + dir + 1,
					0,
				);
				const thisMonthFirst = new Date(
					maxDate.getFullYear(),
					maxDate.getMonth(),
					1,
				);
				if (dir > 0 && newFirst > thisMonthFirst) return;
				newStart = fmtDate(newFirst);
				newEnd = fmtDate(newLast > maxDate ? maxDate : newLast);
			} else if (isYtd) {
				const newYear = Number.parseInt(start.slice(0, 4), 10) + dir;
				if (dir > 0 && newYear > maxDate.getFullYear()) return;
				newStart = newYear + "-01-01";
				newEnd =
					newYear === maxDate.getFullYear() ? maxStr : newYear + "-12-31";
			} else if (mode && !isNaN(+mode) && +mode > 0) {
				const months = +mode;
				// Special-case 12-month presets: if the current view appears to be
				// a calendar year (start == Jan-01 or end == Dec-31) treat the shift
				// as a calendar-year shift instead of a relative month shift. This
				// avoids incorrect results when the picker's internal start/end were
				// snapped to label boundaries (e.g. '2024-12-01').
				try {
					const isCalendarYearView =
						(start && start.slice(5) === "01-01") ||
						(end && end.slice(5) === "12-31");
					if (months === 12 && isCalendarYearView) {
						// Anchor to the year from either start (Jan-01) or end (Dec-31).
						let anchorYear: number | null = null;
						try {
							if (start && start.slice(5) === "01-01")
								anchorYear = Number.parseInt(start.slice(0, 4), 10);
							else if (end && end.slice(5) === "12-31")
								anchorYear = Number.parseInt(end.slice(0, 4), 10);
						} catch (e) {
							anchorYear = null;
						}
						if (anchorYear === null || isNaN(anchorYear as any)) {
							anchorYear = start
								? Number.parseInt(start.slice(0, 4), 10)
								: null;
						}
						if (anchorYear !== null && !isNaN(anchorYear as any)) {
							const newYear = anchorYear + dir;
							newStart = newYear + "-01-01";
							newEnd =
								newYear === maxDate.getFullYear() ? maxStr : newYear + "-12-31";
							newMode =
								newEnd === maxStr && newStart.slice(5) === "01-01"
									? "ytd"
									: String(months);
						} else {
							const shifted = shiftNumericMonthRange(
								start,
								end || start,
								months,
								dir,
								maxDate,
								null,
							);
							if (!shifted) return;
							newStart = shifted.start;
							newEnd = shifted.end;
						}
					} else {
						const shifted = shiftNumericMonthRange(
							start,
							end || start,
							months,
							dir,
							maxDate,
							null,
						);
						if (!shifted) return;
						newStart = shifted.start;
						newEnd = shifted.end;
						// 통월(1개월) 뷰가 현재 달에 도달하면 '이번달(mtd)' 모드로 복원한다.
						// (ytd 의 연도 복원과 대칭) 복원하지 않으면 7/1~오늘이 '1개월' 상대 구간이
						// 되어 다음 '이전' 클릭이 6/1~6/7 처럼 구간을 훼손한다.
						if (months === 1) {
							const thisMonthFirstStr = fmtDate(
								new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
							);
							if (newStart === thisMonthFirstStr && newEnd === maxStr) {
								newMode = "mtd";
							}
						}
					}
				} catch (e) {
					// On any unexpected error, gracefully bail out
					return;
				}
			} else {
				if (!end) return;
				const moved = shiftFreeRange(start, end, dir);
				if (!moved) return;
				const ns = moved.start;
				const ne = moved.end;
				if (dir > 0 && ns > maxDate) return;
				if (dir > 0 && ne > maxDate) ne.setTime(maxDate.getTime());
				newStart = fmtDate(ns);
				newEnd = fmtDate(ne);
			}
			try {
				const root = cfg.rootSelector
					? document.querySelector(cfg.rootSelector) || document
					: document;
				btns(root).forEach((b: Element) => {
					b.classList.remove(activeClass());
					b.classList.add("btn-ghost");
					b.setAttribute("aria-pressed", "false");
				});
				Array.from(
					(root as Element).querySelectorAll("." + cfg.btnClass),
				).forEach((b: Element) => {
					if (btnMatchesMode(b, newMode)) {
						b.classList.add(activeClass());
						b.classList.remove("btn-ghost");
						b.setAttribute("aria-pressed", "true");
					}
				});
			} catch (e) {}
			applyRange(newStart, newEnd, newMode);
		}

		// 사용자가 날짜 입력을 직접 고치면 hidden Instant(startDate/endDate)로 동기화한다.
		// 이게 없으면 화면의 날짜만 바뀌고 서버로는 이전 기간이 그대로 전송되어
		// "기간을 지정하고 검색해도 적용되지 않는" 것처럼 보인다.
		function syncManualInput() {
			const se = el(cfg.startId) as HTMLInputElement | null;
			const ee = el(cfg.endId) as HTMLInputElement | null;
			if (!se || !ee) return;
			const s = se.value || "";
			const e2 = ee.value || "";
			if (!s || !e2) return; // 한쪽만 입력된 중간 상태는 무시
			// 시작이 종료보다 뒤면 사용자가 방금 고친 쪽을 기준으로 맞춰준다.
			let start = s;
			let end = e2;
			if (start > end) {
				if (document.activeElement === se) end = start;
				else start = end;
				se.value = start;
				ee.value = end;
			}
			clearActive(); // 수동 지정이므로 프리셋 활성 표시 해제
			applyRangeNoSubmit(start, end, "");
			// 다른 화면에도 같은 기간이 이어지도록 전역 저장(프리셋 경로와 동일 형식).
			try {
				if (cfg.globalKey && typeof localStorage !== "undefined") {
					const tz = resolvedTimeZone() || null;
					localStorage.setItem(
						cfg.globalKey,
						JSON.stringify({ start: start, end: end, mode: "", timeZone: tz || "" }),
					);
				}
			} catch (e) {}
		}

		try {
			[cfg.startId, cfg.endId].forEach((id: string) => {
				const inputEl = el(id) as HTMLInputElement | null;
				if (!inputEl) return;
				inputEl.addEventListener("change", syncManualInput);
				// 달력 사용성: 데이터가 있는 구간으로 선택 범위를 제한하고,
				// 입력 어디를 눌러도 달력이 열리게 한다(기본 동작은 작은 아이콘만 클릭 가능).
				if (cfg.minDate) inputEl.min = cfg.minDate;
				inputEl.max = maxDateStr();
				inputEl.addEventListener("click", () => {
					try {
						const anyEl = inputEl as any;
						if (typeof anyEl.showPicker === "function") anyEl.showPicker();
					} catch (e) {}
				});
			});
		} catch (e) {}

		try {
			updatePrevNextState();
		} catch (e) {}

		// on init: prefer sessionStorage value if cfg.globalKey provided
		try {
			if (cfg.globalKey) {
				// localStorage 우선(탭 간 공유), 없으면 sessionStorage 폴백(기존 세션 호환).
				const raw =
					(typeof localStorage !== "undefined" &&
						localStorage.getItem(cfg.globalKey)) ||
					sessionStorage.getItem(cfg.globalKey);
				{
					try {
						const stored = raw ? JSON.parse(raw) : null;
						// 조각이 이미 보여 주는 기간과 저장값을 견줘 쓸 기간을 정한다(콜백 모드는 예전처럼 저장값).
						// 저장값이 아직 없으면 조각이 들고 있는 기간을 저장만 한다(firstVisitRange).
						const current = isCallback()
							? null
							: { start: getStart(), end: getEnd(), mode: getMode() };
						const initial = stored
							? resolveInitialRange(current, stored)
							: firstVisitRange(current);
						const obj: any = initial ? { ...initial.range } : null;
						if (obj) {
							const root = cfg.rootSelector
								? document.querySelector(cfg.rootSelector) || document
								: document;
							// try to find a matching button inside the configured root
							let foundBtn: Element | null = null;
							try {
								const candidates = Array.from(
									(root as Element).querySelectorAll(
										"." + (cfg.btnClass || ""),
									),
								);
								for (const c of candidates) {
									if (obj.mode && btnMatchesMode(c, obj.mode)) {
										foundBtn = c as Element;
										break;
									}
								}

								// If explicit mode not present but start/end exist, try to infer which quick-button
								if (!foundBtn && obj.start && obj.end) {
									for (const c of candidates) {
										try {
											let arg: string | null = btnSetArg(c);
											if (arg === null) {
												const onclick = c.getAttribute("onclick") || "";
												const m = onclick.match(
													/set\(\s*(?:'([^']+)'|"([^"]+)"|([^,\)\s]+))/,
												);
												if (!m) continue;
												arg = m[1] || m[2] || m[3];
											}
											let months: any = arg;
											if (/^\d+$/.test(String(arg))) months = Number(arg);
											const computeRange = (monthsParam: any) => {
												const maxStr = maxDateStr();
												const maxDate = new Date(maxStr + "T00:00:00");
												const today = new Date();
												today.setHours(0, 0, 0, 0);
												let startStr = "",
													endStr = "",
													modeStr = "";
												if (monthsParam === 0) {
													startStr = "";
													endStr = "";
													modeStr = "all";
												} else if (monthsParam === "mtd") {
													startStr = fmtDate(
														new Date(
															maxDate.getFullYear(),
															maxDate.getMonth(),
															1,
														),
													);
													endStr = maxStr;
													modeStr = "mtd";
												} else if (monthsParam === "ytd") {
													startStr = maxDate.getFullYear() + "-01-01";
													endStr = maxStr;
													modeStr = "ytd";
												} else {
													const curEnd = getEnd();
													const curStart = getStart();
													const todayStr = fmtDate(today);
													const atDataEnd = !curEnd || curEnd >= todayStr;
													const atDataStart =
														!atDataEnd &&
														!!cfg.minDate &&
														!!curStart &&
														curStart <= cfg.minDate;
													if (atDataStart) {
														const minD = new Date(cfg.minDate + "T00:00:00");
														let e = addDays(addMonthsClamped(minD, monthsParam), -1);
														if (e > maxDate) e = new Date(maxDate);
														startStr = fmtDate(minD);
														endStr = fmtDate(e);
													} else {
														const s = addDays(addMonthsClamped(maxDate, -monthsParam), 1);
														startStr = fmtDate(s);
														endStr = maxStr;
													}
													modeStr = String(monthsParam);
												}
												return { start: startStr, end: endStr, mode: modeStr };
											};
											const r = computeRange(months);
											if (r.start === obj.start && r.end === obj.end) {
												foundBtn = c as Element;
												obj.mode = r.mode || obj.mode;
												break;
											}
										} catch (e) {
											/* ignore candidate errors */
										}
									}
								}
							} catch (e) {}
							// Set visual state and inputs only — do NOT submit form to avoid HTMX reload loops
							try {
								const rootEl = cfg.rootSelector
									? document.querySelector(cfg.rootSelector) || document
									: document;
								btns(rootEl).forEach((b) => {
									b.classList.remove(activeClass());
									b.classList.add("btn-ghost");
									b.setAttribute("aria-pressed", "false");
								});
								if (foundBtn) {
									foundBtn.classList.add(activeClass());
									foundBtn.classList.remove("btn-ghost");
									foundBtn.setAttribute("aria-pressed", "true");
								}
							} catch (e) {}
							// 조각이 이미 이 기간으로 그려져 있으면(서버가 입력값을 채워 보냄) 아래 재제출은 같은 조회를 한 번 더 하는 것이다.
							const alreadyShown = !initial!.submit;
							applyRangeNoSubmit(
								obj.start || "",
								obj.end || "",
								obj.mode || "",
							);
							if (initial!.persist) {
								// 조각(URL)의 기간이 이겼다: 다른 화면도 이 기간을 따르도록 전역 저장과 공용 숨은 입력을 맞춘다.
								persistGlobalRange(obj.start || "", obj.end || "", obj.mode || "");
							}
							// One-time apply: call callback or submit form once per-fragment to ensure data loads
							try {
								const appliedKey =
									"__globalDateRangeApplied:" +
									(cfg.formId || cfg.rootSelector || cfg.btnClass || "");
								if (!alreadyShown && !sessionStorage.getItem(appliedKey)) {
									if (isCallback()) {
										try {
											_s.start = obj.start || "";
											_s.end = obj.end || "";
											_s.mode = obj.mode || "";
											if (typeof cfg.onApply === "function") {
												cfg.onApply(_s.start, _s.end, _s.mode);
											}
										} catch (e) {}
									} else {
										const formEl = el(cfg.formId) as HTMLFormElement | null;
										if (formEl) {
											try {
												if (typeof formEl.requestSubmit === "function")
													formEl.requestSubmit();
												else formEl.submit();
											} catch (e) {}
										} else {
											try {
												applyRange(
													obj.start || "",
													obj.end || "",
													obj.mode || "",
												);
											} catch (e) {}
										}
									}
									try {
										sessionStorage.setItem(appliedKey, "1");
									} catch (e) {}
								}
							} catch (e) {}
						}
					} catch (e) {}
				}
			}
		} catch (e) {}

		return {
			set: (months: any, btn: Element | null) => doSet(months, btn),
			jumpToEdge: (direction: string) => doJumpToEdge(direction),
			shift: (dir: number) => doShift(dir),
			setMinDate: (d: string) => {
				cfg.minDate = d;
			},
			setMaxDate: (d: string) => {
				cfg.maxDate = d;
			},
			setState: (start: string, end: string, mode: string) => {
				if (isCallback()) {
					_s.start = start;
					_s.end = end;
					if (mode !== undefined && mode !== null && mode !== "") {
						_s.mode = mode;
					}
				}
				// Only update visual quick-button state when an explicit mode value
				// (non-empty) is provided. Avoid clearing active button when caller
				// passes an empty-string mode (common when only start/end were stored).
				if (mode !== undefined && mode !== null && mode !== "") {
					try {
						const root = cfg.rootSelector
							? document.querySelector(cfg.rootSelector) || document
							: document;
						btns(root).forEach((b: Element) => {
							try {
								b.classList.remove(activeClass());
								b.classList.add("btn-ghost");
								b.setAttribute("aria-pressed", "false");
							} catch (e) {}
						});
						if (mode) {
							try {
								Array.from(
									(root as Element).querySelectorAll(
										"." + (cfg.btnClass || ""),
									),
								).forEach((b: Element) => {
									if (btnMatchesMode(b, mode)) {
										try {
											b.classList.add(activeClass());
											b.classList.remove("btn-ghost");
											b.setAttribute("aria-pressed", "true");
										} catch (e) {}
									}
								});
							} catch (e) {}
						}
					} catch (e) {}
				}
			},
			getState: () => ({ start: getStart(), end: getEnd(), mode: getMode() }),
			canShift: (dir: number) => {
				try {
					return canShift(dir);
				} catch (e) {
					return false;
				}
			},
		};
	}
	return { create, fmt: fmtDate };
})();

// expose to global
(globalThis as any).DateRangePicker = DateRangePicker as any;
// 테스트가 계산만 따로 부를 수 있게 노출한다(브라우저 동작에는 영향 없음).
(globalThis as any).__dateRangePickerInternals = {
	localDateToInstantIso,
	fmtDate,
	restoredRangeAlreadyShown,
	resolveInitialRange,
	firstVisitRange,
	shiftFreeRange,
	freeRangeShiftAllowed,
	shiftNumericMonthRange,
	addMonthsClamped,
	isWholeMonthRange,
	presetAnchorsAtDataStart,
};
