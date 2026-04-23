@import java.util.UUID
@import java.time.Instant

@param java.util.UUID selectedAccountId = null
@param java.util.UUID selectedStockItemId = null
@param java.time.Instant startDate = null
@param java.time.Instant endDate = null
@param String badgeText = null

!{
    boolean hasFilter = (selectedAccountId != null)
        || (selectedStockItemId != null)
        || (startDate != null)
        || (endDate != null)
        || (badgeText != null && !badgeText.isEmpty());
    String finalBadge = (badgeText != null && !badgeText.isEmpty()) ? badgeText : "필터 적용 중";
}

@if(hasFilter)
    <span class="badge badge-xs badge-primary">${finalBadge}</span>
@endif
