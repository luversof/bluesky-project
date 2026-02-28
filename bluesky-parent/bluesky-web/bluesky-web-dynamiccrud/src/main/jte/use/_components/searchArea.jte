@import net.luversof.web.dynamiccrud.setting.domain.DbField
@import net.luversof.web.dynamiccrud.setting.domain.DbFieldSearchType
@import net.luversof.web.dynamiccrud.util.MessageUtil
@import net.luversof.web.dynamiccrud.util.RequestUtil
@param boolean hasSearchField
@param java.util.List<DbField> dbFieldList

<%--
   field 설정에 따라 적절한 검색 창을 보여줌
   input이 preset을 가지고 있다면 select로 처리
   preset인 경우 |  구분자로 값이 나뉘어지고 1구분자로 첫 값이 나뉘어짐
   input이 boolean을 가지고 있다면 checkbox로 처리
   그외의 경우는 input text로 처리
   date인 경우는 달력보여주기
--%>

@if(hasSearchField)
<div class="flex items-center h-16 px-10 sticky top-14 z-1 bg-base-100 bg-opacity-80" id="searchArea">
        <div class="flex gap-x-1">
                @for(DbField dbField : dbFieldList)
                        @if(dbField.isEnableSearch())
                        <fieldset class="fieldset py-0">
                                <legend class="fieldset-legend text-gray-600 ${dbField.isEnableSearchRequired() ? "required" : ""}">
                                        ${dbField.getColumnName()}
                                        @if(dbField.getColumnSearchType() != null && dbField.getColumnSearchType() != DbFieldSearchType.EQUALS)
                                                <span class="text-gray-600 cursor-pointer tooltip tooltip-right" data-tip="${MessageUtil.getMessage("use.text.columnSearchType." + dbField.getColumnSearchType().name())}">
                                                        @template._components.icon.help()
                                                </span>
                                        @endif
                                </legend>

                                @if(dbField.getColumnPreset() == null || dbField.getColumnPreset().isEmpty())
!{
        String paramVal = RequestUtil.getParameter(dbField.getColumnId());
        String value = paramVal == null || paramVal.isEmpty() ? dbField.getColumnSearchDefaultValue() : paramVal;
        
        String inputHtml = "<input type=\"text\" class=\"input input-sm w-48 searchInput\""
                        + " placeholder=\"" + (dbField.getFormPlaceholder() != null ? dbField.getFormPlaceholder().replace("\"", "&quot;") : "") + "\""
                        + " name=\"" + dbField.getColumnId() + "\""
                        + " value=\"" + (value != null ? value.replace("\"", "&quot;") : "") + "\""
                        + (dbField.isEnableSearchRequired() ? " required=\"required\"" : "")
                        + " " + (dbField.getColumnSearchValidation() != null ? dbField.getColumnSearchValidation() : "data-hasColumnSearchValidation=\"false\"")
                        + "/>";
}
$unsafe{inputHtml}
                                @else
                                        <select class="select select-sm searchInput"
                                                name="${dbField.getColumnId()}"
                                                required="${dbField.isEnableSearchRequired()}">
                                                <option value="">${MessageUtil.getMessage("use.text.select.empty")}</option>
                                                @for(String presetPart : java.util.Arrays.asList(dbField.getColumnPreset().split(",")))
!{
        String optionValue = presetPart.contains("|") ? presetPart.split("\\|")[0] : presetPart;
        String optionText = presetPart.contains("|") ? presetPart.split("\\|")[1] : presetPart;
        
        String currentParamVal = RequestUtil.getParameter(dbField.getColumnId());
        if (currentParamVal == null || currentParamVal.isEmpty()) {
                currentParamVal = dbField.getColumnSearchDefaultValue();
        }
        boolean selected = optionValue.equals(currentParamVal);
}
                                                        <option value="${optionValue}" selected="${selected}">${optionText}</option>
                                                @endfor
                                        </select>
                                @endif
                        </fieldset>
                        @endif
                @endfor

                <button class="btn btn-outline btn-sm btn-info self-end searchButton">
                        @template._components.icon.search()
                        ${MessageUtil.getMessage("button.text.search")}
                </button>
                <button class="btn btn-outline btn-sm self-end resetButton">
                        @template._components.icon.reset()
                        ${MessageUtil.getMessage("button.text.reset")}
                </button>
        </div>
</div>
@endif


