package com.dl.gcbanner.service;

import com.dl.gcbanner.model.SiteMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class MessageStore {

    private final Path filePath;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    public MessageStore(@Value("${messages.file}") String file) {
        this.filePath = Paths.get(file);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    public List<SiteMessage> readAll() {
        rwLock.readLock().lock();
        try {
            if (!Files.exists(filePath)) return new ArrayList<>();
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length == 0) return new ArrayList<>();
            return mapper.readValue(bytes, new TypeReference<List<SiteMessage>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read messages file: " + filePath.toAbsolutePath(), e);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<SiteMessage> readActiveForSite(String site) {
        if (site == null || site.isBlank()) return List.of();

        String wanted = site.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        return readAll().stream()
                .filter(m -> m.getSite() != null && m.getSite().trim().toLowerCase().equals(wanted))
                .filter(m -> "ON".equalsIgnoreCase(m.getStatus()))
                .filter(m -> m.getStartDateTime() == null || !now.isBefore(m.getStartDateTime()))
                .filter(m -> m.getEndDateTime() == null || !now.isAfter(m.getEndDateTime()))
                .toList();
    }

    public List<SiteMessage> replaceAll(List<SiteMessage> newList) {
        if (newList == null) throw new IllegalArgumentException("Request body must be a JSON array (not null).");

        rwLock.writeLock().lock();
        try {
            Path parent = filePath.getParent();
            if (parent != null) Files.createDirectories(parent);

            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(newList) + "\n";

            Path dir = (parent != null) ? parent : Paths.get(".");
            Path tmp = Files.createTempFile(dir, "messages-", ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return newList;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write messages file: " + filePath.toAbsolutePath(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
