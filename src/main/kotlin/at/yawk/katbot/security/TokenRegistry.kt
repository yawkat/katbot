/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import javax.annotation.concurrent.GuardedBy
import javax.inject.Singleton

/**
 * @author yawkat
 */
private const val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

@Singleton
class TokenRegistry {
    private val rng = SecureRandom.getInstance("SHA1PRNG")

    @GuardedBy("this")
    private val tokens: BiMap<IrcUserInfo, String> = HashBiMap.create()

    @Synchronized
    fun createToken(user: IrcUserInfo): String {
        val token = generateToken()
        tokens.put(user, token)
        return token
    }

    @Synchronized
    fun getUserForToken(token: String): IrcUserInfo? =
            tokens.inverse()[token]

    @VisibleForTesting
    internal fun generateToken(): String {
        val tokenChars = rng.ints(0, ALPHABET.length).limit(16).map { ALPHABET[it].toInt() }.toArray()
        val token = String(tokenChars, 0, tokenChars.size)
        return token
    }
}