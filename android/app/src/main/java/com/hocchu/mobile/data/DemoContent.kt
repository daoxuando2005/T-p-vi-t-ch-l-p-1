package com.hocchu.mobile.data

// Explicit prototype fixtures. Replace through a repository when connecting the API.
data class LetterLesson(val letter: String, val sound: String, val word: String, val picture: String)
object DemoContent {
    val lessons = listOf(
        LetterLesson("A", "a", "Con cá", "🐟"),
        LetterLesson("Ă", "á", "Cái khăn", "🧣"),
        LetterLesson("Â", "ớ", "Cái cân", "⚖️"),
        LetterLesson("B", "bờ", "Con bò", "🐮"),
        LetterLesson("C", "cờ", "Con cua", "🦀"),
        LetterLesson("D", "dờ", "Con dê", "🐐"),
        LetterLesson("Đ", "đờ", "Đu đủ", "🥭"),
        LetterLesson("E", "e", "Xe đạp", "🚲"),
        LetterLesson("Ê", "ê", "Quả lê", "🍐"),
        LetterLesson("G", "gờ", "Con gà", "🐔"),
        LetterLesson("H", "hờ", "Bông hoa", "🌼"),
        LetterLesson("I", "i", "Viên bi", "🔵"),
        LetterLesson("K", "ca", "Cái kéo", "✂️"),
        LetterLesson("L", "lờ", "Chiếc lá", "🍃"),
        LetterLesson("M", "mờ", "Con mèo", "🐱"),
        LetterLesson("N", "nờ", "Cái nơ", "🎀"),
        LetterLesson("O", "o", "Quả nho", "🍇"),
        LetterLesson("Ô", "ô", "Cái ô", "☂️"),
        LetterLesson("Ơ", "ơ", "Lá cờ", "🚩"),
        LetterLesson("P", "pờ", "Đèn pin", "🔦"),
        LetterLesson("Q", "quờ", "Quả quýt", "🍊"),
        LetterLesson("R", "rờ", "Con rùa", "🐢"),
        LetterLesson("S", "sờ", "Ngôi sao", "⭐"),
        LetterLesson("T", "tờ", "Quả táo", "🍎"),
        LetterLesson("U", "u", "Cái mũ", "🧢"),
        LetterLesson("Ư", "ư", "Sư tử", "🦁"),
        LetterLesson("V", "vờ", "Con voi", "🐘"),
        LetterLesson("X", "xờ", "Xe buýt", "🚌"),
        LetterLesson("Y", "i dài", "Y tá", "🧑‍⚕️")
    )
}
