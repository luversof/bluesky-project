@import java.time.LocalDate

@param String pickerName
@param String btnClass
@param boolean canPrev
@param boolean canNext
@param String rangeMode = null
@param LocalDate startDate = null
@param LocalDate endDate = null
@param boolean showRangeText = true
@param LocalDate coveredStartDate = null
@param LocalDate coveredEndDate = null

@template.stock.htmx.fragments.components.dateRangeNavBar(
    pickerName = pickerName,
    btnClass = btnClass,
    canPrev = canPrev,
    canNext = canNext,
    rangeMode = rangeMode,
    startDate = startDate,
    endDate = endDate,
    showRangeText = showRangeText,
    coveredStartDate = coveredStartDate,
    coveredEndDate = coveredEndDate
)
