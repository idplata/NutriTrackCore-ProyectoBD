"""Segmentación de las porciones del plato con SAM (vit_b, en CPU).

SAM no sabe qué es cada región: solo propone máscaras. Aquí se descartan las
muy pequeñas o casi del tamaño de la foto; el clasificador después descarta
las que son plato, mesa o cubiertos.
"""
from pathlib import Path

import numpy as np
import torch
from PIL import Image

ML_DIR = Path(__file__).resolve().parent
PESOS_SAM = ML_DIR / "weights" / "sam_vit_b_01ec64.pth"

LADO_MAXIMO = 768        # la foto se reduce a este lado mayor antes de segmentar (CPU)
AREA_MINIMA = 0.004      # fracción de la foto
AREA_MAXIMA = 0.70


class SamSegmenter:
    """Genera máscaras candidatas de una foto de plato."""

    def __init__(self, puntos_por_lado: int = 16):
        if not PESOS_SAM.is_file():
            raise FileNotFoundError(f"Falta {PESOS_SAM}: ejecuta instalar_dev.bat")
        from segment_anything import SamAutomaticMaskGenerator, sam_model_registry
        torch.set_num_threads(max(1, torch.get_num_threads()))
        sam = sam_model_registry["vit_b"](checkpoint=str(PESOS_SAM))
        sam.to("cpu").eval()
        self.generador = SamAutomaticMaskGenerator(
            sam,
            points_per_side=puntos_por_lado,
            points_per_batch=64,
            pred_iou_thresh=0.86,
            stability_score_thresh=0.90,
            crop_n_layers=0,
            min_mask_region_area=0,   # en 0 no necesita OpenCV; el filtro de área se hace abajo
        )

    @staticmethod
    def reducir(imagen: Image.Image) -> Image.Image:
        """Reduce la foto para que el lado mayor mida LADO_MAXIMO."""
        imagen = imagen.convert("RGB")
        escala = LADO_MAXIMO / max(imagen.size)
        if escala < 1:
            imagen = imagen.resize((round(imagen.width * escala), round(imagen.height * escala)), Image.BILINEAR)
        return imagen

    @torch.inference_mode()
    def segment(self, imagen: Image.Image):
        """Devuelve la lista de máscaras booleanas (alto, ancho), de mayor a menor área."""
        arr = np.asarray(imagen)
        total = arr.shape[0] * arr.shape[1]
        mascaras = []
        for m in self.generador.generate(arr):
            frac = m["area"] / total
            if AREA_MINIMA <= frac <= AREA_MAXIMA:
                mascaras.append(m["segmentation"].astype(bool))
        mascaras.sort(key=lambda m: int(m.sum()), reverse=True)
        return mascaras