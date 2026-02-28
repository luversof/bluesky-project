@import net.luversof.web.dynamiccrud.setting.domain.DbField
@import net.luversof.web.dynamiccrud.thymeleaf.util.ThymeleafUtil
@import java.util.List

@param boolean hasSearchField
@param List<DbField> dbFieldList

@template._layout.defaultLayout(
        content = @`
                @template.use._components.searchArea(hasSearchField = hasSearchField, dbFieldList = dbFieldList)

                <div id="indicator" class="text-center my-10 hidden">
                        <span class="loading loading-ring loading-lg"></span>
                </div>

                <div id="listArea"
                        class="px-10 text-center"
                        hx-get="${ThymeleafUtil.getUrl("FRAGMENT_LIST")}"
                        hx-vals="js:{'page' : param.getRequestPage()}"
                        hx-include="#searchArea .searchInput"
                        hx-swap="transition:true"
                        hx-indicator="#indicator"
                        hx-trigger="
                                load delay:0s,
                                listFragmentTrigger,
                                createModalResponseTrigger from:body,
                                updateModalResponseTrigger from:body,
                                deleteModalResponseTrigger from:body,
                                importModalBulkResponseTrigger from:body"
                >
                </div>
        `
)
