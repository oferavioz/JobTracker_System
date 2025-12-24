package Threads;

import Model.ApplyFor;
import Model.Company;
import Model.Contact;
import Model.JobPosition;
import Model.UserProfile;
import System.JobTracker;

import java.time.LocalDateTime;

public class AddApplicationTask implements Runnable {
    private final JobTracker tracker;

    private final UserProfile user;
    private final JobPosition position;
    private final Company company;

    private final String source;   //where the user applied (LinkedIn, company site, etc.)
    private final String notes;

    // optional contact info
    private final String contactName;
    private final String role;
    private final String contactEmail;
    private final String contactPhone;
    private final LocalDateTime contactDate;

    private ApplyFor createdApplication;
    private Contact createdContact;
    private String errorMessage = "";
    private boolean success = false;

    public AddApplicationTask(JobTracker tracker,
                              UserProfile user,
                              JobPosition position,
                              Company company,
                              String source,
                              String notes,
                              String contactName,
                              String role,
                              String contactEmail,
                              String contactPhone,
                              LocalDateTime contactDate) {
        this.tracker = tracker;
        this.user = user;
        this.position = position;
        this.company = company;
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
            synchronized (tracker) {
                createdApplication = tracker.addApplication(user, position, company, source, notes);

                //contact- only if name was provided
                if (contactName != null && !contactName.trim().isEmpty()) {
                    createdContact = tracker.addContact(user, company, contactName, role, contactEmail, contactPhone, contactDate);
                }
            }
            success = true;
        } catch (Exception ex) {
            errorMessage = ex.getMessage();
            success = false;
        }
    }

    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public ApplyFor getCreatedApplication() { return createdApplication; }
    public Contact getCreatedContact() { return createdContact; }
}