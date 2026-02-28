@import net.luversof.web.dynamiccrud.setting.domain.DbField
@import net.luversof.web.dynamiccrud.setting.domain.SubMenu
@import net.luversof.web.dynamiccrud.setting.domain.DbFieldColumnType
@import java.util.Arrays

@param SubMenu subMenu
@param DbField dbField
@param String modalMode

@if(dbField.getColumnPreset() == null || dbField.getColumnPreset().isEmpty())
        @if(dbField.getColumnType() == DbFieldColumnType.BOOLEAN)
                <input type="checkbox" class="toggle toggle-lg toggle-primary ml-2"
                        name="${dbField.getColumnId() + "_checkbox"}"
                        checked="${"true".equalsIgnoreCase(dbField.getColumnDefaultValue())}"
                />
                <input type="hidden"
                        name="${dbField.getColumnId()}"
                        value="${"true".equalsIgnoreCase(dbField.getColumnDefaultValue()) ? "true" : "false"}"
                />
        @elseif(dbField.getColumnType() == DbFieldColumnType.INT)
!{
        String inputHtml = "<input type=\"number\" class=\"input input-sm join-item w-full\""
                        + " name=\"" + dbField.getColumnId() + "\""
                        + " value=\"" + (dbField.getColumnDefaultValue() != null ? dbField.getColumnDefaultValue().replace("\"", "&quot;") : "") + "\""
                        + (dbField.isEnableEditRequired(modalMode) ? " required=\"required\"" : "")
                        + " placeholder=\"" + (dbField.getFormPlaceholder() != null ? dbField.getFormPlaceholder().replace("\"", "&quot;") : "") + "\""
                        + " " + (dbField.getColumnValidation() != null ? dbField.getColumnValidation() : "data-hasColumnValidation=\"false\"")
                        + "/>";
}
$unsafe{inputHtml}
        @elseif(dbField.getColumnType() == DbFieldColumnType.TEXT)
!{
        String textHtml = "<textarea class=\"textarea textarea-sm join-item h-40 w-full\""
                        + " name=\"" + dbField.getColumnId() + "\""
                        + (dbField.isEnableEditRequired(modalMode) ? " required=\"required\"" : "")
                        + " placeholder=\"" + (dbField.getFormPlaceholder() != null ? dbField.getFormPlaceholder().replace("\"", "&quot;") : "") + "\""
                        + " " + (dbField.getColumnValidation() != null ? dbField.getColumnValidation() : "data-hasColumnValidation=\"false\"")
                        + ">"
                        + (dbField.getColumnDefaultValue() != null ? dbField.getColumnDefaultValue() : "")
                        + "</textarea>";
}
$unsafe{textHtml}
        @else
!{
        String defaultHtml = "<input type=\"text\" class=\"input input-sm join-item w-full\""
                        + " name=\"" + dbField.getColumnId() + "\""
                        + " value=\"" + (dbField.getColumnDefaultValue() != null ? dbField.getColumnDefaultValue().replace("\"", "&quot;") : "") + "\""
                        + (dbField.isEnableEditRequired(modalMode) ? " required=\"required\"" : "")
                        + " placeholder=\"" + (dbField.getFormPlaceholder() != null ? dbField.getFormPlaceholder().replace("\"", "&quot;") : "") + "\""
                        + " " + (dbField.getColumnValidation() != null ? dbField.getColumnValidation() : "data-hasColumnValidation=\"false\"")
                        + "/>";
}
$unsafe{defaultHtml}
        @endif
@else
        <select class="select select-sm join-item w-full"
                name="${dbField.getColumnId()}"
                required="${dbField.isEnableEditRequired(modalMode)}">
                <option value="">Select</option>
                @for(String presetPart : Arrays.asList(dbField.getColumnPreset().split(",")))
!{
        String optionValue = presetPart.contains("|") ? presetPart.split("\\|")[0] : presetPart;
        String optionText = presetPart.contains("|") ? presetPart.split("\\|")[1] : presetPart;
        boolean selected = optionValue.equals(dbField.getColumnDefaultValue());
}
                        <option value="${optionValue}" selected="${selected}">${optionText}</option>
                @endfor
        </select>
@endif
