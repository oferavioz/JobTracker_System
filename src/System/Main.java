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
// System controller class: JobTracker
// External files: Company data, Documents
// GUI class and components: JobTrackerGUI

package System;
import Model.*;
import javax.swing.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JobTracker tracker = new JobTracker();
            UserProfile demo = new UserProfile("Ofer Avioz", "oferavioz@gmail.com", "123456789", "054-7829888", "Hi-Tech");
            tracker.addUser(demo);

            Company c1 = new Company("Matrix", "Hi-Tech", "https://www.matrix.co.il");
            JobPosition p1 = new JobPosition("P12345", "Junior Dev", "Hi-Tech", "Tel Aviv", "Full-time", "Active", "Develop and maintain software applications.");
            ApplyFor app1 = tracker.addApplication(demo, p1, c1, LocalDate.of(2025,10,1), "LinkedIn", "Looking forward to this opportunity!");
            app1.setDateApplied(LocalDateTime.now().minusDays(15));

            Company c2 = new Company("Intel", "Hi-Tech", "https://www.intel.com/content/www/us/en/homepage.html");
            JobPosition p2 = new JobPosition("P11111", "SW Engineer student", "Hi-Tech", "Tel Aviv", "Intern", "Active", "Software development internship.");
            ApplyFor app2 = tracker.addApplication(demo, p2, c2, LocalDate.of(2025,12,20) ,"LinkedIn", "Very important position for my career.");
            app2.setDateApplied(LocalDateTime.now().minusDays(7));

            new JobTrackerGUI(tracker);
        });
    }
}