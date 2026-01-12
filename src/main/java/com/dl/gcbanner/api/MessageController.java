package com.dl.gcbanner.api;

import com.dl.gcbanner.model.SiteMessage;
import com.dl.gcbanner.service.MessageStore;
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
}
