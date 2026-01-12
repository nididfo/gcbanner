package com.dl.gcbanner.api;

import com.dl.gcbanner.model.SiteMessage;
import com.dl.gcbanner.service.MessageStore;
import org.springframework.http.MediaType;
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

    @GetMapping("/active")
    public List<SiteMessage> getActive() {
        return store.readActiveNow();
    }

    /**
     * Replaces the entire messages.json file with the JSON array in the request body.
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SiteMessage> replaceAll(@RequestBody List<SiteMessage> messages) {
        return store.replaceAll(messages);
    }
}
