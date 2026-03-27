@import java.util.Map
@import java.math.BigDecimal

@param Map<String, BigDecimal> monthlyDividends

<div class="card bg-base-100 shadow-xl h-full">
    <div class="card-body">
        <h2 class="card-title text-base-content/70">월별 배당금</h2>
        <div class="h-64 flex justify-center items-center">
            <canvas id="dividendChart" class="amount-chart"></canvas>
        </div>
    </div>
    <script>
        (function() {
            if (typeof Chart === 'undefined') return;
            const ctx = document.getElementById('dividendChart').getContext('2d');
            
            const labels = [
                @for(String key : monthlyDividends.keySet())
                    '${key}',
                @endfor
            ];
            const values = [
                @for(BigDecimal val : monthlyDividends.values())
                    $unsafe{val != null ? val.toPlainString() : "0"},
                @endfor
            ];

            new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [{
                        label: '배당금 (원)',
                        data: values,
                        backgroundColor: '#36A2EB',
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: { beginAtZero: true }
                    }
                }
            });
        })();
    </script>
</div>