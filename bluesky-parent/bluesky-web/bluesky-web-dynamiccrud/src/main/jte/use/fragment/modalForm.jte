@import net.luversof.web.dynamiccrud.setting.domain.DbField
@import net.luversof.web.dynamiccrud.setting.domain.SubMenu
@import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType
@import net.luversof.web.dynamiccrud.thymeleaf.util.ThymeleafUtil
@import net.luversof.web.dynamiccrud.util.MessageUtil
@import java.util.Map
@import java.util.List

@param String modalMode

!{
        SubMenu subMenu = ThymeleafUtil.getSubMenu();
        List<DbField> dbFieldList = ThymeleafUtil.getDbFieldList();
}

<dialog id="modal" class="modal">
        <div class="modal-box max-w-xl">
                <form method="dialog">
                        <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">&#x2715;</button>
                </form>
                <h3 class="font-bold text-lg">${MessageUtil.getMessage("use.text.modalForm." + modalMode)}</h3>
                <div class="m-2">
                        <form id="modalForm"
                                hx-post="${ThymeleafUtil.getUrl("FRAGMENT_MODAL_FORM", Map.of("modalMode", modalMode))}"
                                hx-target="#fragmentArea"
                        >
                                @for(DbField dbField : dbFieldList)

                                                <%--
                                                편집 가능한 경우 form 화면 처리
                                                아닌 경우 hidden input으로 처리하여 값을 전달
                                                --%>
                                                @if(dbField.isEnableEdit(modalMode))
                                                <div class="flex flex-wrap" data-columnGroupId="${dbField.getColumnGroupId()}">
                                                        <fieldset class="fieldset grow ${dbField.getColumnType() == DbFieldColumnType.BOOLEAN ? "basis-1/5" : "basis-1/3"}">
                                                                <legend class="fieldset-legend ${dbField.isEnableEditRequired(modalMode) ? "required" : ""}">
                                                                        ${dbField.getColumnName()}
                                                                        @if(dbField.getFormHelpText() != null && !dbField.getFormHelpText().isEmpty())
                                                                        <span class="label-text-alt cursor-pointer tooltip" data-tip="${dbField.getFormHelpText()}">
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

                <div class="flex justify-between">

                        <div>
                                <button class="btn btn-outline btn-sm copySearchButton">${MessageUtil.getMessage("button.text.copySearchArea")}</button>
                                <button class="btn btn-outline btn-sm copyDataButton">${MessageUtil.getMessage("button.text.copyData")}</button>
                        </div>

                        <div class="flex justify-between">
                                <button class="btn btn-outline btn-info btn-sm mr-1" form="modalForm">${MessageUtil.getMessage("button.text." + modalMode)}</button>
                                <form method="dialog">
                                        <!-- if there is a button in form, it will close the modal -->
                                        <button class="btn btn-outline btn-sm">${MessageUtil.getMessage("button.text.cancel")}</button>
                                </form>
                        </div>
                </div>
        </div>
</dialog>
