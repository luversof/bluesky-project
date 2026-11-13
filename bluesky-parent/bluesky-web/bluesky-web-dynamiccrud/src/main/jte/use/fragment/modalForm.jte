@import net.luversof.web.dynamiccrud.setting.domain.DbField
@import net.luversof.web.dynamiccrud.setting.domain.SubMenu
@import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType
@import net.luversof.web.dynamiccrud.util.TemplateUtil
@import net.luversof.web.dynamiccrud.util.MessageUtil
@import java.util.Map
@import java.util.List

@param String modalMode

!{
        SubMenu subMenu = TemplateUtil.getSubMenu();
        List<DbField> dbFieldList = TemplateUtil.getDbFieldList();
}

<dialog id="modal" class="modal">
        <div class="modal-box max-w-2xl p-6">
                <div class="flex items-center justify-between">
                        <h3 class="text-lg font-medium">${MessageUtil.getMessage("use.text.modalForm." + modalMode)}</h3>
                        <form method="dialog">
                                <button class="btn btn-ghost btn-sm btn-square" aria-label="${MessageUtil.getMessage("button.text.cancel")}">
                                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-4" aria-hidden="true">
                                                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
                                        </svg>
                                </button>
                        </form>
                </div>
                <div class="mt-4">
                        <form id="modalForm"
                                hx-post="${TemplateUtil.getUrl("FRAGMENT_MODAL_FORM", Map.of("modalMode", modalMode))}"
                                hx-target="#fragmentArea"
                        >
                                @for(DbField dbField : dbFieldList)

                                                <%--
                                                편집 가능한 경우 form 화면 처리
                                                아닌 경우 hidden input으로 처리하여 값을 전달
                                                --%>
                                                @if(dbField.isEnableEdit(modalMode))
                                                <div class="flex flex-wrap gap-x-3" data-columnGroupId="${dbField.getColumnGroupId()}">
                                                        <fieldset class="fieldset grow ${dbField.getColumnType() == DbFieldColumnType.BOOLEAN ? "basis-1/5" : "basis-1/3"}">
                                                                <legend class="fieldset-legend font-normal text-base-content/70 ${dbField.isEnableEditRequired(modalMode) ? "required" : ""}">
                                                                        ${dbField.getColumnName()}
                                                                        @if(dbField.getFormHelpText() != null && !dbField.getFormHelpText().isEmpty())
                                                                        <span class="cursor-pointer tooltip" data-tip="${dbField.getFormHelpText()}">
                                                                                @template._components.icon.help()
                                                                        </span>
                                                                        @endif
                                                                </legend>
                                                                <%--
                                                                preset이 있는 경우 preset 기준 select를 구성
                                                                없으면 그 다음 분기를 고려
                                                                --%>
                                                                @template.use._components.dbField(subMenu = subMenu, dbField = dbField, modalMode = modalMode)
                                                        </fieldset>

                                                </div>
                                                @else
						<input type="hidden" name="${dbField.getColumnId()}" value="${dbField.getColumnDefaultValue() != null ? dbField.getColumnDefaultValue().replace("\"", "&quot;") : ""}" />
						@endif
                                @endfor

                                @for(DbField dbField : dbFieldList)
                                        @if(dbField.isEnableEdit(modalMode))
                                        <input type="hidden"
                                                name="${"__org__" + dbField.getColumnId()}"
                                                value="${dbField.getColumnDefaultValue() != null ? dbField.getColumnDefaultValue() : ""}"
                                        />
                                        @endif
                                @endfor
                        </form>
                </div>

                <div class="mt-6 flex justify-between gap-2">

                        <div class="flex gap-2">
                                <button class="btn btn-ghost btn-sm copySearchButton">${MessageUtil.getMessage("button.text.copySearchArea")}</button>
                                <button class="btn btn-ghost btn-sm copyDataButton">${MessageUtil.getMessage("button.text.copyData")}</button>
                        </div>

                        <div class="flex gap-2">
                                <button class="btn btn-sm ${"delete".equals(modalMode) ? "btn-error" : "btn-primary"}" form="modalForm">${MessageUtil.getMessage("button.text." + modalMode)}</button>
                                <form method="dialog">
                                        <!-- if there is a button in form, it will close the modal -->
                                        <button class="btn btn-sm">${MessageUtil.getMessage("button.text.cancel")}</button>
                                </form>
                        </div>
                </div>
        </div>
</dialog>
