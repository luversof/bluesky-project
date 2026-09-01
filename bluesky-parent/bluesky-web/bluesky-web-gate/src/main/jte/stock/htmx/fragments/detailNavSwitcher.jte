@import java.util.List
@import net.luversof.web.gate.stock.domain.DetailNavEntry
@import io.github.luversof.boot.context.support.MessageUtil

@param List<DetailNavEntry> entries = java.util.List.of()
@param String switchLabel = ""

<%-- 갈 곳이 하나뿐(=지금 보는 것)이거나 없으면 전환기가 할 일이 없다. --%>
@if(entries != null && entries.size() > 1)
    <div class="dropdown dropdown-end" data-detail-nav>
        <div tabindex="0" role="button" class="btn btn-sm btn-ghost gap-1">
            <span class="whitespace-nowrap">${switchLabel}</span>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-3 h-3">
                <path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
            </svg>
        </div>
        <%-- 목록이 길어도 화면을 넘지 않게 스크롤을 둔다(실측 2026-08-31: 보유 종목 9). --%>
        <ul tabindex="0" class="dropdown-content z-[1] menu menu-sm bg-base-200 rounded-box shadow w-72 max-h-80 flex-nowrap overflow-y-auto p-2">
            @for(DetailNavEntry entry : entries)
                <li>
                    <a href="${entry.href()}" class="${entry.current() ? "active font-semibold" : ""} justify-between gap-3">
                        <span class="truncate">${entry.label()}</span>
                        @if(entry.detail() != null && !entry.detail().isEmpty())
                            <span class="amount-value shrink-0 text-[11px] opacity-60">${entry.detail()}</span>
                        @endif
                    </a>
                </li>
            @endfor
        </ul>
    </div>
@endif
