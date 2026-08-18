@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe.dto.PoeGem
@import net.luversof.web.gate.poe.dto.PoeUniqueItem
@import net.luversof.web.gate.poe.PoeText

@param List<PoeGem> activeGems = java.util.List.of()
@param List<PoeUniqueItem> uniqueItems = java.util.List.of()
@param java.util.Set<String> preSkills = java.util.Set.of()
@param java.util.Set<String> preUniques = java.util.Set.of()
@param String ascendancy = ""
<%-- 실빌드 사용 빈도 순서(API /api/poe/meta/order). 비면 원래 순서 그대로 — 데이터 없다고 화면이 망가지면 안 된다. --%>
@param List<String> skillOrder = java.util.List.of()
@param List<String> ascendancyOrder = java.util.List.of()
@param List<String> itemOrder = java.util.List.of()

<%-- 시뮬 폼의 세 선택 — **직업이 맨 앞**이다. 직업을 먼저 정하면 그 직업이 많이 쓰는 스킬이 위로 올라오고,
     스킬을 먼저 고르면 그 스킬을 많이 쓰는 직업이 위로 올라온다(서로를 좁혀 준다).
     정렬 근거는 poe.ninja 현재 리그 표본이라 "지금 사람들이 실제로 쓰는 순서"다. 목록에서 빼지는 않는다 —
     패싯이 아키타입당 상위 12개로 잘려 있어 "목록에 없음 = 안 쓰임"이 아니기 때문이다. --%>
!{
	boolean ko = PoeText.isKorean();
	// 인기 순위 → 정렬 키. 목록에 없으면 뒤로(원래 순서 유지).
	java.util.Map<String, Integer> skillRank = new java.util.HashMap<>();
	for (int i = 0; i < skillOrder.size(); i++) skillRank.putIfAbsent(skillOrder.get(i), i);
	java.util.Map<String, Integer> itemRank = new java.util.HashMap<>();
	for (int i = 0; i < itemOrder.size(); i++) itemRank.putIfAbsent(itemOrder.get(i), i);
	java.util.List<PoeGem> gems = new java.util.ArrayList<>(activeGems);
	gems.sort(java.util.Comparator.comparingInt(g -> skillRank.getOrDefault(g.name(), Integer.MAX_VALUE)));
	java.util.List<PoeUniqueItem> uniques = new java.util.ArrayList<>(uniqueItems);
	uniques.sort(java.util.Comparator.comparingInt(u -> itemRank.getOrDefault(u.name(), Integer.MAX_VALUE)));
	int popularSkills = (int) gems.stream().filter(g -> skillRank.containsKey(g.name())).count();
	int popularItems = (int) uniques.stream().filter(u -> itemRank.containsKey(u.name())).count();
	// 전직 한글명 — 셀렉트를 서버에서 재배치하므로 여기서 이름을 만든다
	java.util.Map<String, String> ascKo = java.util.Map.ofEntries(
		java.util.Map.entry("Ascendant", "어센던트"), java.util.Map.entry("Reliquarian", "렐리쿼리언"),
		java.util.Map.entry("Juggernaut", "저거넛"), java.util.Map.entry("Berserker", "버서커"), java.util.Map.entry("Chieftain", "치프틴"),
		java.util.Map.entry("Warden", "워든"), java.util.Map.entry("Deadeye", "데드아이"), java.util.Map.entry("Pathfinder", "패스파인더"),
		java.util.Map.entry("Occultist", "오컬티스트"), java.util.Map.entry("Elementalist", "엘리멘탈리스트"), java.util.Map.entry("Necromancer", "네크로맨서"),
		java.util.Map.entry("Slayer", "슬레이어"), java.util.Map.entry("Gladiator", "글래디에이터"), java.util.Map.entry("Champion", "챔피언"),
		java.util.Map.entry("Inquisitor", "인퀴지터"), java.util.Map.entry("Hierophant", "하이로펀트"), java.util.Map.entry("Guardian", "가디언"),
		java.util.Map.entry("Assassin", "어쌔신"), java.util.Map.entry("Trickster", "트릭스터"), java.util.Map.entry("Saboteur", "사보추어"));
	java.util.List<java.util.Map.Entry<String, List<String>>> classGroups = java.util.List.of(
		java.util.Map.entry("사이온|Scion", List.of("Ascendant", "Reliquarian")),
		java.util.Map.entry("머라우더|Marauder", List.of("Juggernaut", "Berserker", "Chieftain")),
		java.util.Map.entry("레인저|Ranger", List.of("Warden", "Deadeye", "Pathfinder")),
		java.util.Map.entry("위치|Witch", List.of("Occultist", "Elementalist", "Necromancer")),
		java.util.Map.entry("듀얼리스트|Duelist", List.of("Slayer", "Gladiator", "Champion")),
		java.util.Map.entry("템플러|Templar", List.of("Inquisitor", "Hierophant", "Guardian")),
		java.util.Map.entry("섀도우|Shadow", List.of("Assassin", "Trickster", "Saboteur")));
	java.util.List<String> topAsc = ascendancyOrder.stream().filter(ascKo::containsKey).limit(5).toList();
}

<div class="min-w-56">
	<label class="mb-1 block text-xs font-semibold text-base-content/70">${MessageUtil.getMessage("poe.sim.opt.ascendancy")}</label>
	<select name="ascendancy" class="select select-sm select-bordered w-full" title="${MessageUtil.getMessage("poe.sim.opt.ascendancy")}">
		<option value="" selected="${ascendancy.isBlank()}">${MessageUtil.getMessage("poe.sim.opt.ascendancy.auto")}</option>
		@if(!topAsc.isEmpty())
			<optgroup label="${ko ? "많이 쓰는 직업" : "Most used"}">
				@for(String asc : topAsc)
					<option value="${asc}" selected="${asc.equals(ascendancy)}">${ko ? ascKo.get(asc) : asc}</option>
				@endfor
			</optgroup>
		@endif
		@for(var group : classGroups)
			<optgroup label="${ko ? group.getKey().split("\\|")[0] : group.getKey().split("\\|")[1]}">
				@for(String asc : group.getValue())
					<option value="${asc}" selected="${asc.equals(ascendancy)}">${ko ? ascKo.get(asc) : asc}</option>
				@endfor
			</optgroup>
		@endfor
	</select>
</div>
<div class="min-w-52 flex-1">
	<label class="mb-1 block text-xs font-semibold text-base-content/70">${MessageUtil.getMessage("poe.sim.opt.skills")}
		@if(popularSkills > 0)<span class="ml-1 font-normal text-base-content/40">${ko ? "· 많이 쓰는 순 " + popularSkills : "· by usage " + popularSkills}</span>@endif
	</label>
	<select name="skills" multiple data-poe-multi data-placeholder="${MessageUtil.getMessage("poe.sim.opt.skills.placeholder")}" size="4" class="select select-sm select-bordered w-full">
		@for(PoeGem skillOpt : gems)
			<option value="${skillOpt.slug()}" selected="${preSkills.contains(skillOpt.slug())}">${PoeText.name(skillOpt.nameKo(), skillOpt.name())}</option>
		@endfor
	</select>
</div>
<div class="min-w-52 flex-1">
	<label class="mb-1 block text-xs font-semibold text-base-content/70">${MessageUtil.getMessage("poe.sim.opt.uniques")}
		@if(popularItems > 0)<span class="ml-1 font-normal text-base-content/40">${ko ? "· 많이 쓰는 순 " + popularItems : "· by usage " + popularItems}</span>@endif
	</label>
	<select name="uniques" multiple data-poe-multi data-placeholder="${MessageUtil.getMessage("poe.sim.opt.uniques.placeholder")}" size="4" class="select select-sm select-bordered w-full">
		@for(PoeUniqueItem uniqueItem : uniques)
			<option value="${uniqueItem.slug()}" selected="${preUniques.contains(uniqueItem.slug())}">${PoeText.name(uniqueItem.nameKo(), uniqueItem.name())}</option>
		@endfor
	</select>
</div>
