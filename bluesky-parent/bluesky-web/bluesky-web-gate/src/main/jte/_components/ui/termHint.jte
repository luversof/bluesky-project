@param String label = ""
@param String desc = ""
!{
	// 헷갈리는 용어(기간 일평균 투입원금 등)에 점선 밑줄을 긋고, 마우스를 올리면 뜻을 떠 있는 풍선으로 보여 준다(사용자 요청 2026-09-28).
	// CSS 툴팁(.tooltip)은 표 머리글을 감싼 가로 스크롤 상자에 잘린다 - 그래서 화면에 고정으로 뜨는 기존 풍선(data-stock-tag-tooltip)을 쓴다.
	// 풍선은 마우스 전용이다. 키보드 · 낭독기는 같은 뜻을 섹션 위 "용어 풀이"(details)에서 읽고, 초점을 받는 정렬 단추는 aria-describedby 로 그 뜻에 잇는다.
	// 이 파일에는 빈 줄도, 태그 밖 글자도 두면 안 된다(srExact 와 같은 까닭 - 호출한 자리에 개행이 끼어든다).
}
<span class="underline decoration-dotted decoration-base-content/40 underline-offset-2 cursor-help" data-stock-tag-tooltip="${desc}" data-term-hint>${label}</span>