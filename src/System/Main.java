// ======================
// Ofer Avioz, 212052385
// ======================

// --- PROJECT CHECK LIST ---
// Base objects: UserProfile, Company, JobPosition, Event, Document
// Association classes: ApplyFor, Contact, NotifyAbout, Publishes, Stores
// Threads: AddApplicationTask, AddEventThread
// System controller class: JobTracker (all logic + data management + a little GUI support)
// External files: Company data (save and load), Documents (load only, saved for the user), Statistics export (txt + HTML)
// GUI class and components: JobTrackerGUI ->
// AWT requirements: button, Label, Checkbox, list, Choice + Menubar, menu, menuItem, popupMenu (all in documents + menubar)
// Everything else is implemented by swing components (more flexible and comfortable to use)..

// IN ADDITION - I built a demo class that automatically demonstrates the program features step by step.
// really enjoyed building it, used AI help to do this, hope you like it :)


// This is the main class in the program.
// This class initializes all the system objects,
// the general manager class, and the GUI class with its components.
// I hope the project meets the requirements and works well, Thank you!
// Enjoy :)


package System;
import Model.*;
import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class Main {

    private static final Path COMPANIES_FILE = JobTracker.dataFile("companies.txt");

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JobTracker tracker = new JobTracker();
            try {
                tracker.loadCompaniesFromFile(COMPANIES_FILE);
            } catch (Exception e) {
                System.err.println("Error loading companies: " + e.getMessage());
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    tracker.saveCompaniesToFile(COMPANIES_FILE);
                    System.out.println("Saved companies to: " + COMPANIES_FILE);
                } catch (IOException e) {
                    System.err.println("Error saving companies: " + e.getMessage());
                }
            }));

            UserProfile demo = new UserProfile("Ofer Avioz", "oferavioz@gmail.com", "123456789", "054-7829888", "Hi-Tech");
            tracker.addUser(demo);

            Company c1 = new Company("Matrix", "Hi-Tech", "https://www.matrix.co.il");
            JobPosition p1 = new JobPosition("P11111", "Junior Dev", "Hi-Tech", "Kfar Saba", "Full-time", "Active", "Develop and maintain software applications.");
            // publish date is > 60 days ago, so it will send a system notification about old posting automatically for the user
            ApplyFor app1 = tracker.addApplication(demo, p1, c1, LocalDate.of(2025,10,1), "LinkedIn", "Looking forward to this opportunity!");
            app1.setDateApplied(LocalDateTime.now().minusDays(15));
            tracker.updateApplicationStage(demo, p1.getPositionID(), ApplicationStage.HOME_ASSIGNMENT);

            Company c2 = new Company("Intel", "Hi-Tech", "https://www.intel.com/content/www/us/en/homepage.html");
            JobPosition p2 = new JobPosition("P22222", "SW Engineer student", "Hi-Tech", "Tel Aviv", "Intern", "Active", "Software development internship.");
            ApplyFor app2 = tracker.addApplication(demo, p2, c2, LocalDate.of(2025,12,20) ,"LinkedIn", "Very important position for my career.");
            app2.setDateApplied(LocalDateTime.now().minusDays(7));
            tracker.addOrEditContactForPosition(demo, p2.getPositionID(), "Noy Cohen", "HR Manager", "noy123@walla.com", "054-1234567");
            tracker.logLastContactForPosition(demo, p2.getPositionID(), "Email", "Follow-up after submitting CV", LocalDateTime.now().minusDays(2));

            JobPosition p3 = new JobPosition("P33333", "Frontend Developer", "Hi-Tech", "Haifa", "Full-time", "Active", "Work on scalable frontend systems.");
            ApplyFor app3 = tracker.addApplication(demo, p3, c2, LocalDate.of(2026,1,1), "Company Website", "");
            app3.setDateApplied(LocalDateTime.now().minusDays(3));
            tracker.addOrEditContactForPosition(demo, p3.getPositionID(), "Dana Levi", "Recruiter", "DanaL@walla.com", "053-3334444");
            tracker.logLastContactForPosition(demo, p3.getPositionID(), "Phone", "Initial phone screening", LocalDateTime.now().minusDays(1));
            tracker.updateApplicationStage(demo, p3.getPositionID(), ApplicationStage.PHONE_CALL);

            Company c3 = new Company("Google", "Hi-Tech", "https://www.google.com/intl/en/about/");
            JobPosition p4 = new JobPosition("P44444", "Backend Developer", "Hi-Tech", "Tel Aviv", "Part-time", "Active", "Work on scalable backend systems.");
            ApplyFor app4 = tracker.addApplication(demo, p4, c3, LocalDate.of(2026,1,1), "Company Website", "");
            app4.setDateApplied(LocalDateTime.now().minusDays(1));
            tracker.addOrEditContactForPosition(demo, p4.getPositionID(), "Lior Azulay", "Recruiter", "lioraz@gmail.com", "052-1112222");
            tracker.logLastContactForPosition(demo, p4.getPositionID(), "Phone", "Initial phone screening", LocalDateTime.of(2026,1,2,12,15));

            Company c4 = new Company("Facebook", "Hi-Tech", "https://about.facebook.com/");
            JobPosition p5 = new JobPosition("P55555", "Data Scientist", "Hi-Tech", "Tel Aviv", "Full-time", "Active", "Analyze and interpret complex data.");
            ApplyFor app5 = tracker.addApplication(demo, p5, c4, LocalDate.of(2025,12,7), "Referral", "Excited about this role!");
            app5.setDateApplied(LocalDateTime.now().minusDays(20));
            tracker.updateApplicationStage(demo, p5.getPositionID(), ApplicationStage.REJECTED);
            tracker.addOrEditContactForPosition(demo, p5.getPositionID(), "Roni Shaked", "HR Specialist", "roni11@gmail.com", "054-5556666");
            tracker.logLastContactForPosition(demo, p5.getPositionID(), "Email", "Received rejection email", LocalDateTime.now().minusDays(5));

            tracker.uploadDocument(demo, "LinkedIn Profile", "URL", "https://www.linkedin.com/in/oferavioz/", "Profile link on LinkedIn", true);
            Path cvPath = JobTracker.dataFile("Ofer Avioz - CV + gradesheet.pdf"); //It's my real CV - Take a look :)
            tracker.uploadDocument(demo, "English CV", "File", cvPath.toString(), "My CV + grade sheet",true);
            tracker.uploadDocument(demo, "GitHub", "URL", "https://github.com/oferavioz", "My GitHub profile", false);

            tracker.addEventToCalendar(demo, new Event("Interview", "Intel technical interview", LocalDateTime.now().plusDays(9).minusHours(2).minusMinutes(12), 120, "Prepare for coding questions."));
            tracker.addEventToCalendar(demo, new Event("Follow-up", "Follow-up with Google recruiter", LocalDateTime.now().plusHours(22), 20, "Send thank you email after interview."));
            tracker.addEventToCalendar(demo, new Event("Phone Call", "Phone screen with Matrix recruiter", LocalDateTime.now().plusHours(24), 120, "Prepare for phone interview."));
            tracker.addEventToCalendar(demo, new Event("Deadline", "Submitting home-assignment", LocalDateTime.now().plusDays(5).withHour(10).withMinute(0), 10, "Complete and submit the assignment - no later than 10 AM."));

            // To open gui manually, and run everything by hand - uncomment the next line:
            new JobTrackerGUI(tracker);

            // === DEMO: uncomment ONE part at a time ===
            // --- Instructions ---
            // When running each part, please wait for the current action to complete!
            // Do not click, touch, or exit pages until demo says that the part is complete.
            // You dont need to do anything, the demo will perform all actions automatically.
            // When the demonstration of the part is complete, you will receive a message dialog
            // and you can exit the part window and proceed to the next part.
            // Enjoy the ride :)

            //JobTrackerGUI gui = new JobTrackerGUI(tracker); // Initialize GUI first for demo use, then open manually part by part

            //Demo.part1(gui); // Part 1: Open + Register + Login
            //Demo.part2(gui); // Part 2: Menu + Profile + Edit Profile + Change Password
            //Demo.part3(gui); // Part 3: Documents - add, show details, edit, show as : links/files/primary, remove
            //Demo.part4(gui); // Part 4: Add Application + Processes + Details actions + edit + remove
            //Demo.part5(gui); // Part 5: Events - add event + show details + edit + remove
            //Demo.part6(gui); // Part 6: Notifications - how it works + show notifications, add notification (new event)
            //Demo.part7(gui); // Part 7: Statistics + Export

            //-------------------------------------------------------------------------------------
            // MenuBar : I wanted to show menubar functionality in the demo but unfortunately,
            // it is not possble to do it automatically (or not automatically), so please test it manually. Thank you :)
            // MenuBar guide :
            // FILE - exit (closes the program, saves data to files), export statistics (to txt file or HTML), logout (logs out current user)
            // EDIT - clear selections in current panel, reset current form fields
            // VIEW - show debug logs (console output of actions performed)
            // HELP - about (shows short info about project), short instructions (shows brief guide on how to use the program- basic functions)
            // GO TO - navigation mini menu to quickly switch between main panels (main, personal area, documents, applications, calendar, notifications, statistics)

        });
    }
}