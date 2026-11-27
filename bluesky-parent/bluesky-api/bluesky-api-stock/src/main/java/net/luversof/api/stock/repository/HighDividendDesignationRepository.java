package net.luversof.api.stock.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.HighDividendDesignation;

public interface HighDividendDesignationRepository
    extends CrudRepository<HighDividendDesignation, UUID> {}
