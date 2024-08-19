package com.bosvel.realBirthdayNotifierBot.model.dao;

import com.bosvel.realBirthdayNotifierBot.model.entity.BirthdayData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface BirthdayDataDAO extends JpaRepository<BirthdayData, Long> {

    BirthdayData findBirthdayDataById(Long id);

    void deleteById(Long id);

    @Query(value = "SELECT bd FROM BirthdayData bd WHERE bd.appUser.telegramUserId = :userId ORDER BY bd.id")
    Page<BirthdayData> findAllByUserIdWithPagination(Long userId, Pageable pageable);

    @Query(value = "SELECT bd FROM BirthdayData bd " +
            "WHERE bd.appUser.telegramUserId = :userId AND bd.monthBirthday = :month " +
            "ORDER BY bd.id")
    Page<BirthdayData> findAllByUserIdAndByMonthWithPagination(Long userId, Pageable pageable, int month);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BirthdayData bd SET bd.name = :name WHERE bd.id = :id")
    int updateNameById(Long id, String name);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BirthdayData bd SET " +
            "bd.birthday = :newBirthday, " +
            "bd.dayBirthday = EXTRACT(DAY FROM CAST(:newBirthday AS DATE)), " +
            "bd.monthBirthday = EXTRACT(MONTH FROM CAST(:newBirthday AS DATE)), " +
            "bd.yearBirthday = EXTRACT(YEAR FROM CAST(:newBirthday AS DATE)) " +
            "WHERE bd.id = :id")
    int updateBirthdayById(@Param("id") Long id, @Param("newBirthday") LocalDate newBirthday);

}
