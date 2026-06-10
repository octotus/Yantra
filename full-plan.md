# Yantra

## An Open-Source Astronomical Instrument for the Hindu Lunisolar Calendar

---

# Vision

Yantra is not a Panchang application.

Yantra is not a calendar application.

Yantra is not an astrology application.

Yantra is a digital astronomical instrument.

The application visualizes the state of the Hindu lunisolar calendar through a system of concentric symbolic rings derived directly from celestial mechanics.

The instrument should feel closer to:

* an astrolabe
* an astronomical clock
* a precision chronometer
* an observatory instrument

than to a modern mobile application.

The instrument should communicate almost entirely through geometry, position, illumination and symbols.

The user should be able to open the application and immediately understand the current state of the sky.

---

# Design Principles

## Instrument First

The instrument itself is the interface.

There are no dashboards.

There are no cards.

There are no information panels.

There are no popup dialogs.

There are no tooltips.

All information must be represented as:

* symbol position
* illumination
* ring position
* celestial geometry

---

## One Screen

The application consists primarily of a single instrument face.

No tabs.

No navigation hierarchy.

No scrolling.

No home screen.

Open app → instrument.

---

## One Explicit Input

A date plaque at the bottom.

Example:

```text
09 JUN 2026
```

Touching the date opens a date selector.

Choosing a date reconfigures the instrument to that moment in time.

The date plaque is the only persistent textual element.

---

# Astronomical Foundation

All displayed information is derived from:

```text
Date
Time
Latitude
Longitude
```

No lookup tables.

No pre-generated Panchang data.

No static calendar databases.

Everything is computed from celestial positions.

---

# Technical Foundation

## Astronomy Engine

Preferred implementation:

* Open source
* Meeus-derived algorithms initially
* Swiss Ephemeris backend optional later

Inputs:

```text
UTC Time
Latitude
Longitude
```

Outputs:

```text
Sun Longitude
Moon Longitude
Moon Phase
Sunrise
Sunset
Moonrise
Moonset
Solar Altitude
Lunar Altitude
```

---

## Calendar Engine

Derived from astronomy.

Calculates:

* Tithi
* Paksha
* Nakshatra
* Solar Rashi
* Lunar Rashi
* Lunar Month
* Adhika Masa
* Kshaya Masa
* Samvatsara

Default system:

* Drik
* Lahiri Ayanamsa

---

# Visual Structure

Outside → Inside

```text
Samvatsara Ring

Solar Rashi Ring

Month Ring

Nakshatra Ring

Lunar Rashi Ring

Tithi Ring

Moon
```

---

# Center

## Moon

The Moon occupies the center of the instrument.

The Moon is rendered according to:

* current phase
* lunar altitude
* local illumination conditions

Examples:

### New Moon

Nearly invisible.

### Full Moon

Strong silver illumination.

### Waxing Crescent

Accurate illuminated crescent.

### Waning Gibbous

Accurate waning phase.

The Moon is the visual anchor of the instrument.

---

# Ring Philosophy

Each ring represents a separate astronomical concept.

No ring is subordinate to another.

Each ring derives from its own astronomical calculation.

---

# Tithi Ring

Represents the current Tithi.

30 symbolic divisions.

Only the active Tithi symbol is illuminated.

Inactive symbols remain engraved bronze.

Tithi is calculated from:

```text
Moon Longitude - Sun Longitude
```

---

# Lunar Rashi Ring

Represents the Moon's current sidereal Rashi.

12 divisions.

Only the active symbol illuminates.

Derived from:

```text
Moon Longitude
```

---

# Nakshatra Ring

27 divisions.

Represents the Moon's current Nakshatra.

Only the active symbol illuminates.

Derived from:

```text
Moon Longitude
```

---

# Month Ring

Represents the current lunar month.

Only one month symbol illuminates.

Month identity is derived from:

* lunation boundaries
* sankrantis
* adhika month logic

Not directly from solar or lunar longitude.

---

# Solar Rashi Ring

Represents the Sun's sidereal Rashi.

12 divisions.

Only the active symbol illuminates.

Derived from:

```text
Sun Longitude
```

This ring balances the otherwise lunar-focused instrument.

---

# Samvatsara Ring

Represents the active year in the 60-year cycle.

Only the current Samvatsara symbol illuminates.

The outermost ring.

The slowest-moving component of the instrument.

---

# Symbol Philosophy

No emojis.

No western zodiac glyphs.

No text labels on the instrument.

No Unicode astrological symbols.

Every element is represented by a custom engraved emblem.

The visual language should feel:

* Indian
* timeless
* astronomical
* mechanical
* precise

---

# Nakshatra Symbol Set

## Ashwini

Horse Head

## Bharani

Yoni

## Krittika

Flame

## Rohini

Cart

## Mrigashirsha

Deer Head

## Ardra

Diamond

## Punarvasu

Bow

## Pushya

Flower

## Ashlesha

Coiled Serpent

## Magha

Throne

## Purva Phalguni

House / Pavilion

Represents comfort, hospitality and luxury.

## Uttara Phalguni

Kamandalu

Represents earned rest, stability and responsibility.

## Hasta

Hand

## Chitra

Jewel

## Swati

Sprout

## Vishakha

Archway

## Anuradha

Lotus

## Jyeshtha

Amulet

## Mula

Roots

## Purva Ashadha

Hand Fan

Represents discernment and separation.

## Uttara Ashadha

Elephant Tusk

Represents endurance and duty.

## Shravana

Three Footprints

## Dhanishta

Drum

## Shatabhisha

Circle

## Purva Bhadrapada

Twin-Faced Mask

## Uttara Bhadrapada

Meditating Twin Forms

## Revati

Fish

---

# Rashi Symbol Set

## Mesha

Ram

## Vrishabha

Bull

## Mithuna

Pair

## Karka

Crab

## Simha

Lion

## Kanya

Maiden

## Tula

Balance

## Vrischika

Scorpion

## Dhanu

Bow

## Makara

Makara

## Kumbha

Pot

## Meena

Fish

All rendered as engraved medallions.

---

# Illumination Philosophy

Only active symbols illuminate.

Inactive symbols remain visible but subdued.

No blinking.

No pulsing.

No excessive glow.

Illumination should resemble reflected light on engraved brass.

---

# Daylight Model

Instrument brightness is determined by solar altitude.

## Sunrise

Warm amber illumination.

## Midday

Maximum brightness.

## Sunset

Golden copper illumination.

## Twilight

Deep blue shadows emerge.

---

# Night Model

Moon becomes primary light source.

## New Moon

Instrument nearly disappears into darkness.

## Quarter Moon

Partial silver illumination.

## Full Moon

Strong silver-white illumination.

---

# Seasonal Model

Instrument appearance changes naturally with:

* latitude
* season
* solar altitude

Examples:

### Montreal Winter

Long night.

Short daylight.

### Montreal Summer

Extended twilight.

### Chennai

Minimal seasonal daylight variation.

---

# Information Discovery

No text cards.

No modal dialogs.

No information overlays.

Touching an element does not display text.

Instead:

The instrument shifts emphasis.

Example:

Touch Moon.

The instrument temporarily brightens:

* Tithi Ring
* Lunar Rashi Ring
* Nakshatra Ring
* Month Ring

revealing the entire lunar state visually.

Touch Solar Rashi.

The instrument emphasizes solar relationships.

The user learns the instrument by observation.

---

# Adhika Masa Representation

Adhika Masa must be represented visually.

No text required.

Proposed:

The active month symbol receives a secondary illuminated halo.

Examples:

```text
Normal Month
    Symbol only

Adhika Month
    Symbol + halo

Kshaya Month
    Symbol + double halo
```

This allows the user to identify Adhika Masa immediately.

---

# Lunar Month Resolver

The month engine determines:

* Month Name
* Adhika Status
* Kshaya Status

Rules:

### Adhika Masa

No Sankranti between two consecutive new moons.

### Nija Month

One Sankranti between two consecutive new moons.

### Kshaya Month

Two Sankrantis between two consecutive new moons.

The month ring displays the resulting state.

---

# Date Plaque

The only persistent text element.

Example:

```text
09 JUN 2026
```

Touching the plaque opens a date picker.

Selecting a date reconfigures the entire instrument.

No other text is required.

---

# Repository Structure

```text
yantra/

astronomy-engine/
calendar-engine/
instrument-renderer/
android-app/

assets/
    nakshatra-symbols/
    rashi-symbols/
    month-symbols/
    samvatsara-symbols/

documentation/
```

---

# Long-Term Goal

Create the most accurate open-source visual astronomical representation of the Hindu lunisolar calendar.

The application should allow a user to understand:

* where the Sun is
* where the Moon is
* what Tithi is active
* what Nakshatra is active
* what Rashi is active
* what Month is active
* what Samvatsara is active

without reading a single word.

The instrument itself becomes the language.

