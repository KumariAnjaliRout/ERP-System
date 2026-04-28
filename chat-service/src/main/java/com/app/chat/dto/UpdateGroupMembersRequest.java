package com.app.chat.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateGroupMembersRequest {

    private List<UUID> userIds;

}