@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil

@param List<Integer> years
@param int currentYear
@param int currentMonth

!{
    java.util.function.Function<Object, String> yearLabel = value -> java.text.MessageFormat.format(MessageUtil.getMessage("stock.label.year"), value);
    java.util.function.Function<Object, String> monthLabel = value -> java.text.MessageFormat.format(MessageUtil.getMessage("stock.label.month"), value);
}

<div class="flex flex-col gap-6">
    <!-- Tabs -->
    <div role="tablist" class="tabs tabs-box bg-base-200 p-1">
         <a role="tab" class="tab tab-lg flex-1 transition-all duration-300 tab-active !bg-primary !text-primary-content font-bold" 
            id="tab-profit" 
            onclick="selectAnalyticsTab('PROFIT')">${MessageUtil.getMessage("stock.analytics.tab.profit")}</a>
         <a role="tab" class="tab tab-lg flex-1 transition-all duration-300 font-bold" 
            id="tab-dividend" 
            onclick="selectAnalyticsTab('DIVIDEND')">${MessageUtil.getMessage("stock.analytics.tab.dividend")}</a>
    </div>

    <!-- Filter Form -->
    <form id="analytics-filter-form"
          hx-get="/stock/htmx/analytics/data"
          hx-target="#analytics-content"
          hx-trigger="submit, change delay:200ms, load">
          
        <!-- Hidden Type -->
        <input type="hidden" name="type" id="input-type" value="PROFIT">
        
        <div class="flex flex-wrap gap-4 items-center justify-between bg-base-100 p-2 rounded-lg border border-base-200">
             <!-- Time Scale -->
             <div class="join shadow-sm">
                    <input class="join-item btn btn-sm" type="radio" name="timeScale" value="YEARLY" aria-label="${MessageUtil.getMessage("stock.analytics.scale.yearly")}" checked onchange="updateAnalyticsVis()"/>
                    <input class="join-item btn btn-sm" type="radio" name="timeScale" value="MONTHLY" aria-label="${MessageUtil.getMessage("stock.analytics.scale.monthly")}" onchange="updateAnalyticsVis()"/>
                    <input class="join-item btn btn-sm" type="radio" name="timeScale" value="TOTAL" aria-label="${MessageUtil.getMessage("stock.analytics.scale.total")}" onchange="updateAnalyticsVis()"/>
             </div>

             <!-- Details -->
             <div class="flex flex-wrap gap-2 items-center">
                 <div id="wrapper-year" style="display:block;">
                     <select name="year" id="select-year" class="select select-bordered select-sm w-24" onchange="updateAnalyticsVis()">
                         @for(Integer y : years)
                                 <option value="${y}" selected="${y == currentYear}">${yearLabel.apply(y)}</option>
                         @endfor
                     </select>
                 </div>
                 
                 <div id="wrapper-month" style="display:none;">
                     <select name="month" id="select-month" class="select select-bordered select-sm w-20" onchange="updateAnalyticsVis()">
                         @for(int m = 1; m <= 12; m++)
                                 <option value="${m}" selected="${m == currentMonth}">${monthLabel.apply(m)}</option>
                         @endfor
                     </select>
                 </div>

                 <!-- Separator -->
                 <div class="hidden sm:block w-px h-6 bg-base-300 mx-2"></div>

                 <!-- Group By -->
                 <div class="join shadow-sm">
                        <input class="join-item btn btn-sm" type="radio" name="groupBy" value="STOCK" aria-label="${MessageUtil.getMessage("stock.label.by.stock")}" checked />
                        <input class="join-item btn btn-sm" type="radio" name="groupBy" value="ACCOUNT" aria-label="${MessageUtil.getMessage("stock.label.by.account")}" />
                </div>
             </div>
        </div>
    </form>

    <!-- Content -->
    <div id="analytics-content" class="min-h-[600px] flex flex-col gap-4">
         <div class="flex justify-center p-20">
            <span class="loading loading-spinner text-primary loading-lg"></span>
        </div>
    </div>
</div>

<!-- Logic -->
<script>
    function selectAnalyticsTab(type) {
        // UI Update
        document.querySelectorAll('.tab').forEach(el => el.classList.remove('tab-active', '!bg-primary', '!text-primary-content'));
        const activeTab = document.getElementById('tab-' + type.toLowerCase());
        if(activeTab) activeTab.classList.add('tab-active', '!bg-primary', '!text-primary-content');
        
        // Value Update
        document.getElementById('input-type').value = type;
        
        // Trigger
        htmx.trigger('#analytics-filter-form', 'submit');
    }

    function updateAnalyticsVis() {
        const timeScaleElement = document.querySelector('input[name="timeScale"]:checked');
        if(!timeScaleElement) return;

        const timeScale = timeScaleElement.value;
        const yearWrapper = document.getElementById('wrapper-year');
        const monthWrapper = document.getElementById('wrapper-month');
        
        if (timeScale === 'TOTAL') {
             yearWrapper.style.display = 'none';
             monthWrapper.style.display = 'none';
        } else if (timeScale === 'YEARLY') {
             yearWrapper.style.display = 'inline-block';
             monthWrapper.style.display = 'none';
        } else {
             yearWrapper.style.display = 'inline-block';
             monthWrapper.style.display = 'inline-block';
        }
    }
</script>