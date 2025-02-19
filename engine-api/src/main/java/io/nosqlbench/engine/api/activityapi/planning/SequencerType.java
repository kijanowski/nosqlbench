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

package io.nosqlbench.engine.api.activityapi.planning;

/**
 * Sequencer types are used to control the type of ordering used with a set of
 * operations.
 */
public enum SequencerType {

    /** Dispense all of the first element, then all of the second, and so forth. */
    concat,

    /** Dispense elements from pre-filled buckets in rotation until they are all empty. */
    bucket,

    /** Space out elements each according to their frequency over the unit interval, with
    // order of appearance taking precedence over equal timing, then take all events in
    // the order that they appear on the unit interval. */
    interval
}
