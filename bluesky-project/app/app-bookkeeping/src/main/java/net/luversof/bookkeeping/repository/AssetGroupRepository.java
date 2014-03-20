package net.luversof.bookkeeping.repository;

import java.util.List;

import net.luversof.bookkeeping.domain.AssetGroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetGroupRepository extends JpaRepository<AssetGroup, Long>, QueryDslPredicateExecutor<AssetGroup> {
	List<AssetGroup> findByUserId(int id);
}
