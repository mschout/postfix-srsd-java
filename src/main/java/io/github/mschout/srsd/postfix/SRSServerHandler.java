package io.github.mschout.srsd.postfix;

import com.google.common.base.Strings;
import io.github.mschout.email.srs.SRS;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.security.InvalidKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Slf4j
public class SRSServerHandler extends ChannelInboundHandlerAdapter {
  private static final String CMD_FORWARD_PREFIX = "srsencoder ";

  private static final String CMD_REVERSE_PREFIX = "srsdecoder ";

  private final SRS srs;

  private final String localAlias;

  @Override
  public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws InterruptedException {
    String request = (String) msg;

    String response = handleRequest(request);

    ctx.writeAndFlush(response).sync();
  }

  private String handleRequest(String request) {
    if (request.toLowerCase().startsWith(CMD_FORWARD_PREFIX)) {
      return forwardAddress(request.substring(CMD_FORWARD_PREFIX.length()));
    } else if (request.toLowerCase().startsWith(CMD_REVERSE_PREFIX)) {
      return reverseAddress(request.substring(CMD_REVERSE_PREFIX.length()));
    } else {
      throw new IllegalArgumentException("Unrecognized SRS request: {}" + request);
    }
  }

  private String forwardAddress(@NotNull String address) {
    try {
      if (!address.contains("@")) {
        return "NOTFOUND address does not contain domain";
      }

      String forward = srs.forward(address, localAlias);
      if (Strings.isNullOrEmpty(forward)) {
        log.error("SRS Forwarding for address {} failed, got null or empty address", forward);
        return "PERM srs forwarding failed";
      }

      if (!address.equals(forward)) log.info("rewrite {} -> {}", address, forward);

      return "OK " + forward;
    } catch (InvalidKeyException e) {
      log.warn("Invalid SRS Key exception on address {}: {}", address, e.getMessage());
      return "NOTFOUND " + e.getMessage();
    }
  }

  private String reverseAddress(@NotNull String address) {
    if (!srs.isSRS(address)) return "NOTFOUND address is not SRS encoded";

    if (!address.contains("@")) return "NOTFOUND address does not contain a domain";

    if (!address.toLowerCase().endsWith("@" + localAlias.toLowerCase())) return "NOTFOUND external domains are ignored";

    String reverse = srs.reverse(address);

    if (Strings.isNullOrEmpty(reverse)) {
      log.error("Failed to reverse address {}: received empty or null address", address);
      return "NOTFOUND invalid srs email";
    }

    if (!address.equals(reverse)) log.info("rewrite {} -> {}", address, reverse);

    return "OK " + reverse;
  }
}
