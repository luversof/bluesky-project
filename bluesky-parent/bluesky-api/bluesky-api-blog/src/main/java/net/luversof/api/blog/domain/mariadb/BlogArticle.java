package net.luversof.api.blog.domain.mariadb;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        indexes = {
            @Index(
                    name = "UK_blogArticle_blogArticleId",
                    columnList = "blogArticleId",
                    unique = true),
            @Index(name = "IDX_blogArticle_blogId", columnList = "blog_id"),
            @Index(name = "IDX_blogArticle_userId", columnList = "user_id")
        })
public class BlogArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idx;

    @NotBlank(groups = {Get.class, Update.class, Delete.class})
    @Column(length = 36, nullable = false)
    private String blogArticleId;

    @NotBlank(groups = {Create.class})
    @Column(name = "blog_id", length = 36, nullable = false)
    private String blogId;

    @NotBlank(groups = {Create.class, Update.class, Delete.class})
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @OneToOne
    @JoinColumn(name = "blogArticleCategory_id", referencedColumnName = "blogArticleCategoryId")
    private BlogArticleCategory blogArticleCategory;

    @OneToMany(mappedBy = "blogArticleId")
    private List<BlogArticleComment> blogArticleCommentList;

    @NotBlank(groups = {Create.class, Update.class})
    @Size(
            min = 3,
            max = 50,
            groups = {Create.class, Update.class})
    private String title;

    @NotBlank(groups = {Create.class, Update.class})
    // @Column(columnDefinition = "TEXT")
    @Lob
    private String content;

    @CreationTimestamp private ZonedDateTime createdDate;

    @UpdateTimestamp private ZonedDateTime lastModifiedDate;

    public interface Create {}

    public interface Get {}

    public interface Update {}

    public interface Delete {}

    public long getIdx() {
        return idx;
    }

    public void setIdx(long idx) {
        this.idx = idx;
    }

    public String getBlogArticleId() {
        return blogArticleId;
    }

    public void setBlogArticleId(String blogArticleId) {
        this.blogArticleId = blogArticleId;
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

    public BlogArticleCategory getBlogArticleCategory() {
        return blogArticleCategory;
    }

    public void setBlogArticleCategory(BlogArticleCategory blogArticleCategory) {
        this.blogArticleCategory = blogArticleCategory;
    }

    public List<BlogArticleComment> getBlogArticleCommentList() {
        return blogArticleCommentList;
    }

    public void setBlogArticleCommentList(List<BlogArticleComment> blogArticleCommentList) {
        this.blogArticleCommentList = blogArticleCommentList;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(ZonedDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlogArticle that = (BlogArticle) o;
        return idx == that.idx
                && java.util.Objects.equals(blogArticleId, that.blogArticleId)
                && java.util.Objects.equals(blogId, that.blogId)
                && java.util.Objects.equals(userId, that.userId)
                && java.util.Objects.equals(blogArticleCategory, that.blogArticleCategory)
                && java.util.Objects.equals(blogArticleCommentList, that.blogArticleCommentList)
                && java.util.Objects.equals(title, that.title)
                && java.util.Objects.equals(content, that.content)
                && java.util.Objects.equals(createdDate, that.createdDate)
                && java.util.Objects.equals(lastModifiedDate, that.lastModifiedDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                idx,
                blogArticleId,
                blogId,
                userId,
                blogArticleCategory,
                blogArticleCommentList,
                title,
                content,
                createdDate,
                lastModifiedDate);
    }

    @Override
    public String toString() {
        return "BlogArticle(idx="
                + idx
                + ", blogArticleId="
                + blogArticleId
                + ", blogId="
                + blogId
                + ", userId="
                + userId
                + ", blogArticleCategory="
                + blogArticleCategory
                + ", blogArticleCommentList="
                + blogArticleCommentList
                + ", title="
                + title
                + ", content="
                + content
                + ", createdDate="
                + createdDate
                + ", lastModifiedDate="
                + lastModifiedDate
                + ")";
    }
}
