package net.luversof.blog.domain.mongo;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
public class Blog {

	@Id 
	private String id;
	
	private String testText;
	
}
