package com.worldfinder.WorldFinder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SkillTotalFilter
{
	TOTAL_500("500 skill total"),
	TOTAL_750("750 skill total"),
	TOTAL_1250("1250 skill total"),
	TOTAL_1500("1500 skill total"),
	TOTAL_1750("1750 skill total"),
	TOTAL_2000("2000 skill total"),
	TOTAL_2200("2200 skill total");

	@Getter
	private final String skillTotal;
}
