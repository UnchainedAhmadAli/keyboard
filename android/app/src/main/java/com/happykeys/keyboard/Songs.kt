package com.happykeys.keyboard

object Songs {
    private val N = mapOf(
        "C4" to 261.63, "D4" to 293.66, "E4" to 329.63, "F4" to 349.23, "G4" to 392.0,
        "A4" to 440.0, "B4" to 493.88, "C5" to 523.25, "D5" to 587.33, "E5" to 659.25,
        "F5" to 698.46, "G5" to 783.99, "A5" to 880.0,
    )

    private fun seq(s: String): List<Double> = s.split(" ").map { N.getValue(it) }

    val all: LinkedHashMap<String, List<Double>> = linkedMapOf(
        "Happy Birthday" to seq("G4 G4 A4 G4 C5 B4 G4 G4 A4 G4 D5 C5 G4 G4 G5 E5 C5 B4 A4 F5 F5 E5 C5 D5 C5"),
        "Twinkle Twinkle" to seq("C4 C4 G4 G4 A4 A4 G4 F4 F4 E4 E4 D4 D4 C4 G4 G4 F4 F4 E4 E4 D4 G4 G4 F4 F4 E4 E4 D4 C4 C4 G4 G4 A4 A4 G4 F4 F4 E4 E4 D4 D4 C4"),
        "Jingle Bells" to seq("E4 E4 E4 E4 E4 E4 E4 G4 C4 D4 E4 F4 F4 F4 F4 F4 E4 E4 E4 E4 D4 D4 E4 D4 G4"),
        "Ode to Joy" to seq("E4 E4 F4 G4 G4 F4 E4 D4 C4 C4 D4 E4 E4 D4 D4 E4 E4 F4 G4 G4 F4 E4 D4 C4 C4 D4 E4 D4 C4 C4"),
        "Piano scale" to seq("C4 D4 E4 F4 G4 A4 B4 C5 D5 E5 F5 G5 A5 G5 F5 E5 D5 C5 B4 A4 G4 F4 E4 D4"),
    )

    val names: List<String> get() = all.keys.toList()
    val allFrequencies: List<Double> get() = N.values.toList()
}

enum class Kind { CHAR, SHIFT, BACKSPACE, ENTER, LANG, SYMBOLS, LETTERS, SPACE }

data class KeyDef(val id: String, val kind: Kind, val weight: Float = 1f)

object Layouts {
    private fun chars(vararg s: String) = s.map { KeyDef(it, Kind.CHAR) }
    private fun shift() = KeyDef("shift", Kind.SHIFT, 1.5f)
    private fun back() = KeyDef("back", Kind.BACKSPACE, 1.5f)
    private fun bottom(symLabel: String, comma: String, symKind: Kind) = listOf(
        KeyDef("lang", Kind.LANG, 1.5f),
        KeyDef(symLabel, symKind, 1.5f),
        KeyDef("space", Kind.SPACE, 5f),
        KeyDef(comma, Kind.CHAR, 1f),
        KeyDef(".", Kind.CHAR, 1f),
        KeyDef("enter", Kind.ENTER, 1.5f),
    )

    val enLetters: List<List<KeyDef>> = listOf(
        chars("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        chars("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        chars("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf(shift()) + chars("z", "x", "c", "v", "b", "n", "m") + listOf(back()),
        bottom("123", ",", Kind.SYMBOLS),
    )
    val enShift: Map<String, String> = mapOf(
        "1" to "!", "2" to "@", "3" to "#", "4" to "$", "5" to "%",
        "6" to "^", "7" to "&", "8" to "*", "9" to "(", "0" to ")",
    )

    val arLetters: List<List<KeyDef>> = listOf(
        chars("١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "٠"),
        chars("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د"),
        chars("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط", "ذ"),
        listOf(shift()) + chars("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ") + listOf(back()),
        bottom("123", "،", Kind.SYMBOLS),
    )
    val arShift: Map<String, String> = mapOf(
        "ض" to "َ", "ص" to "ً", "ث" to "ُ", "ق" to "ٌ", "ف" to "لإ", "غ" to "إ", "ع" to "‘",
        "ه" to "÷", "خ" to "×", "ح" to "؛", "ج" to "<", "د" to ">", "ذ" to "ّ",
        "ش" to "ِ", "س" to "ٍ", "ي" to "]", "ب" to "[", "ل" to "لأ", "ا" to "أ", "ت" to "ـ",
        "ن" to "،", "م" to "/", "ك" to ":", "ط" to "\"",
        "ئ" to "~", "ء" to "ْ", "ؤ" to "}", "ر" to "{", "لا" to "لآ", "ى" to "آ", "ة" to "’",
        "و" to ",", "ز" to ".", "ظ" to "؟",
    )

    fun symbols(comma: String): List<List<KeyDef>> = listOf(
        chars("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        chars("-", "/", ":", ";", "(", ")", "$", "&", "@", "\""),
        chars(".", ",", "?", "!", "'", "#", "%", "+", "="),
        chars("*", "_", "~", "<", ">", "[", "]") + listOf(back()),
        bottom("abc", comma, Kind.LETTERS),
    )
}
