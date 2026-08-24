@import io.github.luversof.boot.context.support.MessageUtil

@import java.util.UUID
@import java.util.List

@param UUID selectedAccountId = null
@param UUID selectedStockItemId = null
@param List<UUID> selectedAccountIds = java.util.List.of()
@param List<UUID> selectedStockItemIds = java.util.List.of()
@param List<String> selectedStockTags = java.util.List.of()
@param String badgeText = null

<%-- 기간은 이 배지의 판단 근거가 아니다.
     날짜가 안 실려 오면 컨트롤러가 프리셋(기본 YTD)으로 채우므로 startDate/endDate 는 사실상 항상 있다.
     그래서 예전 조건(기간이 있으면 필터로 인정)은 배지를 늘 켜 둬서 아무 정보도 주지 못했고,
     오히려 필터를 가장 적게 건 '전체 기간'(날짜 없음)에서만 꺼져 신호가 뒤집혀 있었다.
     기간 자체는 바로 위 기간 내비바가 "시작 ~ 끝"으로 항상 보여 주므로 여기서 빼도 잃는 정보가 없다. --%>
!{
    boolean hasFilter = (selectedAccountId != null)
        || (selectedStockItemId != null)
        || (selectedAccountIds != null && !selectedAccountIds.isEmpty())
        || (selectedStockItemIds != null && !selectedStockItemIds.isEmpty())
        || (selectedStockTags != null && !selectedStockTags.isEmpty())
        || (badgeText != null && !badgeText.isEmpty());
    String finalBadge = (badgeText != null && !badgeText.isEmpty()) ? badgeText : MessageUtil.getMessage("stock.label.filter.applied");
}

@if(hasFilter)
    <span class="badge badge-xs badge-primary">${finalBadge}</span>
@endif
