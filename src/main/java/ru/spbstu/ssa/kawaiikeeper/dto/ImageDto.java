package ru.spbstu.ssa.kawaiikeeper.dto;

import jakarta.annotation.Nonnull;

public record ImageDto(
    @Nonnull
    String externalId,

    @Nonnull
    String imageUrl,

    @Nonnull
    String categoryName
) {

}
