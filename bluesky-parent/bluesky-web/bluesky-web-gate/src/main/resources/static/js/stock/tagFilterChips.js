(() => {
  if (window.__stockTagFilterChipsAttached)
    return;
  window.__stockTagFilterChipsAttached = true;
  const ROOT_SELECTOR = "[data-stock-tag-filter]";
  const CHIP_SELECTOR = "[data-stock-tag-chip]";
  const CLEAR_SELECTOR = "[data-stock-tag-clear]";
  const ACTIVE_TAG_CLASSES = ["btn-primary", "shadow-sm"];
  const INACTIVE_TAG_CLASSES = ["btn-outline", "border-base-300", "bg-base-100", "text-base-content/75"];
  const ACTIVE_CLEAR_CLASSES = ["btn-neutral", "shadow-sm"];
  const INACTIVE_CLEAR_CLASSES = ["btn-outline", "border-base-300", "bg-base-100", "text-base-content/70"];
  function getTagSelect(root) {
    return root.querySelector("[data-stock-tag-select]");
  }
  function getSelectedTagValues(select) {
    return new Set(Array.from(select.selectedOptions)
      .map((option) => option.value)
      .filter((value) => value));
  }
  function setButtonState(button, active, isClear) {
    const activeClasses = isClear ? ACTIVE_CLEAR_CLASSES : ACTIVE_TAG_CLASSES;
    const inactiveClasses = isClear ? INACTIVE_CLEAR_CLASSES : INACTIVE_TAG_CLASSES;
    activeClasses.forEach((className) => button.classList.toggle(className, active));
    inactiveClasses.forEach((className) => button.classList.toggle(className, !active));
    button.setAttribute("aria-pressed", active ? "true" : "false");
  }
  function syncButtons(root) {
    const select = getTagSelect(root);
    if (!select)
      return;
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
    if (!select)
      return;
    const option = Array.from(select.options).find((item) => item.value === tagValue);
    if (!option)
      return;
    option.selected = !option.selected;
    select.dispatchEvent(new Event("change", { bubbles: true }));
  }
  function clearTags(root) {
    const select = getTagSelect(root);
    if (!select)
      return;
    Array.from(select.options).forEach((option) => {
      option.selected = false;
    });
    select.dispatchEvent(new Event("change", { bubbles: true }));
  }
  function attach(root) {
    if (root.dataset.stockTagChipInit === "1") {
      syncButtons(root);
      return;
    }
    root.dataset.stockTagChipInit = "1";
    root.querySelectorAll(CHIP_SELECTOR).forEach((button) => {
      button.addEventListener("click", () => {
        toggleTag(root, button.dataset.tagValue || "");
      });
    });
    const clearButton = root.querySelector(CLEAR_SELECTOR);
    clearButton === null || clearButton === void 0 ? void 0 : clearButton.addEventListener("click", () => {
      clearTags(root);
    });
    const select = getTagSelect(root);
    select === null || select === void 0 ? void 0 : select.addEventListener("change", () => {
      syncButtons(root);
    });
    syncButtons(root);
  }
  function init(scope = document) {
    scope.querySelectorAll(ROOT_SELECTOR).forEach((root) => attach(root));
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => init(document));
  }
  else {
    init(document);
  }
  document.addEventListener("htmx:afterSwap", (event) => {
    var _a;
    const target = (_a = event.detail) === null || _a === void 0 ? void 0 : _a.target;
    init(target instanceof Element ? target : document);
  });
})();
export {};