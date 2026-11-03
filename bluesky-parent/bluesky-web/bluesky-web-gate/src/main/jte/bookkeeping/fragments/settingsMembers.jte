@import net.luversof.web.gate.bookkeeping.constant.BookRole
@import net.luversof.web.gate.bookkeeping.support.BookkeepingView
@import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage
@param SettingsPage page

<%-- 멤버 · 초대. 초대 링크는 1회용 · 7일 만료이고, 만든 직후 한 번만 보인다. OWNER 는 나갈 수 없다(가계부 삭제만). --%>
!{ String base = "/bookkeeping/" + page.book().id() + "/settings"; }
<section aria-labelledby="bk-set-members" class="mb-6">
	<h2 id="bk-set-members" class="text-base font-semibold mb-2">${BookkeepingView.msg("bookkeeping.settings.members")}</h2>
	<ul class="divide-y divide-base-200 rounded-box border border-base-200 bg-base-100">
		@for(var m : page.members())
			<li class="flex flex-wrap items-center gap-2 px-3 py-2">
				<span class="min-w-0 flex-1 text-sm wrap-anywhere">${m.name()}
					@if(m.me())<span class="text-xs text-base-content/60">${BookkeepingView.msg("bookkeeping.settings.member.me")}</span>@endif
				</span>
				<span class="badge badge-sm">${BookkeepingView.msg("bookkeeping.role." + m.role().name())}</span>
				@if(m.role() != BookRole.OWNER && (m.me() || page.owner()))
					<form method="post" action="${base}/members/${m.userId().toString()}/remove" class="contents">
						<button type="submit" class="btn btn-xs btn-ghost text-error">${BookkeepingView.msg(m.me() ? "bookkeeping.settings.member.leave" : "bookkeeping.settings.member.remove")}</button>
					</form>
				@endif
			</li>
		@endfor
	</ul>
	@if(page.owner())
		<div class="mt-3 rounded-box border border-base-200 bg-base-100 p-3">
			<form method="post" action="${base}/invites">
				<button type="submit" class="btn btn-sm">${BookkeepingView.msg("bookkeeping.settings.invite.create")}</button>
			</form>
			@if(page.inviteUrl() != null)
				<label class="form-control mt-3">
					<span class="label-text text-xs mb-1">${BookkeepingView.msg("bookkeeping.settings.invite.link")}</span>
					<input type="text" readonly value="${page.inviteUrl()}" class="input input-sm input-bordered w-full font-mono" />
				</label>
			@endif
			<p class="text-xs text-base-content/60 mt-2">${BookkeepingView.msg("bookkeeping.settings.invite.hint")}</p>
		</div>
	@endif
</section>
