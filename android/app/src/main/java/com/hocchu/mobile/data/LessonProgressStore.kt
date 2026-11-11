package com.hocchu.mobile.data

import android.content.SharedPreferences
import androidx.core.content.edit

/** Separate from the free-reading preference. Free practice never unlocks journey lessons. */
class LessonProgressStore(private val prefs: SharedPreferences) {
    fun readAll() = AlphabetContent.lessons.map { LessonProgress.decode(prefs.getString("lesson.v1.${it.letter}", null)) }
    /** Merge only earned steps; restoring on another device never creates new activity events. */
    fun merge(remote: List<LessonProgress>): List<LessonProgress> = synchronized(prefs) {
        val local=readAll()
        val merged=LessonProgress.merge(local,remote)
        val editor=prefs.edit()
        merged.forEachIndexed {i,p->editor.putString("lesson.v1.${AlphabetContent.lessons[i].letter}",p.encode())}
        check(editor.commit()) {"Could not persist restored progress"}
        merged
    }
    fun save(index: Int, next: LessonProgress): List<LessonProgress> = synchronized(prefs) {
        val all = readAll()
        if (!LessonProgress.unlocked(index, all)) return@synchronized all
        val old = all[index]
        if ((old.read && !next.read) || next.writes < old.writes || next.words < old.words || next.sentences < old.sentences) return@synchronized all
        val normalized = LessonProgress.decode(next.encode())
        prefs.edit { putString("lesson.v1.${AlphabetContent.lessons[index].letter}", normalized.encode()) }
        all.toMutableList().also { it[index] = normalized }
    }
}
