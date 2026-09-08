// 월배당 시뮬레이터의 행 선택 합계(선택한 종목의 월배당·과세표준·평가액·수익률).
//
// 2026-09-08 까지 monthlyDividendSimulator.jte 안의 인라인 <script> 였다(240 줄, JTE 식 없음). assetStatus.ts 와 같은
// 규약으로 옮겼다 - 모듈은 문서당 한 번 실행되므로 첫 실행 때 초기화하고, htmx 재스왑은 htmx:afterSettle 에서 다시
// 초기화한다(섹션의 data-selection-initialized 표식으로 중복 결합을 막는다). 타입은 느슨하다(any).
export {};

(() => {
	const win: any = window;
	if (win.__monthlyDividendSimulatorAttached) {
		if (typeof win.initializeMonthlyDividendSimulator === "function") win.initializeMonthlyDividendSimulator();
		return;
	}
	win.__monthlyDividendSimulatorAttached = true;

		function getMonthlyDividendSimulatorLocale() {
			return document.documentElement.getAttribute('lang') || 'ko-KR';
		}

		function formatMonthlyDividendSimulatorNumber(value: number, fractionDigits: number) {
			return new Intl.NumberFormat(getMonthlyDividendSimulatorLocale(), {
				minimumFractionDigits: fractionDigits,
				maximumFractionDigits: fractionDigits
			}).format(value);
		}

		function formatMonthlyDividendSimulatorCurrency(value: number) {
			return '₩' + formatMonthlyDividendSimulatorNumber(value, 0);
		}

		function formatMonthlyDividendSimulatorSignedCurrency(value: number) {
			const absolute = formatMonthlyDividendSimulatorCurrency(Math.abs(value));
			if (value > 0) {
				return '+' + absolute;
			}
			if (value < 0) {
				return '-' + absolute;
			}
			return absolute;
		}

		function formatMonthlyDividendSimulatorSignedPercent(value: number) {
			const absolute = formatMonthlyDividendSimulatorNumber(Math.abs(value), 2) + '%';
			if (value > 0) {
				return '+' + absolute;
			}
			if (value < 0) {
				return '-' + absolute;
			}
			return absolute;
		}

		function setMonthlyDividendSimulatorSignedTone(element: HTMLElement | null, value: number) {
			if (!element) {
				return;
			}

			element.classList.toggle('text-profit', value > 0);
			element.classList.toggle('text-loss', value < 0);
		}

		function setMonthlyDividendSimulatorRowSelection(row: HTMLElement | null, selected: boolean) {
			if (!row) {
				return;
			}

			row.classList.toggle('monthly-dividend-row-selected', selected);
			row.setAttribute('aria-pressed', selected ? 'true' : 'false');

		}

		function updateMonthlyDividendSimulatorSelectionSummary(section: HTMLElement | null) {
			if (!section) {
				return;
			}

			const summary = section.querySelector<HTMLElement>('[data-monthly-selection-summary]');
			const table = section.querySelector<HTMLTableElement>('[data-monthly-selection-table]');
			if (!summary || !table || !table.tBodies || table.tBodies.length === 0) {
				return;
			}

			const selectedRows = table.tBodies[0].querySelectorAll<HTMLElement>('[data-monthly-selection-row][aria-pressed="true"]');
			const selectedCount = selectedRows.length;
			const reveal = summary.closest<HTMLElement>('.selection-reveal');
			if (reveal) {
				reveal.classList.toggle('is-open', selectedCount > 0);
			} else {
				summary.classList.toggle('hidden', selectedCount === 0);
			}

			if (selectedCount === 0) {
				return;
			}

			let totalLatestMonthlyDividend = 0;
			let totalLatestMonthlyDividendMid = 0;
			let totalLatestMonthlyDividendEnd = 0;
			let totalBuyAmount = 0;
			let totalExpectedMonthlyDividend = 0;
			let totalExpectedTaxableBase = 0;
			let totalCurrentMarketValue = 0;

			for (let i = 0; i < selectedRows.length; i++) {
				const row = selectedRows[i];
				const rowLatestMonthlyDividend = Number(row.dataset.latestMonthlyDividend || '0');
				totalLatestMonthlyDividend += rowLatestMonthlyDividend;
				const rowPayoutWindow = row.dataset.payoutWindow || '';
				if (rowPayoutWindow === 'MID_MONTH') {
					totalLatestMonthlyDividendMid += rowLatestMonthlyDividend;
				} else if (rowPayoutWindow === 'MONTH_END') {
					totalLatestMonthlyDividendEnd += rowLatestMonthlyDividend;
				}
				totalBuyAmount += Number(row.dataset.buyAmount || '0');
				totalExpectedMonthlyDividend += Number(row.dataset.expectedMonthlyDividend || '0');
				totalExpectedTaxableBase += Number(row.dataset.expectedTaxableBase || '0');
				totalCurrentMarketValue += Number(row.dataset.currentMarketValue || '0');
			}

			const totalExpectedAnnualDividend = totalExpectedMonthlyDividend * 12;
			const totalExpectedAnnualTaxableBase = totalExpectedTaxableBase * 12;
			const totalExpectedMonthlyReference = Number(summary.dataset.totalExpectedMonthlyDividend || '0');
			const totalCurrentMarketValueReference = Number(summary.dataset.totalCurrentMarketValue || '0');
			const selectionMonthlyWeight = totalExpectedMonthlyReference > 0
				? (totalExpectedMonthlyDividend / totalExpectedMonthlyReference) * 100
				: 0;
			const selectionMarketWeight = totalCurrentMarketValueReference > 0
				? (totalCurrentMarketValue / totalCurrentMarketValueReference) * 100
				: 0;
			const evaluationProfit = totalCurrentMarketValue - totalBuyAmount;
			const evaluationReturn = totalBuyAmount > 0
				? (evaluationProfit / totalBuyAmount) * 100
				: 0;
			const portfolioExpectedAnnualYield = totalCurrentMarketValue > 0
				? (totalExpectedAnnualDividend / totalCurrentMarketValue) * 100
				: 0;

			const countLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-count]');
			if (countLabel) {
				countLabel.textContent = String(summary.dataset.countTemplate || '').replace('{0}', String(selectedCount));
			}

			const itemCountLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-item-count]');
			if (itemCountLabel) {
				itemCountLabel.textContent = formatMonthlyDividendSimulatorNumber(selectedCount, 0);
			}

			const monthlyWeightLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-monthly-weight]');
			if (monthlyWeightLabel) {
				monthlyWeightLabel.textContent = formatMonthlyDividendSimulatorNumber(selectionMonthlyWeight, 1) + '%';
			}

			const marketWeightLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-market-weight]');
			if (marketWeightLabel) {
				marketWeightLabel.textContent = formatMonthlyDividendSimulatorNumber(selectionMarketWeight, 1) + '%';
			}

			const latestLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-latest]');
			if (latestLabel) {
				latestLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalLatestMonthlyDividend);
			}

			const latestMidLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-latest-mid]');
			if (latestMidLabel) {
				latestMidLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalLatestMonthlyDividendMid);
			}

			const latestEndLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-latest-end]');
			if (latestEndLabel) {
				latestEndLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalLatestMonthlyDividendEnd);
			}

			const monthlyLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-monthly]');
			if (monthlyLabel) {
				monthlyLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedMonthlyDividend);
			}

			const annualDividendLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-annual-dividend]');
			if (annualDividendLabel) {
				annualDividendLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedAnnualDividend);
			}

			const taxableLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-taxable]');
			if (taxableLabel) {
				taxableLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedTaxableBase);
			}

			const annualTaxableLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-annual-taxable]');
			if (annualTaxableLabel) {
				annualTaxableLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalExpectedAnnualTaxableBase);
			}

			const buyLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-buy]');
			if (buyLabel) {
				buyLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalBuyAmount);
			}

			const marketLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-market]');
			if (marketLabel) {
				marketLabel.textContent = formatMonthlyDividendSimulatorCurrency(totalCurrentMarketValue);
			}

			const profitLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-profit]');
			if (profitLabel) {
				profitLabel.textContent = formatMonthlyDividendSimulatorSignedCurrency(evaluationProfit);
				setMonthlyDividendSimulatorSignedTone(profitLabel, evaluationProfit);
			}

			const profitRateLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-profit-rate]');
			if (profitRateLabel) {
				profitRateLabel.textContent = formatMonthlyDividendSimulatorSignedPercent(evaluationReturn);
				setMonthlyDividendSimulatorSignedTone(profitRateLabel, evaluationReturn);
			}

			const annualYieldLabel = summary.querySelector<HTMLElement>('[data-monthly-selection-annual-yield]');
			if (annualYieldLabel) {
				annualYieldLabel.textContent = formatMonthlyDividendSimulatorNumber(portfolioExpectedAnnualYield, 2) + '%';
			}
		}

		function initializeMonthlyDividendSimulatorSelection() {
			const sections = document.querySelectorAll<HTMLElement>('[data-monthly-simulator-selection-section]');
			for (let i = 0; i < sections.length; i++) {
				const section = sections[i];
				if (section.dataset.selectionInitialized === 'true') {
					continue;
				}

				const table = section.querySelector<HTMLTableElement>('[data-monthly-selection-table]');
				if (!table || !table.tBodies || table.tBodies.length === 0) {
					continue;
				}

				section.dataset.selectionInitialized = 'true';

				const rows = table.tBodies[0].querySelectorAll<HTMLElement>('[data-monthly-selection-row]');
				for (let j = 0; j < rows.length; j++) {
					const row = rows[j];

					row.addEventListener('click', function(this: HTMLElement, event: MouseEvent) {
						if (event.target && (event.target as Element).closest('button, a, input, label, select, textarea, [data-profile-order-handle]')) {
							return;
						}

						const isSelected = this.getAttribute('aria-pressed') === 'true';
						setMonthlyDividendSimulatorRowSelection(this, !isSelected);
						updateMonthlyDividendSimulatorSelectionSummary(section);
					});

					row.addEventListener('keydown', function(this: HTMLElement, event: KeyboardEvent) {
						if (event.target && (event.target as Element).closest('button, a, input, label, select, textarea, [data-profile-order-handle]')) {
							return;
						}

						if (event.key !== 'Enter' && event.key !== ' ') {
							return;
						}

						event.preventDefault();
						const isSelected = this.getAttribute('aria-pressed') === 'true';
						setMonthlyDividendSimulatorRowSelection(this, !isSelected);
						updateMonthlyDividendSimulatorSelectionSummary(section);
					});

				}

				const clearButton = section.querySelector<HTMLElement>('[data-monthly-selection-clear]');
				if (clearButton) {
					clearButton.addEventListener('click', function(this: HTMLElement) {
						const selectedRows = table.tBodies[0].querySelectorAll<HTMLElement>('[data-monthly-selection-row][aria-pressed="true"]');
						for (let k = 0; k < selectedRows.length; k++) {
							setMonthlyDividendSimulatorRowSelection(selectedRows[k], false);
						}

						updateMonthlyDividendSimulatorSelectionSummary(section);
					});
				}

				updateMonthlyDividendSimulatorSelectionSummary(section);
			}
		}

	function initializeMonthlyDividendSimulator() {
		initializeMonthlyDividendSimulatorSelection();
	}

	win.initializeMonthlyDividendSimulator = initializeMonthlyDividendSimulator;
	document.addEventListener("htmx:afterSettle", initializeMonthlyDividendSimulator);
	initializeMonthlyDividendSimulator();
})();
