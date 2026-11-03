@import java.util.List
@import net.luversof.web.gate.bookkeeping.constant.AssetKind
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page
@param List<String> currencies

<%-- 자산: 추가 · 수정(?editAsset=) · 숨김 · 순서(같은 종류 안) · 삭제(내역 있으면 숨김). 결제일은 신용카드만 쓴다. --%>
!{ String base = "/bookkeeping/" + page.book().id() + "/settings/assets"; }
<section aria-labelledby="bk-set-assets" class="mb-6">
	<h2 id="bk-set-assets" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.settings.assets")}</h2>
	<ul class="divide-y divide-base-200 rounded-box border border-base-200 bg-base-100">
		@for(var kind : AssetKind.values())
			@for(var ab : page.assets())
				!{ var a = ab.asset(); }
				@if(a.kind() == kind)
					<li class="px-3 py-2">
						@if(a.id().equals(page.editAsset()))
							@template.bookkeeping.fragments.settingsAssetForm(action = base + "/" + a.id(), currencies = currencies, kind = a.kind(), name = a.name(), currency = a.currency(), openingBalance = BookkeepingView.plainAmount(a.openingBalance()), paymentDay = a.paymentDay(), hidden = a.hidden(), editing = true, cancelHref = "/bookkeeping/" + page.book().id() + "/settings")
						@else
							<div class="flex flex-wrap items-center gap-2">
								<span class="min-w-0 flex-1 text-sm wrap-anywhere ${a.hidden() ? "text-base-content/50" : ""}">
									<span class="text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.assetKind." + a.kind().name())}</span>
									${a.name()} (${a.currency()})
									@if(a.hidden())<span class="badge badge-sm badge-ghost ml-1">${BookkeepingView.msg("bookkeeping.settings.hidden")}</span>@endif
								</span>
								<form method="post" action="${base}/${a.id().toString()}/move" class="contents">
									<input type="hidden" name="direction" value="up" />
									<button type="submit" class="btn btn-xs btn-ghost" aria-label="${BookkeepingView.msg("bookkeeping.settings.up", a.name())}">↑</button>
								</form>
								<form method="post" action="${base}/${a.id().toString()}/move" class="contents">
									<input type="hidden" name="direction" value="down" />
									<button type="submit" class="btn btn-xs btn-ghost" aria-label="${BookkeepingView.msg("bookkeeping.settings.down", a.name())}">↓</button>
								</form>
								<a href="/bookkeeping/${page.book().id().toString()}/settings?editAsset=${a.id().toString()}" class="btn btn-xs btn-ghost">${BookkeepingView.msg("bookkeeping.settings.edit")}</a>
								<form method="post" action="${base}/${a.id().toString()}/delete" class="contents">
									<button type="submit" class="btn btn-xs btn-ghost text-error">${BookkeepingView.msg("bookkeeping.settings.delete")}</button>
								</form>
							</div>
						@endif
					</li>
				@endif
			@endfor
		@endfor
	</ul>
	<div class="mt-3 rounded-box border border-base-200 bg-base-100 p-3">
		<h3 class="text-sm font-semibold mb-2">${BookkeepingView.msg("bookkeeping.settings.asset.add")}</h3>
		@template.bookkeeping.fragments.settingsAssetForm(action = base, currencies = currencies, kind = AssetKind.BANK, name = "", currency = page.book().baseCurrency(), openingBalance = "", paymentDay = null, hidden = false, editing = false, cancelHref = "")
	</div>
</section>
