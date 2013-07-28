/**
 * 상단 메뉴 표시
 */
function navbarDisplay() {
	if (location.pathname == "/") {
		$(".navbar .nav li:eq(0)").addClass("active");
		return;
	}
	$(".navbar .nav li").each(function() {
		console.log("pathname : %s", location.pathname);
		if (location.pathname.search($(this).text()) > 0) {
			$(this).addClass("active");
		} else {
			$(this).removeClass();
		}
	});
}


/**
 * blogPost관련 script
 * display를 별도 분리 처리 하지 않음
 */
var blogPost = {
		/**
		 * 포스트 삭제
		 * @param blogPostId
		 */
		remove : function (blogPostId) {
			$("<form />").attr({
				"action" : "/blogPost/" + blogPostId,
				"method" : "post"
			}).append($("<input />").attr({
				"type" : "hidden",
				"name" : "_method"
			}).val("delete")).submit();
		}		
};