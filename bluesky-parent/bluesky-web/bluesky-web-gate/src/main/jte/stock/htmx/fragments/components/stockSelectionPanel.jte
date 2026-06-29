@import java.util.List
@import net.luversof.web.gate.stock.domain.StockItem

@param String tagLabel
@param String stockLabel
@param String allLabel
@param String helperLabel = null
@param String selectedTagSummaryLabel = null
@param String selectedStockSummaryPattern = null
@param String stockPreviewMorePattern = null
@param List<String> stockTagList = java.util.List.of()
@param List<String> selectedStockTags = java.util.List.of()
@param List<StockItem> stockItemList = java.util.List.of()
@param List<StockItem> tagCountStockItemList = stockItemList

!{
    boolean hasTagOptions = stockTagList != null && !stockTagList.isEmpty();
    boolean hasSelectedTags = selectedStockTags != null && !selectedStockTags.isEmpty();
    java.util.function.Function<String, Long> stockTagCount = tag -> tagCountStockItemList != null
        ? tagCountStockItemList.stream()
            .filter(stockItem -> stockItem != null && stockItem.tags() != null && stockItem.tags().contains(tag))
            .count()
        : 0L;
}

@if(hasTagOptions)
    <div class="rounded-2xl border border-base-200 bg-base-200/35 px-3 py-3" data-stock-tag-filter>
        <select name="stockTagList" class="hidden" multiple data-no-multi="1" data-stock-tag-select>
            @for(String stockTag : stockTagList)
                <option value="${stockTag}" selected="${selectedStockTags != null && selectedStockTags.contains(stockTag)}">${stockTag}</option>
            @endfor
        </select>

        <div class="text-xs font-semibold uppercase tracking-[0.18em] text-base-content/45" data-stock-tag-label>${tagLabel}</div>
        @if(helperLabel != null && !helperLabel.isEmpty())
            <p class="mt-1 text-[11px] leading-4 text-base-content/55">${helperLabel}</p>
        @endif

        <div class="mt-3 flex flex-wrap gap-2">
            !{boolean isAllSelected = selectedStockTags == null || selectedStockTags.isEmpty();}
            <button type="button"
                    class="btn btn-sm h-9 min-h-0 rounded-full px-3 text-xs font-medium normal-case transition ${isAllSelected ? "btn-neutral shadow-sm" : "btn-outline border-base-300 bg-base-100 text-base-content/70"}"
                    data-stock-tag-clear
                    aria-pressed="${isAllSelected ? "true" : "false"}">
                ${allLabel}
            </button>
            @for(String stockTag : stockTagList)
                !{boolean selected = selectedStockTags != null && selectedStockTags.contains(stockTag);}
                <button type="button"
                        class="btn btn-sm h-9 min-h-0 rounded-full px-3 text-xs font-medium normal-case transition ${selected ? "btn-primary shadow-sm" : "btn-outline border-base-300 bg-base-100 text-base-content/75"}"
                        data-stock-tag-chip
                        data-tag-value="${stockTag}"
                        aria-pressed="${selected ? "true" : "false"}">
                    <span>${stockTag}</span>
                    <span class="badge badge-ghost badge-xs" data-stock-tag-count>${stockTagCount.apply(stockTag)}</span>
                </button>
            @endfor
        </div>

        @if(hasSelectedTags)
            <div class="mt-3 flex flex-wrap items-center gap-1.5 text-xs text-base-content/65">
                <span class="font-medium text-base-content/75">${selectedTagSummaryLabel}</span>
                @for(String stockTag : selectedStockTags)
                    <span class="badge badge-primary badge-outline gap-1 px-2 py-2">${stockTag}</span>
                @endfor
            </div>
        @endif
    </div>
@endif