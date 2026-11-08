"""Gera os modelos (codigo Java) e as texturas dos invocados do mod: Lobo Espectral (3 niveis) e Golem Runico (3 niveis).

Uso: python tools/gen_summon_models.py
Saidas:
  src/main/java/com/arcane/client/model/SummonLayers.java       (LayerDefinitions)
  src/main/resources/assets/arcane/textures/entity/summon/*.png (texturas + camadas luminosas)
Os cubos sao declarados UMA vez aqui; o empacotador de UV decide onde cada um fica na textura, e a mesma tabela
gera o Java e pinta a textura (nao ha como UV e modelo desencontrarem).
"""
import os
import random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_JAVA = os.path.join(ROOT, "src/main/java/com/arcane/client/model/SummonLayers.java")
OUT_TEX = os.path.join(ROOT, "src/main/resources/assets/arcane/textures/entity/summon")


class Cube:
    def __init__(self, uv, x, y, z, w, h, d, mat, mirror=False, inflate=0.0):
        self.uv, self.x, self.y, self.z, self.w, self.h, self.d = uv, x, y, z, w, h, d
        self.mat, self.mirror, self.inflate = mat, mirror, inflate


class Part:
    def __init__(self, name, pivot, cubes, parent=None, rot=(0, 0, 0)):
        self.name, self.pivot, self.cubes, self.parent, self.rot = name, pivot, cubes, parent, rot


class Model:
    def __init__(self, key, tw, th, parts):
        self.key, self.tw, self.th, self.parts = key, tw, th, parts
        self.uvbox = {}  # nome do uv -> (u, v, w, h, d, mat)

    def collect(self):
        seen = {}
        for p in self.parts:
            for c in p.cubes:
                key = c.uv
                dims = (int(round(c.w)), int(round(c.h)), int(round(c.d)))
                if key in seen:
                    assert seen[key][0] == dims, (key, dims, seen[key])
                else:
                    seen[key] = (dims, c.mat)
        # empacotador em prateleiras (maiores primeiro)
        items = []
        for k, (dims, mat) in seen.items():
            w, h, d = dims
            items.append((k, 2 * (w + d), h + d, w, h, d, mat))
        items.sort(key=lambda t: -t[2])
        x = y = shelf = 0
        for k, bw, bh, w, h, d, mat in items:
            if x + bw > self.tw:
                x, y, shelf = 0, y + shelf, 0
            assert y + bh <= self.th, f"textura {self.key} cheia"
            self.uvbox[k] = (x, y, w, h, d, mat)
            x += bw
            shelf = max(shelf, bh)


# ----------------------------------------------------------------------------- LOBO ESPECTRAL (128x64)
def wolf_model():
    P = []
    body = Part("body", (0, 13, 2), [Cube("body", -4, -3, -7, 8, 7, 14, "fur")])
    mane = Part("mane", (0, 13, -3.5), [Cube("mane", -5, -4, -3.5, 10, 9, 7, "fur_light")])
    head = Part("head", (0, 11.5, -5.5), [Cube("head", -3.5, -3, -5, 7, 6, 5, "fur")])
    snout = Part("snout", (0, 0, 0), [Cube("snout", -2, 0, -9, 4, 3, 4, "fur_dark"),
                                      Cube("jaw", -1.5, 3, -8.5, 3, 1, 3, "bone"),
                                      Cube("fang", -1.5, 3, -9, 1, 2, 1, "bone"),
                                      Cube("fang", 0.5, 3, -9, 1, 2, 1, "bone")], parent="head")
    ear_l = Part("ear_l", (0, 0, 0), [Cube("ear", 1.5, -6, -1, 2, 3, 1, "fur_dark")], parent="head")
    ear_r = Part("ear_r", (0, 0, 0), [Cube("ear", -3.5, -6, -1, 2, 3, 1, "fur_dark", mirror=True)], parent="head")
    legs = []
    for name, x, z in (("leg_fr", -2.5, -2.0), ("leg_fl", 2.5, -2.0), ("leg_br", -2.5, 6.5), ("leg_bl", 2.5, 6.5)):
        legs.append(Part(name, (x, 17, z), [Cube("leg", -1.5, 0, -1.5, 3, 6, 3, "fur", mirror=x > 0),
                                            Cube("paw", -2, 6, -2.2, 4, 1, 4, "armor_dark", mirror=x > 0)]))
    tail = Part("tail", (0, 12, 9), [Cube("tail", -1.5, -0.5, -1.5, 3, 8, 3, "fur_light"),
                                     Cube("tail_tip", -1, 7.5, -1, 2, 3, 2, "glow_tip")], rot=(1.15, 0, 0))
    spikes = []
    for i, z in enumerate((-4, -0.5, 3, 6.5)):
        spikes.append(Part(f"spike{i}", (0, 10, z), [Cube("spike", -0.5, -3, -0.5, 1, 3, 1, "bone")], rot=(0.25, 0, 0)))
    # nivel 1 (Alfa): chifres, ombreiras, colar de espinhos
    horn_l = Part("horn1_l", (0, 0, 0), [Cube("horn", 2, -9, -2, 1, 4, 1, "bone"), Cube("horn_tip", 2.25, -10.5, -2, 1, 2, 1, "bone")], parent="head")
    horn_r = Part("horn1_r", (0, 0, 0), [Cube("horn", -3, -9, -2, 1, 4, 1, "bone", mirror=True), Cube("horn_tip", -3.25, -10.5, -2, 1, 2, 1, "bone", mirror=True)], parent="head")
    pauld_l = Part("pauld1_l", (4, 11.5, -3), [Cube("pauld", 0, 0, -3, 2, 6, 6, "armor"), Cube("pspike", 1, -2, -0.5, 1, 2, 1, "bone")])
    pauld_r = Part("pauld1_r", (-4, 11.5, -3), [Cube("pauld", -2, 0, -3, 2, 6, 6, "armor", mirror=True), Cube("pspike", -2, -2, -0.5, 1, 2, 1, "bone", mirror=True)])
    # nivel 2 (Fenrir): coroa, segunda fileira de espinhos grandes, peitoral e chamas na cauda
    crown = Part("crown2", (0, 0, 0), [Cube("crown", -2.5, -8, -1, 5, 2, 1, "armor"), Cube("crown_s", -0.5, -11, -1, 1, 3, 1, "bone")], parent="head")
    chest = Part("chest2", (0, 13, -4), [Cube("chest", -4.5, 3.5, -4, 9, 3, 6, "armor")])
    big_spikes = []
    for i, z in enumerate((-3, 1, 5)):
        big_spikes.append(Part(f"bigspike{i}", (0, 10, z), [Cube("bigspike", -1, -7, -1, 2, 7, 2, "glow_tip")], rot=(0.35, 0, 0)))
    return Model("wolf", 128, 64, [body, mane, head, snout, ear_l, ear_r, *legs, tail, *spikes,
                                    horn_l, horn_r, pauld_l, pauld_r, crown, chest, *big_spikes])


# ----------------------------------------------------------------------------- GOLEM RUNICO (128x128)
def golem_model():
    legs = []
    for name, x in (("leg_r", -4.5), ("leg_l", 4.5)):
        legs.append(Part(name, (x, 8, 0), [Cube("leg", -3.5, 0, -3, 7, 14, 6, "stone", mirror=x > 0),
                                           Cube("foot", -4, 14, -4.5, 8, 2, 9, "stone_dark", mirror=x > 0)]))
    pelvis = Part("pelvis", (0, 8, 0), [Cube("pelvis", -6, -3, -4, 12, 4, 8, "stone_dark")])
    torso = Part("torso", (0, 6, 0), [Cube("torso", -8, -10, -5, 16, 10, 10, "stone"),
                                      Cube("core", -3, -8, -5.6, 6, 6, 1, "core")])
    chest = Part("chest", (0, -4, 0), [Cube("chest", -11, -8, -6, 22, 8, 12, "stone_dark")])
    head = Part("head", (0, -12, -1), [Cube("head", -5, -9, -5, 10, 9, 10, "stone"), Cube("brow", -5.5, -6, -6, 11, 2, 2, "stone_dark")])
    arms = []
    for name, x in (("arm_r", -14), ("arm_l", 14)):
        arms.append(Part(name, (x, -10, 0), [Cube("arm", -3.5, 0, -3.5, 7, 20, 7, "stone", mirror=x > 0),
                                             Cube("fist", -5, 18, -5, 10, 9, 10, "stone_dark", mirror=x > 0),
                                             Cube("pauld", -5, -4, -5, 10, 5, 10, "armor", mirror=x > 0)]))
    crystals = []
    for i, x in enumerate((-6, 0, 6)):
        crystals.append(Part(f"crystal{i}", (x, -6, 6), [Cube("crystal", -1, -7, -1, 2, 7, 2, "core")], rot=(-0.3, 0, 0)))
    # nivel 1 (Colosso): coroa de chifres e espinhos nas ombreiras
    horn_l = Part("horn1_l", (0, 0, 0), [Cube("ghorn", 3, -13, -2, 2, 5, 2, "armor"), Cube("ghorn_t", 3, -16, -2, 2, 3, 2, "bone")], parent="head")
    horn_r = Part("horn1_r", (0, 0, 0), [Cube("ghorn", -5, -13, -2, 2, 5, 2, "armor", mirror=True), Cube("ghorn_t", -5, -16, -2, 2, 3, 2, "bone", mirror=True)], parent="head")
    ps_l = Part("pspike1_l", (0, 0, 0), [Cube("gpspike", -1, -10, -1, 2, 6, 2, "bone")], parent="arm_l")
    ps_r = Part("pspike1_r", (0, 0, 0), [Cube("gpspike", -1, -10, -1, 2, 6, 2, "bone", mirror=True)], parent="arm_r")
    # nivel 2: aureola de cristais e punhos com cravos
    halo = []
    for i, x in enumerate((-4, 0, 4)):
        halo.append(Part(f"halo2_{i}", (x, -21, -1), [Cube("halo", -0.5, -6, -0.5, 1, 6, 1, "core")], rot=(0, 0, (i - 1) * 0.35)))
    knuckles_l = Part("knuck2_l", (0, 0, 0), [Cube("knuck_s", -4, 21, -6, 2, 2, 1, "bone"), Cube("knuck_s", 2, 21, -6, 2, 2, 1, "bone")], parent="arm_l")
    knuckles_r = Part("knuck2_r", (0, 0, 0), [Cube("knuck_s", -4, 21, -6, 2, 2, 1, "bone"), Cube("knuck_s", 2, 21, -6, 2, 2, 1, "bone")], parent="arm_r")
    return Model("golem", 128, 128, [*legs, pelvis, torso, chest, head, *arms, *crystals, horn_l, horn_r, ps_l, ps_r,
                                      *halo, knuckles_l, knuckles_r])


# ----------------------------------------------------------------------------- Java
def jf(v):
    s = ("%.4f" % v).rstrip("0").rstrip(".")
    return (s if s not in ("", "-0") else "0") + "F"


def java_layer(model, fname):
    L = [f"  public static LayerDefinition {fname}() {{",
         "    MeshDefinition mesh = new MeshDefinition();",
         "    PartDefinition root = mesh.getRoot();"]
    emitted = {}
    for p in model.parts:
        parent = "root" if p.parent is None else emitted[p.parent]
        var = "p_" + p.name
        emitted[p.name] = var
        L.append(f"    PartDefinition {var} = {parent}.addOrReplaceChild(\"{p.name}\", CubeListBuilder.create()")
        for c in p.cubes:
            u, v, *_ = model.uvbox[c.uv]
            mir = ".mirror()" if c.mirror else ""
            unm = ".mirror(false)" if c.mirror else ""
            L.append(f"        .texOffs({u}, {v}){mir}.addBox({jf(c.x)}, {jf(c.y)}, {jf(c.z)}, {jf(c.w)}, {jf(c.h)}, {jf(c.d)}){unm}")
        px, py, pz = p.pivot
        rx, ry, rz = p.rot
        if p.parent is not None:
            # filhos de cabeca/membro: mesma origem do pai
            px = py = pz = 0
        L.append(f"        , PartPose.offsetAndRotation({jf(px)}, {jf(py)}, {jf(pz)}, {jf(rx)}, {jf(ry)}, {jf(rz)}));")
    L.append(f"    return LayerDefinition.create(mesh, {model.tw}, {model.th});")
    L.append("  }")
    return "\n".join(L)


# ----------------------------------------------------------------------------- Texturas
def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, k):
    return tuple(clamp(x * k) for x in c)


PALETTES = {
    # nome: (base por material, brilho)
    "wolf0": dict(fur=(52, 56, 78), fur_light=(78, 84, 112), fur_dark=(34, 36, 52), bone=(205, 208, 222), armor=(60, 62, 80),
                  armor_dark=(36, 38, 52), glow_tip=(120, 200, 255), glow=(90, 190, 255)),
    "wolf1": dict(fur=(36, 36, 48), fur_light=(70, 70, 92), fur_dark=(22, 22, 32), bone=(225, 225, 235), armor=(120, 128, 150),
                  armor_dark=(60, 64, 80), glow_tip=(170, 130, 255), glow=(160, 110, 255)),
    "wolf2": dict(fur=(24, 20, 24), fur_light=(52, 36, 36), fur_dark=(14, 12, 16), bone=(235, 215, 190), armor=(70, 40, 36),
                  armor_dark=(36, 24, 24), glow_tip=(255, 160, 60), glow=(255, 120, 40)),
    "golem0": dict(stone=(110, 112, 120), stone_dark=(76, 78, 88), armor=(150, 150, 160), core=(110, 220, 255), bone=(200, 205, 215), glow=(110, 220, 255)),
    "golem1": dict(stone=(86, 78, 100), stone_dark=(56, 50, 70), armor=(130, 120, 150), core=(190, 130, 255), bone=(220, 210, 235), glow=(190, 130, 255)),
    "golem2": dict(stone=(60, 46, 46), stone_dark=(38, 28, 30), armor=(110, 70, 60), core=(255, 150, 60), bone=(240, 220, 190), glow=(255, 150, 60)),
}
EMISSIVE_MATS = {"glow_tip", "core"}


def paint_face(img, glow, x0, y0, w, h, base, mat, rnd, face, pal, kind):
    for yy in range(h):
        for xx in range(w):
            n = rnd.random()
            k = 0.86 + 0.28 * n
            if kind == "wolf" and mat.startswith("fur"):
                # pelagem: veios verticais
                k *= 0.92 + 0.16 * ((xx * 7 + yy // 2) % 3 == 0)
            if mat in ("stone", "stone_dark", "armor") and kind == "golem":
                if (xx + yy * 3) % 11 == 0:
                    k *= 0.7  # rachaduras
            if face == "top":
                k *= 1.12
            elif face == "bottom":
                k *= 0.7
            elif face in ("left", "right"):
                k *= 0.9
            col = shade(base, k)
            # moldura clara nas bordas de armaduras
            if mat in ("armor", "armor_dark", "stone_dark") and (xx in (0, w - 1) or yy in (0, h - 1)):
                col = shade(base, 1.25)
            img.putpixel((x0 + xx, y0 + yy), col + (255,))
            if mat in EMISSIVE_MATS:
                g = pal["glow_tip"] if "glow_tip" in pal and mat == "glow_tip" else pal["core"]
                glow.putpixel((x0 + xx, y0 + yy), tuple(clamp(c * (0.8 + 0.4 * n)) for c in g) + (255,))
            elif mat.startswith("fur") and kind == "wolf" and face in ("left", "right") and w >= 6 and yy == h // 2 and xx % 3 != 2:
                # veia luminosa na lateral
                glow.putpixel((x0 + xx, y0 + yy), pal["glow"] + (210,))
            elif mat in ("stone", "stone_dark") and kind == "golem" and face in ("front",) and w >= 8 and ((xx - yy) % 9 == 0) and yy % 2 == 0:
                glow.putpixel((x0 + xx, y0 + yy), pal["glow"] + (170,))


def paint_model(model, pal_key, kind):
    pal = PALETTES[pal_key]
    img = Image.new("RGBA", (model.tw, model.th), (0, 0, 0, 0))
    glow = Image.new("RGBA", (model.tw, model.th), (0, 0, 0, 0))
    rnd = random.Random(hash(pal_key) & 0xFFFF)
    rnd = random.Random(sum(ord(c) * (i + 3) for i, c in enumerate(pal_key)))
    for name, (u, v, w, h, d, mat) in model.uvbox.items():
        base = pal.get(mat, (128, 128, 128))
        faces = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "left": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                 "right": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
        for face, (fx, fy, fw, fh) in faces.items():
            paint_face(img, glow, fx, fy, max(1, fw), max(1, fh), base, mat, rnd, face, pal, kind)
    # olhos luminosos na frente da cabeca
    hu, hv, hw, hh, hd, _ = model.uvbox["head"]
    fx, fy = hu + hd, hv + hd
    eye = pal["glow"]
    if kind == "wolf":
        for ex in (1, hw - 3):
            for dx in range(2):
                img.putpixel((fx + ex + dx, fy + 2), eye + (255,))
                glow.putpixel((fx + ex + dx, fy + 2), (255, 255, 255, 255))
                img.putpixel((fx + ex + dx, fy + 3), shade(eye, 0.8) + (255,))
                glow.putpixel((fx + ex + dx, fy + 3), eye + (255,))
    else:
        for ex in (1, hw - 4):
            for dx in range(3):
                glow.putpixel((fx + ex + dx, fy + 4), (255, 255, 255, 255))
                img.putpixel((fx + ex + dx, fy + 4), eye + (255,))
                glow.putpixel((fx + ex + dx, fy + 5), eye + (255,))
                img.putpixel((fx + ex + dx, fy + 5), eye + (255,))
    return img, glow


def main():
    os.makedirs(OUT_TEX, exist_ok=True)
    os.makedirs(os.path.dirname(OUT_JAVA), exist_ok=True)
    wolf, golem = wolf_model(), golem_model()
    wolf.collect()
    golem.collect()
    java = ["// GERADO por tools/gen_summon_models.py - nao edite a mao.",
            "package com.arcane.client.model;", "",
            "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
            "import net.minecraft.client.model.geom.builders.LayerDefinition;",
            "import net.minecraft.client.model.geom.builders.MeshDefinition;",
            "import net.minecraft.client.model.geom.builders.PartDefinition;",
            "import net.minecraft.client.model.geom.PartPose;", "",
            "public final class SummonLayers {", "  private SummonLayers() {}", "",
            java_layer(wolf, "wolf"), "", java_layer(golem, "golem"), "}", ""]
    with open(OUT_JAVA, "w", encoding="utf-8") as f:
        f.write("\n".join(java))
    for tier in range(3):
        for model, kind, prefix in ((wolf, "wolf", "wolf"), (golem, "golem", "golem")):
            img, glow = paint_model(model, f"{prefix}{tier}", kind)
            img.save(os.path.join(OUT_TEX, f"{prefix}_{tier}.png"))
            glow.save(os.path.join(OUT_TEX, f"{prefix}_{tier}_glow.png"))
    print("ok", len(wolf.uvbox), len(golem.uvbox))


if __name__ == "__main__":
    main()
