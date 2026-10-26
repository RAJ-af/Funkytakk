# Edit Profile UI Redesign & PlaceDB Location Integration Plan

A pixel-perfect, modular redesign of FunkyTalk's Edit Profile experience based on the design mockups, featuring specialized BottomSheets for Country selection (250+ countries with CircleFlag), City selection powered by PlaceDB and GPS auto-detection, Age adjustment with a strict 15+ year rule, Gender selection, Language selection with proficiency levels, and Custom Interest creation with zero dummy buttons and guaranteed light-theme text contrast.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were clarified and confirmed with the user in Phase 1:

- **Confirmed Decision 1 (City Data)**: PlaceDB integration will bundle popular/major settlements per country offline for instantaneous loading, combined with live search and GPS location auto-detection. When opening the city sheet, only cities corresponding to the user's currently selected country (e.g. India) will be shown initially.
- **Confirmed Decision 2 (Language Proficiency)**: Adding a language will allow users to choose their proficiency level (`Beginner`, `Intermediate`, `Advanced`, `Native`) so it renders accurately as `Language (Proficiency)` chips (e.g. `Hindi (Advanced) ×`).
- **Confirmed Decision 3 (Manual City Entry)**: When a city is not listed, tapping "City not listed? Enter it manually" will open an inline text entry sheet allowing users to input and save any custom city name.
- **Confirmed Decision 4 (Age Requirement)**: Users must be at least 15 years old to use the app. The age counter and quick picks will enforce `minAge = 15`. Any attempt to decrement below 15 will be blocked with a clear notice.
- **Confirmed Decision 5 (Strict Light Theme Contrast)**: All text styles will use explicit high-contrast colors (`#111827`, `#1F2937`, `#374151`, `#4B5563`) on light backgrounds (`#FAFAEE`, `#FFFFFF`, `#FFF9E6`), completely eliminating the issue where white text rendered invisibly on light themes.
- **Confirmed Decision 6 (Modular Architecture)**: The 1,800+ line monolithic `EditProfileScreen.kt` will be broken down into clean, modular components under `com.example.ui.screens.profile.components`:
  - `EditProfileTopBar.kt`
  - `EditProfileAvatarSection.kt`
  - `EditProfileFormFields.kt`
  - `EditProfilePersonalDetailsGrid.kt`
  - `EditProfileLanguagesSection.kt`
  - `EditProfileInterestsSection.kt`
  - `ChooseCountrySheet.kt`
  - `ChooseCitySheet.kt`
  - `ChooseAgeSheet.kt`
  - `SelectGenderSheet.kt`
  - `AddLanguagesSheet.kt`
  - `AddCustomInterestDialog.kt`
  - `PlaceDbService.kt` (PlaceDB dataset & settlement repository)

---

## 1. Overview & Core Concept

- **What It Does**: Replaces the current cluttered edit profile dialog with an elegant, full-screen, modular profile editor. Users can seamlessly update their display name, username, bio, avatar/cover images, personal details (age, gender, country, city), multilingual proficiencies, and topic interests through dedicated interactive bottom sheets.
- **Target Audience**: Global language learners and community members on FunkyTalk who want a polished identity showcase matching international peers.
- **Key Value**: Eliminates generic and unstyled form fields; provides fluid bottom sheets matching the user's uploaded reference mockups, with real location intelligence and strict validation.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Top Bar & Header**:
   - Circular dark/subtle back button on top-left.
   - Centered bold title: `Edit Profile` in deep slate (`#111827`).
   - Circular bright yellow action button on top-right with dark checkmark icon (`#111827`) to save changes to Supabase.
2. **Avatar & Cover Art**:
   - Centered large circular avatar with modern camera icon badge on bottom-right.
   - Tapping avatar triggers the Android zero-permission Photo Picker (`PickVisualMedia`).
3. **Primary Text Fields**:
   - `Display name` card with light warm background (`#F3F3E8`), clean label, and direct text input.
   - `Username` card with live character counter (`4/30`).
   - `Bio` multiline field with character counter (`48/150`) and soft rounded border.
4. **Personal Details 2x2 Grid**:
   - **Age**: Calendar icon + current age (e.g. `21`) + chevron down. Tapping opens `ChooseAgeSheet`.
   - **Gender**: Person icon + current gender (`Male` / `Female`) + chevron down. Tapping opens `SelectGenderSheet`.
   - **Country**: Globe icon + current country with flag + chevron down. Tapping opens `ChooseCountrySheet`.
   - **City**: Location pin icon + current city + chevron down. Tapping opens `ChooseCitySheet`.
5. **Languages Section**:
   - Section header with amber accent bar on left + yellow `Edit` action button on right.
   - Chips row showing `Native Languages` and `Learning Languages` with country flag circle, language name, proficiency badge, and remove `×` button.
   - Tapping `Edit` opens `AddLanguagesSheet`.
6. **Interests Section**:
   - Multi-category interest chips with custom vector icons (Technology, Writing, Cooking, K-Drama, Fitness, Reading, Art, Nature, Pets, Coding, etc.).
   - `+ Add Interest` dashed card opening `AddCustomInterestDialog`.

### Visual Theme & Color Palette

| Token | Color Value | Description |
| :--- | :--- | :--- |
| **Background** | `#FAFAEE` | Warm organic cream app background |
| **Surface** | `#FFFFFF` | Card & bottom sheet background |
| **Surface Subtle** | `#F5F5EA` | Input field background & inactive chips |
| **Primary Yellow** | `#F8C63D` / `#F7C948` | Done buttons, active indicators, and checkmarks |
| **Text Primary** | `#111827` | High contrast headings & main input text |
| **Text Secondary** | `#4B5563` | Subtitles, labels, and helper descriptions |
| **Text Muted** | `#6B7280` | Character counters, placeholder text |
| **Border Normal** | `#E5E7EB` | Soft outlines on unselected cards |
| **Border Active** | `#F8C63D` | Active selection border on cards |

---

## 3. Detailed Component & Sheet Specifications

### A. Choose Country Sheet (`ChooseCountrySheet.kt`)
- Bottom sheet with drag handle.
- Title `Choose country` with yellow `Done` pill button.
- Search bar with magnifying glass icon: `Search country...`.
- List of 250+ countries from `CountryData.allCountries` with `CircleFlag` circular country flags, dark country names, and custom radio selection indicator (yellow checked circle for active country, grey circle for inactive).
- Selecting a country automatically updates `editCountry` and `editCountryCode`, and resets the city selector to that country's settlements.

### B. Choose City Sheet (`ChooseCitySheet.kt`) with PlaceDB
- Header showing `Choose your city`, country selector pill (`🇮🇳 India ▾`), and yellow `Done` pill button.
- Search bar: `Search cities in <Country>...` (e.g. `Search cities in India...`).
- "Use my current location" row with yellow circular pin icon, detected city name + state, and chevron.
- "POPULAR CITIES" horizontal/grid pills filtered strictly for the active country (e.g. for India: New Delhi, Mumbai, Bengaluru, Hyderabad, Chennai, Kolkata, Pune).
- "ALL CITIES" list with settlement vector icon, city name, province/state, and radio indicator.
- "City not listed? Enter it manually" row at bottom with pencil icon, allowing custom city input.

### C. Choose Age Sheet (`ChooseAgeSheet.kt`)
- Title `Choose your age` with drag handle.
- Large interactive counter:
  - Circular `-` button on left.
  - Large central card with yellow border displaying the age number in bold 32sp text.
  - Circular `+` button on right.
- Enforces `minAge = 15`. If user reaches 15, the `-` button disables or shows a polite prompt.
- "Quick picks" row with selectable pills: `18`, `21`, `25`, `28`, `32`.
- Yellow `Save age` full-width button + `Cancel` text button.

### D. Select Gender Sheet (`SelectGenderSheet.kt`)
- Title `Select gender` with drag handle.
- Two distinct choice cards:
  - `Male`: Person silhouette icon in circle, bold dark label, yellow border and checkmark if selected.
  - `Female`: Female silhouette icon in circle, bold dark label, yellow border and checkmark if selected.
- Yellow `Done` full-width button + `Cancel` text button.

### E. Add Languages Sheet (`AddLanguagesSheet.kt`)
- Title `Add languages`, subtitle `Choose languages you know`.
- Search bar: `Search languages...`.
- List of languages reusing `LanguageData.allLanguages` and `popularLanguages`, with distinct circular badges (e.g. `あ` pink, `fr` blue, `de` orange, `हि` amber).
- When a language is selected, an intuitive proficiency selector sheet/popup appears (`Beginner`, `Intermediate`, `Advanced`, `Native`).
- Yellow `Add selected (N)` full-width button + `Cancel` text button.

### F. Add Custom Interest Dialog (`AddCustomInterestDialog.kt`)
- Yellow sparkle icon badge (`✨`).
- Title `Add a custom interest`, subtitle `Add a hobby, topic, or learning goal.`.
- Text input with tag icon: `e.g. Astronomy, Anime, Poetry` with `0/30` character counter.
- Yellow `Add interest` full-width button + `Cancel` text button.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                         EditProfileScreen.kt                           │
│  (State Orchestrator: DisplayName, Username, Bio, Avatar, Details)     │
└──────────────┬──────────────────┬───────────────────┬──────────────────┘
               │                  │                   │
   ┌───────────▼──────────┐ ┌─────▼──────────────┐ ┌──▼──────────────────┐
   │ Form & Personal Grid │ │  Languages Section │ │  Interests Section  │
   └───────────┬──────────┘ └─────┬──────────────┘ └──┬──────────────────┘
               │                  │                   │
   ┌───────────▼──────────────────▼───────────────────▼──────────────────┐
   │                       Interactive Bottom Sheets                     │
   │  ├─ ChooseCountrySheet.kt (CountryData + CircleFlag)                │
   │  ├─ ChooseCitySheet.kt (PlaceDbService + GPS Location)              │
   │  ├─ ChooseAgeSheet.kt (Counter + Quick Picks + 15+ Constraint)      │
   │  ├─ SelectGenderSheet.kt (Male/Female Selection)                    │
   │  ├─ AddLanguagesSheet.kt (LanguageData + Proficiency Selector)      │
   │  └─ AddCustomInterestDialog.kt (Custom Tag Creation)                │
   └──────────────────────────────────┬──────────────────────────────────┘
                                      │
                         ┌────────────▼─────────────┐
                         │   Supabase Data Layer    │
                         │   (updateProfile RPC)    │
                         └──────────────────────────┘
```

### Data Synchronization & Persistence

- Reads profile via `SupabaseClient.api.getProfile` and `UserProfile`.
- Saves updates via `AuthViewModel.updateProfile` and direct `SupabaseClient.api.updateProfile` call:
  - `profile_name` -> Display name
  - `username` -> Unique handle
  - `bio` / `dob` -> Biography text
  - `country` & `country_code` -> Selected country
  - `city` -> Selected or entered city
  - `gender` -> Selected gender
  - `age` -> Age integer (≥ 15)
  - `native_language` / `learning_language` -> Languages with proficiencies
  - `interests` -> Array of selected topic strings
  - `avatar_url` & `cover_url` -> Cloudinary/Supabase media URLs

---

## 5. Verification & Testing Plan

1. **Compilation Verification**:
   - Run `compile_applet` to verify clean build with zero Kotlin or Compose errors.
2. **Text Contrast & Light Theme Check**:
   - Inspect all text components to ensure `color = Color(0xFF111827)` or theme `onSurface` is explicitly used.
3. **Age Validation**:
   - Verify age cannot be set to less than 15.
4. **PlaceDB Country Filtering**:
   - Verify that switching country updates the city sheet to show that country's popular and all cities.
5. **Photo Picker Integration**:
   - Verify avatar clicking opens system photo picker without requiring broad storage permissions.
6. **Supabase Persistence**:
   - Verify tapping the top-right yellow checkmark saves all updated fields to Supabase and dismisses the screen.
