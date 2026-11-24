package com.nutritrackcore.camera;

import com.nutritrackcore.exceptions.BussinesRuleException;
import com.nutritrackcore.exceptions.NutriTrackException;
import com.nutritrackcore.exceptions.ValidationException;
import com.nutritrackcore.services.UserSession;
import com.nutritrackcore.ui.Icons;
import com.nutritrackcore.ui.Proto;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Ventana "Registrar con foto", en tres pasos como el prototipo:
 * <ol>
 *   <li><b>Celular</b>: enlace para abrir en el celular (misma red Wi-Fi) y espera de la foto.</li>
 *   <li><b>Foto</b>: la foto llegó y el servicio de visión reconoce los alimentos.</li>
 *   <li><b>Revisar</b>: alimentos del catálogo con sus gramos; el usuario ajusta y guarda.</li>
 * </ol>
 * Los alimentos son los de la base de datos: el servicio de visión devuelve los
 * {@code id_alimento} del catálogo y {@link FoodCaptureService} trae su nombre y sus kcal.
 * Todo lo que usa la red va en {@link SwingWorker}.
 */
public class CameraDialog extends JDialog {

    private static final Color INK = new Color(0x0F1F2A);
    private static final Color INK_2 = new Color(0x1E2D38);
    private static final Color MUTED = new Color(0x4F5E6B);
    private static final Color NOTE = new Color(0x5C6B77);
    private static final Color GREEN = new Color(0x0B7A69);
    private static final Color GREEN_DARK = new Color(0x0B5F52);
    private static final Color GREEN_SOFT = new Color(0xE6F3F0);
    private static final Color BORDER = new Color(0xD6DEE4);
    private static final Color LINE = new Color(0xEEF2F4);
    private static final Color FOOTER_BG = new Color(0xFAFCFC);
    private static final Color AMBER_BG = new Color(0xFFF3E3);
    private static final Color AMBER = new Color(0x7A3E00);
    private static final Color TEAL = new Color(0x34D6B5);
    private static final int WIDTH = 780;

    private final FoodCaptureService service;
    private final int userId;
    /** Fecha y hora con la que se guarda la comida; {@code null} = ahora. */
    private final LocalDateTime mealTime;
    /** Nombre de la comida ("Almuerzo"); {@code null} si no se abrió desde una comida. */
    private final String mealName;
    private final Image banner;
    /** kcal de la comida en el plan; 0 si no hay plan. */
    private final double planKcal;

    private final List<CaptureItem> items = new ArrayList<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private Header header;

    // paso 1
    private final JLabel urlLabel = new JLabel(" ");
    private final JPanel ipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel waitingText = new JLabel("Esperando la foto del celular…");
    private final JLabel countdown = new JLabel(" ");
    private final PhoneMock phone = new PhoneMock();
    private String selectedIp;
    private Instant deadline;
    private final Timer clock = new Timer(1000, e -> tick());

    // paso 2
    private final BusyPhoto busyPhoto = new BusyPhoto();

    // paso 3
    private final PhotoBox reviewPhoto = new PhotoBox();
    private final JPanel compareBox = new JPanel(new BorderLayout());
    private final JLabel compareText = new JLabel();
    private final JLabel countLabel = new JLabel();
    private final JPanel rows = new JPanel();
    private final JLabel totalLabel = new JLabel("—");
    private final JButton saveButton = new JButton("Guardar comida");

    private BufferedImage photo;
    private boolean visionAvailable;
    private boolean photoAnalyzed;
    private boolean closing;
    private boolean saving;
    private SwingWorker<?, ?> photoWorker;

    /**
     * @throws NutriTrackException si no hay una sesión iniciada
     */
    public CameraDialog(Window owner, FoodCaptureService service, UserSession session) throws NutriTrackException {
        this(owner, service, session, null, null, null, 0);
    }

    /**
     * @param mealTime fecha y hora de la comida que se registra ({@code null} = ahora)
     * @throws NutriTrackException si no hay una sesión iniciada
     */
    public CameraDialog(Window owner, FoodCaptureService service, UserSession session,
                        LocalDateTime mealTime) throws NutriTrackException {
        this(owner, service, session, mealTime, null, null, 0);
    }

    /**
     * @param mealTime fecha y hora de la comida ({@code null} = ahora)
     * @param mealName nombre de la comida, por ejemplo "Almuerzo" ({@code null} = sin nombre)
     * @param banner   foto de la franja de arriba ({@code null} = verde liso)
     * @param planKcal kcal de esa comida en el plan, para comparar (0 = no comparar)
     * @throws NutriTrackException si no hay una sesión iniciada
     */
    public CameraDialog(Window owner, FoodCaptureService service, UserSession session, LocalDateTime mealTime,
                        String mealName, Image banner, double planKcal) throws NutriTrackException {
        super(owner, "Registrar con foto", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.mealTime = mealTime;
        this.mealName = mealName;
        this.banner = banner;
        this.planKcal = planKcal;
        this.userId = session.getCurrentUser().getIdUser();
        buildLayout();
        setUndecorated(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        getRootPane().registerKeyboardAction(e -> close(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setSize(WIDTH, 600);
        setLocationRelativeTo(owner);
    }

    /** Abre la ventana y empieza a revisar el servicio. Llamar desde el hilo de Swing. */
    public static void open(Window owner, FoodCaptureService service, UserSession session) {
        open(owner, service, session, null, null, null, 0);
    }

    /**
     * Abre la ventana para una comida concreta.
     *
     * @param mealTime fecha y hora con la que se guarda la comida ({@code null} = ahora)
     */
    public static void open(Window owner, FoodCaptureService service, UserSession session, LocalDateTime mealTime) {
        open(owner, service, session, mealTime, null, null, 0);
    }

    /**
     * Abre la ventana para una comida del plan, con su foto y sus kcal para comparar.
     *
     * @param mealTime fecha y hora con la que se guarda la comida ({@code null} = ahora)
     * @param mealName nombre de la comida ("Almuerzo")
     * @param banner   foto de la comida para la franja de arriba
     * @param planKcal kcal de la comida en el plan
     */
    public static void open(Window owner, FoodCaptureService service, UserSession session, LocalDateTime mealTime,
                            String mealName, Image banner, double planKcal) {
        try {
            CameraDialog dialog = new CameraDialog(owner, service, session, mealTime, mealName, banner, planKcal);
            dialog.start();
            dialog.setVisible(true);
        } catch (NutriTrackException e) {
            JOptionPane.showMessageDialog(owner, e.getMessage(), "Registrar con foto", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------ armado

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createLineBorder(new Color(0xC9D3DA)));
        header = new Header();
        root.add(header, BorderLayout.NORTH);
        body.setOpaque(false);
        body.add(linkStep(), "link");
        body.add(busyStep(), "busy");
        body.add(reviewStep(), "review");
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);
        showStep("link");
    }

    /** Paso 1: instrucciones, enlace, direcciones, espera y el celular de muestra. */
    private JComponent linkStep() {
        JPanel left = column();
        left.add(stepRow("1", "Conecta el celular al mismo Wi-Fi",
                "El mismo de este computador. En la universidad usa el punto de acceso del celular.", null));
        left.add(Box.createVerticalStrut(16));

        JPanel linkBox = new RoundBox(new Color(0xF3FAF8), GREEN, 1.5f, 24);
        linkBox.setLayout(new BorderLayout(8, 0));
        linkBox.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 6));
        urlLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        urlLabel.setForeground(GREEN_DARK);
        linkBox.add(urlLabel, BorderLayout.CENTER);
        JButton copy = Proto.button("Copiar", GREEN, Color.WHITE, null, 34, 13);
        copy.addActionListener(e -> copyLink());
        linkBox.add(copy, BorderLayout.EAST);
        linkBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        ipRow.setOpaque(false);
        ipRow.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        JPanel extra = column();
        extra.add(left(linkBox));
        extra.add(Box.createVerticalStrut(8));
        extra.add(left(ipRow));
        left.add(stepRow("2", "Escribe este enlace en el navegador del celular", null, extra));
        left.add(Box.createVerticalStrut(16));
        left.add(stepRow("3", "Toca «Tomar foto» y envíala",
                "Toma el plato desde arriba, con buena luz. La foto llega sola aquí.", null));
        left.add(Box.createVerticalStrut(18));

        JPanel waiting = new RoundBox(new Color(0xFFF8E1), null, 0, 24);
        waiting.setLayout(new BorderLayout(10, 0));
        waiting.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        waiting.add(new Dot(new Color(0xF0A500)), BorderLayout.WEST);
        waitingText.setFont(Proto.font("SemiBold", 13));
        waitingText.setVerticalAlignment(SwingConstants.CENTER);
        waitingText.setForeground(new Color(0x7A4A00));
        waiting.add(waitingText, BorderLayout.CENTER);
        countdown.setFont(Proto.font(null, 12));
        countdown.setForeground(new Color(0x7A4A00));
        waiting.add(countdown, BorderLayout.EAST);
        waiting.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        left.add(left(waiting));

        JPanel right = column();
        phone.setAlignmentX(CENTER_ALIGNMENT);
        right.add(phone);
        right.add(Box.createVerticalStrut(8));
        JLabel caption = text("Así se ve en tu celular", null, 11, NOTE);
        caption.setAlignmentX(CENTER_ALIGNMENT);
        right.add(caption);

        JPanel content = new JPanel(new BorderLayout(26, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        content.add(left, BorderLayout.CENTER);
        content.add(right, BorderLayout.EAST);

        JButton fromPc = Proto.button("Elegir una foto del PC", Color.WHITE, GREEN, GREEN, 42, 13);
        fromPc.addActionListener(e -> chooseFile());
        JButton cancel = Proto.cancelButton("Cancelar");
        cancel.addActionListener(e -> close());
        JPanel step = new JPanel(new BorderLayout());
        step.setOpaque(false);
        step.add(content, BorderLayout.CENTER);
        step.add(footer(fromPc, null, cancel), BorderLayout.SOUTH);
        return step;
    }

    /** Paso 2: la foto con el aviso de que se están reconociendo los alimentos. */
    private JComponent busyStep() {
        JPanel step = new JPanel(new BorderLayout());
        step.setOpaque(false);
        step.setBorder(BorderFactory.createEmptyBorder(22, 24, 24, 24));
        step.add(busyPhoto, BorderLayout.CENTER);
        return step;
    }

    /** Paso 3: foto, comparación con el plan, alimentos reconocidos y guardar. */
    private JComponent reviewStep() {
        JPanel left = column();
        reviewPhoto.setAlignmentX(LEFT_ALIGNMENT);
        left.add(reviewPhoto);
        left.add(Box.createVerticalStrut(10));
        compareBox.setUI(new javax.swing.plaf.PanelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = Proto.smooth(g);
                Object bg = c.getClientProperty("bg");
                g2.setColor(bg instanceof Color color ? color : GREEN_SOFT);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 20, 20);
                g2.dispose();
            }
        });
        compareBox.setOpaque(false);
        compareBox.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        compareText.setFont(Proto.font("SemiBold", 13));
        compareBox.add(compareText);
        compareBox.setAlignmentX(LEFT_ALIGNMENT);
        compareBox.setMaximumSize(new Dimension(260, 80));
        left.add(compareBox);
        left.setPreferredSize(new Dimension(260, 10));

        JPanel right = new JPanel(new BorderLayout(0, 4));
        right.setOpaque(false);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        countLabel.setFont(Proto.font("SemiBold", 15));
        countLabel.setForeground(INK);
        head.add(countLabel, BorderLayout.WEST);
        head.add(text("Gramos estimados", null, 12, MUTED), BorderLayout.EAST);
        right.add(head, BorderLayout.NORTH);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        JPanel rowsTop = new JPanel(new BorderLayout());
        rowsTop.setOpaque(false);
        rowsTop.add(rows, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(rowsTop);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        right.add(scroll, BorderLayout.CENTER);
        JPanel note = new JPanel(new BorderLayout(10, 0));
        note.setOpaque(false);
        note.add(wrapped("La foto da un estimado: revisa los gramos y quita lo que no comiste.", 230, 11, NOTE),
                BorderLayout.CENTER);
        JButton add = Proto.link("+ Agregar alimento", GREEN, 12);
        add.setFont(Proto.font("SemiBold", 12));
        add.addActionListener(e -> addRow());
        note.add(add, BorderLayout.EAST);
        right.add(note, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(22, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));
        content.add(left, BorderLayout.WEST);
        content.add(right, BorderLayout.CENTER);

        JPanel total = column();
        total.add(left(text("Vas a registrar", null, 12, MUTED)));
        totalLabel.setFont(Proto.font("Bold", 20));
        totalLabel.setForeground(INK);
        total.add(left(totalLabel));
        JButton cancel = Proto.cancelButton("Cancelar");
        cancel.addActionListener(e -> close());
        saveButton.setFont(Proto.font("Bold", 14));
        saveButton.setForeground(Color.WHITE);
        saveButton.setContentAreaFilled(false);
        saveButton.setBorderPainted(false);
        saveButton.setFocusPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(c.isEnabled() ? GREEN : new Color(0xC9D3DA));
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 20, 20);
                g2.dispose();
                super.paint(g, c);
            }
        });
        saveButton.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 22));
        saveButton.setPreferredSize(new Dimension(saveButton.getPreferredSize().width, 46));
        saveButton.addActionListener(e -> confirm());

        JPanel step = new JPanel(new BorderLayout());
        step.setOpaque(false);
        step.add(content, BorderLayout.CENTER);
        step.add(footer(total, cancel, saveButton), BorderLayout.SOUTH);
        return step;
    }

    /** Barra de abajo: un componente a la izquierda y botones a la derecha. */
    private static JComponent footer(JComponent start, JComponent middle, JComponent end) {
        JPanel bar = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(FOOTER_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(LINE);
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        bar.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        JPanel startHolder = new JPanel(new GridBagLayout());
        startHolder.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        startHolder.add(start, c);
        bar.add(startHolder, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        if (middle != null) {
            buttons.add(middle);
        }
        buttons.add(end);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    /** Paso numerado: círculo con el número, título, texto y algo debajo. */
    private static JComponent stepRow(String number, String title, String subtitle, JComponent extra) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        JPanel num = new JPanel(new BorderLayout());
        num.setOpaque(false);
        num.add(new NumberCircle(number), BorderLayout.NORTH);
        row.add(num, BorderLayout.WEST);
        JPanel col = column();
        col.add(left(text(title, "SemiBold", 14, INK)));
        if (subtitle != null) {
            col.add(Box.createVerticalStrut(2));
            col.add(left(wrapped(subtitle, 330, 12, MUTED)));
        }
        if (extra != null) {
            col.add(Box.createVerticalStrut(8));
            col.add(left(extra));
        }
        row.add(col, BorderLayout.CENTER);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void showStep(String step) {
        cards.show(body, step);
        header.setStep(switch (step) {
            case "busy" -> 1;
            case "review" -> 2;
            default -> 0;
        });
        if (!"link".equals(step)) {
            clock.stop();
        }
    }

    // ------------------------------------------------------------------ flujo

    private void start() {
        waiting("Preparando el enlace…");
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.checkVisionService();
            }

            @Override
            protected void done() {
                visionAvailable = Boolean.TRUE.equals(result(this));
                openPhoneLink();                       // la foto se toma con el celular, haya o no reconocimiento
            }
        }.execute();
        buildIpChips();
    }

    private void buildIpChips() {
        ipRow.removeAll();
        List<String> ips = LocalNetworkAddress.privateAddresses();
        if (selectedIp == null && !ips.isEmpty()) {
            selectedIp = ips.get(0);
        }
        if (ips.size() > 1) {
            ipRow.add(text("¿No abre? Prueba otra dirección:", null, 12, MUTED));
            for (String ip : ips) {
                boolean on = ip.equals(selectedIp);
                JButton chip = Proto.button(ip, on ? GREEN_SOFT : Color.WHITE, on ? GREEN_DARK : MUTED,
                        on ? GREEN : BORDER, 30, 12);
                chip.getAccessibleContext().setAccessibleName("Usar la dirección " + ip);
                chip.addActionListener(e -> {
                    if (!ip.equals(selectedIp)) {
                        selectedIp = ip;
                        buildIpChips();
                        openPhoneLink();
                    }
                });
                ipRow.add(chip);
            }
        } else {
            ipRow.add(wrapped("Si el celular no abre el enlace, revisa que los dos estén en la misma red.", 300, 12,
                    MUTED));
        }
        ipRow.revalidate();
        ipRow.repaint();
    }

    private void openPhoneLink() {
        cancelPhotoWorker();
        showStep("link");
        try {
            PhoneUploadSession session = service.startPhoneSession(selectedIp);
            urlLabel.setText(session.getUrl());
            phone.setHost(hostOf(session.getUrl()));
            waiting("Esperando la foto del celular…");
            deadline = Instant.now().plus(service.getSettings().getUploadTimeout());
            tick();
            clock.start();
        } catch (CameraException e) {
            urlLabel.setText("—");
            waiting(e.getMessage() + " Puedes elegir una foto del PC.");
            countdown.setText(" ");
            return;
        }
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
                    received(get());
                } catch (ExecutionException e) {
                    showCameraError(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        photoWorker.execute();
    }

    /** Mensaje de la franja ámbar, partido en líneas si es largo. */
    private void waiting(String message) {
        waitingText.setText("<html><div style='width:250px'>" + Proto.escape(message) + "</div></html>");
    }

    private void tick() {
        if (deadline == null) {
            return;
        }
        long left = Math.max(0, Duration.between(Instant.now(), deadline).getSeconds());
        countdown.setText(left == 0 ? "El enlace venció" : String.format("Vence en %d:%02d", left / 60, left % 60));
    }

    private void copyLink() {
        String url = urlLabel.getText();
        if (url != null && url.startsWith("http")) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
            waiting("Enlace copiado. Pégalo en el navegador del celular.");
        }
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Fotos (JPG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        cancelPhotoWorker();
        service.cancel();
        try {
            received(service.loadFromFile(chooser.getSelectedFile().toPath()));
        } catch (CameraException e) {
            showCameraError(e);
        }
    }

    /** Llegó la foto: se reconoce; si el servicio no está, se pasa a revisar para agregar a mano. */
    private void received(CapturedImage image) {
        photo = read(image);
        busyPhoto.setImage(photo);
        reviewPhoto.setImage(photo);
        if (!visionAvailable) {
            photoAnalyzed = false;
            setItems(List.of(), "El reconocimiento no está disponible. Agrega los alimentos con «+ Agregar».");
            return;
        }
        showStep("busy");
        busyPhoto.startAnimation();
        new SwingWorker<List<CaptureItem>, Void>() {
            @Override
            protected List<CaptureItem> doInBackground() throws Exception {
                return service.analyze(userId, image);
            }

            @Override
            protected void done() {
                busyPhoto.stopAnimation();
                if (closing) {
                    return;
                }
                try {
                    List<CaptureItem> found = get();
                    photoAnalyzed = true;
                    setItems(found, found.isEmpty() ? "No reconocimos alimentos. Agrégalos con «+ Agregar»." : null);
                } catch (ExecutionException e) {
                    photoAnalyzed = false;
                    setItems(List.of(), "No se pudo reconocer la foto. Agrega los alimentos con «+ Agregar».");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void setItems(List<CaptureItem> found, String emptyMessage) {
        items.clear();
        items.addAll(found);
        showStep("review");
        rebuildRows(emptyMessage);
    }

    private void confirm() {
        if (items.isEmpty() || saving) {
            return;
        }
        if (items.stream().anyMatch(i -> !i.hasFood())) {
            JOptionPane.showMessageDialog(this, "Hay un alimento sin elegir: tócalo para elegirlo o quítalo.",
                    "Revisa los alimentos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (items.stream().anyMatch(i -> i.getWarning() != null)) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Hay alimentos con alergia o que excluiste de tu dieta. ¿Guardar de todas formas?",
                    "Revisa los avisos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        saving = true;
        saveButton.setEnabled(false);
        saveButton.setText("Guardando…");
        List<CaptureItem> toSave = new ArrayList<>(items);
        boolean fromCamera = photoAnalyzed;
        new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() throws Exception {
                return fromCamera ? service.confirm(userId, toSave, mealTime)
                        : service.confirmManual(userId, toSave, mealTime);
            }

            @Override
            protected void done() {
                try {
                    get();
                    closing = true;
                    dispose();
                } catch (ExecutionException e) {
                    showError(e.getCause());
                    saving = false;
                    saveButton.setText("Guardar comida");
                    saveButton.setEnabled(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void close() {
        closing = true;
        clock.stop();
        busyPhoto.stopAnimation();
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

    // ------------------------------------------------------------------ filas de alimentos

    private void rebuildRows(String emptyMessage) {
        rows.removeAll();
        for (CaptureItem item : items) {
            rows.add(itemRow(item));
        }
        if (items.isEmpty()) {
            JLabel empty = wrapped(emptyMessage == null ? "Agrega los alimentos de tu plato." : emptyMessage, 380,
                    13, MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));
            rows.add(left(empty));
        }
        countLabel.setText(items.isEmpty() ? "Tu plato"
                : "Encontramos " + items.size() + (items.size() == 1 ? " alimento" : " alimentos"));
        updateTotals();
        rows.revalidate();
        rows.repaint();
    }

    private JComponent itemRow(CaptureItem item) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                BorderFactory.createEmptyBorder(8, 0, 8, 0)));

        JPanel names = column();
        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line1.setOpaque(false);
        line1.setBorder(BorderFactory.createEmptyBorder(0, -6, 0, 0));
        String name = item.hasFood() ? item.getFoodName()
                : "¿" + (item.getLabel() == null || item.getLabel().isBlank() ? "Alimento" : item.getLabel()) + "?";
        JLabel nameLabel = text(name, "Medium", 14, item.hasFood() ? INK : AMBER);
        nameLabel.setToolTipText("Toca para cambiar el alimento");
        nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                changeFood(item, nameLabel);
            }
        });
        line1.add(nameLabel);
        JComponent tag = tag(item);
        if (tag != null) {
            line1.add(tag);
        }
        names.add(left(line1));
        String sub = !item.hasFood() ? "Toca el nombre para elegir el alimento"
                : item.getWarning() != null ? item.getWarning()
                : Math.round(item.getKcal()) + " kcal";
        names.add(left(text(sub, null, 12, item.getWarning() != null ? Proto.DANGER : MUTED)));
        row.add(names, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        controls.add(gramsStepper(item));
        JButton remove = new SquareButton("×", new Color(0xFBECEA), Proto.DANGER);
        remove.getAccessibleContext().setAccessibleName("Quitar " + name);
        remove.addActionListener(e -> {
            items.remove(item);
            rebuildRows(null);
        });
        controls.add(remove);
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(controls);
        row.add(holder, BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    /** Píldora de confianza: "97%", "Revisa" si es baja o "Elegir" si no se reconoció. */
    private JComponent tag(CaptureItem item) {
        if (!item.hasFood()) {
            return pill("Elegir", AMBER_BG, AMBER);
        }
        Double conf = item.getConfidence();
        if (conf == null) {
            return null;
        }
        boolean low = conf < service.getSettings().getConfidenceLow();
        return low ? pill("Revisa", AMBER_BG, AMBER) : pill(Math.round(conf * 100) + "%", GREEN_SOFT, GREEN_DARK);
    }

    /** − [180 g] +: cambia los gramos de 10 en 10. */
    private JComponent gramsStepper(CaptureItem item) {
        JPanel box = new RoundBox(Color.WHITE, BORDER, 1f, 16);
        box.setLayout(new BorderLayout());
        JLabel value = text(Math.round(item.getGrams()) + " g", "SemiBold", 13, INK);
        value.setHorizontalAlignment(SwingConstants.CENTER);
        value.setPreferredSize(new Dimension(58, 32));
        JButton less = new SquareButton("−", GREEN_SOFT, GREEN);
        less.getAccessibleContext().setAccessibleName("Menos gramos");
        less.addActionListener(e -> changeGrams(item, Math.max(10, Math.round(item.getGrams() / 10.0) * 10 - 10)));
        JButton more = new SquareButton("+", GREEN_SOFT, GREEN);
        more.getAccessibleContext().setAccessibleName("Más gramos");
        more.addActionListener(e -> changeGrams(item, Math.round(item.getGrams() / 10.0) * 10 + 10));
        box.add(less, BorderLayout.WEST);
        box.add(value, BorderLayout.CENTER);
        box.add(more, BorderLayout.EAST);
        return box;
    }

    private void changeGrams(CaptureItem item, double grams) {
        try {
            service.changeGrams(item, grams);
        } catch (NutriTrackException e) {
            showError(e);
        }
        rebuildRows(null);
    }

    /** Menú con las alternativas del modelo y "Buscar otro…" para cambiar el alimento de una fila. */
    private void changeFood(CaptureItem item, JComponent anchor) {
        JPopupMenu menu = new JPopupMenu();
        for (FoodOption option : item.getAlternatives()) {
            JMenuItem mi = new JMenuItem(option.name());
            mi.addActionListener(e -> applyFood(item, option.foodId()));
            menu.add(mi);
        }
        if (!item.getAlternatives().isEmpty()) {
            menu.addSeparator();
        }
        JMenuItem search = new JMenuItem("Buscar otro alimento…");
        search.addActionListener(e -> askFood().ifPresent(option -> applyFood(item, option.foodId())));
        menu.add(search);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void applyFood(CaptureItem item, int foodId) {
        try {
            service.changeFood(userId, item, foodId);
        } catch (NutriTrackException e) {
            showError(e);
        }
        rebuildRows(null);
    }

    private void addRow() {
        askFood().ifPresent(option -> {
            try {
                items.add(service.newItem(userId, option.foodId(), 100));
            } catch (NutriTrackException e) {
                showError(e);
            }
            rebuildRows(null);
        });
    }

    /** Busca un alimento del catálogo por nombre. */
    private java.util.Optional<FoodOption> askFood() {
        String query = JOptionPane.showInputDialog(this, "Escribe parte del nombre del alimento:",
                "Buscar alimento", JOptionPane.QUESTION_MESSAGE);
        if (query == null || query.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            List<FoodOption> found = service.searchFoods(query);
            if (found.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron alimentos con \"" + query.trim() + "\".");
                return java.util.Optional.empty();
            }
            Object chosen = JOptionPane.showInputDialog(this, "Elige el alimento:", "Buscar alimento",
                    JOptionPane.QUESTION_MESSAGE, null, found.toArray(), found.get(0));
            return java.util.Optional.ofNullable((FoodOption) chosen);
        } catch (NutriTrackException e) {
            showError(e);
            return java.util.Optional.empty();
        }
    }

    private void updateTotals() {
        double total = items.stream().filter(CaptureItem::hasFood).mapToDouble(CaptureItem::getKcal).sum();
        totalLabel.setText(items.isEmpty() ? "—" : Math.round(total) + " kcal");
        saveButton.setEnabled(!items.isEmpty() && !saving);

        boolean show = planKcal > 0 && !items.isEmpty();
        compareBox.setVisible(show);
        if (show) {
            long diff = Math.round(total - planKcal);
            double rel = diff / planKcal;
            String meal = mealName == null ? "comida" : mealName.toLowerCase();
            String plan = Math.round(planKcal) + " kcal";
            boolean more = rel > 0.15;
            String msg = Math.abs(rel) <= 0.15 ? "Se parece a tu " + meal + " del plan (" + plan + "). ¡Bien!"
                    : (diff > 0 ? diff + " kcal más" : -diff + " kcal menos") + " que tu " + meal + " del plan ("
                    + plan + ").";
            compareText.setText("<html><div style='width:180px'>" + msg + "</div></html>");
            compareText.setForeground(more ? AMBER : GREEN_DARK);
            compareBox.putClientProperty("bg", more ? AMBER_BG : GREEN_SOFT);
        }
        compareBox.repaint();
    }

    // ------------------------------------------------------------------ mensajes

    /** Errores mientras se espera la foto: se explican en la franja de espera. */
    private void showCameraError(Throwable error) {
        if (closing) {
            return;
        }
        showStep("link");
        if (error instanceof CameraException camera) {
            waiting(switch (camera.getReason()) {
                case TIMEOUT -> "No llegó la foto. Cambia de dirección o elige una foto del PC.";
                case INVALID_IMAGE -> "La imagen no es válida. Toma otra foto.";
                default -> camera.getMessage();
            });
            countdown.setText(" ");
        } else {
            showError(error);
        }
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

    private static BufferedImage read(CapturedImage image) {
        try {
            return ImageIO.read(new ByteArrayInputStream(image.bytes()));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String hostOf(String url) {
        String h = url.replaceFirst("^https?://", "");
        int slash = h.indexOf('/');
        return slash < 0 ? h : h.substring(0, slash);
    }

    // ------------------------------------------------------------------ piezas

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    private static <T extends JComponent> T left(T c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private static JLabel text(String text, String weight, float size, Color color) {
        return Proto.text(text, weight, size, color);
    }

    private static JLabel wrapped(String text, int width, float size, Color color) {
        return Proto.wrapped(text, width, null, size, color);
    }

    private static JLabel pill(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Proto.font("Bold", 10));
        l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(1, 7, 1, 7));
        return l;
    }

    /** Pinta una imagen cubriendo el área (como object-fit: cover). */
    private static void cover(Graphics2D g2, Image img, int w, int h, double fx, double fy) {
        if (img == null) {
            return;
        }
        int iw = img.getWidth(null);
        int ih = img.getHeight(null);
        if (iw <= 0 || ih <= 0) {
            return;
        }
        double scale = Math.max((double) w / iw, (double) h / ih);
        int dw = (int) Math.ceil(iw * scale);
        int dh = (int) Math.ceil(ih * scale);
        int x = (int) Math.round((w - dw) * fx);
        int y = (int) Math.round((h - dh) * fy);
        g2.drawImage(img, x, y, dw, dh, null);
    }

    /** Franja de arriba: foto de la comida, título del paso, pasos 1-2-3 y cerrar. */
    private final class Header extends JPanel {

        private final JLabel title = text("", "Bold", 21, Color.WHITE);
        private int step;

        Header() {
            super(new BorderLayout(16, 0));
            setPreferredSize(new Dimension(WIDTH, 104));
            setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 20));
            JPanel texts = column();
            texts.add(left(text("Registrar con foto" + (mealName == null ? "" : " · " + mealName), null, 12,
                    new Color(0xCFE7E1))));
            texts.add(left(title));
            JPanel textHolder = new JPanel(new GridBagLayout());
            textHolder.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            textHolder.add(texts, c);
            add(textHolder, BorderLayout.CENTER);

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
            right.setOpaque(false);
            right.add(new StepsIndicator());
            JButton x = new CloseButton();
            x.addActionListener(e -> close());
            right.add(x);
            JPanel rightHolder = new JPanel(new GridBagLayout());
            rightHolder.setOpaque(false);
            rightHolder.add(right);
            add(rightHolder, BorderLayout.EAST);
        }

        void setStep(int step) {
            this.step = step;
            title.setText(switch (step) {
                case 1 -> "Reconociendo tu plato…";
                case 2 -> "Revisa lo que encontramos";
                default -> "Tómale la foto con tu celular";
            });
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setColor(new Color(0x0B4F45));
            g2.fillRect(0, 0, getWidth(), getHeight());
            cover(g2, banner, getWidth(), getHeight(), 0.5, 0.55);
            g2.setPaint(new LinearGradientPaint(0, 0, getWidth(), 0, new float[]{0f, 0.55f, 1f},
                    new Color[]{new Color(6, 40, 36, 235), new Color(6, 40, 36, 179), new Color(6, 40, 36, 89)}));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }

        /** Celular → Foto → Revisar, con los pasos hechos marcados. */
        private final class StepsIndicator extends JComponent {

            private static final String[] NAMES = {"Celular", "Foto", "Revisar"};

            StepsIndicator() {
                setFont(Proto.font("SemiBold", 12));
                setPreferredSize(new Dimension(300, 24));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Proto.smooth(g);
                g2.setFont(getFont());
                int x = 0;
                int cy = getHeight() / 2;
                for (int i = 0; i < NAMES.length; i++) {
                    boolean on = i <= step;
                    g2.setColor(on ? Color.WHITE : new Color(255, 255, 255, 56));
                    g2.fillOval(x, cy - 11, 22, 22);
                    if (i < step) {
                        Icons.check(12, GREEN).paintIcon(this, g2, x + 5, cy - 6);
                    } else {
                        g2.setColor(on ? GREEN : Color.WHITE);
                        g2.setFont(Proto.font("Bold", 11));
                        String n = String.valueOf(i + 1);
                        g2.drawString(n, x + 11 - g2.getFontMetrics().stringWidth(n) / 2, cy + 4);
                        g2.setFont(getFont());
                    }
                    x += 28;
                    g2.setColor(on ? Color.WHITE : new Color(255, 255, 255, 153));
                    g2.drawString(NAMES[i], x, cy + 4);
                    x += g2.getFontMetrics().stringWidth(NAMES[i]) + 8;
                    if (i < NAMES.length - 1) {
                        g2.setColor(i < step ? Color.WHITE : new Color(255, 255, 255, 77));
                        g2.fillRoundRect(x, cy - 1, 22, 2, 2, 2);
                        x += 30;
                    }
                }
                g2.dispose();
            }
        }
    }

    /** Botón redondo translúcido con una ×. */
    private static final class CloseButton extends JButton {

        CloseButton() {
            setPreferredSize(new Dimension(34, 34));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Cerrar");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setColor(new Color(255, 255, 255, getModel().isRollover() ? 70 : 46));
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int c = getWidth() / 2;
            g2.drawLine(c - 5, c - 5, c + 5, c + 5);
            g2.drawLine(c + 5, c - 5, c - 5, c + 5);
            g2.dispose();
        }
    }

    /** Botón pequeño cuadrado (−, + o ×). */
    private static final class SquareButton extends JButton {

        private final Color bg;
        private final Color fg;
        private final String symbol;

        SquareButton(String symbol, Color bg, Color fg) {
            this.symbol = symbol;
            this.bg = bg;
            this.fg = fg;
            setPreferredSize(new Dimension(30, 32));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setColor(getModel().isRollover() ? bg.darker() : bg);
            if ("×".equals(symbol)) {
                g2.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 16, 16);
            } else {
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            switch (symbol) {
                case "−" -> g2.drawLine(cx - 5, cy, cx + 5, cy);
                case "+" -> {
                    g2.drawLine(cx - 5, cy, cx + 5, cy);
                    g2.drawLine(cx, cy - 5, cx, cy + 5);
                }
                default -> {
                    g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                    g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                }
            }
            g2.dispose();
        }
    }

    /** Panel con fondo redondeado y borde opcional. */
    private static class RoundBox extends JPanel {

        private final Color fill;
        private final Color border;
        private final float width;
        private final int arc;

        RoundBox(Color fill, Color border, float width, int arc) {
            this.fill = fill;
            this.border = border;
            this.width = width;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (border != null) {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(width));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
        }
    }

    /** Círculo verde claro con el número del paso. */
    private static final class NumberCircle extends JComponent {

        private final String number;

        NumberCircle(String number) {
            this.number = number;
            setPreferredSize(new Dimension(26, 26));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setColor(GREEN_SOFT);
            g2.fillOval(0, 0, 26, 26);
            g2.setColor(GREEN);
            g2.setFont(Proto.font("Bold", 13));
            g2.drawString(number, 13 - g2.getFontMetrics().stringWidth(number) / 2, 18);
            g2.dispose();
        }
    }

    /** Punto ámbar con halo de la franja "Esperando la foto". */
    private static final class Dot extends JComponent {

        private final Color color;

        Dot(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(18, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            int cy = getHeight() / 2;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 64));
            g2.fillOval(0, cy - 9, 18, 18);
            g2.setColor(color);
            g2.fillOval(4, cy - 5, 10, 10);
            g2.dispose();
        }
    }

    /** Celular dibujado: así se ve la página de subida en el teléfono. */
    private static final class PhoneMock extends JComponent {

        private String host = "";

        PhoneMock() {
            setPreferredSize(new Dimension(188, 350));
            setMaximumSize(new Dimension(188, 350));
        }

        void setHost(String host) {
            this.host = host;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            int w = 188;
            int h = 350;
            g2.setColor(new Color(8, 32, 40, 40));
            g2.fillRoundRect(2, 6, w - 4, h - 4, 30, 30);
            g2.setColor(INK);
            g2.fillRoundRect(0, 0, w, h - 4, 30, 30);
            g2.setColor(new Color(0xF4F8F9));
            g2.fillRoundRect(9, 9, w - 18, h - 22, 22, 22);
            int cx = w / 2;
            g2.setColor(new Color(0xC9D3DA));
            g2.fillRoundRect(cx - 27, 21, 54, 5, 5, 5);
            // barra de dirección
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(21, 36, w - 42, 22, 22, 22);
            g2.setColor(new Color(0xE1E7EB));
            g2.drawRoundRect(21, 36, w - 42, 22, 22, 22);
            g2.setFont(Proto.font(null, 9));
            g2.setColor(MUTED);
            centered(g2, host, cx, 51);
            g2.setFont(Proto.font("Bold", 13));
            g2.setColor(GREEN);
            centered(g2, "NutriTrack", cx, 84);
            g2.setFont(Proto.font(null, 11));
            g2.setColor(INK_2);
            centered(g2, "Foto de tu plato", cx, 104);
            // círculo punteado con la cámara
            g2.setColor(new Color(0x9FB8B2));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 5f},
                    0f));
            g2.drawOval(cx - 56, 118, 112, 112);
            Icons.camera(40, GREEN).paintIcon(this, g2, cx - 20, 154);
            // botón
            g2.setColor(GREEN);
            g2.fillRoundRect(21, 248, w - 42, 38, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setFont(Proto.font("Bold", 13));
            centered(g2, "Tomar foto", cx, 272);
            g2.setFont(Proto.font(null, 9));
            g2.setColor(NOTE);
            centered(g2, "La foto se envía a tu computador", cx, 304);
            centered(g2, "y no se guarda.", cx, 316);
            g2.dispose();
        }

        private static void centered(Graphics2D g2, String s, int cx, int y) {
            g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, y);
        }
    }

    /** Foto del paso 2 con el velo oscuro, "Reconociendo los alimentos…" y la barra animada. */
    private static final class BusyPhoto extends JComponent {

        private Image image;
        private double phase;
        private final Timer anim = new Timer(40, e -> {
            phase = (phase + 0.02) % 1.0;
            repaint();
        });

        BusyPhoto() {
            setPreferredSize(new Dimension(WIDTH - 48, 330));
        }

        void setImage(Image image) {
            this.image = image;
            repaint();
        }

        void startAnimation() {
            phase = 0;
            anim.start();
        }

        void stopAnimation() {
            anim.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            Shape clip = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28);
            g2.setClip(clip);
            g2.setColor(new Color(0x0B1E25));
            g2.fillRect(0, 0, getWidth(), getHeight());
            cover(g2, image, getWidth(), getHeight(), 0.5, 0.5);
            g2.setColor(new Color(6, 28, 34, 148));
            g2.fillRect(0, 0, getWidth(), getHeight());
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            g2.setFont(Proto.font("SemiBold", 13));
            g2.setColor(TEAL);
            Icons.check(13, TEAL).paintIcon(this, g2, cx - 56, cy - 52);
            g2.drawString("Foto recibida", cx - 38, cy - 41);
            g2.setFont(Proto.font("Bold", 18));
            g2.setColor(Color.WHITE);
            String t = "Reconociendo los alimentos…";
            g2.drawString(t, cx - g2.getFontMetrics().stringWidth(t) / 2, cy - 6);
            g2.setColor(new Color(255, 255, 255, 64));
            g2.fillRoundRect(cx - 120, cy + 10, 240, 6, 6, 6);
            g2.setClip(new RoundRectangle2D.Double(cx - 120, cy + 10, 240, 6, 6, 6));
            g2.setColor(TEAL);
            int seg = 150;
            int x = (int) (cx - 120 - seg + phase * (240 + seg));
            g2.fillRoundRect(x, cy + 10, seg, 6, 6, 6);
            g2.setClip(clip);
            g2.setFont(Proto.font(null, 12));
            g2.setColor(new Color(0xD5E6EA));
            String s = "Puede tardar unos segundos";
            g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, cy + 40);
            g2.dispose();
        }
    }

    /** Foto del paso 3 (260 × 260) con el botón "Tomar otra". */
    private final class PhotoBox extends JPanel {

        private Image image;

        PhotoBox() {
            super(null);
            setOpaque(false);
            Dimension d = new Dimension(260, 260);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
            JButton retake = Proto.button("Tomar otra", new Color(255, 255, 255, 240), GREEN, null, 32, 12);
            Proto.withIcon(retake, Icons.refresh(13, GREEN), false);
            retake.addActionListener(e -> {
                service.cancel();
                openPhoneLink();
            });
            Dimension rs = retake.getPreferredSize();
            retake.setBounds(10, 260 - 10 - 32, rs.width, 32);
            add(retake);
        }

        void setImage(Image image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Proto.smooth(g);
            g2.setClip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28));
            g2.setColor(new Color(0xE6ECEF));
            g2.fillRect(0, 0, getWidth(), getHeight());
            cover(g2, image, getWidth(), getHeight(), 0.5, 0.5);
            g2.dispose();
        }
    }

}
