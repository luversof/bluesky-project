package net.luversof.web.dynamiccrud2.setting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.setting.domain.Project;

public interface ProjectRepository extends ListCrudRepository<Project, UUID> {

  List<Project> findAllByOrderByDisplayOrderAscNameAsc();
}
