package Model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Document
{
    private String docName; // unique name for the document
    private String docType;
    private String target; // file - relative path, link - URL
    private LocalDateTime lastUpdate;

    private static final DateTimeFormatter DOC_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Document(String docName, String documentType, String target, LocalDateTime lastUpdate)
    {
        setDocName(docName);
        setDocumentType(documentType);
        setTarget(target);
        setLastUpdate(lastUpdate);
    }

    // Setters
    public void setDocName(String docName) {
        if (docName == null || docName.trim().isEmpty()) {
            throw new IllegalArgumentException("Document name cannot be null or empty.");
        }
        String name = docName.trim().replaceAll("\\s+", " ");
        this.docName = name;
    }

    public void setDocumentType(String documentType) {
        if (documentType == null || documentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Document type cannot be null or empty.");
        }
        String type = documentType.trim().replaceAll("\\s+", " ");
        this.docType = type;
    }

    public void setTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("Target cannot be null or empty.");
        }
        String value = target.trim();
        // If it's a URL document -> basic URL validation
        if (isURL()) {
            if (value.contains(" ") || !value.contains(".")) {
                throw new IllegalArgumentException("Invalid URL.");
            }
        }
        this.target = value;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = (lastUpdate == null) ? LocalDateTime.now() : lastUpdate;
    }

    // Getters
    public String getDocName() {
        return docName;
    }
    public String getDocType() {
        return docType;
    }
    public String getTarget() {
        return target;
    }
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    // toString
    @Override public String toString() {
        return docName + " | " + docType + " | " + target + " | Last update: " + lastUpdate.format(DOC_FMT);
    }

    // Methods
    private void touch() {
        this.lastUpdate = LocalDateTime.now();
    }

    public boolean isURL() {
        return docType != null && docType.equalsIgnoreCase("url");
    }

    public void updateTarget(String newTarget) {
        setTarget(newTarget);
        touch();
    }

    public void reName(String newName) {
        setDocName(newName);
        touch();
    }

}
