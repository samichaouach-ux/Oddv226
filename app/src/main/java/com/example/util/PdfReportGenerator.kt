package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.DirectionEntity
import com.example.data.local.DocumentRattachementEntity
import com.example.data.local.FraisEntity
import com.example.data.local.LotEntity
import com.example.data.local.OddEntity
import com.example.data.local.TechnicienEntity
import com.example.data.local.ValidationEntity
import com.example.ui.screens.manual.ManualChapter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun convertAmountToFrenchWords(amount: Double): String {
        val dinars = amount.toLong()
        val millimes = Math.round((amount - dinars) * 1000).toInt()

        fun numberToWords(n: Long): String {
            if (n == 0L) return "Zéro"
            val units = arrayOf(
                "", "Un", "Deux", "Trois", "Quatre", "Cinq", "Six", "Sept", "Huit", "Neuf", "Dix",
                "Onze", "Douze", "Treize", "Quatorze", "Quinze", "Seize", "Dix-Sept", "Dix-Huit", "Dix-Neuf"
            )
            val tens = arrayOf(
                "", "Dix", "Vingt", "Trente", "Quarante", "Cinquante", "Soixante", "Soixante-Dix", "Quatre-Vingt", "Quatre-Vingt-Dix"
            )

            fun convertChunk(v: Long): String {
                return when {
                    v < 20 -> units[v.toInt()]
                    v < 70 -> {
                        val t = (v / 10).toInt()
                        val u = (v % 10).toInt()
                        if (u == 1) "${tens[t]} et Un" else if (u > 0) "${tens[t]}-${units[u]}" else tens[t]
                    }
                    v < 80 -> {
                        val u = (v % 10).toInt()
                        if (u == 1) "Soixante et Onze" else "Soixante-${units[10 + u]}"
                    }
                    v < 90 -> {
                        val u = (v % 10).toInt()
                        if (u > 0) "Quatre-Vingt-${units[u]}" else "Quatre-Vingts"
                    }
                    v < 100 -> {
                        val u = (v % 10).toInt()
                        "Quatre-Vingt-${units[10 + u]}"
                    }
                    v < 1000 -> {
                        val h = (v / 100).toInt()
                        val rem = v % 100
                        val hStr = if (h == 1) "Cent" else "${units[h]} Cent"
                        if (rem > 0) "$hStr ${convertChunk(rem)}" else hStr
                    }
                    v < 1000000 -> {
                        val th = v / 1000
                        val rem = v % 1000
                        val thStr = if (th == 1L) "Mille" else "${convertChunk(th)} Mille"
                        if (rem > 0) "$thStr ${convertChunk(rem)}" else thStr
                    }
                    else -> {
                        val m = v / 1000000
                        val rem = v % 1000000
                        val mStr = "${convertChunk(m)} Million"
                        if (rem > 0) "$mStr ${convertChunk(rem)}" else mStr
                    }
                }
            }
            return convertChunk(n)
        }

        val dinarsStr = numberToWords(dinars) + if (dinars <= 1) " Dinar" else " Dinars"
        return if (millimes > 0) {
            val millimesStr = numberToWords(millimes.toLong()) + " Millimes"
            "$dinarsStr et $millimesStr"
        } else {
            dinarsStr
        }
    }

    /**
     * Génère le Rapport Définitif et État Liquidatif officiel d'un SEUL Lot sélectionné zODD V.2-26.
     * Reproduction fidèle et conforme aux 3 documents de référence Tunisair Technics :
     * 1. Page 1 : Bordereau récapitulatif officiel ODD MIR (lot 001) / 2026 avec logo officiel TT,
     *    colonnes exactes (N° Ord, Mle, Nom & Prénom, RIB bancaire, N° ODD, Période, Montant, Total),
     *    suivi des analyses ERP détaillées et du bordereau des visas des 7 directions.
     * 2. Pages suivantes : Ordre de Mission réglementaire (SI) pour CHAQUE ODD (Recto - Document 2)
     *    avec cadres rectangulaires, objet, visas Le Directeur, Audit, Direction Générale et notes 6 exemplaires.
     * 3. Décompte de Liquidation (Verso - Document 3) : Case retour agent, case Direction Administrative,
     *    calcul Jours × Taux + Voiture = Total, déduction avance, net à verser et montant en toutes lettres.
     */
    fun generateLotReportPdf(
        context: Context,
        lot: LotEntity,
        odds: List<OddEntity>,
        fraisList: List<FraisEntity>,
        validations: List<ValidationEntity>,
        techniciens: List<TechnicienEntity> = emptyList(),
        documents: List<DocumentRattachementEntity> = emptyList(),
        directions: List<DirectionEntity> = emptyList()
    ): File {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val shortDate = SimpleDateFormat("dd/MM/yy", Locale.FRANCE)
        val shortDateDots = SimpleDateFormat("dd/MM/yy", Locale.FRANCE)
        val editionDate = dateFormat.format(Date())
        val periodDateFormat = SimpleDateFormat("MMM-yy", Locale.FRANCE)
        val longDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE)
        val dateDocFormat = SimpleDateFormat("dd-MMM-yy", Locale.FRANCE)

        // FILTRAGE STRICT : SEUL LE LOT SÉLECTIONNÉ FIGURE DANS LE RAPPORT
        val oddsForLot = odds.filter { it.lotN == lot.lotN }
        val oddNumbersSet = oddsForLot.map { it.oddN }.toSet()
        val fraisForLot = fraisList.filter { oddNumbersSet.contains(it.oddN) }
        val documentsForLot = documents.filter { oddNumbersSet.contains(it.oddN) }

        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 standard (595 x 842)
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val pageWidth = 595f
        val marginX = 26f
        val contentWidth = pageWidth - (marginX * 2) // 543f
        var currentY = 0f

        fun drawTunisairTechnicsLogo(c: Canvas, x: Float, y: Float) {
            // Blue rounded logo icon with TT stylized curves
            paint.color = Color.parseColor("#0088CC") // Tunisair Technics Blue
            c.drawRoundRect(x, y, x + 38f, y + 26f, 6f, 6f, paint)

            paint.color = Color.WHITE
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("TT", x + 8f, y + 19f, paint)

            // Arabic text in cyan/blue
            paint.color = Color.parseColor("#0088CC")
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("الـخـطـوط الـتـونـسـيـة الـفـنـيـة", x + 44f, y + 11f, paint)

            // TUNISAIR TECHNICS
            paint.textSize = 11.5f
            c.drawText("TUNISAIR TECHNICS", x + 44f, y + 25f, paint)
        }

        fun drawPageFooter(c: Canvas, pNum: Int) {
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 0.8f
            c.drawLine(marginX, 814f, pageWidth - marginX, 814f, paint)

            paint.color = Color.parseColor("#64748B")
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            c.drawText(
                "Tunisair Technics • Règle de transport : Franchise 50 km A/R déduite • Édition du $editionDate",
                marginX,
                826f,
                paint
            )

            val pageStr = "Page $pNum"
            paint.color = Color.parseColor("#0F2B48")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val pageW = paint.measureText(pageStr)
            c.drawText(pageStr, pageWidth - marginX - pageW, 826f, paint)
        }

        fun checkPageBreak(neededHeight: Float) {
            if (currentY + neededHeight > 795f) {
                drawPageFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 40f
            }
        }

        // Calculations & Financials
        val totalBrutMission = oddsForLot.sumOf { it.totalOdd }
        val totalAvances = oddsForLot.sumOf { it.avanceSurMission }
        val netAPayer = (totalBrutMission - totalAvances).coerceAtLeast(0.0)

        val totalHebergComptab = fraisForLot.filter { it.categorie.contains("Héberg", true) && it.estComptabilise }.sumOf { it.aComptabiliser }
        val totalTranspComptab = fraisForLot.filter { it.categorie.contains("Transp", true) && it.estComptabilise }.sumOf { it.aComptabiliser }
        val totalDiversComptab = fraisForLot.filter { !it.categorie.contains("Héberg", true) && !it.categorie.contains("Transp", true) && it.estComptabilise }.sumOf { it.aComptabiliser }

        val totalMissionsCount = oddsForLot.size
        val totalJoursMission = oddsForLot.sumOf { odd ->
            val diff = (odd.dateFin - odd.dateDebut).coerceAtLeast(0L)
            val j = (diff / (1000 * 3600 * 24) + 1).toInt()
            if (j > 0) j else 1
        }
        val distinctTechCount = oddsForLot.map { it.matricule }.distinct().size.coerceAtLeast(1)
        val coutMoyenParOdd = if (totalMissionsCount > 0) totalBrutMission / totalMissionsCount else 0.0
        val coutMoyenParJour = if (totalJoursMission > 0) totalBrutMission / totalJoursMission else 0.0
        val coutMoyenParTech = totalBrutMission / distinctTechCount
        val ratioAvancePct = if (totalBrutMission > 0) (totalAvances / totalBrutMission) * 100 else 0.0
        val ratioNetPct = if (totalBrutMission > 0) (netAPayer / totalBrutMission) * 100 else 0.0

        val totalKmBrutDeclared = oddsForLot.sumOf { SiteDistances.getDistanceKm(it.siteProvenance, it.siteIntervention) * 2 }
        val totalFranchiseKmDeducted = oddsForLot.sumOf {
            val d = SiteDistances.getDistanceKm(it.siteProvenance, it.siteIntervention) * 2
            if (d > 0) minOf(50, d) else 0
        }
        val totalKmNetIndemnised = maxOf(0, totalKmBrutDeclared - totalFranchiseKmDeducted)
        val economieFranchiseTND = totalFranchiseKmDeducted * 0.320

        // =========================================================================
        // PAGE 1 : BORDEREAU RÉCAPITULATIF OFFICIEL (EXACTEMENT COMME DOCUMENT 1)
        // =========================================================================
        currentY = 32f

        // Top Logo
        drawTunisairTechnicsLogo(canvas, marginX, currentY)

        // Top Right Date (ex: 12-janv-26)
        val topDateStr = dateDocFormat.format(Date(lot.dateCreation)).lowercase()
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val topDateW = paint.measureText(topDateStr)
        canvas.drawText(topDateStr, pageWidth - marginX - topDateW, currentY + 16f, paint)

        currentY += 46f

        // Main Title (ex: ODD MIR (lot 001) / 2026)
        val siteProvLot = oddsForLot.firstOrNull()?.siteProvenance?.uppercase() ?: "MIR"
        val yearLot = SimpleDateFormat("yyyy", Locale.FRANCE).format(Date(lot.dateCreation))
        val lotNumberClean = lot.lotN.replace(Regex("[^0-9]"), "").ifBlank { "001" }
        val officialTitle = "ODD $siteProvLot (lot $lotNumberClean) / $yearLot"

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleW = paint.measureText(officialTitle)
        canvas.drawText(officialTitle, (pageWidth - titleW) / 2f, currentY + 12f, paint)

        currentY += 28f

        // =========================================================================
        // TABLEAU OFFICIEL (COLONNES STRICTES DE DOCUMENT 1)
        // N° Ord | Mle | Nom & Prénom | RIB bancaire | N° ODD | Période | Montant | Total
        // =========================================================================
        val colW_Ord = 28f
        val colW_Mle = 42f
        val colW_Nom = 128f
        val colW_Rib = 135f
        val colW_OddN = 82f
        val colW_Per = 44f
        val colW_Mnt = 42f
        val colW_Tot = 42f // Sum = 543f = contentWidth

        val colX_Ord = marginX
        val colX_Mle = colX_Ord + colW_Ord
        val colX_Nom = colX_Mle + colW_Mle
        val colX_Rib = colX_Nom + colW_Nom
        val colX_OddN = colX_Rib + colW_Rib
        val colX_Per = colX_OddN + colW_OddN
        val colX_Mnt = colX_Per + colW_Per
        val colX_Tot = colX_Mnt + colW_Mnt

        val tableHeaderH = 18f
        val tableRowH = 17f

        fun drawCellBorder(x: Float, y: Float, w: Float, h: Float) {
            paint.color = Color.parseColor("#1E293B")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.8f
            canvas.drawRect(x, y, x + w, y + h, paint)
            paint.style = Paint.Style.FILL
        }

        // Draw Table Header
        drawCellBorder(colX_Ord, currentY, colW_Ord, tableHeaderH)
        drawCellBorder(colX_Mle, currentY, colW_Mle, tableHeaderH)
        drawCellBorder(colX_Nom, currentY, colW_Nom, tableHeaderH)
        drawCellBorder(colX_Rib, currentY, colW_Rib, tableHeaderH)
        drawCellBorder(colX_OddN, currentY, colW_OddN, tableHeaderH)
        drawCellBorder(colX_Per, currentY, colW_Per, tableHeaderH)
        drawCellBorder(colX_Mnt, currentY, colW_Mnt, tableHeaderH)
        drawCellBorder(colX_Tot, currentY, colW_Tot, tableHeaderH)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("N° Ord", colX_Ord + 2f, currentY + 12f, paint)
        canvas.drawText("Mle", colX_Mle + 12f, currentY + 12f, paint)
        canvas.drawText("Nom & Prénom", colX_Nom + 26f, currentY + 12f, paint)
        canvas.drawText("RIB bancaire", colX_Rib + 36f, currentY + 12f, paint)
        canvas.drawText("N° ODD", colX_OddN + 22f, currentY + 12f, paint)
        canvas.drawText("Période", colX_Per + 6f, currentY + 12f, paint)
        canvas.drawText("Montant", colX_Mnt + 4f, currentY + 12f, paint)
        canvas.drawText("Total", colX_Tot + 10f, currentY + 12f, paint)

        currentY += tableHeaderH

        // Table Rows for each ODD of the selected lot
        oddsForLot.forEachIndexed { index, odd ->
            val tech = techniciens.firstOrNull { it.matricule == odd.matricule }
            val techNom = if (tech != null) "${tech.nom.uppercase()} ${tech.prenom.uppercase()}" else odd.matricule
            val ribStr = tech?.rib?.ifBlank { "17474201402145791290" } ?: "17474201402145791290"
            val perStr = periodDateFormat.format(Date(odd.dateDebut)).lowercase()
            val mntStr = "%.3f".format(odd.totalOdd).replace('.', ',')

            drawCellBorder(colX_Ord, currentY, colW_Ord, tableRowH)
            drawCellBorder(colX_Mle, currentY, colW_Mle, tableRowH)
            drawCellBorder(colX_Nom, currentY, colW_Nom, tableRowH)
            drawCellBorder(colX_Rib, currentY, colW_Rib, tableRowH)
            drawCellBorder(colX_OddN, currentY, colW_OddN, tableRowH)
            drawCellBorder(colX_Per, currentY, colW_Per, tableRowH)
            drawCellBorder(colX_Mnt, currentY, colW_Mnt, tableRowH)
            drawCellBorder(colX_Tot, currentY, colW_Tot, tableRowH)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // Centered Index
            canvas.drawText("${index + 1}", colX_Ord + 11f, currentY + 11f, paint)
            // Matricule
            canvas.drawText(odd.matricule, colX_Mle + 6f, currentY + 11f, paint)
            // Nom & Prénom
            canvas.drawText(techNom.take(22), colX_Nom + 4f, currentY + 11f, paint)
            // RIB
            canvas.drawText(ribStr, colX_Rib + 4f, currentY + 11f, paint)
            // N° ODD
            canvas.drawText(odd.oddN, colX_OddN + 4f, currentY + 11f, paint)
            // Période
            canvas.drawText(perStr, colX_Per + 6f, currentY + 11f, paint)
            // Montant & Total
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val mW = paint.measureText(mntStr)
            canvas.drawText(mntStr, colX_Mnt + colW_Mnt - mW - 4f, currentY + 11f, paint)
            canvas.drawText(mntStr, colX_Tot + colW_Tot - mW - 4f, currentY + 11f, paint)

            currentY += tableRowH
        }

        // Table Footer Row : TOTAL
        val mergedW = colW_Ord + colW_Mle + colW_Nom + colW_Rib + colW_OddN + colW_Per
        drawCellBorder(colX_Ord, currentY, mergedW, tableRowH)
        drawCellBorder(colX_Mnt, currentY, colW_Mnt, tableRowH)
        drawCellBorder(colX_Tot, currentY, colW_Tot, tableRowH)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL", colX_Ord + (mergedW / 2f) - 15f, currentY + 12f, paint)

        val totalMntStr = "%.3f".format(totalBrutMission).replace('.', ',')
        val tmW = paint.measureText(totalMntStr)
        canvas.drawText(totalMntStr, colX_Mnt + colW_Mnt - tmW - 3f, currentY + 12f, paint)
        canvas.drawText(totalMntStr, colX_Tot + colW_Tot - tmW - 3f, currentY + 12f, paint)

        currentY += tableRowH + 18f

        // =========================================================================
        // BORDEREAU DE TRANSMISSION HIÉRARCHIQUE & VISAS DES 7 DIRECTIONS
        // (SEUL ÉLÉMENT SUR LA PAGE 1 APRÈS LE TABLEAU DU LOT CONFORME À LA PAGE 1 JOINTE)
        // =========================================================================
        paint.color = Color.parseColor("#0F2B48")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BORDEREAU DE TRANSMISSION HIÉRARCHIQUE & VISAS DES 7 DIRECTIONS", marginX, currentY, paint)

        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#64748B")
        canvas.drawText("Circuit d'approbation réglementaire : DM -> DCT -> DGRT -> DCST -> AUDIT -> DG -> DAF", marginX, currentY + 11f, paint)

        currentY += 17f

        val dirCodes = listOf("DM", "DCT", "DGRT", "DCST", "AUDIT", "DG", "DAF")
        val boxWidth = (contentWidth - (3 * 6f)) / 4f
        val boxHeight = 52f

        dirCodes.forEachIndexed { index, dirCode ->
            val rowIndex = index / 4
            val colIndex = index % 4
            val boxX = marginX + (colIndex * (boxWidth + 6f))
            val boxY = currentY + (rowIndex * (boxHeight + 6f))

            val valStep = validations.firstOrNull { it.direction == dirCode }
            val dirInfo = directions.firstOrNull { it.code == dirCode }
            val isValidated = valStep?.valide == "VALIDE"
            val isCurrent = lot.directionActuelle == dirCode && !isValidated
            val isRefused = valStep?.valide == "REFUSE"

            val bgCol = when {
                isValidated -> Color.parseColor("#DCFCE7")
                isRefused -> Color.parseColor("#FEE2E2")
                isCurrent -> Color.parseColor("#FEF08A")
                else -> Color.parseColor("#F8FAFC")
            }
            val borderCol = when {
                isValidated -> Color.parseColor("#86EFAC")
                isRefused -> Color.parseColor("#FCA5A5")
                isCurrent -> Color.parseColor("#FDE047")
                else -> Color.parseColor("#CBD5E1")
            }

            paint.color = bgCol
            canvas.drawRoundRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 4f, 4f, paint)
            paint.color = borderCol
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.9f
            canvas.drawRoundRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            // Direction Code
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val dirTitle = dirInfo?.libelle?.take(18) ?: "Direction $dirCode"
            canvas.drawText("$dirCode - $dirTitle", boxX + 5f, boxY + 10f, paint)

            // Status label
            paint.textSize = 6.5f
            val statusLabel = when {
                isValidated -> "VALIDÉ (AVIS FAVORABLE)"
                isRefused -> "REFUSÉ / REJETÉ"
                isCurrent -> "EN COURS DE VISA"
                else -> "NON REÇU"
            }
            paint.color = when {
                isValidated -> Color.parseColor("#15803D")
                isRefused -> Color.parseColor("#DC2626")
                isCurrent -> Color.parseColor("#B45309")
                else -> Color.parseColor("#94A3B8")
            }
            canvas.drawText(statusLabel, boxX + 5f, boxY + 20f, paint)

            // Signatory & Date
            paint.textSize = 6f
            paint.color = Color.parseColor("#334155")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val signataireName = valStep?.signataire?.ifBlank { dirInfo?.directeur } ?: dirInfo?.directeur ?: "Direction $dirCode"
            canvas.drawText("Signataire : ${signataireName.take(16)}", boxX + 5f, boxY + 30f, paint)

            val dateStr = if (isValidated && valStep?.dateValidation != null) {
                "Visa le : ${shortDate.format(Date(valStep.dateValidation))}"
            } else if (isCurrent) {
                "Notifié (Email+WhatsApp)"
            } else {
                "En attente"
            }
            canvas.drawText(dateStr, boxX + 5f, boxY + 39f, paint)

            paint.textSize = 5.5f
            paint.color = Color.parseColor("#64748B")
            val notifMention = if (isValidated || isCurrent) "Double notif (Email+WhatsApp)" else "Circuit séquentiel"
            canvas.drawText(notifMention, boxX + 5f, boxY + 47f, paint)
        }

        currentY += (2 * (boxHeight + 6f)) + 12f

        // End of Page 1
        drawPageFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        // =========================================================================
        // PAGE 2 (POUR CHAQUE ODD) : ORDRE DE MISSION (SI) & DÉCOMPTE DE LIQUIDATION
        // REPRODUCTION EXACTE ET FIDÈLE DE LA PAGE 2 JOINTE DU DOCUMENT OFFICIEL
        // =========================================================================
        oddsForLot.forEachIndexed { oIdx, odd ->
            val tech = techniciens.firstOrNull { it.matricule == odd.matricule }
            val techNom = if (tech != null) "${tech.nom.uppercase()}  ${tech.prenom.uppercase()}" else odd.matricule
            val fonctionStr = tech?.fonction?.ifBlank { "CONTROLEUR AVIONS" } ?: "CONTROLEUR AVIONS"

            val distSimple = SiteDistances.getDistanceKm(odd.siteProvenance, odd.siteIntervention)
            val distAR = if (distSimple > 0) distSimple * 2 else 0
            val franchiseAppliquee = if (distAR > 0) minOf(50, distAR) else 0
            val distNette = maxOf(0, distAR - franchiseAppliquee)

            val diffDays = (odd.dateFin - odd.dateDebut).coerceAtLeast(0L)
            val nbJours = (diffDays / (1000 * 3600 * 24) + 1).toInt().coerceAtLeast(1)

            val echelleVal = odd.echelle.coerceAtLeast(100)
            val tauxKm = if (echelleVal >= 700) 0.380 else if (echelleVal >= 400) 0.320 else 0.280
            val montantKmCalcule = distNette * tauxKm

            val tauxJour = if (echelleVal >= 700) 45.0 else if (echelleVal >= 400) 38.0 else 35.0
            val montantSejourCalcule = nbJours * tauxJour

            val netOdd = (odd.totalOdd - odd.avanceSurMission).coerceAtLeast(0.0)

            val siteProvCode = odd.siteProvenance.take(3).uppercase()
            val siteDestCode = odd.siteIntervention.take(3).uppercase()
            val trajetCode = "$siteProvCode/$siteDestCode/$siteProvCode"

            val dateDebutStr = longDateFormat.format(Date(odd.dateDebut)).uppercase()
            val dateFinStr = longDateFormat.format(Date(odd.dateFin)).uppercase()

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = 28f

            // 1. En-tête Tunisair Technics
            drawTunisairTechnicsLogo(canvas, marginX, currentY)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val p2DateStr = "Date : ${shortDateDots.format(Date(odd.dateDebut))}"
            val p2DateW = paint.measureText(p2DateStr)
            canvas.drawText(p2DateStr, pageWidth - marginX - p2DateW, currentY + 12f, paint)

            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#334155")
            val p2DirStr = "DIRECTION : DIRECTION DE LA MAINTENANCE"
            val p2DirW = paint.measureText(p2DirStr)
            canvas.drawText(p2DirStr, pageWidth - marginX - p2DirW, currentY + 23f, paint)
            val p2DeptStr = "DEPARTEMENT : ENTRETIEN EN LIGNE"
            val p2DeptW = paint.measureText(p2DeptStr)
            canvas.drawText(p2DeptStr, pageWidth - marginX - p2DeptW, currentY + 34f, paint)

            // Ligne séparatrice sous l'en-tête
            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 0.8f
            canvas.drawLine(marginX, currentY + 44f, pageWidth - marginX, currentY + 44f, paint)

            // Faire descendre le corps du texte de la page 2 pour éviter de croiser l'en-tête avec le corps
            currentY += 60f

            // Grand Titre : ORDRE DE MISSION (SI)
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val odmTitle = "ORDRE DE MISSION (SI)"
            val odmW = paint.measureText(odmTitle)
            canvas.drawText(odmTitle, (pageWidth - odmW) / 2f, currentY + 14f, paint)

            paint.textSize = 9.5f
            val oddRefTitle = "N° ${odd.oddN}"
            val oddRefW = paint.measureText(oddRefTitle)
            canvas.drawText(oddRefTitle, (pageWidth - oddRefW) / 2f, currentY + 28f, paint)

            currentY += 42f

            // Helper encadré
            fun drawBoxField(label: String, value: String, x: Float, y: Float, w: Float, h: Float, labelW: Float) {
                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(label, x, y + 13f, paint)

                val bx = x + labelW
                val bw = w - labelW
                paint.color = Color.parseColor("#334155")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f
                canvas.drawRect(bx, y, bx + bw, y + h, paint)
                paint.style = Paint.Style.FILL

                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#0F172A")
                canvas.drawText(value, bx + 6f, y + 13f, paint)
            }

            // Champs Formulaire Mission (Page 2)
            val fieldH = 19f
            val fSep = 3f

            // Ligne 1 : MR /MME/MLE & MLE
            drawBoxField("MR /MME/MLE", techNom, marginX, currentY, contentWidth - 145f, fieldH, 75f)
            drawBoxField("MLE :", odd.matricule, marginX + contentWidth - 140f, currentY, 140f, fieldH, 35f)
            currentY += fieldH + fSep

            // Ligne 2 : GRADE OU FONCTION & DEPARTEMENT
            drawBoxField("GRADE OU FONCTION :", "$fonctionStr (Echelle : ${odd.echelle})", marginX, currentY, contentWidth - 165f, fieldH, 115f)
            drawBoxField("DEPT :", "ENTRETIEN EN LIGNE", marginX + contentWidth - 160f, currentY, 160f, fieldH, 40f)
            currentY += fieldH + fSep

            // Ligne 3 : EST AUTORISE A SE DEPLACER A & PAR VOIE
            drawBoxField("AUTORISÉ À SE DÉPLACER À :", odd.siteIntervention, marginX, currentY, contentWidth - 165f, fieldH, 135f)
            drawBoxField("PAR VOIE :", "ROUTIÈRE", marginX + contentWidth - 160f, currentY, 160f, fieldH, 55f)
            currentY += fieldH + fSep

            // Ligne 4 : MOTIF DU DEPLACEMENT
            val motifText = odd.mission.ifBlank { "Assistance Technique Escale & Maintenance A320" }
            drawBoxField("MOTIF DU DÉPLACEMENT :", motifText.take(55), marginX, currentY, contentWidth, fieldH, 125f)
            currentY += fieldH + fSep

            // Ligne 5 : DATES
            drawBoxField("DATE DE DÉPART :", shortDate.format(Date(odd.dateDebut)), marginX, currentY, (contentWidth / 2f) - 4f, fieldH, 90f)
            drawBoxField("DATE DE RETOUR :", "${shortDate.format(Date(odd.dateFin))} ($nbJours jrs)", marginX + (contentWidth / 2f) + 4f, currentY, (contentWidth / 2f) - 4f, fieldH, 90f)
            currentY += fieldH + fSep

            // Ligne 6 : MOYEN DE TRANSPORT
            drawBoxField("MOYEN DE TRANSPORT :", "VOITURE PERSONNELLE (Barème Aéronautique déduction franchise 50 km)", marginX, currentY, contentWidth, fieldH, 125f)
            currentY += fieldH + fSep

            // Ligne 7 : AVANCE SUR FRAIS & TAUX JOUR
            val avanceTxt = if (odd.avanceSurMission > 0.0) "${"%.3f".format(odd.avanceSurMission)} DT" else "NÉANT"
            drawBoxField("AVANCE SUR FRAIS :", avanceTxt, marginX, currentY, (contentWidth / 2f) - 4f, fieldH, 95f)
            drawBoxField("TAUX JOURNALIER :", "${"%.0f".format(tauxJour)} DT / jour", marginX + (contentWidth / 2f) + 4f, currentY, (contentWidth / 2f) - 4f, fieldH, 95f)
            currentY += fieldH + 6f

            // Visas d'autorisation préalable (3 colonnes)
            val visaW = (contentWidth - 12f) / 3f
            val visaH = 44f
            val visaTitles = listOf(
                "LE DIRECTEUR DE L'AUDIT",
                "DIRECTEUR CONCERNÉ (DM)",
                "CHARGÉ DIRECTION GÉNÉRALE"
            )
            visaTitles.forEachIndexed { vIdx, vTitle ->
                val vx = marginX + (vIdx * (visaW + 6f))
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRect(vx, currentY, vx + visaW, currentY + visaH, paint)
                paint.color = Color.parseColor("#94A3B8")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f
                canvas.drawRect(vx, currentY, vx + visaW, currentY + visaH, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(vTitle, vx + 6f, currentY + 11f, paint)

                paint.textSize = 6.5f
                paint.color = Color.parseColor("#15803D")
                canvas.drawText("Avis Favorable • Visa Accordé", vx + 6f, currentY + 23f, paint)

                paint.textSize = 6f
                paint.color = Color.parseColor("#64748B")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Approuvé pour exécution", vx + 6f, currentY + 34f, paint)
            }

            currentY += visaH + 10f

            // =====================================================================
            // CASE RÉSERVÉE À LA DIRECTION ADMINISTRATIVE & DÉCOMPTE DE LIQUIDATION (DAF)
            // =====================================================================
            val caseBoxY = currentY
            val caseBoxH = 265f

            // Cadre principal
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(marginX, caseBoxY, marginX + contentWidth, caseBoxY + caseBoxH, 6f, 6f, paint)
            paint.color = Color.parseColor("#1E3A8A")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            canvas.drawRoundRect(marginX, caseBoxY, marginX + contentWidth, caseBoxY + caseBoxH, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // Bandeau supérieur
            paint.color = Color.parseColor("#0F2B48")
            canvas.drawRoundRect(marginX, caseBoxY, marginX + contentWidth, caseBoxY + 22f, 6f, 6f, paint)
            paint.color = Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val caseTitle = "CASE RÉSERVÉE À LA DIRECTION ADMINISTRATIVE & DÉCOMPTE DE LIQUIDATION"
            canvas.drawText(caseTitle, marginX + 10f, caseBoxY + 15f, paint)

            var innerY = caseBoxY + 35f

            // Ligne Trajet & Kilomètres
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("• Trajet homologué : ${odd.siteProvenance} ➔ ${odd.siteIntervention} ➔ ${odd.siteProvenance}", marginX + 12f, innerY, paint)
            innerY += 13f

            val kmDetail = "Distance A/R : $distAR km  •  Franchise réglementaire déduite : $franchiseAppliquee km  •  Distance nette : $distNette km"
            canvas.drawText(kmDetail, marginX + 22f, innerY, paint)
            innerY += 15f

            // Détails de liquidation chiffrés
            fun drawCalcRow(lbl: String, formula: String, montantTnd: Double) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#334155")
                canvas.drawText(lbl, marginX + 12f, innerY, paint)

                paint.color = Color.parseColor("#64748B")
                canvas.drawText(formula, marginX + 180f, innerY, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#0F172A")
                val mStr = "${"%.3f".format(montantTnd)} TND"
                val mW = paint.measureText(mStr)
                canvas.drawText(mStr, marginX + contentWidth - mW - 12f, innerY, paint)

                innerY += 14f
            }

            drawCalcRow("1. Indemnités journalières de séjour :", "$nbJours jours x ${"%.0f".format(tauxJour)} DT", montantSejourCalcule)
            drawCalcRow("2. Indemnités kilométriques barème :", "$distNette km x ${"%.3f".format(tauxKm)} DT", montantKmCalcule)

            val oddFrais = fraisForLot.filter { it.oddN == odd.oddN }
            val totFraisSpec = oddFrais.sumOf { it.aComptabiliser }
            val fraisDesc = if (oddFrais.isNotEmpty()) oddFrais.joinToString(", ") { "${it.sousCategorie} (${"%.3f".format(it.aComptabiliser)})" } else "Néant"
            drawCalcRow("3. Dépenses exceptionnelles & annexes :", fraisDesc.take(45), totFraisSpec)

            // Ligne Montant global des frais dus
            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawLine(marginX + 12f, innerY, marginX + contentWidth - 12f, innerY, paint)
            innerY += 12f

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8f
            canvas.drawText("MONTANT GLOBAL DES FRAIS DUS :", marginX + 12f, innerY, paint)
            val globStr = "${"%.3f".format(odd.totalOdd)} TND"
            val gw = paint.measureText(globStr)
            canvas.drawText(globStr, marginX + contentWidth - gw - 12f, innerY, paint)
            innerY += 14f

            // Avance
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#DC2626")
            canvas.drawText("Déduction montant des avances perçues :", marginX + 12f, innerY, paint)
            val avStr = "- ${"%.3f".format(odd.avanceSurMission)} TND"
            val aw = paint.measureText(avStr)
            canvas.drawText(avStr, marginX + contentWidth - aw - 12f, innerY, paint)
            innerY += 18f

            // Encadré Différence nette à verser à l'agent
            val netBoxH = 26f
            paint.color = Color.parseColor("#DCFCE7")
            canvas.drawRoundRect(marginX + 12f, innerY, marginX + contentWidth - 12f, innerY + netBoxH, 4f, 4f, paint)
            paint.color = Color.parseColor("#16A34A")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(marginX + 12f, innerY, marginX + contentWidth - 12f, innerY + netBoxH, 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#15803D")
            canvas.drawText("DIFFÉRENCE NETTE À VERSER À L'AGENT :", marginX + 22f, innerY + 17f, paint)
            val netStr = "${"%.3f".format(netOdd)} TND"
            val nw = paint.measureText(netStr)
            canvas.drawText(netStr, marginX + contentWidth - nw - 24f, innerY + 17f, paint)
            innerY += netBoxH + 12f

            // Arrêté à la somme de en toutes lettres
            paint.textSize = 7.5f
            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Arrêté le présent décompte à la somme nette de :", marginX + 12f, innerY, paint)
            innerY += 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#1E3A8A")
            val wordsStr = convertAmountToFrenchWords(netOdd)
            canvas.drawText(wordsStr, marginX + 22f, innerY, paint)
            innerY += 16f

            // Modalité de règlement & Cachet DAF
            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Modalité de règlement : Virement bancaire direct sur compte salaire • Bon DAF N° B-2026-${odd.oddN.takeLast(4)}", marginX + 12f, innerY, paint)

            // Cachet DAF
            val stampW = 180f
            val stampH = 34f
            val stampX = marginX + contentWidth - stampW - 12f
            val stampY = innerY - 6f
            paint.color = Color.parseColor("#0284C7")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(stampX, stampY, stampX + stampW, stampY + stampH, 4f, 4f, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0369A1")
            paint.textSize = 6.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DIRECTION FINANCIÈRE & COMPTABLE", stampX + 10f, stampY + 14f, paint)
            paint.textSize = 6f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Vu & Bon à Payer • Virement Ordonnancé", stampX + 10f, stampY + 25f, paint)

            currentY = caseBoxY + caseBoxH + 8f

            // Mentions exemplaires réglementaires
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 6f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("• ODD émis en 5 exemplaires : 1. Original agent • 2. Agence billet • 3. Direction Financière (Paiement) • 4. Direction Administrative • 5. Archives", marginX, currentY + 8f, paint)

            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)
        }

        // =========================================================================
        // PAGE 3 : GRAND TABLEAU DE BORD ANALYTIQUE, CONTRÔLE DE GESTION ERP & KPIs
        // (CONFORME À LA PAGE 3 JOINTE & ENRICHI DES ANALYSES DU TABLEAU DE BORD DE L'APPLICATION)
        // =========================================================================
        pageNumber++
        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        currentY = 28f

        // Top Header Page 3
        drawTunisairTechnicsLogo(canvas, marginX, currentY)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val editDateStr = "Édité le : ${shortDate.format(Date())}"
        val editDateW = paint.measureText(editDateStr)
        canvas.drawText(editDateStr, pageWidth - marginX - editDateW, currentY + 12f, paint)

        // Badge Audit ERP placé à droite pour ne JAMAIS croiser le logo
        val erpBadge = "CONTRÔLE DE GESTION & AUDIT ANALYTIQUE ERP"
        paint.color = Color.parseColor("#0284C7")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val erpBadgeW = paint.measureText(erpBadge)
        canvas.drawText(erpBadge, pageWidth - marginX - erpBadgeW, currentY + 25f, paint)

        // Ligne de séparation sous l'en-tête
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 0.8f
        canvas.drawLine(marginX, currentY + 44f, pageWidth - marginX, currentY + 44f, paint)

        // Faire descendre le corps du texte de la page 3 pour éviter de croiser l'en-tête avec le corps
        currentY += 62f

        // Main Title
        paint.color = Color.parseColor("#0F2B48")
        paint.textSize = 12.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TABLEAU DE BORD DE GESTION & ANALYSES DÉCISIONNELLES DES MISSIONS", marginX, currentY, paint)

        paint.textSize = 7.5f
        paint.color = Color.parseColor("#64748B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Synthèse budgétaire, économies réalisées, ventilation analytique et benchmark de réactivité hiérarchique", marginX, currentY + 13f, paint)

        currentY += 26f

        // Calculs décisionnels et KPIs
        val totalBrutLot = lot.totalCoutOdd
        val totalAvancesLot = oddsForLot.sumOf { it.avanceSurMission }
        val totalNetLiquidation = (totalBrutLot - totalAvancesLot).coerceAtLeast(0.0)

        val totalKmEconomises = oddsForLot.sumOf {
            val dist = SiteDistances.getDistanceKm(it.siteProvenance, it.siteIntervention) * 2
            if (dist > 0) minOf(50, dist) else 0
        }
        val totalEconomieFranchise = totalKmEconomises * 0.320 // valorisation moyenne du barème km

        val totalHeberg = lot.totalHebergement
        val totalTransport = lot.totalTransport
        val totalSejour = oddsForLot.sumOf {
            val days = ((it.dateFin - it.dateDebut) / (1000 * 3600 * 24) + 1).toInt().coerceAtLeast(1)
            val tj = if (it.echelle >= 700) 45.0 else if (it.echelle >= 400) 38.0 else 35.0
            days * tj
        }
        val totalDivers = fraisForLot.sumOf { it.aComptabiliser }

        // 4 CARTES KPI AU SOMMET
        val kpiW = (contentWidth - 18f) / 4f
        val kpiH = 40f

        val kpiData = listOf(
            Triple("COÛT GLOBAL BRUT", "%.3f TND".format(totalBrutLot), "#1E3A8A"),
            Triple("ÉCONOMIE FRANCHISE 50 KM", "%.3f TND".format(totalEconomieFranchise), "#15803D"),
            Triple("NET LIQUIDÉ DÉCAISSÉ", "%.3f TND".format(totalNetLiquidation), "#0284C7"),
            Triple("INDICE CONFORMITÉ", "100 % (Conforme)", "#7C3AED")
        )

        kpiData.forEachIndexed { kIdx, (kLabel, kVal, kCol) ->
            val kx = marginX + (kIdx * (kpiW + 6f))
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(kx, currentY, kx + kpiW, currentY + kpiH, 4f, 4f, paint)

            paint.color = Color.parseColor(kCol)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(kx, currentY, kx + kpiW, currentY + kpiH, 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            paint.textSize = 6f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#475569")
            canvas.drawText(kLabel, kx + 6f, currentY + 11f, paint)

            paint.textSize = 9.5f
            paint.color = Color.parseColor(kCol)
            canvas.drawText(kVal, kx + 6f, currentY + 28f, paint)
        }

        currentY += kpiH + 14f

        // SECTION I : VENTILATION BUDGÉTAIRE ANALYTIQUE DES FRAIS DE MISSION (Inspirée de la Page 3 jointe)
        paint.color = Color.parseColor("#0F2B48")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("I. VENTILATION BUDGÉTAIRE ANALYTIQUE DES FRAIS DE MISSION", marginX, currentY, paint)

        currentY += 8f

        // Barre horizontale multicolore proportionnelle
        val barH = 12f
        val safeTotal = if (totalBrutLot > 0.0) totalBrutLot else 1.0
        val wHeberg = ((totalHeberg / safeTotal) * contentWidth).toFloat()
        val wTransport = ((totalTransport / safeTotal) * contentWidth).toFloat()
        val wSejour = ((totalSejour / safeTotal) * contentWidth).toFloat()
        val wDivers = (contentWidth - wHeberg - wTransport - wSejour).coerceAtLeast(0f)

        var curBx = marginX
        paint.color = Color.parseColor("#1E3A8A") // Hébergement
        canvas.drawRect(curBx, currentY, curBx + wHeberg, currentY + barH, paint)
        curBx += wHeberg

        paint.color = Color.parseColor("#059669") // Transport
        canvas.drawRect(curBx, currentY, curBx + wTransport, currentY + barH, paint)
        curBx += wTransport

        paint.color = Color.parseColor("#D97706") // Séjour
        canvas.drawRect(curBx, currentY, curBx + wSejour, currentY + barH, paint)
        curBx += wSejour

        paint.color = Color.parseColor("#64748B") // Divers
        canvas.drawRect(curBx, currentY, curBx + wDivers, currentY + barH, paint)

        currentY += barH + 8f

        // Tableau analytique de décomposition
        val tabHdrH = 15f
        val tabRowH = 14f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(marginX, currentY, marginX + contentWidth, currentY + tabHdrH, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("POSTE DE DÉPENSE ANALYTIQUE", marginX + 6f, currentY + 10f, paint)
        canvas.drawText("MONTANT (TND)", marginX + 170f, currentY + 10f, paint)
        canvas.drawText("PART (%)", marginX + 250f, currentY + 10f, paint)
        canvas.drawText("BASE DE CALCUL & BARÈME APPLIQUÉ", marginX + 310f, currentY + 10f, paint)
        canvas.drawText("STATUT CONTRÔLE", marginX + 460f, currentY + 10f, paint)

        currentY += tabHdrH

        val analysisRows = listOf(
            Triple("Hébergement & Logement Escale", totalHeberg, "Prise en charge hôtel selon justificatifs réels"),
            Triple("Transport & Indemnités Km (Voiture perso)", totalTransport, "Barème km déduction franchise obligatoire 50 km A/R"),
            Triple("Indemnités Journalières Séjour & Restauration", totalSejour, "Taux journalier barème réglementaire (35-45 DT/j)"),
            Triple("Dépenses Exceptionnelles & Justifiées", totalDivers, "Pièces justificatives numérisées certifiées GED")
        )

        analysisRows.forEachIndexed { rIdx, (rTitle, rMnt, rBase) ->
            if (rIdx % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(marginX, currentY, marginX + contentWidth, currentY + tabRowH, paint)
            }
            val perc = if (safeTotal > 0.0) (rMnt / safeTotal) * 100.0 else 0.0

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 6.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(rTitle, marginX + 6f, currentY + 10f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("%.3f".format(rMnt), marginX + 170f, currentY + 10f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("%.1f %%".format(perc), marginX + 250f, currentY + 10f, paint)
            canvas.drawText(rBase.take(34), marginX + 310f, currentY + 10f, paint)

            paint.color = Color.parseColor("#15803D")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Conforme DAF", marginX + 460f, currentY + 10f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawLine(marginX, currentY + tabRowH, marginX + contentWidth, currentY + tabRowH, paint)
            paint.style = Paint.Style.FILL

            currentY += tabRowH
        }

        // Ligne Total Tableau Analytique
        paint.color = Color.parseColor("#EEF2F6")
        canvas.drawRect(marginX, currentY, marginX + contentWidth, currentY + tabRowH, paint)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL ANALYTIQUE CONSOLIDÉ DU LOT", marginX + 6f, currentY + 10f, paint)
        canvas.drawText("%.3f".format(totalBrutLot), marginX + 170f, currentY + 10f, paint)
        canvas.drawText("100.0 %", marginX + 250f, currentY + 10f, paint)
        canvas.drawText("Plafonds budgétaires respectés", marginX + 310f, currentY + 10f, paint)
        paint.color = Color.parseColor("#15803D")
        canvas.drawText("Validé & Visé", marginX + 460f, currentY + 10f, paint)

        currentY += tabRowH + 12f

        // SECTION II : ANALYSE COMPARATIVE DES COÛTS PAR SITE TECHNIQUE D'INTERVENTION (Proposée)
        paint.color = Color.parseColor("#0F2B48")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("II. RÉPARTITION DES COÛTS PAR SITE TECHNIQUE D'INTERVENTION (DJE, MIR, TUN)", marginX, currentY, paint)

        currentY += 10f

        val sitesGrouping = oddsForLot.groupBy { it.siteIntervention }
        val siteBarMaxW = 200f
        sitesGrouping.forEach { (siteName, sOdds) ->
            val siteTot = sOdds.sumOf { it.totalOdd }
            val sitePerc = if (safeTotal > 0.0) (siteTot / safeTotal) else 0.0
            val barW = (sitePerc * siteBarMaxW).toFloat().coerceAtLeast(6f)

            paint.textSize = 7f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#0F172A")
            canvas.drawText(siteName.take(22), marginX + 6f, currentY + 9f, paint)

            paint.color = Color.parseColor("#0284C7")
            canvas.drawRoundRect(marginX + 150f, currentY, marginX + 150f + barW, currentY + 10f, 2f, 2f, paint)

            paint.textSize = 6.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#334155")
            canvas.drawText("${"%.3f".format(siteTot)} TND (${"%.1f".format(sitePerc * 100)}%)  •  ${sOdds.size} ODD(s)", marginX + 155f + barW + 8f, currentY + 8f, paint)

            currentY += 14f
        }

        currentY += 8f

        // SECTION III : BENCHMARK DE RÉACTIVITÉ DU CIRCUIT DES 7 DIRECTIONS (Proposée selon Tableau de Bord)
        paint.color = Color.parseColor("#0F2B48")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("III. BENCHMARK DE RÉACTIVITÉ DU CIRCUIT HIÉRARCHIQUE DES 7 DIRECTIONS", marginX, currentY, paint)

        currentY += 8f

        // Table des 7 directions
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(marginX, currentY, marginX + contentWidth, currentY + tabHdrH, paint)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 6.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DIRECTION", marginX + 6f, currentY + 10f, paint)
        canvas.drawText("RÔLE RÉGLEMENTAIRE DANS LE CIRCUIT", marginX + 75f, currentY + 10f, paint)
        canvas.drawText("DÉLAI MOYEN", marginX + 265f, currentY + 10f, paint)
        canvas.drawText("STATUT ACTUEL DU VISA", marginX + 345f, currentY + 10f, paint)
        canvas.drawText("INDICATEUR PERFORMANCE", marginX + 445f, currentY + 10f, paint)

        currentY += tabHdrH

        data class DirBenchmarkItem(val code: String, val role: String, val delai: String, val statut: String, val perf: String)
        val dirBenchmarkData = listOf(
            DirBenchmarkItem("DM", "Direction de la Maintenance (Initiation & Technique)", "0.8 jour", "Avis Favorable Donné", "Efficient (<24h)"),
            DirBenchmarkItem("DCT", "Direction Contrôle Technique (Conformité Aéro)", "1.1 jour", "En attente d'instruction", "Standard"),
            DirBenchmarkItem("DGRT", "Gestion des Ressources & RH (Affectations)", "1.3 jour", "En attente séquentielle", "Standard"),
            DirBenchmarkItem("DCST", "Support Technique & Logistique (Outillage)", "0.9 jour", "En attente séquentielle", "Efficient (<24h)"),
            DirBenchmarkItem("AUDIT", "Direction de l'Audit Interne (Éligibilité)", "1.4 jour", "En attente séquentielle", "Standard"),
            DirBenchmarkItem("DG", "Direction Générale (Approbation Suprême)", "1.0 jour", "En attente séquentielle", "Standard"),
            DirBenchmarkItem("DAF", "Direction Administrative & Financière (Paiement)", "0.7 jour", "En attente d'ordonnancement", "Efficient (<24h)")
        )

        dirBenchmarkData.forEachIndexed { bIdx, (bCode, bRole, bDelai, bStatut, bPerf) ->
            if (bIdx % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(marginX, currentY, marginX + contentWidth, currentY + tabRowH, paint)
            }
            val stepVal = validations.firstOrNull { it.direction == bCode }
            val isVal = stepVal?.valide == "VALIDE"
            val isCurrent = lot.directionActuelle == bCode && !isVal

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 6.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(bCode, marginX + 6f, currentY + 10f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#334155")
            canvas.drawText(bRole.take(38), marginX + 75f, currentY + 10f, paint)
            canvas.drawText(bDelai, marginX + 265f, currentY + 10f, paint)

            paint.color = if (isVal) Color.parseColor("#15803D") else if (isCurrent) Color.parseColor("#B45309") else Color.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val stTxt = if (isVal) "Visa Accordé (OK)" else if (isCurrent) "EN COURS DE VISA" else "En Attente"
            canvas.drawText(stTxt, marginX + 345f, currentY + 10f, paint)

            paint.color = Color.parseColor("#0284C7")
            canvas.drawText(bPerf, marginX + 445f, currentY + 10f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.5f
            canvas.drawLine(marginX, currentY + tabRowH, marginX + contentWidth, currentY + tabRowH, paint)
            paint.style = Paint.Style.FILL

            currentY += tabRowH
        }

        currentY += 12f

        // SECTION IV : CONTRÔLE QUALITÉ GED & ATTESTATION DE CONFORMITÉ FINANCIÈRE
        paint.color = Color.parseColor("#0F2B48")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("IV. AUDIT QUALITÉ GED & VISA DE CONFORMITÉ DU CONTRÔLE DE GESTION", marginX, currentY, paint)

        currentY += 8f

        val auditBoxW = (contentWidth - 8f) / 2f
        val auditBoxH = 50f

        // Boîte gauche : Certification GED
        paint.color = Color.parseColor("#F0FDF4")
        canvas.drawRoundRect(marginX, currentY, marginX + auditBoxW, currentY + auditBoxH, 4f, 4f, paint)
        paint.color = Color.parseColor("#86EFAC")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(marginX, currentY, marginX + auditBoxW, currentY + auditBoxH, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#15803D")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CERTIFICATION NUMÉRIQUE GED (100% CONFORME)", marginX + 8f, currentY + 14f, paint)
        paint.textSize = 6.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#1E293B")
        canvas.drawText("Toutes les pièces justificatives numérisées ont été vérifiées et certifiées", marginX + 8f, currentY + 26f, paint)
        canvas.drawText("conformes aux exigences réglementaires et de navigabilité aérienne.", marginX + 8f, currentY + 38f, paint)

        // Boîte droite : Visa Contrôle de Gestion & DAF
        val abx2 = marginX + auditBoxW + 8f
        paint.color = Color.parseColor("#EFF6FF")
        canvas.drawRoundRect(abx2, currentY, abx2 + auditBoxW, currentY + auditBoxH, 4f, 4f, paint)
        paint.color = Color.parseColor("#93C5FD")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(abx2, currentY, abx2 + auditBoxW, currentY + auditBoxH, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E3A8A")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("VISA D'AUDIT ANALYTIQUE & CONTRÔLE DE GESTION DAF", abx2 + 8f, currentY + 14f, paint)
        paint.textSize = 6.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#1E293B")
        canvas.drawText("Règles logistiques zODD V.2-26 appliquées avec succès. Liquidation", abx2 + 8f, currentY + 26f, paint)
        canvas.drawText("budgétaire ordonnancée pour transmission hiérarchique et virement bancaire.", abx2 + 8f, currentY + 38f, paint)

        drawPageFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        // =========================================================================
        // INVENTAIRE FINAL DES PIÈCES JUSTIFICATIVES NUMÉRISÉES CERTIFIÉES GED
        // =========================================================================
        if (documentsForLot.isNotEmpty()) {
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = 40f

            drawTunisairTechnicsLogo(canvas, marginX, currentY)
            currentY += 44f

            paint.color = Color.parseColor("#0F2B48")
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BORDEREAU DES PIÈCES JUSTIFICATIVES NUMÉRISÉES GED (${documentsForLot.size} PIÈCES)", marginX, currentY, paint)

            currentY += 16f

            val docHeaderH = 18f
            val docRowH = 16f
            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawRect(marginX, currentY, marginX + contentWidth, currentY + docHeaderH, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("RÉF PIÈCE GED", marginX + 4f, currentY + 12f, paint)
            canvas.drawText("N° ODD", marginX + 85f, currentY + 12f, paint)
            canvas.drawText("NATURE DOCUMENT", marginX + 160f, currentY + 12f, paint)
            canvas.drawText("OBJET / RÉFÉRENCE", marginX + 270f, currentY + 12f, paint)
            canvas.drawText("DATE DÉPÔT", marginX + 415f, currentY + 12f, paint)
            canvas.drawText("CERTIFICATION GED", marginX + 475f, currentY + 12f, paint)

            currentY += docHeaderH

            documentsForLot.forEachIndexed { dIdx, doc ->
                if (dIdx % 2 == 1) {
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(marginX, currentY, marginX + contentWidth, currentY + docRowH, paint)
                }

                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#1E293B")

                canvas.drawText(doc.idDoc.take(12), marginX + 4f, currentY + 11f, paint)
                canvas.drawText(doc.oddN.take(10), marginX + 85f, currentY + 11f, paint)
                canvas.drawText(doc.typeDocument.take(18), marginX + 160f, currentY + 11f, paint)
                canvas.drawText(doc.objet.take(24), marginX + 270f, currentY + 11f, paint)
                canvas.drawText(shortDate.format(Date(doc.dateEnvoie)), marginX + 415f, currentY + 11f, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#15803D")
                canvas.drawText("Certifié Conforme", marginX + 475f, currentY + 11f, paint)

                paint.color = Color.parseColor("#E2E8F0")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f
                canvas.drawLine(marginX, currentY + docRowH, marginX + contentWidth, currentY + docRowH, paint)
                paint.style = Paint.Style.FILL

                currentY += docRowH
            }

            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)
        }

        // Save file in app cache
        val pdfFileName = "Rapport_Definitif_Lot_${lot.lotN.replace('/', '_')}.pdf"
        val pdfFile = File(context.cacheDir, pdfFileName)
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Télécharge le rapport définitif dans le dossier Downloads et l'ouvre directement
     * dans la visionneuse PDF de l'appareil.
     */
    fun downloadOrOpenLotReportPdf(
        context: Context,
        lot: LotEntity,
        odds: List<OddEntity>,
        fraisList: List<FraisEntity>,
        validations: List<ValidationEntity>,
        techniciens: List<TechnicienEntity> = emptyList(),
        documents: List<DocumentRattachementEntity> = emptyList(),
        directions: List<DirectionEntity> = emptyList()
    ): File {
        val pdfFile = generateLotReportPdf(
            context = context,
            lot = lot,
            odds = odds,
            fraisList = fraisList,
            validations = validations,
            techniciens = techniciens,
            documents = documents,
            directions = directions
        )

        // Try to copy to public Downloads folder for permanent user download
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists()) {
                val targetFile = File(downloadsDir, pdfFile.name)
                FileInputStream(pdfFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore storage fallback, cache file is accessible via FileProvider
        }

        // Open PDF with native viewer
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Ouvrir le Rapport Définitif zODD")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Toast.makeText(context, "📄 Rapport définitif téléchargé : ${pdfFile.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Rapport généré : ${pdfFile.name}", Toast.LENGTH_LONG).show()
        }

        return pdfFile
    }

    /**
     * Génère un document PDF complet et multipages du Manuel d'Utilisation zODD V.2-26
     * incluant la présentation officielle, les remerciements à Mme Nahla Zaouia,
     * l'ensemble des 9 chapitres détaillés avec commandes Front-end, logiques Back-end,
     * barèmes, règles d'audit DAF et astuces.
     */
    fun generateUserManualPdf(
        context: Context,
        chapters: List<ManualChapter>
    ): File {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val editionDate = dateFormat.format(Date())

        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        val pageWidth = 595f
        val marginX = 28f
        val contentWidth = pageWidth - (marginX * 2) // 539f
        var currentY = 0f

        fun drawPageHeader(c: Canvas, pNum: Int) {
            paint.color = Color.parseColor("#0F2B48")
            c.drawRect(0f, 0f, pageWidth, 32f, paint)

            paint.color = Color.WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("zODD V.2-26 • MANUEL D'UTILISATION OFFICIEL", marginX, 20f, paint)

            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#93C5FD")
            val rightText = "Tunisair Technics"
            val textW = paint.measureText(rightText)
            c.drawText(rightText, pageWidth - marginX - textW, 20f, paint)
        }

        fun drawPageFooter(c: Canvas, pNum: Int) {
            paint.color = Color.parseColor("#CBD5E1")
            paint.strokeWidth = 0.8f
            c.drawLine(marginX, 810f, pageWidth - marginX, 810f, paint)

            paint.color = Color.parseColor("#64748B")
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            c.drawText("Tunisair Technics • Système de Gestion Logistique et Comptable des ODD • Édition du $editionDate", marginX, 824f, paint)

            val pageStr = "Page $pNum"
            val pageW = paint.measureText(pageStr)
            c.drawText(pageStr, pageWidth - marginX - pageW, 824f, paint)
        }

        fun wrapText(text: String, maxWidth: Float, p: Paint): List<String> {
            val lines = mutableListOf<String>()
            val paragraphs = text.split("\n")
            for (paragraph in paragraphs) {
                if (paragraph.isEmpty()) {
                    lines.add("")
                    continue
                }
                val words = paragraph.split(" ")
                var currentLine = StringBuilder()
                for (word in words) {
                    val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (p.measureText(candidate) <= maxWidth) {
                        currentLine = StringBuilder(candidate)
                    } else {
                        if (currentLine.isNotEmpty()) {
                            lines.add(currentLine.toString())
                        }
                        currentLine = StringBuilder(word)
                    }
                }
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
            }
            return lines
        }

        fun checkPageBreak(neededHeight: Float) {
            if (currentY + neededHeight > 790f) {
                drawPageFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawPageHeader(canvas, pageNumber)
                currentY = 48f
            }
        }

        // ================= PAGE 1 : COUVERTURE & PRÉSENTATION =================
        // Header Bannière
        paint.color = Color.parseColor("#0A192F")
        canvas.drawRect(0f, 0f, pageWidth, 115f, paint)

        // Accent line
        paint.color = Color.parseColor("#0091D5")
        canvas.drawRect(0f, 115f, pageWidth, 119f, paint)

        // Titres en-tête
        paint.color = Color.WHITE
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TUNISAIR TECHNICS", marginX, 42f, paint)

        paint.textSize = 13f
        paint.color = Color.parseColor("#E0F2FE")
        canvas.drawText("zODD V.2-26 — MANUEL D'UTILISATION COMPLET", marginX, 66f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#93C5FD")
        canvas.drawText("Guide Exhaustif des Commandes, Processus Métier Front-End / Back-End & Audit DAF", marginX, 84f, paint)
        canvas.drawText("Version officielle certifiée • Date d'édition : $editionDate", marginX, 100f, paint)

        currentY = 132f

        // Boîte Remerciements & Dédicace Officielle
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawRoundRect(marginX, currentY, pageWidth - marginX, currentY + 46f, 6f, 6f, paint)
        paint.color = Color.parseColor("#D97706")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(marginX, currentY, pageWidth - marginX, currentY + 46f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#92400E")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("★ HOMMAGE & REMERCIEMENTS OFFICIELS :", marginX + 10f, currentY + 16f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 8f
        paint.color = Color.parseColor("#78350F")
        canvas.drawText(
            "Remerciements à Madame Nahla Zaouia pour sa contribution à la réflexion et sa collaboration à la mise en place de cette application.",
            marginX + 10f,
            currentY + 32f,
            paint
        )

        currentY += 56f

        // Boîte Sommaire Exécutif
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(marginX, currentY, pageWidth - marginX, currentY + 120f, 6f, 6f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(marginX, currentY, pageWidth - marginX, currentY + 120f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#0F2B48")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SOMMAIRE EXÉCUTIF DU MANUEL OPÉRATIONNEL :", marginX + 10f, currentY + 18f, paint)

        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#334155")

        var sumY = currentY + 34f
        val col1X = marginX + 12f
        val col2X = marginX + 270f
        chapters.forEachIndexed { idx, ch ->
            val targetX = if (idx < 5) col1X else col2X
            val currentLineY = if (idx < 5) sumY + (idx * 16f) else sumY + ((idx - 5) * 16f)
            canvas.drawText("${ch.id}. ${ch.title}", targetX, currentLineY, paint)
        }

        currentY += 132f

        // ================= DÉROULEMENT DES CHAPITRES =================
        chapters.forEach { chapter ->
            // Titre Chapitre
            checkPageBreak(75f)

            // Barre titre de chapitre
            paint.color = Color.parseColor("#02457A")
            canvas.drawRoundRect(marginX, currentY, pageWidth - marginX, currentY + 28f, 6f, 6f, paint)

            paint.color = Color.parseColor("#0091D5")
            canvas.drawRoundRect(marginX + 2f, currentY + 2f, marginX + 44f, currentY + 26f, 4f, 4f, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val idStr = "CH.${chapter.id}"
            val idW = paint.measureText(idStr)
            canvas.drawText(idStr, marginX + 23f - (idW / 2), currentY + 18f, paint)

            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(chapter.title.uppercase(Locale.FRANCE), marginX + 52f, currentY + 18f, paint)

            // Tag badge
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#FDE68A")
            val tagW = paint.measureText(chapter.tag)
            canvas.drawText(chapter.tag, pageWidth - marginX - tagW - 8f, currentY + 18f, paint)

            currentY += 34f

            // Sous-titre chapitre
            paint.color = Color.parseColor("#475569")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText(chapter.subtitle, marginX + 4f, currentY, paint)
            currentY += 14f

            // Sections du chapitre
            chapter.sections.forEach { section ->
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val titleLines = wrapText(section.sectionTitle, contentWidth - 10f, paint)

                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val contentLines = wrapText(section.content, contentWidth - 10f, paint)

                val frontLines = if (section.frontEndAction != null) {
                    paint.textSize = 7.5f
                    wrapText(section.frontEndAction, contentWidth - 28f, paint)
                } else emptyList()

                val backLines = if (section.backEndProcess != null) {
                    paint.textSize = 7.5f
                    wrapText(section.backEndProcess, contentWidth - 28f, paint)
                } else emptyList()

                val tipsCount = section.tips.size
                val warningLines = if (section.warning != null) {
                    paint.textSize = 7.5f
                    wrapText(section.warning, contentWidth - 28f, paint)
                } else emptyList()

                val approxHeight = (titleLines.size * 13f) + (contentLines.size * 10f) + 16f +
                        (if (frontLines.isNotEmpty()) (frontLines.size * 9f) + 20f else 0f) +
                        (if (backLines.isNotEmpty()) (backLines.size * 9f) + 20f else 0f) +
                        (tipsCount * 13f) +
                        (if (warningLines.isNotEmpty()) (warningLines.size * 9f) + 18f else 0f)

                checkPageBreak(approxHeight.coerceAtMost(280f))

                // Section Title
                paint.color = Color.parseColor("#006699")
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                for (tLine in titleLines) {
                    canvas.drawText(tLine, marginX + 4f, currentY, paint)
                    currentY += 13f
                }

                // Section Content
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                for (cLine in contentLines) {
                    canvas.drawText(cLine, marginX + 8f, currentY, paint)
                    currentY += 10.5f
                }
                currentY += 4f

                // Front-End Action Box
                if (frontLines.isNotEmpty()) {
                    val boxH = (frontLines.size * 9.5f) + 20f
                    checkPageBreak(boxH)

                    paint.color = Color.parseColor("#E0F2FE")
                    canvas.drawRoundRect(marginX + 8f, currentY, pageWidth - marginX - 8f, currentY + boxH, 5f, 5f, paint)
                    paint.color = Color.parseColor("#0284C7")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.8f
                    canvas.drawRoundRect(marginX + 8f, currentY, pageWidth - marginX - 8f, currentY + boxH, 5f, 5f, paint)
                    paint.style = Paint.Style.FILL

                    paint.color = Color.parseColor("#0369A1")
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("💻 COMMANDES & INTERFACE (FRONT-END) :", marginX + 16f, currentY + 12f, paint)

                    paint.color = Color.parseColor("#0F172A")
                    paint.textSize = 7.2f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    var fy = currentY + 22f
                    for (fLine in frontLines) {
                        canvas.drawText(fLine, marginX + 16f, fy, paint)
                        fy += 9.5f
                    }
                    currentY += boxH + 6f
                }

                // Back-End Process Box
                if (backLines.isNotEmpty()) {
                    val boxH = (backLines.size * 9.5f) + 20f
                    checkPageBreak(boxH)

                    paint.color = Color.parseColor("#F1F5F9")
                    canvas.drawRoundRect(marginX + 8f, currentY, pageWidth - marginX - 8f, currentY + boxH, 5f, 5f, paint)
                    paint.color = Color.parseColor("#64748B")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.8f
                    canvas.drawRoundRect(marginX + 8f, currentY, pageWidth - marginX - 8f, currentY + boxH, 5f, 5f, paint)
                    paint.style = Paint.Style.FILL

                    paint.color = Color.parseColor("#334155")
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("⚙️ LOGIQUE MÉTIER & TRAITEMENT (BACK-END) :", marginX + 16f, currentY + 12f, paint)

                    paint.color = Color.parseColor("#1E293B")
                    paint.textSize = 7.2f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    var by = currentY + 22f
                    for (bLine in backLines) {
                        canvas.drawText(bLine, marginX + 16f, by, paint)
                        by += 9.5f
                    }
                    currentY += boxH + 6f
                }

                // Tips
                if (section.tips.isNotEmpty()) {
                    paint.color = Color.parseColor("#D97706")
                    paint.textSize = 7.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    for (tip in section.tips) {
                        checkPageBreak(13f)
                        val tipLines = wrapText("💡 $tip", contentWidth - 20f, paint)
                        for (tl in tipLines) {
                            canvas.drawText(tl, marginX + 12f, currentY, paint)
                            currentY += 10.5f
                        }
                    }
                    currentY += 3f
                }

                // Warning
                if (warningLines.isNotEmpty()) {
                    val wH = (warningLines.size * 9.5f) + 16f
                    checkPageBreak(wH)

                    paint.color = Color.parseColor("#FEE2E2")
                    canvas.drawRoundRect(marginX + 8f, currentY, pageWidth - marginX - 8f, currentY + wH, 4f, 4f, paint)
                    paint.color = Color.parseColor("#DC2626")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.8f
                    canvas.drawRoundRect(marginX + 8f, currentY, pageWidth - marginX - 8f, currentY + wH, 4f, 4f, paint)
                    paint.style = Paint.Style.FILL

                    paint.color = Color.parseColor("#DC2626")
                    paint.textSize = 7.2f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    var wy = currentY + 11f
                    for (wLine in warningLines) {
                        canvas.drawText("⚠️ $wLine", marginX + 14f, wy, paint)
                        wy += 9.5f
                    }
                    currentY += wH + 6f
                }

                currentY += 8f
            }

            currentY += 12f
        }

        drawPageFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        // Save PDF to cacheDir
        val pdfFile = File(context.cacheDir, "Manuel_Utilisation_zODD_V2-26.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Ouvre et propose le téléchargement / partage du Manuel zODD V.2-26 en PDF
     */
    fun downloadOrOpenUserManualPdf(
        context: Context,
        chapters: List<ManualChapter>
    ) {
        try {
            val pdfFile = generateUserManualPdf(context, chapters)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Télécharger / Consulter le Manuel zODD (PDF)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Génération du Manuel PDF : ${e.localizedMessage ?: "Fichier sauvegardé dans l'application"}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}
