/**
 * 상단 메뉴 표시
 */
var navbar = {
	contextPath : "/",
	display : function() {
		if (location.pathname == this.contextPath) {
			$(".navbar .nav li:eq(0)").addClass("active");
			return;
		}
		$(".navbar .navbar-nav li").each(function() {
			console.log("pathname : %s", location.pathname);
			if (location.pathname.search($(this).text()) > 0) {
				$(this).addClass("active");
			} else {
				$(this).removeClass();
			}
		});
	}
};

$(function() {
	var $nav = $(".navbar"),
	_hideShowOffset = 20,
	_lastScroll = 0,
	_detachPoint = 50;
	
	$(window).scroll(function() {
	var t = $(window).scrollTop(),
		e = t > _lastScroll ? "down" : "up",
		i = Math.abs(t - _lastScroll);
	
	if (t >= _detachPoint || 0 >= t || t > -1) {
		if ("down" === e && i >= _hideShowOffset) {
			$nav.slideUp();
		} else if("up" === e && i >= _hideShowOffset) {
			$nav.slideDown();
		}
	}
	
	_lastScroll = t;
	});
});

/**
 * blogPost관련 script display를 별도 분리 처리 하지 않음
 */
var blogPost = {
	/**
	 * 포스트 삭제
	 * 
	 * @param blogPostId
	 */
	remove : function(blogPostId) {
		console.log("blogPost.remove");
		$("<form />").attr({
			"action" : "/blogPost/" + blogPostId,
			"method" : "post"
		}).append($("<input />").attr({
			"type" : "hidden",
			"name" : "_method"
		}).val("delete")).submit();
	},
	/**
	 * data-blogPost-content attribute가 선언된 태그의 안에 해당 content 삽입 처
	 */
	displayContext : function() {
		$("[data-blogPost-content]").each(function() {
			console.log($(this).attr("data-blogPost-content"));
			$(this).html($(this).attr("data-blogPost-content"));
		});
	}
};