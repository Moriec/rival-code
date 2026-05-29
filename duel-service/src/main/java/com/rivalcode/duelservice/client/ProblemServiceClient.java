package com.rivalcode.duelservice.client;

import com.rivalcode.contracts.problems.model.DuelProblemSelectionRequest;
import com.rivalcode.contracts.problems.model.DuelProblemSelectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class ProblemServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.problem-service.base-url:http://localhost:8082}")
    private String problemServiceBaseUrl;

    public DuelProblemSelectionResponse selectProblem(DuelProblemSelectionRequest request) {
        String uri = UriComponentsBuilder
                .fromHttpUrl(problemServiceBaseUrl)
                .path("/internal/problems/select-for-duel")
                .toUriString();

        return restClientBuilder.build()
                .post()
                .uri(uri)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new ResponseStatusException(
                            response.getStatusCode(),
                            "Duel problem selection is unavailable"
                    );
                })
                .body(DuelProblemSelectionResponse.class);
    }
}
