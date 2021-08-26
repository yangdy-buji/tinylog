package org.tinylog.impl.backend;

import java.util.Locale;

import org.tinylog.core.Configuration;
import org.tinylog.core.Framework;
import org.tinylog.core.internal.InternalLogger;
import org.tinylog.core.internal.SafeServiceLoader;
import org.tinylog.impl.writer.Writer;
import org.tinylog.impl.writer.WriterBuilder;

/**
 * Parser and creator for configured writers.
 */
class WriterConfiguration {

	private static final String TYPE_KEY = "type";

	private final Framework framework;
	private final Configuration entireConfiguration;
	private final LevelConfiguration levelConfiguration;

	/**
	 * @param framework The actual logging framework instance
	 * @param configuration The writer configuration to parse
	 */
	WriterConfiguration(Framework framework, Configuration configuration) {
		this.framework = framework;
		this.entireConfiguration = configuration;
		this.levelConfiguration = new LevelConfiguration(configuration.getList(LevelConfiguration.KEY));
	}

	/**
	 * Gets the level configuration of the parsed writer.
	 *
	 * @return The level configuration with activated severity levels and tags
	 */
	public LevelConfiguration getLevelConfiguration() {
		return levelConfiguration;
	}

	/**
	 * Creates the writer.
	 *
	 * @return The created writer or {@code null} if the creation failed
	 */
	public Writer createWriter() {
		String type = entireConfiguration.getValue(TYPE_KEY);
		if (type == null) {
			InternalLogger.error(
				null,
				"Missing writer name in property \"{}\"",
				entireConfiguration.resolveFullKey(TYPE_KEY)
			);
			return null;
		} else {
			String name = type.toLowerCase(Locale.ENGLISH);
			WriterBuilder builder = SafeServiceLoader
				.asList(framework, WriterBuilder.class, "writer builder")
				.stream()
				.filter(writerBuilder -> name.equals(writerBuilder.getName()))
				.findAny()
				.orElse(null);

			if (builder == null) {
				InternalLogger.error(
					null,
					"Could not find any writer builder with the name \"{}\" in the classpath",
					name
				);
				return null;
			} else {
				try {
					return builder.create(framework, entireConfiguration);
				} catch (Exception ex) {
					InternalLogger.error(ex, "Failed to create writer for \"{}\"", name);
					return null;
				}
			}
		}
	}

}
