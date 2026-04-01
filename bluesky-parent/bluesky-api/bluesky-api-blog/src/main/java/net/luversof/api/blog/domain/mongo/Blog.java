package net.luversof.api.blog.domain.mongo;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Blog implements net.luversof.api.blog.domain.Blog<ObjectId> {

    @Id private ObjectId id;

    private String testText;

    @CreatedDate private LocalDateTime createdDate = LocalDateTime.now();

    @CreatedBy private String createBy;

    @LastModifiedDate private LocalDateTime lastModifiedDate;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTestText() {
        return testText;
    }

    public void setTestText(String testText) {
        this.testText = testText;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blog blog = (Blog) o;
        return java.util.Objects.equals(id, blog.id)
                && java.util.Objects.equals(testText, blog.testText)
                && java.util.Objects.equals(createdDate, blog.createdDate)
                && java.util.Objects.equals(createBy, blog.createBy)
                && java.util.Objects.equals(lastModifiedDate, blog.lastModifiedDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, testText, createdDate, createBy, lastModifiedDate);
    }

    @Override
    public String toString() {
        return "Blog(id="
                + id
                + ", testText="
                + testText
                + ", createdDate="
                + createdDate
                + ", createBy="
                + createBy
                + ", lastModifiedDate="
                + lastModifiedDate
                + ")";
    }
}
