@import java.util.List
@import net.luversof.web.gate.poe.PoeText

@param List<String> seasons = java.util.List.of()
@param String season = ""
@param String against = ""
@param String url
@param String againstPatch = ""

<%-- 젬 DPS 랭킹 시즌 고르기 + 비교 시즌(10-02 사용자 요청, PoE1·PoE2 공용). 바꾸면 같은 조각을 그 시즌으로 다시 그린다(가장 가까운 [data-ranking-root] 를 통째로).
     시즌 = 패치 앞 두 마디(3.29 · 4.5) — 배치가 끝날 때마다 그 시즌 보관본이 갱신되고, 시즌이 바뀌면 이전 시즌이 그대로 남는다. --%>
!{ boolean ko = PoeText.isKorean(); }
<form class="flex flex-wrap items-center gap-2 text-xs" data-rank-seasons
      hx-get="${url}" hx-trigger="change" hx-target="closest [data-ranking-root]" hx-swap="outerHTML">
	<label class="flex items-center gap-1">
		<span class="text-base-content/60">${ko ? "시즌" : "Season"}</span>
		<select name="season" class="select select-xs select-bordered" aria-label="${ko ? "랭킹 시즌" : "Ranking season"}">
			@for(String s : seasons)
				<option value="${s.equals(seasons.get(0)) ? "" : s}" selected="${s.equals(season)}">${s}</option>
			@endfor
		</select>
	</label>
	<label class="flex items-center gap-1">
		<span class="text-base-content/60">${ko ? "비교" : "Compare with"}</span>
		<select name="against" class="select select-xs select-bordered" aria-label="${ko ? "비교할 시즌" : "Season to compare"}">
			@for(String s : seasons)
				@if(!s.equals(season))
					<option value="${s}" selected="${s.equals(against)}">${s}</option>
				@endif
			@endfor
			@if(seasons.size() < 2)
				<option value="" selected>${ko ? "이전 시즌 없음" : "No previous season"}</option>
			@endif
		</select>
	</label>
	@if(against.isBlank())
		<span class="text-base-content/50" data-rank-no-compare>${ko ? "이전 시즌 랭킹이 아직 없습니다 — 다음 시즌에 랭킹을 돌리면 순위 변화가 나옵니다." : "No previous season yet — rankings run next season will show changes."}</span>
	@else
		<span class="text-base-content/50">${ko ? "비교 기준 " : "Compared with "}${againstPatch}</span>
	@endif
</form>
