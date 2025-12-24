package System;

import Model.*;
import Model.Event;
import Threads.*;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JobTrackerGUI extends Frame implements ActionListener, ItemListener, WindowListener {

    private final JobTracker tracker;
    private UserProfile activeUser;

    // ===== Common =====
    private final CardLayout cardLayout = new CardLayout();
    private final Panel cards = new Panel(cardLayout);

    private final List outputList = new List(10);
    private final TextArea logArea = new TextArea("", 6, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);

    // ===== Login / Register components =====
    private TextField loginEmailTF, loginPassTF;

    private TextField regFullNameTF, regEmailTF, regPassTF, regPhoneTF, regFieldTF;

    // ===== Main menu (Choice) =====
    private Choice actionChoice;

    // ===== Add Application components =====
    private TextField appCompanyNameTF, appIndustryTF, appWebsiteTF;
    private TextField appPositionIdTF, appTitleTF, appFieldTF, appLocationTF, appEmploymentTF, appStatusTF;
    private TextArea appDescriptionTA, appNotesTA;
    private TextField appSourceTF;

    // Optional contact for application
    private TextField cNameTF, cRoleTF, cEmailTF, cPhoneTF;

    // ===== Add Event components =====
    private Choice eventTypeChoice;
    private TextField eventTitleTF, eventDateTimeTF, eventDurationTF;
    private TextArea eventNotesTA;

    // ===== Documents components =====
    private TextField docNameTF, docTargetTF, docNoteTF;
    private Choice docTypeChoice;
    private Checkbox primaryCB;

    // ===== Menu =====
    private MenuItem miExit, miAbout, miSaveCompanies, miLoadCompanies;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public JobTrackerGUI(JobTracker tracker) {
        super("JobTracker (AWT GUI)");
        this.tracker = tracker;

        buildMenu();
        buildUI();

        addWindowListener(this);

        setSize(900, 650);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildMenu() {
        MenuBar mb = new MenuBar();

        Menu file = new Menu("File");
        miSaveCompanies = new MenuItem("Save Companies");
        miLoadCompanies = new MenuItem("Load Companies");
        miExit = new MenuItem("Exit");

        miSaveCompanies.addActionListener(this);
        miLoadCompanies.addActionListener(this);
        miExit.addActionListener(this);

        file.add(miSaveCompanies);
        file.add(miLoadCompanies);
        file.addSeparator();
        file.add(miExit);

        Menu help = new Menu("Help");
        miAbout = new MenuItem("About");
        miAbout.addActionListener(this);
        help.add(miAbout);

        mb.add(file);
        mb.add(help);

        setMenuBar(mb);
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Left: output list
        Panel left = new Panel(new BorderLayout());
        left.add(new Label("Output"), BorderLayout.NORTH);
        outputList.addItemListener(this);
        left.add(outputList, BorderLayout.CENTER);

        // Bottom log
        Panel bottom = new Panel(new BorderLayout());
        bottom.add(new Label("Log"), BorderLayout.NORTH);
        logArea.setEditable(false);
        bottom.add(logArea, BorderLayout.CENTER);

        // Cards (center)
        cards.add(buildLoginCard(), "LOGIN");
        cards.add(buildMainCard(), "MAIN");

        add(left, BorderLayout.WEST);
        add(cards, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        cardLayout.show(cards, "LOGIN");
    }

    // ================= CARDS =================

    private Panel buildLoginCard() {
        Panel root = new Panel(new GridLayout(1, 2, 20, 0));

        // LOGIN
        Panel loginP = new Panel(new GridBagLayout());
        loginP.add(new Label("Login"), gbc(0,0));

        loginEmailTF = new TextField(22);
        loginPassTF = new TextField(22);
        loginPassTF.setEchoChar('*');

        addRow(loginP, 1, "Email:", loginEmailTF);
        addRow(loginP, 2, "Password:", loginPassTF);

        Button loginBtn = new Button("Login");
        loginBtn.addActionListener(this);

        GridBagConstraints b = gbc(0, 3);
        b.gridwidth = 2;
        b.fill = GridBagConstraints.NONE;
        loginP.add(loginBtn, b);

        // REGISTER
        Panel regP = new Panel(new GridBagLayout());
        regP.add(new Label("Register"), gbc(0,0));

        regFullNameTF = new TextField(22);
        regEmailTF    = new TextField(22);
        regPassTF     = new TextField(22); regPassTF.setEchoChar('*');
        regPhoneTF    = new TextField(22);
        regFieldTF    = new TextField(22);

        addRow(regP, 1, "Full Name:", regFullNameTF);
        addRow(regP, 2, "Email:", regEmailTF);
        addRow(regP, 3, "Password:", regPassTF);
        addRow(regP, 4, "Phone:", regPhoneTF);
        addRow(regP, 5, "Field Of Search:", regFieldTF);

        Button regBtn = new Button("Create User");
        regBtn.addActionListener(this);

        GridBagConstraints rb = gbc(0, 6);
        rb.gridwidth = 2;
        regP.add(regBtn, rb);

        root.add(loginP);
        root.add(regP);
        return root;
    }

    private Panel buildMainCard() {
        Panel root = new Panel(new BorderLayout());

        // Top bar: choice menu + buttons
        Panel top = new Panel(new FlowLayout(FlowLayout.LEFT));
        top.add(new Label("Action:"));

        actionChoice = new Choice();
        actionChoice.add("Add Application");
        actionChoice.add("Add Event");
        actionChoice.add("Upload Document");
        actionChoice.add("View Applications");
        actionChoice.add("View Events (Month/Year)");
        actionChoice.add("View Notifications");
        actionChoice.add("Statistics");
        actionChoice.addItemListener(this);

        Button goBtn = new Button("Go");
        goBtn.addActionListener(this);

        Button logoutBtn = new Button("Logout");
        logoutBtn.addActionListener(this);

        top.add(actionChoice);
        top.add(goBtn);
        top.add(logoutBtn);

        // Center: forms (CardLayout)
        Panel forms = new Panel(new CardLayout());
        forms.add(buildAddApplicationForm(), "FORM_APP");
        forms.add(buildAddEventForm(), "FORM_EVENT");
        forms.add(buildUploadDocumentForm(), "FORM_DOC");
        forms.add(new Label("Select an action and click Go."), "FORM_EMPTY");

        root.add(top, BorderLayout.NORTH);
        root.add(forms, BorderLayout.CENTER);

        // store reference via name lookup
        root.setName("MAIN_ROOT");
        forms.setName("FORMS");

        ((CardLayout) forms.getLayout()).show(forms, "FORM_EMPTY");

        return root;
    }

    private Panel buildAddApplicationForm() {
        Panel outer = new Panel(new BorderLayout());
        outer.add(new Label("Add Application"), BorderLayout.NORTH);

        Panel form = new Panel(new GridBagLayout());
        int y = 0;

        appCompanyNameTF = new TextField(24);
        appIndustryTF = new TextField(24);
        appWebsiteTF = new TextField(24);

        appPositionIdTF = new TextField(24);
        appTitleTF = new TextField(24);
        appFieldTF = new TextField(24);
        appLocationTF = new TextField(24);
        appEmploymentTF = new TextField(24);
        appStatusTF = new TextField(24);
        appDescriptionTA = new TextArea("", 4, 24, TextArea.SCROLLBARS_VERTICAL_ONLY);

        appSourceTF = new TextField(24);
        appNotesTA = new TextArea("", 4, 24, TextArea.SCROLLBARS_VERTICAL_ONLY);

        cNameTF = new TextField(24);
        cRoleTF = new TextField(24);
        cEmailTF = new TextField(24);
        cPhoneTF = new TextField(24);

        addRow(form, y++, "Company Name:", appCompanyNameTF);
        addRow(form, y++, "Industry:", appIndustryTF);
        addRow(form, y++, "Website URL:", appWebsiteTF);

        addRow(form, y++, "Position ID:", appPositionIdTF);
        addRow(form, y++, "Title:", appTitleTF);
        addRow(form, y++, "Field:", appFieldTF);
        addRow(form, y++, "Location:", appLocationTF);
        addRow(form, y++, "Employment Type:", appEmploymentTF);
        addRow(form, y++, "Status:", appStatusTF);
        addRow(form, y++, "Description:", appDescriptionTA);

        addRow(form, y++, "Source (where YOU applied):", appSourceTF);
        addRow(form, y++, "Notes:", appNotesTA);

        addRow(form, y++, "Contact Name (optional):", cNameTF);
        addRow(form, y++, "Contact Role:", cRoleTF);
        addRow(form, y++, "Contact Email:", cEmailTF);
        addRow(form, y++, "Contact Phone:", cPhoneTF);

        Button submit = new Button("Submit Application (Runnable Thread)");
        submit.addActionListener(this);
        submit.setName("BTN_SUBMIT_APP");

        GridBagConstraints sb = gbc(0, y);
        sb.gridwidth = 2;
        form.add(submit, sb);

        outer.add(wrapScroll(form), BorderLayout.CENTER);
        return outer;
    }

    private Panel buildAddEventForm() {
        Panel p = new Panel(new BorderLayout());
        Panel grid = new Panel(new GridLayout(0, 2, 6, 6));

        eventTypeChoice = new Choice();
        eventTypeChoice.add("Interview");
        eventTypeChoice.add("Phone Call");
        eventTypeChoice.add("Home Assignment");
        eventTypeChoice.add("Other");
        eventTypeChoice.addItemListener(this);

        eventTitleTF = new TextField();
        eventDateTimeTF = new TextField("2026-01-01 12:00"); // example
        eventDurationTF = new TextField("60");
        eventNotesTA = new TextArea("", 3, 20, TextArea.SCROLLBARS_VERTICAL_ONLY);

        Button submit = new Button("Add Event (Thread extends Thread)");
        submit.addActionListener(this);
        submit.setName("BTN_SUBMIT_EVENT");

        grid.add(new Label("Type:")); grid.add(eventTypeChoice);
        grid.add(new Label("Title:")); grid.add(eventTitleTF);
        grid.add(new Label("DateTime (yyyy-MM-dd HH:mm):")); grid.add(eventDateTimeTF);
        grid.add(new Label("Duration (minutes):")); grid.add(eventDurationTF);
        grid.add(new Label("Notes:")); grid.add(eventNotesTA);

        p.add(new Label("Add Event"), BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        p.add(submit, BorderLayout.SOUTH);
        return p;
    }

    private Panel buildUploadDocumentForm() {
        Panel p = new Panel(new BorderLayout());
        Panel grid = new Panel(new GridLayout(0, 2, 6, 6));

        docNameTF = new TextField();
        docTargetTF = new TextField();
        docNoteTF = new TextField();

        docTypeChoice = new Choice();
        docTypeChoice.add("file");
        docTypeChoice.add("url");
        docTypeChoice.addItemListener(this);

        CheckboxGroup primaryGroup = new CheckboxGroup();
        Checkbox primaryYes = new Checkbox("Primary: YES", primaryGroup, false);
        Checkbox primaryNo = new Checkbox("Primary: NO", primaryGroup, true);
        primaryYes.addItemListener(this);
        primaryNo.addItemListener(this);

        // we store "primary" with a single checkbox for convenience too
        primaryCB = new Checkbox("Mark as Primary", false);
        primaryCB.addItemListener(this);

        Button uploadBtn = new Button("Upload Document");
        uploadBtn.addActionListener(this);
        uploadBtn.setName("BTN_UPLOAD_DOC");

        grid.add(new Label("Doc Name (unique per user):")); grid.add(docNameTF);
        grid.add(new Label("Doc Type:")); grid.add(docTypeChoice);
        grid.add(new Label("Target (path or URL):")); grid.add(docTargetTF);
        grid.add(new Label("Note:")); grid.add(docNoteTF);

        grid.add(primaryYes); grid.add(primaryNo);
        grid.add(primaryCB); grid.add(new Label(""));

        p.add(new Label("Upload Document"), BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        p.add(uploadBtn, BorderLayout.SOUTH);
        return p;
    }

    // ================= HELPERS =================

    private Panel labeled(String label, Component c) {
        Panel p = new Panel(new BorderLayout());
        p.add(new Label(label), BorderLayout.WEST);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private Panel getFormsPanelFromMainCard() {
        // cards -> MAIN (BorderLayout) -> forms panel in CENTER
        Component[] comps = cards.getComponents();
        for (Component comp : comps) {
            if (comp instanceof Panel && "MAIN".equals(((Panel) comp).getName())) {
                // not used
            }
        }
        // simpler: search by name FORMS
        return findPanelByName(cards, "FORMS");
    }

    private Panel findPanelByName(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (c instanceof Panel) {
                if (name.equals(((Panel) c).getName())) return (Panel) c;
                Panel found = findPanelByName((Panel) c, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void log(String s) {
        logArea.append(s + "\n");
    }

    // ================= LISTENERS =================

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        // ===== MENU =====
        if ("Exit".equals(cmd)) {
            dispose();
            return;
        }
        if ("About".equals(cmd)) {
            log("About: AWT GUI for JobTracker. Includes Runnable Thread + Thread subclass.");
            return;
        }
        if ("Save Companies".equals(cmd)) {
            try {
                tracker.saveCompaniesToFile("companies.txt");
                log("Saved companies to companies.txt");
            } catch (Exception ex) {
                log("Save failed: " + ex.getMessage());
            }
            return;
        }
        if ("Load Companies".equals(cmd)) {
            try {
                tracker.loadCompaniesFromFile("companies.txt");
                log("Loaded companies from companies.txt");
            } catch (Exception ex) {
                log("Load failed: " + ex.getMessage());
            }
            return;
        }

        // ===== LOGIN =====
        if ("Login".equals(cmd)) {
            String email = loginEmailTF.getText();
            String pass = loginPassTF.getText();

            UserProfile u = tracker.Login(email, pass);
            if (u == null) {
                log("Login failed.");
                return;
            }
            activeUser = u;
            log("Logged in as: " + activeUser.getEmail());
            cardLayout.show(cards, "MAIN");
            return;
        }

        if ("Create User".equals(cmd)) {
            try {
                UserProfile u = new UserProfile(
                        regFullNameTF.getText(),
                        regEmailTF.getText(),
                        regPassTF.getText(),
                        regPhoneTF.getText(),
                        regFieldTF.getText()
                );
                tracker.addUser(u);
                log("User created: " + u.getEmail());
            } catch (Exception ex) {
                log("Register failed: " + ex.getMessage());
            }
            return;
        }

        // ===== MAIN =====
        if ("Logout".equals(cmd)) {
            activeUser = null;
            log("Logged out.");
            cardLayout.show(cards, "LOGIN");
            return;
        }

        if ("Go".equals(cmd)) {
            if (activeUser == null) {
                log("Please login first.");
                return;
            }
            Panel forms = getFormsPanelFromMainCard();
            CardLayout cl = (CardLayout) forms.getLayout();

            String selected = actionChoice.getSelectedItem();
            if ("Add Application".equals(selected)) cl.show(forms, "FORM_APP");
            else if ("Add Event".equals(selected)) cl.show(forms, "FORM_EVENT");
            else if ("Upload Document".equals(selected)) cl.show(forms, "FORM_DOC");
            else {
                cl.show(forms, "FORM_EMPTY");
                runViewAction(selected);
            }
            return;
        }

        // ===== SUBMITS =====
        Component src = (Component) e.getSource();
        if (src instanceof Button) {
            String name = ((Button) src).getName();

            if ("BTN_SUBMIT_APP".equals(name)) {
                submitApplicationUsingRunnableThread();
                return;
            }
            if ("BTN_SUBMIT_EVENT".equals(name)) {
                submitEventUsingThreadSubclass();
                return;
            }
            if ("BTN_UPLOAD_DOC".equals(name)) {
                uploadDocumentNow();
                return;
            }
        }
    }

    private void runViewAction(String selected) {
        try {
            if ("View Applications".equals(selected)) {
                outputList.removeAll();
                for (ApplyFor a : tracker.viewApplicationsSummary(activeUser)) {
                    outputList.add(a.toString());
                }
                log("Loaded applications into Output list.");
            } else if ("View Notifications".equals(selected)) {
                outputList.removeAll();
                for (NotifyAbout n : tracker.viewNotifications(activeUser)) {
                    outputList.add(n.toString());
                }
                log("Loaded notifications into Output list.");
            } else if ("Statistics".equals(selected)) {
                String stats = tracker.buildUserStatistics(activeUser);
                log(stats);
            } else if ("View Events (Month/Year)".equals(selected)) {
                // simple example: current month/year
                LocalDateTime now = LocalDateTime.now();
                outputList.removeAll();
                for (Event ev : tracker.viewEvents(activeUser, now.getMonthValue(), now.getYear())) {
                    outputList.add(ev.toString());
                }
                log("Loaded events for current month/year into Output list.");
            }
        } catch (Exception ex) {
            log("Action failed: " + ex.getMessage());
        }
    }

    private void submitApplicationUsingRunnableThread() {
        if (activeUser == null) {
            log("Login required.");
            return;
        }

        try {
            Company company = new Company(
                    appCompanyNameTF.getText(),
                    appIndustryTF.getText(),
                    appWebsiteTF.getText()
            );

            JobPosition position = new JobPosition(
                    appPositionIdTF.getText(),
                    appTitleTF.getText(),
                    appFieldTF.getText(),
                    appLocationTF.getText(),
                    appEmploymentTF.getText(),
                    appStatusTF.getText(),
                    appDescriptionTA.getText()
            );

            AddApplicationTask task = new AddApplicationTask(
                    tracker,
                    activeUser,
                    position,
                    company,
                    appSourceTF.getText(),
                    appNotesTA.getText(),
                    cNameTF.getText(),
                    cRoleTF.getText(),
                    cEmailTF.getText(),
                    cPhoneTF.getText(),
                    LocalDateTime.now()
            );

            Thread t = new Thread(task);
            t.start();
            t.join(); // quick task, OK for assignment demo

            if (task.isSuccess()) {
                log("Application added (Runnable Thread).");
                outputList.add("Added application: " + task.getCreatedApplication());
                if (task.getCreatedContact() != null) {
                    outputList.add("Added contact: " + task.getCreatedContact());
                }
            } else {
                log("AddApplicationTask failed: " + task.getErrorMessage());
            }
        } catch (Exception ex) {
            log("Submit failed: " + ex.getMessage());
        }
    }

    private void submitEventUsingThreadSubclass() {
        if (activeUser == null) {
            log("Login required.");
            return;
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(eventDateTimeTF.getText().trim(), DT_FMT);
            int dur = Integer.parseInt(eventDurationTF.getText().trim());

            Event ev = new Event(
                    eventTypeChoice.getSelectedItem(),
                    eventTitleTF.getText(),
                    dt,
                    dur,
                    eventNotesTA.getText()
            );

            AddEventThread t = new AddEventThread(tracker, activeUser, ev);
            t.start();
            t.join();

            if (t.isSuccess()) {
                log("Event added (Thread subclass).");
                outputList.add("Added event: " + ev);
            } else {
                log("AddEventThread failed: " + t.getErrorMessage());
            }
        } catch (Exception ex) {
            log("Add event failed: " + ex.getMessage());
        }
    }

    private void uploadDocumentNow() {
        if (activeUser == null) {
            log("Login required.");
            return;
        }
        try {
            boolean primary = primaryCB.getState();
            Stores s = tracker.uploadDocument(
                    activeUser,
                    docNameTF.getText(),
                    docTypeChoice.getSelectedItem(),
                    docTargetTF.getText(),
                    docNoteTF.getText(),
                    primary
            );
            log("Document uploaded.");
            outputList.add("Stored: " + s.toString());
        } catch (Exception ex) {
            log("Upload failed: " + ex.getMessage());
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        // Just to satisfy "listeners": log choice/list/checkbox changes
        Object src = e.getSource();
        if (src == actionChoice) {
            log("Selected action: " + actionChoice.getSelectedItem());
        } else if (src == outputList) {
            String sel = outputList.getSelectedItem();
            if (sel != null) log("Selected output item.");
        } else if (src == docTypeChoice) {
            log("Doc type: " + docTypeChoice.getSelectedItem());
        } else if (src == primaryCB) {
            log("Primary checkbox: " + primaryCB.getState());
        } else if (src == eventTypeChoice) {
            log("Event type: " + eventTypeChoice.getSelectedItem());
        }
    }

    private GridBagConstraints gbc(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(6, 10, 6, 10);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private void addRow(Panel p, int y, String label, Component field) {
        GridBagConstraints l = gbc(0, y);
        l.fill = GridBagConstraints.NONE;
        l.weightx = 0;

        GridBagConstraints f = gbc(1, y);
        f.fill = GridBagConstraints.HORIZONTAL;
        f.weightx = 1;

        p.add(new Label(label), l);
        p.add(field, f);
    }

    private ScrollPane wrapScroll(Component c) {
        ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        sp.add(c);
        return sp;
    }

    // ===== WindowListener =====
    @Override public void windowClosing(WindowEvent e) { dispose(); }
    @Override public void windowOpened(WindowEvent e) {}
    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}

    public void start()
    {
        setVisible(true);
    }
}