package net.luversof.api.blog.domain.mariadb;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(
    indexes = {
      @Index(
          name = "UK_blogArticleCategory_blogArticleCategoryId",
          columnList = "blogArticleCategoryId",
          unique = true),
      @Index(name = "IDX_blogArticleCategory_blogId", columnList = "blog_id")
    })
public class BlogArticleCategory implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long idx;

  @NotBlank(groups = {Update.class, Delete.class})
  @Column(length = 36, nullable = false)
  private String blogArticleCategoryId;

  @NotBlank(groups = Create.class)
  @Column(name = "blog_id", length = 36, nullable = false)
  private String blogId;

  @NotBlank(groups = {Create.class, Update.class})
  @Column(length = 64, nullable = false)
  private String name;

  public interface Create {}

  public interface Update {}

  public interface Delete {}

  public long getIdx() {
    return idx;
  }

  public void setIdx(long idx) {
    this.idx = idx;
  }

  public String getBlogArticleCategoryId() {
    return blogArticleCategoryId;
  }

  public void setBlogArticleCategoryId(String blogArticleCategoryId) {
    this.blogArticleCategoryId = blogArticleCategoryId;
  }

  public String getBlogId() {
    return blogId;
  }

  public void setBlogId(String blogId) {
    this.blogId = blogId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BlogArticleCategory that = (BlogArticleCategory) o;
    return idx == that.idx
        && java.util.Objects.equals(blogArticleCategoryId, that.blogArticleCategoryId)
        && java.util.Objects.equals(blogId, that.blogId)
        && java.util.Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(idx, blogArticleCategoryId, blogId, name);
  }

  @Override
  public String toString() {
    return "BlogArticleCategory(idx="
        + idx
        + ", blogArticleCategoryId="
        + blogArticleCategoryId
        + ", blogId="
        + blogId
        + ", name="
        + name
        + ")";
  }
}
