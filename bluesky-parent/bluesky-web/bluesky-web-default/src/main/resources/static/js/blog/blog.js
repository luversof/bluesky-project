$(document).ready(function() {
	 
	/**
	 * blog에서 사용하는 공통 data
	 */
	blogVueData = {
		blogId : function() {
			var res = /\/blog\/([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})\/\w*/.exec(location.pathname);
			return res == undefined ? null : res[1];	
		}(),
		blogArticleId : function() {
			var res = /\/blog\/([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})\/view\/(\d+)/.exec(location.pathname);
			return res == undefined ? null : res[2];
		}(),
		userInfo : userInfo
	};
	
	/**
	 * blog에서 사용하는 공통 method
	 */
	var blogMixin = Vue.extend({
		data : function() {
			var _data = {
				blog : {},
				categoryListResponse : {},
				blogArticle : {},
				blogArticleListResponse : {}
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
			},
			getBlogArticleResponse : function() {
				var _this = this;
				$.ajax({
					type : "GET",
					url : $.i18n.prop("url.blog-article.api.get", this.blogArticleId),
				}).done(function(response) {
					_this.blogArticle = response;
				});
			}
		}
	});
	
	var blogArticleWriteButton = Vue.extend({
		data : function() { 
			return $.extend({}, this.$parent.$data);
		},
		mixins : [blogMixin],
		template : '<a v-if="isDisplayButton()" :href="getWriteUrl()" class="btn btn-outline-primary">{{i18n("blog.menu.write")}}</a>',
		methods : {
			isDisplayButton : function() {
				return this.userInfo.isLogin();
			}
		}
	});
	
	var blogArticleModifyButton = Vue.extend({
		data : function() { 
			if (this.$parent.$data.blog == undefined || this.$parent.userInfo == undefined) {
				alert("modifyButton require blog, userInfo data");
			}
			return this.$parent.$data;
		},
		mixins : [blogMixin],
		template : '<a v-if="isDisplayButton()" :href="getModifyUrl()" class="btn btn-outline-primary">{{i18n("blog.menu.modify")}}</a>',
		mounted : function() {
			if (this.blog.id == undefined) {
				this.getBlogResponse();
			}
		},
		methods : {
			isDisplayButton : function() {
				return this.userInfo.isLogin() && this.blog.userId == this.userInfo.getUserId();
			},
			getModifyUrl : function() {
				return $.i18n.prop("url.blog.view.modify", this.blogId, this.blogArticle.id);
			}
		}
	});
	
	var blogArticleDeleteButton = Vue.extend({
		data : function() { 
			if (this.$parent.$data.blog == undefined || this.$parent.userInfo == undefined) {
				alert("deleteButton require blog, userInfo data");
			}
			return this.$parent.$data;
		},
		mixins : [blogMixin],
		template : '<a v-if="isDisplayButton()" href="#none" @click="remove()" class="btn btn-outline-danger">{{i18n("blog.menu.delete")}}</a>',
		mounted : function() {
			if (this.blog.id == undefined) {
				this.getBlogResponse();
			}
		},
		methods : {
			isDisplayButton : function() {
				return this.userInfo.isLogin() && this.blog.userId == this.userInfo.getUserId();
			},
			getModifyUrl : function() {
				return $.i18n.prop("url.blog.view.modify", this.blogId, this.blogArticle.id);
			},
			remove : function() {
				var _this = this;
				$.ajax({
					type : "DELETE",
					url : $.i18n.prop("url.blog-article.api.get", this.blogId, this.blogArticle.id),
				}).done(function(response) {
					_this.moveListView(1);
				});
			}
		}
	});
	
	var blogCreate = Vue.extend({
		data : function() { 
			return this.$parent.$data 
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
	
	var blogArticleWrite = Vue.extend({
		data : function() {
			var _data = {
				content : "내용",
				title : "제목"
			}
			$.extend(_data, this.$parent.$data);
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
	
	var blogArticleList = Vue.extend({
		data : function() {
			return this.$parent.$data;
		},
		mixins : [blogMixin],
		methods : {
			/**
			 * 글 목록 조회 ajax 호출
			 */
			getBlogArticleListResponse : function() {
				var _this = this;
				$.ajax({
					type : "GET",
					url : $.i18n.prop("url.blog-article.api.get-list"),
					data : { id : this.blogId }
				}).done(function(response) {
					_this.blogArticleListResponse = response;
				});
			},
			/**
			 * 글 목록 조회
			 */
			getBlogArticleList : function() {
				if (this.blogArticleListResponse._embedded == undefined || this.blogArticleListResponse._embedded.blogArticles == undefined) {
					return [];
				}
				return this.blogArticleListResponse._embedded.blogArticles;
			}
		},
		mounted : function() {
			this.getBlogArticleListResponse();
			this.getCategoryListResponse();
		},
		components : {
			blogArticleWriteButton : blogArticleWriteButton
		}
	});
	
	var blogArticleView = Vue.extend({
		data : function() {
			return this.$parent.$data;
		},
		mixins : [blogMixin],
		methods : {
		},
		mounted : function() {
			console.log("blogArticleView", this)
			this.getBlogArticleResponse();
		},
		components : {
			blogArticleModifyButton : blogArticleModifyButton,
			blogArticleDeleteButton : blogArticleDeleteButton
		}
	});
	
	var blogVue = new Vue({
		el : "#blog-content",
		data : blogVueData,
		mixins : [blogMixin],
		components : {
			blogCreate : blogCreate,
			blogArticleList : blogArticleList,
			blogArticleWrite : blogArticleWrite,
			blogArticleView : blogArticleView
		}
	});
	console.log("blogVue : ", blogVue);
});