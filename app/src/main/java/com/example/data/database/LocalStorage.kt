package com.example.data.database

import android.content.Context
import androidx.core.content.edit
import com.example.data.model.JsonUtils
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types

@JsonClass(generateAdapter = true)
data class SavedPromptTemplate(
    val id: String,
    val title: String,
    val template: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

object LocalStorage {
    private const val PREFS_NAME = "local_storage_prefs"
    private const val TEMPLATES_KEY = "saved_prompt_templates"

    fun getTemplates(context: Context): List<SavedPromptTemplate> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(TEMPLATES_KEY, null) ?: return getSeedTemplates()
        return try {
            val type = Types.newParameterizedType(List::class.java, SavedPromptTemplate::class.java)
            val list = JsonUtils.moshi.adapter<List<SavedPromptTemplate>>(type).fromJson(json)
            if (list.isNullOrEmpty()) getSeedTemplates() else list
        } catch (e: Exception) {
            getSeedTemplates()
        }
    }

    fun saveTemplates(context: Context, templates: List<SavedPromptTemplate>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val type = Types.newParameterizedType(List::class.java, SavedPromptTemplate::class.java)
            val json = JsonUtils.moshi.adapter<List<SavedPromptTemplate>>(type).toJson(templates)
            prefs.edit {
                putString(TEMPLATES_KEY, json)
            }
        } catch (e: Exception) {
            // Ignore error
        }
    }

    private fun getSeedTemplates(): List<SavedPromptTemplate> {
        return listOf(
            SavedPromptTemplate(
                id = "seed_1",
                title = "Short Video Hook Generator",
                template = "Create an engaging visual-first hook about {{topic}} for {{audience}} targeting {{platform}}. Ideal tone should be {{tone}}.",
                description = "Generates high CTR hooks for TikTok, Reels, or YouTube Shorts."
            ),
            SavedPromptTemplate(
                id = "seed_2",
                title = "Email Marketing Spark",
                template = "Write a promotional product newsletter about {{topic}} specialized for {{audience}}. Ensure clear Call to Action and a {{tone}} tone.",
                description = "High conversion promotional e-mail formula."
            ),
            SavedPromptTemplate(
                id = "seed_3",
                title = "Aesthetic Copy Refiner",
                template = "Refine the following paragraph into a bold, minimalist, and punchy tagline: {{input_text}}",
                description = "Condenses paragraphs into short elegant marketing slogans."
            )
        )
    }
}
