package com.app.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "chat_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatUser {

    @Id
    private UUID userId;

    private String username;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String organizationId;
}



