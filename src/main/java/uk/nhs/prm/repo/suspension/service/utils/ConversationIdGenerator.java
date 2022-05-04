package uk.nhs.prm.repo.suspension.service.utils;

import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
public class ConversationIdGenerator {

    public String generateConversationId() {
        return UUID.randomUUID().toString();
    }
}
