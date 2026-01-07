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
            pause();

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

        }, "Demo-Part1").start();
    }

    // =========================
    // PART 2: Menu + Personal Area + Edit Profile + Change Password
    // =========================
    public static void part2(JobTrackerGUI gui) {
        new Thread(() -> {
            pause();
            ensureLogin(gui, pickEmail(), pickPass());
            pause();

            // Menu -> Personal Area
            showCard(gui, getStaticCard("C_MENU"));
            pause(500);

            runOnEdt(() -> {
                call(gui, "refreshProfileView");
                call(gui, "showCard", new Class[]{String.class, boolean.class},
                        new Object[]{getStaticCard("C_PROFILE"), true});
            });
            pause(1000);

            // Edit Profile dialog: type and OK
            autoTypeConfirmDialogByLabelAndOk(Map.of(
                    "Full name :", "Demo User Edited",
                    "Phone :", "054-2222222",
                    "Field of search :", "Software"
            ), DIALOG_TIMEOUT_MS, TYPE_DELAY_MS);

            runOnEdt(() -> call(gui, "openEditProfileDialog"));
            pause(1500);

            // =========================
            // Change Password - attempt #1 (invalid: same password)
            // =========================
            autoTypeConfirmDialogByLabelAndOk(Map.of(
                    "Old password :", pickPass(),
                    "New password :", pickPass(),
                    "Repeat new password :", pickPass()
            ), DIALOG_TIMEOUT_MS, TYPE_DELAY_MS);

            runOnEdtLater(() -> call(gui, "openChangePasswordDialog"));

            pause(2600);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1100);
            pause(1700);

            pause(1200);

            // =========================
            // Change Password - attempt #2 (valid)
            // =========================
            String newPass = "987654321";

            autoTypeConfirmDialogByLabelAndOk(Map.of(
                    "Old password :", pickPass(),
                    "New password :", newPass,
                    "Repeat new password :", newPass
            ), DIALOG_TIMEOUT_MS, TYPE_DELAY_MS);

            runOnEdtLater(() -> call(gui, "openChangePasswordDialog"));

            pause(2800);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 1200);
            pause(2800);

            // Update stored demo password for next parts
            lastDemoPass = newPass;

        }, "Demo-Part2").start();
    }

    // =========================
    // PART 3: Documents (Add File + Add URL + Details/Edit + Filters + Remove)
    // =========================
    public static void part3(JobTrackerGUI gui) {
        new Thread(() -> {
            pause();
            ensureLogin(gui, pickEmail(), pickPass());
            pause();

            // Go to docs
            runOnEdt(() -> {
                call(gui, "refreshDocs");
                call(gui, "showCard", new Class[]{String.class, boolean.class}, new Object[]{getStaticCard("C_DOCS"), true});
            });
            pause(1300);

            String demoFile = createDemoFile();

            // Add Document (File) with typing
            autoTypeAddDocumentDialogAndOk(
                    "Demo File", "File", demoFile, "Demo file note", true,
                    DIALOG_TIMEOUT_MS, TYPE_DELAY_MS
            );
            runOnEdtLater(() -> call(gui, "openAddDocumentDialog")); // modal JOptionPane
            pause(1400);
            waitForNoDialogs(3500);

            // Add Document (URL) with typing
            autoTypeAddDocumentDialogAndOk(
                    "Demo URL", "URL", "https://example.com", "Demo url note", false,
                    DIALOG_TIMEOUT_MS, TYPE_DELAY_MS
            );
            runOnEdtLater(() -> call(gui, "openAddDocumentDialog")); // modal
            pause(1400);
            waitForNoDialogs(3500);

            // Ensure docs view refreshed and a selection exists
            runOnEdt(() -> {
                call(gui, "refreshAwtDocsView");
                java.awt.List list = (java.awt.List) get(gui, "awtDocsList");
                if (list != null && list.getItemCount() > 0) list.select(0);
            });
            pause(600);

            // ---- Open Details (IMPORTANT: async, because details dialog is modal JDialog) ----
            runOnEdtLater(() -> call(gui, "detailsSelectedAwtDoc"));

            // Wait specifically for the Details dialog (not for info dialogs)
            JDialog detailsDlg = waitForDialog(DIALOG_TIMEOUT_MS, d ->
                    d.isShowing()
                            && ("Document Details".equalsIgnoreCase(safeTitle(d)) ||
                            (findButtonByText(d, "Edit") != null && findButtonByText(d, "Close") != null))
            );
            if (detailsDlg == null) return;

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
                                && findAnyButton(d, "OK", "Ok", "Save", "Update") != null
                );
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
            setDocsAwtFilter(gui, "All", false);
            selectAwtDocByContains(gui, "LinkedIn Profile 1122");

            // Remove selected doc (Confirm OK) - async because it opens modal confirm dialog
            showInfo("Demo - Documents", "Next: Remove the selected document.", 3500);
            pause(3600);

            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 600);
            runOnEdtLater(() -> call(gui, "removeSelectedAwtDoc")); // modal confirm
            pause(1200);
            waitForNoDialogs(3500);

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
            showInfo("Demo - Processes",
                    "We will now add a new application and fill the entire form.\n" +
                            "We will scroll while typing so you can see all fields being filled.",
                    4800);
            pause(5200);

            showCard(gui, getStaticCard("C_ADD_APP"));
            pause(1400);

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
                    4500);
            pause(4900);

            ApplyFor app = getFirstApplicationFromList(gui);
            if (app == null) return;

            runOnEdtLater(() -> call(gui, "openProcessDetails", new Class[]{ApplyFor.class}, new Object[]{app}));
            pause(1700);

            // -----------------------------------------
            // Step 4: Edit details (status + stage + source + note)
            // -----------------------------------------
            showInfo("Demo - Process Details",
                    "Now we will edit the process details:\n" +
                            "toggle status, change stage, update source, and add a note.",
                    4200);
            pause(4600);

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

            showInfo(
                    "Demo - My Processes",
                    "Changes were saved (stage / source / note / status).\n\n" +
                            "Color explanation in the list:\n" +
                            "• Green = active processes\n" +
                            "• Yellow = overdue processes\n" +
                            "• Red = final stage (Withdrawn / Rejected / Offer)\n" +
                            "• Grey = not active processes" +
                            "\nTurned grey because we toggled status to Not Active.",
                    7000
            );
            pause(7300);

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
                    5600);
            pause(6100);

            // (Optional) Add/Edit Contact dialog (typing + OK)
            autoTypeSimple4FieldsDialogAndOk("Dana Levi", "R&D", "dana@demo.com", "054-1234567",
                    DIALOG_TIMEOUT_MS, TYPE_DELAY_MS);

            runOnEdtLater(() -> {
                JButton b = findButtonInActiveFrame("Add / Edit Contact");
                if (b != null) b.doClick();
            });
            autoPressOkOnNextDialog(DIALOG_TIMEOUT_MS, 3000);
            pause(1500);

            pause(2000);

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
            pause(5000);

            // -----------------------------------------
            // Step 6: Withdraw demo + return to processes and see red
            // -----------------------------------------
            showInfo("Demo - Withdraw",
                    "Now we will demonstrate what 'Withdraw' means:\n" +
                            "withdrawing the application from the process.",
                    4600);
            pause(5100);

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
                    "Back in the list, you should now see the process changed accordingly\n" +
                            "Turned red (final stage)",
                    5200);
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

            showInfo("Demo - Done",
                    "The process was removed successfully.\n" +
                            "This concludes Part 4 of the demo.",
                    4200);
            pause(4600);

        }, "Demo-Part4").start();
    }

    // =========================
    // PART 5: Events
    // =========================
    public static void part5(JobTrackerGUI gui) {
        new Thread(() -> {

            // --- tuned timings (less dialogs, more watching time) ---
            final int INFO_MS = 5200;      // normal info bubble time
            final int WATCH_MS = 6000;     // time to watch calendar changes
            final int EDIT_WATCH_MS = 4200; // time to watch edit form before saving
            final int ERROR_WATCH_MS = 7000; // keep error dialog long

            pause(300);

            // Login first (message must be AFTER login)
            ensureLogin(gui, pickEmail(), pickPass());
            pause(600);

            showInfo("Demo - Events",
                    "Part 5: Events.\nWe will demonstrate the calendar, adding, viewing, editing, conflict detection, and removing an event.",
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
            showInfo("Demo - Events",
                    "This is the calendar.\nYou can double-click a day to view its events.",
                    INFO_MS);
            pause(INFO_MS + 600);

            // -------------------------
            // Add event (+2 days)
            // -------------------------
            showInfo("Demo - Events", "Now we will add a new event (+2 days).", INFO_MS);
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
            showInfo("Demo - Events",
                    "The event was added.\nTake a moment to see it in the calendar.",
                    WATCH_MS-2000);
            pause(10000);
            waitForNoDialogs(2000);

            // -------------------------
            // View tomorrow (existing 2 events)
            // We show the events via internal dialog
            // -------------------------
            showInfo("Demo - Events",
                    "Now we will view tomorrow.\nTomorrow already has existing events.",
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
            showInfo("Demo - Events", "Now we will edit the event we created.", INFO_MS);
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
            pause(EDIT_WATCH_MS);

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
            showInfo("Demo - Events",
                    "Event updated.\nNew date/time: " + editedWhen + "\nNew duration: " + editedDur + " minutes.",
                    INFO_MS-1500);
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
            // IMPORTANT: show the invalid attempt screen BEFORE saving
            // -------------------------
            showInfo("Demo - Events",
                    "Next: conflict warning.\nWe will try to set our event time to overlap with an existing event tomorrow.",
                    INFO_MS);
            pause(INFO_MS + 600);

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
            showInfo("Demo - Events", "Now we will remove the event we created.", INFO_MS);
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
            pause();
            ensureLogin(gui, pickEmail(), pickPass());
            pause();

            runOnEdt(() -> {
                call(gui, "refreshNotifs");
                call(gui, "showCard", new Class[]{String.class, boolean.class}, new Object[]{getStaticCard("C_NOTIFS"), true});
            });
            pause(1300);

            runOnEdt(() -> {
                JList<?> list = (JList<?>) get(gui, "notifsList");
                if (list != null && list.getModel().getSize() > 0) list.setSelectedIndex(0);
                call(gui, "markNotifSeen");
            });
            pause(1200);

            runOnEdt(() -> call(gui, "markAllNotifsSeen"));
            pause(1200);

        }, "Demo-Part6").start();
    }

    // =========================
    // PART 7: Statistics + Export
    // =========================
    public static void part7(JobTrackerGUI gui) {
        new Thread(() -> {
            pause();
            ensureLogin(gui, pickEmail(), pickPass());
            pause();

            runOnEdt(() -> {
                call(gui, "refreshStats");
                call(gui, "showCard", new Class[]{String.class, boolean.class}, new Object[]{getStaticCard("C_STATS"), true});
            });
            pause(1700);

            // Export TXT (native FileDialog may require manual cancel)
            runOnEdtLater(() -> call(gui, "exportStatisticsAsTxt"));
            pause(1000);
            tryCloseNativeFileDialogBestEffort();
            pause(900);

            // Export HTML
            runOnEdtLater(() -> call(gui, "exportStatisticsAsHtml"));
            pause(1000);
            tryCloseNativeFileDialogBestEffort();
            pause(900);

        }, "Demo-Part7").start();
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

    private static void waitUntilShowing(JobTrackerGUI gui, String fieldName, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                Object o = get(gui, fieldName);
                if (o instanceof Component c && c.isShowing()) return;
            } catch (Exception ignored) {}
            pause(40);
        }
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

    private static void selectEventInListByContains(JobTrackerGUI gui, String listFieldName, String needle) {
        runOnEdt(() -> {
            Object o = get(gui, listFieldName);
            if (!(o instanceof JList<?> list)) return;

            ListModel<?> m = list.getModel();
            int best = -1;
            for (int i = 0; i < m.getSize(); i++) {
                Object v = m.getElementAt(i);
                String s = (v == null) ? "" : v.toString();
                if (s.contains(needle)) { best = i; break; }
            }
            if (best >= 0) list.setSelectedIndex(best);
            else if (m.getSize() > 0) list.setSelectedIndex(0);
        });
        pause(200);
    }

    private static String readFirstEventDateTimeFromEditList(JobTrackerGUI gui) {
        try {
            Object o = get(gui, "editEventsList");
            if (!(o instanceof JList<?> list)) return null;
            if (list.getModel().getSize() == 0) return null;

            Object first = list.getModel().getElementAt(0);
            if (first instanceof Model.Event ev && ev.getDateTime() != null) {
                return ev.getDateTime().format(DT_FMT);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
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

    // -----------------------------------------
    // Remove process button (best effort)
    // -----------------------------------------
    private static void clickRemoveProcessButtonBestEffort() {
        String[] candidates = {
                "Remove", "Delete", "Remove Process", "Remove Application", "Remove This Process", "Delete Process"
        };
        JButton b = null;
        for (String c : candidates) {
            b = findButtonInActiveFrame(c);
            if (b != null) break;
        }
        if (b != null) b.doClick();
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

    private static void autoTypeSimple4FieldsDialogAndOk(String a, String b, String c, String d,
                                                         long timeoutMs, int charDelayMs) {
        new Thread(() -> {
            JDialog dlg = waitForDialog(timeoutMs, dd -> dd.isShowing() && findAll(dd, JTextField.class).size() >= 4);
            if (dlg == null) return;

            List<JTextField> tfs = findAll(dlg, JTextField.class);
            if (tfs.size() >= 1) typeComponentBlocking(tfs.get(0), a, charDelayMs);
            if (tfs.size() >= 2) typeComponentBlocking(tfs.get(1), b, charDelayMs);
            if (tfs.size() >= 3) typeComponentBlocking(tfs.get(2), c, charDelayMs);
            if (tfs.size() >= 4) typeComponentBlocking(tfs.get(3), d, charDelayMs);

            pause(200);
            runOnEdtLater(() -> pressOk(dlg));
        }, "Demo-AutoType4Fields").start();
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
    // Native FileDialog close (best effort)
    // -------------------------
    private static void tryCloseNativeFileDialogBestEffort() {
        for (Window w : Window.getWindows()) {
            if (w == null || !w.isVisible()) continue;
            String cn = w.getClass().getName();
            if (cn.contains("FileDialog")) w.dispose();
        }
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