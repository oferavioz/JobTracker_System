package Model;

import java.time.LocalDateTime;

public class Stores
{
    private UserProfile user;
    private Document doc;
    private LocalDateTime storedAt;
    private boolean primary;
    private String note;

    public Stores(UserProfile user, Document document, String note) {
        setUser(user);
        setDocument(document);
        setPrimary(false);
        setNote(note);
        setStoredAt(LocalDateTime.now());
    }

    // Setters
    public void setUser(UserProfile user) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        this.user = user;
    }

    public void setDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null.");
        }
        this.doc = document;
    }

    public void setStoredAt(LocalDateTime storedAt) {
        this.storedAt = (storedAt == null) ? LocalDateTime.now() : storedAt;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void setNote(String note) {
        if (note == null || note.trim().isEmpty()) {
            this.note = "";
            return;
        }
        String value = note.trim().replaceAll("[ \\t]+", " ");
        this.note = value;
    }

    // Getters
    public UserProfile getUser() {
        return user;
    }
    public Document getDocument() {
        return doc;
    }
    public LocalDateTime getStoredAt() {
        return storedAt;
    }
    public boolean isPrimary() {
        return primary;
    }
    public String getNote() {
        return note;
    }

    // toString
    @Override public String toString() {
        return "Stores: " + user.getEmail() +
                " -> " + doc.getDocName() + " | Primary: " + primary +
                (note == null || note.isEmpty() ? "" : " | Note: " + note) +
                " | StoredAt: " + storedAt;
    }

    // Methods
    public void markAsPrimary() {
        this.primary = true;
    }

    public void unmarkAsPrimary() {
        this.primary = false;
    }

    public void updateNote(String newNote) {
        setNote(newNote);
    }

}
