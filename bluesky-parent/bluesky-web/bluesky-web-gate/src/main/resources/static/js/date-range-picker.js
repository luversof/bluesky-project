/**
 * DateRangePicker – 기간 선택 공통 유틸리티
 *
 * 사용법:
 *   const picker = DateRangePicker.create({
 *     formId:       'mySearchForm',
 *     startId:      'myStartDateInput',
 *     endId:        'myEndDateInput',
 *     rangeModeId:  'myRangeModeInput',   // <input type="hidden" name="rangeMode" ...>
 *     btnClass:     'my-range-btn'
 *   });
 *   window.myPicker = picker;   // HTML onclick에서 접근하기 위해 window에 노출
 *
 * HTML 버튼 예시:
 *   <button onclick="myPicker.set(1, this)">1개월</button>
 *   <button onclick="myPicker.set('ytd', this)">올해</button>
 *   <button onclick="myPicker.shift(-1)">‹ 이전</button>
 *   <button onclick="myPicker.shift(1)">다음 ›</button>
 *
 * rangeMode 값 규칙:
 *   숫자 문자열  → N개월 단위 이동  (예: "1", "3", "6", "12", "36")
 *   "ytd"        → 연도 단위 이동
 *   "all"        → 전체 (이전/다음 비활성)
 *   ""           → 사용자 입력 또는 초기값 (날짜 span 기준 이동)
 */
window.DateRangePicker = (function () {
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
    function el(id) {
      return document.getElementById(id);
    }
    function btns() {
      return document.querySelectorAll("." + cfg.btnClass);
    }

    function submit() {
      var form = el(cfg.formId);
      if (form) form.requestSubmit();
    }

    function clearActive() {
      btns().forEach(function (b) {
        b.classList.remove("btn-primary");
      });
    }

    return {
      /**
       * 기간 버튼 클릭 시 호출
       * @param {number|'ytd'|0} months
       * @param {HTMLElement|null} btn  - 클릭한 버튼 (active 표시용)
       */
      set: function (months, btn) {
        clearActive();
        if (btn) btn.classList.add("btn-primary");

        var rmEl = el(cfg.rangeModeId);
        if (rmEl) rmEl.value = months === 0 ? "all" : String(months);

        var today = new Date();
        var startEl = el(cfg.startId);
        var endEl = el(cfg.endId);

        if (months === 0) {
          if (startEl) startEl.value = "";
          if (endEl) endEl.value = "";
        } else if (months === "ytd") {
          if (startEl) startEl.value = today.getFullYear() + "-01-01";
          if (endEl) endEl.value = fmtDate(today);
        } else {
          var s = new Date(today);
          s.setMonth(s.getMonth() - months);
          if (startEl) startEl.value = fmtDate(s);
          if (endEl) endEl.value = fmtDate(today);
        }
        submit();
      },

      /**
       * 이전/다음 버튼 클릭 시 호출
       * @param {-1|1} dir
       */
      shift: function (dir) {
        var startEl = el(cfg.startId);
        var endEl = el(cfg.endId);
        if (!startEl || !startEl.value) return;

        var mode = (el(cfg.rangeModeId) || {}).value || "";
        var today = new Date();
        today.setHours(0, 0, 0, 0);

        if (mode === "all") return; // 전체 모드: 이동 없음

        // ytd 감지: 명시적 mode 또는 날짜가 YYYY-01-01 형태
        var isYtd =
          mode === "ytd" || (mode === "" && startEl.value.slice(5) === "01-01");

        if (isYtd) {
          var newYear = parseInt(startEl.value.slice(0, 4), 10) + dir;
          if (dir > 0 && newYear > today.getFullYear()) return;
          startEl.value = newYear + "-01-01";
          if (endEl)
            endEl.value =
              newYear === today.getFullYear()
                ? fmtDate(today)
                : newYear + "-12-31";
        } else if (mode && !isNaN(+mode) && +mode > 0) {
          // 월 단위 이동
          var months = +mode;
          var s = new Date(startEl.value + "T00:00:00");
          var e = endEl ? new Date(endEl.value + "T00:00:00") : new Date(s);
          s.setMonth(s.getMonth() + dir * months);
          e.setMonth(e.getMonth() + dir * months);
          if (dir > 0 && s > today) return;
          if (dir > 0 && e > today) e = new Date(today);
          startEl.value = fmtDate(s);
          if (endEl) endEl.value = fmtDate(e);
        } else {
          // 커스텀 범위: 날짜 span 기준 이동
          if (!endEl || !endEl.value) return;
          var s = new Date(startEl.value + "T00:00:00");
          var e = new Date(endEl.value + "T00:00:00");
          var ms = e - s;
          if (ms <= 0) return;
          var ns = new Date(s.getTime() + dir * ms);
          var ne = new Date(e.getTime() + dir * ms);
          if (dir > 0 && ns > today) return;
          if (dir > 0 && ne > today) ne = new Date(today);
          startEl.value = fmtDate(ns);
          endEl.value = fmtDate(ne);
        }

        clearActive();
        submit();
      },
    };
  }

  return { create: create };
})();
