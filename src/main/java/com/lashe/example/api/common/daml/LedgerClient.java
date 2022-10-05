package com.lashe.example.api.common.daml;

import com.daml.grpc.adapter.SingleThreadExecutionSequencerPool;
import com.daml.ledger.rxjava.*;
import com.daml.ledger.rxjava.grpc.*;
import com.lashe.example.api.common.daml.impl.PackageManagementClientImpl;
import com.lashe.example.api.common.daml.impl.PartyManagementClientImpl;
import com.lashe.example.api.common.daml.impl.PruningServiceClientImpl;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class LedgerClient {
    private static final String DEFAULT_POOL_NAME = "client";
    private final SingleThreadExecutionSequencerPool pool;
    private ActiveContractsClient activeContractsClient;
    private TransactionsClient transactionsClient;
    private CommandCompletionClient commandCompletionClient;
    private CommandClient commandClient;
    private CommandSubmissionClient commandSubmissionClient;
    private LedgerIdentityClient ledgerIdentityClient;
    private PackageClient packageClient;
    private LedgerConfigurationClient ledgerConfigurationClient;
    private TimeClient timeClient;

    private PackageManagementClient packageManagementClient;

    private PartyManagementClient partyManagementClient;

    private PruningServiceClient pruningServiceClient;
    private String expectedLedgerId;
    private Optional<String> accessToken;
    private final Optional<Duration> timeout;
    private final ManagedChannel channel;
    private String host;
    private int port;

    public static LedgerClient.Builder newBuilder(@NonNull String host, int port) {
        return new LedgerClient.Builder(host, port);
    }

    private LedgerClient(@NonNull NettyChannelBuilder channelBuilder, @NonNull Optional<String> expectedLedgerId,
                         @NonNull Optional<String> accessToken, @NonNull Optional<Duration> timeout,
                         @NonNull String host, int port) {
        this.pool = new SingleThreadExecutionSequencerPool(DEFAULT_POOL_NAME);
        this.channel = channelBuilder.build();
        this.expectedLedgerId = expectedLedgerId.orElse("");
        this.accessToken = accessToken;
        this.timeout = timeout;
        this.host = host;
        this.port = port;
    }

    public Mono<Boolean> isConnected() {
        return Mono.just(servicesInitialized())
                .flatMap(servicesUp -> {
                    if (Boolean.TRUE.equals(servicesUp)) {
                        return RxJava2Adapter.singleToMono(this.ledgerIdentityClient.getLedgerIdentity())
                                .map(Objects::nonNull)
                                .onErrorResume(ex -> {
                                    log.warn("Ledger connection unavailable.", ex);
                                    return Mono.just(false);
                                });
                    } else { return Mono.just(false); }
                });
    }

    private Boolean servicesInitialized() {
        List<Boolean> serviceStatuses = new ArrayList<>();
        serviceStatuses.add(ledgerIdentityClient != null);
        serviceStatuses.add(activeContractsClient != null);
        serviceStatuses.add(transactionsClient != null);
        serviceStatuses.add(commandCompletionClient != null);
        serviceStatuses.add(commandSubmissionClient != null);
        serviceStatuses.add(commandClient != null);
        serviceStatuses.add(packageClient != null);
        serviceStatuses.add(ledgerConfigurationClient != null);
        serviceStatuses.add(timeClient != null);
        serviceStatuses.add(packageManagementClient != null);
        serviceStatuses.add(partyManagementClient != null);
        serviceStatuses.add(pruningServiceClient != null);
        return serviceStatuses.stream().allMatch(serviceUp -> serviceUp);
    }

    public void connect() {
        this.ledgerIdentityClient = new LedgerIdentityClientImpl(this.channel, this.accessToken);
        String reportedLedgerId = this.ledgerIdentityClient.getLedgerIdentity().blockingGet();
        if (this.expectedLedgerId != null && !this.expectedLedgerId.equals(reportedLedgerId)) {
            throw new IllegalArgumentException(String.format("Configured ledger id [%s] is not the same as reported by the ledger [%s]", this.expectedLedgerId, reportedLedgerId));
        } else {
            this.expectedLedgerId = reportedLedgerId;
            this.activeContractsClient = new ActiveContractClientImpl(reportedLedgerId, this.channel, this.pool, this.accessToken);
            this.transactionsClient = new TransactionClientImpl(reportedLedgerId, this.channel, this.pool, this.accessToken);
            this.commandCompletionClient = new CommandCompletionClientImpl(reportedLedgerId, this.channel, this.pool, this.accessToken);
            this.commandSubmissionClient = new CommandSubmissionClientImpl(reportedLedgerId, this.channel, this.accessToken, this.timeout);
            this.commandClient = new CommandClientImpl(reportedLedgerId, this.channel, this.accessToken);
            this.packageClient = new PackageClientImpl(reportedLedgerId, this.channel, this.accessToken);
            this.ledgerConfigurationClient = new LedgerConfigurationClientImpl(reportedLedgerId, this.channel, this.pool, this.accessToken);
            this.timeClient = new TimeClientImpl(reportedLedgerId, this.channel, this.pool, this.accessToken);
            this.packageManagementClient = new PackageManagementClientImpl(this.channel, this.accessToken);
            this.partyManagementClient = new PartyManagementClientImpl(this.channel, this.accessToken);
            this.pruningServiceClient = new PruningServiceClientImpl(this.channel, this.accessToken);
        }
    }

    public String getLedgerId() {
        return this.expectedLedgerId;
    }

    public ActiveContractsClient getActiveContractSetClient() {
        return this.activeContractsClient;
    }

    public TransactionsClient getTransactionsClient() {
        return this.transactionsClient;
    }

    public CommandClient getCommandClient() {
        return this.commandClient;
    }

    public CommandCompletionClient getCommandCompletionClient() {
        return this.commandCompletionClient;
    }

    public CommandSubmissionClient getCommandSubmissionClient() {
        return this.commandSubmissionClient;
    }

    public LedgerIdentityClient getLedgerIdentityClient() {
        return this.ledgerIdentityClient;
    }

    public PackageClient getPackageClient() {
        return this.packageClient;
    }

    public LedgerConfigurationClient getLedgerConfigurationClient() {
        return this.ledgerConfigurationClient;
    }

    public TimeClient getTimeClient() {
        return this.timeClient;
    }

    public PackageManagementClient getPackageManagementClient() { return this.packageManagementClient; }

    public PartyManagementClient getPartyManagementClient() { return this.partyManagementClient; }

    public PruningServiceClient getPruningServiceClient() { return this.pruningServiceClient; }

    public String getServerAddress() { return host + ":" + port; }

    public void close() throws Exception {
        this.channel.shutdownNow();
        this.channel.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        this.pool.close();
    }

    public static final class Builder {
        private final NettyChannelBuilder nettyChannelBuilder;
        private Optional<String> expectedLedgerId;
        private Optional<String> accessToken;
        private Optional<Duration> timeout;
        private final String host;
        private final int port;

        private Builder(String host, int port) {
            this.expectedLedgerId = Optional.empty();
            this.accessToken = Optional.empty();
            this.timeout = Optional.empty();
            this.nettyChannelBuilder = NettyChannelBuilder.forAddress(host, port);
            this.host = host;
            this.port = port;
            this.nettyChannelBuilder.usePlaintext();
        }

        public LedgerClient.Builder withSslContext(@NonNull SslContext sslContext) {
            this.nettyChannelBuilder.sslContext(sslContext);
            this.nettyChannelBuilder.useTransportSecurity();
            return this;
        }

        public LedgerClient.Builder withExpectedLedgerId(@NonNull String expectedLedgerId) {
            this.expectedLedgerId = Optional.of(expectedLedgerId);
            return this;
        }

        public LedgerClient.Builder withAccessToken(@NonNull String accessToken) {
            this.accessToken = Optional.of(accessToken);
            return this;
        }

        public LedgerClient.Builder withTimeout(@NonNull Duration timeout) {
            this.timeout = Optional.of(timeout);
            return this;
        }

        public LedgerClient build() {
            return new LedgerClient(this.nettyChannelBuilder, this.expectedLedgerId, this.accessToken, this.timeout, this.host, this.port);
        }
    }
}
