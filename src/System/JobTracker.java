package System;

import Model.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Vector;

public class JobTracker
{
    private Vector<UserProfile> users;
    private Vector<JobPosition> positions;
    private Vector<Company> companies;
    private Vector<Event> events;
    private Vector<Document> documents;

    private Vector<ApplyFor> applications;
    private Vector<NotifyAbout> notifications;
    private Vector<Contact> contacts;
    private Vector<Publishes> publishedJobs;
    private Vector<Stores> storedDocuments;

    //used in checkOverdueApplications
    private static final int overdue_days = 14;


    public JobTracker()
    {
        users = new Vector<>();
        companies = new Vector<>();
        positions = new Vector<>();
        events = new Vector<>();
        documents = new Vector<>();

        applications = new Vector<>();
        notifications = new Vector<>();
        contacts = new Vector<>();
        publishedJobs = new Vector<>();
        storedDocuments = new Vector<>();
    }

    // Getters
    public Vector<UserProfile> getUsers()
    {
        return users;
    }

    public Vector<Company> getCompanies()
    {
        return companies;
    }

    public Vector<JobPosition> getPositions()
    {
        return positions;
    }

    public Vector<Event> getEvents()
    {
        return events;
    }

    public Vector<Document> getDocuments()
    {
        return documents;
    }

    public Vector<ApplyFor> getApplications()
    {
        return applications;
    }

    public Vector<NotifyAbout> getNotifications()
    {
        return notifications;
    }

    public Vector<Contact> getContacts()
    {
        return contacts;
    }

    public Vector<Publishes> getPublishedJobs()
    {
        return publishedJobs;
    }

    public Vector<Stores> getStoredDocs()
    {
        return storedDocuments;
    }

    // find helpers
    private UserProfile findUserByEmail(String email)
    {
        if (email == null)
        {
            return null;
        }
        String key = email.trim();
        for (UserProfile user : users)
        {
            if (user.getEmail().equalsIgnoreCase(key))
            {
                return user;
            }
        }
        return null;
    }

    private Company findCompanyByName(String companyName)
    {
        if (companyName == null)
        {
            return null;
        }
        String key = companyName.trim();
        for (Company company : companies)
        {
            if (company.getCompanyName().equalsIgnoreCase(key))
            {
                return company;
            }
        }
        return null;
    }

    private JobPosition findPositionByID(String positionID)
    {
        if (positionID == null)
        {
            return null;
        }
        String key = positionID.trim();
        for (JobPosition position : positions)
        {
            if (position.getPositionID().equalsIgnoreCase(key))
            {
                return position;
            }
        }
        return null;
    }

    //to check if event belongs to user
    private boolean isUserEvent(UserProfile user, Event event)
    {
        if (user == null || event == null) {
            return false;
        }
        for (NotifyAbout notification : notifications)
        {
            if (notification == null) continue;
            if (sameUser(notification.getUser(), user) && notification.getEvent() == event) {
                return true;
            }
        }
        return false;
    }

    private boolean isFinalStage(ApplicationStage stage)
    {
        return stage == ApplicationStage.REJECTED || stage == ApplicationStage.WITHDRAWN || stage == ApplicationStage.OFFER;
    }

    // ==== User Methods ====
    public void addUser(UserProfile user)
    {
        if (user == null)
        {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        if (findUserByEmail(user.getEmail()) != null)
        {
            throw new IllegalArgumentException("User already exists: " + user.getEmail());
        }
        users.add(user);
    }

    //helper to compare users by email
    private boolean sameUser(UserProfile a, UserProfile b) {
        if (a == null || b == null) return false;
        if (a.getEmail() == null || b.getEmail() == null) return false;
        return a.getEmail().equalsIgnoreCase(b.getEmail());
    }

    public UserProfile Login(String email, String password)
    {
        UserProfile user = findUserByEmail(email);
        if (user == null)
        {
            return null;
        }
        return user.authenticate(password) ? user : null;
    }

    // ==== Company Methods ====
    public Company addCompany(Company company)
    {
        if (company == null)
        {
            throw new IllegalArgumentException("Company cannot be null.");
        }
        Company existingCompany = findCompanyByName(company.getCompanyName());
        if (existingCompany != null)
        {
            return existingCompany;
        }
        companies.add(company);
        return company;
    }

    public void addBranch(Company company, String location)
    {
        if (company == null)
        {
            throw new IllegalArgumentException("Company cannot be null.");
        }
        if (location == null || location.trim().isEmpty())
        {
            throw new IllegalArgumentException("Location cannot be null or empty.");
        }
        ArrayList<String> updatedBranches = new ArrayList<>();
        if (company.getBranches() != null)

        {
            updatedBranches.addAll(company.getBranches());
        }
        updatedBranches.add(location.trim());
        company.setBranches(updatedBranches);
    }

    // --- Save companies to file ---
    public void saveCompaniesToFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty.");
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (Company company : companies) {
                if (company != null) {
                    bw.write(company.toFileLine());
                    bw.newLine();
                }
            }
        }
    }

    //--- Load companies from file ---
    public void loadCompaniesFromFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty.");
        }
        File f = new File(filePath);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Company company = Company.fromFileLine(line);
                addCompany(company); //prevents duplicates by name
            }
        }
    }

    // ==== JobPosition + Publishes Methods ====
    public JobPosition addPosition(JobPosition position) {
        if (position == null) {
            throw new IllegalArgumentException("JobPosition cannot be null.");
        }
        JobPosition existingPosition = findPositionByID(position.getPositionID());
        if (existingPosition != null) {
            return existingPosition;
        }
        positions.add(position);
        return position;
    }

    public Publishes addPublishedJob(Company company, JobPosition position, String postingChannel) {
        if (company == null || position == null) {
            throw new IllegalArgumentException("Company and JobPosition cannot be null.");
        }
        for (Publishes publishedJob : publishedJobs) {
            if (publishedJob != null &&
                    publishedJob.getCompany() != null &&
                    publishedJob.getPosition() != null &&
                    publishedJob.getCompany().getCompanyName().equalsIgnoreCase(company.getCompanyName()) &&
                    publishedJob.getPosition().getPositionID().equalsIgnoreCase(position.getPositionID())) {
                //update posting channel
                publishedJob.updatePostingChannel(postingChannel);
                return publishedJob;
            }
        }
        Publishes newPublish = new Publishes(company, position, postingChannel);
        publishedJobs.add(newPublish);
        return newPublish;
    }

    public Vector<JobPosition> viewPositionsForCompany(Company company) {
        Vector<JobPosition> result = new Vector<>();
        if (company == null) {
            return result;
        }
        for (Publishes publishedJob : publishedJobs) {
            if (publishedJob != null && publishedJob.getCompany() != null &&
                    publishedJob.getCompany().getCompanyName().equalsIgnoreCase(company.getCompanyName())) {
                result.add(publishedJob.getPosition());
            }
        }
        return result;
    }

    private void ensurePublishes(Company company, JobPosition position, String channel)
    {
        if (channel == null || channel.trim().isEmpty()) {
            throw new IllegalArgumentException("Source cannot be null/empty.");
        }
        for (Publishes pub : publishedJobs) {
            if (pub == null) continue;

            boolean sameCompany = pub.getCompany().getCompanyName()
                    .equalsIgnoreCase(company.getCompanyName());
            boolean samePosition = pub.getPosition().getPositionID()
                    .equalsIgnoreCase(position.getPositionID());

            if (sameCompany && samePosition) {
                // add channel to existing
                pub.updatePostingChannel(channel);
                return;
            }
        }
        // not found so add new
        publishedJobs.add(new Publishes(company, position, channel));
    }

    // ==== ApplyFor Methods ====
    public ApplyFor addApplication(UserProfile user, JobPosition position, Company company, String source, String notes){
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        if (position == null) throw new IllegalArgumentException("Position cannot be null.");
        if (company == null) throw new IllegalArgumentException("Company cannot be null.");

        //to avoid duplications by name
        Company existingCompany = findCompanyByName(company.getCompanyName());
        if (existingCompany != null) {
            company = existingCompany;
        } else {
            companies.add(company);
        }
        //to avoid duplications by ID
        JobPosition existingPosition = findPositionByID(position.getPositionID());
        if (existingPosition != null) {
            position = existingPosition;
        } else {
            positions.add(position);
        }
        //link company to jobposition using Publishes
        ensurePublishes(company, position, source);
        //create application
        ApplyFor application = new ApplyFor(position, user, source, notes);
        applications.add(application);
        return application;
    }

    public Vector<ApplyFor> viewApplicationsSummary(UserProfile user) {
        Vector<ApplyFor> result = new Vector<>();
        if (user == null) {
            return result;
        }
        for (ApplyFor app : applications) {
            if (app != null && app.getUserProfile() != null && sameUser(app.getUserProfile(), user)) {
                result.add(app);
            }
        }
        return result;
    }

    public Vector<ApplyFor> checkOverdueApplications(UserProfile user) {
        Vector<ApplyFor> result = new Vector<>();
        if (user == null) {
            return result;
        }
        String userEmail = user.getEmail();
        for (ApplyFor app : applications) {
            if (app == null || app.getUserProfile() == null) {
                continue;
            }
            if (!app.getUserProfile().getEmail().equalsIgnoreCase(userEmail)) {
                continue;
            }
            ApplicationStage stage = app.getStage();
            if (isFinalStage(stage)) {
                continue;
            }
            long days = app.timeSinceApplied();
            if (days >= overdue_days) {
                result.add(app);
            }
        }
        return result;
    }

    // ==== Contact Methods ====
    public Contact addContact(UserProfile user, Company company, String contactName, String role,
                              String contactEmail, String contactPhone, LocalDateTime contactDate)
    {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null.");
        }
        Company storedCompany = addCompany(company);
        Contact contact = new Contact(user, storedCompany, contactName, role, contactEmail, contactPhone, contactDate);
        contacts.add(contact);
        return contact;
    }

    // ==== Document + Stores Methods ====
    public Vector<Document> viewDocuments(UserProfile user)
    {
        Vector<Document> result = new Vector<>();
        if (user == null)
        {
            return result;
        }
        for (Stores storedDoc : storedDocuments)
        {
            if (storedDoc != null && storedDoc.getUser() != null && sameUser(storedDoc.getUser(), user) && storedDoc.getDocument() != null) {
                result.add(storedDoc.getDocument());
            }
        }
        return result;
    }

    private Stores findStoreByDocName(UserProfile user, String docName) {
        if (user == null || docName == null) return null;
        String targetName = docName.trim();
        for (Stores s : storedDocuments) {
            if (s == null || s.getUser() == null || s.getDocument() == null) {
                continue;
            }
            if (!sameUser(s.getUser(), user)) {
                continue;
            }

            String existing = s.getDocument().getDocName();
            if (existing != null && existing.equalsIgnoreCase(targetName)) {
                return s;
            }
        }
        return null;
    }

    public Stores uploadDocument(UserProfile user, String docName, String docType, String target, String note, boolean primary ){
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        if (findStoreByDocName(user, docName) != null) {
            throw new IllegalArgumentException("Document with the same name already exists for you: " + docName);
        }
        Document document = new Document(docName, docType, target);
        Stores link = new Stores(user, document, note); //storedAt = now
        if (primary){
            //set existing primary to false
            for (Stores s : storedDocuments) {
                if (s != null && s.getUser() != null && sameUser(s.getUser(), user)) {
                    s.unmarkAsPrimary();
                }
            }
            link.markAsPrimary();
        }
        storedDocuments.add(link);
        return link;
    }

    public Vector<Stores> getUserDocuments(UserProfile user) {
        Vector<Stores> result = new Vector<>();
        if (user == null) {
            return result;
        }

        for (Stores s : storedDocuments) {
            if (s == null || s.getUser() == null) {
                continue;
            }
            if (sameUser(s.getUser(), user)) {
                result.add(s);
            }
        }
        return result;
    }

    public boolean removeDocument(UserProfile user, String docName) {
        Stores s = findStoreByDocName(user, docName);
        if (s == null) {
            return false;
        }
        return storedDocuments.remove(s);
    }

    // --- Primary Document Methods ---
    public boolean markDocumentAsPrimary(UserProfile user, String docName) {
        Stores target = findStoreByDocName(user, docName);
        if (target == null) {
            return false;
        }
        for (Stores s : storedDocuments) {
            if (s == null || s.getUser() == null) {
                continue;
            }
            if (sameUser(s.getUser(), user)) {
                s.unmarkAsPrimary();
            }
        }
        target.markAsPrimary();
        return true;
    }

    public boolean unmarkPrimaryDocument(UserProfile user) {
        if (user == null){
            return false;
        }
        boolean changed = false;
        for (Stores s : storedDocuments) {
            if (s == null || s.getUser() == null) {
                continue;
            }
            if (sameUser(s.getUser(), user) && s.isPrimary()) {
                s.unmarkAsPrimary();
                changed = true;
            }
        }
        return changed;
    }
    public Stores getPrimaryDocument(UserProfile user) {
        if (user == null) return null;
        for (Stores s : storedDocuments) {
            if (s == null || s.getUser() == null) {
                continue;
            }
            if (sameUser(s.getUser(), user) && s.isPrimary()) {
                return s;
            }
        }
        return null;
    }

    // ---- Update Documents ----
    public boolean renameDocument(UserProfile user, String oldName, String newName) {
        Stores s = findStoreByDocName(user, oldName);
        if (s == null) {
            return false;
        }
        //avoid duplicates
        Stores other = findStoreByDocName(user, newName);
        if (other != null) {
            throw new IllegalArgumentException("Cannot rename: another document with this name already exists.");
        }
        s.getDocument().reName(newName);
        return true;
    }

    public boolean updateDocumentTarget(UserProfile user, String docName, String newTarget) {
        Stores s = findStoreByDocName(user, docName);
        if (s == null) return false;
        s.getDocument().updateTarget(newTarget);
        return true;
    }

    public boolean updateDocumentNote(UserProfile user, String docName, String newNote) {
        Stores s = findStoreByDocName(user, docName);
        if (s == null) {
            return false;
        }
        s.updateNote(newNote);
        return true;
    }

    // ==== Event + NotifyAbout Methods ====
    public boolean overlapCheck(UserProfile user, Event newEvent) {
        if (user == null || newEvent == null) {
            return false;
        }
        LocalDateTime newStart = newEvent.getDateTime();
        LocalDateTime newEnd = newEvent.getEndDateTime();
        if (newEnd == null || newStart == null) {
            return false;
        }

        for (Event existingEvent : events) {
            if (existingEvent == null) {
                continue;
            }
            if (!isUserEvent(user, existingEvent)) {
                continue;
            }
            LocalDateTime existingEventStart = existingEvent.getDateTime();
            LocalDateTime existingEventEnd = existingEvent.getEndDateTime();
            if (existingEventStart == null || existingEventEnd == null) {
                continue;
            }
            //check overlap
            boolean overlap = newStart.isBefore(existingEventEnd) && existingEventStart.isBefore(newEnd);
            if (overlap) {
                return true;
            }
        }
        return false;
    }

    public void addEventToCalendar(UserProfile user, Event event) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }
        if (overlapCheck(user, event)) {
            throw new IllegalArgumentException("Event overlaps with an existing event.");
        }
        events.add(event);
        //create a notification for this event
        String msg = "Upcoming event: " + event.getTitle();
        NotifyAbout n = new NotifyAbout(user, event, msg);
        notifications.add(n);
    }

    public void cancelEvent(UserProfile user, Event event) {
        if (user == null || event == null) {
            return;
        }
        // only if it belongs to a user
        if (!isUserEvent(user, event)) {
            return;
        }
        // remove notifications that link this user to this event
        for (int i = notifications.size() - 1; i >= 0; i--) {
            NotifyAbout n = notifications.get(i);
            if (n != null && sameUser(n.getUser(), user) && n.getEvent() == event) {
                notifications.remove(i);
            }
        }
        //remove the event itself
        events.remove(event);
    }

    public void reScheduleEvent(UserProfile user, Event event, LocalDateTime newDateTime, int newDuration) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }
        if (!isUserEvent(user, event)) {
            throw new IllegalArgumentException("Event does not belong to user.");
        }
        //temporary update to check overlap
        LocalDateTime oldDT = event.getDateTime();
        int oldDur = event.getDuration();
        event.setDateTime(newDateTime);
        event.setDuration(newDuration);
        event.resetNotification();

        //if overlap
        if (overlapCheck(user, event)) {
            event.setDateTime(oldDT);
            event.setDuration(oldDur);
            throw new IllegalArgumentException("Reschedule creates overlap with another event.");
        }
    }

    public Vector<Event> viewEvents(UserProfile user, int month, int year) {
        Vector<Event> out = new Vector<>();
        if (user == null) return out;
        for (Event e : events) {
            if (e == null) {
                continue;
            }
            if (!isUserEvent(user, e)) {
                continue;
            }
            if (e.getDateTime() == null) {
                continue;
            }
            if (e.getDateTime().getYear() == year && e.getDateTime().getMonthValue() == month) {
                out.add(e);
            }
        }
        return out;
    }

    public Vector<NotifyAbout> viewNotifications(UserProfile user) {
        Vector<NotifyAbout> out = new Vector<>();
        if (user == null) return out;
        for (NotifyAbout n : notifications) {
            if (n != null && sameUser(n.getUser(), user)) {
                out.add(n);
            }
        }
        return out;
    }

    public NotifyAbout sendNotification(UserProfile user, Event event, String msg) {
        if (user == null) {
            throw new IllegalArgumentException("UserProfile cannot be null.");
        }
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }
        NotifyAbout n = new NotifyAbout(user, event, msg);
        notifications.add(n);
        return n;
    }

    // ==== Statistics Methods ====
    public String buildUserStatistics(UserProfile user) {
        if (user == null) {
            return "No user";
        }
        int totalApps = 0;
        int activeApps = 0;
        int totalEvents = 0;
        int upcomingEvents = 0;
        for (ApplyFor a : applications) {
            if (a != null && a.getUserProfile() != null && sameUser(a.getUserProfile(), user)) {
                totalApps++;
                if (!isFinalStage(a.getStage())) {
                    activeApps++;
                }
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (Event e : events) {
            if (e != null && isUserEvent(user, e)) {
                totalEvents++;
                if (e.getDateTime() != null && e.getDateTime().isAfter(now)) {
                    upcomingEvents++;
                }
            }
        }
        return "Statistics for " + user.getEmail() +
                " | Applications: " + totalApps +
                " (Active: " + activeApps + ")" +
                " | Events: " + totalEvents +
                " (Upcoming: " + upcomingEvents + ")";
    }
}

