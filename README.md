# CustomTiles

Universal Rooted Quick Settings Extension for Android.

`CustomTiles` is a zero-bloat Android app and suite of Quick Settings tiles engineered for rooted Android devices (KernelSU, Magisk, APatch).

## Features

### 1. 📷 Camera Tile (`CameraTileService`)
- Direct instant camera launch from lockscreen or unlocked desktop.
- Universal support across all Android devices (no root required).
- Works with stock camera, GCam, and third-party cameras.

### 2. ☀️ Max Brightness Tile (`HbmTileService`)
- Pushes modern OLED displays past 100% manual slider limits into **hardware High Brightness Mode (HBM)** (>1,200 to 3,000+ peak nits).
- **Universal Multi-OEM Driver Engine**:
  - **Xiaomi / Redmi / POCO**: Directly modulates `/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param` (MediaTek & Snapdragon).
  - **Qualcomm Snapdragon Devices**: `/sys/class/drm/card0-DSI-1/hbm` and `/sys/class/graphics/fb0/hbm`.
  - **Google Pixel**: `/sys/class/backlight/panel0-backlight/hbm_mode`.
  - **OnePlus / OPPO / Realme**: `/sys/kernel/oppo_display/hbm`.
  - **Universal Fallback**: Disables auto-dimming and pins brightness registers to 4095.
- **Smart Restoration**: Remembers your previous manual brightness level and auto-brightness state, seamlessly restoring them when toggled off.

### 3. 🎨 Light Colors Tile (`LightColorTileService`)
- Washes out / desaturates display colors into an elegant, muted chromatic paper/e-ink aesthetic.
- **0% Yellow**: Unlike night mode or reading mode, white point remains at native crisp 6500K D65 white with zero blue-light warm filter.
- **Hardware-Accelerated**: Modulates `IColorDisplayManager.setSaturationLevel` directly inside the Android GPU compositor (`SurfaceFlinger`) with 0ms lag.
- **Customizable**: Set your preferred saturation level (default 50%, tunable from 0% monochrome to 90% subtle in the companion app).

## Requirements
- Android 8.0+ (API 26+)
- Root access (KernelSU / Magisk / APatch) for HBM and Light Colors.

## Author
- **Bagas Wastu** ([@bgwastu](https://github.com/bgwastu))
