package com.sep.educonnect.dto.log;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum EventType {
    VIEW("view"),
    CLICK("click"),
    CONVERSION("conversion"),
    RATING("rating"),
    WISHLIST("wishlist"),
    JOIN("join");
    
    private final String event;

    EventType(String event) {
        this.event = event;
    }

    /**
     * Jackson deserializer: finds enum by event string value (case-insensitive)
     */
    @JsonCreator
    public static EventType fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase().trim();
        for (EventType type : EventType.values()) {
            if (type.event.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: " + value);
    }

    /**
     * Jackson serializer: uses the event string value
     */
    @JsonValue
    public String getEvent() {
        return event;
    }
}
