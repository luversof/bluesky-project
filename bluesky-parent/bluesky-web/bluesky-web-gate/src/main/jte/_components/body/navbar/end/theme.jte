<div title="Change Theme" class="dropdown dropdown-end">
	<div tabindex="0" class="btn btn-ghost">
		<span>Theme</span>
		<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-3 h-3">
			<path stroke-linecap="round" stroke-linejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
		</svg>
	</div>
	<div tabindex="0" class="dropdown-content z-[1] bg-base-200 top-px h-[28.6rem] max-h-[calc(100vh-10rem)] w-56 overflow-y-auto mt-16">
		<div class="grid gap-3 p-3">
			@for(String theme : net.luversof.web.gate.util.TemplateUtil.getThemeList())
			<button class="text-start" data-set-theme="${theme}" data-act-class="[&_svg]:visible">
				<span data-theme="${theme}" class="rounded-btn block w-full font-sans">
					<span class="grid">
						<span class="flex gap-2 px-4 py-3">
							<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor" class="invisible h-3 w-3 shrink-0">
								<path d="M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z" />
							</svg>
							<span class="flex-grow text-sm">${theme}</span>
							<span class="flex h-full gap-1">
								<span class="bg-primary rounded-badge w-2"></span>
								<span class="bg-secondary rounded-badge w-2"></span>
								<span class="bg-accent rounded-badge w-2"></span>
								<span class="bg-neutral rounded-badge w-2"></span>
							</span>
						</span>
					</span>
				</span>
			</button>
			@endfor
		</div>
	</div>
</div>