package at.yawk.katbot.exposure

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ExposureCache {
    private val dateListCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(CacheLoader.from<Unit, List<LocalDate>> {
                ExposureLoaderApi.listDates()
            })
    private val exportCache = CacheBuilder.newBuilder()
            .build(CacheLoader.from<LocalDate, TemporaryExposureKeyExport> {
                ExposureLoaderApi.loadKeys(it!!)
            })

    fun listDates() = dateListCache.get(Unit)!!
    fun loadKeys(date: LocalDate) = exportCache.get(date)!!
}