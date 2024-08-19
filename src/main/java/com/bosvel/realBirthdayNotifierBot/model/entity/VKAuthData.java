package com.bosvel.realBirthdayNotifierBot.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode()
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vk_auth_data", indexes = {
        @Index(name = "idx_expireDate", columnList = "expireDate")
})
public class VKAuthData {

    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long telegramUserId;
    private Long vkUserId;
    private String accessToken;
    private String accessTokenId;
    private LocalDate expireDate;
    @CreationTimestamp
    private Timestamp registeredAt;

}
