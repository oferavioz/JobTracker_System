package Model;

import java.time.LocalDateTime;

public class Contact
{
    private UserProfile user;
    private Company company;
    private String contactName;
    private String role;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime contactDate;

    private String lastContactMethod;
    private String lastContactSubject;

    public Contact(UserProfile user, Company company, String contactName,
                   String role, String contactEmail, String contactPhone) {
        setUser(user);
        setCompany(company);
        setContactName(contactName);
        setRole(role);
        setContactEmail(contactEmail);
        setContactPhone(contactPhone);
        setContactDate(LocalDateTime.now());

        this.lastContactMethod = "";
        this.lastContactSubject = "";
    }

    public Contact(UserProfile user, Company company, String contactName, String role,
                   String contactEmail, String contactPhone, LocalDateTime contactDate)
    {
        setUser(user);
        setCompany(company);
        setContactName(contactName);
        setRole(role);
        setContactEmail(contactEmail);
        setContactPhone(contactPhone);
        setContactDate(contactDate);

        this.lastContactMethod = "";
        this.lastContactSubject = "";
    }

    // Setters
    public void setUser(UserProfile user) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        this.user = user;
    }

    public void setCompany(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null.");
        }
        this.company = company;
    }

    public void setContactName(String contactName) {
        if (contactName == null || contactName.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact name cannot be null or empty.");
        }
        this.contactName = contactName.trim().replaceAll("\\s+", " ");
    }

    public void setRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            this.role = "";
            return;
        }
        this.role = role.trim().replaceAll("\\s+", " ");
    }

    public void setContactEmail(String contactEmail) {
        if (contactEmail == null || contactEmail.trim().isEmpty()) {
            this.contactEmail = "";
            return;
        }
        String value = contactEmail.trim();
        if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\..+$")) {
            throw new IllegalArgumentException("Invalid contact email format.");
        }
        this.contactEmail = value;
    }

    public void setContactPhone(String contactPhone) {
        if (contactPhone == null || contactPhone.trim().isEmpty()) {
            this.contactPhone = "";
            return;
        }
        String value = contactPhone.trim();
        if (!value.matches("^\\d{3}-\\d{7}$")) {
            throw new IllegalArgumentException("Invalid phone format. Expected: XXX-XXXXXXX");
        }
        this.contactPhone = value;
    }

    public void setContactDate(LocalDateTime contactDate) {
        this.contactDate = (contactDate == null) ? LocalDateTime.now() : contactDate;
    }

    public void setLastContactMethod(String lastContactMethod) {
        if (lastContactMethod == null || lastContactMethod.trim().isEmpty()) {
            this.lastContactMethod = "";
            return;
        }
        this.lastContactMethod = lastContactMethod.trim().replaceAll("\\s+", " ");
    }

    public void setLastContactSubject(String lastContactSubject) {
        if (lastContactSubject == null || lastContactSubject.trim().isEmpty()) {
            this.lastContactSubject = "";
            return;
        }
        this.lastContactSubject = lastContactSubject.trim().replaceAll("\\s+", " ");
    }

    // Getters
    public UserProfile getUser() {
        return user;
    }

    public Company getCompany() {
        return company;
    }

    public String getContactName() {
        return contactName;
    }

    public String getRole() {
        return role;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public LocalDateTime getContactDate() {
        return contactDate;
    }

    public String getLastContactMethod() {
        return lastContactMethod;
    }

    public String getLastContactSubject() {
        return lastContactSubject;
    }

    // toString
    @Override
    public String toString() {
        return "Contact: " + user.getEmail() + " -> " + company.getCompanyName() +
                " | Name: " + contactName +
                (role == null || role.isEmpty() ? "" : " | Role: " + role) +
                (contactEmail == null || contactEmail.isEmpty() ? "" : " | Email: " + contactEmail) +
                (contactPhone == null || contactPhone.isEmpty() ? "" : " | Phone: " + contactPhone) +
                " | Date: " + contactDate + (lastContactMethod == null ||
                lastContactMethod.isEmpty() ? "" : " | Method: " + lastContactMethod) +
                (lastContactSubject == null || lastContactSubject.isEmpty() ? "" : " | Subject: " + lastContactSubject);
    }

    // Methods
    public String getContactCard() {
        String card = company.getCompanyName() + " | " + contactName;
        if (role != null && !role.isEmpty()) {
            card += " (" + role + ")";
        }
        if (contactEmail != null && !contactEmail.isEmpty()) {
            card += " | " + contactEmail;
        }
        if (contactPhone != null && !contactPhone.isEmpty()) {
            card += " | " + contactPhone;
        }
        return card;
    }

    public void updateContactInfo(String role, String contactEmail, String contactPhone) {
        setRole(role);
        setContactEmail(contactEmail);
        setContactPhone(contactPhone);
    }

    public long timeSinceLastContact() {
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(contactDate, now).toDays();
    }

    public void updateContactDate(LocalDateTime newDate) {
        setContactDate(newDate);
    }

    public String getLastContactInfo() {
        String info = "Last contact: " + contactDate;
        if (lastContactMethod != null && !lastContactMethod.isEmpty()) {
            info += " (" + lastContactMethod + ")";
        }
        if (lastContactSubject != null && !lastContactSubject.isEmpty()) {
            info += " - " + lastContactSubject;
        }
        return info;
    }

    public void logContact(String method, String subject) {
        if ((method == null || method.trim().isEmpty()) &&
                (subject == null || subject.trim().isEmpty())) {
            throw new IllegalArgumentException("You must provide method or subject.");
        }
        setLastContactMethod(method);
        setLastContactSubject(subject);
        setContactDate(LocalDateTime.now());
    }

}
