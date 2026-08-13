package io.github.mschout.srsd.postfix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.mschout.email.srs.SRS;
import io.netty.channel.embedded.EmbeddedChannel;
import java.security.InvalidKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SRSServerHandlerTest {
  private static final String LOCAL_ALIAS = "forwarder.example.com";

  private static final String SRS_ADDRESS = "SRS0=HHH=TT=example.com=user@" + LOCAL_ALIAS;

  private SRS srs;

  private EmbeddedChannel channel;

  @BeforeEach
  void setUp() {
    srs = mock(SRS.class);
    channel = new EmbeddedChannel(new SRSServerHandler(srs, LOCAL_ALIAS));
  }

  private String request(String command) {
    channel.writeInbound(command);
    return channel.readOutbound();
  }

  @Test
  void forwardRewritesAddress() throws InvalidKeyException {
    when(srs.forward("user@example.com", LOCAL_ALIAS)).thenReturn(SRS_ADDRESS);

    assertThat(request("srsencoder user@example.com")).isEqualTo("OK " + SRS_ADDRESS);
  }

  @Test
  void forwardCommandIsCaseInsensitive() throws InvalidKeyException {
    when(srs.forward("user@example.com", LOCAL_ALIAS)).thenReturn(SRS_ADDRESS);

    assertThat(request("SRSEncoder user@example.com")).isEqualTo("OK " + SRS_ADDRESS);
  }

  @Test
  void forwardReturnsNotFoundForAddressWithoutDomain() {
    assertThat(request("srsencoder postmaster"))
        .isEqualTo("NOTFOUND address does not contain domain");
  }

  @Test
  void forwardReturnsPermWhenRewriteIsEmpty() throws InvalidKeyException {
    when(srs.forward("user@example.com", LOCAL_ALIAS)).thenReturn("");

    assertThat(request("srsencoder user@example.com")).isEqualTo("PERM srs forwarding failed");
  }

  @Test
  void forwardReturnsNotFoundOnInvalidKey() throws InvalidKeyException {
    when(srs.forward("user@example.com", LOCAL_ALIAS))
        .thenThrow(new InvalidKeyException("no valid key found"));

    assertThat(request("srsencoder user@example.com")).isEqualTo("NOTFOUND no valid key found");
  }

  @Test
  void reverseRewritesSRSAddress() {
    when(srs.isSRS(SRS_ADDRESS)).thenReturn(true);
    when(srs.reverse(SRS_ADDRESS)).thenReturn("user@example.com");

    assertThat(request("srsdecoder " + SRS_ADDRESS)).isEqualTo("OK user@example.com");
  }

  @Test
  void reverseCommandIsCaseInsensitive() {
    when(srs.isSRS(SRS_ADDRESS)).thenReturn(true);
    when(srs.reverse(SRS_ADDRESS)).thenReturn("user@example.com");

    assertThat(request("SRSDecoder " + SRS_ADDRESS)).isEqualTo("OK user@example.com");
  }

  @Test
  void reverseIgnoresNonSRSAddress() {
    when(srs.isSRS("user@example.com")).thenReturn(false);

    assertThat(request("srsdecoder user@example.com"))
        .isEqualTo("NOTFOUND address is not SRS encoded");
  }

  @Test
  void reverseReturnsNotFoundForAddressWithoutDomain() {
    when(srs.isSRS("SRS0=HHH=TT=example.com=user")).thenReturn(true);

    assertThat(request("srsdecoder SRS0=HHH=TT=example.com=user"))
        .isEqualTo("NOTFOUND address does not contain a domain");
  }

  @Test
  void reverseIgnoresExternalDomains() {
    String external = "SRS0=HHH=TT=example.com=user@other.example.net";
    when(srs.isSRS(external)).thenReturn(true);

    assertThat(request("srsdecoder " + external))
        .isEqualTo("NOTFOUND external domains are ignored");
  }

  @Test
  void reverseMatchesLocalAliasCaseInsensitively() {
    String mixedCase = "SRS0=HHH=TT=example.com=user@" + LOCAL_ALIAS.toUpperCase();
    when(srs.isSRS(mixedCase)).thenReturn(true);
    when(srs.reverse(mixedCase)).thenReturn("user@example.com");

    assertThat(request("srsdecoder " + mixedCase)).isEqualTo("OK user@example.com");
  }

  @Test
  void reverseReturnsNotFoundWhenRewriteIsEmpty() {
    when(srs.isSRS(SRS_ADDRESS)).thenReturn(true);
    when(srs.reverse(SRS_ADDRESS)).thenReturn("");

    assertThat(request("srsdecoder " + SRS_ADDRESS)).isEqualTo("NOTFOUND invalid srs email");
  }

  @Test
  void unrecognizedCommandThrows() {
    assertThatThrownBy(() -> channel.writeInbound("bogus user@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unrecognized SRS request");
  }
}
