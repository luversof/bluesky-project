@import net.luversof.web.gate.poe.RankCompare
@import net.luversof.web.gate.poe.PoeText

@param RankCompare.Change change = null
@param boolean comparing = false

<%-- 순위 변화 · DPS 변화 두 칸(10-02, PoE1·PoE2 공용). 비교가 없으면 칸 자체를 안 그린다(머리줄도 같은 조건). --%>
@if(comparing)
	!{ boolean ko = PoeText.isKorean(); }
	@if(change == null || change.isNew())
		<td class="text-center text-xs font-semibold text-info" data-rank-change="new">${ko ? "신규" : "NEW"}</td>
		<td class="text-right text-xs text-base-content/60">-</td>
	@else
		<td class="text-center font-mono text-xs ${change.rankDelta() > 0 ? "text-success" : change.rankDelta() < 0 ? "text-error" : "text-base-content/60"}" data-rank-change="${change.rankDelta()}" title="${ko ? "이전 " : "Was #"}${change.prevRank()}${ko ? "위" : ""}">${change.rankDelta() > 0 ? "▲" + change.rankDelta() : change.rankDelta() < 0 ? "▼" + (-change.rankDelta()) : "–"}</td>
		<td class="text-right font-mono text-xs ${change.dpsPct() == null ? "text-base-content/60" : change.dpsPct() >= 0.05 ? "text-success" : change.dpsPct() <= -0.05 ? "text-error" : "text-base-content/60"}" data-dps-change>@if(change.dpsPct() == null)-@elseif(Math.abs(change.dpsPct()) < 0.05)0.0%@else${(change.dpsPct() > 0 ? "+" : "") + String.format("%.1f%%", change.dpsPct())}@endif</td>
	@endif
@endif
