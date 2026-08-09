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

data class LagnaSector(
    val index: Int,
    val name: String,
    val startFraction: Double,
    val durationFraction: Double,
)

data class MonthNameSet(
    val id: String,
    val displayName: String,
    val monthNames: List<String>,
    val abbreviations: List<String>,
)

data class CalendarLocaleRule(
    val id: String,
    val displayName: String,
    val newYearMonthIndex: Int,
)

enum class MonthReckoning(val id: String, val displayName: String) {
    Amanta("amanta", "Amanta"),
    Purnimanta("purnimanta", "Purnimanta"),
}

enum class Ayanamsa(
    val id: String,
    val displayName: String,
    internal val swissMode: Int,
    private val offsetFromLahiri: Double,
) {
    Lahiri("lahiri", "Lahiri (Chitrapaksha)", 1, 0.0),
    Raman("raman", "B. V. Raman", 3, -1.446),
    Krishnamurti("krishnamurti", "Krishnamurti (KP)", 5, -0.096),
    ;

    internal fun approximateDegrees(julianDay: Double): Double {
        val yearsSinceJ2000 = (julianDay - 2451545.0) / 365.2425
        return 23.853055 + (50.290966 / 3600.0) * yearsSinceJ2000 + offsetFromLahiri
    }
}

data class YantraState(
    val julianDay: Double,
    val solarLongitude: Double,
    val lunarLongitude: Double,
    val ascendantLongitude: Double?,
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
    val lagnaSectors: List<LagnaSector>,
    val lagnaRashi: Segment?,
    val lagnaDayFraction: Double,
    val samvatsara: Segment,
    val paksha: String,
    val lunarMonth: String,
    val yoga: Segment,
    val monthReckoning: MonthReckoning,
)

data class ObservanceState(
    val tithiIndex: Int,
    val nakshatraName: String,
    val solarRashiName: String,
    val lunarRashiName: String,
    val lunarMonth: String,
    val paksha: String,
    val monthReckoning: MonthReckoning,
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

    val monthNameSets = listOf(
        MonthNameSet(
            id = "en",
            displayName = "English / Sanskrit",
            monthNames = lunarMonths.map { it.name },
            abbreviations = lunarMonths.map { it.abbreviation },
        ),
        MonthNameSet(
            id = "hi",
            displayName = "Hindi",
            monthNames = listOf("चैत्र", "वैशाख", "ज्येष्ठ", "आषाढ़", "श्रावण", "भाद्रपद", "आश्विन", "कार्तिक", "मार्गशीर्ष", "पौष", "माघ", "फाल्गुन"),
            abbreviations = listOf("चै", "वै", "ज्ये", "आषा", "श्रा", "भा", "आश", "का", "मा", "पौ", "माघ", "फा"),
        ),
        MonthNameSet(
            id = "sa",
            displayName = "Sanskrit",
            monthNames = listOf("चैत्रः", "वैशाखः", "ज्येष्ठः", "आषाढः", "श्रावणः", "भाद्रपदः", "आश्विनः", "कार्तिकः", "मार्गशीर्षः", "पौषः", "माघः", "फाल्गुनः"),
            abbreviations = listOf("चै", "वै", "ज्ये", "आषा", "श्रा", "भा", "आश", "का", "मा", "पौ", "माघ", "फा"),
        ),
        MonthNameSet(
            id = "ta",
            displayName = "Tamil",
            monthNames = listOf("சித்திரை", "வைகாசி", "ஆனி", "ஆடி", "ஆவணி", "புரட்டாசி", "ஐப்பசி", "கார்த்திகை", "மார்கழி", "தை", "மாசி", "பங்குனி"),
            abbreviations = listOf("சி", "வை", "ஆனி", "ஆடி", "ஆவ", "பு", "ஐ", "கா", "மா", "தை", "மாசி", "பங்"),
        ),
        MonthNameSet(
            id = "te",
            displayName = "Telugu",
            monthNames = listOf("చైత్రం", "వైశాఖం", "జ్యేష్ఠం", "ఆషాఢం", "శ్రావణం", "భాద్రపదం", "ఆశ్వయుజం", "కార్తీకం", "మార్గశిరం", "పుష్యం", "మాఘం", "ఫాల్గుణం"),
            abbreviations = listOf("చై", "వై", "జ్యే", "ఆషా", "శ్రా", "భా", "ఆశ్", "కా", "మా", "పు", "మాఘ", "ఫా"),
        ),
        MonthNameSet(
            id = "kn",
            displayName = "Kannada",
            monthNames = listOf("ಚೈತ್ರ", "ವೈಶಾಖ", "ಜ್ಯೇಷ್ಠ", "ಆಷಾಢ", "ಶ್ರಾವಣ", "ಭಾದ್ರಪದ", "ಆಶ್ವಯುಜ", "ಕಾರ್ತಿಕ", "ಮಾರ್ಗಶಿರ", "ಪುಷ್ಯ", "ಮಾಘ", "ಫಾಲ್ಗುಣ"),
            abbreviations = listOf("ಚೈ", "ವೈ", "ಜ್ಯೇ", "ಆಷಾ", "ಶ್ರಾ", "ಭಾ", "ಆಶ್", "ಕಾ", "ಮಾ", "ಪು", "ಮಾಘ", "ಫಾ"),
        ),
        MonthNameSet(
            id = "ml",
            displayName = "Malayalam",
            monthNames = listOf("ചൈത്രം", "വൈശാഖം", "ജ്യേഷ്ഠം", "ആഷാഢം", "ശ്രാവണം", "ഭാദ്രപദം", "ആശ്വിനം", "കാർത്തികം", "മാർഗശീർഷം", "പൗഷം", "മാഘം", "ഫാൽഗുനം"),
            abbreviations = listOf("ചൈ", "വൈ", "ജ്യേ", "ആഷാ", "ശ്രാ", "ഭാ", "ആശ്", "കാ", "മാ", "പൗ", "മാഘ", "ഫാ"),
        ),
        MonthNameSet(
            id = "bn",
            displayName = "Bengali",
            monthNames = listOf("চৈত্র", "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন", "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন"),
            abbreviations = listOf("চৈ", "বৈ", "জ্যৈ", "আষা", "শ্রা", "ভা", "আশ", "কা", "অগ্র", "পৌ", "মাঘ", "ফা"),
        ),
        MonthNameSet(
            id = "as",
            displayName = "Assamese",
            monthNames = listOf("চ'ত", "বহাগ", "জেঠ", "আহাৰ", "শাওণ", "ভাদ", "আহিন", "কাতি", "আঘোণ", "পুহ", "মাঘ", "ফাগুন"),
            abbreviations = listOf("চ'", "ব", "জে", "আহা", "শা", "ভা", "আহি", "কা", "আঘ", "পু", "মা", "ফা"),
        ),
        MonthNameSet(
            id = "gu",
            displayName = "Gujarati",
            monthNames = listOf("ચૈત્ર", "વૈશાખ", "જેઠ", "અષાઢ", "શ્રાવણ", "ભાદરવો", "આસો", "કારતક", "માગશર", "પોષ", "મહા", "ફાગણ"),
            abbreviations = listOf("ચૈ", "વૈ", "જેઠ", "અષા", "શ્રા", "ભા", "આસો", "કા", "માગ", "પોષ", "મહા", "ફા"),
        ),
        MonthNameSet(
            id = "mr",
            displayName = "Marathi",
            monthNames = listOf("चैत्र", "वैशाख", "ज्येष्ठ", "आषाढ", "श्रावण", "भाद्रपद", "आश्विन", "कार्तिक", "मार्गशीर्ष", "पौष", "माघ", "फाल्गुन"),
            abbreviations = listOf("चै", "वै", "ज्ये", "आषा", "श्रा", "भा", "आश", "का", "मा", "पौ", "माघ", "फा"),
        ),
        MonthNameSet(
            id = "pa",
            displayName = "Punjabi",
            monthNames = listOf("ਚੇਤ", "ਵੈਸਾਖ", "ਜੇਠ", "ਹਾੜ੍ਹ", "ਸਾਵਣ", "ਭਾਦੋਂ", "ਅੱਸੂ", "ਕੱਤਕ", "ਮੱਘਰ", "ਪੋਹ", "ਮਾਘ", "ਫੱਗਣ"),
            abbreviations = listOf("ਚੇ", "ਵੈ", "ਜੇ", "ਹਾ", "ਸਾ", "ਭਾ", "ਅੱ", "ਕੱ", "ਮੱ", "ਪੋ", "ਮਾਘ", "ਫੱ"),
        ),
        MonthNameSet(
            id = "or",
            displayName = "Odia",
            monthNames = listOf("ଚୈତ୍ର", "ବୈଶାଖ", "ଜ୍ୟେଷ୍ଠ", "ଆଷାଢ଼", "ଶ୍ରାବଣ", "ଭାଦ୍ରବ", "ଆଶ୍ୱିନ", "କାର୍ତ୍ତିକ", "ମାର୍ଗଶୀର", "ପୌଷ", "ମାଘ", "ଫାଲ୍ଗୁନ"),
            abbreviations = listOf("ଚୈ", "ବୈ", "ଜ୍ୟେ", "ଆଷା", "ଶ୍ରା", "ଭା", "ଆଶ୍", "କା", "ମା", "ପୌ", "ମାଘ", "ଫା"),
        ),
    )

    val calendarLocaleRules = listOf(
        CalendarLocaleRule("chaitra", "Chaitra new year", 0),
        CalendarLocaleRule("kartika", "Kartika new year", 7),
        CalendarLocaleRule("mesha", "Mesha / solar new year", 0),
        CalendarLocaleRule("makara", "Makara / Thai-Pongal cycle", 9),
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
