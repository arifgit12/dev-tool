package com.ldapmanager.ui;

import com.ldapmanager.model.LdapUser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserDetailsPanel extends JPanel {
    private JTextArea detailsArea;
    private JList<String> groupsList;
    private DefaultListModel<String> groupsModel;
    private JTable attributesTable;
    private DefaultTableModel attributesTableModel;
    private JButton copyButton;
    private JButton exportButton;
    private LdapUser currentUser;

    public UserDetailsPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("User Details");
        titleLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));

        // Details Panel
        JPanel detailsPanel = new JPanel(new BorderLayout(5, 5));
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Basic Information"));
        detailsArea = new JTextArea(10, 40);
        detailsArea.setEditable(false);
        detailsArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        JScrollPane detailsScrollPane = new JScrollPane(detailsArea);
        detailsPanel.add(detailsScrollPane, BorderLayout.CENTER);

        // Groups Panel
        JPanel groupsPanel = new JPanel(new BorderLayout(5, 5));
        groupsPanel.setBorder(BorderFactory.createTitledBorder("Group Memberships"));
        groupsModel = new DefaultListModel<>();
        groupsList = new JList<>(groupsModel);
        JScrollPane groupsScrollPane = new JScrollPane(groupsList);
        groupsScrollPane.setPreferredSize(new Dimension(300, 150));
        groupsPanel.add(groupsScrollPane, BorderLayout.CENTER);

        // Attributes Panel
        JPanel attributesPanel = new JPanel(new BorderLayout(5, 5));
        attributesPanel.setBorder(BorderFactory.createTitledBorder("All Attributes"));
        String[] columns = {"Attribute", "Value"};
        attributesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        attributesTable = new JTable(attributesTableModel);
        JScrollPane attributesScrollPane = new JScrollPane(attributesTable);
        attributesPanel.add(attributesScrollPane, BorderLayout.CENTER);

        // Split panes
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            detailsPanel, groupsPanel);
        topSplitPane.setResizeWeight(0.6);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            topSplitPane, attributesPanel);
        mainSplitPane.setResizeWeight(0.5);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        copyButton = new JButton("Copy to Clipboard");
        exportButton = new JButton("Export to Excel");

        copyButton.addActionListener(e -> copyToClipboard());
        exportButton.addActionListener(e -> exportToExcel());

        buttonPanel.add(copyButton);
        buttonPanel.add(exportButton);

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(mainSplitPane, BorderLayout.CENTER);
    }

    public void displayUser(LdapUser user) {
        this.currentUser = user;

        if (user == null) {
            clearDisplay();
            return;
        }

        // Display basic info
        StringBuilder details = new StringBuilder();
        details.append("DN: ").append(user.getDn()).append("\n");
        details.append("Username: ").append(user.getUsername()).append("\n");
        details.append("CN: ").append(user.getCn()).append("\n");
        details.append("Full Name: ").append(user.getFullName()).append("\n");
        details.append("Email: ").append(user.getEmail() != null ? user.getEmail() : "N/A").append("\n");
        details.append("First Name: ").append(user.getFirstName() != null ? user.getFirstName() : "N/A").append("\n");
        details.append("Last Name: ").append(user.getLastName() != null ? user.getLastName() : "N/A").append("\n");
        details.append("Telephone: ").append(user.getTelephone() != null ? user.getTelephone() : "N/A").append("\n");
        details.append("Department: ").append(user.getDepartment() != null ? user.getDepartment() : "N/A").append("\n");
        details.append("Title: ").append(user.getTitle() != null ? user.getTitle() : "N/A").append("\n");
        details.append("Enabled: ").append(user.isEnabled() ? "Yes" : "No").append("\n");

        detailsArea.setText(details.toString());
        detailsArea.setCaretPosition(0);

        // Display groups
        groupsModel.clear();
        if (user.getGroups() != null) {
            for (String group : user.getGroups()) {
                groupsModel.addElement(group);
            }
        }

        // Display all attributes
        attributesTableModel.setRowCount(0);
        if (user.getAttributes() != null) {
            List<String> sortedKeys = new ArrayList<>(user.getAttributes().keySet());
            sortedKeys.sort(String::compareToIgnoreCase);

            for (String key : sortedKeys) {
                List<String> values = user.getAttributes().get(key);
                String valueStr = values != null ? String.join(", ", values) : "";
                attributesTableModel.addRow(new Object[]{key, valueStr});
            }
        }
    }

    private void clearDisplay() {
        detailsArea.setText("");
        groupsModel.clear();
        attributesTableModel.setRowCount(0);
        currentUser = null;
    }

    private void copyToClipboard() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this,
                "No user data to copy.",
                "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String text = detailsArea.getText();
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

        JOptionPane.showMessageDialog(this,
            "User details copied to clipboard!",
            "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportToExcel() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this,
                "No user data to export.",
                "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export User Data");
        fileChooser.setSelectedFile(new File("ldap_user_" + currentUser.getUsername() + ".xlsx"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (Workbook workbook = new XSSFWorkbook()) {
                // Basic Info Sheet
                Sheet basicSheet = workbook.createSheet("Basic Info");
                int rowNum = 0;

                String[][] basicData = {
                    {"DN", currentUser.getDn()},
                    {"Username", currentUser.getUsername()},
                    {"CN", currentUser.getCn()},
                    {"Full Name", currentUser.getFullName()},
                    {"Email", currentUser.getEmail()},
                    {"First Name", currentUser.getFirstName()},
                    {"Last Name", currentUser.getLastName()},
                    {"Telephone", currentUser.getTelephone()},
                    {"Department", currentUser.getDepartment()},
                    {"Title", currentUser.getTitle()},
                    {"Enabled", currentUser.isEnabled() ? "Yes" : "No"}
                };

                for (String[] data : basicData) {
                    Row row = basicSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(data[0]);
                    row.createCell(1).setCellValue(data[1] != null ? data[1] : "N/A");
                }

                // Groups Sheet
                Sheet groupsSheet = workbook.createSheet("Groups");
                rowNum = 0;
                Row headerRow = groupsSheet.createRow(rowNum++);
                headerRow.createCell(0).setCellValue("Group Name");

                if (currentUser.getGroups() != null) {
                    for (String group : currentUser.getGroups()) {
                        Row row = groupsSheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(group);
                    }
                }

                // Attributes Sheet
                Sheet attributesSheet = workbook.createSheet("All Attributes");
                rowNum = 0;
                headerRow = attributesSheet.createRow(rowNum++);
                headerRow.createCell(0).setCellValue("Attribute");
                headerRow.createCell(1).setCellValue("Value");

                if (currentUser.getAttributes() != null) {
                    for (Map.Entry<String, List<String>> entry : currentUser.getAttributes().entrySet()) {
                        Row row = attributesSheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(entry.getKey());
                        row.createCell(1).setCellValue(String.join(", ", entry.getValue()));
                    }
                }

                // Auto-size columns
                for (int i = 0; i < 2; i++) {
                    basicSheet.autoSizeColumn(i);
                    attributesSheet.autoSizeColumn(i);
                }
                groupsSheet.autoSizeColumn(0);

                // Write to file
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    workbook.write(outputStream);
                }

                JOptionPane.showMessageDialog(this,
                    "User data exported successfully to: " + file.getAbsolutePath(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting data: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
