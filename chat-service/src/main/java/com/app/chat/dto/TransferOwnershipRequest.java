package com.app.chat.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TransferOwnershipRequest {

    private UUID newOwnerId;
}