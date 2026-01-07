package Model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Publishes
{
    private Company company;
    private JobPosition position;
    private LocalDate publishDate;
    private String postingChannel;

    public Publishes(Company company, JobPosition position, LocalDate publishDate, String postingChannel) {
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

    public void setPublishDate(LocalDate publishDate) {
        this.publishDate = (publishDate == null) ? LocalDate.now() : publishDate;
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
    public LocalDate getPublishDate() {
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
        LocalDate now = LocalDate.now();
        if (publishDate == null) {
            return 0L;
        }
        long days = ChronoUnit.DAYS.between(publishDate, LocalDate.now());
        return Math.max(0, days);
    }

    public void updatePostingChannel(String newChannel) {
        setPostingChannel(newChannel);
    }

}
