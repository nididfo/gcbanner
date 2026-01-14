package com.dl.gcbanner.api;

import com.dl.gcbanner.model.SiteMessage;
import com.dl.gcbanner.service.MessageStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageStore store;

    public MessageController(MessageStore store) {
        this.store = store;
    }

    @GetMapping
    public List<SiteMessage> getAll() {
        return store.readAll();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SiteMessage> replaceAll(@RequestBody List<SiteMessage> messages) {
        return store.replaceAll(messages);
    }

    /**
     * Pass site=... and optional lang=en|fr and optional format=simple|gcdesign
     *
     * - if any active message for that site has render=HTML -> return HTML
     * - otherwise return JSON list
     *
     * Example:
     *   /api/messages/site?site=www.dfo.com&lang=fr&format=gcdesign
     */
    @GetMapping("/site")
    public ResponseEntity<?> getForSite(
            @RequestParam("site") String site,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestParam(value = "format", defaultValue = "simple") String format
    ) {
        List<SiteMessage> active = store.readActiveForSite(site);

        boolean wantsHtml = active.stream()
                .anyMatch(m -> m.getRender() != null && m.getRender().equalsIgnoreCase("HTML"));

        if (wantsHtml) {
            String html = buildHtml(active, lang, format);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(active);
    }

    /**
     * Script tag endpoint:
     * <script src=".../api/messages/site.js?site=www.dfo.com&lang=fr&format=gcdesign"></script>
     */
    @GetMapping(value = "/site.js", produces = "application/javascript")
    public String getForSiteJs(
            @RequestParam("site") String site,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestParam(value = "format", defaultValue = "simple") String format
    ) {
        List<SiteMessage> active = store.readActiveForSite(site);

        boolean wantsHtml = active.stream()
                .anyMatch(m -> "HTML".equalsIgnoreCase(m.getRender()));

        if (!wantsHtml) return "";

        String html = buildHtml(active, lang, format);

        // Escape for JS single-quoted string
        String jsSafe = html
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "");

        return "document.write('" + jsSafe + "');";
    }

    private String buildHtml(List<SiteMessage> messages, String lang, String format) {
        String l = (lang == null) ? "en" : lang.trim().toLowerCase();
        String f = (format == null) ? "simple" : format.trim().toLowerCase();

        if ("gcdesign".equals(f) || "gcds".equals(f) || "gc".equals(f)) {
            return buildGcDesignHtml(messages, l);
        }

        // fallback: original simple HTML
        return buildSimpleDivHtml(messages, l);
    }

    private String buildSimpleDivHtml(List<SiteMessage> messages, String lang) {
        StringBuilder sb = new StringBuilder();

        for (SiteMessage m : messages) {
            String cssClass = safeAttr(m.getType());
            String msg = "fr".equals(lang) ? m.getMessageFr() : m.getMessageEn();

            sb.append("<div class=\"")
                    .append(cssClass)
                    .append("\">")
                    .append(escapeHtml(msg))
                    .append("</div>");
        }
        return sb.toString();
    }

    private String buildGcDesignHtml(List<SiteMessage> messages, String lang) {
        StringBuilder sb = new StringBuilder();

        for (SiteMessage m : messages) {
            String noticeType = toGcdsNoticeType(m.getType());
            String title = "fr".equals(lang) ? m.getTitleFr() : m.getTitleEn();
            String msg = "fr".equals(lang) ? m.getMessageFr() : m.getMessageEn();

            // Optional: if title is missing, set a harmless default
            if (title == null || title.isBlank()) {
                title = defaultTitleForType(noticeType, lang);
            }

            sb.append("<gcds-notice")
                    .append(" type=\"").append(escapeAttr(noticeType)).append("\"")
                    .append(" notice-title-tag=\"h2\"")
                    .append(" notice-title=\"").append(escapeAttr(title)).append("\">")
                    .append("<gcds-text>")
                    .append(escapeHtml(msg))
                    .append("</gcds-text>")
                    .append("</gcds-notice>");
        }

        return sb.toString();
    }

    // success|warning|danger|info
    private String toGcdsNoticeType(String type) {
        if (type == null) return "info";
        String t = type.trim().toLowerCase();

        return switch (t) {
            case "success" -> "success";
            case "warning" -> "warning";
            case "error", "danger" -> "danger"; // your rule: error => danger
            case "info" -> "info";
            default -> "info";
        };
    }

    private String defaultTitleForType(String noticeType, String lang) {
        boolean fr = "fr".equals(lang);
        return switch (noticeType) {
            case "success" -> fr ? "Succès" : "Success";
            case "warning" -> fr ? "Avertissement" : "Warning";
            case "danger"  -> fr ? "Erreur" : "Error";
            default        -> fr ? "Info" : "Info";
        };
    }

    // prevents breaking attributes in a basic way
    private String safeAttr(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    // HTML escaping for inner text
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Attribute escaping (more strict than inner text)
    private String escapeAttr(String s) {
        return escapeHtml(s);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SiteMessage create(@RequestBody SiteMessage message) {
        return store.upsert(message);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SiteMessage updateOne(
            @PathVariable("id") String id,
            @RequestParam("site") String site,
            @RequestBody SiteMessage message
    ) {
        // enforce path/query identity
        message.setId(id);
        message.setSite(site);
        return store.upsert(message);
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteOne(
            @PathVariable("id") String id,
            @RequestParam("site") String site
    ) {
        boolean removed = store.deleteBySiteAndId(site, id);
        if (!removed) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().build();
    }



}
