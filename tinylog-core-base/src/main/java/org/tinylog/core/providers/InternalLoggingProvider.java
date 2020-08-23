/*
 * Copyright 2020 Martin Winandy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.tinylog.core.providers;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.tinylog.core.Level;
import org.tinylog.core.format.message.MessageFormatter;
import org.tinylog.core.runtime.StackTraceLocation;

/**
 * Internal logging provider that prints internal tinylog errors and warnings to {@link System#err}.
 */
public final class InternalLoggingProvider implements LoggingProvider {

	private static final String TAG = "tinylog";

	/** */
	public InternalLoggingProvider() {
	}

	@Override
	public void log(StackTraceLocation location, String tag, Level level, Throwable throwable, Object message,
			Object[] arguments, MessageFormatter formatter) {
		if (TAG.equals(tag) && level.ordinal() <= Level.WARN.ordinal()) {
			StringBuilder builder = new StringBuilder();

			builder.append("TINYLOG ");
			builder.append(level);
			builder.append(": ");

			if (message != null) {
				if (formatter == null || arguments == null) {
					builder.append(message);
				} else {
					builder.append(formatter.format(message.toString(), arguments));
				}
			}

			if (throwable != null) {
				if (message != null) {
					builder.append(": ");
				}

				StringWriter writer = new StringWriter();
				throwable.printStackTrace(new PrintWriter(writer));
				builder.append(writer);
			} else {
				builder.append(System.lineSeparator());
			}

			System.err.print(builder);
		}
	}

}