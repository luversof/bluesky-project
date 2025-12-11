package net.luversof.web.gate.blog.domain;

public record BlogArticleCategory(long idx, String blogArticleCategoryId, String blogId, String name) {

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static class Builder {
		private long idx;
		private String blogArticleCategoryId;
		private String blogId;
		private String name;

		public Builder() {
		}

		public Builder(BlogArticleCategory blogArticleCategory) {
			this.idx = blogArticleCategory.idx();
			this.blogArticleCategoryId = blogArticleCategory.blogArticleCategoryId();
			this.blogId = blogArticleCategory.blogId();
			this.name = blogArticleCategory.name();
		}

		public Builder idx(long idx) {
			this.idx = idx;
			return this;
		}

		public Builder blogArticleCategoryId(String blogArticleCategoryId) {
			this.blogArticleCategoryId = blogArticleCategoryId;
			return this;
		}

		public Builder blogId(String blogId) {
			this.blogId = blogId;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public BlogArticleCategory build() {
			return new BlogArticleCategory(idx, blogArticleCategoryId, blogId, name);
		}
	}
}
