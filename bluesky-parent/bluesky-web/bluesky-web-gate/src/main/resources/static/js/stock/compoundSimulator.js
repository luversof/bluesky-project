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
    const frequencySelect = document.getElementById("stockCompoundFrequency");
    const monthlyNote = document.getElementById("stockCompoundMonthlyNote");
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
    const ratioContainer = document.getElementById("stockCompoundRatio");
    const ratioPrincipalBar = document.getElementById("stockCompoundRatioPrincipalBar");
    const ratioProfitBar = document.getElementById("stockCompoundRatioProfitBar");
    const ratioPrincipalPct = document.getElementById("stockCompoundRatioPrincipalPct");
    const ratioProfitPct = document.getElementById("stockCompoundRatioProfitPct");
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
            if (frequencySelect &&
                (saved.frequency === "yearly" || saved.frequency === "monthly")) {
                frequencySelect.value = saved.frequency;
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
                frequency: (frequencySelect === null || frequencySelect === void 0 ? void 0 : frequencySelect.value) === "monthly" ? "monthly" : "yearly",
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
        const monthly = (frequencySelect === null || frequencySelect === void 0 ? void 0 : frequencySelect.value) === "monthly";
        const contributeAtBegin = (timingSelect === null || timingSelect === void 0 ? void 0 : timingSelect.value) !== "end";
        // 매월 모드: 연 이율/12 의 월 이율로 월복리 (일반적인 적금 계산기 관례)
        const periodsPerYear = monthly ? 12 : 1;
        const periodRate = monthly ? rate / 12 : rate;
        const rows = [];
        let balance = initial;
        let cumulativePrincipal = initial;
        for (let year = 1; year <= years; year++) {
            let yearContribution = 0;
            let yearGain = 0;
            for (let period = 0; period < periodsPerYear; period++) {
                if (contributeAtBegin) {
                    balance += contribution;
                    yearContribution += contribution;
                }
                const gain = balance * periodRate;
                balance += gain;
                yearGain += gain;
                if (!contributeAtBegin) {
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
        renderRatio(finalValue, totalPrincipal, profit);
    }
    function renderRatio(finalValue, totalPrincipal, profit) {
        if (!ratioContainer ||
            !ratioPrincipalBar ||
            !ratioProfitBar ||
            !ratioPrincipalPct ||
            !ratioProfitPct) {
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
    function ratioShares(row) {
        if (row.balance <= 0) {
            return null;
        }
        return {
            principalPct: (row.cumulativePrincipal / row.balance) * 100,
            profitPct: ((row.balance - row.cumulativePrincipal) / row.balance) * 100,
        };
    }
    function buildRatioCell(row) {
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
        principalSegment.style.background = "rgba(99,102,241,0.8)";
        principalSegment.style.width = `${Math.min(100, Math.max(0, shares.principalPct))}%`;
        const profitSegment = document.createElement("div");
        profitSegment.className = "h-1.5";
        profitSegment.style.background = "rgba(34,197,94,0.8)";
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
            tr.appendChild(buildRatioCell(row));
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
                interaction: {
                    mode: "index",
                    intersect: false,
                },
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
                            label: (context) => {
                                const datasets = context.chart.data.datasets;
                                const total = datasets.reduce((sum, dataset) => sum + Number(dataset.data[context.dataIndex] || 0), 0);
                                const base = `${context.dataset.label}: ${formatCurrency(context.parsed.y)}`;
                                if (total <= 0) {
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
            monthlyNote.classList.toggle("hidden", (frequencySelect === null || frequencySelect === void 0 ? void 0 : frequencySelect.value) !== "monthly");
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
    }
    restoreInputs();
    update();
})();
