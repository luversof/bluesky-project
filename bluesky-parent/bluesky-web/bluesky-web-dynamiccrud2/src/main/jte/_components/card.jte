<%-- 구역 카드. 제목 줄 오른쪽에 actions(단추)를 둔다. 제목이 없으면 제목 줄도 없다. help 는 제목 옆 ⓘ 도움말(_components/help).
     collapsible: 제목 줄을 눌러 접고 펼친다(details · summary, 처음 상태는 open). 접히는 카드의 actions 는 제목 줄 안이라 누르면 접히므로 쓰지 않는다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param gg.jte.Content body
@param String title = null
@param String help = null
@param gg.jte.Content actions = null
@param String cssClass = "mb-4"
@param boolean collapsible = false
@param boolean open = true
@param String rememberKey = null

<%-- rememberKey: 접고 편 상태를 이 브라우저에 기억해 다음에 열 때 그대로(js/ui.js, localStorage "card-open:키"). open 은 기억이 없을 때의 처음 상태 --%>
@if(collapsible)
	<details class="card card-collapsible ${cssClass}" open="${open}" data-remember-open="${rememberKey}">
		<summary class="card-summary">
			<h2 class="font-semibold">${title}</h2>
			@if(help != null)@template._components.help(text = help, label = t("common.helpFor", title))@endif
		</summary>
		<div class="mt-2">${body}</div>
	</details>
@else
	<section class="card ${cssClass}">
		@if(title != null || actions != null)
			<div class="flex flex-wrap items-center gap-2 mb-2">
				@if(title != null)<h2 class="font-semibold">${title}</h2>@endif
				@if(help != null)@template._components.help(text = help, label = t("common.helpFor", title))@endif
				@if(actions != null)<div class="ml-auto flex flex-wrap items-center gap-2">${actions}</div>@endif
			</div>
		@endif
		${body}
	</section>
@endif
