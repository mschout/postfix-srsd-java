# Postfix SRSD

This is a Sender Rewriting Scheme (SRS) daemon for postfix, written in Java.
SRS handles address rewriting as part of the SPF/SRS protocol pair.

## What is SRS?

SPF (and related systems) present a challenge to forwarders, since the envelope
sender address might be seen by the destination as a forgery by the forwarding
host. Forwarding services must rewrite the envelope sender address, while
encapsulating the original sender and preventing relay attacks by spammers. The
Sender Rewriting Scheme, or SRS, provides a standard for this rewriting which
makes forwarding compatible with these address verification schemes, preserves
bounce functionality and is not vulnerable to attacks by spammers.

## Dependencies

If building from sources, you need a Java JDK, version 11 or later.

To run the jar file you just need a Java JRE, version 11 or higher.

## Installation

If building from souces:

```
./gradlew jar
```

The generated jar file will be in `build/libs`

## Configuration

You need to generate at least one secret and save it in a file (default is
`/usr/local/etc/postfix/postfix-srsd.secrets').  The format is one secret per
line, with the last one being the current secret used for encoding addresses,
and all lines valid for decoding.

## Configure Postfix

Add something simmiarl to the following to `main.cf`:

```
recipient_canonical_maps = hash:$config_directory/nosrs, socketmap:inet:localhost:2510:srsdecoder
recipient_canonical_classes = envelope_recipient, header_recipient
sender_canonical_maps = hash:$config_directory/nosrs, socketmap:inet:localhost:2510:srsencoder
sender_canonical_classes = envelope_sender
```

the "nosrs" file can be used to sepecify recipients/senders that should not be
rewritten.

## Running

Run the jar file like:

```
java -jar postifx-srsd-java.jar --port 2510 --local-alias bounces.example.com --secret-file /etc/srsd.secrets
```

Run with --help to get usage information:

```
Usage: postfix-srsd [-hV] --local-alias=<localAlias> --secret-file=<secretFile>
                    (--socket=<socketPath> | [[--host=<hostName>]
                    --port=<port>]) ([--log-file=<logFile>]
                    [--syslog-host=<syslogHost>] [--syslog-port=<syslogPort>]
                    [--syslog-facility=<syslogFacility>]
                    [--log-level=<logLevel>])
SRS encoder and decoder daemon for postfix.
  -h, --help                 Show this help message and exit.
      --host=<hostName>      TCP Hostname to bind to
      --local-alias=<localAlias>

      --log-file=<logFile>
      --log-level=<logLevel>
      --port=<port>          TCP Port to listen on
      --secret-file=<secretFile>
                             File containing SRS encoder secrets.  First line
                               is the default secret, additional lines are
                               valid secrets for verifying addresses.
      --socket=<socketPath>  Unix socket file path to listen on.
      --syslog-facility=<syslogFacility>

      --syslog-host=<syslogHost>

      --syslog-port=<syslogPort>

  -V, --version              Print version information and exit.
```

## Docker Compose

There is an example `docker-compose.yml` showing how to run the daaemon with
docker compose.
