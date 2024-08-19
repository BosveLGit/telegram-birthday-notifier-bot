package com.bosvel.realBirthdayNotifierBot.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;


@Getter
@Setter
@EqualsAndHashCode() // exclude = "id"
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    private Long telegramUserId;
    @CreationTimestamp
    private Timestamp registeredAt;
    private LocalDate birthday;
    private String firstName;
    private String lastName;
    private String username;
    private String timezone;
    private int hourNotification;
    private int daysBeforeNotification;

}
