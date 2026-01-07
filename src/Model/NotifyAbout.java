package Model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


//This class was initially designed to notify users about upcoming events (24 hours prior).
//But, later on i decided to extend its functionality to include system notifications without specific events.
//This change was in order to "Notify about" more system related things.
//Obviously, still connect between user and event but also connect between the user and the system!
public class NotifyAbout {
    private UserProfile user;
    private Event event;
    private LocalDateTime createdAt;
    private boolean seen;
    //For notifications without specific event :
    private String title;
    private String message;


    private static final DateTimeFormatter NOTIF_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public NotifyAbout(UserProfile user, Event event) {
        setUser(user);
        setEvent(event);
        this.title = null;
        this.message = null;
        setCreatedAt(LocalDateTime.now());
        setSeen(false);
    }

    public NotifyAbout(UserProfile user, String title, String message) {
        setUser(user);
        this.event = null;
        this.title = (title == null) ? "" : title.trim();
        this.message = (message == null) ? "" : message.trim();
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
    public String getTitle() {
        if (isSystem()) return title;
        return (event == null || event.getTitle() == null) ? "" : event.getTitle();
    }
    public String getMessage() {
        if (isSystem()) return message;
        return (event == null || event.getNotes() == null) ? "" : event.getNotes();
    }

    // toString
    @Override
    public String toString() {
        String seenText = seen ? "Yes" : "No";
        if (isSystem()) {
            String t = (title == null || title.isBlank()) ? "System" : title.trim();
            String shortMsg = (message == null) ? "" : message.trim();
            if (shortMsg.length() > 55) shortMsg = shortMsg.substring(0, 55) + "...";
            return "System | " + t + " | " + shortMsg + " | Seen: " + seenText;
        }
        String id = (event == null) ? "-" : String.valueOf(event.getEventID());
        String title = (event == null || event.getTitle() == null || event.getTitle().isBlank()) ? "(no title)" : event.getTitle().trim();
        String when = (event == null || event.getDateTime() == null) ? "N/A" : event.getDateTime().format(NOTIF_FMT);
        seenText = seen ? "Yes" : "No";
        return "Event #" + id + " | IN 24 HOURS | " + title + " | On: " + when + " | Seen: " + seenText;
    }

    // Methods
    public boolean isSystem() {
        return (event == null);
    }

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

