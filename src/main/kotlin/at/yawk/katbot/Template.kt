package at.yawk.katbot

import java.util.regex.Pattern

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

    fun setWithParameter(name: String, value: (String) -> String): Template {
        val quoted = Pattern.quote(name)
        val regex = "\\$\\{$quoted:([^}]*)\\}".toPattern()

        var r = this
        while (true) {
            val matcher = regex.matcher(r.data)
            if (!matcher.find()) return r
            // todo: prevent infinite loop
            val arg = matcher.group(1)
            val sub = value.invoke(arg)
            r = Template(r.data.substring(0, matcher.start()) + sub + r.data.substring(matcher.end()))
        }
    }

    fun finish(): String = data
}