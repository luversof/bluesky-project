@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

<%-- 키워드 설명(10-04 C111 젬 · C112 고유, 트리 C110 짝) — 인게임은 툴팁의 강조 용어를 Alt 로 푼다(KeywordPopups 용어 · 정의).
     평소 용어 이름 한 줄, Alt 누르는 동안(common.ts html.poe-alt → main.css .poe-kw-box) 정의 --%>
@param java.util.List<Poe2.Keyword> keywords

@if(keywords != null && !keywords.isEmpty())
	!{
		java.util.List<String> kwTerms = new java.util.ArrayList<>();
		for (Poe2.Keyword k : keywords) kwTerms.add(PoeText.name(k.termKo(), k.term()));
	}
	<div class="px-4 pb-2 text-left" data-kw>
		<%-- 단추: 눌러도 펼친다(10-04 C128, 터치 · 키보드) — common.ts 가 .poe-kw-open 을 붙이고, 정의 상자를 누르면 다시 접는다 --%>
		<button type="button" class="poe-popup-reminder poe-kw-hint text-center" data-kw-hint aria-expanded="false">${MessageUtil.getMessage("poe2.tt.keywords.hint")} — ${String.join(" · ", kwTerms)}</button>
		<div class="poe-kw-box border-t border-white/10 pt-1" data-kw-box>
			@for(Poe2.Keyword k : keywords)
				<div class="text-[12px] font-semibold text-[#8888ff] mt-1">${PoeText.name(k.termKo(), k.term())}</div>
				<div class="poe-popup-reminder">${PoeText.name(k.defKo(), k.def())}</div>
			@endfor
		</div>
	</div>
@endif
