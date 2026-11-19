<%-- 이력 찾기의 사용자 칸: 이력에 있는 사람 중에서 치면서 고른다(_components/combobox). 비우면 전체 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String id
@param java.util.List<String> usernames
@param String selected = null

@template._components.combobox(id = id, name = "user", required = false, mustPick = true,
	placeholder = t("admin.history.userPlaceholder"), value = selected, label = selected, options = @`
		@for(var name : usernames)
			@template._components.pickOption(value = name, label = name)
		@endfor
	`)
