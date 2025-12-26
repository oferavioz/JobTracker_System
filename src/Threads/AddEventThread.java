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
    private final Event event; //holds eventID

    private NotifyAbout notification;
    private boolean success;
    private String errorMessage;

    private final Object addLock = new Object();
    private volatile boolean addDone = false;
    private volatile boolean addOk = false;
    private volatile String addError = "";

    private LocalDateTime lastSeenEventDateTime = null;
    private boolean triggeredForThisSchedule = false;

    public AddEventThread(JobTracker tracker, UserProfile user, Event event) {
        this.tracker = tracker;
        this.user = user;
        this.event = event;
        this.success = false;
        this.errorMessage = "";
        setDaemon(true);
    }

    @Override
    public void run() {
        //Add phase: add event + create notification
        try {
            tracker.addEventToCalendar(user, event);
            addOk = true;
            notification = findNotificationForEvent();
        } catch (Exception ex) {
            addOk = false;
            addError = (ex.getMessage() == null) ? "Add event failed." : ex.getMessage();
        } finally {
            synchronized (addLock) {
                addDone = true;
                addLock.notifyAll();
            }
        }
        if (!addOk) {
            success = false;
            errorMessage = addError;
            return;
        }

        //keep running; if time changes then allow re-trigger
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Event live = tracker.getUserEventById(user, event.getEventID());
                if (live == null) {
                    //event was deleted/cancelled
                    success = true;
                    return;
                }
                LocalDateTime dt = live.getDateTime();
                if (dt == null) {
                    success = false;
                    errorMessage = "Event date/time is missing.";
                    return;
                }
                // if rescheduled - reset trigger
                if (lastSeenEventDateTime == null || !dt.equals(lastSeenEventDateTime)) {
                    lastSeenEventDateTime = dt;
                    triggeredForThisSchedule = false;
                }
                //sleep until 24h before event
                LocalDateTime triggerTime = dt.minusHours(24);
                long ms = Duration.between(LocalDateTime.now(), triggerTime).toMillis();
                if (ms > 0) {
                    Thread.sleep(Math.min(ms, 30_000));
                    continue;
                }
                //time to check triggering
                NotifyAbout current = findNotificationForEvent();

                //trigger once per schedule
                if (!triggeredForThisSchedule && current != null && current.shouldTrigger()) {
                    current.markAsTriggered();
                    triggeredForThisSchedule = true;
                }
                Thread.sleep(30_000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            success = false;
            errorMessage = "Thread interrupted.";
        } catch (Exception ex) {
            success = false;
            errorMessage = (ex.getMessage() == null) ? "Unexpected error." : ex.getMessage();
        }
    }

    public void awaitAddResult() throws InterruptedException {
        synchronized (addLock) {
            while (!addDone) addLock.wait();
        }
    }

    public boolean isAddOk() { return addOk; }
    public String getAddError() { return addError; }

    private NotifyAbout findNotificationForEvent() {
        Vector<NotifyAbout> list = tracker.listNotifications(user);
        for (NotifyAbout n : list) {
            if (n == null || n.getEvent() == null) continue;
            if (n.getEvent().getEventID() == event.getEventID()) {
                return n;
            }
        }
        return null;
    }

    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public NotifyAbout getNotification() { return notification; }
}