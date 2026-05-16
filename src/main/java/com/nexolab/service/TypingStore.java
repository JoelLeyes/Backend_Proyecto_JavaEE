package com.nexolab.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TypingStore {

    public record Entry(Long userId, String nombre, long timestamp) {}

    private static final TypingStore INSTANCE = new TypingStore();
    private static final long TTL_MS = 4_000;

    // chatId → userId → Entry
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, Entry>> store =
            new ConcurrentHashMap<>();

    private TypingStore() {}

    public static TypingStore getInstance() { return INSTANCE; }

    public void markTyping(Long chatId, Long userId, String nombre) {
        store.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>())
             .put(userId, new Entry(userId, nombre, System.currentTimeMillis()));
    }

    public List<Entry> getTyping(Long chatId, Long excludeUserId) {
        ConcurrentHashMap<Long, Entry> chatMap = store.get(chatId);
        if (chatMap == null) return List.of();
        long cutoff = System.currentTimeMillis() - TTL_MS;
        List<Entry> result = new ArrayList<>();
        chatMap.forEach((uid, entry) -> {
            if (!uid.equals(excludeUserId) && entry.timestamp() >= cutoff) {
                result.add(entry);
            }
        });
        return result;
    }
}
