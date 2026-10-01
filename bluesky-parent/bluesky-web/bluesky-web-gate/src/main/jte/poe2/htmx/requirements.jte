@import net.luversof.web.gate.poe2.dto.Poe2
@import io.github.luversof.boot.context.support.MessageUtil

@param Integer level = null
@param Poe2.BaseItem base = null

<%-- 툴팁 요구 사항 줄 — PoE1 poe/htmx/uniqueDetail.jte 의 "요구 사항 레벨 N, 힘 …" 과 같은 마크업·메시지 키(10-01 인게임 정합: PoE2 툴팁엔 이 줄이 없었다).
     인게임처럼 라벨 회색 · 수치 흰색, 속성 배치 칸 다음 · 암시 앞. 레벨이 1 이하이고 능력치 요구도 없으면 그리지 않는다. --%>
!{
	String label = "text-[#7f7f7f]";
	String value = "text-[#e0e0e0]";
	int str = base == null || base.reqStr() == null ? 0 : base.reqStr();
	int dex = base == null || base.reqDex() == null ? 0 : base.reqDex();
	int intel = base == null || base.reqInt() == null ? 0 : base.reqInt();
	boolean hasLevel = level != null && level > 1;
}
@if(hasLevel || str > 0 || dex > 0 || intel > 0)
	<div class="poe-tt-sepimg"></div>
	<div class="px-4 py-1.5 text-center text-[12px]" data-tt-requirements>
		<span class="${label}">${MessageUtil.getMessage("poe.items.detail.requirement")} </span>
		@if(hasLevel)<span class="${label}">${MessageUtil.getMessage("poe.items.detail.level")} </span><span class="${value}">${level}</span>@endif@if(str > 0)<span class="${value}">${hasLevel ? ", " : ""}${str} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.str")}</span>@endif@if(dex > 0)<span class="${value}">${hasLevel || str > 0 ? ", " : ""}${dex} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.dex")}</span>@endif@if(intel > 0)<span class="${value}">${hasLevel || str > 0 || dex > 0 ? ", " : ""}${intel} </span><span class="${label}">${MessageUtil.getMessage("poe.items.detail.int")}</span>@endif
	</div>
@endif
