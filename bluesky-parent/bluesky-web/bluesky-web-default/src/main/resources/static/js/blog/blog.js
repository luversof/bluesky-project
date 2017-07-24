$(document).ready(function() {
	var blog = function() {
		return {
			create : function() {
				$.ajax({
					type : form.attr("method"),
					url : form.attr("action"),
					dataType : "json",
					success : function(data) {
						location.href = $.i18n.prop("url.blog.view.list", data.blog.id);
					}
				});
			},
			save : function() {
				var form = $("[name=blog-write]");
				$.ajax({
					type : form.attr("method"),
					url : form.attr("action"),
					dataType : "json",
					data : form.serialize(),
					success : function(data) {
						location.href = $.i18n.prop("url.blog.view.view", data.blog.id, data.id);
					}
				});
			}
			, modify : function() {
				var form = $("[name=blog-modify]");
				$.ajax({
					type : "post",
					url : form.attr("action"),
					dataType : "json",
					data : form.serialize(),
					success : function(data) {
						location.href = $.i18n.prop("url.blog.view.view", data.blog.id, data.id);
					}
				});
			}
			, remove : function(blogId, blogArticleId) {
				$.ajax({
					type : "post",
					url : $.i18n.prop("url.blog.delete.article", blogId, blogArticleId),
					dataType : "json",
					data : { _method : "delete" },
					success : function(data) {
						location.href = $.i18n.prop("url.blog.view.list", blogId);
					}
				});
			}
		};
	}();
	
	$("[name=blog-create] .submit").on("click", function(e) {
		alert(1);
		e.preventDefault();
		alert(2);
		//blog.create();
	});

	$("[name=blog-write] .submit").on("click", blog.save);
	$("[name=blog-modify] .submit").on("click", blog.modify);
	$("[data-blogArticle-delete-id]").on("click", function() {
		blog.remove($(this).attr("data-blogArticle-delete-blogId"), $(this).attr("data-blogArticle-delete-id"));
	});
	
	$("button.btn.cancel").on("click", function() {
		history.back();
	});
});