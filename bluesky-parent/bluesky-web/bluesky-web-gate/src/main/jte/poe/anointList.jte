@import net.luversof.web.gate.poe.dto.PoeAnointEntry
@import net.luversof.web.gate.poe.PoeText

<%-- poedb 속성부여식 도유 목록 — 아뮬렛에 성유 3개로 부여하는 노터블(비싼 성유 우선 정렬).
     행에 data-mod-family 를 달아 페이지의 검색 필터(modFilterBar)가 그대로 걸리게 한다. --%>
@param java.util.List<PoeAnointEntry> anoints
@param boolean ko = false

<div class="grid gap-1.5">
	@for(PoeAnointEntry a : anoints)
		<div class="flex items-start gap-3 rounded-lg border border-base-200 bg-base-100 px-3 py-1.5" data-mod-family="anoint-${a.nodeId()}">
			<span class="flex items-center gap-0.5 shrink-0 pt-0.5">
				@for(PoeAnointEntry.OilRef oil : a.oils())
					@if(oil.icon() != null)
						<img src="/poe-assets/${oil.icon()}" class="w-6 h-6 object-contain" alt="${ko ? oil.nameKo() : oil.name()}" title="${ko ? oil.nameKo() : oil.name()}" loading="lazy">
					@else
						<span class="text-[10px]">${oil.slug()}</span>
					@endif
				@endfor
			</span>
			<span class="min-w-0">
				<%-- 이름 클릭 → 트리 에디터에서 이 노터블을 도유 상태로 열고 중앙 표시(an=+focus=) --%>
				<a class="block text-[13px] font-semibold link-hover" href="/poe/tree?an=${a.nodeId()}&focus=${a.nodeId()}" title="${ko ? "트리에서 보기" : "View on tree"}">${PoeText.name(a.nameKo(), a.name())}</a>
				@for(String line : (ko && a.statsKo() != null && !a.statsKo().isEmpty() ? a.statsKo() : a.stats()))
					<span class="block text-[12px] text-base-content/70">$unsafe{PoeText.highlightValues(line)}</span>
				@endfor
			</span>
		</div>
	@endfor
</div>
