package com.bosvel.realBirthdayNotifierBot.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode(exclude = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "birthday_data",
        indexes = {@Index(name = "idx_app_user_telegram_user_id", columnList = "app_user_telegram_user_id")}
)
public class BirthdayData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToOne
    @JoinColumn(name = "app_user_telegram_user_id", referencedColumnName = "telegramUserId")
    private AppUser appUser;

    @Column(nullable = false)
    private LocalDate birthday;

    private int dayBirthday;
    private int monthBirthday;
    private int yearBirthday;

}
