@import java.util.UUID

@param UUID mainMenuId
@param UUID projectId
@param String error = null

@template._layout.layout(title = "메뉴 추가", content = @`
	@if(error != null)
		<div class="alert alert-error mb-4 max-w-xl" role="alert">${error}</div>
	@endif
	<form method="post" action="/admin/mainMenu/${mainMenuId.toString()}/menu" class="card grid gap-4 max-w-xl">
		<div>
			<label class="label" for="code">주소 이름</label>
			<input id="code" class="input w-full font-mono" name="code" required maxlength="50" pattern="[a-z0-9][a-z0-9\-]{0,49}" title="영문 소문자 · 숫자 · -" placeholder="예: coupon-list" />
			<p class="hint mt-1">사용 화면 주소의 마지막 단계(/프로젝트/메인메뉴/<b>주소 이름</b>). 영문 소문자 · 숫자 · -.</p>
		</div>
		<div>
			<label class="label" for="name">메뉴 이름</label>
			<input id="name" class="input w-full" name="name" required maxlength="100" />
		</div>
		<div>
			<label class="label" for="connectionName">connection</label>
			<%-- 입력하면 이름에 그 글자가 든 connection 을 찾아 후보(datalist)로 보여 준다 --%>
			<input id="connectionName" class="input w-full" name="connectionName" required autocomplete="off"
				list="connectionOptions" placeholder="이름 일부를 입력하세요"
				hx-get="/admin/connection/search" hx-trigger="input changed delay:250ms, focus once"
				hx-target="#connectionOptions" hx-vals="js:{keyword: document.getElementById('connectionName').value}" />
			<datalist id="connectionOptions"></datalist>
			<p class="hint mt-1">connectionInfo 저장소에서 이름으로 찾습니다(대소문자 무시, 최대 30개).</p>
		</div>
		<div>
			<label class="label" for="table">테이블</label>
			<select id="table" class="select w-full" name="table" required
				hx-get="/admin/connection/tables" hx-trigger="change from:#connectionName" hx-include="#connectionName">
				<option value="">connection 을 먼저 고르세요</option>
			</select>
			<p class="hint mt-1">고른 테이블의 컬럼을 필드로 등록하고(이미 등록된 테이블이면 공유), 조회 쿼리를 자동으로 만듭니다.</p>
		</div>
		<div class="flex gap-2">
			<button class="btn btn-primary">등록</button>
			<a class="btn btn-ghost" href="/admin?project=${projectId.toString()}">취소</a>
		</div>
	</form>
`)
