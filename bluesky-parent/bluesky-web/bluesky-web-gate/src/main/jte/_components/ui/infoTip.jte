@import gg.jte.Content
@param String desc = null
@param Content detail = null
@param String label = null
@param String cssClass = ""
!{
	// 용어 · 값 옆 ⓘ 설명 아이콘. 툴팁은 공용 조각(tooltip.jte)이 그린다 - 아이콘 모양도 여기 하나뿐이다(사용자 요청 2026-11-10 툴팁 통합).
	// 짧은 설명은 desc, 여러 줄 내용(계산 내역 등)은 detail 과 그 이름 label.
	// 이 파일에는 빈 줄도, 태그 밖 글자도 두면 안 된다(srExact 와 같은 까닭 - 호출한 자리에 개행이 끼어든다).
}
@template._components.ui.tooltip(text = detail == null ? desc : label, detail = detail, cssClass = cssClass, content = @`<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="inline w-4 h-4 stroke-current opacity-60" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>`)