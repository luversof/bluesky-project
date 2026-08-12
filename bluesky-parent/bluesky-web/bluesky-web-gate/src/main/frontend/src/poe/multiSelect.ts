// PoE sim 폼의 <select multiple data-poe-multi> 를 '검색 + 태그(칩)' 형태의 토큰 입력으로 점진 향상한다.
//  - 원래 <select multiple> 은 폼 제출 호환을 위해 숨겨서 유지하고, 칩 ↔ option.selected 를 동기화.
//  - 입력창에 타이핑하면 매칭 옵션 드롭다운이 뜨고, 클릭하면 칩으로 추가된다. 칩의 × 로 제거.
//  - 항목이 많아(젬 600+, 유니크 1000+) 검색 필수. tailwind 가 ./src 를 스캔하므로 여기 클래스도 빌드에 포함.
(function () {
	function lang(): string {
		try {
			return (document.documentElement.lang || "ko").toLowerCase();
		} catch (e) {
			return "ko";
		}
	}
	function t(ko: string, en: string): string {
		return lang().indexOf("en") === 0 ? en : ko;
	}

	function enhance(select: HTMLSelectElement) {
		if (!select) return;
		if (select.dataset.poeMsd === "1") {
			select.style.setProperty("display", "none", "important");
			return;
		}
		select.dataset.poeMsd = "1";

		var options = Array.prototype.slice.call(select.options) as HTMLOptionElement[];
		var realOpts: HTMLOptionElement[] = options.filter(function (o) {
			return !!o.value;
		});
		var placeholder = select.getAttribute("data-placeholder") || t("검색…", "Search…");

		var wrap = document.createElement("div");
		// 너비는 담는 쪽이 정한다 — 예전엔 sm:w-96 을 박아둬 폭이 유연한 칸(시뮬 폼 한 줄 배치)에서
		// 컨테이너를 384px 로 밀고 나가 가로 스크롤이 생겼다(768px 실측).
		wrap.className = "relative w-full";
		wrap.setAttribute("data-poe-msd-wrap", "1");

		// 칩 + 검색 입력을 담는 가짜 인풋(클릭하면 검색 포커스)
		var control = document.createElement("div");
		control.className =
			"flex flex-wrap items-center gap-1 input input-bordered input-sm h-auto min-h-8 py-1 cursor-text";

		var search = document.createElement("input");
		search.type = "text";
		search.className =
			"flex-1 min-w-24 bg-transparent border-0 outline-none text-sm p-0 focus:outline-none";
		search.placeholder = placeholder;

		var panel = document.createElement("div");
		panel.className =
			"absolute z-30 left-0 right-0 top-full mt-1 bg-base-100 border border-base-300 rounded-box shadow-lg p-1 max-h-72 overflow-auto";
		panel.setAttribute("data-poe-msd-panel", "1");
		panel.hidden = true;

		// 옵션별 드롭다운 항목
		var rowByValue: Record<string, HTMLElement> = {};
		realOpts.forEach(function (opt) {
			var row = document.createElement("button");
			row.type = "button";
			row.className =
				"block w-full text-left text-sm px-2 py-1 rounded hover:bg-base-200 truncate";
			row.textContent = opt.text;
			row.addEventListener("click", function (e) {
				e.preventDefault();
				e.stopPropagation();
				setSelected(opt, true);
				search.value = "";
				filter();
				try {
					search.focus();
				} catch (err) {}
			});
			panel.appendChild(row);
			rowByValue[opt.value] = row;
		});

		function renderChips() {
			// 기존 칩 제거(검색 입력은 유지)
			Array.prototype.slice
				.call(control.querySelectorAll("[data-poe-chip]"))
				.forEach(function (c: HTMLElement) {
					c.remove();
				});
			var selected = realOpts.filter(function (o) {
				return o.selected;
			});
			selected.forEach(function (opt) {
				var chip = document.createElement("span");
				chip.setAttribute("data-poe-chip", "1");
				chip.className =
					"badge badge-sm badge-primary gap-1 max-w-full whitespace-nowrap";
				var label = document.createElement("span");
				label.className = "truncate";
				label.textContent = opt.text;
				var x = document.createElement("button");
				x.type = "button";
				x.className = "shrink-0 opacity-80 hover:opacity-100";
				x.textContent = "×";
				x.setAttribute("aria-label", t("제거", "Remove"));
				x.addEventListener("click", function (e) {
					e.preventDefault();
					e.stopPropagation();
					setSelected(opt, false);
				});
				chip.appendChild(label);
				chip.appendChild(x);
				control.insertBefore(chip, search);
			});
			// 선택 없으면 placeholder 보이게(검색창이 곧 placeholder 표시)
			search.placeholder = selected.length ? t("추가…", "Add…") : placeholder;
		}

		function setSelected(opt: HTMLOptionElement, sel: boolean) {
			opt.selected = sel;
			select.dispatchEvent(new Event("change", { bubbles: true }));
			renderChips();
			filter();
		}

		function filter() {
			var q = search.value.toLowerCase();
			realOpts.forEach(function (opt) {
				var row = rowByValue[opt.value];
				if (!row) return;
				// 이미 선택된 건 드롭다운에서 숨김(칩으로 표시됨)
				var match = !opt.selected && opt.text.toLowerCase().indexOf(q) !== -1;
				row.style.display = match ? "" : "none";
			});
		}

		function openPanel() {
			closeAll(panel);
			panel.hidden = false;
			filter();
		}

		control.addEventListener("click", function () {
			try {
				search.focus();
			} catch (err) {}
			openPanel();
		});
		search.addEventListener("focus", openPanel);
		search.addEventListener("input", function () {
			panel.hidden = false;
			filter();
		});
		// 빈 검색창에서 백스페이스 → 마지막 칩 제거
		search.addEventListener("keydown", function (e) {
			if ((e as KeyboardEvent).key === "Backspace" && search.value === "") {
				var selected = realOpts.filter(function (o) {
					return o.selected;
				});
				if (selected.length) setSelected(selected[selected.length - 1], false);
			}
		});

		var parent = select.parentNode;
		if (parent) {
			parent.insertBefore(wrap, select);
			select.style.setProperty("display", "none", "important");
			control.appendChild(search);
			wrap.appendChild(control);
			wrap.appendChild(panel);
			wrap.appendChild(select);
		}
		renderChips();
	}

	function closeAll(except: HTMLElement | null) {
		Array.prototype.slice
			.call(document.querySelectorAll("[data-poe-msd-panel]"))
			.forEach(function (p: HTMLElement) {
				if (p !== except) p.hidden = true;
			});
	}

	document.addEventListener("click", function (e) {
		var el = e.target as HTMLElement;
		if (!el || typeof el.closest !== "function" || !el.closest("[data-poe-msd-wrap]")) {
			closeAll(null);
		}
	});

	function enhanceAll() {
		Array.prototype.slice
			.call(document.querySelectorAll("select[multiple][data-poe-multi]"))
			.forEach(function (s: HTMLSelectElement) {
				enhance(s);
			});
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", enhanceAll);
	} else {
		enhanceAll();
	}
	document.addEventListener("htmx:afterSwap", enhanceAll);
	document.addEventListener("htmx:afterSettle", enhanceAll);

	// 트리 에디터에서 전직까지 정해 넘어온 경우(?ascendancy=) 최적화 폼 셀렉트에 반영한다.
	// JTE 는 속성명 자리 표현식(`<option ${cond ? "selected" : ""}>`)을 금지해 서버 렌더로는 못 박는다.
	// 반영하지 않으면 사용자가 고른 전직이 무시되고 최적화기가 임의로 다시 고른다.
	function applyAscendancyFromUrl() {
		const wanted = new URLSearchParams(globalThis.location.search).get("ascendancy");
		if (!wanted) return;
		const select = document.querySelector<HTMLSelectElement>('select[name="ascendancy"]');
		if (select && Array.prototype.some.call(select.options, (o: HTMLOptionElement) => o.value === wanted)) {
			select.value = wanted;
		}
	}
	applyAscendancyFromUrl();
})();
