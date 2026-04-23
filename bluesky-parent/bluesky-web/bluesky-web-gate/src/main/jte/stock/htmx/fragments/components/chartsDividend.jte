@import java.util.Map
@import java.math.BigDecimal
@import java.util.List
@import java.util.ArrayList

@param Map<String, BigDecimal> monthlyDividends

!{
    List<String> labels = new ArrayList<>();
    List<BigDecimal> vals = new ArrayList<>();
    if (monthlyDividends != null) {
        for (Map.Entry<String, BigDecimal> e : monthlyDividends.entrySet()) {
            labels.add(e.getKey());
            vals.add(e.getValue() != null ? e.getValue() : BigDecimal.ZERO);
        }
    }
}

@template.stock.htmx.fragments.components.barChart(
    labels = labels,
    values = vals,
    title = "월별 배당금",
    canvasId = "dividendChart"
)
