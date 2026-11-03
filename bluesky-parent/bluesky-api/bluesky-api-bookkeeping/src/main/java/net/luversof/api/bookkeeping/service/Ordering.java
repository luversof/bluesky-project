package net.luversof.api.bookkeeping.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** 위 / 아래 단추 순서 바꾸기. */
final class Ordering {

  private Ordering() {}

  /** group(이미 순서대로)에서 id 를 한 칸 옮긴다. 옮겼으면 true, 끝이라 못 옮겼으면 false. */
  static <T> boolean swap(List<T> group, Function<T, UUID> idOf, UUID id, boolean up) {
    int index = -1;
    for (int i = 0; i < group.size(); i++) {
      if (idOf.apply(group.get(i)).equals(id)) {
        index = i;
        break;
      }
    }
    int other = up ? index - 1 : index + 1;
    if (index < 0 || other < 0 || other >= group.size()) {
      return false;
    }
    Collections.swap(group, index, other);
    return true;
  }
}
