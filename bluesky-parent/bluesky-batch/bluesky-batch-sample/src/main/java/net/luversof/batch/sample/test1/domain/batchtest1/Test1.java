package net.luversof.batch.sample.test1.domain.batchtest1;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Test1 {

	@Id
	private long id;
	
	private String testValue;
}
