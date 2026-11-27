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
- **GraphQL**: `Variables`, a builder for the variables of a GraphQL request that keeps their JSON types.
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
| [`ListUtils`](src/main/java/io/github/piotr_janczukowicz/util/ListUtils.java), [`SetUtils`](src/main/java/io/github/piotr_janczukowicz/util/SetUtils.java), [`MapUtils`](src/main/java/io/github/piotr_janczukowicz/util/MapUtils.java) | Unmodifiable copies: `copyOf` for a required collection, `copy` to keep an absent one absent, `copyNonNull` to turn it into an empty one, and `orEmpty` to do that without copying |
| [`StringUtils`](src/main/java/io/github/piotr_janczukowicz/util/StringUtils.java) | `isBlank`, `isNotBlank` and `stripToNull`, with the semantics of Apache Commons Lang 3 and no dependency on it |
| [`EnumUtils`](src/main/java/io/github/piotr_janczukowicz/util/EnumUtils.java) | `ValueIndex`: an immutable lookup of enum constants by the string a protocol uses, with an `Optional`, a default, or `require`, which fails with `Unknown <label>: <value>` for an unknown value |
| [`IoUtils`](src/main/java/io/github/piotr_janczukowicz/util/IoUtils.java) | `readAllBounded` and `copyAtMost` stop with `BoundedReadLimitException` instead of reading past a limit |
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

    private static final EnumUtils.ValueIndex<State> VALUE_INDEX = EnumUtils.index(State.class, State::value);

    private final String value;

    State(String value) { this.value = value; }

    String value() { return value; }

    static State of(String value) { return VALUE_INDEX.of(value, UNKNOWN); }
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
`JsonTypes` (generic target types), `OptionalNull`, the `@ValueUnion` annotation, and the `JsonDecodeException` / `JsonEncodeException` failures. Implement
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

### Value unions

`@ValueUnion` marks a type whose JSON value is one of several token kinds: it names one branch class per token (`string`,
`integer`, `number`, `flag`, `array`, `object`) and optionally an `unknown` class that keeps any other token as it came;
without it such a token is refused (`X takes a string or an object`). `ValueUnionModule` (package `io.github.piotr_janczukowicz.json.jackson.module.valueunion`) reads the annotated type: it picks
the class from the first token and lets that class read the value (a record with a delegating `@JsonCreator`, a plain record,
or an object union). An integer goes to `integer` when it fits a `long`, else to `number`; a fraction never goes to
`integer`. A branch created from a `BigDecimal` gets the number exactly, in plain notation; a branch created from a list of
strings takes JSON strings only; with an `unknown` class, an array holding any other item goes to it as a whole, and without one it is refused. The annotation is not inherited, so a branch that implements the union reads as itself.
`JsonMappers.snakeCase()` registers the module; on another mapper call `builder.addModule(new ValueUnionModule())`.

```java
@ValueUnion(string = Rule.Text.class, object = RuleDetails.class)
public sealed interface Rule permits Rule.Text, RuleDetails {
    record Text(@JsonValue String value) implements Rule {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Text {}
    }
}
```

### Presence unions

`@PresenceUnion` marks an object union whose branch is picked by the names the object has, where Jackson's deduction
cannot pick it (a branch with no property of its own beside properties several other branches share). `keys` names each
property only one branch declares; the first of them in the object (document order) picks its branch. With none, the
`fallback` is read when the object has every name of `fallbackRequires`; else the `unknown` class keeps the object as it
came, and without it the object is refused. A name counts when present, also with `null`; a non-object value is refused.
`ValueUnionModule` reads it too, so no other wiring is needed; the interface carries no `@JsonTypeInfo`.

```java
@PresenceUnion(keys = {@PresenceUnion.Key(name = "blob_url", type = Commit.class)},
        fallback = ActionsLog.class, fallbackRequires = {"path", "start_line"}, unknown = LocationUnknown.class)
public sealed interface Location permits Commit, ActionsLog, LocationUnknown {}
```

### Empty object as null

`@EmptyObjectAsNull` marks a record component (or field, accessor or parameter) whose JSON value may be an object with no
properties standing for "no value". Such an object and JSON `null` both read as `null`; any other value reads as the
declared type, refused as that type refuses it. Writing is unchanged: a `null` is written as the property's usual null.
`ValueUnionModule` reads it, so no other wiring is needed.

```java
record Commit(String sha, @EmptyObjectAsNull @Nullable User author) {}
```

### Epoch instants

`@EpochInstant(EpochUnit.SECONDS)` or `@EpochInstant(EpochUnit.MILLIS)` marks an `Instant` record component (or field,
accessor or parameter) whose JSON value is an integer count of that unit since 1970-01-01T00:00:00Z. Reading is
lenient: it accepts an integer, a number with a zero fraction (`5.0`, `1.7E12`, read exactly) and ISO-8601 text; JSON
`null` reads as `null`. A number with a non-zero fraction, a boolean, object or array, and an integer outside the range
of `Instant` are refused with a Jackson `DatabindException`. Writing is strict: the integer count is written, and an
`Instant` finer than the unit is refused; a `null` follows the property's usual null handling. `ValueUnionModule` reads
and writes it, so no other wiring is needed. `ValidationUtils.requireWholeSeconds` and `requireWholeMillis` (which let `null`
pass) give a record constructor the same precision check.

```java
record Event(String name, @EpochInstant(EpochUnit.MILLIS) Instant at) {}
```

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
`OptionalNull` and `@ValueUnion` types are registered, and `Instant` values are read with `FastInstantDeserializer`. Add your own modules and build the
mapper once. `JacksonJsonCodec` is a `JsonCodec` over such a mapper.

### Reading nodes and tokens

`JsonNodes.text(node, field)` reads a string field and gives `null` for a missing or non-string one. `BufferedJson` copies one JSON value as tokens, so a deserializer can inspect a
field and then bind a type without building a tree.

### Integral doubles

`IntegralDoubleSerializer` writes a `Double` without a fractional part as a JSON integer (`5.0` becomes `5`) and every other
value, including `NaN`, infinities, negative zero and values beyond the `long` range, as an ordinary number. Register it with
`module.addSerializer(Double.class, new IntegralDoubleSerializer())`.

### Mapper expectations

Do not enable default typing on a mapper that reads untrusted JSON. Keep Jackson's `StreamReadConstraints` at their defaults or
tighter: they bound the size of numbers, and `ValueUnionModule` additionally rejects a number with an exponent beyond 1000
decimal places.

## GraphQL

The `io.github.piotr_janczukowicz.graphql` package holds `Variables`, which builds the variables of a GraphQL request, or the
fields of one input object, in the order they are added. A `null` value is left out, `clearable` writes JSON `null` for
`OptionalNull.ofNull()`, lists are stored as unmodifiable copies without `null` items, and a name can be added only once:

```java
Map<String, @Nullable Object> variables = Variables.builder()
        .value("owner", owner)
        .values("states", states, State::value)
        .value("input", input, CreateLabelInput::toVariable)
        .build();
```

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
HttpTransport transport = new RetryingHttpTransport(HttpTransports.create());

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
also has `fetchOptional`, `find` (an HTTP 404 gives empty), `findOptional` and `fetchExcept` for answers that may be
absent, `fetchWithResponse` for the response headers next to the value (an `ETag`, say), `fetchByStatus`, `exists`,
`stream`, `download`, `location` and `findLocation`, and `multipart` to build a call.

A client can be shared between threads when its transport, authentication provider and observers are.

### Compose the transport

Each decorator wraps another transport and adds one behavior. From the outside in, the order that works is:

| Decorator | What it does |
| --- | --- |
| `ConditionalCachingHttpTransport` | Keeps GET responses that carry an `ETag` or `Last-Modified` and answers a repeated GET from memory when the server replies 304, so the body is not sent again |
| `RetryingHttpTransport` | Retries failed attempts and waits out rate limits, with separate budgets for I/O failures, rate limits and 5xx; it remembers a reported limit so later calls of the same credential do not send a doomed request |
| `MutationPacingHttpTransport` | Keeps a minimum interval between the starts of POST, PUT, PATCH and DELETE for APIs that throttle writes; GET is never delayed |
| `ConcurrencyLimitingHttpTransport` | Lets at most N requests run at once, since the OkHttp transport sends on the caller's thread |
| OkHttp (`HttpTransports.create`) | The network, with timeouts, optional proxy, TLS trust and certificate pins |

```java
HttpTransport transport = new RetryingHttpTransport(
        new ConcurrencyLimitingHttpTransport(HttpTransports.create(), 4),
        RetryConfiguration.defaults());
```

Retries use an I/O budget and a 5xx budget (GET, HEAD and OPTIONS by default) and a rate-limit budget that covers every
method. A limit that names its wait through `Retry-After` or a reset time is waited out when it is at most five minutes;
without a named wait the transport waits 60, 120 and 240 seconds with some jitter. Every value is in `RetryConfiguration`.
The default detector recognizes HTTP 429 only; a provider that signals limits otherwise passes its own `RateLimitDetector` as the
third argument of the `RetryingHttpTransport` constructor. The limits of the conditional cache are `ConditionalCacheConfiguration`.

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

Some endpoints answer 202 until a report is ready. `awaitFetch` repeats the request a fixed number of times with a fixed
pause and returns `HttpComputed.Ready`, `NotReady` or `NoData`:

```java
HttpComputed<Report> report = caller.awaitFetch(
        HttpCall.get("reports/7"), Report.class, HttpAcceptedPolling.defaults());
```

### Observe calls

An `HttpCallObserver` receives one `HttpCallEvent` for every call, after retries and redirects, for metrics, tracing or audit
logs. It runs on the calling thread, and an exception it throws is logged and dropped, so observing never changes an outcome.

## Security

See [SECURITY.md](SECURITY.md) to report a vulnerability.

## License

See [LICENSE](LICENSE).
