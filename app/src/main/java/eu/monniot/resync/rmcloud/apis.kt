package eu.monniot.resync.rmcloud

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.util.*

interface MyRemarkableApi {

    @POST("/token/json/2/user/new")
    suspend fun renewToken(@Header("Authorization") authorization: String): String

    @POST("/token/json/2/device/new")
    suspend fun register(body: RegistrationPayload): String

    companion object {

        fun build(client: OkHttpClient): MyRemarkableApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://my.remarkable.com")
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(MyRemarkableApi::class.java)
        }
    }
}

data class RegistrationPayload(val code: String, val deviceDesc: String, val deviceID: String) {
    companion object {
        fun fromCode(code: String) =
            RegistrationPayload(code, "mobile-android", UUID.randomUUID().toString())
    }
}

interface DocumentStorageApi {
    @GET("/document-storage/json/2/docs")
    suspend fun list(@Header("Authorization") authorization: String): List<Document>

    @PUT("/document-storage/json/2/upload/request")
    suspend fun uploadRequest(
        @Header("Authorization") authorization: String,
        @Body body: List<UploadRequestPayload>
    ): List<UploadRequestResponse>

    @PUT("/document-storage/json/2/upload/update-status")
    suspend fun updateMetadata(
        @Header("Authorization") authorization: String,
        @Body body: List<UpdateMetadataRequest>
    )

    companion object {
        fun build(client: OkHttpClient): DocumentStorageApi {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://document-storage-production-dot-remarkable-production.appspot.com")
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build()

            return retrofit.create(DocumentStorageApi::class.java)
        }
    }
}

data class Document(
    val ID: String,
    val Version: Int,
    val Message: String,
    val Success: Boolean,
    val BlobURLGet: String,
    val BlobURLGetExpires: String,
    val ModifiedClient: String,
    val Type: String, // DocumentType or CollectionType
    val VissibleName: String,
    val CurrentPage: Int,
    val Bookmarked: Boolean,
    val Parent: String
)

data class UploadRequestPayload(val ID: String, val Type: String, val Version: Int)
data class UploadRequestResponse(
    val ID: String,
    val Version: Int,
    val Message: String,
    val Success: Boolean,
    val BlobURLPut: String,
    val BlobURLPutExpires: String
)

data class UpdateMetadataRequest(
    val ID: String,
    val Parent: String,
    val VissibleName: String,
    val Type: String,
    val Version: Int,
    val ModifiedClient: String
)


class RmCloud {


    /*

const DOCUMENT_UPLOAD_URL: &str = "/document-storage/json/2/upload/request";
const DOCUMENT_UPDATE_URL: &str = "/document-storage/json/2/upload/update-status";

    /// Upload a pdf/epub document to the remarkable cloud.
    ///
    /// It is required to know the document id of the folder where the file
    /// will be uploaded under.
    pub async fn upload_epub(
        &self,
        content: &Vec<u8>,
        file_name: &str,
        folder: DocumentId,
    ) -> Result<(), Error> {
        // 1. Check the file name and extension is supported
        let (name, ext) = validate_file_name_for_upload(&file_name)?;

        let doc_id = DocumentId::new();

        // 2. Create the remarkable archive (file format at https://remarkablewiki.com/tech/filesystem#metadata_file_format)
        let archive = archive::make(&doc_id, &ext, content)?;

        // 3. Send an upload request
        let uploads = self.upload_request(&doc_id, EntryType::Document).await?;
        let upload = &uploads[0]; // safe because we would error above if not one available, I think

        // 4. Send the archive to the url obtained in the previous step
        self.upload_archive(&upload.blob_url_put, archive).await?;

        // 5. Update the metadata to make the file visible
        self.update_metadata(doc_id, folder, name, EntryType::Document)
            .await?;

        Ok(())
    }

    async fn upload_request(
        &self,
        doc_id: &DocumentId,
        entry_type: EntryType,
    ) -> Result<Vec<UploadRequestResponse>, Error> {
        debug!("Creating upload request for document {:?}", doc_id);

        let token = self.user_token.as_ref().ok_or(Error::NoTokenAvailable)?;
        let payload = json!([{
            "ID": doc_id.0,
            "Type": entry_type.as_str(),
            "Version": 1 // We only support new documents for now
        }]);

        let response = self
            .http
            .put(DOCUMENT_UPLOAD_URL)
            .header("User-Agent", "rmsync")
            .bearer_auth(&token.0)
            .json(&payload)
            .send()
            .await?;

        let status = response.status();

        if status.is_success() {
            let body = response.json().await?;

            Ok(body)
        } else {
            let body = response.text().await?;

            Err(Error::ApiCallFailure {
                status,
                body,
                api: ApiKind::UploadRequest,
            })
        }
    }

    async fn upload_archive(&self, url: &str, archive: Vec<u8>) -> Result<(), Error> {
        debug!("Uploading archive to the reMarkable cloud");

        // No need for authentication here as its already part of the url
        let response = self
            .http
            .put(url)
            .header("User-Agent", "rmsync")
            .body(archive)
            .send()
            .await?;

        let status = response.status();

        if status.is_success() {
            Ok(())
        } else {
            let body = response.text().await?;
            Err(Error::ApiCallFailure {
                status,
                body,
                api: ApiKind::UploadArchive,
            })
        }
    }

    async fn update_metadata(
        &self,
        doc_id: DocumentId,
        parent: DocumentId,
        name: String,
        entry_type: EntryType,
    ) -> Result<(), Error> {
        debug!("Creating metadata for document id {}", doc_id.0);

        let token = self.user_token.as_ref().ok_or(Error::NoTokenAvailable)?;
        let payload = json!([{
            "ID":             doc_id.0,
            "Parent":         parent.0,
            "VissibleName":   name,
            "Type":           entry_type.as_str(),
            "Version":        1,
            "ModifiedClient": Utc::now().to_rfc3339_opts(SecondsFormat::Nanos, true),
        }]);

        let response = self
            .http
            .put(DOCUMENT_UPDATE_URL)
            .header("User-Agent", "rmsync")
            .bearer_auth(&token.0)
            .json(&payload)
            .send()
            .await?;

        let status = response.status();

        if status.is_success() {
            Ok(())
        } else {
            let body = response.text().await?;

            Err(Error::ApiCallFailure {
                status,
                body,
                api: ApiKind::MetedataUpdate,
            })
        }
    }


#[derive(Deserialize, Debug)]
pub struct Document {
    #[serde(rename = "ID")]
    pub id: DocumentId,
    #[serde(rename = "Version")]
    version: u16,
    #[serde(rename = "Message")]
    message: String,
    #[serde(rename = "Success")]
    success: bool,
    #[serde(rename = "BlobURLGet")]
    blob_url_get: String,
    #[serde(rename = "BlobURLGetExpires")]
    blob_url_get_expires: String,
    #[serde(rename = "ModifiedClient")]
    modified_client: String,
    #[serde(rename = "Type")]
    tpe: String,
    #[serde(rename = "VissibleName")]
    visible_name: String,
    #[serde(rename = "CurrentPage")]
    current_page: u16,
    #[serde(rename = "Bookmarked")]
    bookmarked: bool,
    #[serde(rename = "Parent")]
    parent: DocumentId,
}

enum EntryType {
    #[allow(unused)]
    Collection,
    Document,
}

impl EntryType {
    fn as_str(&self) -> &str {
        match self {
            EntryType::Collection => "CollectionType",
            EntryType::Document => "DocumentType",
        }
    }
}

#[derive(Deserialize, Debug)]
struct UploadRequestResponse {
    #[serde(rename = "ID")]
    id: String,
    #[serde(rename = "Version")]
    version: u32,
    #[serde(rename = "Message")]
    message: String,
    #[serde(rename = "Success")]
    success: bool,
    #[serde(rename = "BlobURLPut")]
    blob_url_put: String,
    #[serde(rename = "BlobURLPutExpires")]
    blob_url_put_expires: String,
}
     */


    companion object {

        val DEVICE_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRoMC11c2VyaWQiOiJhdXRoMHw1ZmM5MWQ2ODY4NDNkYjAwNzc1ZDQxNWEiLCJkZXZpY2UtZGVzYyI6ImRlc2t0b3Atd2luZG93cyIsImRldmljZS1pZCI6ImUyMmQzOGViLWUzODctNDMzZS1hMDI2LWY4NjE5ZGE1M2Y4NCIsImlhdCI6MTYwNzIxNjU3MSwiaXNzIjoick0gV2ViQXBwIiwianRpIjoiY2swdC95NklpVEE9IiwibmJmIjoxNjA3MjE2NTcxLCJzdWIiOiJyTSBEZXZpY2UgVG9rZW4ifQ.iLLPbPgLA22gIJ3a4pHXCqqn4fCFRHF-X3TXv-0oI40"

    }
}