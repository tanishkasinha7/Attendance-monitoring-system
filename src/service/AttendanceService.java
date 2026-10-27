
package service;

import model.AttendanceRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AttendanceService {

    private final List<AttendanceRecord> records = new ArrayList<>();
    private File dataFile = new File("attendance.csv");

    public List<AttendanceRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public File getDataFile() {
        return dataFile;
    }

    private String clean(String value) {
        return value.replace("\uFEFF", "").trim();
    }

    private double parseNumber(String value, String field, int lineNo)
            throws IOException {
        try {
            double number = Double.parseDouble(clean(value));
            if (!Double.isFinite(number)) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid " + field + " at line "
                    + lineNo + ": [" + value + "]");
        }
    }

    public void importCSV(File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("CSV file not found.");
        }

        List<AttendanceRecord> imported = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(
                file.toPath(), StandardCharsets.UTF_8)) {

            String line;
            int lineNo = 0;

            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.replace("\uFEFF", "").trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (lineNo == 1 &&
                        line.toLowerCase(Locale.ROOT).startsWith("rollno,")) {
                    continue;
                }

                String[] data = line.split(",", -1);

                if (data.length != 5) {
                    throw new IOException("Invalid CSV format at line "
                            + lineNo + ". Expected 5 columns.");
                }

                String rollNo = clean(data[0]);
                String name = clean(data[1]);
                String subject = clean(data[2]);
                double attended = parseNumber(data[3], "attended", lineNo);
                double conducted = parseNumber(data[4], "conducted", lineNo);

                if (rollNo.isEmpty() || name.isEmpty() || subject.isEmpty()) {
                    throw new IOException("Missing details at line " + lineNo);
                }

                if (attended < 0 || conducted <= 0 || attended > conducted) {
                    throw new IOException("Invalid attendance at line " + lineNo);
                }

                imported.add(new AttendanceRecord(
                        rollNo, name, subject, attended, conducted));
            }
        }

        if (imported.isEmpty()) {
            throw new IOException("No attendance records found.");
        }

        records.clear();
        records.addAll(imported);
        dataFile = file;
    }

    public void addRecord(AttendanceRecord record) throws IOException {

        for (AttendanceRecord r : records) {
            if (r.getRollNo().equalsIgnoreCase(record.getRollNo())) {
                throw new IOException(
                        "A student with this roll number already exists."
                );
            }
        }

        records.add(record);

        try {
            autoSave();
        } catch (IOException e) {
            records.remove(record);
            throw e;
        }
    }
    public void updateRecord(String rollNo, String subject, String name,
                             double attended, double conducted)
            throws IOException {

        AttendanceRecord record = findRecord(rollNo, subject);

        if (record == null) {
            throw new IOException("Record not found.");
        }

        String oldName = record.getName();
        double oldAttended = record.getAttended();
        double oldConducted = record.getConducted();

        record.setName(name);
        record.updateAttendance(attended, conducted);

        try {
            autoSave();
        } catch (IOException e) {
            record.setName(oldName);
            record.updateAttendance(oldAttended, oldConducted);
            throw e;
        }
    }

    public void deleteRecord(String rollNo, String subject)
            throws IOException {

        AttendanceRecord record = findRecord(rollNo, subject);

        if (record == null) {
            throw new IOException("Record not found.");
        }

        records.remove(record);

        try {
            autoSave();
        } catch (IOException e) {
            records.add(record);
            throw e;
        }
    }

    private AttendanceRecord findRecord(String rollNo, String subject) {
        for (AttendanceRecord record : records) {
            if (record.getRollNo().equalsIgnoreCase(rollNo.trim())
                    && record.getSubject().equalsIgnoreCase(subject.trim())) {
                return record;
            }
        }
        return null;
    }

    public void autoSave() throws IOException {
        if (dataFile == null) {
            throw new IOException("No CSV file selected.");
        }

        File parent = dataFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create the data folder.");
        }

        try (PrintWriter writer = new PrintWriter(
                new FileWriter(dataFile, StandardCharsets.UTF_8))) {

            writer.println("rollNo,name,subject,attended,conducted");

            for (AttendanceRecord r : records) {
                writer.printf(Locale.US, "%s,%s,%s,%.2f,%.2f%n",
                        csv(r.getRollNo()), csv(r.getName()),
                        csv(r.getSubject()), r.getAttended(), r.getConducted());
            }

            if (writer.checkError()) {
                throw new IOException("Could not save the CSV file.");
            }
        }
    }

    private String csv(String value) {
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public void saveReport(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new FileWriter(file, StandardCharsets.UTF_8))) {

            writer.println(
                    "RollNo,Name,Subject,Attended,Conducted,Percentage,Status,RequiredClasses");

            for (AttendanceRecord r : records) {
                writer.printf(Locale.US, "%s,%s,%s,%.2f,%.2f,%.2f,%s,%d%n",
                        csv(r.getRollNo()), csv(r.getName()), csv(r.getSubject()),
                        r.getAttended(), r.getConducted(), r.getPercentage(),
                        r.getStatus(), r.getRequiredClasses(75));
            }

            if (writer.checkError()) {
                throw new IOException("Could not save the report.");
            }
        }
    }
}
