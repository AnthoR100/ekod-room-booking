package fr.ekod.cda.ja.ekod_room_booking.service;

import fr.ekod.cda.ja.ekod_room_booking.exception.InvalidFileTypeException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;
@Service
public class StorageService {

    private final S3Client s3;
    private final String bucket;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    public StorageService(S3Client s3, @Value("${scaleway.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    // List object keys in the bucket
    public List<String> list() {
        return s3.listObjectsV2(b -> b.bucket(bucket)).contents()
                .stream().map(S3Object::key).toList();
    }

    // Upload a file using the One Zone storage class (cheapest non-Glacier)
    public void upload(String key, Path file) {
        s3.putObject(b -> b.bucket(bucket)
                        .key(key)
                        .storageClass(StorageClass.ONEZONE_IA),
                RequestBody.fromFile(file));
    }

    public void upload(String key, MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException();
        }
        s3.putObject(b -> b.bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .storageClass(StorageClass.ONEZONE_IA),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public ResponseInputStream<GetObjectResponse> download(String key) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
