// 화면에 고정으로 뜨는 태그 풍선: [data-stock-tag-tooltip] 위에 마우스를 올리면 그 값을 커서 옆에 보여 준다.
// CSS 툴팁(.tooltip)은 가로 스크롤 상자(표)에 잘리므로, 표 안 종목명 · 연도 칩 · 짧게 보유 배지 · 용어 풀이(termHint)는 이것을 쓴다.
//
// 2026-10-22 까지 assetStatus.ts 와 dividendHistory.ts 가 같은 구현을 한 벌씩 들고 있었다(먼저 실린 쪽이 정의 - typeof 가드).
// 여기 한 벌로 모으고 stockLayout 이 classic script 로 한 번 싣는다. 레이아웃 본문 끝의 classic script 는 조각의 모듈
// 스크립트(defer)보다 먼저 돌고, htmx 로 나중에 온 조각에는 이미 정의돼 있다. 조각은 초기화 때마다
// window.initializeStockTagHoverTooltips(root) 를 부르고, 표식(data-stock-tag-tooltip-initialized)으로 한 번만 묶는다.
// import/export 없이 classic <script src> 로 로드(tableSort.ts 와 같다).
(function () {
	const win: any = window;
	if (typeof win.initializeStockTagHoverTooltips === "function") {
		return;
	}

	win.initializeStockTagHoverTooltips = function (root: ParentNode | null) {
		let tooltip = win.stockTagHoverTooltip;
		if (!tooltip) {
			const tooltipEl = document.createElement("div");
			tooltipEl.id = "stockTagHoverTooltip";
			tooltipEl.className =
				"pointer-events-none fixed left-0 top-0 z-50 hidden w-64 max-w-[calc(100vw-2rem)] rounded-box bg-neutral px-3 py-2 text-left text-xs leading-5 text-neutral-content shadow-lg sm:w-72";
			document.body.appendChild(tooltipEl);

			tooltip = {
				element: tooltipEl,
				show(message: string, clientX: number, clientY: number) {
					if (!message) {
						this.hide();
						return;
					}

					this.element.textContent = message;
					this.element.classList.remove("hidden");
					this.move(clientX, clientY);
				},
				move(clientX: number, clientY: number) {
					if (this.element.classList.contains("hidden")) {
						return;
					}

					const offsetX = 12;
					const offsetY = 18;
					const viewportPadding = 12;
					let left = clientX + offsetX;
					let top = clientY + offsetY;
					const rect = this.element.getBoundingClientRect();

					if (left + rect.width + viewportPadding > win.innerWidth) {
						left = Math.max(viewportPadding, win.innerWidth - rect.width - viewportPadding);
					}

					if (top + rect.height + viewportPadding > win.innerHeight) {
						top = Math.max(viewportPadding, clientY - rect.height - offsetY);
					}

					this.element.style.left = left + "px";
					this.element.style.top = top + "px";
				},
				hide() {
					this.element.classList.add("hidden");
					this.element.textContent = "";
				},
			};

			win.stockTagHoverTooltip = tooltip;
			win.addEventListener(
				"scroll",
				function () {
					if (win.stockTagHoverTooltip) {
						win.stockTagHoverTooltip.hide();
					}
				},
				true,
			);
			win.addEventListener("resize", function () {
				if (win.stockTagHoverTooltip) {
					win.stockTagHoverTooltip.hide();
				}
			});
		}

		const scope = root || document;
		const targets = scope.querySelectorAll<HTMLElement>("[data-stock-tag-tooltip]");
		for (let i = 0; i < targets.length; i++) {
			const target = targets[i];
			if (target.dataset.stockTagTooltipInitialized === "true") {
				continue;
			}

			target.dataset.stockTagTooltipInitialized = "true";

			target.addEventListener("mouseenter", function (this: HTMLElement, event: MouseEvent) {
				tooltip.show(this.dataset.stockTagTooltip, event.clientX, event.clientY);
			});

			target.addEventListener("mousemove", function (this: HTMLElement, event: MouseEvent) {
				tooltip.move(event.clientX, event.clientY);
			});

			target.addEventListener("mouseleave", function () {
				tooltip.hide();
			});
		}
	};
})();
