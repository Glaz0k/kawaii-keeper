package ru.spbstu.ssa.kawaiikeeper.dto;

import jakarta.annotation.Nonnull;

import java.time.Instant;

public record SavedDto(
    @Nonnull
    Long id,

    @Nonnull
    Long userId,

    @Nonnull
    String externalId,

    @Nonnull
    String imageUrl,

    @Nonnull
    String categoryName,

    @Nonnull
    Instant createdAt
) {

}
