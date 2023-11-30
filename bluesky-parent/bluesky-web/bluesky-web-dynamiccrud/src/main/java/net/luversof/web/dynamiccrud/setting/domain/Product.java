package net.luversof.web.dynamiccrud.setting.domain;

import groovy.transform.EqualsAndHashCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Products")
public class Product extends Setting {
	
	@Id
	@Column(length = 20)
	private String product;
	
	private String productName;

}