# Steam IF97 — Android water &amp; steam property calculator

A complete, standalone Android app that computes **every thermodynamic property of
water and steam** from pressure and temperature using the
**IAPWS Industrial Formulation 1997 (IF97, R7-97 revision 2007)** — implemented
natively in Kotlin, offline, with no third-party runtime libraries and no
permissions at all.

<p align="center">
  <img src="design/icon-preview.png" alt="App icon in square, circular and squircle masks" width="640">
</p>

* Package id `com.steam.if97` · version 1.0 · minSdk 26 (Android 8.0) · targetSdk 34
* Input range: **10 – 180 bar** and **100 – 600 °C**
* Languages: **English** and **فارسی** (full UI localisation with right-to-left layout)
* Output: 15 properties per state plus a saturated liquid/vapour table

---

## Contents

1. [What it does](#what-it-does)
2. [فارسی](#فارسی)
3. [Features](#features)
4. [Supported range and IF97 regions](#supported-range-and-if97-regions)
5. [Properties and units](#properties-and-units)
6. [Install the ready-made APK](#install-the-ready-made-apk)
7. [Build from source](#build-from-source)
8. [How the physics works](#how-the-physics-works)
9. [Verification](#verification)
10. [Project structure](#project-structure)
11. [Localisation and RTL](#localisation-and-rtl)
12. [Developer scripts](#developer-scripts)
13. [Limitations and scope](#limitations-and-scope)
14. [References](#references)
15. [License](#license)

---

## What it does

You enter a pressure (bar) and a temperature (°C). The app immediately reports the
phase, the IF97 region and the complete thermodynamic state:

* specific volume *v*, density *ρ*, compressibility factor *Z*
* enthalpy *h*, internal energy *u*, entropy *s*
* heat capacities *c<sub>p</sub>* and *c<sub>v</sub>*, isentropic exponent *k = c<sub>p</sub>/c<sub>v</sub>*
* speed of sound *w*
* Gibbs energy *g*, Helmholtz energy *f*
* cubic expansion coefficient *α*, isothermal compressibility *κ<sub>T</sub>*, Joule–Thomson coefficient *μ*<sub>JT</sub>

…plus the saturation data at the entered pressure: *T*<sub>sat</sub> and the
properties of saturated liquid (f) and saturated vapour (g), including the
latent heat. The **About** screen (`⋮ → About`) explains all of this in English
and Persian, with a language switch that changes the whole app.

Nothing leaves the device: the APK has no network permission, no storage, no
analytics.

---

## فارسی

<div dir="rtl" lang="fa">

**استیم IF97** یک برنامهٔ اندروید کامل و مستقل است که همهٔ خواص ترمودینامیکی آب و
بخار را از روی فشار و دما، بر پایهٔ فرمولاسیون صنعتی IAPWS-IF97 (نسخهٔ R7-97،
بازنگری ۲۰۰۷) محاسبه می‌کند. کل فرمولاسیون به زبان کاتلین و بدون هیچ کتابخانهٔ
ترمودینامیکی بیرونی پیاده‌سازی شده است.

* محدودهٔ ورودی: **۱۰ تا ۱۸۰ بار** و **۱۰۰ تا ۶۰۰ درجهٔ سلسیوس**
* خروجی: ۱۵ خاصیت برای هر حالت، به‌همراه جدول مایع اشباع و بخار اشباع در همان فشار
* زبان: انگلیسی و فارسی، با چیدمان کامل راست‌به‌چپ

**نحوهٔ استفاده:** فشار و دما را تایپ کنید یا لغزنده‌ها را بکشید؛ همهٔ خواص هم‌زمان
به‌روز می‌شوند. کارت نخست فاز، ناحیهٔ IF97 و فاصله تا خط اشباع را نشان می‌دهد.
برای خواندن توضیح کامل (به هر دو زبان) از منوی بالا گزینهٔ «درباره» را باز کنید.

**نکته‌های چیدمان:** همهٔ فاصله‌ها با `start/end` تعریف شده‌اند تا در حالت فارسی
به‌درستی آینه شوند، اعداد و واحدها جهت‌گیری چپ‌به‌راست خود را حفظ می‌کنند، و بخش
فارسی صفحهٔ «درباره» با `layoutDirection="rtl"` و `textDirection="rtl"` صریحاً
راست‌به‌چپ چیده می‌شود؛ حتی اگر زبان دستگاه انگلیسی باشد.

</div>

---

## Features

| | |
| --- | --- |
| Live calculation | Recalculates on every keystroke and slider drag — no “compute” button |
| Input help | Numeric text fields **and** sliders, with range validation (`10–180 bar`, `100–600 °C`) |
| Phase detection | Compressed liquid · on the saturation line · superheated steam · supercritical |
| Region badge | Shows which IF97 region the state belongs to, with *p* in **bar** and *T* in **°C** |
| Distance to saturation | “superheated by 20.12 K”, “subcooled liquid, 60.0 K below saturation” |
| Property list | Grouped in *Volumetric*, *Caloric*, *Derived &amp; thermo-elastic* sections |
| Saturation table | *T*<sub>sat</sub>, *v*, *h*, *u*, *s*, *c<sub>p</sub>*, *w* for liquid and vapour + latent heat |
| Bilingual About | Full explanation in English and Persian, both always visible, Persian forced RTL |
| Language switch | English / فارسی buttons switch the entire app locale (stored by AppCompat) |
| Offline | No `INTERNET` permission, no storage, no tracking; all maths on the device |
| Design | Material 3, light/dark, adaptive launcher icon (smiling steam droplet) |

---

## Supported range and IF97 regions

The app accepts 10–180 bar (1–18 MPa) and 100–600 °C (373.15–873.15 K). Inside
that window three single-phase regions occur:

| Region | Physical meaning | Where it appears in this app |
| --- | --- | --- |
| **1** | Compressed / subcooled liquid (Gibbs form) | e.g. 10 bar at 100 °C, 180 bar at 100–350 °C |
| **2** | Superheated steam and saturated vapour (Gibbs form) | most of the grid above *T*<sub>sat</sub> |
| **3** | Near-critical fluid (Helmholtz form) | narrow strip above ≈165.3 bar, e.g. 180 bar at 355 °C |
| **4** | Saturation line *p*<sub>sat</sub>(*T*) / *T*<sub>sat</sub>(*p*) | the saturation card and phase decision |

Region selection follows the official map:

```
T ≤ 623.15 K            → region 1 if p ≥ psat(T), else region 2
623.15 K < T ≤ 863.15 K → region 3 if p ≥ pB23(T), else region 2
T > 863.15 K            → region 2 (up to 1073.15 K)
```

The saturation line is available up to the critical point (22.064 MPa); above it
the app reports a supercritical fluid.

---

## Properties and units

| Symbol | Property | Unit |
| --- | --- | --- |
| *v* | specific volume | m³/kg |
| *ρ* | density | kg/m³ |
| *Z* | compressibility factor *pv/RT* | – |
| *h* | specific enthalpy | kJ/kg |
| *u* | specific internal energy | kJ/kg |
| *s* | specific entropy | kJ/(kg·K) |
| *c<sub>p</sub>* | isobaric heat capacity | kJ/(kg·K) |
| *c<sub>v</sub>* | isochoric heat capacity | kJ/(kg·K) |
| *k* | isentropic exponent *c<sub>p</sub>/c<sub>v</sub>* | – |
| *w* | speed of sound | m/s |
| *g* | specific Gibbs energy *h − Ts* | kJ/kg |
| *f* | specific Helmholtz energy *u − Ts* | kJ/kg |
| *α* | cubic expansion coefficient | 1/K |
| *κ<sub>T</sub>* | isothermal compressibility | 1/bar |
| *μ*<sub>JT</sub> | Joule–Thomson coefficient | °C/bar |

**Every number shown in the app uses bar for pressure and °C for temperature** —
the region badge, the saturation caption, the subcooling/superheat distances and the
compressibility/Joule–Thomson coefficients are all converted before display.
Internally the engine works in IF97 units (MPa, K, kJ/kg), which is also what the
reference values in [Verification](#verification) use.

---

## Install the ready-made APK

```bash
adb install app-debug.apk
```

Or copy the APK to the phone and open it (allow installing from unknown sources).
It is debug-signed, package `com.steam.if97`, and requests no permissions.

---

## Build from source

Requirements: **JDK 17**, **Android SDK** with `platforms;android-34` and
`build-tools;34.0.0` (Android Studio Ladybug or newer works out of the box).

```bash
# 1. point Gradle at your SDK (or let Android Studio create local.properties)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 2. build
./gradlew :app:assembleDebug        # APK → app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # 9 verification tests
./gradlew :app:lintDebug            # Android lint (0 errors)
```

Or open the folder in Android Studio and press ▶.

The Gradle wrapper (8.7) is included, so a local Gradle installation is not required.

---

## How the physics works

Everything is computed from the fundamental equations of IF97; there are no
interpolated steam tables anywhere in the app.

### Region 1 — compressed liquid (Gibbs form)

```
γ(π, τ) = Σ nᵢ (7.1 − π)^Iᵢ (τ − 1.222)^Jᵢ ,   π = p / 16.53 MPa,  τ = 1386 K / T
v = R T π γ_π / (1000 p)      h = R T τ γ_τ      s = R (τ γ_τ − γ)
c_p = −R τ² γ_ττ              c_v = R (−τ² γ_ττ + (γ_π − τ γ_πτ)² / γ_ππ)
w² = R T · 1000 · γ_π² / ((γ_π − τ γ_πτ)²/(τ² γ_ττ) − γ_ππ)
```

### Region 2 — superheated steam (Gibbs form)

Same structure with an ideal-gas part plus a residual part in
π = p/1 MPa and τ = 540 K/T:

```
γ = ln π + Σ nᵢ° τ^Jᵢ°  +  Σ nᵢ π^Iᵢ (τ − 0.5)^Jᵢ
```

and the standard IF97 expressions for *v, h, s, c<sub>p</sub>, c<sub>v</sub>, w*.

### Region 3 — near-critical (Helmholtz form)

```
φ(δ, τ) = n₁ ln δ + Σ nᵢ δ^Iᵢ τ^Jᵢ ,   δ = ρ / 322 kg m⁻³,  τ = 647.096 K / T
p = δ φ_δ R T ρ / 1000        h = R T (τ φ_τ + δ φ_δ)      s = R (τ φ_τ − φ)
c_p = R (−τ² φ_ττ + (δφ_δ − δτφ_δτ)² / (2δφ_δ + δ²φ_δδ))
w² = R T · 1000 · (2δφ_δ + δ²φ_δδ − (δφ_δ − δτφ_δτ)² / (τ² φ_ττ))
α = (φ_δ − τφ_δτ) / (T (2φ_δ + δφ_δδ))       κ_T = 1000 / (ρ R T (2δφ_δ + δ²φ_δδ))
```

Because the user supplies (*p*, *T*) but this formulation is explicit in (*ρ*, *T*),
the app inverts the equation with a bracketed bisection on the correct single-phase
branch (ρ > ρ<sub>c</sub> for liquid-like, ρ < ρ<sub>c</sub> for vapour-like), which
also yields the saturated densities above 165.29 bar.

> Implementation note: an earlier draft computed *α* with a denominator
> `2δφ_δ + δ²φ_δδ`. The correct *thermo-elastic* denominator is
> `2φ_δ + δφ_δδ` (a factor δ); the error was found by comparing against central
> differences of *v* and is now covered by a regression test.

### Region 4 — saturation line and the B23 boundary

* *p*<sub>sat</sub>(*T*) and *T*<sub>sat</sub>(*p*) from the official
  ten-coefficient formulation (valid 273.15 K → 647.096 K, i.e. 611.657 Pa → 22.064 MPa).
* The B23 boundary *p*<sub>B23</sub>(*T*) = n₁ + n₂T + n₃T² separates regions 2 and 3.

### Derived properties

`g = h − Ts`, `f = u − Ts`, `Z = pv/(RT)`, `k = c_p/c_v`,
`μ_JT = v (Tα − 1)/c_p`, with *u = h − pv*.

---

## Verification

Nine JVM tests run on every build (`./gradlew :app:testDebugUnitTest`). They need no
device and no network.

### 1. Official reference values (IAPWS R7-97, tables 5, 15, 33, 36)

| State | *v* (m³/kg) | *h* (kJ/kg) | *u* (kJ/kg) | *s* (kJ/kg·K) | *c<sub>p</sub>* | *w* (m/s) |
| --- | --- | --- | --- | --- | --- | --- |
| Region 1, 300 K / 3 MPa | 1.00215168e-3 | 115.331273 | 112.324818 | 0.392294792 | 4.17301218 | 1507.73921 |
| Region 1, 300 K / 80 MPa | 9.71180894e-4 | 184.142828 | 106.448356 | 0.368563852 | 4.01008987 | 1634.69054 |
| Region 1, 500 K / 3 MPa | 1.20241800e-3 | 975.542239 | 971.934985 | 2.58041912 | 4.65580682 | 1240.71337 |
| Region 2, 300 K / 0.0035 MPa | 39.4913866 | 2549.91145 | 2411.69160 | 8.52238967 | 1.91300162 | 427.920172 |
| Region 2, 700 K / 30 MPa | 5.42946619e-3 | 2631.49474 | 2468.61076 | 5.17540298 | 10.3505092 | 480.386523 |
| Region 3, 650 K / 500 kg·m⁻³ | 2.0e-3 | 1863.43019 | 1812.26279 | 4.05427273 | 13.8935717 | 502.005554 |
| Region 3, 650 K / 200 kg·m⁻³ | 5.0e-3 | 2375.12401 | 2263.65869 | 4.85438792 | 44.6579342 | 383.444594 |
| Region 3, 750 K / 500 kg·m⁻³ | 2.0e-3 | 2258.68845 | 2102.06931 | 4.46971906 | 6.34165359 | 760.696041 |

Saturation line: *p*<sub>sat</sub>(300 K) = 0.00353658941 MPa,
*p*<sub>sat</sub>(500 K) = 2.63889776 MPa, *T*<sub>sat</sub>(1 bar) = 99.605919 °C,
*T*<sub>sat</sub>(10 bar) = 179.885632 °C, *T*<sub>sat</sub>(100 bar) = 310.999488 °C.

All are reproduced to better than 1·10⁻⁸ relative.

### 2. Independent 386-state grid

`OracleGridTest` replays **386 states** — the complete 10–180 bar / 100–600 °C grid
(10 bar and 25 K steps) plus eight points inside the region-3 strip — that were
computed by an **independent implementation** (the Python `iapws` package) into
[`app/src/test/resources/if97_oracle.csv`](app/src/test/resources/if97_oracle.csv).

* region id, *v*, *h*, *u*, *s*, *c<sub>p</sub>*, *c<sub>v</sub>*, *w*, *Z* — matched to **1·10⁻⁶ relative**
* *α* and *κ<sub>T</sub>* — matched to **1·10⁻⁴ relative** (they are differentiated
  numerically in the oracle, and two rows sitting exactly on the 623.15 K region
  boundary are skipped because a finite difference would straddle two formulations)

### Sample states from the app

| *p* | *T* | Region | *v* (m³/kg) | *h* (kJ/kg) | *s* (kJ/kg·K) | *w* (m/s) |
| --- | --- | --- | --- | --- | --- | --- |
| 10 bar | 200 °C | 2 | 0.206004 | 2828.27 | 6.69549 | 517.3 |
| 100 bar | 500 °C | 2 | 0.0328129 | 3375.06 | 6.59932 | 647.9 |
| 180 bar | 355 °C | 3 | 0.00179066 | 1708.29 | 3.83395 | 548.7 |

Reproduce everything from scratch:

```bash
pip install iapws
python3 tools/gen_oracle.py
./gradlew :app:testDebugUnitTest
```

---

## Project structure

```
.
├── app
│   ├── build.gradle.kts
│   └── src
│       ├── main
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/steam/if97
│       │   │   ├── MainActivity.kt            calculator screen + live update
│       │   │   ├── AboutActivity.kt           bilingual About + language switch
│       │   │   └── core
│       │   │       ├── If97Constants.kt       R, T_c, p_c, ρ_c, limits
│       │   │       ├── If97Tables.kt          coefficient tables (generated)
│       │   │       ├── MathUtil.kt            integer power helper
│       │   │       ├── Props.kt               per-region result container
│       │   │       ├── Region1.kt             Gibbs form — compressed liquid
│       │   │       ├── Region2.kt             Gibbs form — superheated steam
│       │   │       ├── Region3.kt             Helmholtz form + density inversion
│       │   │       ├── Region4.kt             saturation line + B23
│       │   │       └── SteamState.kt          public API (If97.state, saturatedAt)
│       │   └── res
│       │       ├── layout/                    activity_main, activity_about, rows
│       │       ├── values/                    English strings, colours, theme
│       │       ├── values-fa/                 Persian strings (RTL)
│       │       ├── xml/locales_config.xml     per-app language list (Android 13+)
│       │       ├── menu/menu_main.xml         About entry
│       │       ├── drawable/                  adaptive-icon foreground + gradient
│       │       └── mipmap-anydpi-v26/         adaptive launcher icons
│       └── test
│           ├── java/com/steam/if97
│           │   ├── If97VerificationTest.kt    R7-97 reference tables + consistency
│           │   └── OracleGridTest.kt          386 states vs independent iapws
│           └── resources/if97_oracle.csv      reference grid
├── design
│   ├── icon.svg                               icon source (SVG)
│   ├── preview_icon.py                        renders square / circle / squircle
│   └── icon-preview.png
├── tools
│   ├── gen_if97_tables.py                     regenerates core/If97Tables.kt
│   └── gen_oracle.py                          regenerates the test grid
├── gradle/wrapper, gradlew, gradlew.bat
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── README.md
```

---

## Localisation and RTL

* All user-facing text lives in `values/strings.xml` (English) and
  `values-fa/strings.xml` (Persian); the About text is deliberately
  `translatable="false"` because **both** languages are shown on that screen.
* `android:supportsRtl="true"` plus `start/end` (never `left/right`) everywhere lets
  the whole calculator mirror automatically when the app runs in Persian.
* The Persian paragraph carries `android:layoutDirection="rtl"`,
  `android:textDirection="rtl"`, `android:textAlignment="viewStart"` and
  `android:gravity="start"`, so it is laid out right-to-left even with an English
  device locale.
* Numbers, unit symbols and formulas are Latin runs inside the Persian text; the
  Unicode bidi algorithm keeps them in the correct visual order, and parenthesised
  expressions such as `p(ρ, T) = p` mirror correctly.
* `AppCompatDelegate.setApplicationLocales()` + `autoStoreLocales` remember the
  chosen language across restarts; `locales_config.xml` exposes both languages in
  the Android 13+ per-app language settings.

---

## Developer scripts

Only needed when the physics tables or the reference grid change — building the app
never requires Python.

| Script | Purpose | Requires |
| --- | --- | --- |
| `tools/gen_if97_tables.py` | Regenerate `core/If97Tables.kt` from the official R7-97 coefficients | `pip install iapws` |
| `tools/gen_oracle.py` | Regenerate the 386-state verification grid | `pip install iapws` |
| `design/preview_icon.py` | Render `design/icon.svg` into the preview sheet | `pip install cairosvg pillow` |

---

## Limitations and scope

* **Two-phase states** cannot be fixed by (*p*, *T*) alone. On the saturation line the
  app shows the saturated liquid/vapour pair instead of a vapour quality; for a
  wet-steam mixture you need one more property (*x*, *v*, *h* or *s*).
* **Pressure–temperature input only** — there is no (*p*, *h*) or (*p*, *s*) solver yet.
* Valid for **pure ordinary water**. Heavy water, seawater and the scientific
  IAPWS-95 formulation are out of scope.
* The app is verified numerically on the JVM; no instrumentation/UI test suite is
  included, and the layouts have been validated by resource compilation and Android
  lint rather than by an emulator in CI.
* IF97 itself is an industrial approximation of IAPWS-95 (≈0.01 % in the main regions);
  do not use it as a calibration standard.

---

## References

* IAPWS, *Revised Release on the IAPWS Industrial Formulation 1997 for the
  Thermodynamic Properties of Water and Steam*, R7-97(2012) —
  <http://www.iapws.org/relguide/IF97-Rev.html>
* IAPWS, *Release on the IAPWS Formulation 1995 for the Thermodynamic Properties of
  Ordinary Water Substance for General and Scientific Use*, R95-97.
* W. Wagner, H.-J. Kretzschmar, *International Steam Tables*, 3rd edition, Springer, 2019.
* Python reference implementation used for the independent verification grid:
  <https://github.com/jjgomera/iapws>

---

## License

The application code in this repository was written for this project. The IF97
coefficients and equations are published by IAPWS and are freely usable with
attribution. **No license file is included yet** — add one (MIT or Apache-2.0 are
typical for apps like this) before distributing publicly.

## Credits

* Formulation: International Association for the Properties of Water and Steam (IAPWS).
* Verification data: cross-checked against the `iapws` Python package.
* UI: Material 3 (AndroidX), single-activity architecture, no third-party runtime code.
