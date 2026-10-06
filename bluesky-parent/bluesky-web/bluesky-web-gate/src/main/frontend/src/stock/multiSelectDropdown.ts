// 계좌/종목 멀티셀렉트(긴 네이티브 listbox)를 공간 절약형 '드롭다운 + 체크박스'로 점진 향상한다.
//  - 마크업은 그대로 두고(JS 전용), 원래 <select multiple> 은 폼 제출/태그연동/선택저장 호환을 위해 숨겨서 유지.
//  - 체크박스 ↔ select.option.selected 양방향 동기화(태그 자동선택은 select 'change' 로 들어옴).
//  - 평소엔 요약 버튼 1줄, 클릭 시 펼침. 종목은 검색창 제공.
// tailwind 가 ./src 를 스캔하므로 여기 클래스도 빌드에 포함된다(빌드 필요).
(function () {
	// 패널 id 일련번호(aria-controls 용). 조각 교체로 여러 번 초기화돼도 겹치지 않게 모듈 수명 동안 증가만 한다.
	var msdPanelSeq = 0;
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

	// 검색 상자의 이름·알림 문구. 순수 계산이라 모듈 범위에 두고 테스트에 노출한다.
	function msdSearchLabel(fieldLabel: string, langCode: string): string {
		var en = (langCode || "ko").toLowerCase().indexOf("en") === 0;
		var base = en ? "Search" : "검색";
		var field = (fieldLabel || "").trim();
		if (!field) return base;
		return en ? base + " " + field : field + " " + base;
	}
	// 검색어가 있을 때만 말한다 - 패널을 여는 순간의 초기화까지 읽어 주면 시끄럽다.
	function msdSearchStatusText(query: string, matchCount: number, langCode: string): string {
		if (!query) return "";
		var en = (langCode || "ko").toLowerCase().indexOf("en") === 0;
		if (matchCount <= 0) return en ? "No matching items" : "검색 결과 없음";
		return en ? matchCount + " items" : "검색 결과 " + matchCount + "개";
	}
	function msdEmptyText(langCode: string): string {
		var en = (langCode || "ko").toLowerCase().indexOf("en") === 0;
		return en ? "No matching items" : "검색 결과 없음";
	}
	// "전체" 옆의 "전체 취소" 단추 글자(사용자 요청 2026-10-02).
	function msdClearAllText(langCode: string): string {
		var en = (langCode || "ko").toLowerCase().indexOf("en") === 0;
		return en ? "Clear all" : "전체 취소";
	}
	// 토글 단추의 요약. 하나도 안 골랐거나 전부 골랐으면 "전체" - 전부 고른 것을 이름 수십 개로 늘어놓으면 읽을 수 없다.
	function msdSummaryText(selectedTexts: string[], totalCount: number, allLabel: string): string {
		if (!selectedTexts.length || (totalCount > 0 && selectedTexts.length >= totalCount)) return allLabel;
		return selectedTexts.join(", ");
	}
	try {
		(globalThis as any).__msdInternals = {
			msdSearchLabel: msdSearchLabel,
			msdSearchStatusText: msdSearchStatusText,
			msdEmptyText: msdEmptyText,
			msdClearAllText: msdClearAllText,
			msdSummaryText: msdSummaryText,
		};
	} catch (e) {}

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
		toggle.setAttribute("data-msd-toggle", "1");
		toggle.setAttribute("aria-haspopup", "true");
		toggle.setAttribute("aria-expanded", "false");
		var summary = document.createElement("span");
		summary.className = "truncate flex-1 min-w-0 text-left";
		var caret = document.createElement("span");
		caret.className = "opacity-60 text-xs shrink-0";
		caret.textContent = "▾";
		toggle.appendChild(summary);
		toggle.appendChild(caret);

		var panel = document.createElement("div");
		panel.className =
			"absolute z-30 left-0 top-full mt-1 min-w-full w-max max-w-[calc(100vw-2rem)] sm:max-w-sm bg-base-100 border border-base-300 rounded-box shadow-lg p-2 max-h-64 overflow-auto focus:outline-none";
		panel.setAttribute("data-msd-panel", "1");
		// 토글-패널 연결(aria-controls). 실측 2026-09-09: haspopup/expanded/Escape 는 갖췄는데 이것만 빠져 있었다.
		panel.id = "msd-panel-" + (++msdPanelSeq);
		toggle.setAttribute("aria-controls", panel.id);
		panel.hidden = true;
		// 패널 안 아무 곳(항목 이름 글자)을 눌러도 포커스가 패널에 머물게 한다. 실측 2026-10-02: 이름을 누르면 마우스를 누르는 순간 포커스가
		// 패널 밖 <main tabindex="-1"> 로 넘어가 아래 focusin 이 패널을 닫았고, 클릭이 체크박스에 닿지 못했다(체크박스만 눌러야 됐다).
		panel.tabIndex = -1;

		var search: HTMLInputElement | null = null;
		var searchStatus: HTMLElement | null = null;
		var searchEmpty: HTMLElement | null = null;
		if (isStock) {
			search = document.createElement("input");
			search.type = "text";
			search.className = "input input-bordered input-sm w-full mb-1";
			search.placeholder = searchPlaceholder();
			// placeholder 는 값을 치면 사라진다 - 이름은 따로 달아 둔다(실측 2026-09-12: 이 상자만 이름이 없었다).
			search.setAttribute("aria-label", msdSearchLabel(fieldLabel, lang()));
			panel.appendChild(search);
			// 걸러낸 결과 수는 눈으로만 보였다(43 → 2 → 0 으로 줄어도 알림 없음).
			searchStatus = document.createElement("p");
			searchStatus.className = "sr-only";
			searchStatus.setAttribute("role", "status");
			searchStatus.setAttribute("aria-live", "polite");
			panel.appendChild(searchStatus);
			// 0 개일 때 패널에 "전체" 버튼만 남아 빈 상자처럼 보였다.
			searchEmpty = document.createElement("p");
			searchEmpty.className = "px-2 py-1 text-sm text-base-content/70";
			searchEmpty.textContent = msdEmptyText(lang());
			searchEmpty.hidden = true;
			// 같은 문구를 위의 알림 영역이 이미 읽어 준다 - 둘 다 노출하면 "검색 결과 없음" 이 두 번 들린다
			// (실측 2026-09-12: 패널 낭독이 "검색 결과 없음 검색 결과 없음 전체"). 이건 눈으로 보는 몫이다.
			searchEmpty.setAttribute("aria-hidden", "true");
			panel.appendChild(searchEmpty);
		}

		// "전체" = 전부 체크, "전체 취소" = 전부 해제(사용자 요청 2026-10-02). 예전 "전체" 는 해제만 해서 눌러도 아무 일이 없는 것처럼 보였다
		// (아무것도 안 고른 것이 곧 전체라 화면이 그대로였다).
		var allRow = document.createElement("div");
		allRow.className = "flex gap-1 mb-1";
		var itemButtonClass =
			"flex-1 text-left text-sm px-2 py-1 rounded hover:bg-base-200 text-base-content/70";
		var allItem = document.createElement("button");
		allItem.type = "button";
		allItem.className = itemButtonClass;
		allItem.textContent = allLabel;
		allItem.setAttribute("data-msd-select-all", "1");
		var clearItem = document.createElement("button");
		clearItem.type = "button";
		clearItem.className = itemButtonClass;
		clearItem.textContent = msdClearAllText(lang());
		clearItem.setAttribute("data-msd-clear-all", "1");
		allRow.appendChild(allItem);
		allRow.appendChild(clearItem);
		panel.appendChild(allRow);

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
			summary.textContent = msdSummaryText(
				sel.map(function (o) {
					return o.text;
				}),
				realOpts.length,
				allLabel,
			);
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

		// 전부 체크. 검색으로 걸러 둔 상태면 보이는 항목만 - 걸러 낸 뒤 "전체" 를 누르는 뜻은 "이것들 전부" 다.
		allItem.addEventListener("click", function () {
			realOpts.forEach(function (o) {
				var cb = cbByValue[o.value];
				var row = cb ? (cb.closest("label") as HTMLElement | null) : null;
				if (row && row.style.display === "none") return;
				o.selected = true;
				if (cb) cb.checked = true;
			});
			if (emptyOpt) emptyOpt.selected = false;
			select.dispatchEvent(new Event("change", { bubbles: true }));
			updateSummary();
		});
		// 전부 해제 - 아무것도 안 고른 상태(= 거르지 않음)로 돌아간다.
		clearItem.addEventListener("click", function () {
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
				var raw = (search as HTMLInputElement).value;
				var q = raw.toLowerCase();
				var matched = 0;
				Array.prototype.slice.call(list.children).forEach(function (it: HTMLElement) {
					var t = (it.textContent || "").toLowerCase();
					var hit = t.indexOf(q) !== -1;
					it.style.display = hit ? "" : "none";
					if (hit) matched++;
				});
				if (searchEmpty) searchEmpty.hidden = !raw || matched > 0;
				if (searchStatus) {
					var next = msdSearchStatusText(raw, matched, lang());
					if (searchStatus.textContent !== next) searchStatus.textContent = next;
				}
			});
		}

		toggle.addEventListener("click", function (e) {
			e.stopPropagation();
			closeAllPanels(panel);
			setPanelOpen(panel, panel.hidden);
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
		toggle.setAttribute("data-msd-toggle", "1");
		toggle.setAttribute("aria-haspopup", "true");
		toggle.setAttribute("aria-expanded", "false");
		var summary = document.createElement("span");
		summary.className = "truncate flex-1 min-w-0 text-left";
		var caret = document.createElement("span");
		caret.className = "opacity-60 text-xs shrink-0";
		caret.textContent = "▾";
		toggle.appendChild(summary);
		toggle.appendChild(caret);

		var panel = document.createElement("div");
		panel.className =
			"absolute z-30 left-0 top-full mt-1 min-w-full w-max max-w-[calc(100vw-2rem)] sm:max-w-sm bg-base-100 border border-base-300 rounded-box shadow-lg p-2 max-h-72 overflow-auto focus:outline-none";
		panel.setAttribute("data-msd-panel", "1");
		// 토글-패널 연결(aria-controls). 실측 2026-09-09: haspopup/expanded/Escape 는 갖췄는데 이것만 빠져 있었다.
		panel.id = "msd-panel-" + (++msdPanelSeq);
		toggle.setAttribute("aria-controls", panel.id);
		panel.hidden = true;
		// 계좌 · 종목 패널과 같은 이유 - 패널 안을 눌러도 포커스가 밖(main)으로 나가 닫히지 않게.
		panel.tabIndex = -1;

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
			setPanelOpen(panel, panel.hidden);
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

	// 패널의 열림/닫힘과 토글 버튼의 aria-expanded 를 한 곳에서 맞춘다.
	// 실측 2026-09-09: 토글에 펼침 상태가 없어 스크린리더는 눌러도 무슨 일이 났는지 알 수 없었고, Escape 로 닫을 수도 없었다.
	function setPanelOpen(panel: HTMLElement, open: boolean) {
		panel.hidden = !open;
		// 패널은 칸보다 넓어질 수 있다(min-w-full w-max - 실측 2026-10-02: 1280px 에서 종목 칸이 145px 라 이름이 "KODEX 200..." 로
		// 잘리고 "전체 취소" 가 두 줄이 됐다). 넓어진 패널이 화면 오른쪽을 넘으면 칸의 오른쪽 끝에 맞춰 왼쪽으로 펼친다.
		// 오른쪽 끝에 그냥 맞추면 칸이 화면 왼쪽 끝에서 시작하지 않을 때 이번엔 왼쪽으로 넘친다(S41 실측: 자산 성장 375px 태그 패널 left -21px).
		// 넘친 만큼만 왼쪽으로 옮기되 화면 왼쪽 여백(8px)을 넘지 않는다.
		if (open) {
			panel.style.left = "";
			panel.style.right = "";
			var rect = panel.getBoundingClientRect();
			var viewport = document.documentElement.clientWidth || window.innerWidth;
			if (rect.right > viewport - 8) {
				var wrapLeft = panel.parentElement ? panel.parentElement.getBoundingClientRect().left : rect.left;
				var shift = Math.min(rect.right - (viewport - 8), Math.max(0, wrapLeft - 8));
				panel.style.left = -shift + "px";
			}
		}
		var wrap = panel.parentElement;
		var toggle = wrap ? (wrap.querySelector("[data-msd-toggle]") as HTMLElement | null) : null;
		if (toggle) toggle.setAttribute("aria-expanded", open ? "true" : "false");
	}

	function closeAllPanels(except: HTMLElement | null) {
		Array.prototype.slice
			.call(document.querySelectorAll("[data-msd-panel]"))
			.forEach(function (p: HTMLElement) {
				if (p !== except && !p.hidden) setPanelOpen(p, false);
			});
	}

	// Escape: 열린 패널을 닫고 포커스를 토글로 되돌린다(WAI-ARIA 팝업 관례). 패널 밖으로 Tab 이 나가면 닫는다.
	document.addEventListener("keydown", function (e) {
		if (e.key !== "Escape") return;
		var t = e.target as HTMLElement;
		var wrap = t && typeof t.closest === "function" ? (t.closest("[data-msd-wrap]") as HTMLElement | null) : null;
		if (!wrap) return;
		var panel = wrap.querySelector("[data-msd-panel]") as HTMLElement | null;
		if (!panel || panel.hidden) return;
		e.preventDefault();
		setPanelOpen(panel, false);
		var toggle = wrap.querySelector("[data-msd-toggle]") as HTMLElement | null;
		if (toggle) toggle.focus();
	});
	document.addEventListener("focusin", function (e) {
		var t = e.target as HTMLElement;
		var inside = t && typeof t.closest === "function" ? t.closest("[data-msd-wrap]") : null;
		Array.prototype.slice
			.call(document.querySelectorAll("[data-msd-panel]"))
			.forEach(function (p: HTMLElement) {
				if (!p.hidden && p.parentElement !== inside) setPanelOpen(p, false);
			});
	});

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
