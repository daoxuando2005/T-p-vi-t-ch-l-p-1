package com.hocchu.mobile.data

import android.content.SharedPreferences
import androidx.core.content.edit

/** Separate from the free-reading preference. Free practice never unlocks journey lessons. */
class LessonProgressStore(private val prefs: SharedPreferences) {
    fun readAll() = DemoContent.lessons.map { LessonProgress.decode(prefs.getString("lesson.v1.${it.letter}", null)) }
    fun save(index: Int, next: LessonProgress): List<LessonProgress> {
        val all = readAll()
        if (!LessonProgress.unlocked(index, all)) return all
        val old = all[index]
        if ((old.read && !next.read) || next.writes < old.writes || next.words < old.words || next.sentences < old.sentences) return all
        val normalized = LessonProgress.decode(next.encode())
        prefs.edit { putString("lesson.v1.${DemoContent.lessons[index].letter}", normalized.encode()) }
        return all.toMutableList().also { it[index] = normalized }
    }
}
