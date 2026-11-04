[![Codacy Badge](https://app.codacy.com/project/badge/Grade/2aef4762ea294ad89c04966d6bde8f6c)](https://app.codacy.com/gh/piotr-janczukowicz/common-util/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Coverage](https://app.codacy.com/project/badge/Coverage/2aef4762ea294ad89c04966d6bde8f6c)](https://app.codacy.com/gh/piotr-janczukowicz/common-util/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Build](https://github.com/piotr-janczukowicz/common-util/actions/workflows/main.yml/badge.svg)](https://github.com/piotr-janczukowicz/common-util/actions/workflows/main.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.piotr-janczukowicz/common-util)](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/common-util)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)

# common-util

Small Java utilities and annotations shared by the `io.github.piotr-janczukowicz` libraries: fail-fast
argument checks, blank-safe string helpers, enum lookup from wire strings, bounded stream reads, ISO-8601 and HTTP date
handling, a memoized supplier, pagination settings, and annotations such as `@RecordBuilder`. The code uses only the JDK;
its one dependency is the [JSpecify](https://jspecify.dev) nullness annotations.

**Requirements:** Java 21. The library is a Java module, `common.util`, and also works on the class path. It is published on
[Maven Central](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/common-util):

```xml
<dependency>
    <groupId>io.github.piotr-janczukowicz</groupId>
    <artifactId>common-util</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

Replace `LATEST_VERSION` with the version in the Maven Central badge at the top of this page, or with the newest entry on
the [releases page](https://github.com/piotr-janczukowicz/common-util/releases).

`org.jspecify` and `record-builder-core` are `requires static` in the module descriptor: nothing needs them at run time
except your own use of the `@RecordBuilder` annotation processor.

## What is in it

| Class | What it does |
|---|---|
| [`ValidationUtils`](src/main/java/io/github/piotr_janczukowicz/util/ValidationUtils.java) | `require*` checks for arguments and configuration: not null, not blank, positive, at least, at most, between, non-empty, size, unique items, length, pattern, and group checks such as exactly one. Each returns the value so you can assign it |
| [`StringUtils`](src/main/java/io/github/piotr_janczukowicz/util/StringUtils.java) | `isBlank`, `isNotBlank` and `stripToNull`, with the semantics of Apache Commons Lang 3 and no dependency on it |
| [`EnumUtils`](src/main/java/io/github/piotr_janczukowicz/util/EnumUtils.java) | `WireIndex`: an immutable lookup of enum constants by the string a protocol uses, with an `Optional` or a default for an unknown value |
| [`IoUtils`](src/main/java/io/github/piotr_janczukowicz/util/IoUtils.java) | `readAllBounded` and `copyBounded` stop with `BoundedReadLimitException` instead of reading past a limit |
| [`DateTimeUtils`](src/main/java/io/github/piotr_janczukowicz/util/DateTimeUtils.java) | `Instant` to ISO-8601 UTC text, and `Instant` to and from RFC 1123 (HTTP dates) |
| [`EncodingUtils`](src/main/java/io/github/piotr_janczukowicz/util/EncodingUtils.java) | Base64 decoding that reports your message and never echoes the input |
| [`FileUtils`](src/main/java/io/github/piotr_janczukowicz/util/FileUtils.java) | Reads a classpath resource as a string or bytes |
| [`ConcurrencyUtils`](src/main/java/io/github/piotr_janczukowicz/util/ConcurrencyUtils.java) | Sleeps interruptibly and turns an interrupt into an `InterruptedIOException`, restoring the interrupt flag |
| [`LazyValue`](src/main/java/io/github/piotr_janczukowicz/util/LazyValue.java) | A thread-safe memoized supplier; an exception is not cached |
| [`PaginationConfiguration`](src/main/java/io/github/piotr_janczukowicz/pagination/PaginationConfiguration.java) | A page cap (none by default) and a default `per_page` between 1 and 100 for list pagination |
| `@RecordBuilder` | Generates a `RecordNameBuilder` for an annotated record at compile time, marked as generated for JaCoCo |
| `@CatalogSchema`, `@CatalogSimpleXor`, `@SuppressSchemaTypeMismatch` | Runtime annotations that map a type to a schema, for tests that check models against an API description |

## Usage

### Validate arguments

A violation names the argument. `null` fails with `NullPointerException`, anything else with `IllegalArgumentException`.

```java
import static io.github.piotr_janczukowicz.util.ValidationUtils.*;

record Client(String name, int retries) {
    Client {
        requireNotBlank(name, "name");                // "name must not be blank"
        requireBetween(retries, 0, 10, "retries");
    }
}
```

### Look up an enum by its wire value

Build the index once, in a `static final` field of the enum:

```java
enum State {
    OPEN("open"), CLOSED("closed"), UNKNOWN("unknown");

    private static final EnumUtils.WireIndex<State> INDEX = EnumUtils.index(State.class, State::wire);

    private final String wire;

    State(String wire) { this.wire = wire; }

    String wire() { return wire; }

    static State fromWire(String value) { return INDEX.fromWire(value, UNKNOWN); }
}
```

### Read a stream with a limit

```java
byte[] body = IoUtils.readAllBounded(stream, 1_000_000); // BoundedReadLimitException past one million bytes
```

### Generate a builder for a record

```java
@RecordBuilder
public record Point(int x, int y) {}

Point point = PointBuilder.builder().x(1).y(2).build();
```

This needs the `io.soabase.record-builder:record-builder-processor` annotation processor, which
[`common-parent`](https://github.com/piotr-janczukowicz/common-parent) configures for you.

## Security

See [SECURITY.md](SECURITY.md) to report a vulnerability.

## License

See [LICENSE](LICENSE).
