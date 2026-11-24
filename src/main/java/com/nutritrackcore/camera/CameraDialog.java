package com.nutritrackcore.camera;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.model.ConsumptionRecord;
import com.nutritrackcore.services.UserSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Diálogo "Registrar con foto".
 * <p>
 * Flujo: revisa el servicio → muestra el enlace para el celular → espera la foto →
 * reconoce los alimentos → el usuario edita → guarda. Si no hay servicio, permite
 * registrar a mano. Todo lo que usa la red va en {@link SwingWorker}.
 */
public class CameraDialog extends JDialog {
    private static final int PREVIEW_SIZE = 300;

    private final FoodCaptureService service;
    private final int userId;
    private final ItemsTableModel tableModel = new ItemsTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextField linkField = new JTextField();
    private final JComboBox<String> ipCombo = new JComboBox<>();
    private final JLabel preview = new JLabel("Sin foto", SwingConstants.CENTER);
    private final JButton newPhotoButton = new JButton("Otra foto");
    private final JButton fileButton = new JButton("Elegir del disco…");
    private final JButton addButton = new JButton("Agregar alimento…");
    private final JButton changeButton = new JButton("Cambiar alimento…");
    private final JButton removeButton = new JButton("Quitar fila");
    private final JButton confirmButton = new JButton("Confirmar");
    private final JButton cancelButton = new JButton("Cancelar");

    private boolean visionAvailable;
    private boolean photoAnalyzed;
    private boolean closing;
    private SwingWorker<?, ?> photoWorker;

    /**
     * @throws NutriTrackException si no hay una sesión iniciada
     */
    public CameraDialog(Window owner, FoodCaptureService service, UserSession session) throws NutriTrackException {
        super(owner, "Registrar con foto", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.userId = session.getCurrentUser().getIdUser();
        buildLayout();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        setSize(960, 600);
        setLocationRelativeTo(owner);
    }

    /** Abre el diálogo y empieza a revisar el servicio. Llamar desde el hilo de Swing. */
    public static void open(Window owner, FoodCaptureService service, UserSession session) {
        try {
            CameraDialog dialog = new CameraDialog(owner, service, session);
            dialog.start();
            dialog.setVisible(true);
        } catch (NutriTrackException e) {
            JOptionPane.showMessageDialog(owner, e.getMessage(), "Registrar con foto", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------ pantalla

    private void buildLayout() {
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 15f));
        linkField.setEditable(false);
        linkField.setFont(linkField.getFont().deriveFont(16f));
        for (String ip : LocalNetworkAddress.privateAddresses()) {
            ipCombo.addItem(ip);
        }
        ipCombo.setToolTipText("Si el celular no abre el enlace, prueba con otra IP");
        ipCombo.addActionListener(e -> {
            if (visionAvailable && !photoAnalyzed && ipCombo.getSelectedItem() != null) {
                openPhoneLink();
            }
        });

        JPanel linkPanel = new JPanel(new BorderLayout(6, 0));
        linkPanel.add(new JLabel("Abre en el celular:"), BorderLayout.WEST);
        linkPanel.add(linkField, BorderLayout.CENTER);
        linkPanel.add(ipCombo, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        top.add(statusLabel, BorderLayout.NORTH);
        top.add(linkPanel, BorderLayout.SOUTH);

        preview.setPreferredSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE));
        preview.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setDefaultRenderer(Object.class, new WarningRenderer());
        table.setDefaultRenderer(Double.class, new WarningRenderer());

        JPanel rowButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowButtons.add(addButton);
        rowButtons.add(changeButton);
        rowButtons.add(removeButton);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        tablePanel.add(rowButtons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, preview, tablePanel);
        split.setDividerLocation(PREVIEW_SIZE + 10);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(newPhotoButton);
        bottom.add(fileButton);
        bottom.add(confirmButton);
        bottom.add(cancelButton);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        newPhotoButton.addActionListener(e -> openPhoneLink());
        fileButton.addActionListener(e -> chooseFile());
        addButton.addActionListener(e -> addRow());
        changeButton.addActionListener(e -> changeSelectedFood());
        removeButton.addActionListener(e -> removeSelectedRow());
        confirmButton.addActionListener(e -> confirm());
        cancelButton.addActionListener(e -> close());
        setBusy(true);
    }

    private void setBusy(boolean busy) {
        boolean editable = !busy;
        newPhotoButton.setEnabled(editable && visionAvailable);
        fileButton.setEnabled(editable && visionAvailable);
        addButton.setEnabled(editable);
        changeButton.setEnabled(editable);
        removeButton.setEnabled(editable);
        confirmButton.setEnabled(editable && tableModel.getRowCount() > 0);
        table.setEnabled(editable);
    }

    // ------------------------------------------------------------------ flujo

    private void start() {
        status("Revisando el reconocimiento…");
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.checkVisionService();
            }

            @Override
            protected void done() {
                visionAvailable = Boolean.TRUE.equals(result(this));
                if (visionAvailable) {
                    openPhoneLink();
                } else {
                    linkField.setText("");
                    status("El reconocimiento no está disponible. Agrega los alimentos a mano.");
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void openPhoneLink() {
        cancelPhotoWorker();
        try {
            String ip = (String) ipCombo.getSelectedItem();
            PhoneUploadSession session = service.startPhoneSession(ip);
            linkField.setText(session.getUrl());
            linkField.selectAll();
            status("Esperando la foto del celular…");
        } catch (CameraException e) {
            status(e.getMessage() + " Puedes elegir una foto del disco.");
            setBusy(false);
            return;
        }
        setBusy(false);
        photoWorker = new SwingWorker<CapturedImage, Void>() {
            @Override
            protected CapturedImage doInBackground() throws Exception {
                return service.waitForPhoto();
            }

            @Override
            protected void done() {
                if (closing || isCancelled()) {
                    return;
                }
                try {
                    analyze(get());
                } catch (ExecutionException e) {
                    showCameraError(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        photoWorker.execute();
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Fotos (JPG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        cancelPhotoWorker();
        service.cancel();
        linkField.setText("");
        try {
            analyze(service.loadFromFile(chooser.getSelectedFile().toPath()));
        } catch (CameraException e) {
            showCameraError(e);
        }
    }

    private void analyze(CapturedImage image) {
        showPreview(image);
        status("Reconociendo los alimentos…");
        setBusy(true);
        new SwingWorker<List<CaptureItem>, Void>() {
            @Override
            protected List<CaptureItem> doInBackground() throws Exception {
                return service.analyze(userId, image);
            }

            @Override
            protected void done() {
                if (closing) {
                    return;
                }
                try {
                    List<CaptureItem> items = get();
                    photoAnalyzed = true;
                    tableModel.setItems(items);
                    status(items.isEmpty()
                            ? "No se detectaron alimentos. Agrégalos a mano."
                            : "Revisa los gramos y confirma. Las kcal son aproximadas.");
                } catch (ExecutionException e) {
                    showCameraError(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                setBusy(false);
            }
        }.execute();
    }

    private void confirm() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        List<CaptureItem> items = tableModel.getItems();
        if (items.stream().anyMatch(i -> i.getWarning() != null)) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Hay alimentos con alergia o que excluiste de tu dieta. ¿Guardar de todas formas?",
                    "Revisa los avisos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        setBusy(true);
        status("Guardando…");
        boolean fromCamera = photoAnalyzed;
        new SwingWorker<ConsumptionRecord, Void>() {
            @Override
            protected ConsumptionRecord doInBackground() throws Exception {
                return fromCamera ? service.confirm(userId, items) : service.confirmManual(userId, items);
            }

            @Override
            protected void done() {
                try {
                    ConsumptionRecord record = get();
                    JOptionPane.showMessageDialog(CameraDialog.this,
                            String.format("Comida registrada: %.0f kcal.", record.getEstimatedCalories()),
                            "Registrar con foto", JOptionPane.INFORMATION_MESSAGE);
                    closing = true;
                    dispose();
                } catch (ExecutionException e) {
                    showError(e.getCause());
                    status("No se guardó. Corrige y vuelve a confirmar.");
                    setBusy(false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void close() {
        closing = true;
        cancelPhotoWorker();
        service.cancel();
        dispose();
    }

    private void cancelPhotoWorker() {
        if (photoWorker != null) {
            photoWorker.cancel(true);
            photoWorker = null;
        }
    }

    // ------------------------------------------------------------------ filas

    private void addRow() {
        Optional<FoodOption> food = askFood(List.of());
        if (food.isEmpty()) {
            return;
        }
        Optional<Double> grams = askGrams(100);
        if (grams.isEmpty()) {
            return;
        }
        try {
            tableModel.add(service.newItem(userId, food.get().foodId(), grams.get()));
            setBusy(false);
        } catch (NutriTrackException e) {
            showError(e);
        }
    }

    private void changeSelectedFood() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila.");
            return;
        }
        CaptureItem item = tableModel.get(row);
        Optional<FoodOption> food = askFood(item.getAlternatives());
        if (food.isEmpty()) {
            return;
        }
        try {
            service.changeFood(userId, item, food.get().foodId());
            tableModel.fireTableRowsUpdated(row, row);
        } catch (NutriTrackException e) {
            showError(e);
        }
    }

    private void removeSelectedRow() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            tableModel.remove(row);
            setBusy(false);
        }
    }

    /** Ofrece las alternativas del modelo y la opción de buscar otro alimento. */
    private Optional<FoodOption> askFood(List<FoodOption> suggestions) {
        if (!suggestions.isEmpty()) {
            List<Object> choices = new ArrayList<>(suggestions);
            String search = "Buscar otro…";
            choices.add(search);
            Object chosen = JOptionPane.showInputDialog(this, "¿Qué alimento es?", "Elegir alimento",
                    JOptionPane.QUESTION_MESSAGE, null, choices.toArray(), choices.get(0));
            if (chosen == null) {
                return Optional.empty();
            }
            if (chosen instanceof FoodOption option) {
                return Optional.of(option);
            }
        }
        String text = JOptionPane.showInputDialog(this, "Escribe parte del nombre del alimento:", "Buscar alimento",
                JOptionPane.QUESTION_MESSAGE);
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            List<FoodOption> found = service.searchFoods(text);
            if (found.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron alimentos con \"" + text.trim() + "\".");
                return Optional.empty();
            }
            Object chosen = JOptionPane.showInputDialog(this, "Elige el alimento:", "Buscar alimento",
                    JOptionPane.QUESTION_MESSAGE, null, found.toArray(), found.get(0));
            return Optional.ofNullable((FoodOption) chosen);
        } catch (NutriTrackException e) {
            showError(e);
            return Optional.empty();
        }
    }

    private Optional<Double> askGrams(double initial) {
        String text = JOptionPane.showInputDialog(this, "¿Cuántos gramos?", String.valueOf((int) initial));
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(text.trim().replace(',', '.')));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Escribe un número de gramos.");
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ mensajes

    private void status(String text) {
        statusLabel.setText(text);
    }

    private void showPreview(CapturedImage image) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(image.bytes()));
            double scale = Math.min((double) PREVIEW_SIZE / img.getWidth(), (double) PREVIEW_SIZE / img.getHeight());
            Image scaled = img.getScaledInstance((int) (img.getWidth() * scale), (int) (img.getHeight() * scale),
                    Image.SCALE_SMOOTH);
            preview.setIcon(new ImageIcon(scaled));
            preview.setText(null);
        } catch (IOException | RuntimeException e) {
            preview.setIcon(null);
            preview.setText("Sin vista previa");
        }
    }

    /** Errores de la cámara: explica qué pasó y deja seguir a mano. */
    private void showCameraError(Throwable error) {
        if (closing) {
            return;
        }
        if (error instanceof CameraException camera) {
            switch (camera.getReason()) {
                case TIMEOUT -> status("No llegó la foto. Pulsa \"Otra foto\" o elige una del disco.");
                case UNAVAILABLE -> {
                    visionAvailable = false;
                    status("El reconocimiento no está disponible. Agrega los alimentos a mano.");
                }
                case INVALID_IMAGE -> status("La imagen no es válida. Toma otra foto.");
                case NETWORK -> status(camera.getMessage());
                case ESTIMATION -> status("No se pudo reconocer la foto. Agrega los alimentos a mano.");
            }
        } else {
            showError(error);
        }
        setBusy(false);
    }

    private void showError(Throwable error) {
        String message = error.getMessage() == null ? "Ocurrió un error inesperado." : error.getMessage();
        boolean warning = error instanceof ValidationException || error instanceof BussinesRuleException;
        JOptionPane.showMessageDialog(this, message, warning ? "Revisa los datos" : "Error",
                warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private static <T> T result(SwingWorker<T, ?> worker) {
        try {
            return worker.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ tabla

    /** Columnas: alimento, gramos (editable), kcal, confianza y aviso. */
    private final class ItemsTableModel extends AbstractTableModel {
        private final String[] columns = {"Alimento", "Gramos", "kcal", "Confianza", "Aviso"};
        private final List<CaptureItem> items = new ArrayList<>();

        void setItems(List<CaptureItem> newItems) {
            items.clear();
            items.addAll(newItems);
            fireTableDataChanged();
        }

        void add(CaptureItem item) {
            items.add(item);
            fireTableRowsInserted(items.size() - 1, items.size() - 1);
        }

        void remove(int row) {
            items.remove(row);
            fireTableRowsDeleted(row, row);
        }

        CaptureItem get(int row) {
            return items.get(row);
        }

        List<CaptureItem> getItems() {
            return new ArrayList<>(items);
        }

        @Override public int getRowCount() { return items.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 1 || column == 2 ? Double.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1;
        }

        @Override
        public Object getValueAt(int row, int column) {
            CaptureItem item = items.get(row);
            return switch (column) {
                case 0 -> item.hasFood() ? item.getFoodName()
                        : "¿" + (item.getLabel() == null || item.getLabel().isBlank() ? "?" : item.getLabel()) + "? (elige)";
                case 1 -> item.getGrams();
                case 2 -> item.hasFood() ? item.getKcal() : null;
                case 3 -> item.getConfidence() == null ? "—" : Math.round(item.getConfidence() * 100) + " %";
                default -> notice(item);
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column != 1 || !(value instanceof Double grams)) {
                return;
            }
            try {
                service.changeGrams(items.get(row), grams);
            } catch (NutriTrackException e) {
                showError(e);
            }
            fireTableRowsUpdated(row, row);
        }

        private String notice(CaptureItem item) {
            if (item.getWarning() != null) {
                return item.getWarning();
            }
            if (!item.hasFood()) {
                return "Elige el alimento";
            }
            if (item.getConfidence() != null && item.getConfidence() < service.getSettings().getConfidenceLow()) {
                return "Revisa la cantidad";
            }
            return "";
        }
    }

    /** Pinta de rojo las filas con alergia y de amarillo las que hay que revisar. */
    private final class WarningRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            Object shown = value instanceof Double d ? String.format("%.1f", d) : value;
            Component c = super.getTableCellRendererComponent(t, shown, selected, focus, row, column);
            if (!selected) {
                CaptureItem item = tableModel.get(row);
                String notice = (String) tableModel.getValueAt(row, 4);
                if (item.getWarning() != null) {
                    c.setBackground(new Color(0xFF, 0xD6, 0xD6));
                } else if (!notice.isEmpty()) {
                    c.setBackground(new Color(0xFF, 0xF4, 0xCC));
                } else {
                    c.setBackground(Color.WHITE);
                }
            }
            setHorizontalAlignment(value instanceof Double ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return c;
        }
    }
}