package com.nutritrackcore.camera;

/**
 * Páginas HTML que ve el celular. La de subida reduce la foto a 1600 px
 * y la convierte a JPEG con {@code <canvas>} antes de enviarla.
 */
final class PhoneUploadPage {

    private PhoneUploadPage() {
    }

    static String form() {
        return """
            <!DOCTYPE html>
            <html lang="es"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>NutriTrack - Foto del plato</title>
            <style>
              body{font-family:sans-serif;margin:0;padding:24px;text-align:center;background:#f4f7f4}
              h1{font-size:1.3em} label{display:block;margin:24px auto;padding:18px;max-width:320px;
              background:#2e7d32;color:#fff;border-radius:12px;font-size:1.2em}
              input{display:none} #estado{margin-top:16px;font-size:1.05em}
            </style></head><body>
            <h1>NutriTrack Core</h1>
            <p>Toma una foto del plato completo, desde arriba y con buena luz.</p>
            <label>Tomar foto<input id="foto" type="file" accept="image/*" capture="environment"></label>
            <div id="estado"></div>
            <script>
            const estado = document.getElementById('estado');
            document.getElementById('foto').addEventListener('change', async (e) => {
              const file = e.target.files[0];
              if (!file) return;
              estado.textContent = 'Enviando foto...';
              let body = file, type = file.type || 'image/jpeg';
              try {
                const bmp = await createImageBitmap(file);
                const scale = Math.min(1, 1600 / Math.max(bmp.width, bmp.height));
                const canvas = document.createElement('canvas');
                canvas.width = Math.round(bmp.width * scale);
                canvas.height = Math.round(bmp.height * scale);
                canvas.getContext('2d').drawImage(bmp, 0, 0, canvas.width, canvas.height);
                body = await new Promise(r => canvas.toBlob(r, 'image/jpeg', 0.85));
                type = 'image/jpeg';
              } catch (err) { /* se envía el archivo original */ }
              try {
                const res = await fetch(location.pathname, {method: 'POST', headers: {'Content-Type': type}, body});
                estado.textContent = await res.text();
              } catch (err) {
                estado.textContent = 'No se pudo enviar. Revisa que estés en la misma red Wi-Fi.';
              }
            });
            </script></body></html>
            """;
    }

    static String expired() {
        return message("Sesión vencida. Abre de nuevo el enlace desde NutriTrack.");
    }

    static String message(String text) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>"
                + "<body style=\"font-family:sans-serif;padding:24px;text-align:center\"><p>" + safe + "</p></body></html>";
    }
}