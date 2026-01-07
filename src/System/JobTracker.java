package System;

import Model.*;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        return (s == null) ? null : s.trim().replaceAll("\\s+", " ");
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
    private Publishes upsertPublish(Company company, JobPosition position, LocalDate publishDate, String postingChannel) {
        if (company == null || position == null) {
            throw new IllegalArgumentException("Company and JobPosition cannot be null.");
        }
        String channel = norm(postingChannel);
        if (channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Posting channel cannot be null/empty.");
        }

        Company storedCompany = addCompany(company);
        JobPosition storedPosition = addPosition(position);

        String loc = storedPosition.getLocation();
        if (loc != null && !loc.trim().isEmpty()) addBranch(storedCompany, loc);

        for (Publishes pub : publishedJobs) {
            if (pub == null || pub.getCompany() == null || pub.getPosition() == null) continue;
            boolean sameCompany = pub.getCompany().getCompanyName().equalsIgnoreCase(storedCompany.getCompanyName());
            boolean samePosition = pub.getPosition().getPositionID().equalsIgnoreCase(storedPosition.getPositionID());
            if (sameCompany && samePosition) {
                pub.updatePostingChannel(channel);
                //set publish date if missing
                if (publishDate != null) {
                    pub.setPublishDate(publishDate);
                }
                return pub;
            }
        }

        Publishes created = new Publishes(storedCompany, storedPosition, publishDate, channel);

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

    // =========================================================
    // ==================== Company Methods ====================
    // =========================================================

    public Company addCompany(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null.");
        }
        Company existing = findCompanyByName(company.getCompanyName());
        if (existing != null) {
            //merge details if missing
            if ((existing.getIndustry() == null || existing.getIndustry().isBlank()) &&
                    company.getIndustry() != null && !company.getIndustry().isBlank()) {
                existing.setIndustry(company.getIndustry());
            }
            if ((existing.getWebsiteURL() == null || existing.getWebsiteURL().isBlank()) &&
                    company.getWebsiteURL() != null && !company.getWebsiteURL().isBlank()) {
                existing.setWebsiteURL(company.getWebsiteURL());
            }
            //merge branches if incoming has any
            if (company.getBranches() != null) {
                for (String b : company.getBranches()) {
                    addBranch(existing, b);
                }
            }
            return existing;
        }
        companies.add(company);
        return company;
    }

    public void addBranch(Company company, String location) {
        if (company == null) {
            throw new IllegalArgumentException("Company cannot be null.");
        }
        String loc = normalizeBranchLocation(location);
        if (loc == null || loc.isEmpty()) {
            throw new IllegalArgumentException("Location cannot be null or empty.");
        }
        if (company.hasBranch(loc)) return;
        ArrayList<String> updated = new ArrayList<>();
        if (company.getBranches() != null) updated.addAll(company.getBranches());
        updated.add(loc);
        company.setBranches(updated);
    }

    public List<String> getAllBranchesForCompany(Company company) {
        LinkedHashMap<String, String> uniq = new LinkedHashMap<>();
        if (company == null) return new ArrayList<>();
        //stored branches
        if (company.getBranches() != null) {
            for (String b : company.getBranches()) {
                putBranchNormalized(uniq, b);
            }
        }
        //branches from published jobs
        String cname = (company.getCompanyName() == null) ? null : company.getCompanyName().trim();
        if (cname != null && !cname.isEmpty()) {
            for (Publishes pub : publishedJobs) {
                if (pub == null || pub.getCompany() == null || pub.getPosition() == null) continue;
                String pc = pub.getCompany().getCompanyName();
                if (pc == null || !pc.equalsIgnoreCase(cname)) continue;
                putBranchNormalized(uniq, pub.getPosition().getLocation());
            }
        }
        return new ArrayList<>(uniq.values());
    }

    private void putBranchNormalized(LinkedHashMap<String, String> uniq, String raw) {
        String pretty = normalizeBranchLocation(raw);
        if (pretty == null) return;
        String key = pretty.toLowerCase(Locale.ROOT);
        uniq.putIfAbsent(key, pretty);
    }

    private String normalizeBranchLocation(String location) {
        String loc = norm(location);
        if (loc == null) return null;
        loc = loc.replaceAll("\\s+", " ").trim();
        if (loc.isEmpty()) return null;

        String s = loc.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(s.length());
        boolean capNext = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '-' || c == ',' || c == '/' || c == '.') {
                out.append(c);
                capNext = true;
                continue;
            }
            if (capNext && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                capNext = false;
            } else {
                out.append(c);
                capNext = false;
            }
        }
        return out.toString();
    }

    public ArrayList<String> getAllPostingChannelsForCompany(Company company) {
        ArrayList<String> result = new ArrayList<>();
        if (company == null || company.getCompanyName() == null) {
            return result;
        }
        String cname = norm(company.getCompanyName());
        if (cname == null || cname.isEmpty()) {
            return result;
        }
        for (Publishes pub : publishedJobs) {
            if (pub == null || pub.getCompany() == null) continue;
            String pubCompanyName = pub.getCompany().getCompanyName();
            if (pubCompanyName == null || !pubCompanyName.equalsIgnoreCase(cname)) continue;
            String ch = pub.getPostingChannel();
            if (ch == null) continue;
            ch = norm(ch);
            if (ch == null || ch.isEmpty()) continue;
            // avoid duplicates (case-insensitive) without Map/Set
            boolean exists = false;
            for (String existing : result) {
                if (existing != null && existing.equalsIgnoreCase(ch)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) result.add(ch);
        }
        return result;
    }

    public String getAllPostingChannelsForCompanyText(Company company) {
        ArrayList<String> channels = getAllPostingChannelsForCompany(company);
        return channels.isEmpty() ? "N/A" : String.join(", ", channels);
    }

    public String buildCompanyDetailsTextForPosition(String positionID) {
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) {
            return "No position selected.";
        }
        Company c = getCompanyForPosition(pid);
        if (c == null) {
            return "No company linked to this position.";
        }
        String industry = (c.getIndustry() == null || c.getIndustry().isBlank()) ? "N/A" : c.getIndustry().trim();
        String website  = (c.getWebsiteURL() == null || c.getWebsiteURL().isBlank()) ? "N/A" : c.getWebsiteURL().trim();
        List<String> branches = getAllBranchesForCompany(c);
        String brText = branches.isEmpty() ? "(no branches)" : String.join(", ", branches);
        String channel = getAllPostingChannelsForCompanyText(c);
        String chText = (channel == null || channel.isBlank()) ? "N/A" : channel.trim();
        return "Company: " + c.getCompanyName() + "\nIndustry: " + industry + "\nWebsite: " + website
                + "\nBranches: " + brText + "\nPosting channels: " + chText;
    }

    //Save + Load Companies to/from file
    public void saveCompaniesToFile(String filePath) throws IOException {
        String path = norm(filePath);
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("File path cannot be empty.");

        File outFile = new File(path);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Failed to create directories for path: " + parent.getAbsolutePath());
            }
        }

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

    public boolean togglePositionStatus(String positionID) {
        JobPosition p = findPositionByID(positionID);
        if (p == null) return false;
        p.updateStatus();
        return true;
    }

    public LocalDate getPublishDateForPosition(String positionID) {
        Publishes p = findPublishByPositionId(positionID);
        return (p == null) ? null : p.getPublishDate();
    }

    public String publishAgeInParentheses(String positionID) {
        Publishes pub = findPublishByPositionId(positionID);
        if (pub == null) return "";
        long days = pub.daysSincePublish();
        if (days <= 0) return " (today)";
        if (days == 1) return " (1 day ago)";
        return " (" + days + " days ago)";
    }

    public void generateOldPublishNotifications(UserProfile user, long thresholdDays) {
        if (user == null) return;
        Vector<ApplyFor> apps = listApplications(user);
        if (apps == null) return;
        for (ApplyFor app : apps) {
            if (app == null || app.getPosition() == null) continue;
            JobPosition pos = app.getPosition();
            String pid = norm(pos.getPositionID());
            if (pid == null || pid.isEmpty()) continue;
            //only if still active
            String st = (pos.getStatus() == null) ? "" : pos.getStatus().trim();
            if (st.equalsIgnoreCase("Not Active")) continue;
            Publishes pub = findPublishByPositionId(pid);
            if (pub == null || pub.getPublishDate() == null) continue;
            long days = pub.daysSincePublish();
            if (days < thresholdDays) continue;
            if (!hasOldPublishNotification(user, pid)) {
                addOldPublishNotification(user, pid, days, pub.getPublishDate());
            }
        }
    }

    private boolean hasOldPublishNotification(UserProfile user, String pid) {
        for (NotifyAbout n : notifications) {
            if (n == null || n.getUser() == null) continue;
            if (!sameUser(n.getUser(), user)) continue;
            if (n.isSystem()) {
                String t = n.getTitle();
                String m = n.getMessage();
                if ((t != null && t.contains(pid)) || (m != null && m.contains(pid))) return true;
            }
        }
        return false;
    }

    private void addOldPublishNotification(UserProfile user, String pid, long days, LocalDate publishDate) {
        String title = "Old publish - Check if position still Active";
        String msg = "Position " + pid + ", published on " + publishDate + " (" + days + " days ago)";
        notifications.add(new NotifyAbout(user, title, msg));
    }

    // =========================================================
    // ==================== Applications Methods ================
    // =========================================================

    public ApplyFor addApplication(UserProfile user, JobPosition position, Company company, LocalDate publishDate, String source, String notes) {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        if (position == null) throw new IllegalArgumentException("Position cannot be null.");
        if (company == null) throw new IllegalArgumentException("Company cannot be null.");
        //store or merge company (by name)
        Company storedCompany = addCompany(company);
        //location = branch
        String loc = (position.getLocation() == null) ? null : position.getLocation();
        if (loc != null && !loc.trim().isEmpty()) {
            addBranch(storedCompany, loc);
        }
        //link publish and create application
        Publishes pub = upsertPublish(storedCompany, position, publishDate, source);
        JobPosition storedPosition = pub.getPosition();

        ApplyFor existing = findApplication(user, storedPosition.getPositionID());
        if (existing != null) return existing;

        ApplyFor created = new ApplyFor(storedPosition, user, source, notes);
        applications.add(created);
        return created;
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

    public boolean updateApplicationStage(UserProfile user, String positionID, ApplicationStage newStage) {
        ApplyFor app = findApplication(user, positionID);
        if (app == null) return false;
        app.updateStage(newStage);
        return true;
    }

    public boolean isApplicationOverdue(UserProfile user, ApplyFor app) {
        if (user == null || app == null || app.getUserProfile() == null) return false;
        if (!sameUser(app.getUserProfile(), user)) return false;
        if (isFinalStage(app.getStage())) return false;
        return app.timeSinceApplied() >= OVERDUE_DAYS;
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

    public Company getCompanyForPosition(String positionID) {
        return findCompanyForPosition(positionID);
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

    public Contact addOrEditContactForPosition(UserProfile user, String positionID, String contactName, String role, String contactEmail, String contactPhone) {
        if (user == null) throw new IllegalArgumentException("User cannot be null.");
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) {
            throw new IllegalArgumentException("Position ID cannot be empty.");
        }
        Company company = getCompanyForPosition(pid);
        if (company == null) {
            throw new IllegalArgumentException("Cannot edit contact: this position has no company linked.");
        }
        String name = norm(contactName);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Contact name cannot be empty.");
        }
        Contact c = addContact(user, company, name, role, contactEmail, contactPhone, LocalDateTime.now());
        setContactForPosition(user, pid, c.getContactName());
        return c;
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

    public synchronized Contact logLastContactForPosition(UserProfile user, String positionID, String method, String subject, LocalDateTime when) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) {
            throw new IllegalArgumentException("Position ID cannot be empty.");
        }
        Contact c = getContactForPosition(user, pid);
        if (c == null) {
            throw new IllegalArgumentException("No contact is linked to this position.");
        }
        c.logContact(method, subject, when); //updates method+subject and sets date
        return c;
    }

    public String getLastContactInfoForPosition(UserProfile user, String positionID) {
        if (user == null) {
            return "No user.";
        }
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) {
            return "No position selected.";
        }
        Contact c = getContactForPosition(user, pid);
        if (c == null) {
            return "No contact linked to this position.";
        }
        return c.getLastContactInfo();
    }

    public long daysSinceLastContactForPosition(UserProfile user, String positionID) {
        if (user == null) {
            return -1;
        }
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) {
            return -1;
        }
        Contact c = getContactForPosition(user, pid);
        if (c == null) {
            return -1;
        }
        return c.timeSinceLastContact();
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

    //for gui showing notifications
    public List<NotifyAbout> getNotificationsToDisplay(UserProfile user) {
        if (user == null) return List.of();
        generateOldPublishNotifications(user, 60);
        ArrayList<NotifyAbout> out = new ArrayList<>();
        for (NotifyAbout n : notifications) {
            if (n == null || n.getUser() == null) continue;
            if (!sameUser(n.getUser(), user)) continue;
            if (n.isSystem()) {
                out.add(n);
                continue;
            }
            Event e = n.getEvent();
            boolean triggeredAlready = (e != null && e.isNotified());
            if (n.shouldTrigger() || (triggeredAlready && !n.isSeen())) {
                out.add(n);
            }
        }
        //newest first
        out.sort((a, b) -> {
            LocalDateTime ta = (a == null) ? null : a.getCreatedAt();
            LocalDateTime tb = (b == null) ? null : b.getCreatedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return out;
    }

    // =========================================================
    // ==================== Statistics Methods ==================
    // =========================================================

    public String buildUserStatistics(UserProfile user) {
        if (user == null) return "No user";
        int totalApps = 0;
        int activeApps = 0;
        int unActiveApps = 0;
        int totalEvents = 0;
        int upcomingEvents = 0;
        for (ApplyFor a : applications) {
            if (a == null || a.getUserProfile() == null || !sameUser(a.getUserProfile(), user)) {
                continue;
            }
            totalApps++;
            boolean inactive = isFinalStage(a.getStage()) || (a.getPosition() != null && "Not Active".equalsIgnoreCase(a.getPosition().getStatus()));
            if (inactive) unActiveApps++;
            else activeApps++;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Event e : events) {
            if (e != null && isUserEvent(user, e)) {
                totalEvents++;
                if (e.getDateTime() != null && e.getDateTime().isAfter(now)) upcomingEvents++;
            }
        }
        String topCompany = mostAppliedCompany(user);
        double avgDays = avgDaysToFirstStageChange(user);
        return "Applications: " + totalApps + " (Active: " + activeApps +" | Not active: " + unActiveApps + ")" +
                "\nEvents: " + totalEvents + " (Upcoming: " + upcomingEvents + ")" +
                "\n\nMost applied company: " + topCompany +
                "\n\nAvg days to first stage change: " +
                String.format(java.util.Locale.US, "%.2f", avgDays);
    }

    public String mostAppliedCompany(UserProfile user) {
        if (user == null) return "N/A";

        Map<String, Integer> counts = new HashMap<>();

        for (ApplyFor app : applications) {
            if (app == null || app.getUserProfile() == null || app.getPosition() == null) continue;
            if (!sameUser(app.getUserProfile(), user)) continue;

            String pid = app.getPosition().getPositionID();
            Company c = getCompanyForPosition(pid);
            String name = (c == null || c.getCompanyName() == null) ? "Unknown" : c.getCompanyName().trim();

            counts.put(name, counts.getOrDefault(name, 0) + 1);
        }
        if (counts.isEmpty()) return "N/A";
        int max = 0;
        for (int v : counts.values()) max = Math.max(max, v);

        ArrayList<String> winners = new ArrayList<>();
        for (var e : counts.entrySet()) {
            if (e.getValue() == max) winners.add(e.getKey());
        }
        //sort alphabetically
        Collections.sort(winners, String.CASE_INSENSITIVE_ORDER);

        if (winners.size() == 1) {
            return winners.get(0) + " (" + max + ")";
        }
        return String.join(", ", winners) + " (" + max + ")";
    }

    public double avgDaysToFirstStageChange(UserProfile user) {
        if (user == null) return 0.0;

        long totalHours = 0;
        int count = 0;

        for (ApplyFor app : applications) {
            if (app == null || app.getUserProfile() == null) continue;
            if (!sameUser(app.getUserProfile(), user)) continue;

            LocalDateTime a = app.getDateApplied();
            LocalDateTime ch = app.getFirstStageChangeAt();
            if (a == null || ch == null) continue;
            long hours = java.time.Duration.between(a, ch).toHours();
            if (hours < 0) continue;
            totalHours += hours;
            count++;
        }
        if (count == 0) return 0.0;
        double avgHours = (double) totalHours / count;
        return Math.ceil(avgHours / 24.0); //convert to days + round up
    }

    public LinkedHashMap<String, Integer> applicationsPerWeek(UserProfile user, int weeksBack) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        if (user == null || weeksBack <= 0) return out;
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();

        //Sunday -> Sunday
        java.time.DayOfWeek WEEK_START = java.time.DayOfWeek.SUNDAY;
        LocalDate currentWeekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(WEEK_START));
        //init last N weeks with 0
        for (int i = weeksBack - 1; i >= 0; i--) {
            LocalDate start = currentWeekStart.minusWeeks(i);
            LocalDate end = start.plusDays(7);
            String key = start.format(df) + "-" + end.format(df);
            out.put(key, 0);
        }
        for (ApplyFor app : applications) {
            if (app == null || app.getUserProfile() == null) continue;
            if (!sameUser(app.getUserProfile(), user)) continue;
            if (app.getDateApplied() == null) continue;
            LocalDate d = app.getDateApplied().toLocalDate();
            LocalDate start = d.with(java.time.temporal.TemporalAdjusters.previousOrSame(WEEK_START));
            LocalDate end = start.plusDays(7);
            String key = start.format(df) + "-" + end.format(df);
            if (out.containsKey(key)) out.put(key, out.get(key) + 1);
        }

        return out;
    }

    public EnumMap<ApplicationStage, Integer> stageBreakdown(UserProfile user) {
        EnumMap<ApplicationStage, Integer> out = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage st : ApplicationStage.values()) out.put(st, 0);
        if (user == null) return out;
        for (ApplyFor app : applications) {
            if (app == null || app.getUserProfile() == null) continue;
            if (!sameUser(app.getUserProfile(), user)) continue;
            ApplicationStage st = app.getStage();
            if (st == null) continue;
            out.put(st, out.getOrDefault(st, 0) + 1);
        }
        return out;
    }

    public String formatStageBreakdown(UserProfile user) {
        EnumMap<ApplicationStage, Integer> map = stageBreakdown(user);
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (ApplicationStage st : ApplicationStage.values()) {
            int cnt = map.getOrDefault(st, 0);
            if (cnt > 0) {
                sb.append(st.name()).append(": ").append(cnt).append("\n");
                any = true;
            }
        }
        if (!any) sb.append("No applications.\n");
        return sb.toString();
    }


    // ===== Helpers exposed for GUI =====
    public boolean isFinalStage(ApplicationStage stage) {
        return stage == ApplicationStage.REJECTED
                || stage == ApplicationStage.WITHDRAWN
                || stage == ApplicationStage.OFFER;
    }

    public boolean updateStoredDocumentDetails(UserProfile user, String oldName, String newName, String newTarget, String newNote, boolean makePrimary) {
        Stores s = getStoredDocument(user, oldName);
        if (s == null || s.getDocument() == null) return false;
        String nn = (newName == null) ? null : newName.trim();
        String nt = (newTarget == null) ? null : newTarget.trim();
        //rename if changed
        if (nn != null && !nn.isEmpty() && !nn.equalsIgnoreCase(s.getDocument().getDocName())) {
            renameDocument(user, s.getDocument().getDocName(), nn);
        }
        //after rename - get current name
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

    public boolean updateProcessFromDetailsForm(UserProfile user, String positionID, ApplicationStage newStage, String newSource, String noteToAppend) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        String pid = norm(positionID);
        if (pid == null || pid.isEmpty()) {
            throw new IllegalArgumentException("Position ID cannot be empty.");
        }
        ApplyFor app = findApplication(user, pid);
        if (app == null) {
            throw new IllegalArgumentException("Process not found.");
        }
        boolean changed = false;
        //stage
        if (newStage != null && newStage != app.getStage()) {
            app.updateStage(newStage);
            changed = true;
        }
        //source
        String ns = norm(newSource);
        String os = norm(app.getSource());
        if (ns != null && !ns.isEmpty() && (os == null || !ns.equalsIgnoreCase(os))) {
            app.setSource(ns);
            // update posting channel for this position too
            Publishes pub = findPublishByPositionId(pid);
            if (pub != null) {
                pub.updatePostingChannel(ns);
        }
            changed = true;
        }
        //note
        String n = norm(noteToAppend);
        if (n != null && !n.isEmpty()) {
            app.addNote(n);
            changed = true;
        }
        return changed;
    }

}