package com.rivalcode.submissionservice.client;

import com.rivalcode.contracts.problems.model.ProblemExecutionContextDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProblemServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.problem-service.base-url:http://localhost:8082}")
    private String problemServiceBaseUrl;

    public ProblemExecutionContextDto getExecutionContext(UUID problemId, UUID problemVersionId) {
        String uri = UriComponentsBuilder
                .fromHttpUrl(problemServiceBaseUrl)
                .path("/internal/problems/{problemId}/execution-context")
                .queryParamIfPresent("problemVersionId", java.util.Optional.ofNullable(problemVersionId))
                .buildAndExpand(problemId)
                .toUriString();

        return restClientBuilder.build()
                .get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new ResponseStatusException(
                            response.getStatusCode(),
                            "Problem execution context is unavailable"
                    );
                })
                .body(ProblemExecutionContextDto.class);
    }
}
