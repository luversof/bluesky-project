$(document).ready(function() {
	 
	/**
	 * blog에서 사용하는 공통 data
	 */
	blogVueData = {
		blogId : /\/blog\/([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})\/\w*/.exec(location.pathname)[1],
		userInfo : userInfo
	};
	
	/**
	 * blog에서 사용하는 공통 method
	 */
	var blogMixin = Vue.extend({
		data : function() { 
			var _data = {
				blog : {},
				categoryListResponse : {}
			} 
			$.extend(_data, blogVueData);
			return _data;
		},
		mixins : [commonMixin],
		methods : {
			getViewUrl : function(articleId) {
				return $.i18n.prop("url.blog.view.view", this.blogId, articleId);
			},
			getWriteUrl : function() {
				return $.i18n.prop("url.blog.view.write", this.blogId);
			},
			moveListView : function(page) {
				location.href = $.i18n.prop("url.blog.view.list", this.blogId) + (page == undefined ? "" : "?" + $.param({ page : page }));
			},
			/**
			 * 블로그 조회 ajax 호출
			 */
			getBlogResponse : function() {
				var _this = this;
				$.ajax({
					type : "GET",
					url : $.i18n.prop("url.blog.api.get", this.blogId),
				}).done(function(response) {
					_this.blog = response;
				});
			},
			/**
			 * 카테고리 목록 조회 ajax 호출
			 */
			getCategoryListResponse : function() {
				var _this = this;
				$.ajax({
					type : "GET",
					url : $.i18n.prop("url.category.api.get-list"),
					data : { id : this.blogId }
				}).done(function(response) {
					_this.categoryListResponse = response;
				});
			},
			/**
			 * 글 목록 조회
			 */
			getCategoryList : function() {
				if (this.categoryListResponse._embedded == undefined || this.categoryListResponse._embedded.blogArticles == undefined) {
					return [];
				}
				return this.categoryListResponse._embedded.blogArticles;
			}
		},
	});
	
	var blogWriteButton = Vue.extend({
		data : function() { 
			return $.extend({}, blogVueData);
		},
		mixins : [blogMixin],
		template : '\
			<div v-if="userInfo.isLogin()">\
				<a :href="getWriteUrl()" class="btn btn-outline-primary">{{i18n("blog.menu.write")}}</a>\
			</div>'
	});
	
	var blogCreate = Vue.extend({
		data : function() { 
			return blogVueData 
		},
		mixins : [blogMixin],
		methods : {
			/**
			 * blog 생성
			 */
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
	});
	
	var blogWrite = Vue.extend({
		data : function() {
			var _data = {
				content : "내용",
				title : "제목"
			}
			$.extend(_data, blogVueData);
			return _data;
		},
		mixins : [blogMixin],
		methods : {
			write : function(event) {
				var _this = this;
				$.ajax({
					type : "POST",
					url : $.i18n.prop("url.blog-article.api.create"),
					contentType:"application/hal+json; charset=utf-8",
					data : JSON.stringify({
						blog : this.blog._links.self.href,
						title : this.title,
						content : this.content
					}),
					success : function(data) {
						location.href = _this.getViewUrl(data.id);
					}
				});
			}
		},
		mounted : function() {
			this.getBlogResponse();
			this.getCategoryListResponse();
		},
	});
	
	var blogList = Vue.extend({
		data : function() {
			var _data = {
				articleListResponse : {}
			};
			$.extend(_data, blogVueData);
			return _data;
		},
		mixins : [blogMixin],
		methods : {
			/**
			 * 글 목록 조회 ajax 호출
			 */
			getArticleListResponse : function() {
				var _this = this;
				$.ajax({
					type : "GET",
					url : $.i18n.prop("url.blog-article.api.get-list"),
					data : { id : this.blogId }
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
		components : {
			"blog-write-button" : blogWriteButton
		}
	});
	
	var blogView = Vue.extend({
		data : function() {
			return blogVueData;
		},
		mixins : [blogMixin],
	});
	
	var blogVue = new Vue({
		el : "#blog-content",
		data : blogVueData,
		components : {
			"blog-create" : blogCreate,
			"blog-write" : blogWrite,
			"blog-list" : blogList,
			"blog-view" : blogView
		}
	});
	console.log("blogVue : ", blogVue);
});