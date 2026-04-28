package com.erp.Ticketing.dto;

import com.erp.Ticketing.model.TicketHistory;
import com.erp.Ticketing.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

public class MyTicketHistoryDto {

    private Long ticketId;
    private String title;
    private String description;
    private String priority;
    private String status;
    private String oldStatus;
    private String newStatus;
    private String updatedByRole;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    public Long getTicketId() {
        return ticketId;
    }
    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getPriority() {
        return priority;
    }
    public void setPriority(String priority) {
        this.priority = priority;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getOldStatus() {
        return oldStatus;
    }
    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }
    public String getNewStatus() {
        return newStatus;
    }
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    public String getUpdatedByRole() {
        return updatedByRole;
    }
    public void setUpdatedByRole(String updatedByRole) {
        this.updatedByRole = updatedByRole;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


