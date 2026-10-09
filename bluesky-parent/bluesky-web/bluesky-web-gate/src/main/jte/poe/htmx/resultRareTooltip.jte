@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param String name
@param String nameKo
@param java.util.List<String> modLines
<%-- modLines 와 1:1 정렬된 티어 라벨. 재버킷팅(임플리싯/익스플리싯) 시 인덱스로 익스플리싯에만 실어 나른다. --%>
@param java.util.List<String> modTiers = null
@param net.luversof.web.gate.poe.dto.PoeBaseItem base = null
<%-- modLines 와 1:1 정렬된 줄별 리마인더(회색 부연, 10-04 C150 — 빌드 화면 C131 짝). 길이가 다르면 무시. --%>
@param java.util.List<java.util.List<String>> reminders = null
<%-- 결과 목록 행의 CSS 호버 레이어로 쓸 때(group-hover — 서버 호출 없이, 10-05 C151). 페이퍼돌 · 주얼은 data-poe-tip 복제라 false. --%>
@param boolean rowTip = false

<%-- 시뮬 결과 레어 아이템 인게임 툴팁 — 목록 행과 페이퍼돌 공용.
     엘드리치(총주교/포식자)·영향력(셰이퍼/엘더)은 인게임처럼 헤더 밴드 좌우 상단 모서리에 심볼 아이콘으로 표시.
     임플리싯(고정)은 구분선 위, 접두/접미(폭발)는 아래. 모드 마커별 인게임 배색. --%>
!{
	String rareLabel = MessageUtil.getMessage("poe.sim.opt.result.rare");
	String dName = PoeText.name(nameKo, name);
	java.util.List<String> lines = modLines == null ? java.util.List.of() : modLines;
	java.util.Set<String> implicitMarkers = new java.util.HashSet<>(java.util.List.of(
		"총주교", "포식자", "엘드리치", "exarch", "eater", "eldritch"));
	boolean hasExarch = false, hasEater = false, hasShaper = false, hasElder = false, hasEldritchGeneric = false;
	java.util.List<String> ttImplicits = new java.util.ArrayList<>();
	java.util.List<String> ttExplicits = new java.util.ArrayList<>();
	java.util.List<String> ttExplicitTiers = new java.util.ArrayList<>();
	java.util.List<java.util.List<String>> ttImplicitRems = new java.util.ArrayList<>();
	java.util.List<java.util.List<String>> ttExplicitRems = new java.util.ArrayList<>();
	boolean remAligned = reminders != null && reminders.size() == lines.size();
	for (int i = 0; i < lines.size(); i++) {
		String ml = lines.get(i);
		String mk = ml.startsWith("(") && ml.indexOf(") ") > 0 ? ml.substring(1, ml.indexOf(") ")) : null;
		String rest = mk != null ? ml.substring(ml.indexOf(") ") + 2) : ml;
		String tier = modTiers != null && i < modTiers.size() ? modTiers.get(i) : null;
		java.util.List<String> rem = remAligned && reminders.get(i) != null ? reminders.get(i) : java.util.List.of();
		if (mk != null && implicitMarkers.contains(mk)) {
			ttImplicits.add(rest);
			ttImplicitRems.add(rem);
			switch (mk) {
				case "총주교": case "exarch": hasExarch = true; break;
				case "포식자": case "eater": hasEater = true; break;
				default: hasEldritchGeneric = true;
			}
		} else {
			ttExplicits.add(ml);
			ttExplicitTiers.add(tier);
			ttExplicitRems.add(rem);
			if (mk != null && (mk.equals("셰이퍼") || mk.equalsIgnoreCase("shaper"))) hasShaper = true;
			if (mk != null && (mk.equals("엘더") || mk.equalsIgnoreCase("elder"))) hasElder = true;
		}
	}
	// 헤더 좌우 모서리 심볼 — 인게임 배치. 엘드리치는 총주교=좌/포식자=우, 단일이면 양쪽 동일. 영향력은 양쪽 동일.
	String uiBase = "/poe-data/ui-assets/";
	String exarchIco = uiBase + "influence-exarch.png", eaterIco = uiBase + "influence-eater.png";
	String shaperIco = uiBase + "influence-shaper.png", elderIco = uiBase + "influence-elder.png";
	String leftIco = null, rightIco = null;
	if (hasShaper) { leftIco = shaperIco; rightIco = shaperIco; }
	else if (hasElder) { leftIco = elderIco; rightIco = elderIco; }
	else if (hasExarch || hasEater || hasEldritchGeneric) {
		leftIco = hasExarch ? exarchIco : eaterIco;
		rightIco = hasEater ? eaterIco : exarchIco;
	}
}
<div class="poe-tooltip poe-rar-rare w-80 max-w-[calc(100vw-2rem)] max-h-[80vh] overflow-y-auto shadow-2xl ${rowTip ? "hidden group-hover:block absolute left-0 top-full z-30" : ""}" data-rare-tooltip="${rowTip ? "" : null}">
	<div class="poe-tt-band poe-tt-band-2">
		@if(leftIco != null)
			<img src="${leftIco}" style="position:absolute;left:8px;top:6px;width:30px;height:30px;object-fit:contain;filter:drop-shadow(0 1px 2px rgba(0,0,0,.9))" alt="">
		@endif
		@if(rightIco != null)
			<img src="${rightIco}" style="position:absolute;right:8px;top:6px;width:30px;height:30px;object-fit:contain;filter:drop-shadow(0 1px 2px rgba(0,0,0,.9))" alt="">
		@endif
		<div class="text-center font-bold text-[15px] leading-tight text-[#ffff77] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${dName}</div>
		<div class="text-center text-[13px] leading-tight text-[#ffff77] drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">@if(base != null)${net.luversof.web.gate.poe.PoeClassNames.poe1(base.itemClassKo(), base.itemClass())}@else${rareLabel}@endif</div>
	</div>

	<%-- 요구 사항 — 인게임은 **레벨이 먼저**다("요구 능력치 레벨 62, 180 힘"). 방어/무기 수치는 모드 반영값이
	     없어 생략(부정확 방지)하지만, 요구 사항은 베이스 고정값이라 정확히 보여줄 수 있다.
	     이 템플릿만 레벨이 빠지고 라벨·속성명이 하드코딩이라 다른 화면(itemTooltip·itemDetail, 사이클 12)과
	     표기가 달랐다 — 같은 메시지 키를 쓰고 형식을 통일한다.
	     ⚠ 한 줄로 붙여 쓸 것: @if 사이 줄바꿈이 공백으로 렌더돼 "레벨 62 , 180 힘"처럼 쉼표 앞이 벌어진다. --%>
	@if(base != null && (base.dropLevel() > 0 || base.reqStr() > 0 || base.reqDex() > 0 || base.reqInt() > 0))
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px]">
			<span class="text-[#7f7f7f]">${MessageUtil.getMessage("poe.items.detail.requirement")} </span>@if(base.dropLevel() > 0)<span class="text-[#7f7f7f]">${MessageUtil.getMessage("poe.items.detail.level")} </span><span class="text-[#e0e0e0]">${base.dropLevel()}</span>@endif@if(base.reqStr() > 0)<span class="text-[#e0e0e0]">${base.dropLevel() > 0 ? ", " : ""}${base.reqStr()} </span><span class="text-[#7f7f7f]">${MessageUtil.getMessage("poe.items.detail.str")}</span>@endif@if(base.reqDex() > 0)<span class="text-[#e0e0e0]">${(base.dropLevel() > 0 || base.reqStr() > 0) ? ", " : ""}${base.reqDex()} </span><span class="text-[#7f7f7f]">${MessageUtil.getMessage("poe.items.detail.dex")}</span>@endif@if(base.reqInt() > 0)<span class="text-[#e0e0e0]">${(base.dropLevel() > 0 || base.reqStr() > 0 || base.reqDex() > 0) ? ", " : ""}${base.reqInt()} </span><span class="text-[#7f7f7f]">${MessageUtil.getMessage("poe.items.detail.int")}</span>@endif
		</div>
	@endif

	<%-- (엘드리치) 임플리싯은 구분선 위(인게임 배치) — 팩션색 보라 계열 --%>
	@if(!ttImplicits.isEmpty())
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
			@for(int ii = 0; ii < ttImplicits.size(); ii++)
				<div class="text-[#d0a2f7]" data-tt-line>$unsafe{PoeText.highlightValues(ttImplicits.get(ii))}</div>
				@for(String rem : ttImplicitRems.get(ii))
					<div class="text-[11px] leading-snug text-[#7f7f7f]" data-tt-reminder>${rem}</div>
				@endfor
			@endfor
		</div>
	@endif

	<%-- 접두/접미(폭발) 모드 — 마커는 회색 소자, 본문 파랑, 수치 강조 --%>
	<div class="poe-tt-sepimg"></div>
	<div class="px-4 py-1.5 text-[12px] space-y-0.5 text-center">
		@for(int ei = 0; ei < ttExplicits.size(); ei++)
			!{
				String ml = ttExplicits.get(ei);
				int end = ml.startsWith("(") ? ml.indexOf(") ") : -1;
				String mk = end > 0 ? ml.substring(1, end) : null;
				String rest = mk != null ? ml.substring(end + 2) : ml;
				String tier = ei < ttExplicitTiers.size() ? ttExplicitTiers.get(ei) : null;
			}
			<div class="text-[#8888ff]" data-tt-line>@if(mk != null)<span class="text-[10px] text-white/35">(${mk})</span> @endif@if(tier != null && !tier.isEmpty())<span class="text-[10px] font-mono text-white/40 mr-0.5">${tier}</span> @endif$unsafe{PoeText.highlightValues(rest)}</div>
			@for(String rem : ttExplicitRems.get(ei))
				<div class="text-[11px] leading-snug text-[#7f7f7f]" data-tt-reminder>${rem}</div>
			@endfor
		@endfor
	</div>
</div>
