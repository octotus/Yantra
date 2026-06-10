package dev.yantra.app.calendar

data class Segment(
    val index: Int,
    val name: String,
    val symbol: String,
    val description: String,
)

data class MonthSector(
    val index: Int,
    val name: String,
    val abbreviation: String,
    val durationDays: Double,
    val arcDegrees: Double,
)

data class YantraState(
    val julianDay: Double,
    val solarLongitude: Double,
    val lunarLongitude: Double,
    val solarAltitude: Double,
    val lunarAltitude: Double,
    val moonIllumination: Double,
    val tithi: Segment,
    val nakshatra: Segment,
    val rashi: Segment,
    val solarRashi: Segment,
    val lunarRashi: Segment,
    val month: MonthSector,
    val monthSectors: List<MonthSector>,
    val samvatsara: Segment,
    val paksha: String,
    val lunarMonth: String,
    val yoga: Segment,
)

object CalendarCatalog {
    val lunarMonths = listOf(
        MonthSector(0, "Chaitra", "CHA", 29.5, 30.0),
        MonthSector(1, "Vaishakha", "VAI", 29.5, 30.0),
        MonthSector(2, "Jyeshtha", "JYE", 29.5, 30.0),
        MonthSector(3, "Ashadha", "ASH", 29.5, 30.0),
        MonthSector(4, "Shravana", "SHR", 29.5, 30.0),
        MonthSector(5, "Bhadrapada", "BHA", 29.5, 30.0),
        MonthSector(6, "Ashwin", "ASW", 29.5, 30.0),
        MonthSector(7, "Kartika", "KAR", 29.5, 30.0),
        MonthSector(8, "Margashirsha", "MAR", 29.5, 30.0),
        MonthSector(9, "Pausha", "PAU", 29.5, 30.0),
        MonthSector(10, "Magha", "MAG", 29.5, 30.0),
        MonthSector(11, "Phalguna", "PHA", 29.5, 30.0),
    )

    val rashis = listOf(
        Segment(0, "Mesha", "Ram", "Initiation, force, and forward motion."),
        Segment(1, "Vrishabha", "Bull", "Stability, fertility, and endurance."),
        Segment(2, "Mithuna", "Pair", "Exchange, relation, and duality."),
        Segment(3, "Karka", "Crab", "Protection, memory, and inner tides."),
        Segment(4, "Simha", "Lion", "Radiance, sovereignty, and courage."),
        Segment(5, "Kanya", "Maiden", "Refinement, discernment, and service."),
        Segment(6, "Tula", "Balance", "Measure, exchange, and harmony."),
        Segment(7, "Vrischika", "Scorpion", "Depth, secrecy, and transformation."),
        Segment(8, "Dhanu", "Bow", "Aim, pilgrimage, and knowledge."),
        Segment(9, "Makara", "Makara", "Persistence, structure, and ascent."),
        Segment(10, "Kumbha", "Pot", "Containment, circulation, and renewal."),
        Segment(11, "Meena", "Fish", "Completion, passage, and dissolution."),
    )

    val nakshatras = listOf(
        Segment(0, "Ashwini", "Horse Head", "Quickening, healing, and arrival."),
        Segment(1, "Bharani", "Yoni", "Containment, passage, and restraint."),
        Segment(2, "Krittika", "Flame", "Cutting, fire, and purification."),
        Segment(3, "Rohini", "Cart", "Growth, beauty, and fertile movement."),
        Segment(4, "Mrigashirsha", "Deer Head", "Seeking, curiosity, and softness."),
        Segment(5, "Ardra", "Diamond", "Intensity, storm, and piercing clarity."),
        Segment(6, "Punarvasu", "Bow", "Return, renewal, and restoration."),
        Segment(7, "Pushya", "Flower", "Nourishment, blessing, and expansion."),
        Segment(8, "Ashlesha", "Coiled Serpent", "Binding, intuition, and hidden force."),
        Segment(9, "Magha", "Throne", "Lineage, authority, and inheritance."),
        Segment(10, "Purva Phalguni", "Pavilion", "Comfort, luxury, hospitality, and pleasure."),
        Segment(11, "Uttara Phalguni", "Kamandalu", "Earned rest, maturity, generosity, and duty."),
        Segment(12, "Hasta", "Hand", "Skill, grasp, and manifestation."),
        Segment(13, "Chitra", "Jewel", "Craft, brilliance, and formation."),
        Segment(14, "Swati", "Sprout", "Independence, movement, and resilience."),
        Segment(15, "Vishakha", "Archway", "Convergence, ambition, and attainment."),
        Segment(16, "Anuradha", "Lotus", "Friendship, devotion, and resilience."),
        Segment(17, "Jyeshtha", "Amulet", "Seniority, protection, and responsibility."),
        Segment(18, "Mula", "Roots", "Rooting, severance, and hidden origin."),
        Segment(19, "Purva Ashadha", "Hand Fan", "Discernment, popularity, and truth separated from falsehood."),
        Segment(20, "Uttara Ashadha", "Elephant Tusk", "Endurance and unyielding strength."),
        Segment(21, "Shravana", "Three Footprints", "Listening, learning, and sacred movement."),
        Segment(22, "Dhanishta", "Drum", "Rhythm, wealth, and communal order."),
        Segment(23, "Shatabhisha", "Circle", "Enclosure, healing, and secrecy."),
        Segment(24, "Purva Bhadrapada", "Twin-Faced Mask", "Transformation and duality."),
        Segment(25, "Uttara Bhadrapada", "Meditating Twins", "Stability and deep contemplation."),
        Segment(26, "Revati", "Fish", "Safe travel and final journeys."),
    )

    val tithis = List(30) { index ->
        val phase = if (index < 15) "Shukla" else "Krishna"
        val number = (index % 15) + 1
        Segment(index, "$phase $number", "Lunar Arc $number", "The Moon is ${index * 12} degrees from the Sun by tithi measure.")
    }

    val yogas = listOf(
        "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana", "Atiganda", "Sukarman",
        "Dhriti", "Shula", "Ganda", "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
        "Siddhi", "Vyatipata", "Variyana", "Parigha", "Shiva", "Siddha", "Sadhya",
        "Shubha", "Shukla", "Brahma", "Indra", "Vaidhriti"
    ).mapIndexed { index, name -> Segment(index, name, "Union ${index + 1}", "A solar-lunar sum divided into the traditional yoga arc.") }

    val samvatsaras = listOf(
        "Prabhava", "Vibhava", "Shukla", "Pramoduta", "Prajotpatti", "Angirasa", "Shrimukha",
        "Bhava", "Yuva", "Dhata", "Ishvara", "Bahudhanya", "Pramathi", "Vikrama", "Vrisha",
        "Chitrabhanu", "Svabhanu", "Tarana", "Parthiva", "Vyaya", "Sarvajit", "Sarvadhari",
        "Virodhi", "Vikriti", "Khara", "Nandana", "Vijaya", "Jaya", "Manmatha", "Durmukhi",
        "Hevilambi", "Vilambi", "Vikari", "Sharvari", "Plava", "Shubhakrit", "Shobhakrit",
        "Krodhi", "Vishvavasu", "Parabhava", "Plavanga", "Kilaka", "Saumya", "Sadharana",
        "Virodhikrit", "Paridhavi", "Pramadi", "Ananda", "Rakshasa", "Nala", "Pingala",
        "Kalayukti", "Siddharthi", "Raudra", "Durmati", "Dundubhi", "Rudhirodgari",
        "Raktakshi", "Krodhana", "Akshaya"
    ).mapIndexed { index, name -> Segment(index, name, "Year Seal ${index + 1}", "One name in the sixty-year Jovian cycle.") }
}
