$(document).ready(function() {
	 
	
	var blog = function() {
		var _link = {
			get : function(blogId) {
				return $.i18n.prop("url.blog.api.get", blogId);
			}
		}
		
		
		var _get = function(blogId) {
			return $.getJSON({
				url : _link.get(blogId)
			});
		} 
		
		return {
			create : function() {
				
			},
			get : function(blogId) {
				return _get(blogId);
			}
		};
	}();
	
	/*$(".blog-create").on("click", function(e) {
		e.preventDefault();
		blog.create();
	});*/
	
	var blogVue = new Vue({
		el : "#blog-content",
		components : {
			"blog-create" : {
				methods : {
					create : function(event) {
						$.ajax({
							type : "POST",
							url : $.i18n.prop("url.blog.api.create"),
							contentType:"application/hal+json; charset=utf-8",
							data : JSON.stringify({}),
							success : function(data) {
								location.href = $.i18n.prop("url.blog.view.list", data.id);
							}
						});
					}
				}
			},
			"blog-list" : {
				data : function() {
					return { 
						articleListResponse : {}
					}
				},
				methods : {
					getViewUrl : function() {
						var blogId = "b68f7647-6ddd-4b8c-aecf-352e82ad764e";
						var articleId = 123;
						return $.i18n.prop("url.blog.view.view", blogId, articleId);
					},
					getArticleListResponse : function() {
						var _this = this;
						var blogId = "b68f7647-6ddd-4b8c-aecf-352e82ad764e";
						$.ajax({
							type : "GET",
							url : $.i18n.prop("url.article.api.get-list"),
							data : { id : blogId }
						}).done(function(response) {
							_this.articleListResponse = response;
						});
					},
					getArticleList : function() {
						if (this.articleListResponse._embedded == undefined || this.articleListResponse._embedded.blogArticles == undefined) {
							return [];
						}
						return this.articleListResponse._embedded.blogArticles;
					}
					
				},
				mounted : function() {
					console.log("mounted");
					this.getArticleListResponse();
				},
				computed : {
					
				}
			},
			"nav" : {
				data : function() {
					return {
						
					}
				}
			}
		}
	});
});