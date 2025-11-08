package ru.spbstu.ssa.kawaiikeeper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.spbstu.ssa.kawaiikeeper.dto.ImageDto;
import ru.spbstu.ssa.kawaiikeeper.entity.Category;

@RequiredArgsConstructor
@Service
public class ImageService {

    private static final String POLL_TEMPLATE = "/images/%s?session=id&id=%d&count=1";
    private static final String GET_TEMPLATE = "/getImageById/%s";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public @NonNull ImageDto pollNext(@NonNull Category category) {
        String rawResponse = getRawResponseFrom(POLL_TEMPLATE.formatted(category.getCategoryName(), category.getUserId()));
        return parseApiResponse(rawResponse);
    }

    public @NonNull ImageDto getByExternalId(@NonNull String externalId) {
        String rawResponse = getRawResponseFrom(GET_TEMPLATE.formatted(externalId));
        return parseApiResponse(rawResponse);
    }

    private String getRawResponseFrom(String preparedUri) {
        return restClient
            .get()
            .uri(preparedUri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
                throw new RuntimeException("HTTP error: " + res.getStatusCode());
            })
            .body(String.class);
    }

    private ImageDto parseApiResponse(String rawResponse) {
        try {
            JsonNode parsedResponse = objectMapper.readTree(rawResponse);
            String id = parsedResponse.get("id").asText();
            String category = parsedResponse.get("category").asText();
            String url = parsedResponse.get("image").get("compressed").get("url").asText();
            return new ImageDto(id, url, category);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}