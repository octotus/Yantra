package dev.yantra.app.ui

import dev.yantra.app.calendar.YantraState

internal enum class FestivalRank {
    Major,
    Minor,
}

internal data class FestivalDefinition(
    val name: String,
    val rank: FestivalRank,
    val month: String? = null,
    val paksha: String? = null,
    val tithiNumber: Int? = null,
    val solarRashi: String? = null,
    val previousSolarRashi: String? = null,
    val nakshatra: String? = null,
    val astronomicalDefinition: String,
)

internal object FestivalCatalog {
    private val definitions = listOf(
        FestivalDefinition("Makara Sankranti", FestivalRank.Major, solarRashi = "Makara", previousSolarRashi = "Dhanu", astronomicalDefinition = "Solar ingress into sidereal Makara."),
        FestivalDefinition("Chaitra New Year", FestivalRank.Major, month = "Chaitra", paksha = "Shukla", tithiNumber = 1, astronomicalDefinition = "Chaitra Shukla Pratipada."),
        FestivalDefinition("Rama Navami", FestivalRank.Major, month = "Chaitra", paksha = "Shukla", tithiNumber = 9, astronomicalDefinition = "Chaitra Shukla Navami."),
        FestivalDefinition("Akshaya Tritiya", FestivalRank.Major, month = "Vaishakha", paksha = "Shukla", tithiNumber = 3, astronomicalDefinition = "Vaishakha Shukla Tritiya."),
        FestivalDefinition("Krishna Janmashtami", FestivalRank.Major, month = "Bhadrapada", paksha = "Krishna", tithiNumber = 8, astronomicalDefinition = "Bhadrapada Krishna Ashtami."),
        FestivalDefinition("Navaratri Begins", FestivalRank.Major, month = "Ashwin", paksha = "Shukla", tithiNumber = 1, astronomicalDefinition = "Ashwin Shukla Pratipada."),
        FestivalDefinition("Vijayadashami", FestivalRank.Major, month = "Ashwin", paksha = "Shukla", tithiNumber = 10, astronomicalDefinition = "Ashwin Shukla Dashami."),
        FestivalDefinition("Diwali", FestivalRank.Major, month = "Kartika", paksha = "Krishna", tithiNumber = 15, astronomicalDefinition = "Kartika Krishna Amavasya."),
        FestivalDefinition("Karthika Deepam", FestivalRank.Major, month = "Kartika", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Kartika Shukla Purnima, traditionally associated with Krittika nakshatra."),
        FestivalDefinition("Holi", FestivalRank.Minor, month = "Phalguna", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Phalguna Shukla Purnima."),
        FestivalDefinition("Thiruvadirai", FestivalRank.Major, solarRashi = "Dhanu", nakshatra = "Ardra", astronomicalDefinition = "Tamil Margazhi / solar Dhanu when Ardra (Thiruvathirai) nakshatra prevails, traditionally on or near the full moon night."),
        FestivalDefinition("Vaikuntha Ekadashi", FestivalRank.Major, solarRashi = "Dhanu", paksha = "Shukla", tithiNumber = 11, astronomicalDefinition = "Solar Dhanu masa, Shukla Paksha, Ekadashi tithi."),
        FestivalDefinition("Maha Shivaratri", FestivalRank.Major, month = "Phalguna", paksha = "Krishna", tithiNumber = 14, astronomicalDefinition = "Phalguna Krishna Chaturdashi."),
        FestivalDefinition("Hanuman Jayanti", FestivalRank.Minor, month = "Chaitra", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Chaitra Shukla Purnima."),
        FestivalDefinition("Guru Purnima", FestivalRank.Minor, month = "Ashadha", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Ashadha Shukla Purnima."),
        FestivalDefinition("Nag Panchami", FestivalRank.Minor, month = "Shravana", paksha = "Shukla", tithiNumber = 5, astronomicalDefinition = "Shravana Shukla Panchami."),
        FestivalDefinition("Upakarma", FestivalRank.Major, month = "Shravana", paksha = "Shukla", tithiNumber = 15, astronomicalDefinition = "Shravana Shukla Purnima."),
        FestivalDefinition("Ganesh Chaturthi", FestivalRank.Major, month = "Bhadrapada", paksha = "Shukla", tithiNumber = 4, astronomicalDefinition = "Bhadrapada Shukla Chaturthi."),
        FestivalDefinition("Vasant Panchami", FestivalRank.Minor, month = "Magha", paksha = "Shukla", tithiNumber = 5, astronomicalDefinition = "Magha Shukla Panchami."),
        FestivalDefinition("Gita Jayanti", FestivalRank.Minor, month = "Margashirsha", paksha = "Shukla", tithiNumber = 11, astronomicalDefinition = "Margashirsha Shukla Ekadashi."),
        FestivalDefinition("Ekadashi", FestivalRank.Minor, tithiNumber = 11, astronomicalDefinition = "Ekadashi tithi in either paksha."),
        FestivalDefinition("Pradosham", FestivalRank.Minor, tithiNumber = 13, astronomicalDefinition = "Trayodashi tithi in either paksha."),
    )

    fun match(state: YantraState, previousState: YantraState): FestivalDefinition? {
        val tithiNumber = (state.tithi.index % 15) + 1
        return definitions.firstOrNull { festival ->
            (festival.month == null || festival.month == state.lunarMonth) &&
                (festival.paksha == null || festival.paksha == state.paksha) &&
                (festival.tithiNumber == null || festival.tithiNumber == tithiNumber) &&
                (festival.solarRashi == null || festival.solarRashi == state.solarRashi.name) &&
                (festival.previousSolarRashi == null || festival.previousSolarRashi == previousState.solarRashi.name) &&
                (festival.nakshatra == null || festival.nakshatra == state.nakshatra.name)
        }
    }
}
