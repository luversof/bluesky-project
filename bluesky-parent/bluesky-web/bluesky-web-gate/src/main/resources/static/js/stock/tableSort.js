"use strict";
// 클라이언트 측 표 정렬. <table data-sortable> 의 thead <th> 를 클릭하면 해당 열로 tbody 행을 정렬한다.
// 열 타입(숫자/날짜/텍스트)은 셀 내용으로 자동 감지하므로 th 마다 속성을 달 필요가 없다.
// 상세 페이지의 표(매매·배당·보유 종목)는 한 종목/계좌분이라 작아 서버 왕복 없이 즉시 정렬한다.
// 클릭 위임이라 htmx 로 나중에 삽입된 표에도 동작한다. import/export 없이 classic <script src> 로 로드.
(function () {
    function columnType(tbody, idx) {
        var rows = tbody.rows;
        for (var i = 0; i < rows.length; i++) {
            var cell = rows[i].children[idx];
            if (!cell)
                continue;
            var text = (cell.textContent || "").trim();
            if (!text || text === "-")
                continue;
            if (/^\d{4}-\d{2}-\d{2}/.test(text))
                return "date";
            var stripped = text.replace(/[,\s%₩원주]/g, "");
            if (/^[+-]?\d+(\.\d+)?$/.test(stripped))
                return "num";
            return "text";
        }
        return "text";
    }
    function cellNum(cell) {
        var t = (cell ? cell.textContent : "") || "";
        var n = parseFloat(t.replace(/[^0-9.\-]/g, ""));
        return isNaN(n) ? 0 : n;
    }
    function cellText(cell) {
        return ((cell ? cell.textContent : "") || "").trim();
    }
    function sortTable(table, th) {
        var headRow = th.parentElement;
        if (!headRow)
            return;
        var ths = Array.prototype.slice.call(headRow.children);
        var idx = ths.indexOf(th);
        if (idx < 0)
            return;
        var tbody = table.tBodies[0];
        if (!tbody)
            return;
        var type = columnType(tbody, idx);
        var dir = th.getAttribute("data-sort-dir") === "asc" ? "desc" : "asc";
        ths.forEach(function (h) {
            h.removeAttribute("data-sort-dir");
            var a = h.querySelector(".sort-arrow");
            if (a)
                a.textContent = "";
        });
        th.setAttribute("data-sort-dir", dir);
        var arrow = th.querySelector(".sort-arrow");
        if (!arrow) {
            arrow = document.createElement("span");
            arrow.className = "sort-arrow";
            th.appendChild(arrow);
        }
        arrow.textContent = dir === "asc" ? " ▲" : " ▼";
        var rows = Array.prototype.slice.call(tbody.rows);
        rows.sort(function (a, b) {
            var ca = a.children[idx];
            var cb = b.children[idx];
            var cmp;
            if (type === "num") {
                cmp = cellNum(ca) - cellNum(cb);
            }
            else {
                cmp = cellText(ca).localeCompare(cellText(cb), undefined, {
                    numeric: true,
                });
            }
            return dir === "asc" ? cmp : -cmp;
        });
        rows.forEach(function (r) {
            tbody.appendChild(r);
        });
    }
    document.addEventListener("click", function (e) {
        var target = e.target;
        if (!target || typeof target.closest !== "function")
            return;
        // 셀 안의 링크/버튼 클릭은 정렬로 가로채지 않는다.
        if (target.closest("a, button, input, label, select"))
            return;
        var th = target.closest("th");
        if (!th)
            return;
        var table = th.closest("table");
        if (!table || !table.hasAttribute("data-sortable"))
            return;
        sortTable(table, th);
    });
})();
