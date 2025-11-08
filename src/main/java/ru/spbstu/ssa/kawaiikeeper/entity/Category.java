package ru.spbstu.ssa.kawaiikeeper.entity;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public final class Category {

    @Id
    @Nonnull
    private Long userId;

    @Nonnull
    private String categoryName;

}
