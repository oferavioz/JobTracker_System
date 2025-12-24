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
import Threads.*;

public class Main {
    public static void main(String[] args) {

        // 1) Initialize the system controller
        JobTracker tracker = new JobTracker();

        // 2) Initialize GUI in a separate class/file
        JobTrackerGUI gui = new JobTrackerGUI(tracker);

        // 3) Show the GUI
        gui.start();
    }
}