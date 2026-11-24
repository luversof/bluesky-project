<%-- 이름 옆 삭제 단추(확인 후 POST). 휴지통 SVG(currentColor, 눈 아이콘과 같은 선 굵기) - × 는 취소 · 빼기에 쓴다.
     blockedReason: 지금은 지울 수 없을 때(하위 항목이 있음 등) 그 까닭 - 단추는 보이되 흐리게 · 눌러도 아무 일 없음(aria-disabled, 초점은 받아
     키보드 · 화면 낭독기도 까닭을 듣는다). disabled 속성은 초점을 못 받아 까닭이 안 들리므로 쓰지 않는다. 서버도 같은 규칙으로 막는다.
     설명은 브라우저 title 이 아니라 data-tip 툴팁(main.css .icon-btn[data-tip]) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String action
@param String label
@param String confirm
@param String blockedReason = null

@if(blockedReason != null)
	!{var reasonId = "del-why-" + Integer.toHexString(action.hashCode());}
	<span class="inline-flex">
		<button type="button" class="icon-btn" aria-disabled="true" aria-label="${label}" aria-describedby="${reasonId}" data-tip="${blockedReason}">
			<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<path d="M3 6h18" />
				<path d="M8 6V4h8v2" />
				<path d="M19 6l-1 14H6L5 6" />
				<path d="M10 11v6M14 11v6" />
			</svg>
		</button>
		<span id="${reasonId}" class="sr-only">${blockedReason}</span>
	</span>
@else
	<form method="post" action="${action}" class="inline-flex">
		<button class="icon-btn icon-btn-danger" aria-label="${label}" data-tip="${t("common.delete")}" data-confirm="${confirm}">
			<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<path d="M3 6h18" />
				<path d="M8 6V4h8v2" />
				<path d="M19 6l-1 14H6L5 6" />
				<path d="M10 11v6M14 11v6" />
			</svg>
		</button>
	</form>
@endif
