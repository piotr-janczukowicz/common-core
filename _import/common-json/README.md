[![Codacy Badge](https://app.codacy.com/project/badge/Grade/3509395ba69245cea505519869c78870)](https://app.codacy.com/gh/piotr-janczukowicz/common-json/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Coverage](https://app.codacy.com/project/badge/Coverage/3509395ba69245cea505519869c78870)](https://app.codacy.com/gh/piotr-janczukowicz/common-json/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Build](https://github.com/piotr-janczukowicz/common-json/actions/workflows/main.yml/badge.svg)](https://github.com/piotr-janczukowicz/common-json/actions/workflows/main.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.piotr-janczukowicz/common-json)](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/common-json)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
# common-json

JSON building blocks for the `io.github.piotr-janczukowicz` libraries and any Java 21 project that uses Jackson 3: a
Jackson-free `JsonCodec` contract, an `OptionalNull` wrapper for PATCH-style payloads, Jackson adapters for union-shaped
JSON, a module that reads collections as immutable ones, and a serializer that writes `5.0` as `5`.

**Requirements:** Java 21 and Jackson 3 (`tools.jackson`). The library is a Java module, `common.json`, and also works on the
class path. It is published on
[Maven Central](https://central.sonatype.com/artifact/io.github.piotr-janczukowicz/common-json):

```xml
<dependency>
  <groupId>io.github.piotr-janczukowicz</groupId>
  <artifactId>common-json</artifactId>
  <version>LATEST_VERSION</version>
</dependency>
```

Replace `LATEST_VERSION` with the version in the Maven Central badge at the top of this page, or with the newest entry on
the [releases page](https://github.com/piotr-janczukowicz/common-json/releases).

## Usage

The `io.github.piotr_janczukowicz.json` package has no Jackson types: `JsonCodec` (decode and encode bytes), `JsonType` and `JsonTypes` (generic target types), `OptionalNull`, and the `JsonDecodeException` / `JsonEncodeException` failures. Implement `JsonCodec` over your own mapper; the contract is in its Javadoc. The adapters live in `io.github.piotr_janczukowicz.json.jackson` and target Jackson 3 (`tools.jackson`). All of them register on a `SimpleModule`.

### OptionalNull

`OptionalNull<T>` tells apart an absent field (Java `null`), an explicit JSON `null` (`OptionalNull.ofNull()`) and a value (`OptionalNull.of(value)`). Register both parts, and put `@JsonInclude(NON_NULL)` on the enclosing record so an absent field is omitted:

```java
SimpleModule module = new SimpleModule();
module.addDeserializer(OptionalNull.class, new OptionalNullDeserializer());
module.setMixInAnnotation(OptionalNull.class, OptionalNullMixin.class);
```

A value that the inner deserializer reads as `null` (for example an unknown enum constant under `READ_UNKNOWN_ENUM_VALUES_AS_NULL`) fails instead of becoming `OptionalNull.ofNull()`, so a PATCH cannot clear a field by accident.

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

Every `register` method rejects a `null` argument when `register` is called. A union value that holds neither of its forms fails to serialize instead of being written as `null`.

### Immutable collections

`ImmutableCollectionsModule` makes `List`, `Collection`, `Set` and `Map` deserialize as immutable collections, keeping element types. JSON `null` and absent properties stay `null`. Null elements and map entries with a null value are dropped, and the iteration order of sets and maps is unspecified.

```java
JsonMapper mapper = JsonMapper.builder().addModule(new ImmutableCollectionsModule()).build();
```

### Integral doubles

`IntegralDoubleSerializer` writes a `Double` without a fractional part as a JSON integer (`5.0` becomes `5`) and every other
value, including `NaN`, infinities, negative zero and values beyond the `long` range, as an ordinary number. Register it with
`module.addSerializer(Double.class, new IntegralDoubleSerializer())`.

### Mapper expectations

Do not enable default typing on a mapper that reads untrusted JSON. Keep Jackson's `StreamReadConstraints` at their defaults or tighter: they bound the size of numbers, and `ScalarUnionReaders` additionally rejects a number with an exponent beyond 1000 decimal places.

## Security

See [SECURITY.md](SECURITY.md) to report a vulnerability.

## License

See [LICENSE](LICENSE).
