package net.luversof.web.gate.blog.domain;

import java.time.ZonedDateTime;
import java.util.List;

public record BlogArticle(long idx, String blogArticleId, String blogId, String userId,
		BlogArticleCategory blogArticleCategory, List<BlogArticleComment> blogArticleCommentList, String title,
		String content, ZonedDateTime createdDate, ZonedDateTime lastModifiedDate) {

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static class Builder {
		private long idx;
		private String blogArticleId;
		private String blogId;
		private String userId;
		private BlogArticleCategory blogArticleCategory;
		private List<BlogArticleComment> blogArticleCommentList;
		private String title;
		private String content;
		private ZonedDateTime createdDate;
		private ZonedDateTime lastModifiedDate;

		public Builder() {
		}

		public Builder(BlogArticle blogArticle) {
			this.idx = blogArticle.idx();
			this.blogArticleId = blogArticle.blogArticleId();
			this.blogId = blogArticle.blogId();
			this.userId = blogArticle.userId();
			this.blogArticleCategory = blogArticle.blogArticleCategory();
			this.blogArticleCommentList = blogArticle.blogArticleCommentList();
			this.title = blogArticle.title();
			this.content = blogArticle.content();
			this.createdDate = blogArticle.createdDate();
			this.lastModifiedDate = blogArticle.lastModifiedDate();
		}

		public Builder idx(long idx) {
			this.idx = idx;
			return this;
		}

		public Builder blogArticleId(String blogArticleId) {
			this.blogArticleId = blogArticleId;
			return this;
		}

		public Builder blogId(String blogId) {
			this.blogId = blogId;
			return this;
		}

		public Builder userId(String userId) {
			this.userId = userId;
			return this;
		}

		public Builder blogArticleCategory(BlogArticleCategory blogArticleCategory) {
			this.blogArticleCategory = blogArticleCategory;
			return this;
		}

		public Builder blogArticleCommentList(List<BlogArticleComment> blogArticleCommentList) {
			this.blogArticleCommentList = blogArticleCommentList;
			return this;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder content(String content) {
			this.content = content;
			return this;
		}

		public Builder createdDate(ZonedDateTime createdDate) {
			this.createdDate = createdDate;
			return this;
		}

		public Builder lastModifiedDate(ZonedDateTime lastModifiedDate) {
			this.lastModifiedDate = lastModifiedDate;
			return this;
		}

		public BlogArticle build() {
			return new BlogArticle(idx, blogArticleId, blogId, userId, blogArticleCategory, blogArticleCommentList,
					title, content, createdDate, lastModifiedDate);
		}
	}
}
