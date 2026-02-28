@import net.luversof.web.dynamiccrud.thymeleaf.util.ThymeleafUtil
@import net.luversof.web.dynamiccrud.util.MessageUtil
@import java.util.Map

@param String modalMode

<dialog id="modal" class="modal">
        <div class="modal-box max-w-xl">
                <form method="dialog">
                        <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">&#x2715;</button>
                </form>
                <h3 class="font-bold text-lg">${MessageUtil.getMessage("use.text.modalBulkForm." + modalMode)}</h3>
                <div class="m-2">
                        <form id="modalBulkForm"
                                hx-post="${ThymeleafUtil.getUrl("FRAGMENT_MODAL_BULK_FORM", Map.of("modalMode", modalMode))}"
                                hx-target="#fragmentArea"
                        >
                                <div class="w-full m-1">
                                        !{
                                                String textareaHtml = "<textarea class=\"textarea textarea-sm h-60 w-full\" name=\"bulkData\" " + ("export".equals(modalMode) ? "readonly" : "") + "></textarea>";
                                        }
                                        $unsafe{textareaHtml}
                                </div>

                        </form>
                </div>

                <div class="flex justify-between">

                        <div></div>

                        <div class="flex justify-between">
                                @if("export".equals(modalMode))
                                <button class="btn btn-outline btn-sm mr-1 copyDataButton">
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 0 1-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 0 1 1.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 0 0-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 0 1-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 0 0-3.375-3.375h-1.5a1.125 1.125 0 0 1-1.125-1.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H9.75" />
                                        </svg>
                                        ${MessageUtil.getMessage("button.text.copyData")}
                                </button>
                                @endif
                                
                                @if("import".equals(modalMode))
                                <button class="btn btn-outline btn-info btn-sm mr-1" form="modalBulkForm">
                                        ${MessageUtil.getMessage("button.text.import")}
                                </button>
                                @endif
                                
                                <form method="dialog">
                                        <!-- if there is a button in form, it will close the modal -->
                                        <button class="btn btn-outline btn-sm">${MessageUtil.getMessage("button.text.cancel")}</button>
                                </form>
                        </div>
                </div>
        </div>
</dialog>
