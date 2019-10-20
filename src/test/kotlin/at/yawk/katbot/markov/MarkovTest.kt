/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.markov

import at.yawk.katbot.randomChoice
import org.testng.Assert
import org.testng.annotations.Test
import java.util.*

/**
 * @author yawkat
 */
private fun <T> Random.choice(list: List<T>): T? {
    val size = list.size
    return if (size == 0) null else list[nextInt(size)]
}

class MarkovTest {
    @Test
    fun `learn and generate locally`() {
        val dao = MockDao(Random(1))
        learnFromMessage(dao, "TEST", "I am not a number! I am a free man!".split(' '))
        Assert.assertEquals(generateMessage(dao, "TEST"), "I am not a number! I am not a number! I am not a number! I am a free man!")
    }

    internal class MockDao(val rng: Random) : ChainDao {
        private val chains = HashMap<String, Chain>()
        private fun chain(name: String) = chains.getOrPut(name) { Chain() }

        override fun insertSuffix(chain: String, prefix: String, suffix: String) {
            chain(chain).suffixes.add(prefix to suffix)
        }

        override fun insertStart(chain: String, prefix: String) {
            chain(chain).starts.add(prefix)
        }

        override fun selectSuffix(chain: String, prefix: String) =
                rng.choice(chain(chain).suffixes.filter { it.first == prefix }.map { it.second })

        override fun selectStart(chain: String) =
                rng.choice(chain(chain).starts)

        private class Chain {
            // list for reproducibility
            val starts = ArrayList<String>()
            val suffixes = ArrayList<Pair<String, String>>()
        }
    }
}