package com.isp.lg.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WorkerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${looking-glass.worker.url:http://localhost:8000}")
    private String workerBaseUrl;

    public WorkerTaskResponse execute(WorkerTaskRequest request) {
        String url = workerBaseUrl + "/execute";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WorkerTaskRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(url, entity, WorkerTaskResponse.class);
    }
}
