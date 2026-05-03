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
    var _a;
    return (
      ((_a = root.closest("form")) === null || _a === void 0
        ? void 0
        : _a.querySelector(STOCK_SELECT_SELECTOR)) || null
    );
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
    Array.from(stockSelect.options).forEach((option) => {
      if (!option.value) return;
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
  function syncStockSelection(root) {
    const tagSelect = getTagSelect(root);
    const stockSelect = getStockSelect(root);
    if (!tagSelect || !stockSelect) return;
    const selectedValues = getSelectedTagValues(tagSelect);
    let changed = false;
    Array.from(stockSelect.options).forEach((option) => {
      if (!option.value) return;
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
      stockSelect.dispatchEvent(new Event("change", { bubbles: true }));
    }
  }
  function rememberManualStockSelection(root) {
    const stockSelect = getStockSelect(root);
    if (!stockSelect) return;
    Array.from(stockSelect.options).forEach((option) => {
      if (!option.value || option.dataset.tagAutoSelected === "1") return;
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
    const select = getTagSelect(root);
    if (!select) return;
    Array.from(select.options).forEach((option) => {
      option.selected = false;
    });
    select.dispatchEvent(new Event("change", { bubbles: true }));
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
    const actionButton = chipButton || clearButton;
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
      var _a;
      const root =
        (_a = target.closest("form")) === null || _a === void 0
          ? void 0
          : _a.querySelector(ROOT_SELECTOR);
      if (!root) return;
      rememberManualStockSelection(root);
    }
  });
})();
export {};
