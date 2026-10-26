@import net.luversof.web.gate.poe.PoeText
@param String actionLabel
@param boolean actionPicked = false
@param String actionRarity = null
@param boolean hasMult = false
@param String actionError = null
@param boolean poe2 = false
@param String forcedText = null
<%-- 고른 화폐 기준 안내(10-22 P3 · Q2 — PoE1 · PoE2 craftPoolGroups 공용, Q3e 검토로 한 벌로). PoE1 = 처음 뽑는 옵션의 가중치 ÷ 합, PoE2 = 등급 · 징조가
     걸린 후보 중 균등. 사유가 있으면 고른 경우에도 보인다 --%>
!{ boolean ko = PoeText.isKorean(); }
<div class="rounded-box border border-warning/40 bg-warning/10 px-2 py-1.5 text-xs" data-craft-action-pool="${actionPicked ? "1" : "0"}">
	@if(actionPicked)
		<span class="font-medium">${ko ? "고른 화폐 기준" : "Armed currency"}</span> — <span data-craft-action-pool-label>${actionLabel}</span>: ${poe2 ? (ko ? "확률 = 이 수단이 처음 고르는 옵션이 그 줄일 확률(등급 · 징조가 걸린 후보 중 균등)" : "chance = odds the first modifier this picks is that line (uniform among candidates after tier and omens)") : (ko ? "확률 = 이 수단이 처음 뽑는 옵션 하나의 가중치 ÷ 합" : "chance = weight of the first modifier this rolls ÷ total")}@if("RARE".equals(actionRarity))${ko ? " (레어 자리 기준)" : " (as a rare)"}@endif@if(hasMult) · ${ko ? "파란 ×n = 이 화폐가 바꾼 가중치 배수" : "blue ×n = weight multiplier from this currency"}@endif@if(forcedText != null) · <span data-craft-action-forced>${ko ? "확정: " : "Guaranteed: "}${forcedText}</span>@endif@if(actionError != null) · ${actionError}@endif
	@else
		<span class="font-medium" data-craft-action-pool-label>${actionLabel}</span>@if(forcedText != null) — <span data-craft-action-forced>${ko ? "확정: " : "Guaranteed: "}${forcedText}</span>@endif — ${actionError != null ? actionError : poe2 ? (ko ? "이 수단은 표의 옵션을 무작위로 고르지 않습니다 — 지금 아이템 기준 표" : "This doesn't pick a random modifier from the table — showing odds for the current item") : (ko ? "이 수단은 표의 옵션을 무작위로 뽑지 않습니다 — 지금 아이템 기준 표" : "This doesn't roll a random modifier from the table — showing odds for the current item")}
	@endif
</div>
