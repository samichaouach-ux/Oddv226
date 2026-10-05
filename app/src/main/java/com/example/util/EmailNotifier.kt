package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.LotEntity
import java.io.File

data class EmailNotificationData(
    val recipientEmail: String,
    val recipientName: String,
    val recipientPhone: String = "",
    val directionCode: String,
    val subject: String,
    val body: String,
    val whatsappMessage: String = "",
    val attachmentFile: File?
)

object EmailNotifier {

    fun prepareEmailForDirection(
        lot: LotEntity,
        directionCode: String,
        directionName: String,
        directionEmail: String,
        directorName: String,
        directorPhone: String = "",
        pdfFile: File?,
        oddsCount: Int = 0,
        netTotal: Double = lot.totalCoutOdd
    ): EmailNotificationData {
        val subject = "[zODD V.2-26] Demande de Validation Officielle — Lot N° ${lot.lotN}"
        val body = """
            À l'attention de Monsieur/Madame le Directeur de la Direction $directionCode ($directionName),
            $directorName

            Objet : Demande de Visa & Approbation Hiérarchique — Dossier de Mission N° ${lot.lotN}
            Intitulé du Lot : ${lot.libelle}

            Madame, Monsieur le Directeur,

            Le dossier relatif au Lot N° ${lot.lotN} a été finalisé et soumis pour validation réglementaire dans le cadre du circuit des 7 directions (DM -> DCT -> DGRT -> DCST -> AUDIT -> DG -> DAF).

            SYNTHÈSE DU DOSSIER :
            • Référence du Lot : ${lot.lotN}
            • Nombre d'Ordres de Déplacement (ODD) : ${if (oddsCount > 0) oddsCount else "Consolidé"}
            • Montant total brut engagé : ${"%.3f".format(lot.totalCoutOdd)} TND
            • Dont Hébergement : ${"%.3f".format(lot.totalHebergement)} TND
            • Dont Transport (franchise 50 km A/R déduite) : ${"%.3f".format(lot.totalTransport)} TND
            • Net définitif à ordonnancer : ${"%.3f".format(netTotal)} TND
            • Étape active requise : Direction $directionCode ($directionName)
            • Statut actuel : ${lot.statutValidation}

            PIÈCE JOINTE :
            Le Rapport Définitif officiel de liquidation (${pdfFile?.name ?: "Rapport_Definitif_Lot.pdf"}), regroupant l'état nominatif des techniciens, le bordereau de transmission hiérarchique, les ordres de mission réglementaires (SI), le grand tableau de bord d'analyses ERP et l'inventaire des justificatifs certifiés GED, est joint au présent message pour examen et visa.

            NOTIFICATION & RAPPEL WHATSAPP :
            Conformément à la procédure de gouvernance zODD V.2-26, une notification d'urgence parallèle par message WhatsApp a été préparée et transmise au Directeur ($directorName - $directorPhone) afin d'attirer expressément son attention sur l'envoi de cet e-mail et sur la nécessité de procéder à sa validation sous peu.

            Nous vous invitons à apposer votre visa dans l'application zODD V.2-26 afin de permettre la poursuite du traitement vers les directions suivantes jusqu'à l'ordonnancement DAF.

            Cordialement,
            Plateforme zODD V.2-26 • Tunisair Technics
            Système de Gestion Logistique & Comptable Aéronautique
        """.trimIndent()

        val whatsappMessage = """
🚨 *URGENT • TUNISAIR TECHNICS / zODD V.2-26*
━━━━━━━━━━━━━━━━━━━━━━━━
Bonjour Monsieur/Madame le Directeur,
*Direction $directionCode ($directionName)*
Directeur : $directorName

📧 *E-mail officiel transmis avec Rapport Définitif PDF :*
Un e-mail officiel comportant le Rapport Définitif de liquidation pour le *Lot N° ${lot.lotN}* ("${lot.libelle}") vient de vous être envoyé à votre adresse officielle ($directionEmail).

📊 *Synthèse du dossier :*
• Référence : *Lot ${lot.lotN}*
• Volume : *$oddsCount ODD(s)* rattaché(s)
• Net à liquider : *${"%.3f".format(netTotal)} TND*
• Étape hiérarchique active : *Direction $directionCode*

⚠️ *ACTION RAPIDE REQUISE :*
Nous attirons votre attention sur l'envoi de cet e-mail officiel et sur *la nécessité impérative de valider le dossier sous peu* sur l'application zODD afin de permettre la transmission sans délai aux directions suivantes jusqu'au paiement final DAF.

Merci pour votre réactivité.
_Direction zODD V.2-26 • Tunisair Technics_
        """.trimIndent()

        return EmailNotificationData(
            recipientEmail = directionEmail,
            recipientName = directorName,
            recipientPhone = directorPhone,
            directionCode = directionCode,
            subject = subject,
            body = body,
            whatsappMessage = whatsappMessage,
            attachmentFile = pdfFile
        )
    }

    /**
     * Ouvre l'application e-mail avec le PDF joint, et effectue une copie de secours dans le presse-papier.
     */
    fun sendEmail(context: Context, emailData: EmailNotificationData) {
        copyEmailToClipboard(context, emailData, showToast = false)
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailData.recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, emailData.subject)
                putExtra(Intent.EXTRA_TEXT, emailData.body)

                if (emailData.attachmentFile != null && emailData.attachmentFile.exists()) {
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        emailData.attachmentFile
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Transmettre le rapport officiel par email...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Toast.makeText(context, "✉️ Client e-mail ouvert pour Direction ${emailData.directionCode}", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            // Fallback via mailto
            try {
                val mailtoUri = Uri.parse("mailto:${emailData.recipientEmail}?subject=${Uri.encode(emailData.subject)}&body=${Uri.encode(emailData.body)}")
                val fallbackIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                Toast.makeText(context, "✉️ Application de messagerie lancée.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "✉️ E-mail copié dans le presse-papier ! (Destinataire: ${emailData.recipientEmail})", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Envoie l'alerte WhatsApp pour attirer l'attention de la direction sur l'envoi de l'email
     * et la nécessité de le valider sous peu.
     */
    fun sendWhatsApp(context: Context, emailData: EmailNotificationData) {
        copyWhatsAppToClipboard(context, emailData, showToast = false)
        val cleanPhone = emailData.recipientPhone.replace(Regex("[^0-9]"), "")
        val encodedMsg = Uri.encode(emailData.whatsappMessage)
        val waUrl = if (cleanPhone.isNotBlank() && cleanPhone.length >= 8) {
            "https://wa.me/$cleanPhone?text=$encodedMsg"
        } else {
            "https://wa.me/?text=$encodedMsg"
        }

        // 1. Essai WhatsApp standard
        val waIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(waUrl)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(waIntent)
            Toast.makeText(context, "💬 WhatsApp ouvert pour alerter Direction ${emailData.directionCode}", Toast.LENGTH_SHORT).show()
            return
        } catch (_: Exception) {}

        // 2. Essai WhatsApp Business
        val waBizIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(waUrl)
            setPackage("com.whatsapp.w4b")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(waBizIntent)
            Toast.makeText(context, "💬 WhatsApp Business ouvert pour alerter Direction ${emailData.directionCode}", Toast.LENGTH_SHORT).show()
            return
        } catch (_: Exception) {}

        // 3. Essai Navigateur Web (wa.me redirige vers l'application WhatsApp ou Web)
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            Toast.makeText(context, "💬 Alerte WhatsApp lancée via le navigateur.", Toast.LENGTH_SHORT).show()
            return
        } catch (_: Exception) {}

        // 4. Fallback presse-papier
        Toast.makeText(context, "💬 Message d'alerte WhatsApp copié dans le presse-papier !", Toast.LENGTH_LONG).show()
    }

    fun copyEmailToClipboard(context: Context, emailData: EmailNotificationData, showToast: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = "À : ${emailData.recipientEmail}\nObjet : ${emailData.subject}\n\n${emailData.body}"
        val clip = ClipData.newPlainText("E-mail zODD", text)
        clipboard.setPrimaryClip(clip)
        if (showToast) {
            Toast.makeText(context, "📋 E-mail copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyWhatsAppToClipboard(context: Context, emailData: EmailNotificationData, showToast: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Alerte WhatsApp zODD", emailData.whatsappMessage)
        clipboard.setPrimaryClip(clip)
        if (showToast) {
            Toast.makeText(context, "📋 Alerte WhatsApp copiée dans le presse-papier !", Toast.LENGTH_SHORT).show()
        }
    }
}
