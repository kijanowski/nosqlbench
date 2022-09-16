/*
 * Copyright (c) 2022 nosqlbench
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nosqlbench.api.engine.metrics;

import org.HdrHistogram.EncodableHistogram;
import org.HdrHistogram.Histogram;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * HistoIntervalLogger runs a separate thread to snapshotAndWrite encoded histograms on a regular interval.
 * It listens to the metrics registry for any new metrics that match the pattern. Any metrics
 * which both match the pattern and which are {@link EncodableHistogram}s are written the configured
 * logfile at the configured interval.
 */
public class HistoStatsPrometheusAttachment extends CapabilityHook<HdrDeltaHistogramAttachment>
        implements Runnable, MetricsCloseable  {
    private final static Logger logger = LogManager.getLogger(HistoStatsPrometheusAttachment.class);

    private final String name;
    private final String url;
    private final long intervalLength;
    private PromRemoteWriteWriter writer;
    private final Pattern pattern;

    private final List<WriterTarget> targets = new CopyOnWriteArrayList<>();
    private PeriodicRunnable<HistoStatsPrometheusAttachment> executor;
    private long lastRunTime=0L;

    public HistoStatsPrometheusAttachment(String name, String url, Pattern pattern, long intervalLength) {
        this.name = name;
        this.pattern = pattern;
        this.intervalLength = intervalLength;
        this.url = url;
        startLogging();
    }

    public boolean matches(String metricName) {
        return pattern.matcher(metricName).matches();
    }

    /**
     * By convention, it is typical for the logging application
     * to use a comment to indicate the logging application at the head
     * of the log, followed by the log format version, a startLogging time,
     * and a legend (in that order).
     */
    public void startLogging() {
        writer = new PromRemoteWriteWriter(name, url);
        long currentTimeMillis = System.currentTimeMillis();

        this.executor = new PeriodicRunnable<HistoStatsPrometheusAttachment>(this.getInterval(), this);
        executor.startDaemonThread();
    }

    public String toString() {
        return "PromExport:" + this.pattern + ":" + this.intervalLength;
    }

    public long getInterval() {
        return intervalLength;
    }

    @Override
    public void onCapableAdded(String name, HdrDeltaHistogramAttachment chainedHistogram) {
        if (pattern.matcher(name).matches()) {
            this.targets.add(new WriterTarget(name, chainedHistogram.attachHdrDeltaHistogram()));
        }
    }

    @Override
    public void onCapableRemoved(String name, HdrDeltaHistogramAttachment capable) {
        this.targets.remove(new WriterTarget(name,null));
    }

    @Override
    protected Class<HdrDeltaHistogramAttachment> getCapabilityClass() {
        return HdrDeltaHistogramAttachment.class;
    }

    @Override
    public void run() {
        for (WriterTarget target : this.targets) {
            Histogram nextHdrHistogram = target.histoProvider.getNextHdrDeltaHistogram();
            writer.writeInterval(nextHdrHistogram);
        }
        this.lastRunTime = System.currentTimeMillis();
    }

    @Override
    public void closeMetrics() {
        long now = System.currentTimeMillis();
        if (lastRunTime+1000<now) {
            logger.debug("Writing last partial interval: " + this);
            run();
        } else {
            logger.debug("Not writing last partial interval <1s: " + this);
        }
    }

    @Override
    public void chart() {
       // nothing-to-do we only chart HistoIntervals not HistoStats
    }

    private static class WriterTarget implements Comparable<WriterTarget> {

        public String name;
        public HdrDeltaHistogramProvider histoProvider;

        public WriterTarget(String name, HdrDeltaHistogramProvider attach) {
            this.name = name;
            this.histoProvider = attach;
        }

        @Override
        public boolean equals(Object obj) {
            return name.equals(((WriterTarget)obj).name);
        }

        @Override
        public int compareTo(WriterTarget obj) {
            return name.compareTo(obj.name);
        }
    }
}
