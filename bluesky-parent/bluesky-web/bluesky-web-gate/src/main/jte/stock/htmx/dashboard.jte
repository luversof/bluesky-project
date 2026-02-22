@param boolean hxRequest = false
@param boolean isAuthenticated = false

@if(!hxRequest)
@template._layout.stockLayout(
    isAuthenticated = isAuthenticated,
    contentStock = @`
        @template.stock.htmx.dashboardContent(isAuthenticated = isAuthenticated)
    `
)
@else
    @template.stock.htmx.dashboardContent(isAuthenticated = isAuthenticated)
@endif
