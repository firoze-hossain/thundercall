#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Thundercall Icon Generator
#
# IMPORTANT:
# The TC artwork has a DARK BLUE background INSIDE the rounded
# border. We KEEP that background.
#
# Only the WHITE/OFF-WHITE area OUTSIDE the rounded border is
# removed and made transparent.
#
# Outputs:
#   Windows -> Thundercall.ico
#   Linux   -> Thundercall.png
#   macOS   -> Thundercall.icns
# ============================================================

SRC="${1:-src/main/resources/images/TC.png}"
OUT="src/main/resources/packaging"

echo "========================================"
echo " Thundercall Icon Generator"
echo "========================================"
echo "Source: $SRC"
echo "Output: $OUT"
echo

if ! command -v magick >/dev/null 2>&1; then
    echo "ERROR: ImageMagick is not installed."
    echo "Install with: brew install imagemagick"
    exit 1
fi

if ! command -v iconutil >/dev/null 2>&1; then
    echo "ERROR: iconutil is not available."
    echo "Run this script on macOS."
    exit 1
fi

if [[ ! -f "$SRC" ]]; then
    echo "ERROR: Source image not found:"
    echo "  $SRC"
    exit 1
fi

mkdir -p "$OUT"

MASTER="$OUT/Thundercall-master.png"
ICONSET="$OUT/Thundercall.iconset"

rm -rf "$ICONSET"
rm -f "$MASTER"
rm -f "$OUT/Thundercall.png"
rm -f "$OUT/Thundercall.ico"
rm -f "$OUT/Thundercall.icns"

# ------------------------------------------------------------
# 1. Remove ONLY the outside WHITE background.
#
# Do NOT use "-transparent black" or flood-fill black.
# The dark blue background inside the TC rounded-square icon
# must remain untouched.
#
# Corner flood-fill removes only pixels connected to the
# outside corners. White details inside the logo remain.
# ------------------------------------------------------------

echo "[1/3] Removing only outside white background..."

magick "$SRC" \
    -alpha on \
    -fuzz 8% \
    -fill none \
    -draw "color 0,0 floodfill" \
    -draw "color 100%,0 floodfill" \
    -draw "color 0,100% floodfill" \
    -draw "color 100%,100% floodfill" \
    "$MASTER"

# ------------------------------------------------------------
# 2. Put the transparent artwork on a 1024x1024 canvas.
#
# Keep the entire rounded TC icon and give it transparent
# padding around the outside.
# ------------------------------------------------------------

echo "[2/3] Creating 1024x1024 transparent master..."

magick "$MASTER" \
    -trim \
    +repage \
    -background none \
    -gravity center \
    -resize '900x900' \
    -extent 1024x1024 \
    "$MASTER"

# Make absolutely sure the four corners remain transparent.
magick "$MASTER" \
    -alpha on \
    -fuzz 5% \
    -fill none \
    -draw "color 0,0 floodfill" \
    -draw "color 100%,0 floodfill" \
    -draw "color 0,100% floodfill" \
    -draw "color 100%,100% floodfill" \
    "$MASTER"

# ------------------------------------------------------------
# 3. Linux PNG
# ------------------------------------------------------------

echo "[3/3] Creating Windows, Linux and macOS icons..."

cp "$MASTER" "$OUT/Thundercall.png"

# Windows EXE/MSI
magick "$MASTER" \
    -define icon:auto-resize=16,24,32,48,64,128,256 \
    "$OUT/Thundercall.ico"

# macOS ICNS
mkdir -p "$ICONSET"

for SIZE in 16 32 128 256 512; do
    DOUBLE=$((SIZE * 2))

    magick "$MASTER" \
        -background none \
        -resize "${SIZE}x${SIZE}" \
        -gravity center \
        -extent "${SIZE}x${SIZE}" \
        "$ICONSET/icon_${SIZE}x${SIZE}.png"

    magick "$MASTER" \
        -background none \
        -resize "${DOUBLE}x${DOUBLE}" \
        -gravity center \
        -extent "${DOUBLE}x${DOUBLE}" \
        "$ICONSET/icon_${SIZE}x${SIZE}@2x.png"
done

iconutil \
    -c icns \
    "$ICONSET" \
    -o "$OUT/Thundercall.icns"

rm -rf "$ICONSET"
rm -f "$MASTER"

echo
echo "========================================"
echo " Thundercall icons generated"
echo "========================================"
echo
echo "Windows:"
echo "  $OUT/Thundercall.ico"
echo
echo "Linux:"
echo "  $OUT/Thundercall.png"
echo
echo "macOS:"
echo "  $OUT/Thundercall.icns"
echo

echo "Checking corners of Linux PNG..."

for POS in "0,0" "1023,0" "0,1023" "1023,1023"; do
    VALUE="$(magick "$OUT/Thundercall.png" \
        -format "%[pixel:p{$POS}]" info:)"
    echo "  $POS -> $VALUE"
done

echo
echo "Expected: srgba(...,0) at all four corners."
echo
ls -lh \
    "$OUT/Thundercall.png" \
    "$OUT/Thundercall.ico" \
    "$OUT/Thundercall.icns"
