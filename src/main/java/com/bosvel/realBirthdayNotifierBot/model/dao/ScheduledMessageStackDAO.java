package com.bosvel.realBirthdayNotifierBot.model.dao;

import com.bosvel.realBirthdayNotifierBot.model.entity.ScheduledMessageStack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledMessageStackDAO extends JpaRepository<ScheduledMessageStack, Integer> {

    @Query(nativeQuery = true, value =
    "SELECT * FROM scheduled_message_stack " +
    "WHERE timestamp_notification= :sendingTime " +
    "ORDER BY telegram_user_id, social_network ")
    List<ScheduledMessageStack> getBirthdaysForScheduledSending(@Param("sendingTime") LocalDateTime sendingTime);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value =
    "DELETE FROM scheduled_message_stack " +
    "WHERE telegram_user_id = :telegramUserId AND timestamp_notification= :sendingTime")
    void deleteBirthdaysForScheduledSending(@Param("telegramUserId") Long telegramUserId,
                                            @Param("sendingTime") LocalDateTime sendingTime);

    @Modifying
    @Query(nativeQuery = true, value =
    "CREATE TEMPORARY TABLE notification_intermediate ON COMMIT DROP AS " +
    "WITH converted_date AS ( " +
    "SELECT CAST(:basedate AS DATE) AS base_date)," +
    "notification_times AS ( " +
    "    SELECT " +
    "        app_user.telegram_user_id AS telegram_user_id, " +
    "        app_user.days_before_notification AS days_before_notification, " +
    "        converted_date.base_date AS base_date, " +
    "        converted_date.base_date + ((INTERVAL '1 hour' * app_user.hour_notification) - " +
    "              (current_timestamp AT TIME ZONE app_user.timezone - current_timestamp)) AS timestamp_notification " +
    "    FROM app_user AS app_user" +
    "    CROSS JOIN converted_date AS converted_date" +
    "    WHERE app_user.timezone != '' AND app_user.timezone IS NOT NULL ) " +
    "    SELECT " +
    "        telegram_user_id AS telegram_user_id, " +
    "        base_date + CAST(timestamp_notification AS TIME) AS timestamp_notification, " +
    "        base_date + (INTERVAL '1 day' * (days_before_notification + " +
    "               (base_date - CAST(timestamp_notification AS DATE)))) AS date_birthday, " +
    "        days_before_notification AS days_before_notification " +
    "    FROM notification_times; ")
    public void createTemporaryTableAndCalculate(@Param("basedate") Date basedate);

    @Modifying
    @Query(nativeQuery = true, value =
    "INSERT INTO scheduled_message_stack (telegram_user_id, name, birthday, social_network, timestamp_notification, age, days_before_notification) " +
    "SELECT " +
    "   ni.telegram_user_id AS telegram_user_id, " +
    "   bd.name AS name, " +
    "   bd.birthday AS birthday, " +
    "   :socialNetworkValue AS social_network, " +
    "   ni.timestamp_notification AS timestamp_notification, " +
    "   CASE WHEN bd.year_birthday = 1 THEN 0 ELSE EXTRACT(YEAR FROM ni.date_birthday) - bd.year_birthday END AS age, " +
    "   ni.days_before_notification AS days_before_notification " +
    "FROM notification_intermediate ni " +
    "JOIN birthday_data bd ON ni.telegram_user_id = bd.app_user_telegram_user_id " +
    "WHERE " +
    "    bd.day_birthday = EXTRACT(DAY FROM ni.date_birthday) " +
    "    AND bd.month_birthday = EXTRACT(MONTH FROM ni.date_birthday);")
    public void insertIntoScheduledMessageStackFromTemp(@Param("socialNetworkValue") int socialNetworkValue);

    @Query(nativeQuery = true, value =
    "SELECT " +
    "   vk_auth.access_token AS accessToken, " +
    "   CAST(ni.date_birthday AS DATE) AS dateBirthday, " +
    "   ni.telegram_user_id AS telegramUserId, " +
    "   ni.timestamp_notification AS timestampNotification, " +
    "   ni.days_before_notification AS daysBeforeNotification " +
    "FROM notification_intermediate ni " +
    "JOIN vk_auth_data vk_auth ON vk_auth.telegram_user_id = ni.telegram_user_id " +
    "WHERE vk_auth.expire_date > current_timestamp ")
    public List<Object[]> getVKAccessTokensFromTemp();

    @Query(nativeQuery = true, value =
    "SELECT " +
    "   ni.telegram_user_id AS telegramUserId, " +
    "   ni.timestamp_notification AS timestampNotification, " +
    "   vk_auth.expire_date AS expireDate " +
    "FROM notification_intermediate ni " +
    "JOIN vk_auth_data vk_auth ON vk_auth.telegram_user_id = ni.telegram_user_id " +
    "WHERE vk_auth.expire_date IN (:date1, :date3, :date7, :date0)")
    public List<Object[]> findVKAuthDataByExpireDate(LocalDate date1, LocalDate date3, LocalDate date7, LocalDate date0);

}
