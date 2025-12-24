package Threads;

import Model.Event;
import Model.NotifyAbout;
import Model.UserProfile;
import System.JobTracker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Vector;

public class AddEventThread extends Thread {

    private final JobTracker tracker;
    private final UserProfile user;
    private final Event event;

    private NotifyAbout notification;
    private boolean success;
    private String errorMessage;

    public AddEventThread(JobTracker tracker, UserProfile user, Event event) {
        this.tracker = tracker;
        this.user = user;
        this.event = event;
        this.success = false;
        this.errorMessage = "";
    }

    @Override
    public void run() {
        try {
            //add event + creates notification
            synchronized (tracker) {
                tracker.addEventToCalendar(user, event);
                notification = findNotificationForEvent();
            }
            //wait until 24h before the event
            sleepUntil24HoursBefore();
            //trigger notification
            synchronized (tracker) {
                if (notification != null && notification.shouldTrigger()) {
                    notification.markAsTriggered();
                }
            }

            success = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            success = false;
            errorMessage = "Thread interrupted.";
        } catch (Exception ex) {
            success = false;
            errorMessage = ex.getMessage();
        }
    }

    private NotifyAbout findNotificationForEvent() {
        Vector<NotifyAbout> list = tracker.viewNotifications(user);
        for (NotifyAbout n : list) {
            if (n != null && n.getEvent() == event) {
                return n;
            }
        }
        return null;
    }

    private void sleepUntil24HoursBefore() throws InterruptedException {
        if (event == null || event.getDateTime() == null) {
            return;
        }
        LocalDateTime triggerTime = event.getDateTime().minusHours(24);
        long ms = Duration.between(LocalDateTime.now(), triggerTime).toMillis();
        if (ms > 0) {
            Thread.sleep(ms);
        }
    }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public NotifyAbout getNotification() { return notification; }
}