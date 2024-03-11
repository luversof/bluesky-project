// url query parameter 처리
var param = (() => {
	var _params = new URLSearchParams(window.location.search);

	return {
		refreshUrl() {
			window.history.replaceState(null, null, "?" + _params.toString());
		},
		getParams() {
			return _params;
		},
		getParam(paramKey) {
			if (_params.get(paramKey) == '') _params.delete(paramKey)
			return _params.get(paramKey) == null ? null : _params.get(paramKey);
		},
		setParam(paramKey, paramValue) {
			if (paramValue == null || paramValue == '') {
				_params.delete(paramKey);
			} else {
				_params.set(paramKey, paramValue);
			}
			this.refreshUrl();
		},
		resetParam() {
			_params = new URLSearchParams();
			this.refreshUrl();
		},
		getRequestPage() {
			var page = this.getParam("page");
			return (page == null ? 1 : page) - 1;
		}
	}
})();

var boardArticlePage = (() => {
	return {
		addEventListener() {
			document.addEventListener("boardArticlePageResponseTrigger", (event) => {
				event.target.querySelectorAll(".navButton").forEach(el => el.addEventListener("click", (event) => {
					param.setParam("page", event.target.dataset.page);
					htmx.trigger("#boardList", "boardArticlePageTrigger");
				}));
				
			});
		}		
	}	
})();

boardArticlePage.addEventListener();