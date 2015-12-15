package at.yawk.katbot

/**
 * @author yawkat
 */
data class Template(private val data: String) {
    private fun toTemplateExpression(name: String): String {
        // ${name}
        return "${'$'}{$name}"
    }

    fun set(name: String, value: String): Template {
        return Template(data.replace(toTemplateExpression(name), value))
    }

    fun set(name: String, value: () -> String): Template {
        if (data.contains(toTemplateExpression(name)) ) {
            return set(name, value.invoke())
        } else {
            return this
        }
    }

    fun finish(): String = data
}