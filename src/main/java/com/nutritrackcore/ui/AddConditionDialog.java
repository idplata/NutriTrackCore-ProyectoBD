package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.MaskFormatter;

/**
 * Cuadro que se abre al agregar una condición médica. Pide dos datos
 * opcionales: la fecha de diagnóstico y observaciones.
 * <p>
 * El servicio solo guarda observaciones cuando hay fecha de diagnóstico, así
 * que si se escriben observaciones sin fecha se avisa antes de cerrar.
 */
public class AddConditionDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final int WIDTH = 380;

    private final JFormattedTextField dateField = new JFormattedTextField(dateMask());
    private final JTextArea notesArea = new JTextArea(3, 20);
    private boolean accepted;
    private LocalDate diagnosisDate;
    private String notes;

    private AddConditionDialog(Window owner, String conditionName) {
        super(owner, "Agregar condición", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = ScreenKit.column();
        content.setOpaque(true);
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));

        content.add(ScreenKit.left(ScreenKit.label(conditionName, Theme.font(Font.BOLD, 20), Theme.TEXT)));
        content.add(Box.createVerticalStrut(4));
        content.add(ScreenKit.left(ScreenKit.wrapped("Estos datos son opcionales. Puedes dejarlos en blanco.",
                WIDTH - 40, Theme.font(Font.PLAIN, 13), ScreenKit.NOTE)));
        content.add(Box.createVerticalStrut(18));

        FormKit.styleField(dateField, null, Icons.calendar(20, Theme.ICON), WIDTH);
        dateField.setFocusLostBehavior(JFormattedTextField.PERSIST);
        content.add(ScreenKit.left(FormKit.fieldLabel("Fecha de diagnóstico (DD/MM/AAAA)", dateField)));
        content.add(Box.createVerticalStrut(8));
        content.add(ScreenKit.left(dateField));
        content.add(Box.createVerticalStrut(14));

        notesArea.setFont(Theme.font(Font.PLAIN, 14));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ej. controlada con medicamento");
        notesArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        FormKit.fixSize(notesScroll, WIDTH, 84);
        content.add(ScreenKit.left(FormKit.fieldLabel("Observaciones", notesArea)));
        content.add(Box.createVerticalStrut(8));
        content.add(ScreenKit.left(notesScroll));
        content.add(Box.createVerticalStrut(20));

        JButton cancel = FormKit.secondaryButton("Cancelar", null, 120);
        JButton add = FormKit.primaryButton("Agregar", null, 140);
        FormKit.fixSize(cancel, 120, 44);
        FormKit.fixSize(add, 140, 44);
        cancel.addActionListener(e -> dispose());
        add.addActionListener(e -> accept());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(add);
        JComponent buttonRow = ScreenKit.left(buttons);
        buttonRow.setMaximumSize(new Dimension(WIDTH, 44));
        content.add(buttonRow);

        getContentPane().add(content, BorderLayout.CENTER);
        getRootPane().setDefaultButton(add);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * Abre el cuadro y espera a que el usuario lo cierre.
     *
     * @param parent        componente sobre el que se centra
     * @param conditionName nombre de la condición que se agrega
     * @return el cuadro ya cerrado, para leer lo que se escribió; {@code null} si se canceló
     */
    public static AddConditionDialog ask(Component parent, String conditionName) {
        AddConditionDialog dialog = new AddConditionDialog(SwingUtilities.getWindowAncestor(parent), conditionName);
        dialog.setVisible(true);
        return dialog.accepted ? dialog : null;
    }

    /** @return fecha de diagnóstico, o {@code null} si no se escribió */
    public LocalDate getDiagnosisDate() {
        return diagnosisDate;
    }

    /** @return observaciones, o {@code null} si no se escribieron */
    public String getNotes() {
        return notes;
    }

    private void accept() {
        String rawDate = dateField.getText().replace("_", "").replace("/", "").trim();
        String rawNotes = notesArea.getText().trim();
        LocalDate date = null;
        if (!rawDate.isEmpty()) {
            try {
                date = LocalDate.parse(dateField.getText().trim(), DATE_FORMAT);
            } catch (DateTimeParseException e) {
                Messages.showInfo(this, "Escribe la fecha como DD/MM/AAAA, por ejemplo 02/05/2024.");
                dateField.requestFocusInWindow();
                return;
            }
        }
        if (date == null && !rawNotes.isEmpty()) {
            Messages.showInfo(this, "Para guardar observaciones escribe también la fecha de diagnóstico.");
            dateField.requestFocusInWindow();
            return;
        }
        this.diagnosisDate = date;
        this.notes = rawNotes.isEmpty() ? null : rawNotes;
        this.accepted = true;
        dispose();
    }

    private static MaskFormatter dateMask() {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            return mask;
        } catch (ParseException e) {
            throw new IllegalStateException(e);   // el patrón es fijo; no puede fallar
        }
    }
}
