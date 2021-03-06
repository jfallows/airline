/**
 * Copyright (C) 2010-16 the original author or authors.
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
package com.github.rvesse.airline.maven.formats;

import java.io.File;

import com.github.rvesse.airline.help.CommandGroupUsageGenerator;
import com.github.rvesse.airline.help.CommandUsageGenerator;
import com.github.rvesse.airline.help.GlobalUsageGenerator;
import com.github.rvesse.airline.help.cli.CliCommandGroupUsageGenerator;
import com.github.rvesse.airline.help.cli.CliCommandUsageGenerator;
import com.github.rvesse.airline.help.cli.CliGlobalUsageGenerator;
import com.github.rvesse.airline.help.common.AbstractUsageGenerator;

public class CliFormatProvider implements FormatProvider {
    
    @Override
    public String getDefaultMappingName() {
        return "CLI";
    }

    @Override
    public String getExtension(FormatOptions options) {
        return ".txt";
    }

    @Override
    public CommandUsageGenerator getCommandGenerator(File outputDirectory, FormatOptions options) {
        return new CliCommandUsageGenerator(
                options.getColumns() > 0 ? options.getColumns() : AbstractUsageGenerator.DEFAULT_COLUMNS, options.includeHidden());
    }

    @Override
    public CommandGroupUsageGenerator<Object> getGroupGenerator(File outputDirectory, FormatOptions options) {
        return new CliCommandGroupUsageGenerator<>(
                options.getColumns() > 0 ? options.getColumns() : AbstractUsageGenerator.DEFAULT_COLUMNS, options.includeHidden());
    }

    @Override
    public GlobalUsageGenerator<Object> getGlobalGenerator(File outputDirectory, FormatOptions options) {
        return new CliGlobalUsageGenerator<>(
                options.getColumns() > 0 ? options.getColumns() : AbstractUsageGenerator.DEFAULT_COLUMNS, options.includeHidden());
    }

}
