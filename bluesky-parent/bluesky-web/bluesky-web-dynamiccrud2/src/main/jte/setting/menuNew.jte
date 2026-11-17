@import java.util.List
@import java.util.UUID

@param UUID mainMenuId
@param List<String> connectionNames

@template._layout.layout(title = "메뉴 추가", content = @`
	<form method="post" action="/setting/mainMenu/${mainMenuId.toString()}/menu" class="card grid gap-4 max-w-xl">
		<div>
			<label class="label" for="name">메뉴 이름</label>
			<input id="name" class="input w-full" name="name" required maxlength="100" />
		</div>
		<div>
			<label class="label" for="connectionName">connection</label>
			<select id="connectionName" class="select w-full" name="connectionName" required
				hx-get="/setting/connection/tables" hx-target="#table" hx-trigger="change">
				<option value="">고르세요</option>
				@for(var connectionName : connectionNames)
					<option value="${connectionName}">${connectionName}</option>
				@endfor
			</select>
			<p class="hint mt-1">connectionInfo 저장소에 등록된 전체 목록입니다.</p>
		</div>
		<div>
			<label class="label" for="table">테이블</label>
			<select id="table" class="select w-full" name="table" required>
				<option value="">connection 을 먼저 고르세요</option>
			</select>
			<p class="hint mt-1">고른 테이블의 컬럼을 필드로 등록하고(이미 등록된 테이블이면 공유), 조회 쿼리를 자동으로 만듭니다.</p>
		</div>
		<div class="flex gap-2">
			<button class="btn btn-primary">등록</button>
			<a class="btn btn-ghost" href="/setting">취소</a>
		</div>
	</form>
`)
