package Model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotifyAbout
{
    private UserProfile user;
    private Event event;
    private LocalDateTime createdAt;
    private boolean seen;

    private static final DateTimeFormatter NOTIF_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public NotifyAbout(UserProfile user, Event event) {
        setUser(user);
        setEvent(event);
        setCreatedAt(LocalDateTime.now());
        setSeen(false);
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

    // toString
    @Override
    public String toString() {
        String id = (event == null) ? "-" : String.valueOf(event.getEventID());
        String type = (event == null || event.getType() == null || event.getType().isBlank()) ? "Other" : event.getType().trim();
        String title = (event == null || event.getTitle() == null || event.getTitle().isBlank()) ? "(no title)" : event.getTitle().trim();
        String when = (event == null || event.getDateTime() == null) ? "N/A" : event.getDateTime().format(NOTIF_FMT);
        String seenText = seen ? "Yes" : "No";
        return "Event #" + id + " | " + title + " | On: " + when + " | Seen: " + seenText;
    }

    // Methods
    public void markAsSeen() {
        this.seen = true;
    }

    public boolean shouldTrigger() {
        if (event == null || event.getDateTime() == null) {
            return false;
        }
        if (event.isNotified()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventTime = event.getDateTime();
        LocalDateTime triggerTime = eventTime.minusHours(24);
        boolean afterOrEqTrigger = now.isAfter(triggerTime) || now.isEqual(triggerTime);
        boolean beforeEvent = now.isBefore(eventTime);

        return afterOrEqTrigger && beforeEvent;
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

