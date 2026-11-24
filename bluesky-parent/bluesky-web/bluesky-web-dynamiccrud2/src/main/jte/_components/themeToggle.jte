<%-- 라이트 · 다크 전환 단추(js/ui.js). 처음 값은 layout 머리의 짧은 스크립트가 그리기 전에 정한다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t

<button type="button" class="icon-btn" data-theme-toggle aria-label="${t("layout.theme.dark")}" aria-pressed="false" data-tip="${t("layout.theme.toggle")}">
	<span class="theme-icon-light" aria-hidden="true">☾</span><span class="theme-icon-dark" aria-hidden="true">☀</span>
</button>
