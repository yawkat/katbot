/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.markov

import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcConnectionPool
import org.skife.jdbi.v2.DBI
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

/**
 * @author yawkat
 */

internal class ChainDaoTest {
    @DataProvider
    fun dbi(): Array<Array<DBI>> {
        val source = JdbcConnectionPool.create("jdbc:h2:mem:test", "", "")
        val flyway = Flyway()
        flyway.dataSource = source
        flyway.migrate()
        return arrayOf(arrayOf(DBI(source)))
    }

    @Test(dataProvider = "dbi")
    fun `duplicate start`(dbi: DBI) {
        val dao = dbi.open(ChainDao::class.java)
        dao.insertStart("abc", "def")
        dao.insertStart("abc", "def")
        Assert.assertEquals(dao.selectStart("abc"), "def")
    }

    @Test(dataProvider = "dbi")
    fun `duplicate suffix`(dbi: DBI) {
        val dao = dbi.open(ChainDao::class.java)
        dao.insertSuffix("123", "abc", "def")
        dao.insertSuffix("123", "abc", "def")
        Assert.assertEquals(dao.selectSuffix("123", "abc"), "def")
    }

    @Test(dataProvider = "dbi")
    fun `null as default start`(dbi: DBI) {
        val dao = dbi.open(ChainDao::class.java)
        dao.insertStart("123", "abc")
        Assert.assertNull(dao.selectStart("456"))
    }

    @Test(dataProvider = "dbi")
    fun `null as default suffix`(dbi: DBI) {
        val dao = dbi.open(ChainDao::class.java)
        dao.insertStart("123", "abc")
        dao.insertSuffix("123", "abc", "def")
        Assert.assertNull(dao.selectSuffix("123", "yyy"))
    }
}