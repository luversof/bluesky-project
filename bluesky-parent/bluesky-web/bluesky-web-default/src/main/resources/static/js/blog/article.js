$(document).ready(function() {
	$.article = function() {
		var _blogId;
		var _articleId;
		var _link = {
			list : "/api/blogArticles/search/findByBlogId",
			findOne : "/api/blogArticles/{0}",
			create : "/api/blogArticles",
			categoryList : "/api/categories/search/findByBlogId"
		}
		
		var _list = function() {
			var data = {};
			data.id = _blogId;
			return $.getJSON(_link.list, data);
		}
		
		var _findOne = function() {
			return $.getJSON(_link.findOne.format(_articleId));
		}
		
		var _categoryList = function() {
			var data = {};
			data.blogId = _blogId;
			return $.getJSON(_link.categoryList, data);
		}
		
		return {
			setBlogId : function(blogId) {
				_blogId = blogId;
			},
			setArticleId : function(articleId) {
				_articleId = articleId;
			},
			list : function(targetArea, targetTemplate) {
				_list().done(function(data) {
					data.getId = function() {
						var parts = this._links.self.href.split("/");
						return parts[parts.length - 1];
					}
					data.getViewUrl = function() {
						var parts = this._links.self.href.split("/");
						return $.i18n.prop("url.blog.view.view", _blogId, parts[parts.length - 1]);
					}
					data.getCreateDateFormat = function() {
						return moment(this.createDate).format("LL");
					}
					data.isLoginUser = function() {
						return this
					}
					data.userInfo = userInfo;
					console.log(data);
					targetArea.html(Mustache.render(targetTemplate, data));
				});
			},
			findOne : function(targetArea, targetTemplate) {
				_findOne().done(function(data) {
					console.log(Mustache.render($("#articleViewTemplate").html(), data));
					targetArea.html(Mustache.render(targetTemplate, data));
				});
			},
			createForm : function(targetArea, targetTemplate) {
				return _categoryList().done(function(data) {
					targetArea.html(Mustache.render(targetTemplate, data))
				});
			},
			modifyForm : function(targetArea, targetTemplate) {
				return _categoryList().done(function(data) {
					_findOne(articleId)
				});
			},
			create : function(form) {
				console.log("TEST");
				console.log(makeFormDataJSON(form));
				$.ajax({
					type : "post",
					url : _link.create,
					contentType : "application/json",
					data : makeFormDataJSON(form),
					success : function(data) {
						location.href = $.i18n.prop("url.blog.view.view", _blogId, getIdFromDataRest(data));
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
	
	$(document).on("submit", "form[name=article-create]", function(e) {
		e.preventDefault();
		$.article.create($(this));
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
