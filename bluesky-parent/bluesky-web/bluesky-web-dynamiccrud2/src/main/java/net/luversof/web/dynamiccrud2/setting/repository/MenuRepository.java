package net.luversof.web.dynamiccrud2.setting.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.setting.domain.Menu;

public interface MenuRepository extends ListCrudRepository<Menu, UUID> {

  List<Menu> findByMainMenuIdOrderByDisplayOrderAscNameAsc(UUID mainMenuId);
}
