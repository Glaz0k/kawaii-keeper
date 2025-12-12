package ru.spbstu.ssa.kawaiikeeper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.spbstu.ssa.kawaiikeeper.dto.ImageDto;
import ru.spbstu.ssa.kawaiikeeper.entity.Category;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(ImageService.class)
class ImageServiceTest {

    @Autowired
    private ImageService imageService;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    void pollNext_shouldMakeCorrectRequestAndParseResponse() {
        Category category = new Category(123L, "cats");
        String expectedUrl = "/images/cats?session=id&id=123&count=1";
        String jsonResponse = """
            {
                "id": "img_123",
                "category": "cats",
                "image": {
                    "compressed": {
                        "url": "https://example.com/cat.jpg"
                    }
                }
            }
            """;
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ImageDto result = imageService.pollNext(category);

        assertNotNull(result);
        assertEquals("img_123", result.externalId());
        assertEquals("https://example.com/cat.jpg", result.imageUrl());
        assertEquals("cats", result.categoryName());
        mockServer.verify();
    }

    @Test
    void pollNext_whenApiReturnsError_shouldThrowRuntimeException() {
        Category category = new Category(456L, "dogs");
        String expectedUrl = "/images/dogs?session=id&id=456&count=1";
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withServerError());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> imageService.pollNext(category));
        assertTrue(exception.getMessage().contains("HTTP error"));
        mockServer.verify();
    }

    @Test
    void pollNext_whenApiReturnsNotFound_shouldThrowRuntimeException() {
        Category category = new Category(789L, "birds");
        String expectedUrl = "/images/birds?session=id&id=789&count=1";
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> imageService.pollNext(category));
        assertTrue(exception.getMessage().contains("HTTP error"));
        mockServer.verify();
    }

    @Test
    void getByExternalId_shouldMakeCorrectRequestAndParseResponse() {
        String externalId = "img_456";
        String expectedUrl = "/getImageById/img_456";
        String jsonResponse = """
            {
                "id": "img_456",
                "category": "dogs",
                "image": {
                    "compressed": {
                        "url": "https://example.com/dog.jpg"
                    }
                }
            }
            """;

        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ImageDto result = imageService.getByExternalId(externalId);

        assertNotNull(result);
        assertEquals("img_456", result.externalId());
        assertEquals("https://example.com/dog.jpg", result.imageUrl());
        assertEquals("dogs", result.categoryName());
        mockServer.verify();
    }

    @Test
    void getByExternalId_whenApiReturnsError_shouldThrowRuntimeException() {
        String externalId = "img_999";
        String expectedUrl = "/getImageById/img_999";
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withServerError());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> imageService.getByExternalId(externalId));

        assertTrue(exception.getMessage().contains("HTTP error"));
        mockServer.verify();
    }

    @Test
    void getByExternalId_withSpecialCharacters_shouldEncodeUrlCorrectly() {
        String externalId = "img@123#test";
        String expectedUrl = "/getImageById/img@123#test";
        String jsonResponse = """
            {
                "id": "img@123#test",
                "category": "test",
                "image": {
                    "compressed": {
                        "url": "https://example.com/test.jpg"
                    }
                }
            }
            """;
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ImageDto result = imageService.getByExternalId(externalId);

        assertNotNull(result);
        assertEquals("img@123#test", result.externalId());
        mockServer.verify();
    }

    @Test
    void pollNext_whenJsonResponseIsInvalid_shouldThrowRuntimeException() {
        Category category = new Category(123L, "cats");
        String expectedUrl = "/images/cats?session=id&id=123&count=1";
        String invalidJson = "Invalid JSON response";
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(invalidJson, MediaType.APPLICATION_JSON));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> imageService.pollNext(category));
        assertInstanceOf(JsonProcessingException.class, exception.getCause());
        mockServer.verify();
    }

    @Test
    void pollNext_whenJsonMissingFields_shouldThrowRuntimeException() {
        Category category = new Category(123L, "cats");
        String expectedUrl = "/images/cats?session=id&id=123&count=1";
        String incompleteJson = """
            {
                "id": "img_123"
                // missing category and image fields
            }
            """;
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(incompleteJson, MediaType.APPLICATION_JSON));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> imageService.pollNext(category));
        assertNotNull(exception);
        mockServer.verify();
    }

    @Test
    void getByExternalId_whenJsonMissingRequiredFields_shouldThrowRuntimeException() {
        String externalId = "img_123";
        String expectedUrl = "/getImageById/img_123";
        String jsonMissingImageField = """
            {
                "id": "img_123",
                "category": "cats"
                // missing image field
            }
            """;

        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(jsonMissingImageField, MediaType.APPLICATION_JSON));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> imageService.getByExternalId(externalId));

        assertNotNull(exception);

        mockServer.verify();
    }

    @Test
    void pollNext_shouldParseComplexJsonStructureCorrectly() {
        Category category = new Category(999L, "animals");
        String expectedUrl = "/images/animals?session=id&id=999&count=1";
        String jsonResponse = """
            {
                "id": "complex_id_123",
                "category": "wild_animals",
                "image": {
                    "compressed": {
                        "url": "https://cdn.example.com/images/wild/tiger_compressed.jpg",
                        "width": 800,
                        "height": 600
                    },
                    "original": {
                        "url": "https://cdn.example.com/images/wild/tiger.jpg",
                        "width": 4000,
                        "height": 3000
                    }
                },
                "metadata": {
                    "author": "John Doe",
                    "license": "CC BY 4.0"
                }
            }
            """;
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ImageDto result = imageService.pollNext(category);

        assertNotNull(result);
        assertEquals("complex_id_123", result.externalId());
        assertEquals("https://cdn.example.com/images/wild/tiger_compressed.jpg", result.imageUrl());
        assertEquals("wild_animals", result.categoryName());

        mockServer.verify();
    }

    @Test
    void getByExternalId_withEmptyCategoryName_shouldWork() {
        String externalId = "img_empty";
        String expectedUrl = "/getImageById/img_empty";
        String jsonResponse = """
            {
                "id": "img_empty",
                "category": "",
                "image": {
                    "compressed": {
                        "url": "https://example.com/empty.jpg"
                    }
                }
            }
            """;
        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ImageDto result = imageService.getByExternalId(externalId);

        assertNotNull(result);
        assertEquals("img_empty", result.externalId());
        assertEquals("https://example.com/empty.jpg", result.imageUrl());
        assertEquals("", result.categoryName());
        mockServer.verify();
    }

    @Test
    void pollNext_shouldAcceptJsonWithExtraFields() {
        Category category = new Category(777L, "landscape");
        String expectedUrl = "/images/landscape?session=id&id=777&count=1";
        String jsonWithExtraFields = """
            {
                "id": "landscape_123",
                "category": "mountains",
                "extraField1": "extraValue1",
                "image": {
                    "compressed": {
                        "url": "https://example.com/mountain.jpg",
                        "extraNested": "value"
                    },
                    "extraObject": {
                        "test": "test"
                    }
                },
                "tags": ["mountain", "nature", "landscape"],
                "stats": {
                    "views": 1234,
                    "likes": 567
                }
            }
            """;

        mockServer.expect(requestTo(expectedUrl))
            .andRespond(withSuccess(jsonWithExtraFields, MediaType.APPLICATION_JSON));

        ImageDto result = imageService.pollNext(category);

        assertNotNull(result);
        assertEquals("landscape_123", result.externalId());
        assertEquals("https://example.com/mountain.jpg", result.imageUrl());
        assertEquals("mountains", result.categoryName());
        mockServer.verify();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public RestClient restClient(RestClient.Builder builder) {
            return builder.build();
        }

    }
}