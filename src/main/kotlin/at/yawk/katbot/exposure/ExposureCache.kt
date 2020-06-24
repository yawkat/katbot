package at.yawk.katbot.exposure

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ExposureCache : ExposureApi {
    private val delegate: ExposureApi = OnlineExposureApi

    private val dateListCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(CacheLoader.from<Unit, List<LocalDate>> {
                delegate.listDates()
            })
    private val dateExportCache = CacheBuilder.newBuilder()
            .build(CacheLoader.from<LocalDate, TemporaryExposureKeyExport> {
                delegate.loadKeys(it!!)
            })
    private val timeExportCache = CacheBuilder.newBuilder()
            .build(CacheLoader.from<LocalDateTime, TemporaryExposureKeyExport> {
                delegate.loadKeys(it!!)
            })
    private val timeListCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(CacheLoader.from<LocalDate, List<LocalDateTime>> {
                delegate.listTimes(it!!)
            })

    override fun listDates() = dateListCache.get(Unit)!!
    override fun loadKeys(date: LocalDate) = dateExportCache.get(date)!!
    override fun listTimes(date: LocalDate) = timeListCache.get(date)!!
    override fun loadKeys(date: LocalDateTime) = timeExportCache.get(date)!!
}