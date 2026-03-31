package com.example.calendarapp.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class ReminderIconOption(val key: String, val label: String, val icon: ImageVector)
data class IconCategory(val name: String, val tabIcon: ImageVector, val icons: List<ReminderIconOption>)

val ReminderIconCategories: List<IconCategory> = listOf(

    IconCategory("General", Icons.Filled.Notifications, listOf(
        ReminderIconOption("notifications",  "Reminder",   Icons.Filled.Notifications),
        ReminderIconOption("star",           "Important",  Icons.Filled.Star),
        ReminderIconOption("alarm",          "Alarm",      Icons.Filled.Alarm),
        ReminderIconOption("event",          "Event",      Icons.Filled.Event),
        ReminderIconOption("warning",        "Warning",    Icons.Filled.Warning),
        ReminderIconOption("people",         "People",     Icons.Filled.People),
        ReminderIconOption("celebration",    "Party",      Icons.Filled.Celebration),
        ReminderIconOption("shoppingCart",   "Shopping",   Icons.Filled.ShoppingCart),
        ReminderIconOption("flight",         "Travel",     Icons.Filled.Flight),
        ReminderIconOption("directionsCar",  "Drive",      Icons.Filled.DirectionsCar),
        ReminderIconOption("musicNote",      "Music",      Icons.Filled.MusicNote),
        ReminderIconOption("movie",          "Movie",      Icons.Filled.Movie),
        ReminderIconOption("childCare",      "Kids",       Icons.Filled.ChildCare),
        ReminderIconOption("photoCamera",    "Photo",      Icons.Filled.PhotoCamera),
        ReminderIconOption("pets",           "Pets",       Icons.Filled.Pets),
        ReminderIconOption("familyRestroom", "Family",     Icons.Filled.FamilyRestroom),
    )),

    IconCategory("Health", Icons.Filled.Favorite, listOf(
        ReminderIconOption("medicalServices",  "Medical",    Icons.Filled.MedicalServices),
        ReminderIconOption("favorite",         "Health",     Icons.Filled.Favorite),
        ReminderIconOption("spa",              "Spa",        Icons.Filled.Spa),
        ReminderIconOption("psychology",       "Mental",     Icons.Filled.Psychology),
        ReminderIconOption("monitorHeart",     "Heart",      Icons.Filled.MonitorHeart),
        ReminderIconOption("vaccines",         "Vaccine",    Icons.Filled.Vaccines),
        ReminderIconOption("localPharmacy",    "Pharmacy",   Icons.Filled.LocalPharmacy),
        ReminderIconOption("healing",          "Healing",    Icons.Filled.Healing),
        ReminderIconOption("healthAndSafety",  "Safety",     Icons.Filled.HealthAndSafety),
        ReminderIconOption("medication",       "Medication", Icons.Filled.Medication),
        ReminderIconOption("accessibilityNew", "Mobility",   Icons.Filled.AccessibilityNew),
        ReminderIconOption("mood",             "Mood",       Icons.Filled.Mood),
        ReminderIconOption("selfImprovement",  "Mindful",    Icons.Filled.SelfImprovement),
        ReminderIconOption("waterDrop",        "Hydration",  Icons.Filled.WaterDrop),
    )),

    IconCategory("Nature", Icons.Filled.Eco, listOf(
        ReminderIconOption("eco",               "Plant",    Icons.Filled.Eco),
        ReminderIconOption("park",              "Park",     Icons.Filled.Park),
        ReminderIconOption("localFlorist",      "Flower",   Icons.Filled.LocalFlorist),
        ReminderIconOption("grass",             "Grass",    Icons.Filled.Grass),
        ReminderIconOption("forest",            "Forest",   Icons.Filled.Forest),
        ReminderIconOption("energySavingsLeaf", "Leaf",     Icons.Filled.EnergySavingsLeaf),
        ReminderIconOption("terrain",           "Terrain",  Icons.Filled.Terrain),
        ReminderIconOption("beachAccess",       "Beach",    Icons.Filled.BeachAccess),
        ReminderIconOption("wbSunny",           "Sunny",    Icons.Filled.WbSunny),
        ReminderIconOption("cloud",             "Cloud",    Icons.Filled.Cloud),
        ReminderIconOption("yard",              "Yard",     Icons.Filled.Yard),
        ReminderIconOption("filterVintage",     "Floral",   Icons.Filled.FilterVintage),
        ReminderIconOption("acUnit",            "Cold",     Icons.Filled.AcUnit),
        ReminderIconOption("thunderstorm",      "Storm",    Icons.Filled.Thunderstorm),
    )),

    IconCategory("Food", Icons.Filled.Restaurant, listOf(
        ReminderIconOption("restaurant",      "Restaurant", Icons.Filled.Restaurant),
        ReminderIconOption("localCafe",       "Cafe",       Icons.Filled.LocalCafe),
        ReminderIconOption("localBar",        "Bar",        Icons.Filled.LocalBar),
        ReminderIconOption("emojiFoodBev",    "Drink",      Icons.Filled.EmojiFoodBeverage),
        ReminderIconOption("lunchDining",     "Lunch",      Icons.Filled.LunchDining),
        ReminderIconOption("localDrink",      "Drink",      Icons.Filled.LocalDrink),
        ReminderIconOption("bakeryDining",    "Bakery",     Icons.Filled.BakeryDining),
        ReminderIconOption("fastfood",        "Fastfood",   Icons.Filled.Fastfood),
        ReminderIconOption("icecream",        "Ice Cream",  Icons.Filled.Icecream),
        ReminderIconOption("localPizza",      "Pizza",      Icons.Filled.LocalPizza),
        ReminderIconOption("ramenDining",     "Ramen",      Icons.Filled.RamenDining),
        ReminderIconOption("wineBar",         "Wine",       Icons.Filled.WineBar),
        ReminderIconOption("dinnerDining",    "Dinner",     Icons.Filled.DinnerDining),
        ReminderIconOption("breakfastDining", "Breakfast",  Icons.Filled.BreakfastDining),
    )),

    IconCategory("Home", Icons.Filled.Home, listOf(
        ReminderIconOption("home",             "Home",      Icons.Filled.Home),
        ReminderIconOption("weekend",          "Relax",     Icons.Filled.Weekend),
        ReminderIconOption("cleaningServices", "Clean",     Icons.Filled.CleaningServices),
        ReminderIconOption("kingBed",          "Sleep",     Icons.Filled.KingBed),
        ReminderIconOption("bathtub",          "Bath",      Icons.Filled.Bathtub),
        ReminderIconOption("garage",           "Garage",    Icons.Filled.Garage),
        ReminderIconOption("kitchen",          "Kitchen",   Icons.Filled.Kitchen),
        ReminderIconOption("cottage",          "Cottage",   Icons.Filled.Cottage),
        ReminderIconOption("fireplace",        "Fireplace", Icons.Filled.Fireplace),
        ReminderIconOption("deck",             "Deck",      Icons.Filled.Deck),
        ReminderIconOption("singleBed",        "Bed",       Icons.Filled.SingleBed),
        ReminderIconOption("roofing",          "Roof",      Icons.Filled.Roofing),
        ReminderIconOption("house",            "House",     Icons.Filled.House),
        ReminderIconOption("balcony",          "Balcony",   Icons.Filled.Balcony),
    )),

    IconCategory("Work", Icons.Filled.Work, listOf(
        ReminderIconOption("work",           "Work",      Icons.Filled.Work),
        ReminderIconOption("school",         "School",    Icons.Filled.School),
        ReminderIconOption("computer",       "Computer",  Icons.Filled.Computer),
        ReminderIconOption("assignment",     "Task",      Icons.Filled.Assignment),
        ReminderIconOption("businessCenter", "Business",  Icons.Filled.BusinessCenter),
        ReminderIconOption("calculate",      "Calculate", Icons.Filled.Calculate),
        ReminderIconOption("edit",           "Edit",      Icons.Filled.Edit),
        ReminderIconOption("gavel",          "Law",       Icons.Filled.Gavel),
        ReminderIconOption("laptop",         "Laptop",    Icons.Filled.Laptop),
        ReminderIconOption("libraryBooks",   "Library",   Icons.Filled.LibraryBooks),
        ReminderIconOption("science",        "Science",   Icons.Filled.Science),
        ReminderIconOption("code",           "Code",      Icons.Filled.Code),
        ReminderIconOption("campaign",       "Campaign",  Icons.Filled.Campaign),
        ReminderIconOption("menuBook",       "Book",      Icons.Filled.MenuBook),
    )),

    IconCategory("Sport", Icons.Filled.DirectionsRun, listOf(
        ReminderIconOption("directionsRun",    "Run",        Icons.Filled.DirectionsRun),
        ReminderIconOption("sportsBasketball", "Basketball", Icons.Filled.SportsBasketball),
        ReminderIconOption("sportsSoccer",     "Soccer",     Icons.Filled.SportsSoccer),
        ReminderIconOption("pool",             "Swim",       Icons.Filled.Pool),
        ReminderIconOption("hiking",           "Hike",       Icons.Filled.Hiking),
        ReminderIconOption("directionsBike",   "Bike",       Icons.Filled.DirectionsBike),
        ReminderIconOption("sportsTennis",     "Tennis",     Icons.Filled.SportsTennis),
        ReminderIconOption("sailing",          "Sail",       Icons.Filled.Sailing),
        ReminderIconOption("snowboarding",     "Snowboard",  Icons.Filled.Snowboarding),
        ReminderIconOption("skateboarding",    "Skate",      Icons.Filled.Skateboarding),
        ReminderIconOption("directionsWalk",   "Walk",       Icons.Filled.DirectionsWalk),
        ReminderIconOption("nordicWalking",    "Nordic",     Icons.Filled.NordicWalking),
        ReminderIconOption("kayaking",         "Kayak",      Icons.Filled.Kayaking),
        ReminderIconOption("iceSkating",       "Skate",      Icons.Filled.IceSkating),
    )),
)

const val DEFAULT_ICON_KEY = "notifications"

fun iconFromKey(key: String?): ReminderIconOption =
    ReminderIconCategories.flatMap { it.icons }.find { it.key == key }
        ?: ReminderIconCategories.first().icons.first()
