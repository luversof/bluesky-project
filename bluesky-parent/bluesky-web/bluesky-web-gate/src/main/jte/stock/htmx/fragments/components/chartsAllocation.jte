@import java.util.Map
@import java.math.BigDecimal
@import java.util.List
@import java.util.ArrayList
@import io.github.luversof.boot.context.support.MessageUtil

@param Map<String, BigDecimal> allocation

!{
    List<String> labels = new ArrayList<>();
    List<BigDecimal> vals = new ArrayList<>();
    if (allocation != null) {
        for (Map.Entry<String, BigDecimal> e : allocation.entrySet()) {
            labels.add(e.getKey());
            vals.add(e.getValue() != null ? e.getValue() : BigDecimal.ZERO);
        }
    }
}

@template.stock.htmx.fragments.components.doughnutChart(
    labels = labels,
    values = vals,
    title = MessageUtil.getMessage("stock.chart.title.stock.allocation"),
    canvasId = "allocationChart",
    legendId = "allocationLegend"
)
