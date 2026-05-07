"use strict";
console.debug("[multiSelectInit] simple multi-select initializer loaded");
function ensureMultiSelectStyle() {
    if (document.getElementById("simple-multi-style"))
        return;
    const s = document.createElement("style");
    s.id = "simple-multi-style";
    s.appendChild(document.createTextNode(`
/* Remove fixed DaisyUI sizing without overriding native multi-select row height */
.select[multiple], select.select[multiple], .form-control select[multiple] {
	min-height: unset !important;
	max-height: 50vh !important;
	overflow: auto !important;
}
/* Also handle cases where select has size-specific small class */
select.select.select-sm[multiple], .select.select-sm[multiple] {
	min-height: unset !important;
}
/* Make native multi-selects visually match an input box */
select[multiple], select.select[multiple], select.select-bordered[multiple] {
	background: #ffffff !important;
	border: 1px solid #d1d5db !important; /* gray-300 */
	border-radius: 0.375rem !important; /* rounded-md */
	padding: 0.25rem 0.4rem !important;
	box-shadow: none !important;
}
select[multiple]:focus, select.select[multiple]:focus, select.select-bordered[multiple]:focus {
	outline: none !important;
	border-color: #6366f1 !important; /* indigo-500 */
	box-shadow: 0 0 0 3px rgba(99,102,241,0.12) !important;
}
`));
    (document.head || document.documentElement).appendChild(s);
}
(() => {
    const SELECTOR = 'select[name="accountIdList"], select[name="stockItemIdList"], select.select';
    const HARD_CAP = 50; // safety cap to avoid extremely tall controls
    const ALL_OPTION_SELECTORS = 'select[name="accountIdList"], select[name="stockItemIdList"]';
    const selectionSnapshots = new WeakMap();
    function isAllOptionSelect(sel) {
        return sel.matches(ALL_OPTION_SELECTORS);
    }
    function getSelectedValues(sel) {
        return Array.from(sel.selectedOptions).map((option) => option.value);
    }
    function rememberSelectionSnapshot(sel) {
        selectionSnapshots.set(sel, getSelectedValues(sel));
    }
    function enforceExclusiveAllOption(sel) {
        var _a;
        if (!sel.multiple || !isAllOptionSelect(sel)) {
            rememberSelectionSnapshot(sel);
            return;
        }
        const allOption = Array.from(sel.options).find((option) => option.value === "");
        if (!allOption) {
            rememberSelectionSnapshot(sel);
            return;
        }
        const selectedOptions = Array.from(sel.selectedOptions);
        const selectedSpecificOptions = selectedOptions.filter((option) => option.value !== "");
        const hasAllSelected = selectedOptions.some((option) => option.value === "");
        if (hasAllSelected && selectedSpecificOptions.length > 0) {
            const previousSelection = (_a = selectionSnapshots.get(sel)) !== null && _a !== void 0 ? _a : [];
            const previouslyAllOnly = previousSelection.length === 1 && previousSelection[0] === "";
            if (previouslyAllOnly) {
                allOption.selected = false;
            }
            else {
                selectedSpecificOptions.forEach((option) => {
                    option.selected = false;
                });
            }
        }
        rememberSelectionSnapshot(sel);
    }
    function syncLinkedSelectHeights(scope) {
        const forms = Array.from((scope instanceof Element ? scope : document).querySelectorAll("form"));
        forms.forEach((form) => {
            const accountSelect = form.querySelector('select[name="accountIdList"]');
            const stockSelect = form.querySelector('select[name="stockItemIdList"]');
            if (!accountSelect || !stockSelect)
                return;
            const accountHeight = accountSelect.getBoundingClientRect().height;
            if (!accountHeight || accountHeight <= 0)
                return;
            stockSelect.style.height = `${accountHeight}px`;
            stockSelect.style.maxHeight = `${accountHeight}px`;
            stockSelect.style.overflowY = "auto";
        });
    }
    function applySize(sel) {
        try {
            // Skip selects that explicitly opt out
            if (sel.dataset.noMulti === "1" || sel.hasAttribute("data-no-multi"))
                return;
            sel.multiple = true;
            const opts = Array.from(sel.options);
            const attr = sel.getAttribute("data-max-visible");
            let maxVisible = null;
            if (attr) {
                const n = parseInt(attr, 10);
                if (!isNaN(n) && n > 0)
                    maxVisible = n;
            }
            const desired = opts.length;
            const size = Math.min(desired, maxVisible !== null && maxVisible !== void 0 ? maxVisible : desired, HARD_CAP);
            sel.size = Math.max(1, size);
            // mark as initialized
            sel.dataset.simpleMultiInit = "1";
        }
        catch (e) {
            console.warn("[multiSelectInit] applySize error", e);
        }
    }
    function init(root = document) {
        const scope = root instanceof Element ? root : document;
        ensureMultiSelectStyle();
        const sels = Array.from(scope.querySelectorAll(SELECTOR));
        sels.forEach((sel) => {
            try {
                applySize(sel);
                enforceExclusiveAllOption(sel);
                // observe option list changes
                if (!sel._simpleMultiObserver) {
                    const mo = new MutationObserver(() => applySize(sel));
                    mo.observe(sel, { childList: true, subtree: true });
                    sel._simpleMultiObserver = mo;
                }
            }
            catch (e) {
                /* ignore per-select errors */
            }
        });
        syncLinkedSelectHeights(scope);
    }
    document.addEventListener("DOMContentLoaded", () => init(document));
    document.addEventListener("change", (event) => {
        const target = event.target;
        if (!(target instanceof HTMLSelectElement))
            return;
        if (!isAllOptionSelect(target))
            return;
        enforceExclusiveAllOption(target);
    });
    document.addEventListener("htmx:afterSwap", (evt) => {
        try {
            const target = evt && evt.detail && evt.detail.target ? evt.detail.target : document;
            init(target instanceof Element ? target : document);
        }
        catch (e) {
            /* ignore */
        }
    });
    // also watch for newly inserted nodes
    try {
        const mo = new MutationObserver((mutations) => {
            for (const m of mutations) {
                for (const n of Array.from(m.addedNodes)) {
                    if (!(n instanceof Element))
                        continue;
                    try {
                        if (n.matches && n.matches(SELECTOR)) {
                            init(n);
                        }
                        else {
                            const found = n.querySelectorAll
                                ? n.querySelectorAll(SELECTOR)
                                : [];
                            if (found && found.length)
                                init(n);
                        }
                    }
                    catch (e) {
                        /* ignore per-node errors */
                    }
                }
            }
        });
        mo.observe(document.documentElement || document.body, {
            childList: true,
            subtree: true,
        });
    }
    catch (e) {
        /* ignore */
    }
})();
