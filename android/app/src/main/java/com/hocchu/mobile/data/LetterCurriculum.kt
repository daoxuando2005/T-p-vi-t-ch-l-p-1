package com.hocchu.mobile.data

/** Authored practice content. Each row pairs a word with its own short sentence.
 * Audio/pictures scaffold vocabulary not yet independently decodable by beginners. */
data class WordPractice(val text: String, val picture: String, val sentence: List<String>) {
    fun withBlank(letter: String): String {
        val position = text.indexOf(letter.lowercase())
        require(position >= 0) { "$text must contain $letter" }
        return text.replaceRange(position, position + letter.length, "□")
    }
}

object LetterCurriculum {
    private fun word(text: String, picture: String, sentence: String) = WordPractice(text, picture, sentence.split('|'))
    private val content = listOf(
        listOf(word("ba", "👨", "Bé|yêu|ba."), word("hoa", "🌼", "Hoa|rất|thơm."), word("na", "🍈", "Quả|na|rất ngon.")),
        listOf(word("khăn", "🧣", "Bé|có|khăn."), word("chăn", "🛏️", "Chăn|rất|ấm."), word("măng", "🎋", "Mẹ|nấu|măng.")),
        listOf(word("cân", "⚖️", "Mẹ|có|cái cân."), word("chân", "🦶", "Bé|rửa|chân."), word("sân", "🏡", "Bé|chơi|ngoài sân.")),
        listOf(word("bò", "🐮", "Con bò|ăn|cỏ."), word("bé", "🧒", "Bé|đang|học."), word("bóng", "⚽", "Bé|đá|bóng.")),
        listOf(word("cá", "🐟", "Cá|đang|bơi."), word("cua", "🦀", "Con cua|có|hai càng."), word("cam", "🍊", "Bé|ăn|cam.")),
        listOf(word("dê", "🐐", "Con dê|ăn|cỏ."), word("dưa", "🍉", "Bé|ăn|dưa."), word("dừa", "🥥", "Mẹ|mua|quả dừa.")),
        listOf(word("đò", "🛶", "Đò|qua|sông."), word("đèn", "💡", "Bé|bật|đèn."), word("đũa", "🥢", "Mẹ|cầm|đũa.")),
        listOf(word("xe", "🚲", "Bé|đi|xe."), word("ve", "🪲", "Ve|kêu|trên cây."), word("me", "🌳", "Quả me|có|vị chua.")),
        listOf(word("dê", "🐐", "Dê|đang|ăn cỏ."), word("lê", "🍐", "Bé|ăn|quả lê."), word("bê", "🐄", "Bê|đi|theo mẹ.")),
        listOf(word("gà", "🐔", "Gà|đang|gáy."), word("gạo", "🍚", "Mẹ|vo|gạo."), word("gấu", "🧸", "Bé|ôm|gấu bông.")),
        listOf(word("hoa", "🌸", "Hoa|đang|nở."), word("hổ", "🐯", "Hổ|sống|trong rừng."), word("hươu", "🦌", "Hươu|ăn|lá cây.")),
        listOf(word("bi", "🔵", "Bé|chơi|bi."), word("pin", "🔋", "Đèn|cần|pin."), word("chim", "🐦", "Chim|đang|hót.")),
        listOf(word("kéo", "✂️", "Cô|cầm|kéo."), word("kem", "🍦", "Bé|ăn|kem."), word("kiến", "🐜", "Kiến|bò|trên lá.")),
        listOf(word("lá", "🍃", "Lá|có|màu xanh."), word("lê", "🍐", "Quả lê|rất|ngọt."), word("lợn", "🐷", "Lợn|đang|ăn.")),
        listOf(word("mèo", "🐱", "Mèo|đang|ngủ."), word("mũ", "🧢", "Bé|đội|mũ."), word("mưa", "🌧️", "Mưa|rơi|ngoài sân.")),
        listOf(word("nơ", "🎀", "Bé|cài|nơ."), word("na", "🍈", "Mẹ|mua|quả na."), word("nai", "🦌", "Nai|ăn|cỏ.")),
        listOf(word("con bò", "🐮", "Con bò|đang|ăn cỏ."), word("nho", "🍇", "Bé|ăn|nho."), word("voi", "🐘", "Voi|có|vòi dài.")),
        listOf(word("tô", "🥣", "Bé|cầm|cái tô."), word("ngô", "🌽", "Mẹ|luộc|ngô."), word("xô", "🪣", "Bé|xách|xô.")),
        listOf(word("nơ", "🎀", "Chiếc nơ|rất|đẹp."), word("mơ", "🍑", "Quả mơ|có|vị chua."), word("bơ", "🥑", "Bé|ăn|quả bơ.")),
        listOf(word("pin", "🔋", "Bố|thay|pin."), word("đèn pin", "🔦", "Bé|bật|đèn pin."), word("hộp", "📦", "Bé|mở|hộp.")),
        listOf(word("quạt", "🪭", "Bé|bật|quạt."), word("quần", "👖", "Bé|mặc|quần."), word("quả cam", "🍊", "Mẹ|bóc|quả cam.")),
        listOf(word("rùa", "🐢", "Rùa|bò|rất chậm."), word("rau", "🥬", "Mẹ|rửa|rau."), word("rổ", "🧺", "Bé|cầm|cái rổ.")),
        listOf(word("sao", "⭐", "Sao|sáng|trên trời."), word("sóc", "🐿️", "Sóc|chuyền|cành."), word("sư tử", "🦁", "Sư tử|sống|trên đồng cỏ.")),
        listOf(word("táo", "🍎", "Bé|ăn|táo."), word("tôm", "🦐", "Tôm|bơi|dưới nước."), word("túi", "👜", "Mẹ|xách|túi.")),
        listOf(word("cua", "🦀", "Cua|bò|trên cát."), word("su su", "🥒", "Mẹ|gọt|su su."), word("đu quay", "🎠", "Bé|chơi|đu quay.")),
        listOf(word("sư tử", "🦁", "Sư tử|có|bờm."), word("thư", "✉️", "Bé|nhận|thư."), word("cưa", "🪚", "Bố|dùng|cưa.")),
        listOf(word("voi", "🐘", "Voi|đang|uống nước."), word("vịt", "🦆", "Vịt|bơi|trong ao."), word("vở", "📓", "Bé|mở|vở.")),
        listOf(word("xe", "🚲", "Xe|đỗ|trước nhà."), word("xôi", "🍚", "Bé|ăn|xôi."), word("xoài", "🥭", "Quả xoài|rất|thơm.")),
        listOf(word("y tá", "🧑‍⚕️", "Y tá|chăm sóc|em bé."), word("cây", "🌳", "Cây|có|nhiều lá."), word("xe máy", "🛵", "Bố|đi|xe máy."))
    )
    fun words(index: Int): List<WordPractice> = content[index]
    fun options(index: Int, question: Int): List<String> {
        val answer = DemoContent.lessons[index].letter.lowercase()
        val distractors = listOf("a", "ă", "â", "b", "d", "đ", "c", "e", "ê", "o", "ô", "ơ", "u", "ư", "n", "m", "p", "q", "s", "x")
            .filter { it != answer }
        return (listOf(answer) + List(3) { distractors[(index + question * 3 + it) % distractors.size] })
            .let { list -> List(4) { list[(it + question + index) % 4] } }
    }
    /** Deterministic tile order survives recreation. Tile identity supports repeated words. */
    fun tileOrder(index: Int, question: Int): List<Int> {
        val size = words(index)[question].sentence.size
        return List(size) { (it + 1 + question % (size - 1)) % size }
    }
    fun sentenceCorrect(index: Int, question: Int, chosen: List<Int>): Boolean =
        chosen == words(index)[question].sentence.indices.toList()
}
