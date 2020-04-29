package net.luversof.blog.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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
public class BlogComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@NotNull(groups = { Get.class, Update.class, Delete.class })
	private long id;
	
	@Valid
	@ManyToOne
	@JoinColumn(name = "blogArticle_id", foreignKey = @ForeignKey(name = "FK_blogComment_blogArticleId"))
	@NotNull(groups = { Update.class, Delete.class })
	private BlogArticle blogArticle;
	
	@NotEmpty(groups = { Create.class, Update.class })
	private String comment;
	
	@CreatedDate
	private LocalDateTime createdDate;

	@LastModifiedDate
	private LocalDateTime lastModifiedDate;
	
	@Column(name = "user_id", length = 16, nullable = false)
	private UUID userId;
	
	public interface Get {
	}

    public interface Create {
	}

    public interface Update {
	}

    public interface Delete {
	}
}
