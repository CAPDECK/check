package com.example.util

import android.content.res.Resources
import androidx.compose.runtime.Immutable
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flagEmoji: String
)

object LocalizationManager {

    val languages = listOf(
        SupportedLanguage("system", "System Default", "System Default", "🌐"),
        SupportedLanguage("ru", "Русский", "Russian", "🇷🇺"),
        SupportedLanguage("en", "English", "English", "🇬🇧"),
        SupportedLanguage("es", "Español", "Spanish", "🇪🇸"),
        SupportedLanguage("fr", "Français", "French", "🇫🇷"),
        SupportedLanguage("de", "Deutsch", "German", "🇩🇪"),
        SupportedLanguage("it", "Italiano", "Italian", "🇮🇹"),
        SupportedLanguage("pt", "Português", "Portuguese", "🇵🇹"),
        SupportedLanguage("zh", "中文 (简体)", "Chinese", "🇨🇳"),
        SupportedLanguage("ja", "日本語", "Japanese", "🇯🇵"),
        SupportedLanguage("ko", "한국어", "Korean", "🇰🇷"),
        SupportedLanguage("tr", "Türkçe", "Turkish", "🇹🇷"),
        SupportedLanguage("pl", "Polski", "Polish", "🇵🇱"),
        SupportedLanguage("uk", "Українська", "Ukrainian", "🇺🇦"),
        SupportedLanguage("ar", "العربية", "Arabic", "🇸🇦"),
        SupportedLanguage("hi", "हिन्दी", "Hindi", "🇮🇳"),
        SupportedLanguage("nl", "Nederlands", "Dutch", "🇳🇱"),
        SupportedLanguage("sv", "Svenska", "Swedish", "🇸🇪"),
        SupportedLanguage("vi", "Tiếng Việt", "Vietnamese", "🇻🇳"),
        SupportedLanguage("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"),
        SupportedLanguage("cs", "Čeština", "Czech", "🇨🇿"),
        SupportedLanguage("el", "Ελληνικά", "Greek", "🇬🇷"),
        SupportedLanguage("ro", "Română", "Romanian", "🇷🇴"),
        SupportedLanguage("hu", "Magyar", "Hungarian", "🇭🇺"),
        SupportedLanguage("da", "Dansk", "Danish", "🇩🇰"),
        SupportedLanguage("fi", "Suomi", "Finnish", "🇫🇮"),
        SupportedLanguage("no", "Norsk", "Norwegian", "🇳🇴"),
        SupportedLanguage("he", "עברית", "Hebrew", "🇮🇱"),
        SupportedLanguage("fa", "فارسی", "Persian", "🇮🇷"),
        SupportedLanguage("bn", "বাংলা", "Bengali", "🇧🇩"),
        SupportedLanguage("th", "ไทย", "Thai", "🇹🇭"),
        SupportedLanguage("kk", "Қазақша", "Kazakh", "🇰🇿"),
        SupportedLanguage("be", "Беларуская", "Belarusian", "🇧🇾"),
        SupportedLanguage("uz", "Oʻzbekcha", "Uzbek", "🇺🇿"),
        SupportedLanguage("az", "Azərbaycan", "Azerbaijani", "🇦🇿"),
        SupportedLanguage("ka", "ქართული", "Georgian", "🇬🇪"),
        SupportedLanguage("hy", "Հայերեն", "Armenian", "🇦🇲")
    )

    private val supportedCodeSet: Set<String> = languages.asSequence()
        .map { it.code }
        .filter { it != "system" }
        .toSet()

    private val languageByCode: Map<String, SupportedLanguage> = languages.associateBy { it.code }

    // Capture initial JVM locale before any app-level Locale.setDefault() overrides it
    private val initialDeviceLocale: Locale = Locale.getDefault()

    @Volatile
    private var cachedPrefInput: String = ""
    @Volatile
    private var cachedEffectiveLang: String = ""
    @Volatile
    private var cachedSysLangTimestamp: Long = 0L
    @Volatile
    private var cachedSysLangCode: String = ""

    private fun normalizeLangCode(raw: String): String {
        val code = raw.lowercase().substringBefore("-").substringBefore("_").trim()
        return when (code) {
            "iw" -> "he"
            "in" -> "id"
            "ji" -> "yi"
            "nb", "nn" -> "no"
            else -> code
        }
    }

    fun getSystemLocale(): Locale {
        return try {
            val config = Resources.getSystem().configuration
            val locales = androidx.core.os.ConfigurationCompat.getLocales(config)
            if (!locales.isEmpty) {
                locales.get(0) ?: initialDeviceLocale
            } else {
                initialDeviceLocale
            }
        } catch (e: Exception) {
            initialDeviceLocale
        }
    }

    fun getSystemLanguageCode(): String {
        val now = System.currentTimeMillis()
        val cached = cachedSysLangCode
        if (cached.isNotEmpty() && now - cachedSysLangTimestamp < 3000L) {
            return cached
        }
        val raw = normalizeLangCode(getSystemLocale().language)
        val resolved = if (supportedCodeSet.contains(raw)) raw else "en"
        cachedSysLangCode = resolved
        cachedSysLangTimestamp = now
        return resolved
    }

    fun getDetectedSystemLanguageName(): String {
        val sysLang = getSystemLanguageCode()
        return languageByCode[sysLang]?.nativeName ?: sysLang.uppercase()
    }

    fun resolveEffectiveLanguage(pref: String): String {
        if (pref.isNotEmpty() && pref != "system" && pref == cachedPrefInput) {
            val cached = cachedEffectiveLang
            if (cached.isNotEmpty()) return cached
        }
        val clean = normalizeLangCode(pref)
        if (clean.isNotBlank() && clean != "system") {
            val resolved = if (supportedCodeSet.contains(clean)) clean else "en"
            cachedPrefInput = pref
            cachedEffectiveLang = resolved
            return resolved
        }
        return getSystemLanguageCode()
    }

    fun getLocale(pref: String): Locale {
        if (pref.isBlank() || pref == "system") {
            return getSystemLocale()
        }
        return Locale(resolveEffectiveLanguage(pref))
    }

    fun isRtlLanguage(lang: String): Boolean {
        val eff = resolveEffectiveLanguage(lang)
        return eff == "ar" || eff == "he" || eff == "fa"
    }

    fun isRussian(lang: String): Boolean {
        return resolveEffectiveLanguage(lang) == "ru"
    }

    private fun build_ru() = mapOf(
        "create_proj_title" to "Новый проект",
        "proj_name_hint" to "Название проекта (книга, сценарий...)",
        "select_format" to "Формат произведения",
        "cover_accent_color" to "Цветовой маркер обложки",
        "protect_pass_hint" to "Пароль защиты (необязательно)",
        "access_password" to "Пароль доступа (опционально)",
        "palette_studio" to "Конструктор цветовой палитры",
        "customize_palette_desc" to "Создайте свой акцентный цвет, оттенок фона и стиль",
        "custom_palette_active" to "Используется персональная палитра",
        "primary_accent" to "Основной акцент",
        "background_canvas" to "Фон и холст",
        "presets_gallery" to "Готовые темы",
        "saved_palettes" to "Мои палитры",
        "apply_palette" to "Применить",
        "palette_applied" to "Палитра успешно применена!",
        "save_as_new" to "Сохранить тему",
        "enter_palette_name" to "Название темы",
        "reset_palette" to "Сброс темы",
        "hue" to "Цветовой тон",
        "saturation" to "Насыщенность",
        "brightness" to "Яркость",
        "hex_code" to "HEX-код цвета",
        "quick_swatches" to "Быстрые оттенки",
        "custom_bg_option" to "Свой цвет фона",
        "sample_book_title" to "Тайна старого маяка",
        "sample_chapter" to "Глава 3 • 4 820 слов",
        "sample_btn_action" to "Продолжить писать",
        "bottom_bar_style" to "Стиль нижней панели",
        "choose_bottom_bar_desc" to "Выберите оформление нижней панели навигации",
        "bar_standard" to "Обычная",
        "bar_capsule" to "Капсула",
        "bar_segmented" to "Раздельные капсулы",
        "keep_import_title" to "Перенос заметок из Google Keep",
        "keep_import_desc" to "Импортируйте заметки, списки и ярлыки из Google Keep (файлы .zip из Google Takeout, .json, .html, .txt или вставка текста)",
        "keep_select_files" to "Выбрать файлы Takeout / ZIP",
        "keep_paste_text" to "Вставить текст заметок",
        "keep_import_dialog_title" to "Импорт из Google Keep",
        "keep_target_project_desc" to "Выберите проект, в который будут сохранены импортированные заметки из Google Keep:",
        "keep_create_new_proj" to "Создать новый проект",
        "keep_project_name_placeholder" to "Название проекта",
        "keep_or_existing_proj" to "Или выберите существующий проект:",
        "keep_importing" to "Импортируем заметки...",
        "keep_paste_dialog_title" to "Вставить заметки Google Keep",
        "keep_paste_dialog_desc" to "Вставьте текст заметок или скопированный JSON из Google Keep:",
        "keep_paste_placeholder" to "Вставьте сюда текст заметок...",
        "import" to "Импортировать",
        "next" to "Далее",
        "my_studio" to "Моя Творческая Студия",
        "cabinet" to "Кабинет",
        "progress" to "Успехи",
        "settings" to "Настройки",
        "options" to "Опции",
        "new_project" to "Новый проект",
        "search" to "Поиск",
        "active" to "Активные",
        "projects" to "Проекты",
        "favorites" to "Избранное",
        "archive" to "Архив",
        "trash" to "Корзина",
        "folders" to "Папки",
        "documents" to "Документы",
        "create" to "Создать",
        "cancel" to "Отмена",
        "versions" to "Версии",
        "versions_subtitle" to "Версии и Ревизии",
        "saved_versions" to "Сохраненные ревизии:",
        "save_snapshot" to "Сделать снимок",
        "save_snapshot_label" to "Сохранить снимок",
        "close" to "Закрыть",
        "find_replace" to "Найти и Заменить",
        "prompter" to "Суфлер",
        "manual_save" to "Сохранить",
        "words" to "Слов",
        "chars" to "Симв",
        "pages" to "Стр",
        "reading_time" to "Чтение",
        "min" to "мин",
        "sys_settings" to "Настройки",
        "theme" to "Тема приложения",
        "theme_light" to "Светлая",
        "theme_dark" to "Темная",
        "theme_black" to "Черная",
        "color_palette" to "Цветовая палитра",
        "security" to "Безопасность и PIN-код",
        "security_sub" to "Введите 4-значный пароль для блокировки Writer's Studio:",
        "four_digits" to "4 цифры",
        "set" to "Задать",
        "disable_pin" to "Отключить PIN защиту",
        "backups" to "Резервное копирование",
        "export_backup" to "Экспортировать Бэкап",
        "import_backup" to "Импортировать Бэкап",
        "app_language" to "Язык интерфейса",
        "choose_lang_title" to "Выберите язык",
        "search_language" to "Поиск языка...",
        "lang_system" to "Системный",
        "system_auto_detect" to "Автоопределение",
        "all_languages" to "Все языки",
        "languages_count_suffix" to "языков",
        "scene" to "Сцена",
        "action" to "Действие",
        "character" to "Персонаж",
        "dialogue" to "Диалог",
        "custom" to "Свой блок",
        "add_block" to "Добавить блок",
        "save_template" to "Сохранить шаблон",
        "saved_templates" to "Сохраненные шаблоны",
        "lang_ru" to "Русский",
        "lang_en" to "English",
        "lang_es" to "Español",
        "interface_style" to "Стиль интерфейса",
        "style_pixel" to "Pixel Style",
        "style_classic" to "Classic Style",
        "screen_refresh_rate" to "Частота обновления экрана",
        "choose_refresh_rate_desc" to "Вынужденно задать частоту обновления между 60, 90, 120 Гц или включить автоматический режим (максимум возможностей экрана для плавности)",
        "refresh_rate_auto" to "Автоматическая",
        "choose_lang_desc" to "Выберите язык приложения",
        "choose_theme_desc" to "Выберите подходящую вам тему",
        "choose_palette_desc" to "Выберите вашу любимую цветовую гамму",
        "choose_style_desc" to "Выберите оформление приложения",
        "security_sub_desc" to "Защитите Writer's Studio с помощью 4-значного PIN-кода",
        "backups_desc" to "Экспорт или импорт локальных файлов бэкапа",
        "sync_storage" to "Синхронизация папок с памятью",
        "sync_storage_desc" to "Синхронизировать все папки и главы проектов в виде файлов во внутренней памяти телефона (в Documents/WriterStudioProjects)",
        "sync_now" to "Синхронизировать сейчас",
        "editor_pro" to "Режим Профи (Блочный)",
        "editor_basic" to "Режим Базовый (Простой Текст)",
        "create_item" to "Создать новую запись",
        "enter_title" to "Введите заголовок",
        "create_folder_title" to "Создать Папку",
        "folder_name" to "Имя папки",
        "create_doc_title" to "Создать Документ",
        "doc_name" to "Имя документа",
        "unnamed" to "Без имени",
        "delete" to "Удалить",
        "edit" to "Редактировать",
        "empty_text" to "Введите текст...",
        "stats_title" to "Статистика Писателя",
        "stats_streak" to "Писательский стрик (дней):",
        "stats_words" to "Всего написано слов:",
        "stats_chars" to "Всего написано символов:",
        "stats_daily" to "Ежедневная продуктивность",
        "select_editor_mode" to "Режим редактора:",
        "type_doc" to "Блочный документ (Pro)",
        "type_txt" to "Текстовый файл (Basic)",
        "format_bold" to "Полужирный",
        "format_italic" to "Курсив",
        "format_underline" to "Подчеркнутый",
        "project_docs" to "Документы проекта",
        "project_root" to "Корень проекта",
        "create_work_hint" to "Создайте папку или десктопный документ для работы",
        "search_filter" to "Поиск и Фильтрация",
        "sort_by" to "Сортировка: ",
        "sort_changed" to "Изм.",
        "sort_name" to "Имя",
        "sort_created" to "Ср.",
        "category" to "Категория: ",
        "search_placeholder" to "Введите название...",
        "cat_all" to "Все",
        "cat_book" to "Книга",
        "cat_screenplay" to "Сценарий",
        "cat_story" to "Рассказ",
        "cat_text" to "Текст",
        "modified" to "Изменен",
        "restore" to "Восстановить",
        "delete_permanent" to "Стереть навсегда",
        "to_trash" to "В корзину",
        "new_project_dialog_title" to "Новый литературный проект",
        "project_name_label" to "Название проекта (книга, сценарий...)",
        "project_title_hint" to "Название проекта",
        "project_name_placeholder_new" to "Название проекта (книга, сценарий...)",
        "color_indicator" to "Цветовой индикатор",
        "access_password_optional" to "Пароль доступа (опционально)",
        "enter_password" to "Введите пароль",
        "password_hint_desc" to "Оставьте пустым, чтобы проект оставался приватным.",
        "project_type" to "Тип проекта:",
        "color_marker" to "Цветовой маркер:",
        "no_projects_in_section" to "Нет проектов в этом разделе",
        "protected_project" to "Защищенный проект",
        "project_locked_hint" to "Этот проект заблокирован паролем. Пожалуйста, введите пароль:",
        "password" to "Пароль",
        "enter_btn" to "Вход",
        "project_folder" to "Папка проекта",
        "document" to "Документ",
        "speech_live" to "Живая речь: ~",
        "minutes_left" to " минут до конца",
        "mirror_h" to "Зерк H",
        "mirror_v" to "Зерк V",
        "speed" to "Скорость",
        "prompter_theme" to "Тема:",
        "theme_dark_prompter" to "Темная",
        "theme_amber_prompter" to "Янтарь",
        "theme_bright_prompter" to "Светлая",
        "exit" to "Выйти",
        "basic_mode_desc" to "Базовый режим — стандартная работа с текстовыми файлами. Весь контент редактируется в одном большом поле ввода.",
        "pro_mode_desc" to "Про режим — блочный режим сценарной / книжной верстки, возможностью вставки картинок.",
        "days_short" to "дн.",
        "words_suffix" to "слов",
        "chars_suffix" to "зн.",
        "productivity_title" to "Продуктивность (Последние дни):",
        "earlier" to "Ранее",
        "today" to "Сегодня",
        "backup_success" to "Резервная копия успешно восстановлена!",
        "backup_error" to "Ошибка восстановления ревизии",
        "read_error" to "Ошибка чтения файла",
        "pin_saved" to "Пароль установлен",
        "pin_removed" to "Пароль удален",
        "export_error" to "Ошибка сохранения резервной копии",
        "keyboard_cyrillic" to "Ввод (Кириллица)",
        "no_projects_yet" to "Проектов пока нет",
        "create_first_project_desc" to "Создайте свой первый проект, чтобы начать писать и воплотить свои идеи в жизнь."
    )

    private fun build_en() = mapOf(
        "create_proj_title" to "New Project",
        "proj_name_hint" to "Project title (book, screenplay...)",
        "select_format" to "Format",
        "cover_accent_color" to "Cover accent color",
        "protect_pass_hint" to "Protection password (optional)",
        "access_password" to "Access password (optional)",
        "palette_studio" to "Color Palette Studio",
        "customize_palette_desc" to "Customize your accent color, canvas tone, and style",
        "custom_palette_active" to "Custom palette in use",
        "primary_accent" to "Primary Accent",
        "background_canvas" to "Background & Canvas",
        "presets_gallery" to "Preset Themes",
        "saved_palettes" to "My Palettes",
        "apply_palette" to "Apply",
        "palette_applied" to "Palette applied successfully!",
        "save_as_new" to "Save Theme",
        "enter_palette_name" to "Theme Name",
        "reset_palette" to "Reset Theme",
        "hue" to "Hue",
        "saturation" to "Saturation",
        "brightness" to "Brightness",
        "hex_code" to "HEX Color Code",
        "quick_swatches" to "Quick Swatches",
        "custom_bg_option" to "Custom Background Color",
        "sample_book_title" to "Secret of the Lighthouse",
        "sample_chapter" to "Chapter 3 • 4,820 words",
        "sample_btn_action" to "Continue Writing",
        "bottom_bar_style" to "Bottom navigation bar style",
        "choose_bottom_bar_desc" to "Choose the visual design of the bottom navigation bar",
        "bar_standard" to "Standard",
        "bar_capsule" to "Capsule",
        "bar_segmented" to "Segmented capsules",
        "keep_import_title" to "Import notes from Google Keep",
        "keep_import_desc" to "Import notes, checklists, and labels from Google Keep (Google Takeout .zip, .json, .html, .txt or paste text)",
        "keep_select_files" to "Select Takeout / ZIP files",
        "keep_paste_text" to "Paste notes text",
        "keep_import_dialog_title" to "Import from Google Keep",
        "keep_target_project_desc" to "Select destination project for imported Google Keep notes:",
        "keep_create_new_proj" to "Create a new project",
        "keep_project_name_placeholder" to "Project title",
        "keep_or_existing_proj" to "Or select an existing project:",
        "keep_importing" to "Importing notes...",
        "keep_paste_dialog_title" to "Paste Google Keep notes",
        "keep_paste_dialog_desc" to "Paste text of notes or copied JSON from Google Keep:",
        "keep_paste_placeholder" to "Paste notes text here...",
        "import" to "Import",
        "next" to "Next",
        "my_studio" to "My Creative Studio",
        "cabinet" to "Cabinet",
        "progress" to "Progress",
        "settings" to "Settings",
        "options" to "Options",
        "new_project" to "New Project",
        "search" to "Search",
        "active" to "Active",
        "projects" to "Projects",
        "favorites" to "Favorites",
        "archive" to "Archive",
        "trash" to "Trash",
        "folders" to "Folders",
        "documents" to "Documents",
        "create" to "Create",
        "cancel" to "Cancel",
        "versions" to "Versions",
        "versions_subtitle" to "Versions & Snapshots",
        "saved_versions" to "Saved revisions:",
        "save_snapshot" to "Create snapshot",
        "save_snapshot_label" to "Snapshot label",
        "close" to "Close",
        "find_replace" to "Find & Replace",
        "prompter" to "Prompter",
        "manual_save" to "Save",
        "words" to "Words",
        "chars" to "Chars",
        "pages" to "Pages",
        "reading_time" to "Reading",
        "min" to "min",
        "sys_settings" to "Settings",
        "theme" to "App theme",
        "theme_light" to "Light",
        "theme_dark" to "Dark",
        "theme_black" to "Black",
        "color_palette" to "Color palette",
        "security" to "Security and PIN protection",
        "security_sub" to "Enter 4-digit password to lock Writer's Studio:",
        "four_digits" to "4 digits",
        "set" to "Set",
        "disable_pin" to "Disable PIN Protection",
        "backups" to "Backup and Restore",
        "export_backup" to "Export Backup",
        "import_backup" to "Import Backup",
        "app_language" to "Interface language",
        "choose_lang_title" to "Select Language",
        "search_language" to "Search language...",
        "lang_system" to "System Default",
        "system_auto_detect" to "Auto-detected",
        "all_languages" to "All Languages",
        "languages_count_suffix" to "languages",
        "scene" to "Scene Heading",
        "action" to "Action",
        "character" to "Character",
        "dialogue" to "Dialogue",
        "custom" to "Custom Block",
        "add_block" to "Add Block",
        "save_template" to "Save Template",
        "saved_templates" to "Saved Templates",
        "lang_ru" to "Russian",
        "lang_en" to "English",
        "lang_es" to "Spanish",
        "interface_style" to "Interface style",
        "style_pixel" to "Pixel Style",
        "style_classic" to "Classic Style",
        "screen_refresh_rate" to "Screen refresh rate",
        "choose_refresh_rate_desc" to "Force display refresh rate between 60, 90, or 120 Hz, or select Automatic (maximum refresh rate for buttery-smooth animations)",
        "refresh_rate_auto" to "Automatic",
        "choose_lang_desc" to "Choose the app language",
        "choose_theme_desc" to "Select your preferred theme",
        "choose_palette_desc" to "Pick your favorite color palette",
        "choose_style_desc" to "Choose the look of the interface",
        "security_sub_desc" to "Protect Writer's Studio with a 4-digit PIN",
        "backups_desc" to "Export or import local backup files",
        "sync_storage" to "Storage Directory Synchronization",
        "sync_storage_desc" to "Export and sync folders/documents as editable files in the phone internal memory (Documents/WriterStudioProjects)",
        "sync_now" to "Sync Now",
        "editor_pro" to "Pro Mode (Block-based)",
        "editor_basic" to "Basic Mode (Plain Text)",
        "create_item" to "Create new record",
        "enter_title" to "Enter title",
        "create_folder_title" to "Create Folder",
        "folder_name" to "Folder name",
        "create_doc_title" to "Create Document",
        "doc_name" to "Document name",
        "unnamed" to "Unnamed",
        "delete" to "Delete",
        "edit" to "Edit",
        "empty_text" to "Type text here...",
        "stats_title" to "Writer's Statistics",
        "stats_streak" to "Writing Streak (days):",
        "stats_words" to "Total Words Written:",
        "stats_chars" to "Total Characters Written:",
        "stats_daily" to "Daily Productivity Log",
        "select_editor_mode" to "Editor Type:",
        "type_doc" to "Rich Block Document (Pro)",
        "type_txt" to "Simple Text File (Basic)",
        "format_bold" to "Bold",
        "format_italic" to "Italic",
        "format_underline" to "Underline",
        "project_docs" to "Project Documents",
        "project_root" to "Project root",
        "create_work_hint" to "Create a folder or document to start writing",
        "search_filter" to "Search & Filters",
        "sort_by" to "Sort by: ",
        "sort_changed" to "Mod.",
        "sort_name" to "Name",
        "sort_created" to "Crd.",
        "category" to "Category: ",
        "search_placeholder" to "Enter name...",
        "cat_all" to "All",
        "cat_book" to "Book",
        "cat_screenplay" to "Screenplay",
        "cat_story" to "Story",
        "cat_text" to "Text",
        "modified" to "Modified",
        "restore" to "Restore",
        "delete_permanent" to "Delete permanently",
        "to_trash" to "Move to trash",
        "new_project_dialog_title" to "New Literary Project",
        "project_name_label" to "Project title (book, screenplay...)",
        "project_title_hint" to "Project title",
        "project_name_placeholder_new" to "Project title (book, screenplay...)",
        "color_indicator" to "Color indicator",
        "access_password_optional" to "Access password (optional)",
        "enter_password" to "Enter password",
        "password_hint_desc" to "Leave empty to keep the project private.",
        "project_type" to "Project type:",
        "color_marker" to "Color indicator:",
        "no_projects_in_section" to "No projects in this section",
        "protected_project" to "Protected Project",
        "project_locked_hint" to "This project is password protected. Please enter password:",
        "password" to "Password",
        "enter_btn" to "Enter",
        "project_folder" to "Project folder",
        "document" to "Document",
        "speech_live" to "Live speech: ~",
        "minutes_left" to " minutes remaining",
        "mirror_h" to "Mirror H",
        "mirror_v" to "Mirror V",
        "speed" to "Speed",
        "prompter_theme" to "Theme:",
        "theme_dark_prompter" to "Dark",
        "theme_amber_prompter" to "Amber",
        "theme_bright_prompter" to "Bright",
        "exit" to "Exit",
        "basic_mode_desc" to "Basic mode — standard plain text editing. The entire document is managed in a single text field.",
        "pro_mode_desc" to "Pro mode — structured book format with scene tags, block-by-block text fields and rich media insertions.",
        "days_short" to "days",
        "words_suffix" to "words",
        "chars_suffix" to "chars",
        "productivity_title" to "Productivity (Last days):",
        "earlier" to "Earlier",
        "today" to "Today",
        "backup_success" to "Backup successfully restored!",
        "backup_error" to "Error restoring backup",
        "read_error" to "Error reading file",
        "pin_saved" to "PIN configured",
        "pin_removed" to "PIN disabled",
        "export_error" to "Error exporting backup",
        "keyboard_cyrillic" to "On-Screen Cyrillic Input",
        "no_projects_yet" to "No projects yet",
        "create_first_project_desc" to "Create your first project to start writing and bring your ideas to life."
    )

    private fun build_de() = mapOf(
        "my_studio" to "Mein Schreibstudio",
        "cabinet" to "Schreibtisch",
        "progress" to "Fortschritt",
        "settings" to "Einstellungen",
        "options" to "Optionen",
        "new_project" to "Neues Projekt",
        "search" to "Suchen",
        "active" to "Aktiv",
        "projects" to "Projekte",
        "favorites" to "Favoriten",
        "archive" to "Archiv",
        "trash" to "Papierkorb",
        "folders" to "Ordner",
        "documents" to "Dokumente",
        "create" to "Erstellen",
        "cancel" to "Abbrechen",
        "versions" to "Versionen",
        "versions_subtitle" to "Versionen & Snapshots",
        "saved_versions" to "Gespeicherte Versionen:",
        "save_snapshot" to "Snapshot erstellen",
        "save_snapshot_label" to "Snapshot-Bezeichnung",
        "close" to "Schließen",
        "find_replace" to "Suchen & Ersetzen",
        "prompter" to "Teleprompter",
        "manual_save" to "Speichern",
        "words" to "Wörter",
        "chars" to "Zeichen",
        "pages" to "Seiten",
        "reading_time" to "Lesezeit",
        "min" to "Min",
        "sys_settings" to "Einstellungen",
        "theme" to "Design",
        "theme_light" to "Hell",
        "theme_dark" to "Dunkel",
        "theme_black" to "Schwarz (OLED)",
        "color_palette" to "Farbpalette",
        "security" to "Sicherheit & PIN-Schutz",
        "security_sub" to "Geben Sie eine 4-stellige PIN zum Sperren ein:",
        "four_digits" to "4 Ziffern",
        "set" to "Festlegen",
        "disable_pin" to "PIN deaktivieren",
        "backups" to "Sicherung & Wiederherstellung",
        "export_backup" to "Backup exportieren",
        "import_backup" to "Backup importieren",
        "app_language" to "Sprache",
        "lang_system" to "Systemstandard",
        "system_auto_detect" to "Automatische Erkennung",
        "all_languages" to "Alle Sprachen",
        "languages_count_suffix" to "Sprachen",
        "lang_ru" to "Russisch",
        "lang_en" to "Englisch",
        "lang_es" to "Spanisch",
        "interface_style" to "Oberflächenstil",
        "style_pixel" to "Pixel Style",
        "style_classic" to "Classic Style",
        "screen_refresh_rate" to "Bildschirmwiederholrate",
        "choose_refresh_rate_desc" to "Bildwiederholrate auf 60, 90, 120 Hz festlegen oder Automatisch wählen",
        "refresh_rate_auto" to "Automatisch",
        "choose_lang_desc" to "Wählen Sie die App-Sprache",
        "choose_theme_desc" to "Wählen Sie Ihr bevorzugtes Design",
        "choose_palette_desc" to "Wählen Sie Ihre bevorzugte Farbpalette",
        "choose_style_desc" to "Wählen Sie das Erscheinungsbild der Oberfläche",
        "security_sub_desc" to "Schützen Sie Writer's Studio mit einer 4-stelligen PIN",
        "backups_desc" to "Exportieren oder Importieren lokaler Sicherungsdateien",
        "sync_storage" to "Speicherordner-Synchronisierung",
        "sync_storage_desc" to "Exportieren und Synchronisieren von Ordnern/Dokumenten als bearbeitbare Dateien im internen Speicher",
        "sync_now" to "Jetzt synchronisieren",
        "editor_pro" to "Pro-Modus (Blöcke)",
        "editor_basic" to "Basis-Modus (Text)",
        "create_item" to "Neuen Eintrag erstellen",
        "enter_title" to "Titel eingeben",
        "create_folder_title" to "Ordner erstellen",
        "folder_name" to "Ordnername",
        "create_doc_title" to "Dokument erstellen",
        "doc_name" to "Dokumentname",
        "unnamed" to "Ohne Namen",
        "delete" to "Löschen",
        "edit" to "Bearbeiten",
        "empty_text" to "Text hier eingeben...",
        "stats_title" to "Schreibstatistiken",
        "stats_streak" to "Schreibserie (Tage):",
        "stats_words" to "Wörter insgesamt:",
        "stats_chars" to "Zeichen insgesamt:",
        "stats_daily" to "Tägliche Produktivität",
        "select_editor_mode" to "Editor-Typ:",
        "type_doc" to "Strukturierter Block (Pro)",
        "type_txt" to "Einfacher Text (Basic)",
        "format_bold" to "Fett",
        "format_italic" to "Kursiv",
        "format_underline" to "Unterstrichen",
        "project_docs" to "Projektdokumente",
        "project_root" to "Projektverzeichnis",
        "create_work_hint" to "Erstellen Sie einen Ordner oder ein Dokument",
        "search_filter" to "Suchen & Filtern",
        "sort_by" to "Sortieren nach: ",
        "sort_changed" to "Geänd.",
        "sort_name" to "Name",
        "sort_created" to "Erst.",
        "category" to "Kategorie: ",
        "search_placeholder" to "Name eingeben...",
        "cat_all" to "Alle",
        "cat_book" to "Buch",
        "cat_screenplay" to "Drehbuch",
        "cat_story" to "Erzählung",
        "cat_text" to "Text",
        "modified" to "Geändert",
        "restore" to "Wiederherstellen",
        "delete_permanent" to "Endgültig löschen",
        "to_trash" to "In den Papierkorb",
        "new_project_dialog_title" to "Neues literarisches Projekt",
        "project_name_label" to "Projekttitel (Buch, Drehbuch...)",
        "project_title_hint" to "Projekttitel",
        "project_name_placeholder_new" to "Projekttitel (Buch, Drehbuch...)",
        "color_indicator" to "Farbindikator",
        "access_password_optional" to "Zugangspasswort (optional)",
        "enter_password" to "Passwort eingeben",
        "password_hint_desc" to "Leer lassen, um das Projekt privat zu halten.",
        "project_type" to "Projekttyp:",
        "color_marker" to "Farbmarkierung:",
        "no_projects_in_section" to "Keine Projekte in diesem Bereich",
        "protected_project" to "Geschütztes Projekt",
        "project_locked_hint" to "Dieses Projekt ist passwortgeschützt. Bitte Passwort eingeben:",
        "password" to "Passwort",
        "enter_btn" to "Eingabe",
        "project_folder" to "Projektordner",
        "document" to "Dokument",
        "speech_live" to "Gesprochen: ~",
        "minutes_left" to " Minuten verbleibend",
        "mirror_h" to "Spiegel H",
        "mirror_v" to "Spiegel V",
        "speed" to "Geschwindigkeit",
        "prompter_theme" to "Thema:",
        "theme_dark_prompter" to "Dunkel",
        "theme_amber_prompter" to "Bernstein",
        "theme_bright_prompter" to "Hell",
        "exit" to "Beenden",
        "basic_mode_desc" to "Basis-Modus — Standard-Klartextbearbeitung in einem einzigen Eingabefeld.",
        "pro_mode_desc" to "Pro-Modus — strukturiertes Buch-/Drehbuchformat mit Szenenblöcken und Bildeinfügungen.",
        "days_short" to "Tage",
        "words_suffix" to "Wörter",
        "chars_suffix" to "Zeichen",
        "productivity_title" to "Produktivität (Letzte Tage):",
        "earlier" to "Früher",
        "today" to "Heute",
        "backup_success" to "Sicherung erfolgreich wiederhergestellt!",
        "backup_error" to "Fehler beim Wiederherstellen",
        "read_error" to "Fehler beim Lesen der Datei",
        "pin_saved" to "PIN eingerichtet",
        "pin_removed" to "PIN entfernt",
        "export_error" to "Fehler beim Exportieren",
        "keyboard_cyrillic" to "Kyrillische Tastatur",
        "no_projects_yet" to "Noch keine Projekte",
        "create_first_project_desc" to "Erstellen Sie Ihr erstes Projekt, um mit dem Schreiben zu beginnen."
    )

    private fun build_es() = mapOf(
        "my_studio" to "Estudio de Escritor",
        "cabinet" to "Escritorio",
        "progress" to "Progreso",
        "settings" to "Ajustes",
        "options" to "Opciones",
        "new_project" to "Nuevo Proyecto",
        "search" to "Buscar",
        "active" to "Activo",
        "projects" to "Proyectos",
        "favorites" to "Favoritos",
        "archive" to "Archivo",
        "trash" to "Papelera",
        "folders" to "Carpetas",
        "documents" to "Documentos",
        "create" to "Crear",
        "cancel" to "Cancelar",
        "versions" to "Versiones",
        "versions_subtitle" to "Versiones y Copias",
        "saved_versions" to "Revisiones guardadas:",
        "save_snapshot" to "Crear instantánea",
        "save_snapshot_label" to "Etiqueta",
        "close" to "Cerrar",
        "find_replace" to "Buscar y Reemplazar",
        "prompter" to "Prompter",
        "manual_save" to "Guardar",
        "words" to "Palabras",
        "chars" to "Caract",
        "pages" to "Págs",
        "reading_time" to "Lectura",
        "min" to "min",
        "sys_settings" to "Ajustes",
        "theme" to "Tema de la aplicación",
        "theme_light" to "Claro",
        "theme_dark" to "Oscuro",
        "theme_black" to "Negro",
        "color_palette" to "Paleta de colores",
        "security" to "Seguridad y PIN de protección",
        "security_sub" to "Ingrese una contraseña de 4 dígitos para bloquear el Estudio:",
        "four_digits" to "4 dígitos",
        "set" to "Establecer",
        "disable_pin" to "Desactivar PIN",
        "backups" to "Copia de seguridad",
        "export_backup" to "Exportar copia",
        "import_backup" to "Importar copia",
        "app_language" to "Idioma de la interfaz",
        "lang_system" to "Predeterminado del sistema",
        "system_auto_detect" to "Detección automática",
        "all_languages" to "Todos los idiomas",
        "languages_count_suffix" to "idiomas",
        "lang_ru" to "Ruso",
        "lang_en" to "Inglés",
        "lang_es" to "Español",
        "interface_style" to "Estilo de interfaz",
        "style_pixel" to "Pixel Style",
        "style_classic" to "Classic Style",
        "screen_refresh_rate" to "Frecuencia de actualización",
        "choose_refresh_rate_desc" to "Forzar la frecuencia de actualización entre 60, 90, 120 Hz o Automático",
        "refresh_rate_auto" to "Automático",
        "choose_lang_desc" to "Selecciona el idioma de la aplicación",
        "choose_theme_desc" to "Elige tu tema preferido",
        "choose_palette_desc" to "Elige tu paleta de colores favorita",
        "choose_style_desc" to "Elige la apariencia de la interfaz",
        "security_sub_desc" to "Protege Writer's Studio con un PIN de 4 dígitos",
        "backups_desc" to "Exportar o importar archivos de copia de seguridad",
        "sync_storage" to "Sincronización de carpetas",
        "sync_storage_desc" to "Sincronizar carpetas y documentos como archivos editables en el almacenamiento interno",
        "sync_now" to "Sincronizar ahora",
        "editor_pro" to "Modo Pro (Bloques)",
        "editor_basic" to "Modo Básico (Texto)",
        "create_item" to "Nueva entrada",
        "enter_title" to "Ingrese título",
        "create_folder_title" to "Crear Carpeta",
        "folder_name" to "Nombre de carpeta",
        "create_doc_title" to "Crear Documento",
        "doc_name" to "Nombre del documento",
        "unnamed" to "Sin nombre",
        "delete" to "Eliminar",
        "edit" to "Editar",
        "empty_text" to "Escriba texto aquí...",
        "stats_title" to "Estadísticas del Escritor",
        "stats_streak" to "Racha de escritura (días):",
        "stats_words" to "Total de palabras:",
        "stats_chars" to "Total de caracteres:",
        "stats_daily" to "Productividad diaria",
        "select_editor_mode" to "Tipo de editor:",
        "type_doc" to "Documento Pro (Bloques)",
        "type_txt" to "Archivo de texto (Básico)",
        "format_bold" to "Negrita",
        "format_italic" to "Cursiva",
        "format_underline" to "Subrayado",
        "project_docs" to "Documentos del proyecto",
        "project_root" to "Raíz del proyecto",
        "create_work_hint" to "Cree una carpeta o documento para comenzar",
        "search_filter" to "Buscar y Filtros",
        "sort_by" to "Ordenar por: ",
        "sort_changed" to "Mod.",
        "sort_name" to "Nombre",
        "sort_created" to "Creado",
        "category" to "Categoría: ",
        "search_placeholder" to "Ingrese nombre...",
        "cat_all" to "Todo",
        "cat_book" to "Libro",
        "cat_screenplay" to "Guion",
        "cat_story" to "Relato",
        "cat_text" to "Texto",
        "modified" to "Modificado",
        "restore" to "Restaurar",
        "delete_permanent" to "Eliminar definitivamente",
        "to_trash" to "Mover a la papelera",
        "new_project_dialog_title" to "Nuevo proyecto literario",
        "project_name_label" to "Nombre del proyecto (libro, guión...)",
        "project_title_hint" to "Título del proyecto",
        "project_name_placeholder_new" to "Título del proyecto (libro, guión...)",
        "color_indicator" to "Indicador de color",
        "access_password_optional" to "Contraseña de acceso (opcional)",
        "enter_password" to "Introducir contraseña",
        "password_hint_desc" to "Déjelo vacío para mantener el proyecto privado.",
        "project_type" to "Tipo de proyecto:",
        "color_marker" to "Marcador de color:",
        "no_projects_in_section" to "No hay proyectos en esta sección",
        "protected_project" to "Proyecto protegido",
        "project_locked_hint" to "Este proyecto está protegido por contraseña. Por favor ingrese la contraseña:",
        "password" to "Contraseña",
        "enter_btn" to "Entrar",
        "project_folder" to "Carpeta del proyecto",
        "document" to "Documento",
        "speech_live" to "Discurso en vivo: ~",
        "minutes_left" to " minutos restantes",
        "mirror_h" to "Espejo H",
        "mirror_v" to "Espejo V",
        "speed" to "Velocidad",
        "prompter_theme" to "Tema:",
        "theme_dark_prompter" to "Oscuro",
        "theme_amber_prompter" to "Ámbar",
        "theme_bright_prompter" to "Claro",
        "exit" to "Salir",
        "basic_mode_desc" to "Modo Básico — edición estándar de texto plano.",
        "pro_mode_desc" to "Modo Pro — formato estructurado con etiquetas y campos por bloques.",
        "days_short" to "días",
        "words_suffix" to "palabras",
        "chars_suffix" to "caract.",
        "productivity_title" to "Productividad (Últimos días):",
        "earlier" to "Antes",
        "today" to "Hoy",
        "backup_success" to "¡Copia de seguridad restaurada con éxito!",
        "backup_error" to "Error al restaurar copia",
        "read_error" to "Error al leer el archivo",
        "pin_saved" to "Contraseña establecida",
        "pin_removed" to "Contraseña eliminada",
        "export_error" to "Error al guardar copia de seguridad",
        "keyboard_cyrillic" to "Entrada de cirílico en pantalla",
        "no_projects_yet" to "No hay proyectos aún",
        "create_first_project_desc" to "Crea tu primer proyecto para empezar a escribir y dar vida a tus ideas."
    )

    private fun build_fr() = mapOf(
        "my_studio" to "Mon Atelier d'Écriture",
        "cabinet" to "Bureau",
        "progress" to "Progrès",
        "settings" to "Paramètres",
        "options" to "Options",
        "new_project" to "Nouveau projet",
        "search" to "Rechercher",
        "active" to "Actifs",
        "projects" to "Projets",
        "favorites" to "Favoris",
        "archive" to "Archives",
        "trash" to "Corbeille",
        "folders" to "Dossiers",
        "documents" to "Documents",
        "create" to "Créer",
        "cancel" to "Annuler",
        "versions" to "Versions",
        "versions_subtitle" to "Versions & Instantanés",
        "saved_versions" to "Révisions enregistrées :",
        "save_snapshot" to "Créer un instantané",
        "save_snapshot_label" to "Nom de l'instantané",
        "close" to "Fermer",
        "find_replace" to "Rechercher & Remplacer",
        "prompter" to "Téléprompteur",
        "manual_save" to "Enregistrer",
        "words" to "Mots",
        "chars" to "Caract.",
        "pages" to "Pages",
        "reading_time" to "Lecture",
        "min" to "min",
        "sys_settings" to "Paramètres",
        "theme" to "Thème",
        "theme_light" to "Clair",
        "theme_dark" to "Sombre",
        "theme_black" to "Noir (OLED)",
        "color_palette" to "Palette de couleurs",
        "security" to "Sécurité et code PIN",
        "security_sub" to "Entrez un code PIN à 4 chiffres :",
        "four_digits" to "4 chiffres",
        "set" to "Définir",
        "disable_pin" to "Désactiver le code PIN",
        "backups" to "Sauvegarde et restauration",
        "export_backup" to "Exporter la sauvegarde",
        "import_backup" to "Importer une sauvegarde",
        "app_language" to "Langue",
        "lang_system" to "Système par défaut",
        "system_auto_detect" to "Détection automatique",
        "all_languages" to "Toutes les langues",
        "languages_count_suffix" to "langues",
        "interface_style" to "Style d'interface",
        "style_pixel" to "Pixel Style",
        "style_classic" to "Classic Style",
        "screen_refresh_rate" to "Taux de rafraîchissement",
        "choose_refresh_rate_desc" to "Régler le taux à 60, 90, 120 Hz ou choisir Automatique",
        "refresh_rate_auto" to "Automatique",
        "choose_lang_desc" to "Choisissez la langue de l'application",
        "choose_theme_desc" to "Choisissez votre thème préféré",
        "choose_palette_desc" to "Choisissez votre palette de couleurs préférée",
        "choose_style_desc" to "Choisissez l'apparence de l'interface",
        "security_sub_desc" to "Protégez Writer's Studio avec un code PIN à 4 chiffres",
        "backups_desc" to "Exporter ou importer des fichiers de sauvegarde",
        "sync_storage" to "Synchronisation du stockage",
        "sync_storage_desc" to "Synchroniser les fichiers de projet dans la mémoire interne",
        "sync_now" to "Synchroniser maintenant",
        "editor_pro" to "Mode Pro (Blocs)",
        "editor_basic" to "Mode Basique (Texte)",
        "create_item" to "Créer une entrée",
        "enter_title" to "Entrez un titre",
        "create_folder_title" to "Créer un dossier",
        "folder_name" to "Nom du dossier",
        "create_doc_title" to "Créer un document",
        "doc_name" to "Nom du document",
        "unnamed" to "Sans titre",
        "delete" to "Supprimer",
        "edit" to "Modifier",
        "empty_text" to "Tapez votre texte ici...",
        "stats_title" to "Statistiques de l'écrivain",
        "stats_streak" to "Série d'écriture (jours) :",
        "stats_words" to "Total des mots écrits :",
        "stats_chars" to "Total des caractères :",
        "stats_daily" to "Productivité quotidienne",
        "cat_all" to "Tous",
        "cat_book" to "Livre",
        "cat_screenplay" to "Scénario",
        "cat_story" to "Nouvelle",
        "cat_text" to "Texte",
        "modified" to "Modifié",
        "restore" to "Restaurer",
        "delete_permanent" to "Supprimer définitivement",
        "to_trash" to "Mettre à la corbeille",
        "password" to "Mot de passe",
        "enter_btn" to "Entrer",
        "exit" to "Quitter",
        "days_short" to "j.",
        "words_suffix" to "mots",
        "chars_suffix" to "car.",
        "no_projects_yet" to "Aucun projet pour le moment",
        "create_first_project_desc" to "Créez votre premier projet pour commencer à écrire."
    )

    private fun build_it() = mapOf(
        "my_studio" to "Il Mio Studio di Scrittura",
        "cabinet" to "Scrivania",
        "progress" to "Progressi",
        "settings" to "Impostazioni",
        "options" to "Opzioni",
        "new_project" to "Nuovo progetto",
        "search" to "Cerca",
        "active" to "Attivi",
        "projects" to "Progetti",
        "favorites" to "Preferiti",
        "archive" to "Archivio",
        "trash" to "Cestino",
        "folders" to "Cartelle",
        "documents" to "Documenti",
        "create" to "Crea",
        "cancel" to "Annulla",
        "versions" to "Versioni",
        "versions_subtitle" to "Versioni & Snapshot",
        "saved_versions" to "Versioni salvate:",
        "save_snapshot" to "Crea snapshot",
        "save_snapshot_label" to "Etichetta snapshot",
        "close" to "Chiudi",
        "find_replace" to "Trova e Sostituisci",
        "prompter" to "Gobbo",
        "manual_save" to "Salva",
        "words" to "Parole",
        "chars" to "Caratt.",
        "pages" to "Pagine",
        "reading_time" to "Lettura",
        "min" to "min",
        "sys_settings" to "Impostazioni",
        "theme" to "Tema",
        "theme_light" to "Chiaro",
        "theme_dark" to "Scuro",
        "theme_black" to "Nero (OLED)",
        "color_palette" to "Tavolozza colori",
        "security" to "Sicurezza e PIN",
        "security_sub" to "Inserisci un PIN a 4 cifre:",
        "four_digits" to "4 cifre",
        "set" to "Imposta",
        "disable_pin" to "Disattiva PIN",
        "backups" to "Backup e Ripristino",
        "export_backup" to "Esporta Backup",
        "import_backup" to "Importa Backup",
        "app_language" to "Lingua",
        "lang_system" to "Predefinito di sistema",
        "system_auto_detect" to "Rilevamento automatico",
        "all_languages" to "Tutte le lingue",
        "languages_count_suffix" to "lingue",
        "interface_style" to "Stile interfaccia",
        "style_pixel" to "Pixel Style",
        "style_classic" to "Classic Style",
        "screen_refresh_rate" to "Frequenza di aggiornamento",
        "choose_refresh_rate_desc" to "Imposta frequenza su 60, 90, 120 Hz o seleziona Automatico",
        "refresh_rate_auto" to "Automatico",
        "choose_lang_desc" to "Scegli la lingua dell'applicazione",
        "choose_theme_desc" to "Seleziona il tema preferito",
        "choose_palette_desc" to "Scegli la tavolozza di colori preferita",
        "choose_style_desc" to "Scegli l'aspetto dell'interfaccia",
        "security_sub_desc" to "Proteggi Writer's Studio con un PIN a 4 cifre",
        "backups_desc" to "Esporta o importa file di backup",
        "sync_storage" to "Sincronizzazione archivio",
        "sync_storage_desc" to "Sincronizza file di progetto nella memoria interna",
        "sync_now" to "Sincronizza ora",
        "editor_pro" to "Modalità Pro (Blocchi)",
        "editor_basic" to "Modalità Base (Testo)",
        "create_item" to "Nuova voce",
        "create_folder_title" to "Crea Cartella",
        "folder_name" to "Nome cartella",
        "create_doc_title" to "Crea Documento",
        "doc_name" to "Nome documento",
        "unnamed" to "Senza nome",
        "delete" to "Elimina",
        "edit" to "Modifica",
        "empty_text" to "Digita qui il testo...",
        "stats_title" to "Statistiche",
        "stats_streak" to "Striscia di scrittura (giorni):",
        "stats_words" to "Totale parole:",
        "stats_chars" to "Totale caratteri:",
        "stats_daily" to "Produttività giornaliera",
        "cat_all" to "Tutti",
        "cat_book" to "Libro",
        "cat_screenplay" to "Sceneggiatura",
        "cat_story" to "Racconto",
        "cat_text" to "Testo",
        "modified" to "Modificato",
        "restore" to "Ripristina",
        "delete_permanent" to "Elimina per sempre",
        "to_trash" to "Cestina",
        "password" to "Password",
        "enter_btn" to "Entra",
        "exit" to "Esci",
        "days_short" to "giorni",
        "words_suffix" to "parole",
        "chars_suffix" to "caratteri",
        "no_projects_yet" to "Nessun progetto ancora",
        "create_first_project_desc" to "Crea il tuo primo progetto per iniziare a scrivere."
    )

    private fun build_pt() = mapOf(
        "my_studio" to "Meu Estúdio Criativo",
        "cabinet" to "Gabinete",
        "progress" to "Progresso",
        "settings" to "Configurações",
        "options" to "Opções",
        "new_project" to "Novo Projeto",
        "search" to "Pesquisar",
        "active" to "Ativos",
        "projects" to "Projetos",
        "favorites" to "Favoritos",
        "archive" to "Arquivo",
        "trash" to "Lixeira",
        "folders" to "Pastas",
        "documents" to "Documentos",
        "create" to "Criar",
        "cancel" to "Cancelar",
        "versions" to "Versões",
        "close" to "Fechar",
        "find_replace" to "Localizar e Substituir",
        "prompter" to "Teleprompter",
        "manual_save" to "Salvar",
        "words" to "Palavras",
        "chars" to "Caract.",
        "pages" to "Págs",
        "reading_time" to "Leitura",
        "min" to "min",
        "sys_settings" to "Configurações",
        "theme" to "Tema",
        "theme_light" to "Claro",
        "theme_dark" to "Escuro",
        "theme_black" to "Preto (OLED)",
        "color_palette" to "Paleta de cores",
        "security" to "Segurança e PIN",
        "security_sub" to "Digite um PIN de 4 dígitos:",
        "four_digits" to "4 dígitos",
        "set" to "Definir",
        "disable_pin" to "Desativar PIN",
        "backups" to "Backup e Restauração",
        "export_backup" to "Exportar Backup",
        "import_backup" to "Importar Backup",
        "app_language" to "Idioma",
        "lang_system" to "Padrão do sistema",
        "system_auto_detect" to "Detecção automática",
        "all_languages" to "Todos os idiomas",
        "languages_count_suffix" to "idiomas",
        "interface_style" to "Estilo de interface",
        "screen_refresh_rate" to "Taxa de atualização",
        "choose_refresh_rate_desc" to "Definir taxa para 60, 90, 120 Hz ou selecionar Automático",
        "refresh_rate_auto" to "Automático",
        "choose_lang_desc" to "Escolha o idioma do aplicativo",
        "choose_theme_desc" to "Escolha o seu tema preferido",
        "choose_palette_desc" to "Escolha sua paleta de cores",
        "choose_style_desc" to "Escolha a aparência da interface",
        "security_sub_desc" to "Proteja o Writer's Studio com um PIN de 4 dígitos",
        "backups_desc" to "Exportar ou importar arquivos de backup",
        "sync_storage" to "Sincronização de armazenamento",
        "sync_storage_desc" to "Sincronizar arquivos do projeto no armazenamento interno",
        "sync_now" to "Sincronizar agora",
        "editor_pro" to "Modo Pro (Blocos)",
        "editor_basic" to "Modo Básico (Texto)",
        "create_folder_title" to "Criar Pasta",
        "create_doc_title" to "Criar Documento",
        "delete" to "Excluir",
        "edit" to "Editar",
        "stats_title" to "Estatísticas do Escritor",
        "cat_all" to "Todos",
        "cat_book" to "Livro",
        "cat_screenplay" to "Roteiro",
        "cat_story" to "Conto",
        "cat_text" to "Texto",
        "modified" to "Modificado",
        "restore" to "Restaurar",
        "delete_permanent" to "Excluir permanentemente",
        "to_trash" to "Mover para lixeira",
        "password" to "Senha",
        "enter_btn" to "Entrar",
        "exit" to "Sair",
        "days_short" to "dias",
        "words_suffix" to "palavras",
        "chars_suffix" to "caracteres",
        "no_projects_yet" to "Nenhum projeto ainda",
        "create_first_project_desc" to "Crie seu primeiro projeto para começar a escrever."
    )

    private fun build_zh() = mapOf(
        "my_studio" to "我的创作工作室",
        "cabinet" to "工作台",
        "progress" to "写作进度",
        "settings" to "设置",
        "options" to "选项",
        "new_project" to "新建项目",
        "search" to "搜索",
        "active" to "进行中",
        "projects" to "项目列表",
        "favorites" to "收藏夹",
        "archive" to "归档",
        "trash" to "回收站",
        "folders" to "文件夹",
        "documents" to "文档",
        "create" to "创建",
        "cancel" to "取消",
        "versions" to "版本历史",
        "close" to "关闭",
        "find_replace" to "查找与替换",
        "prompter" to "提词器",
        "manual_save" to "保存",
        "words" to "字数",
        "chars" to "字符",
        "pages" to "页数",
        "reading_time" to "阅读用时",
        "min" to "分钟",
        "sys_settings" to "系统设置",
        "theme" to "外观主题",
        "theme_light" to "浅色",
        "theme_dark" to "深色",
        "theme_black" to "纯黑 (OLED)",
        "color_palette" to "配色方案",
        "security" to "安全与PIN码",
        "security_sub" to "输入4位数字PIN码：",
        "four_digits" to "4位数字",
        "set" to "设置",
        "disable_pin" to "关闭PIN码",
        "backups" to "备份与恢复",
        "export_backup" to "导出备份",
        "import_backup" to "导入备份",
        "app_language" to "界面语言",
        "lang_system" to "系统默认",
        "system_auto_detect" to "自动识别",
        "all_languages" to "所有语言",
        "languages_count_suffix" to "种语言",
        "interface_style" to "界面样式",
        "screen_refresh_rate" to "屏幕刷新率",
        "choose_refresh_rate_desc" to "设置屏幕刷新率为 60、90、120 Hz 或选择自动模式",
        "refresh_rate_auto" to "自动",
        "choose_lang_desc" to "选择应用界面语言",
        "choose_theme_desc" to "选择您喜欢的主题",
        "choose_palette_desc" to "选择您喜爱的色彩风格",
        "choose_style_desc" to "选择应用程序外观风格",
        "security_sub_desc" to "使用4位PIN码保护 Writer's Studio",
        "backups_desc" to "导出或导入本地备份文件",
        "sync_storage" to "存储目录同步",
        "sync_storage_desc" to "将项目文件同步到手机内部存储中",
        "sync_now" to "立即同步",
        "editor_pro" to "专业模式 (分块编排)",
        "editor_basic" to "基础模式 (纯文本)",
        "create_folder_title" to "新建文件夹",
        "create_doc_title" to "新建文档",
        "delete" to "删除",
        "edit" to "编辑",
        "empty_text" to "在此输入文本...",
        "stats_title" to "写作统计",
        "cat_all" to "全部",
        "cat_book" to "书籍",
        "cat_screenplay" to "剧本",
        "cat_story" to "故事",
        "cat_text" to "文本",
        "modified" to "修改时间",
        "restore" to "恢复",
        "delete_permanent" to "彻底删除",
        "to_trash" to "移入回收站",
        "password" to "密码",
        "enter_btn" to "进入",
        "exit" to "退出",
        "days_short" to "天",
        "words_suffix" to "字",
        "chars_suffix" to "字符",
        "no_projects_yet" to "暂无项目",
        "create_first_project_desc" to "创建你的第一个项目，开启写作之旅。"
    )

    private fun build_ja() = mapOf(
        "my_studio" to "マイ・クリエイティブスタジオ",
        "cabinet" to "キャビネット",
        "progress" to "執筆進捗",
        "settings" to "設定",
        "options" to "オプション",
        "new_project" to "新規プロジェクト",
        "search" to "検索",
        "active" to "アクティブ",
        "projects" to "プロジェクト",
        "favorites" to "お気に入り",
        "archive" to "アーカイブ",
        "trash" to "ゴミ箱",
        "folders" to "フォルダ",
        "documents" to "ドキュメント",
        "create" to "作成",
        "cancel" to "キャンセル",
        "versions" to "バージョン履歴",
        "close" to "閉じる",
        "find_replace" to "検索と置換",
        "prompter" to "プロンプター",
        "manual_save" to "保存",
        "words" to "単語数",
        "chars" to "文字数",
        "pages" to "ページ",
        "reading_time" to "読了目安",
        "min" to "分",
        "sys_settings" to "設定",
        "theme" to "テーマ",
        "theme_light" to "ライト",
        "theme_dark" to "ダーク",
        "theme_black" to "ブラック (OLED)",
        "color_palette" to "カラーパレット",
        "security" to "セキュリティとPINコード",
        "security_sub" to "4桁のPINを入力してください:",
        "four_digits" to "4桁",
        "set" to "設定",
        "disable_pin" to "PINを解除",
        "backups" to "バックアップと復元",
        "export_backup" to "バックアップをエクスポート",
        "import_backup" to "バックアップをインポート",
        "app_language" to "言語",
        "lang_system" to "システム標準",
        "system_auto_detect" to "自動判別",
        "all_languages" to "すべての言語",
        "languages_count_suffix" to "言語",
        "interface_style" to "UIスタイル",
        "screen_refresh_rate" to "リフレッシュレート",
        "choose_refresh_rate_desc" to "60、90、120 Hzまたは自動を選択",
        "refresh_rate_auto" to "自動",
        "choose_lang_desc" to "アプリの表示言語を選択",
        "choose_theme_desc" to "好みのテーマを選択してください",
        "choose_palette_desc" to "お気に入りのカラーパレットを選択",
        "choose_style_desc" to "インターフェースの外観を選択",
        "security_sub_desc" to "4桁のPINで Writer's Studio を保護",
        "backups_desc" to "バックアップファイルのエクスポート/インポート",
        "sync_storage" to "ストレージ同期",
        "sync_storage_desc" to "プロジェクトファイルを内部ストレージに同期",
        "sync_now" to "今すぐ同期",
        "editor_pro" to "Proモード (ブロック)",
        "editor_basic" to "基本モード (テキスト)",
        "create_folder_title" to "フォルダ作成",
        "create_doc_title" to "ドキュメント作成",
        "delete" to "削除",
        "edit" to "編集",
        "empty_text" to "ここにテキストを入力...",
        "stats_title" to "執筆統計",
        "cat_all" to "すべて",
        "cat_book" to "書籍",
        "cat_screenplay" to "脚本",
        "cat_story" to "短編小説",
        "cat_text" to "テキスト",
        "modified" to "更新日時",
        "restore" to "復元",
        "delete_permanent" to "完全に削除",
        "to_trash" to "ゴミ箱に移動",
        "password" to "パスワード",
        "enter_btn" to "入る",
        "exit" to "終了",
        "days_short" to "日",
        "words_suffix" to "語",
        "chars_suffix" to "文字",
        "no_projects_yet" to "プロジェクトがありません",
        "create_first_project_desc" to "最初のプロジェクトを作成して執筆を始めましょう。"
    )

    private fun build_ko() = mapOf(
        "my_studio" to "나의 창작 스튜디오",
        "cabinet" to "작업실",
        "progress" to "진행 상황",
        "settings" to "설정",
        "options" to "옵션",
        "new_project" to "새 프로젝트",
        "search" to "검색",
        "active" to "진행 중",
        "projects" to "프로젝트",
        "favorites" to "즐겨찾기",
        "archive" to "보관함",
        "trash" to "휴지통",
        "folders" to "폴더",
        "documents" to "문서",
        "create" to "생성",
        "cancel" to "취소",
        "versions" to "버전 기록",
        "close" to "닫기",
        "find_replace" to "찾기 및 바꾸기",
        "prompter" to "프롬프터",
        "manual_save" to "저장",
        "words" to "단어",
        "chars" to "글자 수",
        "pages" to "페이지",
        "reading_time" to "읽기 시간",
        "min" to "분",
        "sys_settings" to "설정",
        "theme" to "테마",
        "theme_light" to "라이트",
        "theme_dark" to "다크",
        "theme_black" to "블랙 (OLED)",
        "color_palette" to "색상 팔레트",
        "security" to "보안 및 PIN 번호",
        "security_sub" to "4자리 비밀번호를 입력하세요:",
        "four_digits" to "4자리",
        "set" to "설정",
        "disable_pin" to "PIN 해제",
        "backups" to "백업 및 복원",
        "export_backup" to "백업 내보내기",
        "import_backup" to "백업 가져오기",
        "app_language" to "언어",
        "lang_system" to "시스템 기본값",
        "system_auto_detect" to "자동 감지",
        "all_languages" to "모든 언어",
        "languages_count_suffix" to "개 언어",
        "interface_style" to "인터페이스 스타일",
        "screen_refresh_rate" to "화면 재생률",
        "choose_refresh_rate_desc" to "60, 90, 120Hz 또는 자동 설정",
        "refresh_rate_auto" to "자동",
        "choose_lang_desc" to "앱 인터페이스 언어 선택",
        "choose_theme_desc" to "원하는 테마를 선택하세요",
        "choose_palette_desc" to "마음에 드는 색상 팔레트를 선택하세요",
        "choose_style_desc" to "인터페이스 스타일을 선택하세요",
        "security_sub_desc" to "4자리 PIN으로 Writer's Studio 보호",
        "backups_desc" to "로컬 백업 파일 내보내기 또는 가져오기",
        "sync_storage" to "저장소 디렉터리 동기화",
        "sync_storage_desc" to "프로젝트 파일을 내부 저장소에 동기화",
        "sync_now" to "지금 동기화",
        "editor_pro" to "프로 모드 (블록)",
        "editor_basic" to "기본 모드 (텍스트)",
        "create_folder_title" to "폴더 생성",
        "create_doc_title" to "문서 생성",
        "delete" to "삭제",
        "edit" to "편집",
        "stats_title" to "집필 통계",
        "cat_all" to "전체",
        "cat_book" to "도서",
        "cat_screenplay" to "시나리오",
        "cat_story" to "단편소설",
        "cat_text" to "텍스트",
        "modified" to "수정됨",
        "restore" to "복원",
        "delete_permanent" to "영구 삭제",
        "to_trash" to "휴지통으로 이동",
        "password" to "비밀번호",
        "enter_btn" to "입력",
        "exit" to "나가기",
        "days_short" to "일",
        "words_suffix" to "단어",
        "chars_suffix" to "자",
        "no_projects_yet" to "프로젝트가 없습니다",
        "create_first_project_desc" to "첫 번째 프로젝트를 만들어 창작을 시작해 보세요."
    )

    private fun build_uk() = mapOf(
        "my_studio" to "Моя Творча Студія",
        "cabinet" to "Кабінет",
        "progress" to "Успіхи",
        "settings" to "Налаштування",
        "options" to "Опції",
        "new_project" to "Новий проєкт",
        "search" to "Пошук",
        "active" to "Активні",
        "projects" to "Проєкти",
        "favorites" to "Обране",
        "archive" to "Архів",
        "trash" to "Кошик",
        "folders" to "Папки",
        "documents" to "Документи",
        "create" to "Створити",
        "cancel" to "Скасувати",
        "versions" to "Версії",
        "close" to "Закрити",
        "find_replace" to "Знайти та Замінити",
        "prompter" to "Суфлер",
        "manual_save" to "Зберегти",
        "words" to "Слів",
        "chars" to "Симв.",
        "pages" to "Стор.",
        "reading_time" to "Читання",
        "min" to "хв",
        "sys_settings" to "Налаштування",
        "theme" to "Тема",
        "theme_light" to "Світла",
        "theme_dark" to "Темна",
        "theme_black" to "Чорна (OLED)",
        "color_palette" to "Палітра кольорів",
        "security" to "Безпека та PIN",
        "security_sub" to "Введіть 4-значний PIN-код:",
        "four_digits" to "4 цифри",
        "set" to "Задати",
        "disable_pin" to "Вимкнути PIN",
        "backups" to "Резервне копіювання",
        "export_backup" to "Експортувати бекап",
        "import_backup" to "Імпортувати бекап",
        "app_language" to "Мова інтерфейсу",
        "lang_system" to "Системна",
        "system_auto_detect" to "Автовизначення",
        "all_languages" to "Всі мови",
        "languages_count_suffix" to "мов",
        "interface_style" to "Стиль інтерфейсу",
        "screen_refresh_rate" to "Частота оновлення",
        "choose_refresh_rate_desc" to "Встановити частоту 60, 90, 120 Гц або вибрати Автоматично",
        "refresh_rate_auto" to "Автоматично",
        "choose_lang_desc" to "Оберіть мову додатку",
        "choose_theme_desc" to "Оберіть бажану тему",
        "choose_palette_desc" to "Оберіть вашу улюблену колірну гаму",
        "choose_style_desc" to "Оберіть оформлення додатку",
        "security_sub_desc" to "Захистіть Writer's Studio за допомогою 4-значного PIN-коду",
        "backups_desc" to "Експорт або імпорт локальних файлів резервних копій",
        "sync_storage" to "Синхронізація папок з пам'яттю",
        "sync_storage_desc" to "Синхронізувати всі папки та розділи у внутрішню пам'ять",
        "sync_now" to "Синхронізувати зараз",
        "editor_pro" to "Режим Про (Блоки)",
        "editor_basic" to "Базовий режим (Текст)",
        "create_folder_title" to "Створити Папку",
        "create_doc_title" to "Створити Документ",
        "delete" to "Видалити",
        "edit" to "Редагувати",
        "stats_title" to "Статистика",
        "cat_all" to "Всі",
        "cat_book" to "Книга",
        "cat_screenplay" to "Сценарій",
        "cat_story" to "Оповідання",
        "cat_text" to "Текст",
        "modified" to "Змінено",
        "restore" to "Відновити",
        "delete_permanent" to "Видалити назавжди",
        "to_trash" to "У кошик",
        "password" to "Пароль",
        "enter_btn" to "Увійти",
        "exit" to "Вийти",
        "days_short" to "дн.",
        "words_suffix" to "слів",
        "chars_suffix" to "знаків",
        "no_projects_yet" to "Проєктів поки немає",
        "create_first_project_desc" to "Створіть свій перший проєкт, щоб розпочати писати.",
        "scene" to "Заголовок сцени",
        "action" to "Опис дії",
        "character" to "Ім'я персонажа",
        "dialogue" to "Діалог",
        "custom" to "Власний блок",
        "add_block" to "Додати блок",
        "choose_lang_title" to "Оберіть мову",
        "search_language" to "Пошук мови...",
        "save_template" to "Зберегти шаблон",
        "saved_templates" to "Збережені шаблони",
        "palette_studio" to "Конструктор палітри",
        "customize_palette_desc" to "Налаштуйте акцентний колір, фон і стиль",
        "primary_accent" to "Основний акцент",
        "background_canvas" to "Фон і полотно",
        "presets_gallery" to "Готові теми",
        "saved_palettes" to "Мої палітри",
        "apply_palette" to "Застосувати",
        "palette_applied" to "Палітру успішно застосовано!",
        "save_as_new" to "Зберегти тему",
        "enter_palette_name" to "Назва теми",
        "reset_palette" to "Скинути тему",
        "hue" to "Колірний тон",
        "saturation" to "Насиченість",
        "brightness" to "Яскравість",
        "hex_code" to "HEX-код кольору",
        "quick_swatches" to "Швидкі відтінки",
        "custom_bg_option" to "Власний колір фону",
        "sample_book_title" to "Таємниця старого маяка",
        "sample_chapter" to "Розділ 3 • 4 820 слів",
        "sample_btn_action" to "Продовжити писати",
        "create_proj_title" to "Новий проєкт",
        "proj_name_hint" to "Назва проєкту (книга, сценарій...)",
        "select_format" to "Формат твору",
        "cover_accent_color" to "Колірний маркер обкладинки",
        "bottom_bar_style" to "Стиль нижньої панелі",
        "choose_bottom_bar_desc" to "Оберіть оформлення нижньої панелі навігації",
        "bar_standard" to "Звичайна",
        "bar_capsule" to "Капсула",
        "bar_segmented" to "Роздільні капсули",
        "today" to "Сьогодні",
        "earlier" to "Раніше"
    )

    private fun build_pl() = mapOf(
        "my_studio" to "Moje Studio Pisarza",
        "cabinet" to "Biurko",
        "progress" to "Postępy",
        "settings" to "Ustawienia",
        "options" to "Opcje",
        "new_project" to "Nowy projekt",
        "search" to "Szukaj",
        "active" to "Aktywne",
        "projects" to "Projekty",
        "favorites" to "Ulubione",
        "archive" to "Archiwum",
        "trash" to "Kosz",
        "folders" to "Foldery",
        "documents" to "Dokumenty",
        "create" to "Utwórz",
        "cancel" to "Anuluj",
        "versions" to "Wersje",
        "close" to "Zamknij",
        "find_replace" to "Znajdź i Zamień",
        "prompter" to "Prompter",
        "manual_save" to "Zapisz",
        "words" to "Słów",
        "chars" to "Znaków",
        "pages" to "Stron",
        "reading_time" to "Czas czytania",
        "min" to "min",
        "sys_settings" to "Ustawienia",
        "theme" to "Motyw",
        "theme_light" to "Jasny",
        "theme_dark" to "Ciemny",
        "theme_black" to "Czarny (OLED)",
        "color_palette" to "Paleta kolorów",
        "security" to "Bezpieczeństwo i PIN",
        "security_sub" to "Wprowadź 4-cyfrowy kod PIN:",
        "four_digits" to "4 cyfry",
        "set" to "Ustaw",
        "disable_pin" to "Wyłącz PIN",
        "backups" to "Kopia zapasowa i przywracanie",
        "export_backup" to "Eksportuj kopię",
        "import_backup" to "Importuj kopię",
        "app_language" to "Język",
        "lang_system" to "Domyślny systemu",
        "system_auto_detect" to "Automatyczne wykrywanie",
        "all_languages" to "Wszystkie języki",
        "languages_count_suffix" to "języków",
        "interface_style" to "Styl interfejsu",
        "screen_refresh_rate" to "Częstotliwość odświeżania",
        "choose_refresh_rate_desc" to "Ustaw odświeżanie na 60, 90, 120 Hz lub wybierz Automatycznie",
        "refresh_rate_auto" to "Automatycznie",
        "choose_lang_desc" to "Wybierz język aplikacji",
        "choose_theme_desc" to "Wybierz preferowany motyw",
        "choose_palette_desc" to "Wybierz ulubioną paletę kolorów",
        "choose_style_desc" to "Wybierz wygląd interfejsu",
        "security_sub_desc" to "Zabezpiecz Writer's Studio 4-cyfrowym kodem PIN",
        "backups_desc" to "Eksportuj lub importuj lokalne pliki kopii zapasowych",
        "sync_storage" to "Synchronizacja pamięci",
        "sync_storage_desc" to "Synchronizuj pliki projektu w pamięci wewnętrznej",
        "sync_now" to "Synchronizuj teraz",
        "editor_pro" to "Tryb Pro (Bloki)",
        "editor_basic" to "Tryb Podstawowy (Tekst)",
        "create_folder_title" to "Utwórz folder",
        "create_doc_title" to "Utwórz dokument",
        "delete" to "Usuń",
        "edit" to "Edytuj",
        "stats_title" to "Statystyki pisarza",
        "cat_all" to "Wszystkie",
        "cat_book" to "Książka",
        "cat_screenplay" to "Scenariusz",
        "cat_story" to "Opowiadanie",
        "cat_text" to "Tekst",
        "modified" to "Zmodyfikowano",
        "restore" to "Przywróć",
        "delete_permanent" to "Usuń trwale",
        "to_trash" to "Do kosza",
        "password" to "Hasło",
        "enter_btn" to "Wejdź",
        "exit" to "Wyjdź",
        "days_short" to "dni",
        "words_suffix" to "słów",
        "chars_suffix" to "znaków",
        "no_projects_yet" to "Brak projektów",
        "create_first_project_desc" to "Utwórz swój pierwszy projekt, aby zacząć pisać."
    )

    private fun build_tr() = mapOf(
        "my_studio" to "Yaratıcı Yazarlık Stüdyosu",
        "cabinet" to "Çalışma Masası",
        "progress" to "İlerleme",
        "settings" to "Ayarlar",
        "options" to "Seçenekler",
        "new_project" to "Yeni Proje",
        "search" to "Ara",
        "active" to "Aktif",
        "projects" to "Projeler",
        "favorites" to "Favoriler",
        "archive" to "Arşiv",
        "trash" to "Çöp Kutusu",
        "folders" to "Klasörler",
        "documents" to "Belgeler",
        "create" to "Oluştur",
        "cancel" to "İptal",
        "versions" to "Sürümler",
        "close" to "Kapat",
        "find_replace" to "Bul ve Değiştir",
        "prompter" to "Prompter",
        "manual_save" to "Kaydet",
        "words" to "Kelime",
        "chars" to "Karakter",
        "pages" to "Sayfa",
        "reading_time" to "Okuma Süresi",
        "min" to "dk",
        "sys_settings" to "Ayarlar",
        "theme" to "Tema",
        "theme_light" to "Açık",
        "theme_dark" to "Koyu",
        "theme_black" to "Siyah (OLED)",
        "color_palette" to "Renk Paleti",
        "security" to "Güvenlik ve PIN",
        "security_sub" to "4 haneli PIN girin:",
        "four_digits" to "4 hane",
        "set" to "Ayarla",
        "disable_pin" to "PIN'i Kaldır",
        "backups" to "Yedekleme ve Geri Yükleme",
        "export_backup" to "Yedeği Dışa Aktar",
        "import_backup" to "Yedeği İçe Aktar",
        "app_language" to "Dil",
        "lang_system" to "Sistem Varsayılanı",
        "system_auto_detect" to "Otomatik Algılama",
        "all_languages" to "Tüm Diller",
        "languages_count_suffix" to "dil",
        "interface_style" to "Arayüz Stili",
        "screen_refresh_rate" to "Ekran Yenileme Hızı",
        "choose_refresh_rate_desc" to "Yenileme hızını 60, 90, 120 Hz veya Otomatik seçin",
        "refresh_rate_auto" to "Otomatik",
        "choose_lang_desc" to "Uygulama dilini seçin",
        "choose_theme_desc" to "Tercih ettiğiniz temayı seçin",
        "choose_palette_desc" to "Favori renk paletinizi seçin",
        "choose_style_desc" to "Arayüz görünümünü seçin",
        "security_sub_desc" to "Writer's Studio'yu 4 haneli PIN ile koruyun",
        "backups_desc" to "Yedek dosyalarını dışa veya içe aktarın",
        "sync_storage" to "Depolama Dizini Senkronizasyonu",
        "sync_storage_desc" to "Proje dosyalarını dahili depolama ile senkronize edin",
        "sync_now" to "Şimdi Senkronize Et",
        "editor_pro" to "Pro Mod (Blok)",
        "editor_basic" to "Temel Mod (Metin)",
        "create_folder_title" to "Klasör Oluştur",
        "create_doc_title" to "Belge Oluştur",
        "delete" to "Sil",
        "edit" to "Düzenle",
        "stats_title" to "Yazar İstatistikleri",
        "cat_all" to "Tümü",
        "cat_book" to "Kitap",
        "cat_screenplay" to "Senaryo",
        "cat_story" to "Hikaye",
        "cat_text" to "Metin",
        "modified" to "Düzenlendi",
        "restore" to "Geri Yükle",
        "delete_permanent" to "Kalıcı Olarak Sil",
        "to_trash" to "Çöp Kutusuna Gönder",
        "password" to "Şifre",
        "enter_btn" to "Giriş",
        "exit" to "Çıkış",
        "days_short" to "gün",
        "words_suffix" to "kelime",
        "chars_suffix" to "karakter",
        "no_projects_yet" to "Henüz proje yok",
        "create_first_project_desc" to "Yazmaya başlamak için ilk projenizi oluşturun."
    )

    private fun build_ar() = mapOf(
        "my_studio" to "استوديو الكاتب الإبداعي",
        "cabinet" to "المكتب",
        "progress" to "الإنجازات",
        "settings" to "الإعدادات",
        "options" to "خيارات",
        "new_project" to "مشروع جديد",
        "search" to "بحث",
        "active" to "نشط",
        "projects" to "المشاريع",
        "favorites" to "المفضلة",
        "archive" to "الأرشيف",
        "trash" to "سلة المهملات",
        "folders" to "مجلدات",
        "documents" to "مستندات",
        "create" to "إنشاء",
        "cancel" to "إلغاء",
        "versions" to "الإصدارات",
        "close" to "إغلاق",
        "find_replace" to "بحث واستبدال",
        "prompter" to "المُلقّن",
        "manual_save" to "حفظ",
        "words" to "كلمة",
        "chars" to "حرف",
        "pages" to "صفحة",
        "reading_time" to "وقت القراءة",
        "min" to "دقيقة",
        "sys_settings" to "الإعدادات",
        "theme" to "المظهر",
        "theme_light" to "فاتح",
        "theme_dark" to "داكن",
        "theme_black" to "أسود (OLED)",
        "color_palette" to "لوحة الألوان",
        "security" to "الأمان ورقم التعريف",
        "security_sub" to "أدخل رمز PIN المكون من 4 أرقام:",
        "four_digits" to "4 أرقام",
        "set" to "تعيين",
        "disable_pin" to "إلغاء القفل",
        "backups" to "النسخ الاحتياطي والاستعادة",
        "export_backup" to "تصدير نسخة احتياطية",
        "import_backup" to "استيراد نسخة احتياطية",
        "app_language" to "اللغة",
        "lang_system" to "النظام الافتراضي",
        "system_auto_detect" to "اكتشاف تلقائي",
        "all_languages" to "جميع اللغات",
        "languages_count_suffix" to "لغة",
        "interface_style" to "نمط الواجهة",
        "screen_refresh_rate" to "معدل تحديث الشاشة",
        "choose_refresh_rate_desc" to "اضبط معدل التحديث على 60 أو 90 أو 120 هرتز أو اختر تلقائي",
        "refresh_rate_auto" to "تلقائي",
        "choose_lang_desc" to "اختر لغة التطبيق",
        "choose_theme_desc" to "اختر المظهر المفضل لديك",
        "choose_palette_desc" to "اختر لوحة الألوان المفضلة لديك",
        "choose_style_desc" to "اختر مظهر الواجهة",
        "security_sub_desc" to "قم بحماية تطبيق الاستوديو برمز PIN مكون من 4 أرقام",
        "backups_desc" to "تصدير أو استيراد ملفات النسخ الاحتياطي",
        "sync_storage" to "مزامنة دليل التخزين",
        "sync_storage_desc" to "مزامنة ملفات المشروع في الذاكرة الداخلية",
        "sync_now" to "مزامنة الآن",
        "editor_pro" to "وضع المحترف (كتل)",
        "editor_basic" to "الوضع الأساسي (نص عادي)",
        "create_folder_title" to "إنشاء مجلد",
        "create_doc_title" to "إنشاء مستند",
        "delete" to "حذف",
        "edit" to "تعديل",
        "stats_title" to "إحصائيات الكاتب",
        "cat_all" to "الكل",
        "cat_book" to "كتاب",
        "cat_screenplay" to "سيناريو",
        "cat_story" to "قصة",
        "cat_text" to "نص",
        "modified" to "مُعدّل",
        "restore" to "استعادة",
        "delete_permanent" to "حذف نهائي",
        "to_trash" to "إلى المهملات",
        "password" to "كلمة المرور",
        "enter_btn" to "دخول",
        "exit" to "خروج",
        "days_short" to "أيام",
        "words_suffix" to "كلمة",
        "chars_suffix" to "حرف",
        "no_projects_yet" to "لا توجد مشاريع حتى الآن",
        "create_first_project_desc" to "أنشئ مشروعك الأول لبدء الكتابة."
    )

    private fun build_hi() = mapOf(
        "my_studio" to "मेरा रचनात्मक स्टूडियो",
        "cabinet" to "कार्यक्षेत्र",
        "progress" to "प्रगति",
        "settings" to "सेटिंग्स",
        "options" to "विकल्प",
        "new_project" to "नया प्रोजेक्ट",
        "search" to "खोजें",
        "active" to "सक्रिय",
        "projects" to "प्रोजेक्ट्स",
        "favorites" to "पसंदीदा",
        "archive" to "संग्रह",
        "trash" to "कचरा पेटी",
        "folders" to "फ़ोल्डर",
        "documents" to "दस्तावेज़",
        "create" to "बनाएं",
        "cancel" to "रद्द करें",
        "versions" to "संस्करण",
        "close" to "बंद करें",
        "find_replace" to "ढूंढें और बदलें",
        "prompter" to "प्रॉम्प्टर",
        "manual_save" to "सहेजें",
        "words" to "शब्द",
        "chars" to "अक्षर",
        "pages" to "पृष्ठ",
        "reading_time" to "पढ़ने का समय",
        "min" to "मिनट",
        "sys_settings" to "सेटिंग्स",
        "theme" to "थीम",
        "theme_light" to "हल्का",
        "theme_dark" to "गहरा",
        "theme_black" to "ब्लैक (OLED)",
        "color_palette" to "रंग पैलेट",
        "security" to "सुरक्षा और पिन",
        "security_sub" to "4-अंकीय पिन दर्ज करें:",
        "four_digits" to "4 अंक",
        "set" to "सेट करें",
        "disable_pin" to "पिन हटाएं",
        "backups" to "बैकअप और पुनर्स्थापना",
        "export_backup" to "बैकअप निर्यात करें",
        "import_backup" to "बैकअप आयात करें",
        "app_language" to "भाषा",
        "lang_system" to "सिस्टम डिफ़ॉल्ट",
        "system_auto_detect" to "स्वतः पहचान",
        "all_languages" to "सभी भाषाएँ",
        "languages_count_suffix" to "भाषाएं",
        "interface_style" to "इंटरफ़ेस शैली",
        "screen_refresh_rate" to "स्क्रीन ताज़ा दर",
        "choose_refresh_rate_desc" to "रिफ्रेश दर 60, 90, 120 Hz पर सेट करें या स्वचालित चुनें",
        "refresh_rate_auto" to "स्वचालित",
        "choose_lang_desc" to "ऐप की भाषा चुनें",
        "choose_theme_desc" to "अपनी पसंदीदा थीम चुनें",
        "choose_palette_desc" to "अपना पसंदीदा रंग पैलेट चुनें",
        "choose_style_desc" to "इंटरफ़ेस का रूप चुनें",
        "security_sub_desc" to "4-अंकीय पिन से Writer's Studio को सुरक्षित करें",
        "backups_desc" to "बैकअप फ़ाइलें निर्यात या आयात करें",
        "sync_storage" to "स्टोरेज डायरेक्टरी सिंक्रोनाइज़ेशन",
        "sync_storage_desc" to "प्रोजेक्ट फ़ाइलों को आंतरिक स्टोरेज में सिंक्रनाइज़ करें",
        "sync_now" to "अभी सिंक्रनाइज़ करें",
        "editor_pro" to "प्रो मोड (ब्लॉक)",
        "editor_basic" to "बेसिक मोड (सादा टेक्स्ट)",
        "create_folder_title" to "फ़ोल्डर बनाएं",
        "create_doc_title" to "दस्तावेज़ बनाएं",
        "delete" to "हटाएं",
        "edit" to "संपादित करें",
        "stats_title" to "लेखक के आँकड़े",
        "cat_all" to "सभी",
        "cat_book" to "किताब",
        "cat_screenplay" to "पटकथा",
        "cat_story" to "कहानी",
        "cat_text" to "टेक्स्ट",
        "modified" to "संशोधित",
        "restore" to "पुनर्स्थापित करें",
        "delete_permanent" to "स्थायी रूप से हटाएं",
        "to_trash" to "कचरा पेटी में भेजें",
        "password" to "पासवर्ड",
        "enter_btn" to "प्रवेश करें",
        "exit" to "बाहर निकलें",
        "days_short" to "दिन",
        "words_suffix" to "शब्द",
        "chars_suffix" to "अक्षर",
        "no_projects_yet" to "अभी कोई परियोजना नहीं है",
        "create_first_project_desc" to "लिखना शुरू करने के लिए अपनी पहली परियोजना बनाएं।"
    )

    private fun build_nl() = mapOf(
        "my_studio" to "Mijn Creatieve Studio",
        "cabinet" to "Werkruimte",
        "progress" to "Voortgang",
        "settings" to "Instellingen",
        "options" to "Opties",
        "new_project" to "Nieuw project",
        "search" to "Zoeken",
        "active" to "Actief",
        "projects" to "Projecten",
        "favorites" to "Favorieten",
        "archive" to "Archief",
        "trash" to "Prullenbak",
        "folders" to "Mappen",
        "documents" to "Documenten",
        "create" to "Maken",
        "cancel" to "Annuleren",
        "delete" to "Verwijderen",
        "edit" to "Bewerken",
        "versions" to "Versies",
        "close" to "Sluiten",
        "find_replace" to "Zoeken & Vervangen",
        "prompter" to "Teleprompter",
        "manual_save" to "Opslaan",
        "words" to "Woorden",
        "chars" to "Tekens",
        "pages" to "Pagina's",
        "reading_time" to "Leestijd",
        "min" to "min",
        "sys_settings" to "Instellingen",
        "theme" to "Thema",
        "theme_light" to "Licht",
        "theme_dark" to "Donker",
        "theme_black" to "Zwart (OLED)",
        "color_palette" to "Kleurenpalet",
        "security" to "Beveiliging & PIN",
        "backups" to "Back-up & Herstel",
        "export_backup" to "Back-up exporteren",
        "import_backup" to "Back-up importeren",
        "app_language" to "Interfacetaal",
        "lang_system" to "Systeemstandaard",
        "system_auto_detect" to "Automatische detectie",
        "all_languages" to "Alle talen",
        "scene" to "Scènekop",
        "action" to "Actiebeschrijving",
        "character" to "Personagenaam",
        "dialogue" to "Dialoog",
        "custom" to "Aangepast blok",
        "add_block" to "Blok toevoegen",
        "choose_lang_title" to "Taal selecteren",
        "search_language" to "Taal zoeken...",
        "save_template" to "Sjabloon opslaan",
        "saved_templates" to "Opgeslagen sjablonen",
        "stats_title" to "Schrijversstatistieken",
        "cat_all" to "Alles",
        "cat_book" to "Boek",
        "cat_screenplay" to "Scenario",
        "cat_story" to "Verhaal",
        "cat_text" to "Tekst",
        "modified" to "Gewijzigd",
        "restore" to "Herstellen",
        "delete_permanent" to "Permanent verwijderen",
        "to_trash" to "Naar prullenbak",
        "password" to "Wachtwoord",
        "enter_btn" to "Inloggen",
        "exit" to "Afsluiten",
        "no_projects_yet" to "Nog geen projecten",
        "create_first_project_desc" to "Maak je eerste project om te beginnen."
    )

    private fun build_sv() = mapOf(
        "my_studio" to "Min Kreativa Studio",
        "cabinet" to "Arbetsyta",
        "progress" to "Framsteg",
        "settings" to "Inställningar",
        "options" to "Alternativ",
        "new_project" to "Nytt projekt",
        "search" to "Sök",
        "active" to "Aktiva",
        "projects" to "Projekt",
        "favorites" to "Favoriter",
        "archive" to "Arkiv",
        "trash" to "Papperskorg",
        "folders" to "Mappar",
        "documents" to "Dokument",
        "create" to "Skapa",
        "cancel" to "Avbryt",
        "delete" to "Radera",
        "edit" to "Redigera",
        "versions" to "Versioner",
        "close" to "Stäng",
        "find_replace" to "Sök & Ersätt",
        "prompter" to "Teleprompter",
        "manual_save" to "Spara",
        "words" to "Ord",
        "chars" to "Tecken",
        "pages" to "Sidor",
        "reading_time" to "Lästid",
        "min" to "min",
        "sys_settings" to "Inställningar",
        "theme" to "Tema",
        "theme_light" to "Ljust",
        "theme_dark" to "Mörkt",
        "theme_black" to "Svart (OLED)",
        "color_palette" to "Färgpalett",
        "security" to "Säkerhet & PIN",
        "backups" to "Säkerhetskopiering",
        "export_backup" to "Exportera säkerhetskopia",
        "import_backup" to "Importera säkerhetskopia",
        "app_language" to "Gränssnittsspråk",
        "lang_system" to "Systemstandard",
        "system_auto_detect" to "Automatisk identifiering",
        "all_languages" to "Alla språk",
        "scene" to "Scenrubrik",
        "action" to "Handlingsbeskrivning",
        "character" to "Karaktärsnamn",
        "dialogue" to "Dialog",
        "custom" to "Anpassat block",
        "add_block" to "Lägg till block",
        "choose_lang_title" to "Välj språk",
        "search_language" to "Sök språk...",
        "save_template" to "Spara mall",
        "saved_templates" to "Sparade mallar",
        "stats_title" to "Författarstatistik",
        "cat_all" to "Alla",
        "cat_book" to "Bok",
        "cat_screenplay" to "Manuskript",
        "cat_story" to "Berättelse",
        "cat_text" to "Text",
        "modified" to "Ändrad",
        "restore" to "Återställ",
        "delete_permanent" to "Radera permanent",
        "to_trash" to "Till papperskorgen",
        "password" to "Lösenord",
        "enter_btn" to "Logga in",
        "exit" to "Avsluta",
        "no_projects_yet" to "Inga projekt än",
        "create_first_project_desc" to "Skapa ditt första projekt för att börja skriva."
    )

    private fun build_cs() = mapOf(
        "my_studio" to "Moje Tvůrčí Studio",
        "cabinet" to "Pracovní plocha",
        "progress" to "Pokrok",
        "settings" to "Nastavení",
        "options" to "Možnosti",
        "new_project" to "Nový projekt",
        "search" to "Hledat",
        "active" to "Aktivní",
        "projects" to "Projekty",
        "favorites" to "Oblíbené",
        "archive" to "Archiv",
        "trash" to "Koš",
        "folders" to "Složky",
        "documents" to "Dokumenty",
        "create" to "Vytvořit",
        "cancel" to "Zrušit",
        "delete" to "Smazat",
        "edit" to "Upravit",
        "versions" to "Verze",
        "close" to "Zavřít",
        "find_replace" to "Najít & Nahradit",
        "prompter" to "Čtecí zařízení",
        "manual_save" to "Uložit",
        "words" to "Slov",
        "chars" to "Znaků",
        "pages" to "Stran",
        "reading_time" to "Doba čtení",
        "min" to "min",
        "sys_settings" to "Nastavení",
        "theme" to "Motiv",
        "theme_light" to "Světlý",
        "theme_dark" to "Tmavý",
        "theme_black" to "Černý (OLED)",
        "color_palette" to "Barevná paleta",
        "security" to "Zabezpečení & PIN",
        "backups" to "Zálohování",
        "export_backup" to "Exportovat zálohu",
        "import_backup" to "Importovat zálohu",
        "app_language" to "Jazyk rozhraní",
        "lang_system" to "Výchozí systémový",
        "system_auto_detect" to "Automatická detekce",
        "all_languages" to "Všechny jazyky",
        "scene" to "Záhlaví scény",
        "action" to "Popis akce",
        "character" to "Jméno postavy",
        "dialogue" to "Dialog",
        "custom" to "Vlastní blok",
        "add_block" to "Přidat blok",
        "choose_lang_title" to "Vybrat jazyk",
        "search_language" to "Hledat jazyk...",
        "save_template" to "Uložit šablonu",
        "saved_templates" to "Uložené šablony",
        "stats_title" to "Statistiky autora",
        "cat_all" to "Vše",
        "cat_book" to "Kniha",
        "cat_screenplay" to "Scénář",
        "cat_story" to "Povídka",
        "cat_text" to "Text",
        "modified" to "Změněno",
        "restore" to "Obnovit",
        "delete_permanent" to "Trvale smazat",
        "to_trash" to "Do koše",
        "password" to "Heslo",
        "enter_btn" to "Vstoupit",
        "exit" to "Odejít",
        "no_projects_yet" to "Zatím žádné projekty",
        "create_first_project_desc" to "Vytvořte svůj první projekt."
    )

    private fun build_el() = mapOf(
        "my_studio" to "Το Δημιουργικό μου Στούντιο",
        "cabinet" to "Επιφάνεια εργασίας",
        "progress" to "Πρόοδος",
        "settings" to "Ρυθμίσεις",
        "options" to "Επιλογές",
        "new_project" to "Νέο έργο",
        "search" to "Αναζήτηση",
        "active" to "Ενεργά",
        "projects" to "Έργα",
        "favorites" to "Αγαπημένα",
        "archive" to "Αρχείο",
        "trash" to "Κάδος",
        "folders" to "Φάκελοι",
        "documents" to "Έγγραφα",
        "create" to "Δημιουργία",
        "cancel" to "Ακύρωση",
        "delete" to "Διαγραφή",
        "edit" to "Επεξεργασία",
        "versions" to "Εκδόσεις",
        "close" to "Κλείσιμο",
        "find_replace" to "Εύρεση & Αντικατάσταση",
        "prompter" to "Υποβολέας",
        "manual_save" to "Αποθήκευση",
        "words" to "Λέξεις",
        "chars" to "Χαρακτήρες",
        "pages" to "Σελίδες",
        "reading_time" to "Χρόνος ανάγνωσης",
        "min" to "λεπτά",
        "sys_settings" to "Ρυθμίσεις",
        "theme" to "Θέμα",
        "theme_light" to "Φωτεινό",
        "theme_dark" to "Σκοτεινό",
        "theme_black" to "Μαύρο (OLED)",
        "color_palette" to "Παλέτα χρωμάτων",
        "security" to "Ασφάλεια & PIN",
        "backups" to "Αντίγραφα ασφαλείας",
        "export_backup" to "Εξαγωγή αντιγράφου",
        "import_backup" to "Εισαγωγή αντιγράφου",
        "app_language" to "Γλώσσα εφαρμογής",
        "lang_system" to "Προεπιλογή συστήματος",
        "system_auto_detect" to "Αυτόματος εντοπισμός",
        "all_languages" to "Όλες οι γλώσσες",
        "scene" to "Επικεφαλίδα σκηνής",
        "action" to "Περιγραφή δράσης",
        "character" to "Όνομα χαρακτήρα",
        "dialogue" to "Διάλογος",
        "custom" to "Προσαρμοσμένο μπλοκ",
        "add_block" to "Προσθήκη μπλοκ",
        "choose_lang_title" to "Επιλογή γλώσσας",
        "search_language" to "Αναζήτηση γλώσσας...",
        "save_template" to "Αποθήκευση προτύπου",
        "saved_templates" to "Αποθηκευμένα πρότυπα",
        "stats_title" to "Στατιστικά συγγραφέα",
        "cat_all" to "Όλα",
        "cat_book" to "Βιβλίο",
        "cat_screenplay" to "Σενάριο",
        "cat_story" to "Ιστορία",
        "cat_text" to "Κείμενο",
        "modified" to "Τροποποιήθηκε",
        "restore" to "Επαναφορά",
        "delete_permanent" to "Μόνιμη διαγραφή",
        "to_trash" to "Στον κάδο",
        "password" to "Κωδικός",
        "enter_btn" to "Είσοδος",
        "exit" to "Έξοδος",
        "no_projects_yet" to "Δεν υπάρχουν έργα ακόμα",
        "create_first_project_desc" to "Δημιουργήστε το πρώτο σας έργο."
    )

    private fun build_ro() = mapOf(
        "my_studio" to "Studioul Meu Creativ",
        "cabinet" to "Spațiu de lucru",
        "progress" to "Progres",
        "settings" to "Setări",
        "options" to "Opțiuni",
        "new_project" to "Proiect nou",
        "search" to "Căutare",
        "active" to "Active",
        "projects" to "Proiecte",
        "favorites" to "Favorite",
        "archive" to "Arhivă",
        "trash" to "Coș de gunoi",
        "folders" to "Dosare",
        "documents" to "Documente",
        "create" to "Creare",
        "cancel" to "Anulare",
        "delete" to "Ștergere",
        "edit" to "Editare",
        "versions" to "Versiuni",
        "close" to "Închidere",
        "find_replace" to "Căutare & Înlocuire",
        "prompter" to "Teleprompter",
        "manual_save" to "Salvare",
        "words" to "Cuvinte",
        "chars" to "Caractere",
        "pages" to "Pagini",
        "reading_time" to "Timp citire",
        "min" to "min",
        "sys_settings" to "Setări",
        "theme" to "Temă",
        "theme_light" to "Luminoasă",
        "theme_dark" to "Întunecată",
        "theme_black" to "Negru (OLED)",
        "color_palette" to "Paletă de culori",
        "security" to "Securitate & PIN",
        "backups" to "Copii de rezervă",
        "export_backup" to "Exportare copie",
        "import_backup" to "Importare copie",
        "app_language" to "Limba aplicației",
        "lang_system" to "Standard sistem",
        "system_auto_detect" to "Detectare automată",
        "all_languages" to "Toate limbile",
        "scene" to "Titlu scenă",
        "action" to "Descrierea acțiunii",
        "character" to "Nume personaj",
        "dialogue" to "Dialog",
        "custom" to "Bloc personalizat",
        "add_block" to "Adăugare bloc",
        "choose_lang_title" to "Selectați limba",
        "search_language" to "Căutați limba...",
        "save_template" to "Salvare șablon",
        "saved_templates" to "Șabloane salvate",
        "stats_title" to "Statistici scriitor",
        "cat_all" to "Toate",
        "cat_book" to "Carte",
        "cat_screenplay" to "Scenariu",
        "cat_story" to "Povestire",
        "cat_text" to "Text",
        "modified" to "Modificat",
        "restore" to "Restaurare",
        "delete_permanent" to "Ștergere definitivă",
        "to_trash" to "La coș",
        "password" to "Parolă",
        "enter_btn" to "Intrare",
        "exit" to "Ieșire",
        "no_projects_yet" to "Niciun proiect încă",
        "create_first_project_desc" to "Creați primul proiect pentru a începe."
    )

    private fun build_hu() = mapOf(
        "my_studio" to "Kreatív Stúdióm",
        "cabinet" to "Munkaterület",
        "progress" to "Előrehaladás",
        "settings" to "Beállítások",
        "options" to "Opciók",
        "new_project" to "Új projekt",
        "search" to "Keresés",
        "active" to "Aktív",
        "projects" to "Projektek",
        "favorites" to "Kedvencek",
        "archive" to "Archívum",
        "trash" to "Kuka",
        "folders" to "Mappák",
        "documents" to "Dokumentumok",
        "create" to "Létrehozás",
        "cancel" to "Mégse",
        "delete" to "Törlés",
        "edit" to "Szerkesztés",
        "versions" to "Verziók",
        "close" to "Bezárás",
        "find_replace" to "Keresés & Csere",
        "prompter" to "Súgógép",
        "manual_save" to "Mentés",
        "words" to "Szó",
        "chars" to "Karakter",
        "pages" to "Oldal",
        "reading_time" to "Olvasási idő",
        "min" to "perc",
        "sys_settings" to "Beállítások",
        "theme" to "Téma",
        "theme_light" to "Világos",
        "theme_dark" to "Sötét",
        "theme_black" to "Fekete (OLED)",
        "color_palette" to "Színpaletta",
        "security" to "Biztonság & PIN",
        "backups" to "Biztonsági mentés",
        "export_backup" to "Mentés exportálása",
        "import_backup" to "Mentés importálása",
        "app_language" to "Alkalmazás nyelve",
        "lang_system" to "Rendszer alapértelmezett",
        "system_auto_detect" to "Automatikus felismerés",
        "all_languages" to "Minden nyelv",
        "scene" to "Jelenetfejléc",
        "action" to "Cselekményleírás",
        "character" to "Karakternév",
        "dialogue" to "Párbeszéd",
        "custom" to "Egyéni blokk",
        "add_block" to "Blokk hozzáadása",
        "choose_lang_title" to "Nyelv kiválasztása",
        "search_language" to "Nyelv keresése...",
        "save_template" to "Sablon mentése",
        "saved_templates" to "Mentett sablonok",
        "stats_title" to "Írói statisztikák",
        "cat_all" to "Mind",
        "cat_book" to "Könyv",
        "cat_screenplay" to "Forgatókönyv",
        "cat_story" to "Novella",
        "cat_text" to "Szöveg",
        "modified" to "Módosítva",
        "restore" to "Visszaállítás",
        "delete_permanent" to "Végleges törlés",
        "to_trash" to "Kukába helyezés",
        "password" to "Jelszó",
        "enter_btn" to "Belépés",
        "exit" to "Kilépés",
        "no_projects_yet" to "Még nincsenek projektek",
        "create_first_project_desc" to "Hozd létre az első projektedet."
    )

    private fun build_da() = mapOf(
        "my_studio" to "Mit Kreative Studie",
        "cabinet" to "Arbejdsrum",
        "progress" to "Fremskridt",
        "settings" to "Indstillinger",
        "options" to "Valgmuligheder",
        "new_project" to "Nyt projekt",
        "search" to "Søg",
        "active" to "Aktive",
        "projects" to "Projekter",
        "favorites" to "Favoritter",
        "archive" to "Arkiv",
        "trash" to "Papirkurv",
        "folders" to "Mapper",
        "documents" to "Dokumenter",
        "create" to "Opret",
        "cancel" to "Annuller",
        "delete" to "Slet",
        "edit" to "Rediger",
        "versions" to "Versioner",
        "close" to "Luk",
        "find_replace" to "Find & Erstat",
        "prompter" to "Teleprompter",
        "manual_save" to "Gem",
        "words" to "Ord",
        "chars" to "Tegn",
        "pages" to "Sider",
        "reading_time" to "Læsetid",
        "min" to "min",
        "sys_settings" to "Indstillinger",
        "theme" to "Tema",
        "theme_light" to "Lyst",
        "theme_dark" to "Mørkt",
        "theme_black" to "Sort (OLED)",
        "color_palette" to "Farvepalet",
        "security" to "Sikkerhed & PIN",
        "backups" to "Sikkerhedskopi",
        "export_backup" to "Eksporter sikkerhedskopi",
        "import_backup" to "Importer sikkerhedskopi",
        "app_language" to "App-sprog",
        "lang_system" to "Systemstandard",
        "system_auto_detect" to "Automatisk registrering",
        "all_languages" to "Alle sprog",
        "scene" to "Scenerubrik",
        "action" to "Handlingsbeskrivelse",
        "character" to "Karakternavn",
        "dialogue" to "Dialog",
        "custom" to "Brugerdefineret blok",
        "add_block" to "Tilføj blok",
        "choose_lang_title" to "Vælg sprog",
        "search_language" to "Søg sprog...",
        "save_template" to "Gem skabelon",
        "saved_templates" to "Gemte skabeloner",
        "stats_title" to "Forfatterstatistik",
        "cat_all" to "Alle",
        "cat_book" to "Bog",
        "cat_screenplay" to "Manuskript",
        "cat_story" to "Historie",
        "cat_text" to "Tekst",
        "modified" to "Ændret",
        "restore" to "Gendan",
        "delete_permanent" to "Slet permanent",
        "to_trash" to "Til papirkurv",
        "password" to "Adgangskode",
        "enter_btn" to "Log ind",
        "exit" to "Afslut",
        "no_projects_yet" to "Ingen projekter endnu",
        "create_first_project_desc" to "Opret dit første projekt for at komme i gang."
    )

    private fun build_fi() = mapOf(
        "my_studio" to "Luova Studioni",
        "cabinet" to "Työtila",
        "progress" to "Edistyminen",
        "settings" to "Asetukset",
        "options" to "Valinnat",
        "new_project" to "Uusi projekti",
        "search" to "Haku",
        "active" to "Aktiiviset",
        "projects" to "Projektit",
        "favorites" to "Suosikit",
        "archive" to "Arkisto",
        "trash" to "Roskakori",
        "folders" to "Kansiot",
        "documents" to "Asiakirjat",
        "create" to "Luo",
        "cancel" to "Peruuta",
        "delete" to "Poista",
        "edit" to "Muokkaa",
        "versions" to "Versiot",
        "close" to "Sulje",
        "find_replace" to "Etsi & Korvaa",
        "prompter" to "Teleprompteri",
        "manual_save" to "Tallenna",
        "words" to "Sanaa",
        "chars" to "Merkkiä",
        "pages" to "Sivua",
        "reading_time" to "Lukuaika",
        "min" to "min",
        "sys_settings" to "Asetukset",
        "theme" to "Teema",
        "theme_light" to "Vaalea",
        "theme_dark" to "Tumma",
        "theme_black" to "Musta (OLED)",
        "color_palette" to "Väripaletti",
        "security" to "Turvallisuus & PIN",
        "backups" to "Varmuuskopiointi",
        "export_backup" to "Vie varmuuskopio",
        "import_backup" to "Tuo varmuuskopio",
        "app_language" to "Sovelluksen kieli",
        "lang_system" to "Järjestelmän oletus",
        "system_auto_detect" to "Automaattinen tunnistus",
        "all_languages" to "Kaikki kielet",
        "scene" to "Kohtauksen otsikko",
        "action" to "Toiminnan kuvaus",
        "character" to "Hahmon nimi",
        "dialogue" to "Vuoropuhelu",
        "custom" to "Mukautettu lohko",
        "add_block" to "Lisää lohko",
        "choose_lang_title" to "Valitse kieli",
        "search_language" to "Etsi kieltä...",
        "save_template" to "Tallenna malli",
        "saved_templates" to "Tallennetut mallit",
        "stats_title" to "Kirjailijan tilastot",
        "cat_all" to "Kaikki",
        "cat_book" to "Kirja",
        "cat_screenplay" to "Käsikirjoitus",
        "cat_story" to "Tarina",
        "cat_text" to "Teksti",
        "modified" to "Muokattu",
        "restore" to "Palauta",
        "delete_permanent" to "Poista pysyvästi",
        "to_trash" to "Roskakoriin",
        "password" to "Salasana",
        "enter_btn" to "Kirjaudu",
        "exit" to "Poistu",
        "no_projects_yet" to "Ei vielä projekteja",
        "create_first_project_desc" to "Luo ensimmäinen projektisi aloittaaksesi."
    )

    private fun build_no() = mapOf(
        "my_studio" to "Mitt Kreative Studio",
        "cabinet" to "Arbeidsrom",
        "progress" to "Fremgang",
        "settings" to "Innstillinger",
        "options" to "Alternativer",
        "new_project" to "Nytt prosjekt",
        "search" to "Søk",
        "active" to "Aktive",
        "projects" to "Prosjekter",
        "favorites" to "Favoritter",
        "archive" to "Arkiv",
        "trash" to "Papirkurv",
        "folders" to "Mapper",
        "documents" to "Dokumenter",
        "create" to "Opprett",
        "cancel" to "Avbryt",
        "delete" to "Slett",
        "edit" to "Rediger",
        "versions" to "Versjoner",
        "close" to "Lukk",
        "find_replace" to "Finn & Erstatt",
        "prompter" to "Teleprompter",
        "manual_save" to "Lagre",
        "words" to "Ord",
        "chars" to "Tegn",
        "pages" to "Sider",
        "reading_time" to "Lesetid",
        "min" to "min",
        "sys_settings" to "Innstillinger",
        "theme" to "Tema",
        "theme_light" to "Lyst",
        "theme_dark" to "Mørkt",
        "theme_black" to "Svart (OLED)",
        "color_palette" to "Fargepalett",
        "security" to "Sikkerhet & PIN",
        "backups" to "Sikkerhetskopi",
        "export_backup" to "Eksporter sikkerhetskopi",
        "import_backup" to "Importer sikkerhetskopi",
        "app_language" to "App-språk",
        "lang_system" to "Systemstandard",
        "system_auto_detect" to "Automatisk gjenkjenning",
        "all_languages" to "Alle språk",
        "scene" to "Scenerubrikk",
        "action" to "Handlingsbeskrivelse",
        "character" to "Karakternavn",
        "dialogue" to "Dialog",
        "custom" to "Tilpasset blokk",
        "add_block" to "Legg til blokk",
        "choose_lang_title" to "Velg språk",
        "search_language" to "Søk språk...",
        "save_template" to "Lagre mal",
        "saved_templates" to "Lagrede maler",
        "stats_title" to "Forfatterstatistikk",
        "cat_all" to "Alle",
        "cat_book" to "Bok",
        "cat_screenplay" to "Manuskript",
        "cat_story" to "Historie",
        "cat_text" to "Tekst",
        "modified" to "Endret",
        "restore" to "Gjenopprett",
        "delete_permanent" to "Slett permanent",
        "to_trash" to "Til papirkurv",
        "password" to "Passord",
        "enter_btn" to "Logg inn",
        "exit" to "Avslutt",
        "no_projects_yet" to "Ingen prosjekter ennå",
        "create_first_project_desc" to "Opprett ditt første prosjekt for å begynne."
    )

    private fun build_he() = mapOf(
        "my_studio" to "הסטודיו היצירתי שלי",
        "cabinet" to "מרחב עבודה",
        "progress" to "התקדמות",
        "settings" to "הגדרות",
        "options" to "אפשרויות",
        "new_project" to "פרויקט חדש",
        "search" to "חיפוש",
        "active" to "פעילים",
        "projects" to "פרויקטים",
        "favorites" to "מועדפים",
        "archive" to "ארכיון",
        "trash" to "אשפה",
        "folders" to "תיקיות",
        "documents" to "מסמכים",
        "create" to "צור",
        "cancel" to "ביטול",
        "delete" to "מחק",
        "edit" to "ערוך",
        "versions" to "גרסאות",
        "close" to "סגור",
        "find_replace" to "חיפוש והחלפה",
        "prompter" to "טלפרומפטר",
        "manual_save" to "שמור",
        "words" to "מילים",
        "chars" to "תווים",
        "pages" to "עמודים",
        "reading_time" to "זמן קריאה",
        "min" to "דק'",
        "sys_settings" to "הגדרות",
        "theme" to "ערכת נושא",
        "theme_light" to "בהיר",
        "theme_dark" to "כהה",
        "theme_black" to "שחור (OLED)",
        "color_palette" to "לוח צבעים",
        "security" to "אבטחה וקוד PIN",
        "backups" to "גיבוי ושחזור",
        "export_backup" to "ייצוא גיבוי",
        "import_backup" to "ייבוא גיבוי",
        "app_language" to "שפת ממשק",
        "lang_system" to "ברירת מחדל של המערכת",
        "system_auto_detect" to "זיהוי אוטומטי",
        "all_languages" to "כל השפות",
        "scene" to "כותרת סצנה",
        "action" to "תיאור פעולה",
        "character" to "שם דמות",
        "dialogue" to "דיאלוג",
        "custom" to "בלוק מותאם",
        "add_block" to "הוסף בלוק",
        "choose_lang_title" to "בחר שפה",
        "search_language" to "חפש שפה...",
        "save_template" to "שמור תבנית",
        "saved_templates" to "תבניות שמורות",
        "stats_title" to "סטטיסטיקת כותב",
        "cat_all" to "הכל",
        "cat_book" to "ספר",
        "cat_screenplay" to "תסריט",
        "cat_story" to "סיפור",
        "cat_text" to "טקסט",
        "modified" to "שונה",
        "restore" to "שחזר",
        "delete_permanent" to "מחק לצמיתות",
        "to_trash" to "העבר לאשפה",
        "password" to "סיסמה",
        "enter_btn" to "כניסה",
        "exit" to "יציאה",
        "no_projects_yet" to "אין עדיין פרויקטים",
        "create_first_project_desc" to "צור את הפרויקט הראשון שלך."
    )

    private fun build_fa() = mapOf(
        "my_studio" to "استودیوی خلاق من",
        "cabinet" to "میز کار",
        "progress" to "پیشرفت",
        "settings" to "تنظیمات",
        "options" to "گزینه‌ها",
        "new_project" to "پروژه جدید",
        "search" to "جستجو",
        "active" to "فعال",
        "projects" to "پروژه‌ها",
        "favorites" to "علاقه‌مندی‌ها",
        "archive" to "آرشیو",
        "trash" to "سطل زباله",
        "folders" to "پوشه‌ها",
        "documents" to "اسناد",
        "create" to "ایجاد",
        "cancel" to "لغو",
        "delete" to "حذف",
        "edit" to "ویرایش",
        "versions" to "نسخه‌ها",
        "close" to "بستن",
        "find_replace" to "جستجو و جایگزینی",
        "prompter" to "متن‌خوان",
        "manual_save" to "ذخیره",
        "words" to "کلمات",
        "chars" to "حروف",
        "pages" to "صفحات",
        "reading_time" to "زمان خواندن",
        "min" to "دقیقه",
        "sys_settings" to "تنظیمات",
        "theme" to "تم",
        "theme_light" to "روشن",
        "theme_dark" to "تاریک",
        "theme_black" to "مشکی (OLED)",
        "color_palette" to "پالت رنگ",
        "security" to "امنیت و پین",
        "backups" to "پشتیبان‌گیری",
        "export_backup" to "خروجی پشتیبان",
        "import_backup" to "ورودی پشتیبان",
        "app_language" to "زبان برنامه",
        "lang_system" to "پیش‌فرض سیستم",
        "system_auto_detect" to "تشخیص خودکار",
        "all_languages" to "تمام زبان‌ها",
        "scene" to "عنوان صحنه",
        "action" to "شرح عمل",
        "character" to "نام شخصیت",
        "dialogue" to "دیالوگ",
        "custom" to "بلاک سفارشی",
        "add_block" to "افزودن بلاک",
        "choose_lang_title" to "انتخاب زبان",
        "search_language" to "جستجوی زبان...",
        "save_template" to "ذخیره الگو",
        "saved_templates" to "الگوهای ذخیره شده",
        "stats_title" to "آمار نویسنده",
        "cat_all" to "همه",
        "cat_book" to "کتاب",
        "cat_screenplay" to "فیلم‌نامه",
        "cat_story" to "داستان",
        "cat_text" to "متن",
        "modified" to "ویرایش شده",
        "restore" to "بازیابی",
        "delete_permanent" to "حذف دائم",
        "to_trash" to "به سطل زباله",
        "password" to "رمز عبور",
        "enter_btn" to "ورود",
        "exit" to "خروج",
        "no_projects_yet" to "هنوز پروژه‌ای نیست",
        "create_first_project_desc" to "اولین پروژه خود را بسازید."
    )

    private fun build_bn() = mapOf(
        "my_studio" to "আমার ক্রিয়েটিভ স্টুডিও",
        "cabinet" to "ওয়ার্কস্পেস",
        "progress" to "অগ্রগতি",
        "settings" to "সেটিংস",
        "options" to "বিকল্প",
        "new_project" to "নতুন প্রজেক্ট",
        "search" to "অনুসন্ধান",
        "active" to "সক্রিয়",
        "projects" to "প্রজেক্টসমূহ",
        "favorites" to "প্রিয়",
        "archive" to "আর্কাইভ",
        "trash" to "ট্র্যাশ",
        "folders" to "ফোল্ডার",
        "documents" to "ডকুমেন্ট",
        "create" to "তৈরি করুন",
        "cancel" to "বাতিল",
        "delete" to "মুছুন",
        "edit" to "সম্পাদনা",
        "versions" to "সংস্করণ",
        "close" to "বন্ধ করুন",
        "find_replace" to "খুঁজুন এবং প্রতিস্থাপন",
        "prompter" to "টেলিপম্পটার",
        "manual_save" to "সংরক্ষণ",
        "words" to "শব্দ",
        "chars" to "অক্ষর",
        "pages" to "পৃষ্ঠা",
        "reading_time" to "পড়ার সময়",
        "min" to "মিনিট",
        "sys_settings" to "সেটিংস",
        "theme" to "থিম",
        "theme_light" to "হালকা",
        "theme_dark" to "গাঢ়",
        "theme_black" to "কালো (OLED)",
        "color_palette" to "কালার প্যালেট",
        "security" to "নিরাপত্তা ও পিন",
        "backups" to "ব্যাকআপ",
        "export_backup" to "ব্যাকআপ রপ্তানি",
        "import_backup" to "ব্যাকআপ আমদানি",
        "app_language" to "অ্যাপ ভাষা",
        "lang_system" to "সিস্টেম ডিফল্ট",
        "system_auto_detect" to "স্বয়ংক্রিয় সনাক্তকরণ",
        "all_languages" to "সকল ভাষা",
        "scene" to "দৃশ্যের শিরোনাম",
        "action" to "কর্মের বিবরণ",
        "character" to "চরিত্রের নাম",
        "dialogue" to "সংলাপ",
        "custom" to "কাস্টম ব্লক",
        "add_block" to "ব্লক যোগ করুন",
        "choose_lang_title" to "ভাষা নির্বাচন করুন",
        "search_language" to "ভাষা খুঁজুন...",
        "save_template" to "টেমপ্লেট সংরক্ষণ",
        "saved_templates" to "সংরক্ষিত টেমপ্লেট",
        "stats_title" to "লেখকের পরিসংখ্যান",
        "cat_all" to "সব",
        "cat_book" to "বই",
        "cat_screenplay" to "চিত্রনাট্য",
        "cat_story" to "গল্প",
        "cat_text" to "পাঠ্য",
        "modified" to "পরিবর্তিত",
        "restore" to "পুনরুদ্ধার",
        "delete_permanent" to "স্থায়ীভাবে মুছুন",
        "to_trash" to "ট্র্যাশে পাঠান",
        "password" to "পাসওয়ার্ড",
        "enter_btn" to "প্রবেশ",
        "exit" to "প্রস্থান",
        "no_projects_yet" to "এখনও কোনও প্রজেক্ট নেই",
        "create_first_project_desc" to "লেখা শুরু করতে আপনার প্রথম প্রজেক্ট তৈরি করুন।"
    )

    private fun build_th() = mapOf(
        "my_studio" to "สตูดิโอสร้างสรรค์ของฉัน",
        "cabinet" to "พื้นที่ทำงาน",
        "progress" to "ความคืบหน้า",
        "settings" to "การตั้งค่า",
        "options" to "ตัวเลือก",
        "new_project" to "โครงการใหม่",
        "search" to "ค้นหา",
        "active" to "ใช้งานอยู่",
        "projects" to "โครงการ",
        "favorites" to "รายการโปรด",
        "archive" to "คลังเก็บ",
        "trash" to "ถังขยะ",
        "folders" to "โฟลเดอร์",
        "documents" to "เอกสาร",
        "create" to "สร้าง",
        "cancel" to "ยกเลิก",
        "delete" to "ลบ",
        "edit" to "แก้ไข",
        "versions" to "เวอร์ชัน",
        "close" to "ปิด",
        "find_replace" to "ค้นหาและแทนที่",
        "prompter" to "เครื่องบอกบท",
        "manual_save" to "บันทึก",
        "words" to "คำ",
        "chars" to "ตัวอักษร",
        "pages" to "หน้า",
        "reading_time" to "เวลาอ่าน",
        "min" to "นาที",
        "sys_settings" to "การตั้งค่า",
        "theme" to "ธีม",
        "theme_light" to "สว่าง",
        "theme_dark" to "มืด",
        "theme_black" to "ดำ (OLED)",
        "color_palette" to "จานสี",
        "security" to "ความปลอดภัยและ PIN",
        "backups" to "การสำรองข้อมูล",
        "export_backup" to "ส่งออกข้อมูลสำรอง",
        "import_backup" to "นำเข้าข้อมูลสำรอง",
        "app_language" to "ภาษาของแอป",
        "lang_system" to "ค่าเริ่มต้นของระบบ",
        "system_auto_detect" to "ตรวจหาอัตโนมัติ",
        "all_languages" to "ทุกภาษา",
        "scene" to "หัวข้อฉาก",
        "action" to "คำอธิบายการกระทำ",
        "character" to "ชื่อตัวละคร",
        "dialogue" to "บทสนทนา",
        "custom" to "บล็อกที่กำหนดเอง",
        "add_block" to "เพิ่มบล็อก",
        "choose_lang_title" to "เลือกภาษา",
        "search_language" to "ค้นหาภาษา...",
        "save_template" to "บันทึกเทมเพลต",
        "saved_templates" to "เทมเพลตที่บันทึกไว้",
        "stats_title" to "สถิติของนักเขียน",
        "cat_all" to "ทั้งหมด",
        "cat_book" to "หนังสือ",
        "cat_screenplay" to "บทภาพยนตร์",
        "cat_story" to "เรื่องสั้น",
        "cat_text" to "ข้อความ",
        "modified" to "แก้ไขเมื่อ",
        "restore" to "กู้คืน",
        "delete_permanent" to "ลบถาวร",
        "to_trash" to "ย้ายไปถังขยะ",
        "password" to "รหัสผ่าน",
        "enter_btn" to "เข้าสู่ระบบ",
        "exit" to "ออก",
        "no_projects_yet" to "ยังไม่มีโครงการ",
        "create_first_project_desc" to "สร้างโครงการแรกของคุณเพื่อเริ่มเขียน"
    )

    private fun build_vi() = mapOf(
        "my_studio" to "Xưởng Sáng Tạo Của Tôi",
        "cabinet" to "Không gian làm việc",
        "progress" to "Tiến độ",
        "settings" to "Cài đặt",
        "options" to "Tùy chọn",
        "new_project" to "Dự án mới",
        "search" to "Tìm kiếm",
        "active" to "Đang hoạt động",
        "projects" to "Dự án",
        "favorites" to "Yêu thích",
        "archive" to "Lưu trữ",
        "trash" to "Thùng rác",
        "folders" to "Thư mục",
        "documents" to "Tài liệu",
        "create" to "Tạo",
        "cancel" to "Hủy",
        "delete" to "Xóa",
        "edit" to "Chỉnh sửa",
        "versions" to "Phiên bản",
        "close" to "Đóng",
        "find_replace" to "Tìm kiếm & Thay thế",
        "prompter" to "Máy nhắc chữ",
        "manual_save" to "Lưu",
        "words" to "Từ",
        "chars" to "Ký tự",
        "pages" to "Trang",
        "reading_time" to "Thời gian đọc",
        "min" to "phút",
        "sys_settings" to "Cài đặt",
        "theme" to "Chủ đề",
        "theme_light" to "Sáng",
        "theme_dark" to "Tối",
        "theme_black" to "Đen (OLED)",
        "color_palette" to "Bảng màu",
        "security" to "Bảo mật & mã PIN",
        "backups" to "Sao lưu & Khôi phục",
        "export_backup" to "Xuất bản sao lưu",
        "import_backup" to "Nhập bản sao lưu",
        "app_language" to "Ngôn ngữ ứng dụng",
        "lang_system" to "Mặc định hệ thống",
        "system_auto_detect" to "Tự động phát hiện",
        "all_languages" to "Tất cả ngôn ngữ",
        "scene" to "Tiêu đề cảnh",
        "action" to "Mô tả hành động",
        "character" to "Tên nhân vật",
        "dialogue" to "Đối thoại",
        "custom" to "Khối tùy chỉnh",
        "add_block" to "Thêm khối",
        "choose_lang_title" to "Chọn ngôn ngữ",
        "search_language" to "Tìm kiếm ngôn ngữ...",
        "save_template" to "Lưu mẫu",
        "saved_templates" to "Mẫu đã lưu",
        "stats_title" to "Thống kê của tác giả",
        "cat_all" to "Tất cả",
        "cat_book" to "Sách",
        "cat_screenplay" to "Kịch bản",
        "cat_story" to "Truyện ngắn",
        "cat_text" to "Văn bản",
        "modified" to "Đã sửa đổi",
        "restore" to "Khôi phục",
        "delete_permanent" to "Xóa vĩnh viễn",
        "to_trash" to "Chuyển vào thùng rác",
        "password" to "Mật khẩu",
        "enter_btn" to "Đăng nhập",
        "exit" to "Thoát",
        "no_projects_yet" to "Chưa có dự án nào",
        "create_first_project_desc" to "Tạo dự án đầu tiên của bạn để bắt đầu viết."
    )

    private fun build_id() = mapOf(
        "my_studio" to "Studio Kreatif Saya",
        "cabinet" to "Ruang Kerja",
        "progress" to "Kemajuan",
        "settings" to "Pengaturan",
        "options" to "Opsi",
        "new_project" to "Proyek Baru",
        "search" to "Cari",
        "active" to "Aktif",
        "projects" to "Proyek",
        "favorites" to "Favorit",
        "archive" to "Arsip",
        "trash" to "Sampah",
        "folders" to "Folder",
        "documents" to "Dokumen",
        "create" to "Buat",
        "cancel" to "Batal",
        "delete" to "Hapus",
        "edit" to "Edit",
        "versions" to "Versi",
        "close" to "Tutup",
        "find_replace" to "Cari & Ganti",
        "prompter" to "Teleprompter",
        "manual_save" to "Simpan",
        "words" to "Kata",
        "chars" to "Karakter",
        "pages" to "Halaman",
        "reading_time" to "Waktu Baca",
        "min" to "menit",
        "sys_settings" to "Pengaturan",
        "theme" to "Tema",
        "theme_light" to "Terang",
        "theme_dark" to "Gelap",
        "theme_black" to "Hitam (OLED)",
        "color_palette" to "Palet Warna",
        "security" to "Keamanan & PIN",
        "backups" to "Cadangan & Pulihkan",
        "export_backup" to "Ekspor Cadangan",
        "import_backup" to "Impor Cadangan",
        "app_language" to "Bahasa Aplikasi",
        "lang_system" to "Bawaan Sistem",
        "system_auto_detect" to "Deteksi Otomatis",
        "all_languages" to "Semua Bahasa",
        "scene" to "Judul Adegan",
        "action" to "Deskripsi Aksi",
        "character" to "Nama Karakter",
        "dialogue" to "Dialog",
        "custom" to "Blok Kustom",
        "add_block" to "Tambah Blok",
        "choose_lang_title" to "Pilih Bahasa",
        "search_language" to "Cari bahasa...",
        "save_template" to "Simpan Templat",
        "saved_templates" to "Templat Tersimpan",
        "stats_title" to "Statistik Penulis",
        "cat_all" to "Semua",
        "cat_book" to "Buku",
        "cat_screenplay" to "Skenario",
        "cat_story" to "Cerita",
        "cat_text" to "Teks",
        "modified" to "Diubah",
        "restore" to "Pulihkan",
        "delete_permanent" to "Hapus Permanen",
        "to_trash" to "Pindahkan ke Sampah",
        "password" to "Kata Sandi",
        "enter_btn" to "Masuk",
        "exit" to "Keluar",
        "no_projects_yet" to "Belum ada proyek",
        "create_first_project_desc" to "Buat proyek pertama Anda untuk mulai menulis."
    )

    private fun build_kk() = mapOf(
        "my_studio" to "Менің Шығармашылық Студиям",
        "cabinet" to "Жұмыс орны",
        "progress" to "Жетістіктер",
        "settings" to "Баптаулар",
        "options" to "Опциялар",
        "new_project" to "Жаңа жоба",
        "search" to "Іздеу",
        "active" to "Белсенді",
        "projects" to "Жобалар",
        "favorites" to "Таңдаулылар",
        "archive" to "Архив",
        "trash" to "Себет",
        "folders" to "Бумалар",
        "documents" to "Құжаттар",
        "create" to "Жасау",
        "cancel" to "Болдырмау",
        "delete" to "Жою",
        "edit" to "Өңдеу",
        "versions" to "Нұсқалар",
        "close" to "Жабу",
        "find_replace" to "Іздеу және ауыстыру",
        "prompter" to "Суфлер",
        "manual_save" to "Сақтау",
        "words" to "Сөздер",
        "chars" to "Таңбалар",
        "pages" to "Беттер",
        "reading_time" to "Оқу уақыты",
        "min" to "мин",
        "sys_settings" to "Баптаулар",
        "theme" to "Тақырып",
        "theme_light" to "Ашық",
        "theme_dark" to "Күңгірт",
        "theme_black" to "Қара (OLED)",
        "color_palette" to "Түстер палитрасы",
        "security" to "Қауіпсіздік және PIN",
        "backups" to "Сақтық көшірме",
        "export_backup" to "Көшірмені экспорттау",
        "import_backup" to "Көшірмені импорттау",
        "app_language" to "Қолданба тілі",
        "lang_system" to "Жүйелік әдепкі",
        "system_auto_detect" to "Автоматты анықтау",
        "all_languages" to "Барлық тілдер",
        "scene" to "Сахна тақырыбы",
        "action" to "Әрекет сипаттамасы",
        "character" to "Кейіпкер аты",
        "dialogue" to "Диалог",
        "custom" to "Арнайы блок",
        "add_block" to "Блок қосу",
        "choose_lang_title" to "Тілді таңдаңыз",
        "search_language" to "Тілді іздеу...",
        "save_template" to "Үлгіні сақтау",
        "saved_templates" to "Сақталған үлгілер",
        "stats_title" to "Жазушы статистикасы",
        "cat_all" to "Барлығы",
        "cat_book" to "Кітап",
        "cat_screenplay" to "Сценарий",
        "cat_story" to "Әңгіме",
        "cat_text" to "Мәтін",
        "modified" to "Өзгертілген",
        "restore" to "Қалпына келтіру",
        "delete_permanent" to "Мүлдем жою",
        "to_trash" to "Себетке тастау",
        "password" to "Құпиясөз",
        "enter_btn" to "Кіру",
        "exit" to "Шығу",
        "no_projects_yet" to "Әзірге жобалар жоқ",
        "create_first_project_desc" to "Жазуды бастау үшін алғашқы жобаңызды жасаңыз."
    )

    private fun build_be() = mapOf(
        "my_studio" to "Мая Творчая Студыя",
        "cabinet" to "Кабінет",
        "progress" to "Поспехі",
        "settings" to "Налады",
        "options" to "Опцыі",
        "new_project" to "Новы праект",
        "search" to "Пошук",
        "active" to "Актыўныя",
        "projects" to "Праекты",
        "favorites" to "Выбранае",
        "archive" to "Архіў",
        "trash" to "Сметніца",
        "folders" to "Папкі",
        "documents" to "Дакументы",
        "create" to "Стварыць",
        "cancel" to "Адмена",
        "delete" to "Выдаліць",
        "edit" to "Рэдагаваць",
        "versions" to "Версіі",
        "close" to "Закрыць",
        "find_replace" to "Знайсці і замяніць",
        "prompter" to "Суфлёр",
        "manual_save" to "Захаваць",
        "words" to "Слоў",
        "chars" to "Знакаў",
        "pages" to "Старонак",
        "reading_time" to "Час чытання",
        "min" to "хв",
        "sys_settings" to "Налады",
        "theme" to "Тэма",
        "theme_light" to "Светлая",
        "theme_dark" to "Цёмная",
        "theme_black" to "Чорная (OLED)",
        "color_palette" to "Каляровая палітра",
        "security" to "Бяспека і PIN",
        "backups" to "Рэзервовае капіяванне",
        "export_backup" to "Экспарт бэкапу",
        "import_backup" to "Імпарт бэкапу",
        "app_language" to "Мова інтэрфейсу",
        "lang_system" to "Сістэмная",
        "system_auto_detect" to "Аўтавызначэнне",
        "all_languages" to "Усе мовы",
        "scene" to "Загаловак сцэны",
        "action" to "Апісанне дзеяння",
        "character" to "Імя персанажа",
        "dialogue" to "Дыялог",
        "custom" to "Кастомны блок",
        "add_block" to "Дадаць блок",
        "choose_lang_title" to "Выбар мовы праграмы",
        "search_language" to "Пошук мовы...",
        "save_template" to "Захаваць шаблон",
        "saved_templates" to "Захаваныя шаблоны",
        "stats_title" to "Статыстыка пісьменніка",
        "cat_all" to "Усе",
        "cat_book" to "Кніга",
        "cat_screenplay" to "Сцэнарый",
        "cat_story" to "Апавяданне",
        "cat_text" to "Тэкст",
        "modified" to "Зменены",
        "restore" to "Аднавіць",
        "delete_permanent" to "Выдаліць назаўжды",
        "to_trash" to "У сметніцу",
        "password" to "Пароль",
        "enter_btn" to "Уваход",
        "exit" to "Выйсці",
        "no_projects_yet" to "Праектаў пакуль няма",
        "create_first_project_desc" to "Стварыце свой першы праект, каб пачаць пісаць."
    )

    private fun build_uz() = mapOf(
        "my_studio" to "Mening Ijodiy Studiyam",
        "cabinet" to "Ish maydoni",
        "progress" to "Muvaffaqiyatlar",
        "settings" to "Sozlamalar",
        "options" to "Parametrlar",
        "new_project" to "Yangi loyiha",
        "search" to "Qidiruv",
        "active" to "Faol",
        "projects" to "Loyihalar",
        "favorites" to "Sevimlilar",
        "archive" to "Arxiv",
        "trash" to "Chiqindi qutisi",
        "folders" to "Papkalar",
        "documents" to "Hujjatlar",
        "create" to "Yaratish",
        "cancel" to "Bekor qilish",
        "delete" to "O'chirish",
        "edit" to "Tahrirlash",
        "versions" to "Versiyalar",
        "close" to "Yopish",
        "find_replace" to "Qidirish va almashtirish",
        "prompter" to "Teleprompter",
        "manual_save" to "Saqlash",
        "words" to "So'zlar",
        "chars" to "Belgilar",
        "pages" to "Sahifalar",
        "reading_time" to "O'qish vaqti",
        "min" to "daqiqa",
        "sys_settings" to "Sozlamalar",
        "theme" to "Mavzu",
        "theme_light" to "Yorug'",
        "theme_dark" to "Qorong'i",
        "theme_black" to "Qora (OLED)",
        "color_palette" to "Ranglar palitrasi",
        "security" to "Xavfsizlik va PIN",
        "backups" to "Zaxira nusxasi",
        "export_backup" to "Zaxirani eksport qilish",
        "import_backup" to "Zaxirani import qilish",
        "app_language" to "Dastur tili",
        "lang_system" to "Tizim standarti",
        "system_auto_detect" to "Avtomatik aniqlash",
        "all_languages" to "Barcha tillar",
        "scene" to "Sahna sarlavhasi",
        "action" to "Harakat tavsifi",
        "character" to "Qahramon ismi",
        "dialogue" to "Dialog",
        "custom" to "Maxsus blok",
        "add_block" to "Blok qo'shish",
        "choose_lang_title" to "Tilni tanlang",
        "search_language" to "Tilni qidirish...",
        "save_template" to "Shablonni saqlash",
        "saved_templates" to "Saqlangan shablonlar",
        "stats_title" to "Yozuvchi statistikasi",
        "cat_all" to "Barchasi",
        "cat_book" to "Kitob",
        "cat_screenplay" to "Ssenariy",
        "cat_story" to "Hikoya",
        "cat_text" to "Matn",
        "modified" to "O'zgartirilgan",
        "restore" to "Qayta tiklash",
        "delete_permanent" to "Butunlay o'chirish",
        "to_trash" to "Chiqindiga yuborish",
        "password" to "Parol",
        "enter_btn" to "Kirish",
        "exit" to "Chiqish",
        "no_projects_yet" to "Hozircha loyihalar yo'q",
        "create_first_project_desc" to "Yozishni boshlash uchun birinchi loyihangizni yarating."
    )

    private fun build_az() = mapOf(
        "my_studio" to "Mənim Yaradıcı Studiyam",
        "cabinet" to "İş masası",
        "progress" to "Uğurlar",
        "settings" to "Tənzimləmələr",
        "options" to "Seçimlər",
        "new_project" to "Yeni layihə",
        "search" to "Axtarış",
        "active" to "Aktiv",
        "projects" to "Layihələr",
        "favorites" to "Sevimlilər",
        "archive" to "Arxiv",
        "trash" to "Zibil qutusu",
        "folders" to "Qovluqlar",
        "documents" to "Sənədlər",
        "create" to "Yarat",
        "cancel" to "Ləğv et",
        "delete" to "Sil",
        "edit" to "Redaktə et",
        "versions" to "Versiyalar",
        "close" to "Bağla",
        "find_replace" to "Axtar və əvəz et",
        "prompter" to "Teleprompter",
        "manual_save" to "Saxla",
        "words" to "Sözlər",
        "chars" to "İşarələr",
        "pages" to "Səhifələr",
        "reading_time" to "Oxu vaxtı",
        "min" to "dəq",
        "sys_settings" to "Tənzimləmələr",
        "theme" to "Mövzu",
        "theme_light" to "İşıqlı",
        "theme_dark" to "Qaranlıq",
        "theme_black" to "Qara (OLED)",
        "color_palette" to "Rəng palitrası",
        "security" to "Təhlükəsizlik və PIN",
        "backups" to "Ehtiyat nüsxə",
        "export_backup" to "Nüsxəni ixrac et",
        "import_backup" to "Nüsxəni idxal et",
        "app_language" to "Tətbiq dili",
        "lang_system" to "Sistem susmaya görə",
        "system_auto_detect" to "Avtomatik aşkarlama",
        "all_languages" to "Bütün dillər",
        "scene" to "Səhnə başlığı",
        "action" to "Hərəkət təsviri",
        "character" to "Personaj adı",
        "dialogue" to "Dialoq",
        "custom" to "Xüsusi blok",
        "add_block" to "Blok əlavə et",
        "choose_lang_title" to "Dil seçin",
        "search_language" to "Dil axtarışı...",
        "save_template" to "Şablonu saxla",
        "saved_templates" to "Saxlanılan şablonlar",
        "stats_title" to "Yazıçı statistikası",
        "cat_all" to "Hamısı",
        "cat_book" to "Kitab",
        "cat_screenplay" to "Ssenari",
        "cat_story" to "Hekayə",
        "cat_text" to "Mətn",
        "modified" to "Dəyişdirildi",
        "restore" to "Bərpa et",
        "delete_permanent" to "Həmişəlik sil",
        "to_trash" to "Zibilə at",
        "password" to "Şifrə",
        "enter_btn" to "Daxil ol",
        "exit" to "Çıxış",
        "no_projects_yet" to "Hələ layihə yoxdur",
        "create_first_project_desc" to "Yazmağa başlamaq üçün ilk layihənizi yaradın."
    )

    private fun build_ka() = mapOf(
        "my_studio" to "ჩემი შემოქმედებითი სტუდია",
        "cabinet" to "სამუშაო სივრცე",
        "progress" to "პროგრესი",
        "settings" to "პარამეტრები",
        "options" to "ოფციები",
        "new_project" to "ახალი პროექტი",
        "search" to "ძიება",
        "active" to "აქტიური",
        "projects" to "პროექტები",
        "favorites" to "რჩეულები",
        "archive" to "არქივი",
        "trash" to "ნაგავი",
        "folders" to "საქაღალდეები",
        "documents" to "დოკუმენტები",
        "create" to "შექმნა",
        "cancel" to "გაუქმება",
        "delete" to "წაშლა",
        "edit" to "რედაქტირება",
        "versions" to "ვერსიები",
        "close" to "დახურვა",
        "find_replace" to "ძებნა და შეცვლა",
        "prompter" to "ტელესუფლიორი",
        "manual_save" to "შენახვა",
        "words" to "სიტყვა",
        "chars" to "სიმბოლო",
        "pages" to "გვერდი",
        "reading_time" to "კითხვის დრო",
        "min" to "წთ",
        "sys_settings" to "პარამეტრები",
        "theme" to "თემა",
        "theme_light" to "ნათელი",
        "theme_dark" to "ბნელი",
        "theme_black" to "შავი (OLED)",
        "color_palette" to "ფერების პალიტრა",
        "security" to "უსაფრთხოება და PIN",
        "backups" to "სარეზერვო ასლი",
        "export_backup" to "ასლის ექსპორტი",
        "import_backup" to "ასლის იმპორტი",
        "app_language" to "აპლიკაციის ენა",
        "lang_system" to "სისტემის ნაგულისხმევი",
        "system_auto_detect" to "ავტომატური ამოცნობა",
        "all_languages" to "ყველა ენა",
        "scene" to "სცენის სათაური",
        "action" to "მოქმედების აღწერა",
        "character" to "პერსონაჟის სახელი",
        "dialogue" to "დიალოგი",
        "custom" to "მორგებული ბლოკი",
        "add_block" to "ბლოკის დამატება",
        "choose_lang_title" to "ენის არჩევა",
        "search_language" to "ენის ძებნა...",
        "save_template" to "შაბლონის შენახვა",
        "saved_templates" to "შენახული შაბლონები",
        "stats_title" to "მწერლის სტატისტიკა",
        "cat_all" to "ყველა",
        "cat_book" to "წიგნი",
        "cat_screenplay" to "სცენარი",
        "cat_story" to "მოთხრობა",
        "cat_text" to "ტექსტი",
        "modified" to "შეცვლილია",
        "restore" to "აღდგენა",
        "delete_permanent" to "სამუდამოდ წაშლა",
        "to_trash" to "ნაგავში გადატანა",
        "password" to "პაროლი",
        "enter_btn" to "შესვლა",
        "exit" to "გასვლა",
        "no_projects_yet" to "პროექტები ჯერ არ არის",
        "create_first_project_desc" to "შექმენით თქვენი პირველი პროექტი წერის დასაწყებად."
    )

    private fun build_hy() = mapOf(
        "my_studio" to "Իմ Ստեղծագործական Ստուդիան",
        "cabinet" to "Աշխատավայր",
        "progress" to "Հաջողություններ",
        "settings" to "Կարգավորումներ",
        "options" to "Ընտրանքներ",
        "new_project" to "Նոր նախագիծ",
        "search" to "Որոնում",
        "active" to "Ակտիվ",
        "projects" to "Նախագծեր",
        "favorites" to "Ընտրյալներ",
        "archive" to "Արխիվ",
        "trash" to "Աղբաման",
        "folders" to "Թղթապանակներ",
        "documents" to "Փաստաթղթեր",
        "create" to "Ստեղծել",
        "cancel" to "Չեղարկել",
        "delete" to "Ջնջել",
        "edit" to "Խմբագրել",
        "versions" to "Տարբերակներ",
        "close" to "Փակել",
        "find_replace" to "Որոնել և փոխարինել",
        "prompter" to "Հուշարար",
        "manual_save" to "Պահպանել",
        "words" to "Բառ",
        "chars" to "Նիշ",
        "pages" to "Էջ",
        "reading_time" to "Ընթերցման ժամանակ",
        "min" to "րոպե",
        "sys_settings" to "Կարգավորումներ",
        "theme" to "Թեմա",
        "theme_light" to "Լուսավոր",
        "theme_dark" to "Մութ",
        "theme_black" to "Սև (OLED)",
        "color_palette" to "Գունային գունապնակ",
        "security" to "Անվտանգություն և PIN",
        "backups" to "Պահուստային պատճեն",
        "export_backup" to "Արտահանել պատճենը",
        "import_backup" to "Ներմուծել պատճենը",
        "app_language" to "Հավելվածի լեզու",
        "lang_system" to "Համակարգային լռելյայն",
        "system_auto_detect" to "Ավտոմատ ճանաչում",
        "all_languages" to "Բոլոր լեզուները",
        "scene" to "Տեսարանի վերնագիր",
        "action" to "Գործողության նկարագրություն",
        "character" to "Կերպարի անունը",
        "dialogue" to "Երկխոսություն",
        "custom" to "Հարմարեցված բլոկ",
        "add_block" to "Ավելացնել բլոկ",
        "choose_lang_title" to "Ընտրեք լեզուն",
        "search_language" to "Փնտրել լեզուն...",
        "save_template" to "Պահպանել ձևանմուշը",
        "saved_templates" to "Պահպանված ձևանմուշներ",
        "stats_title" to "Գրողի վիճակագրություն",
        "cat_all" to "Բոլորը",
        "cat_book" to "Գիրք",
        "cat_screenplay" to "Սցենար",
        "cat_story" to "Պատմվածք",
        "cat_text" to "Տեքստ",
        "modified" to "Փոփոխված է",
        "restore" to "Վերականգնել",
        "delete_permanent" to "Ջնջել ընդմիշտ",
        "to_trash" to "Աղբաման տեղափոխել",
        "password" to "Գաղտնաբառ",
        "enter_btn" to "Մուտք",
        "exit" to "Ելք",
        "no_projects_yet" to "Դեռ նախագծեր չկան",
        "create_first_project_desc" to "Ստեղծեք ձեր առաջին նախագիծը գրել սկսելու համար:"
    )

    private val ru: Map<String, String> by lazy { build_ru() }
    private val en: Map<String, String> by lazy { build_en() }

    private val loadedDictionaries = ConcurrentHashMap<String, Map<String, String>>()

    private fun getDictionaryFor(langCode: String): Map<String, String>? {
        if (langCode == "ru") return ru
        if (langCode == "en") return en
        loadedDictionaries[langCode]?.let { return it }
        val built: Map<String, String>? = when (langCode) {
            "es" -> build_es()
            "fr" -> build_fr()
            "de" -> build_de()
            "it" -> build_it()
            "pt" -> build_pt()
            "zh" -> build_zh()
            "ja" -> build_ja()
            "ko" -> build_ko()
            "uk" -> build_uk()
            "pl" -> build_pl()
            "tr" -> build_tr()
            "ar" -> build_ar()
            "hi" -> build_hi()
            "nl" -> build_nl()
            "sv" -> build_sv()
            "cs" -> build_cs()
            "el" -> build_el()
            "ro" -> build_ro()
            "hu" -> build_hu()
            "da" -> build_da()
            "fi" -> build_fi()
            "no" -> build_no()
            "he" -> build_he()
            "fa" -> build_fa()
            "bn" -> build_bn()
            "th" -> build_th()
            "vi" -> build_vi()
            "id" -> build_id()
            "kk" -> build_kk()
            "be" -> build_be()
            "uz" -> build_uz()
            "az" -> build_az()
            "ka" -> build_ka()
            "hy" -> build_hy()
            else -> null
        }
        if (built != null) {
            loadedDictionaries[langCode] = built
        }
        return built
    }

    private val ruToKey: Map<String, String> by lazy {
        buildMap {
            for ((k, v) in ru) {
                put(v.trim(), k)
                put(v.trim().lowercase(), k)
            }
        }
    }

    private val enToKey: Map<String, String> by lazy {
        buildMap {
            for ((k, v) in en) {
                put(v.trim(), k)
                put(v.trim().lowercase(), k)
            }
        }
    }

    private val studioPhrasesByEn: Map<String, Map<String, String>> by lazy {
        buildStudioPhrasesPart1() + buildStudioPhrasesPart2()
    }

    private fun buildStudioPhrasesPart1(): Map<String, Map<String, String>> = mapOf(
        // CustomPaletteDialog
        "Accent & canvas customization" to mapOf(
            "ru" to "Настройка акцента и холста",
            "uk" to "Налаштування акценту і полотна",
            "be" to "Налада акцэнту і палатна",
            "kk" to "Акцент пен кенепті баптау",
            "es" to "Personalización de acento y lienzo",
            "de" to "Akzent- & Hintergrundanpassung",
            "fr" to "Personnalisation de l'accent et du fond",
            "it" to "Personalizzazione accento e sfondo",
            "pt" to "Personalização de destaque e fundo",
            "pl" to "Personalizacja akcentu i tła",
            "tr" to "Vurgu ve tuval özelleştirme",
            "zh" to "强调色与画布自定义",
            "ja" to "アクセントと背景のカスタマイズ",
            "ko" to "강조색 및 배경 사용자 지정",
            "ar" to "تخصيص اللون المميز والخلفية"
        ),
        "Accent" to mapOf(
            "ru" to "Акцент", "uk" to "Акцент", "be" to "Акцэнт", "kk" to "Акцент",
            "es" to "Acento", "de" to "Akzent", "fr" to "Accent", "it" to "Accento",
            "pt" to "Destaque", "pl" to "Akcent", "tr" to "Vurgu", "zh" to "强调色",
            "ja" to "アクセント", "ko" to "강조", "ar" to "التمييز"
        ),
        "Canvas" to mapOf(
            "ru" to "Фон", "uk" to "Фон", "be" to "Фон", "kk" to "Фон",
            "es" to "Lienzo", "de" to "Hintergrund", "fr" to "Toile", "it" to "Sfondo",
            "pt" to "Fundo", "pl" to "Tło", "tr" to "Tuval", "zh" to "背景",
            "ja" to "背景", "ko" to "배경", "ar" to "الخلفية"
        ),
        "Themes" to mapOf(
            "ru" to "Темы", "uk" to "Теми", "be" to "Тэмы", "kk" to "Тақырыптар",
            "es" to "Temas", "de" to "Themen", "fr" to "Thèmes", "it" to "Temi",
            "pt" to "Temas", "pl" to "Motywy", "tr" to "Temalar", "zh" to "主题",
            "ja" to "テーマ", "ko" to "테마", "ar" to "السمات"
        ),
        "Pastel" to mapOf(
            "ru" to "Пастель", "uk" to "Пастель", "be" to "Пастэль", "kk" to "Пастель",
            "es" to "Pastel", "de" to "Pastell", "fr" to "Pastel", "it" to "Pastello",
            "pt" to "Pastel", "pl" to "Pastelowy", "tr" to "Pastel", "zh" to "柔和",
            "ja" to "パステル", "ko" to "파스텔", "ar" to "باستيل"
        ),
        "Soft" to mapOf(
            "ru" to "Мягкий", "uk" to "М'який", "be" to "Мяккі", "kk" to "Жұмсақ",
            "es" to "Suave", "de" to "Sanft", "fr" to "Doux", "it" to "Morbido",
            "pt" to "Suave", "pl" to "Miękki", "tr" to "Yumuşak", "zh" to "柔光",
            "ja" to "ソフト", "ko" to "부드러움", "ar" to "ناعم"
        ),
        "Vivid" to mapOf(
            "ru" to "Яркий", "uk" to "Яскравий", "be" to "Яркі", "kk" to "Ашық",
            "es" to "Vivo", "de" to "Lebhaft", "fr" to "Vif", "it" to "Vivido",
            "pt" to "Vibrante", "pl" to "Żywy", "tr" to "Canlı", "zh" to "鲜艳",
            "ja" to "ビビッド", "ko" to "선명함", "ar" to "زاهي"
        ),
        "Neon" to mapOf(
            "ru" to "Неон", "uk" to "Неон", "be" to "Неон", "kk" to "Неон",
            "es" to "Neón", "de" to "Neon", "fr" to "Néon", "it" to "Neon",
            "pt" to "Névoa Neon", "pl" to "Neon", "tr" to "Neon", "zh" to "霓虹",
            "ja" to "ネオン", "ko" to "네온", "ar" to "نيون"
        ),
        "Deep" to mapOf(
            "ru" to "Глубокий", "uk" to "Глибокий", "be" to "Глыбокі", "kk" to "Терең",
            "es" to "Profundo", "de" to "Tief", "fr" to "Profond", "it" to "Profondo",
            "pt" to "Profundo", "pl" to "Głęboki", "tr" to "Derin", "zh" to "深邃",
            "ja" to "ディープ", "ko" to "깊은", "ar" to "عميق"
        ),
        "Dark & OLED Surfaces" to mapOf(
            "ru" to "Тёмные и OLED покрытия", "uk" to "Темні та OLED покриття",
            "be" to "Цёмныя і OLED пакрыцці", "kk" to "Күңгірт және OLED беттер",
            "es" to "Superficies oscuras y OLED", "de" to "Dunkle & OLED-Oberflächen",
            "fr" to "Surfaces sombres et OLED", "it" to "Superfici scure e OLED",
            "pt" to "Superfícies escuras e OLED", "pl" to "Ciemne i OLED powierzchnie",
            "tr" to "Koyu ve OLED Yüzeyler", "zh" to "深色与 OLED 表面",
            "ja" to "ダーク & OLED サーフェス", "ko" to "다크 및 OLED 표면", "ar" to "أسطح داكنة و OLED"
        ),
        "Light & Paper Surfaces" to mapOf(
            "ru" to "Светлые и бумажные покрытия", "uk" to "Світлі та паперові покриття",
            "be" to "Светлыя і папяровыя пакрыцці", "kk" to "Ашық және қағаз беттер",
            "es" to "Superficies claras y papel", "de" to "Helle & Papier-Oberflächen",
            "fr" to "Surfaces claires et papier", "it" to "Superfici chiare e carta",
            "pt" to "Superfícies claras e papel", "pl" to "Jasne i papierowe powierzchnie",
            "tr" to "Açık ve Kağıt Yüzeyler", "zh" to "浅色与纸质表面",
            "ja" to "ライト & ペーパー サーフェス", "ko" to "라이트 및 종이 표면", "ar" to "أسطح فاتحة وورقية"
        ),
        "Custom canvas tone" to mapOf(
            "ru" to "Произвольный оттенок фона", "uk" to "Довільний відтінок фону",
            "be" to "Адвольны адценне фону", "kk" to "Еркін фон реңкі",
            "es" to "Tono de fondo personalizado", "de" to "Eigener Hintergrundton",
            "fr" to "Ton de fond personnalisé", "it" to "Tono sfondo personalizzato",
            "pt" to "Tom de fundo personalizado", "pl" to "Własny odcień tła",
            "tr" to "Özel tuval tonu", "zh" to "自定义背景色调",
            "ja" to "カスタム背景トーン", "ko" to "사용자 지정 배경 톤", "ar" to "درجة خلفية مخصصة"
        ),
        "Canvas Hue" to mapOf(
            "ru" to "Оттенок фона", "uk" to "Відтінок фону", "be" to "Адценне фону", "kk" to "Фон реңкі",
            "es" to "Tono del lienzo", "de" to "Hintergrund-Farbton", "fr" to "Teinte du fond", "it" to "Tonalità sfondo",
            "pt" to "Matiz do fundo", "pl" to "Odcień tła", "tr" to "Tuval Tonu", "zh" to "背景色相",
            "ja" to "背景の色相", "ko" to "배경 색상", "ar" to "تدرج الخلفية"
        ),
        "Canvas Lightness" to mapOf(
            "ru" to "Яркость фона", "uk" to "Яскравість фону", "be" to "Яркасць фону", "kk" to "Фон жарықтығы",
            "es" to "Luminosidad del lienzo", "de" to "Hintergrund-Helligkeit", "fr" to "Luminosité du fond", "it" to "Luminosità sfondo",
            "pt" to "Luminosidade do fundo", "pl" to "Jasność tła", "tr" to "Tuval Parlaklığı", "zh" to "背景亮度",
            "ja" to "背景の明度", "ko" to "배경 밝기", "ar" to "سطوع الخلفية"
        ),
        "No saved palettes yet" to mapOf(
            "ru" to "Нет сохраненных палитр", "uk" to "Немає збережених палітр",
            "be" to "Няма захаваных палітраў", "kk" to "Сақталған палитралар жоқ",
            "es" to "Aún no hay paletas guardadas", "de" to "Noch keine gespeicherten Paletten",
            "fr" to "Aucune palette enregistrée", "it" to "Nessuna tavolozza salvata",
            "pt" to "Nenhuma paleta salva", "pl" to "Brak zapisanych palet",
            "tr" to "Henüz kayıtlı palet yok", "zh" to "暂无保存的调色板",
            "ja" to "保存されたパレットはありません", "ko" to "저장된 팔레트가 없습니다", "ar" to "لا توجد لوحات ألوان محفوظة"
        ),
        "Customize colors and save your theme." to mapOf(
            "ru" to "Настройте акцент и фон, затем сохраните тему.",
            "uk" to "Налаштуйте акцент і фон, а потім збережіть тему.",
            "be" to "Наладзьце акцэнт і фон, затым захавайце тэму.",
            "kk" to "Акцент пен фонды баптап, тақырыпты сақтаңыз.",
            "es" to "Personaliza los colores y guarda tu tema.",
            "de" to "Passen Sie die Farben an und speichern Sie Ihr Thema.",
            "fr" to "Personnalisez les couleurs et enregistrez votre thème.",
            "it" to "Personalizza i colori e salva il tuo tema.",
            "pt" to "Personalize as cores e salve seu tema.",
            "pl" to "Dostosuj kolory i zapisz swój motyw.",
            "tr" to "Renkleri özelleştirin ve temanızı kaydedin.",
            "zh" to "自定义颜色并保存您的主题。",
            "ja" to "色をカスタマイズしてテーマを保存します。",
            "ko" to "색상을 사용자 지정하고 테마를 저장하세요.",
            "ar" to "خصص الألوان واحفظ سمتك."
        ),
        "Live" to mapOf(
            "ru" to "Стиль", "uk" to "Стиль", "be" to "Стыль", "kk" to "Стиль",
            "es" to "Vista", "de" to "Vorschau", "fr" to "Aperçu", "it" to "Live",
            "pt" to "Estilo", "pl" to "Styl", "tr" to "Canlı", "zh" to "预览",
            "ja" to "プレビュー", "ko" to "미리보기", "ar" to "معاينة"
        ),
        "Active" to mapOf(
            "ru" to "Активна", "uk" to "Активна", "be" to "Актыўная", "kk" to "Белсенді",
            "es" to "Activo", "de" to "Aktiv", "fr" to "Actif", "it" to "Attivo",
            "pt" to "Ativo", "pl" to "Aktywny", "tr" to "Aktif", "zh" to "当前",
            "ja" to "有効", "ko" to "활성", "ar" to "نشط"
        ),
        "Use" to mapOf(
            "ru" to "Выбрать", "uk" to "Обрати", "be" to "Выбраць", "kk" to "Таңдау",
            "es" to "Usar", "de" to "Wählen", "fr" to "Utiliser", "it" to "Usa",
            "pt" to "Usar", "pl" to "Użyj", "tr" to "Kullan", "zh" to "使用",
            "ja" to "適用", "ko" to "사용", "ar" to "استخدام"
        ),
        // CustomBlockConfigDialog
        "Custom Block Studio" to mapOf(
            "ru" to "Конструктор блока", "uk" to "Конструктор блоку",
            "be" to "Канструктар блока", "kk" to "Блок конструкторы",
            "es" to "Estudio de bloques", "de" to "Block-Studio",
            "fr" to "Studio de bloc", "it" to "Studio blocco",
            "pt" to "Estúdio de bloco", "pl" to "Kreator bloku",
            "tr" to "Özel Blok Stüdyosu", "zh" to "自定义块工作室",
            "ja" to "カスタムブロックスタジオ", "ko" to "커스텀 블록 스튜디오", "ar" to "استوديو الكتل المخصصة"
        ),
        "Presets, badge, style & templates" to mapOf(
            "ru" to "Пресеты, метка, стиль и шаблоны",
            "uk" to "Пресети, мітка, стиль і шаблони",
            "be" to "Прэсеты, метка, стыль і шаблоны",
            "kk" to "Пресеттер, белгі, стиль және үлгілер",
            "es" to "Ajustes, etiqueta, estilo y plantillas",
            "de" to "Vorlagen, Abzeichen, Stil & Muster",
            "fr" to "Préréglages, badge, style et modèles",
            "it" to "Preset, badge, stile e modelli",
            "pt" to "Predefinições, emblema, estilo e modelos",
            "pl" to "Presety, etykieta, styl i szablony",
            "tr" to "Ön ayarlar, rozet, stil ve şablonlar",
            "zh" to "预设、徽章、样式与模板",
            "ja" to "プリセット・バッジ・スタイル・テンプレート",
            "ko" to "프리셋, 배지, 스타일 및 템플릿",
            "ar" to "الإعدادات المسبقة والشارة والنمط والقوالب"
        ),
        "Identity" to mapOf(
            "ru" to "Пресеты и метка", "uk" to "Пресети і мітка", "be" to "Прэсеты і метка", "kk" to "Пресет және белгі",
            "es" to "Identidad", "de" to "Identität", "fr" to "Identité", "it" to "Identità",
            "pt" to "Identidade", "pl" to "Etykieta", "tr" to "Kimlik", "zh" to "标识",
            "ja" to "ラベル", "ko" to "식별", "ar" to "الهوية"
        ),
        "Style" to mapOf(
            "ru" to "Стиль и цвет", "uk" to "Стиль і колір", "be" to "Стыль і колер", "kk" to "Стиль мен түс",
            "es" to "Estilo y color", "de" to "Stil & Farbe", "fr" to "Style et couleur", "it" to "Stile e colore",
            "pt" to "Estilo e cor", "pl" to "Styl i kolor", "tr" to "Stil ve Renk", "zh" to "样式与颜色",
            "ja" to "スタイル", "ko" to "스타일", "ar" to "النمط واللون"
        ),
        "Templates" to mapOf(
            "ru" to "Шаблоны", "uk" to "Шаблони", "be" to "Шаблоны", "kk" to "Үлгілер",
            "es" to "Plantillas", "de" to "Vorlagen", "fr" to "Modèles", "it" to "Modelli",
            "pt" to "Modelos", "pl" to "Szablony", "tr" to "Şablonlar", "zh" to "模板",
            "ja" to "テンプレート", "ko" to "템플릿", "ar" to "القوالب"
        ),
        "Template" to mapOf(
            "ru" to "Шаблон", "uk" to "Шаблон", "be" to "Шаблон", "kk" to "Үлгі",
            "es" to "Plantilla", "de" to "Vorlage", "fr" to "Modèle", "it" to "Modello",
            "pt" to "Modelo", "pl" to "Szablon", "tr" to "Şablon", "zh" to "模板",
            "ja" to "テンプレート", "ko" to "템플릿", "ar" to "قالب"
        ),
        "Apply Block" to mapOf(
            "ru" to "Применить", "uk" to "Застосувати", "be" to "Прымяніць", "kk" to "Қолдану",
            "es" to "Aplicar", "de" to "Anwenden", "fr" to "Appliquer", "it" to "Applica",
            "pt" to "Aplicar", "pl" to "Zastosuj", "tr" to "Uygula", "zh" to "应用",
            "ja" to "適用", "ko" to "적용", "ar" to "تطبيق"
        ),
        "Sample text of your screenplay block..." to mapOf(
            "ru" to "Пример текста вашего блока в сценарии...",
            "uk" to "Приклад тексту вашого блоку в сценарії...",
            "be" to "Прыклад тэксту вашага блока ў сцэнарыі...",
            "kk" to "Сценарийдегі блок мәтінінің үлгісі...",
            "es" to "Texto de ejemplo de tu bloque de guion...",
            "de" to "Beispieltext Ihres Drehbuchblocks...",
            "fr" to "Exemple de texte de votre bloc de scénario...",
            "it" to "Testo di esempio del blocco sceneggiatura...",
            "pt" to "Texto de exemplo do seu bloco de roteiro...",
            "pl" to "Przykładowy tekst Twojego bloku scenariusza...",
            "tr" to "Senaryo bloğunuzun örnek metni...",
            "zh" to "剧本块示例文本...",
            "ja" to "脚本ブロックのサンプルテキスト...",
            "ko" to "시나리오 블록 샘플 텍스트...",
            "ar" to "نص تجريبي لكتلة السيناريو..."
        ),
        "Quick Screenplay Presets" to mapOf(
            "ru" to "Сценарные пресеты", "uk" to "Сценарні пресети",
            "be" to "Сцэнарныя прэсеты", "kk" to "Сценарий пресеттері",
            "es" to "Ajustes rápidos de guion", "de" to "Drehbuch-Schnellvorlagen",
            "fr" to "Préréglages de scénario", "it" to "Preset sceneggiatura",
            "pt" to "Predefinições de roteiro", "pl" to "Szybkie presety scenariusza",
            "tr" to "Hızlı Senaryo Ön Ayarları", "zh" to "快速剧本预设",
            "ja" to "クイック脚本プリセット", "ko" to "빠른 시나리오 프리셋", "ar" to "إعدادات السيناريو السريعة"
        ),
        "Badge Label & Prefix" to mapOf(
            "ru" to "Метка и префикс", "uk" to "Мітка та префікс",
            "be" to "Метка і прэфікс", "kk" to "Белгі және префикс",
            "es" to "Etiqueta y prefijo", "de" to "Abzeichen & Präfix",
            "fr" to "Badge et préfixe", "it" to "Etichetta e prefisso",
            "pt" to "Rótulo e prefixo", "pl" to "Etykieta i prefiks",
            "tr" to "Rozet Etiketi ve Önek", "zh" to "徽章标签与前缀",
            "ja" to "バッジとプレフィックス", "ko" to "배지 라벨 및 접두사", "ar" to "تسمية الشارة والبادئة"
        ),
        "Label (V.O.)" to mapOf(
            "ru" to "Метка (З.К.)", "uk" to "Мітка (З.К.)", "be" to "Метка (З.К.)", "kk" to "Белгі (К.С.)",
            "es" to "Etiqueta (V.O.)", "de" to "Label (V.O.)", "fr" to "Label (V.O.)", "it" to "Etichetta (V.O.)",
            "pt" to "Rótulo (V.O.)", "pl" to "Etykieta (V.O.)", "tr" to "Etiket (V.O.)", "zh" to "标签 (画外音)",
            "ja" to "ラベル (V.O.)", "ko" to "라벨 (V.O.)", "ar" to "الشارة"
        ),
        "Prefix ([V.O.])" to mapOf(
            "ru" to "Префикс ([З.К.])", "uk" to "Префікс ([З.К.])", "be" to "Прэфікс ([З.К.])", "kk" to "Префикс ([К.С.])",
            "es" to "Prefijo ([V.O.])", "de" to "Präfix ([V.O.])", "fr" to "Préfixe ([V.O.])", "it" to "Prefisso ([V.O.])",
            "pt" to "Prefixo ([V.O.])", "pl" to "Prefiks ([V.O.])", "tr" to "Önek ([V.O.])", "zh" to "前缀 ([画外音])",
            "ja" to "プレフィックス ([V.O.])", "ko" to "접두사 ([V.O.])", "ar" to "البادئة"
        ),
        "Block Icon" to mapOf(
            "ru" to "Иконка блока", "uk" to "Іконка блоку", "be" to "Іконка блока", "kk" to "Блок белгішесі",
            "es" to "Icono del bloque", "de" to "Block-Symbol", "fr" to "Icône du bloc", "it" to "Icona blocco",
            "pt" to "Ícone do bloco", "pl" to "Ikona bloku", "tr" to "Blok Simgesi", "zh" to "块图标",
            "ja" to "ブロックアイコン", "ko" to "블록 아이콘", "ar" to "أيقونة الكتلة"
        ),
        "Block Content" to mapOf(
            "ru" to "Текст блока", "uk" to "Текст блоку", "be" to "Тэкст блока", "kk" to "Блок мәтіні",
            "es" to "Contenido del bloque", "de" to "Blockinhalt", "fr" to "Contenu du bloc", "it" to "Contenuto blocco",
            "pt" to "Conteúdo do bloco", "pl" to "Treść bloku", "tr" to "Blok İçeriği", "zh" to "块内容",
            "ja" to "ブロック内容", "ko" to "블록 내용", "ar" to "محتوى الكتلة"
        ),
        "Type block text..." to mapOf(
            "ru" to "Введите текст блока...", "uk" to "Введіть текст блоку...",
            "be" to "Увядзіце тэкст блока...", "kk" to "Блок мәтінін енгізіңіз...",
            "es" to "Escribe el texto del bloque...", "de" to "Blocktext eingeben...",
            "fr" to "Saisissez le texte du bloc...", "it" to "Digita il testo del blocco...",
            "pt" to "Digite o texto do bloco...", "pl" to "Wpisz tekst bloku...",
            "tr" to "Blok metnini yazın...", "zh" to "输入块文本...",
            "ja" to "ブロックテキストを入力...", "ko" to "블록 텍스트 입력...", "ar" to "اكتب نص الكتلة..."
        ),
        "Font & Formatting" to mapOf(
            "ru" to "Шрифт и форматирование", "uk" to "Шрифт і форматування",
            "be" to "Шрыфт і фарматаванне", "kk" to "Қаріп және пішімдеу",
            "es" to "Fuente y formato", "de" to "Schriftart & Formatierung",
            "fr" to "Police et formatage", "it" to "Carattere e formattazione",
            "pt" to "Fonte e formatação", "pl" to "Czcionka i formatowanie",
            "tr" to "Yazı Tipi ve Biçimlendirme", "zh" to "字体与格式",
            "ja" to "フォントとフォーマット", "ko" to "글꼴 및 서식", "ar" to "الخط والتنسيق"
        ),
        "Serif" to mapOf(
            "ru" to "С засечками", "uk" to "Із зарубками", "be" to "З засечкамі", "kk" to "Кертікшелі",
            "es" to "Con serifa", "de" to "Serifen", "fr" to "Empattement", "it" to "Serif",
            "pt" to "Serifada", "pl" to "Szeryfowa", "tr" to "Serif", "zh" to "衬线",
            "ja" to "明朝体", "ko" to "명조", "ar" to "مذيل"
        ),
        "Sans" to mapOf(
            "ru" to "Без засечек", "uk" to "Без зарубок", "be" to "Без засечак", "kk" to "Кертікшесіз",
            "es" to "Sin serifa", "de" to "Serifenlos", "fr" to "Sans empatt.", "it" to "Sans",
            "pt" to "Sem serifa", "pl" to "Bezszeryf.", "tr" to "Sans", "zh" to "无衬线",
            "ja" to "ゴシック", "ko" to "고딕", "ar" to "بسيط"
        ),
        "Mono" to mapOf(
            "ru" to "Сценарный", "uk" to "Сценарний", "be" to "Сцэнарны", "kk" to "Сценарийлік",
            "es" to "Guion", "de" to "Drehbuch", "fr" to "Scénario", "it" to "Copione",
            "pt" to "Roteiro", "pl" to "Scenariusz", "tr" to "Senaryo", "zh" to "等宽剧本",
            "ja" to "等幅", "ko" to "고정폭", "ar" to "أحادي"
        ),
        "Marker Color" to mapOf(
            "ru" to "Цвет маркера", "uk" to "Колір маркера", "be" to "Колер маркера", "kk" to "Маркер түсі",
            "es" to "Color del marcador", "de" to "Markierungsfarbe", "fr" to "Couleur du marqueur", "it" to "Colore marcatore",
            "pt" to "Cor do marcador", "pl" to "Kolor znacznika", "tr" to "İşaretleyici Rengi", "zh" to "标记颜色",
            "ja" to "マーカー色", "ko" to "마커 색상", "ar" to "لون العلامة"
        ),
        "Sliders" to mapOf(
            "ru" to "Спектр", "uk" to "Спектр", "be" to "Спектр", "kk" to "Спектр",
            "es" to "Espectro", "de" to "Spektrum", "fr" to "Spectre", "it" to "Spettro",
            "pt" to "Espectro", "pl" to "Spektrum", "tr" to "Spektrum", "zh" to "滑块",
            "ja" to "スライダー", "ko" to "슬라이더", "ar" to "شرائح"
        ),
        "Save Current Block as Template" to mapOf(
            "ru" to "Сохранить текущий вид как шаблон", "uk" to "Зберегти поточний вигляд як шаблон",
            "be" to "Захаваць бягучы выгляд як шаблон", "kk" to "Ағымдағы блокты үлгі ретінде сақтау",
            "es" to "Guardar bloque actual como plantilla", "de" to "Aktuellen Block als Vorlage speichern",
            "fr" to "Enregistrer ce bloc comme modèle", "it" to "Salva blocco corrente come modello",
            "pt" to "Salvar bloco atual como modelo", "pl" to "Zapisz bieżący blok jako szablon",
            "tr" to "Mevcut Bloğu Şablon Olarak Kaydet", "zh" to "将当前块保存为模板",
            "ja" to "現在のブロックをテンプレート保存", "ko" to "현재 블록을 템플릿으로 저장", "ar" to "حفظ الكتلة الحالية كقالب"
        ),
        "Defaults" to mapOf(
            "ru" to "Базовые", "uk" to "Базові", "be" to "Базавыя", "kk" to "Базалық",
            "es" to "Base", "de" to "Standard", "fr" to "Défaut", "it" to "Base",
            "pt" to "Padrão", "pl" to "Domyślne", "tr" to "Varsayılan", "zh" to "默认",
            "ja" to "標準", "ko" to "기본값", "ar" to "الافتراضي"
        ),
        "Default templates restored" to mapOf(
            "ru" to "Базовые шаблоны добавлены", "uk" to "Базові шаблони додано",
            "be" to "Базавыя шаблоны дададзены", "kk" to "Базалық үлгілер қосылды",
            "es" to "Plantillas predeterminadas restauradas", "de" to "Standardvorlagen wiederhergestellt",
            "fr" to "Modèles par défaut restaurés", "it" to "Modelli predefiniti ripristinati",
            "pt" to "Modelos padrão restaurados", "pl" to "Przywrócono domyślne szablony",
            "tr" to "Varsayılan şablonlar geri yüklendi", "zh" to "已恢复默认模板",
            "ja" to "標準テンプレートを復元しました", "ko" to "기본 템플릿이 복원되었습니다", "ar" to "تمت استعادة القوالب الافتراضية"
        ),
        "No saved templates yet" to mapOf(
            "ru" to "Нет сохранённых шаблонов", "uk" to "Немає збережених шаблонів",
            "be" to "Няма захаваных шаблонаў", "kk" to "Сақталған үлгілер жоқ",
            "es" to "Aún no hay plantillas guardadas", "de" to "Noch keine gespeicherten Vorlagen",
            "fr" to "Aucun modèle enregistré", "it" to "Nessun modello salvato",
            "pt" to "Nenhum modelo salvo", "pl" to "Brak zapisanych szablonów",
            "tr" to "Henüz kayıtlı şablon yok", "zh" to "暂无保存的模板",
            "ja" to "保存されたテンプレートはありません", "ko" to "저장된 템플릿이 없습니다", "ar" to "لا توجد قوالب محفوظة"
        ),
        "Open Custom Block Studio" to mapOf(
            "ru" to "Открыть конструктор блока", "uk" to "Відкрити конструктор блоку",
            "be" to "Адкрыць канструктар блока", "kk" to "Блок конструкторын ашу",
            "es" to "Abrir estudio de bloques", "de" to "Block-Studio öffnen",
            "fr" to "Ouvrir le studio de bloc", "it" to "Apri studio blocco",
            "pt" to "Abrir estúdio de bloco", "pl" to "Otwórz kreator bloku",
            "tr" to "Özel Blok Stüdyosunu Aç", "zh" to "打开自定义块工作室",
            "ja" to "カスタムブロックスタジオを開く", "ko" to "커스텀 블록 스튜디오 열기", "ar" to "فتح استوديو الكتل المخصصة"
        ),
        // Account & Cloud Profile Dialog
        "Account & Cloud" to mapOf(
            "ru" to "Аккаунт и Cloud", "uk" to "Акаунт і Cloud", "be" to "Акаўнт і Cloud", "kk" to "Аккаунт және Cloud",
            "es" to "Cuenta y Nube", "de" to "Konto & Cloud", "fr" to "Compte & Cloud", "it" to "Account & Cloud",
            "pt" to "Conta e Nuvem", "pl" to "Konto i Chmura", "tr" to "Hesap ve Bulut", "zh" to "账户与云端",
            "ja" to "アカウントとクラウド", "ko" to "계정 및 클라우드", "ar" to "الحساب والسحابة"
        ),
        "Profile" to mapOf(
            "ru" to "Профиль", "uk" to "Профіль", "be" to "Профіль", "kk" to "Профиль",
            "es" to "Perfil", "de" to "Profil", "fr" to "Profil", "it" to "Profilo",
            "pt" to "Perfil", "pl" to "Profil", "tr" to "Profil", "zh" to "个人资料",
            "ja" to "プロフィール", "ko" to "프로필", "ar" to "الملف الشخصي"
        ),
        "Stats" to mapOf(
            "ru" to "Статистика", "uk" to "Статистика", "be" to "Статыстыка", "kk" to "Статистика",
            "es" to "Estadísticas", "de" to "Statistik", "fr" to "Stats", "it" to "Statistiche",
            "pt" to "Estatísticas", "pl" to "Statystyki", "tr" to "İstatistik", "zh" to "统计",
            "ja" to "統計", "ko" to "통계", "ar" to "الإحصائيات"
        ),
        "Google Drive Sync" to mapOf(
            "ru" to "Google Drive Синхронизация", "uk" to "Синхронізація Google Drive",
            "be" to "Сінхранізацыя Google Drive", "kk" to "Google Drive синхрондау",
            "es" to "Sincronización con Google Drive", "de" to "Google Drive-Synchronisierung",
            "fr" to "Synchronisation Google Drive", "it" to "Sincronizzazione Google Drive",
            "pt" to "Sincronização do Google Drive", "pl" to "Synchronizacja Google Drive",
            "tr" to "Google Drive Senkronizasyonu", "zh" to "Google Drive 同步",
            "ja" to "Google ドライブ同期", "ko" to "Google 드라이브 동기화", "ar" to "مزامنة Google Drive"
        ),
        "Syncing..." to mapOf(
            "ru" to "Синхронизация...", "uk" to "Синхронізація...", "be" to "Сінхранізацыя...", "kk" to "Синхрондалуда...",
            "es" to "Sincronizando...", "de" to "Synchronisiere...", "fr" to "Synchronisation...", "it" to "Sincronizzazione...",
            "pt" to "Sincronizando...", "pl" to "Synchronizacja...", "tr" to "Senkronize ediliyor...", "zh" to "正在同步...",
            "ja" to "同期中...", "ko" to "동기화 중...", "ar" to "جارٍ المزامنة..."
        ),
        "Connected to Google" to mapOf(
            "ru" to "Подключено к Google", "uk" to "Підключено до Google",
            "be" to "Падключана да Google", "kk" to "Google-ға қосылған",
            "es" to "Conectado a Google", "de" to "Mit Google verbunden",
            "fr" to "Connecté à Google", "it" to "Connesso a Google",
            "pt" to "Conectado ao Google", "pl" to "Połączono z Google",
            "tr" to "Google'a Bağlandı", "zh" to "已连接到 Google",
            "ja" to "Google に接続済み", "ko" to "Google에 연결됨", "ar" to "متصل بـ Google"
        ),
        "Disconnect" to mapOf(
            "ru" to "Выйти", "uk" to "Вийти", "be" to "Выйсці", "kk" to "Шығу",
            "es" to "Desconectar", "de" to "Trennen", "fr" to "Déconnecter", "it" to "Disconnetti",
            "pt" to "Desconectar", "pl" to "Rozłącz", "tr" to "Bağlantıyı Kes", "zh" to "断开连接",
            "ja" to "切断", "ko" to "연결 해제", "ar" to "قطع الاتصال"
        ),
        "Cloud Backup & Sync" to mapOf(
            "ru" to "Облачное резервное копирование", "uk" to "Хмарне резервне копіювання",
            "be" to "Воблачнае рэзервовае капіяванне", "kk" to "Бұлттық сақтық көшірме",
            "es" to "Copia de seguridad en la nube", "de" to "Cloud-Backup & Synchronisierung",
            "fr" to "Sauvegarde et synchro cloud", "it" to "Backup e sincronizzazione cloud",
            "pt" to "Backup e sincronização na nuvem", "pl" to "Kopia zapasowa w chmurze",
            "tr" to "Bulut Yedekleme ve Senkronizasyon", "zh" to "云端备份与同步",
            "ja" to "クラウドバックアップと同期", "ko" to "클라우드 백업 및 동기화", "ar" to "النسخ الاحتياطي والمزامنة السحابية"
        ),
        "Sign in with Google" to mapOf(
            "ru" to "Войти через Google", "uk" to "Увійти через Google",
            "be" to "Увайсці праз Google", "kk" to "Google арқылы кіру",
            "es" to "Iniciar sesión con Google", "de" to "Mit Google anmelden",
            "fr" to "Se connecter avec Google", "it" to "Accedi con Google",
            "pt" to "Entrar com o Google", "pl" to "Zaloguj się przez Google",
            "tr" to "Google ile Giriş Yap", "zh" to "使用 Google 账号登录",
            "ja" to "Google でログイン", "ko" to "Google 계정으로 로그인", "ar" to "تسجيل الدخول باستخدام Google"
        ),
        "Author Name" to mapOf(
            "ru" to "Имя автора", "uk" to "Ім'я автора", "be" to "Імя аўтара", "kk" to "Автордың аты",
            "es" to "Nombre del autor", "de" to "Autorenname", "fr" to "Nom de l'auteur", "it" to "Nome autore",
            "pt" to "Nome do autor", "pl" to "Imię autora", "tr" to "Yazar Adı", "zh" to "作者姓名",
            "ja" to "著者名", "ko" to "작가 이름", "ar" to "اسم المؤلف"
        ),
        "Bio" to mapOf(
            "ru" to "О себе", "uk" to "Про себе", "be" to "Пра сябе", "kk" to "Өзі туралы",
            "es" to "Biografía", "de" to "Biografie", "fr" to "Biographie", "it" to "Biografia",
            "pt" to "Biografia", "pl" to "O mnie", "tr" to "Hakkında", "zh" to "个人简介",
            "ja" to "自己紹介", "ko" to "소개", "ar" to "نبذة"
        ),
        // Editor & Screenplay Headings
        "SCENE HEADING" to mapOf(
            "ru" to "ЗАГОЛОВОК СЦЕНЫ", "uk" to "ЗАГОЛОВОК СЦЕНИ", "be" to "ЗАГАЛОВАК СЦЭНЫ", "kk" to "САХНА ТАҚЫРЫБЫ",
            "es" to "ENCABEZADO DE ESCENA", "de" to "SZENENÜBERSCHRIFT", "fr" to "INTITULÉ DE SCÈNE", "it" to "INTESTAZIONE SCENA",
            "pt" to "CABEÇALHO DE CENA", "pl" to "NAGŁÓWEK SCENY", "tr" to "SAHNE BAŞLIĞI", "zh" to "场景标题",
            "ja" to "シーン見出し", "ko" to "장면 제목", "ar" to "عنوان المشهد"
        ),
        "ACTION" to mapOf(
            "ru" to "ОПИСАНИЕ ДЕЙСТВИЯ", "uk" to "ОПИС ДІЇ", "be" to "АПІСАННЕ ДЗЕЯННЯ", "kk" to "ӘРЕКЕТ СИПАТТАМАСЫ",
            "es" to "ACCIÓN", "de" to "HANDLUNG", "fr" to "ACTION", "it" to "AZIONE",
            "pt" to "AÇÃO", "pl" to "AKCJA", "tr" to "AKSİYON", "zh" to "动作描述",
            "ja" to "ト書き・アクション", "ko" to "지문 / 액션", "ar" to "وصف الحدث"
        ),
        "CHARACTER" to mapOf(
            "ru" to "ИМЯ ПЕРСОНАЖА", "uk" to "ІМ'Я ПЕРСОНАЖА", "be" to "ІМЯ ПЕРСАНАЖА", "kk" to "КЕЙІПКЕР АТЫ",
            "es" to "PERSONAJE", "de" to "FIGUR", "fr" to "PERSONNAGE", "it" to "PERSONAGGIO",
            "pt" to "PERSONAGEM", "pl" to "POSTAĆ", "tr" to "KARAKTER", "zh" to "角色名称",
            "ja" to "キャラクター名", "ko" to "등장인물", "ar" to "اسم الشخصية"
        ),
        "DIALOGUE" to mapOf(
            "ru" to "ДИАЛОГ", "uk" to "ДІАЛОГ", "be" to "ДЫЯЛОГ", "kk" to "ДИАЛОГ",
            "es" to "DIÁLOGO", "de" to "DIALOG", "fr" to "DIALOGUE", "it" to "DIALOGO",
            "pt" to "DIÁLOGO", "pl" to "DIALOG", "tr" to "DİYALOG", "zh" to "对白",
            "ja" to "セリフ", "ko" to "대사", "ar" to "حوار"
        ),
        "CUSTOM BLOCK" to mapOf(
            "ru" to "КАСТОМНЫЙ БЛОК", "uk" to "ВЛАСНИЙ БЛОК", "be" to "КАСТОМНЫ БЛОК", "kk" to "АРНАЙЫ БЛОК",
            "es" to "BLOQUE PERSONALIZADO", "de" to "EIGENER BLOCK", "fr" to "BLOC PERSONNALISÉ", "it" to "BLOCCO PERSONALIZZATO",
            "pt" to "BLOCO PERSONALIZADO", "pl" to "BLOK WŁASNY", "tr" to "ÖZEL BLOK", "zh" to "自定义块",
            "ja" to "カスタムブロック", "ko" to "커스텀 블록", "ar" to "كتلة مخصصة"
        ),
        "Synchronized with your device language" to mapOf(
            "ru" to "Синхронизировано с языком вашего устройства",
            "uk" to "Синхронізовано з мовою вашого пристрою",
            "be" to "Сінхранізавана з мовай вашай прылады",
            "kk" to "Құрылғы тілімен синхрондалған",
            "es" to "Sincronizado con el idioma de tu dispositivo",
            "de" to "Synchronisiert mit Ihrer Gerätesprache",
            "fr" to "Synchronisé avec la langue de votre appareil",
            "it" to "Sincronizzato con la lingua del dispositivo",
            "pt" to "Sincronizado com o idioma do dispositivo",
            "pl" to "Zsynchronizowano z językiem urządzenia",
            "tr" to "Cihazınızın diliyle senkronize edildi",
            "zh" to "已与您的设备语言同步",
            "ja" to "デバイスの言語と同期済み",
            "ko" to "기기 언어와 동기화됨",
            "ar" to "تمت المزامنة مع لغة جهازك"
        ),
        "Choose from 37 world languages..." to mapOf(
            "ru" to "Выбрать из 37 языков мира...",
            "uk" to "Обрати з 37 мов світу...",
            "be" to "Выбраць з 37 моў свету...",
            "kk" to "Әлемнің 37 тілінен таңдау...",
            "es" to "Elegir entre 37 idiomas del mundo...",
            "de" to "Aus 37 Weltsprachen wählen...",
            "fr" to "Choisir parmi 37 langues du monde...",
            "it" to "Scegli tra 37 lingue del mondo...",
            "pt" to "Escolher entre 37 idiomas do mundo...",
            "pl" to "Wybierz spośród 37 języków świata...",
            "tr" to "37 dünya dili arasından seçin...",
            "zh" to "从全球 37 种语言中选择...",
            "ja" to "世界37言語から選択...",
            "ko" to "전 세계 37개 언어 중에서 선택...",
            "ar" to "اختر من بين 37 لغة عالمية..."
        ),
        "37 world languages • Instant sync" to mapOf(
            "ru" to "37 языков мира • Мгновенная синхронизация",
            "uk" to "37 мов світу • Миттєва синхронізація",
            "be" to "37 моў свету • Імгненная сінхранізацыя",
            "kk" to "37 әлем тілі • Лезде синхрондау",
            "es" to "37 idiomas • Sincronización instantánea",
            "de" to "37 Weltsprachen • Sofort-Synchronisierung",
            "fr" to "37 langues • Synchronisation instantanée",
            "it" to "37 lingue • Sincronizzazione istantanea",
            "pt" to "37 idiomas • Sincronização instantânea",
            "pl" to "37 języków • Natychmiastowa synchronizacja",
            "tr" to "37 dünya dili • Anında senkronizasyon",
            "zh" to "37 种语言 • 即时同步",
            "ja" to "37言語 • 即時同期",
            "ko" to "37개 언어 • 즉시 동기화",
            "ar" to "37 لغة عالمية • مزامنة فورية"
        ),
        "Auto-detects device system language" to mapOf(
            "ru" to "Автоопределение языка устройства",
            "uk" to "Автовизначення мови пристрою",
            "be" to "Аўтавызначэнне мовы прылады",
            "kk" to "Құрылғы тілін автоанықтау",
            "es" to "Detecta automáticamente el idioma del sistema",
            "de" to "Erkennt automatisch die Systemsprache",
            "fr" to "Détecte automatiquement la langue du système",
            "it" to "Rileva automaticamente la lingua di sistema",
            "pt" to "Detecta automaticamente o idioma do sistema",
            "pl" to "Automatycznie wykrywa język systemu",
            "tr" to "Cihaz sistem dilini otomatik algılar",
            "zh" to "自动检测设备系统语言",
            "ja" to "端末のシステム言語を自動検出",
            "ko" to "기기 시스템 언어 자동 감지",
            "ar" to "يكتشف لغة نظام الجهاز تلقائيًا"
        ),
        "Popular" to mapOf(
            "ru" to "Популярные", "uk" to "Популярні", "be" to "Папулярныя", "kk" to "Танымал",
            "es" to "Populares", "de" to "Beliebt", "fr" to "Populaires", "it" to "Popolari",
            "pt" to "Populares", "pl" to "Popularne", "tr" to "Popüler", "zh" to "热门",
            "ja" to "人気", "ko" to "인기", "ar" to "الشائعة"
        ),
        "Europe" to mapOf(
            "ru" to "Европа", "uk" to "Європа", "be" to "Еўропа", "kk" to "Еуропа",
            "es" to "Europa", "de" to "Europa", "fr" to "Europe", "it" to "Europa",
            "pt" to "Europa", "pl" to "Europa", "tr" to "Avrupa", "zh" to "欧洲",
            "ja" to "ヨーロッパ", "ko" to "유럽", "ar" to "أوروبا"
        ),
        "Asia" to mapOf(
            "ru" to "Азия", "uk" to "Азія", "be" to "Азія", "kk" to "Азия",
            "es" to "Asia", "de" to "Asien", "fr" to "Asie", "it" to "Asia",
            "pt" to "Ásia", "pl" to "Azja", "tr" to "Asya", "zh" to "亚洲",
            "ja" to "アジア", "ko" to "아시아", "ar" to "آسيا"
        )
    )

    private fun buildStudioPhrasesPart2(): Map<String, Map<String, String>> = mapOf(
        // Add Block Dialog & Editor
        "Add Screenplay Block" to mapOf(
            "ru" to "Добавить блок в сценарий", "uk" to "Додати блок у сценарій", "be" to "Дадаць блок у сцэнарый", "kk" to "Сценарийге блок қосу",
            "es" to "Añadir bloque de guion", "de" to "Drehbuchblock hinzufügen", "fr" to "Ajouter un bloc de scénario", "it" to "Aggiungi blocco sceneggiatura",
            "pt" to "Adicionar bloco de roteiro", "pl" to "Dodaj blok scenariusza", "tr" to "Senaryo Bloğu Ekle", "zh" to "添加剧本块", "ja" to "脚本ブロックを追加", "ko" to "시나리오 블록 추가", "ar" to "إضافة كتلة سيناريو"
        ),
        "Standard element, custom block or template" to mapOf(
            "ru" to "Стандартный элемент, кастомный блок или шаблон", "uk" to "Стандартний елемент, власний блок або шаблон", "be" to "Стандартны элемент, кастомны блок або шаблон", "kk" to "Стандартты элемент, арнайы блок немесе үлгі",
            "es" to "Elemento estándar, bloque personalizado o plantilla", "de" to "Standardelement, eigener Block oder Vorlage", "fr" to "Élément standard, bloc personnalisé ou modèle", "it" to "Elemento standard, blocco personalizzato o modello",
            "pt" to "Elemento padrão, bloco personalizado ou modelo", "pl" to "Standardowy element, własny blok lub szablon", "tr" to "Standart öğe, özel blok veya şablon", "zh" to "标准元素、自定义块或模板", "ja" to "標準要素・カスタムブロック・テンプレート", "ko" to "표준 요소, 커스텀 블록 또는 템플릿"
        ),
        "Custom Block" to mapOf(
            "ru" to "Кастомный блок", "uk" to "Власний блок", "be" to "Кастомны блок", "kk" to "Арнайы блок",
            "es" to "Bloque personalizado", "de" to "Eigener Block", "fr" to "Bloc personnalisé", "it" to "Blocco personalizzato",
            "pt" to "Bloco personalizado", "pl" to "Własny blok", "tr" to "Özel Blok", "zh" to "自定义块", "ja" to "カスタムブロック", "ko" to "커스텀 블록", "ar" to "كتلة مخصصة"
        ),
        "Custom badge, prefix, icon, font, color & templates" to mapOf(
            "ru" to "Свой бейджик, префикс, иконка, шрифт, цвет и шаблоны", "uk" to "Власний бейдж, префікс, іконка, шрифт, колір і шаблони", "be" to "Свой бэйдж, прэфікс, іконка, шрыфт, колер і шаблоны", "kk" to "Өз белгісі, префикс, белгіше, қаріп, түс және үлгілер",
            "es" to "Insignia, prefijo, icono, fuente, color y plantillas", "de" to "Abzeichen, Präfix, Symbol, Schrift, Farbe & Vorlagen", "fr" to "Badge, préfixe, icône, police, couleur et modèles", "it" to "Badge, prefisso, icona, font, colore e modelli",
            "pt" to "Emblema, prefixo, ícone, fonte, cor e modelos", "pl" to "Własna etykieta, prefiks, ikona, czcionka, kolor i szablony", "tr" to "Özel rozet, önek, simge, yazı tipi, renk ve şablonlar", "zh" to "自定义徽章、前缀、图标、字体、颜色与模板", "ja" to "バッジ・プレフィックス・アイコン・フォント・色・テンプレート", "ko" to "커스텀 배지, 접두사, 아이콘, 글꼴, 색상 및 템플릿"
        ),
        "Standard Screenplay Blocks:" to mapOf(
            "ru" to "Базовые блоки сценария:", "uk" to "Базові блоки сценарію:", "be" to "Базавыя блокі сцэнарыя:", "kk" to "Сценарийдің негізгі блоктары:",
            "es" to "Bloques estándar de guion:", "de" to "Standard-Drehbuchblöcke:", "fr" to "Blocs de scénario standard :", "it" to "Blocchi standard sceneggiatura:",
            "pt" to "Blocos padrão de roteiro:", "pl" to "Standardowe bloki scenariusza:", "tr" to "Standart Senaryo Blokları:", "zh" to "标准剧本块:", "ja" to "標準脚本ブロック:", "ko" to "표준 시나리오 블록:"
        ),
        "Scene Heading" to mapOf(
            "ru" to "Заголовок сцены", "uk" to "Заголовок сцени", "be" to "Загаловак сцэны", "kk" to "Сахна тақырыбы",
            "es" to "Encabezado de escena", "de" to "Szenenüberschrift", "fr" to "Intitulé de scène", "it" to "Intestazione scena",
            "pt" to "Cabeçalho de cena", "pl" to "Nagłówek sceny", "tr" to "Sahne Başlığı", "zh" to "场景标题", "ja" to "シーン見出し", "ko" to "장면 제목"
        ),
        "Location and time of action (INT. ROOM - DAY)" to mapOf(
            "ru" to "Место и время действия (ИНТ. КОМНАТА — ДЕНЬ)", "uk" to "Місце та час дії (ІНТ. КІМНАТА — ДЕНЬ)", "be" to "Месца і час дзеяння (ІНТ. ПАКОЙ — ДЗЕНЬ)", "kk" to "Оқиға орны мен уақыты (ИНТ. БӨЛМЕ — КҮН)",
            "es" to "Lugar y tiempo de la acción (INT. HABITACIÓN - DÍA)", "de" to "Ort und Zeit der Handlung (INT. ZIMMER - TAG)", "fr" to "Lieu et moment de l'action (INT. CHAMBRE - JOUR)", "it" to "Luogo e tempo dell'azione (INT. STANZA - GIORNO)",
            "pt" to "Local e tempo da ação (INT. QUARTO - DIA)", "pl" to "Miejsce i czas akcji (WN. POKÓJ - DZIEŃ)", "tr" to "Mekan ve zaman (İÇ. ODA - GÜN)", "zh" to "场景地点与时间 (内景 房间 — 日)", "ja" to "場所と時間 (室内・部屋 — 昼)", "ko" to "장소 및 시간 (S# 실내. 방 — 낮)"
        ),
        "Action Description" to mapOf(
            "ru" to "Описание действия", "uk" to "Опис дії", "be" to "Апісанне дзеяння", "kk" to "Әрекет сипаттамасы",
            "es" to "Descripción de la acción", "de" to "Handlungsbeschreibung", "fr" to "Description de l'action", "it" to "Descrizione dell'azione",
            "pt" to "Descrição da ação", "pl" to "Opis akcji", "tr" to "Aksiyon Açıklaması", "zh" to "动作描述", "ja" to "ト書き・アクション", "ko" to "지문 / 액션 설명"
        ),
        "Narrative of what happens in the shot and setting" to mapOf(
            "ru" to "Что происходит в кадре, действия героев и обстановка", "uk" to "Що відбувається в кадрі, дії героїв та обстановка", "be" to "Што адбываецца ў кадры, дзеянні герояў і абстаноўка", "kk" to "Кадрда не болып жатқаны, кейіпкерлер әрекеті",
            "es" to "Narración de lo que ocurre en escena y el entorno", "de" to "Was in der Szene passiert, Handlungen und Umgebung", "fr" to "Ce qui se passe à l'écran, actions et décor", "it" to "Ciò che accade nell'inquadratura e nell'ambiente",
            "pt" to "Narrativa do que acontece em cena e ambiente", "pl" to "Co dzieje się w kadrze, działania postaci i otoczenie", "tr" to "Sahnede olup bitenler ve ortam anlatımı", "zh" to "画面中发生的事情、角色动作与环境", "ja" to "画面内の出来事・人物の動き・状況描写", "ko" to "장면에서 일어나는 일과 배경 서술"
        ),
        "Character Name" to mapOf(
            "ru" to "Имя персонажа (атрибуция)", "uk" to "Ім'я персонажа (атрибуція)", "be" to "Імя персанажа (атрыбуцыя)", "kk" to "Кейіпкер аты",
            "es" to "Nombre del personaje", "de" to "Figurenname", "fr" to "Nom du personnage", "it" to "Nome del personaggio",
            "pt" to "Nome do personagem", "pl" to "Imię postaci", "tr" to "Karakter Adı", "zh" to "角色名称", "ja" to "キャラクター名", "ko" to "등장인물 이름"
        ),
        "Centered before dialogue to identify speaker" to mapOf(
            "ru" to "Пишется по центру перед репликой героя", "uk" to "Пишеться по центру перед реплікою героя", "be" to "Пішацца па цэнтры перад рэплікай героя", "kk" to "Кейіпкер сөзінің алдында ортаға жазылады",
            "es" to "Centrado antes del diálogo para identificar al hablante", "de" to "Zentriert vor dem Dialog zur Sprecherkennung", "fr" to "Centré avant le dialogue pour identifier le locuteur", "it" to "Centrato prima del dialogo per indicare chi parla",
            "pt" to "Centralizado antes do diálogo para identificar quem fala", "pl" to "Wyśrodkowane przed kwestią bohatera", "tr" to "Konuşmacıyı belirtmek için diyalogdan önce ortalanır", "zh" to "在对白前居中显示以标识说话角色", "ja" to "セリフの前に中央揃えで配置", "ko" to "대사 앞에 가운데 정렬로 화자 표시"
        ),
        "Dialogue" to mapOf(
            "ru" to "Диалог", "uk" to "Діалог", "be" to "Дыялог", "kk" to "Диалог",
            "es" to "Diálogo", "de" to "Dialog", "fr" to "Dialogue", "it" to "Dialogo",
            "pt" to "Diálogo", "pl" to "Dialog", "tr" to "Diyalog", "zh" to "对白", "ja" to "セリフ", "ko" to "대사", "ar" to "حوار"
        ),
        "Character speech with optional parentheticals" to mapOf(
            "ru" to "Речь действующих лиц с поддержкой ремарок", "uk" to "Мова дійових осіб із підтримкою ремарок", "be" to "Мова дзеючых асоб з падтрымкай рэмарак", "kk" to "Ремарка қолдауы бар кейіпкерлер сөзі",
            "es" to "Habla de los personajes con acotaciones opcionales", "de" to "Figurenrede mit optionalen Regieanweisungen", "fr" to "Paroles des personnages avec didascalies", "it" to "Battute dei personaggi con didascalie opzionali",
            "pt" to "Fala dos personagens com rubricas opcionais", "pl" to "Kwestie postaci z obsługą didaskaliów", "tr" to "Parantez içi açıklamalarla karakter konuşması", "zh" to "支持括号提示的角色台词", "ja" to "ト書き（括弧書き）対応のキャラクターのセリフ", "ko" to "지문(괄호)을 지원하는 등장인물 대사"
        ),
        "Prefix" to mapOf(
            "ru" to "Префикс", "uk" to "Префікс", "be" to "Прэфікс", "kk" to "Префикс",
            "es" to "Prefijo", "de" to "Präfix", "fr" to "Préfixe", "it" to "Prefisso",
            "pt" to "Prefixo", "pl" to "Prefiks", "tr" to "Önek", "zh" to "前缀", "ja" to "プレフィックス", "ko" to "접두사"
        ),
        "Scene" to mapOf(
            "ru" to "Сцена", "uk" to "Сцена", "be" to "Сцэна", "kk" to "Сахна",
            "es" to "Escena", "de" to "Szene", "fr" to "Scène", "it" to "Scena",
            "pt" to "Cena", "pl" to "Scena", "tr" to "Sahne", "zh" to "场景", "ja" to "シーン", "ko" to "장면"
        ),
        "Action" to mapOf(
            "ru" to "Действие", "uk" to "Дія", "be" to "Дзеянне", "kk" to "Әрекет",
            "es" to "Acción", "de" to "Handlung", "fr" to "Action", "it" to "Azione",
            "pt" to "Ação", "pl" to "Akcja", "tr" to "Aksiyon", "zh" to "动作", "ja" to "アクション", "ko" to "액션"
        ),
        "Character" to mapOf(
            "ru" to "Персонаж", "uk" to "Персонаж", "be" to "Персанаж", "kk" to "Кейіпкер",
            "es" to "Personaje", "de" to "Figur", "fr" to "Personnage", "it" to "Personaggio",
            "pt" to "Personagem", "pl" to "Postać", "tr" to "Karakter", "zh" to "角色", "ja" to "キャラ", "ko" to "인물"
        ),
        "Custom" to mapOf(
            "ru" to "Кастом", "uk" to "Власний", "be" to "Кастом", "kk" to "Арнайы",
            "es" to "Personal", "de" to "Eigener", "fr" to "Perso", "it" to "Custom",
            "pt" to "Personal", "pl" to "Własny", "tr" to "Özel", "zh" to "自定义", "ja" to "カスタム", "ko" to "커스텀"
        ),
        "Blocks" to mapOf(
            "ru" to "Блоки", "uk" to "Блоки", "be" to "Блокі", "kk" to "Блоктар",
            "es" to "Bloques", "de" to "Blöcke", "fr" to "Blocs", "it" to "Blocchi",
            "pt" to "Blocos", "pl" to "Bloki", "tr" to "Bloklar", "zh" to "分块", "ja" to "ブロック", "ko" to "블록"
        ),
        "Document" to mapOf(
            "ru" to "Документ", "uk" to "Документ", "be" to "Дакумент", "kk" to "Құжат",
            "es" to "Documento", "de" to "Dokument", "fr" to "Document", "it" to "Documento",
            "pt" to "Documento", "pl" to "Dokument", "tr" to "Belge", "zh" to "文档", "ja" to "ドキュメント", "ko" to "문서"
        ),
        "Reading Mode" to mapOf(
            "ru" to "Режим чтения", "uk" to "Режим читання", "be" to "Рэжым чытання", "kk" to "Оқу режимі",
            "es" to "Modo lectura", "de" to "Lesemodus", "fr" to "Mode lecture", "it" to "Modalità lettura",
            "pt" to "Modo leitura", "pl" to "Tryb czytania", "tr" to "Okuma Modu", "zh" to "阅读模式", "ja" to "読書モード", "ko" to "읽기 모드"
        ),
        "Fullscreen" to mapOf(
            "ru" to "На весь экран", "uk" to "На весь екран", "be" to "На ўвесь экран", "kk" to "Толық экран",
            "es" to "Pantalla completa", "de" to "Vollbild", "fr" to "Plein écran", "it" to "Schermo intero",
            "pt" to "Tela cheia", "pl" to "Pełny ekran", "tr" to "Tam Ekran", "zh" to "全屏", "ja" to "全画面", "ko" to "전체 화면"
        ),
        "Add Block" to mapOf(
            "ru" to "Добавить блок", "uk" to "Додати блок", "be" to "Дадаць блок", "kk" to "Блок қосу",
            "es" to "Añadir bloque", "de" to "Block hinzufügen", "fr" to "Ajouter un bloc", "it" to "Aggiungi blocco",
            "pt" to "Adicionar bloco", "pl" to "Dodaj blok", "tr" to "Blok Ekle", "zh" to "添加块", "ja" to "ブロック追加", "ko" to "블록 추가"
        ),
        "+ Add parenthetical" to mapOf(
            "ru" to "+ Добавить ремарку", "uk" to "+ Додати ремарку", "be" to "+ Дадаць рэмарку", "kk" to "+ Ремарка қосу",
            "es" to "+ Añadir acotación", "de" to "+ Regieanweisung", "fr" to "+ Ajouter didascalie", "it" to "+ Aggiungi didascalia",
            "pt" to "+ Adicionar rubrica", "pl" to "+ Dodaj didaskalia", "tr" to "+ Parantez içi ekle", "zh" to "+ 添加括号提示", "ja" to "+ ト書きを追加", "ko" to "+ 지문 추가"
        ),
        "whispering" to mapOf(
            "ru" to "шепотом", "uk" to "пошепки", "be" to "шэптам", "kk" to "сыбырлап",
            "es" to "susurrando", "de" to "flüsternd", "fr" to "en chuchotant", "it" to "sussurrando",
            "pt" to "sussurrando", "pl" to "szeptem", "tr" to "fısıldayarak", "zh" to "低语", "ja" to "ささやき声で", "ko" to "속삭이며"
        ),
        "Action description..." to mapOf(
            "ru" to "Краткий рассказ о том, что происходит в кадре...", "uk" to "Короткий опис того, що відбувається в кадрі...", "be" to "Кароткае апісанне таго, што адбываецца ў кадры...", "kk" to "Кадрда не болып жатқаны туралы қысқаша сипаттама...",
            "es" to "Descripción de lo que ocurre en escena...", "de" to "Beschreibung der Handlung in der Szene...", "fr" to "Description de l'action dans la scène...", "it" to "Descrizione dell'azione nella scena...",
            "pt" to "Descrição da ação na cena...", "pl" to "Opis tego, co dzieje się w scenie...", "tr" to "Sahnede olanların açıklaması...", "zh" to "简要描述画面中发生的事情...", "ja" to "シーンのト書き・状況描写...", "ko" to "장면에서 일어나는 행동 묘사..."
        ),
        "CHARACTER NAME" to mapOf(
            "ru" to "ИМЯ ПЕРСОНАЖА", "uk" to "ІМ'Я ПЕРСОНАЖА", "be" to "ІМЯ ПЕРСАНАЖА", "kk" to "КЕЙІПКЕР АТЫ",
            "es" to "NOMBRE DEL PERSONAJE", "de" to "FIGURENNAME", "fr" to "NOM DU PERSONNAGE", "it" to "NOME PERSONAGGIO",
            "pt" to "NOME DO PERSONAGEM", "pl" to "IMIĘ POSTACI", "tr" to "KARAKTER ADI", "zh" to "角色名称", "ja" to "キャラクター名", "ko" to "등장인물 이름"
        ),
        "Dialogue lines..." to mapOf(
            "ru" to "Слова действующего лица...", "uk" to "Слова дійової особи...", "be" to "Словы дзеючай асобы...", "kk" to "Кейіпкердің сөздері...",
            "es" to "Líneas de diálogo del personaje...", "de" to "Dialogzeilen der Figur...", "fr" to "Réplique du personnage...", "it" to "Battute del personaggio...",
            "pt" to "Falas do personagem...", "pl" to "Kwestia wypowiadana przez postać...", "tr" to "Karakterin diyalog sözleri...", "zh" to "角色台词...", "ja" to "キャラクターのセリフ...", "ko" to "등장인물의 대사..."
        ),
        "Custom block content..." to mapOf(
            "ru" to "Текст кастомного блока...", "uk" to "Текст власного блоку...", "be" to "Тэкст кастомнага блока...", "kk" to "Арнайы блок мәтіні...",
            "es" to "Contenido del bloque personalizado...", "de" to "Inhalt des eigenen Blocks...", "fr" to "Contenu du bloc personnalisé...", "it" to "Contenuto del blocco personalizzato...",
            "pt" to "Conteúdo do bloco personalizado...", "pl" to "Treść własnego bloku...", "tr" to "Özel blok içeriği...", "zh" to "自定义块内容...", "ja" to "カスタムブロックの内容...", "ko" to "커스텀 블록 내용..."
        ),
        "Selected block:" to mapOf(
            "ru" to "Выбран блок:", "uk" to "Обрано блок:", "be" to "Выбраны блок:", "kk" to "Таңдалған блок:",
            "es" to "Bloque seleccionado:", "de" to "Ausgewählter Block:", "fr" to "Bloc sélectionné :", "it" to "Blocco selezionato:",
            "pt" to "Bloco selecionado:", "pl" to "Wybrany blok:", "tr" to "Seçili blok:", "zh" to "已选块:", "ja" to "選択中のブロック:", "ko" to "선택된 블록:"
        ),
        "Scenes:" to mapOf(
            "ru" to "Сцены:", "uk" to "Сцени:", "be" to "Сцэны:", "kk" to "Сахналар:",
            "es" to "Escenas:", "de" to "Szenen:", "fr" to "Scènes :", "it" to "Scene:",
            "pt" to "Cenas:", "pl" to "Sceny:", "tr" to "Sahneler:", "zh" to "场景:", "ja" to "シーン:", "ko" to "장면:"
        ),
        "Script is empty" to mapOf(
            "ru" to "Сценарий пуст", "uk" to "Сценарій порожній", "be" to "Сцэнарый пусты", "kk" to "Сценарий бос",
            "es" to "El guion está vacío", "de" to "Drehbuch ist leer", "fr" to "Le scénario est vide", "it" to "La sceneggiatura è vuota",
            "pt" to "O roteiro está vazio", "pl" to "Scenariusz jest pusty", "tr" to "Senaryo boş", "zh" to "剧本为空", "ja" to "脚本が空です", "ko" to "시나리오가 비어 있습니다"
        ),
        "Open" to mapOf(
            "ru" to "Открыть", "uk" to "Відкрити", "be" to "Адкрыць", "kk" to "Ашу",
            "es" to "Abrir", "de" to "Öffnen", "fr" to "Ouvrir", "it" to "Apri",
            "pt" to "Abrir", "pl" to "Otwórz", "tr" to "Aç", "zh" to "打开", "ja" to "開く", "ko" to "열기"
        ),
        "Productivity" to mapOf(
            "ru" to "Продуктивность", "uk" to "Продуктивність", "be" to "Прадуктыўнасць", "kk" to "Өнімділік",
            "es" to "Productividad", "de" to "Produktivität", "fr" to "Productivité", "it" to "Produttività",
            "pt" to "Produtividade", "pl" to "Produktywność", "tr" to "Üretkenlik", "zh" to "创作效率", "ja" to "生産性", "ko" to "생산성"
        ),
        "(Last 7 days)" to mapOf(
            "ru" to "(последние 7 дней)", "uk" to "(останні 7 днів)", "be" to "(апошнія 7 дзён)", "kk" to "(соңғы 7 күн)",
            "es" to "(últimos 7 días)", "de" to "(Letzte 7 Tage)", "fr" to "(7 derniers jours)", "it" to "(Ultimi 7 giorni)",
            "pt" to "(Últimos 7 dias)", "pl" to "(Ostatnie 7 dni)", "tr" to "(Son 7 gün)", "zh" to "(最近 7 天)", "ja" to "(過去7日間)", "ko" to "(최근 7일)"
        ),
        "Keep it up!" to mapOf(
            "ru" to "Так держать!", "uk" to "Так тримати!", "be" to "Так трымаць!", "kk" to "Жарайсыз!",
            "es" to "¡Sigue así!", "de" to "Weiter so!", "fr" to "Continuez comme ça !", "it" to "Continua così!",
            "pt" to "Continue assim!", "pl" to "Tak trzymaj!", "tr" to "Böyle devam et!", "zh" to "继续保持！", "ja" to "この調子で続けましょう！", "ko" to "계속 화이팅하세요!"
        ),
        "Consistency is the key to great stories." to mapOf(
            "ru" to "Постоянство — ключ к великим историям.", "uk" to "Постійність — ключ до великих історій.", "be" to "Пастаянства — ключ да вялікіх гісторый.", "kk" to "Тұрақтылық — ұлы хикаялардың кілті.",
            "es" to "La constancia es la clave para grandes historias.", "de" to "Beständigkeit ist der Schlüssel zu großen Geschichten.", "fr" to "La régularité est la clé des grandes histoires.", "it" to "La costanza è la chiave per grandi storie.",
            "pt" to "A consistência é a chave para grandes histórias.", "pl" to "Regularność to klucz do wielkich historii.", "tr" to "İstikrar, harika hikayelerin anahtarıdır.", "zh" to "持之以恒是写出伟大故事的关键。", "ja" to "継続こそが素晴らしい物語を生む鍵です。", "ko" to "꾸준함이 위대한 이야기를 만드는 열쇠입니다."
        ),
        "Storage Directory Synchronization" to mapOf(
            "ru" to "Синхронизация папок с памятью", "uk" to "Синхронізація папок із пам'яттю", "be" to "Сінхранізацыя папак з памяццю", "kk" to "Қалталарды жадпен синхрондау",
            "es" to "Sincronización con almacenamiento local", "de" to "Ordner-Synchronisierung mit Gerätespeicher", "fr" to "Synchronisation des dossiers sur l'appareil", "it" to "Sincronizzazione cartelle su memoria",
            "pt" to "Sincronização de pastas com o armazenamento", "pl" to "Synchronizacja folderów z pamięcią", "tr" to "Depolama Dizini Senkronizasyonu", "zh" to "本地存储目录同步", "ja" to "端末ストレージへのフォルダ同期", "ko" to "내부 저장소 폴더 동기화"
        ),
        "Synchronize with local storage" to mapOf(
            "ru" to "Синхронизировать с памятью", "uk" to "Синхронізувати з пам'яттю", "be" to "Сінхранізаваць з памяццю", "kk" to "Жадпен синхрондау",
            "es" to "Sincronizar con memoria local", "de" to "Mit Speicher synchronisieren", "fr" to "Synchroniser avec le stockage", "it" to "Sincronizza con la memoria",
            "pt" to "Sincronizar com armazenamento", "pl" to "Synchronizuj z pamięcią", "tr" to "Yerel depolamayla senkronize et", "zh" to "与本地存储同步", "ja" to "ローカルストレージと同期", "ko" to "로컬 저장소와 동기화"
        ),
        "Google Tensor & Pixel Optimization" to mapOf(
            "ru" to "Оптимизация Google Tensor & Pixel", "uk" to "Оптимізація Google Tensor & Pixel", "be" to "Аптымізацыя Google Tensor & Pixel", "kk" to "Google Tensor & Pixel оңтайландыруы",
            "es" to "Optimización Google Tensor y Pixel", "de" to "Google Tensor & Pixel Optimierung", "fr" to "Optimisation Google Tensor & Pixel", "it" to "Ottimizzazione Google Tensor & Pixel",
            "pt" to "Otimização Google Tensor & Pixel", "pl" to "Optymalizacja Google Tensor i Pixel", "tr" to "Google Tensor ve Pixel Optimizasyonu", "zh" to "Google Tensor 与 Pixel 硬件优化", "ja" to "Google Tensor & Pixel 最適化", "ko" to "Google Tensor 및 Pixel 최적화"
        ),
        "Run Tensor Core Benchmark" to mapOf(
            "ru" to "Экспресс-тест ядер Tensor", "uk" to "Експрес-тест ядер Tensor", "be" to "Экспрэс-тэст ядраў Tensor", "kk" to "Tensor ядроларының экспресс-тесті",
            "es" to "Ejecutar prueba Tensor Core", "de" to "Tensor-Core-Benchmark starten", "fr" to "Lancer le test Tensor Core", "it" to "Esegui benchmark Tensor Core",
            "pt" to "Executar benchmark Tensor Core", "pl" to "Uruchom test rdzeni Tensor", "tr" to "Tensor Çekirdek Testini Çalıştır", "zh" to "运行 Tensor 核心基准测试", "ja" to "Tensor コアベンチマークを実行", "ko" to "Tensor 코어 벤치마크 실행"
        ),
        "Sync Conflict" to mapOf(
            "ru" to "Конфликт синхронизации", "uk" to "Конфлікт синхронізації", "be" to "Канфлікт сінхранізацыі", "kk" to "Синхрондау қақтығысы",
            "es" to "Conflicto de sincronización", "de" to "Synchronisierungskonflikt", "fr" to "Conflit de synchronisation", "it" to "Conflitto di sincronizzazione",
            "pt" to "Conflito de sincronização", "pl" to "Konflikt synchronizacji", "tr" to "Senkronizasyon Çakışması", "zh" to "同步冲突", "ja" to "同期の競合", "ko" to "동기화 충돌"
        ),
        "Keep Local Version" to mapOf(
            "ru" to "Оставить локальную версию", "uk" to "Залишити локальну версію", "be" to "Пакінуць лакальную версію", "kk" to "Жергілікті нұсқаны қалдыру",
            "es" to "Mantener versión local", "de" to "Lokale Version behalten", "fr" to "Garder la version locale", "it" to "Mantieni versione locale",
            "pt" to "Manter versão local", "pl" to "Zachowaj wersję lokalną", "tr" to "Yerel Sürümü Koru", "zh" to "保留本地版本", "ja" to "ローカル版を保持", "ko" to "로컬 버전 유지"
        ),
        "Keep Cloud Version" to mapOf(
            "ru" to "Оставить облачную версию", "uk" to "Залишити хмарну версію", "be" to "Пакінуць воблачную версію", "kk" to "Бұлттық нұсқаны қалдыру",
            "es" to "Mantener versión en la nube", "de" to "Cloud-Version behalten", "fr" to "Garder la version cloud", "it" to "Mantieni versione cloud",
            "pt" to "Manter versão na nuvem", "pl" to "Zachowaj wersję w chmurze", "tr" to "Bulut Sürümünü Koru", "zh" to "保留云端版本", "ja" to "クラウド版を保持", "ko" to "클라우드 버전 유지"
        ),
        "Create Merged Copy" to mapOf(
            "ru" to "Создать объединенную копию", "uk" to "Створити об'єднану копію", "be" to "Стварыць аб'яднаную копію", "kk" to "Біріктірілген көшірме жасау",
            "es" to "Crear copia combinada", "de" to "Zusammengeführte Kopie erstellen", "fr" to "Créer une copie fusionnée", "it" to "Crea copia unita",
            "pt" to "Criar cópia mesclada", "pl" to "Utwórz połączoną kopię", "tr" to "Birleştirilmiş Kopya Oluştur", "zh" to "创建合并副本", "ja" to "統合コピーを作成", "ko" to "병합된 사본 만들기"
        ),
        "Welcome to Writer's Studio" to mapOf(
            "ru" to "Вход в Writer's Studio", "uk" to "Вхід у Writer's Studio", "be" to "Уваход у Writer's Studio", "kk" to "Writer's Studio-ға кіру",
            "es" to "Bienvenido a Writer's Studio", "de" to "Willkommen im Writer's Studio", "fr" to "Bienvenue dans Writer's Studio", "it" to "Benvenuto in Writer's Studio",
            "pt" to "Bem-vindo ao Writer's Studio", "pl" to "Witaj w Writer's Studio", "tr" to "Writer's Studio'ya Hoş Geldiniz", "zh" to "欢迎进入 Writer's Studio", "ja" to "Writer's Studio へようこそ", "ko" to "Writer's Studio에 오신 것을 환영합니다"
        ),
        "Manual" to mapOf(
            "ru" to "Порядок", "uk" to "Порядок", "be" to "Парадак", "kk" to "Реті",
            "es" to "Manual", "de" to "Manuell", "fr" to "Manuel", "it" to "Manuale",
            "pt" to "Manual", "pl" to "Ręcznie", "tr" to "Manuel", "zh" to "手动", "ja" to "手動", "ko" to "수동"
        ),
        "Rename Project" to mapOf(
            "ru" to "Переименовать проект", "uk" to "Перейменувати проєкт", "be" to "Перайменаваць праект", "kk" to "Жоба атын өзгерту",
            "es" to "Renombrar proyecto", "de" to "Projekt umbenennen", "fr" to "Renommer le projet", "it" to "Rinomina progetto",
            "pt" to "Renomear projeto", "pl" to "Zmień nazwę projektu", "tr" to "Projeyi Yeniden Adlandır", "zh" to "重命名项目", "ja" to "プロジェクト名を変更", "ko" to "프로젝트 이름 변경"
        ),
        "Enter new project title:" to mapOf(
            "ru" to "Введите новое название проекта:", "uk" to "Введіть нову назву проєкту:", "be" to "Увядзіце новую назву праекта:", "kk" to "Жобаның жаңа атауын енгізіңіз:",
            "es" to "Introduce el nuevo título del proyecto:", "de" to "Neuen Projekttitel eingeben:", "fr" to "Entrez le nouveau titre du projet :", "it" to "Inserisci il nuovo titolo del progetto:",
            "pt" to "Digite o novo título do projeto:", "pl" to "Wpisz nową nazwę projektu:", "tr" to "Yeni proje başlığını girin:", "zh" to "输入新项目名称:", "ja" to "新しいプロジェクト名を入力:", "ko" to "새 프로젝트 제목 입력:"
        ),
        "Project actions:" to mapOf(
            "ru" to "Действия проекта:", "uk" to "Дії проєкту:", "be" to "Дзеянні праекта:", "kk" to "Жоба әрекеттері:",
            "es" to "Acciones del proyecto:", "de" to "Projektaktionen:", "fr" to "Actions du projet :", "it" to "Azioni progetto:",
            "pt" to "Ações do projeto:", "pl" to "Akcje projektu:", "tr" to "Proje işlemleri:", "zh" to "项目操作:", "ja" to "プロジェクト操作:", "ko" to "프로젝트 작업:"
        ),
        "Rename" to mapOf(
            "ru" to "Переименовать", "uk" to "Перейменувати", "be" to "Перайменаваць", "kk" to "Атын өзгерту",
            "es" to "Renombrar", "de" to "Umbenennen", "fr" to "Renommer", "it" to "Rinomina",
            "pt" to "Renomear", "pl" to "Zmień nazwę", "tr" to "Yeniden Adlandır", "zh" to "重命名", "ja" to "名前を変更", "ko" to "이름 변경"
        ),
        "Move Up" to mapOf(
            "ru" to "Переместить выше", "uk" to "Перемістити вище", "be" to "Перамясціць вышэй", "kk" to "Жоғары жылжыту",
            "es" to "Mover arriba", "de" to "Nach oben", "fr" to "Monter", "it" to "Sposta su",
            "pt" to "Mover para cima", "pl" to "Przenieś wyżej", "tr" to "Yukarı Taşı", "zh" to "上移", "ja" to "上へ移動", "ko" to "위로 이동"
        ),
        "Move Down" to mapOf(
            "ru" to "Переместить ниже", "uk" to "Перемістити нижче", "be" to "Перамясціць ніжэй", "kk" to "Төмен жылжыту",
            "es" to "Mover abajo", "de" to "Nach unten", "fr" to "Descendre", "it" to "Sposta giù",
            "pt" to "Mover para baixo", "pl" to "Przenieś niżej", "tr" to "Aşağı Taşı", "zh" to "下移", "ja" to "下へ移動", "ko" to "아래로 이동"
        ),
        "Delete Project?" to mapOf(
            "ru" to "Удалить проект?", "uk" to "Видалити проєкт?", "be" to "Выдаліць праект?", "kk" to "Жобаны жою керек пе?",
            "es" to "¿Eliminar proyecto?", "de" to "Projekt löschen?", "fr" to "Supprimer le projet ?", "it" to "Eliminare il progetto?",
            "pt" to "Excluir projeto?", "pl" to "Usunąć projekt?", "tr" to "Proje Silinsin mi?", "zh" to "删除项目？", "ja" to "プロジェクトを削除しますか？", "ko" to "프로젝트를 삭제할까요?"
        ),
        "Delete Project Permanently?" to mapOf(
            "ru" to "Удалить проект навсегда?", "uk" to "Видалити проєкт назавжди?", "be" to "Выдаліць праект назаўжды?", "kk" to "Жобаны біржола жою керек пе?",
            "es" to "¿Eliminar proyecto permanentemente?", "de" to "Projekt endgültig löschen?", "fr" to "Supprimer définitivement le projet ?", "it" to "Eliminare definitivamente il progetto?",
            "pt" to "Excluir projeto permanentemente?", "pl" to "Usunąć projekt na zawsze?", "tr" to "Proje Kalıcı Olarak Silinsin mi?", "zh" to "永久删除项目？", "ja" to "プロジェクトを完全に削除しますか？", "ko" to "프로젝트를 영구 삭제할까요?"
        ),
        "Erase" to mapOf(
            "ru" to "Стереть", "uk" to "Стерти", "be" to "Сцерці", "kk" to "Өшіру",
            "es" to "Borrar", "de" to "Löschen", "fr" to "Effacer", "it" to "Cancella",
            "pt" to "Apagar", "pl" to "Wymaż", "tr" to "Sil", "zh" to "擦除", "ja" to "消去", "ko" to "지우기"
        ),
        "Export Document" to mapOf(
            "ru" to "Экспорт документа", "uk" to "Експорт документа", "be" to "Экспарт дакумента", "kk" to "Құжатты экспорттау",
            "es" to "Exportar documento", "de" to "Dokument exportieren", "fr" to "Exporter le document", "it" to "Esporta documento",
            "pt" to "Exportar documento", "pl" to "Eksportuj dokument", "tr" to "Belgeyi Dışa Aktar", "zh" to "导出文档", "ja" to "ドキュメントを書き出す", "ko" to "문서 내보내기"
        ),
        "File Format" to mapOf(
            "ru" to "Формат файла", "uk" to "Формат файлу", "be" to "Фармат файла", "kk" to "Файл пішімі",
            "es" to "Formato de archivo", "de" to "Dateiformat", "fr" to "Format de fichier", "it" to "Formato file",
            "pt" to "Formato do arquivo", "pl" to "Format pliku", "tr" to "Dosya Biçimi", "zh" to "文件格式", "ja" to "ファイル形式", "ko" to "파일 형식"
        ),
        "Page Size" to mapOf(
            "ru" to "Размер страницы", "uk" to "Розмір сторінки", "be" to "Памер старонкі", "kk" to "Бет өлшемі",
            "es" to "Tamaño de página", "de" to "Seitengröße", "fr" to "Taille de page", "it" to "Dimensione pagina",
            "pt" to "Tamanho da página", "pl" to "Rozmiar strony", "tr" to "Sayfa Boyutu", "zh" to "页面大小", "ja" to "ページサイズ", "ko" to "페이지 크기"
        ),
        "Document Primary Font" to mapOf(
            "ru" to "Основной шрифт документа", "uk" to "Основний шрифт документа", "be" to "Асноўны шрыфт дакумента", "kk" to "Құжаттың негізгі қарпі",
            "es" to "Fuente principal del documento", "de" to "Hauptschriftart des Dokuments", "fr" to "Police principale du document", "it" to "Font principale del documento",
            "pt" to "Fonte principal do documento", "pl" to "Główna czcionka dokumentu", "tr" to "Belge Ana Yazı Tipi", "zh" to "文档主字体", "ja" to "ドキュメントの基本フォント", "ko" to "문서 기본 글꼴"
        ),
        "Line Spacing" to mapOf(
            "ru" to "Межстрочный интервал", "uk" to "Міжрядковий інтервал", "be" to "Міжрадковы інтэрвал", "kk" to "Жоларалық интервал",
            "es" to "Interlineado", "de" to "Zeilenabstand", "fr" to "Interligne", "it" to "Interlinea",
            "pt" to "Espaçamento entre linhas", "pl" to "Odstęp między wierszami", "tr" to "Satır Aralığı", "zh" to "行距", "ja" to "行間", "ko" to "줄 간격"
        ),
        "Include Page Numbers" to mapOf(
            "ru" to "Нумерация страниц", "uk" to "Нумерація сторінок", "be" to "Нумарацыя старонак", "kk" to "Беттерді нөмірлеу",
            "es" to "Incluir números de página", "de" to "Seitenzahlen einschließen", "fr" to "Inclure les numéros de page", "it" to "Includi numeri di pagina",
            "pt" to "Incluir números de página", "pl" to "Numeracja stron", "tr" to "Sayfa Numaralarını Ekle", "zh" to "包含页码", "ja" to "ページ番号を含める", "ko" to "페이지 번호 포함"
        ),
        "Draw page index centered in footer" to mapOf(
            "ru" to "Печатать номера страниц внизу по центру", "uk" to "Друкувати номери сторінок унизу по центру", "be" to "Друкаваць нумары старонак унізе па цэнтры", "kk" to "Бет нөмірлерін төменгі ортаға қою",
            "es" to "Mostrar número de página centrado en el pie", "de" to "Seitenzahl zentriert in der Fußzeile", "fr" to "Afficher le numéro de page centré en bas", "it" to "Mostra numero di pagina centrato nel piè di pagina",
            "pt" to "Mostrar número da página centralizado no rodapé", "pl" to "Umieść numery stron na dole pośrodku", "tr" to "Alt bilgide sayfa numarasını ortala", "zh" to "在页脚居中显示页码", "ja" to "フッター中央にページ番号を表示", "ko" to "바닥글 중앙에 페이지 번호 표시"
        ),
        "Export File" to mapOf(
            "ru" to "Экспорт", "uk" to "Експорт", "be" to "Экспарт", "kk" to "Экспорт",
            "es" to "Exportar", "de" to "Exportieren", "fr" to "Exporter", "it" to "Esporta",
            "pt" to "Exportar", "pl" to "Eksportuj", "tr" to "Dışa Aktar", "zh" to "导出文件", "ja" to "書き出す", "ko" to "파일 내보내기"
        ),
        "Rename Document" to mapOf(
            "ru" to "Переименовать документ", "uk" to "Перейменувати документ", "be" to "Перайменаваць дакумент", "kk" to "Құжат атын өзгерту",
            "es" to "Renombrar documento", "de" to "Dokument umbenennen", "fr" to "Renommer le document", "it" to "Rinomina documento",
            "pt" to "Renomear documento", "pl" to "Zmień nazwę dokumentu", "tr" to "Belgeyi Yeniden Adlandır", "zh" to "重命名文档", "ja" to "ドキュメント名を変更", "ko" to "문서 이름 변경"
        ),
        "New Title" to mapOf(
            "ru" to "Новое название", "uk" to "Нова назва", "be" to "Новая назва", "kk" to "Жаңа атау",
            "es" to "Nuevo título", "de" to "Neuer Titel", "fr" to "Nouveau titre", "it" to "Nuovo titolo",
            "pt" to "Novo título", "pl" to "Nowa nazwa", "tr" to "Yeni Başlık", "zh" to "新标题", "ja" to "新しいタイトル", "ko" to "새 제목"
        ),
        "Create Document" to mapOf(
            "ru" to "Создать документ", "uk" to "Створити документ", "be" to "Стварыць дакумент", "kk" to "Құжат жасау",
            "es" to "Crear documento", "de" to "Dokument erstellen", "fr" to "Créer un document", "it" to "Crea documento",
            "pt" to "Criar documento", "pl" to "Utwórz dokument", "tr" to "Belge Oluştur", "zh" to "创建文档", "ja" to "ドキュメントを作成", "ko" to "문서 만들기"
        ),
        "Document Title" to mapOf(
            "ru" to "Название документа", "uk" to "Назва документа", "be" to "Назва дакумента", "kk" to "Құжат атауы",
            "es" to "Título del documento", "de" to "Dokumenttitel", "fr" to "Titre du document", "it" to "Titolo del documento",
            "pt" to "Título do documento", "pl" to "Nazwa dokumentu", "tr" to "Belge Başlığı", "zh" to "文档名称", "ja" to "ドキュメント名", "ko" to "문서 제목"
        ),
        "Select format:" to mapOf(
            "ru" to "Выберите режим:", "uk" to "Оберіть режим:", "be" to "Выберыце рэжым:", "kk" to "Режимді таңдаңыз:",
            "es" to "Seleccionar formato:", "de" to "Format wählen:", "fr" to "Choisir le format :", "it" to "Seleziona formato:",
            "pt" to "Selecionar formato:", "pl" to "Wybierz tryb:", "tr" to "Biçim seçin:", "zh" to "选择格式:", "ja" to "モードを選択:", "ko" to "형식 선택:"
        ),
        "Delete Document?" to mapOf(
            "ru" to "Удалить документ?", "uk" to "Видалити документ?", "be" to "Выдаліць дакумент?", "kk" to "Құжатты жою керек пе?",
            "es" to "¿Eliminar documento?", "de" to "Dokument löschen?", "fr" to "Supprimer le document ?", "it" to "Eliminare il documento?",
            "pt" to "Excluir documento?", "pl" to "Usunąć dokument?", "tr" to "Belge Silinsin mi?", "zh" to "删除文档？", "ja" to "ドキュメントを削除しますか？", "ko" to "문서를 삭제할까요?"
        ),
        "Move to Folder" to mapOf(
            "ru" to "Перекинуть в папку", "uk" to "Перемістити в папку", "be" to "Перамясціць у папку", "kk" to "Қалтаға жылжыту",
            "es" to "Mover a carpeta", "de" to "In Ordner verschieben", "fr" to "Déplacer dans un dossier", "it" to "Sposta nella cartella",
            "pt" to "Mover para pasta", "pl" to "Przenieś do folderu", "tr" to "Klasöre Taşı", "zh" to "移动到文件夹", "ja" to "フォルダへ移動", "ko" to "폴더로 이동"
        ),
        "Export to PDF" to mapOf(
            "ru" to "Экспорт в PDF", "uk" to "Експорт у PDF", "be" to "Экспарт у PDF", "kk" to "PDF-ке экспорттау",
            "es" to "Exportar a PDF", "de" to "Als PDF exportieren", "fr" to "Exporter en PDF", "it" to "Esporta in PDF",
            "pt" to "Exportar para PDF", "pl" to "Eksportuj do PDF", "tr" to "PDF Olarak Dışa Aktar", "zh" to "导出为 PDF", "ja" to "PDF に書き出す", "ko" to "PDF로 내보내기"
        ),
        "Delete Document" to mapOf(
            "ru" to "Удалить", "uk" to "Видалити", "be" to "Выдаліць", "kk" to "Жою",
            "es" to "Eliminar documento", "de" to "Dokument löschen", "fr" to "Supprimer le document", "it" to "Elimina documento",
            "pt" to "Excluir documento", "pl" to "Usuń dokument", "tr" to "Belgeyi Sil", "zh" to "删除文档", "ja" to "ドキュメントを削除", "ko" to "문서 삭제"
        ),
        "Font" to mapOf(
            "ru" to "Шрифт", "uk" to "Шрифт", "be" to "Шрыфт", "kk" to "Қаріп",
            "es" to "Fuente", "de" to "Schrift", "fr" to "Police", "it" to "Font",
            "pt" to "Fonte", "pl" to "Czcionka", "tr" to "Yazı Tipi", "zh" to "字体", "ja" to "フォント", "ko" to "글꼴"
        ),
        "Size" to mapOf(
            "ru" to "Размер", "uk" to "Розмір", "be" to "Памер", "kk" to "Өлшем",
            "es" to "Tamaño", "de" to "Größe", "fr" to "Taille", "it" to "Dimensione",
            "pt" to "Tamanho", "pl" to "Rozmiar", "tr" to "Boyut", "zh" to "大小", "ja" to "サイズ", "ko" to "크기"
        ),
        "Spacing" to mapOf(
            "ru" to "Интервал", "uk" to "Інтервал", "be" to "Інтэрвал", "kk" to "Интервал",
            "es" to "Espaciado", "de" to "Abstand", "fr" to "Espacement", "it" to "Interlinea",
            "pt" to "Espaçamento", "pl" to "Odstęp", "tr" to "Aralık", "zh" to "间距", "ja" to "行間", "ko" to "간격"
        ),
        "Speed" to mapOf(
            "ru" to "Скорость", "uk" to "Швидкість", "be" to "Хуткасць", "kk" to "Жылдамдық",
            "es" to "Velocidad", "de" to "Tempo", "fr" to "Vitesse", "it" to "Velocità",
            "pt" to "Velocidade", "pl" to "Prędkość", "tr" to "Hız", "zh" to "速度", "ja" to "速度", "ko" to "속도"
        ),
        "Text Size" to mapOf(
            "ru" to "Размер", "uk" to "Розмір", "be" to "Памер", "kk" to "Мәтін өлшемі",
            "es" to "Tamaño", "de" to "Textgröße", "fr" to "Taille", "it" to "Dimensione",
            "pt" to "Tamanho", "pl" to "Rozmiar", "tr" to "Metin Boyutu", "zh" to "字号", "ja" to "文字サイズ", "ko" to "글자 크기"
        ),
        "Mirror:" to mapOf(
            "ru" to "Зеркало:", "uk" to "Дзеркало:", "be" to "Люстэрка:", "kk" to "Айна:",
            "es" to "Espejo:", "de" to "Spiegel:", "fr" to "Miroir :", "it" to "Specchio:",
            "pt" to "Espelho:", "pl" to "Lustro:", "tr" to "Ayna:", "zh" to "镜像:", "ja" to "ミラー:", "ko" to "미러:"
        ),
        "Swipe down to collapse settings" to mapOf(
            "ru" to "Проведите пальцем вниз, чтобы свернуть настройки", "uk" to "Проведіть униз, щоб згорнути налаштування", "be" to "Правядзіце ўніз, каб згарнуць налады", "kk" to "Баптауларды жинау үшін төмен сырғытыңыз",
            "es" to "Desliza hacia abajo para ocultar ajustes", "de" to "Nach unten wischen, um Einstellungen einzuklappen", "fr" to "Balayez vers le bas pour réduire les paramètres", "it" to "Scorri verso il basso per chiudere le impostazioni",
            "pt" to "Deslize para baixo para recolher ajustes", "pl" to "Przesuń w dół, aby zwinąć ustawienia", "tr" to "Ayarları daraltmak için aşağı kaydırın", "zh" to "向下滑动收起设置", "ja" to "下にスワイプして設定を閉じる", "ko" to "아래로 스와이프하여 설정 접기"
        )
    )

    fun getString(key: String, lang: String): String {
        val effective = resolveEffectiveLanguage(lang)
        val dict = getDictionaryFor(effective)
        if (dict != null) {
            dict[key]?.let { return it }
        }
        // Fallback chain: English -> Russian -> Key
        return en[key] ?: ru[key] ?: key
    }

    /**
     * Universal phrase synchronizer for all 37 languages.
     * Translates Russian/English pairs into the active language using dictionary keys,
     * studio phrase tables, and dynamic pattern matching.
     */
    fun tr(ruText: String, enText: String, lang: String): String {
        val effective = resolveEffectiveLanguage(lang)
        if (effective == "ru") return ruText
        if (effective == "en") return enText

        // 1. Direct lookup in studioPhrasesByEn
        val directPhrase = studioPhrasesByEn[enText]?.get(effective)
        if (directPhrase != null) return directPhrase

        // 2. Reverse dictionary lookup via key
        val matchedKey = enToKey[enText.trim()]
            ?: ruToKey[ruText.trim()]
            ?: enToKey[enText.trim().lowercase()]
            ?: ruToKey[ruText.trim().lowercase()]
        if (matchedKey != null) {
            val fromDict = getDictionaryFor(effective)?.get(matchedKey)
            if (fromDict != null) return fromDict
        }

        // 3. Dynamic / interpolated patterns (counts, names, hex codes, system language names)
        val dynamic = translateDynamicPhrase(ruText, enText, effective)
        if (dynamic != null) return dynamic

        // 4. Fallback to English for international locales
        return enText
    }

    private fun translateDynamicPhrase(ruText: String, enText: String, effective: String): String? {
        // "Saved (N)" / "Мои (N)"
        if (enText.startsWith("Saved (") && enText.endsWith(")")) {
            val count = enText.substringAfter("Saved (").substringBefore(")")
            val word = when (effective) {
                "uk" -> "Мої"
                "be" -> "Мае"
                "kk" -> "Сақталған"
                "es" -> "Guardadas"
                "de" -> "Gespeichert"
                "fr" -> "Enregistrées"
                "it" -> "Salvate"
                "pt" -> "Salvas"
                "pl" -> "Zapisane"
                "tr" -> "Kayıtlı"
                "zh" -> "已存"
                "ja" -> "保存済"
                "ko" -> "저장됨"
                "ar" -> "المحفوظة"
                else -> getString("saved_palettes", effective).substringBefore(" ")
            }
            return "$word ($count)"
        }
        // "Templates (N)" / "Шаблоны (N)"
        if (enText.startsWith("Templates (") && enText.endsWith(")")) {
            val count = enText.substringAfter("Templates (").substringBefore(")")
            val base = tr("Шаблоны", "Templates", effective)
            return "$base ($count)"
        }
        // "Saved Templates (N)" / "Мои шаблоны (N)"
        if (enText.startsWith("Saved Templates (") && enText.contains(")")) {
            val count = enText.substringAfter("Saved Templates (").substringBefore(")")
            val suffix = if (enText.endsWith(":")) ":" else ""
            val base = getString("saved_templates", effective)
            return "$base ($count)$suffix"
        }
        if (enText.startsWith("Saved Block Templates (") && enText.contains(")")) {
            val count = enText.substringAfter("Saved Block Templates (").substringBefore(")")
            val suffix = if (enText.endsWith(":")) ":" else ""
            val base = getString("saved_templates", effective)
            return "$base ($count)$suffix"
        }
        // "+ All (37)" / "All (37)"
        if (enText == "+ All (37)" || enText == "All (37)") {
            val prefix = if (enText.startsWith("+ ")) "+ " else ""
            return "$prefix${getString("cat_all", effective)} (37)"
        }
        // "System (X)" or "System Default (X)"
        if ((enText.startsWith("System (") || enText.startsWith("System Default (")) && enText.endsWith(")")) {
            val sysName = enText.substringAfter("(").substringBeforeLast(")")
            return "${getString("lang_system", effective)} ($sysName)"
        }
        // "Palette #N" or "Palette N"
        if (enText.startsWith("Palette #") || enText.startsWith("Palette ")) {
            val num = enText.removePrefix("Palette ").trim()
            val word = when (effective) {
                "uk" -> "Палітра"
                "be" -> "Палітра"
                "kk" -> "Палитра"
                "es" -> "Paleta"
                "de" -> "Palette"
                "fr" -> "Palette"
                "it" -> "Tavolozza"
                "pt" -> "Paleta"
                "pl" -> "Paleta"
                "tr" -> "Palet"
                "zh" -> "调色板"
                "ja" -> "パレット"
                "ko" -> "팔레트"
                "ar" -> "لوحة"
                else -> "Palette"
            }
            return "$word $num"
        }
        // "Copied: #HEX" / "#HEX copied"
        if (enText.startsWith("Copied: ")) {
            val hex = enText.removePrefix("Copied: ")
            val word = when (effective) {
                "uk" -> "Скопійовано: $hex"
                "be" -> "Скапіявана: $hex"
                "kk" -> "Көшірілді: $hex"
                "es" -> "Copiado: $hex"
                "de" -> "Kopiert: $hex"
                "fr" -> "Copié : $hex"
                "it" -> "Copiato: $hex"
                "pt" -> "Copiado: $hex"
                "pl" -> "Skopiowano: $hex"
                "tr" -> "Kopyalandı: $hex"
                "zh" -> "已复制: $hex"
                "ja" -> "コピー完了: $hex"
                "ko" -> "복사됨: $hex"
                else -> "Copied: $hex"
            }
            return word
        }
        if (enText.endsWith(" copied")) {
            val hex = enText.removeSuffix(" copied")
            return when (effective) {
                "uk" -> "$hex скопійовано"
                "be" -> "$hex скапіяваны"
                "kk" -> "$hex көшірілді"
                "es" -> "$hex copiado"
                "de" -> "$hex kopiert"
                "fr" -> "$hex copié"
                "it" -> "$hex copiato"
                "pt" -> "$hex copiado"
                "pl" -> "$hex skopiowano"
                "tr" -> "$hex kopyalandı"
                "zh" -> "已复制 $hex"
                "ja" -> "$hex をコピーしました"
                "ko" -> "$hex 복사됨"
                else -> enText
            }
        }
        if (enText.startsWith("Language synced with system: ") || enText.startsWith("Language synchronized with system: ")) {
            val sysName = enText.substringAfter(": ")
            return "${getString("lang_system", effective)}: $sysName"
        }
        if (enText.startsWith("Total: ") && enText.endsWith(" words")) {
            val num = enText.removePrefix("Total: ").removeSuffix(" words")
            return "${getString("words", effective)}: $num"
        }
        if (enText.startsWith("Active document (") && enText.endsWith(" words):")) {
            val num = enText.removePrefix("Active document (").removeSuffix(" words):")
            return "${tr("Документ", "Document", effective)} ($num ${getString("words", effective).lowercase()}):"
        }
        return null
    }

    // --- DATA MODEL & PRESET LANGUAGE SYNCHRONIZERS ---

    private val badgeCanonicalGroups: Map<String, Map<String, String>> = mapOf(
        "custom" to mapOf("ru" to "КАСТОМ", "en" to "CUSTOM", "uk" to "ВЛАСНИЙ", "be" to "КАСТОМ", "kk" to "АРНАЙЫ", "es" to "PERSONAL", "de" to "EIGENER", "fr" to "PERSO", "it" to "CUSTOM", "pt" to "PERSONAL", "pl" to "WŁASNY", "tr" to "ÖZEL", "zh" to "自定义", "ja" to "カスタム", "ko" to "커스텀", "ar" to "مخصص"),
        "vo" to mapOf("ru" to "З.К.", "en" to "V.O.", "uk" to "З.К.", "be" to "З.К.", "kk" to "К.С.", "es" to "V.O.", "de" to "V.O.", "fr" to "V.O.", "it" to "F.C.", "pt" to "V.O."),
        "flashback" to mapOf("ru" to "ФЛЕШБЭК", "en" to "FLASHBACK", "uk" to "ФЛЕШБЕК", "be" to "ФЛЭШБЭК", "kk" to "ФЛЕШБЭК", "es" to "FLASHBACK", "de" to "RÜCKBLENDE", "fr" to "FLASHBACK", "it" to "FLASHBACK", "pt" to "FLASHBACK", "zh" to "闪回", "ja" to "回想", "ko" to "플래시백"),
        "title" to mapOf("ru" to "ТИТР", "en" to "TITLE", "uk" to "ТИТР", "be" to "ТЫТР", "kk" to "ТИТР", "es" to "TÍTULO", "de" to "TITEL", "fr" to "TITRE", "it" to "TITOLO", "pt" to "TÍTULO", "pl" to "NAPIS", "tr" to "BAŞLIK", "zh" to "字幕", "ja" to "字幕", "ko" to "자막"),
        "sfx" to mapOf("ru" to "SFX", "en" to "SFX", "uk" to "SFX", "be" to "SFX", "kk" to "SFX", "es" to "SFX", "de" to "SFX", "fr" to "SFX"),
        "vfx" to mapOf("ru" to "VFX", "en" to "VFX", "uk" to "VFX", "be" to "VFX", "kk" to "VFX", "es" to "VFX", "de" to "VFX", "fr" to "VFX"),
        "note" to mapOf("ru" to "ЗАМЕТКА", "en" to "NOTE", "uk" to "НОТАТКА", "be" to "НАТАТКА", "kk" to "ЖАЗБА", "es" to "NOTA", "de" to "NOTIZ", "fr" to "NOTE", "it" to "NOTA", "pt" to "NOTA", "pl" to "NOTATKA", "tr" to "NOT", "zh" to "备注", "ja" to "ノート", "ko" to "메모", "ar" to "ملاحظة"),
        "camera" to mapOf("ru" to "КАМЕРА", "en" to "CAMERA", "uk" to "КАМЕРА", "be" to "КАМЕРА", "kk" to "КАМЕРА", "es" to "CÁMARA", "de" to "KAMERA", "fr" to "CAMÉRA", "it" to "CAMERA", "pt" to "CÂMERA", "pl" to "KAMERA", "tr" to "KAMERA", "zh" to "镜头", "ja" to "カメラ", "ko" to "카메라"),
        "epigraph" to mapOf("ru" to "ЭПИГРАФ", "en" to "QUOTE", "uk" to "ЕПІГРАФ", "be" to "ЭПІГРАФ", "kk" to "ЭПИГРАФ", "es" to "CITA", "de" to "ZITAT", "fr" to "CITATION", "it" to "CITAZIONE", "pt" to "CITAÇÃO", "pl" to "CYTAT", "tr" to "ALINTI", "zh" to "引语", "ja" to "引用", "ko" to "인용구"),
        "music" to mapOf("ru" to "МУЗЫКА", "en" to "MUSIC", "uk" to "МУЗИКА", "be" to "МУЗЫКА", "kk" to "МУЗЫКА", "es" to "MÚSICA", "de" to "MUSIK", "fr" to "MUSIQUE", "it" to "MUSICA", "pt" to "MÚSICA", "pl" to "MUZYKA", "tr" to "MÜZİK", "zh" to "音乐", "ja" to "音楽", "ko" to "음악"),
        "thought" to mapOf("ru" to "МЫСЛИ", "en" to "THOUGHT", "uk" to "ДУМКИ", "be" to "ДУМКІ", "kk" to "ОЙЛАР", "es" to "PENSAMIENTO", "de" to "GEDANKE", "fr" to "PENSÉE", "it" to "PENSIERO", "pt" to "PENSAMENTO", "pl" to "MYŚL", "tr" to "DÜŞÜNCE", "zh" to "内心", "ja" to "心の声", "ko" to "독백"),
        "screen" to mapOf("ru" to "ЭКРАН", "en" to "SCREEN", "uk" to "ЕКРАН", "be" to "ЭКРАН", "kk" to "ЭКРАН", "es" to "PANTALLA", "de" to "BILDSCHIRM", "fr" to "ÉCRAN", "it" to "SCHERMO", "pt" to "TELA", "pl" to "EKRAN", "tr" to "EKRAN", "zh" to "屏幕", "ja" to "画面", "ko" to "화면")
    )

    private val prefixCanonicalGroups: Map<String, Map<String, String>> = mapOf(
        "vo" to mapOf("ru" to "[З.К.]", "en" to "[V.O.]", "uk" to "[З.К.]", "be" to "[З.К.]", "kk" to "[К.С.]", "es" to "[V.O.]", "de" to "[V.O.]", "fr" to "[V.O.]"),
        "flashback" to mapOf("ru" to "[ФЛЕШБЭК]", "en" to "[FLASHBACK]", "uk" to "[ФЛЕШБЕК]", "be" to "[ФЛЭШБЭК]", "kk" to "[ФЛЕШБЭК]", "es" to "[FLASHBACK]", "de" to "[RÜCKBLENDE]", "fr" to "[FLASHBACK]", "zh" to "[闪回]", "ja" to "[回想]", "ko" to "[플래시백]"),
        "title" to mapOf("ru" to "ТИТР:", "en" to "TITLE:", "uk" to "ТИТР:", "be" to "ТЫТР:", "kk" to "ТИТР:", "es" to "TÍTULO:", "de" to "TITEL:", "fr" to "TITRE:", "it" to "TITOLO:", "pt" to "TÍTULO:", "pl" to "NAPIS:", "tr" to "BAŞLIK:", "zh" to "字幕:", "ja" to "字幕:", "ko" to "자막:"),
        "camera" to mapOf("ru" to "КАМЕРА:", "en" to "ANGLE:", "uk" to "КАМЕРА:", "be" to "КАМЕРА:", "kk" to "КАМЕРА:", "es" to "CÁMARA:", "de" to "KAMERA:", "fr" to "CAMÉRA:", "it" to "CAMERA:", "pt" to "CÂMERA:", "zh" to "镜头:", "ja" to "カメラ:", "ko" to "카메라:"),
        "thought" to mapOf("ru" to "(мысленно)", "en" to "(thinking)", "uk" to "(подумки)", "be" to "(у думках)", "kk" to "(ойша)", "es" to "(pensando)", "de" to "(denkend)", "fr" to "(pensée)", "it" to "(pensando)", "pt" to "(pensando)", "pl" to "(w myślach)", "tr" to "(düşünerek)", "zh" to "(内心)", "ja" to "(心の声)", "ko" to "(속마음)"),
        "screen" to mapOf("ru" to "[СМС]:", "en" to "[TEXT]:", "uk" to "[СМС]:", "be" to "[СМС]:", "kk" to "[СМС]:", "es" to "[SMS]:", "de" to "[SMS]:", "fr" to "[SMS]:", "it" to "[SMS]:", "pt" to "[SMS]:", "zh" to "[短信]:", "ja" to "[メッセージ]:", "ko" to "[문자]:")
    )

    private val templateNameCanonicalGroups: Map<String, Map<String, String>> = mapOf(
        "voiceover" to mapOf("ru" to "Закадровый голос", "en" to "Voiceover", "uk" to "Закадровий голос", "be" to "Закадравы голас", "kk" to "Кадрдан тыс дауыс", "es" to "Voz en off", "de" to "Voiceover", "fr" to "Voix off", "it" to "Voce fuori campo", "pt" to "Voz em off", "pl" to "Głos zza kadru", "tr" to "Dış Ses", "zh" to "画外音", "ja" to "ナレーション", "ko" to "보이스오버"),
        "flashback" to mapOf("ru" to "Флешбэк", "en" to "Flashback", "uk" to "Флешбек", "be" to "Флэшбэк", "kk" to "Флешбэк", "es" to "Flashback", "de" to "Rückblende", "fr" to "Flashback", "it" to "Flashback", "pt" to "Flashback", "pl" to "Retrospekcja", "tr" to "Geriye Dönüş", "zh" to "闪回", "ja" to "回想シーン", "ko" to "플래시백"),
        "title" to mapOf("ru" to "Титр на экране", "en" to "On-Screen Title", "uk" to "Титр на екрані", "be" to "Тытр на экране", "kk" to "Экрандағы титр", "es" to "Título en pantalla", "de" to "Titeleinblendung", "fr" to "Titre à l'écran", "it" to "Titolo a schermo", "pt" to "Título na tela", "pl" to "Napis na ekranie", "tr" to "Ekran Yazısı", "zh" to "屏幕字幕", "ja" to "画面テロップ", "ko" to "화면 자막"),
        "sfx" to mapOf("ru" to "Звуковой эффект", "en" to "Sound Effect", "uk" to "Звуковий ефект", "be" to "Гукавы эфект", "kk" to "Дыбыстық эффект", "es" to "Efecto de sonido", "de" to "Soundeffekt", "fr" to "Effet sonore", "it" to "Effetto sonoro", "pt" to "Efeito sonoro", "pl" to "Efekt dźwiękowy", "tr" to "Ses Efekti", "zh" to "音效", "ja" to "効果音", "ko" to "음향 효과"),
        "vfx" to mapOf("ru" to "Визуальный эффект", "en" to "Visual VFX", "uk" to "Візуальний ефект", "be" to "Візуальны эфект", "kk" to "Визуалды эффект", "es" to "Efecto visual VFX", "de" to "Visueller Effekt", "fr" to "Effet visuel VFX", "it" to "Effetto visivo VFX", "pt" to "Efeito visual VFX", "pl" to "Efekt wizualny VFX", "tr" to "Görsel Efekt VFX", "zh" to "视觉特效", "ja" to "視覚効果 VFX", "ko" to "시각 효과 VFX"),
        "director_note" to mapOf("ru" to "Заметка режиссера", "en" to "Director's Note", "uk" to "Нотатка режисера", "be" to "Нататка рэжысёра", "kk" to "Режиссер жазбасы", "es" to "Nota del director", "de" to "Regieanweisung", "fr" to "Note du réalisateur", "it" to "Nota di regia", "pt" to "Nota do diretor", "pl" to "Notatka reżysera", "tr" to "Yönetmen Notu", "zh" to "导演批注", "ja" to "演出ノート", "ko" to "연출 노트"),
        "author_note" to mapOf("ru" to "Заметка автора", "en" to "Author Note", "uk" to "Нотатка автора", "be" to "Нататка аўтара", "kk" to "Автор жазбасы", "es" to "Nota del autor", "de" to "Autorennotiz", "fr" to "Note de l'auteur", "it" to "Nota dell'autore", "pt" to "Nota do autor", "pl" to "Notatka autora", "tr" to "Yazar Notu", "zh" to "作者批注", "ja" to "作者ノート", "ko" to "작가 노트"),
        "camera" to mapOf("ru" to "Камера / Ракурс", "en" to "Camera Shot", "uk" to "Камера / Ракурс", "be" to "Камера / Ракурс", "kk" to "Камера / Ракурс", "es" to "Plano de cámara", "de" to "Kameraeinstellung", "fr" to "Plan caméra", "it" to "Inquadratura", "pt" to "Plano de câmera", "pl" to "Ujęcie kamery", "tr" to "Kamera Planı", "zh" to "镜头角度", "ja" to "カメラカット", "ko" to "카메라 샷"),
        "epigraph" to mapOf("ru" to "Эпиграф / Цитата", "en" to "Epigraph", "uk" to "Епіграф / Цитата", "be" to "Эпіграф / Цытата", "kk" to "Эпиграф / Дәйексөз", "es" to "Epígrafe / Cita", "de" to "Epigraph / Zitat", "fr" to "Épigraphe / Citation", "it" to "Epigrafe / Citazione", "pt" to "Epígrafe / Citação", "pl" to "Epigraf / Cytat", "tr" to "Epigraf / Alıntı", "zh" to "题词 / 引语", "ja" to "エピグラフ / 引用", "ko" to "제사 / 인용구"),
        "music" to mapOf("ru" to "Музыка", "en" to "Music Cue", "uk" to "Музика", "be" to "Музыка", "kk" to "Музыка", "es" to "Música", "de" to "Musik", "fr" to "Musique", "it" to "Musica", "pt" to "Música", "pl" to "Muzyka", "tr" to "Müzik", "zh" to "音乐", "ja" to "音楽", "ko" to "음악"),
        "thought" to mapOf("ru" to "Внутренний монолог", "en" to "Inner Thought", "uk" to "Внутрішній монолог", "be" to "Унутраны маналог", "kk" to "Ішкі монолог", "es" to "Monólogo interior", "de" to "Innerer Monolog", "fr" to "Monologue intérieur", "it" to "Monologo interiore", "pt" to "Monólogo interior", "pl" to "Monolog wewnętrzny", "tr" to "İç Monolog", "zh" to "内心独白", "ja" to "内心の声", "ko" to "내면의 독백"),
        "screen" to mapOf("ru" to "Экран / СМС", "en" to "Screen / SMS", "uk" to "Екран / СМС", "be" to "Экран / СМС", "kk" to "Экран / СМС", "es" to "Pantalla / SMS", "de" to "Bildschirm / SMS", "fr" to "Écran / SMS", "it" to "Schermo / SMS", "pt" to "Tela / SMS", "pl" to "Ekran / SMS", "tr" to "Ekran / SMS", "zh" to "屏幕 / 短信", "ja" to "画面 / メッセージ", "ko" to "화면 / 문자"),
        "my_template" to mapOf("ru" to "Мой шаблон", "en" to "My Template", "uk" to "Мій шаблон", "be" to "Мой шаблон", "kk" to "Менің үлгім", "es" to "Mi plantilla", "de" to "Meine Vorlage", "fr" to "Mon modèle", "it" to "Il mio modello", "pt" to "Meu modelo", "pl" to "Mój szablon", "tr" to "Şablonum", "zh" to "我的模板", "ja" to "マイテンプレート", "ko" to "내 템플릿")
    )

    fun translateBlockBadge(badge: String, lang: String): String {
        val clean = badge.trim()
        if (clean.isEmpty()) return translateBlockBadge("CUSTOM", lang)
        val eff = resolveEffectiveLanguage(lang)
        for ((_, group) in badgeCanonicalGroups) {
            if (group.values.any { it.equals(clean, ignoreCase = true) }) {
                return group[eff] ?: group["en"] ?: clean
            }
        }
        return clean
    }

    fun translateBlockPrefix(prefix: String, lang: String): String {
        val clean = prefix.trim()
        if (clean.isEmpty()) return ""
        val eff = resolveEffectiveLanguage(lang)
        for ((_, group) in prefixCanonicalGroups) {
            if (group.values.any { it.equals(clean, ignoreCase = true) }) {
                return group[eff] ?: group["en"] ?: clean
            }
        }
        return clean
    }

    fun translateTemplateName(name: String, lang: String): String {
        val clean = name.trim()
        if (clean.isEmpty()) return tr("Шаблон", "Template", lang)
        val eff = resolveEffectiveLanguage(lang)
        for ((_, group) in templateNameCanonicalGroups) {
            if (group.values.any { it.equals(clean, ignoreCase = true) }) {
                return group[eff] ?: group["en"] ?: clean
            }
        }
        return translateBlockBadge(clean, lang)
    }

    fun translatePresetTitle(presetId: String, titleRu: String, titleEn: String, lang: String): String {
        val eff = resolveEffectiveLanguage(lang)
        val key = when (presetId) {
            "vo", "voiceover" -> "voiceover"
            "flashback" -> "flashback"
            "title" -> "title"
            "sfx" -> "sfx"
            "vfx" -> "vfx"
            "note" -> "author_note"
            "camera" -> "camera"
            "epigraph" -> "epigraph"
            "music" -> "music"
            "thought" -> "thought"
            "screen" -> "screen"
            else -> presetId
        }
        return templateNameCanonicalGroups[key]?.get(eff)
            ?: if (eff == "ru") titleRu else titleEn
    }

    fun translateIconLabel(iconId: String, labelRu: String, labelEn: String, lang: String): String {
        val eff = resolveEffectiveLanguage(lang)
        if (eff == "ru") return labelRu
        if (eff == "en") return labelEn
        return when (iconId) {
            "tune" -> translateBlockBadge("CUSTOM", lang)
            "mic" -> translateBlockBadge("V.O.", lang)
            "flashback" -> translateBlockBadge("FLASHBACK", lang)
            "subtitles" -> translateBlockBadge("TITLE", lang)
            "sfx" -> "SFX"
            "vfx" -> "VFX"
            "note" -> translateBlockBadge("NOTE", lang)
            "camera" -> translateBlockBadge("CAMERA", lang)
            "quote" -> translateBlockBadge("QUOTE", lang)
            "music" -> translateBlockBadge("MUSIC", lang)
            "thought" -> translateBlockBadge("THOUGHT", lang)
            "screen" -> translateBlockBadge("SCREEN", lang)
            "location" -> when (eff) { "uk" -> "Місце"; "be" -> "Месца"; "kk" -> "Орын"; "es" -> "Lugar"; "de" -> "Ort"; "fr" -> "Lieu"; "it" -> "Luogo"; "pt" -> "Local"; "pl" -> "Miejsce"; "tr" -> "Konum"; "zh" -> "地点"; "ja" -> "場所"; "ko" -> "장소"; else -> labelEn }
            "bookmark" -> when (eff) { "uk" -> "Закладка"; "be" -> "Закладка"; "kk" -> "Бетбелгі"; "es" -> "Marca"; "de" -> "Marke"; "fr" -> "Signet"; "it" -> "Segno"; "pt" -> "Marca"; "pl" -> "Zakładka"; "tr" -> "İşaret"; "zh" -> "书签"; "ja" -> "しおり"; "ko" -> "북마크"; else -> labelEn }
            "magic" -> when (eff) { "uk" -> "Акцент"; "be" -> "Акцэнт"; "kk" -> "Екпін"; "es" -> "Destaque"; "de" -> "Akzent"; "fr" -> "Accent"; "it" -> "Accento"; "pt" -> "Destaque"; "pl" -> "Akcent"; "tr" -> "Vurgu"; "zh" -> "重点"; "ja" -> "強調"; "ko" -> "강조"; else -> labelEn }
            "time" -> when (eff) { "uk" -> "Час"; "be" -> "Час"; "kk" -> "Уақыт"; "es" -> "Tiempo"; "de" -> "Zeit"; "fr" -> "Temps"; "it" -> "Tempo"; "pt" -> "Tempo"; "pl" -> "Czas"; "tr" -> "Zaman"; "zh" -> "时间"; "ja" -> "時間"; "ko" -> "시간"; else -> labelEn }
            "warning" -> when (eff) { "uk" -> "Важливо"; "be" -> "Важна"; "kk" -> "Маңызды"; "es" -> "Alerta"; "de" -> "Wichtig"; "fr" -> "Alerte"; "it" -> "Avviso"; "pt" -> "Alerta"; "pl" -> "Ważne"; "tr" -> "Uyarı"; "zh" -> "重要"; "ja" -> "重要"; "ko" -> "중요"; else -> labelEn }
            else -> labelEn
        }
    }

    fun translateAlignment(alignment: String, lang: String): String {
        val eff = resolveEffectiveLanguage(lang)
        return when (alignment.uppercase()) {
            "CENTER" -> when (eff) {
                "ru", "uk", "kk" -> "ЦЕНТР"
                "be" -> "ЦЭНТР"
                "es", "it", "pt" -> "CENTRO"
                "de" -> "MITTE"
                "fr" -> "CENTRE"
                "pl" -> "ŚRODEK"
                "tr" -> "ORTA"
                "zh" -> "居中"
                "ja" -> "中央"
                "ko" -> "가운데"
                "ar" -> "وسط"
                else -> "CENTER"
            }
            "RIGHT" -> when (eff) {
                "ru" -> "СПРАВА"
                "uk" -> "ПРАВОРУЧ"
                "be" -> "СПРАВА"
                "kk" -> "ОҢ ЖАҚ"
                "es" -> "DER."
                "de" -> "RECHTS"
                "fr" -> "DROITE"
                "it" -> "DESTRA"
                "pt" -> "DIREITA"
                "pl" -> "PRAWO"
                "tr" -> "SAĞ"
                "zh" -> "右对齐"
                "ja" -> "右寄せ"
                "ko" -> "오른쪽"
                "ar" -> "يمين"
                else -> "RIGHT"
            }
            else -> when (eff) {
                "ru" -> "СЛЕВА"
                "uk" -> "ЛІВОРУЧ"
                "be" -> "ЗЛЕВА"
                "kk" -> "СОЛ ЖАҚ"
                "es" -> "IZQ."
                "de" -> "LINKS"
                "fr" -> "GAUCHE"
                "it" -> "SINISTRA"
                "pt" -> "ESQUERDA"
                "pl" -> "LEWO"
                "tr" -> "SOL"
                "zh" -> "左对齐"
                "ja" -> "左寄せ"
                "ko" -> "왼쪽"
                "ar" -> "يسار"
                else -> "LEFT"
            }
        }
    }

    fun translateFontName(fontId: String, lang: String): String {
        return when (fontId.lowercase()) {
            "monospace" -> tr("Сценарный", "Mono", lang).uppercase()
            "sans" -> tr("Без засечек", "Sans", lang).uppercase()
            else -> tr("С засечками", "Serif", lang).uppercase()
        }
    }

    private val scenePlaceholders: Map<String, String> = mapOf(
        "ru" to "ИНТ. КОМНАТА — ДЕНЬ",
        "en" to "INT. ROOM — DAY",
        "uk" to "ІНТ. КІМНАТА — ДЕНЬ",
        "be" to "ІНТ. ПАКОЙ — ДЗЕНЬ",
        "kk" to "ИНТ. БӨЛМЕ — КҮН",
        "es" to "INT. HABITACIÓN — DÍA",
        "de" to "INT. ZIMMER — TAG",
        "fr" to "INT. CHAMBRE — JOUR",
        "it" to "INT. STANZA — GIORNO",
        "pt" to "INT. QUARTO — DIA",
        "pl" to "WN. POKÓJ — DZIEŃ",
        "tr" to "İÇ. ODA — GÜN",
        "zh" to "内景 房间 — 日",
        "ja" to "室内・部屋 — 昼",
        "ko" to "S# 실내. 방 — 낮",
        "ar" to "داخلي. غرفة — نهار"
    )

    fun getDefaultSceneHeading(lang: String): String {
        val eff = resolveEffectiveLanguage(lang)
        return scenePlaceholders[eff] ?: "INT. ROOM — DAY"
    }

    fun isDefaultSceneHeading(text: String): Boolean {
        val norm = text.trim().replace("-", "—").replace("  ", " ")
        return scenePlaceholders.values.any {
            it.replace("-", "—").equals(norm, ignoreCase = true)
        } || norm.equals("ИНТ. КОМНАТА - ДЕНЬ", ignoreCase = true) || norm.equals("INT. ROOM - DAY", ignoreCase = true)
    }

    fun getSceneChips(lang: String): List<String> {
        return when (resolveEffectiveLanguage(lang)) {
            "ru", "kk" -> listOf("ИНТ.", "НАТ.", "ИНТ./НАТ.", "— ДЕНЬ", "— НОЧЬ", "— УТРО", "— ВЕЧЕР")
            "uk" -> listOf("ІНТ.", "НАТ.", "ІНТ./НАТ.", "— ДЕНЬ", "— НІЧ", "— РАНОК", "— ВЕЧІР")
            "be" -> listOf("ІНТ.", "НАТ.", "ІНТ./НАТ.", "— ДЗЕНЬ", "— НОЧ", "— РАНІЦА", "— ВЕЧАР")
            "es" -> listOf("INT.", "EXT.", "INT./EXT.", "— DÍA", "— NOCHE", "— MAÑANA", "— TARDE")
            "de" -> listOf("INT.", "EXT.", "INT./EXT.", "— TAG", "— NACHT", "— MORGEN", "— ABEND")
            "fr" -> listOf("INT.", "EXT.", "INT./EXT.", "— JOUR", "— NUIT", "— MATIN", "— SOIR")
            "it" -> listOf("INT.", "EST.", "INT./EST.", "— GIORNO", "— NOTTE", "— MATTINA", "— SERA")
            "pt" -> listOf("INT.", "EXT.", "INT./EXT.", "— DIA", "— NOITE", "— MANHÃ", "— TARDE")
            "pl" -> listOf("WN.", "PL.", "WN./PL.", "— DZIEŃ", "— NOC", "— RANO", "— WIECZÓR")
            "tr" -> listOf("İÇ.", "DIŞ.", "İÇ./DIŞ.", "— GÜN", "— GECE", "— SABAH", "— AKŞAM")
            "zh" -> listOf("内景.", "外景.", "内外景.", "— 日", "— 夜", "— 晨", "— 晚")
            "ja" -> listOf("室内.", "屋外.", "室内/屋外.", "— 昼", "— 夜", "— 朝", "— 夕")
            "ko" -> listOf("S# 실내.", "S# 야외.", "실내/야외.", "— 낮", "— 밤", "— 아침", "— 저녁")
            else -> listOf("INT.", "EXT.", "INT./EXT.", "— DAY", "— NIGHT", "— MORNING", "— EVENING")
        }
    }

    private val paletteNameRegex = Regex("^(Палитра|Палітра|Palette|Paleta|Tavolozza|Palet|调色板|パレット|팔레트)\\s*(#?\\d+)$", RegexOption.IGNORE_CASE)

    fun translatePaletteName(name: String, lang: String): String {
        val trimmed = name.trim()
        val eff = resolveEffectiveLanguage(lang)
        val match = paletteNameRegex.find(trimmed)
        if (match != null) {
            val numPart = match.groupValues[2]
            return tr("Палитра $numPart", "Palette $numPart", eff)
        }
        return trimmed
    }

    fun translateThemePresetName(nameRu: String, nameEn: String, lang: String): String {
        val eff = resolveEffectiveLanguage(lang)
        if (eff == "ru") return nameRu
        if (eff == "en") return nameEn
        return when (nameEn) {
            "Golden Age" -> when (eff) { "uk" -> "Золотий вік"; "be" -> "Залаты век"; "kk" -> "Алтын ғасыр"; "es" -> "Edad de Oro"; "de" -> "Goldenes Zeitalter"; "fr" -> "Âge d'Or"; "it" -> "Età dell'Oro"; "pt" -> "Era de Ouro"; "pl" -> "Złoty Wiek"; "tr" -> "Altın Çağ"; "zh" -> "黄金时代"; "ja" -> "ゴールデンエイジ"; "ko" -> "황금시대"; else -> nameEn }
            "Cyberpunk" -> when (eff) { "uk" -> "Кіберпанк"; "be" -> "Кіберпанк"; "kk" -> "Киберпанк"; "zh" -> "赛博朋克"; "ja" -> "サイバーパンク"; "ko" -> "사이버펑크"; else -> nameEn }
            "Emerald Forest" -> when (eff) { "uk" -> "Смарагдовий бір"; "be" -> "Смарагдавы бор"; "kk" -> "Зүмірет орман"; "es" -> "Bosque Esmeralda"; "de" -> "Smaragdwald"; "fr" -> "Forêt d'Émeraude"; "it" -> "Foresta Smeraldo"; "pt" -> "Floresta Esmeralda"; "pl" -> "Szmaragdowy Las"; "tr" -> "Zümrüt Orman"; "zh" -> "翡翠森林"; "ja" -> "エメラルドの森"; "ko" -> "에메랄드 숲"; else -> nameEn }
            "Nordic Ocean" -> when (eff) { "uk" -> "Північний океан"; "be" -> "Паўночны акіян"; "kk" -> "Солтүстік мұхит"; "es" -> "Océano Nórdico"; "de" -> "Nordischer Ozean"; "fr" -> "Océan Nordique"; "it" -> "Oceano Nordico"; "pt" -> "Oceano Nórdico"; "pl" -> "Ocean Nordycki"; "tr" -> "Kuzey Okyanusu"; "zh" -> "北欧海洋"; "ja" -> "北欧オーシャン"; "ko" -> "노르딕 오션"; else -> nameEn }
            "Espresso & Books" -> when (eff) { "uk" -> "Еспресо та книги"; "be" -> "Эспрэса і кнігі"; "kk" -> "Эспрессо мен кітаптар"; "es" -> "Espresso y Libros"; "de" -> "Espresso & Bücher"; "fr" -> "Expresso & Livres"; "it" -> "Espresso e Libri"; "pt" -> "Espresso e Livros"; "pl" -> "Espresso i Książki"; "tr" -> "Espresso ve Kitaplar"; "zh" -> "浓缩咖啡与书"; "ja" -> "エスプレッソと本"; "ko" -> "에스프레소와 책"; else -> nameEn }
            "Royal Amethyst" -> when (eff) { "uk" -> "Королівський аметист"; "be" -> "Каралеўскі аметыст"; "kk" -> "Патша аметисі"; "es" -> "Amatista Real"; "de" -> "Königlicher Amethyst"; "fr" -> "Améthyste Royale"; "it" -> "Ametista Reale"; "pt" -> "Ametista Real"; "pl" -> "Królewski Ametyst"; "tr" -> "Kraliyet Ametisti"; "zh" -> "皇家紫水晶"; "ja" -> "ロイヤルアメジスト"; "ko" -> "로열 자수정"; else -> nameEn }
            "Riviera Sunset" -> when (eff) { "uk" -> "Захід на Рив'єрі"; "be" -> "Захад на Рыўеры"; "kk" -> "Ривьерадағы күн батуы"; "es" -> "Atardecer en la Riviera"; "de" -> "Riviera-Sonnenuntergang"; "fr" -> "Coucher sur la Riviera"; "it" -> "Tramonto in Riviera"; "pt" -> "Pôr do Sol na Riviera"; "pl" -> "Zachód na Riwierze"; "tr" -> "Riviera Gün Batımı"; "zh" -> "里维埃拉日落"; "ja" -> "リビエラの夕暮れ"; "ko" -> "리비에라 선셋"; else -> nameEn }
            "Matrix" -> when (eff) { "uk" -> "Матриця"; "be" -> "Матрыца"; "kk" -> "Матрица"; "zh" -> "黑客帝国"; "ja" -> "マトリックス"; "ko" -> "매트릭스"; else -> nameEn }
            "Sakura Bloom" -> when (eff) { "uk" -> "Цвітіння сакури"; "be" -> "Квітненне сакуры"; "kk" -> "Сакура гүлдеуі"; "es" -> "Flor de Cerezo"; "de" -> "Sakura-Blüte"; "fr" -> "Fleur de Cerisier"; "it" -> "Fiore di Ciliegio"; "pt" -> "Flor de Cerejeira"; "pl" -> "Kwitnąca Sakura"; "tr" -> "Sakura Çiçeği"; "zh" -> "樱花盛开"; "ja" -> "桜ブルーム"; "ko" -> "벚꽃 블룸"; else -> nameEn }
            "Paper Classic" -> when (eff) { "uk" -> "Паперова класика"; "be" -> "Папяровая класіка"; "kk" -> "Қағаз классикасы"; "es" -> "Papel Clásico"; "de" -> "Papier-Klassiker"; "fr" -> "Papier Classique"; "it" -> "Carta Classica"; "pt" -> "Papel Clássico"; "pl" -> "Klasyczny Papier"; "tr" -> "Klasik Kağıt"; "zh" -> "经典纸张"; "ja" -> "ペーパークラシック"; "ko" -> "페이퍼 클래식"; else -> nameEn }
            "Warm Parchment" -> when (eff) { "uk" -> "Теплий пергамент"; "be" -> "Цёплы пергамент"; "kk" -> "Жылы пергамент"; "es" -> "Pergamino Cálido"; "de" -> "Warmes Pergament"; "fr" -> "Parchemin Chaud"; "it" -> "Pergamena Calda"; "pt" -> "Pergaminho Quente"; "pl" -> "Ciepły Pergamin"; "tr" -> "Sıcak Parşömen"; "zh" -> "温暖羊皮纸"; "ja" -> "ウォームパーチメント"; "ko" -> "따뜻한 양피지"; else -> nameEn }
            "Cosmic Indigo" -> when (eff) { "uk" -> "Космічний індиго"; "be" -> "Касмічны індыга"; "kk" -> "Ғарыштық индиго"; "es" -> "Índigo Cósmico"; "de" -> "Kosmisches Indigo"; "fr" -> "Indigo Cosmique"; "it" -> "Indaco Cosmico"; "pt" -> "Índigo Cósmico"; "pl" -> "Kosmiczne Indygo"; "tr" -> "Kozmik İndigo"; "zh" -> "宇宙靛蓝"; "ja" -> "コズミックインディゴ"; "ko" -> "코스믹 인디고"; else -> nameEn }
            else -> nameEn
        }
    }

    fun translateBgToneName(id: String, nameRu: String, nameEn: String, lang: String): String {
        val eff = resolveEffectiveLanguage(lang)
        if (eff == "ru") return nameRu
        if (eff == "en") return nameEn
        return when (id) {
            "oled" -> when (eff) { "uk" -> "OLED Чорний"; "be" -> "OLED Чорны"; "kk" -> "OLED Қара"; "es" -> "Negro OLED"; "de" -> "OLED-Schwarz"; "fr" -> "Noir OLED"; "it" -> "Nero OLED"; "pt" -> "Preto OLED"; "pl" -> "Czerń OLED"; "tr" -> "OLED Siyah"; "zh" -> "OLED 纯黑"; "ja" -> "OLED ブラック"; "ko" -> "OLED 블랙"; else -> nameEn }
            "obsidian" -> when (eff) { "uk" -> "Обсидіан"; "be" -> "Абсідыян"; "kk" -> "Обсидиан"; "zh" -> "黑曜石"; "ja" -> "オブシディアン"; "ko" -> "옵시디언"; else -> nameEn }
            "midnight" -> when (eff) { "uk" -> "Опівнічний індиго"; "be" -> "Паўночны індыга"; "kk" -> "Түнгі индиго"; "es" -> "Índigo Medianoche"; "de" -> "Mitternachtsindigo"; "fr" -> "Indigo de Minuit"; "it" -> "Indaco Mezzanotte"; "pt" -> "Índigo Meia-Noite"; "pl" -> "Nocne Indygo"; "tr" -> "Gece İndigosu"; "zh" -> "午夜靛蓝"; "ja" -> "ミッドナイト"; "ko" -> "미드나잇 인디고"; else -> nameEn }
            "charcoal" -> when (eff) { "uk" -> "Графітовий сланець"; "be" -> "Графітавы сланец"; "kk" -> "Графит тақтатас"; "es" -> "Pizarra Carbón"; "de" -> "Anthrazit-Schiefer"; "fr" -> "Ardoise Anthracite"; "it" -> "Ardesia Antracite"; "pt" -> "Ardósia Carvão"; "pl" -> "Grafitowy Łupek"; "tr" -> "Kömür Kayrak"; "zh" -> "炭灰石板"; "ja" -> "チャコールスレート"; "ko" -> "차콜 슬레이트"; else -> nameEn }
            "espresso" -> when (eff) { "uk" -> "Теплий еспрессо"; "be" -> "Цёплы эспрэса"; "kk" -> "Жылы эспрессо"; "es" -> "Espresso Cálido"; "de" -> "Warmer Espresso"; "fr" -> "Expresso Chaud"; "it" -> "Espresso Caldo"; "pt" -> "Espresso Quente"; "pl" -> "Ciepłe Espresso"; "tr" -> "Sıcak Espresso"; "zh" -> "温暖浓缩咖啡"; "ja" -> "ウォームエスプレッソ"; "ko" -> "웜 에스프레소"; else -> nameEn }
            "forest" -> when (eff) { "uk" -> "Смарагдова хаща"; "be" -> "Смарагдавы гушчар"; "kk" -> "Зүмірет орман"; "es" -> "Bosque Esmeralda"; "de" -> "Smaragdwald"; "fr" -> "Forêt d'Émeraude"; "it" -> "Foresta Smeraldo"; "pt" -> "Floresta Esmeralda"; "pl" -> "Szmaragdowy Las"; "tr" -> "Zümrüt Orman"; "zh" -> "翡翠森林"; "ja" -> "エメラルドの森"; "ko" -> "에메랄드 숲"; else -> nameEn }
            "white" -> when (eff) { "uk" -> "Чистий білий"; "be" -> "Чысты белы"; "kk" -> "Таза ақ"; "es" -> "Blanco Puro"; "de" -> "Reines Weiß"; "fr" -> "Blanc Pur"; "it" -> "Bianco Puro"; "pt" -> "Branco Puro"; "pl" -> "Czysta Biel"; "tr" -> "Saf Beyaz"; "zh" -> "纯白"; "ja" -> "ピュアホワイト"; "ko" -> "퓨어 화이트"; else -> nameEn }
            "linen" -> when (eff) { "uk" -> "Світлий льон"; "be" -> "Светлы лён"; "kk" -> "Ашық зығыр"; "es" -> "Lino Claro"; "de" -> "Helles Leinen"; "fr" -> "Lin Clair"; "it" -> "Lino Chiaro"; "pt" -> "Linho Claro"; "pl" -> "Jasny Len"; "tr" -> "Açık Keten"; "zh" -> "浅亚麻"; "ja" -> "ライトリネン"; "ko" -> "라이트 리넨"; else -> nameEn }
            "mist" -> when (eff) { "uk" -> "Крижаний туман"; "be" -> "Ледзяны туман"; "kk" -> "Мұзды тұман"; "es" -> "Niebla Helada"; "de" -> "Eisnebel"; "fr" -> "Brume Glacée"; "it" -> "Nebbia Ghiacciata"; "pt" -> "Névoa Gelada"; "pl" -> "Lodowa Mgła"; "tr" -> "Buz Sisi"; "zh" -> "冰雾"; "ja" -> "アイスミスト"; "ko" -> "아이스 미스트"; else -> nameEn }
            "ivory" -> when (eff) { "uk" -> "Слонова кістка"; "be" -> "Слановая косць"; "kk" -> "Піл сүйегі"; "es" -> "Marfil Cálido"; "de" -> "Warmes Elfenbein"; "fr" -> "Ivoire Chaud"; "it" -> "Avorio Caldo"; "pt" -> "Marfim Quente"; "pl" -> "Kość Słoniowa"; "tr" -> "Sıcak Fildişi"; "zh" -> "温暖象牙白"; "ja" -> "ウォームアイボリー"; "ko" -> "웜 아이보리"; else -> nameEn }
            "parchment" -> when (eff) { "uk" -> "Теплий пергамент"; "be" -> "Цёплы пергамент"; "kk" -> "Жылы пергамент"; "es" -> "Pergamino Cálido"; "de" -> "Warmes Pergament"; "fr" -> "Parchemin Chaud"; "it" -> "Pergamena Calda"; "pt" -> "Pergaminho Quente"; "pl" -> "Ciepły Pergamin"; "tr" -> "Sıcak Parşömen"; "zh" -> "温暖羊皮纸"; "ja" -> "ウォームパーチメント"; "ko" -> "따뜻한 양피지"; else -> nameEn }
            "sage" -> when (eff) { "uk" -> "М'яка шавлія"; "be" -> "Мяккая шалфея"; "kk" -> "Жұмсақ шалфей"; "es" -> "Salvia Suave"; "de" -> "Sanfter Salbei"; "fr" -> "Sauge Douce"; "it" -> "Salvia Morbida"; "pt" -> "Sálvia Suave"; "pl" -> "Miękka Szałwia"; "tr" -> "Yumuşak Adaçayı"; "zh" -> "柔和鼠尾草"; "ja" -> "ソフトセージ"; "ko" -> "소프트 세이지"; else -> nameEn }
            "snow" -> when (eff) { "uk" -> "Чистий сніг"; "be" -> "Чысты снег"; "kk" -> "Таза қар"; "es" -> "Nieve Pura"; "de" -> "Reiner Schnee"; "fr" -> "Neige Pure"; "it" -> "Neve Pura"; "pt" -> "Neve Pura"; "pl" -> "Czysty Śnieg"; "tr" -> "Saf Kar"; "zh" -> "纯白雪色"; "ja" -> "ピュアスノー"; "ko" -> "퓨어 스노우"; else -> nameEn }
            else -> tr(nameRu, nameEn, eff)
        }
    }

    private val defaultAuthorNames = mapOf(
        "ru" to "Писатель", "en" to "Author", "uk" to "Письменник", "be" to "Пісьменнік", "kk" to "Жазушы",
        "es" to "Autor", "de" to "Autor", "fr" to "Auteur", "it" to "Autore", "pt" to "Autor",
        "pl" to "Pisarz", "tr" to "Yazar", "zh" to "作家", "ja" to "作家", "ko" to "작가", "ar" to "مؤلف"
    )

    private val defaultAuthorBios = mapOf(
        "ru" to "Вдохновение рождается во время работы.",
        "en" to "Inspiration is born while working.",
        "uk" to "Натхнення народжується під час роботи.",
        "be" to "Натхненне нараджаецца падчас працы.",
        "kk" to "Шабыт жұмыс барысында туады.",
        "es" to "La inspiración nace mientras se trabaja.",
        "de" to "Inspiration entsteht während der Arbeit.",
        "fr" to "L'inspiration naît en travaillant.",
        "it" to "L'ispirazione nasce lavorando.",
        "pt" to "A inspiração nasce durante o trabalho.",
        "pl" to "Inspiracja rodzi się podczas pracy.",
        "tr" to "İlham çalışırken doğar.",
        "zh" to "灵感在创作中诞生。",
        "ja" to "インスピレーションは執筆の中に生まれる。",
        "ko" to "영감은 집필하는 과정에서 탄생합니다.",
        "ar" to "يولد الإلهام أثناء العمل."
    )

    fun synchronizeAuthorName(name: String, lang: String): String {
        val eff = resolveEffectiveLanguage(lang)
        val trimmed = name.trim()
        if (trimmed.isEmpty() || defaultAuthorNames.values.any { it.equals(trimmed, ignoreCase = true) }) {
            return defaultAuthorNames[eff] ?: "Author"
        }
        return name
    }

    fun synchronizeAuthorBio(bio: String, lang: String): String {
        val eff = resolveEffectiveLanguage(lang)
        val trimmed = bio.trim()
        if (trimmed.isEmpty() || defaultAuthorBios.values.any { it.equals(trimmed, ignoreCase = true) }) {
            return defaultAuthorBios[eff] ?: "Inspiration is born while working."
        }
        return bio
    }
}

