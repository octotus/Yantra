#!/usr/bin/env python3
"""Derive transparent line artwork from Stellarium's anchored Western illustrations."""

import argparse
from pathlib import Path

import cv2


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
        edges = cv2.Canny(cv2.GaussianBlur(grayscale, (3, 3), 0), 18, 65)
        edges = cv2.dilate(edges, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (2, 2)))
        artwork = cv2.cvtColor(grayscale, cv2.COLOR_GRAY2BGRA)
        artwork[:, :, :3] = (105, 210, 255)  # gold in BGRA order
        artwork[:, :, 3] = edges
        destination = args.output / f"zodiac_art_{name}.png"
        if not cv2.imwrite(str(destination), artwork):
            raise OSError(f"Could not write {destination}")


if __name__ == "__main__":
    main()
