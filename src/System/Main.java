// Ofer Avioz, 212052385
// This is the main class in the program.
// This class initializes all the system objects,
// the general manager class, and the GUI class with its components.
// I hope the project meets the requirements and works well, Thank you!
// Enjoy :)

// --- Project check list ---
// Base objects: UserProfile, Company, JobPosition, Event, Document
// Association classes: ApplyFor, Contact, NotifyAbout, Publishes, Stores
// Threads: AddApplicationTask, AddEventThread
// System controller class: JobTracker (all logic + data management + a little GUI support)
// External files: Company data (save and load), Documents (load only, saved for the user)
// GUI class and components: JobTrackerGUI ->
// AWT requirements: button, Label, Checkbox, list, Choice + Menbar, menu, menuItem, popupMenu (all in documents + menubar)

package System;
import Model.*;
import javax.swing.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class Main {

    private static final String COMPANIES_FILE = "data/companies.txt";

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
            ApplyFor app1 = tracker.addApplication(demo, p1, c1, LocalDate.of(2025,10,1), "LinkedIn", "Looking forward to this opportunity!");
            app1.setDateApplied(LocalDateTime.now().minusDays(15));

            Company c2 = new Company("Intel", "Hi-Tech", "https://www.intel.com/content/www/us/en/homepage.html");
            JobPosition p2 = new JobPosition("P22222", "SW Engineer student", "Hi-Tech", "Tel Aviv", "Intern", "Active", "Software development internship.");
            ApplyFor app2 = tracker.addApplication(demo, p2, c2, LocalDate.of(2025,12,20) ,"LinkedIn", "Very important position for my career.");
            app2.setDateApplied(LocalDateTime.now().minusDays(7));
            tracker.addOrEditContactForPosition(demo, p2.getPositionID(), "Noy Cohen", "HR Manager", "noy123@walla.com", "054-1234567");
            tracker.logLastContactForPosition(demo, p2.getPositionID(), "Email", "Follow-up after submitting CV", LocalDateTime.now().minusDays(2));

            Company c3 = new Company("Google", "Hi-Tech", "https://www.google.com/intl/en/about/");
            JobPosition p3 = new JobPosition("P33333", "Backend Developer", "Hi-Tech", "Tel Aviv", "Full-time", "Active", "Work on scalable backend systems.");
            ApplyFor app3 = tracker.addApplication(demo, p3, c3, LocalDate.of(2026,1,1), "Company Website", "");
            app3.setDateApplied(LocalDateTime.now().minusDays(1));
            tracker.addOrEditContactForPosition(demo, p3.getPositionID(), "Lior Azulay", "Recruiter", "lioraz@gmail.com", "052-1112222");
            tracker.logLastContactForPosition(demo, p3.getPositionID(), "Phone", "Initial phone screening", LocalDateTime.of(2026,1,2,12,15));

            tracker.uploadDocument(demo, "LinkedIn Profile", "URL", "https://www.linkedin.com/in/oferavioz/", "Profile link on LinkedIn", true);
            tracker.uploadDocument(demo, "English CV", "File", "/Users/ofera/Desktop/Ofer Avioz - CV + gradesheet.pdf", "My CV + grade sheet",true);
            tracker.uploadDocument(demo, "GitHub", "URL", "https://github.com/oferavioz", "My GitHub profile", false);

            tracker.addEventToCalendar(demo, new Event("Interview", "Intel technical interview", LocalDateTime.of(2026,1,28, 11,0), 120, "Prepare for coding questions."));
            tracker.addEventToCalendar(demo, new Event("Follow-up", "Follow-up with Google recruiter", LocalDateTime.now().plusHours(22), 20, "Send thank you email after interview."));
            tracker.addEventToCalendar(demo, new Event("Phone Call", "Phone screen with Matrix recruiter", LocalDateTime.now().plusHours(24), 120, "Prepare for phone interview."));

            //new JobTrackerGUI(tracker);


            JobTrackerGUI gui = new JobTrackerGUI(tracker);

            // === DEMO: uncomment ONE part at a time ===
            //Demo.part1(gui); // Part 1: Open + Register + Login
            //Demo.part2(gui); // Part 2: Menu + Profile + Edit Profile + Change Password
            //Demo.part3(gui); // Part 3: Documents (add, show details, edit, show as : links/files/primary, remove)
            //Demo.part4(gui); // Part 4: Add Application + Processes + Details actions
            //Demo.part5(gui); // Part 5: Events (add/edit/remove)
            //Demo.part6(gui); // Part 6: Notifications
            //Demo.part7(gui); // Part 7: Statistics + Export

            // MenuBar : I wanted to show menubar functionality in the demo but unfortunately,
            // it is not possble to do it automatically, so please test it manually. Thank you :)
            // MenuBar guide :
            // FILE -

        });
    }
}