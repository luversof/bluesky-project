"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitBoardArticle = submitBoardArticle;
exports.attachBoardWriteForm = attachBoardWriteForm;
const fetchClient_1 = require("./fetchClient");
const errorHandler_1 = require("./errorHandler");
/**
 * 게시글 작성 예제: EasyMDE 인스턴스에서 값을 읽어 API에 전송
 * 프로젝트에서 이 파일을 컴파일하면 결과 JS를 `src/main/resources/static/js`에서 사용할 수 있습니다.
 */
function submitBoardArticle(easymdeInstance) {
    return __awaiter(this, void 0, void 0, function* () {
        // easymdeInstance가 전달되지 않으면 window에서 읽어옵니다.
        const easymde = easymdeInstance || window.easymde;
        const titleEl = document.getElementById('title');
        if (!titleEl) {
            console.error('title element not found');
            return;
        }
        const title = titleEl.value;
        const content = easymde ? easymde.value() : '';
        try {
            // postJson는 공통 에러 타입(ApiError 등)을 던집니다
            const result = yield (0, fetchClient_1.postJson)('/api/boardArticle', { title, content }, { timeoutMs: 8000 });
            // 성공 처리: 예시로 새 글 보기로 이동
            if (result && result.id) {
                const id = result.id;
                window.location.href = `view?boardArticleId=${encodeURIComponent(id)}`;
            }
            else {
                // API가 성공적으로 200을 반환했지만 id가 없을 경우
                alert('등록은 완료되었지만, 결과를 확인할 수 없습니다.');
            }
        }
        catch (err) {
            // 공통 에러 핸들러에 전달
            (0, errorHandler_1.handleApiError)(err, {
                onDisplayableMessage: (msg) => {
                    // UI에 맞게 토스트나 알림으로 보여주면 좋습니다
                    // 여기선 간단히 alert 사용
                    alert(msg);
                },
                onNonDisplayable: (e) => {
                    // 추가 로깅이나 디버그 정보 표시
                    console.error('API non-displayable error', e);
                    // 필요하면 모달을 띄우거나 개발자용 토스트를 보여주는 로직 추가
                }
            });
        }
    });
}
// DOMContentLoaded 시 버튼에 바인딩하는 유틸 (예시)
function attachBoardWriteForm(easymdeInstance) {
    document.addEventListener('DOMContentLoaded', () => {
        const writeBtn = document.querySelector('.writeButton');
        if (!writeBtn)
            return;
        writeBtn.addEventListener('click', (e) => {
            e.preventDefault();
            submitBoardArticle(easymdeInstance);
        });
    });
}
