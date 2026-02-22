@import java.util.List

@param List<Integer> years
@param int currentYear
@param int currentMonth

<div class="flex flex-col gap-6">
    <!-- Tabs -->
    <div class="tabs tabs-boxed bg-base-200 p-1">
         <a class="tab tab-lg flex-1 transition-all duration-300 tab-active font-bold" 
            id="tab-profit" 
            onclick="selectAnalyticsTab('PROFIT')">자산 성장</a>
         <a class="tab tab-lg flex-1 transition-all duration-300 font-bold" 
            id="tab-dividend" 
            onclick="selectAnalyticsTab('DIVIDEND')">배당 추이</a>
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
                <input class="join-item btn btn-sm" type="radio" name="timeScale" value="YEARLY" aria-label="연도별 흐름" checked onchange="updateAnalyticsVis()"/>
                <input class="join-item btn btn-sm" type="radio" name="timeScale" value="MONTHLY" aria-label="월별 상세" onchange="updateAnalyticsVis()"/>
                <input class="join-item btn btn-sm" type="radio" name="timeScale" value="TOTAL" aria-label="전체 누적" onchange="updateAnalyticsVis()"/>
             </div>

             <!-- Details -->
             <div class="flex gap-2 items-center">
                 <select name="year" id="select-year" class="select select-bordered select-sm" onchange="updateAnalyticsVis()">
                     @for(Integer y : years)
                         <option value="${y}" selected="${y == currentYear}">${y}년</option>
                     @endfor
                 </select>
                 
                 <select name="month" id="select-month" class="select select-bordered select-sm" style="display:none;" onchange="updateAnalyticsVis()">
                     @for(int m = 1; m <= 12; m++)
                         <option value="${m}" selected="${m == currentMonth}">${m}월</option>
                     @endfor
                 </select>

                 <!-- Separator -->
                 <div class="w-px h-6 bg-base-300 mx-2"></div>

                 <!-- Group By -->
                 <div class="join shadow-sm">
                    <input class="join-item btn btn-sm" type="radio" name="groupBy" value="STOCK" aria-label="종목별" checked onchange="htmx.trigger('#analytics-filter-form', 'submit')"/>
                    <input class="join-item btn btn-sm" type="radio" name="groupBy" value="ACCOUNT" aria-label="계좌별" onchange="htmx.trigger('#analytics-filter-form', 'submit')"/>
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
        document.querySelectorAll('.tab').forEach(el => el.classList.remove('tab-active', 'bg-primary', 'text-primary-content'));
        const activeTab = document.getElementById('tab-' + type.toLowerCase());
        if(activeTab) activeTab.classList.add('tab-active', 'bg-primary', 'text-primary-content');
        
        // Value Update
        document.getElementById('input-type').value = type;
        
        // Trigger
        htmx.trigger('#analytics-filter-form', 'submit');
    }

    function updateAnalyticsVis() {
        const timeScaleElement = document.querySelector('input[name="timeScale"]:checked');
        if(!timeScaleElement) return;

        const timeScale = timeScaleElement.value;
        const yearSelect = document.getElementById('select-year');
        const monthSelect = document.getElementById('select-month');
        
        if (timeScale === 'TOTAL') {
             yearSelect.style.display = 'none';
             monthSelect.style.display = 'none';
        } else if (timeScale === 'YEARLY') {
             yearSelect.style.display = 'inline-block';
             monthSelect.style.display = 'none';
        } else {
             yearSelect.style.display = 'inline-block';
             monthSelect.style.display = 'inline-block';
        }
        htmx.trigger('#analytics-filter-form', 'submit');
    }
</script>