@import net.luversof.web.gate.poe.dto.PoeFoulbornGroup
@import net.luversof.web.gate.poe.PoeText

@param java.util.List<PoeFoulbornGroup.FoulbornMod> mods
@param boolean showOriginal = true

<%-- 삿된 옵션 목록 — 인게임 색 관례:
       · 삿된(변이) 모드 = #CD2285 (PoB Global.lua colorCodes.MUTATED, 게임에서 가져온 값)
       · 원본 모드 = 일반 모드 파랑 #8888ff — 대체돼 사라지는 쪽이라 취소선으로 죽인다
     "무엇이 무엇으로 바뀌는가" 가 한눈에 보여야 해서 원본을 위, 삿된을 아래 두고 화살표로 잇는다. --%>
!{ boolean ko = PoeText.isKorean(); }
<div id="poeFoulbornList" class="space-y-1.5">
	@for(PoeFoulbornGroup.FoulbornMod mod : mods)
		<div class="rounded-lg bg-base-200/40 px-2 py-1.5">
			@if(showOriginal && (ko ? mod.origKo() : mod.origEn()) != null && !(ko ? mod.origKo() : mod.origEn()).isEmpty())
				@for(String line : (ko ? mod.origKo() : mod.origEn()))
					<div class="whitespace-pre-line text-[12px] leading-5 text-[#8888ff]/60 line-through decoration-base-content/40">$unsafe{PoeText.highlightValues(line)}</div>
				@endfor
				<div class="my-0.5 text-[11px] leading-none text-[#CD2285]/70">↓ ${ko ? "삿된" : "Foulborn"}</div>
			@elseif(showOriginal)
				<%-- 지도 밖 신규 모드 — 어느 모드를 밀어내는지 확인되지 않았다. 추측해서 짝지어 주지 않는다. --%>
				<div class="mb-0.5 text-[11px] leading-none text-base-content/40">${ko ? "대체 대상 미확인" : "replaced mod unknown"}</div>
			@endif
			@for(String line : (ko ? mod.ko() : mod.en()))
				<div class="whitespace-pre-line text-[13px] leading-5 text-[#CD2285]">$unsafe{PoeText.highlightValues(line)}</div>
			@endfor
		</div>
	@endfor
</div>
