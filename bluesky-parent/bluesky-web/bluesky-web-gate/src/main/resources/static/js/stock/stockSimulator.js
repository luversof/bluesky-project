"use strict";
// @ts-nocheck
(() => {
    const root = document.getElementById("stockSimulatorApp");
    if (!root || root.dataset.stockSimulatorInitialized === "true") {
        return;
    }
    root.dataset.stockSimulatorInitialized = "true";
    const STORAGE_KEY = "stock.dividendGrowthSimulator.v1";
    const STORAGE_SCHEMA_VERSION = 2;
    const MAX_SIMULATION_YEARS = 100;
    const MAX_SCENARIOS = 5;
    const COLOR_CLASSES = [
        "bg-primary",
        "bg-secondary",
        "bg-accent",
        "bg-info",
        "bg-success",
    ];
    const CHART_COLORS = [
        { border: "#2563eb", background: "rgba(37, 99, 235, 0.15)" },
        { border: "#db2777", background: "rgba(219, 39, 119, 0.15)" },
        { border: "#059669", background: "rgba(5, 150, 105, 0.15)" },
        { border: "#d97706", background: "rgba(217, 119, 6, 0.15)" },
        { border: "#7c3aed", background: "rgba(124, 58, 237, 0.15)" },
    ];
    const METRICS = {
        spendingCoveragePct: "spendingCoveragePct",
        annualGap: "annualGap",
        cashReserve: "cashReserve",
        marketValue: "marketValue",
        shareCount: "shares",
        soldSharesForSpending: "soldSharesForSpending",
    };
    const i18nOverrides = globalThis.stockSimulatorI18n &&
        typeof globalThis.stockSimulatorI18n === "object"
        ? globalThis.stockSimulatorI18n
        : {};
    const i18n = Object.assign({ defaultScenarioName: "Base Scenario", activeScenario: "Editing", localOnly: "localStorage only", compareBestChoice: "More Favorable", fieldPrincipal: "Principal", fieldCurrentPrice: "Price", fieldAnnualPriceGrowth: "Price Growth", fieldDividendYield: "Dividend Yield", fieldAnnualDividendGrowth: "Dividend Growth", fieldAnnualSpending: "Spending", fieldAnnualSpendingGrowth: "Spending Growth", reinvestOn: "Reinvest ON", reinvestOff: "Cash Dividend", emptyScenario: "A default scenario has been created.", emptyTable: "No yearly data is available.", deleteConfirm: "Delete the current scenario?", maxScenarios: "You can compare up to five scenarios at once.", summarySustainablePeriod: "Sustainable Period", summaryFinalWealth: "Final Total Wealth", summaryDepletionYear: "Depletion", summaryNotDepleted: "Not Depleted", summaryDeficitStart: "Deficit Starts", summaryNoDeficit: "No Deficit", summaryWealthDeclineStart: "Total Wealth Decline Starts", summaryNoWealthDecline: "No Wealth Decline", summaryCapitalDrawdownStart: "Principal Drawdown Starts", summaryNoPrincipalDrawdown: "No Principal Drawdown", summaryYearsLater: "In {0} years", summaryYearsOrMore: "{0}+ years", summaryWithinHorizon: "Sustainable within the simulation horizon", summaryLatestCoverage: "Latest Spending Coverage", summaryNoSpending: "No Spending", seriesAnnualDividend: "Annual Dividend", seriesAnnualSpending: "Annual Spending", seriesAnnualGap: "Annual Gap", seriesTotalWealth: "Total Wealth", seriesMarketValue: "Market Value", seriesCashReserve: "Cash Reserve", timelinePhaseStable: "Spending Covered", timelinePhaseDeficit: "Deficit", timelinePhaseWealthDecline: "Wealth Decline", timelinePhaseDrawdown: "Principal Drawdown", timelinePhaseDepleted: "Depleted" }, i18nOverrides);
    const currencyFormatter = new Intl.NumberFormat(undefined, {
        maximumFractionDigits: 0,
    });
    const percentFormatter = new Intl.NumberFormat(undefined, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
    const shareFormatter = new Intl.NumberFormat(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 4,
    });
    const elements = {
        addButton: document.getElementById("stockSimulatorAddScenario"),
        duplicateButton: document.getElementById("stockSimulatorDuplicateScenario"),
        deleteButton: document.getElementById("stockSimulatorDeleteScenario"),
        resetButton: document.getElementById("stockSimulatorResetStorage"),
        scenarioList: document.getElementById("stockSimulatorScenarioList"),
        form: document.getElementById("stockSimulatorForm"),
        summaryCards: document.getElementById("stockSimulatorSummaryCards"),
        timelineList: document.getElementById("stockSimulatorTimelineList"),
        yearlyTableBody: document.getElementById("stockSimulatorYearlyTableBody"),
        cashFlowChartCanvas: document.getElementById("stockSimulatorCashFlowChart"),
        assetChartCanvas: document.getElementById("stockSimulatorAssetChart"),
        principalPreview: document.getElementById("stockSimulatorPrincipalPreview"),
        currentPricePreview: document.getElementById("stockSimulatorCurrentPricePreview"),
        annualSpendingPreview: document.getElementById("stockSimulatorAnnualSpendingPreview"),
    };
    let state = loadState();
    let cashFlowChart = null;
    let assetChart = null;
    let hydratingForm = false;
    bindEvents();
    render();
    function bindEvents() {
        if (elements.addButton) {
            elements.addButton.addEventListener("click", handleAddScenario);
        }
        if (elements.duplicateButton) {
            elements.duplicateButton.addEventListener("click", handleDuplicateScenario);
        }
        if (elements.deleteButton) {
            elements.deleteButton.addEventListener("click", handleDeleteScenario);
        }
        if (elements.resetButton) {
            elements.resetButton.addEventListener("click", handleResetScenarios);
        }
        if (elements.form) {
            elements.form.addEventListener("input", handleFormChange);
            elements.form.addEventListener("change", handleFormChange);
        }
    }
    function loadState() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) {
                return createDefaultState();
            }
            const parsed = JSON.parse(raw);
            return sanitizeState(parsed);
        }
        catch (error) {
            console.warn("stockSimulator load failed", error);
            return createDefaultState();
        }
    }
    function saveState() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
        }
        catch (error) {
            console.warn("stockSimulator save failed", error);
        }
    }
    function createDefaultState() {
        const defaultScenario = createDefaultScenario(1);
        return {
            version: STORAGE_SCHEMA_VERSION,
            scenarios: [defaultScenario],
            activeScenarioId: defaultScenario.id,
            metric: "spendingCoveragePct",
        };
    }
    function sanitizeState(raw) {
        const legacyDefaultYears = (raw === null || raw === void 0 ? void 0 : raw.version) == null;
        const scenarios = Array.isArray(raw === null || raw === void 0 ? void 0 : raw.scenarios)
            ? raw.scenarios
                .slice(0, MAX_SCENARIOS)
                .map((scenario, index) => normalizeScenario(migrateLegacyScenario(scenario, legacyDefaultYears), index + 1))
            : [];
        if (!scenarios.length) {
            return createDefaultState();
        }
        const activeScenarioId = scenarios.some((scenario) => scenario.id === raw.activeScenarioId)
            ? raw.activeScenarioId
            : scenarios[0].id;
        return {
            version: STORAGE_SCHEMA_VERSION,
            scenarios,
            activeScenarioId,
            metric: METRICS[raw.metric] ? raw.metric : "spendingCoveragePct",
        };
    }
    function migrateLegacyScenario(scenario, shouldExpandYears) {
        if (!shouldExpandYears || (scenario === null || scenario === void 0 ? void 0 : scenario.years) !== 20) {
            return scenario;
        }
        return Object.assign(Object.assign({}, scenario), { years: MAX_SIMULATION_YEARS });
    }
    function normalizeScenario(rawScenario, index) {
        const scenario = rawScenario || {};
        return {
            id: typeof scenario.id === "string" && scenario.id
                ? scenario.id
                : createId(),
            name: normalizeName(scenario.name, index),
            principal: clampNumber(scenario.principal, 10000, 100000000000, 1000000000),
            currentPrice: clampNumber(scenario.currentPrice, 1, 1000000000, 100000),
            annualPriceGrowthPct: clampNumber(scenario.annualPriceGrowthPct, -100, 300, 4),
            dividendYieldPct: clampNumber(scenario.dividendYieldPct, 0, 100, 4),
            annualDividendGrowthPct: clampNumber(scenario.annualDividendGrowthPct, -100, 300, 4),
            annualSpending: clampNumber(scenario.annualSpending, 0, 100000000000, 30000000),
            annualSpendingGrowthPct: clampNumber(scenario.annualSpendingGrowthPct, -100, 300, 7.2),
            years: Math.round(clampNumber(scenario.years, 1, MAX_SIMULATION_YEARS, 100)),
            reinvestDividends: Boolean(scenario.reinvestDividends),
        };
    }
    function normalizeName(value, index) {
        const trimmed = typeof value === "string" ? value.trim() : "";
        if (trimmed) {
            return trimmed.slice(0, 40);
        }
        return `${i18n.defaultScenarioName} ${index}`;
    }
    function createDefaultScenario(index) {
        return normalizeScenario({
            id: createId(),
            name: `${i18n.defaultScenarioName} ${index}`,
            principal: 1000000000,
            currentPrice: 100000,
            annualPriceGrowthPct: 4,
            dividendYieldPct: 4,
            annualDividendGrowthPct: 4,
            annualSpending: 30000000,
            annualSpendingGrowthPct: 7.2,
            years: 100,
            reinvestDividends: true,
        }, index);
    }
    function createId() {
        var _a;
        if (typeof ((_a = globalThis.crypto) === null || _a === void 0 ? void 0 : _a.randomUUID) === "function") {
            return globalThis.crypto.randomUUID();
        }
        return `scenario-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
    }
    function clampNumber(value, min, max, fallback) {
        const number = Number(value);
        if (!Number.isFinite(number)) {
            return fallback;
        }
        return Math.min(max, Math.max(min, number));
    }
    function getActiveScenario() {
        return (state.scenarios.find((scenario) => scenario.id === state.activeScenarioId) || state.scenarios[0]);
    }
    function handleAddScenario() {
        if (state.scenarios.length >= MAX_SCENARIOS) {
            globalThis.alert(i18n.maxScenarios);
            return;
        }
        const scenario = createDefaultScenario(state.scenarios.length + 1);
        state.scenarios = [...state.scenarios, scenario];
        state.activeScenarioId = scenario.id;
        saveState();
        render();
    }
    function handleDuplicateScenario() {
        if (state.scenarios.length >= MAX_SCENARIOS) {
            globalThis.alert(i18n.maxScenarios);
            return;
        }
        const activeScenario = getActiveScenario();
        if (!activeScenario) {
            return;
        }
        const duplicate = normalizeScenario(Object.assign(Object.assign({}, activeScenario), { id: createId(), name: `${activeScenario.name} Copy` }), state.scenarios.length + 1);
        state.scenarios = [...state.scenarios, duplicate];
        state.activeScenarioId = duplicate.id;
        saveState();
        render();
    }
    function handleDeleteScenario() {
        if (state.scenarios.length <= 1) {
            return;
        }
        if (!globalThis.confirm(i18n.deleteConfirm)) {
            return;
        }
        state.scenarios = state.scenarios.filter((scenario) => scenario.id !== state.activeScenarioId);
        state.activeScenarioId = state.scenarios[0].id;
        saveState();
        render();
    }
    function handleResetScenarios() {
        try {
            localStorage.removeItem(STORAGE_KEY);
        }
        catch (error) {
            console.warn("stockSimulator reset failed", error);
        }
        state = createDefaultState();
        saveState();
        render();
    }
    function handleFormChange(event) {
        var _a;
        if (hydratingForm) {
            return;
        }
        const activeScenario = getActiveScenario();
        if (!activeScenario) {
            return;
        }
        const field = (_a = event.target) === null || _a === void 0 ? void 0 : _a.name;
        if (!field) {
            return;
        }
        const nextScenarios = state.scenarios.map((scenario, index) => {
            if (scenario.id !== activeScenario.id) {
                return scenario;
            }
            return applyScenarioFieldChange(scenario, index + 1, field, event.target);
        });
        state.scenarios = nextScenarios;
        saveState();
        render({ skipForm: true });
    }
    function applyScenarioFieldChange(scenario, index, field, target) {
        const nextScenario = Object.assign({}, scenario);
        if (field === "name") {
            nextScenario.name = normalizeName(target.value, index);
            return normalizeScenario(nextScenario, index);
        }
        if (field === "reinvestDividends") {
            nextScenario.reinvestDividends = Boolean(target.checked);
            return normalizeScenario(nextScenario, index);
        }
        if (field === "years") {
            nextScenario.years = Math.round(clampNumber(target.value, 1, MAX_SIMULATION_YEARS, scenario.years));
            return normalizeScenario(nextScenario, index);
        }
        nextScenario[field] = normalizeScenarioNumericField(field, target.value, scenario[field]);
        return normalizeScenario(nextScenario, index);
    }
    function normalizeScenarioNumericField(field, value, fallback) {
        if (field === "principal") {
            return clampNumber(value, 10000, 100000000000, fallback);
        }
        if (field === "currentPrice") {
            return clampNumber(value, 1, 1000000000, fallback);
        }
        if (field === "annualSpending") {
            return clampNumber(value, 0, 100000000000, fallback);
        }
        if (field === "annualPriceGrowthPct" ||
            field === "annualDividendGrowthPct" ||
            field === "annualSpendingGrowthPct") {
            return clampNumber(value, -100, 300, fallback);
        }
        if (field === "dividendYieldPct") {
            return clampNumber(value, 0, 100, fallback);
        }
        return clampNumber(value, -100, 100000000000, fallback);
    }
    function buildSimulationMap() {
        return new Map(state.scenarios.map((scenario) => [
            scenario.id,
            simulateScenario(scenario),
        ]));
    }
    function simulateScenario(scenario) {
        const principal = scenario.principal;
        const growthRates = {
            annualPriceGrowthRate: scenario.annualPriceGrowthPct / 100,
            dividendGrowthRate: scenario.annualDividendGrowthPct / 100,
            annualSpendingGrowthRate: scenario.annualSpendingGrowthPct / 100,
        };
        let state = createSimulationState(scenario, principal);
        let firstDeficitYear = null;
        let firstWealthDeclineYear = null;
        let firstShareSaleYear = null;
        let depletionYear = null;
        const years = [buildInitialSimulationRecord(principal, state)];
        for (let year = 1; year <= scenario.years; year += 1) {
            const previousRecord = years.at(-1);
            const result = simulateYear(year, state, scenario.reinvestDividends, principal, growthRates);
            years.push(result.record);
            state = result.nextState;
            if (firstDeficitYear === null && result.hadDeficit) {
                firstDeficitYear = year;
            }
            if (firstWealthDeclineYear === null &&
                previousRecord &&
                result.record.totalWealth < previousRecord.totalWealth) {
                firstWealthDeclineYear = year;
            }
            if (firstShareSaleYear === null &&
                result.record.soldSharesForSpending > 0) {
                firstShareSaleYear = year;
            }
            if (depletionYear === null && result.depleted) {
                depletionYear = year;
            }
            if (result.depleted) {
                break;
            }
        }
        const finalYear = years.at(-1);
        return {
            records: years,
            summary: {
                sustainableYears: depletionYear || scenario.years,
                firstDeficitYear,
                firstWealthDeclineYear,
                firstShareSaleYear,
                depletionYear,
                finalWealth: finalYear.totalWealth,
                finalSpendingCoveragePct: finalYear.spendingCoveragePct,
            },
        };
    }
    function createSimulationState(scenario, principal) {
        const sharePrice = scenario.currentPrice;
        const shares = sharePrice > 0 ? Math.floor(principal / sharePrice) : 0;
        const cashReserve = principal - shares * sharePrice;
        return {
            sharePrice,
            shares,
            annualDividendPerShare: sharePrice * (scenario.dividendYieldPct / 100),
            annualSpending: scenario.annualSpending,
            cumulativeDividends: 0,
            cashReserve,
        };
    }
    function buildInitialSimulationRecord(principal, state) {
        return {
            year: 0,
            sharePrice: state.sharePrice,
            shares: state.shares,
            soldSharesForSpending: 0,
            reinvestedShares: 0,
            annualDividend: 0,
            annualSpending: 0,
            annualGap: 0,
            spendingCoveragePct: null,
            netDividendAfterSpending: 0,
            cumulativeDividends: 0,
            cashReserve: state.cashReserve,
            marketValue: state.shares * state.sharePrice,
            totalWealth: state.shares * state.sharePrice + state.cashReserve,
            principalReturnPct: 0,
            yieldOnCostPct: 0,
        };
    }
    function simulateYear(year, state, reinvestDividends, principal, growthRates) {
        const annualDividend = state.shares * state.annualDividendPerShare;
        const plannedAnnualSpending = state.annualSpending;
        const netDividendAfterSpending = annualDividend - plannedAnnualSpending;
        const sharePrice = state.sharePrice * (1 + growthRates.annualPriceGrowthRate);
        const settledCashFlow = settleCashFlow({
            sharePrice,
            shares: state.shares,
            cashReserve: state.cashReserve + netDividendAfterSpending,
            reinvestDividends,
        });
        const cumulativeDividends = state.cumulativeDividends + annualDividend;
        const marketValue = settledCashFlow.shares * sharePrice;
        const totalWealth = marketValue + settledCashFlow.cashReserve;
        return {
            hadDeficit: netDividendAfterSpending < 0,
            depleted: totalWealth <= 0,
            record: {
                year,
                sharePrice,
                shares: settledCashFlow.shares,
                soldSharesForSpending: settledCashFlow.soldSharesForSpending,
                reinvestedShares: settledCashFlow.reinvestedShares,
                annualDividend,
                annualSpending: plannedAnnualSpending,
                annualGap: netDividendAfterSpending,
                spendingCoveragePct: plannedAnnualSpending > 0
                    ? (annualDividend * 100) / plannedAnnualSpending
                    : null,
                netDividendAfterSpending,
                cumulativeDividends,
                cashReserve: settledCashFlow.cashReserve,
                marketValue,
                totalWealth,
                principalReturnPct: principal > 0 ? ((totalWealth - principal) * 100) / principal : 0,
                yieldOnCostPct: principal > 0 ? (annualDividend * 100) / principal : 0,
            },
            nextState: {
                sharePrice,
                shares: settledCashFlow.shares,
                annualDividendPerShare: state.annualDividendPerShare * (1 + growthRates.dividendGrowthRate),
                annualSpending: state.annualSpending * (1 + growthRates.annualSpendingGrowthRate),
                cumulativeDividends,
                cashReserve: settledCashFlow.cashReserve,
            },
        };
    }
    function settleCashFlow({ sharePrice, shares, cashReserve, reinvestDividends, }) {
        let nextShares = shares;
        let nextCashReserve = cashReserve;
        let soldSharesForSpending = 0;
        if (nextCashReserve < 0 && sharePrice > 0 && nextShares > 0) {
            const requiredCash = -nextCashReserve;
            soldSharesForSpending = Math.min(nextShares, Math.ceil(requiredCash / sharePrice));
            nextShares -= soldSharesForSpending;
            nextCashReserve += soldSharesForSpending * sharePrice;
        }
        let reinvestedShares = 0;
        if (reinvestDividends && nextCashReserve > 0 && sharePrice > 0) {
            reinvestedShares = Math.floor(nextCashReserve / sharePrice);
            nextShares += reinvestedShares;
            nextCashReserve -= reinvestedShares * sharePrice;
        }
        return {
            shares: nextShares,
            cashReserve: nextCashReserve,
            soldSharesForSpending,
            reinvestedShares,
        };
    }
    function render(options = {}) {
        const simulations = buildSimulationMap();
        if (!options.skipForm) {
            renderForm();
        }
        renderFieldPreviews();
        renderScenarioList(simulations);
        renderSummaryCards(simulations);
        renderCashFlowChart(simulations);
        renderAssetChart(simulations);
        renderTimelineComparison(simulations);
        renderYearlyTable(simulations);
    }
    function renderForm() {
        const activeScenario = getActiveScenario();
        if (!activeScenario || !elements.form) {
            return;
        }
        hydratingForm = true;
        elements.form.elements.name.value = activeScenario.name;
        elements.form.elements.principal.value = activeScenario.principal;
        elements.form.elements.currentPrice.value = activeScenario.currentPrice;
        elements.form.elements.annualPriceGrowthPct.value =
            activeScenario.annualPriceGrowthPct;
        elements.form.elements.dividendYieldPct.value =
            activeScenario.dividendYieldPct;
        elements.form.elements.annualDividendGrowthPct.value =
            activeScenario.annualDividendGrowthPct;
        elements.form.elements.annualSpending.value = activeScenario.annualSpending;
        elements.form.elements.annualSpendingGrowthPct.value =
            activeScenario.annualSpendingGrowthPct;
        elements.form.elements.years.value = activeScenario.years;
        elements.form.elements.reinvestDividends.checked =
            activeScenario.reinvestDividends;
        hydratingForm = false;
    }
    function renderFieldPreviews() {
        var _a, _b, _c;
        if (!elements.form) {
            return;
        }
        if (elements.principalPreview) {
            elements.principalPreview.textContent = formatInputPreview((_a = elements.form.elements.principal) === null || _a === void 0 ? void 0 : _a.value);
        }
        if (elements.currentPricePreview) {
            elements.currentPricePreview.textContent = formatInputPreview((_b = elements.form.elements.currentPrice) === null || _b === void 0 ? void 0 : _b.value);
        }
        if (elements.annualSpendingPreview) {
            elements.annualSpendingPreview.textContent = formatInputPreview((_c = elements.form.elements.annualSpending) === null || _c === void 0 ? void 0 : _c.value);
        }
    }
    function renderScenarioList(simulations) {
        if (!elements.scenarioList) {
            return;
        }
        if (!state.scenarios.length) {
            elements.scenarioList.innerHTML = `
				<div class="rounded-2xl border border-dashed border-base-300 bg-base-100 px-4 py-5 text-sm text-base-content/60">
					${i18n.emptyScenario}
				</div>`;
            return;
        }
        elements.scenarioList.innerHTML = state.scenarios
            .map((scenario, index) => {
            const simulation = simulations.get(scenario.id);
            const summary = simulation ? simulation.summary : null;
            const active = scenario.id === state.activeScenarioId;
            return `
					<button type="button"
						class="w-full rounded-2xl border p-4 text-left transition ${active
                ? "border-primary bg-primary/5 shadow-sm"
                : "border-base-300 bg-base-100 hover:border-base-content/20 hover:bg-base-200/40"}"
						data-scenario-id="${scenario.id}">
						<div class="flex items-start justify-between gap-3">
							<div class="flex items-start gap-3">
								<span class="mt-1 h-3 w-3 rounded-full ${COLOR_CLASSES[index % COLOR_CLASSES.length]}"></span>
								<div class="space-y-1">
									<p class="font-semibold text-base-content">${escapeHtml(scenario.name)}</p>
									<p class="text-xs text-base-content/55">${scenario.years}y · ${scenario.reinvestDividends
                ? i18n.reinvestOn
                : i18n.reinvestOff}</p>
								</div>
							</div>
							${active
                ? `<span class="badge badge-primary badge-sm">${i18n.activeScenario}</span>`
                : ""}
						</div>
						<div class="mt-4 grid grid-cols-2 gap-3 text-sm">
							<div>
								<p class="text-xs text-base-content/50">${i18n.summarySustainablePeriod}</p>
								<p class="mt-1 font-semibold text-base-content">${summary ? formatSustainablePeriod(summary, scenario.years) : "-"}</p>
							</div>
							<div>
								<p class="text-xs text-base-content/50">${i18n.summaryDeficitStart}</p>
								<p class="mt-1 font-semibold text-base-content">${summary ? formatOptionalYear(summary.firstDeficitYear, i18n.summaryNoDeficit) : "-"}</p>
							</div>
						</div>
					</button>`;
        })
            .join("");
        Array.from(elements.scenarioList.querySelectorAll("[data-scenario-id]")).forEach((button) => {
            button.addEventListener("click", () => {
                state.activeScenarioId = button.dataset.scenarioId;
                saveState();
                render();
            });
        });
    }
    function renderSummaryCards(simulations) {
        if (!elements.summaryCards) {
            return;
        }
        const activeScenario = getActiveScenario();
        const simulation = activeScenario
            ? simulations.get(activeScenario.id)
            : null;
        const summary = simulation ? simulation.summary : null;
        if (!summary) {
            elements.summaryCards.innerHTML = "";
            return;
        }
        const cards = [
            {
                label: i18n.summarySustainablePeriod,
                value: formatSustainablePeriod(summary, activeScenario.years),
                note: summary.depletionYear
                    ? `${i18n.summaryDepletionYear}: ${formatYearOffset(summary.depletionYear)}`
                    : i18n.summaryWithinHorizon,
            },
            {
                label: i18n.summaryDeficitStart,
                value: formatOptionalYear(summary.firstDeficitYear, i18n.summaryNoDeficit),
            },
            {
                label: i18n.summaryWealthDeclineStart,
                value: formatOptionalYear(summary.firstWealthDeclineYear, i18n.summaryNoWealthDecline),
            },
            {
                label: i18n.summaryCapitalDrawdownStart,
                value: formatOptionalYear(summary.firstShareSaleYear, i18n.summaryNoPrincipalDrawdown),
            },
            {
                label: i18n.summaryDepletionYear,
                value: formatDepletionValue(summary.depletionYear),
                note: summary.firstShareSaleYear
                    ? `${i18n.summaryCapitalDrawdownStart}: ${formatYearOffset(summary.firstShareSaleYear)}`
                    : i18n.summaryNoPrincipalDrawdown,
            },
        ];
        elements.summaryCards.innerHTML = cards
            .map((card) => `
					<div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
						<p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${card.label}</p>
						<p class="mt-3 text-2xl font-semibold text-base-content">${card.value}</p>
						<p class="mt-2 text-sm text-base-content/60">${escapeHtml(card.note || activeScenario.name)}</p>
					</div>`)
            .join("");
    }
    function renderTimelineComparison(simulations) {
        if (!elements.timelineList) {
            return;
        }
        const maxYears = state.scenarios.reduce((accumulator, scenario) => Math.max(accumulator, scenario.years), 1);
        const bestScenarioIds = buildBestScenarioSet(simulations);
        elements.timelineList.innerHTML = state.scenarios
            .map((scenario) => {
            const simulation = simulations.get(scenario.id);
            const summary = simulation.summary;
            const active = scenario.id === state.activeScenarioId;
            const isBestChoice = state.scenarios.length > 1 && bestScenarioIds.has(scenario.id);
            const segments = buildTimelineSegments(simulation.records, scenario.years, maxYears);
            const markers = buildTimelineMarkers(summary, scenario.years, maxYears);
            return `
					<div
						class="rounded-2xl border px-4 py-4 transition cursor-pointer ${active
                ? "border-primary bg-primary/5 shadow-sm"
                : "border-base-300 bg-base-100 hover:border-base-content/20"}"
						data-scenario-id="${scenario.id}">
						<div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
							<div class="space-y-1">
								<div class="flex flex-wrap items-center gap-2">
									<p class="font-semibold text-base-content">${escapeHtml(scenario.name)}</p>
									${isBestChoice
                ? `<span class="badge badge-success badge-sm">${escapeHtml(i18n.compareBestChoice)}</span>`
                : ""}
								</div>
								<p class="text-xs text-base-content/55">${scenario.years}y · ${scenario.reinvestDividends
                ? i18n.reinvestOn
                : i18n.reinvestOff}</p>
								<div class="flex flex-wrap gap-2 text-[11px] leading-5 text-base-content/70">
									<span class="rounded-full border border-base-300 bg-base-200/70 px-2 py-1">${escapeHtml(i18n.summaryLatestCoverage)} ${escapeHtml(formatCoveragePercent(summary.finalSpendingCoveragePct))}</span>
									<span class="rounded-full border border-base-300 bg-base-200/70 px-2 py-1">${escapeHtml(i18n.summaryFinalWealth)} ${escapeHtml(formatCompactCurrency(summary.finalWealth))}</span>
								</div>
								<div class="mt-2 flex flex-wrap gap-2 text-[11px] leading-5 text-base-content/70">
									${buildScenarioConfigurationBadges(scenario)}
								</div>
							</div>
							<div class="text-sm font-medium text-base-content/70">${formatSustainablePeriod(summary, scenario.years)}</div>
						</div>
						<div class="mt-4 space-y-3">
							<div class="relative h-4 overflow-hidden rounded-full bg-slate-100 ring-1 ring-slate-200">
								${segments
                .map((segment) => `
											<span
												class="absolute inset-y-0 ${segment.className}"
												style="left: ${segment.leftPct}%; width: ${segment.widthPct}%;"></span>`)
                .join("")}
								${markers
                .map((marker) => `
											<span
												class="absolute inset-y-0 w-px bg-base-content/60"
												style="left: ${marker.leftPct}%;"></span>`)
                .join("")}
							</div>
							<div class="flex items-center justify-between text-[11px] text-base-content/50">
								<span>0y</span>
								<span>${scenario.years}y</span>
							</div>
							<div class="flex flex-wrap gap-2 text-xs text-base-content/70">
								${buildTimelineEventBadges(summary)}
							</div>
						</div>
					</div>`;
        })
            .join("");
        bindScenarioSelection(elements.timelineList);
    }
    function buildBestScenarioSet(simulations) {
        const entries = state.scenarios
            .map((scenario) => {
            var _a;
            return ({
                scenarioId: scenario.id,
                values: buildComparisonValues((_a = simulations.get(scenario.id)) === null || _a === void 0 ? void 0 : _a.summary, scenario.years),
            });
        })
            .sort(compareComparisonEntries);
        if (!entries.length) {
            return new Set();
        }
        const bestEntry = entries[0];
        return new Set(entries
            .filter((entry) => compareComparisonEntries(entry, bestEntry) === 0)
            .map((entry) => entry.scenarioId));
    }
    function buildComparisonValues(summary, simulationYears) {
        return {
            sustainableYears: Number((summary === null || summary === void 0 ? void 0 : summary.sustainableYears) || 0),
            drawdownStartYear: normalizeComparisonYear(summary === null || summary === void 0 ? void 0 : summary.firstShareSaleYear, simulationYears),
            finalCoveragePct: normalizeComparisonCoverage(summary === null || summary === void 0 ? void 0 : summary.finalSpendingCoveragePct),
            finalWealth: Number((summary === null || summary === void 0 ? void 0 : summary.finalWealth) || 0),
            firstDeficitYear: normalizeComparisonYear(summary === null || summary === void 0 ? void 0 : summary.firstDeficitYear, simulationYears),
            firstWealthDeclineYear: normalizeComparisonYear(summary === null || summary === void 0 ? void 0 : summary.firstWealthDeclineYear, simulationYears),
        };
    }
    function normalizeComparisonYear(year, simulationYears) {
        return year ? Number(year) : simulationYears + 1;
    }
    function normalizeComparisonCoverage(value) {
        if (value === null || value === undefined) {
            return Number.POSITIVE_INFINITY;
        }
        return Number(value);
    }
    function compareComparisonEntries(left, right) {
        return (compareDescendingNumber(left.values.sustainableYears, right.values.sustainableYears) ||
            compareDescendingNumber(left.values.drawdownStartYear, right.values.drawdownStartYear) ||
            compareDescendingNumber(left.values.finalCoveragePct, right.values.finalCoveragePct) ||
            compareDescendingNumber(left.values.finalWealth, right.values.finalWealth) ||
            compareDescendingNumber(left.values.firstDeficitYear, right.values.firstDeficitYear) ||
            compareDescendingNumber(left.values.firstWealthDeclineYear, right.values.firstWealthDeclineYear));
    }
    function compareDescendingNumber(left, right) {
        if (left === right) {
            return 0;
        }
        return left > right ? -1 : 1;
    }
    function renderYearlyTable(simulations) {
        if (!elements.yearlyTableBody) {
            return;
        }
        const activeScenario = getActiveScenario();
        const simulation = activeScenario
            ? simulations.get(activeScenario.id)
            : null;
        const yearlyRecords = (simulation === null || simulation === void 0 ? void 0 : simulation.records.slice(1)) || [];
        if (!yearlyRecords.length) {
            elements.yearlyTableBody.innerHTML = `
				<tr>
					<td colspan="10" class="py-8 text-center text-sm text-base-content/60">${i18n.emptyTable}</td>
				</tr>`;
            return;
        }
        const firstDeficitYear = simulation.summary.firstDeficitYear;
        const firstWealthDeclineYear = simulation.summary.firstWealthDeclineYear;
        elements.yearlyTableBody.innerHTML = yearlyRecords
            .map((record) => `
					<tr class="${resolveYearlyRowClass(record, firstDeficitYear, firstWealthDeclineYear)}">
						<td class="font-medium">${record.year}</td>
						<td>${formatShares(record.shares)}</td>
						<td>${formatCurrency(record.annualDividend)}</td>
						<td>${formatCurrency(record.annualSpending)}</td>
						<td class="${record.spendingCoveragePct !== null && record.spendingCoveragePct < 100 ? "font-semibold text-amber-700" : ""}">${formatCoveragePercent(record.spendingCoveragePct)}</td>
						<td class="${record.annualGap < 0 ? "font-semibold text-red-700" : "text-success"}">${formatCurrency(record.annualGap)}</td>
						<td>${formatShares(record.soldSharesForSpending)}</td>
						<td>${formatCurrency(record.cashReserve)}</td>
						<td>${formatCurrency(record.marketValue)}</td>
						<td>${formatCurrency(record.totalWealth)}</td>
					</tr>`)
            .join("");
    }
    function renderCashFlowChart(simulations) {
        var _a;
        if (!elements.cashFlowChartCanvas || typeof Chart === "undefined") {
            return;
        }
        const activeScenario = getActiveScenario();
        const records = activeScenario
            ? ((_a = simulations.get(activeScenario.id)) === null || _a === void 0 ? void 0 : _a.records) || []
            : [];
        const labels = records.map((record) => record.year);
        const datasets = [
            buildLineDataset(i18n.seriesAnnualDividend, records.map((record) => record.annualDividend), "#059669", "rgba(5, 150, 105, 0.15)"),
            buildLineDataset(i18n.seriesAnnualSpending, records.map((record) => record.annualSpending), "#dc2626", "rgba(220, 38, 38, 0.14)"),
            buildLineDataset(i18n.seriesAnnualGap, records.map((record) => record.annualGap), "#d97706", "rgba(217, 119, 6, 0.14)", [6, 4]),
        ];
        cashFlowChart = upsertCurrencyChart(cashFlowChart, elements.cashFlowChartCanvas, labels, datasets, true);
    }
    function renderAssetChart(simulations) {
        var _a;
        if (!elements.assetChartCanvas || typeof Chart === "undefined") {
            return;
        }
        const activeScenario = getActiveScenario();
        const records = activeScenario
            ? ((_a = simulations.get(activeScenario.id)) === null || _a === void 0 ? void 0 : _a.records) || []
            : [];
        const labels = records.map((record) => record.year);
        const datasets = [
            buildLineDataset(i18n.seriesTotalWealth, records.map((record) => record.totalWealth), "#2563eb", "rgba(37, 99, 235, 0.15)"),
            buildLineDataset(i18n.seriesMarketValue, records.map((record) => record.marketValue), "#7c3aed", "rgba(124, 58, 237, 0.14)"),
            buildLineDataset(i18n.seriesCashReserve, records.map((record) => record.cashReserve), "#0f766e", "rgba(15, 118, 110, 0.12)", [4, 4]),
        ];
        assetChart = upsertCurrencyChart(assetChart, elements.assetChartCanvas, labels, datasets, true);
    }
    function buildLineDataset(label, data, borderColor, backgroundColor, borderDash) {
        return {
            label,
            data,
            borderColor,
            backgroundColor,
            borderWidth: 2,
            pointRadius: 0,
            pointHoverRadius: 4,
            tension: 0.2,
            fill: false,
            borderDash,
        };
    }
    function upsertCurrencyChart(existingChart, canvas, labels, datasets, allowNegative) {
        if (!existingChart) {
            return new Chart(canvas, {
                type: "line",
                data: { labels, datasets },
                options: buildCurrencyChartOptions(allowNegative),
            });
        }
        existingChart.data.labels = labels;
        existingChart.data.datasets = datasets;
        existingChart.options.scales.y.min = allowNegative ? undefined : 0;
        existingChart.update();
        return existingChart;
    }
    function buildCurrencyChartOptions(allowNegative) {
        return {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: "index",
                intersect: false,
            },
            plugins: {
                legend: {
                    position: "bottom",
                },
                tooltip: {
                    callbacks: {
                        label(context) {
                            const value = Number(context.parsed.y || 0);
                            return `${context.dataset.label}: ${formatCurrency(value)}`;
                        },
                    },
                },
            },
            scales: {
                x: {
                    title: {
                        display: true,
                        text: "Year",
                    },
                },
                y: {
                    min: allowNegative ? undefined : 0,
                    ticks: {
                        callback(value) {
                            return formatCompactCurrency(Number(value));
                        },
                    },
                },
            },
        };
    }
    function bindScenarioSelection(container) {
        if (!container) {
            return;
        }
        Array.from(container.querySelectorAll("[data-scenario-id]")).forEach((element) => {
            element.addEventListener("click", () => {
                const scenarioId = element.dataset.scenarioId;
                if (!scenarioId || scenarioId === state.activeScenarioId) {
                    return;
                }
                state.activeScenarioId = scenarioId;
                saveState();
                render();
            });
        });
    }
    function buildTimelineSegments(records, scenarioYears, maxYears) {
        const yearlyRecords = records.slice(1);
        if (!yearlyRecords.length) {
            return [
                {
                    leftPct: 0,
                    widthPct: toTimelinePercent(scenarioYears, maxYears),
                    className: "bg-success",
                },
            ];
        }
        const segments = [];
        let currentStage = resolveTimelineStage(yearlyRecords[0], records[0]);
        let segmentStart = 0;
        for (let index = 1; index < yearlyRecords.length; index += 1) {
            const record = yearlyRecords[index];
            const previousRecord = records[index];
            const nextStage = resolveTimelineStage(record, previousRecord);
            if (nextStage === currentStage) {
                continue;
            }
            segments.push(createTimelineSegment(segmentStart, record.year, currentStage, maxYears));
            currentStage = nextStage;
            segmentStart = record.year;
        }
        segments.push(createTimelineSegment(segmentStart, scenarioYears, currentStage, maxYears));
        return segments.filter((segment) => segment.widthPct > 0);
    }
    function createTimelineSegment(startYear, endYear, stage, maxYears) {
        return {
            leftPct: toTimelinePercent(startYear, maxYears),
            widthPct: toTimelinePercent(endYear - startYear, maxYears),
            className: resolveTimelineStageClass(stage),
        };
    }
    function buildTimelineMarkers(summary, scenarioYears, maxYears) {
        return [
            summary.firstDeficitYear,
            summary.firstShareSaleYear,
            summary.firstWealthDeclineYear,
            summary.depletionYear,
        ]
            .filter((year, index, values) => year && year <= scenarioYears && values.indexOf(year) === index)
            .map((year) => ({
            leftPct: toTimelinePercent(year, maxYears),
        }));
    }
    function buildTimelineEventBadges(summary) {
        const badges = [];
        if (summary.firstDeficitYear) {
            badges.push(buildTimelineEventBadge(i18n.timelinePhaseDeficit, summary.firstDeficitYear, "bg-amber-50 text-amber-700"));
        }
        if (summary.firstShareSaleYear) {
            badges.push(buildTimelineEventBadge(i18n.timelinePhaseDrawdown, summary.firstShareSaleYear, "bg-orange-50 text-orange-700"));
        }
        if (summary.firstWealthDeclineYear) {
            badges.push(buildTimelineEventBadge(i18n.timelinePhaseWealthDecline, summary.firstWealthDeclineYear, "bg-yellow-50 text-yellow-700"));
        }
        if (summary.depletionYear) {
            badges.push(buildTimelineEventBadge(i18n.summaryDepletionYear, summary.depletionYear, "bg-red-50 text-red-700"));
        }
        if (!badges.length) {
            return `<span class="rounded-full bg-emerald-50 px-2 py-1 text-emerald-700">${escapeHtml(i18n.summaryWithinHorizon)}</span>`;
        }
        return badges.join("");
    }
    function buildTimelineEventBadge(label, year, className) {
        return `<span class="rounded-full px-2 py-1 ${className}">${escapeHtml(label)} ${escapeHtml(formatYearOffset(year))}</span>`;
    }
    function resolveTimelineStage(record, previousRecord) {
        if (record.totalWealth <= 0) {
            return "depleted";
        }
        if (record.soldSharesForSpending > 0) {
            return "drawdown";
        }
        if (previousRecord && record.totalWealth < previousRecord.totalWealth) {
            return "wealthDecline";
        }
        if (record.annualGap < 0) {
            return "deficit";
        }
        return "stable";
    }
    function resolveTimelineStageClass(stage) {
        if (stage === "depleted") {
            return "bg-red-500";
        }
        if (stage === "drawdown") {
            return "bg-orange-400";
        }
        if (stage === "wealthDecline") {
            return "bg-yellow-400";
        }
        if (stage === "deficit") {
            return "bg-amber-400";
        }
        return "bg-emerald-400";
    }
    function toTimelinePercent(years, maxYears) {
        if (!maxYears) {
            return 0;
        }
        return (years * 100) / maxYears;
    }
    function formatCurrency(value) {
        const rounded = Math.round(Number(value || 0));
        const formatted = currencyFormatter.format(Math.abs(rounded));
        return rounded < 0 ? `-₩${formatted}` : `₩${formatted}`;
    }
    function formatInputPreview(value) {
        if (value === "" || value === null || value === undefined) {
            return "";
        }
        const number = Number(value);
        if (!Number.isFinite(number)) {
            return "";
        }
        return currencyFormatter.format(Math.round(number));
    }
    function formatCompactCurrency(value) {
        const abs = Math.abs(value || 0);
        const sign = Number(value || 0) < 0 ? "-" : "";
        if (abs >= 100000000) {
            return `${sign}₩${(abs / 100000000).toFixed(1)}억`;
        }
        if (abs >= 10000) {
            return `${sign}₩${(abs / 10000).toFixed(0)}만`;
        }
        return formatCurrency(value);
    }
    function formatSignedPercent(value) {
        const number = Number(value);
        if (!Number.isFinite(number)) {
            return formatPercent(0);
        }
        return `${number > 0 ? "+" : ""}${percentFormatter.format(number)}%`;
    }
    function buildScenarioConfigurationSegments(scenario) {
        return [
            `${i18n.fieldPrincipal} ${formatCompactCurrency(scenario.principal)}`,
            `${i18n.fieldCurrentPrice} ${formatCompactCurrency(scenario.currentPrice)}`,
            `${i18n.fieldDividendYield} ${formatPercent(scenario.dividendYieldPct)}`,
            `${i18n.fieldAnnualDividendGrowth} ${formatSignedPercent(scenario.annualDividendGrowthPct)}`,
            `${i18n.fieldAnnualPriceGrowth} ${formatSignedPercent(scenario.annualPriceGrowthPct)}`,
            `${i18n.fieldAnnualSpending} ${formatCompactCurrency(scenario.annualSpending)}`,
            `${i18n.fieldAnnualSpendingGrowth} ${formatSignedPercent(scenario.annualSpendingGrowthPct)}`,
        ];
    }
    function buildScenarioConfigurationBadges(scenario) {
        return buildScenarioConfigurationSegments(scenario)
            .map((segment) => `<span class="rounded-full border border-base-300 bg-base-200/70 px-2 py-1">${escapeHtml(segment)}</span>`)
            .join("");
    }
    function buildScenarioConfigurationText(scenario) {
        return buildScenarioConfigurationSegments(scenario).join(" · ");
    }
    function formatYearOffset(year) {
        return i18n.summaryYearsLater.replace("{0}", currencyFormatter.format(year || 0));
    }
    function formatOptionalYear(year, emptyLabel) {
        return year ? formatYearOffset(year) : emptyLabel;
    }
    function formatSustainablePeriod(summary, simulationYears) {
        if (summary.depletionYear) {
            return formatYearOffset(summary.depletionYear);
        }
        return i18n.summaryYearsOrMore.replace("{0}", currencyFormatter.format(simulationYears || 0));
    }
    function formatDepletionValue(depletionYear) {
        if (!depletionYear) {
            return i18n.summaryNotDepleted;
        }
        return formatYearOffset(depletionYear);
    }
    function formatPercent(value) {
        return `${percentFormatter.format(value || 0)}%`;
    }
    function formatCoveragePercent(value) {
        const number = Number(value);
        if (!Number.isFinite(number)) {
            return i18n.summaryNoSpending;
        }
        return formatPercent(number);
    }
    function formatShares(value) {
        return shareFormatter.format(value || 0);
    }
    function resolveYearlyRowClass(record, firstDeficitYear, firstWealthDeclineYear) {
        if (record.totalWealth <= 0) {
            return "bg-red-50 text-red-900";
        }
        if (record.soldSharesForSpending > 0) {
            return "bg-orange-50";
        }
        if (firstWealthDeclineYear && record.year >= firstWealthDeclineYear) {
            return "bg-yellow-50";
        }
        if (firstDeficitYear && record.year >= firstDeficitYear) {
            return "bg-amber-50";
        }
        return "";
    }
    function escapeHtml(value) {
        return String(value || "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }
})();
