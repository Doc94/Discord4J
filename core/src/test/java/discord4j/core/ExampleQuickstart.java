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

package discord4j.core;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;

public class ExampleQuickstart {

    public static void main(String[] args) {
        DiscordClient client = DiscordClientBuilder.create(System.getenv("token")).build();

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(ready -> System.out.println("Logged in as " + ready.getSelf().getUsername()));

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(MessageCreateEvent::getMessage)
                .filter(msg -> msg.getContent().map("!ping"::equals).orElse(false))
                .flatMap(Message::getChannel)
                .flatMap(channel -> channel.createMessage("Pong!"))
                .subscribe();

        client.login().block();
    }
}
