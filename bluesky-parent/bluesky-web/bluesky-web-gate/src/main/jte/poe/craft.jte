@import java.util.List
@import net.luversof.web.gate.poe.dto.PoeModItemClass
@import net.luversof.web.gate.poe.dto.PoeCraft
@import net.luversof.web.gate.poe.PoeText

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param List<PoeModItemClass> modClasses = java.util.List.of()
@param String activeClass = ""
@param String activeVariant = ""
@param String activeInfluence = ""
@param int ilvl = 84
@param String classLabel = ""
@param PoeCraft.View view = null
@param String error = null
@param String lastAction = null
@param java.util.List<String> added = java.util.List.of()
@param String stateJson = "{}"
@param String historyJson = "[]"
@param int historySize = 0
@param java.util.List<PoeCraft.PoolLine> pool = java.util.List.of()
@param java.util.List<PoeCraft.Essence> essences = java.util.List.of()
@param java.util.List<PoeCraft.PoolLine> calcPool = java.util.List.of()

<%-- PoE1 제작 에뮬레이터(10-15 C335 — 사용자 요청 "craftofexile 처럼 직접 아이템을 제작해 보는 에뮬레이터"). 종류 · 변형 · 영향력 · 아이템 레벨을
     고르면 새 일반 아이템, 화폐를 누르면 그 결과(옵션 확률은 게임 데이터의 풀별 가중치). --%>
!{
	boolean ko = PoeText.isKorean();
	String pageTitle = ko ? "제작 에뮬레이터" : "Crafting Emulator";
	PoeModItemClass active = modClasses.stream().filter(c -> c.itemClass().equals(activeClass)).findFirst().orElse(null);
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4" data-craft-page>
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
				@if(!patch.isEmpty())<span class="badge badge-ghost">${patch}</span>@endif
				<span class="text-sm text-base-content/60">${ko ? "화폐를 하나씩 써 보며 결과와 다음 옵션 확률을 봅니다. 확률은 게임 데이터의 옵션 가중치로 계산합니다." : "Apply currency one step at a time and see the result and the odds of the next modifier, computed from the game's modifier weights."}</span>
			</div>

			<form method="get" action="/poe/craft" class="flex flex-wrap items-end gap-3" data-craft-picker
			      hx-get="/poe/craft" hx-trigger="change" hx-target="[data-craft-page]" hx-select="[data-craft-page]" hx-swap="outerHTML" hx-push-url="true">
				<label class="form-control min-w-0">
					<span class="label-text text-xs">${ko ? "아이템 종류" : "Item class"}</span>
					<select name="itemClass" class="select select-sm select-bordered">
						@for(PoeModItemClass c : modClasses)
							<option value="${c.itemClass()}" selected="${c.itemClass().equals(activeClass)}">${ko && c.nameKo() != null ? c.nameKo() : c.name()}</option>
						@endfor
					</select>
				</label>
				@if(active != null && !active.variants().isEmpty())
					<label class="form-control min-w-0">
						<span class="label-text text-xs">${ko ? "속성 변형" : "Attribute variant"}</span>
						<select name="variant" class="select select-sm select-bordered">
							@for(PoeModItemClass.Variant v : active.variants())
								<option value="${v.key()}" selected="${v.key().equals(activeVariant)}">${ko && v.nameKo() != null ? v.nameKo() : v.name()}</option>
							@endfor
						</select>
					</label>
				@endif
				@if(active != null && active.influences() != null && !active.influences().isEmpty())
					<label class="form-control min-w-0">
						<span class="label-text text-xs">${ko ? "영향력" : "Influence"}</span>
						<select name="influence" class="select select-sm select-bordered">
							<option value="" selected="${activeInfluence.isEmpty()}">${ko ? "없음" : "None"}</option>
							@for(PoeModItemClass.Influence inf : active.influences())
								<option value="${inf.key()}" selected="${inf.key().equals(activeInfluence)}">${ko && inf.nameKo() != null ? inf.nameKo() : inf.name()}</option>
							@endfor
						</select>
					</label>
				@endif
				<label class="form-control w-28">
					<span class="label-text text-xs">${ko ? "아이템 레벨" : "Item level"}</span>
					<input type="number" name="ilvl" min="1" max="100" value="${ilvl}" class="input input-sm input-bordered">
				</label>
			</form>

			@if(view != null)
				@template.poe.htmx.craftEmu(view = view, error = error, lastAction = lastAction, added = added, stateJson = stateJson, historyJson = historyJson, historySize = historySize, pool = pool, essences = essences, classLabel = classLabel)
			@else
				<p class="text-sm text-base-content/60">${ko ? "제작 데이터를 불러오지 못했습니다." : "Crafting data is not available."}</p>
			@endif

			@if(!calcPool.isEmpty())
				<%-- 계산기(10-15 C336) — 계열마다 최소 등급을 고르면 방법별 기대 비용(가장 적은 비용 순). 목표는 일반 → 레어 기준 전체 풀에서 고른다 --%>
				<section class="space-y-3 border-t border-base-300 pt-4" data-craft-calc data-no-success-toast>
					<h2 class="text-lg font-semibold">${ko ? "제작 계산기 — 가장 적은 비용" : "Crafting calculator — cheapest method"}</h2>
					<p class="text-sm text-base-content/60">${ko ? "원하는 옵션의 최소 등급을 고르면 카오스 · 변경 · 에센스 연타의 성공 확률과 기대 비용(poe.ninja 시세)을 비교합니다." : "Pick the minimum tier of the modifiers you want to compare chaos, alteration and essence spam by chance and expected cost (poe.ninja prices)."}</p>
					<form hx-post="/poe/htmx/craft/calc" hx-target="#craft-calc-result" hx-swap="outerHTML" class="space-y-3">
						<input type="hidden" name="itemClass" value="${activeClass}">
						<input type="hidden" name="variant" value="${activeVariant}">
						<input type="hidden" name="influence" value="${activeInfluence}">
						<input type="hidden" name="ilvl" value="${ilvl}">
						<div class="grid gap-3 lg:grid-cols-2">
							@for(String gen : java.util.List.of("prefix", "suffix"))
								!{
									java.util.Map<String, java.util.List<PoeCraft.PoolLine>> fams = new java.util.LinkedHashMap<>();
									for (PoeCraft.PoolLine p : calcPool) if (gen.equals(p.gen())) fams.computeIfAbsent(p.family(), k -> new java.util.ArrayList<>()).add(p);
								}
								<div class="space-y-1 min-w-0">
									<div class="text-sm font-medium">${"prefix".equals(gen) ? (ko ? "접두" : "Prefixes") : (ko ? "접미" : "Suffixes")}</div>
									@for(var fe : fams.entrySet())
										!{
											java.util.List<PoeCraft.PoolLine> tiers = fe.getValue().stream().sorted(java.util.Comparator.comparing(p -> p.tierRank() == null ? 99 : p.tierRank())).toList();
											PoeCraft.PoolLine top = tiers.get(0);
											java.util.List<String> mx = ko && top.ko() != null && !top.ko().isEmpty() ? top.ko() : top.en();
											java.util.List<String> mn = ko && top.koMin() != null && !top.koMin().isEmpty() ? top.koMin() : top.enMin();
										}
										<label class="flex items-center gap-2 text-xs min-w-0" data-craft-calc-family="${gen}:${fe.getKey()}">
											<select name="t_${gen}:${fe.getKey()}" class="select select-xs select-bordered w-24 shrink-0" aria-label="${ko ? "최소 등급" : "Minimum tier"}">
												<option value="">-</option>
												@for(PoeCraft.PoolLine t : tiers)
													@if(t.tierRank() != null)<option value="${t.tierRank()}">T1@if(t.tierRank() > 1)~T${t.tierRank()}@endif</option>@endif
												@endfor
											</select>
											<span class="min-w-0 wrap-anywhere">@for(int i = 0; i < mx.size(); i++)$unsafe{PoeText.highlightValues(net.luversof.web.gate.poe.PoeModRange.range(mn != null && i < mn.size() ? mn.get(i) : null, mx.get(i)))}@if(i < mx.size() - 1), @endif@endfor</span>
										</label>
									@endfor
								</div>
							@endfor
						</div>
						<div class="flex flex-wrap items-center gap-2">
							@if(!essences.isEmpty())
								<select name="essence" class="select select-sm select-bordered min-w-0 flex-1" aria-label="${ko ? "비교할 에센스" : "Essence to compare"}">
									<option value="">${ko ? "에센스 비교 안 함" : "No essence"}</option>
									@for(PoeCraft.Essence e : essences)
										<option value="${e.slug()}">${ko ? e.nameKo() : e.name()} — ${String.join(", ", ko && e.ko() != null && !e.ko().isEmpty() ? e.ko() : e.en())}</option>
									@endfor
								</select>
							@endif
							<button type="submit" class="btn btn-sm btn-primary" data-craft-calc-run>${ko ? "계산" : "Calculate"}</button>
						</div>
					</form>
					@template.poe.htmx.craftCalc(calc = null, targetLabels = java.util.List.of())
				</section>
			@endif

		</div>
	`
)
