package at.yawk.katbot.nina

import org.testng.Assert
import org.testng.annotations.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.File
import java.time.OffsetDateTime
import javax.imageio.ImageIO
import javax.swing.border.StrokeBorder

class NinaTest {
    private val testAnnouncement = Announcement(
            identifier = "DE-BW-S-SE018-20200806-18-000",
            sender = "DE-BW-S-W090",
            sent = OffsetDateTime.parse("2020-08-11T10:58:25Z"),
            status = "Actual",
            msgType = "Update",
            scope = "Public",
            code = listOf("1.0", "medien_regional", "nina"),
            references = "DE-BW-S-W090-20200806-000 DE-BW-S-SE018-20200623-18-000 DE-BW-S-W090-20200623-000 DE-BW-S-SE018-20200609-18-000 DE-BW-S-W090-20200609-000 DE-BW-S-SE018-20200527-18-000 DE-BW-S-W090-20200527-000 DE-BW-S-SE018-20200516-18-000 DE-BW-S-W090-20200516-000 DE-BW-S-SE018-20200509-18-000 DE-BW-S-W090-20200509-000 DE-BW-S-SE018-20200503-18-000 DE-BW-S-W090-20200503-000 DE-BW-S-SE018-20200423-18-000 DE-BW-S-W090-20200423-000 DE-BW-S-SE018-20200418-18-000 DE-BW-S-W090-20200418-000 DE-BW-S-SE018-20200410-18-000 DE-BW-S-W090-20200410-000 DE-BW-S-SE018-20200329,DE-BW-S-W090-20200806-000 DE-BW-S-SE018-20200623-18-000 DE-BW-S-W090-20200623-000 DE-BW-S-SE018-20200609-18-000 DE-BW-S-W090-20200609-000 DE-BW-S-SE018-20200527-18-000 DE-BW-S-W090-20200527-000 DE-BW-S-SE018-20200516-18-000 DE-BW-S-W090-20200516-000 DE-BW-S-SE018-20200509-18-000 DE-BW-S-W090-20200509-000 DE-BW-S-SE018-20200503-18-000 DE-BW-S-W090-20200503-000 DE-BW-S-SE018-20200423-18-000 DE-BW-S-W090-20200423-000 DE-BW-S-SE018-20200418-18-000 DE-BW-S-W090-20200418-000 DE-BW-S-SE018-20200410-18-000 DE-BW-S-W090-20200410-000 DE-BW-S-SE018-20200329-18-000,18--T00:00:00+00:00",
            info = listOf(Announcement.Info(language = "DE",
                    category = listOf("Health"),
                    event = "Gefahreninformation",
                    urgency = "Immediate",
                    severity = "Minor",
                    certainty = "Observed",
                    eventCode = listOf(Announcement.Value(valueName = "profile:DE-BBK-EVENTCODE:01.00R",
                            value = "BBK-EVC-040")),
                    headline = "Landesregierung ändert die Corona-Verordnung",
                    description = "Die Landesregierung hat die neugefasste Corona-Verordnung vom 1. Juli 2020 erstmals geändert. Die Geltungsdauer der Verordnung wird verlängert, die Regelung zur Maskenpflicht an Schulen wird ergänzt. Zudem erfolgen einzelne Korrekturen zur Klarstellung und Beseitigung bestehender Regelungslücken.<br/>Die neue Corona-Verordnung wurde am 5. August 2020 veröffentlicht und gilt ab dem 6. August 2020.<br/>+++ Die wesentlichen Änderungen:<br/>+++ Geltungsdauer:<br/>Die Geltungsdauer der Corona-Verordnung wird bis zum 30. September 2020 verlängert.<br/>+++ Mund-Nasen-Bedeckung:<br/>Ab 14. September 2020 muss an weiterführenden Schulen, beruflichen Schulen und Sonderpädagogischen Bildungs- und Beratungszentren außerhalb der Unterrichtsräume eine Mund-Nasen-Bedeckung getragen werden. Dies gilt insbesondere auf Fluren, Pausenhöfen sowie in Treppenhäusern und Toiletten. Die Maskenpflicht an Schulen gilt nicht innerhalb der Unterrichtsräume, in zugehörigen Sportanlagen bzw. Sportstätten sowie bei der Nahrungsaufnahme.<br/>Auf allen Großmärkten, Wochenmärkten, Spezial- und Jahrmärkten, die in geschlossenen Räumen stattfinden, muss eine Mund-Nasen-Bedeckung getragen werden.<br/>+++ Datenverarbeitung:<br/>Die Alternativmöglichkeit zur Angabe einer E-Mail-Adresse bei der Datenerhebung wird gestrichen, da die Datenverarbeitung mittels E-Mail – insbesondere etwa die Kontaktaufnahme durch Gesundheitsbehörden – häufig nicht den Anforderungen der Ende-zu-Ende-Verschlüsselung entspricht.<br/>Bei Großmärkten, Wochenmärkten, Spezial- und Jahrmärkten entfällt die Pflicht zur Datenerhebung.<br/>In Betriebskantinen muss nur bei externen Gästen eine Datenverarbeitung erfolgen.<br/>+++ Wie bisher gilt:<br/>In der Öffentlichkeit ist, wo immer möglich, zu anderen Personen ein Abstand von mindestens 1,5 m einzuhalten.<br/>Die Maskenpflicht bleibt bestehen. Es gelten die zuvor beschriebenen Änderungen.<br/>Die Kontaktbeschränkungen gelten weiterhin. Ansammlungen im öffentlichen und im privaten Raum sind mit bis zu 20 Personen erlaubt. Veranstaltungen sind seit dem 1. August 2020 mit bis zu 500 Personen möglich. Größere Veranstaltungen mit mehr Gästen sind bis einschließlich 31. Oktober 2020 weiterhin verboten. Bei privaten Veranstaltungen mit bis zu 100 Personen muss kein Hygienekonzept erstellt werden.<br/>Clubs und Diskotheken dürfen weiterhin nicht öffnen.<br/>Prostitutionsstätten, Bordelle und ähnliche Einrichtungen sowie jede sonstige Ausübung des Prostitutionsgewerbes bleiben ebenfalls untersagt.<br/>+++ Die neue Rechtsverordnung finden Sie hier:<br/>https://www.baden-wuerttemberg.de/de/service/aktuelle-infos-zu-corona/aktuelle-corona-verordnung-des-landes-baden-wuerttemberg",
                    instruction = "Waschen Sie sich regelmäßig und gründlich die Hände.<br/>Schützen Sie sich und Ihre Mitmenschen, indem Sie sich an die empfohlenen Hygienemaßnahmen halten.<br/>Diese finden Sie unter: https://www.infektionsschutz.de",
                    web = """Aktuelle Informationen zum Corona-Virus finden Sie auf den Internetseiten der Landesregierung: 
https://www.baden-wuerttemberg.de/de/service/aktuelle-infos-zu-corona

Die Regelungen der aktuell gültigen Corona-Verordnung im Detail finden Sie hier:
https://www.baden-wuerttemberg.de/corona-verordnung

Fragen zu Corona und den Verordnungen beantwortet auch COREY, der Chatbot der Landesregierung:
https://im.baden-wuerttemberg.de/chatbot-corey""",
                    contact = null,
                    parameter = listOf(Announcement.Value(valueName = "instructionText",
                            value = "Schützen Sie sich und Ihre Mitmenschen, indem Sie sich an die empfohlenen Hygienemaßnahmen halten.\nDiese finden Sie unter: https://www.infektionsschutz.de"),
                            Announcement.Value(valueName = "instructionCode",
                                    value = "BBK-ISC-132 shortCode:BBK-ISC-011"),
                            Announcement.Value(valueName = "sender_langname",
                                    value = "Ministerium für Inneres, Digitalisierung und Migration Baden-Württemberg"),
                            Announcement.Value(valueName = "sender_signature",
                                    value = "Ministerium für Inneres, Digitalisierung und Migration Baden-Württemberg\nWilly-Brandt-Straße 41\n70173 Stuttgart")),
                    area = listOf(Announcement.Area(areaDesc = "Bundesland: Land Baden-Württemberg",
                            polygon = emptyList(),
                            geocode = emptyList())
                    ))))

    @Test
    fun testAnnouncementText() {
        Assert.assertEquals(
                testAnnouncement.toText(),
                """
Landesregierung ändert die Corona-Verordnung
============================================

Die Landesregierung hat die neugefasste Corona-Verordnung vom 1. Juli 2020 erstmals geändert. Die Geltungsdauer der Verordnung wird verlängert, die Regelung zur Maskenpflicht an Schulen wird ergänzt. Zudem erfolgen einzelne Korrekturen zur Klarstellung und Beseitigung bestehender Regelungslücken.
Die neue Corona-Verordnung wurde am 5. August 2020 veröffentlicht und gilt ab dem 6. August 2020.
+++ Die wesentlichen Änderungen:
+++ Geltungsdauer:
Die Geltungsdauer der Corona-Verordnung wird bis zum 30. September 2020 verlängert.
+++ Mund-Nasen-Bedeckung:
Ab 14. September 2020 muss an weiterführenden Schulen, beruflichen Schulen und Sonderpädagogischen Bildungs- und Beratungszentren außerhalb der Unterrichtsräume eine Mund-Nasen-Bedeckung getragen werden. Dies gilt insbesondere auf Fluren, Pausenhöfen sowie in Treppenhäusern und Toiletten. Die Maskenpflicht an Schulen gilt nicht innerhalb der Unterrichtsräume, in zugehörigen Sportanlagen bzw. Sportstätten sowie bei der Nahrungsaufnahme.
Auf allen Großmärkten, Wochenmärkten, Spezial- und Jahrmärkten, die in geschlossenen Räumen stattfinden, muss eine Mund-Nasen-Bedeckung getragen werden.
+++ Datenverarbeitung:
Die Alternativmöglichkeit zur Angabe einer E-Mail-Adresse bei der Datenerhebung wird gestrichen, da die Datenverarbeitung mittels E-Mail – insbesondere etwa die Kontaktaufnahme durch Gesundheitsbehörden – häufig nicht den Anforderungen der Ende-zu-Ende-Verschlüsselung entspricht.
Bei Großmärkten, Wochenmärkten, Spezial- und Jahrmärkten entfällt die Pflicht zur Datenerhebung.
In Betriebskantinen muss nur bei externen Gästen eine Datenverarbeitung erfolgen.
+++ Wie bisher gilt:
In der Öffentlichkeit ist, wo immer möglich, zu anderen Personen ein Abstand von mindestens 1,5 m einzuhalten.
Die Maskenpflicht bleibt bestehen. Es gelten die zuvor beschriebenen Änderungen.
Die Kontaktbeschränkungen gelten weiterhin. Ansammlungen im öffentlichen und im privaten Raum sind mit bis zu 20 Personen erlaubt. Veranstaltungen sind seit dem 1. August 2020 mit bis zu 500 Personen möglich. Größere Veranstaltungen mit mehr Gästen sind bis einschließlich 31. Oktober 2020 weiterhin verboten. Bei privaten Veranstaltungen mit bis zu 100 Personen muss kein Hygienekonzept erstellt werden.
Clubs und Diskotheken dürfen weiterhin nicht öffnen.
Prostitutionsstätten, Bordelle und ähnliche Einrichtungen sowie jede sonstige Ausübung des Prostitutionsgewerbes bleiben ebenfalls untersagt.
+++ Die neue Rechtsverordnung finden Sie hier:
https://www.baden-wuerttemberg.de/de/service/aktuelle-infos-zu-corona/aktuelle-corona-verordnung-des-landes-baden-wuerttemberg

Aktuelle Informationen zum Corona-Virus finden Sie auf den Internetseiten der Landesregierung: 
https://www.baden-wuerttemberg.de/de/service/aktuelle-infos-zu-corona

Die Regelungen der aktuell gültigen Corona-Verordnung im Detail finden Sie hier:
https://www.baden-wuerttemberg.de/corona-verordnung

Fragen zu Corona und den Verordnungen beantwortet auch COREY, der Chatbot der Landesregierung:
https://im.baden-wuerttemberg.de/chatbot-corey

DE-BW-S-SE018-20200806-18-000
                """.trim() + "\n"
        )
        println(testAnnouncement.toText())
    }

    private val testPolygon = PolygonDeserializer.parse("9.5236,53.9173 9.5233,53.9164 9.5227,53.9156 9.5218,53.9148 9.5207,53.9142 9.5195,53.9137 9.5181,53.9134 9.5166,53.9132 9.5151,53.9132 9.5136,53.9134 9.5122,53.9137 9.511,53.9142 9.5099,53.9149 9.5091,53.9156 9.5085,53.9164 9.5082,53.9173 9.5083,53.9182 9.5086,53.9191 9.5091,53.9199 9.51,53.9206 9.5111,53.9213 9.5123,53.9217 9.5137,53.9221 9.5152,53.9223 9.5167,53.9223 9.5182,53.9221 9.5196,53.9217 9.5208,53.9212 9.5219,53.9206 9.5227,53.9198 9.5233,53.919 9.5236,53.9181 9.5236,53.9173")

    @Test
    fun testPolygon() {
        Assert.assertTrue(testPolygon.contains(
                WarningChannel("", longitude = 9.522263, latitude = 53.919578).point))
        Assert.assertFalse(testPolygon.contains(
                WarningChannel("", longitude = 11.027850, latitude = 49.573713).point))
    }

    @Test(enabled = false)
    fun drawPoly() {
        val bufferedImage = BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB)
        val gfx = bufferedImage.createGraphics()
        val p = Path2D.Double(testPolygon)
        p.transform(AffineTransform.getTranslateInstance(-9.508, -53.913))
        p.transform(AffineTransform.getScaleInstance(10000.0, 10000.0))
        gfx.stroke = BasicStroke()
        gfx.color = Color.black
        gfx.draw(p)
        gfx.dispose()
        ImageIO.write(bufferedImage, "png", File("test.png"))
    }
}