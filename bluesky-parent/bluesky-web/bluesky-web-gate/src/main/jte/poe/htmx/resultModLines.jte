@import java.util.List

<%-- 결과 아이템 모드 라인 — 선두 출처·젠 마커 "(접두)/(접미)/(에센스·접X)/(엘드리치)/(도유)" 를
     모드 페이지 관례색으로 렌더(접두 #8888ff · 접미 amber · 에센스 teal · 엘드리치 fuchsia). --%>
@param List<String> modLines
<%-- modLines 와 1:1 정렬된 티어 라벨("T{순위}/{총티어}"). 티어 없는 줄/구 히스토리는 빈 문자열 또는 null. --%>
@param List<String> modTiers = null

<%-- 인게임 아이템처럼 모드 한 줄씩(w-full 로 이름 아래로 내려 스택). 마커는 출처색 소자, 본문은 모드 파랑,
     티어(인게임 Alt 고급 모드 설명 파리티)는 마커 옆 회색 소자 배지. --%>
<div class="w-full mt-0.5 space-y-0.5 text-xs leading-5">
@for(int li = 0; li < modLines.size(); li++)
	!{
		String mLine = modLines.get(li);
		int mkEnd = mLine.startsWith("(") ? mLine.indexOf(") ") : -1;
		String mk = mkEnd > 0 ? mLine.substring(1, mkEnd) : null;
		String mRest = mk != null ? mLine.substring(mkEnd + 2) : mLine;
		String mkColor = mk == null ? ""
			: (mk.contains("접두") || mk.contains("prefix")) && !mk.contains("에센스") && !mk.contains("essence") ? "text-[#8888ff]"
			: (mk.contains("접미") || mk.contains("suffix")) && !mk.contains("에센스") && !mk.contains("essence") ? "text-amber-500"
			: mk.startsWith("에센스") || mk.startsWith("essence") ? "text-teal-500"
			: mk.contains("엘드리치") || mk.contains("eldritch") ? "text-fuchsia-500"
			: "text-base-content/45";
		String tier = modTiers != null && li < modTiers.size() ? modTiers.get(li) : null;
	}
	<div class="text-[#8888ff]">@if(mk != null)<span class="${mkColor} text-[10px]">(${mk})</span> @endif@if(tier != null && !tier.isEmpty())<span class="text-[10px] font-mono text-base-content/50 mr-0.5" title="인게임 고급 모드 설명(Alt)의 티어 — 최상위=T1">${tier}</span> @endif${mRest}</div>
@endfor
</div>
