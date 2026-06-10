# Month Ring Specification (Revised)

## Purpose

The Month Ring visualizes the structure of the current Vikrama year.

Unlike the other rings, which represent the instantaneous state of the sky, the Month Ring represents the temporal geometry of the entire lunar year.

The ring answers:

> What is the shape of this year?

rather than:

> What is happening right now?

---

# Design Philosophy

The Month Ring does not represent individual lunations.

The Month Ring does not represent month bookkeeping.

The Month Ring does not display separate Adhika and Nija sectors.

Instead, the ring represents the total amount of time occupied by each month name within the current Vikrama year.

The user should immediately perceive:

* normal years
* adhika years
* kshaya years

through geometry alone.

No additional indicators, badges, halos, labels or explanatory text are required.

---

# Ring Structure

The Month Ring contains twelve sectors:

```text
CHA
VAI
JYE
ASH
SHR
BHA
ASW
KAR
MAR
PAU
MAG
PHA
```

representing:

```text
Chaitra
Vaishakha
Jyeshtha
Ashadha
Shravana
Bhadrapada
Ashwin
Kartika
Margashirsha
Pausha
Magha
Phalguna
```

The ring always contains one sector per month name.

---

# Sector Width

Sector width is proportional to the total duration occupied by that month name within the current Vikrama year.

Formula:

```text
sectorArc =
(monthDurationDays / totalYearDays) × 360°
```

Where:

```text
monthDurationDays
```

is the total duration assigned to that month name during the current year.

---

# Normal Year

In a normal year:

```text
12 lunar months
```

Each month occupies approximately:

```text
29–30 days
```

Result:

```text
≈ 30° sector width
```

The ring appears nearly symmetrical.

Example:

```text
CHA 30°
VAI 29°
JYE 30°
ASH 29°
SHR 30°
...
```

---

# Adhika Masa

When an Adhika Masa occurs:

Example:

```text
Adhika Ashadha
29.6 days

Ashadha
29.4 days
```

The durations are combined.

Total Ashadha duration:

```text
59.0 days
```

Ashadha therefore occupies a larger sector.

Example:

```text
CHA 27°
VAI 28°
JYE 27°

ASH 55°

SHR 28°
BHA 27°
...
```

The ring visually communicates:

> This year spent an unusually long time in Ashadha.

No separate Adhika Ashadha sector exists.

No duplicated month labels exist.

No Adhika indicators are required.

The expanded arc itself is the indicator.

---

# Kshaya Masa

When a Kshaya Masa occurs:

The affected month occupies a correspondingly smaller arc.

Example:

```text
CHA 31°
VAI 30°
JYE 30°

ASH 12°

SHR 31°
BHA 30°
...
```

The compressed sector immediately indicates the unusual structure of the year.

No additional Kshaya indicator is required.

---

# Active Month

The currently active month is illuminated.

Example:

```text
ASH
```

glows.

All other month abbreviations remain engraved bronze.

The active month indication is independent of sector width.

---

# Labels

Use three-letter abbreviations.

Examples:

```text
CHA
VAI
JYE
ASH
SHR
BHA
ASW
KAR
MAR
PAU
MAG
PHA
```

The abbreviation is centered within the corresponding arc.

Larger sectors naturally provide more spacing.

Smaller sectors compress gracefully.

---

# Visual Meaning

The Month Ring serves as a fingerprint of the current Vikrama year.

### Normal Year

```text
12 nearly equal sectors
```

### Adhika Year

```text
One expanded sector
```

### Kshaya Year

```text
One compressed sector
```

The user should be able to identify the structure of the year immediately without reading any explanatory text.

---

# Relationship to Other Rings

The Month Ring is unique.

All other rings describe the current astronomical state:

```text
Moon
Tithi
Lunar Rashi
Nakshatra
Solar Rashi
Samvatsara
```

The Month Ring describes the structure of the entire year.

It is therefore the only ring whose geometry depends on a full-year calculation rather than a single instant.

---

# Success Criteria

A user familiar with Yantra should be able to determine:

* the active month
* whether the year contains an Adhika Masa
* whether the year contains a Kshaya Masa
* which month is affected
* the relative distribution of months across the year

using geometry alone, without any additional indicators or explanatory text.

