export {};

interface Window {
	__stockTagFilterChipsAttached?: boolean;
}

(() => {
	if ((window as Window).__stockTagFilterChipsAttached) return;
	(window as Window).__stockTagFilterChipsAttached = true;

	const ROOT_SELECTOR = "[data-stock-tag-filter]";
	const CHIP_SELECTOR = "[data-stock-tag-chip]";
	const CLEAR_SELECTOR = "[data-stock-tag-clear]";
	const TAG_SELECT_SELECTOR = "[data-stock-tag-select]";
	const STOCK_SELECT_SELECTOR = 'select[name="stockItemIdList"]';
	const ACTIVE_TAG_CLASSES = ["btn-primary", "shadow-sm"];
	const INACTIVE_TAG_CLASSES = [
		"btn-outline",
		"border-base-300",
		"bg-base-100",
		"text-base-content/75",
	];
	const ACTIVE_CLEAR_CLASSES = ["btn-neutral", "shadow-sm"];
	const INACTIVE_CLEAR_CLASSES = [
		"btn-outline",
		"border-base-300",
		"bg-base-100",
		"text-base-content/70",
	];

	function getTagSelect(root: ParentNode): HTMLSelectElement | null {
		return root.querySelector(TAG_SELECT_SELECTOR);
	}

	function getStockSelect(root: HTMLElement): HTMLSelectElement | null {
		return root.closest("form")?.querySelector(STOCK_SELECT_SELECTOR) ?? null;
	}

	function getStockOptions(stockSelect: HTMLSelectElement): HTMLOptionElement[] {
		return Array.from(stockSelect.options).filter((option) => !!option.value);
	}

	function getAllStockOption(
		stockSelect: HTMLSelectElement,
	): HTMLOptionElement | null {
		return Array.from(stockSelect.options).find((option) => !option.value) ?? null;
	}

	function getSelectedTagValues(select: HTMLSelectElement): Set<string> {
		return new Set(
			Array.from(select.selectedOptions)
				.map((option) => option.value)
				.filter((value) => value),
		);
	}

	function setButtonState(
		button: HTMLElement,
		active: boolean,
		isClear: boolean,
	): void {
		const activeClasses = isClear ? ACTIVE_CLEAR_CLASSES : ACTIVE_TAG_CLASSES;
		const inactiveClasses = isClear
			? INACTIVE_CLEAR_CLASSES
			: INACTIVE_TAG_CLASSES;
		activeClasses.forEach((className) =>
			button.classList.toggle(className, active),
		);
		inactiveClasses.forEach((className) =>
			button.classList.toggle(className, !active),
		);
		button.setAttribute("aria-pressed", active ? "true" : "false");
	}

	function optionMatchesSelectedTags(
		option: HTMLOptionElement,
		selectedValues: Set<string>,
	): boolean {
		if (!selectedValues.size) return false;

		return (option.dataset.stockTags || "")
			.split("|")
			.map((value) => value.trim())
			.filter((value) => value)
			.some((tag) => selectedValues.has(tag));
	}

	function primeStockSelectionState(root: HTMLElement): void {
		const tagSelect = getTagSelect(root);
		const stockSelect = getStockSelect(root);
		if (!stockSelect) return;

		const selectedValues = tagSelect
			? getSelectedTagValues(tagSelect)
			: new Set<string>();

		getStockOptions(stockSelect).forEach((option) => {

			const autoSelected =
				option.selected && optionMatchesSelectedTags(option, selectedValues);
			if (autoSelected) {
				option.dataset.tagAutoSelected = "1";
				delete option.dataset.tagManualSelected;
				return;
			}

			delete option.dataset.tagAutoSelected;
			if (option.selected) {
				option.dataset.tagManualSelected = "1";
			} else {
				delete option.dataset.tagManualSelected;
			}
		});
	}

	function applyAllStockSelection(root: HTMLElement): void {
		const tagSelect = getTagSelect(root);
		const stockSelect = getStockSelect(root);
		if (!stockSelect) return;

		if (tagSelect) {
			Array.from(tagSelect.options).forEach((option) => {
				option.selected = false;
			});
		}

		getStockOptions(stockSelect).forEach((option) => {
			option.selected = false;
			delete option.dataset.tagAutoSelected;
			delete option.dataset.tagManualSelected;
		});

		const allOption = getAllStockOption(stockSelect);
		if (allOption) {
			allOption.selected = true;
		}

		syncButtons(root);
		syncStockSelection(root);
	}

	function syncStockSelection(root: HTMLElement): void {
		const tagSelect = getTagSelect(root);
		const stockSelect = getStockSelect(root);
		if (!tagSelect || !stockSelect) return;

		const selectedValues = getSelectedTagValues(tagSelect);
		const stockOptions = getStockOptions(stockSelect);
		const allOption = getAllStockOption(stockSelect);
		let changed = false;

		if (!selectedValues.size) {
			const hasManualSelection = stockOptions.some(
				(option) => option.dataset.tagManualSelected === "1",
			);

			stockOptions.forEach((option) => {
				delete option.dataset.tagAutoSelected;
				const shouldSelect = hasManualSelection
					? option.dataset.tagManualSelected === "1"
					: false;

				if (option.selected !== shouldSelect) {
					option.selected = shouldSelect;
					changed = true;
				}
			});

			if (allOption && allOption.selected !== !hasManualSelection) {
				allOption.selected = !hasManualSelection;
				changed = true;
			}

			if (changed) {
				stockSelect.dataset.tagSyncing = "1";
				stockSelect.dispatchEvent(new Event("change", { bubbles: true }));
			}
			return;
		}

		if (allOption && allOption.selected) {
			allOption.selected = false;
			changed = true;
		}

		stockOptions.forEach((option) => {

			const manualSelected = option.dataset.tagManualSelected === "1";
			const autoSelected = optionMatchesSelectedTags(option, selectedValues);
			const shouldSelect = manualSelected || autoSelected;

			if (option.selected !== shouldSelect) {
				option.selected = shouldSelect;
				changed = true;
			}

			if (autoSelected && !manualSelected) {
				option.dataset.tagAutoSelected = "1";
			} else {
				delete option.dataset.tagAutoSelected;
			}
		});

		if (changed) {
			stockSelect.dataset.tagSyncing = "1";
			stockSelect.dispatchEvent(new Event("change", { bubbles: true }));
		}
	}

	function rememberManualStockSelection(root: HTMLElement): void {
		const stockSelect = getStockSelect(root);
		if (!stockSelect) return;

		if (getAllStockOption(stockSelect)?.selected) {
			getStockOptions(stockSelect).forEach((option) => {
				option.selected = false;
				delete option.dataset.tagAutoSelected;
				delete option.dataset.tagManualSelected;
			});
			return;
		}

		getStockOptions(stockSelect).forEach((option) => {
			if (option.dataset.tagAutoSelected === "1") return;

			if (option.selected) {
				option.dataset.tagManualSelected = "1";
			} else {
				delete option.dataset.tagManualSelected;
			}
		});
	}

	function syncButtons(root: HTMLElement): void {
		const select = getTagSelect(root);
		if (!select) return;

		const selectedValues = getSelectedTagValues(select);
		root.querySelectorAll<HTMLElement>(CHIP_SELECTOR).forEach((button) => {
			const tagValue = button.dataset.tagValue || "";
			setButtonState(button, selectedValues.has(tagValue), false);
		});

		const clearButton = root.querySelector<HTMLElement>(CLEAR_SELECTOR);
		if (clearButton) {
			setButtonState(clearButton, selectedValues.size === 0, true);
		}
	}

	function toggleTag(root: HTMLElement, tagValue: string): void {
		const select = getTagSelect(root);
		if (!select) return;

		const option = Array.from(select.options).find(
			(item) => item.value === tagValue,
		);
		if (!option) return;

		option.selected = !option.selected;
		select.dispatchEvent(new Event("change", { bubbles: true }));
	}

	function clearTags(root: HTMLElement): void {
		applyAllStockSelection(root);
	}

	function findRoots(scope: ParentNode = document): HTMLElement[] {
		const roots: HTMLElement[] = [];
		if (scope instanceof HTMLElement && scope.matches(ROOT_SELECTOR)) {
			roots.push(scope);
		}
		if ("querySelectorAll" in scope) {
			roots.push(
				...Array.from(scope.querySelectorAll<HTMLElement>(ROOT_SELECTOR)),
			);
		}
		return roots;
	}

	function init(scope: ParentNode = document): void {
		findRoots(scope).forEach((root) => {
			primeStockSelectionState(root);
			syncButtons(root);
			syncStockSelection(root);
		});
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", () => init(document));
	} else {
		init(document);
	}

	document.addEventListener("htmx:afterSwap", (event: Event) => {
		const target = (event as CustomEvent<{ target?: Element }>).detail?.target;
		init(target instanceof Element ? target : document);
	});

	document.addEventListener("click", (event: Event) => {
		const target = event.target;
		if (!(target instanceof Element)) return;

		const chipButton = target.closest<HTMLElement>(CHIP_SELECTOR);
		const clearButton = target.closest<HTMLElement>(CLEAR_SELECTOR);
		const actionButton = chipButton ?? clearButton;
		if (!actionButton) return;

		const root = actionButton.closest<HTMLElement>(ROOT_SELECTOR);
		if (!root) return;

		event.preventDefault();
		if (chipButton) {
			toggleTag(root, chipButton.dataset.tagValue || "");
		} else {
			clearTags(root);
		}
	});

	document.addEventListener("change", (event: Event) => {
		const target = event.target;
		if (!(target instanceof Element)) return;

		if (target.matches(TAG_SELECT_SELECTOR)) {
			const root = target.closest<HTMLElement>(ROOT_SELECTOR);
			if (!root) return;

			syncButtons(root);
			syncStockSelection(root);
			return;
		}

		if (target.matches(STOCK_SELECT_SELECTOR)) {
			const stockSelect = target as HTMLSelectElement;
			const root = stockSelect
				.closest("form")
				?.querySelector<HTMLElement>(ROOT_SELECTOR);
			if (!root) return;

			if (stockSelect.dataset.tagSyncing === "1") {
				delete stockSelect.dataset.tagSyncing;
				rememberManualStockSelection(root);
				return;
			}

			if (getAllStockOption(stockSelect)?.selected) {
				applyAllStockSelection(root);
				return;
			}

			rememberManualStockSelection(root);
		}
	});
})();
