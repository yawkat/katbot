/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.markov

import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.SqlUpdate

/**
 * @author yawkat
 */
internal interface ChainDao {
    @SqlQuery("select suffix from markovSuffixes where chainName = :chain and prefix = :prefix order by rand() limit 1")
    fun selectSuffix(@Bind("chain") chain: String,
                     @Bind("prefix") prefix: String): String?

    @SqlQuery("select prefix from markovStarts where chainName = :chain order by rand() limit 1")
    fun selectStart(@Bind("chain") chain: String): String?

    @SqlUpdate("merge into markovSuffixes (chainName, prefix, suffix) values (:chain, :prefix, :suffix)")
    fun insertSuffix(@Bind("chain") chain: String, @Bind("prefix") prefix: String, @Bind("suffix") suffix: String)

    @SqlUpdate("merge into markovStarts (chainName, prefix) values (:chain, :prefix)")
    fun insertStart(@Bind("chain") chain: String, @Bind("prefix") prefix: String)
}