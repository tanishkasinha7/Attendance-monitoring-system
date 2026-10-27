
package ui;

import model.AttendanceRecord;
import service.AttendanceService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Main extends JFrame {

    private final AttendanceService service = new AttendanceService();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Roll No", "Name", "Subject", "Attended",
                    "Conducted", "Percentage", "Status", "Required Classes"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable table = new JTable(tableModel);

    public Main() {
        setTitle("Attendance Monitoring & Shortage Prediction System");
        setSize(1050, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JLabel heading = new JLabel(
                "Attendance Monitoring & Shortage Prediction System",
                SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 20));
        heading.setBorder(BorderFactory.createEmptyBorder(12, 5, 12, 5));
        add(heading, BorderLayout.NORTH);

        table.setRowHeight(25);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 4, 8, 8));

        addButton(buttons, "Import Records", this::importRecords);
        addButton(buttons, "View All", this::refreshTable);
        addButton(buttons, "Add Student", this::addRecord);
        addButton(buttons, "Update Attendance", this::updateRecord);
        addButton(buttons, "Delete Record", this::deleteRecord);
        addButton(buttons, "Check Shortage", this::showShortages);
        addButton(buttons, "Predict Attendance", this::predictAttendance);
        addButton(buttons, "Save Report", this::saveReport);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 8, 10, 8));
        bottom.add(buttons, BorderLayout.CENTER);

        add(bottom, BorderLayout.SOUTH);

        // Load the default CSV file automatically when it exists.
        File defaultFile = service.getDataFile();
        if (defaultFile.isFile()) {
            try {
                service.importCSV(defaultFile);
                refreshTable();
            } catch (IOException e) {
                showError(e.getMessage());
            }
        }
    }

    private void addButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        panel.add(button);
    }

    private void importRecords() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Attendance CSV File");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                service.importCSV(chooser.getSelectedFile());
                refreshTable();
                JOptionPane.showMessageDialog(this,
                        "Records imported successfully.");
            } catch (IOException e) {
                showError(e.getMessage());
            }
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);

        for (AttendanceRecord r : service.getRecords()) {
            tableModel.addRow(new Object[]{
                    r.getRollNo(),
                    r.getName(),
                    r.getSubject(),
                    r.getAttended(),
                    r.getConducted(),
                    String.format("%.2f%%", r.getPercentage()),
                    r.getStatus(),
                    r.getRequiredClasses(75)
            });
        }
    }

    private void addRecord() {
        JTextField rollField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField subjectField = new JTextField();
        JTextField attendedField = new JTextField();
        JTextField conductedField = new JTextField();

        Object[] fields = {
                "Roll Number:", rollField,
                "Student Name:", nameField,
                "Subject:", subjectField,
                "Classes Attended:", attendedField,
                "Classes Conducted:", conductedField
        };

        int choice = JOptionPane.showConfirmDialog(
                this, fields, "Add Attendance Record",
                JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String roll = rollField.getText().trim();
            String name = nameField.getText().trim();
            String subject = subjectField.getText().trim();

            if (roll.isEmpty() || name.isEmpty() || subject.isEmpty()) {
                throw new IllegalArgumentException(
                        "Roll number, name, and subject are required.");
            }

            double attended = Double.parseDouble(
                    attendedField.getText().trim());
            double conducted = Double.parseDouble(
                    conductedField.getText().trim());

            validateAttendance(attended, conducted);

            service.addRecord(new AttendanceRecord(
                    roll, name, subject, attended, conducted));

            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Record added and saved successfully.");

        } catch (NumberFormatException e) {
            showError("Enter valid numeric attendance values.");
        } catch (IllegalArgumentException | IOException e) {
            showError(e.getMessage());
        }
    }

    private void updateRecord() {
        int row = table.getSelectedRow();

        if (row == -1) {
            showError("Select a record from the table first.");
            return;
        }

        String roll = tableModel.getValueAt(row, 0).toString();
        String oldName = tableModel.getValueAt(row, 1).toString();
        String subject = tableModel.getValueAt(row, 2).toString();

        JTextField nameField = new JTextField(oldName);
        JTextField attendedField = new JTextField(
                tableModel.getValueAt(row, 3).toString());
        JTextField conductedField = new JTextField(
                tableModel.getValueAt(row, 4).toString());

        Object[] fields = {
                "Roll Number: " + roll,
                "Subject: " + subject,
                "Student Name:", nameField,
                "Classes Attended:", attendedField,
                "Classes Conducted:", conductedField
        };

        int choice = JOptionPane.showConfirmDialog(
                this, fields, "Update Attendance",
                JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be empty.");
            }

            double attended = Double.parseDouble(
                    attendedField.getText().trim());
            double conducted = Double.parseDouble(
                    conductedField.getText().trim());

            validateAttendance(attended, conducted);

            service.updateRecord(
                    roll, subject, name, attended, conducted);

            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Record updated and saved successfully.");

        } catch (NumberFormatException e) {
            showError("Enter valid numeric attendance values.");
        } catch (IllegalArgumentException | IOException e) {
            showError(e.getMessage());
        }
    }

    private void deleteRecord() {
        int row = table.getSelectedRow();

        if (row == -1) {
            showError("Select a record from the table first.");
            return;
        }

        String roll = tableModel.getValueAt(row, 0).toString();
        String name = tableModel.getValueAt(row, 1).toString();
        String subject = tableModel.getValueAt(row, 2).toString();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete " + name + "'s " + subject + " record?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.deleteRecord(roll, subject);
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Record deleted and changes saved.");
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    private void showShortages() {
        tableModel.setRowCount(0);

        for (AttendanceRecord r : service.getRecords()) {
            if (r.getPercentage() < 75) {
                addTableRow(r);
            }
        }

        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No students have an attendance shortage.");
        }
    }

    private void addTableRow(AttendanceRecord r) {
        tableModel.addRow(new Object[]{
                r.getRollNo(), r.getName(), r.getSubject(),
                r.getAttended(), r.getConducted(),
                String.format("%.2f%%", r.getPercentage()),
                r.getStatus(), r.getRequiredClasses(75)
        });
    }

    private void predictAttendance() {
        int row = table.getSelectedRow();

        if (row == -1) {
            showError("Select a student-subject record first.");
            return;
        }

        String roll = tableModel.getValueAt(row, 0).toString();
        String subject = tableModel.getValueAt(row, 2).toString();

        AttendanceRecord selected = null;

        for (AttendanceRecord r : service.getRecords()) {
            if (r.getRollNo().equals(roll)
                    && r.getSubject().equalsIgnoreCase(subject)) {
                selected = r;
                break;
            }
        }

        if (selected == null) {
            showError("Record not found. Click View All and try again.");
            return;
        }

        String futureText = JOptionPane.showInputDialog(
                this, "How many future classes will be conducted?");
        if (futureText == null) {
            return;
        }

        String plannedText = JOptionPane.showInputDialog(
                this, "In how many of those classes will the student attend?");
        if (plannedText == null) {
            return;
        }

        try {
            int future = Integer.parseInt(futureText.trim());
            int planned = Integer.parseInt(plannedText.trim());

            double result = selected.predict(future, planned);

            JOptionPane.showMessageDialog(this,
                    String.format("Predicted attendance: %.2f%%", result));
        } catch (NumberFormatException e) {
            showError("Enter whole numbers for future classes.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void saveReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Attendance Report");
        chooser.setSelectedFile(new File("attendance_report.csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            if (file.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                        this, "This file already exists. Replace it?",
                        "Confirm Save", JOptionPane.YES_NO_OPTION);

                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                service.saveReport(file);
                JOptionPane.showMessageDialog(this,
                        "Report saved successfully.");
            } catch (IOException e) {
                showError(e.getMessage());
            }
        }
    }

    private void validateAttendance(double attended, double conducted) {
        if (!Double.isFinite(attended) || !Double.isFinite(conducted)
                || conducted <= 0 || attended < 0 || attended > conducted) {
            throw new IllegalArgumentException(
                    "Attended classes must be at least 0 and no greater "
                            + "than conducted classes. Conducted classes "
                            + "must be greater than 0.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
