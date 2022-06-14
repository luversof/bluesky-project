package net.luversof.blog.domain.mysql;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Data;

/**
 * BlogArticle에 대한 댓글
 * 댓글은 무조건 Article에 종속인게 좋은 걸까? *
 * 어차피 서비스가 한 묶음이니 종속으로 구현하고 별도 구현이 필요한 경우 따로 고민 
 * @author luver
 *
 */
@Data
@Entity
public class BlogArticleComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@NotBlank(groups = { Get.class, Update.class, Delete.class })
	@Column(length = 36, nullable = false, unique = true)
	private String blogArticleCommentId;
	
	@NotBlank(groups = { Create.class, CreateParam.class, Update.class, Delete.class })
	@Column(name = "blogArticle_id", length = 36, nullable = false)
	private String blogArticleId;
	
	@NotBlank(groups = { Create.class, CreateParam.class, Update.class })
	private String comment;
	
	@CreatedDate
	private ZonedDateTime createdDate;

	@LastModifiedDate
	private ZonedDateTime lastModifiedDate;
	
	@NotBlank(groups = { Create.class, Update.class, Delete.class })
	@Column(name = "user_id", length = 36, nullable = false)
	private String userId;
	
	public interface Get {
	}

    public interface Create {
	}
    
    public interface CreateParam {
	}

    public interface Update {
	}

    public interface Delete {
	}
}
