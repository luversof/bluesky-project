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

const DateRangePicker = (function () {
	function create(cfg: any) {
		const _s: PickerState = { start: "", end: "", mode: "" };

		const isCallback = () => typeof cfg.onApply === "function";
		const activeClass = () => cfg.activeClass || "btn-primary";
		const resolvedTimeZone = () => {
			try {
				return Intl?.DateTimeFormat?.().resolvedOptions().timeZone || "";
			} catch (e) {
				return "";
			}
		};
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
				nextStart = addMonthsClamped(startDate, dir * months);
				nextEnd = addMonthsClamped(endDate, dir * months);
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

		const el = (id?: string) => (id ? document.getElementById(id) : null);
		const btns = (root?: Element | Document) =>
			cfg.btnClass
				? Array.from((root || document).querySelectorAll("." + cfg.btnClass))
				: ([] as Element[]);

		function clearActive(root?: Element | Document) {
			btns(root).forEach((b) => b.classList.remove(activeClass()));
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
				const s = new Date(start + "T00:00:00");
				const e = new Date(end + "T00:00:00");
				const ms = e.getTime() - s.getTime();
				if (ms <= 0) return false;
				const ns = new Date(s.getTime() + dir * ms);
				const ne = new Date(e.getTime() + dir * ms);
				// when shifting forward, new end must not exceed maxDate
				if (dir > 0 && ne > maxDate) return false;
				// when shifting backward, new start must not be before minDate
				if (dir < 0 && minDate) return ns >= minDate;
				return true;
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
				prevEls.forEach((el) => {
					el.disabled = disablePrev;
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
				nextEls.forEach((el) => {
					el.disabled = disableNext;
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
				const atDataEnd = !curEnd || curEnd >= todayStr;
				const atDataStart =
					!atDataEnd && !!cfg.minDate && !!curStart && curStart <= cfg.minDate;
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
				});
				if (btn) {
					btn.classList.add(activeClass());
					btn.classList.remove("btn-ghost");
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
				const s = new Date(start + "T00:00:00");
				const e = new Date(end + "T00:00:00");
				const ms = e.getTime() - s.getTime();
				if (ms <= 0) return;
				const ns = new Date(s.getTime() + dir * ms);
				const ne = new Date(e.getTime() + dir * ms);
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
				});
				Array.from(
					(root as Element).querySelectorAll("." + cfg.btnClass),
				).forEach((b: Element) => {
					if (btnMatchesMode(b, newMode)) {
						b.classList.add(activeClass());
						b.classList.remove("btn-ghost");
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
				if (raw) {
					try {
						const obj = JSON.parse(raw);
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
								});
								if (foundBtn) {
									foundBtn.classList.add(activeClass());
									foundBtn.classList.remove("btn-ghost");
								}
							} catch (e) {}
							applyRangeNoSubmit(
								obj.start || "",
								obj.end || "",
								obj.mode || "",
							);
							// One-time apply: call callback or submit form once per-fragment to ensure data loads
							try {
								const appliedKey =
									"__globalDateRangeApplied:" +
									(cfg.formId || cfg.rootSelector || cfg.btnClass || "");
								if (!sessionStorage.getItem(appliedKey)) {
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
(globalThis as any).__dateRangePickerInternals = { localDateToInstantIso, fmtDate };
