package net.luversof.web.dynamiccrud2.repository;

import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.domain.DbTable;

/** 위치로 id 찾기는 {@link DbTableQueries} */
public interface DbTableRepository extends ListCrudRepository<DbTable, UUID>, DbTableQueries {}
