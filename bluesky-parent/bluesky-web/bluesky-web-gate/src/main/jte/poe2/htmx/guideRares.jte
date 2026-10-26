@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.poe2.dto.Poe2
@import net.luversof.web.gate.poe2.Poe2View
@import net.luversof.web.gate.poe.PoeText

@param Poe2.GuideRares rares = null
<%-- 거래소 주소 경로의 리그(10-08 C172) --%>
@param String tradeLeague = null
<%-- 칸 → 지금 아이템(C310 — 한국어 이름 + 빌드 카드와 같은 툴팁 레이어). 컨트롤러가 같은 코드로 빌드를 읽어 넘긴다 --%>
@param java.util.Map<String, Poe2.BuildItem> slotItems = java.util.Map.of()

<%-- 가이드 "좋은 레어로 바꾸면"(10-02 사용자 요청) — buildGuide.jte 에서 따로 불러오는 조각. 같은 기준: PoE1 가이드 "레어 목표"(2등급 중간 롤, 접두 3 · 접미 3). --%>
!{
	boolean korean = PoeText.isKorean();
	java.util.function.Function<Double, String> pct = v -> v == null ? "-" : Math.abs(v) < 0.05 ? "0" : String.format("%+.1f%%", v);
	java.util.function.Function<Double, String> tone = v -> v == null || Math.abs(v) < 0.05 ? "text-base-content/60" : v > 0 ? "text-success" : "text-error";
	Poe2.GuideRares guide = rares;
}
@if(guide == null || guide.error() != null)
	<div class="rounded-box border border-base-200 bg-base-100 p-2 text-xs text-base-content/60" data-guide-rares-error>${korean ? "레어 목표를 계산하지 못했습니다." : "Could not compute rare targets."}</div>
@elseif(guide.rareTargets() != null && !guide.rareTargets().isEmpty())
		<%-- ⑧ 레어 목표(10-02 사용자 요청 "고유만 보지 말고 레어로도") — 칸마다 지금 베이스의 좋은 레어(2등급 중간 롤, 접두 3 · 접미 3)로
		     바꾸면. 피해용 · 생존용을 따로 골라 지금 아이템 대비 증감. 고유를 낀 칸도(그 고유 대신 레어). PoE1 가이드 "레어 목표"와 같은 기준. --%>
			<div class="rounded-box border border-base-200 bg-base-100 p-2 space-y-2" data-guide-rares>
				<div class="text-xs font-semibold">
					${korean ? "좋은 레어로 바꾸면" : "If you swap to a good rare"}
					<span class="font-normal text-base-content/60">${korean ? "(같은 베이스 · 옵션 2등급 중간 롤 · 접두 3 · 접미 3 · 지금 아이템 대비)" : "(same base · tier-2 mid rolls · 3 prefixes + 3 suffixes · vs current item)"}</span>
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
								!{ Poe2.BuildItem cur = slotItems == null ? null : slotItems.get(rt.slot()); }
								@if(cur != null && cur.mods() != null && !cur.mods().isEmpty())
									<span class="poe-hover ${itemColor} underline decoration-dotted underline-offset-2" tabindex="0" data-rare-current>${PoeText.name(cur.nameKo(), cur.name())}<span data-poe-tip class="hidden">@template.poe2.htmx.buildItemTooltip(item = cur)</span></span>
								@else
									<span class="${itemColor}">${korean && rt.itemKo() != null ? rt.itemKo() : cur != null ? PoeText.name(cur.nameKo(), cur.name()) : rt.item()}</span>
								@endif
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
											<%-- 올리면 인게임 레어 툴팁 레이어(10-14 C310 사용자 요청 "레이어 처리가 안 되어 있다" — 빌드 카드 아이템과 같은 [data-poe-tip] 인라인 방식, common.ts .poe-hover).
											     이름 띠 = 피해용/생존용 레어 + 베이스(인게임 레어 이름 두 줄 자리), 아래 옵션(2등급 중간 롤) --%>
											!{ java.util.List<String> tipLines = korean && r.linesKo() != null && !r.linesKo().isEmpty() ? r.linesKo() : r.lines(); String tipBase = rt.baseKo() != null && korean ? rt.baseKo() : rt.base(); }
											<span class="poe-hover inline-flex flex-wrap items-baseline gap-1.5" tabindex="0" data-rare-hover>
												<span class="text-base-content/70 underline decoration-dotted underline-offset-2">${focus}</span>
												<span class="font-mono ${tone.apply(r.dps())}">DPS ${pct.apply(r.dps())}</span>
												<span class="font-mono ${tone.apply(r.ehp())}">EHP ${pct.apply(r.ehp())}</span>
												<span data-poe-tip class="hidden">
													<div class="poe-tooltip poe-rar-rare w-80 max-w-[calc(100vw-2rem)] max-h-[80vh] overflow-y-auto shadow-2xl" style="border-color:#ffff77" data-rare-tip>
														<div class="poe-tt-band poe-tt-band-2">
															<div class="text-center font-bold text-[15px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:#ffff77">${focus}</div>
															<div class="text-center text-[13px] leading-tight drop-shadow-[0_1px_2px_rgba(0,0,0,0.9)]" style="color:#ffff77">${tipBase}</div>
														</div>
														@if(tipLines != null && !tipLines.isEmpty())
															<div class="poe-tt-sepimg"></div>
															<div class="px-4 py-2 text-center space-y-0.5">
																@for(String line : tipLines)
																	<div class="text-[12px] leading-snug text-[#8888ff] whitespace-pre-line">$unsafe{PoeText.highlightValues(line)}</div>
																@endfor
															</div>
														@endif
													</div>
												</span>
											</span>
										</summary>
										<div class="poe-tooltip poe-rar-rare mt-1 px-3 py-1.5 space-y-0.5">
											@for(String line : korean && r.linesKo() != null && !r.linesKo().isEmpty() ? r.linesKo() : r.lines())
												<div class="text-[12px] leading-snug text-[#8888ff] text-center">$unsafe{PoeText.highlightValues(line)}</div>
											@endfor
										</div>
										<%-- 거래소(10-08 C172, PoE1 가이드 레어 목표 짝) — 분류 + 옵션, 옵션 5개 이상이면 2개는 빠져도 --%>
										@if(PoeText.trade2Url(tradeLeague, r.tradeQuery()) != null)
											<div class="mt-1"><a class="badge badge-xs badge-outline badge-info" target="_blank" rel="noopener" href="${PoeText.trade2Url(tradeLeague, r.tradeQuery())}" data-guide-trade title="${korean ? "분류 + 옵션(이 수치의 85% 이상)으로 거래소 검색(즉시 구입 · 가격순)" : "Search trade — item class + mods at 85%+"}">${MessageUtil.getMessage("poe.common.trade")}</a></div>
										@endif
										@if(r.itemText() != null)
											<%-- PoB 아이템 텍스트 복사(10-02) — 엔진이 잰 그 아이템. PoB "Create custom" 에 붙여 넣는다(common.ts [data-copy-target]) --%>
											!{ String copyId = "poe2RareText-" + rt.slot().replaceAll("[^A-Za-z0-9]", "") + "-" + f; }
											<div class="mt-1 flex items-center gap-2">
												<textarea id="${copyId}" class="sr-only" readonly tabindex="-1" aria-hidden="true" data-rare-item-text>${r.itemText()}</textarea>
												<button type="button" class="btn btn-ghost btn-xs" data-copy-target="#${copyId}" data-copied-label="${MessageUtil.getMessage("poe.common.copied")}">${MessageUtil.getMessage("poe.build.guide.copyitem")}</button>
												<span class="text-base-content/60">${MessageUtil.getMessage("poe.build.guide.copyitem.hint")}</span>
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

