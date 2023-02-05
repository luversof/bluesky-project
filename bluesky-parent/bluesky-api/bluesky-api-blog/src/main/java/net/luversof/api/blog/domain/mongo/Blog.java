package net.luversof.api.blog.domain.mongo;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.Data;

@Data
@Document
public class Blog implements net.luversof.api.blog.domain.Blog<ObjectId> {

	@Id 
	private ObjectId id;
	
	private String testText;
	
	@CreatedDate
	private LocalDateTime createdDate = LocalDateTime.now();
	
	@CreatedBy
	private String createBy;
	
	@LastModifiedDate
	private LocalDateTime lastModifiedDate;
	
}
