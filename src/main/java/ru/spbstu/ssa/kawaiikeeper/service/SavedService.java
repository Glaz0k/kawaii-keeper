package ru.spbstu.ssa.kawaiikeeper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.dto.ImageDto;
import ru.spbstu.ssa.kawaiikeeper.entity.Saved;
import ru.spbstu.ssa.kawaiikeeper.repository.SavedRepository;

@RequiredArgsConstructor
@Service
public class SavedService {

    private final SavedRepository savedRepository;
    private final ImageService imageService;

    public void saveImage(long userId, String externalId) {
        ImageDto image = imageService.getByExternalId(externalId);
        saveImage(userId, image);
    }

    public void saveImage(long userId, @NonNull ImageDto image) {
        savedRepository.save(new Saved(
            null,
            userId,
            image.externalId(),
            image.imageUrl(),
            image.categoryName(),
            null)
        );
    }
}
