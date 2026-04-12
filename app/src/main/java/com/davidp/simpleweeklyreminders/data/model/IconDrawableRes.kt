package com.davidp.simpleweeklyreminders.data.model

import com.davidp.simpleweeklyreminders.R

fun iconDrawableRes(key: String): Int? = when (key) {
    // General
    "notifications" -> R.drawable.ic_reminder_notifications
    "star" -> R.drawable.ic_reminder_star
    "alarm" -> R.drawable.ic_reminder_alarm
    "event" -> R.drawable.ic_reminder_event
    "warning" -> R.drawable.ic_reminder_warning
    "people" -> R.drawable.ic_reminder_people
    "celebration" -> R.drawable.ic_reminder_celebration
    "shoppingCart" -> R.drawable.ic_reminder_shopping_cart
    "flight" -> R.drawable.ic_reminder_flight
    "directionsCar" -> R.drawable.ic_reminder_directions_car
    "musicNote" -> R.drawable.ic_reminder_music_note
    "movie" -> R.drawable.ic_reminder_movie
    "childCare" -> R.drawable.ic_reminder_child_care
    "photoCamera" -> R.drawable.ic_reminder_photo_camera
    "pets" -> R.drawable.ic_reminder_pets
    "familyRestroom" -> R.drawable.ic_reminder_family_restroom
    // Health
    "medicalServices" -> R.drawable.ic_reminder_medical_services
    "favorite" -> R.drawable.ic_reminder_favorite
    "spa" -> R.drawable.ic_reminder_spa
    "psychology" -> R.drawable.ic_reminder_psychology
    "monitorHeart" -> R.drawable.ic_reminder_monitor_heart
    "vaccines" -> R.drawable.ic_reminder_vaccines
    "localPharmacy" -> R.drawable.ic_reminder_local_pharmacy
    "healing" -> R.drawable.ic_reminder_healing
    "healthAndSafety" -> R.drawable.ic_reminder_health_and_safety
    "medication" -> R.drawable.ic_reminder_medication
    "accessibilityNew" -> R.drawable.ic_reminder_accessibility_new
    "mood" -> R.drawable.ic_reminder_mood
    "selfImprovement" -> R.drawable.ic_reminder_self_improvement
    "waterDrop" -> R.drawable.ic_reminder_water_drop
    // Nature
    "eco" -> R.drawable.ic_reminder_eco
    "park" -> R.drawable.ic_reminder_park
    "localFlorist" -> R.drawable.ic_reminder_local_florist
    "grass" -> R.drawable.ic_reminder_grass
    "forest" -> R.drawable.ic_reminder_forest
    "energySavingsLeaf" -> R.drawable.ic_reminder_energy_savings_leaf
    "terrain" -> R.drawable.ic_reminder_terrain
    "beachAccess" -> R.drawable.ic_reminder_beach_access
    "wbSunny" -> R.drawable.ic_reminder_wb_sunny
    "cloud" -> R.drawable.ic_reminder_cloud
    "yard" -> R.drawable.ic_reminder_yard
    "filterVintage" -> R.drawable.ic_reminder_filter_vintage
    "acUnit" -> R.drawable.ic_reminder_ac_unit
    "thunderstorm" -> R.drawable.ic_reminder_thunderstorm
    // Food
    "restaurant" -> R.drawable.ic_reminder_restaurant
    "localCafe" -> R.drawable.ic_reminder_local_cafe
    "localBar" -> R.drawable.ic_reminder_local_bar
    "emojiFoodBev" -> R.drawable.ic_reminder_emoji_food_bev
    "lunchDining" -> R.drawable.ic_reminder_lunch_dining
    "localDrink" -> R.drawable.ic_reminder_local_drink
    "bakeryDining" -> R.drawable.ic_reminder_bakery_dining
    "fastfood" -> R.drawable.ic_reminder_fastfood
    "icecream" -> R.drawable.ic_reminder_icecream
    "localPizza" -> R.drawable.ic_reminder_local_pizza
    "ramenDining" -> R.drawable.ic_reminder_ramen_dining
    "wineBar" -> R.drawable.ic_reminder_wine_bar
    "dinnerDining" -> R.drawable.ic_reminder_dinner_dining
    "breakfastDining" -> R.drawable.ic_reminder_breakfast_dining
    // Home
    "home" -> R.drawable.ic_reminder_home
    "weekend" -> R.drawable.ic_reminder_weekend
    "cleaningServices" -> R.drawable.ic_reminder_cleaning_services
    "kingBed" -> R.drawable.ic_reminder_king_bed
    "bathtub" -> R.drawable.ic_reminder_bathtub
    "garage" -> R.drawable.ic_reminder_garage
    "kitchen" -> R.drawable.ic_reminder_kitchen
    "cottage" -> R.drawable.ic_reminder_cottage
    "fireplace" -> R.drawable.ic_reminder_fireplace
    "deck" -> R.drawable.ic_reminder_deck
    "singleBed" -> R.drawable.ic_reminder_single_bed
    "roofing" -> R.drawable.ic_reminder_roofing
    "house" -> R.drawable.ic_reminder_house
    "balcony" -> R.drawable.ic_reminder_balcony
    // Work
    "work" -> R.drawable.ic_reminder_work
    "school" -> R.drawable.ic_reminder_school
    "computer" -> R.drawable.ic_reminder_computer
    "assignment" -> R.drawable.ic_reminder_assignment
    "businessCenter" -> R.drawable.ic_reminder_business_center
    "calculate" -> R.drawable.ic_reminder_calculate
    "edit" -> R.drawable.ic_reminder_edit
    "gavel" -> R.drawable.ic_reminder_gavel
    "laptop" -> R.drawable.ic_reminder_laptop
    "libraryBooks" -> R.drawable.ic_reminder_library_books
    "science" -> R.drawable.ic_reminder_science
    "code" -> R.drawable.ic_reminder_code
    "campaign" -> R.drawable.ic_reminder_campaign
    "menuBook" -> R.drawable.ic_reminder_menu_book
    // Sport
    "directionsRun" -> R.drawable.ic_reminder_directions_run
    "sportsBasketball" -> R.drawable.ic_reminder_sports_basketball
    "sportsSoccer" -> R.drawable.ic_reminder_sports_soccer
    "pool" -> R.drawable.ic_reminder_pool
    "hiking" -> R.drawable.ic_reminder_hiking
    "directionsBike" -> R.drawable.ic_reminder_directions_bike
    "sportsTennis" -> R.drawable.ic_reminder_sports_tennis
    "sailing" -> R.drawable.ic_reminder_sailing
    "snowboarding" -> R.drawable.ic_reminder_snowboarding
    "skateboarding" -> R.drawable.ic_reminder_skateboarding
    "directionsWalk" -> R.drawable.ic_reminder_directions_walk
    "nordicWalking" -> R.drawable.ic_reminder_nordic_walking
    "kayaking" -> R.drawable.ic_reminder_kayaking
    "iceSkating" -> R.drawable.ic_reminder_ice_skating
    else -> null
}
