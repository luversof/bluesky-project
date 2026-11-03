@import net.luversof.web.gate.bookkeeping.constant.CategoryKind
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page

<%-- 분류: 수입 · 지출마다 대분류와 그 소분류. 이름 · 숨김은 ?editCategory= 로 그 줄을 폼으로 그린다. 쓰인 분류는 지우면 숨겨진다. --%>
!{ String base = "/bookkeeping/" + page.book().id() + "/settings"; }
<section aria-labelledby="bk-set-categories" class="mb-6">
	<h2 id="bk-set-categories" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.settings.categories")}</h2>
	<div class="grid gap-4 md:grid-cols-2">
		@for(var kind : CategoryKind.values())
			<div class="rounded-box border border-base-200 bg-base-100 p-3">
				<h3 class="text-sm font-semibold mb-2">${BookkeepingView.msg("bookkeeping.kind." + kind.name())}</h3>
				<ul class="divide-y divide-base-200">
					@for(var c : page.categories())
						@if(c.kind() == kind && c.parentId() == null)
							@template.bookkeeping.fragments.settingsCategoryRow(page = page, c = c, child = false)
							@for(var child : page.categories())
								@if(c.id().equals(child.parentId()))
									@template.bookkeeping.fragments.settingsCategoryRow(page = page, c = child, child = true)
								@endif
							@endfor
						@endif
					@endfor
				</ul>
				<form method="post" action="${base}/categories" class="mt-3 flex flex-wrap items-end gap-2">
					<input type="hidden" name="kind" value="${kind.name()}" />
					<label class="form-control">
						<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.category.parent")}</span>
						<select name="parentId" class="select select-sm select-bordered">
							<option value="">${BookkeepingView.msg("bookkeeping.settings.category.top")}</option>
							@for(var c : page.categories())
								@if(c.kind() == kind && c.parentId() == null && !c.hidden())
									<option value="${c.id().toString()}">${c.name()}</option>
								@endif
							@endfor
						</select>
					</label>
					<label class="form-control">
						<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.category.name")}</span>
						<input type="text" name="name" required maxlength="100" class="input input-sm input-bordered w-36" />
					</label>
					<button type="submit" class="btn btn-sm">${BookkeepingView.msg("bookkeeping.settings.add")}</button>
				</form>
			</div>
		@endfor
	</div>
</section>
