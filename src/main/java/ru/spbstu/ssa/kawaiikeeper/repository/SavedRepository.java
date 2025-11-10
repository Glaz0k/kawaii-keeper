package ru.spbstu.ssa.kawaiikeeper.repository;

import org.springframework.data.repository.CrudRepository;
import ru.spbstu.ssa.kawaiikeeper.entity.Saved;

import java.util.List;

public interface SavedRepository extends CrudRepository< Saved, Long > {

    List< Saved > findByUserIdOrderByCreatedAtAsc(long userId);

    Boolean existsByUserId(long userId);
}
