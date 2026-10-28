package com.nutritrackcore.ui;

/**
 * Pantalla que vuelve a leer sus datos de los servicios cada vez que se
 * muestra. {@link AppShellPanel} llama a {@link #refresh()} al abrirla.
 */
public interface Refreshable {

    /** Vuelve a leer los datos y redibuja la pantalla. */
    void refresh();
}
