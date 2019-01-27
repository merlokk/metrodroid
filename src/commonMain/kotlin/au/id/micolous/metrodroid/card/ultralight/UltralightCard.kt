/*
 * UltralightCard.kt
 *
 * Copyright 2016-2018 Michael Farrell <micolous+git@gmail.com>
 * Copyright 2019 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.card.ultralight

import au.id.micolous.metrodroid.card.*
import au.id.micolous.metrodroid.multi.Localizer
import au.id.micolous.metrodroid.multi.R
import au.id.micolous.metrodroid.serializers.XMLListIdx
import au.id.micolous.metrodroid.transit.TransitData
import au.id.micolous.metrodroid.transit.TransitIdentity
import au.id.micolous.metrodroid.ui.ListItem
import au.id.micolous.metrodroid.ui.ListItemRecursive
import au.id.micolous.metrodroid.util.ImmutableByteArray
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

expect object UltralightTransitRegistry {
    val allFactories: List<UltralightCardTransitFactory>
}

/**
 * Utility class for reading MIFARE Ultralight / Ultralight C
 */
@Serializable
data class UltralightCard constructor(
        /**
         * Get the model of Ultralight card this is.
         *
         * Note: this is NULL on dumps from old versions of Metrodroid.
         *
         * @return Model of Ultralight card this is.
         */
        @Optional
        val cardModel: String = "",
        @XMLListIdx("index")
        val pages: List<UltralightPage>,
        @Optional
        override val isPartialRead: Boolean = false) : CardProtocol() {
    @Transient
    override val rawData: List<ListItem>
        get() = pages.mapIndexed { idx, sector ->
            val sectorIndexString = idx.toString()

            if (sector.isUnauthorized) {
                ListItemRecursive.collapsedValue(Localizer.localizeString(
                        R.string.unauthorized_page_title_format, sectorIndexString),
                        null, null)
            } else {
                ListItemRecursive.collapsedValue(Localizer.localizeString(
                        R.string.page_title_format, sectorIndexString), null, sector.data.toHexDump())
            }
        }

    private fun findTransitFactory(): UltralightCardTransitFactory? {
        for (factory in UltralightTransitRegistry.allFactories) {
            try {
                if (factory.check(this))
                    return factory
            } catch (e: IndexOutOfBoundsException) {
                /* Not the right factory. Just continue  */
            } catch (e: UnauthorizedException) {
            }

        }
        return null
    }

    override fun parseTransitIdentity(): TransitIdentity? = findTransitFactory()?.parseTransitIdentity(this)

    override fun parseTransitData(): TransitData? = findTransitFactory()?.parseTransitData(this)

    fun getPage(index: Int): UltralightPage = pages[index]

    fun readPages(startPage: Int, pageCount: Int): ImmutableByteArray {
        var data = ImmutableByteArray.empty()
        for (index in startPage until startPage + pageCount) {
            data += getPage(index).data
        }
        return data
    }

    companion object {
        const val PAGE_SIZE = 4
    }
}