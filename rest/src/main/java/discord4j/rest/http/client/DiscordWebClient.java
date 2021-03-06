/*
 * This file is part of Discord4J.
 *
 * Discord4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discord4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J. If not, see <http://www.gnu.org/licenses/>.
 */

package discord4j.rest.http.client;

import discord4j.common.GitProperties;
import discord4j.rest.http.ExchangeStrategies;
import discord4j.rest.http.ReaderStrategy;
import discord4j.rest.http.WriterStrategy;
import discord4j.rest.json.response.ErrorResponse;
import discord4j.rest.route.Routes;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Reactor Netty based HTTP client dedicated to Discord REST API requests.
 */
public class DiscordWebClient {

    private static final Logger log = Loggers.getLogger(DiscordWebClient.class);

    public static final String REQUEST_TIMESTAMP_KEY = "requestTimestamp";

    private final HttpClient httpClient;
    private final HttpHeaders defaultHeaders;
    private final ExchangeStrategies exchangeStrategies;

    /**
     * Create a new {@link DiscordWebClient} wrapping HTTP, Discord and encoding/decoding resources.
     *
     * @param httpClient a Reactor Netty HTTP client
     * @param exchangeStrategies a strategy to transform requests and responses
     * @param token a Discord token for API authorization
     */
    public DiscordWebClient(HttpClient httpClient, ExchangeStrategies exchangeStrategies, String token) {
        final Properties properties = GitProperties.getProperties();
        final String version = properties.getProperty(GitProperties.APPLICATION_VERSION, "3");
        final String url = properties.getProperty(GitProperties.APPLICATION_URL, "https://discord4j.com");

        final HttpHeaders defaultHeaders = new DefaultHttpHeaders();
        defaultHeaders.add(HttpHeaderNames.CONTENT_TYPE, "application/json");
        defaultHeaders.add(HttpHeaderNames.AUTHORIZATION, "Bot " + token);
        defaultHeaders.add(HttpHeaderNames.USER_AGENT, "DiscordBot(" + url + ", " + version + ")");
        defaultHeaders.add("X-RateLimit-Precision", "millisecond");

        this.httpClient = httpClient;
        this.defaultHeaders = defaultHeaders;
        this.exchangeStrategies = exchangeStrategies;
    }

    /**
     * Return the underlying Reactor Netty HTTP client.
     *
     * @return the HTTP client used by this {@link DiscordWebClient}
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Return the default headers used in every request.
     *
     * @return the {@link HttpHeaders} used by this {@link DiscordWebClient} in every request
     */
    public HttpHeaders getDefaultHeaders() {
        return defaultHeaders;
    }

    /**
     * Return the strategy used for request and response conversion
     *
     * @return the {@link ExchangeStrategies} used by this {@link DiscordWebClient} in every request
     */
    public ExchangeStrategies getExchangeStrategies() {
        return exchangeStrategies;
    }

    /**
     * Exchange a request for a {@link reactor.core.publisher.Mono} response of the specified type.
     * <p>
     * The request will be processed according to the writer strategies and its response according to the reader
     * strategies available.
     *
     * @param request the method, headers and URI of the client HTTP request
     * @param body an object representing the body of the request
     * @param responseType the desired response type
     * @param responseConsumer the consumer to use while processing the response
     * @param <R> the type of the request body, can be <code>null</code>
     * @param <T> the type of the response body, can be {@link java.lang.Void}
     * @return a {@link reactor.core.publisher.Mono} of {@link T} with the response
     */
    public <R, T> Mono<T> exchange(ClientRequest request, @Nullable R body, Class<T> responseType,
                                   Consumer<HttpClientResponse> responseConsumer) {
        Objects.requireNonNull(responseType);

        HttpHeaders requestHeaders = buildHttpHeaders(request, body);
        String contentType = requestHeaders.get(HttpHeaderNames.CONTENT_TYPE);
        HttpClient.RequestSender sender = httpClient
                .baseUrl(Routes.BASE_URL)
                .observe((connection, newState) -> log.debug("{} {}", newState, connection))
                .headers(headers -> headers.setAll(requestHeaders))
                .request(request.getMethod())
                .uri(request.getUrl());
        return exchangeStrategies.writers().stream()
                .filter(s -> s.canWrite(body != null ? body.getClass() : null, contentType))
                .findFirst()
                .map(DiscordWebClient::<R>cast)
                .map(writer -> writer.write(sender, body))
                .orElseGet(() -> Mono.error(noWriterException(body, contentType)))
                .flatMap(receiver -> receiver.responseSingle((response, content) -> {
                    responseConsumer.accept(response);

                    String responseContentType = response.responseHeaders().get(HttpHeaderNames.CONTENT_TYPE);
                    Optional<ReaderStrategy<?>> readerStrategy = exchangeStrategies.readers().stream()
                            .filter(s -> s.canRead(responseType, responseContentType))
                            .findFirst();
                    int responseStatus = response.status().code();
                    if (responseStatus >= 400 && responseStatus < 600) {
                        return Mono.justOrEmpty(readerStrategy)
                                .map(DiscordWebClient::<ErrorResponse>cast)
                                .flatMap(s -> s.read(content, ErrorResponse.class))
                                .flatMap(s -> Mono.<T>error(clientException(request, response, s)))
                                .switchIfEmpty(Mono.error(clientException(request, response, null)));
                    } else {
                        return readerStrategy.map(DiscordWebClient::<T>cast)
                                .map(s -> s.read(content, responseType))
                                .orElseGet(() -> Mono.error(noReaderException(responseType, responseContentType)));
                    }
                }))
                .subscriberContext(ctx -> ctx.put(REQUEST_TIMESTAMP_KEY, Instant.now().toEpochMilli()));
    }

    private <R> HttpHeaders buildHttpHeaders(ClientRequest request, @Nullable R body) {
        HttpHeaders headers = new DefaultHttpHeaders().add(defaultHeaders).setAll(request.getHeaders());
        if (body == null) {
            headers.remove(HttpHeaderNames.CONTENT_TYPE);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    private static <T> WriterStrategy<T> cast(WriterStrategy<?> strategy) {
        return (WriterStrategy<T>) strategy;
    }

    @SuppressWarnings("unchecked")
    private static <T> ReaderStrategy<T> cast(ReaderStrategy<?> strategy) {
        return (ReaderStrategy<T>) strategy;
    }

    private static ClientException clientException(ClientRequest request, HttpClientResponse response,
                                                   @Nullable ErrorResponse errorResponse) {
        return new ClientException(request, response, errorResponse);
    }

    private static RuntimeException noWriterException(@Nullable Object body, String contentType) {
        return new RuntimeException("No strategies to write this request: " + body + " - " + contentType);
    }

    private static RuntimeException noReaderException(Object body, String contentType) {
        return new RuntimeException("No strategies to read this response: " + body + " - " + contentType);
    }
}
