package net.luversof.api.stock.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockPredictionGrade;

public interface StockPredictionGradeRepository extends CrudRepository<StockPredictionGrade, UUID> {

  List<StockPredictionGrade> findByPredictionIdIn(Collection<UUID> predictionIds);
}
