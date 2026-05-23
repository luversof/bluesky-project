import { putJson } from "../fetchClient.js";
import { handleApiError } from "../errorHandler.js";
(() => {
    const lists = Array.from(document.querySelectorAll("[data-profile-order-list]"));
    lists.forEach((list) => initializeProfileOrderList(list));
    function initializeProfileOrderList(list) {
        if (list.dataset.initialized === "true") {
            return;
        }
        list.dataset.initialized = "true";
        const orderList = list;
        const status = resolveStatusElement(orderList.dataset.profileOrderStatusId);
        const reorderEnabled = orderList.dataset.profileReorderEnabled === "true";
        if (!reorderEnabled) {
            return;
        }
        const orderEndpoint = orderList.dataset.profileOrderEndpoint;
        const successMessage = orderList.dataset.successMessage || "표시 순서를 저장했습니다.";
        const fallbackErrorMessage = orderList.dataset.fallbackErrorMessage ||
            "표시 순서를 저장하지 못했습니다. 다시 시도해 주세요.";
        const rowSelector = orderList.dataset.profileOrderRowSelector || "tr[data-profile-order-row]";
        const handleSelector = orderList.dataset.profileOrderHandleSelector || "";
        let currentAllSymbols = parseSymbols(orderList.dataset.profileOrderAllSymbols);
        if (!orderEndpoint) {
            return;
        }
        let draggedRow = null;
        let armedHandleRow = null;
        let originalVisibleOrder = [];
        let saving = false;
        if (currentAllSymbols.length === 0) {
            currentAllSymbols = collectSymbols();
        }
        renderDisplayOrderValues();
        orderList.addEventListener("pointerdown", (event) => {
            armedHandleRow = findHandleRow(event.target);
        });
        orderList.addEventListener("dragstart", (event) => {
            if (saving) {
                event.preventDefault();
                return;
            }
            const row = findDragStartRow(event.target);
            if (!row) {
                armedHandleRow = null;
                event.preventDefault();
                return;
            }
            draggedRow = row;
            originalVisibleOrder = collectSymbols();
            row.classList.add("opacity-50");
            if (event.dataTransfer) {
                event.dataTransfer.effectAllowed = "move";
                event.dataTransfer.setData("text/plain", row.dataset.symbol || "");
            }
        });
        orderList.addEventListener("dragover", (event) => {
            if (!draggedRow || saving) {
                return;
            }
            event.preventDefault();
            const targetRow = findDropTargetRow(event.target);
            if (!targetRow || targetRow === draggedRow) {
                return;
            }
            const rect = targetRow.getBoundingClientRect();
            const shouldInsertBefore = event.clientY < rect.top + rect.height / 2;
            const nextSibling = shouldInsertBefore
                ? targetRow
                : targetRow.nextElementSibling;
            if (nextSibling !== draggedRow) {
                if (nextSibling instanceof Element) {
                    nextSibling.before(draggedRow);
                }
                else {
                    orderList.appendChild(draggedRow);
                }
                renderDisplayOrderValues(collectSymbols());
            }
        });
        orderList.addEventListener("drop", (event) => {
            if (!draggedRow) {
                return;
            }
            armedHandleRow = null;
            event.preventDefault();
        });
        orderList.addEventListener("dragend", async () => {
            const currentDraggedRow = draggedRow;
            draggedRow = null;
            armedHandleRow = null;
            if (!currentDraggedRow) {
                return;
            }
            currentDraggedRow.classList.remove("opacity-50");
            const currentVisibleOrder = collectSymbols();
            if (arraysEqual(originalVisibleOrder, currentVisibleOrder)) {
                renderDisplayOrderValues();
                return;
            }
            saving = true;
            const requestSymbols = buildRequestSymbols(currentVisibleOrder);
            try {
                await putJson(orderEndpoint, { symbols: requestSymbols });
                currentAllSymbols = requestSymbols;
                renderDisplayOrderValues();
                showStatus("success", successMessage);
            }
            catch (error) {
                restoreOrder(originalVisibleOrder);
                renderDisplayOrderValues();
                handleApiError(error, {
                    onDisplayableMessage: (message) => showStatus("error", message),
                    onNonDisplayable: () => showStatus("error", fallbackErrorMessage),
                });
            }
            finally {
                saving = false;
            }
        });
        function findDragStartRow(target) {
            if (!(target instanceof HTMLElement)) {
                return null;
            }
            if (handleSelector) {
                const row = target.closest(rowSelector);
                if (!(row instanceof HTMLTableRowElement)) {
                    return null;
                }
                return armedHandleRow === row ? row : null;
            }
            const row = target.closest(rowSelector);
            return row instanceof HTMLTableRowElement ? row : null;
        }
        function findHandleRow(target) {
            if (!(target instanceof HTMLElement) || !handleSelector) {
                return null;
            }
            const handle = target.closest(handleSelector);
            if (!handle) {
                return null;
            }
            const row = handle.closest(rowSelector);
            return row instanceof HTMLTableRowElement ? row : null;
        }
        function findDropTargetRow(target) {
            if (!(target instanceof HTMLElement)) {
                return null;
            }
            const row = target.closest(rowSelector);
            return row instanceof HTMLTableRowElement ? row : null;
        }
        function collectRows() {
            return Array.from(orderList.querySelectorAll(rowSelector));
        }
        function collectSymbols() {
            return collectRows()
                .map((row) => row.dataset.symbol || "")
                .filter((symbol) => symbol.length > 0);
        }
        function buildRequestSymbols(visibleSymbols) {
            if (currentAllSymbols.length === 0) {
                return visibleSymbols;
            }
            const visibleSet = new Set(visibleSymbols);
            let visibleIndex = 0;
            const requestSymbols = [];
            for (const symbol of currentAllSymbols) {
                if (visibleSet.has(symbol)) {
                    if (visibleIndex < visibleSymbols.length) {
                        requestSymbols.push(visibleSymbols[visibleIndex++]);
                    }
                }
                else {
                    requestSymbols.push(symbol);
                }
            }
            for (; visibleIndex < visibleSymbols.length; visibleIndex += 1) {
                requestSymbols.push(visibleSymbols[visibleIndex]);
            }
            return requestSymbols;
        }
        function restoreOrder(symbols) {
            const rowBySymbol = new Map(collectRows().map((row) => [row.dataset.symbol || "", row]));
            symbols.forEach((symbol) => {
                const row = rowBySymbol.get(symbol);
                if (row) {
                    orderList.appendChild(row);
                }
            });
        }
        function renderDisplayOrderValues(previewVisibleSymbols) {
            const previewAllSymbols = previewVisibleSymbols
                ? buildRequestSymbols(previewVisibleSymbols)
                : currentAllSymbols;
            const orderBySymbol = new Map(previewAllSymbols.map((symbol, index) => [symbol, index + 1]));
            collectRows().forEach((row, index) => {
                const valueEl = row.querySelector("[data-profile-order-value]");
                if (!valueEl) {
                    return;
                }
                const symbol = row.dataset.symbol || "";
                valueEl.textContent = String(orderBySymbol.get(symbol) || index + 1);
            });
        }
        function showStatus(type, message) {
            if (!status) {
                if (type === "error") {
                    alert(message);
                }
                return;
            }
            status.hidden = false;
            status.className =
                type === "success"
                    ? "alert alert-success border border-success/20 bg-success/10 text-success-content"
                    : "alert alert-error border border-error/20 bg-error/10 text-error-content";
            status.textContent = message;
        }
    }
    function parseSymbols(rawValue) {
        if (!rawValue) {
            return [];
        }
        return rawValue
            .split(",")
            .map((symbol) => symbol.trim())
            .filter((symbol) => symbol.length > 0);
    }
    function arraysEqual(left, right) {
        if (left.length !== right.length) {
            return false;
        }
        return left.every((value, index) => value === right[index]);
    }
    function resolveStatusElement(statusId) {
        if (!statusId) {
            return null;
        }
        const found = document.getElementById(statusId);
        return found instanceof HTMLDivElement ? found : null;
    }
})();
