package com.bosvel.realBirthdayNotifierBot.model.entity;

import com.bosvel.realBirthdayNotifierBot.service.SocialNetworks;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_message_stack")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ScheduledMessageStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Long telegramUserId;
    private String name;
    private LocalDate birthday;
    private LocalDateTime timestampNotification;
    private SocialNetworks socialNetwork;
    private int age;
    private int daysBeforeNotification;
    private String friendURL;
    private String additionalText;

}
