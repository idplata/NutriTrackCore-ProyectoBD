"""Huella visual de un plato completo con DINOv2 (versión small, en CPU).

Se usa para buscar en Nutrition5k los platos que más se parecen a la foto y
tomar de ellos una referencia del peso. La primera vez descarga el modelo
(unos 90 MB) desde Hugging Face y luego lo usa desde la caché.
"""
import numpy as np
import torch
from PIL import Image

MODELO = "facebook/dinov2-small"


class DinoEmbedder:
    """Convierte una imagen en un vector normalizado de 384 valores."""

    def __init__(self, modelo: str = MODELO):
        from transformers import AutoImageProcessor, AutoModel
        self.procesador = AutoImageProcessor.from_pretrained(modelo)
        self.modelo = AutoModel.from_pretrained(modelo).eval()

    @torch.inference_mode()
    def embed(self, imagenes):
        """Lista de PIL.Image (RGB) -> matriz numpy (n, 384) normalizada."""
        entradas = self.procesador(images=list(imagenes), return_tensors="pt")
        salida = self.modelo(**entradas).pooler_output
        salida = torch.nn.functional.normalize(salida, dim=-1)
        return salida.cpu().numpy().astype(np.float32)

    def embed_one(self, imagen: Image.Image):
        return self.embed([imagen])[0]