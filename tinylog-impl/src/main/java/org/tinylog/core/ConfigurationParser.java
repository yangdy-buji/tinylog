/*
 * Copyright 2017 Martin Winandy
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

package org.tinylog.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.tinylog.Level;
import org.tinylog.configuration.Configuration;
import org.tinylog.provider.InternalLogger;

/**
 * Parser for properties based configuration.
 *
 * @see Configuration
 */
public final class ConfigurationParser {

	private static final int MAX_LOCALE_ARGUMENTS = 3;

	/** */
	private ConfigurationParser() {
	}

	/**
	 * Loads the locale from configuration.
	 *
	 * @return Locale from configuration or {@link Locale#ROOT} if no locale is configured
	 */
	public static Locale getLocale() {
		String tag = Configuration.get("locale");
		if (tag == null) {
			return Locale.ROOT;
		} else {
			String[] splittedTag = tag.split("_", MAX_LOCALE_ARGUMENTS);
			if (splittedTag.length == 1) {
				return new Locale(splittedTag[0]);
			} else if (splittedTag.length == 2) {
				return new Locale(splittedTag[0], splittedTag[1]);
			} else {
				return new Locale(splittedTag[0], splittedTag[1], splittedTag[2]);
			}
		}
	}

	/**
	 * Loads the global severity level from configuration.
	 *
	 * @return Severity level from configuration or {@link Level#TRACE} if no severity level is configured
	 */
	public static Level getGlobalLevel() {
		return parse(Configuration.get("level"), Level.TRACE);
	}

	/**
	 * Loads custom severity levels for packages or classes from configuration.
	 *
	 * @return All found custom severity levels
	 */
	public static Map<String, Level> getCustomLevels() {
		Map<String, Level> levels = new HashMap<String, Level>();
		for (Entry<String, String> entry : Configuration.getSiblings("level@").entrySet()) {
			String packageOrClass = entry.getKey().substring("level@".length());
			Level level = parse(entry.getValue(), null);
			if (level != null) {
				levels.put(packageOrClass, level);
			}
		}
		return levels;
	}

	/**
	 * Loads all tags from writers in configuration.
	 *
	 * @return Found tags
	 */
	public static List<String> getTags() {
		List<String> tags = new ArrayList<String>();
		for (String writerProperty : Configuration.getSiblings("writer").keySet()) {
			String tag = Configuration.get(writerProperty + ".tag");
			if (tag != null && !tag.isEmpty() && !tag.equals("-") && !tags.contains(tag)) {
				tags.add(tag);
			}
		}
		return tags;
	}

	/**
	 * Reads a severity level from configuration.
	 *
	 * @param property
	 *            Key of property
	 * @param defaultValue
	 *            Default value, if property doesn't exist or is invalid
	 * @return Severity level
	 */
	private static Level parse(final String property, final Level defaultValue) {
		if (property == null) {
			return defaultValue;
		} else {
			try {
				return Level.valueOf(property.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ex) {
				InternalLogger.log(Level.ERROR, "Illegal severity level: " + property);
				return defaultValue;
			}
		}
	}

}
