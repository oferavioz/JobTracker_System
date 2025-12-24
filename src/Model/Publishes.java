package Model;

import java.time.LocalDateTime;

public class Publishes
{
    private Company company;
    private JobPosition position;
    private LocalDateTime publishDate;
    private String postingChannel;

    public Publishes(Company company, JobPosition position, String postingChannel) {
        setCompany(company);
        setPosition(position);
        setPublishDate(LocalDateTime.now());
        setPostingChannel(postingChannel);
    }

    public Publishes(Company company, JobPosition position, LocalDateTime publishDate, String postingChannel)
    {
        setCompany(company);
        setPosition(position);
        setPublishDate(publishDate);
        setPostingChannel(postingChannel);
    }

    // Setters
    public void setCompany(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null.");
        }
        this.company = company;
    }

    public void setPosition(JobPosition position) {
        if (position == null) {
            throw new IllegalArgumentException("JobPosition cannot be null.");
        }
        this.position = position;
    }

    public void setPublishDate(LocalDateTime publishDate) {
        this.publishDate = (publishDate == null) ? LocalDateTime.now() : publishDate;
    }

    public void setPostingChannel(String postingChannel) {
        if (postingChannel == null || postingChannel.trim().isEmpty()) {
            throw new IllegalArgumentException("Posting channel cannot be null or empty.");
        }
        String value = postingChannel.trim().replaceAll("\\s+", " ");
        this.postingChannel = value;
    }

    // Getters
    public Company getCompany() {
        return company;
    }
    public JobPosition getPosition() {
        return position;
    }
    public LocalDateTime getPublishDate() {
        return publishDate;
    }
    public String getPostingChannel() {
        return postingChannel;
    }

    // toString
    @Override public String toString() {
        return "Publishes: " + company.getCompanyName() +
                " -> " + position.getPositionID() + " (" + position.getTitle() + ")" +
                " | Published: " + publishDate +
                (postingChannel == null || postingChannel.isEmpty() ? "" : " | Channel: " + postingChannel);
    }

    // Methods
    public Long daysSincePublish() {
        LocalDateTime now = LocalDateTime.now();
        if (publishDate == null) {
            return 0L;
        }
        long days = java.time.Duration.between(publishDate, now).toDays();
        return (days < 0) ? 0L : days;
    }

    public void updatePostingChannel(String newChannel) {
        if (newChannel == null || newChannel.trim().isEmpty()) {
            return;
        }
        String value = newChannel.trim().replaceAll("\\s+", " ");
        //check if the value already exists in postingChannel
        if (postingChannel == null || postingChannel.trim().isEmpty()) {
            setPostingChannel(value);
            return;
        }
        //avoid duplicates
        String[] lines = postingChannel.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().equalsIgnoreCase(value)) {
                return;
            }
        }
        postingChannel += "\n" + value;
    }

}
