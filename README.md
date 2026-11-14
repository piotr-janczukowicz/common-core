[![Codacy Badge](https://app.codacy.com/project/badge/Grade/df617ba464a4461fb1b33a10e01dba13)](https://app.codacy.com/gh/piotr-janczukowicz/common-core/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Coverage](https://app.codacy.com/project/badge/Coverage/df617ba464a4461fb1b33a10e01dba13)](https://app.codacy.com/gh/piotr-janczukowicz/common-core/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Build](https://github.com/piotr-janczukowicz/common-core/actions/workflows/main.yml/badge.svg)](https://github.com/piotr-janczukowicz/common-core/actions/workflows/main.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.piotr-janczukowicz/common-core)](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/common-core)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)

# common-core

Shared building blocks for the `io.github.piotr-janczukowicz` libraries and any Java 21 project that uses OkHttp and
Jackson 3, in one artifact:

- **Utilities**: fail-fast argument checks, blank-safe string helpers, enum lookup from wire strings, bounded stream reads,
  ISO-8601 and HTTP date handling, a memoized supplier, immutable collection copies, pagination helpers and annotations such as `@RecordBuilder`.
- **JSON**: a Jackson-free `JsonCodec` contract, an `OptionalNull` wrapper for PATCH-style payloads, Jackson adapters for
  union-shaped JSON, a module that reads collections as immutable ones, and a serializer that writes `5.0` as `5`.
- **HTTP**: a small stack for REST clients, built on OkHttp, with immutable request and response types, a transport with
  composable decorators, and a client with authentication, redirects, an origin guard and polling.

**Requirements:** Java 21. The library is a Java module, `common.core`, and also works on the class path. It is published on
[Maven Central](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/common-core):

```xml
<dependency>
  <groupId>io.github.piotr-janczukowicz</groupId>
  <artifactId>common-core</artifactId>
  <version>LATEST_VERSION</version>
</dependency>
```

Replace `LATEST_VERSION` with the version in the Maven Central badge at the top of this page, or with the newest entry on
the [releases page](https://github.com/piotr-janczukowicz/common-core/releases).

OkHttp, Jackson 3 (`tools.jackson`) and SLF4J come with it. `org.jspecify` and `record-builder-core` are `requires static` in
the module descriptor: nothing needs them at run time except your own use of the `@RecordBuilder` annotation processor.

The test helpers are published as a test-jar: scripted transports and loopback servers for the HTTP stack, a mutable clock,
the `@UnitTest` and `@IntegrationTest` tags, ArchUnit rules for models, and the base classes of the catalog tests that check
models against an API description:

```xml
<dependency>
  <groupId>io.github.piotr-janczukowicz</groupId>
  <artifactId>common-core</artifactId>
  <version>LATEST_VERSION</version>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

## Utilities

Packages `util`, `annotation`, `openapi.model` and `pagination` under `io.github.piotr_janczukowicz`. The code uses only the
JDK and the [JSpecify](https://jspecify.dev) nullness annotations.

### What is in it

| Class | What it does |
|---|---|
| [`ValidationUtils`](src/main/java/io/github/piotr_janczukowicz/util/ValidationUtils.java) | `require*` checks for arguments and configuration: not null, not blank, positive, at least, at most, between, non-empty, size, unique items, no blank or null elements, length, pattern, path safety (`requireSafePath`, `requireFilePath`), and group checks such as exactly one. Each returns the value so you can assign it |
| [`ListUtils`](src/main/java/io/github/piotr_janczukowicz/util/ListUtils.java), [`SetUtils`](src/main/java/io/github/piotr_janczukowicz/util/SetUtils.java), [`MapUtils`](src/main/java/io/github/piotr_janczukowicz/util/MapUtils.java) | Unmodifiable copies: `copyOf` for a required collection, `copyOrNull` to keep an absent one absent, `copyOrEmpty` to turn it into an empty one, and `orEmpty` to do that without copying |
| [`StringUtils`](src/main/java/io/github/piotr_janczukowicz/util/StringUtils.java) | `isBlank`, `isNotBlank` and `stripToNull`, with the semantics of Apache Commons Lang 3 and no dependency on it |
| [`EnumUtils`](src/main/java/io/github/piotr_janczukowicz/util/EnumUtils.java) | `WireIndex`: an immutable lookup of enum constants by the string a protocol uses, with an `Optional`, a default, or `requireFromWire`, which fails with `Unknown <label>: <value>` for an unknown value |
| [`IoUtils`](src/main/java/io/github/piotr_janczukowicz/util/IoUtils.java) | `readAllBounded` and `copyBounded` stop with `BoundedReadLimitException` instead of reading past a limit |
| [`DateTimeUtils`](src/main/java/io/github/piotr_janczukowicz/util/DateTimeUtils.java) | `Instant` to ISO-8601 UTC text, `Instant` to and from RFC 1123 (HTTP dates), epoch seconds to `Instant` within a valid range, ISO dates and the start of a day in UTC |
| [`EncodingUtils`](src/main/java/io/github/piotr_janczukowicz/util/EncodingUtils.java) | Base64 encoding, and decoding (also of text wrapped over several lines) that reports your message and never echoes the input |
| [`NumberUtils`](src/main/java/io/github/piotr_janczukowicz/util/NumberUtils.java) | `parseInt` and `parseLong` for text from outside, such as a header: an empty result instead of an exception for text that is absent, blank, out of range, or not an optional `-` and ASCII digits |
| [`FileUtils`](src/main/java/io/github/piotr_janczukowicz/util/FileUtils.java) | Reads a classpath resource as a string or bytes |
| [`ConcurrencyUtils`](src/main/java/io/github/piotr_janczukowicz/util/ConcurrencyUtils.java) | Sleeps interruptibly and turns an interrupt into an `InterruptedIOException`, restoring the interrupt flag |
| [`LazyValue`](src/main/java/io/github/piotr_janczukowicz/util/LazyValue.java) | A thread-safe memoized supplier; an exception is not cached |
| [`PaginationConfiguration`](src/main/java/io/github/piotr_janczukowicz/pagination/PaginationConfiguration.java) | A page cap (none by default) and a default `per_page` between 1 and 100 for list pagination |
| [`Page`](src/main/java/io/github/piotr_janczukowicz/pagination/Page.java) and [`Pages`](src/main/java/io/github/piotr_janczukowicz/pagination/Pages.java) | The page contract of a paginated list, and lazy sequential streams of its items or pages that load a page only when needed |
| [`LinkHeader`](src/main/java/io/github/piotr_janczukowicz/pagination/LinkHeader.java) | Finds the `rel="next"` relation of an HTTP `Link` header, the link to the next page |
| [`PageWalkGuard`](src/main/java/io/github/piotr_janczukowicz/pagination/PageWalkGuard.java) | Refuses a next link that was already followed, so a server cannot walk the caller in circles |
| [`PageQueries`](src/main/java/io/github/piotr_janczukowicz/pagination/PageQueries.java) | `boundPerPage` picks the page size within an endpoint maximum, and `withParameters` adds page parameters to a base query |
| `@RecordBuilder` | Generates a `RecordNameBuilder` for an annotated record at compile time, marked as generated for JaCoCo |
| `@CatalogSchema`, `@CatalogSimpleXor`, `@SuppressSchemaTypeMismatch` | Runtime annotations that map a type to a schema, for tests that check models against an API description |

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

## JSON

The `io.github.piotr_janczukowicz.json` package has no Jackson types: `JsonCodec` (decode and encode bytes), `JsonType` and
`JsonTypes` (generic target types), `OptionalNull`, and the `JsonDecodeException` / `JsonEncodeException` failures. Implement
`JsonCodec` over your own mapper; the contract is in its Javadoc. The adapters live in
`io.github.piotr_janczukowicz.json.jackson` and target Jackson 3 (`tools.jackson`). All of them register on a `SimpleModule`.

### OptionalNull

`OptionalNull<T>` tells apart an absent field (Java `null`), an explicit JSON `null` (`OptionalNull.ofNull()`) and a value
(`OptionalNull.of(value)`). `JsonMappers.snakeCase()` registers `OptionalNullModule`; on another mapper call
`builder.addModule(new OptionalNullModule())`. Put `@JsonInclude(NON_NULL)` on the enclosing record so an absent field is
omitted.

A value that the inner deserializer reads as `null` (for example an unknown enum constant under
`READ_UNKNOWN_ENUM_VALUES_AS_NULL`) fails instead of becoming `OptionalNull.ofNull()`, so a PATCH cannot clear a field by
accident.

### Union adapters

| Adapter | Use it for |
| --- | --- |
| `StringOrObjectAdapters` | a value that is either a string or an object |
| `ArrayOrWrappedAdapters` | a string array that is either bare or wrapped in one named property |
| `PolymorphicObjectAdapters` | an object whose type is picked by the first present, non-null property |
| `ScalarUnionReaders` | building a value from a string, number, boolean, string array or `null` inside your own deserializer |

```java
StringOrObjectAdapters.register(
        module,
        Label.class,
        Label::name,
        Label::details,
        Label::ofName,
        Label::ofDetails,
        Label.Details.class);
```

Every `register` method rejects a `null` argument when `register` is called. A union value that holds neither of its forms
fails to serialize instead of being written as `null`.

### Immutable collections

`ImmutableCollectionsModule` makes `List`, `Collection`, `Set` and `Map` deserialize as immutable collections, keeping element
types. JSON `null` and absent properties stay `null`. Null elements and map entries with a null value are kept, lists and maps keep
the order of the JSON, and the iteration order of sets is unspecified.

```java
JsonMapper mapper = JsonMapper.builder().addModule(new ImmutableCollectionsModule()).build();
```

### Mapper defaults

`JsonMappers.snakeCase()` returns a `JsonMapper.Builder` for APIs whose JSON uses `snake_case` names and add properties over
time: unknown properties are ignored, duplicate keys and fractional numbers for integers are refused, an unknown enum constant
reads as `null`, a single value reads as a one-element array, dates are written as text, collections are immutable,
`OptionalNull` is registered, and `Instant` values are read with `FastInstantDeserializer`. Add your own modules and build the
mapper once. `JacksonJsonCodec` is a `JsonCodec` over such a mapper.

### Reading nodes and tokens

`JsonNodes.textOrNull(node, field)` reads a string field and gives `null` for a missing or non-string one. `JsonTokens` checks
the token a parser is on and reads an object unless it is empty. `BufferedJson` copies one JSON value as tokens, so a deserializer can inspect a
field and then bind a type without building a tree.

### Integral doubles

`IntegralDoubleSerializer` writes a `Double` without a fractional part as a JSON integer (`5.0` becomes `5`) and every other
value, including `NaN`, infinities, negative zero and values beyond the `long` range, as an ordinary number. Register it with
`module.addSerializer(Double.class, new IntegralDoubleSerializer())`.

### Mapper expectations

Do not enable default typing on a mapper that reads untrusted JSON. Keep Jackson's `StreamReadConstraints` at their defaults or
tighter: they bound the size of numbers, and `ScalarUnionReaders` additionally rejects a number with an exponent beyond 1000
decimal places.

## HTTP

A small HTTP stack for Java 21 REST clients, built on OkHttp: immutable request and response types, a transport with
composable decorators for retries, rate limits, concurrency limits, write pacing and conditional caching, and a client that adds
a base URI, authentication, redirect handling, an origin guard, error mapping and polling. It is the base of
[`github-rest-client`](https://github.com/piotr-janczukowicz/github-rest-client) and works for any JSON API.

The stack has no JSON mapper of its own: you pass a `JsonCodec` (see [JSON](#json)) that reads and writes bytes with the
mapper you use.

### Layers

| Package | Contents |
| --- | --- |
| `io.github.piotr_janczukowicz.http` | `HttpRequest`, `HttpResponse`, `HttpStreamResponse`, `HttpHeaders` with `HttpHeaderNames` and `HttpHeadersValidator`, `HttpMethod`, `HttpStatus`, `HttpBody`, `MultipartFormData`, `HttpUserAgents`, `HttpErrorBodies` (bounded error text), `Digests` (keyed digests) and their exceptions |
| `io.github.piotr_janczukowicz.http.transport` | `HttpTransport`, the OkHttp implementation (`HttpTransports`), the decorators, the rate-limit detection (`RateLimitDetector`, `RateLimitGate`), and the timeout, proxy, TLS and retry settings |
| `io.github.piotr_janczukowicz.http.client` | `HttpClient`, the callers that send an `HttpCall` and decode the answer (`HttpClientCaller`, `AbstractHttpCaller`, `HttpResourceApi`), `HttpResult`, `HttpDownloads`, the error mapper, redirect policy, origin guard, polling and call observers |
| `io.github.piotr_janczukowicz.http.client.authentication` | `AuthenticationProviders`: bearer, renewable bearer, basic and none |
| `io.github.piotr_janczukowicz.http.uri`, `io.github.piotr_janczukowicz.uri` | `HttpUris` (resolving API paths, query copies, `originOf`), `ParsedUrl`, `PathTemplate` and `PathSegments` |

The transport knows nothing about base URIs or credentials; the client knows nothing about connections. You compose the
transport, hand it to the client, and the client owns it from then on: `close()` closes the whole stack.

### Send a request

```java
HttpTransport transport = new RetryingHttpTransport(HttpTransports.okHttp());

try (HttpClient client = HttpClient.builder(
                transport, HttpClientConfiguration.of(URI.create("https://api.example.com/v1/")))
        .auth(AuthenticationProviders.bearer(token))
        .build()) {

    HttpClientCaller caller = new HttpClientCaller(client, codec); // codec: your JsonCodec

    Widget widget = caller.fetch(HttpCall.get("widgets/42"), Widget.class);

    HttpCall create = caller.json(HttpCall.post("widgets"), new NewWidget("name"));
    HttpResponse created = caller.send(create);
}
```

A 2xx answer becomes an `HttpResponse` or a decoded value. Any other status becomes the unchecked exception that the client's
`HttpErrorMapper` builds (an `HttpException` by default), and a failure of the network stays an `IOException`. The caller
also has `fetchOptional`, `fetchOrEmpty` and `fetchUnless` for answers that may be absent, `fetchResult` for the response
headers next to the value (an `ETag`, say), `fetchByStatus`, `isExisting`, `stream`, `download`, `redirectLocation` and
`redirectLocationIfAny`, and `multipart` and `jsonIfPresent` to build a call.

A client can be shared between threads when its transport, authentication provider and observers are.

### Compose the transport

Each decorator wraps another transport and adds one behavior. From the outside in, the order that works is:

| Decorator | What it does |
| --- | --- |
| `ConditionalCachingHttpTransport` | Keeps GET responses that carry an `ETag` or `Last-Modified` and answers a repeated GET from memory when the server replies 304, so the body is not sent again |
| `RetryingHttpTransport` | Retries failed attempts and waits out rate limits, with separate budgets for I/O failures, rate limits and 5xx; it remembers a reported limit so later calls of the same credential do not send a doomed request |
| `MutationPacingHttpTransport` | Keeps a minimum interval between the starts of POST, PUT, PATCH and DELETE for APIs that throttle writes; GET is never delayed |
| `ConcurrencyLimitingHttpTransport` | Lets at most N requests run at once, since the OkHttp transport sends on the caller's thread |
| OkHttp (`HttpTransports.okHttp`) | The network, with timeouts, optional proxy, TLS trust and certificate pins |

```java
HttpTransport transport = new RetryingHttpTransport(
        new ConcurrencyLimitingHttpTransport(HttpTransports.okHttp(), 4),
        RetryConfiguration.defaults());
```

Retries use an I/O budget and a 5xx budget (GET, HEAD and OPTIONS by default) and a rate-limit budget that covers every
method. A limit that names its wait through `Retry-After` or a reset time is waited out when it is at most five minutes;
without a named wait the transport waits 60, 120 and 240 seconds with some jitter. Every value is in `RetryConfiguration`.

### Authenticate

| Provider | Use it for |
| --- | --- |
| `AuthenticationProviders.bearer(token)` | A fixed token |
| `AuthenticationProviders.bearer(supplier)` | A token read on every request |
| `AuthenticationProviders.bearer(supplier, isRenewable)` | A token that expires: after a 401 the client sends the request once more if a different token is available |
| `AuthenticationProviders.basic(user, password)` | A user and password, or a client ID and secret |
| `AuthenticationProviders.header(name, token)` | A fixed token sent as the raw value of a header of its own, such as `PRIVATE-TOKEN` |
| `AuthenticationProviders.none()` | No credentials |

Default headers never replace an authentication header. Only a header set on one call can, which is how a single request
sends another credential.

### Redirects and origins

The client follows up to five redirects by default. A GET or HEAD may leave the origin, and then only `Accept` and
`User-Agent` are kept: `Authorization`, `Proxy-Authorization` and `Cookie` are never forwarded to another origin, and a
`RedirectPolicy` that lists one of them is rejected. `HttpOriginGuard` accepts an absolute URI only when it matches the
base origin and path prefix.

### Wait for a result that is computed in the background

Some endpoints answer 202 until a report is ready. `fetchWhenReady` repeats the request a fixed number of times with a fixed
pause and returns `HttpComputed.Ready`, `NotReady` or `NoData`:

```java
HttpComputed<Report> report = caller.fetchWhenReady(
        HttpCall.get("reports/7"), Report.class, HttpAcceptedPolling.defaults());
```

### Observe calls

An `HttpCallObserver` receives one `HttpCallEvent` for every call, after retries and redirects, for metrics, tracing or audit
logs. It runs on the calling thread, and an exception it throws is logged and dropped, so observing never changes an outcome.

## Security

See [SECURITY.md](SECURITY.md) to report a vulnerability.

## License

See [LICENSE](LICENSE).
