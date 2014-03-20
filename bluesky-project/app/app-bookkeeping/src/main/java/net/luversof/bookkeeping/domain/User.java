package net.luversof.bookkeeping.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class User {
	@Id
	int id;
}
