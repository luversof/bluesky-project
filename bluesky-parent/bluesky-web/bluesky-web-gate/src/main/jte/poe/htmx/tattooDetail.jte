@import net.luversof.web.gate.poe.dto.PoeTattoo
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeTattoo tattoo = null

<%-- PoE 문신(카루이) 게임 아이템 툴팁 — 유니크/젬과 동일한 게임 헤더밴드 시스템 사용.
     문신 · 룬 접목은 인게임 화폐(StackableCurrency) — 화폐 틀 · 이름색(poe-rar-currency, 10-23 Q77 — 예전엔 일반 등급 흰 틀). 호버 시 #poePreview 플로팅. --%>
@if(tattoo != null)
	!{
		boolean ko = PoeText.isKorean();
		String displayName = PoeText.name(tattoo.nameKo(), tattoo.name());
		java.util.List<String> lines = ko && tattoo.statsKo() != null && !tattoo.statsKo().isEmpty() ? tattoo.statsKo() : tattoo.stats();
		// 화폐 이름색(게임 #aa9e82) + 값 색 관례(라벨 회색 · 수치 흰 · 모드 파랑)
		String rarity = "#aa9e82";
		String label = "text-[#7f7f7f]";
		String value = "text-[#e0e0e0]";
		String aug = "text-[#8888ff]";
		// 대상 종류 낱말 = 게임(10-23 Q72 — ClientStrings PassiveSkillSearchNotable '주요' · Keystone '핵심노드' · Mastery '숙련' · Strength '힘' 등).
		//   'Attribute' 는 ClientStrings 에 없어 게임 한국어 관례 '능력치'(예: '모든 능력치'). 목록 칩은 사이트 UI 라 그대로
		java.util.Map<String, String> typeLabels = new java.util.HashMap<>();
		typeLabels.put("Keystone", ko ? "핵심노드" : "Keystone");
		typeLabels.put("Notable", ko ? "주요" : "Notable");
		typeLabels.put("Mastery", ko ? "숙련" : "Mastery");
		typeLabels.put("Small Attribute", ko ? "능력치" : "Attribute");
		typeLabels.put("Small Strength", ko ? "힘" : "Strength");
		typeLabels.put("Small Dexterity", ko ? "민첩" : "Dexterity");
		typeLabels.put("Small Intelligence", ko ? "지능" : "Intelligence");
		String typeLabel = typeLabels.getOrDefault(tattoo.targetType(), tattoo.targetType());
		// 대체 대상 패시브 문구(인게임 요구줄): "주요 스킬 스킬을 대체" / 소형은 "패시브 스킬을 대체"
		boolean bigTarget = tattoo.notable() || tattoo.keystone() || "Notable".equals(tattoo.targetType()) || "Keystone".equals(tattoo.targetType()) || "Mastery".equals(tattoo.targetType());
		// 게임 TattooDisplayReplaces '{0} 패시브 스킬 1개 교체' / 'Replaces one {0} Passive Skill'(Q72 — 예전 '… 패시브 스킬을 대체')
		String replaceLine = ko
			? (typeLabel + " 패시브 스킬 1개 교체")
			: ("Replaces one " + typeLabel + " Passive Skill");
		// 사용법 = 게임 CurrencyItems.Directions(Q71) — 룬 접목(숙련 대상)은 따로. 룬 접목 한국어 '수 습니다'는 게임 원문 오타 그대로
		boolean runegraft = "Mastery".equals(tattoo.targetType());
		String directions = runegraft
			? (ko ? "이 아이템을 우클릭한 뒤 할당되어 있는\n패시브 숙련 스킬을 좌클릭하십시오.\n같은 유형의 룬 접목은 한 번에\n하나씩만 사용할 수 습니다." : "Right click this item then left click\nan allocated Passive Mastery Skill.\nOnly one Runegraft of any given\ntype can be in use at a time.")
			: (ko ? "이 아이템을 우클릭한 뒤 할당되어 있는 패시브 스킬을 좌클릭하십시오. 문신은 최대 50개 적용할 수 있습니다." : "Right click this item then left click an allocated Passive Skill. Maximum 50 Tattoos.");
		// 문신 아이콘(tree-icons 평탄화 규칙) — 히네코라 전용 아트 58종은 미추출이라 onerror 로 숨김
		String tattooIcon = tattoo.icon() != null && tattoo.icon().contains("SkillIcons/")
			? "tree/" + tattoo.icon().substring(tattoo.icon().indexOf("SkillIcons/") + 11).toLowerCase().replace("/", "_")
			: null;
	}
	<div class="poe-tooltip poe-rar-currency poe-tt-w-gem max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
		<%-- 헤더 밴드 (게임 일반 아이템 아트 — 아이콘 + 이름) --%>
		<div class="poe-tt-band">
			<div class="flex items-center justify-center gap-1.5">
				@if(tattooIcon != null)
					<img src="/poe-assets/${tattooIcon}" class="w-7 h-7 object-contain shrink-0" alt="" loading="lazy" data-onerror="hide">
				@endif
				<span class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${displayName}</span>
			</div>
		</div>

		<%-- 분류 줄 없음(Q77 — 인게임 화폐 툴팁엔 종류 줄이 없고, 룬 접목에도 '문신'이라 나왔다). ko 영문 병기만(사이트 관례) --%>
		@if(ko)
			<div class="px-4 pt-1.5 text-center text-[10px] text-white/30">${tattoo.name()}</div>
		@endif

		<%-- 요구/제약 (대체 대상 + 연결 조건) --%>
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5">
			<div class="${label}">${replaceLine}</div>
			<%-- 인접 조건(10-23 Q73) — 게임 PassiveSkillTattooAdjacentRequirementLower · Upper1 문장. 상한 100 은 데이터의 '제한 없음'(게임 MaxAdjacent 0) --%>
			@if(tattoo.minConnected() > 0)
				<div data-tattoo-adjacent-min>${ko ? "할당되어 있는 인접 패시브 스킬이 " + tattoo.minConnected() + "개 이상이어야 합니다" : "Requires at least " + tattoo.minConnected() + " adjacent Passive Skills to be allocated"}</div>
			@endif
			@if(tattoo.maxConnected() == 1)
				<div data-tattoo-adjacent-max>${ko ? "할당되어 있는 인접 패시브 스킬이 1개 이하여야 합니다" : "Requires that no more than 1 adjacent Passive Skill is allocated"}</div>
			@elseif(tattoo.maxConnected() > 1 && tattoo.maxConnected() < 100)
				<div data-tattoo-adjacent-max>${ko ? "할당되어 있는 인접 패시브 스킬이 " + tattoo.maxConnected() + "개 이하여야 합니다" : "Requires that no more than " + tattoo.maxConnected() + " adjacent Passive Skills to be allocated"}</div>
			@endif
		</div>

		<%-- 문신 효과(모드) — 파랑 --%>
		@if(lines != null && !lines.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-2 text-center space-y-0.5">
				@for(String line : lines)
					<div class="text-[12px] leading-snug ${aug} whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
				@endfor
			</div>
		@endif

		<%-- 리마인더 (인게임 하단 회색 이탤릭 안내) --%>
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-2 text-center">
			<p class="text-[11px] leading-5 italic text-white/35 whitespace-pre-line" data-tattoo-directions>${directions}</p>
		</div>
	</div>
@endif
