console.debug("[multiSelectInit] simple multi-select initializer loaded");function ensureMultiSelectStyle(){if(document.getElementById("simple-multi-style"))return;const s=document.createElement("style");s.id="simple-multi-style",s.appendChild(document.createTextNode(`
/* Remove fixed DaisyUI sizing without overriding native multi-select row height */
.select[multiple], select.select[multiple], .form-control select[multiple] {
	min-height: unset !important;
	max-height: 50vh !important;
	overflow: auto !important;
	/* daisyUI v5 .select \uB294 display:inline-flex \uB77C\uC11C multiple \uC140\uB809\uD2B8\uC758 option \uB4E4\uC774
	   flex \uC544\uC774\uD15C\uC73C\uB85C 1\uAE00\uC790 \uD3ED\uAE4C\uC9C0 \uC904\uC5B4 \uC138\uB85C\uB85C \uC313\uC778\uB2E4. \uB124\uC774\uD2F0\uBE0C listbox(block)\uB85C \uB418\uB3CC\uB9B0\uB2E4. */
	display: block !important;
}
/* Also handle cases where select has size-specific small class */
select.select.select-sm[multiple], .select.select-sm[multiple] {
	min-height: unset !important;
}
/* Make native multi-selects visually match an input box (\uD14C\uB9C8 \uBCC0\uC218 \uC0AC\uC6A9 \u2192 light/dark \uBAA8\uB450 \uB300\uC751) */
select[multiple], select.select[multiple], select.select-bordered[multiple] {
	background-color: var(--color-base-100) !important;
	color: var(--color-base-content) !important;
	border: 1px solid var(--color-base-300) !important;
	border-radius: var(--radius-field, 0.375rem) !important;
	padding: 0.25rem 0.4rem !important;
	box-shadow: none !important;
}
select[multiple] option {
	background-color: var(--color-base-100);
	color: var(--color-base-content);
}
select[multiple]:focus, select.select[multiple]:focus, select.select-bordered[multiple]:focus {
	border-color: var(--color-primary) !important;
	outline: 2px solid var(--color-primary) !important;
	outline-offset: 0 !important;
	box-shadow: none !important;
}
`)),(document.head||document.documentElement).appendChild(s)}(()=>{const SELECTOR='select[multiple], select[data-max-visible], select[name="accountIdList"], select[name="stockItemIdList"], select[name="stockTagList"]',ALL_OPTION_SELECTORS='select[name="accountIdList"], select[name="stockItemIdList"]',selectionSnapshots=new WeakMap;function isAllOptionSelect(sel){return sel.matches(ALL_OPTION_SELECTORS)}function getSelectedValues(sel){return Array.from(sel.selectedOptions).map(option=>option.value)}function rememberSelectionSnapshot(sel){selectionSnapshots.set(sel,getSelectedValues(sel))}function enforceExclusiveAllOption(sel){var _a;if(!sel.multiple||!isAllOptionSelect(sel)){rememberSelectionSnapshot(sel);return}const allOption=Array.from(sel.options).find(option=>option.value==="");if(!allOption){rememberSelectionSnapshot(sel);return}const selectedOptions=Array.from(sel.selectedOptions),selectedSpecificOptions=selectedOptions.filter(option=>option.value!=="");if(selectedOptions.some(option=>option.value==="")&&selectedSpecificOptions.length>0){const previousSelection=(_a=selectionSnapshots.get(sel))!==null&&_a!==void 0?_a:[];previousSelection.length===1&&previousSelection[0]===""?allOption.selected=!1:selectedSpecificOptions.forEach(option=>{option.selected=!1})}rememberSelectionSnapshot(sel)}function syncLinkedSelectHeights(scope){Array.from((scope instanceof Element?scope:document).querySelectorAll("form")).forEach(form=>{const accountSelect=form.querySelector('select[name="accountIdList"]'),stockSelect=form.querySelector('select[name="stockItemIdList"]');if(!accountSelect||!stockSelect)return;const accountHeight=accountSelect.getBoundingClientRect().height;!accountHeight||accountHeight<=0||(stockSelect.style.height=`${accountHeight}px`,stockSelect.style.maxHeight=`${accountHeight}px`,stockSelect.style.overflowY="auto")})}function applySize(sel){try{if(sel.dataset.noMulti==="1"||sel.hasAttribute("data-no-multi"))return;sel.multiple=!0;const opts=Array.from(sel.options),attr=sel.getAttribute("data-max-visible");let maxVisible=null;if(attr){const n=parseInt(attr,10);!isNaN(n)&&n>0&&(maxVisible=n)}const desired=opts.length,size=Math.min(desired,maxVisible!=null?maxVisible:desired,50);sel.size=Math.max(1,size),sel.dataset.simpleMultiInit="1"}catch(e){console.warn("[multiSelectInit] applySize error",e)}}function init(root=document){const scope=root instanceof Element?root:document;ensureMultiSelectStyle(),Array.from(scope.querySelectorAll(SELECTOR)).forEach(sel=>{try{if(applySize(sel),enforceExclusiveAllOption(sel),!sel._simpleMultiObserver){const mo=new MutationObserver(()=>applySize(sel));mo.observe(sel,{childList:!0,subtree:!0}),sel._simpleMultiObserver=mo}}catch(e){}}),syncLinkedSelectHeights(scope)}document.addEventListener("DOMContentLoaded",()=>init(document)),document.addEventListener("change",event=>{const target=event.target;target instanceof HTMLSelectElement&&isAllOptionSelect(target)&&enforceExclusiveAllOption(target)}),document.addEventListener("htmx:afterSwap",evt=>{try{const target=evt&&evt.detail&&evt.detail.target?evt.detail.target:document;init(target instanceof Element?target:document)}catch(e){}});try{new MutationObserver(mutations=>{for(const m of mutations)for(const n of Array.from(m.addedNodes))if(n instanceof Element)try{if(n.matches&&n.matches(SELECTOR))init(n);else{const found=n.querySelectorAll?n.querySelectorAll(SELECTOR):[];found&&found.length&&init(n)}}catch(e){}}).observe(document.documentElement||document.body,{childList:!0,subtree:!0})}catch(e){}})();
