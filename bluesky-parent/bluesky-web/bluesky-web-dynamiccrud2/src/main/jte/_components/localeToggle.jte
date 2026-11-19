<%-- 상단 언어 전환(한국어 ↔ English). 지금 언어의 반대를 보이고, 누르면 LocaleController 가 쿠키에 저장한 뒤 보던 화면으로 돌아온다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import net.luversof.web.dynamiccrud2.config.Msg

!{var english = "en".equals(Msg.locale().getLanguage());}

<form method="post" action="/locale" class="inline-flex">
	<input type="hidden" name="lang" value="${english ? "ko" : "en"}" />
	<button class="btn btn-sm btn-ghost" lang="${english ? "ko" : "en"}" title="${t("layout.locale.change")}">${english ? "한국어" : "English"}</button>
</form>
