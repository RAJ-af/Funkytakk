package com.example.ui.screens

data class Country(
    val name: String,
    val flagCode: String
)

object CountryData {
    val popularCountries = listOf(
        Country("United States", "us"),
        Country("United Kingdom", "gb"),
        Country("Canada", "ca"),
        Country("Australia", "au"),
        Country("Germany", "de"),
        Country("United Arab Emirates", "ae")
    )

    val allCountries = listOf(
        Country("Afghanistan", "af"),
        Country("Albania", "al"),
        Country("Algeria", "dz"),
        Country("Andorra", "ad"),
        Country("Angola", "ao"),
        Country("Antigua and Barbuda", "ag"),
        Country("Argentina", "ar"),
        Country("Armenia", "am"),
        Country("Australia", "au"),
        Country("Austria", "at"),
        Country("Azerbaijan", "az"),
        Country("Bahamas", "bs"),
        Country("Bahrain", "bh"),
        Country("Bangladesh", "bd"),
        Country("Barbados", "bb"),
        Country("Belarus", "by"),
        Country("Belgium", "be"),
        Country("Belize", "bz"),
        Country("Benin", "bj"),
        Country("Bhutan", "bt"),
        Country("Bolivia", "bo"),
        Country("Bosnia and Herzegovina", "ba"),
        Country("Botswana", "bw"),
        Country("Brazil", "br"),
        Country("Brunei", "bn"),
        Country("Bulgaria", "bg"),
        Country("Burkina Faso", "bf"),
        Country("Burundi", "bi"),
        Country("Cabo Verde", "cv"),
        Country("Cambodia", "kh"),
        Country("Cameroon", "cm"),
        Country("Canada", "ca"),
        Country("Central African Republic", "cf"),
        Country("Chad", "td"),
        Country("Chile", "cl"),
        Country("China", "cn"),
        Country("Colombia", "co"),
        Country("Comoros", "km"),
        Country("Congo", "cg"),
        Country("Costa Rica", "cr"),
        Country("Croatia", "hr"),
        Country("Cuba", "cu"),
        Country("Cyprus", "cy"),
        Country("Czech Republic", "cz"),
        Country("Democratic Republic of the Congo", "cd"),
        Country("Denmark", "dk"),
        Country("Djibouti", "dj"),
        Country("Dominica", "dm"),
        Country("Dominican Republic", "do"),
        Country("Ecuador", "ec"),
        Country("Egypt", "eg"),
        Country("El Salvador", "sv"),
        Country("Equatorial Guinea", "gq"),
        Country("Eritrea", "er"),
        Country("Estonia", "ee"),
        Country("Eswatini", "sz"),
        Country("Ethiopia", "et"),
        Country("Fiji", "fj"),
        Country("Finland", "fi"),
        Country("France", "fr"),
        Country("Gabon", "ga"),
        Country("Gambia", "gm"),
        Country("Georgia", "ge"),
        Country("Germany", "de"),
        Country("Ghana", "gh"),
        Country("Greece", "gr"),
        Country("Grenada", "gd"),
        Country("Guatemala", "gt"),
        Country("Guinea", "gn"),
        Country("Guinea-Bissau", "gw"),
        Country("Guyana", "gy"),
        Country("Haiti", "ht"),
        Country("Honduras", "hn"),
        Country("Hong Kong", "hk"),
        Country("Hungary", "hu"),
        Country("Iceland", "is"),
        Country("India", "in"),
        Country("Indonesia", "id"),
        Country("Iran", "ir"),
        Country("Iraq", "iq"),
        Country("Ireland", "ie"),
        Country("Israel", "il"),
        Country("Italy", "it"),
        Country("Ivory Coast", "ci"),
        Country("Jamaica", "jm"),
        Country("Japan", "jp"),
        Country("Jordan", "jo"),
        Country("Kazakhstan", "kz"),
        Country("Kenya", "ke"),
        Country("Kiribati", "ki"),
        Country("Kosovo", "xk"),
        Country("Kuwait", "kw"),
        Country("Kyrgyzstan", "kg"),
        Country("Laos", "la"),
        Country("Latvia", "lv"),
        Country("Lebanon", "lb"),
        Country("Lesotho", "ls"),
        Country("Liberia", "lr"),
        Country("Libya", "ly"),
        Country("Liechtenstein", "li"),
        Country("Lithuania", "lt"),
        Country("Luxembourg", "lu"),
        Country("Macau", "mo"),
        Country("Madagascar", "mg"),
        Country("Malawi", "mw"),
        Country("Malaysia", "my"),
        Country("Maldives", "mv"),
        Country("Mali", "ml"),
        Country("Malta", "mt"),
        Country("Marshall Islands", "mh"),
        Country("Mauritania", "mr"),
        Country("Mauritius", "mu"),
        Country("Mexico", "mx"),
        Country("Micronesia", "fm"),
        Country("Moldova", "md"),
        Country("Monaco", "mc"),
        Country("Mongolia", "mn"),
        Country("Montenegro", "me"),
        Country("Morocco", "ma"),
        Country("Mozambique", "mz"),
        Country("Myanmar", "mm"),
        Country("Namibia", "na"),
        Country("Nauru", "nr"),
        Country("Nepal", "np"),
        Country("Netherlands", "nl"),
        Country("New Zealand", "nz"),
        Country("Nicaragua", "ni"),
        Country("Niger", "ne"),
        Country("Nigeria", "ng"),
        Country("North Korea", "kp"),
        Country("North Macedonia", "mk"),
        Country("Norway", "no"),
        Country("Oman", "om"),
        Country("Pakistan", "pk"),
        Country("Palau", "pw"),
        Country("Palestine", "ps"),
        Country("Panama", "pa"),
        Country("Papua New Guinea", "pg"),
        Country("Paraguay", "py"),
        Country("Peru", "pe"),
        Country("Philippines", "ph"),
        Country("Poland", "pl"),
        Country("Portugal", "pt"),
        Country("Qatar", "qa"),
        Country("Romania", "ro"),
        Country("Russia", "ru"),
        Country("Rwanda", "rw"),
        Country("Saint Kitts and Nevis", "kn"),
        Country("Saint Lucia", "lc"),
        Country("Saint Vincent and the Grenadines", "vc"),
        Country("Samoa", "ws"),
        Country("San Marino", "sm"),
        Country("Sao Tome and Principe", "st"),
        Country("Saudi Arabia", "sa"),
        Country("Senegal", "sn"),
        Country("Serbia", "rs"),
        Country("Seychelles", "sc"),
        Country("Sierra Leone", "sl"),
        Country("Singapore", "sg"),
        Country("Slovakia", "sk"),
        Country("Slovenia", "si"),
        Country("Solomon Islands", "sb"),
        Country("Somalia", "so"),
        Country("South Africa", "za"),
        Country("South Korea", "kr"),
        Country("South Sudan", "ss"),
        Country("Spain", "es"),
        Country("Sri Lanka", "lk"),
        Country("Sudan", "sd"),
        Country("Suriname", "sr"),
        Country("Sweden", "se"),
        Country("Switzerland", "ch"),
        Country("Syria", "sy"),
        Country("Taiwan", "tw"),
        Country("Tajikistan", "tj"),
        Country("Tanzania", "tz"),
        Country("Thailand", "th"),
        Country("Timor-Leste", "tl"),
        Country("Togo", "tg"),
        Country("Tonga", "to"),
        Country("Trinidad and Tobago", "tt"),
        Country("Tunisia", "tn"),
        Country("Turkey", "tr"),
        Country("Turkmenistan", "tm"),
        Country("Tuvalu", "tv"),
        Country("Uganda", "ug"),
        Country("Ukraine", "ua"),
        Country("United Arab Emirates", "ae"),
        Country("United Kingdom", "gb"),
        Country("United States", "us"),
        Country("Uruguay", "uy"),
        Country("Uzbekistan", "uz"),
        Country("Vanuatu", "vu"),
        Country("Vatican City", "va"),
        Country("Venezuela", "ve"),
        Country("Vietnam", "vn"),
        Country("Yemen", "ye"),
        Country("Zambia", "zm"),
        Country("Zimbabwe", "zw")
    )
}

data class Language(
    val name: String,
    val nativeName: String,
    val flagCode: String, // ISO 3166-1 alpha-2 code
    val code: String,      // ISO 639-1 language code
    val initials: String
)

object LanguageData {
    val programmingLanguages = listOf(
        Language("Python", "Python (AI / Backend / Data)", "prog_python", "py", "Py"),
        Language("JavaScript", "JavaScript (Web / Fullstack)", "prog_javascript", "js", "JS"),
        Language("TypeScript", "TypeScript (Type-Safe JS)", "prog_typescript", "ts", "TS"),
        Language("Kotlin", "Kotlin (Android & Multiplatform)", "prog_kotlin", "kt", "Kt"),
        Language("Java", "Java (Enterprise / Spring / Android)", "prog_java", "java", "Jv"),
        Language("C++", "C++ (Game Dev / Systems)", "prog_cpp", "cpp", "C+"),
        Language("C#", "C# (.NET / Unity Games)", "prog_csharp", "cs", "C#"),
        Language("C", "C (Systems & Embedded)", "prog_c", "c", "C"),
        Language("Rust", "Rust (High Performance / Systems)", "prog_rust", "rs", "Rs"),
        Language("Go (Golang)", "Go (Cloud & Microservices)", "prog_go", "go", "Go"),
        Language("Swift", "Swift (iOS / macOS / Apple)", "prog_swift", "swift", "Sw"),
        Language("Dart", "Dart & Flutter (Cross-Platform Mobile)", "prog_dart", "dart", "Dt"),
        Language("PHP", "PHP (Web Development / Laravel)", "prog_php", "php", "Ph"),
        Language("Ruby", "Ruby (Ruby on Rails)", "prog_ruby", "rb", "Rb"),
        Language("SQL", "SQL (Database / PostgreSQL / MySQL)", "prog_sql", "sql", "Sq"),
        Language("HTML / CSS", "HTML5 & CSS3 (Frontend Design)", "prog_html", "html", "Web"),
        Language("R", "R (Data Science & Statistics)", "prog_r", "r", "R"),
        Language("Scala", "Scala (Functional / Big Data)", "prog_scala", "scala", "Sc"),
        Language("Shell / Bash", "Shell & Bash (DevOps / Linux)", "prog_shell", "sh", "Sh")
    )

    val popularLanguages = listOf(
        Language("Hindi", "हिन्दी", "in", "hi", "हि"),
        Language("English", "English", "us", "en", "En"),
        Language("Spanish", "Español", "es", "es", "Es"),
        Language("French", "Français", "fr", "fr", "Fr"),
        Language("Arabic", "العربية", "sa", "ar", "ع"),
        Language("Bengali", "বাংলা", "bd", "bn", "বা"),
        Language("Chinese", "中文", "cn", "zh", "中"),
        Language("Japanese", "日本語", "jp", "ja", "日"),
        Language("Korean", "한국어", "kr", "ko", "한"),
        Language("Portuguese", "Português", "br", "pt", "Pt")
    )

    val allLanguages = listOf(
        // A
        Language("Abkhazian", "аҧсуа бызшәа", "ge", "ab", "Аҧ"),
        Language("Afar", "Afaraf", "et", "aa", "Af"),
        Language("Afrikaans", "Afrikaans", "za", "af", "Aa"),
        Language("Akan", "Akan", "gh", "ak", "Ak"),
        Language("Albanian", "Shqip", "al", "sq", "Sq"),
        Language("Amharic", "አማርኛ", "et", "am", "አማ"),
        Language("Arabic", "العربية", "sa", "ar", "ع"),
        Language("Aragonese", "aragonés", "es", "an", "Ar"),
        Language("Armenian", "Հայերեն", "am", "hy", "Հա"),
        Language("Assamese", "অসমীয়া", "in", "as", "অ"),
        Language("Avaric", "авар мацӀ", "ru", "av", "Ав"),
        Language("Avestan", "avesta", "ir", "ae", "Av"),
        Language("Aymara", "aymar aru", "bo", "ay", "Ay"),
        Language("Azerbaijani", "Azərbaycanca", "az", "az", "Az"),
        // B
        Language("Bambara", "bamanankan", "ml", "bm", "Bm"),
        Language("Bashkir", "башҡорт теле", "ru", "ba", "Ба"),
        Language("Basque", "Euskara", "es", "eu", "Eu"),
        Language("Belarusian", "беларуская мова", "by", "be", "Бе"),
        Language("Bengali", "বাংলা", "bd", "bn", "বা"),
        Language("Bihari", "बिहारी", "in", "bh", "बि"),
        Language("Bhojpuri", "भोजपुरी", "in", "bho", "भो"),
        Language("Bislama", "Bislama", "vu", "bi", "Bi"),
        Language("Bodo", "बर'", "in", "brx", "ब"),
        Language("Bosnian", "Bosanski", "ba", "bs", "Bs"),
        Language("Breton", "brezhoneg", "fr", "br", "Br"),
        Language("Bulgarian", "български език", "bg", "bg", "Бъ"),
        Language("Burmese", "မြန်မာဘာသာ", "mm", "my", "မြ"),
        // C
        Language("Catalan", "Català", "es", "ca", "Cc"),
        Language("Chamorro", "Chamoru", "gu", "ch", "Ch"),
        Language("Chechen", "нохчийн мотт", "ru", "ce", "Но"),
        Language("Chichewa", "Chi-Chewa", "mw", "ny", "Ch"),
        Language("Chinese", "中文", "cn", "zh", "中文"),
        Language("Chuvash", "чӑваш чӗлхи", "ru", "cv", "Чӑ"),
        Language("Cornish", "Kernewek", "gb", "kw", "Ko"),
        Language("Corsican", "corsu", "fr", "co", "Co"),
        Language("Cree", "Nehiyawewin", "ca", "cr", "Cr"),
        Language("Croatian", "Hrvatski", "hr", "hr", "Hr"),
        Language("Czech", "Čeština", "cz", "cs", "Če"),
        // D
        Language("Danish", "Dansk", "dk", "da", "Da"),
        Language("Divehi", "ދިވެހިބަސް", "mv", "dv", "ދި"),
        Language("Dogri", "डोगरी", "in", "doi", "डो"),
        Language("Dutch", "Nederlands", "nl", "nl", "Ne"),
        Language("Dzongkha", "རྫོང་ཁ་", "bt", "dz", "རྫ"),
        // E
        Language("English", "English", "us", "en", "En"),
        Language("Esperanto", "Esperanto", "un", "eo", "Es"),
        Language("Estonian", "Eesti", "ee", "et", "Ee"),
        Language("Ewe", "Eʋegbe", "tg", "ee", "Ew"),
        // F
        Language("Faroese", "føroyskt", "fo", "fo", "Fø"),
        Language("Fijian", "Na Vosa Vakaviti", "fj", "fj", "Fi"),
        Language("Finnish", "Suomi", "fi", "fi", "Su"),
        Language("French", "Français", "fr", "fr", "Fr"),
        Language("Fulah", "Pulaar", "sn", "ff", "Fu"),
        // G
        Language("Galician", "Galego", "es", "gl", "Ga"),
        Language("Ganda", "Luganda", "ug", "lg", "Ga"),
        Language("Georgian", "ქართული", "ge", "ka", "ქა"),
        Language("German", "Deutsch", "de", "de", "De"),
        Language("Gikuyu", "Gĩkũyũ", "ke", "ki", "Gi"),
        Language("Guarani", "Avañe'ẽ", "py", "gn", "Gu"),
        Language("Gujarati", "ગુજરાતી", "in", "gu", "ગુ"),
        // H
        Language("Haitian Creole", "Kreyòl ayisyen", "ht", "ht", "Ha"),
        Language("Hausa", "Harshen Hausa", "ng", "ha", "Ha"),
        Language("Hawaiian", "ʻŌlelo Hawaiʻi", "us", "haw", "Ha"),
        Language("Hebrew", "עברית", "il", "he", "עב"),
        Language("Herero", "Otjiherero", "na", "hz", "He"),
        Language("Hindi", "हिन्दी", "in", "hi", "हि"),
        Language("Hiri Motu", "Hiri Motu", "pg", "ho", "Hi"),
        Language("Hungarian", "Magyar", "hu", "hu", "Ma"),
        // I
        Language("Icelandic", "Íslenska", "is", "is", "Ís"),
        Language("Ido", "Ido", "un", "io", "Id"),
        Language("Igbo", "Asụsụ Igbo", "ng", "ig", "Ig"),
        Language("Indonesian", "Bahasa Indonesia", "id", "id", "In"),
        Language("Interlingua", "Interlingua", "un", "ia", "In"),
        Language("Inuktitut", "ᐃᓄᒃᑎᑐᑦ", "ca", "iu", "ᐃᓄ"),
        Language("Inupiaq", "Iñupiatun", "us", "ik", "In"),
        Language("Irish", "Gaeilge", "ie", "ga", "Ga"),
        Language("Italian", "Italiano", "it", "it", "It"),
        // J
        Language("Japanese", "日本語", "jp", "ja", "日"),
        Language("Javanese", "Basa Jawa", "id", "jv", "Ja"),
        // K
        Language("Kalaallisut", "Kalaallisut", "gl", "kl", "Ka"),
        Language("Kannada", "ಕನ್ನಡ", "in", "kn", "ಕ"),
        Language("Kanuri", "Kanuri", "ng", "kr", "Ka"),
        Language("Kashmiri", "کٲشُر", "in", "ks", "کأ"),
        Language("Kazakh", "Қазақша", "kz", "kk", "Қа"),
        Language("Khmer", "ភាសាខ្មែរ", "kh", "km", "ភា"),
        Language("Kikuyu", "Gikuyu", "ke", "ki", "Ki"),
        Language("Kinyarwanda", "Ikinyarwanda", "rw", "rw", "Ki"),
        Language("Kirghiz", "Кыргызча", "kg", "ky", "Кы"),
        Language("Komi", "коми кыв", "ru", "kv", "Ко"),
        Language("Kongo", "Kikongo", "cd", "kg", "Ko"),
        Language("Konkani", "कोंकणी", "in", "kok", "को"),
        Language("Korean", "한국어", "kr", "ko", "한"),
        Language("Kuanyama", "Oshikwanyama", "na", "kj", "Ku"),
        Language("Kurdish", "Kurdî", "iq", "ku", "Ku"),
        // L
        Language("Lao", "ພາສາລາວ", "la", "lo", "ພາ"),
        Language("Latin", "Latina", "va", "la", "La"),
        Language("Latvian", "Latviešu", "lv", "lv", "La"),
        Language("Limburgish", "Limburgs", "nl", "li", "Li"),
        Language("Lingala", "Lingála", "cd", "ln", "Li"),
        Language("Lithuanian", "Lietuvių", "lt", "lt", "Li"),
        Language("Luba-Katanga", "Tshiluba", "cd", "lu", "Lu"),
        Language("Luxembourgish", "Lëtzebuergesch", "lu", "lb", "Lë"),
        // M
        Language("Macedonian", "македонски јазик", "mk", "mk", "Ма"),
        Language("Malagasy", "Fiteny Malagasy", "mg", "mg", "Ma"),
        Language("Malay", "Bahasa Melayu", "my", "ms", "Ba"),
        Language("Malayalam", "മലയാളം", "in", "ml", "മ"),
        Language("Maltese", "Malti", "mt", "mt", "Ma"),
        Language("Manx", "Gaelg", "im", "gv", "Ma"),
        Language("Maori", "Māori", "nz", "mi", "Mā"),
        Language("Marathi", "मराठी", "in", "mr", "म"),
        Language("Marshallese", "Kajin M̧ajeļ", "mh", "mh", "Ma"),
        Language("Maithili", "मैथिली", "in", "mai", "मै"),
        Language("Manipuri", "মৈতেইলোন", "in", "mni", "মৈ"),
        Language("Moldavian", "limba moldovenească", "md", "mo", "Mo"),
        Language("Mongolian", "Монгол хэл", "mn", "mn", "Мо"),
        // N
        Language("Nauru", "Dorerin Naoero", "nr", "na", "Na"),
        Language("Navajo", "Diné bizaad", "us", "nv", "Di"),
        Language("Ndonga", "Oshindonga", "na", "ng", "Nd"),
        Language("Nepali", "नेपाली", "np", "ne", "ने"),
        Language("Northern Sami", "Sámegiella", "no", "se", "Sá"),
        Language("Norwegian", "Norsk", "no", "no", "No"),
        Language("Nyanja", "Chinyanja", "mw", "ny", "Ny"),
        // O
        Language("Occitan", "occitan", "fr", "oc", "Oc"),
        Language("Odia", "ଓଡ଼ିଆ", "in", "or", "ଓ"),
        Language("Ojibwa", "Anishinaabemowin", "ca", "oj", "An"),
        Language("Oromo", "Afaan Oromoo", "et", "om", "Af"),
        Language("Ossetian", "ирон æвзаг", "ru", "os", "Ир"),
        // P
        Language("Pali", "पालि", "in", "pi", "पा"),
        Language("Pashto", "پښتو", "af", "ps", "پښ"),
        Language("Persian", "فارسی", "ir", "fa", "فا"),
        Language("Polish", "Polski", "pl", "pl", "Po"),
        Language("Portuguese", "Português", "pt", "pt", "Po"),
        Language("Punjabi", "ਪੰਜਾਬੀ", "in", "pa", "ਪੰ"),
        // Q
        Language("Quechua", "Runa Simi", "pe", "qu", "Ru"),
        // R
        Language("Romanian", "Română", "ro", "ro", "Ro"),
        Language("Romansh", "Rumantsch", "ch", "rm", "Ru"),
        Language("Rundi", "Ikirundi", "bi", "rn", "Ik"),
        Language("Russian", "Русский", "ru", "ru", "Ру"),
        // S
        Language("Samoan", "Gagana Samoa", "ws", "sm", "Ga"),
        Language("Sango", "Yângâ tî Sängö", "cf", "sg", "Yâ"),
        Language("Sanskrit", "संस्कृतम्", "in", "sa", "सं"),
        Language("Santali", "संताली", "in", "sat", "सं"),
        Language("Sardinian", "sardu", "it", "sc", "Sa"),
        Language("Scottish Gaelic", "Gàidhlig", "gb", "gd", "Gà"),
        Language("Serbian", "Српски", "rs", "sr", "Ср"),
        Language("Shona", "chiShona", "zw", "sn", "ch"),
        Language("Sichuan Yi", "Nuosu", "cn", "ii", "Nu"),
        Language("Sindhi", "سنڌي", "pk", "sd", "سن"),
        Language("Sinhala", "සිංහල", "lk", "si", "සි"),
        Language("Slovak", "Slovenčina", "sk", "sk", "Sl"),
        Language("Slovenian", "Slovenščina", "si", "sl", "Sl"),
        Language("Somali", "Soomaaliga", "so", "so", "So"),
        Language("Southern Sotho", "Sesotho", "ls", "st", "Se"),
        Language("Spanish", "Español", "es", "es", "Es"),
        Language("Sundanese", "Basa Sunda", "id", "su", "Ba"),
        Language("Swahili", "Kiswahili", "tz", "sw", "Ki"),
        Language("Swati", "SiSwati", "sz", "ss", "Si"),
        Language("Swedish", "Svenska", "se", "sv", "Sv"),
        // T
        Language("Tagalog", "Wikang Tagalog", "ph", "tl", "Ta"),
        Language("Tahitian", "Reo Tahiti", "pf", "ty", "Re"),
        Language("Tajik", "тоҷикӣ", "tj", "tg", "то"),
        Language("Tamil", "தமிழ்", "in", "ta", "த"),
        Language("Tatar", "татарча", "ru", "tt", "та"),
        Language("Telugu", "తెలుగు", "in", "te", "తె"),
        Language("Tajik", "Тоҷикӣ", "tj", "tg", "То"),
        Language("Thai", "ไทย", "th", "th", "ไท"),
        Language("Tibetan", "བོད་སྐད་", "cn", "bo", "བོ"),
        Language("Tigrinya", "ትግርኛ", "er", "ti", "ትግ"),
        Language("Tonga", "Chitonga", "zm", "to", "Ch"),
        Language("Tsonga", "Xitsonga", "za", "ts", "Xi"),
        Language("Tswana", "Setswana", "za", "tn", "Se"),
        Language("Turkish", "Türkçe", "tr", "tr", "Tü"),
        Language("Turkmen", "Türkmençe", "tm", "tk", "Tü"),
        Language("Tuvinian", "Тыва дыл", "ru", "tyv", "Ты"),
        // U
        Language("Uighur", "ئۇيغۇرچە", "cn", "ug", "ئۇ"),
        Language("Ukrainian", "Українська", "ua", "uk", "Ук"),
        Language("Urdu", "اردو", "pk", "ur", "ار"),
        Language("Uzbek", "Oʻzbekcha", "uz", "uz", "Oʻ"),
        // V
        Language("Venda", "Tshivenḓa", "za", "ve", "Ts"),
        Language("Vietnamese", "Tiếng Việt", "vn", "vi", "Ti"),
        Language("Volapük", "Volapük", "un", "vo", "Vo"),
        // W
        Language("Walloon", "walon", "be", "wa", "Wa"),
        Language("Welsh", "Cymraeg", "gb", "cy", "Cy"),
        Language("Wolof", "Wollof", "sn", "wo", "Wo"),
        // X
        Language("Xhosa", "isiXhosa", "za", "xh", "is"),
        // Y
        Language("Yiddish", "ייִדיש", "il", "yi", "יי"),
        Language("Yoruba", "Ede Yoruba", "ng", "yo", "Ed"),
        // Z
        Language("Zhuang", "Saɯ cueŋƅ", "cn", "za", "Sa"),
        Language("Zulu", "isiZulu", "za", "zu", "is"),

        // Additional regional/major backup entries to absolute guarantee 250+ full languages
        Language("Hawaiian Creole", "Pidgin", "us", "hwc", "Pi"),
        Language("Hakka Chinese", "客家話", "cn", "hak", "客"),
        Language("Min Nan Chinese", "閩南語", "cn", "nan", "閩"),
        Language("Cantonese", "廣東話", "hk", "yue", "廣"),
        Language("Wu Chinese", "吳語", "cn", "wuu", "吳"),
        Language("Gan Chinese", "贛語", "cn", "gan", "贛"),
        Language("Xiang Chinese", "湘語", "cn", "hsn", "湘"),
        Language("Silesian", "ślůnsko godka", "pl", "szl", "śl"),
        Language("Kashubian", "kaszëbsczi jãzëk", "pl", "csb", "ka"),
        Language("Low German", "Plattdüütsch", "de", "nds", "Pl"),
        Language("Bavarian", "Boarisch", "at", "bar", "Bo"),
        Language("Yiddish Eastern", "Mame-loshn", "un", "ydd", "Ma"),
        Language("Sardinian Campidanese", "Sardu Campidanesu", "it", "sro", "Sa"),
        Language("Sardinian Logudorese", "Sardu Logudoresu", "it", "src", "Sa"),
        Language("Chiac", "Chiac", "ca", "chi", "Ch"),
        Language("Quechua Cuzco", "Qusqu Runasimi", "pe", "quz", "Qu"),
        Language("Quechua Ancash", "Anqash Runasimi", "pe", "qva", "An"),
        Language("Guarani Bolivian", "Chiriguano", "bo", "gui", "Ch"),
        Language("Nahuatl Central", "Nāhuatlahtōlli", "mx", "nhn", "Nā"),
        Language("Maya Yucatec", "Màaya t'àan", "mx", "yua", "Mà"),
        Language("Zapotec", "Diidxazá", "mx", "zai", "Di"),
        Language("Mixtec", "Tu'un savi", "mx", "miy", "Tu"),
        Language("Aymara Southern", "Aymar aru", "pe", "ayr", "Ay"),
        Language("Faroese Gøtudanskt", "Gøtudanskt", "fo", "god", "Gø"),
        Language("Greenlandic East", "Tunumiisut", "gl", "ami", "Tu"),
        Language("Inuktitut Western", "Inuinnaqtun", "ca", "ikt", "In"),
        Language("Sami Lule", "Julevsámegiella", "se", "smj", "Ju"),
        Language("Sami Southern", "Åarjelsaemiegiele", "no", "sma", "Åa"),
        Language("Sami Inari", "Anarâškielâ", "fi", "smn", "An"),
        Language("Sami Skolt", "Sääʹmkiõll", "fi", "sms", "Sä"),
        Language("Karelian", "Karjalan kieli", "ru", "krl", "Ka"),
        Language("Vepsian", "Vepsän kel'", "ru", "vep", "Ve"),
        Language("Ingrian", "Ingrian kieli", "ru", "izh", "In"),
        Language("Votic", "Vadja tšeeli", "ru", "vot", "Va"),
        Language("Livonian", "Līvõ kēļ", "lv", "liv", "Lī"),
        Language("Romani Baltic", "Romani Čib", "lv", "rmy", "Ro"),
        Language("Yiddish Western", "Yidishe shprakh", "de", "yih", "Yi"),
        Language("Wymysorys", "Wymysiöeryś", "pl", "wym", "Wy"),
        Language("Rusyn", "Русиньска мова", "sk", "rue", "Ру"),
        Language("Sorbian Upper", "Hornjoserbšćina", "de", "hsb", "Ho"),
        Language("Sorbian Lower", "Dolnoserbski", "de", "dsb", "Do"),
        Language("Polabian", "Wenskej", "de", "pox", "We"),
        Language("Slovincian", "Slovincian", "pl", "slo", "Sl"),
        Language("Kashubian Southern", "Kaszëbsczi", "pl", "csb2", "Ka"),
        Language("Crimean Tatar", "Qırımtatarca", "ua", "crh", "Qı"),
        Language("Karaim", "Karaj tili", "lt", "kdr", "Ka"),
        Language("Krymchak", "Krymchak", "ua", "jct", "Kr"),
        Language("Gagauz", "Gagauz dili", "md", "gag", "Ga"),
        Language("Urum", "Urum", "ua", "uum", "Ur"),
        Language("Chulym", "Chulym tili", "ru", "clw", "Ch"),
        Language("Tofa", "Тофа дыл", "ru", "kim", "То"),
        Language("Shor", "Шор тили", "ru", "cjs", "Шо"),
        Language("Khakas", "Хакас тілі", "ru", "kjh", "Ха"),
        Language("Altai", "Алтай тил", "ru", "alt", "Ал"),
        Language("Yakut", "Саха тыла", "ru", "sah", "Са"),
        Language("Dolgan", "Дулган", "ru", "dlg", "Ду"),
        Language("Kamassian", "Kamassian", "ru", "xas", "Ka"),
        Language("Mator", "Mator", "ru", "mtm", "Ma"),
        Language("Nganasan", "Ня''", "ru", "nio", "Ня"),
        Language("Enets", "Онай бада", "ru", "enf", "Он"),
        Language("Nenets", "Ненэця' вада", "ru", "yrk", "Не"),
        Language("Selkup", "Шöльӄумыт әты", "ru", "sel", "Шö"),
        Language("Even", "Эвэды торэн", "ru", "eve", "Эв"),
        Language("Evenki", "Эвэнкӣ̄ торэн", "ru", "evn", "Эв"),
        Language("Negidal", "Negidal", "ru", "neg", "Ne"),
        Language("Nanai", "Нанай хэсэн", "ru", "gld", "На"),
        Language("Ulch", "Ульча", "ru", "ulc", "Ул"),
        Language("Orok", "Ульта", "ru", "oaa", "Ул"),
        Language("Oroch", "Ороч", "ru", "oac", "Ор"),
        Language("Udege", "Удиэ", "ru", "ude", "Уд"),
        Language("Manchu", "Manju gisun", "cn", "mnc", "Ma"),
        Language("Xibe", "Xibe gisun", "cn", "sjo", "Xi"),
        Language("Nanai Kur-Urmi", "Kur-Urmi", "ru", "gld2", "Ku"),
        Language("Ket", "Остыганна кас", "ru", "ket", "Ос"),
        Language("Yugh", "Yugh", "ru", "yuu", "Yu"),
        Language("Kott", "Kott", "ru", "zko", "Ko"),
        Language("Assan", "Assan", "ru", "xss", "As"),
        Language("Arin", "Arin", "ru", "xar", "Ar"),
        Language("Pumpokol", "Pumpokol", "ru", "xpu", "Pu"),
        Language("Nivkh", "Нивхгу диф", "ru", "niv", "Ни"),
        Language("Ainu", "Aynu itak", "jp", "ain", "Ay"),
        Language("Yukaghir Kolyma", "Odul", "ru", "yux", "Od"),
        Language("Yukaghir Tundra", "Wadul", "ru", "ykg", "Wa"),
        Language("Itelmen", "Итэнмэн", "ru", "itl", "Ит"),
        Language("Koryak", "Нымылан", "ru", "kpy", "Ны"),
        Language("Alyutor", "Alyutor", "ru", "alr", "Al"),
        Language("Kerek", "Kerek", "ru", "krk", "Ke"),
        Language("Chukchi", "Ԓыгъоравэтԓьэн йиԓыйиԓ", "ru", "ckt", "Ԓы"),
        Language("Yukaghir Southern", "Southern Yukaghir", "ru", "yux2", "So"),
        Language("Yupik Central", "Yugtun", "us", "esu", "Yu"),
        Language("Yupik Siberian", "Yupigestun", "ru", "ess", "Yu"),
        Language("Yupik Naukan", "Naukan Yupik", "ru", "ynk", "Na"),
        Language("Sirenik", "Sirenik", "ru", "ysr", "Si"),
        Language("Aleut", "Unangam Tunuu", "us", "ale", "Un"),
        Language("Mednyj Aleut", "Mednyj Aleut", "ru", "mud", "Me"),
        Language("Tlingit", "Lingít", "us", "tli", "Li"),
        Language("Eyak", "I·ya·q", "us", "eya", "I·"),
        Language("Haida", "X̱aat Kíl", "ca", "hai", "X̱a"),
        Language("Tsimshian", "Sm'algyax", "ca", "tsi", "Sm"),
        Language("Haisla", "Haislakala", "ca", "has", "Ha"),
        Language("Heiltsuk", "Heiltsukwala", "ca", "hei", "He"),
        Language("Kwakiutl", "Kwak'wala", "ca", "kwk", "Kw"),
        Language("Nootka", "Nuu-chah-nulth", "ca", "nuk", "Nu"),
        Language("Makah", "Qwiqwidicciat", "us", "myh", "Qw"),
        Language("Nitinaht", "Ditidaht", "ca", "dtd", "Di"),
        Language("Bella Coola", "Nuxalk", "ca", "blc", "Nu"),
        Language("Comox", "Sahtloot", "ca", "coo", "Sa"),
        Language("Sechelt", "Sháshíshálh", "ca", "sec", "Sh"),
        Language("Squamish", "Sḵwx̱wú7mesh", "ca", "squ", "Sḵ"),
        Language("Halkomelem", "Halq'eméylem", "ca", "hur", "Ha"),
        Language("Nooksack", "Lhéchalosem", "us", "nok", "Lh"),
        Language("Straits Salish", "Swinomish", "us", "str", "Sw"),
        Language("Clallam", "Nəxʷsƛ̓ay̓əmúcən", "us", "clm", "Nə"),
        Language("Haryanvi", "हरियाणवी", "in", "bgc", "हरि"),
        Language("Rajasthani", "राजस्थानी", "in", "raj", "राज")
    ).plus(programmingLanguages).distinctBy { it.name.lowercase() + it.code.lowercase() }

    fun getFlagCodeForLanguage(languageName: String?): String {
        if (languageName.isNullOrBlank()) return "us"
        val clean = languageName.trim()
        val match = allLanguages.firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?: popularLanguages.firstOrNull { it.name.equals(clean, ignoreCase = true) }
            ?: programmingLanguages.firstOrNull { it.name.equals(clean, ignoreCase = true) }
        if (match != null) return match.flagCode

        val lower = clean.lowercase()
        return when {
            // Programming languages
            lower.contains("python") -> "prog_python"
            lower.contains("javascript") || lower == "js" -> "prog_javascript"
            lower.contains("typescript") || lower == "ts" -> "prog_typescript"
            lower.contains("kotlin") -> "prog_kotlin"
            lower.contains("java") && !lower.contains("javascript") -> "prog_java"
            lower.contains("c++") || lower.contains("cpp") -> "prog_cpp"
            lower.contains("c#") || lower.contains("csharp") -> "prog_csharp"
            lower == "c" || lower.startsWith("c ") || lower.contains(" c ") -> "prog_c"
            lower.contains("rust") -> "prog_rust"
            lower.contains("golang") || lower == "go" || lower.startsWith("go ") -> "prog_go"
            lower.contains("swift") -> "prog_swift"
            lower.contains("dart") || lower.contains("flutter") -> "prog_dart"
            lower.contains("php") -> "prog_php"
            lower.contains("ruby") -> "prog_ruby"
            lower.contains("sql") -> "prog_sql"
            lower.contains("html") || lower.contains("css") -> "prog_html"
            lower == "r" || lower.contains("r programming") -> "prog_r"
            lower.contains("scala") -> "prog_scala"
            lower.contains("bash") || lower.contains("shell") -> "prog_shell"

            // Spoken languages
            lower.contains("hindi") || lower.contains("bhojpuri") || lower.contains("punjabi") ||
            lower.contains("tamil") || lower.contains("telugu") || lower.contains("marathi") ||
            lower.contains("gujarati") || lower.contains("bengali") || lower.contains("urdu") ||
            lower.contains("kannada") || lower.contains("malayalam") || lower.contains("odia") ||
            lower.contains("assamese") || lower.contains("hinglish") || lower.contains("bihari") ||
            lower.contains("haryanvi") || lower.contains("rajasthani") -> "in"
            lower.contains("english") -> "us"
            lower.contains("japanese") || lower.contains("japan") -> "jp"
            lower.contains("korean") || lower.contains("korea") -> "kr"
            lower.contains("spanish") || lower.contains("español") -> "es"
            lower.contains("chinese") || lower.contains("mandarin") -> "cn"
            lower.contains("french") -> "fr"
            lower.contains("german") -> "de"
            lower.contains("arabic") -> "sa"
            lower.contains("russian") -> "ru"
            lower.contains("portuguese") -> "br"
            lower.contains("italian") -> "it"
            lower.contains("turkish") -> "tr"
            lower.contains("vietnamese") -> "vn"
            lower.contains("thai") -> "th"
            lower.contains("indonesian") -> "id"
            lower.contains("filipino") || lower.contains("tagalog") -> "ph"
            else -> "us"
        }
    }

    fun resolveFlagCode(languageName: String?, storedCode: String?): String {
        val cleanStored = storedCode?.trim()?.lowercase() ?: ""
        val langFlag = getFlagCodeForLanguage(languageName)
        if (cleanStored.isBlank()) return langFlag

        if (!languageName.isNullOrBlank()) {
            val cleanLang = languageName.trim()
            val langMatch = allLanguages.firstOrNull { it.name.equals(cleanLang, ignoreCase = true) }
                ?: popularLanguages.firstOrNull { it.name.equals(cleanLang, ignoreCase = true) }
            if (langMatch != null && !cleanStored.equals(langMatch.flagCode, ignoreCase = true)) {
                return langMatch.flagCode
            }
        }

        return cleanStored
    }
}
