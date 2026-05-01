function updateDailySummaryFilters() {
  const typeRadio = document.querySelector('input[name="type"]:checked');
  if (!typeRadio) return;
  const type = typeRadio.value;

  const timeScaleRadio = document.querySelector(
    'input[name="timeScale"]:checked',
  );
  const groupByRadio = document.querySelector('input[name="groupBy"]:checked');

  // UI Logic: Hide Period Options based on Type
  const periodTotal = document.getElementById("period-total");
  const periodYearly = document.getElementById("period-yearly");
  const periodMonthly = document.getElementById("period-monthly");
  const periodGroup = document.getElementById("period-group");

  if (type === "PROFIT") {
    // Profit supports TOTAL only
    if (periodTotal && !periodTotal.checked) {
      periodTotal.checked = true;
    }

    if (periodYearly) periodYearly.classList.add("hidden");
    if (periodMonthly) periodMonthly.classList.add("hidden");
  } else {
    // DIVIDEND supports all
    if (periodYearly) periodYearly.classList.remove("hidden");
    if (periodMonthly) periodMonthly.classList.remove("hidden");
  }

  // Select Elements
  const yearSelect = document.getElementById("filter-year");
  const monthSelect = document.getElementById("filter-month");
  const accountSelect = document.getElementById("filter-account");

  // Re-read timeScale because we might have forced it to TOTAL
  const currentTimeScale = document.querySelector(
    'input[name="timeScale"]:checked',
  ).value;
  const currentGroupBy = document.querySelector(
    'input[name="groupBy"]:checked',
  ).value;

  // Visibility of Year/Month selects
  const yearWrapper = document.getElementById("filter-year-wrapper");
  const monthWrapper = document.getElementById("filter-month-wrapper");
  const accountWrapper =
    document.getElementById("filter-account").parentElement;

  if (currentTimeScale === "TOTAL" || currentTimeScale === "YEARLY") {
    if (yearWrapper) yearWrapper.style.display = "none";
    if (monthWrapper) monthWrapper.style.display = "none";
  } else {
    if (yearWrapper) yearWrapper.style.display = "inline-flex";
  }

  // Month Select Logic
  if (monthWrapper) monthWrapper.style.display = "none";

  // Account Select Logic
  if (currentGroupBy === "ACCOUNT") {
    if (accountWrapper) accountWrapper.style.display = "none";
  } else {
    if (accountWrapper) accountWrapper.style.display = "inline-flex";
  }

  // Trigger HTMX form submit after UI state is updated
  const form = document.getElementById("daily-summary-filter-form");
  if (form && window.htmx) {
    htmx.trigger(form, "submit");
  }
}

window.updateDailySummaryUI = updateDailySummaryFilters;
window.updateDailySummaryFilters = updateDailySummaryFilters;

// If we want to auto-init when this script is loaded:
setTimeout(updateDailySummaryFilters, 0);
