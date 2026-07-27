@import java.util.List

<%-- 결과 아이템 모드 라인 — 선두 출처·젠 마커 "(접두)/(접미)/(에센스·접X)/(엘드리치)/(도유)" 를
     모드 페이지 관례색으로 렌더(접두 #8888ff · 접미 amber · 에센스 teal · 엘드리치 fuchsia). --%>
@param List<String> modLines

<span class="text-xs text-base-content/60">
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
	}
	@if(li > 0)<span class="text-base-content/30"> / </span>@endif
	@if(mk != null)<span class="${mkColor}">(${mk})</span> @endif${mRest}
@endfor
</span>
