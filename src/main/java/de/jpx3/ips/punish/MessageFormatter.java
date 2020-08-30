package de.jpx3.ips.punish;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class MessageFormatter {
  public static String formatMessage(
    String layout, BanEntry banEntry
  ) {
    Preconditions.checkNotNull(layout);

    Map<String, String> replacementMapping =
      banEntry == null ? ImmutableMap.of() : prepareMapping(banEntry);

    String formattedMessage = replaceKeys(layout, replacementMapping);
    return translateColorCodes(formattedMessage);
  }

  private static Map<String, String> prepareMapping(BanEntry entry) {
    Map<String, String> replacements =
      Maps.newHashMap();

    String entryReason = entry.reason();
    long entryDurationInMilliSeconds =
      entry.ending() - System.currentTimeMillis();
    long entryDurationInSeconds =
      TimeUnit.MILLISECONDS.toSeconds(entryDurationInMilliSeconds);

    String formattedEntryDuration = formatDurationFrom(
      TimeUnitTypeNameResolver.FULL,
      ", ",
      entryDurationInSeconds
    );

    String formattedEntryDurationShort = formatDurationFrom(
      TimeUnitTypeNameResolver.SHORTED,
      " ",
      entryDurationInSeconds
    );

    replacements.put("reason", entryReason);
    replacements.put("expire", formattedEntryDuration);
    replacements.put("expire-short", formattedEntryDurationShort);

    return replacements;
  }

  private final static String REGEX_FORMATTED_OPENING_CURLY_BRACE = "\\{";
  private final static String REGEX_FORMATTED_CLOSING_CURLY_BRACE = "}";

  private static String replaceKeys(
    String layout, Map<String, String> keyToReplacement
  ) {
    for (
      Map.Entry<String, String> keyToReplacementEntry :
      keyToReplacement.entrySet()
    ) {
      String key = keyToReplacementEntry.getKey();
      String replacement = keyToReplacementEntry.getValue();

      String keyWrappedInCurlyBraces =
        REGEX_FORMATTED_OPENING_CURLY_BRACE +
        key +
        REGEX_FORMATTED_CLOSING_CURLY_BRACE;

      layout = layout.replaceAll(keyWrappedInCurlyBraces, replacement);
    }

    return layout;
  }

  private static String translateColorCodes(String input) {
    return ChatColor.translateAlternateColorCodes('&', input);
  }

  private final static List<TimeUnit> WEIGHT_ORDERED_TIME_UNITS =
    Lists.newLinkedList();
  private final static long SECONDS_IN_A_HUNDRED_YEARS =
    TimeUnit.DAYS.toSeconds(365 * 100);
  private final static String WILL_NOT_EXPIRE_EXPRESSION =
    "Never";

  private static String formatDurationFrom(
    TimeUnitTypeNameResolver nameResolver,
    String spliterator, long timeInSeconds
  ) {
    StringBuilder resultBuilder = new StringBuilder();

    if(timeInSeconds > SECONDS_IN_A_HUNDRED_YEARS) {
      resultBuilder.append(WILL_NOT_EXPIRE_EXPRESSION);
      return resultBuilder.toString();
    }

    for (TimeUnit timeUnit : WEIGHT_ORDERED_TIME_UNITS) {
      String timeUnitName = unitNameOf(timeUnit, nameResolver);
      long unitInSeconds = secondsOf(timeUnit);
      long fittingUnitAmount = timeInSeconds / unitInSeconds;

      fittingUnitAmount = Math.min(99, fittingUnitAmount);

      if(fittingUnitAmount > 1) {
        resultBuilder
          .append(String.format("%02d", fittingUnitAmount))
          .append(timeUnitName);
        timeInSeconds -= fittingUnitAmount * unitInSeconds;

        if(timeInSeconds >= 1) {
          resultBuilder.append(spliterator);
        }
      }
    }

    return resultBuilder.toString();
  }

  private static String unitNameOf(
    TimeUnit timeUnit,
    TimeUnitTypeNameResolver nameResolver
  ) {
    return nameResolver.nameBy(timeUnit);
  }

  private static boolean timeUnitInvalid(TimeUnit unit) {
    return secondsOf(unit) < 1;
  }

  private static long secondsOf(TimeUnit unit) {
    return unit.toSeconds(1);
  }

  static {
    List<TimeUnit> timeUnits = Lists.newArrayList(Arrays.asList(TimeUnit.values()));
    timeUnits.removeIf(MessageFormatter::timeUnitInvalid);
    timeUnits.sort(
      Comparator
        .comparing(MessageFormatter::secondsOf)
        .reversed()
    );
    WEIGHT_ORDERED_TIME_UNITS.addAll(timeUnits);
  }

  public enum TimeUnitTypeNameResolver {
    FULL(timeUnit ->
      " " + timeUnit.name().toLowerCase()),
    SHORTED(timeUnit ->
      String.valueOf(timeUnit.name().charAt(0)).toLowerCase());

    private Function<TimeUnit, String> mapper;

    TimeUnitTypeNameResolver(Function<TimeUnit, String> mapper) {
      this.mapper = mapper;
    }

    private String nameBy(TimeUnit unit) {
      return mapper.apply(unit);
    }
  }
}
