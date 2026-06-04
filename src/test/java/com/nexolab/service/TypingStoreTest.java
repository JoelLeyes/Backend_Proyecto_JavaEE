package com.nexolab.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypingStoreTest {

    @Test
    void markTypingStoresEntriesAndExcludesCurrentUser() {
        TypingStore store = TypingStore.getInstance();
        Long chatId = 9_001L;

        store.markTyping(chatId, 10L, "Ana");
        store.markTyping(chatId, 11L, "Luis");

        List<TypingStore.Entry> typing = store.getTyping(chatId, 10L);

        assertEquals(1, typing.size());
        assertTrue(typing.stream().anyMatch(entry -> entry.userId().equals(11L) && "Luis".equals(entry.nombre())));
    }
}