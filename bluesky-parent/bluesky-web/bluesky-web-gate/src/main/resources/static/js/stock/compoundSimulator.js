"use strict";
(() => {
    const root = document.getElementById("stockCompoundSimulator");
    if (!root || root.dataset.compoundSimulatorInitialized === "true") {
        return;
    }
    root.dataset.compoundSimulatorInitialized = "true";
    const STORAGE_KEY = "stock.compoundSimulator.v1";
    const MAX_YEARS = 100;
    const initialInput = document.getElementById("stockCompoundInitial");
    const contributionInput = document.getElementById("stockCompoundContribution");
    const timingSelect = document.getElementById("stockCompoundTiming");
    const rateInput = document.getElementById("stockCompoundRatePct");
    const yearsInput = document.getElementById("stockCompoundYears");
    const initialPreview = document.getElementById("stockCompoundInitialPreview");
    const contributionPreview = document.getElementById("stockCompoundContributionPreview");
    const finalValueEl = document.getElementById("stockCompoundFinalValue");
    const totalPrincipalEl = document.getElementById("stockCompoundTotalPrincipal");
    const totalProfitEl = document.getElementById("stockCompoundTotalProfit");
    const totalReturnEl = document.getElementById("stockCompoundTotalReturn");
    const tableBody = document.getElementById("stockCompoundYearlyTableBody");
    const chartCanvas = document.getElementById("stockCompoundGrowthChart");
    const currencyFormatter = new Intl.NumberFormat("ko-KR");
    let growthChart = null;
    function readNumber(input, fallback) {
        const value = Number(input === null || input === void 0 ? void 0 : input.value);
        return Number.isFinite(value) ? value : fallback;
    }
    function formatCurrency(value) {
        const rounded = Math.round(value || 0);
        const formatted = currencyFormatter.format(Math.abs(rounded));
        return rounded < 0 ? `-₩${formatted}` : `₩${formatted}`;
    }
    function formatSignedPercent(value) {
        if (!Number.isFinite(value)) {
            return "-";
        }
        return `${value > 0 ? "+" : ""}${value.toFixed(1)}%`;
    }
    function applyProfitColor(element, value) {
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
            if (timingSelect &&
                (saved.timing === "begin" || saved.timing === "end")) {
                timingSelect.value = saved.timing;
            }
            if (rateInput && Number.isFinite(Number(saved.ratePct))) {
                rateInput.value = String(saved.ratePct);
            }
            if (yearsInput && Number.isFinite(Number(saved.years))) {
                yearsInput.value = String(saved.years);
            }
        }
        catch (_a) {
            // 저장값이 손상된 경우 기본값으로 진행한다.
        }
    }
    function persistInputs() {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify({
                initial: readNumber(initialInput, 0),
                contribution: readNumber(contributionInput, 0),
                timing: (timingSelect === null || timingSelect === void 0 ? void 0 : timingSelect.value) === "end" ? "end" : "begin",
                ratePct: readNumber(rateInput, 0),
                years: readNumber(yearsInput, 1),
            }));
        }
        catch (_a) {
            // localStorage를 못 쓰는 환경에서도 계산은 계속 동작한다.
        }
    }
    function simulate() {
        const initial = Math.max(0, readNumber(initialInput, 0));
        const contribution = Math.max(0, readNumber(contributionInput, 0));
        const rate = readNumber(rateInput, 0) / 100;
        const years = Math.min(MAX_YEARS, Math.max(1, Math.floor(readNumber(yearsInput, 1))));
        const contributeAtBegin = (timingSelect === null || timingSelect === void 0 ? void 0 : timingSelect.value) !== "end";
        const rows = [];
        let balance = initial;
        let cumulativePrincipal = initial;
        for (let year = 1; year <= years; year++) {
            if (contributeAtBegin) {
                balance += contribution;
                cumulativePrincipal += contribution;
            }
            const gain = balance * rate;
            balance += gain;
            if (!contributeAtBegin) {
                balance += contribution;
                cumulativePrincipal += contribution;
            }
            rows.push({ year, contribution, cumulativePrincipal, gain, balance });
        }
        return rows;
    }
    function renderPreviews() {
        if (initialPreview) {
            initialPreview.textContent = currencyFormatter.format(Math.round(Math.max(0, readNumber(initialInput, 0))));
        }
        if (contributionPreview) {
            contributionPreview.textContent = currencyFormatter.format(Math.round(Math.max(0, readNumber(contributionInput, 0))));
        }
    }
    function renderSummary(rows) {
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
    }
    function renderTable(rows) {
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
                    td.className = "text-right font-mono tabular-nums";
                }
                tr.appendChild(td);
            });
            tableBody.appendChild(tr);
        }
    }
    function renderChart(rows) {
        const Chart = globalThis.Chart;
        if (!chartCanvas || typeof Chart === "undefined") {
            return;
        }
        const labels = rows.map((row) => String(row.year));
        const principalData = rows.map((row) => Math.round(row.cumulativePrincipal));
        const profitData = rows.map((row) => Math.round(row.balance - row.cumulativePrincipal));
        const i18n = root.dataset;
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
                        backgroundColor: "rgba(99,102,241,0.8)",
                        stack: "total",
                    },
                    {
                        label: profitLabel,
                        data: profitData,
                        backgroundColor: "rgba(34,197,94,0.8)",
                        stack: "total",
                    },
                ],
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { stacked: true },
                    y: {
                        stacked: true,
                        ticks: {
                            callback: (value) => currencyFormatter.format(value),
                        },
                    },
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: (context) => `${context.dataset.label}: ${formatCurrency(context.parsed.y)}`,
                        },
                    },
                },
            },
        });
    }
    function update() {
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
    }
    restoreInputs();
    update();
})();
