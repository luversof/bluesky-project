package net.luversof.web.dynamiccrud2.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.domain.Menu;

public interface MenuRepository extends ListCrudRepository<Menu, UUID> {

  List<Menu> findByMainMenuIdOrderByDisplayOrderAscNameAsc(UUID mainMenuId);

  Optional<Menu> findByMainMenuIdAndCode(UUID mainMenuId, String code);
}
