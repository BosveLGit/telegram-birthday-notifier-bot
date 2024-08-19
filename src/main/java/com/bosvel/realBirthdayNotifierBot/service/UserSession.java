package com.bosvel.realBirthdayNotifierBot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class UserSession {

    private final Map<Long, Map<String, Object>> data = new ConcurrentHashMap<>();

    @Autowired
    private RedisService redisService;

    public Object get(Long userId, String key, Object defValue) {
        return data.getOrDefault(userId, Collections.emptyMap())
                .getOrDefault(key, defValue);
    }

    public void put(Long userId, String key, Object value) {
        data.putIfAbsent(userId, new HashMap<>());
        Map<String, Object> session = data.get(userId);
        session.put(key, value);
    }

    public void clear(Long userId) {
        data.remove(userId);
    }

    public void removeValue(Long userId, String key) {
        Map<String, Object> innerMap = data.get(userId);
        if (innerMap != null) {
            innerMap.remove(key);
        }
    }

    public void saveDataToRedis() {

        if(redisService == null) {
            return;
        }

        try {
            redisService.saveUserSessionData(data);
        } catch (Exception e) {
            log.error("Error saving data in Redis (UserSession): " + e.getMessage(), e);
        }
    }

    public void loadDataFromRedis() {

        if(redisService == null) {
            return;
        }

        try {
            redisService.loadUserSessionData(data);
        } catch (Exception e) {
            log.error("Error loading data from Redis (UserSession): " + e.getMessage(), e);
        }
    }

}
