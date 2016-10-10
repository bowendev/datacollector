/*
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.config.upgrade;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.streamsets.pipeline.api.Config;
import com.streamsets.pipeline.config.AvroSchemaLookupMode;
import com.streamsets.pipeline.config.DestinationAvroSchemaSource;
import com.streamsets.pipeline.config.OriginAvroSchemaSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting common data format upgrade code
 * that can be used in individual stage upgraders until ConfigBean upgrading
 * is possible.
 */
public class DataFormatUpgradeHelper {
  private static final Joiner PERIOD = Joiner.on(".");

  private DataFormatUpgradeHelper() {
  }

  public static void upgradeAvroParserWithSchemaRegistrySupport(List<Config> configs) {
    List<Config> toRemove = new ArrayList<>();
    List<Config> toAdd = new ArrayList<>();

    Optional<Config> avroSchema = findByName(configs, "avroSchema");
    if (!avroSchema.isPresent()) {
      throw new IllegalStateException("Config 'avroSchema' is missing, this upgrader cannot be applied.");
    }

    String configName = avroSchema.get().getName();
    String prefix = configName.substring(0, configName.lastIndexOf("."));

    // schemaInMessage was removed and superseded by OriginAvroSchemaSource.SOURCE
    Optional<Config> schemaInMessage = findByName(configs, "schemaInMessage");

    // This upgrader intentionally behaves differently then plain new stage on the canvas. On new stage user is forced
    // to chose where is the Avro schema as this decision is no longer "simple". However for people who are upgrading
    // we're making the same decision that they selected in the past.
    if (schemaInMessage.isPresent()) {
      if ((boolean) schemaInMessage.get().<Boolean>getValue()) {
        toAdd.add(new Config(PERIOD.join(prefix, "avroSchemaSource"), OriginAvroSchemaSource.SOURCE));
      } else {
        toAdd.add(new Config(PERIOD.join(prefix, "avroSchemaSource"), OriginAvroSchemaSource.INLINE));
      }
      toRemove.add(schemaInMessage.get());
    } else {
      toAdd.add(new Config(PERIOD.join(prefix, "avroSchemaSource"), OriginAvroSchemaSource.SOURCE));
    }

    // New configs added
    toAdd.add(new Config(PERIOD.join(prefix, "schemaRegistryUrls"), new ArrayList<>()));
    toAdd.add(new Config(PERIOD.join(prefix, "schemaLookupMode"), AvroSchemaLookupMode.AUTO));
    toAdd.add(new Config(PERIOD.join(prefix, "subject"), ""));
    toAdd.add(new Config(PERIOD.join(prefix, "schemaId"), 0));

    configs.removeAll(toRemove);
    configs.addAll(toAdd);
  }

  public static void upgradeAvroGeneratorWithSchemaRegistrySupport(List<Config> configs) {
    List<Config> toRemove = new ArrayList<>();
    List<Config> toAdd = new ArrayList<>();

    Optional<Config> avroSchema = findByName(configs, "avroSchema");

    if (!avroSchema.isPresent()) {
      throw new IllegalStateException("Config 'avroSchema' is missing, this upgrader cannot be applied.");
    }

    String configName = avroSchema.get().getName();
    String prefix = configName.substring(0, configName.lastIndexOf("."));

    // avroSchemaInHeader was removed and superseded by DestinationAvroSchemaSource.HEADER
    Optional<Config> avroSchemaInHeader = findByName(configs, "avroSchemaInHeader");
    if (avroSchemaInHeader.isPresent()) {
      if ((boolean) avroSchemaInHeader.get().getValue()) {
        toAdd.add(new Config(PERIOD.join(prefix, "avroSchemaSource"), DestinationAvroSchemaSource.HEADER));
      } else {
        toAdd.add(new Config(PERIOD.join(prefix, "avroSchemaSource"), DestinationAvroSchemaSource.INLINE));
      }
      toRemove.add(avroSchemaInHeader.get());
    } else {
      toAdd.add(new Config(PERIOD.join(prefix, "avroSchemaSource"), DestinationAvroSchemaSource.INLINE));
    }

    toAdd.add(new Config(PERIOD.join(prefix, "registerSchema"), false));
    toAdd.add(new Config(PERIOD.join(prefix, "schemaRegistryUrlsForRegistration"), new ArrayList<>()));
    toAdd.add(new Config(PERIOD.join(prefix, "schemaRegistryUrls"), new ArrayList<>()));
    toAdd.add(new Config(PERIOD.join(prefix, "schemaLookupMode"), AvroSchemaLookupMode.AUTO));
    toAdd.add(new Config(PERIOD.join(prefix, "subject"), ""));
    toAdd.add(new Config(PERIOD.join(prefix, "subjectToRegister"), ""));
    toAdd.add(new Config(PERIOD.join(prefix, "schemaId"), 0));

    configs.removeAll(toRemove);
    configs.addAll(toAdd);
  }

  static Optional<Config> findByName(List<Config> configs, String name) {
    for (Config config : configs) {
      if (config.getName().endsWith(name)) {
        return Optional.of(config);
      }
    }
    return Optional.absent();
  }
}
