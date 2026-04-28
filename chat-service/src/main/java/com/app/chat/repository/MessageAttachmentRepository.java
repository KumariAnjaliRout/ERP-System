package com.app.chat.repository;

import com.app.chat.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageAttachmentRepository
        extends JpaRepository<MessageAttachment, Long> {

    Optional<MessageAttachment> findByMessageId(Long messageId);


}
