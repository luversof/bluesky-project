console.debug("[multiSelectInit] simple multi-select initializer loaded");

function ensureMultiSelectStyle() {
	if (document.getElementById("simple-multi-style")) return;
	const s = document.createElement("style");
	s.id = "simple-multi-style";
	s.appendChild(
		document.createTextNode(`
/* Remove fixed DaisyUI sizing without overriding native multi-select row height */
.select[multiple], select.select[multiple], .form-control select[multiple] {
	min-height: unset !important;
	max-height: 50vh !important;
	overflow: auto !important;
	/* daisyUI v5 .select 는 display:inline-flex 라서 multiple 셀렉트의 option 들이
	   flex 아이템으로 1글자 폭까지 줄어 세로로 쌓인다. 네이티브 listbox(block)로 되돌린다. */
	display: block !important;
}
/* Also handle cases where select has size-specific small class */
select.select.select-sm[multiple], .select.select-sm[multiple] {
	min-height: unset !important;
}
/* Make native multi-selects visually match an input box (테마 변수 사용 → light/dark 모두 대응) */
select[multiple], select.select[multiple], select.select-bordered[multiple] {
	background-color: var(--color-base-100) !important;
	color: var(--color-base-content) !important;
	border: 1px solid var(--color-base-300) !important;
	border-radius: var(--radius-field, 0.375rem) !important;
	padding: 0.25rem 0.4rem !important;
	box-shadow: none !important;
}
select[multiple] option {
	background-color: var(--color-base-100);
	color: var(--color-base-content);
}
select[multiple]:focus, select.select[multiple]:focus, select.select-bordered[multiple]:focus {
	border-color: var(--color-primary) !important;
	outline: 2px solid var(--color-primary) !important;
	outline-offset: 0 !important;
	box-shadow: none !important;
}
`),
	);
	(document.head || document.documentElement).appendChild(s);
}

(() => {
	const SELECTOR =
		'select[multiple], select[data-max-visible], select[name="accountIdList"], select[name="stockItemIdList"], select[name="stockTagList"]';
	const HARD_CAP = 50; // safety cap to avoid extremely tall controls
	const ALL_OPTION_SELECTORS =
		'select[name="accountIdList"], select[name="stockItemIdList"]';
	const selectionSnapshots = new WeakMap<HTMLSelectElement, string[]>();

	function isAllOptionSelect(sel: HTMLSelectElement): boolean {
		return sel.matches(ALL_OPTION_SELECTORS);
	}

	function getSelectedValues(sel: HTMLSelectElement): string[] {
		return Array.from(sel.selectedOptions).map((option) => option.value);
	}

	function rememberSelectionSnapshot(sel: HTMLSelectElement): void {
		selectionSnapshots.set(sel, getSelectedValues(sel));
	}

	function enforceExclusiveAllOption(sel: HTMLSelectElement): void {
		if (!sel.multiple || !isAllOptionSelect(sel)) {
			rememberSelectionSnapshot(sel);
			return;
		}

		const allOption = Array.from(sel.options).find(
			(option) => option.value === "",
		);
		if (!allOption) {
			rememberSelectionSnapshot(sel);
			return;
		}

		const selectedOptions = Array.from(sel.selectedOptions);
		const selectedSpecificOptions = selectedOptions.filter(
			(option) => option.value !== "",
		);
		const hasAllSelected = selectedOptions.some(
			(option) => option.value === "",
		);

		if (hasAllSelected && selectedSpecificOptions.length > 0) {
			const previousSelection = selectionSnapshots.get(sel) ?? [];
			const previouslyAllOnly =
				previousSelection.length === 1 && previousSelection[0] === "";

			if (previouslyAllOnly) {
				allOption.selected = false;
			} else {
				selectedSpecificOptions.forEach((option) => {
					option.selected = false;
				});
			}
		}

		rememberSelectionSnapshot(sel);
	}

	// 실측 2026-09-10(qa/cpu-profile-cols.cjs, 4배 CPU): 예전엔 여기서 getBoundingClientRect 를 읽어 htmx 목록 교체 직후
	// MutationObserver 안에서 강제 동기 레이아웃이 났다(매매 493ms·배당 371ms 자체 시간, 가장 긴 작업의 대부분).
	// ResizeObserver 는 레이아웃이 끝난 뒤 크기를 알려주므로 읽기 비용이 없고, 옵션 수가 바뀌어 높이가 변해도 따라간다.
	function linkSelectHeights(accountSelect: HTMLSelectElement, stockSelect: HTMLSelectElement, height: number): void {
		if (!height || height <= 0) return;
		const px = `${height}px`;
		if (stockSelect.style.height === px) return;
		stockSelect.style.height = px;
		stockSelect.style.maxHeight = px;
		stockSelect.style.overflowY = "auto";
	}

	function syncLinkedSelectHeights(scope: ParentNode | Document): number {
		const forms = Array.from(
			(scope instanceof Element ? scope : document).querySelectorAll("form"),
		) as HTMLFormElement[];
		let linked = 0;
		forms.forEach((form) => {
			const accountSelect = form.querySelector<HTMLSelectElement>(
				'select[name="accountIdList"]',
			);
			const stockSelect = form.querySelector<HTMLSelectElement>(
				'select[name="stockItemIdList"]',
			);
			if (!accountSelect || !stockSelect) return;
			linked++;
			const RO = (globalThis as any).ResizeObserver;
			if (typeof RO !== "function") {
				// 폴백(구형 브라우저): 예전처럼 즉시 읽는다.
				linkSelectHeights(accountSelect, stockSelect, accountSelect.getBoundingClientRect().height);
				return;
			}
			if ((stockSelect as any)._linkedHeightObserver) return;
			const ro = new RO((entries: any[]) => {
				for (const entry of entries) {
					const box = entry.borderBoxSize && entry.borderBoxSize[0];
					const height = box ? box.blockSize : entry.contentRect ? entry.contentRect.height : 0;
					linkSelectHeights(accountSelect, stockSelect, height);
				}
			});
			ro.observe(accountSelect, { box: "border-box" });
			(stockSelect as any)._linkedHeightObserver = ro;
		});
		return linked;
	}
	(globalThis as any).__multiSelectInitInternals = { syncLinkedSelectHeights, linkSelectHeights };
	function applySize(sel: HTMLSelectElement) {
		try {
			// Skip selects that explicitly opt out
			if (sel.dataset.noMulti === "1" || sel.hasAttribute("data-no-multi"))
				return;
			sel.multiple = true;
			const opts = Array.from(sel.options);
			const attr = sel.getAttribute("data-max-visible");
			let maxVisible: number | null = null;
			if (attr) {
				const n = parseInt(attr, 10);
				if (!isNaN(n) && n > 0) maxVisible = n;
			}
			const desired = opts.length;
			const size = Math.min(desired, maxVisible ?? desired, HARD_CAP);
			sel.size = Math.max(1, size);
			// mark as initialized
			sel.dataset.simpleMultiInit = "1";
		} catch (e) {
			console.warn("[multiSelectInit] applySize error", e);
		}
	}

	function init(root: ParentNode | Document = document) {
		const scope = root instanceof Element ? root : document;
		ensureMultiSelectStyle();
		const sels = Array.from(
			scope.querySelectorAll(SELECTOR),
		) as HTMLSelectElement[];
		sels.forEach((sel) => {
			try {
				applySize(sel);
				enforceExclusiveAllOption(sel);
				// observe option list changes
				if (!(sel as any)._simpleMultiObserver) {
					const mo = new MutationObserver(() => applySize(sel));
					mo.observe(sel, { childList: true, subtree: true });
					(sel as any)._simpleMultiObserver = mo;
				}
			} catch (e) {
				/* ignore per-select errors */
			}
		});
		syncLinkedSelectHeights(scope);
	}

	document.addEventListener("DOMContentLoaded", () => init(document));

	document.addEventListener("change", (event: Event) => {
		const target = event.target;
		if (!(target instanceof HTMLSelectElement)) return;
		if (!isAllOptionSelect(target)) return;

		enforceExclusiveAllOption(target);
	});

	document.addEventListener("htmx:afterSwap", (evt: any) => {
		try {
			const target =
				evt && evt.detail && evt.detail.target ? evt.detail.target : document;
			init(target instanceof Element ? target : document);
		} catch (e) {
			/* ignore */
		}
	});

	// also watch for newly inserted nodes
	try {
		const mo = new MutationObserver((mutations) => {
			for (const m of mutations) {
				for (const n of Array.from(m.addedNodes)) {
					if (!(n instanceof Element)) continue;
					try {
						if ((n as Element).matches && (n as Element).matches(SELECTOR)) {
							init(n as Element);
						} else {
							const found = (n as Element).querySelectorAll
								? (n as Element).querySelectorAll(SELECTOR)
								: [];
							if (found && found.length) init(n as Element);
						}
					} catch (e) {
						/* ignore per-node errors */
					}
				}
			}
		});
		mo.observe(document.documentElement || document.body, {
			childList: true,
			subtree: true,
		});
	} catch (e) {
		/* ignore */
	}
})();
