package Model;

import java.time.LocalDateTime;

public class NotifyAbout
{
    private UserProfile user;
    private Event event;
    private LocalDateTime createdAt;
    private boolean seen;
    private String message;

    public NotifyAbout(UserProfile user, Event event, String message) {
        setUser(user);
        setEvent(event);
        setCreatedAt(LocalDateTime.now());
        setSeen(false);
        setMessage(message);
    }
    public NotifyAbout(UserProfile user, Event event, LocalDateTime createdAt, boolean seen, String message)
    {
        setUser(user);
        setEvent(event);
        setCreatedAt(createdAt);
        setSeen(seen);
        setMessage(message);
    }

    // Setters
    public void setUser(UserProfile user) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        this.user = user;
    }

    public void setEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }
        this.event = event;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = (createdAt == null) ? LocalDateTime.now() : createdAt;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public void setMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            this.message = "";
            return;
        }
        String value = message.trim().replaceAll("\\s+", " ");
        this.message = value;
    }

    // Getters
    public UserProfile getUser() {
        return user;
    }
    public Event getEvent() {
        return event;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public boolean isSeen() {
        return seen;
    }
    public String getMessage() {
        return message;
    }

    // toString
    @Override public String toString() {
        return "NotifyAbout: " +
                user.getEmail() + " | Event #" + event.getEventID() + " | Created: " + createdAt +
                " | Seen: " + seen + (message == null ||
                message.isEmpty() ? "" : " | Message: " + message);
    }

    // Methods
    public void markAsSeen() {
        this.seen = true;
    }

    public boolean shouldTrigger() {
        if (event == null || event.getDateTime() == null) {
            return false;
        }
        if (seen) {
            return false;
        }
        if (event.isNotified()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventTime = event.getDateTime();
        LocalDateTime triggerTime = eventTime.minusHours(24);

        boolean afterOrEqualTrigger = now.isAfter(triggerTime) || now.isEqual(triggerTime);
        boolean beforeOrEqualEvent = now.isBefore(eventTime) || now.isEqual(eventTime);

        return afterOrEqualTrigger && beforeOrEqualEvent;
    }

    //Marks that this notification has been triggered
    public void markAsTriggered() {
        //update creation time to the actual trigger time
        this.createdAt = LocalDateTime.now();
        //prevent future triggers for the same event
        if (event != null) {
            event.markAsNotified();
        }
    }
}

