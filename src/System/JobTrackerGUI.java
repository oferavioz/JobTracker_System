package System;

import Model.*;
import Model.Event;
import Threads.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import java.util.*;
import java.util.List;

// IMPORTANT :
// I know that its not good often to mix AWT and Swing components in the same GUI,
// but in order to answer the porject requirements (= use at least one AWT component from lec 11),
// i had to do it this way.
// Swing gives more flexibility and better looks so most of the GUI is Swing based,
// and only the Documents panel and the MenuBar uses AWT components or being implemented by AWT.
// Thank you for understanding! enjoy checking my project :)
// (if it was up to me - i would do everything in Swing only)


public class JobTrackerGUI extends JFrame {

    private final JobTracker tracker;
    private UserProfile activeUser;

    private final CardLayout cardsLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardsLayout);

    private final JTextArea logArea = new JTextArea(6, 40);

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MONTH_TITLE_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private static final String C_LOGIN = "LOGIN";
    private static final String C_REGISTER = "REGISTER";
    private static final String C_MENU = "MENU";
    private static final String C_PROFILE = "PROFILE";
    private static final String C_DOCS = "DOCS";
    private static final String C_ADD_APP = "ADD_APP";
    private static final String C_EVENTS = "EVENTS";
    private static final String C_ADD_EVENT = "ADD_EVENT";
    private static final String C_EDIT_EVENT = "EDIT_EVENT";
    private static final String C_REMOVE_EVENT = "REMOVE_EVENT";
    private static final String C_NOTIFS = "NOTIFS";
    private static final String C_PROCESSES = "PROCESSES";
    private static final String C_STATS = "STATS";
    private static final String C_PROCESS_DETAILS = "PROCESS_DETAILS";

    private final Deque<String> navStack = new ArrayDeque<>();
    private String currentCard = null;

    // ===== LOGIN =====
    private JTextField loginEmailTF;
    private JPasswordField loginPassPF;

    // ===== REGISTER =====
    private JTextField regFullNameTF, regEmailTF, regPhoneTF, regFieldTF;
    private JPasswordField regPassPF;

    // ===== PROFILE VIEW =====
    private JLabel profNameVal, profEmailVal, profPhoneVal, profFieldVal;

    // ===== DOCS =====
    private java.awt.List awtDocsList;
    private Choice awtDocsChoice;
    private Checkbox awtPrimaryOnlyCB;
    private java.awt.Label awtDocsLabel;
    private Button awtOpenBtn;
    private Button awtDetailsBtn;
    private PopupMenu awtDocsPopup;
    private final java.util.List<Stores> docsAll = new ArrayList<>();
    private final List<Stores> awtDocsIndex = new ArrayList<>();

    // ===== ADD APP =====
    private JTextField appCompanyNameTF, appIndustryTF, appWebsiteTF;
    private JTextField appPositionIdTF, appTitleTF, appLocationTF;
    private JTextArea appDescriptionTA, appNotesTA;
    private JTextField appSourceTF;
    private JComboBox<String> appFieldCB;
    private JComboBox<String> appEmploymentCB;
    private JTextField cNameTF, cRoleTF, cEmailTF, cPhoneTF;
    private JTextField appPublishDateTF;
    private JLabel pdPublishDateVal;

    // ===== EVENTS =====
    private int calMonth;
    private int calYear;
    private LocalDate selectedDay;

    private JLabel calMonthLabel;
    private JPanel calGrid;

    private JComboBox<String> eventTypeCB;
    private JTextField eventTitleTF, eventDateTimeTF, eventDurationTF;
    private JTextArea eventNotesTA;

    private final DefaultListModel<Event> removeEventsModel = new DefaultListModel<>();
    private final JList<Event> removeEventsList = new JList<>(removeEventsModel);

    private final DefaultListModel<Event> editEventsModel = new DefaultListModel<>();
    private final JList<Event> editEventsList = new JList<>(editEventsModel);

    private JComboBox<String> editEventTypeCB;
    private JTextField editEventTitleTF, editEventDateTimeTF, editEventDurationTF;
    private JTextArea editEventNotesTA;

    // ===== NOTIFICATIONS =====
    private final DefaultListModel<NotifyAbout> notifsModel = new DefaultListModel<>();
    private final JList<NotifyAbout> notifsList = new JList<>(notifsModel);

    // ===== PROCESSES =====
    private final DefaultListModel<ApplyFor> appsModel = new DefaultListModel<>();
    private final JList<ApplyFor> appsList = new JList<>(appsModel);

    // ===== PROCESS DETAILS =====
    private ApplyFor selectedProcessApp;
    private JLabel pdPositionIdVal, pdTitleVal, pdCompanyVal, pdStageVal, pdSourceVal, pdAppliedAtVal;
    private JLabel pdLocationVal;
    private JTextArea pdNotesTA;
    private JComboBox<ApplicationStage> pdStageCB;
    private JTextField pdSourceTF;
    private JTextArea pdAddNoteTA;
    private JLabel pdContactVal;
    private JLabel pdLastContactVal;
    private JButton pdViewLastContactBtn;
    private JButton pdLogContactBtn;
    private JLabel pdStatusVal;
    private JButton pdToggleStatusBtn;

    // ===== STATS =====
    private final JLabel statsHeader = new JLabel("No data");
    private final JTextPane statsDetailsPane = new JTextPane();
    private final JTextArea statsStagesTA = new JTextArea(10, 24);
    private final WeekBarChartPanel statsWeekChart = new WeekBarChartPanel();

    // ==== menu bar ====
    private JPanel logWrap;
    private boolean logShown = false;

    public JobTrackerGUI(JobTracker tracker) {
        super("JobTracker");
        this.tracker = tracker;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        addCard(C_LOGIN, buildLoginCard());
        addCard(C_REGISTER, buildRegisterCard());
        addCard(C_MENU, buildMenuCard());
        addCard(C_PROFILE, buildProfileCard());
        addCard(C_DOCS, buildDocsCard());
        addCard(C_ADD_APP, buildAddApplicationCard());
        addCard(C_EVENTS, buildEventsCard());
        addCard(C_ADD_EVENT, buildAddEventCard());
        addCard(C_EDIT_EVENT, buildEditEventCard());
        addCard(C_REMOVE_EVENT, buildRemoveEventCard());
        addCard(C_NOTIFS, buildNotifsCard());
        addCard(C_PROCESSES, buildProcessesCard());
        addCard(C_PROCESS_DETAILS, buildProcessDetailsCard());
        addCard(C_STATS, buildStatsCard());

        add(cards, BorderLayout.CENTER);
        logWrap = buildLogPanel();
        logWrap.setVisible(false);
        add(logWrap, BorderLayout.SOUTH);

        setMenuBar(buildAwtMenuBar());

        setSize(1050, 720);
        setLocationRelativeTo(null);

        setVisible(true);

        showCard(C_LOGIN, false);
    }

    // =========================
    // Navigation + Layout
    // =========================
    private void addCard(String name, JPanel panel) {
        cards.add(panel, name);
    }

    private void showCard(String name) {
        showCard(name, true);
    }

    private void showCard(String name, boolean pushHistory) {
        if (name == null) return;
        if (pushHistory && currentCard != null && !Objects.equals(currentCard, name)) {
            navStack.push(currentCard);
        }
        currentCard = name;
        cardsLayout.show(cards, name);
    }

    private void clearInputsOnExit(String card) {
        if (card == null) return;
        switch (card) {
            case C_LOGIN -> {
                if (loginEmailTF != null) loginEmailTF.setText("");
                if (loginPassPF != null) loginPassPF.setText("");
            }
            case C_REGISTER -> {
                if (regFullNameTF != null) regFullNameTF.setText("");
                if (regEmailTF != null) regEmailTF.setText("");
                if (regPassPF != null) regPassPF.setText("");
                if (regPhoneTF != null) regPhoneTF.setText("");
                if (regFieldTF != null) regFieldTF.setText("");
            }
            case C_ADD_APP -> clearAddAppForm();
            case C_ADD_EVENT -> clearAddEventForm();
            default -> {}
        }
    }

    private void goBack() {
        if (currentCard != null) {
            clearInputsOnExit(currentCard);
        }
        if (!navStack.isEmpty()) {
            String prev = navStack.pop();
            currentCard = prev;
            cardsLayout.show(cards, prev);
            return;
        }
        if (activeUser != null) {
            currentCard = C_MENU;
            cardsLayout.show(cards, C_MENU);
        } else {
            currentCard = C_LOGIN;
            cardsLayout.show(cards, C_LOGIN);
        }
    }

    private JPanel header(String title, boolean showBack, boolean showLogout) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (showBack) {
            JButton back = new JButton("Back");
            back.addActionListener(e -> goBack());
            right.add(back);
        }
        if (showLogout) {
            JButton logout = new JButton("Logout");
            logout.addActionListener(e -> doLogout());
            right.add(logout);
        }

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setPreferredSize(right.getPreferredSize());
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 20f));
        h.add(left, BorderLayout.WEST);
        h.add(t, BorderLayout.CENTER);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(new EmptyBorder(6, 10, 8, 10));
        p.add(new JLabel("Log"), BorderLayout.NORTH);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return p;
    }

    private void log(String s) {
        logArea.append(s + "\n");
    }

    private boolean requireLogin() {
        if (activeUser != null) return true;
        JOptionPane.showMessageDialog(this, "Please login first.", "Login", JOptionPane.WARNING_MESSAGE);
        navStack.clear();
        showCard(C_LOGIN, false);
        return false;
    }

    private void doLogout() {
        activeUser = null;
        navStack.clear();
        log("Logged out.");
        showCard(C_LOGIN, false);
    }

    // =========================
    // LOGIN / REGISTER
    // =========================
    private JPanel buildLoginCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel title = new JLabel("Welcome to JobTracker system! (:");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        root.add(title, BorderLayout.NORTH);

        JPanel centerWrap = new JPanel(new GridBagLayout());
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(25, 60, 25, 60));
        form.setMaximumSize(new Dimension(700, 500));

        loginEmailTF = new JTextField();
        loginPassPF = new JPasswordField();
        makeBigField(loginEmailTF);
        makeBigField(loginPassPF);
        form.add(stackLabelAndField("Email :", loginEmailTF));
        form.add(Box.createVerticalStrut(18));
        form.add(stackLabelAndField("Password :", loginPassPF));
        form.add(Box.createVerticalStrut(22));
        JButton loginBtn = new JButton("Login");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setPreferredSize(new Dimension(140, 40));
        loginBtn.addActionListener(e -> doLogin());
        JButton goRegister = new JButton("<html>No user? <u>Register here</u></html>");
        goRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        goRegister.setBorderPainted(false);
        goRegister.setContentAreaFilled(false);
        goRegister.setFocusPainted(false);
        goRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        goRegister.addActionListener(e -> showCard(C_REGISTER));
        form.add(loginBtn);
        form.add(Box.createVerticalStrut(14));
        form.add(goRegister);
        centerWrap.add(form);
        root.add(centerWrap, BorderLayout.CENTER);
        return root;
    }

    private void doLogin() {
        String email = loginEmailTF.getText().trim();
        String pass = new String(loginPassPF.getPassword());
        UserProfile u = tracker.login(email, pass);
        loginEmailTF.setText("");
        loginPassPF.setText("");
        if (u == null) {
            log("Login failed.");
            JOptionPane.showMessageDialog(this, "Login failed.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        activeUser = u;
        navStack.clear();
        log("Logged in as: " + safe(activeUser.getEmail()));
        showCard(C_MENU, false);
    }

    private JPanel buildRegisterCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 60, 20, 60));
        root.add(header("Register", true, false), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 200, 10, 200));
        GridBagConstraints gc = gb();
        gc.gridy = 0;
        regFullNameTF = new JTextField(22);
        regEmailTF = new JTextField(22);
        regPassPF = new JPasswordField(22);
        regPhoneTF = new JTextField(22);
        regFieldTF = new JTextField(22);

        addRow(form, gc, "Full name :", regFullNameTF);
        addRow(form, gc, "Email :", regEmailTF);
        addRow(form, gc, "Password :", regPassPF);
        addRow(form, gc, "Phone :", regPhoneTF);
        addRow(form, gc, "Field of search :", regFieldTF);

        JButton create = new JButton("Create User");
        create.addActionListener(e -> doRegister());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(create);
        root.add(form, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        return root;
    }

    private void doRegister() {
        try {
            tracker.registerUser(regFullNameTF.getText(), regEmailTF.getText(), new String(regPassPF.getPassword()), regPhoneTF.getText(), regFieldTF.getText());
            log("User created.");
            JOptionPane.showMessageDialog(this, "User created. Now login.", "OK", JOptionPane.INFORMATION_MESSAGE);
            navStack.clear();
            showCard(C_LOGIN, false);
        } catch (Exception ex) {
            log("Register failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Register Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================
    // MAIN MENU
    // =========================
    private JPanel buildMenuCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Main Menu", false, true), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBorder(new EmptyBorder(25, 120, 25, 120));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(12, 12, 12, 12);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        grid.add(menuBtn("Personal Area", () -> { refreshProfileView(); showCard(C_PROFILE); }), gc);

        gc.gridwidth = 1;

        gc.gridy = 1; gc.gridx = 0;
        grid.add(menuBtn("My Documents", () -> { refreshDocs(); showCard(C_DOCS); }), gc);
        gc.gridx = 1;
        grid.add(menuBtn("Add Application", () -> showCard(C_ADD_APP)), gc);

        gc.gridy = 2; gc.gridx = 0;
        grid.add(menuBtn("Events Calendar", () -> { resetCalendarToNow(); refreshCalendar(); showCard(C_EVENTS); }), gc);
        gc.gridx = 1;
        grid.add(menuBtn("My Notifications", () -> { refreshNotifs(); showCard(C_NOTIFS); }), gc);

        gc.gridy = 3; gc.gridx = 0;
        grid.add(menuBtn("My Processes", () -> { refreshProcesses(); showCard(C_PROCESSES); }), gc);
        gc.gridx = 1;
        grid.add(menuBtn("Statistics", () -> { refreshStats(); showCard(C_STATS); }), gc);

        root.add(grid, BorderLayout.CENTER);
        return root;
    }

    private JButton menuBtn(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(280, 60));
        b.addActionListener(e -> {
            if (!requireLogin()) return;
            action.run();
        });
        return b;
    }

    // =========================
    // PROFILE
    // =========================
    private JPanel buildProfileCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Personal Area", true, true), BorderLayout.NORTH);

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(20, 25, 20, 25)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(10, 10, 10, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;

        JLabel title = new JLabel("My Profile");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        gc.gridx = 0; gc.gridwidth = 2;
        card.add(title, gc);

        gc.gridwidth = 1;
        Font keyFont = new Font("SansSerif", Font.BOLD, 14);
        Font valFont = new Font("SansSerif", Font.PLAIN, 14);

        gc.gridy++; gc.gridx = 0;
        JLabel nameKey = new JLabel("Full name:"); nameKey.setFont(keyFont);
        card.add(nameKey, gc);
        gc.gridx = 1;
        profNameVal = new JLabel("-"); profNameVal.setFont(valFont);
        card.add(profNameVal, gc);

        gc.gridy++; gc.gridx = 0;
        JLabel emailKey = new JLabel("Email:"); emailKey.setFont(keyFont);
        card.add(emailKey, gc);
        gc.gridx = 1;
        profEmailVal = new JLabel("-"); profEmailVal.setFont(valFont);
        card.add(profEmailVal, gc);

        gc.gridy++; gc.gridx = 0;
        JLabel phoneKey = new JLabel("Phone:"); phoneKey.setFont(keyFont);
        card.add(phoneKey, gc);
        gc.gridx = 1;
        profPhoneVal = new JLabel("-"); profPhoneVal.setFont(valFont);
        card.add(profPhoneVal, gc);

        gc.gridy++; gc.gridx = 0;
        JLabel fieldKey = new JLabel("Field of search:"); fieldKey.setFont(keyFont);
        card.add(fieldKey, gc);
        gc.gridx = 1;
        profFieldVal = new JLabel("-"); profFieldVal.setFont(valFont);
        card.add(profFieldVal, gc);

        gc.gridy++; gc.gridx = 0; gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.EAST;

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton editBtn = new JButton("Edit Profile");
        editBtn.addActionListener(e -> openEditProfileDialog());

        JButton changePassBtn = new JButton("Change Password");
        changePassBtn.addActionListener(e -> openChangePasswordDialog());

        actions.add(editBtn);
        actions.add(changePassBtn);
        card.add(actions, gc);
        centerWrap.add(card);
        root.add(centerWrap, BorderLayout.CENTER);
        refreshProfileView();
        return root;
    }

    private void refreshProfileView() {
        if (activeUser == null) {
            profNameVal.setText("-");
            profEmailVal.setText("-");
            profPhoneVal.setText("-");
            profFieldVal.setText("-");
            return;
        }
        profNameVal.setText(safe(activeUser.getFullName()));
        profEmailVal.setText(safe(activeUser.getEmail()));
        profPhoneVal.setText(safe(activeUser.getPhone()));
        profFieldVal.setText(safe(activeUser.getFieldOfSearch()));
    }

    private void openEditProfileDialog() {
        if (!requireLogin()) return;
        JTextField fullName = new JTextField(safe(activeUser.getFullName()), 22);
        JTextField phone = new JTextField(safe(activeUser.getPhone()), 22);
        JTextField field = new JTextField(safe(activeUser.getFieldOfSearch()), 22);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        addRow(p, gc, "Full name :", fullName);
        addRow(p, gc, "Phone :", phone);
        addRow(p, gc, "Field of search :", field);
        int res = JOptionPane.showConfirmDialog(this, p, "Edit profile", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            tracker.editProfile(activeUser, fullName.getText(), phone.getText(), field.getText());
            log("Profile updated.");
            refreshProfileView();
        } catch (Exception ex2) {
            JOptionPane.showMessageDialog(this, ex2.getMessage(), "Update error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openChangePasswordDialog() {
        if (!requireLogin()) return;
        JPasswordField oldP = new JPasswordField(22);
        JPasswordField newP = new JPasswordField(22);
        JPasswordField againP = new JPasswordField(22);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        addRow(p, gc, "Old password :", oldP);
        addRow(p, gc, "New password :", newP);
        addRow(p, gc, "Repeat new password :", againP);

        int res = JOptionPane.showConfirmDialog(this, p, "Change password", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        String oldPass = new String(oldP.getPassword());
        String newPass = new String(newP.getPassword());
        String again = new String(againP.getPassword());
        if (!newPass.equals(again)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            tracker.changePassword(activeUser, oldPass, newPass);
            log("Password updated.");
            JOptionPane.showMessageDialog(this, "Password changed.", "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex2) {
            JOptionPane.showMessageDialog(this, ex2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================
    // DOCUMENTS
    // =========================
    private JPanel buildDocsCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("My Documents", true, true), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.setBorder(new EmptyBorder(10, 10, 10, 10));
        center.add(buildDocsAwtPanel(), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));

        JButton add = new JButton("Add Document");
        add.addActionListener(e -> openAddDocumentDialog());

        JButton remove = new JButton("Remove Document");
        remove.addActionListener(e -> removeSelectedOrByName());

        bottom.add(add);
        bottom.add(remove);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private Stores getSelectedStore() {
        return getSelectedStoreFromAwt();
    }

    private void refreshDocs() {
        try {
            if (!requireLogin()) return;

            docsAll.clear();
            Vector<Stores> all = tracker.listUserDocuments(activeUser);
            for (Stores s : all) {
                if (s == null || s.getDocument() == null) continue;
                docsAll.add(s);
            }

            log("Documents refreshed.");
            refreshAwtDocsView();
        } catch (Exception ex) {
            log("Docs refresh failed: " + ex.getMessage());
        }
    }

    private void openAddDocumentDialog() {
        if (!requireLogin()) return;
        JTextField name = new JTextField(22);
        JComboBox<String> type = new JComboBox<>(new String[]{"File", "URL"});
        JTextField target = new JTextField(22);
        JTextField note = new JTextField(22);
        JCheckBox primary = new JCheckBox("Mark as Primary");

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        addRow(p, gc, "Doc Name :", name);
        addRow(p, gc, "Doc Type (File/Link) :", type);
        addRow(p, gc, "Target (path or URL) :", target);
        addRow(p, gc, "Note :", note);

        gc.gridx = 0; gc.gridwidth = 2;
        p.add(primary, gc);

        int res = JOptionPane.showConfirmDialog(this, p, "Add Document", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            tracker.uploadDocument(activeUser, name.getText(), Objects.toString(type.getSelectedItem(), "File"),
                    target.getText(), note.getText(), primary.isSelected(), LocalDateTime.now());
            log("Document uploaded.");
            refreshDocs();
        } catch (Exception ex2) {
            JOptionPane.showMessageDialog(this, ex2.getMessage(), "Upload Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedOrByName() {
        if (!requireLogin()) return;
        Stores selected = getSelectedStore();
        String nameToRemove;
        if (selected != null && selected.getDocument() != null) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Remove document: " + safe(selected.getDocument().getDocName()) + " ?",
                    "Remove Document", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            nameToRemove = selected.getDocument().getDocName();
        } else {
            nameToRemove = JOptionPane.showInputDialog(this, "Enter document name to remove:",
                    "Remove Document", JOptionPane.QUESTION_MESSAGE);
            if (nameToRemove == null || nameToRemove.trim().isEmpty()) return;
            nameToRemove = nameToRemove.trim();
        }
        try {
            boolean ok = tracker.removeDocument(activeUser, nameToRemove);
            log(ok ? "Document removed." : "Document not found.");
            refreshDocs();
        } catch (Exception ex2) {
            JOptionPane.showMessageDialog(this, ex2.getMessage(), "Remove Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openStoreDetailsDialog(Stores s) {
        if (s == null || s.getDocument() == null) return;
        JTextArea ta = new JTextArea(12, 44);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        Runnable refreshDetails = () -> {
            Stores updated = tracker.getStoredDocument(activeUser, s.getDocument().getDocName());
            if (updated == null) updated = s;

            Document d = updated.getDocument();
            String note = updated.getNote();
            String storedAt = (updated.getStoredAt() == null) ? "-" : updated.getStoredAt().format(DT_FMT);
            if (note == null || note.isBlank()) note = "(no note)";

            String details = "Name: " + safe(d.getDocName()) + "\n" +
                    "Type: " + safe(d.getDocType()) + "\n" +
                    "Target: " + safe(d.getTarget()) + "\n" +
                    "Stored at: " + safe(storedAt) + "\n" +
                    "Primary: " + (updated.isPrimary() ? "Yes" : "No") + "\n\n" +
                    "Note:\n" + note;
            ta.setText(details);
            ta.setCaretPosition(0);
        };
        refreshDetails.run();
        JButton openBtn = new JButton("Open");
        openBtn.addActionListener(e -> openStoredDocument(tracker.getStoredDocument(activeUser, s.getDocument().getDocName())));

        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> {
            Stores current = tracker.getStoredDocument(activeUser, s.getDocument().getDocName());
            if (current != null) openEditStoreDialog(current);
            refreshDocs();
            refreshDetails.run();
        });

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftBtns.add(openBtn);
        leftBtns.add(editBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(ta).dispose());

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightBtns.add(closeBtn);

        JPanel btns = new JPanel(new BorderLayout());
        btns.add(leftBtns, BorderLayout.WEST);
        btns.add(rightBtns, BorderLayout.EAST);

        JDialog dlg = new JDialog(this, "Document Details", true);
        dlg.setLayout(new BorderLayout(8, 8));
        dlg.add(new JScrollPane(ta), BorderLayout.CENTER);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void openEditStoreDialog(Stores s) {
        if (!requireLogin()) return;
        if (s == null || s.getDocument() == null) return;
        Document d = s.getDocument();

        JTextField nameTF = new JTextField(safe(d.getDocName()), 22);
        JTextField targetTF = new JTextField(safe(d.getTarget()), 22);
        JCheckBox primaryCB = new JCheckBox("Primary", s.isPrimary());
        JTextArea noteTA = new JTextArea(5, 24);
        noteTA.setLineWrap(true);
        noteTA.setWrapStyleWord(true);
        noteTA.setText(s.getNote() == null ? "" : s.getNote());

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        addRow(p, gc, "Document Name:", nameTF);
        addRow(p, gc, "Target:", targetTF);

        gc.gridx = 0; gc.gridwidth = 2;
        p.add(primaryCB, gc);
        gc.gridy++;

        gc.gridwidth = 1;
        addRow(p, gc, "Note:", new JScrollPane(noteTA));

        int res = JOptionPane.showConfirmDialog(this, p, "Edit Document", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            tracker.updateStoredDocumentDetails(activeUser, d.getDocName(), nameTF.getText(),
                    targetTF.getText(), noteTA.getText(), primaryCB.isSelected());
            log("Document updated.");
            refreshDocs();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Edit failed: " + ex.getMessage(),
                    "Edit Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openStoredDocument(Stores s) {
        try {
            if (s == null || s.getDocument() == null) return;
            Document d = s.getDocument();
            String target = d.getTarget();
            if (target == null || target.isBlank()) return;
            if (!Desktop.isDesktopSupported()) return;
            Desktop desk = Desktop.getDesktop();
            if (isUrlDoc(d)) desk.browse(new URI(target.trim()));
            else {
                File f = new File(target.trim());
                if (!f.exists()) {
                    JOptionPane.showMessageDialog(this, "File not found:\n" + f.getAbsolutePath(),
                            "Open", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                desk.open(f);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Open failed: " + ex.getMessage(),
                    "Open", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isUrlDoc(Document d) {
        if (d == null) return false;
        String t = d.getDocType();
        return t != null && t.trim().equalsIgnoreCase("URL");
    }

    private Panel buildDocsAwtPanel() {
        Panel root = new Panel(new BorderLayout(6, 6));

        Panel top = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));

        awtDocsLabel = new java.awt.Label("View:");
        awtDocsChoice = new Choice();
        awtDocsChoice.add("All");
        awtDocsChoice.add("Files");
        awtDocsChoice.add("Links");

        awtPrimaryOnlyCB = new Checkbox("Primary only");

        awtOpenBtn = new Button("Open");
        awtDetailsBtn = new Button("Details");

        awtDocsChoice.addItemListener(e -> refreshAwtDocsView());
        awtPrimaryOnlyCB.addItemListener(e -> refreshAwtDocsView());

        awtOpenBtn.addActionListener(e -> openSelectedAwtDoc());
        awtDetailsBtn.addActionListener(e -> detailsSelectedAwtDoc());

        top.add(awtDocsLabel);
        top.add(awtDocsChoice);
        top.add(awtPrimaryOnlyCB);
        top.add(awtOpenBtn);
        top.add(awtDetailsBtn);

        awtDocsList = new java.awt.List(14, false);
        awtDocsList.addActionListener(e -> detailsSelectedAwtDoc());

        awtDocsPopup = new PopupMenu();

        MenuItem pmOpen = new MenuItem("Open");
        pmOpen.addActionListener(e -> openSelectedAwtDoc());
        awtDocsPopup.add(pmOpen);

        MenuItem pmDetails = new MenuItem("Details");
        pmDetails.addActionListener(e -> detailsSelectedAwtDoc());
        awtDocsPopup.add(pmDetails);

        awtDocsPopup.addSeparator();

        MenuItem pmRemove = new MenuItem("Remove");
        pmRemove.addActionListener(e -> removeSelectedAwtDoc());
        awtDocsPopup.add(pmRemove);

        awtDocsList.add(awtDocsPopup);

        awtDocsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { maybeShowDocsPopup(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { maybeShowDocsPopup(e); }
        });

        root.add(top, BorderLayout.NORTH);
        root.add(awtDocsList, BorderLayout.CENTER);

        refreshAwtDocsView();

        return root;
    }

    private void maybeShowDocsPopup(java.awt.event.MouseEvent e) {
        if (awtDocsPopup == null || awtDocsList == null) return;
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
            awtDocsPopup.show(awtDocsList, e.getX(), e.getY());
        }
    }

    private void refreshAwtDocsView() {
        if (awtDocsList == null) return;

        if (activeUser == null) {
            awtDocsList.removeAll();
            awtDocsIndex.clear();
            return;
        }

        awtDocsList.removeAll();
        awtDocsIndex.clear();

        String mode = (awtDocsChoice == null) ? "All" : Objects.toString(awtDocsChoice.getSelectedItem(), "All");
        boolean primaryOnly = awtPrimaryOnlyCB != null && awtPrimaryOnlyCB.getState();

        ArrayList<Stores> all = new ArrayList<>(docsAll);

        for (Stores s : all) {
            if (s == null || s.getDocument() == null) continue;

            if (primaryOnly && !s.isPrimary()) continue;

            boolean isUrl = isUrlDoc(s.getDocument());
            if (mode.equalsIgnoreCase("Files") && isUrl) continue;
            if (mode.equalsIgnoreCase("Links") && !isUrl) continue;

            Document d = s.getDocument();
            awtDocsList.add(d == null ? "-" : d.toString());
            awtDocsIndex.add(s);
        }

        if (awtDocsList.getItemCount() > 0) {
            awtDocsList.select(0);
        }
    }

    private Stores getSelectedStoreFromAwt() {
        if (awtDocsList == null) return null;
        int idx = awtDocsList.getSelectedIndex();
        if (idx < 0 || idx >= awtDocsIndex.size()) return null;
        return awtDocsIndex.get(idx);
    }

    private void openSelectedAwtDoc() {
        Stores s = getSelectedStoreFromAwt();
        if (s == null || s.getDocument() == null) return;
        Stores updated = tracker.getStoredDocument(activeUser, s.getDocument().getDocName());
        if (updated == null) updated = s;
        openStoredDocument(updated);
    }

    private void detailsSelectedAwtDoc() {
        Stores s = getSelectedStoreFromAwt();
        if (s == null || s.getDocument() == null) return;
        Stores updated = tracker.getStoredDocument(activeUser, s.getDocument().getDocName());
        if (updated == null) updated = s;
        openStoreDetailsDialog(updated);
    }

    private void removeSelectedAwtDoc() {
        if (!requireLogin()) return;
        Stores s = getSelectedStoreFromAwt();
        if (s == null || s.getDocument() == null) return;

        int ok = JOptionPane.showConfirmDialog(this,
                "Remove document: " + safe(s.getDocument().getDocName()) + " ?",
                "Remove Document", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            boolean done = tracker.removeDocument(activeUser, s.getDocument().getDocName());
            log(done ? "Document removed." : "Document not found.");
            refreshDocs();
        } catch (Exception ex2) {
            JOptionPane.showMessageDialog(this, ex2.getMessage(), "Remove Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================
    // ADD APPLICATION
    // =========================
    private JPanel buildAddApplicationCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Add Application", true, true), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 20, 10, 20));
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        appCompanyNameTF = new JTextField();
        appIndustryTF = new JTextField();
        appWebsiteTF = new JTextField();

        appPositionIdTF = new JTextField();
        appTitleTF = new JTextField();
        appLocationTF = new JTextField();
        appDescriptionTA = new JTextArea(3, 20);
        appNotesTA = new JTextArea(3, 20);
        appSourceTF = new JTextField();
        appFieldCB = new JComboBox<>(new String[]{"Software", "Hardware", "Embedded", "Data", "QA", "IT", "Other"});
        appEmploymentCB = new JComboBox<>(new String[]{"Student", "Internship", "Part-time", "Full-time", "Contract", "Other"});
        appPublishDateTF = new JTextField();

        cNameTF = new JTextField();
        cRoleTF = new JTextField();
        cEmailTF = new JTextField();
        cPhoneTF = new JTextField();

        addRow(form, gc, "Position ID: ", appPositionIdTF);
        addRow(form, gc, "Title: ", appTitleTF);
        addRow(form, gc, "Field: ", appFieldCB);
        addRow(form, gc, "Publish Date yyyy-MM-dd (optional): ", appPublishDateTF);
        addRow(form, gc, "Company Name: ", appCompanyNameTF);
        addRow(form, gc, "Industry: ", appIndustryTF);
        addRow(form, gc, "Company's website(URL): ", appWebsiteTF);
        addRow(form, gc, "Location: ", appLocationTF);
        addRow(form, gc, "Employment Type: ", appEmploymentCB);
        addRow(form, gc, "Description: ", new JScrollPane(appDescriptionTA));
        addRow(form, gc, "Source (where YOU applied): ", appSourceTF);
        addRow(form, gc, "Notes: ", new JScrollPane(appNotesTA));

        addRow(form, gc, "Contact Name (optional): ", cNameTF);
        addRow(form, gc, "Contact Role (optional): ", cRoleTF);
        addRow(form, gc, "Contact Email (optional): ", cEmailTF);
        addRow(form, gc, "Contact Phone (optional): ", cPhoneTF);

        JButton submit = new JButton("Submit");
        submit.addActionListener(e -> submitApplication());

        root.add(new JScrollPane(form), BorderLayout.CENTER);
        root.add(wrapSouth(submit), BorderLayout.SOUTH);
        return root;
    }

    private void submitApplication() {
        if (!requireLogin()) return;
        String positionId = appPositionIdTF.getText();
        String title = appTitleTF.getText();
        String field = Objects.toString(appFieldCB.getSelectedItem(), "Other");
        String location = appLocationTF.getText();
        String employmentType = Objects.toString(appEmploymentCB.getSelectedItem(), "Other");
        String description = appDescriptionTA.getText();

        String companyName = appCompanyNameTF.getText();
        String industry = appIndustryTF.getText();
        String website = appWebsiteTF.getText();

        LocalDate publishDate = null;
        String publishTxt = (appPublishDateTF == null) ? null : appPublishDateTF.getText();

        if (publishTxt != null && !publishTxt.trim().isEmpty()) {
            try {
                publishDate = LocalDate.parse(publishTxt.trim(), DateTimeFormatter.ISO_LOCAL_DATE); // yyyy-MM-dd
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid publish date. Use format yyyy-MM-dd (example: 2025-12-28)",
                        "Add Application Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String source = appSourceTF.getText();
        String notes = appNotesTA.getText();

        String contactName = cNameTF.getText();
        String contactRole = cRoleTF.getText();
        String contactEmail = cEmailTF.getText();
        String contactPhone = cPhoneTF.getText();

        final JobPosition position;
        final Company company;
        try {
            company = new Company(companyName, industry, website);
            position = new JobPosition(positionId, title, field, location, employmentType, "Active", description);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Add Application Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        AddApplicationTask task = new AddApplicationTask(tracker, activeUser, position, company, publishDate, source, notes, contactName,
                contactRole, contactEmail, contactPhone, LocalDateTime.now());
        Thread t = new Thread(() -> {
            task.run(); //background work
            SwingUtilities.invokeLater(() -> {
                if (task.isSuccess()) {
                    log("Application added.");
                    JOptionPane.showMessageDialog(this, "Application added.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    clearAddAppForm();
                    refreshProcesses();
                    showCard(C_MENU, false);
                } else {
                    JOptionPane.showMessageDialog(this, task.getErrorMessage(), "Add Application Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }, "AddApplicationTask-Thread");
        t.start();
    }

    private void clearAddAppForm() {
        appCompanyNameTF.setText("");
        appIndustryTF.setText("");
        appWebsiteTF.setText("");
        appPositionIdTF.setText("");
        appTitleTF.setText("");
        appLocationTF.setText("");
        if (appFieldCB != null) appFieldCB.setSelectedIndex(0);
        if (appEmploymentCB != null) appEmploymentCB.setSelectedIndex(0);
        appDescriptionTA.setText("");
        appNotesTA.setText("");
        appSourceTF.setText("");
        appPublishDateTF.setText("");

        cNameTF.setText("");
        cRoleTF.setText("");
        cEmailTF.setText("");
        cPhoneTF.setText("");
    }

    // =========================
    // EVENTS (Calendar)
    // =========================
    private JPanel buildEventsCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Events", true, true), BorderLayout.NORTH);
        resetCalendarToNow();
        JButton prev = new JButton("<");
        JButton next = new JButton(">");

        calMonthLabel = new JLabel("", SwingConstants.CENTER);
        calMonthLabel.setFont(calMonthLabel.getFont().deriveFont(Font.BOLD, 18f));

        prev.addActionListener(e -> { shiftMonth(-1); refreshCalendar(); });
        next.addActionListener(e -> { shiftMonth(+1); refreshCalendar(); });

        JPanel calTop = new JPanel(new BorderLayout(8, 8));
        calTop.setBorder(new EmptyBorder(10, 10, 10, 10));
        calTop.add(prev, BorderLayout.WEST);
        calTop.add(calMonthLabel, BorderLayout.CENTER);
        calTop.add(next, BorderLayout.EAST);

        calGrid = new JPanel(new GridLayout(0, 7, 6, 6));
        JPanel calWrap = new JPanel(new BorderLayout());
        calWrap.setBorder(new EmptyBorder(10, 10, 10, 10));
        calWrap.add(calTop, BorderLayout.NORTH);
        calWrap.add(calGrid, BorderLayout.CENTER);
        root.add(calWrap, BorderLayout.CENTER);

        JButton addBtn = new JButton("Add Event");
        addBtn.addActionListener(e -> {
            showCard(C_ADD_EVENT);
            if (selectedDay != null && eventDateTimeTF != null) {
                eventDateTimeTF.setText(selectedDay.atTime(9, 0).format(DT_FMT));
            }
        });
        JButton editBtn = new JButton("Edit Event");
        editBtn.addActionListener(e -> { refreshEditEventsList(); showCard(C_EDIT_EVENT); });
        JButton removeBtn = new JButton("Remove Event");
        removeBtn.addActionListener(e -> { refreshRemoveEventsList(); showCard(C_REMOVE_EVENT); });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bottom.add(addBtn);
        bottom.add(editBtn);
        bottom.add(removeBtn);

        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private void resetCalendarToNow() {
        LocalDateTime now = LocalDateTime.now();
        calMonth = now.getMonthValue();
        calYear = now.getYear();
        selectedDay = now.toLocalDate();
    }

    private void shiftMonth(int delta) {
        calMonth += delta;
        if (calMonth == 0) { calMonth = 12; calYear--; }
        if (calMonth == 13) { calMonth = 1; calYear++; }
        selectedDay = YearMonth.of(calYear, calMonth).atDay(1);
    }

    private void refreshCalendar() {
        try {
            if (!requireLogin()) return;
            calGrid.removeAll();
            YearMonth ym = YearMonth.of(calYear, calMonth);
            LocalDate firstDay = ym.atDay(1);
            calMonthLabel.setText(firstDay.format(MONTH_TITLE_FMT));

            Vector<Event> monthEvents = tracker.listEvents(activeUser, calMonth, calYear);
            Map<LocalDate, List<Event>> byDay = new HashMap<>();
            for (Event ev : monthEvents) {
                if (ev == null || ev.getDateTime() == null) continue;
                LocalDate day = ev.getDateTime().toLocalDate();
                byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(ev);
            }
            for (List<Event> list : byDay.values()) {
                list.sort(Comparator.comparing(Event::getDateTime));
            }
            String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (String n : names) {
                JLabel l = new JLabel(n, SwingConstants.CENTER);
                l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
                JPanel p = new JPanel(new BorderLayout());
                p.add(l, BorderLayout.CENTER);
                p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 210, 210)));
                calGrid.add(p);
            }
            int offset = firstDay.getDayOfWeek().getValue() % 7;
            int daysInMonth = ym.lengthOfMonth();
            int totalCells = 42;
            for (int cell = 0; cell < totalCells; cell++) {
                int dayNum = cell - offset + 1;
                if (dayNum < 1 || dayNum > daysInMonth) {
                    calGrid.add(createEmptyDayCell());
                    continue;
                }
                LocalDate date = ym.atDay(dayNum);
                List<Event> eventsThisDay = byDay.getOrDefault(date, List.of());
                calGrid.add(createDayCell(date, eventsThisDay));
            }
            calGrid.revalidate();
            calGrid.repaint();
            log("Events calendar refreshed: " + calMonth + "/" + calYear);
        } catch (Exception ex) {
            log("Calendar refresh failed: " + ex.getMessage());
        }
    }

    private JComponent createEmptyDayCell() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        p.setBackground(new Color(250, 250, 250));
        return p;
    }

    private void openDayEventsDialog(LocalDate date, java.util.List<Event> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("Events on ").append(date).append("\n\n");
        if (events != null) {
            for (Event ev : events) {
                if (ev == null || ev.getDateTime() == null) continue;
                String t = ev.getDateTime().toLocalTime().format(TIME_FMT);
                sb.append(t).append(" | ").append(ev.getType() == null ? "Other" : ev.getType())
                        .append(" | ").append(ev.getTitle() == null ? "(no title)" : ev.getTitle()).append("\n");
                String notes = ev.getNotes();
                if (notes != null && !notes.isBlank()) {
                    sb.append("   Notes: ").append(notes.trim()).append("\n");
                }
                sb.append("\n");
            }
        }
        JTextArea ta = new JTextArea(sb.toString(), 14, 48);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Day Events", JOptionPane.INFORMATION_MESSAGE);
    }

    private JComponent createDayCell(LocalDate date, List<Event> events) {
        JPanel cell = new JPanel(new BorderLayout(4, 4));
        if (selectedDay != null && selectedDay.equals(date)) {
            cell.setBorder(BorderFactory.createLineBorder(new Color(60, 120, 220), 2));
        } else {
            cell.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        }
        cell.setBackground(Color.WHITE);

        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
        dayLabel.setBorder(new EmptyBorder(4, 6, 0, 0));
        dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD, 14f));
        cell.add(dayLabel, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(0, 6, 6, 6));
        list.setOpaque(false);
        int maxShow = 3;
        for (int i = 0; i < Math.min(maxShow, events.size()); i++) {
            Event ev = events.get(i);
            if (ev == null || ev.getDateTime() == null) continue;
            String t = ev.getDateTime().toLocalTime().format(TIME_FMT);
            JLabel l = new JLabel(t + " - " + safe(ev.getTitle()));
            l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
            list.add(l);
        }
        if (events.size() > maxShow) {
            JLabel more = new JLabel("...");
            more.setFont(more.getFont().deriveFont(Font.PLAIN, 12f));
            list.add(more);
        }
        cell.add(list, BorderLayout.CENTER);

        cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
        java.awt.event.MouseAdapter clickHandler = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectedDay = date;
                refreshCalendar();
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (events != null && !events.isEmpty()) openDayEventsDialog(date, events);
                    else JOptionPane.showMessageDialog(JobTrackerGUI.this, "No events on this day.", "Day Events", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        cell.addMouseListener(clickHandler);
        dayLabel.addMouseListener(clickHandler);
        list.addMouseListener(clickHandler);
        for (Component comp : list.getComponents()) comp.addMouseListener(clickHandler);

        return cell;
    }

    private JPanel buildAddEventCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Add Event", true, true), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 20, 10, 20));
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        eventTypeCB = new JComboBox<>(new String[]{"Interview", "Phone Call", "Follow-up", "Meeting", "Deadline", "Other"});
        eventTitleTF = new JTextField();
        eventDateTimeTF = new JTextField();
        eventDurationTF = new JTextField();
        eventNotesTA = new JTextArea(4, 20);

        addRow(form, gc, "Type", eventTypeCB);
        addRow(form, gc, "Title", eventTitleTF);
        addRow(form, gc, "DateTime (yyyy-MM-dd HH:mm)", eventDateTimeTF);
        addRow(form, gc, "Duration (minutes)", eventDurationTF);
        addRow(form, gc, "Notes", new JScrollPane(eventNotesTA));

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> submitEvent());

        root.add(new JScrollPane(form), BorderLayout.CENTER);
        root.add(wrapSouth(addBtn), BorderLayout.SOUTH);
        return root;
    }

    private void submitEvent() {
        if (!requireLogin()) return;
        final Event ev;
        try {
            LocalDateTime dt = LocalDateTime.parse(eventDateTimeTF.getText().trim(), DT_FMT);
            int dur = Integer.parseInt(eventDurationTF.getText().trim());
            ev = new Event(
                    Objects.toString(eventTypeCB.getSelectedItem(), "Other"),
                    eventTitleTF.getText().trim(), dt, dur, eventNotesTA.getText());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Event Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        AddEventThread t = new AddEventThread(tracker, activeUser, ev);
        t.start();
        new Thread(() -> {
            try {
                t.awaitAddResult();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Thread interrupted.", "Event Error", JOptionPane.ERROR_MESSAGE));
                return;
            }
            SwingUtilities.invokeLater(() -> {
                if (!t.isAddOk()) {
                    JOptionPane.showMessageDialog(this, t.getAddError(), "Event Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                log("Event added.");
                refreshCalendar();
                refreshNotifs();
                clearAddEventForm();
                showCard(C_EVENTS, false);
            });
        }, "WaitAddPhase").start();
    }

    private void clearAddEventForm() {
        eventTitleTF.setText("");
        eventDateTimeTF.setText("");
        eventDurationTF.setText("");
        eventNotesTA.setText("");
        if (eventTypeCB.getItemCount() > 0) eventTypeCB.setSelectedIndex(0);
    }

    private JPanel buildRemoveEventCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Remove Event", true, true), BorderLayout.NORTH);

        removeEventsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        removeEventsList.setCellRenderer(eventCellRenderer());
        JButton remove = new JButton("Remove");
        remove.addActionListener(e -> removeSelectedEvent());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bottom.add(remove);
        root.add(new JScrollPane(removeEventsList), BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private void refreshRemoveEventsList() {
        try {
            if (!requireLogin()) return;
            removeEventsModel.clear();
            Vector<Event> monthEvents = tracker.listEvents(activeUser, calMonth, calYear);
            ArrayList<Event> filtered = new ArrayList<>();
            for (Event ev : monthEvents) {
                if (ev == null || ev.getDateTime() == null) continue;
                if (selectedDay != null && !ev.getDateTime().toLocalDate().equals(selectedDay)) continue;
                filtered.add(ev);
            }
            filtered.sort(Comparator.comparing(Event::getDateTime));
            filtered.forEach(removeEventsModel::addElement);
        } catch (Exception ex) {
            log("refreshRemoveEventsList failed: " + ex.getMessage());
        }
    }

    private void removeSelectedEvent() {
        if (!requireLogin()) return;
        Event ev = removeEventsList.getSelectedValue();
        if (ev == null) {
            JOptionPane.showMessageDialog(this, "Select an event first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Remove selected event?", "Remove Event", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        tracker.cancelEvent(activeUser, ev);
        log("Event removed.");
        refreshCalendar();
        refreshNotifs();
        showCard(C_EVENTS, false);
    }

    private JPanel buildEditEventCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Edit Event", true, true), BorderLayout.NORTH);
        editEventsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        editEventsList.setCellRenderer(eventCellRenderer());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        editEventTypeCB = new JComboBox<>(new String[]{"Interview", "Call", "Meeting", "Deadline", "Other"});
        editEventTitleTF = new JTextField();
        editEventDateTimeTF = new JTextField();
        editEventDurationTF = new JTextField();
        editEventNotesTA = new JTextArea(4, 20);

        addRow(form, gc, "Type :", editEventTypeCB);
        addRow(form, gc, "Title :", editEventTitleTF);
        addRow(form, gc, "DateTime (yyyy-MM-dd HH:mm) :", editEventDateTimeTF);
        addRow(form, gc, "Duration (minutes) :", editEventDurationTF);
        addRow(form, gc, "Notes :", new JScrollPane(editEventNotesTA));

        JButton save = new JButton("Save Changes");
        save.addActionListener(e -> submitEditEvent());
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.add(form, BorderLayout.CENTER);
        right.add(wrapSouth(save), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(editEventsList), right);
        split.setResizeWeight(0.45);

        editEventsList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Event ev = editEventsList.getSelectedValue();
            if (ev != null) fillEditFormFromEvent(ev);
        });
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    private void refreshEditEventsList() {
        try {
            if (!requireLogin()) return;
            editEventsModel.clear();
            Vector<Event> monthEvents = tracker.listEvents(activeUser, calMonth, calYear);
            ArrayList<Event> filtered = new ArrayList<>();
            for (Event ev : monthEvents) {
                if (ev == null || ev.getDateTime() == null) continue;
                if (selectedDay != null && !ev.getDateTime().toLocalDate().equals(selectedDay)) continue;
                filtered.add(ev);
            }
            filtered.sort(Comparator.comparing(Event::getDateTime));
            filtered.forEach(editEventsModel::addElement);
            if (!filtered.isEmpty()) {
                editEventsList.setSelectedIndex(0);
                fillEditFormFromEvent(filtered.get(0));
            }
        } catch (Exception ex) {
            log("refreshEditEventsList failed: " + ex.getMessage());
        }
    }

    private void fillEditFormFromEvent(Event ev) {
        editEventTypeCB.setSelectedItem(ev.getType());
        editEventTitleTF.setText(safe(ev.getTitle()));
        editEventDateTimeTF.setText(ev.getDateTime() == null ? "" : ev.getDateTime().format(DT_FMT));
        editEventDurationTF.setText(String.valueOf(ev.getDuration()));
        editEventNotesTA.setText(safe(ev.getNotes()));
    }

    private void submitEditEvent() {
        if (!requireLogin()) return;
        Event ev = editEventsList.getSelectedValue();
        if (ev == null) {
            JOptionPane.showMessageDialog(this, "Select an event first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            LocalDateTime newDT = LocalDateTime.parse(editEventDateTimeTF.getText().trim(), DT_FMT);
            int newDur = Integer.parseInt(editEventDurationTF.getText().trim());

            String newType  = Objects.toString(editEventTypeCB.getSelectedItem(), "Other");
            String newTitle = editEventTitleTF.getText();
            String newNotes = editEventNotesTA.getText();

            tracker.updateEventDetails(activeUser, ev, newType, newTitle, newDT, newDur, newNotes);
            log("Event updated.");
            refreshCalendar();
            refreshNotifs();
            showCard(C_EVENTS, false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Edit Event Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DefaultListCellRenderer eventCellRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Event ev && ev.getDateTime() != null) {
                    String t = ev.getDateTime().toLocalTime().format(TIME_FMT);
                    setText(t + " | " + safe(ev.getType()) + " | " + safe(ev.getTitle()));
                }
                return this;
            }
        };
    }

    // =========================
    // NOTIFICATIONS
    // =========================
    private JPanel buildNotifsCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("My Notifications", true, true), BorderLayout.NORTH);
        notifsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notifsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NotifyAbout n) {
                    setText(n.toString());
                    if (!isSelected) {
                        setBackground(n.isSeen() ? new Color(245, 245, 245) : new Color(230, 245, 255));
                    }
                }
                return this;
            }
        });
        JButton markSeen = new JButton("Mark Selected As Seen");
        markSeen.addActionListener(e -> markNotifSeen());

        JButton markAll = new JButton("Mark All As Seen");
        markAll.addActionListener(e -> markAllNotifsSeen());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(markAll);
        bottom.add(markSeen);

        root.add(new JScrollPane(notifsList), BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private void refreshNotifs() {
        try {
            if (!requireLogin()) return;
            notifsModel.clear();
            List<NotifyAbout> list = tracker.getNotificationsToDisplay(activeUser);
            for (NotifyAbout n : list) notifsModel.addElement(n);
            log("Notifications refreshed.");
        } catch (Exception ex) {
            log("Notifs refresh failed: " + ex.getMessage());
        }
    }

    private void markNotifSeen() {
        try {
            if (!requireLogin()) return;
            NotifyAbout n = notifsList.getSelectedValue();
            if (n == null) return;
            tracker.markNotificationSeen(activeUser, n);
            refreshNotifs();
        } catch (Exception ex) {
            log("Mark seen failed: " + ex.getMessage());
        }
    }

    private void markAllNotifsSeen() {
        try {
            if (!requireLogin()) return;
            tracker.markAllNotificationsSeen(activeUser);
            refreshNotifs();
        } catch (Exception ex) {
            log("Mark all seen failed: " + ex.getMessage());
        }
    }

    // =========================
    // PROCESSES
    // =========================
    private JPanel buildProcessesCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("My Processes", true, true), BorderLayout.NORTH);
        appsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    ApplyFor app = appsList.getSelectedValue();
                    if (app != null) openProcessDetails(app);
                }
            }
        });

        appsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ApplyFor app) {
                    Color bg = new Color(210, 255, 210); //default green
                    String st = (app.getPosition() == null) ? "" : String.valueOf(app.getPosition().getStatus());
                    boolean posNotActive = st.equalsIgnoreCase("Not Active");
                    if (tracker.isFinalStage(app.getStage())) {
                        bg = new Color(255, 210, 210); //final stage red
                    } else if (posNotActive) {
                        bg = new Color(230, 230, 230); //not active grey
                    } else if (tracker.isApplicationOverdue(activeUser, app)) {
                        bg = new Color(255, 245, 180); //overdue yellow
                    }
                    if (isSelected) c.setBackground(bg.darker());
                    else c.setBackground(bg);
                }
                return c;
            }
        });
        JButton details = new JButton("Open Details");
        details.addActionListener(e -> {
            ApplyFor app = appsList.getSelectedValue();
            if (app != null) openProcessDetails(app);
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(details);

        root.add(new JScrollPane(appsList), BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private void refreshProcesses() {
        try {
            if (!requireLogin()) return;
            appsModel.clear();
            Vector<ApplyFor> all = tracker.listApplications(activeUser);
            for (ApplyFor a : all) appsModel.addElement(a);
            log("Processes refreshed.");
        } catch (Exception ex) {
            log("Processes refresh failed: " + ex.getMessage());
        }
    }

    private void openProcessDetails(ApplyFor app) {
        selectedProcessApp = app;
        refreshProcessDetails();
        showCard(C_PROCESS_DETAILS);
    }

    private JPanel buildProcessDetailsCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Process Details", true, true), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBorder(new EmptyBorder(15, 20, 15, 20));
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        pdPositionIdVal = new JLabel("-");
        pdTitleVal = new JLabel("-");
        pdLocationVal = new JLabel("-");
        pdCompanyVal = new JLabel("-");
        pdStageVal = new JLabel("-");
        pdSourceVal = new JLabel("-");
        pdAppliedAtVal = new JLabel("-");
        pdContactVal = new JLabel("-");
        pdLastContactVal = new JLabel("-"); // NEW
        pdStatusVal = new JLabel("-");
        pdPublishDateVal = new JLabel("-");

        addRow(grid, gc, "Position ID:", pdPositionIdVal);
        addRow(grid, gc, "Publish date:", pdPublishDateVal);
        addRow(grid, gc, "Title:", pdTitleVal);
        addRow(grid, gc, "Location:", pdLocationVal);

        JPanel companyRow = new JPanel(new BorderLayout(8, 0));
        companyRow.setOpaque(false);

        JButton companyViewBtn = new JButton("View company's details");
        companyViewBtn.addActionListener(e -> openCompanyDetailsForSelectedProcess());

        companyRow.add(pdCompanyVal, BorderLayout.CENTER);
        companyRow.add(companyViewBtn, BorderLayout.EAST);
        addRow(grid, gc, "Company:", companyRow);

        addRow(grid, gc, "Stage:", pdStageVal);
        addRow(grid, gc, "Source:", pdSourceVal);
        addRow(grid, gc, "Applied at:", pdAppliedAtVal);

        //Contact + Last Contact
        addRow(grid, gc, "Contact:", pdContactVal);
        addRow(grid, gc, "Last Contact:", pdLastContactVal);

        //Buttons below Contact + Last Contact
        pdViewLastContactBtn = new JButton("View Last Contact");
        pdLogContactBtn = new JButton("Log Communication");
        pdViewLastContactBtn.addActionListener(e -> showLastContactDialogForSelectedProcess());
        pdLogContactBtn.addActionListener(e -> openLogCommunicationDialogForSelectedProcess());

        JPanel commBtnsRow = new JPanel();
        commBtnsRow.setOpaque(false);
        commBtnsRow.setLayout(new BoxLayout(commBtnsRow, BoxLayout.X_AXIS));

        commBtnsRow.add(Box.createHorizontalGlue());
        commBtnsRow.add(pdViewLastContactBtn);
        commBtnsRow.add(Box.createHorizontalStrut(8));
        commBtnsRow.add(pdLogContactBtn);

        addRow(grid, gc, "", commBtnsRow);
        //Position Status + Toggle Button
        pdToggleStatusBtn = new JButton("Toggle Status");
        pdToggleStatusBtn.addActionListener(e -> {
            if (!requireLogin()) return;
            if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;

            String pid = selectedProcessApp.getPosition().getPositionID();
            boolean ok = tracker.togglePositionStatus(pid);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Failed to update status.", "Status", JOptionPane.ERROR_MESSAGE);
                return;
            }
            refreshProcessDetails();
            refreshProcesses();
            refreshStats();
        });
        JPanel statusRow = new JPanel(new BorderLayout(8, 0));
        statusRow.setOpaque(false);
        statusRow.add(pdStatusVal, BorderLayout.CENTER);
        statusRow.add(pdToggleStatusBtn, BorderLayout.EAST);

        addRow(grid, gc, "Position Status:", statusRow);

        //Notes
        pdNotesTA = new JTextArea(7, 28);
        pdNotesTA.setEditable(false);
        pdNotesTA.setLineWrap(true);
        pdNotesTA.setWrapStyleWord(true);

        JScrollPane notesScroll = new JScrollPane(pdNotesTA);
        notesScroll.setPreferredSize(new Dimension(420, 160));

        gc.gridwidth = 1;
        gc.gridx = 0;
        gc.weightx = 0;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        grid.add(new JLabel("Notes:"), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        grid.add(notesScroll, gc);

        gc.gridy++;

        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;

        //actions panel
        JPanel actions = new JPanel(new GridBagLayout());
        actions.setBorder(new EmptyBorder(10, 0, 0, 0));
        GridBagConstraints ac = gb();
        ac.gridy = 0;

        pdStageCB = new JComboBox<>(ApplicationStage.values());
        addRow(actions, ac, "New Stage:", pdStageCB);

        pdSourceTF = new JTextField(22);
        addRow(actions, ac, "New Source:", pdSourceTF);

        pdAddNoteTA = new JTextArea(3, 22);
        pdAddNoteTA.setLineWrap(true);
        pdAddNoteTA.setWrapStyleWord(true);
        addRow(actions, ac, "Append Note:", new JScrollPane(pdAddNoteTA));

        JButton contactBtn = new JButton("Add / Edit Contact");
        contactBtn.addActionListener(e -> openAddOrEditContactForSelectedProcess());
        addRow(actions, ac, "", contactBtn);

        JButton saveBtn = new JButton("Save Changes");
        saveBtn.addActionListener(e -> {
            if (!requireLogin()) return;
            if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;
            String pid = selectedProcessApp.getPosition().getPositionID();
            ApplicationStage newStage = (ApplicationStage) pdStageCB.getSelectedItem();
            String newSource = pdSourceTF.getText();
            String note = pdAddNoteTA.getText();
            try {
                tracker.updateProcessFromDetailsForm(activeUser, pid, newStage, newSource, note);
                pdAddNoteTA.setText("");
                pdSourceTF.setText("");
                refreshProcesses();
                refreshProcessDetails();
                showCard(C_PROCESSES, false);
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(this, ex2.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        addRow(actions, ac, "", saveBtn);

        JButton withdrawBtn = new JButton("Withdraw");
        withdrawBtn.addActionListener(e -> {
            if (!requireLogin()) return;
            if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;
            tracker.withdrawApplication(activeUser, selectedProcessApp.getPosition().getPositionID());
            refreshProcesses();
            refreshProcessDetails();
        });
        JButton removeBtn = new JButton("Remove Process");
        removeBtn.addActionListener(e -> {
            if (!requireLogin()) return;
            if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;
            int ok = JOptionPane.showConfirmDialog(this, "Remove this process?", "Remove", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            tracker.removeApplication(activeUser, selectedProcessApp.getPosition().getPositionID());
            selectedProcessApp = null;
            refreshProcesses();
            showCard(C_PROCESSES, false);
        });
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.add(withdrawBtn);
        btnRow.add(removeBtn);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(grid, BorderLayout.WEST);
        center.add(actions, BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);
        root.add(btnRow, BorderLayout.SOUTH);
        return root;
    }

    private void showLastContactDialogForSelectedProcess() {
        if (!requireLogin()) return;
        if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;

        String pid = selectedProcessApp.getPosition().getPositionID();
        String info = tracker.getLastContactInfoForPosition(activeUser, pid);
        long days = tracker.daysSinceLastContactForPosition(activeUser, pid);

        //If there is no communication - not available
        String msg;
        if (info == null || info.trim().isEmpty() || days < 0) {
            msg = "Not available";
        } else {
            msg = safe(info) + "\n\nDays since last contact: " + days;
        }

        JTextArea ta = new JTextArea(msg, 10, 44);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Last Contact", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openLogCommunicationDialogForSelectedProcess() {
        if (!requireLogin()) return;
        if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;

        String pid = selectedProcessApp.getPosition().getPositionID();
        //ensure a contact exists for the position
        Contact c = tracker.getContactForPosition(activeUser, pid);
        if (c == null) {
            int ok = JOptionPane.showConfirmDialog(
                    this,
                    "No contact is linked to this position.\nYou must add a contact first.\n\nOpen Add/Edit Contact now?",
                    "No Contact",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (ok != JOptionPane.OK_OPTION) return;
            openAddOrEditContactForSelectedProcess();
            //re-check after adding
            c = tracker.getContactForPosition(activeUser, pid);
            if (c == null) return;
        }
        JComboBox<String> methodCB = new JComboBox<>(new String[]{"Email", "Phone", "LinkedIn", "WhatsApp", "Meeting", "Other"
        });
        JTextField subjectTF = new JTextField(26);
        JTextField WhenContactedTF = new JTextField(LocalDateTime.now().format(DT_FMT), 26);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        addRow(p, gc, "Method:", methodCB);
        addRow(p, gc, "Subject / Summary:", subjectTF);
        addRow(p, gc, "When (yyyy-MM-dd HH:mm):", WhenContactedTF);
        int res = JOptionPane.showConfirmDialog(this, p, "Log Communication", JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;
        String method = Objects.toString(methodCB.getSelectedItem(), "Other");
        String subject = subjectTF.getText();

        LocalDateTime when;
        try {
            String txt = WhenContactedTF.getText().trim();
            when = txt.isEmpty() ? LocalDateTime.now() : LocalDateTime.parse(txt, DT_FMT);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid date/time. Use format yyyy-MM-dd HH:mm (example: 2026-01-03 10:20)",
                    "Log Communication Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            tracker.logLastContactForPosition(activeUser, pid, method, subject, when); // חדש
            log("Communication logged for position: " + pid);

            refreshProcessDetails();
            refreshProcesses();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Log Communication Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCompanyDetailsForSelectedProcess() {
        if (!requireLogin()) return;
        if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;
        String pid = selectedProcessApp.getPosition().getPositionID();
        String txt = tracker.buildCompanyDetailsTextForPosition(pid);

        JTextArea ta = new JTextArea(12, 44);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setText(txt);
        ta.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Company Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshProcessDetails() {
        try {
            if (!requireLogin()) return;
            if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;

            JobPosition p = selectedProcessApp.getPosition();
            String pid = p.getPositionID();

            String status = safe(p.getStatus());
            pdStatusVal.setText(status.isBlank() ? "-" : status);
            boolean isActive = !status.equalsIgnoreCase("Not Active");
            pdToggleStatusBtn.setText(isActive ? "Mark Not Active" : "Mark Active");

            LocalDate pub = tracker.getPublishDateForPosition(pid);
            if (pub == null) {
                pdPublishDateVal.setText("-");
            } else {
                pdPublishDateVal.setText(pub.toString() + tracker.publishAgeInParentheses(pid));
            }

            Company c = tracker.getCompanyForPosition(pid);
            Contact contact = tracker.getContactForPosition(activeUser, pid);

            if (contact == null) {
                pdContactVal.setText("No contact for this position.");
            } else {
                String line = contact.getContactCard();
                pdContactVal.setText(line);
            }
            //Last Contact
            String info = tracker.getLastContactInfoForPosition(activeUser, pid);
            long days = tracker.daysSinceLastContactForPosition(activeUser, pid);
            //If there is no communication - not available
            if (info == null || info.trim().isEmpty() || days < 0) {
                pdLastContactVal.setText("Not available");
                if (pdViewLastContactBtn != null) pdViewLastContactBtn.setEnabled(false);
            } else {
                String raw = info.trim();
                String displayDate = raw;

                int idx = raw.toLowerCase().indexOf("last contact:");
                if (idx >= 0) displayDate = raw.substring(idx + "last contact:".length()).trim();

                int nlIdx = displayDate.indexOf('\n');
                if (nlIdx >= 0) displayDate = displayDate.substring(0, nlIdx).trim();

                pdLastContactVal.setText(displayDate);
                if (pdViewLastContactBtn != null) pdViewLastContactBtn.setEnabled(true);
            }

            pdPositionIdVal.setText(safe(pid));
            pdTitleVal.setText(safe(p.getTitle()));
            pdLocationVal.setText(safe(p.getLocation()));
            pdCompanyVal.setText(c == null ? "-" : safe(c.getCompanyName()));
            pdStageVal.setText(selectedProcessApp.getStage() == null ? "-" : selectedProcessApp.getStage().name());
            pdSourceVal.setText(safe(selectedProcessApp.getSource()));
            pdAppliedAtVal.setText(selectedProcessApp.getDateApplied() == null ? "-" : selectedProcessApp.getDateApplied().format(DT_FMT));
            pdNotesTA.setText(safe(selectedProcessApp.getNotes()));

            pdStageCB.setSelectedItem(selectedProcessApp.getStage());
            pdSourceTF.setText("");

        } catch (Exception ex) {
            log("refreshProcessDetails failed: " + ex.getMessage());
        }
    }

    private void openAddOrEditContactForSelectedProcess() {
        if (!requireLogin()) return;
        if (selectedProcessApp == null || selectedProcessApp.getPosition() == null) return;

        String pid = selectedProcessApp.getPosition().getPositionID();
        Contact existing = tracker.getContactForPosition(activeUser, pid);

        JTextField nameTF  = new JTextField(existing == null ? "" : safe(existing.getContactName()), 22);
        JTextField roleTF  = new JTextField(existing == null ? "" : safe(existing.getRole()), 22);
        JTextField emailTF = new JTextField(existing == null ? "" : safe(existing.getContactEmail()), 22);
        JTextField phoneTF = new JTextField(existing == null ? "" : safe(existing.getContactPhone()), 22);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = gb();
        gc.gridy = 0;

        addRow(p, gc, "Name:", nameTF);
        addRow(p, gc, "Role:", roleTF);
        addRow(p, gc, "Email:", emailTF);
        addRow(p, gc, "Phone:", phoneTF);

        int res = JOptionPane.showConfirmDialog(this, p,
                (existing == null ? "Add Contact" : "Edit Contact"), JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            tracker.addOrEditContactForPosition(activeUser, pid,
                    nameTF.getText(), roleTF.getText(), emailTF.getText(), phoneTF.getText());
            refreshProcessDetails();
            refreshProcesses();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Contact update failed: " + ex.getMessage(), "Contact Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================
    // STATS
    // =========================
    private JPanel buildStatsCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(header("Statistics", true, true), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBorder(new EmptyBorder(20, 20, 20, 20));

        statsHeader.setFont(statsHeader.getFont().deriveFont(Font.BOLD, 18f));
        center.add(statsHeader, BorderLayout.NORTH);

        statsDetailsPane.setEditable(false);
        statsDetailsPane.setContentType("text/html");
        statsDetailsPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        statsStagesTA.setEditable(false);
        statsStagesTA.setLineWrap(true);
        statsStagesTA.setWrapStyleWord(true);
        //Fonts for section titles
        Font base = UIManager.getFont("Label.font");
        Font sectionTitleFont = (base == null) ? new Font("SansSerif", Font.BOLD, 16) : base.deriveFont(Font.BOLD, base.getSize2D() + 3f);
        //Left part : Full details
        JPanel left = new JPanel(new BorderLayout(8, 8));
        JLabel detailsTitle = new JLabel("Details");
        detailsTitle.setFont(sectionTitleFont);
        left.add(detailsTitle, BorderLayout.NORTH);
        left.add(new JScrollPane(statsDetailsPane), BorderLayout.CENTER);

        //Right part : Chart + Stage breakdown
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        //Chart title
        JLabel chartTitle = new JLabel("Applications per week", SwingConstants.CENTER);
        chartTitle.setFont(sectionTitleFont);

        JPanel chartTitleWrap = new JPanel(new BorderLayout());
        chartTitleWrap.setOpaque(false);
        chartTitleWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        chartTitleWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartTitleWrap.add(chartTitle, BorderLayout.CENTER);

        statsWeekChart.setPreferredSize(new Dimension(420, 220));
        statsWeekChart.setMinimumSize(new Dimension(420, 220));
        statsWeekChart.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel stagesTitle = new JLabel("Stage breakdown");
        stagesTitle.setFont(sectionTitleFont);
        stagesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane stagesScroll = new JScrollPane(statsStagesTA);
        stagesScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        stagesScroll.setPreferredSize(new Dimension(420, 220));

        right.add(chartTitleWrap);
        right.add(Box.createVerticalStrut(8));
        right.add(statsWeekChart);
        right.add(Box.createVerticalStrut(14));
        right.add(stagesTitle);
        right.add(Box.createVerticalStrut(8));
        right.add(stagesScroll);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.58);
        center.add(split, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private void refreshStats() {
        try {
            if (!requireLogin()) return;
            statsHeader.setText("Statistics for " + safe(activeUser.getFullName()));
            //Full details
            String full = tracker.buildUserStatistics(activeUser);
            int idxStage = full.indexOf("Stage breakdown:");
            int idxWeeks = full.indexOf("Applications per week");

            int cut = full.length();
            if (idxStage >= 0) cut = Math.min(cut, idxStage);
            if (idxWeeks >= 0) cut = Math.min(cut, idxWeeks);

            String leftPart = full.substring(0, cut).trim();
            statsDetailsPane.setText(toBoldDetailsHtml(leftPart));
            statsDetailsPane.setCaretPosition(0);
            //Chart (4 weeks)
            LinkedHashMap<String, Integer> perWeek = tracker.applicationsPerWeek(activeUser, 4);
            statsWeekChart.setData(perWeek);
            //Stage breakdown
            String stage = tracker.formatStageBreakdown(activeUser);
            stage = (stage == null) ? "" : stage.trim();
            statsStagesTA.setText(stage.isBlank() ? "(no data)" : stage);
            statsStagesTA.setCaretPosition(0);
            log("Statistics refreshed.");
        } catch (Exception ex) {
            log("Stats failed: " + ex.getMessage());
        }
    }

    private String toBoldDetailsHtml(String text) {
        if (text == null) text = "";
        String s = text.trim();
        if (s.isBlank()) return "<html><body>No statistics available.</body></html>";
        s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        //bold specific keys
        String[] keys = {"Applications:", "Events:", "Most applied company:", "Avg days to first stage change:"};
        for (String k : keys) {
            s = s.replace(k, "<b>" + k + "</b>");
        }
        //new lines
        s = s.replace("\n", "<br>");
        return "<html><body style='font-family:SansSerif; font-size:12px;'>" + s + "</body></html>";
    }

    // =========================
    // Small UI helpers
    // =========================
    private void makeBigField(JComponent c) {
        Font f = c.getFont();
        c.setFont(f.deriveFont(Font.PLAIN, 18f));
        Dimension pref = c.getPreferredSize();
        c.setPreferredSize(new Dimension(620, Math.max(pref.height, 48)));
        c.setMaximumSize(new Dimension(900, 48));
        c.setMinimumSize(new Dimension(200, 48));
    }

    private JPanel stackLabelAndField(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);

        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(l);
        p.add(Box.createVerticalStrut(8));
        p.add(field);
        return p;
    }

    private GridBagConstraints gb() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.weightx = 0;
        return gc;
    }

    private void addRow(JPanel panel, GridBagConstraints gc, String label, Component comp) {
        gc.gridwidth = 1;
        gc.gridx = 0;
        gc.weightx = 0;
        panel.add(new JLabel(label), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        panel.add(comp, gc);

        gc.gridy++;
    }

    private JPanel wrapSouth(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(c);
        return p;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    // ========================= for stats chart =========================
    private static class WeekBarChartPanel extends JPanel {
        private java.util.List<String> labels = java.util.List.of();
        private java.util.List<Integer> values = java.util.List.of();

        public void setData(LinkedHashMap<String, Integer> data) {
            if (data == null || data.isEmpty()) {
                labels = java.util.List.of();
                values = java.util.List.of();
            } else {
                labels = new ArrayList<>(data.keySet());
                values = new ArrayList<>(data.values());
            }
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (labels.isEmpty()) {
                g.drawString("No data", 10, 20);
                return;
            }
            int w = getWidth();
            int h = getHeight();

            int padding = 30;
            int baseY = h - padding;
            int leftX = padding;
            int rightX = w - 10;

            int max = 1;
            for (int v : values) {
                max = Math.max(max, v);
            }
            //axis
            g.drawLine(leftX, baseY, rightX, baseY);
            g.drawLine(leftX, baseY, leftX, 10);

            int n = values.size();
            int usableW = rightX - leftX - 10;
            int slot = Math.max(1, usableW / n);
            int barW = Math.max(6, (int)(slot * 0.65));

            for (int i = 0; i < n; i++) {
                int v = values.get(i);
                int barH = (int) ((baseY - 20) * (v / (double) max));
                int x = leftX + 5 + i * slot;
                int y = baseY - barH;
                g.fillRect(x, y, barW, barH);
                //value
                String vs = String.valueOf(v);
                FontMetrics fm = g.getFontMetrics();
                if (barH >= 18) {
                    int tx = x + (barW - fm.stringWidth(vs)) / 2;
                    int ty = y + (barH / 2) + (fm.getAscent() / 2) - 2;

                    Color old = g.getColor();
                    g.setColor(Color.WHITE);
                    g.drawString(vs, tx, ty);
                    g.setColor(old);
                } else {
                    g.drawString(vs, x, Math.max(12, y - 4));
                }
                //label
                String lab = labels.get(i);
                g.drawString(lab, x, baseY + 14);
            }
        }
    }

    // ======= Menubar =======
    private MenuBar buildAwtMenuBar() {
        MenuBar mb = new MenuBar();

        //File
        Menu mFile = new Menu("File");

        MenuItem miExit = new MenuItem("Exit");
        miExit.addActionListener(e -> exitApp());
        mFile.add(miExit);

        Menu mExportStats = new Menu("Export Statistics");

        MenuItem miExportTxt = new MenuItem("Export as TXT...");
        miExportTxt.addActionListener(e -> exportStatisticsAsTxt());

        MenuItem miExportHtml = new MenuItem("Export as HTML...");
        miExportHtml.addActionListener(e -> exportStatisticsAsHtml());

        mExportStats.add(miExportTxt);
        mExportStats.add(miExportHtml);

        mFile.add(mExportStats);
        mFile.addSeparator();

        MenuItem miLogout = new MenuItem("Logout");
        miLogout.addActionListener(e -> doLogout());
        mFile.add(miLogout);

        //Edit
        Menu mEdit = new Menu("Edit");

        MenuItem miClearSel = new MenuItem("Clear selections");
        miClearSel.addActionListener(e -> clearSelections());
        mEdit.add(miClearSel);

        MenuItem miResetForm = new MenuItem("Reset current form");
        miResetForm.addActionListener(e -> resetCurrentForm());
        mEdit.add(miResetForm);

        //View
        Menu mView = new Menu("View");

        CheckboxMenuItem miToggleLog = new CheckboxMenuItem("Show debug log");
        miToggleLog.addItemListener(e -> setLogVisible(miToggleLog.getState()));
        mView.add(miToggleLog);

        //Help
        Menu mHelp = new Menu("Help");

        MenuItem miAbout = new MenuItem("About");
        miAbout.addActionListener(e -> showAbout());
        mHelp.add(miAbout);

        MenuItem miInstr = new MenuItem("Short instructions");
        miInstr.addActionListener(e -> showInstructions());
        mHelp.add(miInstr);

        //Go to
        Menu mGo = new Menu("Go to");

        MenuItem goMenu = new MenuItem("Main Menu");
        goMenu.addActionListener(e -> {
            if (!requireLogin()) return;
            showCard(C_MENU, false);
        });
        mGo.add(goMenu);

        MenuItem goProfile = new MenuItem("Personal Area");
        goProfile.addActionListener(e -> {
            if (!requireLogin()) return;
            refreshProfileView();
            showCard(C_PROFILE);
        });
        mGo.add(goProfile);

        MenuItem goDocs = new MenuItem("My Documents");
        goDocs.addActionListener(e -> {
           if (!requireLogin()) return;
           refreshDocs();
           showCard(C_DOCS);
        });
        mGo.add(goDocs);

        MenuItem goAddApp = new MenuItem("Add Application");
        goAddApp.addActionListener(e -> {
            if (!requireLogin()) return;
            clearAddAppForm();
            showCard(C_ADD_APP);
        });
        mGo.add(goAddApp);

        MenuItem goEvents = new MenuItem("Events Calendar");
        goEvents.addActionListener(e -> {
            if (!requireLogin()) return;
            refreshCalendar();
            showCard(C_EVENTS);
        });
        mGo.add(goEvents);

        MenuItem goNotifs = new MenuItem("My Notifications");
        goNotifs.addActionListener(e -> {
            if (!requireLogin()) return;
            refreshNotifs();
            showCard(C_NOTIFS);
        });
        mGo.add(goNotifs);

        MenuItem goProc = new MenuItem("My Processes");
        goProc.addActionListener(e -> {
            if (!requireLogin()) return;
            refreshProcesses();
            showCard(C_PROCESSES);
        });
        mGo.add(goProc);

        MenuItem goStats = new MenuItem("Statistics");
        goStats.addActionListener(e -> {
            if (!requireLogin()) return;
            refreshStats();
            showCard(C_STATS);
        });
        mGo.add(goStats);

        mb.add(mFile);
        mb.add(mEdit);
        mb.add(mView);
        mb.add(mHelp);
        mb.add(mGo);

        return mb;
    }

    private void exitApp() {
        int ok = JOptionPane.showConfirmDialog(this, "Exit the application?", "Exit", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) System.exit(0);
    }

    private void setLogVisible(boolean show) {
        logShown = show;
        if (logWrap != null) {
            logWrap.setVisible(show);
            revalidate();
            repaint();
        }
    }

    private void clearSelections() {
        if (appsList != null) appsList.clearSelection();
        if (notifsList != null) notifsList.clearSelection();
        if (removeEventsList != null) removeEventsList.clearSelection();
        if (editEventsList != null) editEventsList.clearSelection();
        log("Selections cleared.");
    }

    private void resetCurrentForm() {
        if (currentCard == null) return;
        switch (currentCard) {
            case C_LOGIN -> {
                if (loginEmailTF != null) loginEmailTF.setText("");
                if (loginPassPF != null) loginPassPF.setText("");
            }
            case C_REGISTER -> {
                if (regFullNameTF != null) regFullNameTF.setText("");
                if (regEmailTF != null) regEmailTF.setText("");
                if (regPassPF != null) regPassPF.setText("");
                if (regPhoneTF != null) regPhoneTF.setText("");
                if (regFieldTF != null) regFieldTF.setText("");
            }
            case C_ADD_APP -> clearAddAppForm();
            case C_ADD_EVENT -> clearAddEventForm();
            case C_EDIT_EVENT -> {
                if (editEventTitleTF != null) editEventTitleTF.setText("");
                if (editEventDateTimeTF != null) editEventDateTimeTF.setText("");
                if (editEventDurationTF != null) editEventDurationTF.setText("");
                if (editEventNotesTA != null) editEventNotesTA.setText("");
                if (editEventTypeCB != null && editEventTypeCB.getItemCount() > 0) editEventTypeCB.setSelectedIndex(0);
            }
            case C_PROCESS_DETAILS -> {
                if (pdSourceTF != null) pdSourceTF.setText("");
                if (pdAddNoteTA != null) pdAddNoteTA.setText("");
                if (pdStageCB != null && selectedProcessApp != null) pdStageCB.setSelectedItem(selectedProcessApp.getStage());
            }
            default -> {
                JOptionPane.showMessageDialog(this, "No resettable form on this screen.", "Reset Form", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        log("Current form reset.");
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "JobTracker System\nFinal assignment in Software Engineering course" +
                        "\nThis project's goal is to help users track their job application processes.",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showInstructions() {
        String txt =
                "Quick guide:\n" +
                        "- Use Main Menu to navigate.\n" +
                        "- Documents: add/remove/open files & links.\n" +
                        "- Events: double-click a day to view events.\n" +
                        "- Processes: double-click a process to open details.\n";
        JTextArea ta = new JTextArea(txt, 10, 40);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Short instructions", JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildStatisticsTxtContent() throws Exception {
        refreshStats();
        String content = tracker.buildUserStatistics(activeUser);
        if (content == null) content = "";

        String stage = tracker.formatStageBreakdown(activeUser);
        stage = (stage == null) ? "" : stage.trim();

        if (!content.contains("Stage breakdown:")) {
            content += "\n\n====================\nStage breakdown:\n";
            content += stage.isBlank() ? "(no data)\n" : (stage + "\n");
        }
        content = content.trim();
        if (content.isEmpty()) return null;
        return content + "\n";
    }

    private void saveTxtTo(java.nio.file.Path outPath, String content) throws Exception {
        java.nio.file.Files.createDirectories(outPath.getParent());
        java.nio.file.Files.writeString(outPath, content, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void exportStatisticsAsTxt() {
        if (!requireLogin()) return;

        final String content;
        try {
            content = buildStatisticsTxtContent();
            if (content == null) {
                JOptionPane.showMessageDialog(this, "No statistics to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to build statistics: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FileDialog fd = new FileDialog(this, "Save statistics as TXT", FileDialog.SAVE);
        fd.setFile("statistics.txt");
        fd.setVisible(true);

        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) return;

        java.nio.file.Path outPath = new java.io.File(dir, file).toPath();
        try {
            saveTxtTo(outPath, content);
            log("Statistics exported to TXT: " + outPath.toAbsolutePath());
            JOptionPane.showMessageDialog(this, "Exported:\n" + outPath.toAbsolutePath(), "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportStatisticsTxtTo(java.nio.file.Path outPath) {
        if (!requireLogin()) return;

        final String content;
        try {
            content = buildStatisticsTxtContent();
            if (content == null) {
                JOptionPane.showMessageDialog(this, "No statistics to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to build statistics: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            saveTxtTo(outPath, content);
            log("Statistics exported to TXT: " + outPath.toAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildStatisticsHtmlContent() throws Exception {
        refreshStats();

        String full = tracker.buildUserStatistics(activeUser);
        if (full == null) full = "";
        full = full.trim();
        if (full.isEmpty()) return null;

        String stage = "";
        try {
            stage = tracker.formatStageBreakdown(activeUser);
            stage = (stage == null) ? "" : stage.trim();
        } catch (Exception ignored) {}

        String stageHtml = stage
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");

        String extraStageSection = "";
        if (!full.contains("Stage breakdown:")) {
            extraStageSection =
                    "<hr>" +
                            "<h3>Stage breakdown</h3>" +
                            "<div style='font-family:SansSerif; font-size:12px;'>" +
                            (stage.isBlank() ? "(no data)" : stageHtml) +
                            "</div>";
        }

        String body = toBoldDetailsHtml(full).replaceFirst("(?i)<html>\\s*<body", "<html><body");

        return "<!DOCTYPE html>\n" +
                "<html>\n<head>\n<meta charset=\"UTF-8\">\n" +
                "<title>JobTracker - Statistics</title>\n</head>\n<body>\n" +
                "<h2>Statistics for " + safe(activeUser.getFullName()) + "</h2>\n" +
                body
                        .replaceFirst("(?i)^\\s*<html>\\s*<body[^>]*>", "")
                        .replaceFirst("(?i)</body>\\s*</html>\\s*$", "") +
                extraStageSection +
                "\n</body>\n</html>\n";
    }

    private void saveHtmlTo(java.nio.file.Path outPath, String html) throws Exception {
        java.nio.file.Files.createDirectories(outPath.getParent());
        java.nio.file.Files.writeString(outPath, html, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void exportStatisticsAsHtml() {
        if (!requireLogin()) return;
        final String html;
        try {
            html = buildStatisticsHtmlContent();
            if (html == null) {
                JOptionPane.showMessageDialog(this, "No statistics to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to build statistics: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
            return;
        }
        FileDialog fd = new FileDialog(this, "Save statistics as HTML", FileDialog.SAVE);
        fd.setFile("statistics.html");
        fd.setVisible(true);

        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) return;

        java.nio.file.Path outPath = new java.io.File(dir, file).toPath();

        try {
            saveHtmlTo(outPath, html);
            log("Statistics exported to HTML: " + outPath.toAbsolutePath());
            JOptionPane.showMessageDialog(this, "Exported:\n" + outPath.toAbsolutePath(), "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportStatisticsHtmlTo(java.nio.file.Path outPath) {
        if (!requireLogin()) return;
        final String html;
        try {
            html = buildStatisticsHtmlContent();
            if (html == null) {
                JOptionPane.showMessageDialog(this, "No statistics to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to build statistics: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            saveHtmlTo(outPath, html);
            log("Statistics exported to HTML: " + outPath.toAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
        }
    }
}