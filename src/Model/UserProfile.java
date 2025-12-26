package Model;

public class UserProfile
{
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String fieldOfSearch;

    public UserProfile(String fullName, String email, String password, String phone, String fieldOfSearch)
    {
        setFullName(fullName);
        setEmail(email);
        setPassword(password);
        setPhone(phone);
        setFieldOfSearch(fieldOfSearch);
    }

    // Setters
    public void setFullName(String fullName)
    {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be null or empty.");
        }
        if (!fullName.matches("^[A-Za-z ]+$")) {
            throw new IllegalArgumentException("Full name can only contain alphabetic characters and spaces.");
        }
        String s = fullName.trim().replaceAll("\\s+", " ").toLowerCase();
        StringBuilder out = new StringBuilder(s.length());
        out.append(Character.toUpperCase(s.charAt(0)));

        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(s.charAt(i - 1) == ' ' ? Character.toUpperCase(c) : c);
        }
        this.fullName = out.toString();
    }

    public void setEmail(String email)
    {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\..+$")) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        this.email = email;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        String p = password.trim(); // so it won't count spaces
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
        this.password = password;
    }

    public void setPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }
        if (!phone.matches("^\\d{3}-\\d{7}$")) {
            throw new IllegalArgumentException("Invalid phone number format. Expected format: XXX-XXXXXXX");
        }
        this.phone = phone;
    }

    public void setFieldOfSearch(String fieldOfSearch)
    {
        if (fieldOfSearch == null || fieldOfSearch.trim().isEmpty()) {
            throw new IllegalArgumentException("Field of search cannot be null or empty.");
        }
        this.fieldOfSearch = fieldOfSearch;
    }

    // Getters
    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getFieldOfSearch() {
        return fieldOfSearch;
    }

    // toString
    @Override public String toString() {
        return fullName + " | " + email + " | " + phone + " | " + fieldOfSearch;
    }

    // Methods
    public boolean authenticate(String passwordAttempt) {
        return passwordAttempt != null && passwordAttempt.equals(this.password);
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (!authenticate(oldPassword)) {
            throw new IllegalArgumentException("Old password does not match.");
        }
        if (newPassword.equals(oldPassword)){
            throw new IllegalArgumentException("New password must be different from the old password.");
        }
        setPassword(newPassword);
    }

    public void updateProfile(String newFullName, String newPhone, String newFieldOfSearch) {
        setFullName(newFullName);
        setPhone(newPhone);
        setFieldOfSearch(newFieldOfSearch);
    }

}