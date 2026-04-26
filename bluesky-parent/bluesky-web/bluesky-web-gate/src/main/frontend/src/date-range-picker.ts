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

const DateRangePicker = (function () {
	function create(cfg: any) {
		const _s: PickerState = { start: "", end: "", mode: "" };

		const isCallback = () => typeof cfg.onApply === "function";
		const activeClass = () => cfg.activeClass || "btn-primary";

		const el = (id?: string) => (id ? document.getElementById(id) : null);
		const btns = (root?: Element | Document) =>
			cfg.btnClass
				? Array.from((root || document).querySelectorAll("." + cfg.btnClass))
				: ([] as Element[]);

		function clearActive(root?: Element | Document) {
			btns(root).forEach((b) => b.classList.remove(activeClass()));
		}

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
					const s = new Date(start + "T00:00:00");
					const e = end ? new Date(end + "T00:00:00") : new Date(s);
					s.setMonth(s.getMonth() + dir * months);
					e.setMonth(e.getMonth() + dir * months);
					if (dir > 0 && s > maxDate) return false;
					if (dir < 0 && minDate) return s >= minDate;
					return true;
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
						// ensure visual dimming even if template used inline styles
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
			// fallback to instant input if date input empty
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
			try {
				if (isCallback()) {
					_s.start = startStr;
					_s.end = endStr;
					_s.mode = modeStr;
					cfg.onApply(startStr, endStr, modeStr);
					try {
						if (cfg.globalKey && typeof sessionStorage !== "undefined") {
							const tz =
								Intl?.DateTimeFormat?.().resolvedOptions().timeZone || null;
							sessionStorage.setItem(
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

					// notify global sync listeners (callback mode)
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
									},
								}),
							);
						}
					} catch (e) {}
					// ensure prev/next buttons reflect new state in callback mode as well
					try {
						updatePrevNextState();
						// run again after a short delay to let charts/DOM finish animating
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
						function localDateToInstantIso(ds: string, addDays?: number) {
							if (!ds) return "";
							const parts = ds.split("-");
							const y = Number.parseInt(parts[0], 10);
							const m = Number.parseInt(parts[1], 10) - 1;
							const d = Number.parseInt(parts[2], 10);
							const dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
							return dt.toISOString();
						}
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
							const tz =
								Intl?.DateTimeFormat?.().resolvedOptions().timeZone || "UTC";
							if (cfg.timeZoneId) {
								const tzEl = el(cfg.timeZoneId) as HTMLInputElement | null;
								if (tzEl) tzEl.value = tz || "UTC";
							}
						} catch (e) {}
						// persist selection to sessionStorage even when we're using form submit mode
						try {
							if (cfg.globalKey && typeof sessionStorage !== "undefined") {
								const tz2 =
									Intl?.DateTimeFormat?.().resolvedOptions().timeZone || null;
								sessionStorage.setItem(
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

						// notify global sync listeners that the active global range changed
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
						if (typeof form.requestSubmit === "function") {
							form.requestSubmit();
						} else {
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
					function localDateToInstantIso(ds: string, addDays?: number) {
						if (!ds) return "";
						const parts = ds.split("-");
						const y = Number.parseInt(parts[0], 10);
						const m = Number.parseInt(parts[1], 10) - 1;
						const d = Number.parseInt(parts[2], 10);
						const dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
						return dt.toISOString();
					}
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
						const tz =
							Intl?.DateTimeFormat?.().resolvedOptions().timeZone || "UTC";
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
				// add active style to clicked button and remove ghost style
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
				startStr = fmtDate(
					new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
				);
				endStr = maxStr;
				modeStr = "mtd";
			} else if (months === "ytd") {
				startStr = maxDate.getFullYear() + "-01-01";
				endStr = maxStr;
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
					let e = new Date(minD);
					e.setMonth(e.getMonth() + months);
					if (e > maxDate) e = new Date(maxDate);
					startStr = fmtDate(minD);
					endStr = fmtDate(e);
				} else {
					const s = new Date(maxDate);
					s.setMonth(s.getMonth() - months);
					startStr = fmtDate(s);
					endStr = maxStr;
				}
				modeStr = String(months);
			}
			// ensure visual state: toggle classes within configured rootSelector
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
			clearActive();
			let startStr = "",
				endStr = "";
			if (direction === "end") {
				endStr = maxStr;
				if (mode && !isNaN(Number(mode)) && +mode > 0) {
					const s = new Date(maxDate);
					s.setMonth(s.getMonth() - +mode);
					startStr = fmtDate(s);
				} else if (mode === "mtd") {
					startStr = fmtDate(
						new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
					);
				} else if (mode === "ytd") {
					startStr = maxDate.getFullYear() + "-01-01";
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
				if (mode && !isNaN(Number(mode)) && +mode > 0) {
					let e = new Date(minD);
					e.setMonth(e.getMonth() + +mode);
					if (e > maxDate) e = new Date(maxDate);
					startStr = fmtDate(minD);
					endStr = fmtDate(e);
				} else if (mode === "mtd") {
					const first = new Date(minD.getFullYear(), minD.getMonth(), 1);
					const last = new Date(minD.getFullYear(), minD.getMonth() + 1, 0);
					startStr = fmtDate(first);
					endStr = fmtDate(last > maxDate ? maxDate : last);
				} else if (mode === "ytd") {
					const minYear = minD.getFullYear();
					startStr = minYear + "-01-01";
					endStr =
						minYear === maxDate.getFullYear() ? maxStr : minYear + "-12-31";
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
			applyRange(startStr, endStr, getMode());
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
			const newMode = isMtd ? "1" : isYtd ? "12" : mode;
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
				const s = new Date(start + "T00:00:00");
				const e = end ? new Date(end + "T00:00:00") : new Date(s);
				s.setMonth(s.getMonth() + dir * months);
				e.setMonth(e.getMonth() + dir * months);
				if (dir > 0 && s > maxDate) return;
				if (dir > 0 && e > maxDate) e.setTime(maxDate.getTime());
				newStart = fmtDate(s);
				newEnd = fmtDate(e);
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
			// reactivate appropriate button inside configured root
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
					if (
						(b.getAttribute("onclick") || "").indexOf(
							"set(" + newMode + ",",
						) !== -1
					) {
						b.classList.add(activeClass());
						b.classList.remove("btn-ghost");
					}
				});
			} catch (e) {}
			applyRange(newStart, newEnd, newMode);
		}

		try {
			updatePrevNextState();
		} catch (e) {}

		// on init: prefer sessionStorage value if cfg.globalKey provided
		try {
			if (cfg.globalKey) {
				const raw = sessionStorage.getItem(cfg.globalKey);
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
									(root as Element).querySelectorAll("." + (cfg.btnClass || "")),
								);
								for (const c of candidates) {
									const onclick = c.getAttribute("onclick") || "";
									if (
										onclick.indexOf("set(" + obj.mode + ",") !== -1 ||
										onclick.indexOf("set('" + obj.mode + "'") !== -1
									) {
										foundBtn = c as Element;
										break;
									}
								}
							} catch (e) {}
							// Set visual state and inputs only
							try {
								const rootEl = cfg.rootSelector
									? document.querySelector(cfg.rootSelector) || document
									: document;
								btns(rootEl).forEach((b: Element) => {
									b.classList.remove(activeClass());
									b.classList.add("btn-ghost");
								});
								if (foundBtn) {
									foundBtn.classList.add(activeClass());
									foundBtn.classList.remove("btn-ghost");
								}
							} catch (e) {}
							// update inputs (no submit) so hidden instant inputs are present
							applyRangeNoSubmit(obj.start || "", obj.end || "", obj.mode || "");

							// One-time apply: call callback or submit form once per-fragment to ensure data loads
							try {
								const appliedKey = '__globalDateRangeApplied:' + (cfg.formId || cfg.rootSelector || cfg.btnClass || '');
								if (!sessionStorage.getItem(appliedKey)) {
									if (isCallback()) {
										// ensure internal state matches session then invoke callback
										try {
											_s.start = obj.start || '';
											_s.end = obj.end || '';
											_s.mode = obj.mode || '';
											if (typeof cfg.onApply === 'function') {
												cfg.onApply(_s.start, _s.end, _s.mode);
											}
										} catch (e) {}
									} else {
										// non-callback (form) mode: submit the configured form so server-side fragment loads with session range
										const formEl = el(cfg.formId) as HTMLFormElement | null;
										if (formEl) {
											try {
												if (typeof formEl.requestSubmit === 'function') formEl.requestSubmit();
												else formEl.submit();
											} catch (e) {}
										} else {
											// fallback: call applyRange which will set inputs and attempt submission
											try { applyRange(obj.start || '', obj.end || '', obj.mode || ''); } catch (e) {}
										}
									}
									try { sessionStorage.setItem(appliedKey, '1'); } catch (e) {}
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
					// only overwrite internal mode when a non-empty mode is provided
					if (mode !== undefined && mode !== null && mode !== '') {
						_s.mode = mode;
					}
				}
				// Update button visuals only when caller explicitly provides a mode value.
				// This prevents accidental clearing of active state during chart-driven
				// view syncs that pass an empty mode string.
				if (mode !== undefined) {
					try {
						const root = cfg.rootSelector
							? document.querySelector(cfg.rootSelector) || document
							: document;
						// clear and set ghost style
						btns(root).forEach((b: Element) => {
							try {
								b.classList.remove(activeClass());
								b.classList.add('btn-ghost');
							} catch (e) {}
						});
						// if a concrete mode was provided, try to reactivate matching button
						if (mode) {
							try {
								Array.from((root as Element).querySelectorAll('.' + (cfg.btnClass || ''))).forEach((b: Element) => {
									const onclick = b.getAttribute && b.getAttribute('onclick') ? b.getAttribute('onclick') || '' : '';
									if (onclick.indexOf('set(' + mode + ',') !== -1 || onclick.indexOf("set('" + mode + "'") !== -1) {
										try { b.classList.add(activeClass()); b.classList.remove('btn-ghost'); } catch (e) {}
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

// --- Global sync helpers: ensure session-stored global range is applied to forms ---
(function () {
	function parseGlobal() {
		try {
			if (typeof sessionStorage === "undefined") return null;
			var raw = sessionStorage.getItem("globalDateRange");
			if (!raw) return null;
			return JSON.parse(raw);
		} catch (e) {
			return null;
		}
	}

	function localDateToInstantIso(ds: string, addDays?: number) {
		if (!ds) return "";
		try {
			var parts = ds.split("-");
			var y = Number.parseInt(parts[0], 10);
			var m = Number.parseInt(parts[1], 10) - 1;
			var d = Number.parseInt(parts[2], 10);
			var dt = new Date(y, m, d + (addDays || 0), 0, 0, 0, 0);
			return dt.toISOString();
		} catch (e) {
			return "";
		}
	}

	function ensureHiddenInput(
		form: HTMLFormElement,
		name: string,
		value: string,
	) {
		try {
			var el = form.querySelector(
				'input[name="' + name + '"]',
			) as HTMLInputElement | null;
			if (!el) {
				el = document.createElement("input");
				el.type = "hidden";
				el.name = name;
				form.appendChild(el);
			}
			if (el.value !== (value || "")) el.value = value || "";
		} catch (e) {}
	}

	function applyToForm(form: HTMLFormElement) {
		var g = parseGlobal();
		if (!g) return;
		// server expects ISO instants for startDate/endDate
		var sIso = g.start ? localDateToInstantIso(g.start, 0) : "";
		var eIso = g.end ? localDateToInstantIso(g.end, 1) : "";
		ensureHiddenInput(form, "startDate", sIso);
		ensureHiddenInput(form, "endDate", eIso);
		ensureHiddenInput(form, "rangeMode", g.mode || "");
		ensureHiddenInput(form, "timeZone", g.timeZone || "");
	}

	function shouldApplyToForm(form: HTMLFormElement) {
		try {
			if (!form) return false;
			// Apply to forms that either opt-in via class or target stock/htmx endpoints
			if (form.classList.contains("global-date-range-form")) return true;
			var a = form.getAttribute("action") || "";
			if (a.indexOf("/stock/htmx") !== -1) return true;
			// also apply to forms that have hx-get or hx-post pointing to stock htmx
			var hx =
				form.getAttribute("hx-get") || form.getAttribute("hx-post") || "";
			if (hx.indexOf("/stock/htmx") !== -1) return true;
			return false;
		} catch (e) {
			return false;
		}
	}

	function syncAll() {
		try {
			var forms = Array.from(
				document.getElementsByTagName("form"),
			) as HTMLFormElement[];
			forms.forEach(function (f) {
				if (shouldApplyToForm(f)) applyToForm(f);
			});
		} catch (e) {}
	}

	// Update a specific form (closest ancestor) before HTMX request
	try {
		document.addEventListener("htmx:beforeRequest", function (evt: any) {
			try {
				var el = evt && evt.detail && evt.detail.elt ? evt.detail.elt : null;
				if (!el) return;
				// If the triggering element sits inside a form, update that form's hidden inputs
				var form =
					el.closest && el.closest("form")
						? (el.closest("form") as HTMLFormElement)
						: null;
				if (form && shouldApplyToForm(form)) {
					applyToForm(form);
					return;
				}
				// If this is an element with hx-get/hx-post pointing to /stock/htmx, set hx-vals so htmx includes params
				var hxget =
					el.getAttribute &&
					(el.getAttribute("hx-get") || el.getAttribute("hx-post"));
				if (hxget && hxget.indexOf("/stock/htmx") !== -1) {
					var g = parseGlobal();
					if (!g) return;
					var sIso = g.start ? localDateToInstantIso(g.start, 0) : "";
					var eIso = g.end ? localDateToInstantIso(g.end, 1) : "";
					var vals = {
						startDate: sIso,
						endDate: eIso,
						rangeMode: g.mode || "",
						timeZone: g.timeZone || "",
					};
					try {
						// merge with existing hx-vals if present
						var existing = el.getAttribute("hx-vals");
						if (existing) {
							try {
								var exObj = JSON.parse(existing);
								for (var k in exObj) {
									if (!(k in vals)) (vals as any)[k] = exObj[k];
								}
							} catch (e) {}
						}
						el.setAttribute("hx-vals", JSON.stringify(vals));
					} catch (e) {}
				}
			} catch (e) {}
		});
	} catch (e) {}

	// Intercept clicks on anchor links to stock pages and append global date params when missing
	function appendQueryParamsToLink(a: HTMLAnchorElement) {
		try {
			if (!a || !a.href) return;
			if (
				a.target === "_blank" ||
				a.hasAttribute("download") ||
				a.hasAttribute("data-no-global")
			)
				return;
			var loc = window.location;
			var url: URL;
			try {
				url = new URL(a.href, loc.origin);
			} catch (e) {
				return;
			}
			if (url.origin !== loc.origin) return;
			if (url.pathname.indexOf("/stock") === -1) return;
			var params = new URLSearchParams(url.search);
			if (
				params.has("startDate") ||
				params.has("endDate") ||
				params.has("rangeMode")
			)
				return;
			var g = parseGlobal();
			if (!g) return;
			if (g.start) params.set("startDate", localDateToInstantIso(g.start, 0));
			if (g.end) params.set("endDate", localDateToInstantIso(g.end, 1));
			if (g.mode) params.set("rangeMode", g.mode || "");
			if (g.timeZone) params.set("timeZone", g.timeZone || "");
			url.search = params.toString();
			a.href = url.toString();
		} catch (e) {}
	}

	try {
		document.addEventListener(
			"click",
			function (evt: MouseEvent) {
				try {
					if (!evt || evt.defaultPrevented) return;
					// ignore modifier clicks (open in new tab/window)
					if (evt.metaKey || evt.ctrlKey || evt.shiftKey || evt.altKey) return;
					if (evt.button && evt.button !== 0) return;
					var target = evt.target as Element | null;
					if (!target) return;
					var a =
						target.closest && (target.closest as any)("a")
							? ((target.closest as any)("a") as HTMLAnchorElement)
							: null;
					if (!a) return;
					appendQueryParamsToLink(a);
				} catch (e) {}
			},
			true,
		);
	} catch (e) {}

	// On full page load / PJAX / initial render
	try {
		document.addEventListener("DOMContentLoaded", function() {
			syncAll();
			// After syncing hidden inputs, submit forms that target /stock/htmx so fragments
			// load with the session-stored range. Use a short timeout to allow DOM to finish.
			setTimeout(function() {
				try {
					var forms = Array.from(document.getElementsByTagName('form')) as HTMLFormElement[];
					forms.forEach(function(f) {
						try {
							if (shouldApplyToForm(f)) {
								// if the form contains visible UI date inputs and the session has range,
								// prefer submitting to load fragment data consistently
								var g = parseGlobal();
								if (g && (g.start || g.end)) {
									try {
										if (typeof f.requestSubmit === 'function') f.requestSubmit();
										else f.submit();
									} catch(e) {}
								}
							}
						} catch(e) {}
					});
				} catch(e) {}
			}, 30);
		});
	} catch (e) {}

	// Also sync after HTMX swaps (when new fragments loaded)
	try {
		document.addEventListener("htmx:afterSwap", function () {
			setTimeout(syncAll, 20);
		});
	} catch (e) {}

	// When DateRangePicker updates session storage, it dispatches this event — react by syncing forms
	try {
		window.addEventListener("globalDateRange:changed", function () {
			setTimeout(syncAll, 10);
		});
	} catch (e) {}

	// expose for debugging
	try {
		(window as any).GlobalDateRangeSync = {
			syncAll: syncAll,
			applyToForm: applyToForm,
		};
	} catch (e) {}
})();
