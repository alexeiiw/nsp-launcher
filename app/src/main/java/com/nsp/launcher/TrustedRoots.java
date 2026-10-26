package com.nsp.launcher;

import android.content.Context;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * SSL socket factory alternativo: confía SOLO en las raíces que empaquetamos
 * (ISRG Root X1/X2 de Let's Encrypt). Se usa como reintento cuando el almacén
 * del sistema del dispositivo (de 2015) no conoce esas raíces.
 */
public final class TrustedRoots {

    private static SSLSocketFactory factory;

    private TrustedRoots() {
    }

    public static synchronized SSLSocketFactory factory(Context context) {
        if (factory != null) return factory;
        try {
            CertificateFactory certificates = CertificateFactory.getInstance("X.509");
            List<Certificate> roots = new ArrayList<Certificate>();
            int[] ids = {R.raw.isrgrootx1, R.raw.isrgrootx2};
            for (int id : ids) {
                InputStream in = context.getResources().openRawResource(id);
                try {
                    roots.add(certificates.generateCertificate(in));
                } finally {
                    in.close();
                }
            }

            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);
            int index = 0;
            for (Certificate root : roots) {
                store.setCertificateEntry("isrg" + (index++), root);
            }
            TrustManagerFactory trust = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trust.init(store);
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, trust.getTrustManagers(), null);
            factory = ssl.getSocketFactory();
        } catch (Exception e) {
            factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        return factory;
    }
}
