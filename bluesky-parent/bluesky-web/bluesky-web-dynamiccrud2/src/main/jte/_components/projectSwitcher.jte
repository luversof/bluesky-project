<%-- 상단 프로젝트 고르기(사용 화면): 지금 프로젝트 이름을 누르면 볼 수 있는 프로젝트 링크 목록이 펼쳐진다.
     select 대신 링크 목록 - 스크립트 없이 이동하고, 키보드로 고르다 바로 넘어가지 않으며, 새 탭으로도 연다.
     바깥 클릭 · Esc 로 닫기는 js/ui.js([data-dropdown]). 프로젝트가 하나뿐이면 레이아웃이 이름만 보인다 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@import java.util.List
@import net.luversof.web.dynamiccrud2.domain.Project

@param Project current
@param List<Project> projects

<details class="nav-dropdown" data-dropdown>
	<%-- select-none: 빠르게 두 번 누르면 이름 글자가 선택되던 것(DMT2 80c8003) --%>
	<summary class="font-semibold hover:underline select-none" aria-label="${t("layout.project.switch", current.getName())}">${current.getName()} <span class="nav-dropdown-caret" aria-hidden="true">▾</span></summary>
	<ul class="nav-dropdown-list">
		@for(var project : projects)
			<li><a href="/${project.getCode()}" aria-current="${project.getId().equals(current.getId()) ? "page" : null}">${project.getName()}</a></li>
		@endfor
	</ul>
</details>
