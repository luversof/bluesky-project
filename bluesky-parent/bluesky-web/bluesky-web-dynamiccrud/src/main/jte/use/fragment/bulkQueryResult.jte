@import java.util.List
@import java.util.Map
@import net.luversof.web.dynamiccrud.use.domain.BulkQueryResult

@param List<BulkQueryResult> results = null
@param String parseError = null

<div class="mt-2 flex flex-col gap-3">
@if(parseError != null)
        <div class="text-sm" style="color: var(--color-danger)">
                실행 오류: ${parseError}
        </div>
@elseif(results != null)
        @for(var r : results)
        <div>
                <div class="text-xs text-neutral-500 mb-1 whitespace-pre-wrap">${r.sql()}</div>
                @if("error".equals(r.type()))
                <div class="text-sm" style="color: var(--color-danger)">실행 오류: ${r.error()}</div>
                @elseif("update".equals(r.type()))
                <div class="text-sm text-base-content"><b>${String.valueOf(r.affectedRows())}</b> 행이 반영되었습니다.</div>
                @elseif("select".equals(r.type()))
                <div class="text-xs text-neutral-500 mb-1">조회 결과 ${r.rows().size()} 건</div>
                @if(!r.rows().isEmpty())
                <div class="overflow-auto" style="max-height: 32vh">
                        <table class="table table-compact bg-neutral-50">
                                <thead class="bg-neutral-50">
                                        <tr>
                                                @for(var key : r.rows().get(0).keySet())
                                                <th class="whitespace-nowrap">${key}</th>
                                                @endfor
                                        </tr>
                                </thead>
                                <tbody>
                                        @for(var row : r.rows())
                                        <tr>
                                                @for(var value : row.values())
                                                <td class="whitespace-nowrap">${value != null ? value.toString() : ""}</td>
                                                @endfor
                                        </tr>
                                        @endfor
                                </tbody>
                        </table>
                </div>
                @endif
                @endif
        </div>
        @endfor
@endif
</div>
