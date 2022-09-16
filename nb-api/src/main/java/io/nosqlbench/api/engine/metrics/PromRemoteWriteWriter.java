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

import com.github.wnameless.json.flattener.JsonFlattener;
import org.HdrHistogram.Histogram;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xerial.snappy.Snappy;
import prometheus.Types;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PromRemoteWriteWriter {
    private final static Logger logger = LogManager.getLogger(PromRemoteWriteWriter.class);
    private String histogramMetricName;
    private String url;

    public PromRemoteWriteWriter(String histogramMetricName, String url) {
        this.histogramMetricName = histogramMetricName;
        this.url = url;
    }

    public void writeInterval(Histogram h) {
        // Is there a better way to do this with existing abstractions?
        logger.debug("prom export write interval, "+histogramMetricName);
        List<Double> percentiles  = Arrays.asList(25.00D, 50.00D, 75.00D, 90.00D, 95.00D, 98.00D, 99.00D, 99.90D);
        List<Types.TimeSeries> timeSeriesList = new ArrayList<>();
        Map<String, String> labels = Map.of();
        for (double percentile : percentiles) {
            long valueAtPercentile = h.getValueAtPercentile(percentile);
            labels.put("le", Long.toString(valueAtPercentile));
            timeSeriesList.add(createTimeSeries(histogramMetricName+"_bucket", Double.toString(h.getCountAtValue(valueAtPercentile)), labels));
        }
        labels.put("le", Long.toString(h.getMaxValue()));
        timeSeriesList.add(createTimeSeries(histogramMetricName+"_bucket", Double.toString(1), labels));

        labels.clear();
        timeSeriesList.add(createTimeSeries(histogramMetricName+"_count", Long.toString(h.getTotalCount()), labels));

        try {
            write(timeSeriesList, url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private prometheus.Types.TimeSeries.Builder timeSeriesBuilder = prometheus.Types.TimeSeries.newBuilder();
    private prometheus.Types.Sample.Builder sampleBuilder = prometheus.Types.Sample.newBuilder();

    private prometheus.Remote.WriteRequest.Builder writeRequestBuilder = prometheus.Remote.WriteRequest.newBuilder();
    private final CloseableHttpClient httpClient = HttpClients.createSystem();

    public Map<String,Object> flattenJSONAsMap(String line ){
        return JsonFlattener.flattenAsMap(line);
    }

    public Types.TimeSeries createTimeSeries(String name, String value, Map<String, String> labels){
        timeSeriesBuilder.clear();
        sampleBuilder.clear();
        Types.Label metricNameLabel = Types.Label.newBuilder().setName("__name__").setValue(name).build();
        timeSeriesBuilder.addLabels(metricNameLabel);
        for (Map.Entry<String, String> label: labels.entrySet()) {
            Types.Label appLabel = Types.Label.newBuilder().setName(label.getKey()).setValue(label.getValue()).build();
            timeSeriesBuilder.addLabels(appLabel);
        }
        sampleBuilder.setValue(Double.parseDouble(value));
        sampleBuilder.setTimestamp(System.currentTimeMillis());
        timeSeriesBuilder.addSamples(sampleBuilder.build());
        return timeSeriesBuilder.build();
    }


    public void write( List<Types.TimeSeries> timeSeriesList, String url ) throws Exception{
        try{
            writeRequestBuilder.clear();
            prometheus.Remote.WriteRequest writeRequest= writeRequestBuilder.addAllTimeseries(timeSeriesList).build();
            byte[] compressed = Snappy.compress(writeRequest.toByteArray());
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-type","application/x-www-form-urlencoded");
            httpPost.setHeader("Content-Encoding", "snappy");
            httpPost.setHeader("X-Prometheus-Remote-Write-Version", "0.1.0");

            ByteArrayEntity byteArrayEntity = new ByteArrayEntity(compressed);

            httpPost.getRequestLine();
            httpPost.setEntity(byteArrayEntity);
            httpClient.execute(httpPost);
        }catch(UnsupportedEncodingException uee){
            throw uee;
        }catch (IOException ioe){
            throw ioe;
        }catch (Exception ex) {
            throw ex;
        }
    }

}
