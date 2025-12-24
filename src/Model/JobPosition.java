package Model;

public class JobPosition
{
    private String positionID;
    private String title;
    private String field;
    private String location;
    private String employmentType;
    private String status;
    private String description;

    public JobPosition(String positionID, String title, String field, String location,
                       String employmentType, String status, String description)
    {
        setPositionID(positionID);
        setTitle(title);
        setField(field);
        setLocation(location);
        setEmploymentType(employmentType);
        setStatus(status);
        setDescription(description);
    }

    // Setters
    public void setPositionID(String positionID) {
        if (positionID == null || positionID.trim().isEmpty()) {
            throw new IllegalArgumentException("Position ID cannot be null or empty.");
        }
        String id = positionID.trim();
        this.positionID = id;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty.");
        }
        String t = title.trim().replaceAll("\\s+", " ");
        this.title = t;
    }

    public void setField(String field) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("Field cannot be null or empty.");
        }
        String f = field.trim().replaceAll("\\s+", " ");
        this.field = f;
    }

    public void setLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty.");
        }
        String loc = location.trim().replaceAll("\\s+", " ");
        this.location = loc;
    }

    public void setEmploymentType(String employmentType) {
        if (employmentType == null || employmentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Employment type cannot be null or empty.");
        }
        String type = employmentType.trim().replaceAll("\\s+", " ");
        this.employmentType = type;
    }

    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty.");
        }
        String s = status.trim().replaceAll("\\s+", " ").toLowerCase();
        if (s.equals("active")) {
            this.status = "Active";
        } else if (s.equals("not active") || s.equals("inactive")) {
            this.status = "Not Active";
        } else {
            throw new IllegalArgumentException("Status must be 'Active' or 'Not Active'.");
        }
    }

    public void setDescription(String description) {
        this.description = (description == null) ? "" : description;
    }

    // Getters
    public String getPositionID() {
        return positionID;
    }

    public String getTitle() {
        return title;
    }

    public String getField() {
        return field;
    }

    public String getLocation() {
        return location;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    // toString
    @Override public String toString() {
        return "Position number : " + positionID + " | " + title + " | " + field + " | " + location +
               " | " + employmentType + " | " + status + " | Description: " + description;
    }

    // Methods
    public String getShortSummary() {
        return positionID + " - " + title + " (" + location + ") [" + status + "]";
    }

    public void updateStatus() {
        if (status != null && status.equalsIgnoreCase("Active")) {
            setStatus("Not Active");
        } else {
            setStatus("Active");
        }
    }
}
