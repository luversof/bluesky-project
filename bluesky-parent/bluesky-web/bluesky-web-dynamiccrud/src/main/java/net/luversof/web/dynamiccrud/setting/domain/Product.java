package net.luversof.web.dynamiccrud.setting.domain;

import groovy.transform.EqualsAndHashCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "Products")
public class Product extends SettingClass {
	
	@Id
	@Column(length = 20)
	private String product;
	
	private String productName;

}