package org.tinylog.impl.policies;

import java.time.Clock;
import java.time.LocalTime;

/**
 * Builder for creating an instance of {@link DailyPolicy}.
 */
public class DailyPolicyBuilder extends AbstractDatePolicyBuilder {

	/** */
	public DailyPolicyBuilder() {
	}

	@Override
	public String getName() {
		return "daily";
	}

	@Override
	protected AbstractDatePolicy createPolicy(Clock clock, LocalTime time) {
		return new DailyPolicy(clock, time);
	}

}
