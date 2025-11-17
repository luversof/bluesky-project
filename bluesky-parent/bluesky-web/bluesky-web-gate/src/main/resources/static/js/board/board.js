import { fetchJson, postJson, putJson, deleteJson } from "../fetchClient.js";
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
    setIsAuthenticated(value) {
      isAuthenticated = Boolean(value);
    },
    isAuthenticated() {
      return isAuthenticated;
    },
  };
})();
/**
 * BoardAction - 페이지 이동 관리
 */
const boardAction = (() => {
  const getUrlParams = () => {
    return new URLSearchParams(globalThis.location.search);
  };
  return {
    moveToList() {
      const boardAlias = boardData.getBoardAlias();
      const params = getUrlParams();
      params.delete("boardArticleId");
      const queryString = params.toString();
      const path = boardAlias ? `/board/${boardAlias}/list` : "list";
      globalThis.location.href = `${path}${
        queryString ? "?" + queryString : ""
      }`;
    },
    moveToWrite() {
      const boardAlias = boardData.getBoardAlias();
      const params = getUrlParams();
      const queryString = params.toString();
      const path = boardAlias ? `/board/${boardAlias}/write` : "write";
      globalThis.location.href = `${path}${
        queryString ? "?" + queryString : ""
      }`;
    },
    moveToView(boardArticleId) {
      const boardAlias = boardData.getBoardAlias();
      console.log(
        "moveToView - boardAlias:",
        boardAlias,
        "boardArticleId:",
        boardArticleId
      );
      const params = getUrlParams();
      params.set("boardArticleId", boardArticleId);
      const path = boardAlias ? `/board/${boardAlias}/view` : "view";
      console.log("moveToView - path:", path, "params:", params.toString());
      globalThis.location.href = `${path}?${params.toString()}`;
    },
    moveToModify(boardArticleId) {
      const boardAlias = boardData.getBoardAlias();
      const params = getUrlParams();
      params.set("boardArticleId", boardArticleId);
      const path = boardAlias ? `/board/${boardAlias}/modify` : "modify";
      globalThis.location.href = `${path}?${params.toString()}`;
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
        const handleNavButtonClick = (clickEvent) => {
          const page = clickEvent.currentTarget.dataset.page;
          if (!page) {
            return;
          }
          const params = new URLSearchParams(globalThis.location.search);
          params.set("page", page);
          if (globalThis.htmx) {
            globalThis.htmx.trigger("#boardList", "listHtmxTrigger");
          }
        };
        for (const navButton of target.querySelectorAll(".navButton")) {
          navButton.addEventListener("click", handleNavButtonClick);
        }
        for (const dateEl of target.querySelectorAll("[data-date]")) {
          const date = dateEl.dataset.date;
          if (date && globalThis.dayjs) {
            dateEl.textContent = globalThis.dayjs().to(date);
          }
        }
        for (const writeButton of target.querySelectorAll(".writeButton")) {
          writeButton.addEventListener("click", () =>
            boardAction.moveToWrite()
          );
        }
        const handleRowClick = (clickEvent) => {
          const row = clickEvent.currentTarget;
          const boardArticleId = row.dataset.boardarticleid;
          if (boardArticleId) {
            boardAction.moveToView(boardArticleId);
          }
        };
        for (const rowEl of target.querySelectorAll(
          "table tr[data-boardArticleId]"
        )) {
          rowEl.addEventListener("click", handleRowClick);
        }
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
      const handleModifyClick = (event) => {
        const boardArticleId = event.currentTarget.dataset.boardarticleid;
        if (boardArticleId) {
          boardAction.moveToModify(boardArticleId);
        }
      };
      const handleDeleteClick = async (event) => {
        const boardArticleId = event.currentTarget.dataset.boardarticleid;
        if (!boardArticleId) {
          return;
        }
        if (!globalThis.confirm("정말 삭제하시겠습니까?")) {
          return;
        }
        try {
          await deleteJson("/api/boardArticle", {
            id: boardArticleId,
            boardId: boardData.getBoardId(),
          });
          globalThis.alert("삭제되었습니다.");
          boardAction.moveToList();
        } catch (err) {
          handleApiError(err, {
            onDisplayableMessage: (msg) => alert(msg),
            onNonDisplayable: (e) => console.error("Delete error", e),
          });
        }
      };
      for (const button of document.querySelectorAll(".writeButton")) {
        button.addEventListener("click", () => boardAction.moveToWrite());
      }
      for (const button of document.querySelectorAll(".listButton")) {
        button.addEventListener("click", () => boardAction.moveToList());
      }
      for (const button of document.querySelectorAll(".modifyButton")) {
        button.addEventListener("click", handleModifyClick);
      }
      for (const button of document.querySelectorAll(".deleteButton")) {
        button.addEventListener("click", handleDeleteClick);
      }
    },
  };
})();

/**
 * BoardComment - 댓글 영역 관리
 */
const boardComment = (() => {
  const state = { page: 0, size: 10, last: false, loading: false };
  let listEl;
  let countEl;
  let loadMoreButton;
  let textareaEl;
  let submitButton;
  let formEl;

  const getBodyDataset = () => (document.body ? document.body.dataset : {});

  const formatRelativeDate = (isoString) => {
    if (!isoString) {
      return "";
    }
    if (globalThis.dayjs) {
      return globalThis.dayjs(isoString).fromNow();
    }
    const date = new Date(isoString);
    return Number.isNaN(date.getTime()) ? "" : date.toLocaleString();
  };

  const isOwner = (comment) => {
    const currentUserId = boardData.getCurrentUserId();
    if (!currentUserId || !comment || !comment.userId) {
      return false;
    }
    return (
      currentUserId.toString().toLowerCase() ===
      comment.userId.toString().toLowerCase()
    );
  };

  const updateCount = (value) => {
    if (countEl) {
      countEl.textContent = String(value ?? 0);
    }
  };

  const clearList = () => {
    if (listEl) {
      listEl.innerHTML = "";
    }
  };

  const renderEmptyState = () => {
    if (!listEl) {
      return;
    }
    const emptyText = listEl.dataset.emptyText || "No comments";
    const placeholder = document.createElement("p");
    placeholder.className = "text-sm text-gray-500";
    placeholder.textContent = emptyText;
    listEl.appendChild(placeholder);
  };

  const createDeleteButton = (commentId) => {
    const dataset = getBodyDataset();
    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn btn-xs btn-error";
    button.textContent = dataset.commentDeleteLabel || "삭제";
    button.addEventListener("click", () => handleDelete(commentId));
    return button;
  };

  const createCommentElement = (comment) => {
    const wrapper = document.createElement("div");
    wrapper.className = "border rounded-lg p-4 bg-base-100";
    wrapper.dataset.commentId = comment.id;

    const header = document.createElement("div");
    header.className = "flex items-center justify-between gap-4";

    const author = document.createElement("div");
    author.className = "text-sm font-semibold text-gray-700";
    author.textContent = comment.username || comment.userId || "익명";

    const meta = document.createElement("div");
    meta.className = "text-xs text-gray-400";
    meta.textContent = formatRelativeDate(comment.createdDate);

    header.append(author, meta);

    const content = document.createElement("p");
    content.className = "mt-2 whitespace-pre-line text-sm";
    content.textContent = comment.content || "";

    wrapper.append(header, content);

    if (isOwner(comment)) {
      const actions = document.createElement("div");
      actions.className = "mt-3 flex justify-end";
      actions.appendChild(createDeleteButton(comment.id));
      wrapper.appendChild(actions);
    }

    return wrapper;
  };

  const renderComments = (comments, reset) => {
    if (!listEl) {
      return;
    }
    if (reset) {
      clearList();
    }
    if (!comments.length) {
      if (!listEl.children.length) {
        renderEmptyState();
      }
      return;
    }
    for (const comment of comments) {
      listEl.appendChild(createCommentElement(comment));
    }
  };

  const toggleLoadMore = () => {
    if (!loadMoreButton) {
      return;
    }
    if (state.last) {
      loadMoreButton.classList.add("hidden");
    } else {
      loadMoreButton.classList.remove("hidden");
    }
  };

  const loadComments = async ({ reset = false } = {}) => {
    if (state.loading) {
      return;
    }
    const boardArticleId = boardData.getBoardArticleId();
    if (!boardArticleId) {
      return;
    }
    state.loading = true;
    const nextPage = reset ? 0 : state.page;
    try {
      const params = new URLSearchParams({
        page: String(nextPage),
        size: String(state.size),
        sort: "createdDate,desc",
      });
      const pageData = await fetchJson(
        `/api/boardArticleComment/search/findByBoardArticleId/${boardArticleId}?${params.toString()}`
      );
      const comments = (pageData && pageData.content) || [];
      renderComments(comments, reset);
      const totalElements =
        pageData && typeof pageData.totalElements === "number"
          ? pageData.totalElements
          : comments.length;
      updateCount(totalElements);
      state.last = pageData ? Boolean(pageData.last) : true;
      state.page =
        (pageData && typeof pageData.number === "number"
          ? pageData.number
          : nextPage) + 1;
      toggleLoadMore();
    } catch (err) {
      handleApiError(err, {
        onDisplayableMessage: (msg) => alert(msg),
        onNonDisplayable: (e) => console.error("Comment load error", e),
      });
    } finally {
      state.loading = false;
    }
  };

  const handleDelete = async (commentId) => {
    if (!commentId) {
      return;
    }
    const dataset = getBodyDataset();
    const confirmMessage =
      dataset.commentDeleteConfirm || "댓글을 삭제하시겠습니까?";
    if (!globalThis.confirm(confirmMessage)) {
      return;
    }
    try {
      await deleteJson("/api/boardArticleComment", { id: commentId });
      if (dataset.commentDeleteSuccess) {
        alert(dataset.commentDeleteSuccess);
      }
      await loadComments({ reset: true });
    } catch (err) {
      handleApiError(err, {
        onDisplayableMessage: (msg) => alert(msg),
        onNonDisplayable: (e) => console.error("Comment delete error", e),
      });
    }
  };

  const submitComment = async () => {
    if (!textareaEl) {
      return;
    }
    const dataset = getBodyDataset();
    const content = textareaEl.value.trim();
    if (!content) {
      alert(dataset.commentRequired || "댓글 내용을 입력해주세요.");
      textareaEl.focus();
      return;
    }
    const boardArticleId = boardData.getBoardArticleId();
    if (!boardArticleId) {
      return;
    }
    try {
      await postJson("/api/boardArticleComment", {
        boardArticleId,
        content,
      });
      textareaEl.value = "";
      if (dataset.commentCreateSuccess) {
        alert(dataset.commentCreateSuccess);
      }
      await loadComments({ reset: true });
    } catch (err) {
      handleApiError(err, {
        onDisplayableMessage: (msg) => alert(msg),
        onNonDisplayable: (e) => console.error("Comment create error", e),
      });
    }
  };

  const registerEvents = () => {
    if (loadMoreButton) {
      loadMoreButton.addEventListener("click", () =>
        loadComments({ reset: false })
      );
    }
    if (formEl) {
      formEl.addEventListener("submit", (event) => {
        event.preventDefault();
        submitComment();
      });
    }
    if (submitButton) {
      submitButton.addEventListener("click", (event) => {
        event.preventDefault();
        submitComment();
      });
    }
    if (textareaEl) {
      textareaEl.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
          event.preventDefault();
          submitComment();
        }
      });
    }
  };

  return {
    async init() {
      listEl = document.getElementById("commentList");
      countEl = document.getElementById("commentCount");
      loadMoreButton = document.getElementById("commentLoadMore");
      textareaEl = document.getElementById("commentContent");
      submitButton = document.querySelector(".commentSubmitButton");
      formEl = document.getElementById("commentForm");

      if (!listEl) {
        return;
      }

      registerEvents();
      await loadComments({ reset: true });
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
      if (contentEl && globalThis.EasyMDE) {
        easymde = new globalThis.EasyMDE({
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
      const handleWriteClick = () => this.writeAndMoveToView();
      for (const button of document.querySelectorAll(".cancelButton")) {
        button.addEventListener("click", () => boardAction.moveToList());
      }
      for (const button of document.querySelectorAll(".writeButton")) {
        button.addEventListener("click", handleWriteClick);
      }
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
          alert("게시글이 등록되었습니다.");
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
      if (contentEl && globalThis.EasyMDE) {
        easymde = new globalThis.EasyMDE({
          element: contentEl,
          spellChecker: false,
          initialValue: initialContent || "",
        });
      }
    },
    addEventListener() {
      const handleCancelClick = () => {
        const params = new URLSearchParams(globalThis.location.search);
        const boardArticleId = params.get("boardArticleId");
        if (boardArticleId) {
          boardAction.moveToView(boardArticleId);
        } else {
          boardAction.moveToList();
        }
      };
      const handleModifyClick = () => this.modifyAndMoveToView();
      for (const button of document.querySelectorAll(".cancelButton")) {
        button.addEventListener("click", handleCancelClick);
      }
      for (const button of document.querySelectorAll(".modifyButton")) {
        button.addEventListener("click", handleModifyClick);
      }
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
globalThis.boardData = boardData;
globalThis.boardAction = boardAction;
globalThis.boardList = boardList;
globalThis.boardView = boardView;
globalThis.boardComment = boardComment;
globalThis.boardWrite = boardWrite;
globalThis.boardModify = boardModify;
// DOM 로드 후 자동 초기화
document.addEventListener("DOMContentLoaded", () => {
  // body의 data 속성에서 값 읽어오기
  const body = document.body;
  const boardMode = body.dataset.boardMode;
  const boardAlias = body.dataset.boardAlias;
  const boardId = body.dataset.boardId;
  const boardArticleId = body.dataset.boardArticleId;
  const isAuthenticated = body.dataset.isAuthenticated === "true";
  const currentUserId = body.dataset.currentUserId;
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
  boardData.setBoardArticleId(boardArticleId || null);
  boardData.setCurrentUserId(currentUserId || null);
  boardData.setIsAuthenticated(isAuthenticated);
  // 모드별 초기화
  if (boardMode === "list") {
    boardList.addEventListener();
  } else if (boardMode === "write") {
    boardWrite.loadEasyMDE();
    boardWrite.addEventListener();
  } else if (boardMode === "view") {
    boardView.addEventListener();
    boardComment.init();
  } else if (boardMode === "modify") {
    const contentEl = document.getElementById("content");
    const initialContent = contentEl ? contentEl.value : "";
    boardModify.loadEasyMDE(initialContent);
    boardModify.addEventListener();
  }
});
