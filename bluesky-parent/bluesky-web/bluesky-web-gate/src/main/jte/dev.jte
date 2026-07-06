@template._layout.defaultLayout(
	content = @`
	<div class="max-w-5xl mx-auto px-4 pb-16">
	<div class="prose">

		<div class="collapse">
			<input type="checkbox" />
			<h1 class="collapse-title m-0">
				FE 관련 개발 설정
			</h1>
			<div class="collapse-content">
				<h2 class="m-0">npm 설정</h2>
				<div>
					 <code>src/main/frontend</code> 위치에서 아래 명령 실행하면 FE 프로젝트 관련 설정이 설치됩니다.
					<div class="mockup-code">
						<pre data-prefix="$"><code>npm install</code></pre>
					</div>
				</div>
				<h2 class="m-0">watch 사용</h2>
				<div>
					프로젝트 최상위 위치에 제공되는 <code>npm-watch.bat</code>를 실행하거나
					<div class="mockup-code">
						<pre data-prefix="$"><code>npm-watch.bat</code></pre>
					</div>
					<code>src/main/frontend</code> 위치에서 직접 실행하면 개발하는 template 소스의 tailwindcss class 변경이 실시간으로 반영됩니다.
					<div class="mockup-code">
						<pre data-prefix="$"><code>npm run watch</code></pre>
					</div>
				</div>
			</div>
		</div>

		<div class="collapse">
			<input type="checkbox" />
			<h1 class="collapse-title m-0">
				BE 개발 관련 dom attribute 설정 정보
			</h1>
			<div class="collapse-content">
				tailwindcss 유틸이나 main.css 컴포넌트 class 명명 규칙과 별개로 사용하기 위한 값들에 대한 목록
				<ul>
					<li>board
						<ul>
							<li>css selector
								<ul>
									<li><b>.writeButton</b> : 글쓰기 버튼</li>
									<li><b>.listButton</b> : 글목록 버튼</li>
								</ul>
							</li>
							<li>Htmx trigger
								<ul>
									<li>요청
										<ul>
											<li><b>showListTrigger</b></li>
										</ul>
									</li>
									<li>응답
										<ul>
											<li><b>showListResponseTrigger</b></li>
										</ul>
									</li>
								</ul>
							</li>
						</ul>
					</li>
				</ul>
			</div>
		</div>
	</div>

	<!-- ============================================================
	     UI 컴포넌트 쇼케이스 — main.css @layer components 회귀 확인용.
	     CSS 수정 후 이 페이지(light/dark 각각)만 훑으면 깨진 컴포넌트를 빠르게 잡는다.
	     ============================================================ -->
	<div class="space-y-10 mt-8">
		<h1 class="text-2xl font-bold">UI Component 쇼케이스</h1>
		<p class="text-sm text-base-content/60">main.css 에서 직접 소유하는 컴포넌트 목록. 우측 상단 토글로 dark 테마도 확인할 것.</p>

		<section>
			<h2 class="text-lg font-semibold mb-3">btn</h2>
			<div class="flex flex-wrap items-center gap-2">
				<button class="btn">default</button>
				<button class="btn btn-primary">primary</button>
				<button class="btn btn-secondary">secondary</button>
				<button class="btn btn-warning">warning</button>
				<button class="btn btn-error">error</button>
				<button class="btn btn-neutral">neutral</button>
				<button class="btn btn-ghost">ghost</button>
				<button class="btn btn-outline">outline</button>
				<button class="btn btn-active">active</button>
				<button class="btn btn-disabled">disabled</button>
			</div>
			<div class="flex flex-wrap items-center gap-2 mt-2">
				<button class="btn btn-sm">btn-sm</button>
				<button class="btn btn-xs">btn-xs</button>
				<button class="btn btn-sm btn-circle">○</button>
				<button class="btn btn-sm btn-primary">sm primary</button>
			</div>
			<div class="mt-2">
				<div class="join shadow-sm">
					<input class="join-item btn btn-sm" type="radio" name="devSeg" aria-label="세그먼트A" checked />
					<input class="join-item btn btn-sm" type="radio" name="devSeg" aria-label="세그먼트B" />
					<input class="join-item btn btn-sm" type="radio" name="devSeg" aria-label="세그먼트C" />
				</div>
				<span class="text-xs text-base-content/50 ml-2">radio-as-btn 세그먼트 (join)</span>
			</div>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">badge</h2>
			<div class="flex flex-wrap items-center gap-2">
				<span class="badge">default</span>
				<span class="badge badge-primary">primary</span>
				<span class="badge badge-secondary">secondary</span>
				<span class="badge badge-info">info</span>
				<span class="badge badge-success">success</span>
				<span class="badge badge-error">error</span>
				<span class="badge badge-ghost">ghost</span>
				<span class="badge badge-error badge-outline">error outline</span>
				<span class="badge badge-lg">lg</span>
				<span class="badge badge-sm">sm</span>
				<span class="badge badge-xs">xs</span>
				<span class="badge badge-neutral">neutral</span>
			</div>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">alert / toast</h2>
			<div class="space-y-2">
				<div class="alert">default alert</div>
				<div class="alert alert-info">info alert</div>
				<div class="alert alert-success">success alert</div>
				<div class="alert alert-error">error alert</div>
			</div>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">card / stat</h2>
			<div class="grid grid-cols-1 md:grid-cols-3 gap-3">
				<div class="card bg-base-100 border border-base-200 rounded-box shadow-sm">
					<div class="card-body p-4">
						<h3 class="card-title text-base">card-title</h3>
						<p class="text-sm text-base-content/60">card-body 내용</p>
						<div class="card-actions"><button class="btn btn-sm btn-primary">action</button></div>
					</div>
				</div>
				@template._components.ui.statCard(label = "statCard label", value = "1,234,567", sub = "sub text", tone = "text-dividend")
				<div class="stats border border-base-200 shadow-sm">
					<div class="stat">
						<div class="stat-title">stat-title</div>
						<div class="stat-value">89.4%</div>
						<div class="stat-desc">stat-desc</div>
					</div>
				</div>
				<div class="stats border border-base-200 shadow-sm">
					<div class="stat">
						<div class="stat-figure text-primary">
							<svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
								<path stroke-linecap="round" stroke-linejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
							</svg>
						</div>
						<div class="stat-title">stat-figure 포함</div>
						<div class="stat-value text-profit">+1,234</div>
						<div class="stat-desc">아이콘은 우측 세로중앙</div>
					</div>
				</div>
			</div>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">table (sm / zebra / hover / pin-rows)</h2>
			<div class="max-h-40 overflow-auto border border-base-200 rounded-box">
				<table class="table table-sm table-zebra table-hover table-pin-rows">
					<thead><tr><th>컬럼A</th><th>컬럼B</th><th class="text-right">금액</th></tr></thead>
					<tbody>
						<tr><td>1행</td><td><span class="badge badge-success badge-sm">badge</span></td><td class="text-right font-mono text-profit">+12,345</td></tr>
						<tr><td>2행</td><td>텍스트</td><td class="text-right font-mono text-loss">-6,789</td></tr>
						<tr><td>3행</td><td>텍스트</td><td class="text-right font-mono text-dividend">1,013,173</td></tr>
						<tr><td>4행</td><td>텍스트</td><td class="text-right font-mono">0</td></tr>
						<tr><td>5행</td><td>텍스트</td><td class="text-right font-mono">0</td></tr>
					</tbody>
				</table>
			</div>
			<p class="text-xs text-base-content/50 mt-1">스크롤 시 thead 고정(pin-rows), 손익 색: 빨강=수익 / 파랑=손실 / 초록=배당</p>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">form (input / select / textarea / checkbox / toggle / label)</h2>
			<div class="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-2xl">
				<div class="form-control">
					<label class="label p-1"><span class="label-text text-xs">input</span></label>
					<input class="input input-bordered input-sm w-full" placeholder="placeholder" />
				</div>
				<div class="form-control">
					<label class="label p-1"><span class="label-text text-xs">select</span></label>
					<select class="select select-bordered select-sm w-full">
						<option>옵션 1</option><option>옵션 2</option>
					</select>
				</div>
				<div class="form-control">
					<label class="label p-1"><span class="label-text text-xs">textarea</span></label>
					<textarea class="textarea textarea-bordered w-full" rows="2" placeholder="textarea"></textarea>
				</div>
				<div class="space-y-2">
					<label class="label"><input type="checkbox" class="checkbox checkbox-sm checkbox-primary" checked /> <span class="label-text">checkbox</span></label>
					<label class="label"><input type="checkbox" class="toggle toggle-primary" checked /> <span class="label-text">toggle</span></label>
					<label class="label"><input type="checkbox" class="toggle toggle-xs" /> <span class="label-text">toggle-xs</span></label>
				</div>
			</div>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">loading / skeleton / progress</h2>
			<div class="flex flex-wrap items-center gap-4">
				<span class="loading loading-spinner loading-sm text-primary"></span>
				<span class="loading loading-spinner"></span>
				<span class="loading loading-ring loading-lg text-primary"></span>
				<div class="skeleton h-6 w-32"></div>
				<div class="skeleton h-6 w-6 rounded-full"></div>
			</div>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">tabs / menu / join</h2>
			<div role="tablist" class="tabs tabs-box w-fit bg-base-200/70 p-1 mb-3">
				<a role="tab" class="tab tab-active">활성 탭</a>
				<a role="tab" class="tab">탭 2</a>
				<a role="tab" class="tab">탭 3</a>
			</div>
			<div class="flex flex-wrap gap-6 items-start">
				<ul class="menu p-2 gap-1 bg-base-100 border border-base-200 rounded-box w-44">
					<li><a class="bg-primary/10 text-primary">활성 메뉴</a></li>
					<li><a>메뉴 항목</a></li>
					<li><a>메뉴 항목</a></li>
				</ul>
				<div class="join">
					<button class="join-item btn btn-sm">&laquo;</button>
					<button class="join-item btn btn-sm btn-active">1</button>
					<button class="join-item btn btn-sm">2</button>
					<button class="join-item btn btn-sm">&raquo;</button>
				</div>
			</div>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">dropdown / tooltip / collapse / divider</h2>
			<div class="flex flex-wrap gap-6 items-start">
				<div class="dropdown">
					<div tabindex="0" role="button" class="btn btn-sm">dropdown ▾</div>
					<ul tabindex="0" class="menu menu-sm dropdown-content bg-base-100 rounded-box z-1 mt-1 w-40 p-2 shadow">
						<li><a>항목 1</a></li>
						<li><a>항목 2</a></li>
					</ul>
				</div>
				<div class="tooltip" data-tip="tooltip 내용">
					<button class="btn btn-sm">tooltip hover</button>
				</div>
			</div>
			<div class="divider">divider</div>
			<div class="collapse bg-base-200/50 rounded-box max-w-md">
				<input type="checkbox" />
				<div class="collapse-title font-medium">collapse 제목 (클릭)</div>
				<div class="collapse-content"><p class="text-sm">collapse 내용</p></div>
			</div>
		</section>

		<section>
			<h2 class="text-lg font-semibold mb-3">색 토큰 (base / semantic)</h2>
			<div class="flex flex-wrap gap-2 text-xs">
				<div class="rounded-box border border-base-300 bg-base-100 p-3">base-100</div>
				<div class="rounded-box bg-base-200 p-3">base-200</div>
				<div class="rounded-box bg-base-300 p-3">base-300</div>
				<div class="rounded-box bg-primary text-primary-content p-3">primary</div>
				<div class="rounded-box bg-secondary text-secondary-content p-3">secondary</div>
				<div class="rounded-box bg-info text-info-content p-3">info</div>
				<div class="rounded-box bg-success text-success-content p-3">success</div>
				<div class="rounded-box bg-warning text-warning-content p-3">warning</div>
				<div class="rounded-box bg-error text-error-content p-3">error</div>
				<div class="rounded-box bg-neutral text-neutral-content p-3">neutral</div>
			</div>
		</section>
	</div>
	</div>
	`
)
