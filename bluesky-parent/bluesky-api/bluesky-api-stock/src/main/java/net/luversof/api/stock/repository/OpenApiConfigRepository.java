package net.luversof.api.stock.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.OpenApiConfig;

public interface OpenApiConfigRepository extends CrudRepository<OpenApiConfig, UUID> {
	Optional<OpenApiConfig> findByProvider(String provider);
}
