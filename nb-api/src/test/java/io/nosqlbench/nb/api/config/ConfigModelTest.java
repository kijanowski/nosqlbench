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

package io.nosqlbench.nb.api.config;

import io.nosqlbench.api.config.standard.ConfigModel;
import io.nosqlbench.api.config.standard.NBConfiguration;
import io.nosqlbench.api.config.standard.Param;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigModelTest {

    @Test
    public void testMultipleParams() {
        ConfigModel cm = ConfigModel.of(ConfigModelTest.class,
            Param.defaultTo(List.of("a","b"),"value").setRequired(false),
            Param.required("c",int.class));
        NBConfiguration cfg = cm.apply(Map.of("c", 232));
        assertThat(cfg.getOptional("a")).isEmpty();
        assertThat(cfg.get("c",int.class)).isEqualTo(232);

    }

    @Test
    public void testMutuallyExclusiveVerifier() {
        ConfigModel cm = ConfigModel.of(ConfigModelTest.class,
            Param.defaultTo(List.of("a","b"),"a_or_b")
                .notWith("c"),
            Param.defaultTo(List.of("c","d"),"c_or_d")
        );
        Assertions.assertDoesNotThrow(() -> Map.of("a","ayy"));

        Map<String,String> invalid1 = Map.of("a","ayy","c","see");
        RuntimeException error1 = Assertions.assertThrows(RuntimeException.class, () -> cm.apply(invalid1));
        assertThat(error1.toString()).contains("Both 'a' and 'c' were specified.");

        Map<String,String> invalid2 = Map.of("a","ayy","d","dee");
        RuntimeException error2 = Assertions.assertThrows(RuntimeException.class, () -> cm.apply(invalid2));
        assertThat(error2.toString()).contains("Both 'a' and 'd' were specified.");

        Map<String,String> invalid3 = Map.of("b","bee","c","see");
        RuntimeException error3 = Assertions.assertThrows(RuntimeException.class, () -> cm.apply(invalid3));
        assertThat(error3.toString()).contains("Both 'b' and 'c' were specified.");

        Map<String,String> invalid4 = Map.of("b","bee","d","dee");
        RuntimeException error4 = Assertions.assertThrows(RuntimeException.class, () -> cm.apply(invalid4));
        assertThat(error4.toString()).contains("Both 'b' and 'd' were specified.");

    }
}
