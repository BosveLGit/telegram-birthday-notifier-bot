package com.bosvel.realBirthdayNotifierBot.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class RedisService {
    
    private final HashOperations<String, Long, String> hashOperations;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.hashOperations = redisTemplate.opsForHash();
        this.redisTemplate = redisTemplate;
    }

    public void saveCommandQueue(Map<Long, Deque<String>> commandQueue) {
        commandQueue.forEach((key, value) -> {
            JSONArray jsonArray = new JSONArray(value);
            hashOperations.put("commandQueue", key, jsonArray.toString());
        });
        redisTemplate.expire("commandQueue", 3600, TimeUnit.SECONDS);
    }

    public void loadCommandQueue(Map<Long, Deque<String>> commandQueue) throws RedisConnectionFailureException {

        commandQueue.clear();
        Map<Long, String> entries = hashOperations.entries("commandQueue");

        entries.forEach((key, value) -> {
            JSONArray jsonArray = new JSONArray(value);
            Deque<String> deque = new ArrayDeque<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                deque.add(jsonArray.getString(i));
            }
            commandQueue.put(key, deque);
        });

    }

    public void saveUserSessionData(Map<Long, Map<String, Object>> userSessionData) {

        userSessionData.forEach((key, value) -> {
            JSONObject json = new JSONObject(value);
            hashOperations.put("userSessionData", key, json.toString());
        });
        redisTemplate.expire("userSessionData", 3600, TimeUnit.SECONDS);

    }

    public void loadUserSessionData(Map<Long, Map<String, Object>> userSessionData) {

        userSessionData.clear();
        Map<Long, String> entries = hashOperations.entries("userSessionData");

        entries.forEach((key, value) -> {
            JSONObject json = new JSONObject(value);
            Map<String, Object> map = json.toMap();
            userSessionData.put(key, new HashMap<>(map));

        });

    }

}