# Discord4J Rest
The `rest` module provides a low-level HTTP client specifically for Discord which properly handles Discord's [ratelimiting system](https://discordapp.com/developers/docs/topics/rate-limits).

## Installation
Just replace `@VERSION@` with the latest given by ![](https://img.shields.io/maven-central/v/com.discord4j/discord4j-rest.svg?style=flat-square)
### Gradle
```groovy
repositories {
  mavenCentral()
}

dependencies {
  implementation 'com.discord4j:discord4j-rest:@VERSION@'
}
```
### Maven
```xml
<dependencies>
  <dependency>
    <groupId>com.discord4j</groupId>
    <artifactId>discord4j-rest</artifactId>
    <version>@VERSION@</version>
  </dependency>
</dependencies>
```

### SBT
```scala
libraryDependencies ++= Seq(
  "com.discord4j" % "discord4j-rest" % "@VERSION@"
)
```

## Development builds
Please follow our instructions at [Using Jitpack](https://github.com/Discord4J/Discord4J/wiki/Using-Jitpack)

## Example Usage
```java
ObjectMapper mapper = new ObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .addHandler(new UnknownPropertyHandler(true))
        .registerModules(new PossibleModule(), new Jdk8Module());

DiscordWebClient webClient = new DiscordWebClient(HttpClient.create().compress(true),
        ExchangeStrategies.jackson(mapper), token);

Router router = new DefaultRouter(webClient, Schedulers.elastic(), Schedulers.elastic());

RestClient restClient = new RestClient(router);

restClient.getApplicationService().getCurrentApplicationInfo()
        .map(ApplicationInfoResponse::getName)
        .subscribe(name -> System.out.println("My name is " + name));
```
