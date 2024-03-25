
var boardAction = (() => {
	return {
		moveToList() {
			param.deleteParam("boardArticleId");
			location.href = "list" + (param.getParams().size > 0 ? "?" + param.getParams().toString() : "");
		},
		moveToWrite() {
			location.href = "write" + (param.getParams().size > 0 ? "?" + param.getParams().toString() : "");
		},
		moveToView(boardArticleId) {
			param.setParam("boardArticleId", boardArticleId)
			location.href = "view" + (param.getParams().size > 0 ? "?" + param.getParams().toString() : "");
		}
	}
})();

var boardList = (() => {
	return {
		addEventListener() {
			document.addEventListener("listFragmentResponseTrigger", (event) => {
				event.target.querySelectorAll(".navButton").forEach(el => el.addEventListener("click", (event) => {
					param.setParam("page", event.target.dataset.page);
					htmx.trigger("#boardList", "listFragmentTrigger");
				}));
				
				event.target.querySelectorAll("[data-date]").forEach(el => el.textContent = dayjs().to(el.dataset.date));
				
				event.target.querySelectorAll(".writeButton").forEach(el => el.addEventListener("click", () => boardAction.moveToWrite()));
				
				event.target.querySelectorAll("table tr[data-boardArticleId]").forEach(el => el.addEventListener("click", (event) => {
					var boardArticleId = event.target.closest("tr").dataset.boardarticleid;
					boardAction.moveToView(boardArticleId);
				}));
			});
		}
	}	
})();

var boardView = (() => {
	return {
		addEventListener() {
			document.querySelectorAll(".writeButton").forEach(el => el.addEventListener("click", () => boardAction.moveToWrite()));
			document.querySelectorAll(".listButton").forEach(el => el.addEventListener("click", () => boardAction.moveToList()));
		}
	}
})();

var boardWrite = (() => {
	return {
		addEventListener() {
			document.querySelectorAll(".cancelButton").forEach(el => el.addEventListener("click", () => boardAction.moveToList()));
			document.querySelectorAll(".writeButton").forEach(el => el.addEventListener("click", () => this.writeAndMoveToView()));
		},
		writeAndMoveToView() {
			alert("글쓰기");
			
		}
	}
})();


document.addEventListener("DOMContentLoaded", () => {
	if (boardMode == "list") {
		boardList.addEventListener();
	}
	if (boardMode == "write") {
		boardWrite.addEventListener();
	}
});