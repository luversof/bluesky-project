import { postJson, putJson, deleteJson } from "../fetchClient.js";
import { handleApiError } from "../errorHandler.js";
/**
 * BoardData - 게시판 정보 관리
 */
const boardData = (() => {
  let boardAlias;
  let boardMode;
  let boardId;
  let boardArticleId;
  let currentUserId;
  let isAuthenticated = false;
  let loginUrl = "/login";
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
    setBoardArticleId(id) {
      boardArticleId = id;
    },
    getBoardArticleId() {
      return boardArticleId;
    },
    setCurrentUserId(id) {
      currentUserId = id;
    },
    getCurrentUserId() {
      return currentUserId;
    },
    setIsAuthenticated(flag) {
      isAuthenticated = flag;
    },
    getIsAuthenticated() {
      return isAuthenticated;
    },
    setLoginUrl(url) {
      loginUrl = url;
    },
    getLoginUrl() {
      return loginUrl;
    },
  };
})();
/**
 * BoardComment - 댓글 관리
 */
const boardComment = (() => {
  const commentListElId = "commentList";
  const commentFormId = "commentForm";
  const renderEmpty = (emptyText) => {
    const el = document.getElementById(commentListElId);
    if (el)
      el.innerHTML = `<div class="text-sm text-gray-500">${emptyText}</div>`;
  };
  const renderComments = (pageData) => {
    const el = document.getElementById(commentListElId);
    if (!el) return;
    const content = (pageData && pageData.content) || [];
    if (!content.length) {
      const emptyText = el.dataset.emptyText || "No comments";
      renderEmpty(emptyText);
      return;
    }
    el.innerHTML = "";
    content.forEach((c) => {
      var _a;
      const wrapper = document.createElement("div");
      wrapper.className = "border p-2 rounded";
      const header = document.createElement("div");
      header.className = "flex justify-between items-start";
      const author = document.createElement("div");
      author.innerHTML = `<b>${c.username || ((_a = c.user) === null || _a === void 0 ? void 0 : _a.username) || ""}</b> <small class=\"text-gray-500\">${window.dayjs ? window.dayjs(c.createdDate).fromNow() : c.createdDate}</small>`;
      header.appendChild(author);
      const actions = document.createElement("div");
      if (
        boardData.getCurrentUserId &&
        boardData.getCurrentUserId() &&
        boardData.getCurrentUserId() === (c.userId || (c.user && c.user.id))
      ) {
        const mod = document.createElement("button");
        mod.className = "btn btn-sm btn-ghost modifyCommentButton";
        mod.textContent = "수정";
        mod.dataset.commentId = c.id;
        actions.appendChild(mod);
        const del = document.createElement("button");
        del.className = "btn btn-sm btn-error deleteCommentButton ml-2";
        del.textContent = "삭제";
        del.dataset.commentId = c.id;
        actions.appendChild(del);
      }
      header.appendChild(actions);
      const body = document.createElement("div");
      body.className = "mt-2 comment-body";
      body.textContent = c.comment || c.content || "";
      wrapper.appendChild(header);
      wrapper.appendChild(body);
      el.appendChild(wrapper);
    });
    // wire up action buttons
    el.querySelectorAll(".deleteCommentButton").forEach((btn) => {
      btn.addEventListener("click", async (e) => {
        const id = e.currentTarget.dataset.commentId;
        if (!id) return;
        if (!confirm("댓글을 삭제하시겠습니까?")) return;
        try {
          await deleteJson("/api/boardArticleComment", {
            id,
            boardArticleId: boardData.getBoardArticleId(),
          });
          load();
        } catch (err) {
          handleApiError(err, {
            onDisplayableMessage: (msg) => alert(msg),
            onNonDisplayable: (e) => console.error(e),
          });
        }
      });
    });
    el.querySelectorAll(".modifyCommentButton").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        const id = e.currentTarget.dataset.commentId;
        if (!id) return;
        const parent = e.currentTarget.closest("div.border");
        if (!parent) return;
        const body = parent.querySelector(".comment-body");
        const original = body.textContent || "";
        const ta = document.createElement("textarea");
        ta.className = "w-full border p-1";
        ta.value = original;
        body.innerHTML = "";
        body.appendChild(ta);
        const save = document.createElement("button");
        save.className = "btn btn-sm btn-primary mt-2";
        save.textContent = "저장";
        save.addEventListener("click", async () => {
          try {
            await putJson("/api/boardArticleComment", {
              id,
              content: ta.value,
              boardArticleId: boardData.getBoardArticleId(),
            });
            load();
          } catch (err) {
            handleApiError(err, {
              onDisplayableMessage: (msg) => alert(msg),
              onNonDisplayable: (e) => console.error(e),
            });
          }
        });
        body.appendChild(save);
      });
    });
  };
  const load = async (page = 0) => {
    const id = boardData.getBoardArticleId();
    if (!id) return;
    try {
      const resp = await fetch(
        `/api/boardArticleComment/search/findByBoardArticleId/${id}?page=${page}`,
        { headers: { Accept: "application/json" } },
      );
      if (!resp.ok) throw resp;
      const data = await resp.json();
      renderComments(data);
    } catch (err) {
      console.error("Load comments error", err);
      renderEmpty("댓글을 불러올 수 없습니다.");
    }
  };
  const addEventListener = () => {
    // Template uses a textarea with id 'commentContent' and a button '.commentSubmitButton'
    const textarea = document.getElementById("commentContent");
    const submitBtn = document.querySelector(".commentSubmitButton");
    if (!submitBtn || !textarea) return;
    submitBtn.addEventListener("click", async (e) => {
      e.preventDefault();
      const comment = textarea.value.trim();
      if (!comment) {
        alert("댓글을 입력해주세요.");
        return;
      }
      try {
        await postJson("/api/boardArticleComment", {
          boardArticleId: boardData.getBoardArticleId(),
          content: comment,
        });
        textarea.value = "";
        load();
      } catch (err) {
        handleApiError(err, {
          onDisplayableMessage: (msg) => alert(msg),
          onNonDisplayable: (e) => console.error(e),
        });
      }
    });
  };
  return {
    load,
    renderComments,
    addEventListener,
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
      const targetUrl = `${path}${queryString ? "?" + queryString : ""}`;
      if (!boardData.getIsAuthenticated()) {
        if (
          confirm(
            "로그인이 필요한 서비스입니다. 로그인 페이지로 이동하시겠습니까?",
          )
        ) {
          window.location.href =
            boardData.getLoginUrl() +
            "?redirectUrl=" +
            encodeURIComponent(targetUrl);
        }
        return;
      }
      window.location.href = targetUrl;
    },
    moveToView(boardArticleId) {
      const boardAlias = boardData.getBoardAlias();
      console.log(
        "moveToView - boardAlias:",
        boardAlias,
        "boardArticleId:",
        boardArticleId,
      );
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
      const targetUrl = `${path}?${params.toString()}`;
      if (!boardData.getIsAuthenticated()) {
        if (
          confirm(
            "로그인이 필요한 서비스입니다. 로그인 페이지로 이동하시겠습니까?",
          )
        ) {
          window.location.href =
            boardData.getLoginUrl() +
            "?redirectUrl=" +
            encodeURIComponent(targetUrl);
        }
        return;
      }
      window.location.href = targetUrl;
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
          if (!boardArticleId) return;
          if (!confirm("정말 삭제하시겠습니까?")) return;
          try {
            await deleteJson("/api/boardArticle", {
              id: boardArticleId,
              boardId: boardData.getBoardId(),
            });
            alert("삭제되었습니다.");
            boardAction.moveToList();
          } catch (err) {
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
        } else {
          console.log("No id in result, moving to list");
          boardAction.moveToList();
        }
      } catch (err) {
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
          } else {
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
      } catch (err) {
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
window.boardComment = boardComment;
// DOM 로드 후 자동 초기화
document.addEventListener("DOMContentLoaded", () => {
  // data-board-mode 속성을 가진 요소에서 값 읽어오기
  const boardContainer =
    document.querySelector("[data-board-mode]") || document.body;
  const boardMode = boardContainer.dataset.boardMode;
  const boardAlias = boardContainer.dataset.boardAlias;
  const boardId = boardContainer.dataset.boardId;
  const boardArticleId = boardContainer.dataset.boardArticleId;
  const isAuthenticated = boardContainer.dataset.isAuthenticated;
  const currentUserId = boardContainer.dataset.currentUserId;
  const appConfig = document.getElementById("app-config");
  if (appConfig && appConfig.dataset.loginUrl) {
    boardData.setLoginUrl(appConfig.dataset.loginUrl);
  }
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
  if (boardArticleId) {
    boardData.setBoardArticleId(boardArticleId);
  }
  if (typeof isAuthenticated !== "undefined") {
    boardData.setIsAuthenticated(
      isAuthenticated === "true" || isAuthenticated === "1",
    );
  }
  if (currentUserId) {
    boardData.setCurrentUserId(currentUserId);
  }
  // 모드별 초기화
  if (boardMode === "list") {
    boardList.addEventListener();
  } else if (boardMode === "write") {
    boardWrite.loadEasyMDE();
    boardWrite.addEventListener();
  } else if (boardMode === "view") {
    boardView.addEventListener();
    // 댓글 초기화
    if (window.boardComment) {
      window.boardComment.addEventListener();
      window.boardComment.load();
    }
  } else if (boardMode === "modify") {
    const contentEl = document.getElementById("content");
    const initialContent = contentEl ? contentEl.value : "";
    boardModify.loadEasyMDE(initialContent);
    boardModify.addEventListener();
  }
});
