package System;

import Model.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JobTracker {
    private final Vector<UserProfile> users = new Vector<>();
    private final Vector<Company> companies = new Vector<>();
    private final Vector<JobPosition> positions = new Vector<>();
    private final Vector<Event> events = new Vector<>();
    private final Vector<Document> documents = new Vector<>();

    //Relations
    private final Vector<ApplyFor> applications = new Vector<>();
    private final Vector<NotifyAbout> notifications = new Vector<>();
    private final Vector<Contact> contacts = new Vector<>();
    private final Vector<Publishes> publishedJobs = new Vector<>();
    private final Vector<Stores> storedDocuments = new Vector<>();

    // One contact per position (per user)
    private final Map<String, String> positionContact = new HashMap<>();

    private static final int OVERDUE_DAYS = 14;

    public JobTracker() {}

    // =========================================================
    // ==================== Internal Helpers ====================
    // =========================================================

    private boolean sameUser(UserProfile a, UserProfile b) {
        if (a == null || b == null) return false;
        if (a.getEmail() == null || b.getEmail() == null) return false;
        return a.getEmail().equalsIgnoreCase(b.getEmail());
    }

    //trim for strings
    private String norm(String s) {
        return (s == null) ? null : s.trim();
    }

    private String posKey(UserProfile user, String positionID) {
        if (user == null || user.getEmail() == null || positionID == null) return null;
        return user.getEmail().trim().toLowerCase() + "|" + positionID.trim().toLowerCase();
    }

    private UserProfile findUserByEmail(String email) {
        String key = norm(email);
        if (key == null || key.isEmpty()) return null;
        for (UserProfile u : users) {
            if (u != null && u.getEmail() != null && u.getEmail().equalsIgnoreCase(key)) return u;
        }
        return null;
    }

    private Company findCompanyByName(String companyName) {
        String key = norm(companyName);
        if (key == null || key.isEmpty()) return null;
        for (Company c : companies) {
            if (c != null && c.getCompanyName() != null && c.getCompanyName().equalsIgnoreCase(key)) return c;
        }
        return null;
    }

    private JobPosition findPositionByID(String positionID) {
        String key = norm(positionID);
        if (key == null || key.isEmpty()) return null;
        for (JobPosition p : positions) {
            if (p != null && p.getPositionID() != null && p.getPositionID().equalsIgnoreCase(key)) return p;
        }
        return null;
    }

    private ApplyFor findApplication(UserProfile user, String positionID) {
        if (user == null) return null;
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) return null;

        for (ApplyFor app : applications) {
            if (app == null || app.getUserProfile() == null || app.getPosition() == null) continue;
            if (!sameUser(app.getUserProfile(), user)) continue;

            String existing = app.getPosition().getPositionID();
            if (existing != null && existing.equalsIgnoreCase(pid)) return app;
        }
        return null;
    }

    private Contact findContact(UserProfile user, String companyName, String contactName) {
        if (user == null) return null;
        String cKey = norm(companyName);
        String nKey = norm(contactName);
        if (cKey == null || cKey.isEmpty() || nKey == null || nKey.isEmpty()) return null;
        for (Contact c : contacts) {
            if (c == null || c.getUser() == null || c.getCompany() == null) continue;
            if (!sameUser(c.getUser(), user)) continue;

            String cName = (c.getCompany() != null) ? c.getCompany().getCompanyName() : null;
            String cn = c.getContactName();
            if (cName == null || cn == null) continue;
            if (cName.equalsIgnoreCase(cKey) && cn.equalsIgnoreCase(nKey)) return c;
        }
        return null;
    }

    private Stores findStoreByDocName(UserProfile user, String docName) {
        if (user == null) return null;
        String target = norm(docName);
        if (target == null || target.isEmpty()) return null;
        for (Stores s : storedDocuments) {
            if (s == null || s.getUser() == null || s.getDocument() == null) continue;
            if (!sameUser(s.getUser(), user)) continue;
            String existing = s.getDocument().getDocName();
            if (existing != null && existing.equalsIgnoreCase(target)) return s;
        }
        return null;
    }

    //Event belongs to user if NotifyAbout links them
    private boolean isUserEvent(UserProfile user, Event event) {
        if (user == null || event == null) return false;
        int id = event.getEventID();

        for (NotifyAbout n : notifications) {
            if (n == null) continue;
            if (!sameUser(n.getUser(), user)) continue;
            Event e = n.getEvent();
            if (e != null && e.getEventID() == id) return true;
        }
        return false;
    }

    //avoids duplicates (company and position), updates posting channel if already exists
    private Publishes upsertPublish(Company company, JobPosition position, String postingChannel) {
        if (company == null || position == null) {
            throw new IllegalArgumentException("Company and JobPosition cannot be null.");
        }
        String channel = norm(postingChannel);
        if (channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Posting channel cannot be null/empty.");
        }
        Company storedCompany = addCompany(company);
        JobPosition storedPosition = addPosition(position);
        for (Publishes pub : publishedJobs) {
            if (pub == null || pub.getCompany() == null || pub.getPosition() == null) continue;

            boolean sameCompany = pub.getCompany().getCompanyName().equalsIgnoreCase(storedCompany.getCompanyName());
            boolean samePosition = pub.getPosition().getPositionID().equalsIgnoreCase(storedPosition.getPositionID());
            if (sameCompany && samePosition) {
                pub.updatePostingChannel(channel);
                return pub;
            }
        }

        Publishes created = new Publishes(storedCompany, storedPosition, channel);
        publishedJobs.add(created);
        return created;
    }

    private Publishes findPublishByPositionId(String positionID) {
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) return null;
        for (Publishes p : publishedJobs) {
            if (p == null || p.getCompany() == null || p.getPosition() == null) continue;
            String existing = p.getPosition().getPositionID();
            if (existing != null && existing.equalsIgnoreCase(pid)) return p;
        }
        return null;
    }

    private Company findCompanyForPosition(String positionID) {
        Publishes p = findPublishByPositionId(positionID);
        return (p == null) ? null : p.getCompany();
    }

    private NotifyAbout resetNotificationForEvent(UserProfile user, Event event) {
        if (user == null || event == null) return null;
        for (int i = notifications.size() - 1; i >= 0; i--) {
            NotifyAbout n = notifications.get(i);
            if (n == null) continue;
            if (!sameUser(n.getUser(), user)) continue;
            if (n.getEvent() != null && n.getEvent().getEventID() == event.getEventID()) {
                notifications.remove(i);
            }
        }
        event.setNotified(false);
        NotifyAbout fresh = new NotifyAbout(user, event);
        notifications.add(fresh);
        return fresh;
    }

    // =========================================================
    // ==================== User Methods =======================
    // =========================================================

    public void addUser(UserProfile user) {
        if (user == null) throw new IllegalArgumentException("UserProfile cannot be null.");
        if (findUserByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException("User already exists: " + user.getEmail());
        }
        users.add(user);
    }

    public UserProfile registerUser(String fullName, String email, String password, String phone, String field) {
        UserProfile u = new UserProfile(fullName, email, password, phone, field);
        addUser(u);
        return u;
    }

    public UserProfile login(String email, String password) {
        UserProfile user = findUserByEmail(email);
        if (user == null) return null;
        return user.authenticate(password) ? user : null;
    }

    public void changePassword(UserProfile activeUser, String oldPass, String newPass) {
        if (activeUser == null) throw new IllegalArgumentException("UserProfile cannot be null.");
        activeUser.changePassword(oldPass, newPass);
    }

    public void editProfile(UserProfile activeUser, String newFullName, String newPhone, String newFieldOfSearch) {
        if (activeUser == null) throw new IllegalArgumentException("UserProfile cannot be null.");
        UserProfile stored = findUserByEmail(activeUser.getEmail());
        if (stored == null) throw new IllegalArgumentException("User does not exist in the system.");
        stored.updateProfile(newFullName, newPhone, newFieldOfSearch);
    }

    public UserProfile getUserByEmail(String email) {
        return findUserByEmail(email);
    }

    // =========================================================
    // ==================== Company Methods ====================
    // =========================================================

    public Company addCompany(Company company) {
        if (company == null) throw new IllegalArgumentException("Company cannot be null.");
        Company existing = findCompanyByName(company.getCompanyName());
        if (existing != null) return existing;
        companies.add(company);
        return company;
    }

    public Company getCompanyByName(String companyName) {
        return findCompanyByName(companyName);
    }

    public Vector<Company> listCompanies() {
        return new Vector<>(companies);
    }

    public Vector<Company> searchCompanies(String query) {
        Vector<Company> out = new Vector<>();
        String q = norm(query);
        if (q == null || q.isEmpty()) return out;

        String key = q.toLowerCase();
        for (Company c : companies) {
            if (c == null) continue;
            String name = c.getCompanyName();
            String ind = c.getIndustry();
            if ((name != null && name.toLowerCase().contains(key)) ||
                    (ind != null && ind.toLowerCase().contains(key))) {
                out.add(c);
            }
        }
        return out;
    }

    public void addBranch(Company company, String location) {
        if (company == null) throw new IllegalArgumentException("Company cannot be null.");
        String loc = norm(location);
        if (loc == null || loc.isEmpty()) throw new IllegalArgumentException("Location cannot be null or empty.");

        ArrayList<String> updated = new ArrayList<>();
        if (company.getBranches() != null) updated.addAll(company.getBranches());
        updated.add(loc);
        company.setBranches(updated);
    }

    public boolean updateCompanyDetails(String companyName, String industry, String websiteUrl) {
        Company c = findCompanyByName(companyName);
        if (c == null) return false;
        if (industry != null) c.setIndustry(industry);
        if (websiteUrl != null) c.setWebsiteURL(websiteUrl);
        return true;
    }

    //Save + Load Companies to/from file
    public void saveCompaniesToFile(String filePath) throws IOException {
        String path = norm(filePath);
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("File path cannot be empty.");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            for (Company c : companies) {
                if (c == null) continue;
                bw.write(c.toFileLine());
                bw.newLine();
            }
        }
    }

    public void loadCompaniesFromFile(String filePath) throws IOException {
        String path = norm(filePath);
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("File path cannot be empty.");

        File f = new File(path);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Company company = Company.fromFileLine(line);
                addCompany(company);
            }
        }
    }

    // =========================================================
    // ==================== Position + Publish ==================
    // =========================================================

    public JobPosition addPosition(JobPosition position) {
        if (position == null) throw new IllegalArgumentException("JobPosition cannot be null.");
        JobPosition existing = findPositionByID(position.getPositionID());
        if (existing != null) return existing;
        positions.add(position);
        return position;
    }

    public JobPosition getPositionByID(String positionID) {
        return findPositionByID(positionID);
    }

    public boolean togglePositionStatus(String positionID) {
        JobPosition p = findPositionByID(positionID);
        if (p == null) return false;
        p.updateStatus();
        return true;
    }

    public boolean updatePositionDetails(String positionID,
                                         String title, String field, String location,
                                         String employmentType, String status, String description) {
        JobPosition p = findPositionByID(positionID);
        if (p == null) return false;

        if (title != null) p.setTitle(title);
        if (field != null) p.setField(field);
        if (location != null) p.setLocation(location);
        if (employmentType != null) p.setEmploymentType(employmentType);
        if (status != null) p.setStatus(status);
        if (description != null) p.setDescription(description);
        return true;
    }

    //links Company <-> Position
    public Publishes publishJob(Company company, JobPosition position, String postingChannel) {
        return upsertPublish(company, position, postingChannel);
    }

    public Publishes getPublishByPositionId(String positionID) {
        return findPublishByPositionId(positionID);
    }

    public Vector<JobPosition> listPositionsForCompany(Company company) {
        Vector<JobPosition> out = new Vector<>();
        if (company == null || company.getCompanyName() == null) return out;

        for (Publishes p : publishedJobs) {
            if (p == null || p.getCompany() == null || p.getPosition() == null) continue;
            if (p.getCompany().getCompanyName().equalsIgnoreCase(company.getCompanyName())) {
                out.add(p.getPosition());
            }
        }
        return out;
    }

    public Vector<Publishes> listPublishedJobsForCompany(Company company) {
        Vector<Publishes> out = new Vector<>();
        if (company == null || company.getCompanyName() == null) return out;

        for (Publishes p : publishedJobs) {
            if (p == null || p.getCompany() == null) continue;
            if (p.getCompany().getCompanyName().equalsIgnoreCase(company.getCompanyName())) out.add(p);
        }
        return out;
    }

    // =========================================================
    // ==================== Applications Methods ================
    // =========================================================

    public ApplyFor addApplication(UserProfile user, JobPosition position, Company company, String source, String notes) {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        if (position == null) throw new IllegalArgumentException("Position cannot be null.");
        if (company == null) throw new IllegalArgumentException("Company cannot be null.");

        //store + link publish (company<->position)
        Publishes pub = upsertPublish(company, position, source);
        JobPosition storedPosition = pub.getPosition();
        //prevent duplicates per user+position
        ApplyFor existing = findApplication(user, storedPosition.getPositionID());
        if (existing != null) return existing;
        ApplyFor created = new ApplyFor(storedPosition, user, source, notes);
        applications.add(created);
        return created;
    }

    //for gui form submission
    public ApplyFor addApplicationFromForm(UserProfile user, String positionId, String title, String field, String location,
            String employmentType, String description, String companyName, String industry, String website,
            String source, String notes, String contactName, String contactRole, String contactEmail, String contactPhone) {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        Company company = new Company(companyName, industry, website);
        JobPosition position = new JobPosition(positionId, title, field, location, employmentType, "Active", description);

        ApplyFor app = addApplication(user, position, company, source, notes);
        String cName = norm(contactName);
        if (cName != null && !cName.isEmpty()) {
            addContact(user, company, cName, contactRole, contactEmail, contactPhone, LocalDateTime.now());
            setContactForPosition(user, positionId, cName);
        }
        return app;
    }

    public Vector<ApplyFor> listApplications(UserProfile user) {
        Vector<ApplyFor> out = new Vector<>();
        if (user == null) return out;

        for (ApplyFor app : applications) {
            if (app != null && app.getUserProfile() != null && sameUser(app.getUserProfile(), user)) {
                out.add(app);
            }
        }
        //newest first
        out.sort((a, b) -> {
            if (a == null || a.getDateApplied() == null) return 1;
            if (b == null || b.getDateApplied() == null) return -1;
            return b.getDateApplied().compareTo(a.getDateApplied());
        });

        return out;
    }

    public ApplyFor getApplication(UserProfile user, String positionID) {
        return findApplication(user, positionID);
    }

    public boolean updateApplicationStage(UserProfile user, String positionID, ApplicationStage newStage) {
        ApplyFor app = findApplication(user, positionID);
        if (app == null) return false;
        app.updateStage(newStage);
        return true;
    }

    //appends to the notes field
    public boolean addApplicationNote(UserProfile user, String positionID, String note) {
        ApplyFor app = findApplication(user, positionID);
        if (app == null) return false;
        app.addNote(note);
        return true;
    }

    //replace the entire notes field
    public boolean setApplicationNotes(UserProfile user, String positionID, String notes) {
        ApplyFor app = findApplication(user, positionID);
        if (app == null) return false;
        app.setNotes(notes);
        return true;
    }

    public boolean updateApplicationSource(UserProfile user, String positionID, String source) {
        ApplyFor app = findApplication(user, positionID);
        if (app == null) return false;
        app.setSource(source);
        return true;
    }

    public boolean withdrawApplication(UserProfile user, String positionID) {
        return updateApplicationStage(user, positionID, ApplicationStage.WITHDRAWN);
    }

    public boolean removeApplication(UserProfile user, String positionID) {
        ApplyFor app = findApplication(user, positionID);
        if (app == null) return false;

        boolean removed = applications.remove(app);
        if (removed) clearContactForPosition(user, positionID);
        return removed;
    }

    public Vector<ApplyFor> listApplicationsForCompany(UserProfile user, Company company) {
        Vector<ApplyFor> out = new Vector<>();
        if (user == null || company == null) return out;

        Vector<JobPosition> companyPositions = listPositionsForCompany(company);
        for (ApplyFor app : applications) {
            if (app == null || app.getUserProfile() == null || app.getPosition() == null) continue;
            if (!sameUser(app.getUserProfile(), user)) continue;

            for (JobPosition p : companyPositions) {
                if (p != null && app.getPosition().getPositionID().equalsIgnoreCase(p.getPositionID())) {
                    out.add(app);
                    break;
                }
            }
        }
        return out;
    }

    public Vector<ApplyFor> checkOverdueApplications(UserProfile user) {
        Vector<ApplyFor> out = new Vector<>();
        if (user == null) return out;

        for (ApplyFor app : applications) {
            if (app == null || app.getUserProfile() == null) continue;
            if (!sameUser(app.getUserProfile(), user)) continue;

            if (isFinalStage(app.getStage())) continue;

            long days = app.timeSinceApplied();
            if (days >= OVERDUE_DAYS) out.add(app);
        }
        return out;
    }

    public Company getCompanyForPosition(String positionID) {
        return findCompanyForPosition(positionID);
    }

    public String getPostingChannelForPosition(String positionID) {
        Publishes p = findPublishByPositionId(positionID);
        return (p == null) ? null : p.getPostingChannel();
    }


    // =========================================================
    // ==================== Contacts Methods ====================
    // =========================================================

    public Contact addContact(UserProfile user, Company company, String contactName, String role,
                              String contactEmail, String contactPhone, LocalDateTime contactDate) {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        if (company == null) throw new IllegalArgumentException("Company cannot be null.");

        Company storedCompany = addCompany(company);

        String cName = norm(contactName);
        if (cName == null || cName.isEmpty()) throw new IllegalArgumentException("Contact name cannot be empty.");

        Contact existing = findContact(user, storedCompany.getCompanyName(), cName);
        if (existing != null) {
            existing.updateContactInfo(role, contactEmail, contactPhone);
            if (contactDate != null) existing.updateContactDate(contactDate);
            return existing;
        }

        Contact created = new Contact(user, storedCompany, cName, role, contactEmail, contactPhone, contactDate);
        contacts.add(created);
        return created;
    }

    public Vector<Contact> listContacts(UserProfile user) {
        Vector<Contact> out = new Vector<>();
        if (user == null) return out;

        for (Contact c : contacts) {
            if (c != null && c.getUser() != null && sameUser(c.getUser(), user)) out.add(c);
        }
        return out;
    }

    public Vector<Contact> listContactsForCompany(UserProfile user, Company company) {
        Vector<Contact> out = new Vector<>();
        if (user == null || company == null || company.getCompanyName() == null) return out;

        for (Contact c : contacts) {
            if (c == null || c.getUser() == null || c.getCompany() == null) continue;
            if (!sameUser(c.getUser(), user)) continue;
            if (c.getCompany().getCompanyName().equalsIgnoreCase(company.getCompanyName())) out.add(c);
        }
        return out;
    }

    public boolean updateContactInfo(UserProfile user, String companyName, String contactName,
                                     String role, String email, String phone) {
        Contact c = findContact(user, companyName, contactName);
        if (c == null) return false;
        c.updateContactInfo(role, email, phone);
        return true;
    }

    public boolean logContact(UserProfile user, String companyName, String contactName, String method, String subject) {
        Contact c = findContact(user, companyName, contactName);
        if (c == null) return false;
        c.logContact(method, subject);
        return true;
    }

    public boolean removeContact(UserProfile user, String companyName, String contactName) {
        Contact c = findContact(user, companyName, contactName);
        if (c == null) return false;
        boolean removed = contacts.remove(c);
        if (!removed) return false;
        // clear mapping
        String uEmail = (user != null && user.getEmail() != null) ? user.getEmail().trim().toLowerCase() : null;
        if (uEmail == null) return true;

        String targetCompany = norm(companyName);
        String targetContact = norm(contactName);
        if (targetCompany == null || targetContact == null) return true;
        for (Publishes p : publishedJobs) {
            if (p == null || p.getCompany() == null || p.getPosition() == null) continue;
            String cName = p.getCompany().getCompanyName();
            String pid = p.getPosition().getPositionID();
            if (cName == null || pid == null) continue;
            if (cName.equalsIgnoreCase(targetCompany)) {
                String key = uEmail + "|" + pid.trim().toLowerCase();
                String mapped = positionContact.get(key);
                if (mapped != null && mapped.equalsIgnoreCase(targetContact)) {
                    positionContact.remove(key);
                }
            }
        }
        return true;
    }

    public Contact getContact(UserProfile user, String companyName, String contactName) {
        return findContact(user, companyName, contactName);
    }

    //one contact per position (per user)
    public boolean setContactForPosition(UserProfile user, String positionID, String contactName) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) return false;
        String pid = norm(positionID);
        String cn = norm(contactName);
        if (pid == null || pid.isEmpty() || cn == null || cn.isEmpty()) return false;

        Company comp = findCompanyForPosition(pid);
        if (comp == null || comp.getCompanyName() == null) return false;

        Contact c = findContact(user, comp.getCompanyName(), cn);
        if (c == null) return false;

        positionContact.put(posKey(user, pid), c.getContactName().trim());
        return true;
    }

    public boolean clearContactForPosition(UserProfile user, String positionID) {
        String key = posKey(user, positionID);
        if (key == null) return false;
        return positionContact.remove(key) != null;
    }

    public Contact getContactForPosition(UserProfile user, String positionID) {
        if (user == null) return null;
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) return null;
        String key = posKey(user, pid);
        if (key == null) return null;
        Company comp = findCompanyForPosition(pid);
        if (comp == null || comp.getCompanyName() == null) return null;
        String name = positionContact.get(key);
        if (name == null) return null;

        return findContact(user, comp.getCompanyName(), name);
    }

    // =========================================================
    // ==================== Documents Methods ===================
    // =========================================================

    public Stores uploadDocument(UserProfile user, String docName, String docType, String target, String note, boolean primary) {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        String dn = norm(docName);
        if (dn == null || dn.isEmpty()) throw new IllegalArgumentException("Document name cannot be empty.");
        if (findStoreByDocName(user, dn) != null) {
            throw new IllegalArgumentException("Document with the same name already exists for you: " + dn);
        }

        Document document = new Document(dn, docType, target);
        documents.add(document);

        Stores link = new Stores(user, document, note);
        if (primary) {
            link.markAsPrimary();
        }
        storedDocuments.add(link);
        return link;
    }

    //lists all documents for user
    public Vector<Stores> listUserDocuments(UserProfile user) {
        Vector<Stores> out = new Vector<>();
        if (user == null) return out;

        for (Stores s : storedDocuments) {
            if (s != null && s.getUser() != null && sameUser(s.getUser(), user)) out.add(s);
        }
        return out;
    }

    public Stores getStoredDocument(UserProfile user, String docName) {
        return findStoreByDocName(user, docName);
    }

    public boolean removeDocument(UserProfile user, String docName) {
        Stores s = findStoreByDocName(user, docName);
        if (s == null) return false;

        Document d = s.getDocument();
        boolean removed = storedDocuments.remove(s);

        if (removed && d != null) {
            boolean stillUsed = false;
            for (Stores other : storedDocuments) {
                if (other != null && other.getDocument() == d) {
                    stillUsed = true;
                    break;
                }
            }
            if (!stillUsed) documents.remove(d);
        }
        return removed;
    }

    public boolean markDocumentAsPrimary(UserProfile user, String docName) {
        Stores target = findStoreByDocName(user, docName);
        if (target == null) return false;

        for (Stores s : storedDocuments) {
            if (s != null && s.getUser() != null && sameUser(s.getUser(), user)) {
                s.unmarkAsPrimary();
            }
        }
        target.markAsPrimary();
        return true;
    }

    public boolean unmarkPrimaryDocument(UserProfile user) {
        if (user == null) return false;

        boolean changed = false;
        for (Stores s : storedDocuments) {
            if (s == null || s.getUser() == null) continue;
            if (sameUser(s.getUser(), user) && s.isPrimary()) {
                s.unmarkAsPrimary();
                changed = true;
            }
        }
        return changed;
    }

    public boolean renameDocument(UserProfile user, String oldName, String newName) {
        Stores s = findStoreByDocName(user, oldName);
        if (s == null) return false;

        String nn = norm(newName);
        if (nn == null || nn.isEmpty()) throw new IllegalArgumentException("New name cannot be empty.");

        if (findStoreByDocName(user, nn) != null) {
            throw new IllegalArgumentException("Cannot rename: another document with this name already exists.");
        }

        s.getDocument().reName(nn);
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
        if (s == null) return false;
        s.updateNote(newNote);
        return true;
    }

    // =========================================================
    // ==================== Events Methods ======================
    // =========================================================

    public boolean overlapCheck(UserProfile user, Event newEvent) {
        if (user == null || newEvent == null) return false;

        LocalDateTime newStart = newEvent.getDateTime();
        LocalDateTime newEnd = newEvent.getEndDateTime();
        if (newStart == null || newEnd == null) return false;

        for (Event existing : events) {
            if (existing == null) continue;
            if (existing == newEvent) continue;
            if (!isUserEvent(user, existing)) continue;

            LocalDateTime es = existing.getDateTime();
            LocalDateTime ee = existing.getEndDateTime();
            if (es == null || ee == null) continue;

            boolean overlap = newStart.isBefore(ee) && es.isBefore(newEnd);
            if (overlap) return true;
        }
        return false;
    }

    public synchronized void addEventToCalendar(UserProfile user, Event event) {
        if (user == null) throw new IllegalArgumentException("UserProfile cannot be null.");
        if (event == null) throw new IllegalArgumentException("Event cannot be null.");
        if (overlapCheck(user, event)) throw new IllegalArgumentException("Event overlaps with an existing event.");

        event.setNotified(false);
        events.add(event);
        notifications.add(new NotifyAbout(user, event));
    }

    public synchronized void cancelEvent(UserProfile user, Event event) {
        if (user == null || event == null) return;
        if (!isUserEvent(user, event)) return;

        for (int i = notifications.size() - 1; i >= 0; i--) {
            NotifyAbout n = notifications.get(i);
            if (n == null) continue;
            if (!sameUser(n.getUser(), user)) continue;
            if (n.getEvent() != null && n.getEvent().getEventID() == event.getEventID()) {
                notifications.remove(i);
            }
        }
        events.remove(event);
    }

    public synchronized void reScheduleEvent(UserProfile user, Event event, LocalDateTime newDateTime, int newDuration) {
        if (user == null) throw new IllegalArgumentException("UserProfile cannot be null.");
        if (event == null) throw new IllegalArgumentException("Event cannot be null.");

        Event target = getUserEventById(user, event.getEventID());
        if (target == null) throw new IllegalArgumentException("Event does not belong to user.");

        LocalDateTime oldDT = target.getDateTime();
        int oldDur = target.getDuration();
        boolean oldNotified = target.isNotified();

        target.reSchedule(newDateTime, newDuration);

        if (overlapCheck(user, target)) {
            target.reSchedule(oldDT, oldDur);
            target.setNotified(oldNotified);
            throw new IllegalArgumentException("Reschedule creates overlap with another event.");
        }
        resetNotificationForEvent(user, target);
    }

    public synchronized void updateEventDetails(UserProfile user, Event event, String newType, String newTitle, LocalDateTime newDateTime, int newDuration, String newNotes) {
        if (user == null) throw new IllegalArgumentException("UserProfile cannot be null.");
        if (event == null) throw new IllegalArgumentException("Event cannot be null.");

        Event target = getUserEventById(user, event.getEventID());
        if (target == null) throw new IllegalArgumentException("Event does not belong to user.");

        boolean timeChanged =
                (target.getDateTime() == null && newDateTime != null) ||
                        (target.getDateTime() != null && !target.getDateTime().equals(newDateTime)) ||
                        (target.getDuration() != newDuration);
        if (timeChanged) {
            reScheduleEvent(user, target, newDateTime, newDuration);
        }
        target.updateDetails(newType, newTitle, target.getDateTime(), target.getDuration(), newNotes);
    }

    public Vector<Event> listEvents(UserProfile user, int month, int year) {
        Vector<Event> out = new Vector<>();
        if (user == null) return out;

        for (Event e : events) {
            if (e == null || e.getDateTime() == null) continue;
            if (!isUserEvent(user, e)) continue;

            if (e.getDateTime().getYear() == year && e.getDateTime().getMonthValue() == month) out.add(e);
        }
        return out;
    }

    public Vector<Event> listEventsForDay(UserProfile user, int day, int month, int year) {
        Vector<Event> out = new Vector<>();
        if (user == null) return out;

        for (Event e : events) {
            if (e == null || e.getDateTime() == null) continue;
            if (!isUserEvent(user, e)) continue;

            if (e.getDateTime().getYear() == year &&
                    e.getDateTime().getMonthValue() == month &&
                    e.getDateTime().getDayOfMonth() == day) {
                out.add(e);
            }
        }
        return out;
    }

    public synchronized Event getUserEventById(UserProfile user, int eventId) {
        if (user == null || eventId <= 0) return null;
        for (Event e : events) {
            if (e != null && e.getEventID() == eventId && isUserEvent(user, e)) return e;
        }
        return null;
    }

    // =========================================================
    // ==================== Notifications Methods ===============
    // =========================================================

    public synchronized Vector<NotifyAbout> listNotifications(UserProfile user) {
        Vector<NotifyAbout> out = new Vector<>();
        if (user == null) return out;
        for (NotifyAbout n : notifications) {
            if (n != null && sameUser(n.getUser(), user)) out.add(n);
        }
        return out;
    }

    public boolean markNotificationSeen(UserProfile user, NotifyAbout notification) {
        if (user == null || notification == null) return false;
        if (!sameUser(notification.getUser(), user)) return false;
        notification.markAsSeen();
        return true;
    }

    public int markAllNotificationsSeen(UserProfile user) {
        if (user == null) return 0;

        int count = 0;
        for (NotifyAbout n : notifications) {
            if (n != null && sameUser(n.getUser(), user) && !n.isSeen()) {
                n.markAsSeen();
                count++;
            }
        }
        return count;
    }

    // Triggers and returns notifications whose time has come (based on your NotifyAbout.shouldTrigger())
    public Vector<NotifyAbout> triggerDueNotifications(UserProfile user) {
        Vector<NotifyAbout> triggered = new Vector<>();
        if (user == null) return triggered;

        for (NotifyAbout n : notifications) {
            if (n == null) continue;
            if (!sameUser(n.getUser(), user)) continue;

            if (n.shouldTrigger()) {
                n.markAsTriggered();
                triggered.add(n);
            }
        }
        return triggered;
    }

    // What the GUI should show as a notification "feed"
    public List<NotifyAbout> getNotificationsToDisplay(UserProfile user) {
        Vector<NotifyAbout> v = listNotifications(user);
        ArrayList<NotifyAbout> list = new ArrayList<>();

        for (NotifyAbout n : v) {
            if (n == null || n.getEvent() == null) continue;
            if (!n.getEvent().isNotified()) continue;
            list.add(n);
        }

        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list;
    }

    // =========================================================
    // ==================== Statistics Methods ==================
    // =========================================================

    public String buildUserStatistics(UserProfile user) {
        if (user == null) return "No user";

        int totalApps = 0;
        int activeApps = 0;
        int totalEvents = 0;
        int upcomingEvents = 0;

        for (ApplyFor a : applications) {
            if (a != null && a.getUserProfile() != null && sameUser(a.getUserProfile(), user)) {
                totalApps++;
                if (!isFinalStage(a.getStage())) activeApps++;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (Event e : events) {
            if (e != null && isUserEvent(user, e)) {
                totalEvents++;
                if (e.getDateTime() != null && e.getDateTime().isAfter(now)) upcomingEvents++;
            }
        }

        return "Statistics for " + user.getEmail() +
                " | Applications: " + totalApps +
                " (Active: " + activeApps + ")" +
                " | Events: " + totalEvents +
                " (Upcoming: " + upcomingEvents + ")";
    }


    // ===== Helpers exposed for GUI (business logic stays here) =====

    public boolean isFinalStage(ApplicationStage stage) {
        return stage == ApplicationStage.REJECTED
                || stage == ApplicationStage.WITHDRAWN
                || stage == ApplicationStage.OFFER;
    }

    public boolean isApplicationOverdue(UserProfile user, ApplyFor app) {
        if (user == null || app == null || app.getPosition() == null) return false;
        Vector<ApplyFor> overdue = checkOverdueApplications(user);
        for (ApplyFor a : overdue) if (a == app) return true;
        return false;
    }

    public Event buildEventFromForm(String type, String title, String dateTimeText, String durationText, String notes) {
        String t = (type == null || type.trim().isEmpty()) ? "Other" : type.trim();
        String ttl = (title == null) ? "" : title.trim();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dt = LocalDateTime.parse(dateTimeText.trim(), fmt);
        int dur = Integer.parseInt(durationText.trim());
        return new Event(t, ttl, dt, dur, notes);
    }

    public boolean updateStoredDocumentDetails(UserProfile user,
                                               String oldName,
                                               String newName,
                                               String newTarget,
                                               String newNote,
                                               boolean makePrimary) {

        Stores s = getStoredDocument(user, oldName);
        if (s == null || s.getDocument() == null) return false;

        String nn = (newName == null) ? null : newName.trim();
        String nt = (newTarget == null) ? null : newTarget.trim();

        // rename if changed
        if (nn != null && !nn.isEmpty() && !nn.equalsIgnoreCase(s.getDocument().getDocName())) {
            renameDocument(user, s.getDocument().getDocName(), nn);
        }

        // after rename, re-fetch by latest name
        String currentName = (nn != null && !nn.isEmpty()) ? nn : s.getDocument().getDocName();
        Stores updated = getStoredDocument(user, currentName);
        if (updated == null || updated.getDocument() == null) return false;

        if (nt != null && !nt.isEmpty()) {
            updateDocumentTarget(user, currentName, nt);
        }

        if (newNote != null) {
            updateDocumentNote(user, currentName, newNote);
        }

        if (makePrimary) markDocumentAsPrimary(user, currentName);
        else unmarkPrimaryDocument(user);

        return true;
    }
}