package com.bosvel.realBirthdayNotifierBot.model.dao;

import com.bosvel.realBirthdayNotifierBot.model.entity.VKAuthData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface VKAuthDataDAO extends JpaRepository<VKAuthData, Long> {
    @Query(value = "SELECT TRUE AS active, (expire_date - CURRENT_DATE) <= 7 AS canBeReconnected " +
            "FROM vk_auth_data " +
            "WHERE telegram_user_id = :userId AND expire_date > current_timestamp",
            nativeQuery = true)
    Map<String, Boolean> getStatusAuthInVK(@Param("userId") Long userId);

    @Transactional
    @Modifying
    int deleteVKAuthDataByTelegramUserId(@Param("userId") Long userId);


}
