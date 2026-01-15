package net.luversof.app.google.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.app.google.domain.GoogleIamServiceAccountInfo;

public interface GoogleIamServiceAccountInfoRepository extends CrudRepository<GoogleIamServiceAccountInfo, UUID> {
	Optional<GoogleIamServiceAccountInfo> findByUserId(UUID userId);
}
