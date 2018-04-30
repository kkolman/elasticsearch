/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ml.job.process.normalizer;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.ml.job.process.normalizer.output.NormalizerResultHandler;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Normalizer process that doesn't use native code.
 *
 * Instead, all scores sent for normalization are multiplied by a supplied factor.  Obviously this is useless
 * for production operation of the product, but it serves two useful purposes in development:
 * - By supplying a factor of 1.0 it can be used as a no-op when native processes are not available
 * - It can be used to produce results in testing that do not vary based on changes to the real normalization algorithms
 */
public class MultiplyingNormalizerProcess implements NormalizerProcess {
    private static final Logger LOGGER = Loggers.getLogger(MultiplyingNormalizerProcess.class);

    private final Settings settings;
    private final double factor;
    private final PipedInputStream processOutStream;
    private XContentBuilder builder;
    private boolean shouldIgnoreHeader;

    public MultiplyingNormalizerProcess(Settings settings, double factor) {
        this.settings = settings;
        this.factor = factor;
        processOutStream = new PipedInputStream();
        try {
            XContent xContent = XContentFactory.xContent(XContentType.JSON);
            PipedOutputStream processInStream = new PipedOutputStream(processOutStream);
            builder = new XContentBuilder(xContent, processInStream);
        } catch (IOException e) {
            LOGGER.error("Could not set up no-op pipe", e);
        }
        shouldIgnoreHeader = true;
    }

    @Override
    public void writeRecord(String[] record) throws IOException {
        if (shouldIgnoreHeader) {
            shouldIgnoreHeader = false;
            return;
        }
        NormalizerResult result = new NormalizerResult();
        try {
            // This isn't great as the order must match the order in Normalizer.normalize(),
            // but it's only for developers who cannot run the native processes
            result.setLevel(record[0]);
            result.setPartitionFieldName(record[1]);
            result.setPartitionFieldValue(record[2]);
            result.setPersonFieldName(record[3]);
            result.setFunctionName(record[4]);
            result.setValueFieldName(record[5]);
            result.setProbability(Double.parseDouble(record[6]));
            result.setNormalizedScore(factor * Double.parseDouble(record[7]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IOException("Unable to write to no-op normalizer", e);
        }
        // Write lineified JSON
        builder.lfAtEnd();
        result.toXContent(builder, null);
    }

    @Override
    public void close() throws IOException {
        builder.close();
    }

    @Override
    public NormalizerResultHandler createNormalizedResultsHandler() {
        return new NormalizerResultHandler(settings, processOutStream);
    }

    @Override
    public boolean isProcessAlive() {
        // Sanity check: make sure the process hasn't terminated already
        return true;
    }

    @Override
    public String readError() {
        return "";
    }
}
