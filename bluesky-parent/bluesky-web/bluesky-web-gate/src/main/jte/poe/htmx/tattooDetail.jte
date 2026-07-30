@import net.luversof.web.gate.poe.dto.PoeTattoo
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeTattoo tattoo = null

<%-- PoE 문신(카루이) 게임 아이템 툴팁 — 유니크/젬과 동일한 게임 헤더밴드 시스템 사용.
     문신은 인게임 일반(Normal) 등급 아이템이라 흰색 헤더(poe-rar-white). 호버 시 #poePreview 플로팅. --%>
@if(tattoo != null)
	!{
		boolean ko = PoeText.isKorean();
		String displayName = PoeText.name(tattoo.nameKo(), tattoo.name());
		java.util.List<String> lines = ko && tattoo.statsKo() != null && !tattoo.statsKo().isEmpty() ? tattoo.statsKo() : tattoo.stats();
		// 일반 등급 색(게임 아이템명) + 값 색 관례(라벨 회색 · 수치 흰 · 모드 파랑)
		String rarity = "#c8c8c8";
		String label = "text-[#7f7f7f]";
		String value = "text-[#e0e0e0]";
		String aug = "text-[#8888ff]";
		// 대상 타입 한글 라벨(문신 목록과 동일 관례)
		java.util.Map<String, String> typeLabels = new java.util.HashMap<>();
		typeLabels.put("Keystone", ko ? "키스톤" : "Keystone");
		typeLabels.put("Notable", ko ? "노터블" : "Notable");
		typeLabels.put("Mastery", ko ? "마스터리" : "Mastery");
		typeLabels.put("Small Attribute", ko ? "속성" : "Attribute");
		typeLabels.put("Small Strength", ko ? "힘" : "Strength");
		typeLabels.put("Small Dexterity", ko ? "민첩" : "Dexterity");
		typeLabels.put("Small Intelligence", ko ? "지능" : "Intelligence");
		String typeLabel = typeLabels.getOrDefault(tattoo.targetType(), tattoo.targetType());
		// 대체 대상 패시브 문구(인게임 요구줄): "노터블 패시브 스킬을 대체" / 소형은 "패시브 스킬을 대체"
		boolean bigTarget = tattoo.notable() || tattoo.keystone() || "Notable".equals(tattoo.targetType()) || "Keystone".equals(tattoo.targetType()) || "Mastery".equals(tattoo.targetType());
		String replaceLine = ko
			? (typeLabel + " 패시브 스킬을 대체")
			: ("Replaces a " + typeLabel + " Passive Skill");
		// 문신 아이콘(tree-icons 평탄화 규칙) — 히네코라 전용 아트 58종은 미추출이라 onerror 로 숨김
		String tattooIcon = tattoo.icon() != null && tattoo.icon().contains("SkillIcons/")
			? "tree/" + tattoo.icon().substring(tattoo.icon().indexOf("SkillIcons/") + 11).toLowerCase().replace("/", "_")
			: null;
	}
	<div class="poe-tooltip poe-rar-white poe-tt-w-gem max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:${rarity}">
		<%-- 헤더 밴드 (게임 일반 아이템 아트 — 아이콘 + 이름) --%>
		<div class="poe-tt-band">
			<div class="flex items-center justify-center gap-1.5">
				@if(tattooIcon != null)
					<img src="/poe-assets/${tattooIcon}" class="w-7 h-7 object-contain shrink-0" alt="" loading="lazy" onerror="this.style.display='none'">
				@endif
				<span class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:${rarity}">${displayName}</span>
			</div>
		</div>

		<%-- 아이템 클래스(문신) + 영문 병기 --%>
		<div class="px-4 pt-1.5 text-center text-[11px]">
			<span class="text-white/70">${ko ? "문신" : "Tattoo"}</span>
			@if(ko)<div class="text-[10px] text-white/30">${tattoo.name()}</div>@endif
		</div>

		<%-- 요구/제약 (대체 대상 + 연결 조건) --%>
		<div class="poe-tt-sepimg"></div>
		<div class="px-4 py-1.5 text-center text-[12px] space-y-0.5">
			<div class="${label}">${replaceLine}</div>
			@if(tattoo.minConnected() > 0)
				<div><span class="${label}">${ko ? "연결된 패시브 필요: " : "Connected passives: "}</span><span class="${value}">${tattoo.minConnected()}@if(tattoo.maxConnected() > tattoo.minConnected())–${tattoo.maxConnected()}@endif</span></div>
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
			<p class="text-[11px] leading-5 italic text-white/35">${ko ? "이 아이템을 우클릭한 다음 패시브 스킬을 좌클릭하면 적용됩니다." : "Right click this item then left click a passive skill to apply it."}</p>
		</div>
	</div>
@endif
