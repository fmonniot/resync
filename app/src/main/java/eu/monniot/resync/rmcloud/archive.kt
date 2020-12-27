package eu.monniot.resync.rmcloud

import nl.siegmann.epublib.util.IOUtil
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun make_archive(documentId: String, extension: String, epubBytes: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    val zipStream = ZipOutputStream(out)

    // EPub file
    zipStream.putNextEntry(ZipEntry("$documentId.$extension"))
    IOUtil.copy(ByteArrayInputStream(epubBytes), zipStream)

    // .pagedata file
    zipStream.putNextEntry(ZipEntry("$documentId.pagedata"))
    IOUtil.copy(ByteArrayInputStream(ByteArray(0)), zipStream)

    // .content file
    zipStream.putNextEntry(ZipEntry("$documentId.content"))
    val content = """
        {
          "dummyDocument": false,
          "extraMetadata": {
            "LastBrushColor": "",
            "LastBrushThicknessScale": "",
            "LastColor": "",
            "LastEraserThicknessScale": "",
            "LastEraserTool": "",
            "LastPen": "Finelinerv2",
            "LastPenColor": "",
            "LastPenThicknessScale": "",
            "LastPencil": "",
            "LastPencilColor": "",
            "LastPencilThicknessScale": "",
            "LastTool": "Finelinerv2",
            "ThicknessScale": "",
            "LastFinelinerv2Size": "1"
          },
          "fileType": "$extension",
          "fontName": "EB Garamond",
          "lastOpenedPage": 0,
          "lineHeight": 100,
          "margins": 50,
          "orientation": "portrait",
          "pageCount": 0,
          "pages": null,
          "textAlignment": "justify",
          "textScale": 1.2,
          "transform": {
            "m11": 1,
            "m12": 0,
            "m13": 0,
            "m21": 0,
            "m22": 1,
            "m23": 0,
            "m31": 0,
            "m32": 0,
            "m33": 1
          }
        }
    """.trimIndent()
    IOUtil.copy(ByteArrayInputStream(content.toByteArray()), zipStream)

    // Finish the archive
    zipStream.close()

    return out.toByteArray()
}


/*
pub(crate) fn make(
    id: &DocumentId,
    ext: &String,
    content: &Vec<u8>,
) -> Result<Vec<u8>, ArchiveError> {
    let mut buffer: Vec<u8> = Vec::new();
    let w = std::io::Cursor::new(&mut buffer);
    let mut zip = ZipWriter::new(w);

    let options = FileOptions::default()
        .compression_method(zip::CompressionMethod::Stored)
        .unix_permissions(0o644);

    // actual file
    zip.start_file(format!("{}.{}", id.0, ext), options.clone())?;
    zip.write_all(&content)?;

    // .pagedata file
    zip.start_file(format!("{}.pagedata", id.0), options.clone())?;
    zip.write_all(&Vec::new())?;

    // .content file
    zip.start_file(format!("{}.content", id.0), options.clone())?;

    let content = json!(
        {
            "dummyDocument": false,
            "extraMetadata": {
                "LastBrushColor": "",
                "LastBrushThicknessScale": "",
                "LastColor": "",
                "LastEraserThicknessScale": "",
                "LastEraserTool": "",
                "LastPen": "Finelinerv2",
                "LastPenColor": "",
                "LastPenThicknessScale": "",
                "LastPencil": "",
                "LastPencilColor": "",
                "LastPencilThicknessScale": "",
                "LastTool": "Finelinerv2",
                "ThicknessScale": "",
                "LastFinelinerv2Size": "1"
            },
            "fileType": ext,
            "fontName": "EB Garamond",
            "lastOpenedPage": 0,
            "lineHeight": 100,
            "margins": 50,
            "orientation": "portrait",
            "pageCount": 0,
            "pages": null,
            "textAlignment": "justify",
            "textScale": 1.2,
            "transform": {
                "m11": 1,
                "m12": 0,
                "m13": 0,
                "m21": 0,
                "m22": 1,
                "m23": 0,
                "m31": 0,
                "m32": 0,
                "m33": 1
            }
        }
    );
    let content = serde_json::to_vec(&content)?;
    zip.write_all(&content)?;

    // Finalize the archive and drop the borrow on the byte buffer
    zip.finish()?;
    drop(zip);

    Ok(buffer)
}
 */