$(document).ready(function() {
	var blog = function() {
		return {
			create : function() {
				var form = $("[name=blog-create]");
				$.ajax({
					type : form.attr("method"),
					url : form.attr("action"),
					dataType : "json",
					success : function(data) {
						location.href = $.i18n.prop("url.blog.view.list", data.id);
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
			, remove : function(blogId, articleId) {
				$.ajax({
					type : "post",
					url : $.i18n.prop("url.blog.delete.article", blogId, articleId),
					dataType : "json",
					data : { _method : "delete" },
					success : function(data) {
						location.href = $.i18n.prop("url.blog.view.list", blogId);
					}
				});
			}
		};
	}();
	
	$("[name=blog-create] :submit").on("click", function(e) {
		e.preventDefault();
		blog.create();
	});

	$("[name=blog-write] :submit").on("click", function(e) {
		e.preventDefault();
		blog.save();
	});
	
	$("[name=blog-modify] :submit").on("click", function(e) {
		e.preventDefault();
		blog.modify();
	});
	
	$("[data-delete-blog-article-articleId]").on("click", function() {
		blog.remove($(this).attr("data-delete-blog-article-blogId"), $(this).attr("data-delete-blog-article-articleId"));
	});
});
