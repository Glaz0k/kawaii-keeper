package ru.spbstu.ssa.kawaiikeeper.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.spbstu.ssa.kawaiikeeper.dto.ImageDto;
import ru.spbstu.ssa.kawaiikeeper.dto.SavedDto;
import ru.spbstu.ssa.kawaiikeeper.entity.Saved;
import ru.spbstu.ssa.kawaiikeeper.repository.SavedRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedServiceTest {

    @Mock
    private SavedRepository savedRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private SavedService savedService;

    @Test
    void saveImage_withExternalId_shouldGetImageAndSave() {
        long userId = 123L;
        String externalId = "img_123";
        ImageDto imageDto = new ImageDto(externalId, "https://example.com/image.jpg", "cats");

        when(imageService.getByExternalId(externalId)).thenReturn(imageDto);
        when(savedRepository.save(any(Saved.class))).thenAnswer(invocation -> {
            Saved saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        savedService.saveImage(userId, externalId);

        verify(imageService).getByExternalId(externalId);
        verify(savedRepository).save(any(Saved.class));
    }

    @Test
    void saveImage_withExternalId_whenImageServiceThrowsException_shouldPropagate() {
        long userId = 123L;
        String externalId = "img_123";

        when(imageService.getByExternalId(externalId)).thenThrow(new RuntimeException("Image not found"));

        assertThrows(RuntimeException.class, () -> savedService.saveImage(userId, externalId));

        verify(imageService).getByExternalId(externalId);
        verify(savedRepository, never()).save(any(Saved.class));
    }

    @Test
    void saveImage_withImageDto_shouldSaveCorrectly() {
        long userId = 123L;
        ImageDto imageDto = new ImageDto("img_456", "https://example.com/image2.jpg", "dogs");

        when(savedRepository.save(any(Saved.class))).thenAnswer(invocation -> {
            Saved saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        savedService.saveImage(userId, imageDto);

        verify(savedRepository).save(any(Saved.class));
        verify(imageService, never()).getByExternalId(anyString());
    }

    @Test
    void saveImage_withImageDto_shouldCreateCorrectSavedEntity() {
        long userId = 456L;
        ImageDto imageDto = new ImageDto("ext_789", "https://example.com/photo.jpg", "nature");

        savedService.saveImage(userId, imageDto);

        verify(savedRepository).save(argThat(saved ->
            saved.getUserId() == userId &&
                saved.getExternalId().equals(imageDto.externalId()) &&
                saved.getImageUrl().equals(imageDto.imageUrl()) &&
                saved.getCategoryName().equals(imageDto.categoryName()) &&
                saved.getId() == null
        ));
    }

    @Test
    void findOrderedImages_shouldReturnOrderedList() {
        long userId = 123L;
        Instant now = Instant.now();

        List< Saved > savedEntities = List.of(
            new Saved(1L, userId, "img1", "https://example.com/1.jpg", "cat1", now.minus(2, ChronoUnit.DAYS)),
            new Saved(2L, userId, "img2", "https://example.com/2.jpg", "cat2", now.minus(1, ChronoUnit.DAYS)),
            new Saved(3L, userId, "img3", "https://example.com/3.jpg", "cat3", now)
        );

        when(savedRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(savedEntities);

        List< SavedDto > result = savedService.findOrderedImages(userId);

        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(1L, result.get(0).id());
        assertEquals(2L, result.get(1).id());
        assertEquals(3L, result.get(2).id());

        SavedDto firstDto = result.get(0);
        assertEquals(1L, firstDto.id());
        assertEquals(userId, firstDto.userId());
        assertEquals("img1", firstDto.externalId());
        assertEquals("https://example.com/1.jpg", firstDto.imageUrl());
        assertEquals("cat1", firstDto.categoryName());
        assertEquals(now.minus(2, ChronoUnit.DAYS), firstDto.createdAt());

        verify(savedRepository).findByUserIdOrderByCreatedAtAsc(userId);
    }

    @Test
    void findOrderedImages_whenNoImages_shouldReturnEmptyList() {
        long userId = 123L;

        when(savedRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());

        List< SavedDto > result = savedService.findOrderedImages(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(savedRepository).findByUserIdOrderByCreatedAtAsc(userId);
    }

    @Test
    void hasImages_whenImagesExist_shouldReturnTrue() {
        long userId = 123L;

        when(savedRepository.existsByUserId(userId)).thenReturn(true);

        boolean result = savedService.hasImages(userId);

        assertTrue(result);
        verify(savedRepository).existsByUserId(userId);
    }

    @Test
    void hasImages_whenNoImages_shouldReturnFalse() {
        long userId = 123L;

        when(savedRepository.existsByUserId(userId)).thenReturn(false);

        boolean result = savedService.hasImages(userId);

        assertFalse(result);
        verify(savedRepository).existsByUserId(userId);
    }

    @Test
    void removeImage_shouldCallRepositoryDelete() {
        long imageId = 456L;

        savedService.removeImage(imageId);

        verify(savedRepository).deleteById(imageId);
    }

    @Test
    void clearImages_shouldCallRepositoryDeleteByUserId() {
        long userId = 123L;

        savedService.clearImages(userId);

        verify(savedRepository).deleteByUserId(userId);
    }

    @Test
    void findOrderedImages_shouldMapAllFieldsCorrectly() {
        long userId = 999L;
        Instant createdAt = Instant.now();

        Saved savedEntity = new Saved(99L, userId, "ext_999", "https://example.com/99.jpg", "test_cat", createdAt);

        when(savedRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(savedEntity));

        List< SavedDto > result = savedService.findOrderedImages(userId);

        assertEquals(1, result.size());
        SavedDto dto = result.get(0);

        assertEquals(99L, dto.id());
        assertEquals(userId, dto.userId());
        assertEquals("ext_999", dto.externalId());
        assertEquals("https://example.com/99.jpg", dto.imageUrl());
        assertEquals("test_cat", dto.categoryName());
        assertEquals(createdAt, dto.createdAt());
    }

    @Test
    void saveImage_shouldNotSetIdBeforeSaving() {
        long userId = 123L;
        ImageDto imageDto = new ImageDto("test", "http://example.com/test.jpg", "test");

        savedService.saveImage(userId, imageDto);

        verify(savedRepository).save(argThat(saved ->
            saved.getId() == null // ID должен быть null перед сохранением в БД
        ));
    }

    @Test
    void saveImage_withExternalId_shouldHandleImageServiceResponse() {
        long userId = 123L;
        String externalId = "external_123";
        ImageDto expectedImage = new ImageDto(externalId, "http://example.com/image.jpg", "category");

        when(imageService.getByExternalId(externalId)).thenReturn(expectedImage);

        savedService.saveImage(userId, externalId);

        verify(savedRepository).save(argThat(saved ->
            saved.getExternalId().equals(expectedImage.externalId()) &&
                saved.getImageUrl().equals(expectedImage.imageUrl()) &&
                saved.getCategoryName().equals(expectedImage.categoryName())
        ));
    }

    @Test
    void integration_saveAndFind_shouldWorkTogether() {
        long userId = 123L;
        ImageDto imageDto = new ImageDto("img_123", "http://example.com/image.jpg", "cats");

        savedService.saveImage(userId, imageDto);
        verify(savedRepository).save(any(Saved.class));

        when(savedRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        List< SavedDto > result = savedService.findOrderedImages(userId);
        assertNotNull(result);

        assertTrue(result.isEmpty());
    }

    @Test
    void removeImage_withZeroId_shouldWork() {
        long imageId = 0L;

        savedService.removeImage(imageId);

        verify(savedRepository).deleteById(0L);
    }

    @Test
    void clearImages_withZeroUserId_shouldWork() {
        long userId = 0L;

        savedService.clearImages(userId);

        verify(savedRepository).deleteByUserId(0L);
    }

    @Test
    void hasImages_withZeroUserId_shouldCheckRepository() {
        long userId = 0L;

        when(savedRepository.existsByUserId(userId)).thenReturn(false);

        boolean result = savedService.hasImages(userId);

        assertFalse(result);
        verify(savedRepository).existsByUserId(0L);
    }
}