@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeGroups
@import net.luversof.web.gate.poe.PoeText
@import io.github.luversof.boot.context.support.MessageUtil

@param List<PoeGroups.Group> groups = java.util.List.of()
@param String field
@param String target
@param String groupLabelPrefix
@param String allLabel
@param String itemLabelPrefix = null
@param String activeValue = "all"

<%-- 그룹 칩 필터 — 한 컨테이너(data-chip-group) 안의 모든 칩이 단일 선택(common.ts). 카테고리별 행으로 묶어 표시.
     항목 라벨: itemLabelPrefix 있으면 메시지 키(고유 카테고리), 없으면 데이터의 한/영(PoeText).
     data-slot: 탭 전환(일반↔고유) 시 필터 유지용 정규 슬롯. activeValue: 초기 활성 칩(탭 전환 진입값). --%>
<div class="space-y-1.5" data-chip-group data-chip-target="${target}" data-chip-field="${field}">
	<div class="flex flex-wrap items-center gap-1.5">
		<span class="w-16 shrink-0"></span>
		<button type="button" class="poe-chip ${"all".equals(activeValue) ? "poe-chip-active" : ""}" data-chip-value="all">${allLabel}</button>
	</div>
	@for(PoeGroups.Group group : groups)
		<div class="flex flex-wrap items-center gap-1.5">
			<span class="w-16 shrink-0 text-xs font-semibold text-base-content/50">${MessageUtil.getMessage(groupLabelPrefix + group.key())}</span>
			@for(PoeGroups.Entry entry : group.entries())
				<button type="button" class="poe-chip ${entry.key().equals(activeValue) ? "poe-chip-active" : ""}" data-chip-value="${entry.key()}" data-slot="${entry.slot() == null ? "" : entry.slot()}">${itemLabelPrefix != null ? MessageUtil.getMessage(itemLabelPrefix + entry.key()) : PoeText.name(entry.ko(), entry.key())}</button>
			@endfor
		</div>
	@endfor
</div>
