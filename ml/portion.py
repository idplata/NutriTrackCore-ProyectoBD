"""Estimación de gramos de cada porción.

1. Peso total del plato: promedio ponderado del peso de los platos de
   Nutrition5k más parecidos a la foto (vecinos según DINOv2).
2. Reparto: cada porción recibe una parte del total proporcional a
   área × densidad del alimento.
3. Si los platos vecinos traen ese mismo alimento (según n5k_map.csv), se
   mezcla el reparto con los gramos que tenían allí.

Si no existe el índice (no se ha corrido build_index.py) se usa solo el área
de la foto con un peso de referencia; la confianza baja.
"""
import json
from pathlib import Path

import numpy as np

ML_DIR = Path(__file__).resolve().parent
INDICE = ML_DIR / "index" / "n5k_index.npz"
INDICE_ALIMENTOS = ML_DIR / "index" / "n5k_foods.json"

VECINOS = 7
PESO_REFERENCIA_G = 420.0      # plato típico de Nutrition5k cuando no hay índice
GRAMOS_MIN, GRAMOS_MAX = 5.0, 1500.0


class PortionModel:
    """Calcula los gramos de las porciones detectadas en una foto."""

    def __init__(self, alimentos, embedder=None, log=print):
        self.alimentos = alimentos
        self.embedder = embedder
        self.vectores = None
        if embedder is not None and INDICE.is_file():
            datos = np.load(INDICE, allow_pickle=False)
            self.vectores = datos["vectores"]
            self.masas = datos["masas"]
            self.platos = [str(d) for d in datos["platos"]]
            por_nombre = {f.nombre.lower(): i for i, f in enumerate(alimentos)}
            self.gramos_por_plato = []          # lista de {índice de alimento: gramos}
            contenido = json.loads(INDICE_ALIMENTOS.read_text(encoding="utf-8")) if INDICE_ALIMENTOS.is_file() else {}
            for d in self.platos:
                g = {}
                for nombre, gramos in contenido.get(d, {}).items():
                    i = por_nombre.get(nombre.lower())
                    if i is not None:
                        g[i] = g.get(i, 0.0) + float(gramos)
                self.gramos_por_plato.append(g)
            log(f"[porciones] Índice de Nutrition5k: {len(self.platos)} platos")
        else:
            log("[porciones] Sin índice de Nutrition5k: se estima solo por área (corre build_index.py)")

    def vecinos(self, imagen):
        """Índices y pesos de los platos más parecidos, y su similitud media."""
        v = self.embedder.embed_one(imagen)
        sim = self.vectores @ v
        idx = np.argsort(-sim)[:VECINOS]
        pesos = np.exp((sim[idx] - sim[idx].max()) * 20)
        pesos /= pesos.sum()
        return idx, pesos, float(sim[idx].mean())

    def estimate(self, imagen, porciones):
        """porciones: lista de (índice de alimento, área en píxeles).

        Devuelve (lista de gramos, factor de confianza entre 0 y 1).
        """
        if not porciones:
            return [], 0.0
        area_total = imagen.width * imagen.height
        frac_comida = sum(a for _, a in porciones) / area_total
        idx = pesos = None
        if self.vectores is not None:
            idx, pesos, similitud = self.vecinos(imagen)
            total = float(np.dot(self.masas[idx], pesos))
            factor = float(np.clip((similitud - 0.3) / 0.5, 0.3, 1.0))
        else:
            total = PESO_REFERENCIA_G * float(np.clip(frac_comida / 0.45, 0.25, 2.0))
            factor = 0.5

        bruto = np.array([a * self.alimentos[i].densidad for i, a in porciones], dtype=float)
        reparto = total * bruto / bruto.sum()

        gramos = []
        for k, (i, _) in enumerate(porciones):
            g = reparto[k]
            if idx is not None:
                previos = [(self.gramos_por_plato[j].get(i), w) for j, w in zip(idx, pesos)]
                previos = [(x, w) for x, w in previos if x]
                if previos:
                    peso_ref = sum(w for _, w in previos)
                    ref = sum(x * w for x, w in previos) / peso_ref
                    g = 0.5 * g + 0.5 * ref
            gramos.append(float(np.clip(g, GRAMOS_MIN, GRAMOS_MAX)))
        return gramos, factor