@import java.util.Map
@import org.springframework.data.domain.Page
@import net.luversof.web.dynamiccrud.use.domain.ContentInfo
@import net.luversof.web.dynamiccrud.use.domain.ContentInfo.ContentKeyInfo
@import net.luversof.web.dynamiccrud.thymeleaf.util.ThymeleafUtil
@import net.luversof.web.dynamiccrud.setting.domain.SubMenu
@import net.luversof.web.dynamiccrud.util.MessageUtil
@import com.fasterxml.jackson.databind.ObjectMapper

@param Page<?> page
@param ContentInfo contentInfo
@param ObjectMapper jacksonObjectMapper

!{
        SubMenu targetSubMenu = ThymeleafUtil.getSubMenu();
}

<div id="content">
        <script>
                /*<![CDATA[*/
                var contentList = !{
                        String pageContentJson = "[]";
                        try {
                                pageContentJson = jacksonObjectMapper.writeValueAsString(page.getContent());
                        } catch (Exception e) {}
                }$unsafe{pageContentJson};
                /*]]>*/
        </script>

        <div class="flex items-center justify-between h-14">
                <div class="flex p-2">
                        <label id="useListAreaRowSpan" class="swap swap-rotate">
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="swap-on w-6 h-6">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12H12m-8.25 5.25h16.5" />
                                </svg>
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="swap-off w-6 h-6">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
                                </svg>
                        </label>
                        총 ${page.getTotalElements()} 건
                </div>
                <div>
                        @if(targetSubMenu.isShowExportButton())
                        <button class="btn btn-outline btn-sm exportButton"
                                hx-get="${ThymeleafUtil.getUrl("FRAGMENT_MODAL_BULK_FORM", Map.of("modalMode", "export"))}"
                                hx-trigger="exportModalBulkTrigger"
                                hx-target="#fragmentArea"
                        >
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
                                </svg>
                                ${MessageUtil.getMessage("button.text.export")}
                        </button>
                        @endif

                        @if(targetSubMenu.isShowImportButton())
                        <button class="btn btn-outline btn-sm importButton"
                                hx-get="${ThymeleafUtil.getUrl("FRAGMENT_MODAL_BULK_FORM", Map.of("modalMode", "import"))}"
                                hx-target="#fragmentArea"
                        >
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
                                </svg>
                                ${MessageUtil.getMessage("button.text.import")}
                        </button>
                        @endif

                        @if(targetSubMenu.isEnableExcel())
                        <a
                                href="${ThymeleafUtil.getUriComponentsBuilder().replacePath(ThymeleafUtil.getUrl("FRAGMENT_EXCEL")).toUriString()}"
                                target="_blank"
                                download
                        >
                                <button class="btn btn-outline btn-sm excelButton">
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                                                <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25M9 16.5v.75m3-3v3M15 12v5.25m-4.5-15H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                                        </svg>
                                        ${MessageUtil.getMessage("button.text.excel")}
                                </button>
                        </a>
                        @endif
                </div>
        </div>

        @if(contentInfo.getProcessedContentMapList() != null)
        <table class="table table-xs" id="contentTable" aria-describedby="contentTable">
                <thead>
                        <tr>
                                <th class="w-0 p-0"></th>
                                @for(ContentKeyInfo contentKeyInfo : contentInfo.getContentKeyInfoList())
                                        @if(contentKeyInfo.visible())
                                        <th>${contentKeyInfo.key()}</th>
                                        @endif
                                @endfor
                                @if(targetSubMenu.isEnableUpdate())
                                <th class="w-10">${MessageUtil.getMessage("use.text.update")}</th>
                                @endif
                                @if(targetSubMenu.isShowRowCheckBox())
                                <th class="w-10">
                                        <input name="contentDataCheckToggle" type="checkbox" class="checkbox" />
                                </th>
                                @endif
                        </tr>
                </thead>
                <tbody>
!{
        int index = 0;
}
                        @for(Map<String, Object> content : contentInfo.getProcessedContentMapList())
                                <tr class="hover">
                                        <td class="w-0 p-0 contentData">
                                                @for(ContentKeyInfo contentKeyInfo : contentInfo.getContentKeyInfoList())
                                                !{
                                                        Map<String, Object> originContentMap = contentInfo.getOriginContentMapList().get(index);
                                                        String originVal = "";
                                                        if (originContentMap.get(contentKeyInfo.originKey()) != null) {
                                                                originVal = String.valueOf(originContentMap.get(contentKeyInfo.originKey()));
                                                        }
                                                }
                                                <input type="hidden"
                                                        name="${contentKeyInfo.originKey()}"
                                                        value="${originVal}"
                                                />
                                                <input type="hidden"
                                                        name="__org__${contentKeyInfo.originKey()}"
                                                        value="${originVal}"
                                                />
                                                @endfor
                                        </td>
                                        @for(ContentKeyInfo contentKeyInfo : contentInfo.getContentKeyInfoList())
                                                @if(contentKeyInfo.visible())
                                                <td>$unsafe{String.valueOf(content.get(contentKeyInfo.key()))}</td>
                                                @endif
                                        @endfor
                                        
                                        @if(targetSubMenu.isEnableUpdate())
                                        <td class="contentDataMenu">
                                                <button class="btn btn-sm"
                                                        hx-get="${ThymeleafUtil.getUrl("FRAGMENT_MODAL_FORM", Map.of("modalMode", "update"))}"
                                                        hx-target="#fragmentArea"
                                                >
                                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                                                                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                                                        </svg>
                                                </button>
                                        </td>
                                        @endif
                                        
                                        @if(targetSubMenu.isShowRowCheckBox())
                                        <td class="contentDataCheck">
                                                <input name="contentDataCheck" type="checkbox" class="checkbox" value="${index}" />
                                        </td>
                                        @endif
                                </tr>
!{
        index++;
}
                        @endfor
                </tbody>
        </table>
        @endif

        @if(page.getTotalElements() <= 0)
        <div role="alert" class="alert">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="stroke-info shrink-0 w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                <span>${MessageUtil.getMessage("use.text.not_exist_data")}</span>
        </div>
        @endif

        <div class="flex justify-between items-center p-2">
                <div class="w-3/12"></div>
                <div class="grow">
                        @template.use._components.pagination(page = page)
                </div>
                <div class="w-3/12 text-right">
                        @if(targetSubMenu.isEnableInsert())
                        <button class="btn btn-outline btn-info btn-sm"
                                hx-get="${ThymeleafUtil.getUrl("FRAGMENT_MODAL_FORM", Map.of("modalMode", "create"))}"
                                hx-target="#fragmentArea"
                        >
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.832 19.82a4.5 4.5 0 01-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 011.13-1.897L16.863 4.487zm0 0L19.5 7.125" />
                                </svg>
                                ${MessageUtil.getMessage("button.text.create")}
                        </button>
                        @endif

                        @if(targetSubMenu.isEnableDelete())
                        <button class="btn btn-outline btn-error btn-sm deleteButton"
                                hx-post="${ThymeleafUtil.getUrl("FRAGMENT_MODAL_FORM", Map.of("modalMode", "delete"))}"
                                hx-include="#contentTable tr:has(input[name=contentDataCheck]:checked) .contentData input"
                                hx-trigger="deleteModalTrigger"
                                hx-target="#fragmentArea"
                        >
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                                        <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                                </svg>
                                ${MessageUtil.getMessage("button.text.delete")}
                        </button>
                        @endif
                </div>
        </div>
</div>
