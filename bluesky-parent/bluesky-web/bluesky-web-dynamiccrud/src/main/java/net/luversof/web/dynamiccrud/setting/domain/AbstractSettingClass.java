package net.luversof.web.dynamiccrud.setting.domain;

import java.time.ZonedDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class AbstractSettingClass {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	private String name;
	
	private String operator;
	
	@Column(updatable = false)
	@CreationTimestamp
	private ZonedDateTime registerDate;

	@Column(updatable = false)
	@UpdateTimestamp
	private ZonedDateTime lastModifiedDate; 

}
