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

activitydef = {
    "alias" : "cycle_tlrate_change",
    "driver" : "diag",
    "cycles" : "1B",
    "threads" : "2",
    "tlrate" : "1000",
    "op" : "noop"
};

print('starting cycle_rate_change with thread-local rate limiters');
print("measured total cycle increment per second is expected to adjust to 5000 then 1000");

var activity=scenario.start(activitydef);
// print('activity:'+ JSON.stringify(activity,null,2));
print('started');

var activity_params=activities[activity.getAlias()]
// print('params:'+JSON.stringify(activity_params,null,2));



var activity_metrics=metrics[activity.getAlias()]
// print('metrics:'+JSON.stringify(activity_metrics,null,2));
scenario.waitMillis(1000);
var activity_metrics=metrics[activity.getAlias()]
// print('metrics:'+JSON.stringify(activity_metrics,null,2));

var result=activity_metrics.result;

var lastcount=result.count;

for(i=0;i<20;i++) {
    if (i==2) {
        activity_params.tlrate='250';
    }
    print('tlrate now:' + activity_params.tlrate);
    scenario.waitMillis(1000);
    var nextcount=result.count;
    // print("next count: " + nextcount);
    // print("last count: " + lastcount);
    var cycles = (nextcount - lastcount);
    print("total cycles/second: " + cycles);
    // print(" waittime: " + activity_metrics.cycles.waittime.value);
    lastcount=nextcount;
    if (cycles>450 && cycles<550) {
        print("total cycles adjusted, exiting on iteration " + i);
        break;
    }
    // print('metrics:'+JSON.stringify(activity_metrics,null,2));
    // print('params:'+JSON.stringify(activity_params,null,2));



}
// scenario.stop(activitydef);
// print('cycle_rate_change activity finished');
//
//
