// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    long durationMinutes = request.getDuration();
    Collection<String> attendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    ArrayList<TimeRange> mandatoryTimes = new ArrayList<>();
    ArrayList<TimeRange> optionalTimes = new ArrayList<>();

    if (durationMinutes > 24 * 60) {
      return mandatoryTimes;
    }
    mandatoryTimes.add(TimeRange.WHOLE_DAY);
    optionalTimes.add(TimeRange.WHOLE_DAY);
    if ((attendees.isEmpty() && optionalAttendees.isEmpty()) || events.isEmpty()) {
      return mandatoryTimes;
    }

    // For each event, if there are no event attendees that are in the request, skip it.
    // If the event has any regular attendees, process the event in mandatoryTimes, otherwise
    // the event only has optional attendees, so it will be processed in optionalTimes
    for (Event event : events) {
      Set<String> eventAttendees = event.getAttendees();
      boolean mandatoriesAttending = attendingEvent(eventAttendees, attendees);
      boolean optionals = false;
      if (mandatoriesAttending == false) {
        optionals = attendingEvent(eventAttendees, optionalAttendees);
        if (optionals == false) {
          continue;
        }
      }

      checkEventCompatibility(optionals ? optionalTimes : mandatoryTimes, event.getWhen(),
                              durationMinutes);
    }

    // if TimeRanges from optionalTimes and mandatoryTimes overlap, and if there is room
    // for another TimeRange within the overlaping time, add the TimeRange to
    // availableTimes
    ArrayList<TimeRange> availableTimes = new ArrayList<>();
    for (TimeRange optionalTime : optionalTimes) {
      for (TimeRange mandatoryTime : mandatoryTimes) {
        if (!optionalTime.overlaps(mandatoryTime)) {
          continue;
        }
        int start = Math.max(optionalTime.start(), mandatoryTime.start());
        int end = Math.min(optionalTime.end(), mandatoryTime.end());
        if (end == TimeRange.END_OF_DAY && end - start >= durationMinutes - 1) {
          availableTimes.add(TimeRange.fromStartEnd(start, end, true));
        } else if (end - start >= durationMinutes) {
          availableTimes.add(TimeRange.fromStartEnd(start, end, false));
        }
      }
    }

    if (availableTimes.isEmpty()) {
      if (optionalTimes.isEmpty() && attendees.isEmpty()) {
        return optionalTimes;
      }
      return mandatoryTimes;
    } else {
      return availableTimes;
    }
  }

  // if any TimeRanges in timeRanges overlap with the eventTimeRange, remove the current
  // TimeRange and add another if there is enough room before or after the eventTimeRange
  private void checkEventCompatibility(
      ArrayList<TimeRange> timeRanges, TimeRange eventTimeRange, long durationMinutes) {
    int i = 0;
    while (i < timeRanges.size()) {
      TimeRange timeRange = timeRanges.get(i);
      if (timeRange.overlaps(eventTimeRange)) {
        if (timeRange.start() + durationMinutes <= eventTimeRange.start()) {
          timeRanges.add(
              TimeRange.fromStartEnd(timeRange.start(), eventTimeRange.start(), false));
        }
        if (timeRange.end() >= durationMinutes + eventTimeRange.end()) {
          timeRanges.add(
              TimeRange.fromStartEnd(eventTimeRange.end(), timeRange.end(),
                                     timeRange.end() == TimeRange.END_OF_DAY));
        }
        timeRanges.remove(i);
      } else {
        i++;
      }
    }
  }

  private boolean attendingEvent(
      Collection<String> eventAttendees, Collection<String> requestAttendees) {
    for (String attendee : requestAttendees) {
      if (eventAttendees.contains(attendee)) {
        return true;
      }
    }
    return false;
  }
}
