package net.luversof.web.gate.blog.domain;

import java.time.ZonedDateTime;
import java.util.List;

public record Blog(
    long idx,
    String blogId,
    String userId,
    List<BlogArticleCategory> blogArticleCategoryList,
    ZonedDateTime createdDate) {

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  public static class Builder {
    private long idx;
    private String blogId;
    private String userId;
    private List<BlogArticleCategory> blogArticleCategoryList;
    private ZonedDateTime createdDate;

    public Builder() {}

    public Builder(Blog blog) {
      this.idx = blog.idx();
      this.blogId = blog.blogId();
      this.userId = blog.userId();
      this.blogArticleCategoryList = blog.blogArticleCategoryList();
      this.createdDate = blog.createdDate();
    }

    public Builder idx(long idx) {
      this.idx = idx;
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

    public Builder blogArticleCategoryList(List<BlogArticleCategory> blogArticleCategoryList) {
      this.blogArticleCategoryList = blogArticleCategoryList;
      return this;
    }

    public Builder createdDate(ZonedDateTime createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public Blog build() {
      return new Blog(idx, blogId, userId, blogArticleCategoryList, createdDate);
    }
  }
}
