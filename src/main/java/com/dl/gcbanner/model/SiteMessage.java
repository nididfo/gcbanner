package com.dl.gcbanner.model;

import java.time.LocalDateTime;

public class SiteMessage {
    private String id;

    private String site;          // e.g. "www.dfo.com"
    private String render;        // "HTML" or "JSON"

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    // For GC Design System notice type mapping:
    // success, warning, error, info
    private String type;

    private String status;        // ON, OFF
    private String severity;      // optional

    private String titleEn;       // NEW
    private String titleFr;       // NEW

    private String messageEn;
    private String messageFr;

    private String createdBy;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getRender() { return render; }
    public void setRender(String render) { this.render = render; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }

    public String getTitleFr() { return titleFr; }
    public void setTitleFr(String titleFr) { this.titleFr = titleFr; }

    public String getMessageEn() { return messageEn; }
    public void setMessageEn(String messageEn) { this.messageEn = messageEn; }

    public String getMessageFr() { return messageFr; }
    public void setMessageFr(String messageFr) { this.messageFr = messageFr; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
