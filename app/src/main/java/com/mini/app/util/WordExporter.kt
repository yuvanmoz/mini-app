package com.mini.app.util

import android.content.Context
import android.os.Environment
import com.mini.app.domain.model.Post
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WordExporter {

    fun exportToWord(context: Context, posts: List<Post>): Result<File> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Mini_Saved_Posts_$timestamp.docx"

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            var targetFile = File(downloadsDir, fileName)

            if (!downloadsDir.canWrite()) {
                val altDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: downloadsDir
                targetFile = File(altDir, fileName)
            }

            val imageEntries = mutableListOf<ImageEntry>()
            var imageCounter = 1

            // Process image files for embedding in Word document
            posts.forEachIndexed { postIdx, post ->
                post.mediaItems.forEachIndexed { mediaIdx, item ->
                    val path = item.localPath
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && file.length() > 0) {
                            val rId = "rId_img_${postIdx}_$mediaIdx"
                            val ext = file.extension.ifEmpty { "jpg" }
                            val zipPath = "word/media/image_${imageCounter}.$ext"
                            imageEntries.add(
                                ImageEntry(
                                    rId = rId,
                                    localFile = file,
                                    zipPath = zipPath,
                                    fileName = "image_${imageCounter}.$ext",
                                    extension = ext
                                )
                            )
                            imageCounter++
                        }
                    }
                }
            }

            val zipOut = ZipOutputStream(FileOutputStream(targetFile))

            // 1. [Content_Types].xml
            zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
            zipOut.write(buildContentTypesXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 2. _rels/.rels
            zipOut.putNextEntry(ZipEntry("_rels/.rels"))
            zipOut.write(buildPackageRelsXml().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 3. word/_rels/document.xml.rels
            zipOut.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zipOut.write(buildDocumentRelsXml(imageEntries).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 4. word/document.xml
            zipOut.putNextEntry(ZipEntry("word/document.xml"))
            zipOut.write(buildDocumentXml(posts, imageEntries).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 5. Write images into word/media/
            imageEntries.forEach { img ->
                zipOut.putNextEntry(ZipEntry(img.zipPath))
                img.localFile.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }

            zipOut.finish()
            zipOut.close()

            Result.success(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private data class ImageEntry(
        val rId: String,
        val localFile: File,
        val zipPath: String,
        val fileName: String,
        val extension: String
    )

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Default Extension="png" ContentType="image/png"/>
    <Default Extension="jpg" ContentType="image/jpeg"/>
    <Default Extension="jpeg" ContentType="image/jpeg"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
    }

    private fun buildPackageRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
    }

    private fun buildDocumentRelsXml(images: List<ImageEntry>): String {
        val relsSb = StringBuilder()
        images.forEach { img ->
            relsSb.append("""    <Relationship Id="${img.rId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/${img.fileName}"/>""")
            relsSb.append("\n")
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$relsSb</Relationships>""".trimIndent()
    }

    private fun buildDocumentXml(posts: List<Post>, images: List<ImageEntry>): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
<w:body>
""")

        // Title Header
        sb.append("""
    <w:p>
        <w:pPr><w:jc w:val="center"/><w:spacing w:after="300"/></w:pPr>
        <w:r>
            <w:rPr><w:b/><w:sz w:val="48"/><w:color w:val="1DA1F2"/></w:rPr>
            <w:t>Mini - Saved Posts Library</w:t>
        </w:r>
    </w:p>
    <w:p>
        <w:pPr><w:jc w:val="center"/><w:spacing w:after="400"/></w:pPr>
        <w:r>
            <w:rPr><w:italic/><w:sz w:val="20"/><w:color w:val="657786"/></w:rPr>
            <w:t>Exported on ${escapeXml(SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()))} (${posts.size} posts)</w:t>
        </w:r>
    </w:p>
""")

        val imageMap = images.associateBy { it.rId }

        posts.forEachIndexed { postIdx, post ->
            val dateStr = if (post.originalDateMs > 0) sdf.format(Date(post.originalDateMs)) else sdf.format(Date(post.savedAtMs))

            // Author Header
            val favMarker = if (post.isFavorite) " ⭐" else ""
            sb.append("""
    <w:p>
        <w:pPr><w:spacing w:before="300" w:after="60"/></w:pPr>
        <w:r>
            <w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="14171A"/></w:rPr>
            <w:t>${escapeXml(post.author.displayName)}$favMarker </w:t>
        </w:r>
        <w:r>
            <w:rPr><w:color w:val="657786"/><w:sz w:val="24"/></w:rPr>
            <w:t>@${escapeXml(post.author.handle)} · ${escapeXml(dateStr)}</w:t>
        </w:r>
    </w:p>
""")

            // Personal Note (if attached)
            if (!post.note.isNullOrBlank()) {
                sb.append("""
    <w:p>
        <w:pPr>
            <w:pBdr><w:left w:val="single" w:sz="18" w:space="10" w:color="6366F1"/></w:pBdr>
            <w:spacing w:before="80" w:after="100"/>
            <w:ind w:left="200"/>
        </w:pPr>
        <w:r>
            <w:rPr><w:b/><w:sz w:val="20"/><w:color w:val="6366F1"/></w:rPr>
            <w:t>PERSONAL NOTE: </w:t>
        </w:r>
        <w:r>
            <w:rPr><w:italic/><w:sz w:val="22"/><w:color w:val="292F33"/></w:rPr>
            <w:t xml:space="preserve">${escapeXml(post.note)}</w:t>
        </w:r>
    </w:p>
""")
            }

            // Post Text
            val textLines = post.text.split("\n")
            textLines.forEach { line ->
                sb.append("""
    <w:p>
        <w:pPr><w:spacing w:after="100"/></w:pPr>
        <w:r>
            <w:rPr><w:sz w:val="24"/><w:color w:val="292F33"/></w:rPr>
            <w:t xml:space="preserve">${escapeXml(line)}</w:t>
        </w:r>
    </w:p>
""")
            }

            // Tags
            if (post.tags.isNotEmpty()) {
                val tagsStr = post.tags.joinToString(" ") { "#${it.name}" }
                sb.append("""
    <w:p>
        <w:pPr><w:spacing w:after="120"/></w:pPr>
        <w:r>
            <w:rPr><w:color w:val="1DA1F2"/><w:sz w:val="22"/></w:rPr>
            <w:t>${escapeXml(tagsStr)}</w:t>
        </w:r>
    </w:p>
""")
            }

            // Images
            post.mediaItems.forEachIndexed { mediaIdx, _ ->
                val rId = "rId_img_${postIdx}_$mediaIdx"
                val imgEntry = imageMap[rId]
                if (imgEntry != null) {
                    sb.append(buildDrawingXml(imgEntry.rId))
                }
            }

            // Divider line between posts
            sb.append("""
    <w:p>
        <w:pPr>
            <w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="E1E8ED"/></w:pBdr>
            <w:spacing w:before="200" w:after="300"/>
        </w:pPr>
    </w:p>
""")
        }

        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    private fun buildDrawingXml(rId: String): String {
        val widthEmu = 4500000L  // ~4.7 inches width in EMUs (English Metric Units)
        val heightEmu = 3000000L // ~3.1 inches height in EMUs

        return """
    <w:p>
        <w:pPr><w:jc w:val="center"/><w:spacing w:before="120" w:after="180"/></w:pPr>
        <w:r>
            <w:drawing>
                <wp:inline distT="0" distB="0" distL="0" distR="0">
                    <wp:extent cx="$widthEmu" cy="$heightEmu"/>
                    <wp:docPr id="1" name="Picture"/>
                    <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                        <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                            <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                                <pic:nvPicPr>
                                    <pic:cNvPr id="0" name="Picture"/>
                                    <pic:cNvPicPr/>
                                </pic:nvPicPr>
                                <pic:blipFill>
                                    <a:blip r:embed="$rId"/>
                                    <a:stretch><a:fillRect/></a:stretch>
                                </pic:blipFill>
                                <pic:spPr>
                                    <a:xfrm>
                                        <a:off x="0" y="0"/>
                                        <a:ext cx="$widthEmu" cy="$heightEmu"/>
                                    </a:xfrm>
                                    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                                </pic:spPr>
                            </pic:pic>
                        </a:graphicData>
                    </a:graphic>
                </wp:inline>
            </w:drawing>
        </w:r>
    </w:p>
"""
    }
}
