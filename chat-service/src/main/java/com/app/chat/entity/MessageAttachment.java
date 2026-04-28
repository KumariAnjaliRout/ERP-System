package com.app.chat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id")
    private Long messageId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String s3Key;
}
