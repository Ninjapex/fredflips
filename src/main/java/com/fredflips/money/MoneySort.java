package com.fredflips.money;

import java.util.function.ToLongFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MoneySort
{
	REALISTIC("GP/hr (realistic)", MoneyResult::getRealisticGpPerHour),
	INSTANT("GP/hr (instant)", MoneyResult::getInstantGpPerHour),
	OFFERS("GP/hr (with offers)", MoneyResult::getOffersGpPerHour);

	private final String displayName;
	private final ToLongFunction<MoneyResult> value;

	@Override
	public String toString()
	{
		return displayName;
	}
}
