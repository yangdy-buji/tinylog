/*
 * Copyright 2019 Martin Winandy
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

package org.tinylog.format;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.util.Locale;

import org.tinylog.Level;
import org.tinylog.provider.InternalLogger;

/**
 * Advances message formatter that replaces '{}' placeholders with given arguments.
 *
 * <p>
 * Unlike {@link LegacyMessageFormatter}, choice format and decimal format compatible patterns can be used in
 * placeholders, and curly brackets can be escaped by a backslash.
 * </p>
 */
public class AdvancedMessageFormatter extends AbstractMessageFormatter {

	private static final int ADDITIONAL_STRING_BUILDER_CAPACITY = 32;

	private final DecimalFormatSymbols symbols;

	/**
	 * @param locale
	 *            Locale for formatting numbers
	 */
	public AdvancedMessageFormatter(final Locale locale) {
		symbols = new DecimalFormatSymbols(locale);
	}

	@Override
	public String format(final String message, final Object[] arguments) {
		int length = message.length();

		StringBuilder builder = new StringBuilder(length + ADDITIONAL_STRING_BUILDER_CAPACITY);
		StringBuilder buffer = new StringBuilder(length + ADDITIONAL_STRING_BUILDER_CAPACITY);
		StringBuilder current = builder;

		int argumentIndex = 0;
		int openingTickIndex = -1;
		int openingCurlyBrackets = 0;

		for (int index = 0; index < length; ++index) {
			char character = message.charAt(index);
			if (character == '\'' && index + 1 < length && openingCurlyBrackets == 0) {
				if (message.charAt(index + 1) == '\'') {
					current.append('\'');
					index += 1;
				} else {
					openingTickIndex = openingTickIndex < 0 ? index : -1;
				}
			} else if (character == '{' && index + 1 < length && argumentIndex < arguments.length && openingTickIndex < 0) {
				if (openingCurlyBrackets++ == 0) {
					current = buffer;
				} else {
					current.append(character);
				}
			} else if (character == '}' && openingCurlyBrackets > 0 && openingTickIndex < 0) {
				if (--openingCurlyBrackets == 0) {
					Object argument = resolve(arguments[argumentIndex++]);
					if (buffer.length() == 0) {
						builder.append(argument);
					} else {
						builder.append(format(buffer.toString(), argument));
						buffer.setLength(0);
					}
					buffer.setLength(0);
					current = builder;
				} else {
					current.append(character);
				}
			} else {
				current.append(character);
			}
		}

		if (buffer.length() > 0) {
			builder.append('{');
			builder.append(buffer);
		}

		if (openingTickIndex >= 0) {
			builder.insert(openingTickIndex, '\'');
		}

		return builder.toString();
	}

	/**
	 * Formats a pattern of a placeholder.
	 *
	 * @param pattern
	 *            Pattern of placeholder
	 * @param argument
	 *            Replacement for placeholder
	 * @return Formatted pattern
	 */
	private String format(final String pattern, final Object argument) {
		try {
			return getFormatter(pattern, argument).format(argument);
		} catch (IllegalArgumentException ex) {
			InternalLogger.log(Level.WARN, "Illegal argument '" + String.valueOf(argument) + "' for pattern '" + pattern + "'");
			return String.valueOf(argument);
		}
	}

	/**
	 * Gets the format object for a pattern of a placeholder. {@link ChoiceFormat} and {@link DecimalFormat} are
	 * supported.
	 *
	 * @param pattern
	 *            Pattern of placeholder
	 * @param argument
	 *            Replacement for placeholder
	 * @return Format object
	 */
	private Format getFormatter(final String pattern, final Object argument) {
		if (pattern.indexOf('|') != -1) {
			int start = pattern.indexOf('{');
			if (start >= 0 && start < pattern.lastIndexOf('}')) {
				return new ChoiceFormat(format(pattern, new Object[] { argument }));
			} else {
				return new ChoiceFormat(pattern);
			}
		} else {
			return new DecimalFormat(pattern, symbols);
		}
	}

}
