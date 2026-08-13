package io.github.mschout.srsd.postfix;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Test helper for encoding and decoding netstrings ("length:payload,"). */
final class Netstrings {
  private Netstrings() {}

  static byte[] netstring(String payload) {
    byte[] data = payload.getBytes(StandardCharsets.UTF_8);
    return (data.length + ":" + payload + ",").getBytes(StandardCharsets.UTF_8);
  }

  static String readNetstring(InputStream in) throws IOException {
    var length = new StringBuilder();

    int b;
    while ((b = in.read()) != ':') {
      if (b == -1) throw new IOException("EOF while reading netstring length");
      length.append((char) b);
    }

    byte[] payload = in.readNBytes(Integer.parseInt(length.toString()));

    if (in.read() != ',') throw new IOException("netstring payload not terminated with ','");

    return new String(payload, StandardCharsets.UTF_8);
  }
}
