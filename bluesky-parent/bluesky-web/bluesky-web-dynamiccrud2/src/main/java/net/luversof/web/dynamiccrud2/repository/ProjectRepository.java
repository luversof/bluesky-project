package net.luversof.web.dynamiccrud2.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.domain.Project;

public interface ProjectRepository extends ListCrudRepository<Project, UUID> {

  List<Project> findAllByOrderByDisplayOrderAscNameAsc();

  Optional<Project> findByCode(String code);
}
