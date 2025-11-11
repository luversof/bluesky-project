import { postJson, putJson, deleteJson } from "../fetchClient.js";
import { handleApiError } from "../errorHandler.js";
/**
 * BoardData - 게시판 정보 관리
 */
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
        },
    };
})();
/**
 * BoardAction - 페이지 이동 관리
 */
const boardAction = (() => {
    const getUrlParams = () => {
        return new URLSearchParams(window.location.search);
    };
    return {
        moveToList() {
            const boardAlias = boardData.getBoardAlias();
            const params = getUrlParams();
            params.delete("boardArticleId");
            const queryString = params.toString();
            const path = boardAlias ? `/board/${boardAlias}/list` : "list";
            window.location.href = `${path}${queryString ? "?" + queryString : ""}`;
        },
        moveToWrite() {
            const boardAlias = boardData.getBoardAlias();
            const params = getUrlParams();
            const queryString = params.toString();
            const path = boardAlias ? `/board/${boardAlias}/write` : "write";
            window.location.href = `${path}${queryString ? "?" + queryString : ""}`;
        },
        moveToView(boardArticleId) {
            const boardAlias = boardData.getBoardAlias();
            console.log("moveToView - boardAlias:", boardAlias, "boardArticleId:", boardArticleId);
            const params = getUrlParams();
            params.set("boardArticleId", boardArticleId);
            const path = boardAlias ? `/board/${boardAlias}/view` : "view";
            console.log("moveToView - path:", path, "params:", params.toString());
            window.location.href = `${path}?${params.toString()}`;
        },
        moveToModify(boardArticleId) {
            const boardAlias = boardData.getBoardAlias();
            const params = getUrlParams();
            params.set("boardArticleId", boardArticleId);
            const path = boardAlias ? `/board/${boardAlias}/modify` : "modify";
            window.location.href = `${path}?${params.toString()}`;
        },
    };
})();
/**
 * BoardList - 목록 화면 관리
 */
const boardList = (() => {
    return {
        addEventListener() {
            document.addEventListener("listHtmxResponseTrigger", (event) => {
                const target = event.target;
                // 페이지네이션 버튼
                target.querySelectorAll(".navButton").forEach((el) => {
                    el.addEventListener("click", (e) => {
                        const button = e.target;
                        const page = button.dataset.page;
                        if (page) {
                            const params = new URLSearchParams(window.location.search);
                            params.set("page", page);
                            window.htmx.trigger("#boardList", "listHtmxTrigger");
                        }
                    });
                });
                // 날짜 포맷팅 (dayjs 사용)
                target.querySelectorAll("[data-date]").forEach((el) => {
                    const date = el.dataset.date;
                    if (date && window.dayjs) {
                        el.textContent = window.dayjs().to(date);
                    }
                });
                // 글쓰기 버튼
                target.querySelectorAll(".writeButton").forEach((el) => {
                    el.addEventListener("click", () => boardAction.moveToWrite());
                });
                // 게시글 행 클릭
                target
                    .querySelectorAll("table tr[data-boardArticleId]")
                    .forEach((el) => {
                    el.addEventListener("click", (e) => {
                        const row = e.target.closest("tr");
                        const boardArticleId = row.dataset.boardarticleid;
                        if (boardArticleId) {
                            boardAction.moveToView(boardArticleId);
                        }
                    });
                });
            });
        },
    };
})();
/**
 * BoardView - 상세보기 화면 관리
 */
const boardView = (() => {
    return {
        addEventListener() {
            // 글쓰기 버튼
            document.querySelectorAll(".writeButton").forEach((el) => {
                el.addEventListener("click", () => boardAction.moveToWrite());
            });
            // 목록 버튼
            document.querySelectorAll(".listButton").forEach((el) => {
                el.addEventListener("click", () => boardAction.moveToList());
            });
            // 수정 버튼
            document.querySelectorAll(".modifyButton").forEach((el) => {
                el.addEventListener("click", () => {
                    const boardArticleId = el.dataset.boardarticleid;
                    if (boardArticleId) {
                        boardAction.moveToModify(boardArticleId);
                    }
                });
            });
            // 삭제 버튼
            document.querySelectorAll(".deleteButton").forEach((el) => {
                el.addEventListener("click", async () => {
                    const boardArticleId = el.dataset.boardarticleid;
                    if (!boardArticleId)
                        return;
                    if (!confirm("정말 삭제하시겠습니까?"))
                        return;
                    try {
                        await deleteJson("/api/boardArticle", {
                            id: boardArticleId,
                            boardId: boardData.getBoardId(),
                        });
                        alert("삭제되었습니다.");
                        boardAction.moveToList();
                    }
                    catch (err) {
                        handleApiError(err, {
                            onDisplayableMessage: (msg) => alert(msg),
                            onNonDisplayable: (e) => console.error("Delete error", e),
                        });
                    }
                });
            });
        },
    };
})();
/**
 * BoardWrite - 글쓰기 화면 관리
 */
const boardWrite = (() => {
    let easymde;
    return {
        loadEasyMDE() {
            const contentEl = document.getElementById("content");
            if (contentEl && window.EasyMDE) {
                easymde = new window.EasyMDE({
                    element: contentEl,
                    spellChecker: false,
                    autosave: {
                        enabled: true,
                        uniqueId: "board-write-" + boardData.getBoardId(),
                        delay: 1000,
                    },
                });
            }
        },
        addEventListener() {
            // 취소 버튼
            document.querySelectorAll(".cancelButton").forEach((el) => {
                el.addEventListener("click", () => boardAction.moveToList());
            });
            // 작성 버튼
            document.querySelectorAll(".writeButton").forEach((el) => {
                el.addEventListener("click", () => this.writeAndMoveToView());
            });
        },
        async writeAndMoveToView() {
            const titleEl = document.getElementById("title");
            if (!titleEl) {
                alert("제목 입력란을 찾을 수 없습니다.");
                return;
            }
            const title = titleEl.value.trim();
            const content = easymde ? easymde.value() : "";
            if (!title) {
                alert("제목을 입력해주세요.");
                titleEl.focus();
                return;
            }
            if (!content) {
                alert("내용을 입력해주세요.");
                return;
            }
            try {
                const result = await postJson("/api/boardArticle", {
                    boardId: boardData.getBoardId(),
                    title,
                    content,
                });
                console.log("Write result:", result);
                // BoardArticle 타입: { id, userId, boardId, title, content, createdDate, lastModifiedDate }
                if (result && result.id) {
                    // LocalStorage에 저장된 autosave 내용 제거
                    if (easymde) {
                        easymde.clearAutosavedValue();
                    }
                    console.log("Moving to view with id:", result.id);
                    boardAction.moveToView(result.id);
                }
                else {
                    console.log("No id in result, moving to list");
                    alert("게시글이 등록되었습니다.");
                    boardAction.moveToList();
                }
            }
            catch (err) {
                handleApiError(err, {
                    onDisplayableMessage: (msg) => alert(msg),
                    onNonDisplayable: (e) => console.error("Write error", e),
                });
            }
        },
    };
})();
/**
 * BoardModify - 수정 화면 관리
 */
const boardModify = (() => {
    let easymde;
    return {
        loadEasyMDE(initialContent) {
            const contentEl = document.getElementById("content");
            if (contentEl && window.EasyMDE) {
                easymde = new window.EasyMDE({
                    element: contentEl,
                    spellChecker: false,
                    initialValue: initialContent || "",
                });
            }
        },
        addEventListener() {
            // 취소 버튼
            document.querySelectorAll(".cancelButton").forEach((el) => {
                el.addEventListener("click", () => {
                    const params = new URLSearchParams(window.location.search);
                    const boardArticleId = params.get("boardArticleId");
                    if (boardArticleId) {
                        boardAction.moveToView(boardArticleId);
                    }
                    else {
                        boardAction.moveToList();
                    }
                });
            });
            // 수정 버튼
            document.querySelectorAll(".modifyButton").forEach((el) => {
                el.addEventListener("click", () => this.modifyAndMoveToView());
            });
        },
        async modifyAndMoveToView() {
            const titleEl = document.getElementById("title");
            const boardArticleIdEl = document.getElementById("boardArticleId");
            if (!titleEl || !boardArticleIdEl) {
                alert("필수 입력란을 찾을 수 없습니다.");
                return;
            }
            const title = titleEl.value.trim();
            const content = easymde ? easymde.value() : "";
            const boardArticleId = boardArticleIdEl.value;
            if (!title) {
                alert("제목을 입력해주세요.");
                titleEl.focus();
                return;
            }
            if (!content) {
                alert("내용을 입력해주세요.");
                return;
            }
            try {
                await putJson("/api/boardArticle", {
                    id: boardArticleId,
                    boardId: boardData.getBoardId(),
                    title,
                    content,
                });
                // LocalStorage에 저장된 autosave 내용 제거
                if (easymde) {
                    easymde.clearAutosavedValue();
                }
                alert("수정되었습니다.");
                boardAction.moveToView(boardArticleId);
            }
            catch (err) {
                handleApiError(err, {
                    onDisplayableMessage: (msg) => alert(msg),
                    onNonDisplayable: (e) => console.error("Modify error", e),
                });
            }
        },
    };
})();
// 전역으로 노출
window.boardData = boardData;
window.boardAction = boardAction;
window.boardList = boardList;
window.boardView = boardView;
window.boardWrite = boardWrite;
window.boardModify = boardModify;
// DOM 로드 후 자동 초기화
document.addEventListener("DOMContentLoaded", () => {
    // body의 data 속성에서 값 읽어오기
    const body = document.body;
    const boardMode = body.dataset.boardMode;
    const boardAlias = body.dataset.boardAlias;
    const boardId = body.dataset.boardId;
    // boardData에 값 설정
    if (boardMode) {
        boardData.setBoardMode(boardMode);
    }
    if (boardAlias) {
        boardData.setBoardAlias(boardAlias);
    }
    if (boardId) {
        boardData.setBoardId(boardId);
    }
    // 모드별 초기화
    if (boardMode === "list") {
        boardList.addEventListener();
    }
    else if (boardMode === "write") {
        boardWrite.loadEasyMDE();
        boardWrite.addEventListener();
    }
    else if (boardMode === "view") {
        boardView.addEventListener();
    }
    else if (boardMode === "modify") {
        const contentEl = document.getElementById("content");
        const initialContent = contentEl ? contentEl.value : "";
        boardModify.loadEasyMDE(initialContent);
        boardModify.addEventListener();
    }
});
