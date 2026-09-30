#!/usr/bin/env python3
"""Генерирует все ресурсы мода: текстуры (PNG 16x16), модели, blockstates, item definitions,
loot tables, рецепты и lang-файлы. Запуск из корня проекта: python3 tools/generate_resources.py
Зависимостей нет (PNG пишется вручную через zlib)."""
import colorsys
import json
import os
import struct
import zlib

MOD = "colorful_tnt"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", MOD)
DATA = os.path.join(ROOT, "data", MOD)

# id, цвет, английское имя, русское имя, ингредиенты рецепта (к обычному TNT)
TYPES = [
    ("white_tnt",   0xF2F2F2, "White TNT",   "Белый динамит",        ["minecraft:white_dye"]),
    ("yellow_tnt",  0xF5D72B, "Yellow TNT",  "Жёлтый динамит",       ["minecraft:yellow_dye"]),
    ("orange_tnt",  0xF28C1E, "Orange TNT",  "Оранжевый динамит",    ["minecraft:orange_dye"]),
    ("red_tnt",     0xD92B2B, "Red TNT",     "Красный динамит",      ["minecraft:red_dye"]),
    ("green_tnt",   0x3DBE3D, "Green TNT",   "Зелёный динамит",      ["minecraft:green_dye"]),
    ("blue_tnt",    0x2F5FE0, "Blue TNT",    "Синий динамит",        ["minecraft:blue_dye"]),
    ("purple_tnt",  0x8E3BD1, "Purple TNT",  "Фиолетовый динамит",   ["minecraft:purple_dye"]),
    ("black_tnt",   0x2A2A2A, "Black TNT",   "Чёрный динамит",       ["minecraft:black_dye"]),
    ("water_tnt",   0x2F8FE6, "Water TNT",   "Водный динамит",       ["minecraft:water_bucket"]),
    ("sand_tnt",    0xE0C878, "Sand TNT",    "Песочный динамит",     ["minecraft:sand"]),
    ("lava_tnt",    0xFF6A10, "Lava TNT",    "Лавовый динамит",      ["minecraft:lava_bucket"]),
    ("rainbow_tnt", 0xFFFFFF, "Rainbow TNT", "Радужный динамит",
     ["minecraft:red_dye", "minecraft:orange_dye", "minecraft:yellow_dye", "minecraft:green_dye",
      "minecraft:light_blue_dye", "minecraft:blue_dye", "minecraft:purple_dye"]),
]

LETTERS = {  # 3x5
    "T": ["###", ".#.", ".#.", ".#.", ".#."],
    "N": ["#.#", "###", "###", "#.#", "#.#"],
}


def png(path, pixels, scale=1):
    h = len(pixels)
    w = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        line = bytearray([0])
        for px in row:
            line += bytes(px) * scale
        for _ in range(scale):
            raw += line
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    data = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", w * scale, h * scale, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
            + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c) + (255,)


def noise(x, y, seed):
    n = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n >> 8) & 255) / 255.0


def base_color(tid, base, x, y, seed):
    k = 0.92 + 0.16 * noise(x, y, seed)
    if tid == "rainbow_tnt":
        r, g, b = colorsys.hsv_to_rgb(x / 16.0, 0.9, 1.0)
        return shade((int(r * 255), int(g * 255), int(b * 255)), k)
    if tid == "water_tnt":
        wave = 1.15 if (x + y * 2) % 6 < 2 else 0.95
        return shade(base, k * wave)
    if tid == "lava_tnt":
        hot = 1.25 if noise(x // 2, y // 2, seed + 7) > 0.6 else 0.9
        return shade(base, k * hot)
    return shade(base, k)


def side_texture(tid, base):
    seed = sum(map(ord, tid))
    px = [[base_color(tid, base, x, y, seed) for x in range(16)] for y in range(16)]
    # тёмные рамки сверху/снизу
    for x in range(16):
        for y in (0, 15):
            px[y][x] = tuple(int(v * 0.6) for v in px[y][x][:3]) + (255,)
    # светлая полоса с надписью TNT
    for y in range(5, 11):
        for x in range(16):
            px[y][x] = (235, 232, 222, 255)
    text = "TNT"
    xs = 3
    for ch in text:
        for ry, row in enumerate(LETTERS[ch]):
            for rx, c in enumerate(row):
                if c == "#":
                    px[5 + ry][xs + rx] = (40, 40, 40, 255) if False else (45, 45, 45, 255)
        xs += 4
    # строка 10 у полосы оставим светлой, 5..9 буквы
    return px


def top_texture(tid, base):
    seed = sum(map(ord, tid)) + 1
    px = [[base_color(tid, base, x, y, seed) for x in range(16)] for y in range(16)]
    for i in range(16):
        for j in range(16):
            if i in (0, 15) or j in (0, 15):
                px[i][j] = tuple(int(v * 0.6) for v in px[i][j][:3]) + (255,)
    for i in range(6, 10):       # «отверстие» для запала
        for j in range(6, 10):
            px[i][j] = (30, 30, 30, 255)
    px[7][7] = px[7][8] = (200, 160, 60, 255)
    return px


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main():
    lang_en, lang_ru = {}, {}
    for tid, color, en, ru, ingredients in TYPES:
        base = rgb(color)
        png(os.path.join(ASSETS, "textures", "block", f"{tid}_side.png"), side_texture(tid, base))
        png(os.path.join(ASSETS, "textures", "block", f"{tid}_top.png"), top_texture(tid, base))

        model = f"{MOD}:block/{tid}"
        write_json(os.path.join(ASSETS, "models", "block", f"{tid}.json"), {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "top": f"{MOD}:block/{tid}_top",
                "bottom": f"{MOD}:block/{tid}_top",
                "side": f"{MOD}:block/{tid}_side",
            },
        })
        write_json(os.path.join(ASSETS, "blockstates", f"{tid}.json"), {
            "variants": {"lit=false": {"model": model}, "lit=true": {"model": model}},
        })
        write_json(os.path.join(ASSETS, "items", f"{tid}.json"), {
            "model": {"type": "minecraft:model", "model": model},
        })
        write_json(os.path.join(DATA, "loot_table", "blocks", f"{tid}.json"), {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:{tid}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })
        write_json(os.path.join(DATA, "recipe", f"{tid}.json"), {
            "type": "minecraft:crafting_shapeless",
            "category": "redstone",
            "ingredients": ["minecraft:tnt"] + ingredients,
            "result": {"id": f"{MOD}:{tid}", "count": 1},
        })
        lang_en[f"block.{MOD}.{tid}"] = en
        lang_ru[f"block.{MOD}.{tid}"] = ru

    write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang_en)
    write_json(os.path.join(ASSETS, "lang", "ru_ru.json"), lang_ru)

    # иконка мода 128x128 из радужной текстуры
    png(os.path.join(ASSETS, "icon.png"), side_texture("rainbow_tnt", rgb(0xFFFFFF)), scale=8)
    print("Ресурсы сгенерированы для", len(TYPES), "видов динамита")


if __name__ == "__main__":
    main()
