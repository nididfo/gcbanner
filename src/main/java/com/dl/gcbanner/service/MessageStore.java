package com.dl.gcbanner.service;

import com.dl.gcbanner.model.SiteMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageStore {

    private final Path filePath;
    private final ObjectMapper mapper;

    public MessageStore(@Value("${messages.file}") String file) {
        this.filePath = Paths.get(file);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    public List<SiteMessage> readAll() {
        System.out.println("==================================================");
        System.out.println("Working dir: " + Paths.get("").toAbsolutePath());
        System.out.println("Reading messages from: " + filePath.toAbsolutePath());

        try {
            if (!Files.exists(filePath)) {
                System.out.println("messages.json NOT FOUND at: " + filePath.toAbsolutePath());
                System.out.println("==================================================");
                return new ArrayList<>();
            }

            byte[] bytes = Files.readAllBytes(filePath);

            if (bytes.length == 0) {
                System.out.println("messages.json is EMPTY");
                System.out.println("==================================================");
                return new ArrayList<>();
            }

            System.out.println("messages.json FOUND (" + bytes.length + " bytes)");

            List<SiteMessage> list = mapper.readValue(bytes, new TypeReference<List<SiteMessage>>() {});
            System.out.println("Loaded " + list.size() + " messages");
            System.out.println("==================================================");
            return list;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read messages file: " + filePath.toAbsolutePath(), e);
        }
    }

    public List<SiteMessage> readActiveNow() {
        LocalDateTime now = LocalDateTime.now();
        return readAll().stream()
                .filter(m -> "ON".equalsIgnoreCase(m.getStatus()))
                .filter(m -> m.getStartDateTime() == null || !now.isBefore(m.getStartDateTime()))
                .filter(m -> m.getEndDateTime() == null || !now.isAfter(m.getEndDateTime()))
                .toList();
    }
}
