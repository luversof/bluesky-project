@import java.util.List
@import net.luversof.web.gate.stock.domain.Account
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Account> accounts = null
@param List<Integer> years
@param int currentYear
@param int currentMonth

!{
    java.util.function.Function<Object, String> yearLabel = value -> java.text.MessageFormat.format(MessageUtil.getMessage("stock.label.year"), value);
    java.util.function.Function<Object, String> monthLabel = value -> java.text.MessageFormat.format(MessageUtil.getMessage("stock.label.month"), value);
}

<!-- Daily Summary: 컨트롤 + 데이터를 하나의 카드로 통합 -->
<div class="card bg-base-100 border border-base-200 rounded-box shadow-sm">

    <!-- Filters Form (카드 상단 고정 컨트롤 영역) -->
    <form id="daily-summary-filter-form"
          hx-get="/stock/htmx/daily-summary/data"
          hx-target="#daily-summary-content"
          hx-trigger="submit, load"
          hx-indicator="#daily-summary-loading"
          class="border-b border-base-200">

        <!-- Top Control Panel -->
        <div class="flex flex-col lg:flex-row items-center justify-between gap-4 p-4">

            <!-- Left: Type & Period -->
            <div class="flex flex-wrap items-center justify-center gap-2 sm:gap-4 w-full lg:w-auto">
                <!-- Type -->
                <div class="join shadow-sm">
                    <input class="join-item btn btn-sm" type="radio" name="type" value="PROFIT" aria-label="${MessageUtil.getMessage("stock.summary.type.profit")}" checked onclick="updateDailySummaryUI()"/>
                    <input class="join-item btn btn-sm" type="radio" name="type" value="DIVIDEND" aria-label="${MessageUtil.getMessage("stock.summary.type.dividend")}" onclick="updateDailySummaryUI()"/>
                </div>

                <!-- Separator (Visual) -->
                <div class="hidden sm:block w-px h-6 bg-base-content/20"></div>

                <!-- Period -->
                <div class="join shadow-sm" id="period-group">
                    <input class="join-item btn btn-sm" type="radio" name="timeScale" value="TOTAL" id="period-total" aria-label="${MessageUtil.getMessage("stock.summary.scale.total")}" checked onclick="updateDailySummaryUI()"/>
                    <input class="join-item btn btn-sm" type="radio" name="timeScale" value="YEARLY" id="period-yearly" aria-label="${MessageUtil.getMessage("stock.summary.scale.yearly")}" onclick="updateDailySummaryUI()"/>
                    <input class="join-item btn btn-sm" type="radio" name="timeScale" value="MONTHLY" id="period-monthly" aria-label="${MessageUtil.getMessage("stock.summary.scale.monthly")}" onclick="updateDailySummaryUI()"/>
                </div>
            </div>

            <!-- Right: Grouping & Selects -->
            <div class="flex flex-wrap items-center justify-center gap-2 w-full lg:w-auto">
                <!-- Grouping -->
                <div class="join shadow-sm">
                    <input class="join-item btn btn-sm" type="radio" name="groupBy" value="STOCK" aria-label="${MessageUtil.getMessage("stock.label.by.stock")}" onclick="updateDailySummaryUI()"/>
                    <input class="join-item btn btn-sm" type="radio" name="groupBy" value="ACCOUNT" aria-label="${MessageUtil.getMessage("stock.label.by.account")}" onclick="updateDailySummaryUI()"/>
                    <input class="join-item btn btn-sm" type="radio" name="groupBy" value="SUMMARY" aria-label="${MessageUtil.getMessage("stock.label.summary")}" checked onclick="updateDailySummaryUI()"/>
                </div>

                <!-- Year Filter -->
                <div class="select-wrapper" style="display:none;" id="filter-year-wrapper">
                    <select name="year" id="filter-year" class="select select-bordered select-sm w-24" data-no-multi="1" onchange="updateDailySummaryUI()">
                        @for(Integer y : years)
                            <option value="${y}" selected="${y == currentYear}">${yearLabel.apply(y)}</option>
                        @endfor
                    </select>
                </div>

                <!-- Month Filter -->
                <div class="select-wrapper" style="display:none;" id="filter-month-wrapper">
                    <select name="month" id="filter-month" class="select select-bordered select-sm w-20" data-no-multi="1" onchange="updateDailySummaryUI()">
                        @for(int m = 1; m <= 12; m++)
                            <option value="${m}" selected="${m == currentMonth}">${monthLabel.apply(m)}</option>
                        @endfor
                    </select>
                </div>

                <!-- Account Filter -->
                <div class="select-wrapper">
                    <select name="accountId" id="filter-account" class="select select-bordered select-sm w-40" data-no-multi="1" onchange="updateDailySummaryUI()">
                        <option value="">${MessageUtil.getMessage("stock.summary.account.all")}</option>
                        @if(accounts != null)
                            @for(Account acc : accounts)
                                <option value="${acc.id().toString()}">${acc.name()}</option>
                            @endfor
                        @endif
                    </select>
                </div>
            </div>

        </div>

        <!-- Loading Indicator -->
        <div id="daily-summary-loading" class="htmx-indicator hidden flex justify-center py-2">
            <span class="loading loading-spinner text-primary"></span>
        </div>

        <script src="/js/stock/dailySummary.js"></script>

    </form>

    <!-- Data Content Area (카드 하단, 컨트롤과 같은 카드 내부) -->
    <div id="daily-summary-content" class="min-h-[300px]">
        <div class="flex justify-center py-10">
            <span class="loading loading-spinner text-primary"></span>
        </div>
    </div>

</div>