@import java.util.Map
@import java.math.BigDecimal
@import java.util.List
@import java.util.ArrayList

@param Map<String, BigDecimal> accountAllocation

!{
    List<String> labels = new ArrayList<>();
    List<BigDecimal> vals = new ArrayList<>();
    if (accountAllocation != null) {
        for (Map.Entry<String, BigDecimal> e : accountAllocation.entrySet()) {
            labels.add(e.getKey());
            vals.add(e.getValue() != null ? e.getValue() : BigDecimal.ZERO);
        }
    }
}

@template.stock.htmx.fragments.components.doughnutChart(
    labels = labels,
    values = vals,
    title = "계좌 비중",
    canvasId = "accountAllocationChart",
    legendId = "accountAllocationLegend"
)
