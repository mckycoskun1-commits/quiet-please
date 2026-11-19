plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.17.493" apply false
    id("dev.architectury.loom-no-remap") version "1.17.493" apply false
}

// src/ klasoru bu surumun halinde tutulur; commit oncesi "Reset active project" calistir
stonecutter active "26.1.2-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)
    properties { tags(version) }

    // //? if fabric / forge / neoforge
    constants.match(loader, "fabric", "forge", "neoforge")

    // Kaynak kod 26.1.2 adlariyla yazilir; diger surumler icin bu adlar degistirilir.
    // Her kural iki yonludur: (26.1.2 deseni, eski ad, eski ad deseni, 26.1.2 adi)
    replacements {
        // 1.21.1 ve oncesi: 26.1 oncesi adlar
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\.extractRenderState\((?=g, |g\.pose\(\), |pose, )""", ".render(", """\.render\((?=g, |g\.pose\(\), |pose, )""", ".extractRenderState(")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            exclude("**/gui/GuiGraphics.java")
            replace("""\bGuiGraphicsExtractor\b""", "GuiGraphics", """\bGuiGraphics\b""", "GuiGraphicsExtractor")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\bIdentifier\b""", "ResourceLocation", """\bResourceLocation\b""", "Identifier")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\bnet\.minecraft\.util\.Util\b""", "net.minecraft.Util", """\bnet\.minecraft\.Util\b""", "net.minecraft.util.Util")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\.scrollAmount\(\)""", ".getScrollAmount()", """\.getScrollAmount\(\)""", ".scrollAmount()")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\bg\.outline\(""", "g.renderOutline(", """\bg\.renderOutline\(""", "g.outline(")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\bg\.centeredText\(""", "g.drawCenteredString(", """\bg\.drawCenteredString\(""", "g.centeredText(")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\bg\.text\(""", "g.drawString(", """\bg\.drawString\(""", "g.text(")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\bg\.item\(""", "g.renderItem(", """\bg\.renderItem\(""", "g.item(")
        }
        regex(current.parsed < "26.1") {
            include("**/*.java")
            replace("""\bscrollBarX\(\)""", "getScrollbarPosition()", """\bgetScrollbarPosition\(\)""", "scrollBarX()")
        }
        // 1.19.2 ve 1.18.2: kendi GUI yardimcilarimiz (Btn, HintBox, Tooltip)
        regex(current.parsed < "1.20") {
            include("**/gui/*.java")
            exclude("**/gui/Btn.java", "**/gui/HintBox.java", "**/gui/Tooltip.java", "**/gui/Slider.java", "**/gui/GuiGraphics.java")
            replace("""\bButton\.builder\(""", "Btn.builder(", """\bBtn\.builder\(""", "Button.builder(")
        }
        regex(current.parsed < "1.20") {
            include("**/gui/*.java")
            exclude("**/gui/Btn.java", "**/gui/HintBox.java", "**/gui/Tooltip.java", "**/gui/Slider.java", "**/gui/GuiGraphics.java")
            replace("""\bButton(?=\s+\w+\s*[=;:,)(])(?!\s+b =)|(?<=List<)Button(?=>)""", "Btn", """\bBtn(?=\s+\w+\s*[=;:,)(])(?!\s+b =)|(?<=List<)Btn(?=>)""", "Button")
        }
        regex(current.parsed < "1.20") {
            include("**/gui/*.java")
            exclude("**/gui/Btn.java", "**/gui/HintBox.java", "**/gui/Tooltip.java", "**/gui/Slider.java", "**/gui/GuiGraphics.java")
            replace("""\bEditBox\b(?=\(|\s+\w+\s*[=;])""", "HintBox", """\bHintBox\b(?=\(|\s+\w+\s*[=;])""", "EditBox")
        }
        regex(current.parsed < "1.20") {
            include("**/gui/*.java")
            exclude("**/gui/Btn.java", "**/gui/HintBox.java", "**/gui/Tooltip.java", "**/gui/Slider.java", "**/gui/GuiGraphics.java")
            replace("""\.withTooltip\(v -> Tooltip\.create\(""", ".withTooltip(v -> Tooltip.lines(", """\.withTooltip\(v -> Tooltip\.lines\(""", ".withTooltip(v -> Tooltip.create(")
        }
        // 1.18.2: Component.literal yok, rebuildWidgets yok
        regex(current.parsed < "1.19") {
            include("**/*.java")
            replace("""\bComponent\.(literal\(|empty\(\))""", "com.quietplease.Txt.\$1", """\bcom\.quietplease\.Txt\.(literal\(|empty\(\))""", "Component.\$1")
        }
        regex(current.parsed < "1.19") {
            include("**/*.java")
            replace("""\brebuildWidgets\(\);""", "init(minecraft, width, height);", """\binit\(minecraft, width, height\);""", "rebuildWidgets();")
        }
        // 26.2 ve sonrasi: ekran ve HUD Minecraft.gui altinda
        regex(current.parsed >= "26.2") {
            include("**/*.java")
            replace("""\b(mc|minecraft|Minecraft\.getInstance\(\))\.setScreen\(""", "\$1.gui.setScreen(", """\b(mc|minecraft|Minecraft\.getInstance\(\))\.gui\.setScreen\(""", "\$1.setScreen(")
        }
        regex(current.parsed >= "26.2") {
            include("**/*.java")
            replace("""\b(mc|Minecraft\.getInstance\(\))\.screen\b(?!\()""", "\$1.gui.screen()", """\b(mc|Minecraft\.getInstance\(\))\.gui\.screen\(\)""", "\$1.screen")
        }
        regex(current.parsed >= "26.2") {
            include("**/*.java")
            replace("""\.gui\.(getChat|setOverlayMessage)\(""", ".gui.hud.\$1(", """\.gui\.hud\.(getChat|setOverlayMessage)\(""", ".gui.\$1(")
        }
        regex(current.parsed >= "26.2") {
            include("**/*.java")
            replace("""\.getOverlay\(\)""", ".gui.overlay()", """\.gui\.overlay\(\)""", ".getOverlay()")
        }
    }
}

stonecutter flags {
    // Jar MANIFEST.MF dosyasina Stonecutter satirlari eklenmesin
    set(dev.kikugie.stonecutter.controller.flag.StonecutterFlag.GENERATE_MANIFEST, false)
}

// Butun jar'lari derleyip dist/ klasorune kopyalar
tasks.register("collectJars") {
    group = "build"
    description = "Builds every Minecraft version and loader and copies the jars to dist/"
    dependsOn(subprojects.map { "${it.path}:collectJar" })
}
