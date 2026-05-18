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
  const i18nOverrides =
    globalThis.stockSimulatorI18n &&
    typeof globalThis.stockSimulatorI18n === "object"
      ? globalThis.stockSimulatorI18n
      : {};

  const i18n = {
    defaultScenarioName: "Base Scenario",
    activeScenario: "Editing",
    localOnly: "localStorage only",
    reinvestOn: "Reinvest ON",
    reinvestOff: "Cash Dividend",
    emptyScenario: "A default scenario has been created.",
    emptyTable: "No yearly data is available.",
    deleteConfirm: "Delete the current scenario?",
    maxScenarios: "You can compare up to five scenarios at once.",
    summarySustainablePeriod: "Sustainable Period",
    summaryDepletionYear: "Depletion",
    summaryNotDepleted: "Not Depleted",
    summaryDeficitStart: "Deficit Starts",
    summaryNoDeficit: "No Deficit",
    summaryWealthDeclineStart: "Total Wealth Decline Starts",
    summaryNoWealthDecline: "No Wealth Decline",
    summaryCapitalDrawdownStart: "Principal Drawdown Starts",
    summaryNoPrincipalDrawdown: "No Principal Drawdown",
    summaryYearsLater: "In {0} years",
    summaryYearsOrMore: "{0}+ years",
    summaryWithinHorizon: "Sustainable within the simulation horizon",
    summaryLatestCoverage: "Latest Spending Coverage",
    summaryNoSpending: "No Spending",
    metricAnnualGap: "Annual Gap",
    tooltipCoverage: "Coverage",
    ...i18nOverrides,
  };

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
    metricButtons: Array.from(
      document.querySelectorAll("#stockSimulatorMetricSelector [data-metric]"),
    ),
    summaryCards: document.getElementById("stockSimulatorSummaryCards"),
    compareTableBody: document.getElementById("stockSimulatorCompareTableBody"),
    yearlyTableBody: document.getElementById("stockSimulatorYearlyTableBody"),
    chartCanvas: document.getElementById("stockSimulatorChart"),
    principalPreview: document.getElementById("stockSimulatorPrincipalPreview"),
    currentPricePreview: document.getElementById(
      "stockSimulatorCurrentPricePreview",
    ),
    annualSpendingPreview: document.getElementById(
      "stockSimulatorAnnualSpendingPreview",
    ),
  };

  let state = loadState();
  let chart = null;
  let hydratingForm = false;

  bindEvents();
  render();

  function bindEvents() {
    if (elements.addButton) {
      elements.addButton.addEventListener("click", handleAddScenario);
    }
    if (elements.duplicateButton) {
      elements.duplicateButton.addEventListener(
        "click",
        handleDuplicateScenario,
      );
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
    elements.metricButtons.forEach((button) => {
      button.addEventListener("click", () => {
        const metric = button.dataset.metric;
        if (!metric || !METRICS[metric]) {
          return;
        }
        state.metric = metric;
        saveState();
        renderMetricButtons();
        renderChart(buildSimulationMap());
      });
    });
  }

  function loadState() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return createDefaultState();
      }
      const parsed = JSON.parse(raw);
      return sanitizeState(parsed);
    } catch (error) {
      console.warn("stockSimulator load failed", error);
      return createDefaultState();
    }
  }

  function saveState() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch (error) {
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
    const legacyDefaultYears = raw?.version == null;
    const scenarios = Array.isArray(raw?.scenarios)
      ? raw.scenarios
          .slice(0, MAX_SCENARIOS)
          .map((scenario, index) =>
            normalizeScenario(
              migrateLegacyScenario(scenario, legacyDefaultYears),
              index + 1,
            ),
          )
      : [];

    if (!scenarios.length) {
      return createDefaultState();
    }

    const activeScenarioId = scenarios.some(
      (scenario) => scenario.id === raw.activeScenarioId,
    )
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
    if (!shouldExpandYears || scenario?.years !== 20) {
      return scenario;
    }

    return {
      ...scenario,
      years: MAX_SIMULATION_YEARS,
    };
  }

  function normalizeScenario(rawScenario, index) {
    const scenario = rawScenario || {};
    return {
      id:
        typeof scenario.id === "string" && scenario.id
          ? scenario.id
          : createId(),
      name: normalizeName(scenario.name, index),
      principal: clampNumber(
        scenario.principal,
        10000,
        100000000000,
        1000000000,
      ),
      currentPrice: clampNumber(scenario.currentPrice, 1, 1000000000, 100000),
      annualPriceGrowthPct: clampNumber(
        scenario.annualPriceGrowthPct,
        -100,
        300,
        4,
      ),
      dividendYieldPct: clampNumber(scenario.dividendYieldPct, 0, 100, 4),
      annualDividendGrowthPct: clampNumber(
        scenario.annualDividendGrowthPct,
        -100,
        300,
        4,
      ),
      annualSpending: clampNumber(
        scenario.annualSpending,
        0,
        100000000000,
        30000000,
      ),
      annualSpendingGrowthPct: clampNumber(
        scenario.annualSpendingGrowthPct,
        -100,
        300,
        7.2,
      ),
      years: Math.round(
        clampNumber(scenario.years, 1, MAX_SIMULATION_YEARS, 100),
      ),
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
    return normalizeScenario(
      {
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
      },
      index,
    );
  }

  function createId() {
    if (typeof globalThis.crypto?.randomUUID === "function") {
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
    return (
      state.scenarios.find(
        (scenario) => scenario.id === state.activeScenarioId,
      ) || state.scenarios[0]
    );
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

    const duplicate = normalizeScenario(
      {
        ...activeScenario,
        id: createId(),
        name: `${activeScenario.name} Copy`,
      },
      state.scenarios.length + 1,
    );
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

    state.scenarios = state.scenarios.filter(
      (scenario) => scenario.id !== state.activeScenarioId,
    );
    state.activeScenarioId = state.scenarios[0].id;
    saveState();
    render();
  }

  function handleResetScenarios() {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch (error) {
      console.warn("stockSimulator reset failed", error);
    }
    state = createDefaultState();
    saveState();
    render();
  }

  function handleFormChange(event) {
    if (hydratingForm) {
      return;
    }
    const activeScenario = getActiveScenario();
    if (!activeScenario) {
      return;
    }

    const field = event.target?.name;
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
    const nextScenario = { ...scenario };

    if (field === "name") {
      nextScenario.name = normalizeName(target.value, index);
      return normalizeScenario(nextScenario, index);
    }

    if (field === "reinvestDividends") {
      nextScenario.reinvestDividends = Boolean(target.checked);
      return normalizeScenario(nextScenario, index);
    }

    if (field === "years") {
      nextScenario.years = Math.round(
        clampNumber(target.value, 1, MAX_SIMULATION_YEARS, scenario.years),
      );
      return normalizeScenario(nextScenario, index);
    }

    nextScenario[field] = normalizeScenarioNumericField(
      field,
      target.value,
      scenario[field],
    );
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

    if (
      field === "annualPriceGrowthPct" ||
      field === "annualDividendGrowthPct" ||
      field === "annualSpendingGrowthPct"
    ) {
      return clampNumber(value, -100, 300, fallback);
    }

    if (field === "dividendYieldPct") {
      return clampNumber(value, 0, 100, fallback);
    }

    return clampNumber(value, -100, 100000000000, fallback);
  }

  function buildSimulationMap() {
    return new Map(
      state.scenarios.map((scenario) => [
        scenario.id,
        simulateScenario(scenario),
      ]),
    );
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
      const result = simulateYear(
        year,
        state,
        scenario.reinvestDividends,
        principal,
        growthRates,
      );
      years.push(result.record);
      state = result.nextState;

      if (firstDeficitYear === null && result.hadDeficit) {
        firstDeficitYear = year;
      }
      if (
        firstWealthDeclineYear === null &&
        previousRecord &&
        result.record.totalWealth < previousRecord.totalWealth
      ) {
        firstWealthDeclineYear = year;
      }
      if (
        firstShareSaleYear === null &&
        result.record.soldSharesForSpending > 0
      ) {
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

  function simulateYear(
    year,
    state,
    reinvestDividends,
    principal,
    growthRates,
  ) {
    const annualDividend = state.shares * state.annualDividendPerShare;
    const plannedAnnualSpending = state.annualSpending;
    const netDividendAfterSpending = annualDividend - plannedAnnualSpending;
    const sharePrice =
      state.sharePrice * (1 + growthRates.annualPriceGrowthRate);
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
        spendingCoveragePct:
          plannedAnnualSpending > 0
            ? (annualDividend * 100) / plannedAnnualSpending
            : null,
        netDividendAfterSpending,
        cumulativeDividends,
        cashReserve: settledCashFlow.cashReserve,
        marketValue,
        totalWealth,
        principalReturnPct:
          principal > 0 ? ((totalWealth - principal) * 100) / principal : 0,
        yieldOnCostPct: principal > 0 ? (annualDividend * 100) / principal : 0,
      },
      nextState: {
        sharePrice,
        shares: settledCashFlow.shares,
        annualDividendPerShare:
          state.annualDividendPerShare * (1 + growthRates.dividendGrowthRate),
        annualSpending:
          state.annualSpending * (1 + growthRates.annualSpendingGrowthRate),
        cumulativeDividends,
        cashReserve: settledCashFlow.cashReserve,
      },
    };
  }

  function settleCashFlow({
    sharePrice,
    shares,
    cashReserve,
    reinvestDividends,
  }) {
    let nextShares = shares;
    let nextCashReserve = cashReserve;
    let soldSharesForSpending = 0;

    if (nextCashReserve < 0 && sharePrice > 0 && nextShares > 0) {
      const requiredCash = -nextCashReserve;
      soldSharesForSpending = Math.min(
        nextShares,
        Math.ceil(requiredCash / sharePrice),
      );
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
    renderMetricButtons();
    renderSummaryCards(simulations);
    renderComparisonTable(simulations);
    renderYearlyTable(simulations);
    renderChart(simulations);
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
    if (!elements.form) {
      return;
    }

    if (elements.principalPreview) {
      elements.principalPreview.textContent = formatInputPreview(
        elements.form.elements.principal?.value,
      );
    }

    if (elements.currentPricePreview) {
      elements.currentPricePreview.textContent = formatInputPreview(
        elements.form.elements.currentPrice?.value,
      );
    }

    if (elements.annualSpendingPreview) {
      elements.annualSpendingPreview.textContent = formatInputPreview(
        elements.form.elements.annualSpending?.value,
      );
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
            class="w-full rounded-2xl border p-4 text-left transition ${
              active
                ? "border-primary bg-primary/5 shadow-sm"
                : "border-base-300 bg-base-100 hover:border-base-content/20 hover:bg-base-200/40"
            }"
            data-scenario-id="${scenario.id}">
            <div class="flex items-start justify-between gap-3">
              <div class="flex items-start gap-3">
                <span class="mt-1 h-3 w-3 rounded-full ${COLOR_CLASSES[index % COLOR_CLASSES.length]}"></span>
                <div class="space-y-1">
                  <p class="font-semibold text-base-content">${escapeHtml(scenario.name)}</p>
                  <p class="text-xs text-base-content/55">${scenario.years}y · ${
                    scenario.reinvestDividends
                      ? i18n.reinvestOn
                      : i18n.reinvestOff
                  }</p>
                </div>
              </div>
              ${
                active
                  ? `<span class="badge badge-primary badge-sm">${i18n.activeScenario}</span>`
                  : ""
              }
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

    Array.from(
      elements.scenarioList.querySelectorAll("[data-scenario-id]"),
    ).forEach((button) => {
      button.addEventListener("click", () => {
        state.activeScenarioId = button.dataset.scenarioId;
        saveState();
        render();
      });
    });
  }

  function renderMetricButtons() {
    elements.metricButtons.forEach((button) => {
      const active = button.dataset.metric === state.metric;
      button.classList.toggle("btn-primary", active);
      button.classList.toggle("btn-outline", !active);
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
    const finalRecord = simulation?.records?.at(-1);
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
        value: formatOptionalYear(
          summary.firstWealthDeclineYear,
          i18n.summaryNoWealthDecline,
        ),
      },
      {
        label: i18n.summaryCapitalDrawdownStart,
        value: formatOptionalYear(
          summary.firstShareSaleYear,
          i18n.summaryNoPrincipalDrawdown,
        ),
      },
      {
        label: i18n.summaryDepletionYear,
        value: formatDepletionValue(summary.depletionYear),
        note: summary.firstShareSaleYear
          ? `${i18n.summaryCapitalDrawdownStart}: ${formatYearOffset(summary.firstShareSaleYear)}`
          : i18n.summaryNoPrincipalDrawdown,
      },
      {
        label: i18n.summaryLatestCoverage,
        value: formatCoveragePercent(summary.finalSpendingCoveragePct),
        note: finalRecord
          ? `${formatCurrency(finalRecord.annualDividend)} / ${formatCurrency(finalRecord.annualSpending)}`
          : activeScenario.name,
      },
    ];

    elements.summaryCards.innerHTML = cards
      .map(
        (card) => `
          <div class="rounded-2xl border border-base-300 bg-base-100 px-4 py-4 shadow-sm">
            <p class="text-xs font-medium uppercase tracking-wide text-base-content/50">${card.label}</p>
            <p class="mt-3 text-2xl font-semibold text-base-content">${card.value}</p>
            <p class="mt-2 text-sm text-base-content/60">${escapeHtml(card.note || activeScenario.name)}</p>
          </div>`,
      )
      .join("");
  }

  function renderComparisonTable(simulations) {
    if (!elements.compareTableBody) {
      return;
    }

    elements.compareTableBody.innerHTML = state.scenarios
      .map((scenario) => {
        const summary = simulations.get(scenario.id).summary;
        const active = scenario.id === state.activeScenarioId;
        return `
          <tr class="${active ? "bg-primary/5" : ""}">
            <td class="font-medium">${escapeHtml(scenario.name)}</td>
            <td>${formatSustainablePeriod(summary, scenario.years)}</td>
            <td>${formatOptionalYear(summary.firstDeficitYear, i18n.summaryNoDeficit)}</td>
            <td>${formatOptionalYear(summary.firstShareSaleYear, i18n.summaryNoPrincipalDrawdown)}</td>
            <td>${formatDepletionValue(summary.depletionYear)}</td>
            <td>${formatCoveragePercent(summary.finalSpendingCoveragePct)}</td>
          </tr>`;
      })
      .join("");
  }

  function renderYearlyTable(simulations) {
    if (!elements.yearlyTableBody) {
      return;
    }

    const activeScenario = getActiveScenario();
    const simulation = activeScenario
      ? simulations.get(activeScenario.id)
      : null;
    const yearlyRecords = simulation?.records.slice(1) || [];
    if (!yearlyRecords.length) {
      elements.yearlyTableBody.innerHTML = `
        <tr>
          <td colspan="10" class="py-8 text-center text-sm text-base-content/60">${i18n.emptyTable}</td>
        </tr>`;
      return;
    }

    const firstDeficitYear = simulation.summary.firstDeficitYear;
    elements.yearlyTableBody.innerHTML = yearlyRecords
      .map(
        (record) => `
          <tr class="${resolveYearlyRowClass(record, firstDeficitYear)}">
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
          </tr>`,
      )
      .join("");
  }

  function renderChart(simulations) {
    if (!elements.chartCanvas || typeof Chart === "undefined") {
      return;
    }

    const maxYears = state.scenarios.reduce(
      (accumulator, scenario) =>
        Math.max(
          accumulator,
          simulations.get(scenario.id)?.records.at(-1)?.year || 0,
        ),
      0,
    );
    const labels = Array.from({ length: maxYears + 1 }, (_, index) => index);
    const metricKey = METRICS[state.metric] || METRICS.spendingCoveragePct;
    const metricType = resolveMetricType(metricKey);

    const datasets = state.scenarios.map((scenario, index) => {
      const records = simulations.get(scenario.id).records;
      const color = CHART_COLORS[index % CHART_COLORS.length];
      return {
        label: scenario.name,
        scenarioId: scenario.id,
        data: labels.map((yearIndex) => {
          const record = records[yearIndex];
          return record ? record[metricKey] : null;
        }),
        borderColor: color.border,
        backgroundColor: color.background,
        borderWidth: 2,
        pointRadius: 0,
        pointHoverRadius: 4,
        tension: 0.2,
      };
    });

    if (!chart) {
      chart = new Chart(elements.chartCanvas, {
        type: "line",
        data: { labels, datasets },
        options: {
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
                  const prefix = `${context.dataset.label}: `;
                  return prefix + formatMetricValue(value, metricType);
                },
                afterLabel(context) {
                  return buildCoverageTooltip(context, simulations);
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
              min: resolveMetricMin(metricKey),
              ticks: {
                callback(value) {
                  return formatCompactMetricValue(Number(value), metricType);
                },
              },
            },
          },
        },
      });
      return;
    }

    chart.data.labels = labels;
    chart.data.datasets = datasets;
    chart.options.plugins.tooltip.callbacks.label = function label(context) {
      const value = Number(context.parsed.y || 0);
      const prefix = `${context.dataset.label}: `;
      return prefix + formatMetricValue(value, metricType);
    };
    chart.options.plugins.tooltip.callbacks.afterLabel = function afterLabel(
      context,
    ) {
      return buildCoverageTooltip(context, simulations);
    };
    chart.options.scales.y.ticks.callback = function callback(value) {
      return formatCompactMetricValue(Number(value), metricType);
    };
    chart.options.scales.y.min = resolveMetricMin(metricKey);
    chart.update();
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

  function buildCoverageTooltip(context, simulations) {
    const simulation = simulations.get(context.dataset.scenarioId);
    const record = simulation?.records?.[context.dataIndex];
    if (!record) {
      return "";
    }

    return [
      `${i18n.tooltipCoverage}: ${formatCoveragePercent(record.spendingCoveragePct)}`,
      `${i18n.metricAnnualGap}: ${formatCurrency(record.annualGap)}`,
    ];
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

  function formatYearOffset(year) {
    return i18n.summaryYearsLater.replace(
      "{0}",
      currencyFormatter.format(year || 0),
    );
  }

  function formatOptionalYear(year, emptyLabel) {
    return year ? formatYearOffset(year) : emptyLabel;
  }

  function formatSustainablePeriod(summary, simulationYears) {
    if (summary.depletionYear) {
      return formatYearOffset(summary.depletionYear);
    }

    return i18n.summaryYearsOrMore.replace(
      "{0}",
      currencyFormatter.format(simulationYears || 0),
    );
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

  function formatSignedPercent(value) {
    const number = Number(value || 0);
    const sign = number >= 0 ? "+" : "-";
    return `${sign}${percentFormatter.format(Math.abs(number))}%`;
  }

  function formatShares(value) {
    return shareFormatter.format(value || 0);
  }

  function formatCompactShares(value) {
    const abs = Math.abs(value || 0);
    if (abs >= 1000) {
      return `${(value / 1000).toFixed(1)}k`;
    }
    return formatShares(value);
  }

  function formatCompactPercent(value) {
    return `${Math.round(Number(value || 0))}%`;
  }

  function resolveMetricType(metricKey) {
    if (metricKey === METRICS.spendingCoveragePct) {
      return "percent";
    }

    if (
      metricKey === METRICS.shareCount ||
      metricKey === METRICS.soldSharesForSpending
    ) {
      return "shares";
    }

    return "currency";
  }

  function formatMetricValue(value, metricType) {
    if (metricType === "percent") {
      return formatPercent(value);
    }

    if (metricType === "shares") {
      return formatShares(value);
    }

    return formatCurrency(value);
  }

  function formatCompactMetricValue(value, metricType) {
    if (metricType === "percent") {
      return formatCompactPercent(value);
    }

    if (metricType === "shares") {
      return formatCompactShares(value);
    }

    return formatCompactCurrency(value);
  }

  function resolveMetricMin(metricKey) {
    if (
      metricKey === METRICS.annualGap ||
      metricKey === METRICS.cashReserve
    ) {
      return undefined;
    }

    return 0;
  }

  function resolveYearlyRowClass(record, firstDeficitYear) {
    if (record.totalWealth <= 0) {
      return "bg-red-50 text-red-900";
    }

    if (record.soldSharesForSpending > 0) {
      return "bg-orange-50";
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
