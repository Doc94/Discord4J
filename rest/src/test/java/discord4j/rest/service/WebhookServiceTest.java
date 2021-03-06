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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */
package discord4j.rest.service;

import discord4j.common.jackson.Possible;
import discord4j.rest.RestTests;
import discord4j.rest.json.request.WebhookModifyRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "D4J_TEST_DISCORD", matches = "true")
public class WebhookServiceTest {

    private static final long permanentChannel = Long.parseUnsignedLong(System.getenv("permanentChannel"));
    private static final long guild = Long.parseUnsignedLong(System.getenv("guild"));
    private static final long permanentWebhook = Long.parseUnsignedLong(System.getenv("permanentWebhook"));

    private WebhookService webhookService;

    @BeforeAll
    public void setup() {
        webhookService = new WebhookService(RestTests.defaultRouter());
    }

    @Test
    public void testCreateWebhook() {
        // TODO
    }

    @Test
    public void testGetChannelWebhooks() {
        webhookService.getChannelWebhooks(permanentChannel).then().block();
    }

    @Test
    public void testGetGuildWebhooks() {
        webhookService.getGuildWebhooks(guild).then().block();
    }

    @Test
    public void testGetWebhook() {
        webhookService.getWebhook(permanentWebhook).block();
    }

    @Test
    public void testModifyWebhook() {
        WebhookModifyRequest req = new WebhookModifyRequest(Possible.of("Permanent Webhook"), Possible.absent());
        webhookService.modifyWebhook(permanentWebhook, req, null).block();
    }

    @Test
    public void testDeleteWebhook() {
        // TODO
    }
}
