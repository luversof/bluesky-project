@import net.luversof.web.gate.poe.dto.PoeTattoo
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param PoeTattoo tattoo = null

<%-- PoE 문신(카루이) 게임 툴팁 — 호버 시 #poePreview 에 떠서 표시. 유니크/젬 아이템 레이어와 동형. --%>
@if(tattoo != null)
	!{
		boolean ko = PoeText.isKorean();
		String displayName = PoeText.name(tattoo.nameKo(), tattoo.name());
		java.util.List<String> lines = ko && tattoo.statsKo() != null && !tattoo.statsKo().isEmpty() ? tattoo.statsKo() : tattoo.stats();
		// 대상 타입 한글 라벨(문신 목록과 동일 관례)
		java.util.Map<String, String> typeLabels = new java.util.HashMap<>();
		typeLabels.put("Keystone", ko ? "키스톤" : "Keystone");
		typeLabels.put("Notable", ko ? "노터블" : "Notable");
		typeLabels.put("Mastery", ko ? "마스터리" : "Mastery");
		typeLabels.put("Small Attribute", ko ? "소형(속성)" : "Small attribute");
		typeLabels.put("Small Strength", ko ? "소형(힘)" : "Small strength");
		typeLabels.put("Small Dexterity", ko ? "소형(민첩)" : "Small dexterity");
		typeLabels.put("Small Intelligence", ko ? "소형(지능)" : "Small intelligence");
		String typeLabel = typeLabels.getOrDefault(tattoo.targetType(), tattoo.targetType());
		// 문신 아이콘(tree-icons 평탄화 규칙) — 히네코라 전용 아트 58종은 미추출이라 onerror 로 숨김
		String tattooIcon = tattoo.icon() != null && tattoo.icon().contains("SkillIcons/")
			? "tree/" + tattoo.icon().substring(tattoo.icon().indexOf("SkillIcons/") + 11).toLowerCase().replace("/", "_")
			: null;
	}
	<div class="poe-tooltip poe-tt-w-gem max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:#b57edc">
		<%-- 헤더 밴드 (이름 — 카루이 문신은 목록과 동일한 앰버색) --%>
		<div class="poe-tt-band">
			<div class="flex items-center justify-center gap-1.5">
				@if(tattooIcon != null)
					<img src="/poe-assets/${tattooIcon}" class="w-6 h-6 object-contain shrink-0" alt="" loading="lazy" onerror="this.style.display='none'">
				@endif
				<span class="text-center font-bold text-[15px] leading-tight text-amber-300 drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]">${displayName}</span>
			</div>
		</div>

		<%-- 대상 타입 + 연결 조건 (회색) --%>
		<div class="px-4 pt-1 text-center text-[11.5px] text-[#7f7f7f]">
			${ko ? "대체 대상" : "Replaces"}: ${typeLabel}
			@if(tattoo.minConnected() > 0)
				· ${ko ? "연결" : "adj"} ${tattoo.minConnected()}@if(tattoo.maxConnected() > tattoo.minConnected())–${tattoo.maxConnected()}@endif
			@endif
		</div>

		<%-- 문신 효과 스탯 라인 (파랑 — 모드 색) --%>
		@if(lines != null && !lines.isEmpty())
			<div class="poe-tt-sepimg"></div>
			<div class="px-4 py-1.5 text-center space-y-0.5">
				@for(String line : lines)
					<div class="text-[12.5px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
				@endfor
			</div>
		@endif

		<%-- 영문명(한글 로케일일 때 하단 회색 이탤릭) --%>
		@if(ko)
			<div class="px-4 py-2 text-center border-t border-white/10">
				<p class="text-[11px] italic text-white/25">${tattoo.name()}</p>
			</div>
		@endif
	</div>
@endif
