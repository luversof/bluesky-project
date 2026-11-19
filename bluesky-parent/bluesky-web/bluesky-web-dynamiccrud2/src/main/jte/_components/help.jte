<%-- 항목 설명 도움말: 원 안 물음표 아이콘, 마우스를 올리거나 키보드로 초점을 주면 설명(툴팁)이 뜬다(CSS 만, .help / .help-tip).
     화면에 늘 깔리던 긴 안내 글을 대신한다. 화면 낭독기는 aria-describedby 로 설명을 읽는다. label = 무엇에 대한 도움말인지 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String text
@param String label = net.luversof.web.dynamiccrud2.config.Msg.t("common.help")
@param boolean alignEnd = false

!{var tipId = "help-" + Integer.toHexString(text.hashCode());}

<%-- alignEnd: 오른쪽 끝 자리(표 마지막 열 머리 등)에서는 설명을 왼쪽으로 펼쳐 화면 · 스크롤 상자 밖으로 나가지 않게 --%>
<span class="help ${alignEnd ? "help-end" : ""}">
	<button type="button" class="help-btn" aria-label="${label}" aria-describedby="${tipId}">
		<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
			<circle cx="12" cy="12" r="10" />
			<path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3" />
			<path d="M12 17h.01" />
		</svg>
	</button>
	<span id="${tipId}" role="tooltip" class="help-tip">${text}</span>
</span>
