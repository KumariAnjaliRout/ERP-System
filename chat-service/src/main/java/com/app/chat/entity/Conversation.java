package com.app.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // nullable, only for GROUP

    // Org isolation (cross-service → keep as simple field)
//    @Column(nullable = false)
    private String orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    // Creator stored as UUID (no ChatUser relation)
    @Column(nullable = false)
    private UUID createdBy;

    private LocalDateTime lastMessageAt;

}
