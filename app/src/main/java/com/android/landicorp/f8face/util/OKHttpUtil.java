package com.android.landicorp.f8face.util;

import android.content.Context;

import com.android.landicorp.f8face.R;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.OkHttpClient;

/**
 * Created by admin on 2019/7/18.
 */

public class OKHttpUtil {
    //获取证书流
    public static void readHttpsCer(Context context) {
        try {
            InputStream is = context.getAssets().open("wxpay.cer");
            addCertificate(is); // 这里将证书读取出来，，放在配置中byte[]里
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // 证书数据
    private static List<byte[]> CERTIFICATES_DATA = new ArrayList<>();

    /**
     * 添加https证书
     *
     * @param inputStream
     */
    private synchronized static void addCertificate(InputStream inputStream) {
        if (inputStream != null) {

            try {
                int ava = 0;// 数据当次可读长度
                int len = 0;// 数据总长度
                ArrayList<byte[]> data = new ArrayList<>();
                while ((ava = inputStream.available()) > 0) {
                    byte[] buffer = new byte[ava];
                    inputStream.read(buffer);
                    data.add(buffer);
                    len += ava;
                }

                byte[] buff = new byte[len];
                int dstPos = 0;
                for (byte[] bytes : data) {
                    int length = bytes.length;
                    System.arraycopy(bytes, 0, buff, dstPos, length);
                    dstPos += length;
                }

                CERTIFICATES_DATA.add(buff);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * https证书
     *
     * @return
     */
    public static List<byte[]> getCertificatesData() {
        return CERTIFICATES_DATA;
    }
    public static OkHttpClient createOkHttp() {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // 添加证书
        List<InputStream> certificates = new ArrayList<>();
        List<byte[]> certs_data = getCertificatesData();

        // 将字节数组转为数组输入流
        if (certs_data != null && !certs_data.isEmpty()) {
            for (byte[] bytes : certs_data) {
                certificates.add(new ByteArrayInputStream(bytes));
            }
        }


        SSLSocketFactory sslSocketFactory = getSocketFactory(certificates);
        if (sslSocketFactory != null) {
            builder.sslSocketFactory(sslSocketFactory);
        }

        return builder.build();
    }


    /**
     * 添加证书
     *
     * @param certificates
     */
    private static SSLSocketFactory getSocketFactory(List<InputStream> certificates) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            try {
                for (int i = 0, size = certificates.size(); i < size; ) {
                    InputStream certificate = certificates.get(i);
                    String certificateAlias = Integer.toString(i++);
                    keyStore.setCertificateEntry(certificateAlias, certificateFactory
                            .generateCertificate(certificate));
                    if (certificate != null)
                        certificate.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OkHttpClient customClient(Context context) {
        OkHttpClient okHttpClient = null;
        try {
            KeyStore keystore = KeyStore.getInstance("BKS");
            InputStream inputStream = context.getResources().openRawResource(R.raw.wxpay);
            keystore.load(inputStream, "123456".toCharArray());

            // TrustManager decides which certificate authorities to use.
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            okHttpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory())
                    .build();
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
        return okHttpClient;
    }


}
