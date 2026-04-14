/**
 * DateRangePicker – 기간 선택 공통 유틸리티
 *
 * ▶ 폼 기반 사용법 (기존):
 *   const picker = DateRangePicker.create({
 *     formId:       'mySearchForm',
 *     startId:      'myStartDateInput',
 *     endId:        'myEndDateInput',
 *     rangeModeId:  'myRangeModeInput',
 *     btnClass:     'my-range-btn'
 *   });
 *
 * ▶ 콜백 기반 사용법 (Chart.js 등 DOM 입력 없이 사용):
 *   const picker = DateRangePicker.create({
 *     btnClass:    'my-range-btn',
 *     activeClass: 'btn-active',          // 기본값: 'btn-primary'
 *     minDate:     '2020-01-01',          // 데이터 시작일 (jumpToEdge 'start' 용)
 *     maxDate:     '2026-04-14',          // 데이터 종료일 (미설정 시 오늘)
 *     onApply:     function(start, end, mode) { ... }
 *   });
 *   picker.setMinDate('2020-01-01');  // 나중에 변경 가능
 *   picker.setMaxDate('2026-04-14');
 *   picker.setState(start, end, '');  // 줌/팬 등 외부 상태 변경 시
 *
 * HTML 버튼 예시:
 *   <button onclick="myPicker.set(1, this)">1개월</button>
 *   <button onclick="myPicker.set('mtd', this)">이번달</button>
 *   <button onclick="myPicker.set('ytd', this)">올해</button>
 *   <button onclick="myPicker.shift(-1)">‹ 이전</button>
 *   <button onclick="myPicker.shift(1)">다음 ›</button>
 *   <button onclick="myPicker.jumpToEdge('start')">«</button>
 *   <button onclick="myPicker.jumpToEdge('end')">»</button>
 *
 * rangeMode 값 규칙:
 *   숫자 문자열  → N개월 단위 이동  (예: "1", "3", "6", "12", "36")
 *   "mtd"        → 이번달 (월 단위 이동)
 *   "ytd"        → 올해 (연도 단위 이동)
 *   "all"        → 전체 (이전/다음 비활성)
 *   ""           → 사용자 입력 또는 커스텀 (날짜 span 기준 이동)
 */
window.DateRangePicker = (function () {
  /**
   * 로컬 시간 기준 yyyy-mm-dd 반환.
   * toISOString()은 UTC 기준이라 KST(+9h) 등에서 자정에 날짜가 하루 밀리므로 사용 금지.
   */
  function fmtDate(d) {
    return (
      d.getFullYear() +
      "-" +
      String(d.getMonth() + 1).padStart(2, "0") +
      "-" +
      String(d.getDate()).padStart(2, "0")
    );
  }

  function create(cfg) {
    // ── 콜백 모드 내부 상태 ────────────────────────────────────────────────
    var _s = { start: "", end: "", mode: "" };

    function isCallback() {
      return typeof cfg.onApply === "function";
    }
    function activeClass() {
      return cfg.activeClass || "btn-primary";
    }

    function el(id) {
      return id ? document.getElementById(id) : null;
    }
    function btns() {
      return cfg.btnClass
        ? document.querySelectorAll("." + cfg.btnClass)
        : { forEach: function () {} };
    }
    function clearActive() {
      btns().forEach(function (b) {
        b.classList.remove(activeClass());
      });
    }

    // ── 현재 start / end / mode 읽기 ─────────────────────────────────────
    function getStart() {
      return isCallback() ? _s.start : (el(cfg.startId) || {}).value || "";
    }
    function getEnd() {
      return isCallback() ? _s.end : (el(cfg.endId) || {}).value || "";
    }
    function getMode() {
      return isCallback() ? _s.mode : (el(cfg.rangeModeId) || {}).value || "";
    }

    // maxDate: cfg.maxDate 설정 시 그 값, 없으면 오늘
    function maxDateStr() {
      return cfg.maxDate || fmtDate(new Date());
    }

    // ── 날짜 범위 최종 적용 (DOM 갱신 또는 콜백 호출) ──────────────────────
    function applyRange(startStr, endStr, modeStr) {
      if (isCallback()) {
        _s.start = startStr;
        _s.end = endStr;
        _s.mode = modeStr;
        cfg.onApply(startStr, endStr, modeStr);
      } else {
        var se = el(cfg.startId),
          ee = el(cfg.endId),
          me = el(cfg.rangeModeId);
        if (se) se.value = startStr || "";
        if (ee) ee.value = endStr || "";
        if (me) me.value = modeStr;
        var form = el(cfg.formId);
        if (form) form.requestSubmit();
      }
    }

    // ── set ──────────────────────────────────────────────────────────────
    function doSet(months, btn) {
      clearActive();
      if (btn) btn.classList.add(activeClass());

      var maxStr = maxDateStr();
      var maxDate = new Date(maxStr + "T00:00:00");
      var today = new Date();
      today.setHours(0, 0, 0, 0);
      var startStr, endStr, modeStr;

      if (months === 0) {
        startStr = "";
        endStr = "";
        modeStr = "all";
      } else if (months === "mtd") {
        startStr = fmtDate(
          new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
        );
        endStr = maxStr;
        modeStr = "mtd";
      } else if (months === "ytd") {
        startStr = maxDate.getFullYear() + "-01-01";
        endStr = maxStr;
        modeStr = "ytd";
      } else {
        // N개월 – 이중 앵커 (끝 앵커 기본, 시작 앵커는 minDate 근처)
        var curEnd = getEnd();
        var curStart = getStart();
        var todayStr = fmtDate(today);
        var atDataEnd = !curEnd || curEnd >= todayStr;
        var atDataStart =
          !atDataEnd && !!cfg.minDate && !!curStart && curStart <= cfg.minDate;
        if (atDataStart) {
          var minD = new Date(cfg.minDate + "T00:00:00");
          var e = new Date(minD);
          e.setMonth(e.getMonth() + months);
          if (e > maxDate) e = new Date(maxDate);
          startStr = fmtDate(minD);
          endStr = fmtDate(e);
        } else {
          var s = new Date(maxDate);
          s.setMonth(s.getMonth() - months);
          startStr = fmtDate(s);
          endStr = maxStr;
        }
        modeStr = String(months);
      }
      applyRange(startStr, endStr, modeStr);
    }

    // ── jumpToEdge ────────────────────────────────────────────────────────
    function doJumpToEdge(direction) {
      var mode = getMode();
      if (mode === "all") return;

      var maxStr = maxDateStr();
      var maxDate = new Date(maxStr + "T00:00:00");
      clearActive();

      var startStr, endStr;
      if (direction === "end") {
        endStr = maxStr;
        if (mode && !isNaN(+mode) && +mode > 0) {
          var s = new Date(maxDate);
          s.setMonth(s.getMonth() - +mode);
          startStr = fmtDate(s);
        } else if (mode === "mtd") {
          startStr = fmtDate(
            new Date(maxDate.getFullYear(), maxDate.getMonth(), 1),
          );
        } else if (mode === "ytd") {
          startStr = maxDate.getFullYear() + "-01-01";
        } else {
          var cs = getStart(),
            ce = getEnd();
          if (!cs || !ce) return;
          var ms = new Date(ce + "T00:00:00") - new Date(cs + "T00:00:00");
          if (ms <= 0) return;
          startStr = fmtDate(new Date(maxDate.getTime() - ms));
        }
      } else {
        // 'start'
        if (!cfg.minDate) return;
        var minD = new Date(cfg.minDate + "T00:00:00");
        if (mode && !isNaN(+mode) && +mode > 0) {
          var e = new Date(minD);
          e.setMonth(e.getMonth() + +mode);
          if (e > maxDate) e = new Date(maxDate);
          startStr = fmtDate(minD);
          endStr = fmtDate(e);
        } else if (mode === "mtd") {
          var first = new Date(minD.getFullYear(), minD.getMonth(), 1);
          var last = new Date(minD.getFullYear(), minD.getMonth() + 1, 0);
          startStr = fmtDate(first);
          endStr = fmtDate(last > maxDate ? maxDate : last);
        } else if (mode === "ytd") {
          var minYear = minD.getFullYear();
          startStr = minYear + "-01-01";
          endStr =
            minYear === maxDate.getFullYear() ? maxStr : minYear + "-12-31";
        } else {
          var cs = getStart(),
            ce = getEnd();
          if (!cs || !ce) return;
          var ms = new Date(ce + "T00:00:00") - new Date(cs + "T00:00:00");
          if (ms <= 0) return;
          var ne = new Date(minD.getTime() + ms);
          if (ne > maxDate) ne = new Date(maxDate);
          startStr = fmtDate(minD);
          endStr = fmtDate(ne);
        }
      }
      applyRange(startStr, endStr, mode);
    }

    // ── shift ─────────────────────────────────────────────────────────────
    function doShift(dir) {
      var start = getStart(),
        end = getEnd(),
        mode = getMode();
      if (!start || mode === "all") return;

      var maxStr = maxDateStr();
      var maxDate = new Date(maxStr + "T00:00:00");
      clearActive();

      var isMtd = mode === "mtd";
      var isYtd =
        !isMtd &&
        (mode === "ytd" || (mode === "" && start.slice(5) === "01-01"));
      var newStart, newEnd;
      // mtd 이동 후 → 1개월 버튼, ytd 이동 후 → 12개월 버튼 활성화
      var newMode = isMtd ? "1" : isYtd ? "12" : mode;

      if (isMtd) {
        var curFirst = new Date(start + "T00:00:00");
        var newFirst = new Date(
          curFirst.getFullYear(),
          curFirst.getMonth() + dir,
          1,
        );
        var newLast = new Date(
          curFirst.getFullYear(),
          curFirst.getMonth() + dir + 1,
          0,
        );
        var thisMonthFirst = new Date(
          maxDate.getFullYear(),
          maxDate.getMonth(),
          1,
        );
        if (dir > 0 && newFirst > thisMonthFirst) return;
        newStart = fmtDate(newFirst);
        newEnd = fmtDate(newLast > maxDate ? maxDate : newLast);
      } else if (isYtd) {
        var newYear = parseInt(start.slice(0, 4), 10) + dir;
        if (dir > 0 && newYear > maxDate.getFullYear()) return;
        newStart = newYear + "-01-01";
        newEnd =
          newYear === maxDate.getFullYear() ? maxStr : newYear + "-12-31";
      } else if (mode && !isNaN(+mode) && +mode > 0) {
        var months = +mode;
        var s = new Date(start + "T00:00:00");
        var e = end ? new Date(end + "T00:00:00") : new Date(s);
        s.setMonth(s.getMonth() + dir * months);
        e.setMonth(e.getMonth() + dir * months);
        if (dir > 0 && s > maxDate) return;
        if (dir > 0 && e > maxDate) e = new Date(maxDate);
        newStart = fmtDate(s);
        newEnd = fmtDate(e);
      } else {
        if (!end) return;
        var s = new Date(start + "T00:00:00");
        var e = new Date(end + "T00:00:00");
        var ms = e - s;
        if (ms <= 0) return;
        var ns = new Date(s.getTime() + dir * ms);
        var ne = new Date(e.getTime() + dir * ms);
        if (dir > 0 && ns > maxDate) return;
        if (dir > 0 && ne > maxDate) ne = new Date(maxDate);
        newStart = fmtDate(ns);
        newEnd = fmtDate(ne);
      }
      // 이동 후 해당 N개월 버튼 재활성화 (mtd/ytd → 1개월/1년, 그 외 → 동일 버튼)
      btns().forEach(function (b) {
        if (
          (b.getAttribute("onclick") || "").indexOf(
            "set(" + newMode + ",",
          ) !== -1
        ) {
          b.classList.add(activeClass());
        }
      });
      applyRange(newStart, newEnd, newMode);
    }

    return {
      /** 기간 버튼 클릭: months = 숫자 | 'mtd' | 'ytd' | 0(전체) */
      set: function (months, btn) {
        doSet(months, btn);
      },
      /** 처음/끝 이동: direction = 'start' | 'end' */
      jumpToEdge: function (direction) {
        doJumpToEdge(direction);
      },
      /** 이전/다음 이동: dir = -1 | 1 */
      shift: function (dir) {
        doShift(dir);
      },

      // ── 외부 상태 설정 ─────────────────────────────────────────────────
      /** 데이터 시작일 설정 (jumpToEdge 'start' 기준점) */
      setMinDate: function (d) {
        cfg.minDate = d;
      },
      /** 데이터 종료일 설정 (기본: 오늘). 차트 최신 데이터 날짜 등에 활용 */
      setMaxDate: function (d) {
        cfg.maxDate = d;
      },
      /** 외부에서 상태 일괄 변경 (차트 zoom/pan 등 커스텀 모드 진입 시) */
      setState: function (start, end, mode) {
        if (isCallback()) {
          _s.start = start;
          _s.end = end;
          _s.mode = mode;
        }
        clearActive();
      },
      /** 현재 상태 반환 */
      getState: function () {
        return { start: getStart(), end: getEnd(), mode: getMode() };
      },
    };
  }

  return { create: create, fmt: fmtDate };
})();
