# Astronomical detail views

Tapping a rashi, nakshatra or month ring sector opens its astronomical detail
screen. All 12 rashis, 27 nakshatras and lunar month names are supported. The
previous/next buttons navigate signs/stations or successive physical lunations,
including repeated adhika months. Tithi retains its existing annotation.

`AstronomicalDetailScreen.kt` provides the shared navigation, interval panel and
timeline. `CelestialChart.kt` renders all three view types with a north-up,
east-left stereographic projection. Stars and patterns are bundled, and opening
a chart requires no network. The timeline selects a chart timestamp and can load
that timestamp into the main instrument.

## Coordinates and data

The catalogue contains 5,044 XHIP-derived bright stars from d3-celestial and
39 pattern definitions adapted from Stellarium. `StarCatalog.kt` validates all
pattern star references when loading. Single-star nakshatras remain single-star
patterns; the mockup's invented connecting lines are not used. Full source
attribution is bundled in `assets/licenses/CELESTIAL-DATA-NOTICE.txt`.

Catalogue positions remain fixed; proper motion is not applied. These are
canonical diagrams, not local horizon charts or occultation predictions.
Rashi and nakshatra framing uses a constant J2000 reference configuration;
the masa chart follows the Moon without changing its north-up orientation.

`SwissEphemeris.nativeChart` supplies geocentric Sun/Moon equatorial coordinates
in the J2000 frame, calculated illumination, sidereal longitudes, and a sampled
ecliptic of date transformed to J2000. All native calls share a lock because
Swiss Ephemeris has mutable global configuration. Full chart calculations do
not run for each ordinary instrument longitude request.

Only rashi and nakshatra charts highlight longitude intervals. Months use a
time progress bar. `lunarMonthIntervals` resolves new moons and solar ingresses;
Purnimanta moves the boundaries to the preceding full moons. Month labels follow
the engine's ingress convention. Detail calculations use an independent engine
cache and run off the UI thread.

## Validation status

The dataset was checked for complete 12/27 coverage, unique star IDs, finite
coordinates and magnitudes, and valid edge endpoints. Android compilation,
device rendering, ephemeris comparisons and calendar-boundary regression tests
remain to be run. In particular, review compact screens, font scaling, new/full
Moon rendering, all constellation frames, and adhika/kshaya transitions in both
calendar modes before release.

The initial implementation does not include formal constellation boundaries,
free pan/zoom, or local rise/set predictions. The rashi timing panel labels
solar and lunar transit intervals explicitly.
