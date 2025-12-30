package Threads;

import Model.ApplyFor;
import Model.Company;
import Model.Contact;
import Model.JobPosition;
import Model.UserProfile;
import System.JobTracker;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AddApplicationTask implements Runnable {
    private final JobTracker tracker;

    private final UserProfile user;
    private final JobPosition position;
    private final Company company;
    private final LocalDate publishDate;

    private final String source;
    private final String notes;

    private final String contactName;
    private final String role;
    private final String contactEmail;
    private final String contactPhone;
    private final LocalDateTime contactDate; // last communication

    private ApplyFor createdApplication;
    private Contact createdContact;
    private String errorMessage = "";
    private boolean success = false;

    public AddApplicationTask(JobTracker tracker, UserProfile user, JobPosition position, Company company, LocalDate publishDate,
                              String source, String notes,
                              String contactName, String role, String contactEmail, String contactPhone,
                              LocalDateTime contactDate) {
        this.tracker = tracker;
        this.user = user;
        this.position = position;
        this.company = company;
        this.publishDate = publishDate;
        this.source = source;
        this.notes = notes;

        this.contactName = contactName;
        this.role = role;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.contactDate = contactDate;
    }

    @Override
    public void run() {
        try {
            //create application (includes company merge + branch + publish)
            createdApplication = tracker.addApplication(user, position, company, publishDate, source, notes);

            //optional - create/update contact + link to position
            if (contactName != null && !contactName.trim().isEmpty()) {
                String pid = createdApplication.getPosition().getPositionID();
                Company storedCompany = tracker.getCompanyForPosition(pid);

                //if for any reason company isn't linked, fall back to the passed company
                if (storedCompany == null) storedCompany = company;

                LocalDateTime last = (contactDate != null) ? contactDate : LocalDateTime.now();
                createdContact = tracker.addContact(user, storedCompany, contactName, role, contactEmail, contactPhone, last);
                tracker.setContactForPosition(user, pid, createdContact.getContactName());
            }

            success = true;
        } catch (Exception ex) {
            errorMessage = (ex.getMessage() != null) ? ex.getMessage() : ex.toString();
            success = false;
        }
    }

    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public ApplyFor getCreatedApplication() { return createdApplication; }
    public Contact getCreatedContact() { return createdContact; }
}