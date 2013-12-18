/**
 * blog관련 script display를 별도 분리 처리 하지 않음
 */
var blog = {
	/**
	 * 포스트 삭제
	 * 
	 * @param blogId
	 */
	remove : function(blogId) {
		console.log("blog.remove");
		$("<form />").attr({
			"action" : "/blog/" + blogId,
			"method" : "post"
		}).append($("<input />").attr({
			"type" : "hidden",
			"name" : "_method"
		}).val("delete")).submit();
	}
	/**
	 * data-blog-content attribute가 선언된 태그의 안에 해당 content 삽입 처리
	 */
	, displayContext : function() {
		$("[data-blog-content]").each(function() {
			console.log($(this).attr("data-blog-content"));
			$(this).html($(this).attr("data-blog-content"));
		});
	}
	, save : function() {
		var form = $(".write.form-horizontal");
		$.ajax({
			type : form.attr("method")
			, url : form.attr("action")
			, dataType : "json"
			, data : form.serialize()
			, success : function(data) {
				location.href = "/blog/" + data;
			}
		});
	}
	, modify : function() {
		var form = $(".modify.form-horizontal");
		$.ajax({
			type : "put"
			, url : form.attr("action")
			, dataType : "json"
			, data : form.serialize()
			, success : function(data) {
				location.href = "/blog/" + data;
			}
		});
	}
};

$(document).ready(function() {
	$(".write.form-horizontal .submit").on("click", function() {
		blog.save();
	});
	$(".modify.form-horizontal .submit").on("click", function() {
		console.log("Test");
		blog.modify();
	});
});