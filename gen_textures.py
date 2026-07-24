"""
Generate reinforced dirt textures:
- Top/bottom: beam cross-section square in center (reinforced_{name}.png)
- Side: subtle vertical crack fill line in center (reinforced_{name}_side.png)
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

# Beam square: centered 6×6 → pixels [5..10]
BX, BY = 5, 5
# Beam square: top-center 6×6 → pixels [5..10, 0..5]
TX, TY = 5, 0
BW, BH = 6, 6
WOOD = (130, 100, 60)

# Crack lines: centered strips
CX, CW = 7, 2  # vertical:   x=[7..8]
CY, CH = 2, 2  # horizontal: y=[2..3]（_end.png 端头 y=0-5，中心 y=2.5，2px线居中）


def make_top(name, img, pixels, w, h):
    """Top/bottom: beam cross-section."""
    import random
    random.seed(hash(name) & 0xFFFFFFFF)

    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            if BX <= x < BX + BW and BY <= y < BY + BH:
                if x == BX or x == BX + BW - 1 or y == BY or y == BY + BH - 1:
                    r, g, b = 60, 40, 20
                else:
                    r = WOOD[0] + random.randint(-8, 8)
                    g = WOOD[1] + random.randint(-6, 6)
                    b = WOOD[2] + random.randint(-5, 5)
            else:
                dist = max(BX - x, x - (BX + BW - 1), BY - y, y - (BY + BH - 1), 0)
                if dist == 1:
                    r, g, b = max(0, r - 25), max(0, g - 18), max(0, b - 12)
                elif dist == 2:
                    r, g, b = max(0, r - 12), max(0, g - 8), max(0, b - 5)
            pixels[x, y] = (r, g, b, a)


def darken(pixels, x, y):
    """Darken a pixel for crack line effect."""
    r, g, b, a = pixels[x, y]
    if a == 0:
        return
    r = max(0, r - 35)
    g = max(0, g - 25)
    b = max(0, b - 18)
    pixels[x, y] = (r, g, b, a)


def make_side(img, pixels, w, h):
    """Side: vertical crack (纵裂纹)."""
    for y in range(h):
        for x in range(CX, CX + CW):
            darken(pixels, x, y)


def make_side_cross(img, pixels, w, h):
    """Side: vertical + horizontal cross crack (纵横裂纹)."""
    for y in range(h):
        for x in range(CX, CX + CW):
            darken(pixels, x, y)
    for y in range(CY, CY + CH):
        for x in range(w):
            darken(pixels, x, y)


def make_side_horiz(img, pixels, w, h):
    """Side: horizontal crack only (横裂纹)."""
    for y in range(CY, CY + CH):
        for x in range(w):
            darken(pixels, x, y)


def draw_beam_square(pixels, w, h, bx, by, name):
    """Draw the beam cross-section square at (bx, by), covering existing pixels."""
    import random
    random.seed(hash(name) & 0xFFFFFFFF)
    for y in range(h):
        for x in range(w):
            if bx <= x < bx + BW and by <= y < by + BH:
                r, g, b, a = pixels[x, y]
                if a == 0:
                    continue
                if x == bx or x == bx + BW - 1 or y == by or y == by + BH - 1:
                    r2, g2, b2 = 60, 40, 20
                else:
                    r2 = WOOD[0] + random.randint(-8, 8)
                    g2 = WOOD[1] + random.randint(-6, 6)
                    b2 = WOOD[2] + random.randint(-5, 5)
                pixels[x, y] = (r2, g2, b2, a)
            elif not (bx <= x < bx + BW and by <= y < by + BH):
                dist = max(bx - x, x - (bx + BW - 1), by - y, y - (by + BH - 1), 0)
                if dist == 1:
                    r, g, b, a = pixels[x, y]
                    if a == 0: continue
                    pixels[x, y] = (max(0, r - 25), max(0, g - 18), max(0, b - 12), a)
                elif dist == 2:
                    r, g, b, a = pixels[x, y]
                    if a == 0: continue
                    pixels[x, y] = (max(0, r - 12), max(0, g - 8), max(0, b - 5), a)


def make_end(name, img, pixels, w, h, with_cross):
    """Side: beam end at top-center, overlaid on crack pattern.
       with_cross=True → 纵横裂纹底（竖梁）, False → 横裂纹底（横梁）."""
    # First draw the crack pattern
    if with_cross:
        make_side_cross(img, pixels, w, h)
    else:
        make_side_horiz(img, pixels, w, h)
    # Then draw beam square on top (覆盖纹路)
    draw_beam_square(pixels, w, h, TX, TY, name)


for name in VARIANTS:
    src_path = os.path.join(SRC, f"{name}.png")
    if not os.path.exists(src_path):
        print(f"  SKIP {name}: source not found")
        continue

    # --- Top/bottom: beam cross-section (端头贴图) ---
    img = Image.open(src_path).convert("RGBA")
    pixels = img.load()
    make_top(name, img, pixels, img.size[0], img.size[1])
    img.save(os.path.join(DST, f"reinforced_{name}.png"))

    # --- Side: vertical crack (纵裂纹) ---
    img2 = Image.open(src_path).convert("RGBA")
    pixels2 = img2.load()
    make_side(img2, pixels2, img2.size[0], img2.size[1])
    img2.save(os.path.join(DST, f"reinforced_{name}_side.png"))

    # --- Side: cross crack (纵横裂纹) ---
    img3 = Image.open(src_path).convert("RGBA")
    pixels3 = img3.load()
    make_side_cross(img3, pixels3, img3.size[0], img3.size[1])
    img3.save(os.path.join(DST, f"reinforced_{name}_side_cross.png"))

    # --- Side: horizontal crack (横裂纹) ---
    img4 = Image.open(src_path).convert("RGBA")
    pixels4 = img4.load()
    make_side_horiz(img4, pixels4, img4.size[0], img4.size[1])
    img4.save(os.path.join(DST, f"reinforced_{name}_side_horiz.png"))

    # --- Side end: 竖梁版（纵横裂纹 + 端头覆盖） ---
    img5 = Image.open(src_path).convert("RGBA")
    pixels5 = img5.load()
    make_end(name, img5, pixels5, img5.size[0], img5.size[1], with_cross=True)
    img5.save(os.path.join(DST, f"reinforced_{name}_end.png"))

    # --- Side end: 横梁版（横裂纹 + 端头覆盖） ---
    img6 = Image.open(src_path).convert("RGBA")
    pixels6 = img6.load()
    make_end(name, img6, pixels6, img6.size[0], img6.size[1], with_cross=False)
    img6.save(os.path.join(DST, f"reinforced_{name}_end_horiz.png"))

    print(f"  {name}: 6 textures")

print(f"Done: {len(VARIANTS)} variants × 6 textures")
