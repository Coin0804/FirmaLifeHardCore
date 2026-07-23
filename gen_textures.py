"""
Generate reinforced dirt textures — TFC dirt with a square timber
cross-section in the center, as if a support beam was hammered in.
"""
import os
from PIL import Image

SRC = "../.fork/TerraFirmaCraft/src/main/resources/assets/tfc/textures/block/dirt"
DST = "src/main/resources/assets/firmalifehardcore/textures/block"
os.makedirs(DST, exist_ok=True)

VARIANTS = [
    "entisol", "aridisol", "oxisol", "fluvisol",
    "andisol", "podzol", "alfisol", "mollisol",
]

# Beam square: centered 6×6 in a 16×16 texture → pixels [5..10]
BX, BY = 5, 5   # top-left of beam square
BW, BH = 6, 6   # beam square size
WOOD = (130, 100, 60)  # base wood color

for name in VARIANTS:
    src_path = os.path.join(SRC, f"{name}.png")
    if not os.path.exists(src_path):
        print(f"  SKIP {name}: source not found")
        continue

    img = Image.open(src_path).convert("RGBA")
    pixels = img.load()
    w, h = img.size

    import random
    random.seed(hash(name) & 0xFFFFFFFF)

    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue

            if BX <= x < BX + BW and BY <= y < BY + BH:
                # ---- Beam square ----
                # Edge: darker outline of the beam
                if x == BX or x == BX + BW - 1 or y == BY or y == BY + BH - 1:
                    r2, g2, b2 = 60, 40, 20  # dark outline
                else:
                    # Inner wood with subtle grain variation
                    r2 = WOOD[0] + random.randint(-8, 8)
                    g2 = WOOD[1] + random.randint(-6, 6)
                    b2 = WOOD[2] + random.randint(-5, 5)
                r, g, b = r2, g2, b2
            else:
                # ---- Surrounding dirt — slightly compacted (darker) ----
                dist = max(BX - x, x - (BX + BW - 1), BY - y, y - (BY + BH - 1), 0)
                if dist == 1:
                    # Immediately adjacent: darker compacted ring
                    r = max(0, r - 25)
                    g = max(0, g - 18)
                    b = max(0, b - 12)
                elif dist == 2:
                    # One pixel further: slight darkening
                    r = max(0, r - 12)
                    g = max(0, g - 8)
                    b = max(0, b - 5)
                # else: unchanged dirt

            pixels[x, y] = (r, g, b, a)

    dst_path = os.path.join(DST, f"reinforced_{name}.png")
    img.save(dst_path)
    print(f"  {name}: {w}x{h} → {dst_path}")

print(f"Done: {len(VARIANTS)} reinforced textures with beam square")
