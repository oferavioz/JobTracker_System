package Model;

import java.util.ArrayList;

public class Company
{
    private String companyName;
    private String industry;
    private String websiteURL;
    private ArrayList<String> branches; // list of branch locations

    public Company(String companyName, String industry, String websiteURL)
    {
        setCompanyName(companyName);
        setIndustry(industry);
        setWebsiteURL(websiteURL);
        this.branches = new ArrayList<>();
    }
    public Company(String companyName, String industry, String websiteURL, ArrayList<String> branches) {
        setCompanyName(companyName);
        setIndustry(industry);
        setWebsiteURL(websiteURL);
        setBranches(branches);
    }

    // Setters
    public void setCompanyName(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name cannot be null or empty.");
        }
        if (!companyName.matches("^[A-Za-z0-9 .,&\\-']+$")) {
            throw new IllegalArgumentException("Company name contains invalid characters.");
        }
        String s = companyName.trim().replaceAll("\\s+", " ").toLowerCase();
        StringBuilder out = new StringBuilder(s.length());
        boolean capNext = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                out.append(c);
                capNext = true;
                continue;
            }
            if (capNext && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                capNext = false;
            } else {
                out.append(c);
                capNext = false;
            }
        }
        this.companyName = out.toString();
    }

    public void setIndustry(String industry) {
        if (industry == null || industry.trim().isEmpty()) {
            throw new IllegalArgumentException("Industry cannot be null or empty.");
        }
        String ind = industry.trim().replaceAll("\\s+", " ");
        this.industry = ind;
    }

    public void setWebsiteURL(String websiteURL) {
        if (websiteURL == null || websiteURL.trim().isEmpty()) {
            throw new IllegalArgumentException("Website URL cannot be null or empty.");
        }
        String url = websiteURL.trim();
        //must contain a dot and no spaces
        if (url.contains(" ") || !url.contains(".")) {
            throw new IllegalArgumentException("Invalid website URL.");
        }
        this.websiteURL = url;
    }

    public void setBranches(ArrayList<String> branches) {
        this.branches = (branches == null) ? new ArrayList<>() : new ArrayList<>(branches);
    }

    // Getters
    public String getCompanyName() {
        return companyName;
    }

    public String getIndustry() {
        return industry;
    }

    public String getWebsiteURL() {
        return websiteURL;
    }

    public ArrayList<String> getBranches() {
        return branches;
    }

    // toString
    @Override public String toString() {
        return companyName + " | " + industry + " | " + websiteURL + " | Branches: " + branches;
    }

    // Methods
    public boolean hasBranch(String location) {
        if (location == null || branches == null) return false;
        String target = location.trim().replaceAll("\\s+", " ");
        if (target.isEmpty()) return false;
        for (String b : branches) {
            if (b != null && b.equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    // For writing to a file requirement
    // To save/load data reliably (System.JobTracker will do the actual file operations).
    public String toFileLine() {
        String b = (branches == null || branches.isEmpty()) ? "" : String.join(";", branches);
        return companyName + "|" + industry + "|" + websiteURL + "|" + b;
    }

    public static Company fromFileLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty company line.");
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid company line format.");
        }
        String name = parts[0];
        String ind = parts[1];
        String url = parts[2];

        ArrayList<String> br = new ArrayList<>();
        if (!parts[3].isEmpty()) {
            String[] arr = parts[3].split(";");
            for (String x : arr) {
                br.add(x);
            }
        }
        Company c = new Company(name, ind, url);
        c.setBranches(br);
        return c;
    }
}