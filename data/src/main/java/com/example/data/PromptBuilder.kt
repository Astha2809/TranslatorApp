package com.example.data

object PromptBuilder {

    /**
     * Creates a prompt to translate the given text into the specified language.
     */
    fun buildTranslatePrompt(text: String, language: String): String {
        return """
            Translate the following text to $language.

            Rules:
            1. Return the complete translated sentence.
            2. Do not stop early.
            3. The translation should be formal and polite.
            4. Do not use slang or informal expressions.
            5. If the text cannot be translated accurately, respond with only the phrase "Translation not possible."

            Text to translate: "$text"
        """.trimIndent()
    }

    /**
     * Creates a prompt that asks the model to generate a four-line poem about a topic.
     */
    fun buildPoemPrompt(topic: String): String {
        return "Write a four-line poem about the following topic: $topic"
    }

    /**
     * Example of another prompt builder to showcase LLM skills.
     * This one asks the model to summarize a text in a specified number of sentences.
     */
    fun buildSummarizePrompt(text: String, sentences: Int): String {
        return "Summarize the following text in $sentences sentences: $text"
    }

    /**
     * Example of another prompt builder.
     * This one asks the model to act as a specific character.
     */
    fun buildActAsPrompt(character: String, question: String): String {
        return "I want you to act as $character. I want you to respond and answer like $character. $question"
    }
}
