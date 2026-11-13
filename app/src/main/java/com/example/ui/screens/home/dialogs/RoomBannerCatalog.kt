package com.example.ui.screens.home.dialogs

data class BannerOption(val name: String, val url: String)

val languageBanners = mapOf(
    "Japanese" to listOf(
        BannerOption("Sakura Street", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
        BannerOption("Mount Fuji", "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=800"),
        BannerOption("Tokyo Night", "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800"),
        BannerOption("Kyoto Temple", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
        BannerOption("Anime Style", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800")
    ),
    "English" to listOf(
        BannerOption("London Big Ben", "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800"),
        BannerOption("New York Skyline", "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800"),
        BannerOption("Stonehenge Sunset", "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800"),
        BannerOption("Hollywood Streets", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800")
    ),
    "Korean" to listOf(
        BannerOption("Seoul Sunset", "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800"),
        BannerOption("Jeonju Hanok", "https://images.unsplash.com/photo-1538669715515-5c3756c07bae?w=800"),
        BannerOption("Jeju Island Beach", "https://images.unsplash.com/photo-1504609773096-104ff2c73ba4?w=800"),
        BannerOption("Gyeongju Palace", "https://images.unsplash.com/photo-1525373612132-b3e820780006?w=800")
    ),
    "Spanish" to listOf(
        BannerOption("Barcelona Cathedral", "https://images.unsplash.com/photo-1509840144506-2c990f23a7f7?w=800"),
        BannerOption("Madrid Royal", "https://images.unsplash.com/photo-1539650116574-8efeb43e2750?w=800"),
        BannerOption("Spanish Archways", "https://images.unsplash.com/photo-1485081669829-bacb8c7bb1f3?w=800")
    ),
    "Chinese" to listOf(
        BannerOption("Shanghai Bund Sunset", "https://images.unsplash.com/photo-1474181487882-5abf3f016c2d?w=800"),
        BannerOption("The Great Wall", "https://images.unsplash.com/photo-1508504509543-191d5ac64987?w=800"),
        BannerOption("Beijing Forbidden City", "https://images.unsplash.com/photo-1490730141103-6cac27aaab94?w=800")
    )
)

val defaultBannersList = listOf(
    BannerOption("Scenic Mountains", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800"),
    BannerOption("Golden Shoreline", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"),
    BannerOption("Cyberpunk Highway", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800"),
    BannerOption("Historic Library", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800")
)

object BannerPack {
    val worldwideScenics = listOf(
        BannerOption("Tokyo Akihabara Night", "https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=800"),
        BannerOption("Kyoto Red Toriis", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
        BannerOption("Mount Fuji Sakura", "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=800"),
        BannerOption("Eiffel Tower Paris", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800"),
        BannerOption("London Thames Big Ben", "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800"),
        BannerOption("New York Central Park", "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800"),
        BannerOption("Taj Mahal Sunrise", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=800"),
        BannerOption("Seoul Han River Bridge", "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800"),
        BannerOption("Sydney Opera House", "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=800"),
        BannerOption("Rome Colosseum Dusk", "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800"),
        BannerOption("Santorini Sunset Cliffs", "https://images.unsplash.com/photo-1533105079780-92b9be482077?w=800"),
        BannerOption("Barcelona Park Guell", "https://images.unsplash.com/photo-1523531294919-4bcd7c65e216?w=800"),
        BannerOption("Rio de Janeiro Beach", "https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=800"),
        BannerOption("Giza Great Pyramids", "https://images.unsplash.com/photo-1503177119275-0aa32b31d468?w=800"),
        BannerOption("Berlin Brandenburg Gate", "https://images.unsplash.com/photo-1599946347371-68eb71b16afc?w=800"),
        BannerOption("Machu Picchu Incan Citadel", "https://images.unsplash.com/photo-1587590227264-0ac64ce63ce8?w=800"),
        BannerOption("Canadian Alpine Lake", "https://images.unsplash.com/photo-1500043357865-c6b8827df7f1?w=800"),
        BannerOption("Grand Canyon Red Rocks", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800"),
        BannerOption("Great Wall Wilderness", "https://images.unsplash.com/photo-1508504509543-191d5ac64987?w=800"),
        BannerOption("Amalfi Coast Pastel Towns", "https://images.unsplash.com/photo-1486016006115-74a41448aea2?w=800"),
        BannerOption("Singapore Marina Sands", "https://images.unsplash.com/photo-1525596667581-2b083dc3954f?w=800"),
        BannerOption("Indian Amber Palace", "https://images.unsplash.com/photo-1473163928189-364b2c4e1135?w=800"),
        BannerOption("Venice Scenic Canal", "https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=800"),
        BannerOption("Iceland Northern Lights", "https://images.unsplash.com/photo-1483168527879-c66136b56105?w=800")
    )

    val cozyVibes = listOf(
        BannerOption("Aesthetic Rainy Window", "https://images.unsplash.com/photo-1428908728789-d2de25dbd4e2?w=800"),
        BannerOption("Cozy Espresso Table", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800"),
        BannerOption("Warm Fireplace Lounge", "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800"),
        BannerOption("Classic Library Shelves", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800"),
        BannerOption("Cathedral Glass Study", "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=800"),
        BannerOption("Sunlit Greenhouse Plants", "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=800"),
        BannerOption("Minimalist Oak Desk", "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=800"),
        BannerOption("Vintage Vinyl Record", "https://images.unsplash.com/photo-1539628399213-d6482e76f184?w=800"),
        BannerOption("Acoustic Parlor Guitar", "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=800"),
        BannerOption("Steaming Herbal Tea", "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=800"),
        BannerOption("Bonsai Tree Studio", "https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=800"),
        BannerOption("Lo-fi Coffee Shop View", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800"),
        BannerOption("Candlelit Wooden Rest", "https://images.unsplash.com/photo-1517705008128-361805f42e8a?w=800"),
        BannerOption("Cozy Wool Knit Couch", "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?w=800"),
        BannerOption("Study Corner Lo-fi", "https://images.unsplash.com/photo-1544640808-32ca72ac7f37?w=800")
    )

    val cyberpunkAndArt = listOf(
        BannerOption("Akira Cyberpunk Sunset", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800"),
        BannerOption("Cyber Highway Grid", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800"),
        BannerOption("Vaporwave Starry Sky", "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?w=800"),
        BannerOption("Neo Tokyo Alley", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800"),
        BannerOption("Retro Outrun Sunset", "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=800"),
        BannerOption("Pastel Paint Splash", "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800"),
        BannerOption("Memphis Geometric Art", "https://images.unsplash.com/photo-1557672172-298e090bd0f1?w=800"),
        BannerOption("Cloud Horizons", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"),
        BannerOption("Tokyo Train Crossing", "https://images.unsplash.com/photo-1528164344705-47542687000d?w=800"),
        BannerOption("Neon Synthwave Grid", "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800"),
        BannerOption("Hologram Terminal", "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800"),
        BannerOption("Abstract Ink Drops", "https://images.unsplash.com/photo-1563089145-599997674d42?w=800"),
        BannerOption("Cosmic Space Nebulae", "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=800"),
        BannerOption("Fantasy Forest Meadow", "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800")
    )

    val animeAndAnimated = listOf(
        BannerOption("Animated Cyberpunk", "https://i.giphy.com/media/xT9IgzoKnwFNmISR8I/giphy.gif"),
        BannerOption("Animated Sakura", "https://i.giphy.com/media/3o7TKSjRrfIPjeiVyM/giphy.gif"),
        BannerOption("Animated Lofi Study", "https://i.giphy.com/media/Lmq2eMv782q88/giphy.gif"),
        BannerOption("Animated Neon Sunset", "https://i.giphy.com/media/26tn33aiTi1jkl6H6/giphy.gif"),
        BannerOption("Animated Sunset Train", "https://i.giphy.com/media/3oKIPnAiaMCws8nOsE/giphy.gif"),
        BannerOption("Animated Kyoto Torii", "https://i.giphy.com/media/l41K3o5fA88P3A156/giphy.gif"),
        BannerOption("Animated Rainy Tokyo", "https://i.giphy.com/media/xUPGcJ9uOAL2h28vMG/giphy.gif"),
        BannerOption("Animated Starry Galaxy", "https://i.giphy.com/media/3oKIPmuu8WUKRM77vG/giphy.gif"),
        BannerOption("Cyber Anime Skyline", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800"),
        BannerOption("Cherry Blossom Sky", "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=800")
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
