package com.nutritrackcore.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import java.util.Map;
import javax.swing.UIManager;

/**
 * Colores, tipografía y apariencia general de la aplicación.
 * <p>
 * Usa FlatLaf (aspecto moderno para Swing) y la fuente Poppins, que viene
 * dentro del proyecto en {@code resources/fonts}. Todos los paneles toman sus
 * colores y fuentes de aquí para que la aplicación se vea igual en todas las
 * pantallas.
 */
public final class Theme {

    /** Verde principal: botones, enlaces y elementos activos. */
    public static final Color ACCENT = new Color(0x0B7A69);
    /** Verde claro para resaltar sobre fondos oscuros. */
    public static final Color ACCENT_LIGHT = new Color(0x34D6B5);
    /** Verde del ícono del logo. */
    public static final Color LOGO = new Color(0x12B39A);
    /** Fondo oscuro del panel de bienvenida (debajo de la foto). */
    public static final Color DARK_BG = new Color(0x0C2A33);

    /** Texto principal. */
    public static final Color TEXT = new Color(0x0F1F2A);
    /** Texto de etiquetas de formulario. */
    public static final Color TEXT_LABEL = new Color(0x1E2D38);
    /** Texto secundario. */
    public static final Color TEXT_MUTED = new Color(0x4F5E6B);
    /** Íconos dentro de los campos. */
    public static final Color ICON = new Color(0x6B7A87);
    /** Texto claro sobre el panel oscuro. */
    public static final Color ON_DARK = Color.WHITE;
    /** Texto secundario sobre el panel oscuro. */
    public static final Color ON_DARK_MUTED = new Color(0xC9D6DC);
    /** Bordes de campos. */
    public static final Color BORDER = new Color(0xD6DEE4);
    /** Fondo general. */
    public static final Color BACKGROUND = Color.WHITE;

    private static final String FAMILY = "Poppins";
    private static final String[] FONT_FILES = {
        "Poppins-Regular.ttf", "Poppins-Medium.ttf", "Poppins-SemiBold.ttf", "Poppins-Bold.ttf"
    };

    private static String loadedFamily = Font.SANS_SERIF;

    private Theme() {
    }

    /**
     * Instala FlatLaf, registra la fuente Poppins y ajusta los valores por
     * defecto de Swing (bordes redondeados, color de acento, botón para ver la
     * contraseña). Debe llamarse una vez, antes de crear cualquier ventana.
     */
    public static void install() {
        registerFonts();

        Map<String, String> extras = new HashMap<>();
        extras.put("@accentColor", "#0B7A69");
        FlatLaf.setGlobalExtraDefaults(extras);
        FlatLightLaf.setup();

        UIManager.put("defaultFont", font(Font.PLAIN, 14));
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("CheckBox.arc", 6);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Component.focusColor", new Color(0x330B7A69, true));
        UIManager.put("PasswordField.showRevealButton", true);
        UIManager.put("TextComponent.selectAllOnFocusPolicy", "never");
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.okButtonText", "Aceptar");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.yesButtonText", "Sí");
        UIManager.put("OptionPane.noButtonText", "No");
    }

    /**
     * Devuelve la fuente de la aplicación.
     *
     * @param weight {@link Font#PLAIN} o {@link Font#BOLD}
     * @param size   tamaño en puntos
     * @return fuente Poppins (o la fuente del sistema si no se pudo cargar)
     */
    public static Font font(int weight, float size) {
        return new Font(loadedFamily, weight, 1).deriveFont(size);
    }

    /**
     * Devuelve una variante intermedia de Poppins (Medium o SemiBold), que Java
     * no puede pedir solo con {@link Font#BOLD}.
     *
     * @param variant "Medium" o "SemiBold"
     * @param size    tamaño en puntos
     * @return la fuente pedida o, si no existe, la normal en negrita
     */
    public static Font font(String variant, float size) {
        Font f = new Font(FAMILY + " " + variant, Font.PLAIN, 1);
        if (!f.getFamily().startsWith(FAMILY)) {
            return font(Font.BOLD, size);
        }
        return f.deriveFont(size);
    }

    private static void registerFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        boolean ok = true;
        for (String file : FONT_FILES) {
            try (var in = Theme.class.getResourceAsStream("/fonts/" + file)) {
                if (in == null) {
                    ok = false;
                    continue;
                }
                ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
            } catch (Exception e) {           // fuente dañada o ilegible: se usa la del sistema
                ok = false;
            }
        }
        if (ok) {
            loadedFamily = FAMILY;
        }
    }
}
