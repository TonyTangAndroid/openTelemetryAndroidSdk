package com.example.hello_otel;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class GzipCompressionTest {

    public static void main(String[] args) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://your-server-url.com/your-endpoint")
                .addHeader("Accept-Encoding", "gzip")
                .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            long compressedSize = response.body().contentLength();
            System.out.println("Compressed Response Size: " + compressedSize + " bytes");

            InputStream responseStream = response.body().byteStream();
            GZIPInputStream gzipInputStream = new GZIPInputStream(responseStream);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }

            byte[] decompressedData = byteArrayOutputStream.toByteArray();
            long decompressedSize = decompressedData.length;
            System.out.println("Decompressed Response Size: " + decompressedSize + " bytes");

            double compressionRate = ((double) decompressedSize - compressedSize) / decompressedSize * 100;
            System.out.println("Compression Rate: " + compressionRate + "%");
        } else {
            System.out.println("Request failed with code: " + response.code());
        }
    }
}