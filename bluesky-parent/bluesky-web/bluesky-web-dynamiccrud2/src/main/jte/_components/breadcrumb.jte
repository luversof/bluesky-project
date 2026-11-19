<%-- 관리 화면 위치 표시: 프로젝트 관리 › 프로젝트 › 메인 메뉴 › 메뉴 › 지금 화면. 마지막 칸은 지금 화면(aria-current), 링크가 없는 칸은 글자만.
     예전 "← 돌아가기" 링크 자리를 대신한다(layout 의 breadcrumb 자리, h1 위) --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.admin.controller.Crumb

@param List<Crumb> items

<nav aria-label="${t("breadcrumb.label")}" class="mb-2 text-sm">
	<ol class="flex flex-wrap items-center gap-x-1.5 gap-y-1">
		@for(var i = 0; i < items.size(); i++)
			!{var item = items.get(i);}
			!{var last = i == items.size() - 1;}
			<li class="flex items-center gap-1.5 min-w-0">
				@if(i > 0)<span class="hint" aria-hidden="true">›</span>@endif
				@if(last)
					<span class="font-medium truncate" aria-current="page">${item.label()}</span>
				@elseif(item.href() != null)
					<a class="hint hover:underline truncate" href="${item.href()}">${item.label()}</a>
				@else
					<span class="hint truncate">${item.label()}</span>
				@endif
			</li>
		@endfor
	</ol>
</nav>
