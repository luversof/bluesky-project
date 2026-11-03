@import java.util.List
@import net.luversof.web.gate.bookkeeping.constant.AssetKind
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@param String action
@param List<String> currencies
@param AssetKind kind
@param String name
@param String currency
@param String openingBalance
@param Integer paymentDay
@param boolean hidden
@param boolean editing
@param String cancelHref

<%-- 자산 추가 · 수정 폼. 결제일은 신용카드가 아니면 API 가 무시한다. 내역이 있는 자산은 통화를 바꿀 수 없다(API 가 거절). --%>
<form method="post" action="${action}" class="grid gap-2 sm:grid-cols-3 items-end">
	<label class="form-control w-full">
		<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.asset.kind")}</span>
		<select name="kind" class="select select-sm select-bordered w-full">
			@for(var k : AssetKind.values())
				<option value="${k.name()}" selected="${k == kind}">${BookkeepingView.msg("bookkeeping.assetKind." + k.name())}</option>
			@endfor
		</select>
	</label>
	<label class="form-control w-full">
		<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.asset.name")}</span>
		<input type="text" name="name" value="${name}" required maxlength="100" class="input input-sm input-bordered w-full" />
	</label>
	<label class="form-control w-full">
		<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.asset.currency")}</span>
		<select name="currency" class="select select-sm select-bordered w-full">
			@for(var c : currencies)
				<option value="${c}" selected="${c.equals(currency)}">${c}</option>
			@endfor
		</select>
	</label>
	<label class="form-control w-full">
		<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.asset.openingBalance")}</span>
		<input type="text" name="openingBalance" value="${openingBalance}" inputmode="decimal" autocomplete="off" class="input input-sm input-bordered w-full" />
	</label>
	<label class="form-control w-full">
		<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.asset.paymentDay")}</span>
		<input type="number" name="paymentDay" value="${paymentDay == null ? "" : paymentDay.toString()}" min="1" max="31" class="input input-sm input-bordered w-full" />
	</label>
	<div class="flex flex-wrap items-center gap-2">
		@if(editing)
			<label class="flex items-center gap-1 text-xs">
				<input type="checkbox" name="hidden" value="1" checked="${hidden}" class="checkbox checkbox-sm" />
				${BookkeepingView.msg("bookkeeping.settings.hidden")}
			</label>
		@endif
		<button type="submit" class="btn btn-sm btn-primary">${BookkeepingView.msg(editing ? "bookkeeping.settings.save" : "bookkeeping.settings.add")}</button>
		@if(editing)
			<a href="${cancelHref}" class="btn btn-sm btn-ghost">${BookkeepingView.msg("bookkeeping.settings.cancel")}</a>
		@endif
	</div>
</form>
