// Ofer Avioz, 212052385
// This is the main class in the program.
// This class initializes all the system objects,
// the general manager class, and the GUI class with its components.
// I hope the project meets the requirements and works well, Thank you!
// Enjoy :)

// Project check list-
// Base objects: UserProfile, Company, JobPosition, Event, Document
// Association classes: ApplyFor, Contact, NotifyAbout, Publishes, Stores
// Threads: AddApplicationTask, AddEventThread
// System controller class: JobTracker
// External files: Company data
// GUI class and components: JobTrackerGUI

package System;
import Model.*;
import javax.swing.*;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JobTracker tracker = new JobTracker();

            UserProfile demo = new UserProfile("Ofer Avioz", "oferavioz@gmail.com", "123456789", "054-7829888", "Hi-Tech");
            tracker.addUser(demo);

            Company c = new Company("DemoCompany", "Software", "https://demo.com");
            JobPosition p = new JobPosition(
                    "POS-001",
                    "Junior Dev",
                    "Hi-Tech",
                    "Tel Aviv",
                    "Full-time",
                    "Active",
                    "seed for overdue test"
            );

            ApplyFor app = tracker.addApplication(demo, p, c, "LinkedIn", "seeded app");
            app.setDateApplied(LocalDateTime.now().minusDays(15));

            new JobTrackerGUI(tracker);
        });
    }
}