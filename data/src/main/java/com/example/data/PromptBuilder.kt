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
     * Creates a prompt that asks the model to generate a caption for an image based on a specified style.
     */
//    fun buildDescribeImagePrompt(style: String): String {
//        return when (style) {
//            "Short" -> "Generate a very short, one-sentence description for this image."
//            "Detailed" -> "Generate a detailed and comprehensive description for this image, covering all key elements."
//            "Funny" -> "Generate a short,funny and humorous caption for this image."
//            "Professional" -> "Generate a professional and formal caption for this image, suitable for a business context."
//            "Accessibility-friendly" -> "Generate an accessibility-friendly caption for this image, focusing on clear visual details for someone who cannot see it."
//            else -> "Generate a clear description for this image."
//        }
//    }

    fun buildDescribeImagePrompt(style: String): String {
        val baseRules = """
        Describe only what is clearly visible in the image.
        Do not assume or invent details.
        If something is unclear, explicitly say it is unclear.
    """.trimIndent()

        return when (style) {
            "Short" -> """
            $baseRules
            Generate a single-sentence description (max 15 words).
        """.trimIndent()

            "Detailed" -> """
            $baseRules
            Generate a detailed description in 3–4 sentences.
            First list the main objects, then describe the scene.
            Each point must be on a separate line.
            Do not combine multiple points in one line.
        """.trimIndent()

            "Funny" -> """
            $baseRules
            Generate a short, lighthearted caption.
            Use gentle humor but do not add fictional elements.
        """.trimIndent()

            "Professional" -> """
            $baseRules
            Generate a formal, professional caption suitable for business use.
            Avoid emojis, slang, or exaggeration.
        """.trimIndent()

            "Accessibility-friendly" -> """
            $baseRules
            Generate an accessibility-friendly description.
            Avoid color-only descriptions.
            Mention spatial relationships and object positions.
            Do not infer personal attributes.
        """.trimIndent()

            else -> """
            $baseRules
            Generate a clear and factual description.
        """.trimIndent()
        }
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
