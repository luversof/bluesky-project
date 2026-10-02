@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe.PoeText

@param java.util.List<Poe2.SimChoice> skills = java.util.List.of()
@param java.util.List<Poe2.SimChoice> ascendancies = java.util.List.of()
@param java.util.List<String> popularAscendancies = java.util.List.of()
@param java.util.List<Poe2.SimClassGroup> classes = java.util.List.of()
@param String skill = ""
@param String ascendancy = ""

<%-- PoE2 시뮬 선택지 — PoE1 시뮬 폼(poe/htmx/simSelects.jte)과 같은 모양: **직업이 맨 앞**, 전직 셀렉트는 자동 → "많이 쓰는 직업"(상위 5)
     → 기본 직업별 묶음(optgroup, 트리 순서), 이름은 로케일 이름만(PoeText.name). 인원은 보여 주지 않는다.
     목록은 게임 데이터의 액티브 스킬·전직 전부, 순서는 poe.ninja 스냅샷 집계(새 버전이 나오면 API 가 다시 받는다) — 스킬은 고른 전직에서 많이 쓰는 순,
     "많이 쓰는 직업"은 고른 스킬을 많이 쓰는 순. 한쪽이 바뀌면 이 조각 전체를 다시 그린다(sim.jte 의 hx-trigger).
     스킬은 PoE1 과 같은 검색형 위젯의 단일 선택 모드 — PoE2 시뮬은 메인 스킬 하나로 실빌드 후보를 찾는다. --%>
!{
	boolean ko = PoeText.isKorean();
	String selSkill = skill == null || skill.isBlank() ? (skills.isEmpty() ? "" : skills.get(0).name()) : skill;
	java.util.Map<String, Poe2.SimChoice> ascByName = new java.util.HashMap<>();
	for (Poe2.SimChoice c : ascendancies) ascByName.put(c.name(), c);
	java.util.List<Poe2.SimClassGroup> groups = classes;
	if (groups.isEmpty() && !ascendancies.isEmpty()) {
		// 옛 API(묶음 없음) — 한 묶음으로라도 그린다
		groups = java.util.List.of(new Poe2.SimClassGroup("All", ko ? "전체" : "All", ascendancies));
	}
}
<div class="min-w-56">
	<label class="mb-1 block text-xs font-semibold text-base-content/70">${MessageUtil.getMessage("poe.sim.opt.ascendancy")}</label>
	<select name="ascendancy" class="select select-sm select-bordered w-full" title="${ko ? "전직" : "Ascendancy"}" data-sim-ascendancy>
		<option value="" selected="${ascendancy == null || ascendancy.isBlank()}">${ko ? "자동(전 전직 실빌드)" : "Auto (all ascendancies)"}</option>
		@if(!popularAscendancies.isEmpty())
			<optgroup label="${ko ? "많이 쓰는 직업" : "Most used"}">
				@for(String name : popularAscendancies)
					!{ Poe2.SimChoice c = ascByName.get(name); }
					<option value="${name}" selected="${name.equals(ascendancy)}">${c == null ? name : PoeText.name(c.nameKo(), c.name())}</option>
				@endfor
			</optgroup>
		@endif
		@for(Poe2.SimClassGroup group : groups)
			<optgroup label="${PoeText.name(group.nameKo(), group.name())}">
				@for(Poe2.SimChoice c : group.ascendancies())
					<option value="${c.name()}" selected="${c.name().equals(ascendancy) && !popularAscendancies.contains(c.name())}">${PoeText.name(c.nameKo(), c.name())}</option>
				@endfor
			</optgroup>
		@endfor
	</select>
</div>
<div class="min-w-52 flex-1">
	<label class="mb-1 block text-xs font-semibold text-base-content/70">${ko ? "주 스킬" : "Main skill"}<span class="ml-1 font-normal text-base-content/40">${ko ? "· 많이 쓰는 순" : "· by usage"}</span></label>
	<%-- data-poe-multi: PoE1 과 같은 검색형 위젯(multiSelect.js) — multiple 이 없으니 단일 선택 모드(칩 하나, 고르면 교체) --%>
	<select name="skill" data-poe-multi data-placeholder="${ko ? "스킬 선택…" : "Select skills…"}" class="select select-sm select-bordered w-full" title="${ko ? "주 스킬" : "Main skill"}" data-sim-skill>
		@for(Poe2.SimChoice c : skills)
			<option value="${c.name()}" selected="${c.name().equals(selSkill)}">${PoeText.name(c.nameKo(), c.name())}</option>
		@endfor
	</select>
</div>
