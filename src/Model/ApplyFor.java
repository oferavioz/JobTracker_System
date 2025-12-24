package Model;

import java.time.LocalDateTime;

public class ApplyFor
{
    private JobPosition position;
    private UserProfile user;
    private LocalDateTime dateApplied;
    private ApplicationStage stage;
    private String source;
    private String notes;

    public ApplyFor(JobPosition position, UserProfile user, String source, String notes) {
        setPosition(position);
        setUserProfile(user);
        setDateApplied(LocalDateTime.now());
        setStage(ApplicationStage.APPLIED);
        setSource(source);
        setNotes(notes);
    }

    // Setters
    public void setUserProfile(UserProfile user) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        this.user = user;
    }

    public void setPosition(JobPosition position) {
        if (position == null) {
            throw new IllegalArgumentException("JobPosition cannot be null.");
        }
        this.position = position;
    }

    public void setDateApplied(LocalDateTime dateApplied) {
        this.dateApplied = (dateApplied == null) ? LocalDateTime.now() : dateApplied;
    }

    public void setStage(ApplicationStage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage cannot be null.");
        }
        this.stage = stage;
    }

    public void setSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source cannot be null or empty.");
        }
        String value = source.trim().replaceAll("\\s+", " ");
        this.source = value;
    }

    public void setNotes(String notes) {
        if (notes == null || notes.trim().isEmpty()) {
            this.notes = "";
            return;
        }
        //create a bulleted list format every line drop
        String value = notes.trim().replaceAll("[ \\t]+", " ");
        value = value.replace("\n", "\n- ");
        if (!value.startsWith("- ")) {
            value = "- " + value;
        }
        this.notes = value;
    }

    // Getters
    public UserProfile getUserProfile() {
        return user;
    }

    public JobPosition getPosition() {
        return position;
    }

    public LocalDateTime getDateApplied() {
        return dateApplied;
    }

    public ApplicationStage getStage() {
        return stage;
    }

    public String getSource() {
        return source;
    }

    public String getNotes() {
        return notes;
    }

    // toString
    @Override
    public String toString() {
        return "Application: " +
                user.getEmail() + " -> " +
                position.getPositionID() + " (" + position.getTitle() + ")" +
                " | Stage: " + stage + " | Applied: " + dateApplied +
                " | Source: " + source +
                (notes == null || notes.isEmpty() ? "" : " | Notes: " + notes);
    }

    // Methods
    public String getSummary() {
        return position.getTitle() + " | " + stage + " | " + dateApplied;
    }

    public void updateStage(ApplicationStage newStage) {
        setStage(newStage);
    }

    public void addNote(String additionalNote) {
        if (additionalNote == null || additionalNote.trim().isEmpty()) {
            return;
        }
        String value = additionalNote.trim().replaceAll("[ \\t]+", " ");
        String combined;
        if (this.notes == null || this.notes.isEmpty()) {
            combined = value;
        } else {
            combined = this.notes + "\n" + value;
        }
        setNotes(combined);
    }

    public long timeSinceApplied() {
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(dateApplied, now).toDays();
    }

}
