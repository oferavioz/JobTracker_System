package Model;

import java.time.LocalDateTime;

public class Event
{
    private static int nextID = 1; //running counter for unique events
    private final int eventID;
    private String type;
    private LocalDateTime dateTime;
    private int duration;
    private String title;
    private String notes;
    private boolean notified;

    public Event(String type, String title, LocalDateTime dateTime, int duration, String notes)
    {
        this.eventID = nextID++;

        setType(type);
        setTitle(title);
        setDateTime(dateTime);
        setDuration(duration);
        setNotes(notes);

        this.notified = false;
    }
    public Event(int eventID, String type, String title, LocalDateTime dateTime, int duration, String notes, boolean notified)
    {
        if (eventID <= 0) {
            throw new IllegalArgumentException("Event ID must be positive.");
        }
        this.eventID = eventID;
        if (eventID >= nextID) {
            nextID = eventID + 1;
        }

        setType(type);
        setTitle(title);
        setDateTime(dateTime);
        setDuration(duration);
        setNotes(notes);

        this.notified = notified;
    }

    // Setters
    public static void updateNextID(int maxExistingID) {
        if (maxExistingID >= nextID) {
            nextID = maxExistingID + 1;
        }
    }

    public void setType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty.");
        }
        String value = type.trim().replaceAll("\\s+", " ");
        this.type = value;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Event title cannot be null or empty.");
        }
        String value = title.trim().replaceAll("\\s+", " ");
        this.title = value;
    }

    public void setDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("Event date and time cannot be null.");
        }
        this.dateTime = dateTime;
    }

    public void setDuration(int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Event duration must be positive.");
        }
        this.duration = duration;
    }

    public void setNotes(String notes) {
        if (notes == null) {
            this.notes = "";
            return;
        }
        String value = notes.trim().replaceAll("[ \\t]+", " ");
        this.notes = value;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    // Getters
    public int getEventID() {
        return eventID;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public int getDuration() {
        return duration;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isNotified() {
        return notified;
    }

    // toString
    @Override public String toString() {
        return "Event #" + eventID + " | " + type + " | " + title + " | DateTime: " + dateTime +
                " | Duration: " + duration + " mins" + " | Notes: " + notes + " | Notified? "
                + notified;
    }

    // Methods
    public void markAsNotified() {
        this.notified = true;
    }

    public void resetNotification() {
        this.notified = false;
    }

    public void reSchedule(LocalDateTime newDateTime) {
        setDateTime(newDateTime);
        resetNotification();
    }

    public void updateDetails(String newType, String newTitle, LocalDateTime newDateTime,
                              int newDuration, String newNotes) {
        setType(newType);
        setTitle(newTitle);
        setDateTime(newDateTime);
        setDuration(newDuration);
        setNotes(newNotes);
        resetNotification();
    }

    public LocalDateTime getEndDateTime()
    {
        return dateTime.plusMinutes(duration);
    }

}
