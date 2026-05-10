(() => {
  if (window.__stockTagFilterChipsAttached) return;
  window.__stockTagFilterChipsAttached = true;
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
  function getTagSelect(root) {
    return root.querySelector(TAG_SELECT_SELECTOR);
  }
  function getStockSelect(root) {
    var _a, _b;
    return (_b =
      (_a = root.closest("form")) === null || _a === void 0
        ? void 0
        : _a.querySelector(STOCK_SELECT_SELECTOR)) !== null && _b !== void 0
      ? _b
      : null;
  }
  function getStockOptions(stockSelect) {
    return Array.from(stockSelect.options).filter((option) => !!option.value);
  }
  function getAllStockOption(stockSelect) {
    var _a;
    return (_a = Array.from(stockSelect.options).find(
      (option) => !option.value,
    )) !== null && _a !== void 0
      ? _a
      : null;
  }
  function getSelectedTagValues(select) {
    return new Set(
      Array.from(select.selectedOptions)
        .map((option) => option.value)
        .filter((value) => value),
    );
  }
  function setButtonState(button, active, isClear) {
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
  function optionMatchesSelectedTags(option, selectedValues) {
    if (!selectedValues.size) return false;
    return (option.dataset.stockTags || "")
      .split("|")
      .map((value) => value.trim())
      .filter((value) => value)
      .some((tag) => selectedValues.has(tag));
  }
  function primeStockSelectionState(root) {
    const tagSelect = getTagSelect(root);
    const stockSelect = getStockSelect(root);
    if (!stockSelect) return;
    const selectedValues = tagSelect
      ? getSelectedTagValues(tagSelect)
      : new Set();
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
  function applyAllStockSelection(root) {
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
  function syncStockSelection(root) {
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
  function rememberManualStockSelection(root) {
    var _a;
    const stockSelect = getStockSelect(root);
    if (!stockSelect) return;
    if (
      (_a = getAllStockOption(stockSelect)) === null || _a === void 0
        ? void 0
        : _a.selected
    ) {
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
  function syncButtons(root) {
    const select = getTagSelect(root);
    if (!select) return;
    const selectedValues = getSelectedTagValues(select);
    root.querySelectorAll(CHIP_SELECTOR).forEach((button) => {
      const tagValue = button.dataset.tagValue || "";
      setButtonState(button, selectedValues.has(tagValue), false);
    });
    const clearButton = root.querySelector(CLEAR_SELECTOR);
    if (clearButton) {
      setButtonState(clearButton, selectedValues.size === 0, true);
    }
  }
  function toggleTag(root, tagValue) {
    const select = getTagSelect(root);
    if (!select) return;
    const option = Array.from(select.options).find(
      (item) => item.value === tagValue,
    );
    if (!option) return;
    option.selected = !option.selected;
    select.dispatchEvent(new Event("change", { bubbles: true }));
  }
  function clearTags(root) {
    applyAllStockSelection(root);
  }
  function findRoots(scope = document) {
    const roots = [];
    if (scope instanceof HTMLElement && scope.matches(ROOT_SELECTOR)) {
      roots.push(scope);
    }
    if ("querySelectorAll" in scope) {
      roots.push(...Array.from(scope.querySelectorAll(ROOT_SELECTOR)));
    }
    return roots;
  }
  function init(scope = document) {
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
  document.addEventListener("htmx:afterSwap", (event) => {
    var _a;
    const target =
      (_a = event.detail) === null || _a === void 0 ? void 0 : _a.target;
    init(target instanceof Element ? target : document);
  });
  document.addEventListener("click", (event) => {
    const target = event.target;
    if (!(target instanceof Element)) return;
    const chipButton = target.closest(CHIP_SELECTOR);
    const clearButton = target.closest(CLEAR_SELECTOR);
    const actionButton =
      chipButton !== null && chipButton !== void 0 ? chipButton : clearButton;
    if (!actionButton) return;
    const root = actionButton.closest(ROOT_SELECTOR);
    if (!root) return;
    event.preventDefault();
    if (chipButton) {
      toggleTag(root, chipButton.dataset.tagValue || "");
    } else {
      clearTags(root);
    }
  });
  document.addEventListener("change", (event) => {
    var _a, _b;
    const target = event.target;
    if (!(target instanceof Element)) return;
    if (target.matches(TAG_SELECT_SELECTOR)) {
      const root = target.closest(ROOT_SELECTOR);
      if (!root) return;
      syncButtons(root);
      syncStockSelection(root);
      return;
    }
    if (target.matches(STOCK_SELECT_SELECTOR)) {
      const stockSelect = target;
      const root =
        (_a = stockSelect.closest("form")) === null || _a === void 0
          ? void 0
          : _a.querySelector(ROOT_SELECTOR);
      if (!root) return;
      if (stockSelect.dataset.tagSyncing === "1") {
        delete stockSelect.dataset.tagSyncing;
        rememberManualStockSelection(root);
        return;
      }
      if (
        (_b = getAllStockOption(stockSelect)) === null || _b === void 0
          ? void 0
          : _b.selected
      ) {
        applyAllStockSelection(root);
        return;
      }
      rememberManualStockSelection(root);
    }
  });
})();
export {};
