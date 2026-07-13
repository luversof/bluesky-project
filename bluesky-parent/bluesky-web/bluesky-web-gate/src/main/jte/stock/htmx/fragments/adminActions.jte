@import net.luversof.web.gate.util.CspNonceHolder
@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false

!{
    String dataManagementLabel = MessageUtil.getMessage("stock.admin.label.data.management");
    String updateStockItemsLabel = MessageUtil.getMessage("stock.admin.button.update.stock.items");
    String updateTradesLabel = MessageUtil.getMessage("stock.admin.button.update.trades");
    String updateDividendsLabel = MessageUtil.getMessage("stock.admin.button.update.dividends");
    String updatePriceHistoriesLabel = MessageUtil.getMessage("stock.admin.button.update.price.histories");
    String updateStockItemsConfirm = MessageUtil.getMessage("stock.admin.confirm.update.stock.items");
    String updateTradesConfirm = MessageUtil.getMessage("stock.admin.confirm.update.trades");
    String updateDividendsConfirm = MessageUtil.getMessage("stock.admin.confirm.update.dividends");
    String updatePriceHistoriesConfirm = MessageUtil.getMessage("stock.admin.confirm.update.price.histories");
    String updatingLabel = MessageUtil.getMessage("stock.admin.message.updating");
    String pleaseWaitLabel = MessageUtil.getMessage("stock.admin.message.please.wait");
    String lastRunLabel = MessageUtil.getMessage("stock.admin.label.last.run");
}

@if(isAuthenticated)
@template._components.ui.tableCard(cssClass = "mb-6", body = @`
    <div class="card-body">
        <h2 class="card-title text-2xl mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
                ${dataManagementLabel}
        </h2>
        <div class="flex flex-wrap gap-4">
            <button hx-post="/api/stock/admin/stock-items"
                    data-admin-action="stock-items"
                    hx-swap="none"
                    hx-indicator="#update-loading"
                    hx-confirm="${updateStockItemsConfirm}"
                    data-reload-after-request="true"
                    class="btn btn-outline gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                ${updateStockItemsLabel}
            </button>
            <button hx-post="/api/stock/admin/trades"
                    data-admin-action="trades"
                    hx-swap="none"
                    hx-indicator="#update-loading"
                    hx-confirm="${updateTradesConfirm}"
                    data-reload-after-request="true"
                    class="btn btn-outline gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
                </svg>
                ${updateTradesLabel}
            </button>
            <button hx-post="/api/stock/admin/dividends"
                    data-admin-action="dividends"
                    hx-swap="none"
                    hx-indicator="#update-loading"
                    hx-confirm="${updateDividendsConfirm}"
                    data-reload-after-request="true"
                    class="btn btn-outline gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                ${updateDividendsLabel}
            </button>
            <button hx-post="/api/stock/admin/price-histories"
                    data-admin-action="price-histories"
                    hx-swap="none"
                    hx-indicator="#update-loading"
                    hx-confirm="${updatePriceHistoriesConfirm}"
                    data-reload-after-request="true"
                    class="btn btn-outline gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
                ${updatePriceHistoriesLabel}
            </button>
        </div>
        <%-- 갱신 이력은 서버에 없으므로 이 브라우저에서 실행한 시각을 localStorage 로 기억해 표시한다 --%>
        <div class="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-[11px] text-base-content/40">
            <span class="font-semibold">${lastRunLabel}</span>
            <span>${updateStockItemsLabel}: <span data-admin-last-run="stock-items">-</span></span>
            <span>${updateTradesLabel}: <span data-admin-last-run="trades">-</span></span>
            <span>${updateDividendsLabel}: <span data-admin-last-run="dividends">-</span></span>
            <span>${updatePriceHistoriesLabel}: <span data-admin-last-run="price-histories">-</span></span>
        </div>
    </div>
    
    <div id="update-loading" class="htmx-indicator fixed inset-0 bg-black/50 z-50 flex items-center justify-center">
        <div class="bg-base-100 p-8 rounded-box shadow-xl flex flex-col items-center gap-4">
            <span class="loading loading-spinner loading-lg text-primary"></span>
            <div class="text-lg font-bold">${updatingLabel}</div>
            <div class="text-sm opacity-70">${pleaseWaitLabel}</div>
        </div>
    </div>

    <script nonce="${CspNonceHolder.getNonce()}">
    (function() {
        var STORAGE_KEY = 'stock.admin.lastRun.v1';
        function readMap() {
            try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || {}; } catch (e) { return {}; }
        }
        // 저장된 실행 시각 표시
        var lastRunMap = readMap();
        document.querySelectorAll('[data-admin-last-run]').forEach(function(el) {
            var ts = lastRunMap[el.getAttribute('data-admin-last-run')];
            if (ts) el.textContent = new Date(ts).toLocaleString();
        });
        // 갱신 성공 시 시각 기록 (기록 후 data-reload-after-request 위임이 새로고침한다)
        document.addEventListener('htmx:afterRequest', function(evt) {
            var btn = evt.target && evt.target.closest ? evt.target.closest('[data-admin-action]') : null;
            if (!btn || !evt.detail || !evt.detail.successful) return;
            var map = readMap();
            map[btn.getAttribute('data-admin-action')] = Date.now();
            try { localStorage.setItem(STORAGE_KEY, JSON.stringify(map)); } catch (e) {}
        });
    })();
    </script>
`)
@endif