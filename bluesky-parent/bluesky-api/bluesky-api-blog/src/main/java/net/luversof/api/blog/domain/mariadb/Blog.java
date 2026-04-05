package net.luversof.api.blog.domain.mariadb;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * blog 정보
 *
 * @author luver
 */
@Entity
@Table(
    indexes = {
      @Index(name = "UK_blog_blogId", columnList = "blogId", unique = true),
      @Index(name = "IDX_blog_userId", columnList = "user_id")
    })
public class Blog implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long idx;

  @Column(length = 36, nullable = false)
  private String blogId;

  @NotBlank(groups = Create.class)
  @Column(name = "user_id", length = 36, nullable = false)
  private String userId;

  @OneToMany(mappedBy = "blogId")
  private List<BlogArticleCategory> blogArticleCategoryList;

  @CreationTimestamp private ZonedDateTime createdDate;

  public interface Create {}

  public long getIdx() {
    return idx;
  }

  public void setIdx(long idx) {
    this.idx = idx;
  }

  public String getBlogId() {
    return blogId;
  }

  public void setBlogId(String blogId) {
    this.blogId = blogId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public List<BlogArticleCategory> getBlogArticleCategoryList() {
    return blogArticleCategoryList;
  }

  public void setBlogArticleCategoryList(List<BlogArticleCategory> blogArticleCategoryList) {
    this.blogArticleCategoryList = blogArticleCategoryList;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Blog blog = (Blog) o;
    return idx == blog.idx
        && java.util.Objects.equals(blogId, blog.blogId)
        && java.util.Objects.equals(userId, blog.userId)
        && java.util.Objects.equals(blogArticleCategoryList, blog.blogArticleCategoryList)
        && java.util.Objects.equals(createdDate, blog.createdDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(idx, blogId, userId, blogArticleCategoryList, createdDate);
  }

  @Override
  public String toString() {
    return "Blog(idx="
        + idx
        + ", blogId="
        + blogId
        + ", userId="
        + userId
        + ", blogArticleCategoryList="
        + blogArticleCategoryList
        + ", createdDate="
        + createdDate
        + ")";
  }
}
