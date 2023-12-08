package net.luversof.web.dynamiccrud.setting.domain;

import java.time.ZonedDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class Setting {
	
	@Column(length = 40)
	private String operator;
	
	@CreationTimestamp
	private ZonedDateTime registerDate;

	@UpdateTimestamp
	private ZonedDateTime modifyDate; 

}
