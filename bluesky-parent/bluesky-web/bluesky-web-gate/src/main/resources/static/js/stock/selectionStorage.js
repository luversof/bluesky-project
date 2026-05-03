(() => {
  if (window.__stockSelectionStorageAttached) return;
  window.__stockSelectionStorageAttached = true;
  const GLOBAL_KEY = "__stockSelection:global";
  const DEBUG = window.__stockSelectionStorageDebug === true;
  const injectedForms = new WeakSet();
  let lastInjectionTimestamp = 0;
  const INJECTION_WINDOW_MS = 3000;
  function getAppConfigValue(name, fallback = "") {
    try {
      const appConfig = document.getElementById("app-config");
      return (
        (appConfig && appConfig.dataset && appConfig.dataset[name]) || fallback
      );
    } catch (e) {
      return fallback;
    }
  }
  function getFormForElement(el) {
    if (!(el instanceof Element)) return null;
    return el.closest("form");
  }
  function saveFromForm(form) {
    try {
      if (!form) return;
      const acc = form.querySelector('select[name="accountIdList"]');
      const stk = form.querySelector('select[name="stockItemIdList"]');
      const out = { accountIdList: [], stockItemIdList: [] };
      if (acc) {
        if (acc.multiple) {
          for (const o of Array.from(acc.selectedOptions)) {
            if (o && o.value)
              out.accountIdList.push({ id: o.value, text: o.text });
          }
        } else if (acc.value) {
          const txt =
            (acc.options[acc.selectedIndex] &&
              acc.options[acc.selectedIndex].text) ||
            acc.value;
          out.accountIdList.push({ id: acc.value, text: txt });
        }
      }
      if (stk) {
        if (stk.multiple) {
          for (const o of Array.from(stk.selectedOptions)) {
            if (o && o.value)
              out.stockItemIdList.push({ id: o.value, text: o.text });
          }
        } else if (stk.value) {
          const txt =
            (stk.options[stk.selectedIndex] &&
              stk.options[stk.selectedIndex].text) ||
            stk.value;
          out.stockItemIdList.push({ id: stk.value, text: txt });
        }
      }
      try {
        sessionStorage.setItem(GLOBAL_KEY, JSON.stringify(out));
      } catch (e) {
        /* ignore */
      }
    } catch (e) {
      /* ignore */
    }
  }
  function restoreToForm(form) {
    try {
      if (!form) return;
      const raw = sessionStorage.getItem(GLOBAL_KEY);
      if (!raw) return;
      const obj = JSON.parse(raw);
      const acc = form.querySelector('select[name="accountIdList"]');
      const stk = form.querySelector('select[name="stockItemIdList"]');
      let restored = false;
      if (DEBUG)
        console.debug(
          "[selectionStorage] restoreToForm start",
          form && form.id,
          obj,
        );
      if (
        acc &&
        obj &&
        Array.isArray(obj.accountIdList) &&
        obj.accountIdList.length > 0
      ) {
        if (acc.multiple) {
          const current = new Set(
            Array.from(acc.selectedOptions).map((o) => o.value),
          );
          for (const sel of obj.accountIdList) {
            if (!current.has(sel.id)) {
              restored = true;
              break;
            }
          }
          Array.from(acc.options).forEach((o) => (o.selected = false));
          for (const sel of obj.accountIdList) {
            let opt = Array.from(acc.options).find((o) => o.value === sel.id);
            if (!opt) {
              opt = document.createElement("option");
              opt.value = sel.id;
              opt.text = sel.text || sel.id;
              acc.insertBefore(opt, acc.firstChild);
              restored = true;
            }
            opt.selected = true;
          }
        } else {
          const sel = obj.accountIdList[0];
          if (acc.value !== sel.id) restored = true;
          let opt = Array.from(acc.options).find((o) => o.value === sel.id);
          if (!opt) {
            opt = document.createElement("option");
            opt.value = sel.id;
            opt.text = sel.text || sel.id;
            acc.insertBefore(opt, acc.firstChild);
            restored = true;
          }
          acc.value = sel.id;
        }
        acc.dispatchEvent(new Event("change", { bubbles: true }));
        if (DEBUG)
          console.debug(
            "[selectionStorage] restored account",
            form && form.id,
            restored,
          );
      }
      if (
        stk &&
        obj &&
        Array.isArray(obj.stockItemIdList) &&
        obj.stockItemIdList.length > 0
      ) {
        if (stk.multiple) {
          const current = new Set(
            Array.from(stk.selectedOptions).map((o) => o.value),
          );
          for (const sel of obj.stockItemIdList) {
            if (!current.has(sel.id)) {
              restored = true;
              break;
            }
          }
          Array.from(stk.options).forEach((o) => (o.selected = false));
          for (const sel of obj.stockItemIdList) {
            let opt = Array.from(stk.options).find((o) => o.value === sel.id);
            if (!opt) {
              opt = document.createElement("option");
              opt.value = sel.id;
              opt.text = sel.text || sel.id;
              stk.insertBefore(opt, stk.firstChild);
              restored = true;
            }
            opt.selected = true;
          }
        } else {
          const sel = obj.stockItemIdList[0];
          if (stk.value !== sel.id) restored = true;
          let opt = Array.from(stk.options).find((o) => o.value === sel.id);
          if (!opt) {
            opt = document.createElement("option");
            opt.value = sel.id;
            opt.text = sel.text || sel.id;
            stk.insertBefore(opt, stk.firstChild);
            restored = true;
          }
          stk.value = sel.id;
        }
        stk.dispatchEvent(new Event("change", { bubbles: true }));
        if (DEBUG)
          console.debug(
            "[selectionStorage] restored stock",
            form && form.id,
            restored,
          );
      }
      // 자동 제출: 복원으로 인해 값이 변경되었고 폼에 HTMX 속성이 있으면 submit 트리거
      try {
        if (
          restored &&
          form &&
          (form.hasAttribute("hx-get") ||
            form.hasAttribute("hx-post") ||
            form.hasAttribute("hx-put") ||
            form.hasAttribute("hx-delete"))
        ) {
          // Avoid double-submit: if we recently injected selection into an HTMX request for this form
          // skip scheduling another automatic submit. We use a form-based WeakSet when possible and
          // fall back to a short timestamp window.
          let skipAuto = false;
          try {
            if (injectedForms.has(form)) {
              injectedForms.delete(form);
              skipAuto = true;
            } else if (
              lastInjectionTimestamp &&
              Date.now() - lastInjectionTimestamp < INJECTION_WINDOW_MS
            ) {
              lastInjectionTimestamp = 0;
              skipAuto = true;
            }
          } catch (e) {
            /* ignore */
          }
          if (!skipAuto) {
            if (DEBUG)
              console.debug(
                "[selectionStorage] auto-submit scheduled",
                form && form.id,
              );
            setTimeout(() => {
              try {
                if (typeof form.requestSubmit === "function") {
                  form.requestSubmit();
                } else {
                  form.dispatchEvent(
                    new Event("submit", { bubbles: true, cancelable: true }),
                  );
                }
              } catch (e) {
                /* ignore */
              }
            }, 20);
          } else {
            if (DEBUG)
              console.debug(
                "[selectionStorage] skipped auto-submit (injection detected)",
                form && form.id,
              );
          }
        }
      } catch (e) {
        /* ignore */
      }
      if (DEBUG)
        console.debug(
          "[selectionStorage] restoreToForm end",
          form && form.id,
          restored,
        );
    } catch (e) {
      /* ignore */
    }
  }
  function restoreForAllForms() {
    try {
      const forms = document.querySelectorAll("form");
      forms.forEach((f) => {
        if (
          f.querySelector('select[name="accountIdList"]') ||
          f.querySelector('select[name="stockItemIdList"]')
        ) {
          restoreToForm(f);
        }
      });
    } catch (e) {
      /* ignore */
    }
  }
  function migratePerFormKeys() {
    try {
      const prefix = "__stockSelection:";
      const accMap = new Map();
      const stkMap = new Map();
      try {
        const curRaw = sessionStorage.getItem(GLOBAL_KEY);
        if (curRaw) {
          const cur = JSON.parse(curRaw);
          if (cur && Array.isArray(cur.accountIdList))
            for (const s of cur.accountIdList)
              accMap.set(String(s.id), s.text || s.id);
          if (cur && Array.isArray(cur.stockItemIdList))
            for (const s of cur.stockItemIdList)
              stkMap.set(String(s.id), s.text || s.id);
        }
      } catch (e) {
        /* ignore */
      }
      const keysToRemove = [];
      for (let i = 0; i < sessionStorage.length; i++) {
        const key = sessionStorage.key(i);
        if (!key) continue;
        if (key.startsWith(prefix) && key !== GLOBAL_KEY) {
          try {
            const raw = sessionStorage.getItem(key);
            if (!raw) {
              keysToRemove.push(key);
              continue;
            }
            const obj = JSON.parse(raw);
            if (obj && Array.isArray(obj.accountIdList)) {
              for (const s of obj.accountIdList)
                if (s && s.id) accMap.set(String(s.id), s.text || s.id);
            }
            if (obj && Array.isArray(obj.stockItemIdList)) {
              for (const s of obj.stockItemIdList)
                if (s && s.id) stkMap.set(String(s.id), s.text || s.id);
            }
            keysToRemove.push(key);
          } catch (e) {
            /* ignore malformed */
          }
        }
      }
      if (accMap.size > 0 || stkMap.size > 0) {
        const out = { accountIdList: [], stockItemIdList: [] };
        for (const [id, text] of accMap) out.accountIdList.push({ id, text });
        for (const [id, text] of stkMap) out.stockItemIdList.push({ id, text });
        try {
          sessionStorage.setItem(GLOBAL_KEY, JSON.stringify(out));
        } catch (e) {
          /* ignore */
        }
      }
      for (const k of keysToRemove) {
        try {
          sessionStorage.removeItem(k);
        } catch (e) {
          /* ignore */
        }
      }
    } catch (e) {
      /* ignore */
    }
  }
  function initRestore() {
    try {
      migratePerFormKeys();
      restoreForAllForms();
    } catch (e) {
      /* ignore */
    }
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initRestore);
  } else {
    initRestore();
  }
  document.addEventListener("htmx:afterSwap", function () {
    try {
      restoreForAllForms();
    } catch (e) {
      /* ignore */
    }
  });
  // HTMX: before sending a request, inject saved selection as parameters
  document.addEventListener(
    "htmx:configRequest",
    function (e) {
      try {
        const ev = e;
        let params =
          ev.detail && ev.detail.parameters ? ev.detail.parameters : null;
        const triggerElt = ev.detail && ev.detail.elt ? ev.detail.elt : null;
        // If there is no parameters object, create one so programmatic HTMX calls
        // (htmx.ajax) can be sanitized and can receive injected params when appropriate.
        if (!params) {
          params = {};
          if (ev.detail) ev.detail.parameters = params;
        }
        // If the trigger element explicitly requests a reset (data-selection-reset="true")
        // or its visible text matches the localized reset label, treat this as a reset action.
        // clear stored selection and do not inject saved params.
        try {
          if (triggerElt instanceof Element) {
            const t = (triggerElt.textContent || "").trim();
            const attrReset =
              triggerElt.getAttribute &&
              triggerElt.getAttribute("data-selection-reset");
            const resetLabel = getAppConfigValue("commonButtonReset", "Reset");
            if (attrReset === "true" || t === resetLabel) {
              try {
                sessionStorage.removeItem(GLOBAL_KEY);
              } catch (err) {
                /* ignore */
              }
              // Remove any injected or form-collected selection params so the reset request
              // is sent without account/stock filters.
              try {
                if (params) {
                  delete params.accountIdList;
                  delete params.stockItemIdList;
                }
              } catch (err) {
                /* ignore */
              }
              // Also clear the select elements in the form so the UI immediately reflects reset.
              try {
                const formEl =
                  triggerElt.closest("form") || document.querySelector("form");
                if (formEl) {
                  const accSel = formEl.querySelector(
                    'select[name="accountIdList"]',
                  );
                  const stkSel = formEl.querySelector(
                    'select[name="stockItemIdList"]',
                  );
                  if (accSel) {
                    if (accSel.multiple) {
                      Array.from(accSel.options).forEach(
                        (o) => (o.selected = false),
                      );
                    } else {
                      try {
                        accSel.value = "";
                      } catch (e) {
                        /* ignore */
                      }
                    }
                    accSel.dispatchEvent(
                      new Event("change", { bubbles: true }),
                    );
                  }
                  if (stkSel) {
                    if (stkSel.multiple) {
                      Array.from(stkSel.options).forEach(
                        (o) => (o.selected = false),
                      );
                    } else {
                      try {
                        stkSel.value = "";
                      } catch (e) {
                        /* ignore */
                      }
                    }
                    stkSel.dispatchEvent(
                      new Event("change", { bubbles: true }),
                    );
                  }
                }
              } catch (err) {
                /* ignore */
              }
              if (DEBUG)
                console.debug(
                  "[selectionStorage] reset trigger detected - cleared stored selection and removed params",
                  { text: t, attrReset },
                );
              return;
            }
          }
        } catch (err) {
          /* ignore */
        }
        // If this request targets an endpoint that should NOT receive account/stock filters
        // (e.g. asset-growth or trade-history), skip injection entirely. We must handle both
        // declarative HX attributes (hx-get on elements) and programmatic calls which set
        // the request `path` directly (ev.detail.path). For programmatic calls, also strip
        // any existing `accountIdList`/`stockItemIdList` query params from the path.
        try {
          const excluded = ["asset-growth", "trade-history"];
          // 1) If the path (set by htmx or provided programmatically) mentions an excluded
          // endpoint, remove account/stock query params from the path and skip injection.
          try {
            const path =
              ev.detail && ev.detail.path ? String(ev.detail.path) : null;
            if (path) {
              for (const kw of excluded) {
                if (path.indexOf(kw) !== -1) {
                  try {
                    const url = new URL(path, location.href);
                    let changed = false;
                    if (url.searchParams.has("accountIdList")) {
                      url.searchParams.delete("accountIdList");
                      changed = true;
                    }
                    if (url.searchParams.has("stockItemIdList")) {
                      url.searchParams.delete("stockItemIdList");
                      changed = true;
                    }
                    if (changed) {
                      const newPath =
                        url.pathname +
                        (url.search ? url.search : "") +
                        (url.hash ? url.hash : "");
                      ev.detail.path = newPath;
                      if (params) {
                        try {
                          delete params.accountIdList;
                        } catch (e) {
                          /* ignore */
                        }
                        try {
                          delete params.stockItemIdList;
                        } catch (e) {
                          /* ignore */
                        }
                      }
                      if (DEBUG)
                        console.debug(
                          "[selectionStorage] sanitized path for excluded endpoint",
                          { oldPath: path, newPath, kw },
                        );
                    }
                  } catch (e) {
                    /* ignore URL parse errors */
                  }
                  return;
                }
              }
            }
          } catch (e) {
            /* ignore */
          }
          // 2) If the triggering element has an HX attribute targeting an excluded endpoint,
          // skip injection.
          if (triggerElt instanceof Element) {
            const hxAncestor =
              triggerElt.closest &&
              (triggerElt.closest("[hx-get]") ||
                triggerElt.closest("[hx-post]") ||
                triggerElt.closest("[hx-put]") ||
                triggerElt.closest("[hx-delete]"));
            const hxAttr = hxAncestor
              ? hxAncestor.getAttribute("hx-get") ||
                hxAncestor.getAttribute("hx-post") ||
                hxAncestor.getAttribute("hx-put") ||
                hxAncestor.getAttribute("hx-delete") ||
                ""
              : triggerElt.getAttribute
                ? triggerElt.getAttribute("hx-get") || ""
                : "";
            if (hxAttr) {
              for (const kw of excluded) {
                if (hxAttr.indexOf(kw) !== -1) {
                  if (DEBUG)
                    console.debug(
                      "[selectionStorage] skipping injection for excluded endpoint",
                      { hxAttr, kw },
                    );
                  return;
                }
              }
            }
          }
        } catch (err) {
          /* ignore */
        }
        const raw = sessionStorage.getItem(GLOBAL_KEY);
        if (!raw) return;
        const obj = JSON.parse(raw);
        let injected = false;
        if (
          obj &&
          Array.isArray(obj.accountIdList) &&
          obj.accountIdList.length > 0 &&
          !params.accountIdList
        ) {
          params.accountIdList = obj.accountIdList.map((s) => s.id);
          injected = true;
        }
        if (
          obj &&
          Array.isArray(obj.stockItemIdList) &&
          obj.stockItemIdList.length > 0 &&
          !params.stockItemIdList
        ) {
          params.stockItemIdList = obj.stockItemIdList.map((s) => s.id);
          injected = true;
        }
        if (injected) {
          // try to associate injection with a form if possible, otherwise fall back to timestamp window
          const form =
            triggerElt instanceof Element ? triggerElt.closest("form") : null;
          if (form) injectedForms.add(form);
          lastInjectionTimestamp = Date.now();
          if (DEBUG)
            console.debug(
              "[selectionStorage] injected params into htmx request",
              { formId: form && form.id, params },
            );
        }
      } catch (e) {
        /* ignore */
      }
    },
    true,
  );
  // Guard: avoid replacing UI with a full page response. Allow innerHTML fragments
  // (which often do not include the container id) while aborting swaps when the
  // server returned a full HTML document or an empty response.
  document.addEventListener("htmx:beforeSwap", function (e) {
    try {
      const ev = e;
      const detail = ev.detail;
      if (!detail) return;
      const target = detail.target || null;
      const serverResponse = detail.serverResponse || null;
      if (!target || !serverResponse) return;
      try {
        const wrapper = document.createElement("div");
        wrapper.innerHTML = serverResponse;
        // If response explicitly contains the target id (outerHTML case), allow swap.
        if (target.id && wrapper.querySelector("#" + target.id)) {
          return;
        }
        // If the response looks like a full HTML document, abort to avoid replacing
        // the fragment with a whole page.
        if (
          wrapper.querySelector("html") ||
          wrapper.querySelector("head") ||
          wrapper.querySelector("body")
        ) {
          if (DEBUG)
            console.debug(
              "[selectionStorage] beforeSwap: server returned full document, aborting swap",
              target.id,
            );
          detail.shouldSwap = false;
          return;
        }
        // If there's no element content or only whitespace, abort the swap.
        const hasElements = wrapper.querySelector("*") !== null;
        if (!hasElements || (wrapper.textContent || "").trim().length === 0) {
          if (DEBUG)
            console.debug(
              "[selectionStorage] beforeSwap: empty server response, aborting swap",
              target.id,
            );
          detail.shouldSwap = false;
          return;
        }
        // Otherwise it's likely a fragment intended for innerHTML replacement — allow.
        if (DEBUG)
          console.debug(
            "[selectionStorage] beforeSwap: fragment response without target id — allowing inner swap",
            target.id,
          );
      } catch (err) {
        /* ignore parsing errors */
      }
    } catch (err) {
      /* ignore */
    }
  });
  // Previously we saved selection on every select `change` event. User requested
  // saving only when the user intentionally runs the search action — therefore
  // we no longer persist on change. Persistence happens on submit below when
  // the submitter indicates an explicit save intent.
  document.addEventListener(
    "submit",
    function (e) {
      try {
        const form = e.target;
        if (!form) return;
        // Decide whether this submit should persist selection. We persist only when
        // the submitter (the button that triggered the submit) explicitly indicates
        // save intent. This can be either a `data-selection-save="true"` attribute
        // (recommended) or the visible localized search label (legacy). Programmatic submits
        // without a submitter will NOT persist unless we detect a primary htmx button.
        const submitter = e.submitter;
        let shouldSave = false;
        if (submitter instanceof Element) {
          try {
            const attrSave =
              submitter.getAttribute &&
              submitter.getAttribute("data-selection-save");
            const txt = (submitter.textContent || "").trim();
            const searchLabel = getAppConfigValue(
              "commonButtonSearch",
              "Search",
            );
            if (attrSave === "true" || txt === searchLabel) shouldSave = true;
          } catch (err) {
            /* ignore */
          }
        }
        // If the form uses HTMX for submission, ensure the submit does not fall
        // back to a full page navigation. When the user pressed Enter (no submitter)
        // or a programmatic submit happened, forward the submit to the primary
        // `data-selection-save` button (which we configure to use hx-* attributes)
        // so HTMX performs the request and the page does not reload.
        try {
          // Consider the form to be HTMX-driven if the form itself has hx-* attributes
          // or if any descendant element (button/link) declares hx-* attributes.
          const hxTriggerElt = form.querySelector(
            "[hx-get], [hx-post], [hx-put], [hx-delete]",
          );
          const hasHx =
            !!hxTriggerElt ||
            form.hasAttribute("hx-get") ||
            form.hasAttribute("hx-post") ||
            form.hasAttribute("hx-put") ||
            form.hasAttribute("hx-delete");
          if (hasHx) {
            // Prefer an explicit data-selection-save button, otherwise use the first
            // hx-triggering element inside the form, or fall back to a submit button.
            const primary =
              form.querySelector('[data-selection-save="true"]') ||
              hxTriggerElt ||
              form.querySelector('button[type="submit"]');
            if (primary && submitter !== primary) {
              // Persist before forwarding when appropriate
              if (
                shouldSave &&
                (form.querySelector('select[name="accountIdList"]') ||
                  form.querySelector('select[name="stockItemIdList"]'))
              ) {
                saveFromForm(form);
              }
              e.preventDefault();
              try {
                primary.click();
              } catch (err) {
                /* ignore */
              }
              return;
            }
          }
        } catch (err) {
          /* ignore forwarding errors */
        }
        if (
          shouldSave &&
          (form.querySelector('select[name="accountIdList"]') ||
            form.querySelector('select[name="stockItemIdList"]'))
        ) {
          saveFromForm(form);
        }
      } catch (e) {
        /* ignore */
      }
    },
    true,
  );
  window.stockSelectionStorage = {
    saveFromForm,
    restoreToForm,
    GLOBAL_KEY,
  };
})();
export {};
