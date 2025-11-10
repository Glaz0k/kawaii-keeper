package ru.spbstu.ssa.kawaiikeeper.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.dto.ImageDto;
import ru.spbstu.ssa.kawaiikeeper.dto.SavedDto;
import ru.spbstu.ssa.kawaiikeeper.entity.Saved;
import ru.spbstu.ssa.kawaiikeeper.repository.SavedRepository;

import java.util.List;

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

    public List< SavedDto > findOrderedImages(long userId) {
        return savedRepository.findByUserIdOrderByCreatedAtAsc(userId)
            .stream()
            .map(saved -> new SavedDto(
                saved.getId(),
                saved.getUserId(),
                saved.getExternalId(),
                saved.getImageUrl(),
                saved.getCategoryName(),
                saved.getCreatedAt()
            ))
            .toList();
    }

    public boolean hasImages(long userId) {
        return savedRepository.existsByUserId(userId);
    }

    public void removeImage(long id) {
        savedRepository.deleteById(id);
    }

    @Transactional
    public void clearImages(long userId) {
        savedRepository.deleteByUserId(userId);
    }

}
