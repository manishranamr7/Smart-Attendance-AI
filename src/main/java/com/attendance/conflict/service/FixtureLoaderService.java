package com.attendance.conflict.service;

import com.attendance.conflict.dto.EventDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FixtureLoaderService {

    private final ObjectMapper objectMapper;

    public List<EventDTO> loadFixtureEvents(String fixtureFilename) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:fixtures/" + fixtureFilename);
            if (!resource.exists()) {
                resource = resolver.getResource("file:fixtures/" + fixtureFilename);
            }
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, new TypeReference<List<EventDTO>>() {});
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load fixture file: " + fixtureFilename, e);
        }
    }
}
