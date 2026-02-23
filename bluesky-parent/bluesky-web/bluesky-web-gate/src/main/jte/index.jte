@param boolean isAuthenticated = false
@param String username = ""

@template._layout.defaultLayout(
	isAuthenticated = isAuthenticated,
	username = username,
	content = @`
		index 본문
	`
)