$(document).ready(function() {
	/*var */$.article = function() {
		var _link = {
			list : "/api/blogArticles/search/findByBlogId"	
		}
		
		var _list = function(blogId) {
			var data = {};
			data.id = blogId;
			return $.getJSON(_link.list, data);
		}
		
		return {
			list : function(blogId) {
				_list(blogId).done(function(data) {
					console.log("data : ", data);
					console.log(Mustache.render($("#articleListTemplate").html(), data));
				});
			},
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
		$.article.create();
	});

	$("[name=blog-write] :submit").on("click", function(e) {
		e.preventDefault();
		$.article.save();
	});
	
	$("[name=blog-modify] :submit").on("click", function(e) {
		e.preventDefault();
		$.article.modify();
	});
	
	$("[data-delete-blog-article-articleId]").on("click", function() {
		$.remove($(this).attr("data-delete-blog-article-blogId"), $(this).attr("data-delete-blog-article-articleId"));
	});
});
