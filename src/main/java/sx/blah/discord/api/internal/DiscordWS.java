package sx.blah.discord.api.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import sx.blah.discord.Discord4J;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.internal.json.GatewayPayload;
import sx.blah.discord.api.internal.json.requests.IdentifyRequest;
import sx.blah.discord.api.internal.json.requests.ResumeRequest;
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent;
import sx.blah.discord.handle.impl.events.LoginEvent;
import sx.blah.discord.util.LogMarkers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.InflaterInputStream;

public class DiscordWS extends WebSocketAdapter {

	private WebSocketClient wsClient;

	private DiscordClientImpl client;
	private String gateway;
	private int[] shard;

	private DispatchHandler dispatchHandler;
	private ScheduledExecutorService keepAlive = Executors.newSingleThreadScheduledExecutor();

	private static final int MAX_RECONNECT_ATTEMPTS = 5; // TODO: Expose
	private final AtomicBoolean shouldAttemptReconnect = new AtomicBoolean(false);

	protected long seq = 0;
	protected String sessionId;

	/**
	 * When the bot has received all available guilds.
	 */
	public boolean isReady = false;

	/**
	 * When the bot has received the initial Ready payload from Discord.
	 */
	public boolean hasReceivedReady = false;

	public DiscordWS(IDiscordClient client, String gateway, int[] shard, boolean isDaemon) {
		this.client = (DiscordClientImpl) client;
		this.gateway = gateway;
		this.shard = shard;
		this.dispatchHandler = new DispatchHandler(this, this.client);

		try {
			wsClient = new WebSocketClient(new SslContextFactory());
			wsClient.setDaemon(isDaemon);
			wsClient.getPolicy().setMaxBinaryMessageSize(Integer.MAX_VALUE);
			wsClient.getPolicy().setMaxTextMessageSize(Integer.MAX_VALUE);
			wsClient.start();
			wsClient.connect(this, new URI(gateway), new ClientUpgradeRequest());
		} catch (Exception e) {
			//TODO: Log exception
		}
	}

	@Override
	public void onWebSocketText(String message) {
		JsonObject payload = DiscordUtils.GSON.fromJson(message, JsonObject.class);
		GatewayOps op = GatewayOps.values()[payload.get("op").getAsInt()];
		JsonElement d = payload.has("d") && !(payload.get("d") instanceof JsonNull) ? payload.get("d") : null;

		switch (op) {
			case HELLO:
				shouldAttemptReconnect.set(false);
				beginHeartbeat(d.getAsJsonObject().get("heartbeat_interval").getAsInt());
				send(new GatewayPayload(GatewayOps.IDENTIFY, new IdentifyRequest(client.token, shard)));
				break;
			case RECONNECT:
				try {
					disconnect(DiscordDisconnectedEvent.Reason.RECONNECT_OP);
					wsClient.connect(this, new URI(this.gateway), new ClientUpgradeRequest());
				} catch (IOException | URISyntaxException e) {
					Discord4J.LOGGER.error(LogMarkers.WEBSOCKET, "Encountered error while handling RECONNECT op, REPORT THIS TO THE DISCORD4J DEV! {}", e);
				}
				break;
			case DISPATCH: dispatchHandler.handle(payload); break;
			case INVALID_SESSION: disconnect(DiscordDisconnectedEvent.Reason.INVALID_SESSION_OP); break;
			case HEARTBEAT_ACK: /* TODO: Handle missed pings */ break;

			default:
				Discord4J.LOGGER.warn(LogMarkers.WEBSOCKET, "Unhandled opcode received: {} (ignoring), REPORT THIS TO THE DISCORD4J DEV!", op);
		}
	}

	@Override
	public void onWebSocketConnect(Session sess) {
		super.onWebSocketConnect(sess);
		System.out.println("Connected!");

		if (shouldAttemptReconnect.get()) {
			send(GatewayOps.RESUME, new ResumeRequest(sessionId, seq, client.getToken()));
		}
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode, reason);
		System.out.println("closed with statuscode " + statusCode + " and reason " + reason);

		if (statusCode == 1006 || statusCode == 1001) { // Which status codes represent errors? All but 1000?
			disconnect(DiscordDisconnectedEvent.Reason.ABNORMAL_CLOSE);
		}
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		cause.printStackTrace();
	}

	public void send(GatewayOps op, Object payload) {
		send(new GatewayPayload(op, payload));
	}

	public void send(GatewayPayload payload) {
		send(DiscordUtils.GSON.toJson(payload));
	}

	public void send(String message) {
		if (getSession().isOpen()) {
			getSession().getRemote().sendStringByFuture(message);
		} else {
			System.out.println("Attempt to send message on closed session. This should never happen"); // somehow invalid state?
		}
	}

	protected void beginHeartbeat(long interval) {
		if (keepAlive.isShutdown()) keepAlive = Executors.newSingleThreadScheduledExecutor();

		keepAlive.scheduleAtFixedRate(() -> {
			send(GatewayOps.HEARTBEAT, seq);
		}, 0, interval, TimeUnit.MILLISECONDS);
	}


	// TODO: Access modifier
	public void disconnect(DiscordDisconnectedEvent.Reason reason) {
		Discord4J.LOGGER.debug(LogMarkers.WEBSOCKET, "Disconnected with reason {}", reason);

		keepAlive.shutdown();
		switch (reason) {
			case ABNORMAL_CLOSE:
				beginReconnect();
				break;
			case LOGGED_OUT:
				clearCaches();
				getSession().close();
				break;
			default:
				System.out.println("Unhandled reason " + reason);
		}
	}

	private void beginReconnect() {
		System.out.println("beginning reconnect");

		hasReceivedReady = false;
		isReady = false;
		shouldAttemptReconnect.set(true);
		int curAttempt = 0;

		while (curAttempt < MAX_RECONNECT_ATTEMPTS && shouldAttemptReconnect.get()) {
			System.out.println("current attempt: " + curAttempt);
			try {
				wsClient.connect(this, new URI(gateway), new ClientUpgradeRequest());

				int timeout = (int) Math.min(1024, Math.pow(2, curAttempt)) + ThreadLocalRandom.current().nextInt(-2, 2);
				client.getDispatcher().waitFor((LoginEvent event) -> {
					System.out.println("received login event in reconnect waitFor");
					return true;
				}, timeout, TimeUnit.SECONDS, () -> {
					System.out.println("reconnect attempt timed out.");
				});

			} catch (IOException | URISyntaxException | InterruptedException e) {
				e.printStackTrace();
			}

			curAttempt++;
		}

		System.out.println("Reconnection failed after " + MAX_RECONNECT_ATTEMPTS + " attempts.");
	}

	private void clearCaches() {
		client.guildList.clear();
		client.privateChannels.clear();
		client.ourUser = null;
		client.REGIONS.clear();
		seq = 0;
		sessionId = null;
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new ByteArrayInputStream(payload))));
		String message = reader.lines().collect(Collectors.joining());
		this.onWebSocketText(message);
	}
}
