package com.nutritrackcore.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Buscador "tipo Google": un campo de texto que, mientras se escribe, muestra
 * debajo las opciones que coinciden (sin importar tildes ni mayúsculas).
 * Al elegir una se ejecuta la acción indicada y el campo se limpia.
 *
 * @param <T> tipo de los elementos que se buscan
 */
public class SearchBox<T> extends JPanel {

    private static final int MAX_RESULTS = 8;

    private final JTextField field = new JTextField();
    private final DefaultListModel<T> model = new DefaultListModel<>();
    private final JList<T> list = new JList<>(model);
    private final JPopupMenu popup = new JPopupMenu();
    private final Function<T, String> toText;
    private final Consumer<T> onPick;
    private List<T> items = new ArrayList<>();

    /**
     * @param placeholder texto gris cuando el campo está vacío
     * @param toText      cómo se muestra cada elemento
     * @param onPick      qué hacer con el elemento elegido
     */
    public SearchBox(String placeholder, Function<T, String> toText, Consumer<T> onPick) {
        super(new BorderLayout());
        this.toText = toText;
        this.onPick = onPick;
        setOpaque(false);
        FormKit.styleField(field, placeholder, Icons.search(16, Theme.TEXT_MUTED), 260);
        field.getAccessibleContext().setAccessibleName(placeholder);
        add(field, BorderLayout.CENTER);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(Theme.font(Font.PLAIN, 14));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected,
                                                          boolean focus) {
                @SuppressWarnings("unchecked")
                String text = SearchBox.this.toText.apply((T) value);
                super.getListCellRendererComponent(l, text, index, selected, focus);
                setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        popup.add(scroll);
        popup.setFocusable(false);

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popup.isVisible()) {
                    return;
                }
                int i = list.getSelectedIndex();
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> list.setSelectedIndex(Math.min(model.size() - 1, i + 1));
                    case KeyEvent.VK_UP -> list.setSelectedIndex(Math.max(0, i - 1));
                    case KeyEvent.VK_ENTER -> pick(list.getSelectedValue());
                    case KeyEvent.VK_ESCAPE -> popup.setVisible(false);
                    default -> {
                        return;
                    }
                }
                e.consume();
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                pick(list.getSelectedValue());
            }
        });
    }

    /**
     * Cambia los elementos entre los que se busca.
     *
     * @param items elementos disponibles
     */
    public void setItems(List<T> items) {
        this.items = new ArrayList<>(items);
    }

    private void update() {
        String query = normalize(field.getText());
        model.clear();
        if (query.isEmpty()) {
            popup.setVisible(false);
            return;
        }
        for (T item : items) {
            if (normalize(toText.apply(item)).contains(query)) {
                model.addElement(item);
                if (model.size() == MAX_RESULTS) {
                    break;
                }
            }
        }
        if (model.isEmpty() || !field.isShowing()) {
            popup.setVisible(false);
            return;
        }
        list.setSelectedIndex(0);
        list.setVisibleRowCount(model.size());
        popup.setPopupSize(new Dimension(field.getWidth(), list.getPreferredScrollableViewportSize().height + 4));
        popup.show(field, 0, field.getHeight() + 2);
        field.requestFocusInWindow();
    }

    private void pick(T item) {
        popup.setVisible(false);
        if (item != null) {
            field.setText("");
            onPick.accept(item);
        }
    }

    /** Quita tildes y pasa a minúsculas para comparar. */
    static String normalize(String text) {
        String clean = Normalizer.normalize(text == null ? "" : text.trim(), Normalizer.Form.NFD);
        return clean.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
