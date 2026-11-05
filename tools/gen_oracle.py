#!/usr/bin/env python3
"""Generate the IF97 reference grid used by the Android unit tests.

Every row is one (pressure, temperature) pair from the app range plus a few
points inside the narrow region-3 strip. Values come from the independent
Python implementation `iapws` (IAPWS-IF97 R7-97). alpha_v and kappa_t are
obtained by central differences of the specific volume so they do not depend on
that library's own derivative bookkeeping.

Usage:
    python3 tools/gen_oracle.py
"""
import csv
import os

import iapws.iapws97 as m

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(ROOT, "app", "src", "test", "resources", "if97_oracle.csv")

UNSTABLE = 0

PRESSURES = [10 + 10 * i for i in range(18)]      # 10 ... 180 bar
TEMPERATURES = [100 + 25 * i for i in range(21)]  # 100 ... 600 degC
EXTRA = [
    (160, 350.0), (165, 351.0), (170, 353.0), (175, 354.0),
    (180, 350.5), (180, 352.0), (180, 355.0), (180, 356.5),
]


def volume(T, P):
    return m.IAPWS97(T=T, P=P).v


def props(p_bar, t_c):
    T = t_c + 273.15
    P = p_bar / 10.0
    st = m.IAPWS97(T=T, P=P)
    if st.region not in (1, 2, 3):
        return None
    if st.h is None or st.w is None or st.cp is None or st.cv is None:
        return None
    dt = 0.01
    dp = 1e-5
    region = int(st.region)
    # A central difference is only meaningful inside one region; at a region
    # boundary (e.g. exactly 623.15 K) it would blend two formulations.
    region_stable = all(
        m.IAPWS97(T=tt, P=pp).region == region
        for tt, pp in ((T + dt, P), (T - dt, P), (T, P + dp), (T, P - dp))
    )
    if region_stable:
        alpha = float((volume(T + dt, P) - volume(T - dt, P)) / (2 * dt) / st.v)
        kappa = float(-(volume(T, P + dp) - volume(T, P - dp)) / (2 * dp) / st.v)
    else:
        alpha = float("nan")
        kappa = float("nan")
        global UNSTABLE
        UNSTABLE += 1
    u = float(st.h - 1000.0 * P * st.v)
    z = float(1000.0 * P * st.v / (0.461526 * T))
    return {
        "region": region, "v": float(st.v), "h": float(st.h), "u": u,
        "s": float(st.s), "cp": float(st.cp), "cv": float(st.cv), "w": float(st.w),
        "alpha_v": alpha, "kappa_t": kappa, "z": z,
    }


def fmt(x):
    """CSV-safe representation; NaN marks a skipped numerical derivative."""
    return "NaN" if x != x else repr(float(x))


def main():
    pairs = [(p, t) for p in PRESSURES for t in TEMPERATURES] + EXTRA
    os.makedirs(os.path.dirname(TARGET), exist_ok=True)
    written = 0
    with open(TARGET, "w", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["p_bar", "t_c", "region", "v", "h", "u", "s", "cp", "cv",
                    "w", "alpha_v", "kappa_t", "z"])
        for p, t in pairs:
            r = props(p, t)
            if r is None:
                continue
            w.writerow([p, t, r["region"], fmt(r["v"]), fmt(r["h"]), fmt(r["u"]),
                        fmt(r["s"]), fmt(r["cp"]), fmt(r["cv"]), fmt(r["w"]),
                        fmt(r["alpha_v"]), fmt(r["kappa_t"]), fmt(r["z"])])
            written += 1
    print("wrote %d states to %s (%d rows without stable derivative)"
          % (written, TARGET, UNSTABLE))


if __name__ == "__main__":
    main()
