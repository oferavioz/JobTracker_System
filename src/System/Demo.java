package System;

import Model.ApplyFor;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

public class Demo {

    // =========================
    // Tweakable speed controls
    // =========================
    public static int STEP_DELAY_MS = 1100;     // delay between main steps
    public static int TYPE_DELAY_MS = 35;       // per-character delay for typing
    public static int TYPE_DELAY_AREA_MS = 18;  // per-character delay for text areas
    public static int DIALOG_TIMEOUT_MS = 4000; // wait for dialogs
    public static int MESSAGE_SHOW_MS = 4000;   // how long to keep info dialogs visible

    // Default credentials that exist in your Main initialization (demo user)
    private static final String DEFAULT_EMAIL = "oferavioz@gmail.com";
    private static final String DEFAULT_PASS  = "123456789";

    // If Part1 registered a fresh user, we keep creds here
    private static volatile String lastDemoEmail = null;
    private static volatile String lastDemoPass  = null;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final java.util.concurrent.atomic.AtomicLong AUTO_OK_SEQ = new java.util.concurrent.atomic.AtomicLong(0);

    // =========================
    // PART 1: Open system + Register + Login
    // =========================
    public static void part1(JobTrackerGUI gui) {
        new Thread(() -> {
            pause(600);

            showInfo(
                    "Demo - Part 1",
                    "We will now start Part 1 of the demo.\n\n" +
                            "In this part we will demonstrate how to register to the system,\n" +
                            "and then log in with the newly created user.",
                    6500
            );
            pause(7100);

            // Open Register screen
            showCard(gui, getStaticCard("C_REGISTER"));
            pause();

            // Unique email for repeated runs
            String email = "demo_user_" + System.currentTimeMillis() + "@demo.com";
            String pass  = "123456789";
            lastDemoEmail = email;
            lastDemoPass  = pass;

            // Auto close the "User created" message dialog
            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS);

            // Type into register fields + click Create
            typeField(gui, "regFullNameTF", "Demo User", TYPE_DELAY_MS);
            typeField(gui, "regEmailTF", email, TYPE_DELAY_MS);
            typePass(gui,  "regPassPF", pass, TYPE_DELAY_MS);
            typeField(gui, "regPhoneTF", "054-1111111", TYPE_DELAY_MS);
            typeField(gui, "regFieldTF", "Hi-Tech", TYPE_DELAY_MS);

            pause(250);
            runOnEdt(() -> call(gui, "doRegister"));

            pause(1400);

            // Login with the new user
            ensureLogin(gui, email, pass);

            pause(900);
            showInfo(
                    "Demo - Part 1",
                    "Part 1 completed.\n" +
                            "We registered a new user and then logged in successfully.",
                    5200
            );
            pause(5700);

        }, "Demo-Part1").start();
    }

    // =========================
    // PART 2: Menu + Personal Area + Edit Profile + Change Password
    // =========================
    public static void part2(JobTrackerGUI gui) {
        new Thread(() -> {
            pause();
            ensureLogin(gui, pickEmail(), pickPass());
            pause(600);

            showInfo(
                    "Demo - Part 2",
                    "We will now start Part 2 of the demo.\n\n" +
                            "In this part we will demonstrate:\n" +
                            "1) Entering the Personal Area\n" +
                            "2) Editing profile details\n" +
                            "3) Changing the password (invalid attempt, then a valid one).", 8000);
            pause(8600);

            // Menu -> Personal Area
            showCard(gui, getStaticCard("C_MENU"));
            pause(500);

            runOnEdt(() -> {
                call(gui, "refreshProfileView");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_PROFILE"), true});
            });
            pause(1000);

            showInfo(
                    "Demo - Edit Profile",
                    "This is the user's personal area. \nNext: we will edit the profile details.\n" +
                            "We will update the full name, phone number, and field of search.",
                    5200);
            pause(5700);

            // Edit Profile dialog: type and OK
            autoTypeConfirmDialogByLabelAndOk(Map.of(
                    "Full name :", "Demo User Edited",
                    "Phone :", "054-2222222",
                    "Field of search :", "Software"), DIALOG_TIMEOUT_MS, TYPE_DELAY_MS);

            runOnEdt(() -> call(gui, "openEditProfileDialog"));
            pause(1500);

            // =========================
            // Change Password - attempt #1 (invalid: same password)
            // =========================
            showInfo(
                    "Demo - Change Password",
                    "Next: we will change the password.\n" +
                            "Important: the new password must be DIFFERENT from the old password.\n" +
                            "First, we will intentionally try an invalid change (same password) to show the validation message.",
                    8000);
            pause(8600);

            autoTypeConfirmDialogByLabelAndOk(Map.of(
                    "Old password :", pickPass(),
                    "New password :", pickPass(),
                    "Repeat new password :", pickPass()), DIALOG_TIMEOUT_MS, TYPE_DELAY_MS);

            runOnEdtLater(() -> call(gui, "openChangePasswordDialog"));

            pause(2200);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 2400);
            pause(4200);

            showInfo(
                    "Demo - Change Password",
                    "We received a warning because we entered the SAME password as the current one.\n" +
                            "Now we will enter a completely new password and confirm it correctly.",
                    6500);
            pause(7100);

            // =========================
            // Change Password - attempt #2 (valid)
            // =========================
            String newPass = "987654321";

            autoTypeConfirmDialogByLabelAndOk(Map.of(
                    "Old password :", pickPass(),
                    "New password :", newPass,
                    "Repeat new password :", newPass), DIALOG_TIMEOUT_MS, TYPE_DELAY_MS);

            runOnEdtLater(() -> call(gui, "openChangePasswordDialog"));

            pause(2800);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1200);
            pause(2800);

            // Update stored demo password for next parts
            lastDemoPass = newPass;

            showInfo(
                    "Demo - Part 2",
                    "Part 2 completed.\n\n" +
                            "We entered the Personal Area, edited the profile details,\n" +
                            "and changed the password successfully.",
                    6500);
            pause(7100);

        }, "Demo-Part2").start();
    }

    // =========================
    // PART 3: Documents (Add File + Add URL + Details/Edit + Filters + Remove)
    // =========================
    public static void part3(JobTrackerGUI gui) {
        new Thread(() -> {
            pause();
            ensureLogin(gui, pickEmail(), pickPass());
            pause(600);

            showInfo(
                    "Demo - Part 3",
                    "We will now start Part 3 of the demo: Documents.\n" +
                            "In this part we will demonstrate:\n" +
                            "1) Entering the Documents screen\n" +
                            "2) Adding a document from a FILE\n" +
                            "3) Adding a document from a URL\n" +
                            "4) Viewing document details and editing the document name\n" +
                            "5) Using filters (Links / Files / Primary only)\n" +
                            "6) Removing a document", 9500);
            pause(10100);

            showInfo(
                    "Demo - Documents",
                    "Now we will open the Documents screen and refresh the documents list.", 4200);
            pause(4700);

            // Go to docs
            runOnEdt(() -> {
                call(gui, "refreshDocs");
                call(gui, "showCard", new Class[]{String.class, boolean.class}, new Object[]{getStaticCard("C_DOCS"), true});
            });
            pause(1300);

            String demoFile = createDemoFile();

            showInfo(
                    "Demo - Documents",
                    "Next: we will add a new document using a FILE.\n" +
                            "We will fill the dialog fields and confirm.", 5200);
            pause(5700);

            // Add Document (File) with typing
            autoTypeAddDocumentDialogAndOk(
                    "Demo File", "File", demoFile, "Demo file note", true,
                    DIALOG_TIMEOUT_MS, TYPE_DELAY_MS
            );
            runOnEdtLater(() -> call(gui, "openAddDocumentDialog")); // modal JOptionPane
            pause(1400);
            waitForNoDialogs(3500);

            // Add Document (URL) with typing
            showInfo(
                    "Demo - Documents",
                    "Now we will add another document using a URL.\n" +
                            "Again, we will fill the dialog and confirm.", 5200);
            pause(5700);

            autoTypeAddDocumentDialogAndOk(
                    "Demo URL", "URL", "https://example.com", "Demo url note", false,
                    DIALOG_TIMEOUT_MS, TYPE_DELAY_MS);
            runOnEdtLater(() -> call(gui, "openAddDocumentDialog"));
            pause(1400);
            waitForNoDialogs(3500);

            showInfo("Demo - Documents",
                    "We added two documents.\nNow we will refresh the list and select a document to view its details.", 5200);
            pause(5700);

            // Ensure docs view refreshed and a selection exists
            runOnEdt(() -> {
                call(gui, "refreshAwtDocsView");
                java.awt.List list = (java.awt.List) get(gui, "awtDocsList");
                if (list != null && list.getItemCount() > 0) list.select(0);
            });
            pause(600);

            showInfo("Demo - Documents",
                    "Next: we will open the selected document details.\n" +
                            "Inside the details window, we will click Edit and change the document name.", 6200);
            pause(6700);

            // ---- Open Details (IMPORTANT: async, because details dialog is modal JDialog) ----
            runOnEdtLater(() -> call(gui, "detailsSelectedAwtDoc"));

            // Wait specifically for the Details dialog (not for info dialogs)
            JDialog detailsDlg = waitForDialog(DIALOG_TIMEOUT_MS, d ->
                    d.isShowing()
                            && ("Document Details".equalsIgnoreCase(safeTitle(d)) ||
                            (findButtonByText(d, "Edit") != null && findButtonByText(d, "Close") != null))
            );
            if (detailsDlg == null) return;
            pause(1000);

            showInfo("Demo - Documents",
                    "Now we will edit the document name.\n" +
                            "We will change it to: \"LinkedIn Profile 1122\" and confirm.", 5200);
            pause(5700);

            // Click "Edit" in Details
            runOnEdtLater(() -> {
                JButton editBtn = findButtonByText(detailsDlg, "Edit");
                if (editBtn != null) editBtn.doClick();
            });

            // Wait for the Edit Document confirm dialog (JOptionPane)
            JDialog editDlg = waitForDialog(DIALOG_TIMEOUT_MS, d ->
                    d.isShowing() && ("Edit Document".equalsIgnoreCase(safeTitle(d)) || hasLabel(d, "Document Name:"))
            );
            if (editDlg == null) {
                // if title differs, fallback: any dialog with 2 textfields + an OK button
                editDlg = waitForDialog(DIALOG_TIMEOUT_MS, d ->
                        d.isShowing()
                                && findAll(d, JTextField.class).size() >= 2
                                && findAnyButton(d, "OK", "Ok", "Save", "Update") != null);
            }
            if (editDlg == null) return;

            // Auto-type ONLY the document name -> "LinkedIn Profile 1122"
            List<JTextField> tfs = findAll(editDlg, JTextField.class);
            if (!tfs.isEmpty()) {
                typeComponentBlocking(tfs.get(0), "LinkedIn Profile 1122", TYPE_DELAY_MS);
            }
            pause(150);
            JDialog finalEditDlg = editDlg;
            runOnEdtLater(() -> pressOk(finalEditDlg));

            // Close Details dialog
            pause(700);
            runOnEdtLater(() -> {
                JButton closeBtn = findButtonByText(detailsDlg, "Close");
                if (closeBtn != null) closeBtn.doClick();
                else detailsDlg.dispose();
            });

            waitForNoDialogs(3500);
            pause(400);

            showInfo("Demo - Documents",
                    "Next: we will demonstrate the document filters.\n" +
                            "We will switch between Links-only, Files-only, and Primary-only.", 6500);
            pause(7000);

            // === Filters: show once Links, once Files, once Primary only ===
            showInfo("Demo - Documents", "Now showing: LINKS only", 2200);
            pause(2300);
            setDocsAwtFilter(gui, "Links", false);

            showInfo("Demo - Documents", "Now showing: FILES only", 2200);
            pause(2300);
            setDocsAwtFilter(gui, "Files", false);

            showInfo("Demo - Documents", "Now showing: PRIMARY only", 2200);
            pause(2300);
            setDocsAwtFilter(gui, "All", true);

            // Back to normal view for remove
            showInfo("Demo - Documents",
                    "Now we will return to the full list and remove the document we edited.\n" +
                            "We will select \"LinkedIn Profile 1122\" and confirm the removal dialog.", 6500);
            pause(7000);

            setDocsAwtFilter(gui, "All", false);
            selectAwtDocByContains(gui, "LinkedIn Profile 1122");

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 600);
            runOnEdtLater(() -> call(gui, "removeSelectedAwtDoc"));
            pause(1200);
            waitForNoDialogs(3500);

            showInfo(
                    "Demo - Part 3",
                    "Part 3 completed.\n\n" +
                            "We added documents (File + URL), edited a document name,\n" +
                            "used filters, and removed a document successfully.", 7000);
            pause(7600);


        }, "Demo-Part3").start();
    }

    // =========================
    // PART 4: Add Application + Processes + Details actions
    // =========================

    public static void part4(JobTrackerGUI gui) {
        new Thread(() -> {
            pause();
            ensureLogin(gui, pickEmail(), pickPass());
            pause(100);

            // Go to menu (safe)
            runOnEdtLater(() -> call(gui, "showCard", new Class[]{String.class, boolean.class},
                    new Object[]{getStaticCard("C_MENU"), false}));

            // If the GUI pops "Please login first", handle it and login again
            JDialog needLogin = waitForDialog(1600, d -> {
                if (!d.isShowing()) return false;
                String title = safeTitle(d).toLowerCase(java.util.Locale.ROOT);
                if (title.contains("login")) return true;
                for (JLabel l : findAll(d, JLabel.class)) {
                    String t = (l.getText() == null) ? "" : l.getText().toLowerCase(java.util.Locale.ROOT);
                    if (t.contains("login") || t.contains("log in")) return true;
                }
                return false;
            });
            if (needLogin != null) {
                runOnEdtLater(() -> pressOk(needLogin));
                pause(350);
                ensureLogin(gui, pickEmail(), pickPass());
                pause(2000);
            }

            // -----------------------------------------
            // Step 1: Add Application form (with scrolling)
            // -----------------------------------------
            showInfo("Demo – Processes",
                    "Part 4 – Processes\n\n" +
                            "What we will do:\n" +
                            "1) Create a new application and fill the entire form\n" +
                            "2) Open 'My Processes' and see it in the list\n" +
                            "3) Open details and edit: Status / Stage / Source / Note\n" +
                            "4) Demonstrate communication with a contact\n" +
                            "5) Withdraw, then Remove to see the list update",
                    8000);
            pause(8500);

            showCard(gui, getStaticCard("C_ADD_APP"));
            pause(1400);

            showInfo("Demo - New Application",
                    "We are now in the 'Add New Application' form.\n" +
                            "We will fill all fields step-by-step, scrolling down as needed.",
                    5200);
            pause(5700);

            String pid = "PDEMO_" + (System.currentTimeMillis() % 100000);

            // Fill top area
            scrollToField(gui, "appPositionIdTF"); pause(500);
            typeField(gui, "appPositionIdTF", pid, TYPE_DELAY_MS);
            typeField(gui, "appTitleTF", "Demo SWE Intern", TYPE_DELAY_MS);
            runOnEdt(() -> selectComboIndex(gui, "appFieldCB", 0));
            typeField(gui, "appPublishDateTF", "2025-12-01", TYPE_DELAY_MS);

            // Scroll a bit so they see progress
            smoothScrollBy(gui, +260, 8, 60);

            // Fill company area
            scrollToField(gui, "appCompanyNameTF"); pause(400);
            typeField(gui, "appCompanyNameTF", "DemoCorp", TYPE_DELAY_MS);
            typeField(gui, "appIndustryTF", "Hi-Tech", TYPE_DELAY_MS);
            typeField(gui, "appWebsiteTF", "https://example.com", TYPE_DELAY_MS);
            typeField(gui, "appLocationTF", "Tel Aviv", TYPE_DELAY_MS);
            runOnEdt(() -> selectComboIndex(gui, "appEmploymentCB", 1));

            smoothScrollBy(gui, +320, 10, 55);

            // Fill description/source/notes
            scrollToField(gui, "appDescriptionTA"); pause(350);
            typeArea(gui, "appDescriptionTA", "Demo description for the position.", TYPE_DELAY_AREA_MS);
            typeField(gui, "appSourceTF", "LinkedIn", TYPE_DELAY_MS);
            typeArea(gui, "appNotesTA", "Demo notes.", TYPE_DELAY_AREA_MS);

            smoothScrollBy(gui, +420, 12, 50);

            // Optional contact (bottom)
            scrollToField(gui, "cNameTF"); pause(350);
            typeField(gui, "cNameTF", "Dana Levi", TYPE_DELAY_MS);
            typeField(gui, "cRoleTF", "HR Manager", TYPE_DELAY_MS);
            typeField(gui, "cEmailTF", "dana@demo.com", TYPE_DELAY_MS);
            typeField(gui, "cPhoneTF", "054-1234567", TYPE_DELAY_MS);

            pause(600);

            // Submit
            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1200);
            runOnEdtLater(() -> call(gui, "submitApplication"));
            pause(2600);
            waitForNoDialogs(3000);

            // -----------------------------------------
            // Step 2: Back to menu + explanation message
            // -----------------------------------------
            showCard(gui, getStaticCard("C_MENU"), false);
            pause(900);

            showInfo("Demo - Processes",
                    "After adding the new process, we will now open 'My Processes'\n" +
                            "to display all existing processes.",
                    4500);
            pause(4900);

            // -----------------------------------------
            // Step 3: My Processes + show message about new process
            // -----------------------------------------
            runOnEdtLater(() -> {
                call(gui, "refreshProcesses");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_PROCESSES"), true});
            });
            pause(1700);

            showInfo("Demo - My Processes",
                    "You can now see the new process in the list.\n" +
                            "Next, we will open its details and edit it.",
                    4900);
            pause(5300);

            ApplyFor app = getFirstApplicationFromList(gui);
            if (app == null) return;

            runOnEdtLater(() -> call(gui, "openProcessDetails", new Class[]{ApplyFor.class}, new Object[]{app}));
            pause(1700);

            showInfo("Demo - process details",
                    "This is the process details window for the newly created process.\n" +
                            "Here, we can view all information and perform various actions.\n"+
                            "For instance - we can see the company details ->",
                    6500);
            pause(7000);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 5000);
            runOnEdtLater(() -> {
                JButton b = findButtonInActiveFrame("View company's details");
                if (b == null) b = findButtonInActiveFrame("View Company Details");
                if (b == null) b = findButtonInActiveFrame("Company Info");
                if (b != null) b.doClick();
            });
            pause(2200);
            waitForNoDialogs(6000);

            showInfo("Demo – Company Details Shown",
                    "Now that we reviewed the company details,\n" +
                            "We can also view the last logged communication with the contact\n" +
                            "Because this is a new process, there is no communication yet.\n" + "(we will add some later)",
                    6000);
            pause(6400);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 6000);
            runOnEdtLater(() -> clickButtonField(gui, "pdViewLastContactBtn"));
            pause(2200);
            waitForNoDialogs(7000);

            // -----------------------------------------
            // Step 4: Edit details (status + stage + source + note)
            // -----------------------------------------
            showInfo("Demo - Process Details",
                    "Now we will edit the process details:\n" +
                            "toggle status, change stage, update source, and add a note.",
                    5000);
            pause(5400);

            runOnEdtLater(() -> clickButtonField(gui, "pdToggleStatusBtn"));
            pause(1200);

            runOnEdt(() -> selectComboIndex(gui, "pdStageCB", 1));
            pause(450);

            typeField(gui, "pdSourceTF", "Company Website", TYPE_DELAY_MS);
            typeArea(gui, "pdAddNoteTA", "Demo appended note from Demo.part4", TYPE_DELAY_AREA_MS);

            pause(600);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1100);
            runOnEdtLater(() -> {
                JButton save = findButtonInActiveFrame("Save Changes");
                if (save != null) save.doClick();
            });
            pause(2000);
            waitForNoDialogs(500);

            ensureStillLoggedIn(gui);

            // Back to processes list and explain colors + changes
            runOnEdtLater(() -> {
                call(gui, "refreshProcesses");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_PROCESSES"), false});
            });
            pause(100);

            showInfo("Demo - My Processes",
                    "Changes were saved (stage / source / note / status).\n\n" +
                            "Color explanation in the list:\n" +
                            "• Green = active processes\n" +
                            "• Yellow = overdue processes\n" +
                            "• Red = final stage (Withdrawn / Rejected / Offer)\n" +
                            "• Grey = not active processes" +
                            "\n\nProcess turned grey because we toggled status to Not Active.",
                    8500);
            pause(9300);

            // Re-open details
            ApplyFor appAgain = getFirstApplicationFromList(gui);
            if (appAgain == null) return;

            runOnEdtLater(() -> call(gui, "openProcessDetails", new Class[]{ApplyFor.class}, new Object[]{appAgain}));
            pause(1700);

            // -----------------------------------------
            // Step 5: Add communication with contact + show updated info
            // -----------------------------------------
            showInfo("Demo - Communication",
                    "Now we will demonstrate adding communication with the contact.\n" +
                            "After logging it, we will view the updated communication details.",
                    6500);
            pause(7000);

            // Log Communication dialog (typing + OK)
            autoTypeLogCommunicationDialogAndOk(
                    "Email",
                    "Demo follow-up",
                    LocalDateTime.now().format(DT_FMT),
                    DIALOG_TIMEOUT_MS,
                    TYPE_DELAY_MS);

            runOnEdtLater(() -> clickButtonField(gui, "pdLogContactBtn"));
            pause(2000);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 900);
            pause(1600);

            // View Last Contact (shows updated communication)
            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 4000);
            runOnEdtLater(() -> clickButtonField(gui, "pdViewLastContactBtn"));
            pause(6000);

            // -----------------------------------------
            // Step 6: Withdraw demo + return to processes and see red
            // -----------------------------------------
            showInfo("Demo - Withdraw",
                    "Now we will demonstrate what 'Withdraw' means:\n" +
                            "withdrawing the application from the process.",
                    5000);
            pause(5600);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1100);
            runOnEdtLater(() -> {
                JButton w = findButtonInActiveFrame("Withdraw");
                if (w != null) w.doClick();
            });
            pause(2200);
            waitForNoDialogs(2500);

            ensureStillLoggedIn(gui);

            // Back to processes to see color change
            runOnEdtLater(() -> {
                call(gui, "refreshProcesses");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_PROCESSES"), false});
            });
            pause(1600);

            showInfo("Demo - My Processes",
                    "Back in the list, you should now see the process changed accordingly.\n" +
                            "It's color turned red (final stage)", 5200);
            pause(5700);

            // -----------------------------------------
            // Step 7: Enter again and remove the process
            // -----------------------------------------
            ApplyFor appToRemove = getFirstApplicationFromList(gui);
            if (appToRemove == null) return;

            runOnEdtLater(() -> call(gui, "openProcessDetails", new Class[]{ApplyFor.class}, new Object[]{appToRemove}));
            pause(1700);

            showInfo("Demo - Remove Process",
                    "Now we will demonstrate removing the process.\n" +
                            "We will click 'Remove' and confirm the dialog.",
                    5600);
            pause(6100);

            // Click remove/delete button (robust: tries multiple common captions)
            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1000);
            runOnEdtLater(() -> {
                JButton delBtn = findButtonInActiveFrame("Remove Process");
                if (delBtn != null) delBtn.doClick();
            });
            pause(2400);
            waitForNoDialogs(3500);

            ensureStillLoggedIn(gui);

            // Back to processes after removal
            runOnEdtLater(() -> {
                call(gui, "refreshProcesses");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_PROCESSES"), false});
            });
            pause(1000);

            showInfo("Demo - process Removed",
                    "The process was removed successfully.\n" ,4200);
            pause(4600);

            showInfo("Demo - Done",
                    "Part 4 completed\n" ,4200);
            pause(4600);

        }, "Demo-Part4").start();
    }

    // =========================
    // PART 5: Events
    // =========================
    public static void part5(JobTrackerGUI gui) {
        new Thread(() -> {

            // --- tuned timings (less dialogs, more watching time) ---
            final int INFO_MS = 5200; // normal info bubble time
            final int WATCH_MS = 6000; // time to watch calendar changes
            final int EDIT_WATCH_MS = 4200; // time to watch edit form before saving
            final int ERROR_WATCH_MS = 7000; // keep error dialog long

            pause(300);

            // Login first (message must be AFTER login)
            ensureLogin(gui, pickEmail(), pickPass());
            pause(600);

            showInfo("Demo – Events (Part 5)",
                    "Part 5 - Events\n\n" +
                            "What we will do:\n" +
                    "In this part we will:\n" +
                            "1) Open the calendar\n" +
                            "2) Add a new event\n" +
                            "3) Edit the event\n" +
                            "4) Demonstrate an overlap conflict\n" +
                            "5) Remove the event",
                    INFO_MS);
            pause(INFO_MS + 500);

            // Enter calendar
            runOnEdt(() -> {
                call(gui, "resetCalendarToNow");
                call(gui, "refreshCalendar");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_EVENTS"), true});
            });
            pause(1400);

            // Show message that we are in calendar
            showInfo("Demo – Calendar View",
                    "This is your calendar.\n" +
                            "Tip: You can double-click any day to view its events.",
                    INFO_MS);
            pause(INFO_MS + 600);

            // -------------------------
            // Add event (+2 days)
            // -------------------------
            showInfo("Demo – Add Event",
                    "Now we will add a new event scheduled for +2 days from today.", INFO_MS);
            pause(INFO_MS + 600);

            LocalDateTime createdDT = LocalDateTime.now().plusDays(2).withHour(10).withMinute(30);
            String createdTitle = "Demo Event";
            int createdDur = 30;
            String createdWhen = createdDT.format(DT_FMT);

            showCard(gui, getStaticCard("C_ADD_EVENT"));
            pause(1000);

            runOnEdt(() -> selectComboByValue(gui, "eventTypeCB", "Meeting"));
            typeField(gui, "eventTitleTF", createdTitle, TYPE_DELAY_MS);
            typeField(gui, "eventDateTimeTF", createdWhen, TYPE_DELAY_MS);
            typeField(gui, "eventDurationTF", String.valueOf(createdDur), TYPE_DELAY_MS);
            typeArea(gui, "eventNotesTA", "Created by Demo.part5", TYPE_DELAY_AREA_MS);

            pause(450);
            runOnEdt(() -> call(gui, "submitEvent"));

            // Wait for return to calendar
            pause(2400);

            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, createdDT.toLocalDate(), 1);
            pause(900);

            // IMPORTANT: give real time to look after adding
            showInfo("Demo – Event Added",
                    "The event was added successfully.\n" +
                            "Take a moment to see it appear in the calendar.",
                    WATCH_MS - 2000);
            pause(10000);
            waitForNoDialogs(2000);

            // -------------------------
            // View tomorrow (existing 2 events)
            // We show the events via internal dialog
            // -------------------------
            showInfo("Demo – View a Busy Day",
                    "Now we will open tomorrow.\n" +
                            "Tomorrow already has events, so you can see how the daily list looks.",
                    INFO_MS);
            pause(INFO_MS + 600);

            // Build list of tomorrow events (using gui.tracker + gui.activeUser via reflection)
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            java.util.List<Model.Event> tomorrowEvents = new ArrayList<>();
            try {
                Object tracker = get(gui, "tracker");
                Object user = get(gui, "activeUser");
                Integer calMonth = (Integer) get(gui, "calMonth");
                Integer calYear  = (Integer) get(gui, "calYear");

                if (tracker != null && user != null && calMonth != null && calYear != null) {
                    Method m = tracker.getClass().getDeclaredMethod("listEvents", user.getClass(), int.class, int.class);
                    m.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Vector<Model.Event> monthEvents = (Vector<Model.Event>) m.invoke(tracker, user, calMonth, calYear);
                    for (Model.Event ev : monthEvents) {
                        if (ev != null && ev.getDateTime() != null && ev.getDateTime().toLocalDate().equals(tomorrow)) {
                            tomorrowEvents.add(ev);
                        }
                    }
                }
            } catch (Exception ignored) {}

            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, tomorrow, 1);
            pause(900);

            // Keep Day Events dialog open long enough
            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 8000);
            runOnEdtLater(() -> call(gui, "openDayEventsDialog",
                    new Class[]{LocalDate.class, java.util.List.class},
                    new Object[]{tomorrow, tomorrowEvents}));
            pause(9000);
            waitForNoDialogs(5000);

            // -------------------------
            // Edit our created event
            // -------------------------
            showInfo("Demo – Edit Event",
                    "Now we will edit the event we created:\n" +
                            "• Change title\n" +
                            "• Move it to another day\n" +
                            "• Update duration and notes",
                    INFO_MS);
            pause(INFO_MS + 600);

            // Set selectedDay = createdDay so Edit list filters correctly
            LocalDate createdDay = createdDT.toLocalDate();
            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, createdDay, 1);
            pause(900);

            runOnEdt(() -> {
                call(gui, "refreshEditEventsList");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_EDIT_EVENT"), true});
            });
            pause(1600);

            // Select our event in edit list (by title contains)
            runOnEdt(() -> {
                try {
                    JList<?> list = (JList<?>) get(gui, "editEventsList");
                    if (list == null) return;
                    ListModel<?> model = list.getModel();
                    int size = model.getSize();
                    for (int i = 0; i < size; i++) {
                        Object it = model.getElementAt(i);
                        if (it instanceof Model.Event ev) {
                            String t = ev.getTitle();
                            if (t != null && t.contains(createdTitle)) {
                                list.setSelectedIndex(i);
                                list.ensureIndexIsVisible(i);
                                return;
                            }
                        }
                    }
                    if (size > 0) list.setSelectedIndex(0);
                } catch (Exception ignored) {}
            });
            pause(700);

            // Make changes (NO explanation before)
            LocalDateTime editedDT = createdDT.plusDays(1);
            String editedWhen = editedDT.format(DT_FMT);
            int editedDur = createdDur + 20;
            String editedTitle = createdTitle + " (Edited)";

            typeField(gui, "editEventTitleTF", editedTitle, TYPE_DELAY_MS);
            typeField(gui, "editEventDateTimeTF", editedWhen, TYPE_DELAY_MS);
            typeField(gui, "editEventDurationTF", String.valueOf(editedDur), TYPE_DELAY_MS);
            typeArea(gui, "editEventNotesTA", "Updated in Demo.part5 (+1 day, +20 minutes)", TYPE_DELAY_AREA_MS);

            // Let the user SEE the edit screen before saving
            pause(EDIT_WATCH_MS-1000);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 2200);
            runOnEdt(() -> {
                try {
                    Field f = JobTrackerGUI.class.getDeclaredField("selectedDay");
                    f.setAccessible(true);
                    f.set(gui, editedDT.toLocalDate());
                } catch (Exception ignored) {}

                call(gui, "submitEditEvent");
            });
            pause(2400);

            // Explain changes AFTER saving
            showInfo("Demo – Changes Saved",
                    "Event updated successfully.\n" +
                            "New date/time: " + editedWhen + "\n" +
                            "New duration: " + editedDur + " minutes", INFO_MS);
            pause(INFO_MS + 600);

            // Show the moved day events to demonstrate changes
            LocalDate movedDay = editedDT.toLocalDate();
            java.util.List<Model.Event> movedEvents = new ArrayList<>();
            try {
                Object tracker = get(gui, "tracker");
                Object user = get(gui, "activeUser");
                Integer calMonth = (Integer) get(gui, "calMonth");
                Integer calYear  = (Integer) get(gui, "calYear");

                if (tracker != null && user != null && calMonth != null && calYear != null) {
                    Method m = tracker.getClass().getDeclaredMethod("listEvents", user.getClass(), int.class, int.class);
                    m.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Vector<Model.Event> monthEvents = (Vector<Model.Event>) m.invoke(tracker, user, calMonth, calYear);
                    for (Model.Event ev : monthEvents) {
                        if (ev != null && ev.getDateTime() != null && ev.getDateTime().toLocalDate().equals(movedDay)) {
                            movedEvents.add(ev);
                        }
                    }
                }
            } catch (Exception ignored) {}

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 8000);
            runOnEdtLater(() -> call(gui, "openDayEventsDialog",
                    new Class[]{LocalDate.class, java.util.List.class},
                    new Object[]{movedDay, movedEvents}));
            pause(9000);
            waitForNoDialogs(5000);

            // -------------------------
            // Conflict demo:
            // Our event vs an existing event tomorrow
            // -------------------------
            showInfo("Demo – Conflict Warning",
                    "Next we will demonstrate overlap validation.\n" +
                            "We will try setting our event time to overlap with an existing event.\n" +
                            "You should see a warning message and the change will not be saved.", INFO_MS+1000);
            pause(INFO_MS + 1600);

            // Step 1: get a real existing time from tomorrow events (first one)
            LocalDateTime conflictDT = null;
            try {
                if (!tomorrowEvents.isEmpty() && tomorrowEvents.get(0) != null) {
                    conflictDT = tomorrowEvents.get(0).getDateTime();
                }
            } catch (Exception ignored) {}

            if (conflictDT == null) {
                conflictDT = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
            }
            String conflictWhen = conflictDT.format(DT_FMT);

            // Step 2: go to Edit screen for our moved day and select our event
            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, movedDay, 1);
            pause(900);

            runOnEdt(() -> {
                call(gui, "refreshEditEventsList");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_EDIT_EVENT"), true});
            });
            pause(1600);

            runOnEdt(() -> {
                try {
                    JList<?> list = (JList<?>) get(gui, "editEventsList");
                    if (list == null) return;
                    ListModel<?> model = list.getModel();
                    int size = model.getSize();
                    for (int i = 0; i < size; i++) {
                        Object it = model.getElementAt(i);
                        if (it instanceof Model.Event ev) {
                            String t = ev.getTitle();
                            if (t != null && (t.contains(createdTitle) || t.contains("Demo Event"))) {
                                list.setSelectedIndex(i);
                                list.ensureIndexIsVisible(i);
                                return;
                            }
                        }
                    }
                    if (size > 0) list.setSelectedIndex(0);
                } catch (Exception ignored) {}
            });
            pause(700);

            // Set conflicting datetime and KEEP the edit screen visible
            typeField(gui, "editEventDateTimeTF", conflictWhen, TYPE_DELAY_MS);

            // Let user see the invalid attempt screen clearly BEFORE Save
            pause(4200);

            // Keep the error dialog open long enough to be seen
            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, ERROR_WATCH_MS);

            // Save -> should fail due to conflict (and stay on edit screen)
            runOnEdt(() -> call(gui, "submitEditEvent"));
            pause(1200);
            waitForNoDialogs(2000);

            // Restore to valid time and save OK
            typeField(gui, "editEventDateTimeTF", editedWhen, TYPE_DELAY_MS);
            pause(2200);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 2200);
            runOnEdt(() -> call(gui, "submitEditEvent"));
            pause(2400);

            // -------------------------
            // Remove the event
            // -------------------------
            showInfo("Demo – Remove Event",
                    "Now we will remove the event we created.\n" +
                            "After removal, it should disappear from the list and the calendar.", INFO_MS);
            pause(INFO_MS + 600);

            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, movedDay, 1);
            pause(900);

            runOnEdt(() -> {
                call(gui, "refreshRemoveEventsList");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_REMOVE_EVENT"), true});
            });
            pause(1600);

            runOnEdt(() -> {
                try {
                    JList<?> list = (JList<?>) get(gui, "removeEventsList");
                    if (list == null) return;
                    ListModel<?> model = list.getModel();
                    int size = model.getSize();
                    for (int i = 0; i < size; i++) {
                        Object it = model.getElementAt(i);
                        if (it instanceof Model.Event ev) {
                            String t = ev.getTitle();
                            if (t != null && (t.contains("Demo Event"))) {
                                list.setSelectedIndex(i);
                                list.ensureIndexIsVisible(i);
                                return;
                            }
                        }
                    }
                    if (size > 0) list.setSelectedIndex(0);
                } catch (Exception ignored) {}
            });
            pause(900);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 2600);
            runOnEdtLater(() -> call(gui, "removeSelectedEvent"));
            pause(4000);

            // Finish message (required)
            showInfo("Demo - Events", "Part 5 completed.", INFO_MS);
            pause(INFO_MS + 300);

            pause(2500);
        }, "Demo-Part5").start();
    }

    // =========================
    // PART 6: Notifications
    // =========================
    public static void part6(JobTrackerGUI gui) {
        new Thread(() -> {
            // Slower pacing + emphasize long explanations
            final int SHORT_MS = 4500; // short info
            final int INFO_MS = 6500; // normal info
            final int BIG_MS = 9000; // big info (multi-paragraph)
            final int HUGE_MS = 13000; // very big info (the main explanation)
            final int WATCH_MS = 3400; // time to watch list/calendar changes

            pause(500);

            // Login -> then go to main menu first (as requested)
            ensureLogin(gui, pickEmail(), pickPass());
            pause(900);

            runOnEdt(() -> call(gui, "showCard", new Class[]{String.class, boolean.class},
                    new Object[]{getStaticCard("C_MENU"), false}));
            pause(1200);

            showInfo("Demo - Notifications",
                    "Part 6 - Notifications\n\n" +
                            "Now we will demonstrate the notifications feature.\n" +
                            "Entering: My Notifications.", INFO_MS);
            pause(INFO_MS + 700);

            // Enter My Notifications
            runOnEdt(() -> {
                call(gui, "refreshNotifs");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_NOTIFS"), true});
            });
            pause(1800);

            // Explain 2 types (BIG/HUGE)
            showInfo("Demo - Notifications",
                    "There are 2 notification types:\n\n" +
                            "1) System notifications (not tied to a specific event):\n" +
                            "   Alerts about processes where the job was published more than 60 days ago,\n" +
                            "   so the user may want to check if the position is still open.\n\n" +
                            "2) Event notifications:\n" +
                            "   The user receives a notification about ~24 hours before an event occurs.",
                    HUGE_MS);
            pause(HUGE_MS + 900);

            // Go to calendar
            runOnEdt(() -> {
                call(gui, "resetCalendarToNow");
                call(gui, "refreshCalendar");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_EVENTS"), true});
            });
            pause(1800);

            // Highlight tomorrow with the blue rectangle (show the click)
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, tomorrow, 1);
            pause(1100);

            showInfo("Demo - Notifications",
                    "In the calendar you can see that tomorrow (in less than 24 hours)\n" +
                            "there are 2 events.\n" +
                            "Therefore, we expect to see event notifications in My Notifications.",
                    BIG_MS);
            pause(BIG_MS + 900);

            // Back to notifications - show the messages
            runOnEdt(() -> {
                call(gui, "refreshNotifs");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_NOTIFS"), true});
            });
            pause(1900);

            // Give a moment to watch the list
            pause(WATCH_MS + 600);

            // Mark one as seen (show selection)
            showInfo("Demo - Notifications",
                    "Now we can mark a notification as 'Seen'.\n" +
                            "We will select one notification and mark it as seen using the single-item button.",
                    BIG_MS);
            pause(BIG_MS + 700);

            runOnEdt(() -> {
                JList<?> list = (JList<?>) get(gui, "notifsList");
                if (list != null && list.getModel().getSize() > 0) {
                    list.setSelectedIndex(0);
                    list.ensureIndexIsVisible(0);
                }
            });
            pause(1100);
            pause(WATCH_MS + 400);

            // Click "Mark Selected As Seen"
            runOnEdtLater(() -> {
                JButton b = findButtonInActiveFrame("Mark Selected As Seen");
                if (b != null) b.doClick();
            });
            pause(1700);

            runOnEdt(() -> call(gui, "refreshNotifs"));
            pause(1100);
            pause(WATCH_MS + 400);

            showInfo("Demo - Notifications",
                    "The selected notification is now marked as seen.\n" +
                            "Additionally, there is an option to mark ALL notifications as 'Seen'.",
                    BIG_MS);
            pause(BIG_MS + 700);

            // Click "Mark All As Seen" and let viewer see the change
            runOnEdtLater(() -> {
                JButton b = findButtonInActiveFrame("Mark All As Seen");
                if (b != null) b.doClick();
            });
            pause(1600);

            runOnEdt(() -> call(gui, "refreshNotifs"));
            pause(1100);
            pause(WATCH_MS + 800);

            // Now: add event in +2 days, and show that no notif yet (>24h)
            showInfo("Demo - Notifications",
                    "Next: we will add a new event to the calendar\n" +
                            "and verify that an event notification is created only when the event is within ~24 hours.",
                    BIG_MS);
            pause(BIG_MS + 900);

            // Go to calendar
            runOnEdt(() -> {
                call(gui, "resetCalendarToNow");
                call(gui, "refreshCalendar");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_EVENTS"), true});
            });
            pause(1800);

            // Create an event +2 days (must be >24h)
            LocalDateTime addDT = LocalDateTime.now()
                    .plusDays(2)
                    .withHour(10)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            String addTitle = "Notifications Demo Event";
            int addDur = 30;

            // Move blue rectangle to the new event day (show click)
            LocalDate addDay = addDT.toLocalDate();
            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, addDay, 1);
            pause(1100);

            // Open Add Event screen and fill
            showCard(gui, getStaticCard("C_ADD_EVENT"));
            pause(1300);

            runOnEdt(() -> selectComboByValue(gui, "eventTypeCB", "Meeting"));
            typeField(gui, "eventTitleTF", addTitle, TYPE_DELAY_MS);
            typeField(gui, "eventDateTimeTF", addDT.format(DT_FMT), TYPE_DELAY_MS);
            typeField(gui, "eventDurationTF", String.valueOf(addDur), TYPE_DELAY_MS);
            typeArea(gui, "eventNotesTA", "Added in Demo.part6", TYPE_DELAY_AREA_MS);

            pause(550);
            runOnEdt(() -> call(gui, "submitEvent"));
            pause(2700);

            // Back to calendar: refresh + highlight the day again so it is visible
            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, addDay, 1);
            pause(1200);
            pause(WATCH_MS + 900);

            // Go to notifications: should NOT show event notif yet
            runOnEdt(() -> {
                call(gui, "refreshNotifs");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_NOTIFS"), true});
            });
            pause(1900);

            pause(WATCH_MS + 800);

            showInfo("Demo - Notifications",
                    "We did not receive a notification for the new event yet,\n" +
                            "because it occurs in more than 24 hours.",
                    BIG_MS);
            pause(BIG_MS + 800);

            // Now change the event time to within 7 hours -> should generate event notification
            runOnEdt(() -> {
                call(gui, "resetCalendarToNow");
                call(gui, "refreshCalendar");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_EVENTS"), true});
            });
            pause(1800);

            // Highlight the event day again before editing (show click)
            runOnEdt(() -> call(gui, "refreshCalendar"));
            clickCalendarDay(gui, addDay, 1);
            pause(1100);

            // Prepare edit list filtered by that day
            try {
                Field f = JobTrackerGUI.class.getDeclaredField("selectedDay");
                f.setAccessible(true);
                f.set(gui, addDay);
            } catch (Exception ignored) {}

            runOnEdt(() -> {
                call(gui, "refreshEditEventsList");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_EDIT_EVENT"), true});
            });
            pause(1900);

            // Select our event in the edit list (by title contains)
            runOnEdt(() -> {
                try {
                    JList<?> list = (JList<?>) get(gui, "editEventsList");
                    if (list == null) return;
                    ListModel<?> model = list.getModel();
                    for (int i = 0; i < model.getSize(); i++) {
                        Object it = model.getElementAt(i);
                        if (it instanceof Model.Event ev) {
                            String t = ev.getTitle();
                            if (t != null && t.contains(addTitle)) {
                                list.setSelectedIndex(i);
                                list.ensureIndexIsVisible(i);
                                return;
                            }
                        }
                    }
                    if (model.getSize() > 0) list.setSelectedIndex(0);
                } catch (Exception ignored) {}
            });
            pause(1100);

            // Change time to within 7 hours
            LocalDateTime nearDT = LocalDateTime.now()
                    .plusHours(7)
                    .withSecond(0)
                    .withNano(0);

            typeField(gui, "editEventDateTimeTF", nearDT.format(DT_FMT), TYPE_DELAY_MS);

            // Let viewer see the edit screen before saving (longer)
            pause(5200);

            runOnEdt(() -> call(gui, "submitEditEvent"));
            pause(2900);

            // Back to notifications: should now show an event notification
            runOnEdt(() -> {
                call(gui, "refreshNotifs");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_NOTIFS"), true});
            });
            pause(2000);

            pause(WATCH_MS + 900);

            showInfo("Demo - Notifications",
                    "After changing the event to occur within less than 24 hours,\n" +
                            "an event notification was received as expected.",
                    BIG_MS);
            pause(BIG_MS + 800);

            // Finish Part 6 (big end screen)
            showInfo("Demo - Notifications",
                    "Part 6 completed: Notifications.",
                    INFO_MS + 1000);
            pause(INFO_MS + 1500);

        }, "Demo-Part6").start();
    }

    // =========================
    // PART 7: Statistics + Export
    // =========================
    public static void part7(JobTrackerGUI gui) {
        new Thread(() -> {

            // Slower pacing + big messages longer
            final int START_MSG_MS = 8000;
            final int BIG_MSG_MS = 12000;
            final int MID_MSG_MS = 8000;
            final int WATCH_NO_MSG_MS = 4200;

            pause(500);

            // Ensure logged in, then go to Main Menu
            ensureLogin(gui, pickEmail(), pickPass());
            pause(900);

            runOnEdt(() -> call(gui, "showCard", new Class[]{String.class, boolean.class},
                    new Object[]{getStaticCard("C_MENU"), false}));
            pause(1200);

            // Start Part 7 message (shown on main menu)
            showInfo("Demo - Statistics",
                    "Part 7 - Statistics for the current user\n\n" +
                            "We will review the Statistics screen, then change a process to see the impact,\n" +
                            "and finally export the statistics as TXT and HTML.",
                    START_MSG_MS);
            pause(START_MSG_MS + 700);

            // Enter Statistics
            runOnEdt(() -> {
                call(gui, "refreshStats");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_STATS"), true});
            });
            pause(1700);

            // --- Explain the screen (split into 3 big messages) ---
            showInfo("Demo - Statistics (Left side)",
                    "On the LEFT side you can see:\n" +
                            "- Total number of processes for the user\n" +
                            "- How many processes are Active vs Not Active\n" +
                            "- Total number of events and how many are upcoming\n" +
                            "- The company with the most applications and the number of applications\n" +
                            "- Average time (in days) to move from the initial stage to later stages",
                    BIG_MSG_MS-1000);
            pause(BIG_MSG_MS);

            pause(WATCH_NO_MSG_MS);

            showInfo("Demo - Statistics (Right side - chart)",
                    "On the RIGHT side (top) you can see a chart:\n" +
                            "- Applications opened per week\n" +
                            "- Displayed for the last 4 weeks",
                    MID_MSG_MS-2000);
            pause(MID_MSG_MS);

            pause(WATCH_NO_MSG_MS);

            showInfo("Demo - Statistics (Right side - stages)",
                    "On the RIGHT side (bottom) you can see:\n" +
                            "- Stage breakdown for all processes\n" +
                            "- This section updates when process stages change",
                    MID_MSG_MS-2000);
            pause(MID_MSG_MS);

            pause(WATCH_NO_MSG_MS);

            // --- Go to My Processes ---
            runOnEdt(() -> {
                call(gui, "refreshProcesses");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_PROCESSES"), true});
            });
            pause(1600);

            showInfo("Demo - Processes",
                    "Now we will change a process to demonstrate the impact on statistics.\n" +
                            "We will open the first process (Backend), then change its stage and status.",
                    MID_MSG_MS);
            pause(MID_MSG_MS + 700);

            // Select a process: prefer one whose toString contains "Backend", else first
            final ApplyFor[] chosen = new ApplyFor[1];
            runOnEdt(() -> {
                try {
                    JList<?> list = (JList<?>) get(gui, "appsList");
                    if (list == null || list.getModel().getSize() == 0) return;

                    int best = -1;
                    ListModel<?> model = list.getModel();
                    for (int i = 0; i < model.getSize(); i++) {
                        Object it = model.getElementAt(i);
                        if (it != null && it.toString().toLowerCase(java.util.Locale.ROOT).contains("backend")) {
                            best = i;
                            break;
                        }
                    }
                    if (best < 0) best = 0;

                    list.setSelectedIndex(best);
                    list.ensureIndexIsVisible(best);
                    Object v = list.getSelectedValue();
                    if (v instanceof ApplyFor a) chosen[0] = a;
                } catch (Exception ignored) {}
            });
            pause(900);

            if (chosen[0] == null) return;

            // Open process details
            runOnEdt(() -> call(gui, "openProcessDetails", new Class[]{ApplyFor.class}, new Object[]{chosen[0]}));
            pause(1700);

            showInfo("Demo - Process Details",
                    "Now we will change the process:\n" +
                            "- Change the stage from an initial stage to a later stage (Interview)\n" +
                            "- Also toggle the process status to Not Active (for demonstration)",
                    BIG_MSG_MS);
            pause(BIG_MSG_MS + 700);

            // --- Apply changes: stage + status (NO hard-coded enum constant) ---

            runOnEdt(() -> {
                try {
                    // Toggle status to Not Active if not already
                    JLabel statusVal = (JLabel) get(gui, "pdStatusVal");
                    JButton toggleBtn = (JButton) get(gui, "pdToggleStatusBtn");
                    String st = (statusVal == null || statusVal.getText() == null) ? "" : statusVal.getText().trim();
                    boolean isNotActive = st.equalsIgnoreCase("Not Active") || st.equalsIgnoreCase("Not active");
                    if (!isNotActive && toggleBtn != null && toggleBtn.isEnabled()) {
                        toggleBtn.doClick();
                    }
                } catch (Exception ignored) {}
            });

            pause(800);

            runOnEdt(() -> {
                try {
                    JComboBox<?> cb = (JComboBox<?>) get(gui, "pdStageCB");
                    if (cb != null && cb.getItemCount() > 0) {
                        Object pick = null;

                        // Prefer "Interview"
                        for (int i = 0; i < cb.getItemCount(); i++) {
                            Object it = cb.getItemAt(i);
                            if (it == null) continue;
                            String s = it.toString().toLowerCase(java.util.Locale.ROOT);
                            if (s.contains("interview")) { pick = it; break; }
                        }

                        // Otherwise: pick something "later" looking (best-effort)
                        if (pick == null) {
                            for (int i = 0; i < cb.getItemCount(); i++) {
                                Object it = cb.getItemAt(i);
                                if (it == null) continue;
                                String s = it.toString().toLowerCase(java.util.Locale.ROOT);
                                if (s.contains("hr") || s.contains("phone") || s.contains("test") || s.contains("assignment")
                                        || s.contains("offer") || s.contains("reject") || s.contains("withdraw")) {
                                    pick = it; break;
                                }
                            }
                        }

                        if (pick != null) cb.setSelectedItem(pick);
                        else if (cb.getItemCount() > 1) cb.setSelectedIndex(1);
                    }
                } catch (Exception ignored) {}
            });

            pause(900);

            runOnEdtLater(() -> {
                JButton save = findButtonInActiveFrame("Save Changes");
                if (save != null) save.doClick();
            });
            pause(2200);

            // Back to Statistics and refresh
            runOnEdt(() -> {
                call(gui, "refreshStats");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_STATS"), false});
            });
            pause(1800);

            pause(WATCH_NO_MSG_MS + 1000);

            showInfo("Demo - Statistics (Updated)",
                    "Now you should see changes in the statistics:\n" +
                            "- On the LEFT: Active vs Not Active counts changed\n" +
                            "- The average days to move from initial stage to later stages may change\n" +
                            "- On the RIGHT (bottom): stage breakdown changed",
                    BIG_MSG_MS);
            pause(BIG_MSG_MS + 900);

            pause(WATCH_NO_MSG_MS);

            // --- Export statistics ---
            showInfo("Demo - Export Statistics",
                    "Finally, we will export the statistics to TXT and HTML files.\n" +
                            "The files will be created in \"data\" folder in the project.",
                    MID_MSG_MS);
            pause(MID_MSG_MS + 700);

            Path dataDir = resolveDataDir();

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1200);
            runOnEdtLater(() -> call(gui, "exportStatisticsTxtTo",
                    new Class[]{Path.class},
                    new Object[]{dataDir.resolve("statistics.txt")}));
            pause(1600);
            waitForNoDialogs(2500);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1200);
            runOnEdtLater(() -> call(gui, "exportStatisticsHtmlTo",
                    new Class[]{Path.class},
                    new Object[]{dataDir.resolve("statistics.html")}));
            pause(1600);
            waitForNoDialogs(2500);

            showInfo("Demo - Export Completed",
                    "The statistics have been exported as TXT and HTML files in the \"data\" folder.",
                    MID_MSG_MS);
            pause(MID_MSG_MS + 800);


            showInfo("Demo - Done", "Part 7 completed.", 5200);
            pause(5600);

        }, "Demo-Part7").start();
    }

    // part 7 helper :
    private static Path resolveDataDir() {
        // 1) Prefer: directory where the app (jar/classes) is located
        Path baseDir = null;
        try {
            baseDir = Paths.get(Demo.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath();

            // If running from a JAR -> baseDir is the jar file path; use its parent
            // If running from IDE -> baseDir is classes dir; use it as starting point
            if (Files.isRegularFile(baseDir)) {
                baseDir = baseDir.getParent();
            }
        } catch (Exception ignored) {}

        // 2) Fallback: working directory (less reliable, but better than nothing)
        if (baseDir == null) {
            baseDir = Paths.get("").toAbsolutePath();
        }

        // 3) Walk up and look for an existing "data" folder
        Path p = baseDir;
        for (int i = 0; i < 8 && p != null; i++) {
            Path cand = p.resolve("data");
            if (Files.isDirectory(cand)) return cand;
            p = p.getParent();
        }

        // 4) If not found - create "data" next to app location
        Path fallback = baseDir.resolve("data").toAbsolutePath().normalize();
        try { Files.createDirectories(fallback); } catch (Exception ignored) {}
        return fallback;
    }

    // =========================================================
    // Helpers
    // =========================================================

    private static void ensureLogin(JobTrackerGUI gui, String email, String pass) {
        showCard(gui, getStaticCard("C_LOGIN"), false);
        pause(700);

        // Type email + pass
        typeField(gui, "loginEmailTF", email, TYPE_DELAY_MS);
        typePass(gui, "loginPassPF", pass, TYPE_DELAY_MS);

        pause(200);
        runOnEdt(() -> call(gui, "doLogin"));

        pause(1300);
    }

    private static String pickEmail() {
        return (lastDemoEmail != null) ? lastDemoEmail : DEFAULT_EMAIL;
    }

    private static String pickPass() {
        return (lastDemoPass != null) ? lastDemoPass : DEFAULT_PASS;
    }

    private static void pause() { pause(STEP_DELAY_MS); }
    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else {
            try { SwingUtilities.invokeAndWait(r); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    private static void runOnEdtLater(Runnable r) {
        SwingUtilities.invokeLater(r);
    }

    private static void showCard(JobTrackerGUI gui, String card) {
        showCard(gui, card, true);
    }

    private static void showCard(JobTrackerGUI gui, String card, boolean pushHistory) {
        runOnEdt(() -> call(gui, "showCard", new Class[]{String.class, boolean.class}, new Object[]{card, pushHistory}));
    }

    private static String getStaticCard(String fieldName) {
        Object v = getStatic(JobTrackerGUI.class, fieldName);
        return (v == null) ? null : v.toString();
    }

    private static void showInfo(String title, String message, int showMs) {
        new Thread(() -> {
            final JDialog[] holder = new JDialog[1];

            runOnEdt(() -> {
                JOptionPane pane = new JOptionPane(
                        message,
                        JOptionPane.INFORMATION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION
                );

                JDialog dlg = pane.createDialog(null, title);
                dlg.setModal(false); // do not block EDT
                dlg.setAlwaysOnTop(true);
                holder[0] = dlg;
                dlg.setVisible(true);
            });

            pause(Math.max(300, showMs));

            runOnEdt(() -> {
                if (holder[0] != null && holder[0].isShowing()) {
                    holder[0].dispose();
                }
            });
        }, "Demo-Info").start();
    }

    private static void waitForNoDialogs(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            boolean any = false;
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isShowing()) {
                    if (d.getModalityType() == Dialog.ModalityType.MODELESS) continue;
                    any = true;
                    break;
                }
            }
            if (!any) return;
            pause(50);
        }
    }

    // --- AWT Docs controls (use JobTrackerGUI fields directly) ---
    private static void setDocsAwtFilter(JobTrackerGUI gui, String type, boolean primaryOnly) {
        runOnEdt(() -> {
            Choice ch = (Choice) get(gui, "awtDocsChoice");
            Checkbox cb = (Checkbox) get(gui, "awtPrimaryOnlyCB");

            if (ch != null && type != null) ch.select(type);
            if (cb != null) cb.setState(primaryOnly);

            call(gui, "refreshAwtDocsView");

            java.awt.List list = (java.awt.List) get(gui, "awtDocsList");
            if (list != null && list.getItemCount() > 0) list.select(0);
        });

        pause(650);
    }

    private static void selectAwtDocByContains(JobTrackerGUI gui, String needle) {
        runOnEdt(() -> {
            java.awt.List list = (java.awt.List) get(gui, "awtDocsList");
            if (list == null) return;

            int n = list.getItemCount();
            for (int i = 0; i < n; i++) {
                String it = list.getItem(i);
                if (it != null && it.contains(needle)) {
                    list.select(i);
                    return;
                }
            }
            if (n > 0) list.select(0);
        });
        pause(250);
    }

    private static void clickCalendarDay(JobTrackerGUI gui, LocalDate date, int clickCount) {
        if (date == null) return;
        String dayTxt = String.valueOf(date.getDayOfMonth());

        runOnEdt(() -> {
            JPanel grid = (JPanel) get(gui, "calGrid");
            if (grid == null) return;

            JLabel target = null;
            for (JLabel lab : findAll(grid, JLabel.class)) {
                if (!lab.isShowing()) continue;
                String t = lab.getText();
                if (t != null && t.trim().equals(dayTxt)) {
                    target = lab;
                    break;
                }
            }
            if (target == null) return;

            int x = Math.max(2, target.getWidth() / 2);
            int y = Math.max(2, target.getHeight() / 2);

            long now = System.currentTimeMillis();
            java.awt.event.MouseEvent ev = new java.awt.event.MouseEvent(
                    target,
                    java.awt.event.MouseEvent.MOUSE_CLICKED,
                    now,
                    java.awt.event.InputEvent.BUTTON1_DOWN_MASK,
                    x, y,
                    clickCount,
                    false,
                    java.awt.event.MouseEvent.BUTTON1
            );
            target.dispatchEvent(ev);
        });
    }

    // -----------------------------------------
    // Scrolling helpers (best effort)
    // -----------------------------------------
    private static void scrollToField(JobTrackerGUI gui, String fieldName) {
        Object o = null;
        try { o = get(gui, fieldName); } catch (Exception ignored) {}

        if (!(o instanceof Component comp)) return;

        runOnEdt(() -> {
            if (comp instanceof JComponent jc) {
                // If inside JScrollPane/JViewport, this usually scrolls nicely
                jc.scrollRectToVisible(new Rectangle(0, 0, Math.max(1, jc.getWidth()), Math.max(1, jc.getHeight())));
                jc.requestFocusInWindow();

                // Fallback: if we have an ancestor JScrollPane, nudge its scrollbar
                Container sp = SwingUtilities.getAncestorOfClass(JScrollPane.class, jc);
                if (sp instanceof JScrollPane jsp && jsp.getVerticalScrollBar() != null) {
                    // try to align roughly with component Y
                    try {
                        Point p = SwingUtilities.convertPoint(jc.getParent(), jc.getLocation(), jsp.getViewport().getView());
                        int target = Math.max(0, p.y - 40);
                        jsp.getVerticalScrollBar().setValue(target);
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private static void smoothScrollBy(JobTrackerGUI gui, int deltaY, int steps, int stepDelayMs) {
        if (steps <= 0) steps = 1;
        int per = deltaY / steps;

        for (int i = 0; i < steps; i++) {
            final int dy = per;

            runOnEdt(() -> {
                // Try to find any visible JScrollPane and scroll it
                JScrollPane any = findFirstVisibleScrollPane();
                if (any != null && any.getVerticalScrollBar() != null) {
                    JScrollBar sb = any.getVerticalScrollBar();
                    sb.setValue(sb.getValue() + dy);
                }
            });

            pause(Math.max(10, stepDelayMs));
        }
    }

    private static JScrollPane findFirstVisibleScrollPane() {
        for (Frame f : Frame.getFrames()) {
            if (!(f instanceof JFrame jf) || !jf.isVisible()) continue;
            List<JScrollPane> sps = findAll(jf.getContentPane(), JScrollPane.class);
            for (JScrollPane sp : sps) {
                if (sp != null && sp.isShowing()) return sp;
            }
        }
        return null;
    }

    // If the GUI suddenly requires login again, auto-login and continue.
    private static void ensureStillLoggedIn(JobTrackerGUI gui) {
        // Case A: ANY dialog that contains "login" text (title/labels) -> close it and login
        JDialog needLogin = waitForDialog(2200, d -> {
            if (!d.isShowing()) return false;

            String title = safeTitle(d).toLowerCase(java.util.Locale.ROOT);
            if (title.contains("login")) return true;

            // scan labels for any "login" phrasing
            for (JLabel l : findAll(d, JLabel.class)) {
                String t = (l.getText() == null) ? "" : l.getText().toLowerCase(java.util.Locale.ROOT);
                if (t.contains("login")) return true;
                if (t.contains("log in")) return true;
                if (t.contains("please") && t.contains("login")) return true;
            }
            return false;
        });

        if (needLogin != null) {
            runOnEdtLater(() -> pressOk(needLogin));
            pause(350);
            ensureLogin(gui, pickEmail(), pickPass());
            pause(1400);
            return;
        }

        // Case B: redirected to login screen (email field visible)
        try {
            Object tf = get(gui, "loginEmailTF");
            if (tf instanceof JComponent jc && jc.isShowing()) {
                ensureLogin(gui, pickEmail(), pickPass());
                pause(1400);
            }
        } catch (Exception ignored) {}
    }


    // -------------------------
    // Typing helpers (visible)
    // -------------------------
    private static void typeField(JobTrackerGUI gui, String fieldName, String text, int charDelayMs) {
        Object o = get(gui, fieldName);
        if (o instanceof JTextComponent tc) {
            typeComponentBlocking(tc, text, charDelayMs);
        }
    }

    private static void typeArea(JobTrackerGUI gui, String fieldName, String text, int charDelayMs) {
        Object o = get(gui, fieldName);
        if (o instanceof JTextComponent tc) {
            typeComponentBlocking(tc, text, charDelayMs);
        }
    }

    private static void typePass(JobTrackerGUI gui, String fieldName, String text, int charDelayMs) {
        Object o = get(gui, fieldName);
        if (o instanceof JTextComponent tc) {
            typeComponentBlocking(tc, text, charDelayMs);
        }
    }

    private static void typeComponentBlocking(JTextComponent comp, String text, int charDelayMs) {
        if (comp == null) return;
        if (text == null) text = "";

        CountDownLatch latch = new CountDownLatch(1);

        String finalText = text;
        SwingUtilities.invokeLater(() -> {
            comp.setText("");
            comp.requestFocusInWindow();

            final int[] i = {0};
            javax.swing.Timer t = new javax.swing.Timer(Math.max(1, charDelayMs), null);
            t.addActionListener(e -> {
                if (i[0] >= finalText.length()) {
                    t.stop();
                    latch.countDown();
                    return;
                }
                String cur = comp.getText();
                comp.setText(cur + finalText.charAt(i[0]++));
                comp.setCaretPosition(comp.getText().length());
            });
            t.setInitialDelay(0);
            t.start();
        });
        try { latch.await(); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    // -------------------------
    // Combos / buttons
    // -------------------------
    private static void selectComboIndex(JobTrackerGUI gui, String fieldName, int idx) {
        Object o = get(gui, fieldName);
        if (o instanceof JComboBox<?> cb && cb.getItemCount() > 0) {
            cb.setSelectedIndex(Math.min(idx, cb.getItemCount() - 1));
        }
    }

    private static void selectComboByValue(JobTrackerGUI gui, String fieldName, String value) {
        Object o = get(gui, fieldName);
        if (o instanceof JComboBox<?> cb) {
            for (int i = 0; i < cb.getItemCount(); i++) {
                Object it = cb.getItemAt(i);
                if (it != null && it.toString().equalsIgnoreCase(value)) {
                    cb.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private static void clickButtonField(JobTrackerGUI gui, String fieldName) {
        Object o = get(gui, fieldName);
        if (o instanceof JButton b && b.isEnabled()) b.doClick();
    }

    private static ApplyFor getFirstApplicationFromList(JobTrackerGUI gui) {
        Object o = get(gui, "appsList");
        if (o instanceof JList<?> list) {
            if (list.getModel().getSize() == 0) return null;
            runOnEdt(() -> list.setSelectedIndex(0));
            Object v = list.getSelectedValue();
            if (v instanceof ApplyFor a) return a;
        }
        return null;
    }

    // -------------------------
    // Dialog automation (with typing)
    // -------------------------
    private static void autoPressOkOnNextDialog(long timeoutMs) {
        autoPressOkOnNextDialog(timeoutMs, MESSAGE_SHOW_MS);
    }

    private static void autoPressOkOnNextDialog(long timeoutMs, long keepOpenMs) {
        final long mySeq = AUTO_OK_SEQ.incrementAndGet();
        final Set<JDialog> alreadyShowing = new HashSet<>();
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing()) {
                alreadyShowing.add(d);
            }
        }
        new Thread(() -> {
            JDialog dlg = waitForDialog(timeoutMs, d -> {
                if (!d.isShowing()) return false;
                if (AUTO_OK_SEQ.get() != mySeq) return false;
                if (alreadyShowing.contains(d)) return false;
                if (d.getModalityType() == Dialog.ModalityType.MODELESS) return false;
                if (findAll(d.getContentPane(), JOptionPane.class).isEmpty()) return false;
                return findAnyButton(d, "OK", "Ok", "Yes", "Save", "Update", "Apply") != null;
            });

            if (dlg == null) return;
            if (AUTO_OK_SEQ.get() != mySeq) return;

            pause(Math.max(0, keepOpenMs));
            if (AUTO_OK_SEQ.get() != mySeq) return;

            runOnEdtLater(() -> {
                if (dlg.isShowing()) pressOk(dlg);
            });
        }, "Demo-AutoOk").start();
    }

    private static void autoTypeConfirmDialogByLabelAndOk(Map<String, String> byLabel,
                                                          long timeoutMs,
                                                          int charDelayMs) {
        new Thread(() -> {
            JDialog dlg = waitForDialog(timeoutMs, d -> d.isShowing() && hasAnyLabel(d, byLabel.keySet()));
            if (dlg == null) return;

            // Type into matching fields (found by labels)
            List<JLabel> labels = findAll(dlg, JLabel.class);
            for (JLabel lab : labels) {
                String txt = lab.getText();
                if (txt == null) continue;
                String want = byLabel.get(txt);
                if (want == null) continue;

                Container parent = lab.getParent();
                if (parent == null) continue;
                Component[] comps = parent.getComponents();
                for (int i = 0; i < comps.length - 1; i++) {
                    if (comps[i] == lab) {
                        Component next = comps[i + 1];
                        if (next instanceof JTextComponent tc) {
                            typeComponentBlocking(tc, want, charDelayMs);
                        }
                        break;
                    }
                }
            }

            pause(2500);
            runOnEdtLater(() -> pressOk(dlg));
        }, "Demo-AutoTypeByLabel").start();
    }

    private static void autoTypeAddDocumentDialogAndOk(String name, String type, String target,
                                                       String note, boolean primary,
                                                       long timeoutMs, int charDelayMs) {
        new Thread(() -> {
            JDialog dlg = waitForDialog(timeoutMs, d ->
                    d.isShowing() && ("Add Document".equalsIgnoreCase(safeTitle(d)) || hasLabel(d, "Doc Name :"))
            );
            if (dlg == null) return;

            List<JTextField> tfs = findAll(dlg, JTextField.class);
            List<JComboBox> cbs = findAll(dlg, JComboBox.class);
            List<JCheckBox> cks = findAll(dlg, JCheckBox.class);

            if (tfs.size() >= 1) typeComponentBlocking(tfs.get(0), name, charDelayMs);
            if (!cbs.isEmpty()) runOnEdtLater(() -> cbs.get(0).setSelectedItem(type));
            if (tfs.size() >= 2) typeComponentBlocking(tfs.get(1), target, charDelayMs);
            if (tfs.size() >= 3) typeComponentBlocking(tfs.get(2), note, charDelayMs);
            if (!cks.isEmpty()) runOnEdtLater(() -> cks.get(0).setSelected(primary));

            pause(200);
            runOnEdtLater(() -> pressOk(dlg));
        }, "Demo-AddDocType").start();
    }

    private static void autoTypeLogCommunicationDialogAndOk(String method, String subject, String when,
                                                            long timeoutMs, int charDelayMs) {
        new Thread(() -> {
            JDialog dlg = waitForDialog(timeoutMs, dd -> dd.isShowing() && ("Log Communication".equalsIgnoreCase(safeTitle(dd)) || hasLabel(dd, "Method:")));
            if (dlg == null) return;

            List<JComboBox> cbs = findAll(dlg, JComboBox.class);
            List<JTextField> tfs = findAll(dlg, JTextField.class);

            if (!cbs.isEmpty()) runOnEdtLater(() -> cbs.get(0).setSelectedItem(method));
            if (tfs.size() >= 1) typeComponentBlocking(tfs.get(0), subject, charDelayMs);
            if (tfs.size() >= 2) typeComponentBlocking(tfs.get(1), when, charDelayMs);

            pause(200);
            runOnEdtLater(() -> pressOk(dlg));
        }, "Demo-AutoTypeLogComm").start();
    }

    // ---- robust dialog wait ----
    private static JDialog waitForDialog(long timeoutMs, Predicate<JDialog> predicate) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isShowing()) {
                    try {
                        if (predicate == null || predicate.test(d)) return d;
                    } catch (Exception ignored) {}
                }
            }
            pause(40);
        }
        return null;
    }

    private static void pressOk(JDialog dlg) {
        JButton b = findAnyButton(dlg, "OK", "Ok", "Yes", "Save", "Update", "Apply");
        if (b != null) b.doClick();
        else dlg.dispose();
    }

    private static JButton findAnyButton(Container root, String... texts) {
        if (root == null || texts == null) return null;
        for (JButton b : findAll(root, JButton.class)) {
            if (b.getText() == null) continue;
            for (String t : texts) {
                if (t != null && b.getText().equalsIgnoreCase(t)) return b;
            }
        }
        return null;
    }

    private static JButton findButtonByText(Container root, String text) {
        for (JButton b : findAll(root, JButton.class)) {
            if (b.getText() != null && b.getText().equalsIgnoreCase(text)) return b;
        }
        return null;
    }

    private static boolean hasLabel(Container root, String labelText) {
        if (labelText == null) return false;
        for (JLabel l : findAll(root, JLabel.class)) {
            if (l.getText() != null && l.getText().equals(labelText)) return true;
        }
        return false;
    }

    private static boolean hasAnyLabel(Container root, Collection<String> labels) {
        if (labels == null || labels.isEmpty()) return false;
        for (JLabel l : findAll(root, JLabel.class)) {
            String t = l.getText();
            if (t != null && labels.contains(t)) return true;
        }
        return false;
    }

    private static String safeTitle(JDialog d) {
        try {
            String t = d.getTitle();
            return t == null ? "" : t.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static JButton findButtonInActiveFrame(String text) {
        // Prefer the currently active window (prevents clicking buttons from other screens)
        try {
            Window aw = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            if (aw instanceof RootPaneContainer rpc) {
                JButton b = findButtonByText(rpc.getContentPane(), text);
                if (b != null) return b;
            }
        } catch (Exception ignored) {}

        // Fallback: any visible frame
        for (Frame f : Frame.getFrames()) {
            if (f instanceof JFrame jf && jf.isVisible()) {
                JButton b = findButtonByText(jf.getContentPane(), text);
                if (b != null) return b;
            }
        }
        return null;
    }

    private static <T> List<T> findAll(Container root, Class<T> cls) {
        List<T> out = new ArrayList<>();
        if (root == null) return out;

        Deque<Component> q = new ArrayDeque<>();
        q.add(root);
        while (!q.isEmpty()) {
            Component c = q.poll();
            if (cls.isInstance(c)) out.add(cls.cast(c));
            if (c instanceof Container ct) {
                for (Component ch : ct.getComponents()) q.add(ch);
            }
        }
        return out;
    }

    // -------------------------
    // Demo file
    // -------------------------
    private static String createDemoFile() {
        try {
            Path p = Paths.get("data", "demo_document.txt");
            Files.createDirectories(p.getParent());
            Files.writeString(p, "Demo document content\nCreated at: " + LocalDateTime.now());
            return p.toAbsolutePath().toString();
        } catch (Exception e) {
            return Paths.get("data", "demo_document.txt").toAbsolutePath().toString();
        }
    }

    // -------------------------
    // Reflection
    // -------------------------
    private static Object get(JobTrackerGUI gui, String fieldName) {
        try {
            Field f = JobTrackerGUI.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(gui);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field: " + fieldName, e);
        }
    }

    private static Object getStatic(Class<?> cls, String fieldName) {
        try {
            Field f = cls.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get static field: " + fieldName, e);
        }
    }

    private static Object call(JobTrackerGUI gui, String method) {
        return call(gui, method, new Class[]{}, new Object[]{});
    }

    private static Object call(JobTrackerGUI gui, String method, Class<?>[] sig, Object[] args) {
        try {
            Method m = JobTrackerGUI.class.getDeclaredMethod(method, sig);
            m.setAccessible(true);
            return m.invoke(gui, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call: " + method, e);
        }
    }
}