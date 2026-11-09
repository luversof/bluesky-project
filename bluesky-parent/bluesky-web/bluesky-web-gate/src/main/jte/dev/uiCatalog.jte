@import java.math.BigDecimal
@import java.util.List

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"

<%-- 표 UI 견본(사용자 요청 2026-11-10: "테이블도 다 ui 가 제각각" - 통합 전에 기준 모양을 고르는 화면).
     주식 화면 표 39 개가 클래스 조합 14 가지 · 감싸는 상자 6 가지로 갈라져 있다. 같은 견본 데이터를 후보마다 그려 나란히 보고,
     고른 기준을 _components/ui 의 표 컴포넌트 하나로 옮긴다. 메뉴에는 없다(/dev/ui-catalog 로 직접 연다). 가짜 데이터만 쓴다. 개발용 화면이라 문구는 한국어로만 둔다(주식 화면의 i18n 검사 대상 밖). --%>
!{
	// 열: 종목, 계좌, 수량, 평가금액, 평가손익, 수익률(%). 긴 이름 · 손실 · 값 없음(-) 을 일부러 섞는다.
	List<List<Object>> rows = List.of(
		List.of("KODEX 200", "연금저축", 120L, new BigDecimal("4512000"), new BigDecimal("312500"), new BigDecimal("7.44")),
		List.of("TIGER 미국S&P500", "ISA", 85L, new BigDecimal("1873400"), new BigDecimal("-41200"), new BigDecimal("-2.15")),
		List.of("KODEX 한국부동산리츠인프라 (이름이 아주 긴 종목 예시)", "일반", 300L, new BigDecimal("1395000"), new BigDecimal("0"), new BigDecimal("0.00")),
		List.of("RISE 200위클리커버드콜", "연금저축", 40L, new BigDecimal("402800"), new BigDecimal("18760"), new BigDecimal("4.88")),
		List.of("삼성전자", "일반", 15L, new BigDecimal("1023000"), new BigDecimal("-187500"), new BigDecimal("-15.49")));
	String sortButton = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-end px-1 text-xs text-base-content/60 hover:text-base-content";
	String sortButtonStart = "btn btn-ghost btn-xs h-auto min-h-6 w-full justify-start px-1 text-xs text-base-content/60 hover:text-base-content";
	java.util.function.Function<BigDecimal, String> pctClass = v -> v.signum() > 0 ? "text-profit" : (v.signum() < 0 ? "text-loss" : "text-base-content/60");
}

@template._layout.stockLayout(
	pageTitle = "UI 견본",
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentStock = @`
		<div class="space-y-8" data-ui-catalog>
			@template._components.ui.pageHeader(title = "UI 견본", description = "같은 내용을 후보마다 그렸습니다. 항목마다 코드 하나씩 골라 주세요. 고른 것이 공용 컴포넌트의 기준이 됩니다. 괄호 안 숫자는 지금 주식 화면에서 그 모양을 쓰는 수입니다.")
			<div role="note" class="text-sm text-base-content/70">표 기준은 확정되었습니다(2026-11-10): D2 · Z2 · B3 · H2 · N1 · W2. 아래 표 항목은 참고용으로 남겨 둡니다. 새로 고를 항목은 맨 아래 BT · BG · AL · CT · LK · IN · TB 입니다.</div>

			<%-- ===== D: 밀도 ===== --%>
			<section aria-labelledby="cat-density" class="space-y-3">
				<h2 id="cat-density" class="text-lg font-bold">D · 밀도(글자 크기와 칸 여백)</h2>
				@for(var v : List.of(List.of("D1", "table-xs", "아주 촘촘 - 열이 많은 표용 (9)"), List.of("D2", "table-sm", "촘촘 - 지금 가장 많이 씀 (30)"), List.of("D3", "", "기본 - daisyUI 기본 크기 (0)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(2)}</span></h3>
						<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
							<table class="table ${v.get(1)} w-full" aria-label="${v.get(0)} 견본">
								<thead><tr>
									<th scope="col">종목</th><th scope="col">계좌</th><th scope="col" class="text-right">수량</th><th scope="col" class="text-right">평가금액</th><th scope="col" class="text-right">평가손익</th><th scope="col" class="text-right">수익률</th>
								</tr></thead>
								<tbody>
									@for(var r : rows)
										<tr>
											<th scope="row" class="font-medium">${(String) r.get(0)}</th>
											<td>${(String) r.get(1)}</td>
											<td class="text-right tabular-nums">${String.valueOf(r.get(2))}</td>
											@template._components.ui.amountCell(value = (BigDecimal) r.get(3))
											@template._components.ui.amountCell(value = (BigDecimal) r.get(4), signed = true)
											<td class="text-right tabular-nums ${pctClass.apply((BigDecimal) r.get(5))}">${((BigDecimal) r.get(5)).toPlainString()}%</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					</div>
				@endfor
			</section>

			<%-- ===== Z: 줄 구분 ===== --%>
			<section aria-labelledby="cat-zebra" class="space-y-3">
				<h2 id="cat-zebra" class="text-lg font-bold">Z · 줄 구분</h2>
				@for(var v : List.of(List.of("Z1", "table-zebra", "", "줄무늬 (14)"), List.of("Z2", "", "", "구분 없음 - 줄 사이 선만 (25)"), List.of("Z3", "", "hover:bg-base-200", "구분 없음 + 마우스를 올린 줄 강조 (0)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(3)}</span></h3>
						<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
							<table class="table table-sm ${v.get(1)} w-full" aria-label="${v.get(0)} 견본">
								<thead><tr>
									<th scope="col">종목</th><th scope="col">계좌</th><th scope="col" class="text-right">평가금액</th><th scope="col" class="text-right">수익률</th>
								</tr></thead>
								<tbody>
									@for(var r : rows)
										<tr class="${v.get(2)}">
											<th scope="row" class="font-medium">${(String) r.get(0)}</th>
											<td>${(String) r.get(1)}</td>
											@template._components.ui.amountCell(value = (BigDecimal) r.get(3))
											<td class="text-right tabular-nums ${pctClass.apply((BigDecimal) r.get(5))}">${((BigDecimal) r.get(5)).toPlainString()}%</td>
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					</div>
				@endfor
			</section>

			<%-- ===== B: 표를 감싸는 상자 ===== --%>
			<section aria-labelledby="cat-box" class="space-y-3">
				<h2 id="cat-box" class="text-lg font-bold">B · 표를 감싸는 상자</h2>
				<div class="space-y-1">
					<h3 class="text-sm font-semibold">B1 <span class="font-normal text-base-content/60">가로 스크롤 상자만 - 배경 · 테두리 없음 (21, 상자 없이 표만 5)</span></h3>
					<div class="overflow-x-auto">
						<table class="table table-sm w-full" aria-label="B1 견본">
							<thead><tr><th scope="col">종목</th><th scope="col" class="text-right">평가금액</th><th scope="col" class="text-right">평가손익</th></tr></thead>
							<tbody>
								@for(var r : rows)
									<tr><th scope="row" class="font-medium">${(String) r.get(0)}</th>@template._components.ui.amountCell(value = (BigDecimal) r.get(3))@template._components.ui.amountCell(value = (BigDecimal) r.get(4), signed = true)</tr>
								@endfor
							</tbody>
						</table>
					</div>
				</div>
				<div class="space-y-1">
					<h3 class="text-sm font-semibold">B2 <span class="font-normal text-base-content/60">둥근 테두리 상자 (2)</span></h3>
					<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
						<table class="table table-sm w-full" aria-label="B2 견본">
							<thead><tr><th scope="col">종목</th><th scope="col" class="text-right">평가금액</th><th scope="col" class="text-right">평가손익</th></tr></thead>
							<tbody>
								@for(var r : rows)
									<tr><th scope="row" class="font-medium">${(String) r.get(0)}</th>@template._components.ui.amountCell(value = (BigDecimal) r.get(3))@template._components.ui.amountCell(value = (BigDecimal) r.get(4), signed = true)</tr>
								@endfor
							</tbody>
						</table>
					</div>
				</div>
				<div class="space-y-1">
					<h3 class="text-sm font-semibold">B3 <span class="font-normal text-base-content/60">카드 안 - 제목과 표가 한 카드 (11)</span></h3>
					@template._components.ui.tableCard(body = @`
						<div class="card-body p-4 gap-3">
							<h4 class="card-title text-base">종목별 평가</h4>
							<div class="overflow-x-auto">
								<table class="table table-sm w-full" aria-label="B3 견본">
									<thead><tr><th scope="col">종목</th><th scope="col" class="text-right">평가금액</th><th scope="col" class="text-right">평가손익</th></tr></thead>
									<tbody>
										@for(var r : rows)
											<tr><th scope="row" class="font-medium">${(String) r.get(0)}</th>@template._components.ui.amountCell(value = (BigDecimal) r.get(3))@template._components.ui.amountCell(value = (BigDecimal) r.get(4), signed = true)</tr>
										@endfor
									</tbody>
								</table>
							</div>
						</div>
					`)
				</div>
			</section>

			<%-- ===== H: 머리글 ===== --%>
			<section aria-labelledby="cat-head" class="space-y-3">
				<h2 id="cat-head" class="text-lg font-bold">H · 머리글</h2>
				<div class="space-y-1">
					<h3 class="text-sm font-semibold">H1 <span class="font-normal text-base-content/60">글자만 - 정렬 없음</span></h3>
					<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
						<table class="table table-sm w-full" aria-label="H1 견본">
							<thead><tr><th scope="col">종목</th><th scope="col" class="text-right">평가금액</th><th scope="col" class="text-right">수익률</th></tr></thead>
							<tbody>
								@for(var r : rows)
									<tr><th scope="row" class="font-medium">${(String) r.get(0)}</th>@template._components.ui.amountCell(value = (BigDecimal) r.get(3))<td class="text-right tabular-nums ${pctClass.apply((BigDecimal) r.get(5))}">${((BigDecimal) r.get(5)).toPlainString()}%</td></tr>
								@endfor
							</tbody>
						</table>
					</div>
				</div>
				<div class="space-y-1">
					<h3 class="text-sm font-semibold">H2 <span class="font-normal text-base-content/60">정렬 단추 + 용어 설명 ⓘ (sortButtonHeader - 자산 현황 · 배당 수익 표)</span></h3>
					<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
						<table class="table table-sm w-full" aria-label="H2 견본">
							<thead><tr class="text-base-content/60">
								@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "name", sortType = "text", label = "종목", buttonClass = sortButtonStart)
								@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "value", sortType = "number", label = "평가금액", cellClass = "text-right", buttonClass = sortButton)
								@template.stock.htmx.fragments.components.sortButtonHeader(sortKey = "rate", sortType = "number", label = "수익률", hint = "평가손익을 매입원금으로 나눈 값입니다.", cellClass = "text-right", buttonClass = sortButton)
							</tr></thead>
							<tbody>
								@for(var r : rows)
									<tr><th scope="row" class="font-medium">${(String) r.get(0)}</th>@template._components.ui.amountCell(value = (BigDecimal) r.get(3))<td class="text-right tabular-nums ${pctClass.apply((BigDecimal) r.get(5))}">${((BigDecimal) r.get(5)).toPlainString()}%</td></tr>
								@endfor
							</tbody>
						</table>
					</div>
				</div>
				<div class="space-y-1">
					<h3 class="text-sm font-semibold">H3 <span class="font-normal text-base-content/60">머리글 줄에 배경색</span></h3>
					<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
						<table class="table table-sm w-full" aria-label="H3 견본">
							<thead class="bg-base-200"><tr><th scope="col">종목</th><th scope="col" class="text-right">평가금액</th><th scope="col" class="text-right">수익률</th></tr></thead>
							<tbody>
								@for(var r : rows)
									<tr><th scope="row" class="font-medium">${(String) r.get(0)}</th>@template._components.ui.amountCell(value = (BigDecimal) r.get(3))<td class="text-right tabular-nums ${pctClass.apply((BigDecimal) r.get(5))}">${((BigDecimal) r.get(5)).toPlainString()}%</td></tr>
								@endfor
							</tbody>
						</table>
					</div>
				</div>
				<p class="text-xs text-base-content/60">머리글을 스크롤에 붙이는 동작(지금 table-pin-rows · table-head-sticky 두 방식)은 모양이 아니라 동작이라 통합할 때 하나로 맞춥니다.</p>
			</section>

			<%-- ===== N: 숫자 열 ===== --%>
			<section aria-labelledby="cat-num" class="space-y-3">
				<h2 id="cat-num" class="text-lg font-bold">N · 숫자 열</h2>
				@for(var v : List.of(List.of("N1", "tabular-nums", "본문 글꼴 + 자릿수 맞춤 (amountCell 기본)"), List.of("N2", "font-mono", "고정폭 글꼴 (일부 합계 · 배당 표)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(2)}</span></h3>
						<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100">
							<table class="table table-sm w-full" aria-label="${v.get(0)} 견본">
								<thead><tr><th scope="col">종목</th><th scope="col" class="text-right">수량</th><th scope="col" class="text-right">평가금액</th><th scope="col" class="text-right">평가손익</th></tr></thead>
								<tbody>
									@for(var r : rows)
										<tr>
											<th scope="row" class="font-medium">${(String) r.get(0)}</th>
											<td class="text-right ${v.get(1)}">${String.valueOf(r.get(2))}</td>
											@template._components.ui.amountCell(value = (BigDecimal) r.get(3), tdClass = "text-right " + v.get(1))
											@template._components.ui.amountCell(value = (BigDecimal) r.get(4), signed = true, tdClass = "text-right " + v.get(1))
										</tr>
									@endfor
								</tbody>
							</table>
						</div>
					</div>
				@endfor
			</section>

			<%-- ===== W: 긴 글자 ===== --%>
			<section aria-labelledby="cat-wrap" class="space-y-3">
				<h2 id="cat-wrap" class="text-lg font-bold">W · 긴 글자(좁은 화면에서 차이가 큽니다)</h2>
				@for(var v : List.of(List.of("W1", "whitespace-nowrap", "줄바꿈 안 함 - 넘치면 가로 스크롤 (7)"), List.of("W2", "", "줄바꿈 - 칸 안에서 접힘 (32)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(2)}</span></h3>
						<div class="overflow-x-auto rounded-box border border-base-300/60 bg-base-100 max-w-md">
							<table class="table table-sm ${v.get(1)} w-full" aria-label="${v.get(0)} 견본">
								<thead><tr><th scope="col">종목</th><th scope="col">계좌</th><th scope="col" class="text-right">평가금액</th></tr></thead>
								<tbody>
									@for(var r : rows)
										<tr><th scope="row" class="font-medium">${(String) r.get(0)}</th><td>${(String) r.get(1)}</td>@template._components.ui.amountCell(value = (BigDecimal) r.get(3))</tr>
									@endfor
								</tbody>
							</table>
						</div>
					</div>
				@endfor
			</section>

			<%-- ===== BT: 버튼 ===== --%>
			<section aria-labelledby="cat-btn" class="space-y-3">
				<h2 id="cat-btn" class="text-lg font-bold">BT · 버튼(보조 동작의 모양)</h2>
				<p class="text-sm text-base-content/70">주 동작은 btn-primary 하나로 둡니다. 고를 것은 보조 동작(취소 · 내보내기 · 펼치기 등)의 모양입니다. 크기는 본문 sm, 표 · 도구 막대 안 xs 로 맞춥니다.</p>
				@for(var v : List.of(List.of("BT1", "btn", "회색 바탕 (21)"), List.of("BT2", "btn btn-outline", "테두리 (18)"), List.of("BT3", "btn btn-ghost", "바탕 없음 - 지금 도구 막대 (28)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(2)}</span></h3>
						<div class="flex flex-wrap items-center gap-2 rounded-box border border-base-300/60 bg-base-100 p-3">
							<button type="button" class="btn btn-primary btn-sm">저장</button>
							<button type="button" class="${v.get(1)} btn-sm">취소</button>
							<button type="button" class="${v.get(1)} btn-sm">내보내기</button>
							<span class="mx-2 text-xs text-base-content/50">표 · 도구 막대 안</span>
							<button type="button" class="${v.get(1)} btn-xs">펼치기</button>
							<button type="button" class="btn btn-error btn-outline btn-sm">삭제</button>
						</div>
					</div>
				@endfor
			</section>

			<%-- ===== BG: 배지 ===== --%>
			<section aria-labelledby="cat-badge" class="space-y-3">
				<h2 id="cat-badge" class="text-lg font-bold">BG · 배지(상태 · 분류 · 개수)</h2>
				@for(var v : List.of(List.of("BG1", "badge-outline", "테두리 (25)"), List.of("BG2", "badge-ghost", "회색 바탕 - 색 없이 (15)"), List.of("BG3", "", "진한 바탕 (11)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(2)}</span></h3>
						<div class="flex flex-wrap items-center gap-2 rounded-box border border-base-300/60 bg-base-100 p-3">
							<span class="badge badge-sm ${v.get(1)} badge-success">적중</span>
							<span class="badge badge-sm ${v.get(1)} badge-error">실패</span>
							<span class="badge badge-sm ${v.get(1)} badge-warning">늦음</span>
							<span class="badge badge-sm ${v.get(1)} badge-info">월배당</span>
							<span class="badge badge-sm ${v.get(1)}">기타</span>
							<span class="badge badge-sm ${v.get(1)} badge-neutral">+3</span>
						</div>
					</div>
				@endfor
				<p class="text-xs text-base-content/60">크기는 sm 하나로 맞춥니다(지금 xs 18 · sm 27 · 기본 5).</p>
			</section>

			<%-- ===== AL: 알림 ===== --%>
			<section aria-labelledby="cat-alert" class="space-y-3">
				<h2 id="cat-alert" class="text-lg font-bold">AL · 알림 상자</h2>
				<div class="space-y-1">
					<h3 class="text-sm font-semibold">AL1 <span class="font-normal text-base-content/60">진한 바탕 - daisyUI 기본 (8)</span></h3>
					<div class="space-y-2 rounded-box border border-base-300/60 bg-base-100 p-3">
						<div role="alert" class="alert alert-error">데이터를 불러오지 못했습니다.</div>
						<div role="alert" class="alert alert-warning">시세가 3일 전 값입니다.</div>
						<div role="status" class="alert alert-success">저장했습니다.</div>
						<div role="note" class="alert alert-info">장 시작 뒤 예측은 적중률에서 뺍니다.</div>
					</div>
				</div>
				<div class="space-y-1">
					<h3 class="text-sm font-semibold">AL2 <span class="font-normal text-base-content/60">옅은 바탕 + 테두리 (5)</span></h3>
					<div class="space-y-2 rounded-box border border-base-300/60 bg-base-100 p-3">
						<div role="alert" class="alert alert-error bg-error/10 border border-error/20 text-base-content">데이터를 불러오지 못했습니다.</div>
						<div role="alert" class="alert alert-warning bg-warning/10 border border-warning/20 text-base-content">시세가 3일 전 값입니다.</div>
						<div role="status" class="alert alert-success bg-success/10 border border-success/20 text-base-content">저장했습니다.</div>
						<div role="note" class="alert alert-info bg-info/10 border border-info/20 text-base-content">장 시작 뒤 예측은 적중률에서 뺍니다.</div>
					</div>
				</div>
			</section>

			<%-- ===== CT: 카드 제목 ===== --%>
			<section aria-labelledby="cat-ct" class="space-y-3">
				<h2 id="cat-ct" class="text-lg font-bold">CT · 카드 제목 크기</h2>
				<div class="grid gap-3 md:grid-cols-2">
					@for(var v : List.of(List.of("CT1", "text-base", "16px - 공용 card 기본 (16)"), List.of("CT2", "text-lg", "18px (16)")))
						<div class="space-y-1">
							<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(2)}</span></h3>
							@template._components.ui.card(body = @`
								<h4 class="card-title ${v.get(1)} mb-2">종목별 평가</h4>
								<p class="text-sm text-base-content/70">카드 본문 글자는 14px 입니다. 제목과 본문의 크기 차이를 비교해 보세요.</p>
							`)
						</div>
					@endfor
				</div>
			</section>

			<%-- ===== LK: 링크 ===== --%>
			<section aria-labelledby="cat-link" class="space-y-3">
				<h2 id="cat-link" class="text-lg font-bold">LK · 본문 · 표 안 링크</h2>
				@for(var v : List.of(List.of("LK1", "link link-hover", "마우스를 올리면 밑줄 (35)"), List.of("LK2", "link link-hover no-underline", "밑줄 없음 - 색만 (24)"), List.of("LK3", "link link-primary", "늘 강조색 + 밑줄 (2)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(2)}</span></h3>
						<div class="rounded-box border border-base-300/60 bg-base-100 p-3 text-sm">
							보유 종목 <a href="#cat-link" class="${v.get(1)}">KODEX 200</a> 의 상세 화면으로 갑니다. 표 안: <a href="#cat-link" class="${v.get(1)} font-medium">TIGER 미국S&amp;P500</a>
						</div>
					</div>
				@endfor
			</section>

			<%-- ===== IN: 입력 ===== --%>
			<section aria-labelledby="cat-input" class="space-y-3">
				<h2 id="cat-input" class="text-lg font-bold">IN · 입력 · 선택 크기</h2>
				@for(var v : List.of(List.of("IN1", "", "기본 높이 (28)"), List.of("IN2", "-sm", "작게 (14)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(2)}</span></h3>
						<div class="flex flex-wrap items-end gap-3 rounded-box border border-base-300/60 bg-base-100 p-3">
							<label class="form-control w-48"><span class="label-text text-sm">기준일</span><input type="date" class="input input-bordered input${v.get(1)} w-full" value="2026-11-10" /></label>
							<label class="form-control w-48"><span class="label-text text-sm">계좌</span><select class="select select-bordered select${v.get(1)} w-full"><option>전체</option><option>연금저축</option></select></label>
							<label class="form-control w-56"><span class="label-text text-sm">종목 검색</span><input type="text" class="input input-bordered input${v.get(1)} w-full" placeholder="이름 · 코드" /></label>
							<button type="button" class="btn btn-primary btn${v.get(1)}">조회</button>
						</div>
					</div>
				@endfor
			</section>

			<%-- ===== TB: 탭 ===== --%>
			<section aria-labelledby="cat-tab" class="space-y-3">
				<h2 id="cat-tab" class="text-lg font-bold">TB · 탭</h2>
				@for(var v : List.of(List.of("TB1", "tabs tabs-box w-full bg-base-200/70 p-1", "tab flex-1", "상자형 · 칸 나눔 (자산 성장 · 배당 기간별, 9)"), List.of("TB2", "tabs tabs-box tabs-xs w-fit", "tab", "상자형 · 작게 (3)"), List.of("TB3", "flex gap-1", "tab", "상자 없이 탭 글자만 (6)")))
					<div class="space-y-1">
						<h3 class="text-sm font-semibold">${v.get(0)} <span class="font-normal text-base-content/60">${v.get(3)}</span></h3>
						<div class="rounded-box border border-base-300/60 bg-base-100 p-3">
							<div role="tablist" aria-label="${v.get(0)} 견본" class="${v.get(1)}">
								<a role="tab" href="#cat-tab" class="${v.get(2)} tab-active" aria-selected="true">연도별</a>
								<a role="tab" href="#cat-tab" class="${v.get(2)}" aria-selected="false">월별</a>
								<a role="tab" href="#cat-tab" class="${v.get(2)}" aria-selected="false">종목별</a>
							</div>
						</div>
					</div>
				@endfor
			</section>
		</div>
	`
)
