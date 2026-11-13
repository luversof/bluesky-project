@import java.util.List
@import java.util.Map
@import net.luversof.web.dynamiccrud.use.domain.BulkQueryResult

@param List<BulkQueryResult> results = null
@param String parseError = null

<div class="mt-2 flex flex-col gap-4">
@if(parseError != null)
        <div class="text-sm text-error">실행 오류: ${parseError}</div>
@elseif(results != null)
        @for(var r : results)
        <div>
                <div class="mb-1 font-mono text-xs text-base-content/60 whitespace-pre-wrap">${r.sql()}</div>
                @if("error".equals(r.type()))
                <div class="text-sm text-error">실행 오류: ${r.error()}</div>
                @elseif("update".equals(r.type()))
                <div class="text-sm"><span class="font-mono font-medium tabular-nums">${String.format("%,d", r.affectedRows())}</span>행 반영</div>
                @elseif("select".equals(r.type()))
                <div class="mb-1 text-xs text-base-content/60">조회 결과 <span class="font-mono tabular-nums">${String.format("%,d", r.rows().size())}</span>건</div>
                @if(!r.rows().isEmpty())
                <div class="overflow-auto border border-base-300" style="max-height: 32vh">
                        <table class="table table-xs table-pin-rows tabular-nums">
                                <thead>
                                        <tr>
                                                @for(var key : r.rows().get(0).keySet())
                                                <th class="whitespace-nowrap font-medium">${key}</th>
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
