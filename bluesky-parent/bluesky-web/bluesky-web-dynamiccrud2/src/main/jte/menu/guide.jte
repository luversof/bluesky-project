<%-- 사용 안내(상단 오른쪽 "가이드"). 본문은 언어별 조각(menu/guideKo · guideEn) - 문단이 길어 메시지 키로 나누지 않는다.
     구성: 차례 · 구조 · 시작하기 · 테이블 필드 · 쿼리 · 검색 · 권한과 숨기기 · 사용 화면 · 변경 이력 · JSON 호출 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t

@template._layout.layout(title = t("guide.title"), content = @`
	<div class="guide-doc max-w-4xl">
		@if("ko".equals(net.luversof.web.dynamiccrud2.config.Msg.locale().getLanguage()))
			@template.menu.guideKo()
		@else
			@template.menu.guideEn()
		@endif
	</div>
`)
