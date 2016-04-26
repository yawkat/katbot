/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import com.google.common.base.Ascii
import java.util.*

/**
 * @author yawkat
 */
internal object Canonical {
    private val canonicalChars = BitSet()

    init {
        for (c in '\u0000'..'\uffff') {
            if (c.isLetterOrDigit()) {
                canonicalChars.set(c.toInt())
            }
        }
        canonicalChars.set(' '.toInt())
    }

    fun isCanonicalChar(char: Char) = canonicalChars.get(char.toInt())

    fun equalsCanonical(a: String, b: String): Boolean {
        var i = 0
        var j = 0
        while (true) {
            if (i < a.length && !isCanonicalChar(a[i])) {
                i++
                continue
            }
            if (j < b.length && !isCanonicalChar(b[j])) {
                j++
                continue
            }

            if (i >= a.length || j >= b.length) {
                return i >= a.length && j >= b.length
            }

            if (Ascii.toLowerCase(a[i]) != Ascii.toLowerCase(b[j]) ||
                    !a[i].equals(b[j], ignoreCase = true)) {
                return false // mismatch
            }
            i++
            j++
        }
    }
}