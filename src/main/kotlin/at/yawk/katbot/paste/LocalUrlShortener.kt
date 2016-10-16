/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.paste

import at.yawk.katbot.UrlShortener
import java.net.URI
import javax.inject.Inject

/**
 * @author yawkat
 */
class LocalUrlShortener @Inject constructor(private val pasteProvider: PasteProvider) : UrlShortener {
    override fun shorten(uri: URI): URI = pasteProvider.createPaste(Paste(Paste.Type.URL, uri.toASCIIString()))
}