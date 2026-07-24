"""Generate all reinforced soil block models and blockstates."""
import json
import os

DIR_MODELS = "src/main/resources/assets/firmalifehardcore/models/block"
DIR_BLOCKSTATES = "src/main/resources/assets/firmalifehardcore/blockstates"

VARIANTS = [
    "entisol", "aridisol", "oxisol", "fluvisol",
    "andisol", "podzol", "alfisol", "mollisol",
]

# ===== Normal block models (vertical parent) =====
STATES = {
    "none": {
        "north": "_side", "south": "_side", "east": "_side", "west": "_side",
    },
    "x": {
        "north": "_side_cross", "south": "_side_cross", "east": "_end", "west": "_end",
    },
    "z": {
        "north": "_end", "south": "_end", "east": "_side_cross", "west": "_side_cross",
    },
    "both": {
        "north": "_end", "south": "_end", "east": "_end", "west": "_end",
    },
}

# ===== Beam block models (horizontal parent) =====
BEAM_STATES = {
    "none": {
        "north": "_orig", "south": "_orig", "east": "_orig", "west": "_orig",
    },
    "x": {
        "north": "_side_horiz", "south": "_side_horiz", "east": "_end_horiz", "west": "_end_horiz",
    },
    "z": {
        "north": "_end_horiz", "south": "_end_horiz", "east": "_side_horiz", "west": "_side_horiz",
    },
    "both": {
        "north": "_end_horiz", "south": "_end_horiz", "east": "_end_horiz", "west": "_end_horiz",
    },
}

os.makedirs(DIR_MODELS, exist_ok=True)
os.makedirs(DIR_BLOCKSTATES, exist_ok=True)

count = 0
for name in VARIANTS:
    base = f"firmalifehardcore:block/reinforced_{name}"
    dirt = f"tfc:block/dirt/{name}"

    # ---- Normal models ----
    for state, sides in STATES.items():
        tex = {
            "particle": base + "_side",
            "top": base,
            "bottom": base,
            "north": base + sides["north"],
            "south": base + sides["south"],
            "east": base + sides["east"],
            "west": base + sides["west"],
        }
        path = os.path.join(DIR_MODELS, f"reinforced_{name}_{state}.json")
        with open(path, "w") as f:
            json.dump({"parent": "firmalifehardcore:block/reinforced_soil_vertical", "textures": tex}, f, indent=2)
            f.write("\n")
        count += 1

    # ---- Beam models ----
    for state, sides in BEAM_STATES.items():
        tex = {
            "particle": dirt,
            "top": dirt,
            "bottom": dirt,
            "north": dirt if sides["north"] == "_orig" else base + sides["north"],
            "south": dirt if sides["south"] == "_orig" else base + sides["south"],
            "east": dirt if sides["east"] == "_orig" else base + sides["east"],
            "west": dirt if sides["west"] == "_orig" else base + sides["west"],
        }
        path = os.path.join(DIR_MODELS, f"reinforced_{name}_beam_{state}.json")
        with open(path, "w") as f:
            json.dump({"parent": "firmalifehardcore:block/reinforced_soil_horizontal", "textures": tex}, f, indent=2)
            f.write("\n")
        count += 1

    # ---- Normal blockstate ----
    bs = {
        "variants": {
            "axis_x=false,axis_z=false": {"model": f"firmalifehardcore:block/reinforced_{name}_none"},
            "axis_x=true,axis_z=false":  {"model": f"firmalifehardcore:block/reinforced_{name}_x"},
            "axis_x=false,axis_z=true":  {"model": f"firmalifehardcore:block/reinforced_{name}_z"},
            "axis_x=true,axis_z=true":   {"model": f"firmalifehardcore:block/reinforced_{name}_both"},
        }
    }
    path = os.path.join(DIR_BLOCKSTATES, f"reinforced_{name}.json")
    with open(path, "w") as f:
        json.dump(bs, f, indent=2)
        f.write("\n")

    # ---- Beam blockstate ----
    bs_beam = {
        "variants": {
            "axis_x=false,axis_z=false": {"model": f"firmalifehardcore:block/reinforced_{name}_beam_none"},
            "axis_x=true,axis_z=false":  {"model": f"firmalifehardcore:block/reinforced_{name}_beam_x"},
            "axis_x=false,axis_z=true":  {"model": f"firmalifehardcore:block/reinforced_{name}_beam_z"},
            "axis_x=true,axis_z=true":   {"model": f"firmalifehardcore:block/reinforced_{name}_beam_both"},
        }
    }
    path = os.path.join(DIR_BLOCKSTATES, f"reinforced_{name}_beam.json")
    with open(path, "w") as f:
        json.dump(bs_beam, f, indent=2)
        f.write("\n")

# Delete old simple beam model files (no longer needed)
for name in VARIANTS:
    old = os.path.join(DIR_MODELS, f"reinforced_{name}_beam.json")
    if os.path.exists(old):
        os.remove(old)

print(f"Generated {count} model files + {len(VARIANTS)*2} blockstate files")
