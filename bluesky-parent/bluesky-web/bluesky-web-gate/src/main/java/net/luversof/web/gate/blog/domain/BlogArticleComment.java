package net.luversof.web.gate.blog.domain;

import java.time.ZonedDateTime;

public record BlogArticleComment(
        long idx,
        String blogArticleCommentId,
        String blogArticleId,
        String userId,
        String comment,
        ZonedDateTime createdDate,
        ZonedDateTime lastModifiedDate) {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private long idx;
        private String blogArticleCommentId;
        private String blogArticleId;
        private String userId;
        private String comment;
        private ZonedDateTime createdDate;
        private ZonedDateTime lastModifiedDate;

        public Builder() {}

        public Builder(BlogArticleComment blogArticleComment) {
            this.idx = blogArticleComment.idx();
            this.blogArticleCommentId = blogArticleComment.blogArticleCommentId();
            this.blogArticleId = blogArticleComment.blogArticleId();
            this.userId = blogArticleComment.userId();
            this.comment = blogArticleComment.comment();
            this.createdDate = blogArticleComment.createdDate();
            this.lastModifiedDate = blogArticleComment.lastModifiedDate();
        }

        public Builder idx(long idx) {
            this.idx = idx;
            return this;
        }

        public Builder blogArticleCommentId(String blogArticleCommentId) {
            this.blogArticleCommentId = blogArticleCommentId;
            return this;
        }

        public Builder blogArticleId(String blogArticleId) {
            this.blogArticleId = blogArticleId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
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

        public BlogArticleComment build() {
            return new BlogArticleComment(
                    idx,
                    blogArticleCommentId,
                    blogArticleId,
                    userId,
                    comment,
                    createdDate,
                    lastModifiedDate);
        }
    }
}
