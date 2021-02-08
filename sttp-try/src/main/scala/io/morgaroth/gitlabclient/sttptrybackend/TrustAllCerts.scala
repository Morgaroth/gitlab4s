package io.morgaroth.gitlabclient.sttptrybackend

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl._

object TrustAllCerts {
  def configure(): Unit = {
    val allTrustManager = new X509TrustManager {
      def getAcceptedIssuers: Array[X509Certificate] = null

      override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

      override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
    }
    val trustManagers: Array[TrustManager] = Array[TrustManager](allTrustManager)

    // Install the all-trusting trust manager
    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustManagers, new SecureRandom)
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory)

    // Create all-trusting host name verifier
    val allHostsValid = new HostnameVerifier {
      def verify(hostname: String, session: SSLSession) = true
    }
    // Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
  }

}
