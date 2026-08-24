// 복리 계산 본체. 화면(DOM)과 떼어 두어 브라우저 없이 검증할 수 있게 한다.
// 이 파일 전체가 IIFE 라 안쪽 함수는 내보낼 수 없어서, 계산만 모듈 최상위로 올렸다.
// 동작은 그대로다 - 아래 simulate() 가 입력값을 읽어 이 함수를 부른다.

const COMPOUND_MAX_YEARS = 100;
// 이율 허용 범위. 입력 필드의 min/max 와 같은 값이다.
// HTML 의 min/max 는 타이핑을 막지 못하고 필드를 :invalid 로만 표시하므로(값은 그대로 읽힌다),
// 연수처럼 코드에서도 잘라 준다. 특히 -100% 아래로 내려가면 잔액이 양수와 음수를 오간다
// (실측: -150% 로 10년 -> 1년차 -5,500,000 / 2년차 +2,250,000 / 3년차 -1,625,000 ...).
const COMPOUND_MIN_RATE_PCT = -100;
const COMPOUND_MAX_RATE_PCT = 200;

interface YearRow {
	year: number;
	contribution: number;
	cumulativePrincipal: number;
	gain: number;
	balance: number;
}

interface CompoundInput {
	initial: number;
	contribution: number;
	ratePct: number;
	years: number;
	monthly: boolean;
	contributeAtBegin: boolean;
}

/**
 * 연차별 적립 결과. 입력 범위를 여기서 자르므로 호출부는 날값을 그대로 넘겨도 된다.
 *
 * 매월 모드는 연 이율/12 의 월 이율로 월복리 (일반적인 적금 계산기 관례).
 */
function projectCompound(input: CompoundInput): YearRow[] {
	const initial = Math.max(0, input.initial);
	const contribution = Math.max(0, input.contribution);
	const rate =
		Math.min(
			COMPOUND_MAX_RATE_PCT,
			Math.max(COMPOUND_MIN_RATE_PCT, input.ratePct),
		) / 100;
	const years = Math.min(
		COMPOUND_MAX_YEARS,
		Math.max(1, Math.floor(input.years)),
	);
	const periodsPerYear = input.monthly ? 12 : 1;
	const periodRate = input.monthly ? rate / 12 : rate;

	const rows: YearRow[] = [];
	let balance = initial;
	let cumulativePrincipal = initial;

	for (let year = 1; year <= years; year++) {
		let yearContribution = 0;
		let yearGain = 0;
		for (let period = 0; period < periodsPerYear; period++) {
			if (input.contributeAtBegin) {
				balance += contribution;
				yearContribution += contribution;
			}
			const gain = balance * periodRate;
			balance += gain;
			yearGain += gain;
			if (!input.contributeAtBegin) {
				balance += contribution;
				yearContribution += contribution;
			}
		}
		cumulativePrincipal += yearContribution;
		rows.push({
			year,
			contribution: yearContribution,
			cumulativePrincipal,
			gain: yearGain,
			balance,
		});
	}

	return rows;
}

// 이 파일은 type="module" 없이 classic <script src> 로 로드된다. export 문을 넣으면 브라우저가
// "Unexpected token 'export'" 로 파일 전체를 거부해 화면 기능이 통째로 죽는다(실제로 그렇게 깨뜨렸다).
// 그래서 검증용으로는 export 대신 전역에 붙인다 - 브라우저에서는 쓰이지 않고 테스트만 읽는다.
(globalThis as any).__stockCompoundSimulatorInternals = {
	projectCompound,
	COMPOUND_MIN_RATE_PCT,
	COMPOUND_MAX_RATE_PCT,
	COMPOUND_MAX_YEARS,
};

(() => {
	const root = document.getElementById("stockCompoundSimulator");
	if (!root || root.dataset.compoundSimulatorInitialized === "true") {
		return;
	}
	root.dataset.compoundSimulatorInitialized = "true";

	const STORAGE_KEY = "stock.compoundSimulator.v1";
	// 이율/연수 상한은 모듈 최상위의 COMPOUND_* 상수를 쓴다(계산과 같은 값을 한 곳에서 관리).
	// 인출 시뮬레이터는 annualRateToMonthlyRate 에서 이미 같은 하한을 두고 있다.

	const initialInput = document.getElementById(
		"stockCompoundInitial",
	) as HTMLInputElement | null;
	const contributionInput = document.getElementById(
		"stockCompoundContribution",
	) as HTMLInputElement | null;
	const frequencySelect = document.getElementById(
		"stockCompoundFrequency",
	) as HTMLSelectElement | null;
	const monthlyNote = document.getElementById("stockCompoundMonthlyNote");
	const timingSelect = document.getElementById(
		"stockCompoundTiming",
	) as HTMLSelectElement | null;
	const rateInput = document.getElementById(
		"stockCompoundRatePct",
	) as HTMLInputElement | null;
	const yearsInput = document.getElementById(
		"stockCompoundYears",
	) as HTMLInputElement | null;
	const initialPreview = document.getElementById("stockCompoundInitialPreview");
	const contributionPreview = document.getElementById(
		"stockCompoundContributionPreview",
	);
	const finalValueEl = document.getElementById("stockCompoundFinalValue");
	const totalPrincipalEl = document.getElementById(
		"stockCompoundTotalPrincipal",
	);
	const totalProfitEl = document.getElementById("stockCompoundTotalProfit");
	const totalReturnEl = document.getElementById("stockCompoundTotalReturn");
	const tableBody = document.getElementById("stockCompoundYearlyTableBody");
	const ratioContainer = document.getElementById("stockCompoundRatio");
	const ratioPrincipalBar = document.getElementById(
		"stockCompoundRatioPrincipalBar",
	);
	const ratioProfitBar = document.getElementById(
		"stockCompoundRatioProfitBar",
	);
	const ratioPrincipalPct = document.getElementById(
		"stockCompoundRatioPrincipalPct",
	);
	const ratioProfitPct = document.getElementById(
		"stockCompoundRatioProfitPct",
	);
	const chartCanvas = document.getElementById(
		"stockCompoundGrowthChart",
	) as HTMLCanvasElement | null;

	// 앱 로케일을 쓴다. stock-charts.ts 의 resolveLocale 과 같은 규칙이다 - 이 파일들은 클래식 스크립트라
	// import 를 쓸 수 없어 규칙을 옮겨 적고, compactNumberParity 옆의 selectorsResolve 처럼 테스트로 묶는다.
	// 예전에는 이 파일만 로케일을 따로 정해, 같은 화면 안에서도 숫자 자릿수 구분이 갈릴 수 있었다
	// (compoundSimulator 는 "ko-KR" 고정, stockSimulator 는 undefined = 브라우저 로케일).
	const appLocale =
		document.body?.dataset?.locale ||
		document.documentElement?.lang ||
		navigator.language ||
		"ko-KR";
	const currencyFormatter = new Intl.NumberFormat(appLocale);
	let growthChart: any = null;

	function readNumber(input: HTMLInputElement | null, fallback: number) {
		const raw = input?.value?.trim();
		// Number("") === 0 이므로 빈 입력은 명시적으로 fallback 처리한다
		if (!raw) {
			return fallback;
		}
		const value = Number(raw);
		return Number.isFinite(value) ? value : fallback;
	}

	// 시리즈 색은 main.css 의 CSS 변수(--color-compound-*)를 단일 소스로 사용한다
	function seriesColor(name: string, fallback: string) {
		const value = getComputedStyle(document.documentElement)
			.getPropertyValue(name)
			.trim();
		return value || fallback;
	}

	function formatCurrency(value: number) {
		const rounded = Math.round(value || 0);
		const formatted = currencyFormatter.format(Math.abs(rounded));
		return rounded < 0 ? `-₩${formatted}` : `₩${formatted}`;
	}

	function formatSignedPercent(value: number) {
		if (!Number.isFinite(value)) {
			return "-";
		}
		return `${value > 0 ? "+" : ""}${value.toFixed(1)}%`;
	}

	function applyProfitColor(element: HTMLElement | null, value: number) {
		if (!element) {
			return;
		}
		element.classList.toggle("text-profit", value >= 0);
		element.classList.toggle("text-loss", value < 0);
	}

	function restoreInputs() {
		try {
			const raw = localStorage.getItem(STORAGE_KEY);
			if (!raw) {
				return;
			}
			const saved = JSON.parse(raw);
			if (initialInput && Number.isFinite(Number(saved.initial))) {
				initialInput.value = String(saved.initial);
			}
			if (contributionInput && Number.isFinite(Number(saved.contribution))) {
				contributionInput.value = String(saved.contribution);
			}
			if (
				frequencySelect &&
				(saved.frequency === "yearly" || saved.frequency === "monthly")
			) {
				frequencySelect.value = saved.frequency;
			}
			if (
				timingSelect &&
				(saved.timing === "begin" || saved.timing === "end")
			) {
				timingSelect.value = saved.timing;
			}
			if (rateInput && Number.isFinite(Number(saved.ratePct))) {
				rateInput.value = String(saved.ratePct);
			}
			if (yearsInput && Number.isFinite(Number(saved.years))) {
				yearsInput.value = String(saved.years);
			}
		} catch {
			// 저장값이 손상된 경우 기본값으로 진행한다.
		}
	}

	function persistInputs() {
		try {
			localStorage.setItem(
				STORAGE_KEY,
				JSON.stringify({
					initial: readNumber(initialInput, 0),
					contribution: readNumber(contributionInput, 0),
					frequency:
						frequencySelect?.value === "monthly" ? "monthly" : "yearly",
					timing: timingSelect?.value === "end" ? "end" : "begin",
					ratePct: readNumber(rateInput, 0),
					years: readNumber(yearsInput, 1),
				}),
			);
		} catch {
			// localStorage를 못 쓰는 환경에서도 계산은 계속 동작한다.
		}
	}

	function simulate(): YearRow[] {
		return projectCompound({
			initial: readNumber(initialInput, 0),
			contribution: readNumber(contributionInput, 0),
			ratePct: readNumber(rateInput, 0),
			years: readNumber(yearsInput, 1),
			monthly: frequencySelect?.value === "monthly",
			contributeAtBegin: timingSelect?.value !== "end",
		});
	}

	function renderPreviews() {
		if (initialPreview) {
			initialPreview.textContent = currencyFormatter.format(
				Math.round(Math.max(0, readNumber(initialInput, 0))),
			);
		}
		if (contributionPreview) {
			contributionPreview.textContent = currencyFormatter.format(
				Math.round(Math.max(0, readNumber(contributionInput, 0))),
			);
		}
	}

	function renderSummary(rows: YearRow[]) {
		const last = rows[rows.length - 1];
		const finalValue = last ? last.balance : 0;
		const totalPrincipal = last ? last.cumulativePrincipal : 0;
		const profit = finalValue - totalPrincipal;
		const returnPct = totalPrincipal > 0 ? (profit / totalPrincipal) * 100 : 0;

		if (finalValueEl) {
			finalValueEl.textContent = formatCurrency(finalValue);
		}
		if (totalPrincipalEl) {
			totalPrincipalEl.textContent = formatCurrency(totalPrincipal);
		}
		if (totalProfitEl) {
			totalProfitEl.textContent = formatCurrency(profit);
			applyProfitColor(totalProfitEl, profit);
		}
		if (totalReturnEl) {
			totalReturnEl.textContent = formatSignedPercent(returnPct);
			applyProfitColor(totalReturnEl, profit);
		}
		renderRatio(finalValue, totalPrincipal, profit);
	}

	function renderRatio(
		finalValue: number,
		totalPrincipal: number,
		profit: number,
	) {
		if (
			!ratioContainer ||
			!ratioPrincipalBar ||
			!ratioProfitBar ||
			!ratioPrincipalPct ||
			!ratioProfitPct
		) {
			return;
		}
		if (finalValue <= 0) {
			ratioContainer.classList.add("hidden");
			return;
		}
		ratioContainer.classList.remove("hidden");
		const principalPct = (totalPrincipal / finalValue) * 100;
		const profitPct = (profit / finalValue) * 100;
		// 수익이 음수면 원금 바가 100%를 채우고 수익 비율은 음수로만 표기한다.
		ratioPrincipalBar.style.width = `${Math.min(100, Math.max(0, principalPct))}%`;
		ratioProfitBar.style.width = `${Math.min(100, Math.max(0, profitPct))}%`;
		ratioPrincipalPct.textContent = `${principalPct.toFixed(1)}%`;
		ratioProfitPct.textContent = `${profitPct.toFixed(1)}%`;
	}

	function ratioShares(row: YearRow) {
		if (row.balance <= 0) {
			return null;
		}
		return {
			principalPct: (row.cumulativePrincipal / row.balance) * 100,
			profitPct:
				((row.balance - row.cumulativePrincipal) / row.balance) * 100,
		};
	}

	function buildRatioCell(row: YearRow) {
		const td = document.createElement("td");
		td.className = "text-right";
		const shares = ratioShares(row);
		if (!shares) {
			td.textContent = "-";
			return td;
		}

		const wrapper = document.createElement("div");
		wrapper.className = "flex items-center justify-end gap-2";

		const bar = document.createElement("div");
		bar.className =
			"flex h-1.5 w-20 shrink-0 overflow-hidden rounded-full bg-base-200";
		const principalSegment = document.createElement("div");
		principalSegment.className = "h-1.5";
		principalSegment.style.background = "var(--color-compound-principal)";
		principalSegment.style.width = `${Math.min(100, Math.max(0, shares.principalPct))}%`;
		const profitSegment = document.createElement("div");
		profitSegment.className = "h-1.5";
		profitSegment.style.background = "var(--color-compound-profit)";
		profitSegment.style.width = `${Math.min(100, Math.max(0, shares.profitPct))}%`;
		bar.appendChild(principalSegment);
		bar.appendChild(profitSegment);

		const text = document.createElement("span");
		text.className =
			"font-mono tabular-nums text-xs text-base-content/60 whitespace-nowrap";
		text.textContent = `${shares.principalPct.toFixed(1)} / ${shares.profitPct.toFixed(1)}%`;

		wrapper.appendChild(bar);
		wrapper.appendChild(text);
		td.appendChild(wrapper);
		return td;
	}

	function renderTable(rows: YearRow[]) {
		if (!tableBody) {
			return;
		}
		tableBody.replaceChildren();
		for (const row of rows) {
			const tr = document.createElement("tr");
			const cells = [
				String(row.year),
				formatCurrency(row.contribution),
				formatCurrency(row.cumulativePrincipal),
				formatCurrency(row.gain),
				formatCurrency(row.balance),
			];
			cells.forEach((text, index) => {
				const td = document.createElement("td");
				td.textContent = text;
				if (index > 0) {
					td.className = "text-right font-mono tabular-nums amount-value";
				}
				tr.appendChild(td);
			});
			tr.appendChild(buildRatioCell(row));
			tableBody.appendChild(tr);
		}
	}

	function renderChart(rows: YearRow[]) {
		const Chart = (globalThis as any).Chart;
		if (!chartCanvas || typeof Chart === "undefined") {
			return;
		}
		const labels = rows.map((row) => String(row.year));
		const principalData = rows.map((row) => Math.round(row.cumulativePrincipal));
		const profitData = rows.map((row) =>
			Math.round(row.balance - row.cumulativePrincipal),
		);
		const i18n = (root as HTMLElement).dataset;
		const principalLabel = i18n.seriesPrincipal || "Contributions";
		const profitLabel = i18n.seriesProfit || "Cumulative Gain";

		if (growthChart) {
			growthChart.data.labels = labels;
			growthChart.data.datasets[0].data = principalData;
			growthChart.data.datasets[1].data = profitData;
			growthChart.update();
			return;
		}

		growthChart = new Chart(chartCanvas, {
			type: "bar",
			data: {
				labels,
				datasets: [
					{
						label: principalLabel,
						data: principalData,
						// canvas 는 var() 를 해석하지 못하므로 계산된 값을 읽어 넣는다
						backgroundColor: seriesColor(
							"--color-compound-principal",
							"rgba(99,102,241,0.8)",
						),
						stack: "total",
						borderRadius: 3,
						maxBarThickness: 36,
					},
					{
						label: profitLabel,
						data: profitData,
						backgroundColor: seriesColor(
							"--color-compound-profit",
							"rgba(206,57,69,0.75)",
						),
						stack: "total",
						borderRadius: 3,
						maxBarThickness: 36,
					},
				],
			},
			options: {
				responsive: true,
				maintainAspectRatio: false,
				interaction: {
					mode: "index",
					intersect: false,
				},
				scales: {
					x: { stacked: true },
					y: {
						stacked: true,
						ticks: {
							callback: (value: number) => currencyFormatter.format(value),
						},
					},
				},
				plugins: {
					tooltip: {
						callbacks: {
							label: (context: any) => {
								const datasets = context.chart.data.datasets;
								const values = datasets.map((dataset: any) =>
									Number(dataset.data[context.dataIndex] || 0),
								);
								const total = values.reduce(
									(sum: number, v: number) => sum + v,
									0,
								);
								const base = `${context.dataset.label}: ${formatCurrency(context.parsed.y)}`;
								// 수익이 음수인 해는 구성 비율이 성립하지 않으므로(원금이 100% 를 넘음) 금액만 표시
								if (total <= 0 || values.some((v: number) => v < 0)) {
									return base;
								}
								const pct = ((context.parsed.y / total) * 100).toFixed(1);
								return `${base} (${pct}%)`;
							},
						},
					},
				},
			},
		});
	}

	function update() {
		if (monthlyNote) {
			monthlyNote.classList.toggle(
				"hidden",
				frequencySelect?.value !== "monthly",
			);
		}
		const rows = simulate();
		renderPreviews();
		renderSummary(rows);
		renderTable(rows);
		renderChart(rows);
		persistInputs();
	}

	const form = document.getElementById("stockCompoundForm");
	if (form) {
		form.addEventListener("input", update);
		form.addEventListener("change", update);
		form.addEventListener("submit", (event) => event.preventDefault());
		// 금액 증감 버튼: data-amount-add 만큼 더하고(0 이면 초기화) 즉시 재계산
		form.addEventListener("click", (event) => {
			const button = (event.target as HTMLElement).closest?.(
				"[data-amount-add-target]",
			) as HTMLElement | null;
			if (!button) return;
			const target = document.getElementById(
				button.getAttribute("data-amount-add-target") || "",
			) as HTMLInputElement | null;
			if (!target) return;
			const delta = Number(button.getAttribute("data-amount-add")) || 0;
			target.value = String(delta === 0 ? 0 : readNumber(target, 0) + delta);
			update();
		});
	}

	restoreInputs();
	update();
})();
