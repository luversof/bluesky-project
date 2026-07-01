// 계좌/종목 멀티셀렉트(긴 네이티브 listbox)를 공간 절약형 '드롭다운 + 체크박스'로 점진 향상한다.
//  - 마크업은 그대로 두고(JS 전용), 원래 <select multiple> 은 폼 제출/태그연동/선택저장 호환을 위해 숨겨서 유지.
//  - 체크박스 ↔ select.option.selected 양방향 동기화(태그 자동선택은 select 'change' 로 들어옴).
//  - 평소엔 요약 버튼 1줄, 클릭 시 펼침. 종목은 검색창 제공.
// tailwind 가 ./src 를 스캔하므로 여기 클래스도 빌드에 포함된다(빌드 필요).
(function () {
	var NAMES = ["accountIdList", "stockItemIdList"];

	function lang(): string {
		try {
			return (document.documentElement.lang || "ko").toLowerCase();
		} catch (e) {
			return "ko";
		}
	}
	function searchPlaceholder(): string {
		return lang().indexOf("en") === 0 ? "Search" : "검색";
	}

	function enhance(select: HTMLSelectElement) {
		if (!select) return;
		if (select.dataset.msd === "1") {
			// 이미 변환됨: htmx 스왑/multiSelectInit 의 비동기 재처리로 네이티브 listbox 가
			// 다시 보일 수 있으므로 재숨김만 보장하고 빠진다(중복 래퍼 생성 방지).
			select.style.setProperty("display", "none", "important");
			return;
		}
		if (select.getAttribute("data-no-multi") === "1") return;
		select.dataset.msd = "1";

		var options = Array.prototype.slice.call(select.options) as HTMLOptionElement[];
		var realOpts: HTMLOptionElement[] = options.filter(function (o) {
			return !!o.value;
		});
		var emptyArr: HTMLOptionElement[] = options.filter(function (o) {
			return !o.value;
		});
		var emptyOpt: HTMLOptionElement | null = emptyArr.length ? emptyArr[0] : null;
		var allLabel = (emptyOpt && emptyOpt.text ? emptyOpt.text : "전체").trim();
		var isStock = select.name === "stockItemIdList";

		// 필드 라벨(계좌/종목 등)을 읽어 커스텀 드롭다운 토글의 접근명에 포함
		var fcEl = select.closest(".form-control");
		var fcLabelEl = fcEl
			? (fcEl.querySelector(".label-text") as HTMLElement | null)
			: null;
		var fieldLabel =
			fcLabelEl && fcLabelEl.textContent ? fcLabelEl.textContent.trim() : "";

		var wrap = document.createElement("div");
		wrap.className = "relative w-full";
		wrap.setAttribute("data-msd-wrap", "1");

		var toggle = document.createElement("button");
		toggle.type = "button";
		toggle.className =
			"btn btn-sm btn-outline btn-block justify-between font-normal gap-2";
		var summary = document.createElement("span");
		summary.className = "truncate flex-1 min-w-0 text-left";
		var caret = document.createElement("span");
		caret.className = "opacity-60 text-xs shrink-0";
		caret.textContent = "▾";
		toggle.appendChild(summary);
		toggle.appendChild(caret);

		var panel = document.createElement("div");
		panel.className =
			"absolute z-30 left-0 right-0 top-full mt-1 bg-base-100 border border-base-300 rounded-box shadow-lg p-2 max-h-64 overflow-auto";
		panel.setAttribute("data-msd-panel", "1");
		panel.hidden = true;

		var search: HTMLInputElement | null = null;
		if (isStock) {
			search = document.createElement("input");
			search.type = "text";
			search.className = "input input-bordered input-sm w-full mb-1";
			search.placeholder = searchPlaceholder();
			panel.appendChild(search);
		}

		var allItem = document.createElement("button");
		allItem.type = "button";
		allItem.className =
			"block w-full text-left text-sm px-2 py-1 rounded hover:bg-base-200 text-base-content/70";
		allItem.textContent = allLabel;
		panel.appendChild(allItem);

		var list = document.createElement("div");
		var cbByValue: Record<string, HTMLInputElement> = {};
		realOpts.forEach(function (opt) {
			var item = document.createElement("label");
			item.className =
				"flex items-center gap-2 px-2 py-1 rounded hover:bg-base-200 cursor-pointer text-sm";
			var cb = document.createElement("input");
			cb.type = "checkbox";
			cb.className = "checkbox checkbox-sm shrink-0";
			cb.value = opt.value;
			cb.checked = opt.selected;
			var txt = document.createElement("span");
			txt.className = "truncate";
			txt.textContent = opt.text;
			item.appendChild(cb);
			item.appendChild(txt);
			list.appendChild(item);
			cbByValue[opt.value] = cb;
			cb.addEventListener("change", function () {
				opt.selected = cb.checked;
				if (cb.checked && emptyOpt) emptyOpt.selected = false;
				select.dispatchEvent(new Event("change", { bubbles: true }));
				updateSummary();
			});
		});
		panel.appendChild(list);

		function updateSummary() {
			var sel = realOpts.filter(function (o) {
				return o.selected;
			});
			if (sel.length === 0) {
				summary.textContent = allLabel;
			} else {
				summary.textContent = sel
					.map(function (o) {
						return o.text;
					})
					.join(", ");
			}
			toggle.title = summary.textContent || "";
			toggle.setAttribute(
				"aria-label",
				(fieldLabel ? fieldLabel + ": " : "") + (summary.textContent || ""),
			);
		}

		function syncFromSelect() {
			realOpts.forEach(function (o) {
				var cb = cbByValue[o.value];
				if (cb && cb.checked !== o.selected) cb.checked = o.selected;
			});
			updateSummary();
		}

		allItem.addEventListener("click", function () {
			realOpts.forEach(function (o) {
				o.selected = false;
			});
			if (emptyOpt) emptyOpt.selected = true;
			Object.keys(cbByValue).forEach(function (v) {
				cbByValue[v].checked = false;
			});
			select.dispatchEvent(new Event("change", { bubbles: true }));
			updateSummary();
		});

		if (search) {
			search.addEventListener("input", function () {
				var q = (search as HTMLInputElement).value.toLowerCase();
				Array.prototype.slice.call(list.children).forEach(function (it: HTMLElement) {
					var t = (it.textContent || "").toLowerCase();
					it.style.display = t.indexOf(q) !== -1 ? "" : "none";
				});
			});
		}

		toggle.addEventListener("click", function (e) {
			e.stopPropagation();
			closeAllPanels(panel);
			panel.hidden = !panel.hidden;
			if (!panel.hidden && search) {
				search.value = "";
				search.dispatchEvent(new Event("input"));
				try {
					search.focus();
				} catch (err) {}
			}
		});

		// 태그 자동선택 등 외부에서 select 가 바뀌면 체크박스 재동기화
		select.addEventListener("change", function () {
			syncFromSelect();
		});
		// 태그 칩/해제 처리(applyAllStockSelection 등)는 change 를 안 쏠 수 있어, 외부에서 강제 재동기화용으로 노출.
		(select as any).__msdSync = syncFromSelect;

		var parent = select.parentNode;
		if (parent) {
			parent.insertBefore(wrap, select);
			// multiSelectInit 의 `select[multiple] { display:block !important }` 보다 우선하도록 인라인 !important 로 숨긴다.
			select.style.setProperty("display", "none", "important");
			wrap.appendChild(toggle);
			wrap.appendChild(panel);
			wrap.appendChild(select);
		}
		updateSummary();
	}

	// 태그 패널(stockSelectionPanel)을 동일한 드롭다운으로 감싼다. 칩/태그선택 로직은 그대로(tagFilterChips 위임).
	function enhanceTag(card: HTMLElement) {
		if (!card || card.dataset.msd === "1") return;
		card.dataset.msd = "1";

		var tagSelect = card.querySelector(
			"[data-stock-tag-select]",
		) as HTMLSelectElement | null;
		// 태그 select 는 칩이 UI 라 항상 숨김(어떤 규칙이 보이게 해도 인라인 !important 로 강제).
		if (tagSelect) tagSelect.style.setProperty("display", "none", "important");
		var clearBtn = card.querySelector("[data-stock-tag-clear]") as HTMLElement | null;
		var labelEl = card.querySelector("[data-stock-tag-label]") as HTMLElement | null;
		var allLabel = (
			clearBtn && clearBtn.textContent ? clearBtn.textContent : "전체"
		).trim();
		var tagLabelText = (
			labelEl && labelEl.textContent ? labelEl.textContent : "태그"
		).trim();

		var col = document.createElement("div");
		col.className = "form-control w-full";
		var lab = document.createElement("label");
		lab.className = "label p-1";
		var labSpan = document.createElement("span");
		labSpan.className = "label-text text-xs";
		labSpan.textContent = tagLabelText;
		lab.appendChild(labSpan);
		col.appendChild(lab);

		var wrap = document.createElement("div");
		wrap.className = "relative w-full";
		wrap.setAttribute("data-msd-wrap", "1");

		var toggle = document.createElement("button");
		toggle.type = "button";
		toggle.className =
			"btn btn-sm btn-outline btn-block justify-between font-normal gap-2";
		var summary = document.createElement("span");
		summary.className = "truncate flex-1 min-w-0 text-left";
		var caret = document.createElement("span");
		caret.className = "opacity-60 text-xs shrink-0";
		caret.textContent = "▾";
		toggle.appendChild(summary);
		toggle.appendChild(caret);

		var panel = document.createElement("div");
		panel.className =
			"absolute z-30 left-0 right-0 top-full mt-1 bg-base-100 border border-base-300 rounded-box shadow-lg p-2 max-h-72 overflow-auto";
		panel.setAttribute("data-msd-panel", "1");
		panel.hidden = true;

		// 카드 자체 라벨은 컬럼 라벨과 중복 → 숨기고, 카드 외곽 스타일은 평평하게.
		if (labelEl) labelEl.style.display = "none";
		card.classList.remove(
			"rounded-2xl",
			"border",
			"border-base-200",
			"bg-base-200/35",
			"px-3",
			"py-3",
		);

		function updateSummary() {
			var selected: string[] = [];
			if (tagSelect) {
				Array.prototype.slice
					.call(tagSelect.selectedOptions)
					.forEach(function (o: HTMLOptionElement) {
						if (o.value) selected.push(o.value);
					});
			}
			summary.textContent = selected.length === 0 ? allLabel : selected.join(", ");
			toggle.title = summary.textContent || "";
			toggle.setAttribute(
				"aria-label",
				(tagLabelText ? tagLabelText + ": " : "") + (summary.textContent || ""),
			);
		}
		if (tagSelect) {
			tagSelect.addEventListener("change", updateSummary);
			// 칩/해제 처리는 change 를 안 쏠 수 있어 외부 강제 재동기화용으로 노출.
			(tagSelect as any).__msdSync = updateSummary;
		}

		toggle.addEventListener("click", function (e) {
			e.stopPropagation();
			closeAllPanels(panel);
			panel.hidden = !panel.hidden;
		});

		var parent = card.parentNode;
		if (parent) {
			parent.insertBefore(col, card);
			col.appendChild(wrap);
			wrap.appendChild(toggle);
			wrap.appendChild(panel);
			panel.appendChild(card);
		}
		updateSummary();
	}

	function closeAllPanels(except: HTMLElement | null) {
		Array.prototype.slice
			.call(document.querySelectorAll("[data-msd-panel]"))
			.forEach(function (p: HTMLElement) {
				if (p !== except) p.hidden = true;
			});
	}

	// 바깥 클릭 시 닫기
	document.addEventListener("click", function (e) {
		var t = e.target as HTMLElement;
		if (!t || typeof t.closest !== "function" || !t.closest("[data-msd-wrap]")) {
			closeAllPanels(null);
		}
	});

	// 태그 칩/해제 클릭 시 종목 select 가 조용히 바뀔 수 있어, 직후 종목 체크박스를 강제 재동기화.
	document.addEventListener("click", function (e) {
		var t = e.target as HTMLElement;
		if (!t || typeof t.closest !== "function") return;
		if (!t.closest("[data-stock-tag-chip]") && !t.closest("[data-stock-tag-clear]"))
			return;
		var form = t.closest("form");
		setTimeout(function () {
			var scope = (form || document) as ParentNode;
			var stockSel = scope.querySelector(
				'select[name="stockItemIdList"]',
			) as any;
			if (stockSel && typeof stockSel.__msdSync === "function")
				stockSel.__msdSync();
			var tagSel = scope.querySelector('select[name="stockTagList"]') as any;
			if (tagSel && typeof tagSel.__msdSync === "function") tagSel.__msdSync();
		}, 0);
	});

	// 변환 완료된 네이티브 select 가 (multiSelectInit 의 display:block !important 등으로)
	// 다시 보이지 않도록 재확정한다. 스왑 후 비동기 재처리보다 늦게 한 번 더 실행한다.
	function reassertHidden() {
		["accountIdList", "stockItemIdList", "stockTagList"].forEach(function (name) {
			Array.prototype.slice
				.call(document.querySelectorAll('select[name="' + name + '"][data-msd="1"]'))
				.forEach(function (s: HTMLSelectElement) {
					s.style.setProperty("display", "none", "important");
				});
		});
	}

	function enhanceAll() {
		NAMES.forEach(function (name) {
			var sels = document.querySelectorAll(
				'select[multiple][name="' + name + '"]',
			);
			Array.prototype.slice.call(sels).forEach(function (s: HTMLSelectElement) {
				enhance(s);
			});
		});
		Array.prototype.slice
			.call(document.querySelectorAll("[data-stock-tag-filter]"))
			.forEach(function (c: HTMLElement) {
				enhanceTag(c);
			});
		reassertHidden();
		// 마이크로태스크(MutationObserver 등) 이후 한 번 더 보장
		setTimeout(reassertHidden, 0);
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", enhanceAll);
	} else {
		enhanceAll();
	}
	document.addEventListener("htmx:afterSwap", enhanceAll);
	document.addEventListener("htmx:afterSettle", enhanceAll);
})();
