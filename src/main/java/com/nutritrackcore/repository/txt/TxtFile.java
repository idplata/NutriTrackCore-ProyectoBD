package com.nutritrackcore.repository.txt;

import com.nutritrackcore.exceptions.PersistenceExceptions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Única clase que abre archivos .txt. Siempre usa UTF-8.
 * <p>
 * Crea la carpeta y el archivo (con su cabecera) si no existen. Cada archivo
 * tiene una primera línea de cabecera que empieza con {@code #}; esa línea y
 * las líneas vacías se ignoran al leer y se vuelven a escribir al reemplazar
 * el contenido.
 * <p>
 * Los repositorios {@code *Txt} reciben una instancia de esta clase en lugar de
 * tocar el sistema de archivos directamente.
 */
public class TxtFile {

    /** Ruta del archivo en disco. */
    private final Path path;

    /** Línea de cabecera (comienza con {@code #}) que se escribe al inicio del archivo. */
    private final String header;

    /**
     * Crea el acceso a un archivo .txt. Si la carpeta o el archivo no existen,
     * los crea y escribe la cabecera.
     *
     * @param filePath ruta del archivo
     * @param header   línea de cabecera del archivo (debe empezar con {@code #})
     * @throws PersistenceExceptions si no se puede crear la carpeta o el archivo
     */
    public TxtFile(String filePath, String header) throws PersistenceExceptions {
        this.path = Paths.get(filePath);
        this.header = header;
        createIfMissing();
    }

    /**
     * Crea la carpeta contenedora y el archivo con su cabecera si todavía no
     * existen. Si el archivo ya existe, no lo modifica.
     *
     * @throws PersistenceExceptions si ocurre un error de entrada/salida al crearlos
     */
    private void createIfMissing() throws PersistenceExceptions {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(path)) {
                Files.write(path, List.of(header), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new PersistenceExceptions("No se pudo crear el archivo: " + path, e);
        }
    }

    /**
     * Lee el archivo y devuelve solo las líneas de datos.
     *
     * @return las líneas de datos, sin la cabecera ({@code #}) ni líneas vacías
     * @throws PersistenceExceptions si no se puede leer el archivo
     */
    public List<String> readLines() throws PersistenceExceptions {
        try {
            List<String> data = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    data.add(line);
                }
            }
            return data;
        } catch (IOException e) {
            throw new PersistenceExceptions("No se pudo leer el archivo: " + path, e);
        }
    }

    /**
     * Reemplaza todo el contenido del archivo por la cabecera seguida de las
     * líneas indicadas.
     * <p>
     * Escribe primero en un archivo temporal y luego lo mueve encima del
     * original, para no dejar el .txt corrupto si algo falla. Si el sistema de
     * archivos no soporta el movimiento atómico, usa un movimiento normal.
     *
     * @param dataLines líneas de datos a escribir (sin la cabecera)
     * @throws PersistenceExceptions si no se puede escribir el archivo
     */
    public void rewrite(List<String> dataLines) throws PersistenceExceptions {
        Path tmp = Paths.get(path.toString() + ".tmp");
        try {
            List<String> all = new ArrayList<>();
            all.add(header);
            all.addAll(dataLines);
            Files.write(tmp, all, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenceExceptions("No se pudo escribir el archivo: " + path, e);
        }
    }
}
