package com.example.ui.screens.home.dialogs

data class BannerOption(val name: String, val url: String)

val languageBanners = mapOf(
    "Hindi" to listOf(
        BannerOption("Taj Mahal Sunrise", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=800"),
        BannerOption("Jaipur Pink City", "https://images.unsplash.com/photo-1477587458883-47145ed94245?w=800"),
        BannerOption("Kerala Backwaters", "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=800"),
        BannerOption("Varanasi Evening Aarti", "https://images.unsplash.com/photo-1561361513-2d000a50f0dc?w=800"),
        BannerOption("Festive Lights GIF", "https://i.giphy.com/media/26tOZ42Mg6pbTUPHW/giphy.gif")
    ),
    "Japanese" to listOf(
        BannerOption("Sakura Street", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
        BannerOption("Mount Fuji", "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=800"),
        BannerOption("Tokyo Night", "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800"),
        BannerOption("Kyoto Fushimi Inari", "https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=800"),
        BannerOption("Sakura Petals GIF", "https://i.giphy.com/media/3o7TKSjRrfIPjeiVyM/giphy.gif")
    ),
    "English" to listOf(
        BannerOption("London Big Ben", "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800"),
        BannerOption("New York Skyline", "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800"),
        BannerOption("Misty Woodland", "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800"),
        BannerOption("Cozy Coffee Lounge", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800"),
        BannerOption("Rainy Window GIF", "https://i.giphy.com/media/Lmq2eMv782q88/giphy.gif")
    ),
    "Korean" to listOf(
        BannerOption("Seoul Sunset", "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800"),
        BannerOption("Jeonju Hanok", "https://images.unsplash.com/photo-1538669715515-5c3756c07bae?w=800"),
        BannerOption("Jeju Island Beach", "https://images.unsplash.com/photo-1504609773096-104ff2c73ba4?w=800"),
        BannerOption("Gyeongju Palace", "https://images.unsplash.com/photo-1525373612132-b3e820780006?w=800"),
        BannerOption("Neon Seoul GIF", "https://i.giphy.com/media/26tn33aiTi1jkl6H6/giphy.gif")
    ),
    "Spanish" to listOf(
        BannerOption("Barcelona Cathedral", "https://images.unsplash.com/photo-1509840144506-2c990f23a7f7?w=800"),
        BannerOption("Madrid Royal", "https://images.unsplash.com/photo-1539650116574-8efeb43e2750?w=800"),
        BannerOption("Spanish Archways", "https://images.unsplash.com/photo-1485081669829-bacb8c7bb1f3?w=800")
    ),
    "Chinese" to listOf(
        BannerOption("Shanghai Bund Sunset", "https://images.unsplash.com/photo-1474181487882-5abf3f016c2d?w=800"),
        BannerOption("The Great Wall", "https://images.unsplash.com/photo-1508504509543-191d5ac64987?w=800"),
        BannerOption("Beijing Dusk", "https://images.unsplash.com/photo-1490730141103-6cac27aaab94?w=800")
    ),
    "French" to listOf(
        BannerOption("Eiffel Tower Dusk", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800"),
        BannerOption("Parisian Cafe", "https://images.unsplash.com/photo-1509439581779-6298f75bf6e5?w=800"),
        BannerOption("Louvre Courtyard", "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800")
    ),
    "Arabic" to listOf(
        BannerOption("Desert Dunes Sunset", "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=800"),
        BannerOption("Dubai Marina Night", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800"),
        BannerOption("Historic Souk Lanterns", "https://images.unsplash.com/photo-1548013146-72479768bada?w=800")
    ),
    "German" to listOf(
        BannerOption("Berlin Skyline", "https://images.unsplash.com/photo-1599946347371-68eb71b16afc?w=800"),
        BannerOption("Bavarian Alps", "https://images.unsplash.com/photo-1467269204594-9661b134dd2b?w=800"),
        BannerOption("Historic Castle", "https://images.unsplash.com/photo-1534313314376-a72289b6181e?w=800")
    )
)

val defaultBannersList = listOf(
    BannerOption("Taj Mahal Agra", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=800"),
    BannerOption("Scenic Mountains", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800"),
    BannerOption("Golden Shoreline", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"),
    BannerOption("Lo-fi Study GIF", "https://i.giphy.com/media/Lmq2eMv782q88/giphy.gif"),
    BannerOption("Cyberpunk Highway", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800"),
    BannerOption("Historic Library", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800")
)

object BannerPack {
    val indianScenics = listOf(
        BannerOption("Taj Mahal Sunrise", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=800"),
        BannerOption("Hawa Mahal Jaipur", "https://images.unsplash.com/photo-1477587458883-47145ed94245?w=800"),
        BannerOption("Kerala Backwaters", "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?w=800"),
        BannerOption("Mumbai Marine Drive", "https://images.unsplash.com/photo-1529253355930-ddbe423a2ac7?w=800"),
        BannerOption("Varanasi River Ghats", "https://images.unsplash.com/photo-1561361513-2d000a50f0dc?w=800"),
        BannerOption("Goa Sunset Palms", "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=800"),
        BannerOption("Pangong Lake Ladakh", "https://images.unsplash.com/photo-1589793907316-f94025b46850?w=800"),
        BannerOption("Udaipur Lake Palace", "https://images.unsplash.com/photo-1599661046289-e31897846e41?w=800"),
        BannerOption("Amber Fort Rajasthan", "https://images.unsplash.com/photo-1473163928189-364b2c4e1135?w=800"),
        BannerOption("Himalayan Morning Peaks", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800")
    )

    val animatedGifs = listOf(
        BannerOption("Lo-fi Study Rain", "https://i.giphy.com/media/Lmq2eMv782q88/giphy.gif"),
        BannerOption("Sakura Breeze", "https://i.giphy.com/media/3o7TKSjRrfIPjeiVyM/giphy.gif"),
        BannerOption("Neon Cyberpunk City", "https://i.giphy.com/media/xT9IgzoKnwFNmISR8I/giphy.gif"),
        BannerOption("Sunset Express Train", "https://i.giphy.com/media/3oKIPnAiaMCws8nOsE/giphy.gif"),
        BannerOption("Festive Fireworks", "https://i.giphy.com/media/26tOZ42Mg6pbTUPHW/giphy.gif"),
        BannerOption("Synthwave Horizon", "https://i.giphy.com/media/26tn33aiTi1jkl6H6/giphy.gif"),
        BannerOption("Ocean Twilight Waves", "https://i.giphy.com/media/l0ExdMHUDKteztyfe/giphy.gif"),
        BannerOption("Starry Galaxy Glow", "https://i.giphy.com/media/3og0IFrHkIglEOg8Ba/giphy.gif"),
        BannerOption("Twilight Rain", "https://i.giphy.com/media/l0HlRnAWXxn0MhKLK/giphy.gif"),
        BannerOption("Neon Lights Loop", "https://i.giphy.com/media/3o7abB06u9bNzA8lu8/giphy.gif"),
        BannerOption("Anime Clouds Drift", "https://i.giphy.com/media/26ufdipQqU2lhNA4g/giphy.gif"),
        BannerOption("Coffee Steam Loop", "https://i.giphy.com/media/l3vR85PnGsBwu1PFK/giphy.gif")
    )

    val worldwideScenics = listOf(
        BannerOption("Tokyo Akihabara Night", "https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=800"),
        BannerOption("Kyoto Red Toriis", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
        BannerOption("Mount Fuji Sakura", "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=800"),
        BannerOption("Eiffel Tower Paris", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800"),
        BannerOption("London Thames Big Ben", "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800"),
        BannerOption("New York Central Park", "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800"),
        BannerOption("Seoul Han River Bridge", "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800"),
        BannerOption("Sydney Opera House", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800"),
        BannerOption("Rome Colosseum Dusk", "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800"),
        BannerOption("Santorini Sunset Cliffs", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800"),
        BannerOption("Barcelona Park Guell", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800"),
        BannerOption("Rio de Janeiro Beach", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800")
    )

    val cozyVibes = listOf(
        BannerOption("Aesthetic Rainy Window", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800"),
        BannerOption("Cozy Espresso Table", "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=800"),
        BannerOption("Warm Fireplace Lounge", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800"),
        BannerOption("Classic Library Shelves", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800"),
        BannerOption("Cathedral Glass Study", "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=800"),
        BannerOption("Sunlit Greenhouse Plants", "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=800"),
        BannerOption("Minimalist Oak Desk", "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=800")
    )

    val cyberpunkAndArt = listOf(
        BannerOption("Akira Cyberpunk Sunset", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800"),
        BannerOption("Cyber Highway Grid", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800"),
        BannerOption("Vaporwave Starry Sky", "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?w=800"),
        BannerOption("Neo Tokyo Alley", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800"),
        BannerOption("Retro Outrun Sunset", "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=800"),
        BannerOption("Pastel Paint Splash", "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800")
    )

    val animeAndAnimated = listOf(
        BannerOption("Sakura Station Sunset", "drawable://img_anime_banner_sakura_station_1794610601752"),
        BannerOption("Cyber Tokyo Rooftop", "drawable://img_anime_banner_cyber_rooftop_1794610601065"),
        BannerOption("Cozy Study Library", "drawable://img_anime_banner_cozy_library_1794610601091"),
        BannerOption("Starry Mountain Shrine", "drawable://img_anime_banner_starry_shrine_1794610601727"),
        BannerOption("Ocean Seaside Train", "drawable://img_anime_banner_ocean_train_1794610602249"),
        BannerOption("Cherry Blossom Path", "drawable://img_wallpaper_cherry_blossom_1781625392725"),
        BannerOption("Balcony Night View", "drawable://img_wallpaper_balcony_night_1781625409367"),
        BannerOption("Moonlight Valley", "drawable://img_wallpaper_moonlight_valley_1781625422948"),
        BannerOption("Daytime City Street", "drawable://img_wallpaper_day_street_1781625437347"),
        BannerOption("Lighthouse Sunset", "drawable://img_wallpaper_lighthouse_sunset_1781625450547")
    )


    val gradients: List<BannerOption> = listOf(
        BannerOption("Sunset Bliss (#1)", "gradient:#FF4500,#FF8C00,#FFD700"),
        BannerOption("Neon Cyberpunk (#2)", "gradient:#8A2BE2,#FF007F,#00FFFF"),
        BannerOption("Aurora Dream (#3)", "gradient:#00FF87,#60EFFF,#21007F"),
        BannerOption("Lofi Lavender (#4)", "gradient:#E0B0FF,#DDA0DD,#4B0082"),
        BannerOption("Royal Velvet (#5)", "gradient:#800080,#9932CC,#1F005C"),
        BannerOption("Ocean Breeze (#6)", "gradient:#00FFFF,#0080FF,#000080"),
        BannerOption("Emerald Forest (#7)", "gradient:#32CD32,#00FF7F,#006400"),
        BannerOption("Matcha Milkshake (#8)", "gradient:#C1E1C1,#779ECB,#033E3E"),
        BannerOption("Electric Orange (#9)", "gradient:#FFA500,#FF4500,#E60000"),
        BannerOption("Sweet Peach (#10)", "gradient:#FFDAB9,#FFB6C1,#FFF0F5"),
        BannerOption("Midnight Purple (#11)", "gradient:#000000,#4B0082,#8A2BE2"),
        BannerOption("Desert Mirage (#12)", "gradient:#F4A460,#E9967A,#8B4513"),
        BannerOption("Tropical Lime (#13)", "gradient:#DFFF00,#32CD32,#008000"),
        BannerOption("Bubblegum Horizon (#14)", "gradient:#FFC0CB,#FF69B4,#8A2BE2"),
        BannerOption("Glacier Ice (#15)", "gradient:#F0F8FF,#E0FFFF,#00BFFF"),
        BannerOption("Volcano Ash (#16)", "gradient:#1C1C1C,#3A3A3A,#8B0000"),
        BannerOption("Autumn Rust (#17)", "gradient:#8B4513,#A0522D,#CD853F"),
        BannerOption("Cotton Candy (#18)", "gradient:#FFB6C1,#87CEFA,#E6E6FA"),
        BannerOption("Mystic Turquoise (#19)", "gradient:#40E0D0,#48D1CC,#008080"),
        BannerOption("Warm Sandalwood (#20)", "gradient:#FFE4C4,#BC8F8F,#8B4513")
    ) + List(30) { index ->
        val palettes = listOf(
            listOf("#7F00FF", "#E100FF"),
            listOf("#FF416C", "#FF4B2B"),
            listOf("#00B4DB", "#0083B0"),
            listOf("#A8FF78", "#78FFD6"),
            listOf("#F27121", "#E94057", "#8A2387"),
            listOf("#00C6FF", "#0072FF"),
            listOf("#FFE259", "#FFA751"),
            listOf("#F857A6", "#FF5858"),
            listOf("#11998E", "#38EF7D"),
            listOf("#101820", "#F2AA4C")
        )
        val adjectives = listOf(
            "Retro", "Cosmic", "Neon", "Cyber", "Deep", "Shining", "Velvet", "Plum", "Aura", "Golden"
        )
        val nouns = listOf(
            "Cascade", "Splendor", "Elysium", "Mist", "Vortex", "Breeze", "Wave", "Glow", "Bliss", "Mirage"
        )
        val palette = palettes[index % palettes.size]
        val adj = adjectives[(index * 2) % adjectives.size]
        val noun = nouns[(index * 3) % nouns.size]
        BannerOption(
            name = "$adj $noun (#${index + 21})",
            url = "gradient:" + palette.joinToString(",")
        )
    }
}
