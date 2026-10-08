#!/usr/bin/env python3
"""Derive transparent line artwork from Stellarium's anchored Western illustrations."""

import argparse
from pathlib import Path

import cv2
import numpy as np


NAMES = (
    "aries", "taurus", "gemini", "cancer", "leo", "virgo",
    "libra", "scorpius", "sagittarius", "capricornus", "aquarius", "pisces",
)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("stellarium_western", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    for name in NAMES:
        source = args.stellarium_western / "illustrations" / f"{name}.webp"
        grayscale = cv2.imread(str(source), cv2.IMREAD_GRAYSCALE)
        if grayscale is None:
            raise FileNotFoundError(source)
        # Keep only the exterior contours. Internal shading and anatomical lines
        # compete with the constellation itself when the artwork is projected.
        silhouette = np.where(grayscale > 8, 255, 0).astype(np.uint8)
        silhouette = cv2.morphologyEx(
            silhouette,
            cv2.MORPH_CLOSE,
            cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5)),
        )
        contours, _ = cv2.findContours(silhouette, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        edges = np.zeros_like(grayscale)
        cv2.drawContours(
            edges,
            [contour for contour in contours if cv2.contourArea(contour) > 8],
            -1,
            255,
            2,
            cv2.LINE_AA,
        )
        artwork = cv2.cvtColor(grayscale, cv2.COLOR_GRAY2BGRA)
        artwork[:, :, :3] = (105, 210, 255)  # gold in BGRA order
        artwork[:, :, 3] = edges
        destination = args.output / f"zodiac_art_{name}.png"
        if not cv2.imwrite(str(destination), artwork):
            raise OSError(f"Could not write {destination}")


if __name__ == "__main__":
    main()
