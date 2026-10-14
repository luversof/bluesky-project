@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe2.Poe2View
@import net.luversof.web.gate.poe.PoeText

@param Poe2.CommonGaps gaps = null

<%-- 닌자 공통 특성 중 이 빌드에 없는 것(10-14 C318, 사용자 요청 "빌드에서는 PoB 코드를 가져오면 개선할 점을 알려주는 업그레이드 가이드" + "닌자의 해당
     직업/스킬 특성을 참고") — buildGuide.jte 에서 따로 불러오는 조각. 엔진 계산 없이 인원만(효과는 위 표 · 시뮬레이터 다듬기가 잰다). --%>
!{
	boolean korean = PoeText.isKorean();
	String nm = gaps == null ? "" : (korean && gaps.skillKo() != null ? gaps.skillKo() : String.valueOf(gaps.skill()));
	String an = gaps == null ? "" : (korean && gaps.ascendancyKo() != null ? gaps.ascendancyKo() : String.valueOf(gaps.ascendancy()));
}
@if(gaps != null && gaps.n() > 0)
	<div class="rounded-box border border-base-200 bg-base-100 p-2 space-y-1.5 text-xs" data-guide-common-gaps>
		<div class="font-semibold">
			${korean ? "닌자 " + gaps.n() + "명 공통 특성 중 이 빌드에 없는 것" : "Common traits of " + gaps.n() + " ninja builds missing here"}
			<span class="font-normal text-base-content/60">(${an} · ${nm} — ${korean ? "보조젬 · 고유는 절반 이상, 특화 · 핵심은 4분의 3 이상이 쓰는 것. 효과는 빌드마다 달라 위 표 · 시뮬레이터 다듬기로 확인" : "supports and uniques used by half or more, notables and keystones by three quarters or more; effect differs per build — check the tables above or the simulator"})</span>
		</div>
		@if(gaps.supports().isEmpty() && gaps.uniques().isEmpty() && gaps.nodes().isEmpty())
			<div class="text-base-content/60" data-common-gaps-none>${korean ? "공통 특성을 이미 다 갖추고 있습니다." : "This build already has all the common traits."}</div>
		@endif
		@if(!gaps.supports().isEmpty())
			<div data-common-gaps="supports"><span class="text-base-content/60">${korean ? "보조젬" : "Supports"}:</span>
				@for(Poe2.CommonGap g : gaps.supports())<span class="badge badge-outline badge-sm mr-1">${korean && g.nameKo() != null ? g.nameKo() : g.name()} <span class="ml-1 font-mono text-base-content/60">${g.count()}/${gaps.n()}</span></span>@endfor
			</div>
		@endif
		@if(!gaps.uniques().isEmpty())
			<div data-common-gaps="uniques"><span class="text-base-content/60">${korean ? "고유" : "Uniques"}:</span>
				@for(Poe2.CommonGap g : gaps.uniques())<span class="badge badge-outline badge-sm mr-1"><span class="poe-name-unique">${korean && g.nameKo() != null ? g.nameKo() : g.name()}</span> <span class="ml-1 text-base-content/60">${Poe2View.slotName(g.slot())}</span> <span class="ml-1 font-mono text-base-content/60">${g.count()}/${gaps.n()}</span></span>@endfor
			</div>
		@endif
		@if(!gaps.nodes().isEmpty())
			<div data-common-gaps="nodes"><span class="text-base-content/60">${korean ? "패시브" : "Passives"}:</span>
				@for(Poe2.CommonGap g : gaps.nodes())<span class="badge badge-outline badge-sm mr-1">${korean && g.nameKo() != null ? g.nameKo() : g.name()} <span class="ml-1 font-mono text-base-content/60">${g.count()}/${gaps.n()}</span></span>@endfor
			</div>
		@endif
	</div>
@endif
