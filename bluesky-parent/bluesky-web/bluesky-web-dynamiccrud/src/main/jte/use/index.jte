@import net.luversof.web.dynamiccrud.setting.domain.DbField
@import net.luversof.web.dynamiccrud.util.TemplateUtil
@import java.util.List

@param boolean hasSearchField
@param List<DbField> dbFieldList

@template._layout.defaultLayout(
        content = @`
                @template.use._components.searchArea(hasSearchField = hasSearchField, dbFieldList = dbFieldList)

                <div id="indicator" class="py-12 text-center hidden">
                        <span class="loading loading-spinner loading-sm text-base-content/50"></span>
                </div>

                <div id="listArea"
                        hx-get="${TemplateUtil.getUrl("FRAGMENT_LIST")}"
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
