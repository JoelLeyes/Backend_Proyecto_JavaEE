package com.nexolab.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilTest {
    //
    @Test
    void generateTokenAndReadUserId() {
        Long expectedUserId = 123L;

        String token = JwtUtil.generateToken(expectedUserId);

        assertEquals(expectedUserId, JwtUtil.getUserIdFromToken(token));
    }
}