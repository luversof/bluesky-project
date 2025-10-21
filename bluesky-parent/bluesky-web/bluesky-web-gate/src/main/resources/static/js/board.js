
const boardAction = (() => {
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

const boardList = (() => {
	return {
		addEventListener() {
			document.addEventListener("listHtmxResponseTrigger", event => {
				for (const el of event.target.querySelectorAll(".navButton")) {
					el.addEventListener("click", event => {
						param.setParam("page", event.target.dataset.page);
						htmx.trigger("#boardList", "listHtmxTrigger");
					});
				}
				
				for (const el of event.target.querySelectorAll("[data-date]")) {
					el.textContent = dayjs().to(el.dataset.date);
				}
				
				for (const el of event.target.querySelectorAll(".writeButton")) {
					el.addEventListener("click", () => boardAction.moveToWrite());
				}
				
				for (const el of event.target.querySelectorAll("table tr[data-boardArticleId]")) {
					el.addEventListener("click", event => {
						let boardArticleId = event.target.closest("tr").dataset.boardarticleid;
						boardAction.moveToView(boardArticleId);
					});
				}
			});
		}
	}	
})();

const boardView = (() => {
	return {
		addEventListener() {
			for (const el of document.querySelectorAll(".writeButton")) {
				el.addEventListener("click", () => boardAction.moveToWrite());
			}
			for (const el of document.querySelectorAll(".listButton")) {
				el.addEventListener("click", () => boardAction.moveToList());
			}
		}
	}
})();

const boardWrite = (() => {
	return {
		addEventListener() {
			for (const el of document.querySelectorAll(".cancelButton")) {
				el.addEventListener("click", () => boardAction.moveToList());
			}
			for (const el of document.querySelectorAll(".writeButton")) {
				el.addEventListener("click", () => this.writeAndMoveToView());
			}
		},
		writeAndMoveToView() {
			alert("글쓰기");
			
		}
	}
})();


document.addEventListener("DOMContentLoaded", () => {
	if (boardMode == "list") {
		console.log("TEST")
		boardList.addEventListener();
	}
	if (boardMode == "write") {
		boardWrite.addEventListener();
	}
});