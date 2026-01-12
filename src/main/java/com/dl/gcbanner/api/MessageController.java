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
     * Pass site=... and optional lang=en|fr
     * - if any active message for that site has render=HTML -> return HTML divs
     * - otherwise return JSON list
     *
     * Example:
     *   /api/messages/site?site=chartgo.com&lang=en
     */
    @GetMapping("/site")
    public ResponseEntity<?> getForSite(
            @RequestParam("site") String site,
            @RequestParam(value = "lang", defaultValue = "en") String lang
    ) {
        List<SiteMessage> active = store.readActiveForSite(site);

        boolean wantsHtml = active.stream()
                .anyMatch(m -> m.getRender() != null && m.getRender().equalsIgnoreCase("HTML"));

        if (wantsHtml) {
            String html = buildHtml(active, lang);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(active);
    }

    private String buildHtml(List<SiteMessage> messages, String lang) {
        StringBuilder sb = new StringBuilder();
        String l = (lang == null) ? "en" : lang.trim().toLowerCase();

        for (SiteMessage m : messages) {
            String cssClass = safeAttr(m.getType());
            String msg = "fr".equals(l) ? m.getMessageFr() : m.getMessageEn();
            sb.append("<div class=\"")
                    .append(cssClass)
                    .append("\">")
                    .append(escapeHtml(msg))
                    .append("</div>");
        }
        return sb.toString();
    }

    // prevents breaking the class attribute (very basic safety)
    private String safeAttr(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    // basic HTML escaping so messages can't inject HTML/JS
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @GetMapping(value = "/site.js", produces = "application/javascript")
    public String getForSiteJs(
            @RequestParam("site") String site,
            @RequestParam(value = "lang", defaultValue = "en") String lang
    ) {
        List<SiteMessage> active = store.readActiveForSite(site);

        boolean wantsHtml = active.stream()
                .anyMatch(m -> "HTML".equalsIgnoreCase(m.getRender()));

        if (!wantsHtml) return "";

        String html = buildHtml(active, lang)
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "");

        return "document.write('" + html + "');";
    }

}
