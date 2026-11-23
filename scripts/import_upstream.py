#!/usr/bin/env python3
"""Import a pinned, MIT-licensed Android scanner and apply pagomi. branding.

Bootstrap only: run once from the repository root, never from routine CI.\nThe upstream revision is deliberately pinned so
builds and license audits are reproducible. Do not run on a dirty checkout.
"""
from pathlib import Path
import json
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = json.loads((ROOT / "branding" / "brand.json").read_text(encoding="utf-8"))
UPSTREAM = "https://github.com/sammyZi/pdf-tools.git"
REVISION = "6a48d60ccbbaca4dd18840b9d85f00d44b51f311"
OLD_PACKAGE = "com.sammyi57.pdftools"
NEW_PACKAGE = SETTINGS["applicationId"]
OLD_PATH = Path("app/src/main/java/com/sammyi57/pdftools")
NEW_PATH = Path("app/src/main/java") / Path(*NEW_PACKAGE.split("."))
OLD_TEST_PATH = Path("app/src/test/java/com/sammyi57/pdftools")
OLD_ANDROID_TEST_PATH = Path("app/src/androidTest/java/com/sammyi57/pdftools")

def replace_file(path: Path, old: str, new: str) -> None:
    value = path.read_text(encoding="utf-8")
    path.write_text(value.replace(old, new), encoding="utf-8")

def copy_tree(src: Path, dst: Path) -> None:
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst)

def main() -> None:
    with tempfile.TemporaryDirectory(prefix="pagomi-upstream-") as temp:
        source = Path(temp) / "source"
        subprocess.run(["git", "clone", "--quiet", "--no-checkout", UPSTREAM, str(source)], check=True)
        subprocess.run(["git", "-C", str(source), "checkout", "--quiet", "--detach", REVISION], check=True)
        for directory in ("app", "gradle"):
            copy_tree(source / directory, ROOT / directory)
        for filename in (".gitignore", "build.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "settings.gradle.kts", "THIRD_PARTY_NOTICES.md"):
            shutil.copy2(source / filename, ROOT / filename)

        # Retain the upstream author's MIT copyright notice, as required.
        (ROOT / "LICENSE").write_text((source / "LICENSE").read_text(encoding="utf-8") + "\nModifications copyright (c) 2026 Brendigo.\n", encoding="utf-8")

    # The original debug signing key is not an identity for a new app.
    (ROOT / "app/debug.keystore").unlink(missing_ok=True)
    for pattern in ("mipmap-hdpi", "mipmap-mdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"):
        shutil.rmtree(ROOT / "app/src/main/res" / pattern, ignore_errors=True)

    paths = [OLD_PATH, OLD_TEST_PATH, OLD_ANDROID_TEST_PATH]
    for old in paths:
        source_path = ROOT / old
        if not source_path.exists():
            continue
        new_relative = Path(str(old).replace("com/sammyi57/pdftools", NEW_PACKAGE.replace(".", "/")))
        dest = ROOT / new_relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(source_path), str(dest))
    for test_root in ("app/src/main/java", "app/src/test/java", "app/src/androidTest/java"):
        for kotlin in (ROOT / test_root).rglob("*.kt"):
            replace_file(kotlin, OLD_PACKAGE, NEW_PACKAGE)
            replace_file(kotlin, "PDF Tools", SETTINGS["brand"])
            replace_file(kotlin, "bhingesamarth@gmail.com", SETTINGS["supportUrl"])
            replace_file(kotlin, "https://forms.gle/Fc3RHMTXAiA8fT5VA", SETTINGS["supportUrl"])
        # Remove the empty old package tree, if any.
        leftover = ROOT / test_root / "com/sammyi57"
        if leftover.exists():
            shutil.rmtree(leftover)

    gradle = ROOT / "app/build.gradle.kts"
    replace_file(gradle, OLD_PACKAGE, NEW_PACKAGE)
    replace_file(gradle, 'versionCode = 3', 'versionCode = 1')
    replace_file(gradle, 'versionName = "2.1.0"', 'versionName = "0.1.0"')
    replace_file(gradle, "PDF_TOOLS_UPLOAD_", "PAGOMI_UPLOAD_")
    # Never inherit the original application's upload or debug signing keys.
    content = gradle.read_text(encoding="utf-8")
    start = content.index("  signingConfigs {")
    end = content.index("\n  buildTypes {", start)
    content = content[:start] + '''  signingConfigs {
    val store = findProperty("PAGOMI_UPLOAD_STORE_FILE") as String?
    if (store != null) create("release") {
      storeFile = file(store)
      storePassword = findProperty("PAGOMI_UPLOAD_STORE_PASSWORD") as String?
      keyAlias = findProperty("PAGOMI_UPLOAD_KEY_ALIAS") as String?
      keyPassword = findProperty("PAGOMI_UPLOAD_KEY_PASSWORD") as String?
    }
  }
''' + content[end:]
    # An unsigned release is safer than silently distributing a debug-signed release.
    content = content.replace('signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")', 'signingConfigs.findByName("release")')
    gradle.write_text(content, encoding="utf-8")
    replace_file(ROOT / "settings.gradle.kts", 'rootProject.name = "PDF Tools"', 'rootProject.name = "pagomi"')

    theme = ROOT / NEW_PATH / "Theme.kt"
    theme_text = theme.read_text(encoding="utf-8")
    for old, new in SETTINGS["colorReplacements"].items():
        theme_text = theme_text.replace(old, new)
    theme_text = theme_text.replace("object Brand {", """object Brand {
  // Brand tokens live here. Edit this single object to re-skin the application.
  const val name = "pagomi."
  const val fullName = "pagomi. – PDF Scanner & Editor"
""")
    theme.write_text(theme_text, encoding="utf-8")

    for path in (ROOT / "app/src/main/res/values/themes.xml", ROOT / "app/src/main/AndroidManifest.xml"):
        replace_file(path, "PDF Tools", SETTINGS["brand"])
        replace_file(path, "#F6F2EE", SETTINGS["colors"]["background"])
    replace_file(ROOT / "app/src/main/res/values/themes.xml",
                 '<string name="app_name">pagomi.</string>',
                 '<string name="app_name">pagomi.</string>')
    # A fresh launcher vector is used on all API levels; avoid upstream branding and stale mipmap files.
    manifest = ROOT / "app/src/main/AndroidManifest.xml"
    replace_file(manifest, "@mipmap/ic_launcher_round", "@drawable/pagomi_launcher")
    replace_file(manifest, "@mipmap/ic_launcher", "@drawable/pagomi_launcher")
    shutil.copy2(ROOT / "branding/assets/pagomi_launcher.xml", ROOT / "app/src/main/res/drawable/pagomi_launcher.xml")
    shutil.copy2(ROOT / "branding/assets/pagomi_launcher.xml", ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml")
    (ROOT / "app/src/main/res/drawable/ic_launcher_background.xml").unlink(missing_ok=True)
    # Existing adaptive icon paths would point at deleted upstream art; remove them.
    shutil.rmtree(ROOT / "app/src/main/res/mipmap-anydpi-v26", ignore_errors=True)

    print("Imported upstream revision:", REVISION)
    print("Android applicationId:", NEW_PACKAGE)
    print("Brand:", SETTINGS["brand"])

if __name__ == "__main__":
    main()
