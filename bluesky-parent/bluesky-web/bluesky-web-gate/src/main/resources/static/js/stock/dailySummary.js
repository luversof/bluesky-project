
function updateDailySummaryFilters() {
    const typeRadio = document.querySelector('input[name="type"]:checked');
    if (!typeRadio) return;
    const type = typeRadio.value;

    const timeScaleRadio = document.querySelector('input[name="timeScale"]:checked');
    const groupByRadio = document.querySelector('input[name="groupBy"]:checked');
    
    // UI Logic: Hide Period Options based on Type
    const periodTotal = document.getElementById('period-total');
    const periodYearly = document.getElementById('period-yearly');
    const periodMonthly = document.getElementById('period-monthly');
    const periodGroup = document.getElementById('period-group');

    if (type === 'PROFIT') {
        // Profit supports TOTAL only
        if (periodTotal && !periodTotal.checked) {
            periodTotal.checked = true;
        }

        if (periodYearly) periodYearly.classList.add('hidden');
        if (periodMonthly) periodMonthly.classList.add('hidden');
    } else {
        // DIVIDEND supports all
        if (periodYearly) periodYearly.classList.remove('hidden');
        if (periodMonthly) periodMonthly.classList.remove('hidden');
    }

    // Select Elements
    const yearSelect = document.getElementById('filter-year');
    const monthSelect = document.getElementById('filter-month');
    const accountSelect = document.getElementById('filter-account');

    // Re-read timeScale because we might have forced it to TOTAL
    const currentTimeScale = document.querySelector('input[name="timeScale"]:checked').value;
    const currentGroupBy = document.querySelector('input[name="groupBy"]:checked').value;

    // Visibility of Year/Month selects
    if (currentTimeScale === 'TOTAL') {
        if (yearSelect) yearSelect.style.display = 'none';
        if (monthSelect) monthSelect.style.display = 'none';
        // Note: Logic in original code said: if TOTAL or YEARLY -> yearSelect.style.display = 'none' ?! 
        // Original: if (currentTimeScale === 'TOTAL' || currentTimeScale === 'YEARLY') { yearSelect.style.display = 'none'; }
        // This seems WRONG for YEARLY. Usually Yearly needs Year filter? 
        // Ah, maybe "Yearly" means "Group by Year" so it shows list of years? Then you don't select ONE year.
        // Yes, "Yearly" usually means "Show me data for ALL years" or "Group by Year".
        // Let's stick to original logic:
        if (yearSelect) yearSelect.style.display = 'none';
    } else {
        // MONTHLY (or others if added)
        // If Monthly, do we need Year Select? Yes, to specify which year to show months for?
        // Original logic: else { yearSelect.style.display = 'inline-block'; }
        // So ONLY 'MONTHLY' shows year select.
        // Wait, original logic:
        // if (currentTimeScale === 'TOTAL' || currentTimeScale === 'YEARLY') { yearSelect.style.display = 'none'; } else { yearSelect.style.display = 'inline-block'; }
        
        if (yearSelect) yearSelect.style.display = 'inline-block';
    }

    // Month Select Logic
    // Original: monthSelect.style.display = 'none'; (Always hidden)
    if (monthSelect) monthSelect.style.display = 'none'; 

    // Account Select Logic
    if (currentGroupBy === 'ACCOUNT') {
        if (accountSelect) accountSelect.style.display = 'none';
    } else {
        if (accountSelect) accountSelect.style.display = 'inline-block';
    }

    // Trigger HTMX Request
    const form = document.getElementById('daily-summary-filter-form');
    // Only trigger if we are called from an event (not initial load if we want to avoid double load, but function is called via onclick)
    // The form has hx-trigger="submit, change delay:200ms, load"
    // So 'change' events on inputs will trigger it automatically!
    // BUT we are changing 'checked' state programmatically in some cases (type=PROFIT -> force TOTAL).
    // Programmatic change might not trigger 'change' event.
    // Also, we want to trigger it when we click the radio buttons.
    // The original code explicitly called: htmx.trigger('#daily-summary-filter-form', 'submit');
    
    if (form && window.htmx) {
        // We use 'submit' to be safe, as hx-trigger includes 'submit'
        htmx.trigger(form, 'submit');
    }
}

// Initialize on load? 
// The original code had: setTimeout(updateDailySummaryFilters, 0);
// We can expose the function to window so the inline onclicks work.
window.updateDailySummaryFilters = updateDailySummaryFilters;

// If we want to auto-init when this script is loaded:
setTimeout(updateDailySummaryFilters, 0);
