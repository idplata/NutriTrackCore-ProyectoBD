"""Clasificación de cada porción con CLIP (open_clip, ViT-B-32, en CPU).

Compara el recorte de cada porción con las descripciones en inglés del
catálogo. Además de los alimentos hay clases de "fondo" (plato, mesa,
cubiertos...) para descartar los recortes que no son comida.

Los vectores de texto del catálogo se guardan en index/text_cache.npz y solo
se recalculan cuando cambia el catálogo o los alias.
"""
import hashlib
from pathlib import Path

import numpy as np
import torch
from PIL import Image

ML_DIR = Path(__file__).resolve().parent
CACHE = ML_DIR / "index" / "text_cache.npz"

ARQUITECTURA = "ViT-B-32"
PESOS = "laion2b_s34b_b79k"
PLANTILLAS = ("a photo of {}, a type of food.", "a close-up photo of {} on a plate.")
FONDO = ("an empty plate", "a white plate", "a bowl", "a wooden table", "a tablecloth",
         "a fork and knife", "a spoon", "a napkin", "a cup", "a glass", "a hand", "a tray")


class ClipClassifier:
    """Clasifica recortes de imagen contra el catálogo de alimentos."""

    def __init__(self, alimentos, log=print):
        import open_clip
        self.modelo, _, self.preprocesar = open_clip.create_model_and_transforms(ARQUITECTURA, pretrained=PESOS)
        self.modelo.eval()
        self.tokenizer = open_clip.get_tokenizer(ARQUITECTURA)
        self.alimentos = alimentos
        self.escala = float(self.modelo.logit_scale.exp())
        self._preparar_textos(log)

    # ------------------------------------------------------------------ textos
    def _frases(self):
        """Lista de (índice de alimento o -1 si es fondo, frase)."""
        frases = []
        for i, f in enumerate(self.alimentos):
            for alias in f.aliases:
                for p in PLANTILLAS:
                    frases.append((i, p.format(alias)))
        for fondo in FONDO:
            frases.append((-1, f"a photo of {fondo}."))
        return frases

    @torch.inference_mode()
    def encode_texts(self, textos, lote=256):
        vectores = []
        for i in range(0, len(textos), lote):
            tokens = self.tokenizer(textos[i:i + lote])
            v = self.modelo.encode_text(tokens)
            vectores.append(torch.nn.functional.normalize(v, dim=-1))
        return torch.cat(vectores).cpu().numpy().astype(np.float32)

    def _preparar_textos(self, log):
        frases = self._frases()
        clave = hashlib.sha1(("|".join(f"{i}:{t}" for i, t in frases) + ARQUITECTURA + PESOS)
                             .encode("utf-8")).hexdigest()
        vectores = None
        if CACHE.is_file():
            datos = np.load(CACHE, allow_pickle=False)
            if str(datos["clave"]) == clave:
                vectores = datos["vectores"]
        if vectores is None:
            log(f"[clip] Calculando {len(frases)} frases del catálogo (solo la primera vez)...")
            vectores = self.encode_texts([t for _, t in frases])
            CACHE.parent.mkdir(parents=True, exist_ok=True)
            np.savez(CACHE, clave=np.array(clave), vectores=vectores)
        dueños = np.array([i for i, _ in frases])
        # Un vector por alimento: promedio de sus frases (más estable que una sola)
        n = len(self.alimentos)
        por_alimento = np.zeros((n, vectores.shape[1]), dtype=np.float32)
        for i in range(n):
            v = vectores[dueños == i].mean(axis=0)
            por_alimento[i] = v / np.linalg.norm(v)
        fondo = vectores[dueños == -1]
        self.textos = np.vstack([por_alimento, fondo])   # filas: alimentos y luego fondo
        self.n_alimentos = n

    # ------------------------------------------------------------------ imágenes
    @torch.inference_mode()
    def encode_images(self, imagenes, lote=16):
        vectores = []
        for i in range(0, len(imagenes), lote):
            x = torch.stack([self.preprocesar(im) for im in imagenes[i:i + lote]])
            v = self.modelo.encode_image(x)
            vectores.append(torch.nn.functional.normalize(v, dim=-1))
        return torch.cat(vectores).cpu().numpy().astype(np.float32)

    def classify(self, recortes):
        """Lista de PIL.Image -> lista de (probabilidades por alimento, probabilidad de fondo).

        Las probabilidades salen de un softmax sobre alimentos + fondo, así que
        un recorte de plato vacío deja poca probabilidad a los alimentos.
        """
        if not recortes:
            return []
        imagenes = self.encode_images(recortes)
        logits = self.escala * imagenes @ self.textos.T
        logits -= logits.max(axis=1, keepdims=True)
        prob = np.exp(logits)
        prob /= prob.sum(axis=1, keepdims=True)
        return [(p[:self.n_alimentos], float(p[self.n_alimentos:].sum())) for p in prob]


def recorte_con_mascara(imagen: Image.Image, mascara: np.ndarray, margen: float = 0.08) -> Image.Image:
    """Recorta la porción y pinta de gris lo que queda fuera de la máscara."""
    ys, xs = np.nonzero(mascara)
    alto, ancho = mascara.shape
    y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
    my, mx = int((y1 - y0) * margen), int((x1 - x0) * margen)
    y0, y1 = max(0, y0 - my), min(alto, y1 + my)
    x0, x1 = max(0, x0 - mx), min(ancho, x1 + mx)
    arr = np.asarray(imagen.convert("RGB")).copy()
    arr[~mascara] = (arr[~mascara] * 0.25 + 128 * 0.75).astype(np.uint8)
    return Image.fromarray(arr[y0:y1, x0:x1])