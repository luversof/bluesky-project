package net.luversof.api.blog.domain.mariadb;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * BlogArticle에 대한 댓글 댓글은 무조건 Article에 종속인게 좋은 걸까? * 어차피 서비스가 한 묶음이니 종속으로 구현하고 별도 구현이 필요한 경우 따로 고민
 *
 * @author luver
 */
@Entity
@Table(
        indexes = {
            @Index(
                    name = "UK_blogArticleComment_blogArticleCommentId",
                    columnList = "blogArticleCommentId",
                    unique = true),
            @Index(name = "IDX_blogArticleComment_blogArticleId", columnList = "blogArticle_id"),
            @Index(name = "IDX_blogArticleComment_userId", columnList = "user_id")
        })
public class BlogArticleComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idx;

    @NotBlank(groups = {Get.class, Update.class, Delete.class})
    @Column(length = 36, nullable = false)
    private String blogArticleCommentId;

    @NotBlank(groups = {Create.class, Update.class, Delete.class})
    @Column(name = "blogArticle_id", length = 36, nullable = false)
    private String blogArticleId;

    @NotBlank(groups = {Create.class, Update.class, Delete.class})
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @NotBlank(groups = {Create.class, Update.class})
    private String comment;

    @CreatedDate private ZonedDateTime createdDate;

    @LastModifiedDate private ZonedDateTime lastModifiedDate;

    public interface Get {}

    public interface Create {}

    public interface Update {}

    public interface Delete {}

    public long getIdx() {
        return idx;
    }

    public void setIdx(long idx) {
        this.idx = idx;
    }

    public String getBlogArticleCommentId() {
        return blogArticleCommentId;
    }

    public void setBlogArticleCommentId(String blogArticleCommentId) {
        this.blogArticleCommentId = blogArticleCommentId;
    }

    public String getBlogArticleId() {
        return blogArticleId;
    }

    public void setBlogArticleId(String blogArticleId) {
        this.blogArticleId = blogArticleId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
        BlogArticleComment that = (BlogArticleComment) o;
        return idx == that.idx
                && java.util.Objects.equals(blogArticleCommentId, that.blogArticleCommentId)
                && java.util.Objects.equals(blogArticleId, that.blogArticleId)
                && java.util.Objects.equals(userId, that.userId)
                && java.util.Objects.equals(comment, that.comment)
                && java.util.Objects.equals(createdDate, that.createdDate)
                && java.util.Objects.equals(lastModifiedDate, that.lastModifiedDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                idx,
                blogArticleCommentId,
                blogArticleId,
                userId,
                comment,
                createdDate,
                lastModifiedDate);
    }

    @Override
    public String toString() {
        return "BlogArticleComment(idx="
                + idx
                + ", blogArticleCommentId="
                + blogArticleCommentId
                + ", blogArticleId="
                + blogArticleId
                + ", userId="
                + userId
                + ", comment="
                + comment
                + ", createdDate="
                + createdDate
                + ", lastModifiedDate="
                + lastModifiedDate
                + ")";
    }
}
