package com.app.chat.events;

import com.app.chat.entity.Role;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncEvent {

    private UUID userId;
    private String username;
    private Role role;
    private String organizationId;
}
