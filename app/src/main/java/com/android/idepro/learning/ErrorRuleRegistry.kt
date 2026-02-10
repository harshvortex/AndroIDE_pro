package com.android.idepro.learning

object ErrorRuleRegistry {
    private val rules = listOf(
        ErrorRule(
            pattern = "Unresolved reference: (.+)".toRegex(),
            title = "Unresolved Reference",
            explanation = "This means the compiler doesn't know what {0} is. It could be a missing import, a typo, or a dependency that hasn't been added.",
            suggestion = "Check the spelling or add the required import statement."
        ),
        ErrorRule(
            pattern = "Expecting '(.+)'".toRegex(),
            title = "Syntax Error",
            explanation = "The compiler was expecting a '{0}' at this position.",
            suggestion = "Add the missing character or check your syntax."
        )
    )

    fun match(output: String): ErrorMatchResult? {
        for (rule in rules) {
            val matchResult = rule.pattern.find(output)
            if (matchResult != null) {
                val capturedValue = matchResult.groupValues.getOrNull(1) ?: ""
                return ErrorMatchResult(
                    title = rule.title,
                    explanation = rule.explanation.replace("{0}", capturedValue),
                    suggestion = rule.suggestion
                )
            }
        }
        return null
    }
}

private data class ErrorRule(
    val pattern: Regex,
    val title: String,
    val explanation: String,
    val suggestion: String
)
