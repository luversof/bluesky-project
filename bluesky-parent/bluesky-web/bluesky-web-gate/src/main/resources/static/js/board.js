const boardData = (() => {
	let boardAlias;
	let boardMode;
	let boardId;
	return {
		setBoardAlias(alias) {
			boardAlias = alias;
		},
		getBoardAlias() {
			return boardAlias;
		},
		setBoardMode(mode) {
			boardMode = mode;
		},
		getBoardMode() {
			return boardMode;
		},
		setBoardId(id) {
			boardId = id;
		},
		getBoardId() {
			return boardId;
		}
	};

})();


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

var a;

const boardWrite = (() => {
	let easymde;
	return {
		loadEasyMDE() {
			easymde = new EasyMDE({
				element: document.getElementById('content'),
			});
		},
		addEventListener() {
			for (const el of document.querySelectorAll(".cancelButton")) {
				el.addEventListener("click", () => boardAction.moveToList());
			}
			for (const el of document.querySelectorAll(".writeButton")) {
				el.addEventListener("click", () => this.writeAndMoveToView());
			}
		},
		
		writeAndMoveToView() {
			console.log("title:", document.getElementById("title").value);
			console.log("content:", easymde.value());
			
			fetch("/api/boardArticle", {
				method: "POST",
				headers: {
					"Content-Type": "application/json"
				},
				body: JSON.stringify({
					boardId: boardData.getBoardId(),
					title: document.getElementById("title").value,
					content: easymde.value()
				})
			})
			.then(response => errorHandler.handle(response))
			;
			
		}
	}
})();


document.addEventListener("DOMContentLoaded", () => {
	if (boardData.getBoardMode() == "list") {
		boardList.addEventListener();
	}
	if (boardData.getBoardMode() == "write") {
		boardWrite.loadEasyMDE();
		boardWrite.addEventListener();
	}
});