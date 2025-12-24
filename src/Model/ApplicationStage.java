package Model;

public enum ApplicationStage
{
    APPLIED,
    PHONE_CALL,
    INTERVIEW,
    HOME_ASSIGNMENT,
    OFFER,
    REJECTED,
    WITHDRAWN;

    public String getDisplayName() {
        switch (this) {
            case APPLIED: return "Applied";
            case PHONE_CALL: return "Phone Call";
            case INTERVIEW: return "Interview";
            case HOME_ASSIGNMENT: return "Home Assignment";
            case OFFER: return "Offer";
            case REJECTED: return "Rejected";
            case WITHDRAWN: return "Withdrawn";
            default: return this.name();
        }
    }
}