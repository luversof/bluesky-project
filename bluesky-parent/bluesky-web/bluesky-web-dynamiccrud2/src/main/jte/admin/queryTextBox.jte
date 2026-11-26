<%-- 쿼리 입력칸(admin/queryEditor). "기본 쿼리 다시 만들기"(AdminController.redraftQuery)가 이 조각만 바꾼다 - 저장은 하지 않는다.
     error: 기본 쿼리를 만들지 못한 사유(PK · unique 없음 등) - 입력칸은 원래 쿼리 그대로 --%>
@import static net.luversof.web.dynamiccrud2.config.Msg.t
@param String queryText
@param String error = null

@template._components.alert(message = error, cssClass = "mb-2")
<%-- 처음엔 작게(두세 줄), 줄이 늘면 내용만큼 커진다(.textarea-autogrow: CSS field-sizing, 안 되는 브라우저는 js/ui.js) --%>
<textarea aria-label="${t("admin.query.col.query")}" id="q-queryText" class="textarea textarea-autogrow code w-full" name="queryText" rows="3" spellcheck="false" required>${queryText}</textarea>
