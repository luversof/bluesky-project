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
	blogVueData = {
		blogId : "",
	};
	
	var blogVue = new Vue({
		el : "#blog-content",
		data : blogVueData,
		methods : {
			/**
			 * 글보기 페이지 이동
			 */
			getViewUrl : function(articleId) {
				return $.i18n.prop("url.blog.view.view", this.blogId, articleId);
			}
		},
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
					};
				},
				methods : {
					/**
					 * 글목록 페이지 이동
					 */
					getListUrl : function(page) {
						console.log("getListUrl : ", this.$parent.blogId);
						location.href = $.i18n.prop("url.blog.view.list", this.$parent.blogId) + "?" + $.param({ page : page });
					},
					/**
					 * 글 목록 조회 ajax 호출
					 */
					getArticleListResponse : function() {
						var _this = this;
						$.ajax({
							type : "GET",
							url : $.i18n.prop("url.article.api.get-list"),
							data : { id : this.$parent.blogId }
						}).done(function(response) {
							_this.articleListResponse = response;
						});
					},
					/**
					 * 글 목록 조회
					 */
					getArticleList : function() {
						if (this.articleListResponse._embedded == undefined || this.articleListResponse._embedded.blogArticles == undefined) {
							return [];
						}
						return this.articleListResponse._embedded.blogArticles;
					}
				},
				mounted : function() {
					this.getArticleListResponse();
				},
				computed : {
					
				},
				components : {
					"blog-write-button" : {
						template : '\
							<div>\
								버튼버튼버튼\
							</div>'
					}		
				}
			}
			
		}
	});
	console.log("blogVue : ", blogVue);
});