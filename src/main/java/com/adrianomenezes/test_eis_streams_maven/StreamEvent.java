package com.adrianomenezes.test_eis_streams_maven;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.util.Properties;

public class StreamEvent {

    public static void main(String[] args) {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream-event");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        final String inTopic = "word_topic";
        final String outTopic = "word_result_topic";

        KStream<String,String> wordSequence = builder.stream(inTopic);

        wordSequence
                .groupByKey()
                .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
                .aggregate(() -> String.valueOf(""),
                        (k,v,x) -> x.concat(v)
                        ,Materialized.with(Serdes.String(),Serdes.String()))
                .toStream()
                .map((k,v) -> KeyValue.pair(k.key(),v.replaceAll("\"","")))
                .to(outTopic, Produced.with(Serdes.String(),Serdes.String()));


        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.cleanUp();
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    }

}

