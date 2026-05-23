import { putJson } from "../fetchClient.js";
import { handleApiError } from "../errorHandler.js";
(() => {
    const list = document.getElementById("monthlyDividendProfileOrderList");
    const status = document.getElementById("monthlyDividendProfileOrderStatus");
    if (!list || list.dataset.initialized === "true") {
        return;
    }
    list.dataset.initialized = "true";
    const orderList = list;
    const reorderEnabled = orderList.dataset.profileReorderEnabled === "true";
    if (!reorderEnabled) {
        return;
    }
    const orderEndpoint = orderList.dataset.profileOrderEndpoint;
    const successMessage = orderList.dataset.successMessage || "표시 순서를 저장했습니다.";
    const fallbackErrorMessage = orderList.dataset.fallbackErrorMessage ||
        "표시 순서를 저장하지 못했습니다. 다시 시도해 주세요.";
    if (!orderEndpoint) {
        return;
    }
    let draggedRow = null;
    let originalOrder = [];
    let saving = false;
    renderDisplayOrderValues();
    orderList.addEventListener("dragstart", (event) => {
        if (saving) {
            event.preventDefault();
            return;
        }
        const row = findRow(event.target);
        if (!row) {
            return;
        }
        draggedRow = row;
        originalOrder = collectSymbols();
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
        const targetRow = findRow(event.target);
        if (!targetRow || targetRow === draggedRow) {
            return;
        }
        const rect = targetRow.getBoundingClientRect();
        const shouldInsertBefore = event.clientY < rect.top + rect.height / 2;
        const nextSibling = shouldInsertBefore
            ? targetRow
            : targetRow.nextElementSibling;
        if (nextSibling !== draggedRow) {
            orderList.insertBefore(draggedRow, nextSibling);
            renderDisplayOrderValues();
        }
    });
    orderList.addEventListener("drop", (event) => {
        if (!draggedRow) {
            return;
        }
        event.preventDefault();
    });
    orderList.addEventListener("dragend", async () => {
        const currentDraggedRow = draggedRow;
        draggedRow = null;
        if (!currentDraggedRow) {
            return;
        }
        currentDraggedRow.classList.remove("opacity-50");
        const currentOrder = collectSymbols();
        if (arraysEqual(originalOrder, currentOrder)) {
            renderDisplayOrderValues();
            return;
        }
        saving = true;
        try {
            await putJson(orderEndpoint, { symbols: currentOrder });
            showStatus("success", successMessage);
        }
        catch (error) {
            restoreOrder(originalOrder);
            handleApiError(error, {
                onDisplayableMessage: (message) => showStatus("error", message),
                onNonDisplayable: () => showStatus("error", fallbackErrorMessage),
            });
        }
        finally {
            saving = false;
        }
    });
    function findRow(target) {
        if (!(target instanceof HTMLElement)) {
            return null;
        }
        return target.closest("tr[data-profile-order-row]");
    }
    function collectRows() {
        return Array.from(orderList.querySelectorAll("tr[data-profile-order-row]"));
    }
    function collectSymbols() {
        return collectRows()
            .map((row) => row.dataset.symbol || "")
            .filter((symbol) => symbol.length > 0);
    }
    function restoreOrder(symbols) {
        const rowBySymbol = new Map(collectRows().map((row) => [row.dataset.symbol || "", row]));
        symbols.forEach((symbol) => {
            const row = rowBySymbol.get(symbol);
            if (row) {
                orderList.appendChild(row);
            }
        });
        renderDisplayOrderValues();
    }
    function renderDisplayOrderValues() {
        collectRows().forEach((row, index) => {
            const valueEl = row.querySelector("[data-profile-order-value]");
            if (valueEl) {
                valueEl.textContent = String(index + 1);
            }
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
    function arraysEqual(left, right) {
        if (left.length !== right.length) {
            return false;
        }
        return left.every((value, index) => value === right[index]);
    }
})();
