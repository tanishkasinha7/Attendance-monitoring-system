
package model;

public class AttendanceRecord {
    private String rollNo;
    private String name;
    private String subject;
    private double attended;
    private double conducted;

    public AttendanceRecord(String rollNo, String name, String subject,
                            double attended, double conducted) {
        this.rollNo = rollNo;
        this.name = name;
        this.subject = subject;
        this.attended = attended;
        this.conducted = conducted;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public double getAttended() {
        return attended;
    }

    public double getConducted() {
        return conducted;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPercentage() {
        return conducted <= 0 ? 0 : attended / conducted * 100;
    }

    public String getStatus() {
        return getPercentage() >= 75 ? "Eligible" : "Shortage";
    }

    public int getRequiredClasses(double minimum) {
        if (getPercentage() >= minimum) {
            return 0;
        }

        double required =
                (minimum * conducted - 100 * attended) / (100 - minimum);

        return Math.max(0, (int) Math.ceil(required - 1e-10));
    }

    public double predict(int futureClasses, int plannedAttendance) {
        if (futureClasses < 0 || plannedAttendance < 0
                || plannedAttendance > futureClasses) {
            throw new IllegalArgumentException("Invalid future attendance.");
        }

        double total = conducted + futureClasses;
        return total == 0 ? 0 : (attended + plannedAttendance) / total * 100;
    }

    public void updateAttendance(double attended, double conducted) {
        if (conducted <= 0 || attended < 0 || attended > conducted) {
            throw new IllegalArgumentException("Invalid attendance values.");
        }

        this.attended = attended;
        this.conducted = conducted;
    }
}
