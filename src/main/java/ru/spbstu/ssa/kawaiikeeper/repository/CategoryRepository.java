package ru.spbstu.ssa.kawaiikeeper.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.spbstu.ssa.kawaiikeeper.entity.Category;

@Repository
public interface CategoryRepository extends CrudRepository< Category, Long > {

}
