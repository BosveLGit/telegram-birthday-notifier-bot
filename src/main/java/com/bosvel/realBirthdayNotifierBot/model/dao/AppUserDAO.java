package com.bosvel.realBirthdayNotifierBot.model.dao;



import com.bosvel.realBirthdayNotifierBot.model.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface AppUserDAO extends JpaRepository<AppUser, Long> {

    AppUser findAppUserByTelegramUserId(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE AppUser SET hourNotification = :hourNotification WHERE telegramUserId = :id")
    int setHourNotification(int hourNotification, Long id);

    @Modifying
    @Transactional
    @Query("UPDATE AppUser SET daysBeforeNotification = :daysBeforeNotification WHERE telegramUserId = :id")
    int setDaysBeforeNotification(int daysBeforeNotification, Long id);

    @Modifying
    @Transactional
    @Query("UPDATE AppUser SET timezone = :timezone WHERE telegramUserId = :id")
    int setTimezone(String timezone, Long id);

    @Query(nativeQuery = true, value =
    "SELECT " +
    "hour_notification AS hourNotification," +
    "timezone," +
    "days_before_notification AS daysBeforeNotification " +
    "FROM app_user " +
    "WHERE telegram_user_id = :userId")
    Map<String, Object> getSettingFields(@Param("userId") Long userId);



}
