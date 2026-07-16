@import io.github.luversof.boot.context.support.MessageUtil

@param boolean isAuthenticated = false
@param String username = ""
@param String loginUrl = "https://user.web.bluesky.local/login/redirect"
@param String profile = "local"
@param String patch = ""
@param boolean hasTreeData = false

!{
	String pageTitle = MessageUtil.getMessage("poe.tree.title");
	String patchLabel = java.text.MessageFormat.format(MessageUtil.getMessage("poe.gems.patch"), patch);
	String hintLabel = MessageUtil.getMessage("poe.tree.hint");
	String legendKeystoneLabel = MessageUtil.getMessage("poe.tree.legend.keystone");
	String legendNotableLabel = MessageUtil.getMessage("poe.tree.legend.notable");
	String legendJewelLabel = MessageUtil.getMessage("poe.tree.legend.jewel");
	String legendMasteryLabel = MessageUtil.getMessage("poe.tree.legend.mastery");
}

@template._layout.poeLayout(
	title = pageTitle,
	isAuthenticated = isAuthenticated,
	username = username,
	loginUrl = loginUrl,
	profile = profile,
	contentPoe = @`
		<div class="space-y-4">
			<div class="flex flex-wrap items-baseline gap-3">
				<h1 class="text-2xl font-bold">${pageTitle}</h1>
				<span class="badge badge-ghost">${patchLabel}</span>
			</div>


			@if(!hasTreeData)
				@template._components.ui.emptyState(message = MessageUtil.getMessage("poe.gems.no.data"), hint = "tools/poe-extract: node parse-tree.mjs")
			@else
				<div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-base-content/50">
					<span>${hintLabel}</span>
					<span class="ml-auto flex items-center gap-3">
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#cf6642"></span> ${legendKeystoneLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#d4a94e"></span> ${legendNotableLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#4ec9d4"></span> ${legendJewelLabel}</span>
						<span><span class="inline-block w-2.5 h-2.5 rounded-full align-[-1px]" style="background:#9a6ad4"></span> ${legendMasteryLabel}</span>
					</span>
				</div>
				<div class="relative">
					<canvas id="poeTreeCanvas" class="w-full h-[72vh] rounded-2xl shadow-sm cursor-grab" style="background:#0e1116" data-locale="${net.luversof.web.gate.poe.PoeText.isKorean() ? "ko" : "en"}"></canvas>
					<div id="poeTreeTooltip" class="pointer-events-none absolute hidden z-10 max-w-xs rounded-box bg-neutral text-neutral-content shadow-xl border border-neutral-content/15 px-3 py-2"></div>
				</div>
				<script src="/js/poe/tree.js"></script>
			@endif
		</div>
	`
)
