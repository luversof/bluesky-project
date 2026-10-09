@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe2.Poe2View
@import net.luversof.web.gate.poe.PoeText

@param Poe2.GuideRares rares = null
<%-- 거래소 주소 경로의 리그(10-08 C172) --%>
@param String tradeLeague = null

<%-- 가이드 "좋은 레어로 바꾸면"(10-02 사용자 요청) — buildGuide.jte 에서 따로 불러오는 조각. 같은 기준: PoE1 가이드 "레어 목표"(2티어 중간 롤, 접두 3 · 접미 3). --%>
!{
	boolean korean = PoeText.isKorean();
	java.util.function.Function<Double, String> pct = v -> v == null ? "-" : Math.abs(v) < 0.05 ? "0" : String.format("%+.1f%%", v);
	java.util.function.Function<Double, String> tone = v -> v == null || Math.abs(v) < 0.05 ? "text-base-content/60" : v > 0 ? "text-success" : "text-error";
	Poe2.GuideRares guide = rares;
}
@if(guide == null || guide.error() != null)
	<div class="rounded-box border border-base-200 bg-base-100 p-2 text-xs text-base-content/60" data-guide-rares-error>${korean ? "레어 목표를 계산하지 못했습니다." : "Could not compute rare targets."}</div>
@elseif(guide.rareTargets() != null && !guide.rareTargets().isEmpty())
		<%-- ⑧ 레어 목표(10-02 사용자 요청 "고유만 보지 말고 레어로도") — 칸마다 지금 베이스의 좋은 레어(2티어 중간 롤, 접두 3 · 접미 3)로
		     바꾸면. 피해용 · 생존용을 따로 골라 지금 아이템 대비 증감. 고유를 낀 칸도(그 고유 대신 레어). PoE1 가이드 "레어 목표"와 같은 기준. --%>
			<div class="rounded-box border border-base-200 bg-base-100 p-2 space-y-2" data-guide-rares>
				<div class="text-xs font-semibold">
					${korean ? "좋은 레어로 바꾸면" : "If you swap to a good rare"}
					<span class="font-normal text-base-content/60">${korean ? "(같은 베이스 · 옵션 2티어 중간 롤 · 접두 3 · 접미 3 · 지금 아이템 대비)" : "(same base · tier-2 mid rolls · 3 prefixes + 3 suffixes · vs current item)"}</span>
				</div>
				<div class="grid gap-2 md:grid-cols-2">
					@for(Poe2.GuideRareTarget rt : guide.rareTargets())
						!{
							// API 가 바꿀 이유가 없는 레어를 이미 지웠다(PoE1 isUpgrade 기준, 10-02 UU) — 남은 게 있으면 바꿀 만한 칸
							boolean better = rt.dps() != null || rt.ehp() != null;
							String itemColor = "UNIQUE".equals(rt.rarity()) ? "text-[#af6025]" : "RARE".equals(rt.rarity()) ? "text-[#ffff77]" : "text-base-content/70";
						}
						<div class="rounded-lg border ${better ? "border-success/40" : "border-base-200"} p-2 text-xs" data-guide-rare="${rt.slot()}">
							<div class="flex flex-wrap items-baseline gap-1.5">
								<span class="font-semibold">${Poe2View.slotName(rt.slot())}</span>
								<span class="${itemColor}">${korean && rt.itemKo() != null ? rt.itemKo() : rt.item()}</span>
								<span class="text-base-content/60">· ${rt.baseKo() != null && korean ? rt.baseKo() : rt.base()}</span>
								@if(!better)
									<span class="ml-auto text-base-content/60" data-rare-keep>${korean ? "지금 아이템이 더 좋음" : "current item is better"}</span>
								@endif
							</div>
							@for(int f = 0; f < 2; f++)
								!{
									Poe2.GuideRare r = f == 0 ? rt.dps() : rt.ehp();
									String focus = f == 0 ? (korean ? "피해용 레어" : "DPS rare") : (korean ? "생존용 레어" : "EHP rare");
								}
								@if(r != null)
									<details class="mt-1" data-rare-focus="${f == 0 ? "dps" : "ehp"}">
										<summary class="cursor-pointer">
											<span class="text-base-content/70">${focus}</span>
											<span class="font-mono ${tone.apply(r.dps())}">DPS ${pct.apply(r.dps())}</span>
											<span class="font-mono ${tone.apply(r.ehp())}">EHP ${pct.apply(r.ehp())}</span>
										</summary>
										<div class="poe-tooltip poe-rar-rare mt-1 px-3 py-1.5 space-y-0.5">
											@for(String line : korean && r.linesKo() != null && !r.linesKo().isEmpty() ? r.linesKo() : r.lines())
												<div class="text-[12px] leading-snug text-[#8888ff] text-center">$unsafe{PoeText.highlightValues(line)}</div>
											@endfor
										</div>
										<%-- 거래소(10-08 C172, PoE1 가이드 레어 목표 짝) — 분류 + 옵션, 옵션 5개 이상이면 2개는 빠져도 --%>
										@if(PoeText.trade2Url(tradeLeague, r.tradeQuery()) != null)
											<div class="mt-1"><a class="badge badge-xs badge-outline badge-info" target="_blank" rel="noopener" href="${PoeText.trade2Url(tradeLeague, r.tradeQuery())}" data-guide-trade title="${korean ? "분류 + 옵션(이 수치의 85% 이상)으로 거래소 검색(즉시 구입 · 가격순)" : "Search trade — item class + mods at 85%+"}">${korean ? "거래소" : "Trade"}</a></div>
										@endif
										@if(r.itemText() != null)
											<%-- PoB 아이템 텍스트 복사(10-02) — 엔진이 잰 그 아이템. PoB "Create custom" 에 붙여 넣는다(common.ts [data-copy-target]) --%>
											!{ String copyId = "poe2RareText-" + rt.slot().replaceAll("[^A-Za-z0-9]", "") + "-" + f; }
											<div class="mt-1 flex items-center gap-2">
												<textarea id="${copyId}" class="sr-only" readonly tabindex="-1" aria-hidden="true" data-rare-item-text>${r.itemText()}</textarea>
												<button type="button" class="btn btn-ghost btn-xs" data-copy-target="#${copyId}" data-copied-label="${korean ? "복사됨 ✓" : "Copied ✓"}">${korean ? "아이템 텍스트 복사" : "Copy item text"}</button>
												<span class="text-base-content/60">${korean ? "PoB 아이템 탭 “Create custom” 에 붙여 넣기" : "paste into PoB Items → Create custom"}</span>
											</div>
										@endif
									</details>
								@endif
							@endfor
						</div>
					@endfor
				</div>
			</div>
		@endif

