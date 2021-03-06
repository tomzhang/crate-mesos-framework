/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.frameworks.mesos;

import com.beust.jcommander.ParameterException;
import io.crate.frameworks.mesos.config.Configuration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MainTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testParseConfigurationWithProtectedArg() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(
                "Argument \"-Des.cluster.name\" is protected and managed by the framework. It cannot be set by the user");
        Main.parseConfiguration(new String[]{"--crate-version", "0.47.0", "-Des.cluster.name=foo"});
    }

    @Test
    public void testVersionByVersionNumber() throws Exception {
        Configuration configuration = Main.parseConfiguration(new String[]{
                "--crate-version", "0.48.2"
        });
        assertThat(configuration.version, is("0.48.2"));
    }

    @Test
    public void testVersionByInvalidVersionNumber() throws Exception {
        expectedException.expect(ParameterException.class);
        expectedException.expectMessage("The specified Crate version \"0.48.x\" isn't a valid version or download location");
        Main.parseConfiguration(new String[]{
                "--crate-version", "0.48.x"
        });
    }

    @Test
    public void testVersionByURL() throws Exception {
        String downloadLocation = "https://cdn.crate.io/downloads/releases/nightly/crate-0.39.0-201406050908-74848fd.tar.gz";
        Configuration configuration = Main.parseConfiguration(new String[]{
                "--crate-version", downloadLocation
        });
        assertThat(configuration.version, is(downloadLocation));
        downloadLocation = "https://cdn.crate.io/downloads/releases/dev/crate-0.49.0-SNAPSHOT.tar.gz";
        configuration = Main.parseConfiguration(new String[]{
                "--crate-version", downloadLocation
        });
        assertThat(configuration.version, is(downloadLocation));
    }

    @Test
    public void testVersionByInvalidURL() throws Exception {
        expectedException.expect(ParameterException.class);
        expectedException.expectMessage("The specified Crate version \"http://cdn.crate.io/downloads/releases/download.tar.gz\" isn't a valid version or download location.");
        Main.parseConfiguration(new String[]{
                "--crate-version", "http://cdn.crate.io/downloads/releases/download.tar.gz"
        });
    }
    @Test
    public void testValidCrateArgsAreSet() throws Exception {
        Configuration configuration = Main.parseConfiguration(new String[]{
                "--crate-version", "0.47.0",
                "-Des.cluster.routing.allocation.awareness.attributes=mesos_zone"
        });
        assertThat(configuration.crateArgs().get(0),
                is("-Des.cluster.routing.allocation.awareness.attributes=mesos_zone"));
    }
}