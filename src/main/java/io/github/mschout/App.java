package io.github.mschout;

import io.github.mschout.srsd.postfix.SRSServer;

/**
 * Hello world!
 *
 */
public class App {

  public static void main(String[] args) throws InterruptedException {
    new SRSServer(8000).run();
  }
}
