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
package discord4j.core.object.entity.channel;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.ExtendedInvite;
import discord4j.core.object.ExtendedPermissionOverwrite;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Webhook;
import discord4j.core.object.entity.poll.Poll;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.InviteCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.PollCreateMono;
import discord4j.core.spec.PollCreateSpec;
import discord4j.core.spec.WebhookCreateSpec;
import discord4j.core.spec.legacy.LegacyInviteCreateSpec;
import discord4j.core.spec.legacy.LegacyMessageCreateSpec;
import discord4j.core.spec.legacy.LegacyWebhookCreateSpec;
import discord4j.discordjson.json.BulkDeleteRequest;
import discord4j.discordjson.json.ChannelData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** An internal implementation of {@link GuildMessageChannel} designed to streamline inheritance. */
class BaseGuildMessageChannel extends BaseChannel implements GuildMessageChannel {

    /** Delegates {@link GuildChannel} operations. */
    private final BaseGuildChannel guildChannel;

    /** Delegates {@link MessageChannel} operations. */
    private final BaseMessageChannel messageChannel;

    /** Delegates {@link CategorizableChannel} operations. */
    private final BaseCategorizableChannel categorizableChannel;

    /**
     * Constructs an {@code BaseGuildMessageChannel} with an associated {@link GatewayDiscordClient} and Discord data.
     *
     * @param gateway The {@link GatewayDiscordClient} associated to this object, must be non-null.
     * @param data The raw data as represented by Discord, must be non-null.
     */
    BaseGuildMessageChannel(final GatewayDiscordClient gateway, final ChannelData data) {
        super(gateway, data);
        guildChannel = new BaseGuildChannel(gateway, data);
        messageChannel = new BaseMessageChannel(gateway, data);
        categorizableChannel = new BaseCategorizableChannel(gateway, data);
    }

    @Override
    public Snowflake getGuildId() {
        return guildChannel.getGuildId();
    }

    @Override
    public Mono<Guild> getGuild() {
        return guildChannel.getGuild();
    }

    @Override
    public Mono<Guild> getGuild(EntityRetrievalStrategy retrievalStrategy) {
        return guildChannel.getGuild(retrievalStrategy);
    }

    @Override
    public Set<ExtendedPermissionOverwrite> getPermissionOverwrites() {
        return guildChannel.getPermissionOverwrites();
    }

    @Override
    public Optional<ExtendedPermissionOverwrite> getOverwriteForMember(Snowflake memberId) {
        return guildChannel.getOverwriteForMember(memberId);
    }

    @Override
    public Optional<ExtendedPermissionOverwrite> getOverwriteForRole(Snowflake roleId) {
        return guildChannel.getOverwriteForRole(roleId);
    }

    @Override
    public Mono<PermissionSet> getEffectivePermissions(Snowflake memberId) {
        return guildChannel.getEffectivePermissions(memberId);
    }

    @Override
    public Mono<PermissionSet> getEffectivePermissions(Member member) {
        return guildChannel.getEffectivePermissions(member);
    }

    @Override
    public String getName() {
        return guildChannel.getName();
    }

    @Override
    public int getRawPosition() {
        return guildChannel.getRawPosition();
    }

    @Override
    public Mono<Integer> getPosition() {
        return guildChannel.getPosition();
    }


    @Override
    public Mono<Void> addMemberOverwrite(Snowflake memberId, PermissionOverwrite overwrite, @Nullable String reason) {
        return guildChannel.addMemberOverwrite(memberId, overwrite, reason);
    }

    @Override
    public Mono<Void> addRoleOverwrite(Snowflake roleId, PermissionOverwrite overwrite, @Nullable String reason) {
        return guildChannel.addRoleOverwrite(roleId, overwrite, reason);
    }

    @Override
    public Optional<Snowflake> getLastMessageId() {
        return messageChannel.getLastMessageId();
    }

    @Override
    public Mono<Message> getLastMessage() {
        return messageChannel.getLastMessage();
    }

    @Override
    public Mono<Message> getLastMessage(EntityRetrievalStrategy retrievalStrategy) {
        return messageChannel.getLastMessage(retrievalStrategy);
    }

    @Override
    public Optional<Instant> getLastPinTimestamp() {
        return messageChannel.getLastPinTimestamp();
    }

    @Override
    public Mono<Message> createMessage(final Consumer<? super LegacyMessageCreateSpec> spec) {
        return messageChannel.createMessage(spec);
    }

    @Override
    public Mono<Message> createMessage(MessageCreateSpec spec) {
        return messageChannel.createMessage(spec);
    }

    @Override
    public Mono<Poll> createPoll(PollCreateSpec spec) {
        return messageChannel.createPoll(spec);
    }

    @Override
    public PollCreateMono createPoll() {
        return messageChannel.createPoll();
    }

    @Override
    public Mono<Void> type() {
        return messageChannel.type();
    }

    @Override
    public Flux<Long> typeUntil(final Publisher<?> until) {
        return messageChannel.typeUntil(until);
    }

    @Override
    public Flux<Message> getMessagesBefore(final Snowflake messageId) {
        return messageChannel.getMessagesBefore(messageId);
    }

    @Override
    public Flux<Message> getMessagesAfter(final Snowflake messageId) {
        return messageChannel.getMessagesAfter(messageId);
    }

    @Override
    public Mono<Message> getMessageById(final Snowflake id) {
        return messageChannel.getMessageById(id);
    }

    @Override
    public Mono<Message> getMessageById(Snowflake id, EntityRetrievalStrategy retrievalStrategy) {
        return messageChannel.getMessageById(id, retrievalStrategy);
    }

    @Override
    public Flux<Message> getPinnedMessages() {
        return messageChannel.getPinnedMessages();
    }

    @Override
    public Optional<Snowflake> getCategoryId() {
        return categorizableChannel.getCategoryId();
    }

    @Override
    public Mono<Category> getCategory() {
        return categorizableChannel.getCategory();
    }

    @Override
    public Mono<Category> getCategory(EntityRetrievalStrategy retrievalStrategy) {
        return categorizableChannel.getCategory(retrievalStrategy);
    }

    @Override
    public Mono<ExtendedInvite> createInvite(final Consumer<? super LegacyInviteCreateSpec> spec) {
        return categorizableChannel.createInvite(spec);
    }

    @Override
    public Mono<ExtendedInvite> createInvite(InviteCreateSpec spec) {
        return categorizableChannel.createInvite(spec);
    }

    @Override
    public Flux<ExtendedInvite> getInvites() {
        return categorizableChannel.getInvites();
    }

    /**
     * Gets the channel topic, if present.
     *
     * @return The channel topic, if present.
     */
    @Override
    public Optional<String> getTopic() {
        return Possible.flatOpt(getData().topic());
    }

    @Override
    public Flux<Snowflake> bulkDelete(final Publisher<Snowflake> messageIds) {
        return getRestChannel().bulkDelete(messageIds);
    }

    @Override
    public Flux<Message> bulkDeleteMessages(final Publisher<Message> messages) {
        // FIXME This is essentially a copy of the RestChannel implementation which incurs a potentially
        //  problematic amount of duplication. Optimally, this method should be able to delegate to
        //  bulkDelete, but no implementation has been found that can do so in a performant manner.
        final Instant timeLimit = Instant.now().minus(Duration.ofDays(14L));

        return Flux.from(messages)
            .distinct(Message::getId)
            .buffer(100)
            .flatMap(allMessages -> {
                final List<Message> eligibleMessages = new ArrayList<>(0);
                final Collection<Message> ineligibleMessages = new ArrayList<>(0);

                for (final Message message : allMessages) {
                    if (message.getId().getTimestamp().isBefore(timeLimit)) {
                        ineligibleMessages.add(message);

                    } else {
                        eligibleMessages.add(message);
                    }
                }

                if (eligibleMessages.size() == 1) {
                    ineligibleMessages.add(eligibleMessages.get(0));
                    eligibleMessages.clear();
                }

                final Collection<String> eligibleIds = eligibleMessages.stream()
                    .map(Message::getId)
                    .map(Snowflake::asString)
                    .collect(Collectors.toList());

                return Mono.just(eligibleIds)
                    .filter(chunk -> !chunk.isEmpty())
                    .flatMap(chunk -> getClient().getRestClient()
                        .getChannelService()
                        .bulkDeleteMessages(getId().asLong(), BulkDeleteRequest.builder().messages(chunk).build()))
                    .thenMany(Flux.fromIterable(ineligibleMessages));
            });
    }

    @Override
    public Mono<Webhook> createWebhook(final Consumer<? super LegacyWebhookCreateSpec> spec) {
        return Mono.defer(
                () -> {
                    LegacyWebhookCreateSpec mutatedSpec = new LegacyWebhookCreateSpec();
                    spec.accept(mutatedSpec);
                    return getClient().getRestClient().getWebhookService()
                            .createWebhook(getId().asLong(), mutatedSpec.asRequest(), mutatedSpec.getReason());
                })
                .map(data -> new Webhook(getClient(), data));
    }

    @Override
    public Mono<Webhook> createWebhook(WebhookCreateSpec spec) {
        Objects.requireNonNull(spec);
        return Mono.defer(
                () -> getClient().getRestClient().getWebhookService()
                        .createWebhook(getId().asLong(), spec.asRequest(), spec.reason()))
                .map(data -> new Webhook(getClient(), data));
    }

    /**
     * Requests to retrieve the webhooks of the channel. Requires the MANAGE_WEBHOOKS permission.
     *
     * @return A {@link Flux} that continually emits the {@link Webhook webhooks} of the channel. If an error is
     * received, it is emitted through the {@code Flux}.
     */
    @Override
    public Flux<Webhook> getWebhooks() {
        return getClient().getRestClient().getWebhookService()
                .getChannelWebhooks(getId().asLong())
                .map(data -> new Webhook(getClient(), data));
    }

    /**
     * Returns all members in the guild which have access to <b>view</b> this channel.
     *
     * @return A {@link Flux} that continually emits all members from {@link Guild#getMembers()} which have access to
     * view this channel {@link discord4j.rest.util.Permission#VIEW_CHANNEL}
     */
    @Override
    public Flux<Member> getMembers() {
        return getGuild()
                .flatMapMany(Guild::getMembers)
                .filterWhen(member -> getEffectivePermissions(member.getId())
                        .map(permissions -> permissions.contains(Permission.VIEW_CHANNEL)));
    }

    @Override
    public String toString() {
        return "GuildMessageChannel{" +
                "guildChannel=" + guildChannel +
                ", messageChannel=" + messageChannel +
                "} " + super.toString();
    }
}
