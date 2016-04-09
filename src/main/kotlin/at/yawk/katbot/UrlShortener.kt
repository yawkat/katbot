/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.paste.client.PasteClient
import at.yawk.paste.model.URLPasteData
import com.google.inject.ImplementedBy
import java.net.URI
import javax.inject.Inject

/**
 * @author yawkat
 */
@ImplementedBy(PasteUrlShortenerImpl::class)
interface UrlShortener {
    fun shorten(uri: URI): URI
}

internal class PasteUrlShortenerImpl @Inject constructor(
        val config: Config,
        val pasteClient: PasteClient
) : UrlShortener {

    override fun shorten(uri: URI): URI {
        val pasteData = URLPasteData()
        pasteData.url = uri.toURL()
        return URI(pasteClient.save(pasteData))
    }
}