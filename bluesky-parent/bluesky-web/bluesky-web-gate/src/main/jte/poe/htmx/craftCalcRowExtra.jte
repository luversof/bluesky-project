@import net.luversof.web.gate.poe.PoeText
@import net.luversof.web.gate.poe.PoeModRange
@import net.luversof.web.gate.poe.dto.PoeCraft

@param PoeCraft.PoolLine top
@param java.util.List<PoeCraft.PoolLine> tiers
@param String key
@param PoeCraft.CalcExtras extras = null
@param String infName = ""

<%-- 계산기 목표 줄 끝(10-21 C548 사용자 요청 "제작 계산기에 타락 · 정복자 · 제작 옵션이 안 보인다 · 태그도") — 옵션 태그(인게임 상세 보기 " — 태그"),
     고른 영향력 때문에 들어온 계열은 그 영향력 이름, 같은 작업대 옵션이 채우는 계열은 '작업대 ≈ Tn'(계산기 · 최적 조합이 이 계열 목표를 T1~Tn 이하로
     고르면 작업대로도 채운다 — benchCounts 규칙) --%>
!{
	boolean ko = PoeText.isKorean();
	java.util.List<String> tg = ko ? (top.tagsKo() == null ? java.util.List.of() : top.tagsKo()) : (top.tags() == null ? java.util.List.of() : top.tags());
	boolean inf = extras != null && extras.influenceIds() != null && tiers.stream().anyMatch(t -> extras.influenceIds().contains(t.id()));
	PoeCraft.BenchEquiv be = extras == null || extras.bench() == null ? null : extras.bench().stream().filter(b -> key.equals(b.gen() + ":" + b.family())).findFirst().orElse(null);
	String bench = "";
	if (be != null) {
		java.util.List<String> bx = ko && be.ko() != null && !be.ko().isEmpty() ? be.ko() : (be.en() == null ? java.util.List.<String>of() : be.en());
		java.util.List<String> bn = ko && be.koMin() != null && !be.koMin().isEmpty() ? be.koMin() : (be.enMin() == null ? java.util.List.<String>of() : be.enMin());
		java.util.List<String> parts = new java.util.ArrayList<>();
		for (int i = 0; i < bx.size(); i++) parts.add(PoeModRange.range(i < bn.size() ? bn.get(i) : null, bx.get(i)));
		bench = String.join(", ", parts);
	}
}
@if(!tg.isEmpty())<span class="text-[10px] text-base-content/60" data-craft-calc-tags> — ${String.join(", ", tg)}</span>@endif
@if(inf) <span class="badge badge-xs badge-outline badge-warning whitespace-nowrap" data-craft-calc-inf title="${ko ? infName + " 영향력 옵션 — 베이스가 이 영향력일 때만 붙는다" : infName + " influence modifier — only on a base with this influence"}">${infName}</span>@endif
@if(be != null) <span class="badge badge-xs badge-outline badge-info whitespace-nowrap" data-craft-calc-bench="${be.tier()}" title="${ko ? "작업대 옵션: " + bench + " — 이 계열 목표를 T1~T" + be.tier() + " 이하로 고르면 계산기 · 최적 조합이 작업대로도 채웁니다(빈 자리 · 같은 그룹이 없을 때)" : "Bench craft: " + bench + " — with this family's target at T1~T" + be.tier() + " or looser, the calculator and optimizer may finish it on the bench"}">${ko ? "작업대" : "Bench"} ≈ T${be.tier()}</span>@endif
