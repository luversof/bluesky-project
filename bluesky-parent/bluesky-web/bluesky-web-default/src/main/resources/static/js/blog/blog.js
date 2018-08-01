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
			var _data = blogVueData;
			_data.categoryListResponse = {};
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
			 * 카테고리 목록 조회 ajax 호출
			 */
			getCategoryListResponse : function() {
				var _this = this;
				console.log("asdfasdf ", this.blogId);
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
			return blogVueData;
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
			var _data = blogVueData;
			return _data;
		},
		mixins : [blogMixin],
		methods : {
			
		},
		mounted : function() {
			this.getCategoryListResponse();
		},
	});
	
	var blogList = Vue.extend({
		data : function() {
			var _data = blogVueData;
			_data.articleListResponse = {};
			return _data;
		},
		mixins : [blogMixin],
		methods : {
			/**
			 * 글 목록 조회 ajax 호출
			 */
			getArticleListResponse : function() {
				console.log("test - ajax call");
				var _this = this;
				$.ajax({
					type : "GET",
					url : $.i18n.prop("url.article.api.get-list"),
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
	
	var blogVue = new Vue({
		el : "#blog-content",
		data : blogVueData,
		components : {
			"blog-create" : blogCreate,
			"blog-write" : blogWrite,
			"blog-list" : blogList
		}
	});
	console.log("blogVue : ", blogVue);
});